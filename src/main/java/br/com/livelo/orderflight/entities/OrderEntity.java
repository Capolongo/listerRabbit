package br.com.livelo.orderflight.entities;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ORDERS")
public class OrderEntity {

    //    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ORDERS_SEQ")
//    @SequenceGenerator(name = "ORDERS_SEQ", sequenceName = "ORDERS_SEQ", allocationSize = 1)
    @Column(name = "ID")
    @Id
    private String id;

    @Column(name = "COMMERCE_ORDER_ID")
    private String commerceOrderId;

    //TODO -- verificar com relação ao token da cvc - Defini como text, pois pode haver uma quantidade não previsível para outros parceiros
    @Column(name = "PARTNER_ORDER_ID")
    private String partnerOrderId;

    @Column(name = "PARTNER_CODE")
    private String partnerCode;

    @CreationTimestamp
    @Column(name = "SUBMITTED_DATE")
    private LocalDateTime submittedDate;

    @Column(name = "CHANNEL")
    private String channel;

    @Column(name = "TIER_CODE")
    private String tierCode;

    @Column(name = "ORIGIN_ORDER")
    private String originOrder;

    @Column(name = "CUSTOMER_IDENTIFIER")
    private String customerIdentifier;

    @CreationTimestamp
    @Column(name = "CREATE_DATE")
    private LocalDateTime createDate;

    @UpdateTimestamp
    @Column(name = "LAST_MODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    @Column(name = "STATUS")
    private Integer status;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumns({
            @JoinColumn(name = "ORDER_PRICE_ID", referencedColumnName = "ID"),
    })
    private OrderPriceEntity orderPriceEntity;
}