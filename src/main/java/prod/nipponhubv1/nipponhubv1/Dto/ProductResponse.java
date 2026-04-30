package prod.nipponhubv1.nipponhubv1.Dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class ProductResponse {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private BigDecimal basePrice;
    private String categoryName;
    private String franchiseName;
    private List<VariantResponse> variants;
    private List<String> imageUrls;
    private FlashSaleInfo activeFlashSale;  // null si pas de vente flash active
    private boolean active;
}
