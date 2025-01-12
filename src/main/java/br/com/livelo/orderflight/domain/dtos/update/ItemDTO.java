package br.com.livelo.orderflight.domain.dtos.update;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ItemDTO {
    private String id;
    private String commerceItemId;
    private String partnerOrderId;
    private StatusDTO status;
    private Long price;
    private Long quantity;
    private String currency;
    private String deliveryDate;
    private List<ItemDocumentDTO> documents;
    private PartnerInfoSummaryDTO partnerInfo;
    private Boolean forceUpdate;
}
