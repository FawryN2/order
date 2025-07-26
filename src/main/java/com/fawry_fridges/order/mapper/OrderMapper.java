package com.fawry_fridges.order.mapper;

import com.fawry_fridges.order.dto.OrderDto;
import com.fawry_fridges.order.enities.OrderEntity;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface OrderMapper {

        OrderMapper INSTANCE = Mappers.getMapper(OrderMapper.class);

        OrderDto toDto(OrderEntity entity);

        OrderEntity toEntity(OrderDto dto);
}
