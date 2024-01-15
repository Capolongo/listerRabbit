package br.com.livelo.orderflight.domain.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ORDERS_STATUS")
public class OrderStatusEntity extends BaseEntity {
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ORDERS_STATUS_SEQ")
    @SequenceGenerator(name = "ORDERS_STATUS_SEQ", sequenceName = "ORDERS_STATUS_SEQ", allocationSize = 1)
    @Id
    private Long id;

    private String code;

    private String description;

    private String partnerCode;

    private String partnerDescription;

    private String partnerResponse;

    private LocalDateTime statusDate;
}