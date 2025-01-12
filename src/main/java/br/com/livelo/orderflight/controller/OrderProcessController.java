package br.com.livelo.orderflight.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.livelo.orderflight.domain.dtos.repository.PaginationOrderProcessResponse;
import br.com.livelo.orderflight.service.order.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/orders")
public class OrderProcessController {
    private final OrderService orderService;

    @GetMapping("/process")
    public ResponseEntity<PaginationOrderProcessResponse> getOrdersByStatus(@RequestParam String statusCode,
                                                                            @RequestParam(required = false) String limitArrivalDate,
                                                                            @RequestParam(required = false, defaultValue = "1") Integer page,
                                                                            @RequestParam(required = false, defaultValue = "${order.orderProcessMaxRows}") Integer rows) {
        log.debug("OrderProcessController.getOrdersByStatus() - Start - statusCode: [{}], limitArrivalDate: [{}], page: [{}], rows: [{}]", statusCode, limitArrivalDate, page, rows);

        var orders = orderService.getOrdersByStatusCode(statusCode, Optional.ofNullable(limitArrivalDate), page, rows);

        log.debug("OrderProcessController.getOrdersByStatus() - End - response: [{}]", orders);
        return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(orders);
    }
}