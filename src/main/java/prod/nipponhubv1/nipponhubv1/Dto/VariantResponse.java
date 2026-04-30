package prod.nipponhubv1.nipponhubv1.Dto;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class VariantResponse {
    private Long id;
    private String label;
    private BigDecimal extraPrice;
    private BigDecimal finalPrice;   // basePrice + extraPrice (calculé)
    private String imageUrl;
    private int stockQuantity;       // Stock dans le pays du client
}
