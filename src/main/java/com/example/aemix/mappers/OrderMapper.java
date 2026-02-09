package com.example.aemix.mappers;

import com.example.aemix.dto.responses.OrderResponse;
import com.example.aemix.entities.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {
    
    @Mapping(target = "cityId", source = "city.id")
    @Mapping(target = "cityName", source = "city.name")
    @Mapping(target = "emailOrTelegramUsername", source = "order.userOrders.user.emailOrTelegramId")
    @Mapping(target = "title", ignore = true)
    OrderResponse toDto(Order order);
    
    Order toEntity(OrderResponse orderResponse);
}
