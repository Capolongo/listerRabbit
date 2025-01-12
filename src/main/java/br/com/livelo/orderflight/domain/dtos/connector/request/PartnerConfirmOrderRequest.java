package br.com.livelo.orderflight.domain.dtos.connector.request;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PartnerConfirmOrderRequest {
  private String id;
  private String commerceOrderId;
  private String commerceItemId;
  private String partnerOrderId;
  private String partnerOrderLinkId;
  private String partnerCode;
  private String submittedDate;
  private String expirationDate;
  private List<PartnerConfirmOrderPaxRequest> paxs;
  private List<String> segmentsPartnerIds;
}
