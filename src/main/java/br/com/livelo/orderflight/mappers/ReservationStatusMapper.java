package br.com.livelo.orderflight.mappers;

import br.com.livelo.orderflight.domain.dto.reservation.response.PartnerReservationResponse;
import br.com.livelo.orderflight.domain.entity.OrderStatusEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ReservationStatusMapper {

    @Mapping(target = "code", source = "partnerReservationResponse.status.code")
    @Mapping(target = "description", source = "partnerReservationResponse.status.description")
    @Mapping(target = "partnerCode", source = "partnerReservationResponse.status.partnerCode")
    @Mapping(target = "partnerDescription", source = "partnerReservationResponse.status.partnerDescription")
    @Mapping(target = "statusDate", expression = "java(java.time.LocalDateTime.now())")
    OrderStatusEntity toOrderStatus(PartnerReservationResponse partnerReservationResponse);

    @AfterMapping
    default void buildPartnerResponse(PartnerReservationResponse partnerReservationResponse, @MappingTarget OrderStatusEntity orderStatusEntity) throws JsonProcessingException {
        orderStatusEntity.setPartnerResponse(new ObjectMapper().writeValueAsString(partnerReservationResponse));
    }

}