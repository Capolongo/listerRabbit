package br.com.livelo.orderflight.service.reservation.impl;

import br.com.livelo.orderflight.domain.dto.reservation.request.ReservationItem;
import br.com.livelo.orderflight.domain.dto.reservation.request.ReservationRequest;
import br.com.livelo.orderflight.domain.dto.reservation.response.PartnerReservationOrdersPriceDescriptionFlight;
import br.com.livelo.orderflight.domain.dto.reservation.response.PartnerReservationOrdersPriceDescriptionTaxes;
import br.com.livelo.orderflight.domain.dto.reservation.response.PartnerReservationResponse;
import br.com.livelo.orderflight.domain.dto.reservation.response.ReservationResponse;
import br.com.livelo.orderflight.domain.dtos.pricing.response.PricingCalculateFlight;
import br.com.livelo.orderflight.domain.dtos.pricing.response.PricingCalculatePrice;
import br.com.livelo.orderflight.domain.dtos.pricing.response.PricingCalculateResponse;
import br.com.livelo.orderflight.domain.dtos.pricing.response.PricingCalculateTaxes;
import br.com.livelo.orderflight.domain.entity.*;
import br.com.livelo.orderflight.exception.OrderFlightException;
import br.com.livelo.orderflight.exception.enuns.OrderFlightErrorType;
import br.com.livelo.orderflight.mappers.PricingCalculateRequestMapper;
import br.com.livelo.orderflight.mappers.ReservationMapper;
import br.com.livelo.orderflight.proxies.ConnectorPartnersProxy;
import br.com.livelo.orderflight.proxies.PricingProxy;
import br.com.livelo.orderflight.service.order.OrderService;
import br.com.livelo.orderflight.service.reservation.ReservationService;
import br.com.livelo.orderflight.utils.LogUtils;
import br.com.livelo.orderflight.utils.OrderItemUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static br.com.livelo.orderflight.enuns.StatusLivelo.INITIAL;
import static br.com.livelo.orderflight.exception.enuns.OrderFlightErrorType.ORDER_FLIGHT_INTERNAL_ERROR;
import static br.com.livelo.orderflight.exception.enuns.OrderFlightErrorType.ORDER_FLIGHT_ORDER_STATUS_INVALID_BUSINESS_ERROR;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationServiceImpl implements ReservationService {
    private final OrderService orderService;
    private final ConnectorPartnersProxy partnerConnectorProxy;
    private final PricingProxy pricingProxy;
    private final ReservationMapper reservationMapper;
    private final PricingCalculateRequestMapper pricingCalculateRequestMapper;


    public ReservationResponse createOrder(ReservationRequest request, String transactionId, String customerId, String channel, String listPriceId, String userId) {
        log.info("ReservationServiceImpl.createOrder - Creating Order: {} transactionId: {} listPriceId: {} transactionId: {}", LogUtils.writeAsJson(request), transactionId, listPriceId, userId);
        OrderEntity order = null;
        try {
            PartnerReservationResponse partnerReservationResponse = null;

            OrderItemUtils.hasMoreThanOneTravel(request.getItems());

            var commerceItemsIds = request.getItems().stream().map(ReservationItem::getCommerceItemId).collect(Collectors.toList());
            commerceItemsIds.add(request.getCommerceOrderId());

            log.info("Validating existence of order on db. ids: {}", commerceItemsIds);
            var orderOptional = this.orderService.findByCommerceOrderIdInAndExpirationDateAfter(commerceItemsIds);
            if (orderOptional.isPresent()) {
                order = orderOptional.get();
                log.info("ReservationServiceImpl.createOrder order exists in db! Validating status and reservation on partner. orderId: {}", order.getId());
                this.isOrderStatusInitial(order);

                if (this.isSameOrderItems(request, orderOptional)) {
                    log.info("ReservationServiceImpl.getPartnerOrder - Validating reservation status on partner. orderId: {}", order.getId());
                    partnerReservationResponse = this.getPartnerOrder(orderOptional.get().getPartnerOrderId(), transactionId, request.getPartnerCode(), userId);

                    if (!INITIAL.getCode().equals(partnerReservationResponse.getCurrentStatus().getCode())) {
                        this.updateStatus(order, partnerReservationResponse);
                        var message = String.format("Reservation not valid in partner. partnerStatus: %s", partnerReservationResponse.getCurrentStatus().getCode());
                        throw new OrderFlightException(OrderFlightErrorType.ORDER_FLIGHT_PARTNER_RESERVATION_EXPIRED_BUSINESS_ERROR, null, message);
                    }

                    order.setCommerceOrderId(request.getCommerceOrderId());
                    this.validateAndSetAmounts(partnerReservationResponse, order);
                    log.info("Order reserved on partner! Proceed with pricing. {}! order: {} transactionId: {}", request.getPartnerCode(), request.getCommerceOrderId(), transactionId);
                } else {
                    log.info("ReservationServiceImpl.createOrder - order has items differents. Deleting and recreating order. orderId: {}", order.getId());
                    this.orderService.delete(order);
                    order = null;
                }
            }

            if (!this.existsReservationInPartner(partnerReservationResponse)) {
                request.getItems().sort(Comparator.comparing(ReservationItem::getSkuId));
                var partnerReservationRequest = reservationMapper.toPartnerReservationRequest(request);
                partnerReservationResponse = partnerConnectorProxy.createReserve(request.getPartnerCode(), partnerReservationRequest, transactionId, userId);
            }

            if (this.isNewOrder(order)) {
                order = reservationMapper.toOrderEntity(request, partnerReservationResponse, transactionId, customerId, channel, listPriceId);
            }

            var pricingCalculatePrice = this.priceOrder(request, partnerReservationResponse, transactionId, userId);
            this.setPrices(order, pricingCalculatePrice, listPriceId);

            addPartnerOrderLinkIdToItems(order.getPartnerCode(), order.getCommerceOrderId(), order.getItems());
            this.orderService.save(order);

            log.info("ReservationServiceImpl.createOrder - Order created Order: {} transactionId: {} listPriceId: {}", LogUtils.writeAsJson(order), transactionId, listPriceId);
            return reservationMapper.toReservationResponse(order, 15);
        } catch (OrderFlightException e) {
            throw e;
        } catch (Exception e) {
            throw new OrderFlightException(OrderFlightErrorType.ORDER_FLIGHT_INTERNAL_ERROR, e.getMessage(), "Unknown error on create reservation!", e);
        }
    }

    private void updateStatus(OrderEntity order, PartnerReservationResponse partnerReservationResponse) {
        order.getStatusHistory().add(OrderStatusHistoryEntity.builder()
                .code(partnerReservationResponse.getCurrentStatus().getCode())
                .description(partnerReservationResponse.getCurrentStatus().getDescription())
                .partnerCode(partnerReservationResponse.getCurrentStatus().getPartnerCode())
                .partnerDescription(partnerReservationResponse.getCurrentStatus().getPartnerDescription())
                .build());

        order.setCurrentStatus(OrderCurrentStatusEntity.builder()
                .code(partnerReservationResponse.getCurrentStatus().getCode())
                .description(partnerReservationResponse.getCurrentStatus().getDescription())
                .partnerCode(partnerReservationResponse.getCurrentStatus().getPartnerCode())
                .partnerDescription(partnerReservationResponse.getCurrentStatus().getPartnerDescription()).build());

        this.orderService.save(order);
    }

    private void isOrderStatusInitial(OrderEntity order) {
        if (!INITIAL.getCode().equals(order.getCurrentStatus().getCode())) {
            throw new OrderFlightException(ORDER_FLIGHT_ORDER_STATUS_INVALID_BUSINESS_ERROR, null, "Order has not initial status. Aborting create order");
        }
    }

    private boolean isNewOrder(OrderEntity order) {
        return Objects.isNull(order);
    }

    private boolean existsReservationInPartner(PartnerReservationResponse partnerReservationResponse) {
        return Objects.nonNull(partnerReservationResponse);
    }

    private List<PricingCalculatePrice> priceOrder(ReservationRequest request, PartnerReservationResponse partnerReservationResponse, String transactionId, String userId) {
        var pricingCalculateRequest = pricingCalculateRequestMapper.toPricingCalculateRequest(partnerReservationResponse, request.getCommerceOrderId(), request.getPartnerCode());

        log.info("ReservationServiceImpl.priceOrder - {} isInternational", pricingCalculateRequest.getTravelInfo().getIsInternational());
        var pricingCalculateResponse = pricingProxy.calculate(pricingCalculateRequest, transactionId, userId);

        return getPricingCalculateByCommerceOrderId(request.getCommerceOrderId(), pricingCalculateResponse);
    }

    private PartnerReservationResponse getPartnerOrder(String partnerOrderId, String transactionId, String partnerCode, String userId) {
        return partnerConnectorProxy.getReservation(partnerOrderId, transactionId, partnerCode, userId);
    }

    private List<PricingCalculatePrice> getPricingCalculateByCommerceOrderId(String commerceOrderId, List<PricingCalculateResponse> pricingCalculateResponses) {

        var pricingCalculate = pricingCalculateResponses.stream()
                .filter(pricing -> commerceOrderId.equals(pricing.getId()))
                .findFirst()
                .orElseThrow(() ->
                        new OrderFlightException(ORDER_FLIGHT_INTERNAL_ERROR, null, "ReservationServiceImpl.getPricingCalculateByCommerceOrderId - Order not found in pricing response. commerceOrderId: " + commerceOrderId)
                );

        return pricingCalculate.getPrices();
    }

    private void setPrices(OrderEntity order, List<PricingCalculatePrice> prices, String listPrice) {
        var clientPrice = prices.stream()
                .filter(price -> listPrice.equals(price.getPriceListId()))
                .findFirst()
                .orElseThrow(() ->
                        new OrderFlightException(
                                ORDER_FLIGHT_INTERNAL_ERROR,
                                null,
                                "ReservationServiceImpl.getPricingCalculateByCommerceOrderId - PriceListId not found in pricing calculate response. listPrice: " + listPrice
                        )
                );
        order.getPrice().setPointsAmount(clientPrice.getPointsAmount());
        order.getPrice().setAccrualPoints(clientPrice.getAccrualPoints());
        order.getPrice().setAmount(clientPrice.getAmount());
        this.setOrderPriceDescription(order, clientPrice);

        this.setOrderItemsPrice(order, prices, listPrice);
    }

    private void setOrderItemsPrice(OrderEntity order, List<PricingCalculatePrice> prices, String listPrice) {
        var clientPrice = prices.stream()
                .filter(price -> listPrice.equals(price.getPriceListId()))
                .findFirst()
                .orElseThrow(() ->
                        new OrderFlightException(
                                ORDER_FLIGHT_INTERNAL_ERROR,
                                null,
                                "ReservationServiceImpl.getPricingCalculateByCommerceOrderId - PriceListId not found in pricing calculate response. listPrice: " + listPrice
                        )
                );

        order.getItems()
                .forEach(item -> {
                    if (orderService.isFlightItem(item)) {
                        item.getPrice().setPointsAmount(clientPrice.getFlight().getPointsAmount());
                        item.getPrice().setAmount(clientPrice.getFlight().getAmount());
                        item.getPrice().setMultiplier(clientPrice.getFlight().getMultiplier());
                        item.getPrice().setMultiplierAccrual(clientPrice.getFlight().getMultiplierAccrual());
                        item.getPrice().setMarkup(clientPrice.getFlight().getMarkup());
                        item.getPrice().setAccrualPoints(clientPrice.getAccrualPoints());

                        if (item.getPrice().getPricesModalities() == null) {
                            this.buildPricesModalities(
                                    prices,
                                    item,
                                    clientPrice.getFlight().getAmount(),
                                    clientPrice.getFlight().getPointsAmount(),
                                    clientPrice.getFlight().getMultiplier(),
                                    clientPrice.getFlight().getMultiplierAccrual(),
                                    clientPrice.getFlight().getMarkup()
                            );
                        } else {
                            this.setPricesModalitiesValues(
                                    prices,
                                    item,
                                    clientPrice.getFlight().getAmount(),
                                    clientPrice.getFlight().getPointsAmount(),
                                    clientPrice.getFlight().getMultiplier(),
                                    clientPrice.getFlight().getMultiplierAccrual(),
                                    clientPrice.getFlight().getMarkup()
                            );
                        }
                    }

                    if (!orderService.isFlightItem(item)) {
                        item.getPrice().setPointsAmount(clientPrice.getTaxes().getPointsAmount());
                        item.getPrice().setMultiplier(clientPrice.getTaxes().getMultiplier());
                        item.getPrice().setMultiplierAccrual(clientPrice.getTaxes().getMultiplierAccrual());
                        item.getPrice().setMarkup(clientPrice.getTaxes().getMarkup());
                        item.getPrice().setAmount(clientPrice.getTaxes().getAmount());

                        if (item.getPrice().getPricesModalities() == null) {
                            this.buildPricesModalities(
                                    prices,
                                    item,
                                    clientPrice.getTaxes().getAmount(),
                                    clientPrice.getTaxes().getPointsAmount(),
                                    clientPrice.getTaxes().getMultiplier(),
                                    clientPrice.getTaxes().getMultiplierAccrual(),
                                    clientPrice.getTaxes().getMarkup()
                            );
                        } else {
                            this.setPricesModalitiesValues(
                                    prices,
                                    item,
                                    clientPrice.getTaxes().getAmount(),
                                    clientPrice.getTaxes().getPointsAmount(),
                                    clientPrice.getTaxes().getMultiplier(),
                                    clientPrice.getTaxes().getMultiplierAccrual(),
                                    clientPrice.getTaxes().getMarkup()
                            );
                        }

                    }
                });
    }

    private void setPricesModalitiesValues(List<PricingCalculatePrice> prices, OrderItemEntity item, BigDecimal amount, BigDecimal pointsAmount, Float multiplier, Float multiplierAccrual, Float markup) {
        prices.forEach(priceItem -> {
            PriceModalityEntity priceModalityEntity = findModalityByPriceList(item.getPrice().getPricesModalities(), priceItem.getPriceListId());
            priceModalityEntity.setAmount(amount);
            priceModalityEntity.setMultiplier(multiplier);
            priceModalityEntity.setMultiplierAccrual(multiplierAccrual);
            priceModalityEntity.setMarkup(markup);
            priceModalityEntity.setAccrualPoints(priceItem.getAccrualPoints().doubleValue());
            priceModalityEntity.setPointsAmount(pointsAmount);
        });
    }

    private void buildPricesModalities(List<PricingCalculatePrice> prices, OrderItemEntity item, BigDecimal amount, BigDecimal pointsAmount, Float multiplier, Float multiplierAccrual, Float markup) {
        var pricesModalities = prices.stream()
                .map(price -> PriceModalityEntity.builder()
                        .amount(amount)
                        .pointsAmount(pointsAmount)
                        .multiplier(multiplier)
                        .multiplierAccrual(multiplierAccrual)
                        .markup(markup)
                        .accrualPoints(price.getAccrualPoints().doubleValue())
                        .priceListId(price.getPriceListId())
                        .build())
                .collect(Collectors.toSet());

        item.getPrice().setPricesModalities(pricesModalities);
    }

    private PriceModalityEntity findModalityByPriceList(Set<PriceModalityEntity> modalityEntities, String priceListId) {
        return modalityEntities.stream().filter(item -> priceListId.equals(item.getPriceListId()))
                .findFirst().orElse(null);
    }

    private void setOrderPriceDescription(OrderEntity order, PricingCalculatePrice price) {
        for (PricingCalculateFlight pricingCalculateFlight : price.getPricesDescription().getFlights()) {
            order.getPrice().getOrdersPriceDescription()
                    .forEach(priceDescription -> {
                        if (pricingCalculateFlight.getPassengerType().equals(priceDescription.getType())) {
                            priceDescription.setPointsAmount(pricingCalculateFlight.getPointsAmount());
                            priceDescription.setAmount(pricingCalculateFlight.getAmount());
                        }
                    });
        }

        for (PricingCalculateTaxes tax : price.getPricesDescription().getTaxes()) {
            order.getPrice().getOrdersPriceDescription()
                    .forEach(priceDescription -> {
                        if (tax.getType().equals(priceDescription.getType())) {
                            priceDescription.setAmount(tax.getAmount());
                            priceDescription.setPointsAmount(tax.getPointsAmount());
                        }
                    });
        }
    }


    private boolean isSameOrderItems(ReservationRequest request, Optional<OrderEntity> orderOptional) {
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
            return false;
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
            log.warn("Tokens are diferent!");
        }
    }

    private void addPartnerOrderLinkIdToItems(String partnerCode, String orderId, Set<OrderItemEntity> items) {
        Integer index = 0;
        for (OrderItemEntity item : items) {
            buildPartnerOrderLinkId(partnerCode, item, orderId, index);
            index++;
        }
    }

    private void buildPartnerOrderLinkId(String partnerCode, OrderItemEntity item, String orderId, Integer index) {
        String linkIndex = "00" + index;
        String partnerOrderLink = partnerCode.toUpperCase() + "-" + orderId + linkIndex;
        item.setPartnerOrderLinkId(partnerOrderLink);
    }

    private void validateAndSetAmounts(PartnerReservationResponse partnerReservationResponse, OrderEntity order) {
        if (partnerReservationResponse.getAmount() == null || BigDecimal.ZERO.equals(partnerReservationResponse.getAmount())) {
            partnerReservationResponse.setAmount(order.getPrice().getPartnerAmount());
        }

        this.validateAmountOrderPriceDescriptionNullable(partnerReservationResponse, order);


        for (OrderItemEntity item : order.getItems()) {
            this.validateAmountPartnerNullable(item, partnerReservationResponse);
        }
    }

    private void validateAmountOrderPriceDescriptionNullable(PartnerReservationResponse partnerReservationResponse, OrderEntity orderEntity) {
        for (PartnerReservationOrdersPriceDescriptionFlight flightPriceDescription : partnerReservationResponse.getOrdersPriceDescription().getFlights()) {
            if (Objects.isNull(flightPriceDescription.getAmount())) {
                var orderPrices = orderEntity.getPrice().getOrdersPriceDescription().stream().filter(price -> !price.getType().contains("TAX")).toList();
                orderPrices.stream()
                        .filter(orderPrice -> flightPriceDescription.getPassengerType().equals(orderPrice.getType()))
                        .findFirst()
                        .ifPresent(orderPrice -> flightPriceDescription.setAmount(orderPrice.getAmount()));
            }
        }

        for (PartnerReservationOrdersPriceDescriptionTaxes taxesDescription : partnerReservationResponse.getOrdersPriceDescription().getTaxes()) {
            if (Objects.isNull(taxesDescription.getAmount())) {
                var orderPrices = orderEntity.getPrice().getOrdersPriceDescription().stream().filter(price -> price.getType().contains("TAX")).toList();
                orderPrices.stream()
                        .filter(orderPrice -> taxesDescription.getType().equals(orderPrice.getType()))
                        .findFirst()
                        .ifPresent(orderPrice -> taxesDescription.setAmount(orderPrice.getAmount()));
            }
        }
    }

    private void validateAmountPartnerNullable(OrderItemEntity orderItem, PartnerReservationResponse partnerReservationResponse) {
        partnerReservationResponse.getItems().stream()
                .filter(item -> orderItem.getProductType().equals(item.getType()))
                .findFirst()
                .ifPresent(item -> {
                    if (Objects.isNull(item.getAmount()) || BigDecimal.ZERO.equals(item.getAmount())) {
                        item.setAmount(orderItem.getPrice().getPartnerAmount());
                    }
                });
    }
}
