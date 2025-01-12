package br.com.livelo.orderflight.proxies;

import br.com.livelo.exceptions.WebhookException;
import br.com.livelo.orderflight.client.PartnerConnectorClient;
import br.com.livelo.orderflight.domain.dto.reservation.connector.ConnectorErrorResponse;
import br.com.livelo.orderflight.domain.dto.reservation.request.PartnerReservationRequest;
import br.com.livelo.orderflight.domain.dto.reservation.response.PartnerReservationResponse;
import br.com.livelo.orderflight.domain.dtos.connector.request.PartnerConfirmOrderRequest;
import br.com.livelo.orderflight.domain.dtos.connector.response.PartnerConfirmOrderResponse;
import br.com.livelo.orderflight.domain.dtos.headers.RequiredHeaders;
import br.com.livelo.orderflight.domain.entity.OrderEntity;
import br.com.livelo.orderflight.exception.ConnectorReservationBusinessException;
import br.com.livelo.orderflight.exception.ConnectorReservationInternalException;
import br.com.livelo.orderflight.exception.OrderFlightException;
import br.com.livelo.orderflight.exception.enuns.OrderFlightErrorType;
import br.com.livelo.orderflight.mappers.ConfirmOrderMapper;
import br.com.livelo.orderflight.utils.DynatraceUtils;
import br.com.livelo.orderflight.utils.LogUtils;
import br.com.livelo.partnersconfigflightlibrary.dto.WebhookDTO;
import br.com.livelo.partnersconfigflightlibrary.services.PartnersConfigService;
import br.com.livelo.partnersconfigflightlibrary.utils.ErrorsType;
import br.com.livelo.partnersconfigflightlibrary.utils.Webhooks;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.net.URI;

import static br.com.livelo.orderflight.constants.AppConstants.INTERNAL_PARTNER_ERROR;
import static br.com.livelo.orderflight.exception.enuns.OrderFlightErrorType.*;

@Slf4j
@Component
@AllArgsConstructor
public class ConnectorPartnersProxy {
    private final PartnersConfigService partnersConfigService;
    private final PartnerConnectorClient partnerConnectorClient;
    private final ObjectMapper objectMapper;
    private final ConfirmOrderMapper confirmOrderMapper;

    @Retryable(retryFor = ConnectorReservationInternalException.class, maxAttempts = 1)
    public PartnerReservationResponse createReserve(String partnerCode, PartnerReservationRequest request, String transactionId, String userId) {
        try {
            var webhook = this.partnersConfigService.getPartnerWebhook(partnerCode, Webhooks.RESERVATION);
            var url = URI.create(webhook.getConnectorUrl());
            log.info("ConnectorPartnersProxy.createReserve: call connector partner create reserve. partner: [{}] url: [{}] request: [{}]", partnerCode, url, LogUtils.writeAsJson(request));

            ResponseEntity<PartnerReservationResponse> response = partnerConnectorClient.createReserve(url, request, transactionId, userId);
            log.info("ConnectorPartnersProxy.createReserve: reservation created on partner connector! response: [{}]", LogUtils.writeAsJson(response));

            return response.getBody();
        } catch (FeignException e) {
            var status = HttpStatus.valueOf(e.status());
            var errorType = status.is4xxClientError() ? ORDER_FLIGHT_CONNECTOR_CREATE_RESERVATION_BUSINESS_ERROR : ORDER_FLIGHT_CONNECTOR_CREATE_RESERVATION_INTERNAL_ERROR;
            throw handleFeignException(errorType, e, "ConnectorPartnersProxy.createReserve: Error on partner connector calls. httpStatus: [%s] ResponseBody: [%s]");
        } catch (WebhookException e) {
            throw handleWebhookException(e);
        } catch (Exception e) {
            throw new OrderFlightException(OrderFlightErrorType.ORDER_FLIGHT_INTERNAL_ERROR, e.getMessage(), "ConnectorPartnersProxy.createReserve: Unknown error on connector create reserve call! partner: " + partnerCode, e);
        }
    }

