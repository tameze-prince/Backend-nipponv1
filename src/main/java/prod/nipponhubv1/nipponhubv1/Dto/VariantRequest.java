package prod.nipponhubv1.nipponhubv1.Dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VariantRequest {
    @NotBlank private String label;
    private BigDecimal extraPrice;
    private String imageUrl;
}
