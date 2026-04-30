package prod.nipponhubv1.nipponhubv1.Mappers;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import prod.nipponhubv1.nipponhubv1.Dto.OrderResponse;
import prod.nipponhubv1.nipponhubv1.Models.Order;

@Mapper(componentModel = "spring", uses = {OrderItemMapper.class})
public interface OrderMapper {

    @Mapping(target = "cityName",    source = "city.name")
    @Mapping(target = "countryName", source = "country.name")
    @Mapping(target = "invoiceUrl",  source = "invoice.pdfUrl")
    OrderResponse toResponse(Order order);

    List<OrderResponse> toResponseList(List<Order> orders);
}
