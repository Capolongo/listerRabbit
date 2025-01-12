package br.com.livelo.orderflight.domain.dto.reservation.response;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PartnerReservationChangeRule {
	private String type;
	private String description;
}
