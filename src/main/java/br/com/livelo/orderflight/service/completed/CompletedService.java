package br.com.livelo.orderflight.service.completed;

import br.com.livelo.orderflight.domain.dtos.repository.OrderProcess;
import com.fasterxml.jackson.core.JsonProcessingException;

public interface CompletedService {
    void orderProcess(OrderProcess payload);
}
