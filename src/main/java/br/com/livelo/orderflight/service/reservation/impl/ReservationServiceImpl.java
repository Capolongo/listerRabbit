package br.com.livelo.orderflight.service.reservation.impl;

import br.com.livelo.orderflight.domain.dto.reservation.request.ReservationItem;
import br.com.livelo.orderflight.domain.dto.reservation.request.ReservationRequest;
import br.com.livelo.orderflight.domain.dto.reservation.response.PartnerReservationResponse;
import br.com.livelo.orderflight.domain.dto.reservation.response.ReservationResponse;
import br.com.livelo.orderflight.domain.dtos.pricing.request.PricingCalculateItem;
import br.com.livelo.orderflight.domain.dtos.pricing.response.PricingCalculatePrice;
import br.com.livelo.orderflight.domain.dtos.pricing.response.PricingCalculateResponse;
import br.com.livelo.orderflight.domain.entity.OrderEntity;
import br.com.livelo.orderflight.domain.entity.OrderItemEntity;
import br.com.livelo.orderflight.domain.entity.SegmentEntity;
import br.com.livelo.orderflight.exception.OrderFlightException;
import br.com.livelo.orderflight.exception.enuns.OrderFlightErrorType;
import br.com.livelo.orderflight.mappers.PriceCalculateRequestMapper;
import br.com.livelo.orderflight.mappers.PricingCalculateRequestMapper;
import br.com.livelo.orderflight.mappers.ReservationMapper;
import br.com.livelo.orderflight.proxies.ConnectorPartnersProxy;
import br.com.livelo.orderflight.proxies.PricingProxy;
import br.com.livelo.orderflight.service.order.OrderService;
import br.com.livelo.orderflight.service.reservation.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static br.com.livelo.orderflight.exception.enuns.OrderFlightErrorType.ORDER_FLIGHT_INTERNAL_ERROR;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationServiceImpl implements ReservationService {
    private final OrderService orderService;
    private final ConnectorPartnersProxy partnerConnectorProxy;
    private final PricingProxy pricingProxy;
    private final ReservationMapper reservationMapper;
    private final PriceCalculateRequestMapper priceCalculateRequestMapper;
    private static final String PARTNER_RESERVATION_SUCCESS = "LIVPNR-1007";

    public ReservationResponse createOrder(ReservationRequest request, String transactionId, String customerId,
                                           String channel, String listPrice) {
        try {
            var orderOptional = this.orderService.findByCommerceOrderId(request.getCommerceOrderId());
            if (orderOptional.isPresent()) {
                var order = orderOptional.get();
                if (this.isSameOrderItems(request, orderOptional)) {
                    var connectorReservationResponse = partnerConnectorProxy.reservationStatus(orderOptional.get().getPartnerOrderId(), transactionId, request.getPartnerCode());
                    if (PARTNER_RESERVATION_SUCCESS.equals(connectorReservationResponse.getStatus().getCode())) {
                        var pricingCalculateRequest = priceCalculateRequestMapper.toPricingCalculateRequest(order);
                        var pricingCalculateResponse = pricingProxy.calculate(pricingCalculateRequest);
                        var pricingCalculatePrice = getPricingCalculateByCommerceOrderId(request.getCommerceOrderId(), pricingCalculateResponse, listPrice);

                        //TODO atualizar os valores da preficicacao para o orderEntity
                        this.orderService.save(order);
                        return reservationMapper.toReservationResponse(order, 15);
                    }
                    throw new OrderFlightException(OrderFlightErrorType.ORDER_FLIGHT_PARTNER_RESERVATION_EXPIRED_BUSINESS_ERROR, null, null);
                } else {
                    orderOptional.ifPresent(this.orderService::delete);
                }
            }

            var partnerReservationResponse = partnerConnectorProxy
                    .createReserve(reservationMapper.toPartnerReservationRequest(request), transactionId);
            var pricingCalculateRequest = PricingCalculateRequestMapper.toPricingCalculateRequest(partnerReservationResponse, request.getCommerceOrderId());
            var pricingCalculateResponse = pricingProxy.calculate(pricingCalculateRequest);

            var pricingCalculatePrice = getPricingCalculateByCommerceOrderId(request.getCommerceOrderId(), pricingCalculateResponse, listPrice);

            var orderEntity = reservationMapper.toOrderEntity(request, partnerReservationResponse, transactionId,
                    customerId, channel, listPrice, pricingCalculatePrice);

            this.orderService.save(orderEntity);
            log.info("Order created Order: {} transactionId: {} listPrice: {}", orderEntity.toString(), transactionId,
                    listPrice);
            // deve vir do connector
            return reservationMapper.toReservationResponse(orderEntity, 15);
        } catch (OrderFlightException e) {
            throw e;
        } catch (Exception e) {
            throw new OrderFlightException(OrderFlightErrorType.ORDER_FLIGHT_INTERNAL_ERROR, e.getMessage(), null, e);
        }
    }

    private PricingCalculatePrice getPricingCalculateByCommerceOrderId(String commerceOrderId, List<PricingCalculateResponse> pricingCalculateResponses, String listPrice) {

        var pricingCalculate = pricingCalculateResponses.stream()
                .filter(pricing -> commerceOrderId.equals(pricing.getId()))
                .findFirst()
                .orElseThrow(() ->
                        new OrderFlightException(ORDER_FLIGHT_INTERNAL_ERROR, null, "Order not found in pricing response. commerceOrderId: " + commerceOrderId)
                );

        return pricingCalculate.getPrices().stream()
                .filter(price -> listPrice.equals(price.getPriceListId())).findFirst()
                .orElseThrow(() -> new OrderFlightException(ORDER_FLIGHT_INTERNAL_ERROR, null, "PriceListId not found in pricing calculate response. listPrice: " + listPrice));
    }


    public boolean isSameOrderItems(ReservationRequest request, Optional<OrderEntity> orderOptional) {
        return orderOptional.map(order -> {
            if (order.getItems().size() == request.getItems().size()) {
                var orderCommerceItemsIds = this.getOrderCommerceItemsIds(order);
                var requestItemsIds = this.getRequestItemsIds(request);

                var orderTokens = this.getOrderTokens(order);
                var requestTokens = new HashSet<>(request.getSegmentsPartnerIds());
                var isSameCommerceItemsId = requestItemsIds.containsAll(orderCommerceItemsIds);

                if (isSameCommerceItemsId) {
                    this.hasSameTokens(orderTokens, requestTokens);
                }
                return isSameCommerceItemsId;
            }
            return Boolean.FALSE;
        }).orElse(false);
    }

    private Set<String> getOrderCommerceItemsIds(OrderEntity order) {
        return order.getItems().stream()
                .map(OrderItemEntity::getCommerceItemId)
                .collect(Collectors.toSet());
    }

    private Set<String> getOrderTokens(OrderEntity order) {
        return order.getItems().stream()
                .flatMap(orderItem -> orderItem.getSegments().stream()
                        .map(SegmentEntity::getPartnerId))
                .collect(Collectors.toSet());
    }

    private Set<String> getRequestItemsIds(ReservationRequest request) {
        return request.getItems().stream()
                .map(ReservationItem::getCommerceItemId)
                .collect(Collectors.toSet());
    }

    private void hasSameTokens(Set<String> orderTokens, Set<String> requestTokens) {
        if (orderTokens.size() != requestTokens.size() || !orderTokens.containsAll(requestTokens)) {
            throw new OrderFlightException(OrderFlightErrorType.ORDER_FLIGHT_DIVERGENT_TOKEN_BUSINESS_ERROR,
                    "Tokens do parceiro divergentes", null);
        }
    }
}
