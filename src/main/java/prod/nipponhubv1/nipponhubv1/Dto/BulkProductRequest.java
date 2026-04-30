package prod.nipponhubv1.nipponhubv1.Dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class BulkProductRequest {
    @Valid
    @NotEmpty
    private List<ProductRequest> products;
}