    public PartnerReservationResponse getReservation(String id, String transactionId, String partnerCode, String userId) {
        try {
            log.info("ConnectorPartnersProxy.getReservation: id: [{}], transactionId: [{}], partnerCode: [{}], userId: [{}]", id, transactionId, partnerCode, userId);
            var webhook = this.partnersConfigService.getPartnerWebhook(partnerCode, Webhooks.GETRESERVATION);
            var url = URI.create(webhook.getConnectorUrl().replace("{id}", id));
            log.info("ConnectorPartnersProxy.getReservation: url: [{}]", url);
            ResponseEntity<PartnerReservationResponse> response = partnerConnectorClient.getReservation(url, transactionId, userId);
            log.info("ConnectorPartnersProxy.getReservation: Reservation found on connector partner! response: [{}]", LogUtils.writeAsJson(response));
            return response.getBody();
        } catch (FeignException e) {
            var status = HttpStatus.valueOf(e.status());
            var errorType = status.is4xxClientError() ? ORDER_FLIGHT_CONNECTOR_GET_RESERVATION_BUSINESS_ERROR : ORDER_FLIGHT_CONNECTOR_GET_RESERVATION_INTERNAL_ERROR;
            throw handleFeignException(errorType, e, "ConnectorPartnersProxy.getReservation: Error on partner get reservation connector calls. httpStatus: [%s] ResponseBody: [%s]");
        } catch (WebhookException e) {
            throw handleWebhookException(e);
        } catch (Exception e) {
            throw new OrderFlightException(OrderFlightErrorType.ORDER_FLIGHT_INTERNAL_ERROR, e.getMessage(), "ConnectorPartnersProxy.getReservation: Unknown error on connector getReservation call! partner: " + partnerCode, e);
        }
    }

    public PartnerConfirmOrderResponse confirmOnPartner(String partnerCode, OrderEntity order, RequiredHeaders headers) throws OrderFlightException {

        var connectorConfirmOrderRequest = confirmOrderMapper.orderEntityToConnectorConfirmOrderRequest(order);
        try {
            log.info("ConnectorPartnersProxy.confirmOnPartner - start - id: [{}], commerceOrderId: [{}], partnerCode: [{}], connectorConfirmOrderRequest [{}]", connectorConfirmOrderRequest.getId(), connectorConfirmOrderRequest.getCommerceOrderId(), partnerCode, LogUtils.writeAsJson(connectorConfirmOrderRequest));

            WebhookDTO webhook = partnersConfigService.getPartnerWebhook(partnerCode.toUpperCase(), Webhooks.CONFIRMATION);
            var connectorUrl = webhook.getConnectorUrl().replace("{id}", connectorConfirmOrderRequest.getPartnerOrderId());
            final var connectorUri = URI.create(connectorUrl);

            ResponseEntity<PartnerConfirmOrderResponse> response = partnerConnectorClient.confirmOrder(connectorUri, connectorConfirmOrderRequest, headers.getTransactionId(), headers.getUserId());
            var connectorConfirmOrderResponse = response.getBody();
            log.info("ConnectorPartnersProxy.confirmOnPartner - end - id: [{}], commerceOrderId: [{}], response: [{}]", connectorConfirmOrderRequest.getId(), connectorConfirmOrderRequest.getCommerceOrderId(), LogUtils.writeAsJson(connectorConfirmOrderResponse));
            return connectorConfirmOrderResponse;
        } catch (FeignException exception) {
            var connectorConfirmOrderResponse = getConfirmationResponseError(exception, connectorConfirmOrderRequest);

            var message = String.format("Error on confirm order on Partner [%s]!. Order [%s] sent to PMA!", partnerCode, connectorConfirmOrderRequest.getId());
            var entries = DynatraceUtils.buildEntries(ORDER_FLIGHT_CONNECTOR_INTERNAL_ERROR, message);
            DynatraceUtils.setDynatraceErrorEntries(entries);

            log.warn("ConnectorPartnersProxy.confirmOnPartner exception - id: [{}], commerceOrderId: [{}], partnerCode: [{}], exception response: [{}]", connectorConfirmOrderRequest.getId(), connectorConfirmOrderRequest.getCommerceOrderId(), partnerCode, LogUtils.writeAsJson(connectorConfirmOrderResponse));
            return connectorConfirmOrderResponse;
        }
    }

    public PartnerConfirmOrderResponse getConfirmationOnPartner(String partnerCode, String partnerOrderId, String id) throws OrderFlightException {
        try {
            WebhookDTO webhook = partnersConfigService.getPartnerWebhook(partnerCode.toUpperCase(), Webhooks.GETCONFIRMATION);
            final var connectorUri = URI.create(webhook.getConnectorUrl().replace("{id}", partnerOrderId));
            log.info("ConnectorPartnersProxy.getConfirmationOnPartner - connectorUri - partnerOrderId: [{}], uri: [{}]", id, connectorUri);
            var connectorGetConfirmation = partnerConnectorClient.getConfirmation(connectorUri);
            PartnerConfirmOrderResponse responseBody = connectorGetConfirmation.getBody();

            log.info("ConnectorPartnersProxy.getConfirmationOnPartner - success - partnerOrderId: [{}], response: [{}]", id, LogUtils.writeAsJson(responseBody));
            return responseBody;
        } catch (FeignException exception) {
            log.error("ConnectorPartnersProxy.getConfirmationOnPartner exception - partnerOrderId: [{}], partnerCode: [{}], exception: [{}]", partnerOrderId, partnerCode, exception.getCause());
            throw new OrderFlightException(ORDER_FLIGHT_CONNECTOR_INTERNAL_ERROR, ORDER_FLIGHT_CONNECTOR_INTERNAL_ERROR.getDescription(), null, exception);
        }
    }

