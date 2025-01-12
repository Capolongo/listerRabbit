package br.com.livelo.orderflight.controller;

import br.com.livelo.orderflight.domain.dtos.orderValidate.request.OrderValidateRequestDTO;
import br.com.livelo.orderflight.domain.dtos.orderValidate.response.OrderValidateResponseDTO;
import br.com.livelo.orderflight.service.order.OrderService;
import br.com.livelo.orderflight.service.validation.impl.OrderValidatorServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/orders")
@Slf4j
public class OrderValidationController {
    private final OrderValidatorServiceImpl orderValidatorService;

    @PostMapping("/validate")
    public ResponseEntity<OrderValidateResponseDTO> validateOrder(@Valid @RequestBody(required = true) final OrderValidateRequestDTO orderValidateRequest) {
        log.info("OrderValidationController.validateOrder() - Start - Partner retrieve with request: {}", orderValidateRequest);
        OrderValidateResponseDTO orderValidateResponse = orderValidatorService.validateOrder(orderValidateRequest);
        log.info("OrderValidationController.validateOrder() - End - Partner retrieve with response: {}", orderValidateResponse);
        return ResponseEntity.ok(orderValidateResponse);
    }
}
