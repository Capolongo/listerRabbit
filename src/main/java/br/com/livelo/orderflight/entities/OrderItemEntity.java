package br.com.livelo.orderflight.entities;

import java.time.LocalDateTime;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.SequenceGenerator;
import org.hibernate.annotations.UpdateTimestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ORDER_ITEM")
public class OrderItemEntity extends BaseEntity {

    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ORDER_ITEM_SEQ")
    @SequenceGenerator(name = "ORDER_ITEM_SEQ", sequenceName = "ORDER_ITEM_SEQ", allocationSize = 1)
    @Id
    private Long id;

    private String commerceItemId;

    private String skuId;

    private String productId;

    private Integer quantity;

    private String externalCoupon;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "ORDER_ITEM_PRICE_ID")
    private OrderItemPriceEntity price;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "TRAVEL_INFO_ID")
    private TravelInfoEntity travelInfoEntity;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "ORDER_ITEM_ID")
    private Set<SegmentEntity> segments;

}