    public PartnerConfirmOrderResponse getVoucherOnPartner(String partnerCode, String partnerOrderId, String orderId) {
        try {
            WebhookDTO webhook = partnersConfigService.getPartnerWebhook(partnerCode.toUpperCase(), Webhooks.VOUCHER);
            final var connectorUri = URI.create(webhook.getConnectorUrl().replace("{id}", partnerOrderId));
            var connectorGetVoucher = partnerConnectorClient.getVoucher(connectorUri);

            log.info("ConnectorPartnersProxy.getVoucherOnPartner - Partner response - response: [{}] partnerOrderId: [{}] orderId: [{}]", LogUtils.writeAsJson(connectorGetVoucher.getBody()), partnerOrderId, orderId);
            return connectorGetVoucher.getBody();
        } catch (FeignException exception) {
            log.error("ConnectorPartnersProxy.getVoucherOnPartner exception - partnerOrderId: [{}], partnerCode: [{}], orderId: [{}], exception: [{}]", partnerOrderId, partnerCode, orderId, exception.getCause(), exception);
            throw new OrderFlightException(ORDER_FLIGHT_CONNECTOR_INTERNAL_ERROR, ORDER_FLIGHT_CONNECTOR_INTERNAL_ERROR.getDescription(), null, exception);
        }
    }

    private PartnerConfirmOrderResponse getConfirmationResponseError(FeignException feignException, PartnerConfirmOrderRequest connectorConfirmOrderRequest) throws OrderFlightException {
        final String content = feignException.contentUTF8();
        try {
            log.info("ConnectorPartnersProxy.getResponseError() - contentUTF8: [{}]", LogUtils.writeAsJson(content));
            var connectorConfirmOrderResponse = objectMapper.readValue(content, PartnerConfirmOrderResponse.class);
            if (connectorConfirmOrderResponse.getCurrentStatus() == null) {
                log.error("ConnectorPartnersProxy.getResponseError - ORDER_FLIGHT_CONNECTOR_INTERNAL_ERROR - id: [{}], commerceOrderId: [{}], contentUTF8: [{}]", connectorConfirmOrderRequest.getId(), connectorConfirmOrderRequest.getCommerceOrderId(), LogUtils.writeAsJson(content));
                throw new OrderFlightException(ORDER_FLIGHT_CONNECTOR_INTERNAL_ERROR, content, null);
            }

            return connectorConfirmOrderResponse;
        } catch (Exception e) {
            log.error("ConnectorPartnersProxy.getResponseError - ORDER_FLIGHT_CONNECTOR_INTERNAL_ERROR - id: [{}], commerceOrderId: [{}], contentUTF8: [{}], exception: [{}]", connectorConfirmOrderRequest.getId(), connectorConfirmOrderRequest.getCommerceOrderId(), LogUtils.writeAsJson(content), e);
            throw new OrderFlightException(ORDER_FLIGHT_CONNECTOR_INTERNAL_ERROR, content, null, e);
        }
    }

    private static OrderFlightException handleFeignException(OrderFlightErrorType errorType, FeignException e, String format) {
        var status = HttpStatus.valueOf(e.status());
        var message = String.format(format, e.status(), e.contentUTF8());
        var errorOptional = LogUtils.readFromJson(e.contentUTF8(), ConnectorErrorResponse.class);

        if(errorOptional.isPresent()) {
            var error = errorOptional.get();
            errorType = INTERNAL_PARTNER_ERROR.equals(error.code()) ? ORDER_FLIGHT_PARTNER_INTERNAL_ERROR : errorType;
        }

        if (status.is4xxClientError()) {
            log.warn("Business error on connector call url: {} status: {} body: {}", e.request().url(), e.status(), e.responseBody(), e);
            return new ConnectorReservationBusinessException(errorType, message, e);
        }
        log.warn("Internal error on connector call url: {} status: {} body: {}", e.request().url(), e.status(), e.responseBody(), e);
        return new ConnectorReservationInternalException(errorType, message, e);
    }

    private static OrderFlightException handleWebhookException(WebhookException e) {
        var orderFlightErrorType = ErrorsType.UNKNOWN.equals(e.getError()) ? ORDER_FLIGHT_CONFIG_FLIGHT_INTERNAL_ERROR : ORDER_FLIGHT_CONFIG_FLIGHT_BUSINESS_ERROR;
        var message = String.format("Error on connector calls! error: %S", e.getError());
        return new OrderFlightException(orderFlightErrorType, e.getMessage(), message, e);
    }
}
