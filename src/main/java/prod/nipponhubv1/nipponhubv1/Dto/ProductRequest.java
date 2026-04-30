package prod.nipponhubv1.nipponhubv1.Dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class ProductRequest {
    @NotBlank private String name;
    @NotNull  private Long categoryId;
    private Long franchiseId;
    private String description;
    @NotNull @Positive private BigDecimal basePrice;
    private BigDecimal purchasePrice;
    private List<VariantRequest> variants;
    private Integer initialStock;
    private Long stockCountryId;
    private Integer imageCount;
}
