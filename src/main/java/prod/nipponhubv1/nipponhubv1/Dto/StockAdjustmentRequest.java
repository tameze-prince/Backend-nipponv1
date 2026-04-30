package prod.nipponhubv1.nipponhubv1.Dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import prod.nipponhubv1.nipponhubv1.Models.Enums.MovementType;

@Data
public class StockAdjustmentRequest {
    @NotNull private Long variantId;
    @NotNull private Long countryId;
    @NotNull private MovementType movementType;
    @NotNull @Min(1) private int quantity;
    private String reason;
}
