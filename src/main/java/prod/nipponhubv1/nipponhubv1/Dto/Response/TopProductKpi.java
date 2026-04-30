package prod.nipponhubv1.nipponhubv1.Dto.Response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TopProductKpi {
    private Long   productId;
    private String productName;
    private long   totalSold;
}
