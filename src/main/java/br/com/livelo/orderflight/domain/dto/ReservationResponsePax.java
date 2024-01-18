package br.com.livelo.orderflight.domain.dto;

import java.util.Set;

public record ReservationResponsePax(String type,
									 String firstName,
									 String lastName,
									 String email,
									 String areaCode,
									 String phoneNumber,
									 String gender,
									 String birthDate,
									 Set<ReservationResponseDocument> document) {

}