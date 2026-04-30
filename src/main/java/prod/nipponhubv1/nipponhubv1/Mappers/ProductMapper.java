package prod.nipponhubv1.nipponhubv1.Mappers;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import prod.nipponhubv1.nipponhubv1.Dto.ProductRequest;
import prod.nipponhubv1.nipponhubv1.Dto.ProductResponse;
import prod.nipponhubv1.nipponhubv1.Models.Product;


@Mapper(componentModel = "spring", uses = {VariantMapper.class})
public interface ProductMapper {

    @Mapping(target = "categoryName",  source = "category.name")
    @Mapping(target = "franchiseName", source = "franchise.name")
    @Mapping(target = "imageUrls",     expression = "java(product.getImages() != null ? product.getImages().stream().map(img -> img.getUrl()).toList() : java.util.Collections.emptyList())")
    @Mapping(target = "activeFlashSale", ignore = true)  // calculé en service
    ProductResponse toResponse(Product product);

    @Mapping(target = "id",       ignore = true)
    @Mapping(target = "slug",     ignore = true)   // généré en service
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "franchise",ignore = true)
    @Mapping(target = "variants", ignore = true)
    @Mapping(target = "images",   ignore = true)
    @Mapping(target = "flashSales",ignore = true)
    @Mapping(target = "active",   constant = "true")
    @Mapping(target = "createdAt",ignore = true)
    @Mapping(target = "updatedAt",ignore = true)
    Product toEntity(ProductRequest req);

    List<ProductResponse> toResponseList(List<Product> products);
}
