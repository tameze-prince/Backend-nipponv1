package prod.nipponhubv1.nipponhubv1.Mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import prod.nipponhubv1.nipponhubv1.Dto.VariantRequest;
import prod.nipponhubv1.nipponhubv1.Dto.VariantResponse;
import prod.nipponhubv1.nipponhubv1.Models.ProductVariant;

@Mapper(componentModel = "spring")
public interface VariantMapper {

    @Mapping(target = "finalPrice",     ignore = true)  // calculé en service
    @Mapping(target = "stockQuantity",  ignore = true)  // requiert le pays
    VariantResponse toResponse(ProductVariant variant);

    @Mapping(target = "id",      ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "stocks",  ignore = true)
    @Mapping(target = "active",  constant = "true")
    ProductVariant toEntity(VariantRequest req);
}
