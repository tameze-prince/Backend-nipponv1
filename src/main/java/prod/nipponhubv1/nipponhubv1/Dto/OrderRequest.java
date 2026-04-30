package prod.nipponhubv1.nipponhubv1.Dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderRequest {
    @NotNull private Long cityId;
    @NotNull @NotEmpty private List<OrderItemRequest> items;
    private int pointsToUse;
    private String notes;
    
}
