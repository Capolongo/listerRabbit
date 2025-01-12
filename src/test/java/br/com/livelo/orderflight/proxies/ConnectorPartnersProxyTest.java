package br.com.livelo.orderflight.proxies;

import br.com.livelo.exceptions.WebhookException;
import br.com.livelo.orderflight.client.PartnerConnectorClient;
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
import br.com.livelo.orderflight.mock.MockBuilder;
import br.com.livelo.partnersconfigflightlibrary.dto.WebhookDTO;
import br.com.livelo.partnersconfigflightlibrary.services.impl.PartnersConfigServiceImpl;
import br.com.livelo.partnersconfigflightlibrary.utils.Webhooks;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static br.com.livelo.orderflight.exception.enuns.OrderFlightErrorType.*;
import static br.com.livelo.partnersconfigflightlibrary.utils.ErrorsType.PARTNER_INACTIVE;
import static br.com.livelo.partnersconfigflightlibrary.utils.ErrorsType.UNKNOWN;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConnectorPartnersProxyTest {

    @Mock
    private PartnersConfigServiceImpl partnersConfigService;
    @Mock
    private PartnerConnectorClient partnerConnectorClient;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private ConfirmOrderMapper confirmOrderMapper;

    @InjectMocks
    private ConnectorPartnersProxy proxy;

    private void setup() {
        WebhookDTO webhook = WebhookDTO.builder().connectorUrl("http://url-mock.com").name("name").build();
        when(partnersConfigService.getPartnerWebhook(anyString(), any(Webhooks.class))).thenReturn(webhook);
    }

    @Test
    void shouldReturnConfirmOnPartner() {
        ResponseEntity<PartnerConfirmOrderResponse> confirmOrderResponse = MockBuilder.connectorConfirmOrderResponse();
        setup();
        when(confirmOrderMapper.orderEntityToConnectorConfirmOrderRequest(any(OrderEntity.class))).thenReturn(MockBuilder.connectorConfirmOrderRequest());
        when(partnerConnectorClient.confirmOrder(any(URI.class), any(PartnerConfirmOrderRequest.class), anyString(), anyString()))
                .thenReturn(confirmOrderResponse);

        PartnerConfirmOrderResponse response = proxy.confirmOnPartner("CVC", MockBuilder.orderEntity(), new RequiredHeaders("", ""));

        assertEquals(confirmOrderResponse.getBody(), response);
        assertEquals(200, confirmOrderResponse.getStatusCode().value());
        verify(partnerConnectorClient).confirmOrder(any(URI.class), any(PartnerConfirmOrderRequest.class), anyString(), anyString());
        verifyNoMoreInteractions(partnerConnectorClient);
    }

    @Test
    void shouldReturnFailedWhenCatchFeignException() throws OrderFlightException, JsonProcessingException {
        FeignException mockException = Mockito.mock(FeignException.class);
        setup();

        when(confirmOrderMapper.orderEntityToConnectorConfirmOrderRequest(any(OrderEntity.class))).thenReturn(MockBuilder.connectorConfirmOrderRequest());

        when(mockException.contentUTF8())
                .thenReturn(
                        new String(MockBuilder.connectorConfirmOrderResponse().getBody().toString().getBytes(),
                                StandardCharsets.UTF_8));

        when(objectMapper.readValue(anyString(), eq(PartnerConfirmOrderResponse.class)))
                .thenReturn(MockBuilder.connectorConfirmOrderResponse().getBody());

        when(partnerConnectorClient.confirmOrder(any(URI.class),
                any(PartnerConfirmOrderRequest.class), anyString(), anyString()))
                .thenThrow(mockException);

        PartnerConfirmOrderResponse response = proxy.confirmOnPartner("CVC",
                MockBuilder.orderEntity(), new RequiredHeaders("", ""));

        assertEquals(MockBuilder.connectorConfirmOrderResponse().getBody(),
                response);
    }

    @Test
    void shouldThrowException() {
        Exception exception = assertThrows(Exception.class, () -> {
            proxy.confirmOnPartner("CVC",
                    MockBuilder.orderEntity(), new RequiredHeaders("", ""));
        });

        assertTrue(exception.getMessage().contains("Cannot invoke"));
    }

    @Test
    void shouldThrowOrderFlightExceptionOnConfirmOnPartner() throws OrderFlightException, JsonProcessingException {
        FeignException mockException = Mockito.mock(FeignException.class);
        when(mockException.contentUTF8())
                .thenReturn(
                        new String(MockBuilder.connectorConfirmOrderResponse().getBody().toString().getBytes(),
                                StandardCharsets.UTF_8));

        var mock = MockBuilder.connectorConfirmOrderResponse().getBody();
        mock.setCurrentStatus(null);

        when(confirmOrderMapper.orderEntityToConnectorConfirmOrderRequest(any(OrderEntity.class))).thenReturn(MockBuilder.connectorConfirmOrderRequest());

        when(objectMapper.readValue(anyString(), eq(PartnerConfirmOrderResponse.class)))
                .thenReturn(mock);

        when(partnerConnectorClient.confirmOrder(any(URI.class),
                any(PartnerConfirmOrderRequest.class), anyString(), anyString()))
                .thenThrow(mockException);

        when(partnersConfigService.getPartnerWebhook("CVC", Webhooks.CONFIRMATION))
                .thenReturn(WebhookDTO.builder().connectorUrl("www").build());

        try {
            proxy.confirmOnPartner("CVC", MockBuilder.orderEntity(), new RequiredHeaders("", ""));
        } catch (OrderFlightException exception) {
            assertEquals(OrderFlightErrorType.ORDER_FLIGHT_CONNECTOR_INTERNAL_ERROR, exception.getOrderFlightErrorType());
        }
    }

    @Test
    void shouldMakeReservation() {
        var request = mock(PartnerReservationRequest.class);
        var response = mock(PartnerReservationResponse.class);
        setup();

        when(partnerConnectorClient.createReserve(any(), any(), any(), anyString()))
                .thenReturn(ResponseEntity.ok(response));

        PartnerReservationResponse result = proxy.createReserve("cvc", request, "transactionID", "userId");
        assertNotNull(result);
    }

    @Test
    void shouldThrowsException_WhenFeignResponse5xxStatus() {
        var request = mock(PartnerReservationRequest.class);
        setup();
        var feignException = makeFeignMockExceptionWithStatus(500);
        makeException(request, feignException);
        var requestMock = mock(Request.class);

        doReturn("{\"code\": \"CONNECTOR_BUSINESS_ERROR\", \"message\": \"Internal partner error\", \"details\": {\"error\": \"Error on CVC!\"}}").when(feignException).contentUTF8();
        doReturn(requestMock).when(feignException).request();
        doReturn("http://test").when(requestMock).url();

        var exception = assertThrows(ConnectorReservationInternalException.class,
                () -> proxy.createReserve("cvc", request, "transactionId", "userId"));

        assertEquals(OrderFlightErrorType.ORDER_FLIGHT_CONNECTOR_CREATE_RESERVATION_INTERNAL_ERROR, exception.getOrderFlightErrorType());
    }

    @Test
    void shouldThrowsException_WhenFeignExceptionResponseIsDifferentOf5xxxStatus() {
        var request = mock(PartnerReservationRequest.class);
        var feignException = makeFeignMockExceptionWithStatus(400);

        var requestMock = mock(Request.class);

        doReturn("{\"code\": \"CONNECTOR_BUSINESS_ERROR\", \"message\": \"Internal partner error\", \"details\": {\"error\": \"Error on CVC!\"}}").when(feignException).contentUTF8();
        doReturn(requestMock).when(feignException).request();
        doReturn("http://test").when(requestMock).url();
        setup();
        makeException(request, feignException);

        var exception = assertThrows(ConnectorReservationBusinessException.class,
                () -> proxy.createReserve("cvc", request, "transactionId", "userId"));

        assertEquals(ORDER_FLIGHT_CONNECTOR_CREATE_RESERVATION_BUSINESS_ERROR, exception.getOrderFlightErrorType());
    }

    @Test
    void shouldThrowsOrderFlightException_WhenFeignExceptionResponseIsDifferentOf5xxxStatus() {
        var request = mock(PartnerReservationRequest.class);
        setup();
        doThrow(OrderFlightException.class).when(partnerConnectorClient).createReserve(any(), any(), anyString(), anyString());

        assertThrows(OrderFlightException.class, () -> proxy.createReserve("cvc", request, "transactionId", "userId"));
    }

    @Test
    void shouldThrowException_WhenFeignReturnSomethingWrong() {
        var request = mock(PartnerReservationRequest.class);
        setup();
        when(partnerConnectorClient.createReserve(any(), any(), any(), any())).thenThrow(new RuntimeException("Simulated internal error"));

        assertThrows(OrderFlightException.class,
                () -> proxy.createReserve("cvc", mock(PartnerReservationRequest.class), "transactionId", "userId"));

        var exception = assertThrows(OrderFlightException.class,
                () -> proxy.createReserve("cvc", request, "transactionId", "userId"));

        assertEquals(ORDER_FLIGHT_INTERNAL_ERROR, exception.getOrderFlightErrorType());
    }

    @Test
    void shouldThrowFlightConnectorBusinessError_WhenThereIsBadRequest() {
        var request = mock(PartnerReservationRequest.class);
        var feignException = makeFeignMockExceptionWithStatus(400);
        var requestMock = mock(Request.class);

        doReturn("{\"code\": \"CONNECTOR_BUSINESS_ERROR\", \"message\": \"Internal partner error\", \"details\": {\"error\": \"Error on CVC!\"}}").when(feignException).contentUTF8();
        WebhookDTO webhook = WebhookDTO.builder().connectorUrl("http://url-mock.com").name("name").build();
        when(partnersConfigService.getPartnerWebhook(anyString(), any(Webhooks.class))).thenReturn(webhook);
        makeException(request, feignException);
        doReturn(requestMock).when(feignException).request();
        doReturn("http://test").when(requestMock).url();

        var exception = assertThrows(ConnectorReservationBusinessException.class,
                () -> proxy.createReserve("cvc", request, "transactionId", "userId"));

        assertEquals(ORDER_FLIGHT_CONNECTOR_CREATE_RESERVATION_BUSINESS_ERROR, exception.getOrderFlightErrorType());
    }

    @Test
    void shouldThrowFlightConnectorInternalError_WhenThereIsSomeInternalError() {
        var request = mock(PartnerReservationRequest.class);
        var feignException = makeFeignMockExceptionWithStatus(400);
        var requestMock = mock(Request.class);

        doReturn("{\"code\": \"CONNECTOR_BUSINESS_ERROR\", \"message\": \"Internal partner error\", \"details\": {\"error\": \"Error on CVC!\"}}").when(feignException).contentUTF8();
        doReturn(requestMock).when(feignException).request();
        doReturn("http://test").when(requestMock).url();
        makeException(request, feignException);
        setup();
        var exception = assertThrows(OrderFlightException.class,
                () -> proxy.createReserve("cvc", request, "transactionId", "userId"));

        assertEquals(OrderFlightErrorType.ORDER_FLIGHT_CONNECTOR_CREATE_RESERVATION_BUSINESS_ERROR, exception.getOrderFlightErrorType());

    }

    @Test
    void shouldThrowFlightConnectorInternalError_WhenThereIsSomeInternalErrorAndBodyIsNull() {
        var request = mock(PartnerReservationRequest.class);
        var feignException = makeFeignMockExceptionWithStatus(400);
        var requestMock = mock(Request.class);

        doReturn("").when(feignException).contentUTF8();
        doReturn(requestMock).when(feignException).request();
        doReturn("http://test").when(requestMock).url();
        makeException(request, feignException);
        setup();
        var exception = assertThrows(OrderFlightException.class,
                () -> proxy.createReserve("cvc", request, "transactionId", "userId"));

        assertEquals(OrderFlightErrorType.ORDER_FLIGHT_CONNECTOR_CREATE_RESERVATION_BUSINESS_ERROR, exception.getOrderFlightErrorType());

    }

    @Test
    void shouldThrowPartnerInternalError_WhenThereIsSomePartnerInternalError() {
        var request = mock(PartnerReservationRequest.class);
        var feignException = makeFeignMockExceptionWithStatus(400);
        var requestMock = mock(Request.class);

        doReturn("{\"code\": \"INTERNAL_PARTNER_ERROR\", \"message\": \"Internal partner error\", \"details\": {\"error\": \"Error on CVC!\"}}").when(feignException).contentUTF8();
        doReturn(requestMock).when(feignException).request();
        doReturn("http://test").when(requestMock).url();
        makeException(request, feignException);
        setup();
        var exception = assertThrows(OrderFlightException.class,
                () -> proxy.createReserve("cvc", request, "transactionId", "userId"));

        assertEquals(ORDER_FLIGHT_PARTNER_INTERNAL_ERROR, exception.getOrderFlightErrorType());

    }

    @Test
    void shouldThrowInternalErrorException_WhenCallPartnerWebhookErrorTypeIsUnknown() {
        var request = mock(PartnerReservationRequest.class);
        var exceptionExpected = mock(WebhookException.class);
        doThrow(exceptionExpected).when(partnersConfigService).getPartnerWebhook(any(), any());

        when(exceptionExpected.getError()).thenReturn(UNKNOWN);

        var exception = assertThrows(OrderFlightException.class,
                () -> proxy.createReserve("cvc", request, "transactionId", "userId"));

        assertEquals(ORDER_FLIGHT_CONFIG_FLIGHT_INTERNAL_ERROR, exception.getOrderFlightErrorType());
    }

    @Test
    void shouldThrowBusinessErrorException_WhenCallPartnerInactive() {
        var request = mock(PartnerReservationRequest.class);
        var exceptionExpected = mock(WebhookException.class);

        doThrow(exceptionExpected).when(partnersConfigService).getPartnerWebhook(any(), any());

        when(exceptionExpected.getError()).thenReturn(PARTNER_INACTIVE);

        var exception = assertThrows(OrderFlightException.class,
                () -> proxy.createReserve("cvc", request, "transactionId", "userId"));

        assertEquals(ORDER_FLIGHT_CONFIG_FLIGHT_BUSINESS_ERROR, exception.getOrderFlightErrorType());
    }

    @Test
    void shouldGetReservation() {
        var expected = mock(PartnerReservationResponse.class);

        WebhookDTO webhook = WebhookDTO.builder().connectorUrl("http://url-mock.com").name("name").build();
        when(partnersConfigService.getPartnerWebhook(anyString(),
                any(Webhooks.class)))
                .thenReturn(webhook);
        when(partnerConnectorClient.getReservation(any(), any(), any()))
                .thenReturn(ResponseEntity.ok(expected));
        var response = this.proxy.getReservation("123", "123", "123", "123");
        assertEquals(expected, response);
    }

    @Test
    void shouldThrowInternalError_WhenGetReservation() {
        var exceptionExpected = new WebhookException(UNKNOWN, "");

        doThrow(exceptionExpected).when(partnersConfigService).getPartnerWebhook(any(), any());
        var exception = assertThrows(OrderFlightException.class,
                () -> this.proxy.getReservation("123", "123", "123", ""));
        assertEquals(ORDER_FLIGHT_CONFIG_FLIGHT_INTERNAL_ERROR, exception.getOrderFlightErrorType());
    }

    @Test
    void shouldThrowBusinessError_WhenGetReservation() {

        var exceptionExpected = new WebhookException(PARTNER_INACTIVE, "");

        doThrow(exceptionExpected).when(partnersConfigService).getPartnerWebhook(any(), any());
        var exception = assertThrows(OrderFlightException.class,
                () -> this.proxy.getReservation("123", "123", "123", ""));
        assertEquals(ORDER_FLIGHT_CONFIG_FLIGHT_BUSINESS_ERROR, exception.getOrderFlightErrorType());
    }

    @Test
    void shouldThrowConnectorBusinessError_WhenStatus400() {

        var exceptionExpected = mock(FeignException.class);
        var requestMock = mock(Request.class);

        doReturn(400).when(exceptionExpected).status();
        doReturn("{\"code\": \"CONNECTOR_BUSINESS_ERROR\", \"message\": \"Internal partner error\", \"details\": {\"error\": \"Error on CVC!\"}}").when(exceptionExpected).contentUTF8();
        doThrow(exceptionExpected).when(partnersConfigService).getPartnerWebhook(any(), any());
        doReturn(requestMock).when(exceptionExpected).request();
        doReturn("http://test").when(requestMock).url();
        var exception = assertThrows(OrderFlightException.class,
                () -> this.proxy.getReservation("123", "123", "123", ""));
        assertEquals(ORDER_FLIGHT_CONNECTOR_GET_RESERVATION_BUSINESS_ERROR, exception.getOrderFlightErrorType());
    }

    @Test
    void shouldThrowConnectorInternalError_WhenStatus500() {
        var segmentsPartnerIds = List.of("asdf", "fdsa");
        var exceptionExpected = mock(FeignException.class);
        var requestMock = mock(Request.class);

        doReturn(500).when(exceptionExpected).status();
        doReturn("{\"code\": \"CONNECTOR_INTERNAL_ERROR\", \"message\": \"Internal partner error\", \"details\": {\"error\": \"Error on CVC!\"}}").when(exceptionExpected).contentUTF8();
        doThrow(exceptionExpected).when(partnersConfigService).getPartnerWebhook(any(), any());
        doReturn(requestMock).when(exceptionExpected).request();
        doReturn("http://test").when(requestMock).url();
        var exception = assertThrows(OrderFlightException.class,
                () -> this.proxy.getReservation("123", "123", "123", ""));
        assertEquals(ORDER_FLIGHT_CONNECTOR_GET_RESERVATION_INTERNAL_ERROR, exception.getOrderFlightErrorType());
    }

    @Test
    void shouldThrowPartnerInternalError_WhenStatus500() {
        var exceptionExpected = mock(FeignException.class);
        var requestMock = mock(Request.class);

        doReturn(500).when(exceptionExpected).status();
        doReturn("{\"code\": \"INTERNAL_PARTNER_ERROR\", \"message\": \"Internal partner error\", \"details\": {\"error\": \"Error on CVC!\"}}").when(exceptionExpected).contentUTF8();
        doThrow(exceptionExpected).when(partnersConfigService).getPartnerWebhook(any(), any());
        doReturn(requestMock).when(exceptionExpected).request();
        doReturn("http://test").when(requestMock).url();
        var exception = assertThrows(OrderFlightException.class,
                () -> this.proxy.getReservation("123", "123", "123", ""));
        assertEquals(ORDER_FLIGHT_PARTNER_INTERNAL_ERROR, exception.getOrderFlightErrorType());
    }

    @Test
    void shouldThrowInternalError_WhenUnknownError() {
        var exception = assertThrows(OrderFlightException.class,
                () -> this.proxy.getReservation("123", "123", "123", ""));
        assertEquals(ORDER_FLIGHT_INTERNAL_ERROR, exception.getOrderFlightErrorType());
    }

    private void makeException(PartnerReservationRequest partnerReservationRequest, FeignException feignException) {
        when(partnerConnectorClient.createReserve(any(), any(), any(), anyString())).thenThrow(feignException);
    }

    private FeignException makeFeignMockExceptionWithStatus(Integer statusCode) {
        var feignException = Mockito.mock(FeignException.class);
        Mockito.when(feignException.status()).thenReturn(statusCode);
        return feignException;
    }

    @Test
    void shouldReturnGetConfirmationOnPartner() throws Exception {
        ResponseEntity<PartnerConfirmOrderResponse> confirmOrderResponse = MockBuilder.connectorConfirmOrderResponse();
        setup();
        when(partnerConnectorClient.getConfirmation(any(URI.class))).thenReturn(confirmOrderResponse);

        PartnerConfirmOrderResponse response = proxy.getConfirmationOnPartner("CVC", "10071014", "lf123");

        assertEquals(confirmOrderResponse.getBody(), response);
        assertEquals(200, confirmOrderResponse.getStatusCode().value());
        verify(partnerConnectorClient).getConfirmation(any(URI.class));
        verifyNoMoreInteractions(partnerConnectorClient);
    }

    @Test
    void shouldThrowExceptionGetConfirmationOnPartner() {
        setup();
        when(partnerConnectorClient.getConfirmation(any(URI.class))).thenThrow(FeignException.class);

        assertThrows(OrderFlightException.class, () -> {
            proxy.getConfirmationOnPartner("CVC", "10071014", "lf123");
        });
    }

    @Test
    void shouldReturnGetVoucherOnPartner() throws Exception {
        ResponseEntity<PartnerConfirmOrderResponse> voucherResponse = MockBuilder.connectorVoucherResponse();
        setup();
        when(partnerConnectorClient.getVoucher(any(URI.class))).thenReturn(voucherResponse);

        PartnerConfirmOrderResponse response = proxy.getVoucherOnPartner("CVC", "partnerOrderId", "orderId");

        assertEquals(voucherResponse.getBody(), response);
        assertEquals(200, voucherResponse.getStatusCode().value());
        verify(partnerConnectorClient).getVoucher(any(URI.class));
        verifyNoMoreInteractions(partnerConnectorClient);
    }

    @Test
    void shouldThrowExceptionGetVoucherOnPartner() {
        setup();
        when(partnerConnectorClient.getVoucher(any(URI.class))).thenThrow(FeignException.class);

        assertThrows(OrderFlightException.class, () -> {
            proxy.getVoucherOnPartner("CVC", "10071014", "lf123");
        });
    }
}
