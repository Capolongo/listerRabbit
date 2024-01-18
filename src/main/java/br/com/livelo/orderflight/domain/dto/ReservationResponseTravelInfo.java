package br.com.livelo.orderflight.domain.dto;

import java.util.Set;

public record ReservationResponseTravelInfo (String type,
											 String reservationCode,
											 Integer adultQuantity,
											 Integer childQuantity,
											 Integer babyQuantity,
											 String voucher,
											 String typeClass,
											 Set<ReservationResponsePax> paxs){

}