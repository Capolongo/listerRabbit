package br.com.livelo.orderflight.repository;

import br.com.livelo.orderflight.domain.dtos.repository.OrderProcess;
import br.com.livelo.orderflight.domain.entity.OrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

public interface OrderRepository extends JpaRepository<OrderEntity, String> {
    Optional<OrderEntity> findByCommerceOrderId(String commerceOrderId);

    @Query("SELECT o FROM OrderEntity o JOIN FETCH o.items oi WHERE o.commerceOrderId = :commerceOrderId OR oi.commerceItemId in :commerceItemsIds")
    Optional<OrderEntity> findByCommerceOrderIdOrItemsCommerceItemsId(String commerceOrderId, Set<String> commerceItemsIds);

    Page<OrderProcess> findAllByCurrentStatusCode(String statusCode, Pageable pageable);

    Page<OrderProcess> findAllByCurrentStatusCodeAndArrivalDateLessThan(String statusCode,
                                                                        LocalDateTime expirationDate, Pageable pageable);

}
