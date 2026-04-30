package prod.nipponhubv1.nipponhubv1.Dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderItemRequest {
    @NotNull private Long variantId;
    @NotNull @Min(1) private int quantity;
}
