package prod.nipponhubv1.nipponhubv1.Mappers;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import prod.nipponhubv1.nipponhubv1.Dto.StockResponse;
import prod.nipponhubv1.nipponhubv1.Models.Stock;

@Mapper(componentModel = "spring")
public interface StockMapper {

    @Mapping(target = "variantId",    source = "variant.id")
    @Mapping(target = "variantLabel", source = "variant.label")
    @Mapping(target = "productName",  source = "variant.product.name")
    @Mapping(target = "countryCode",  source = "country.code")
    StockResponse toResponse(Stock stock);

    List<StockResponse> toResponseList(List<Stock> stocks);
}
