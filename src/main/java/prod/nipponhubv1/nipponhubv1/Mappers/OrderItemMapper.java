package prod.nipponhubv1.nipponhubv1.Mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import prod.nipponhubv1.nipponhubv1.Dto.OrderItemResponse;
import prod.nipponhubv1.nipponhubv1.Models.OrderItem;

@Mapper(componentModel = "spring")
public interface OrderItemMapper {

    @Mapping(target = "variantId",    source = "variant.id")
    @Mapping(target = "productName",  source = "variant.product.name")
    @Mapping(target = "variantLabel", source = "variant.label")
    OrderItemResponse toResponse(OrderItem item);
}
