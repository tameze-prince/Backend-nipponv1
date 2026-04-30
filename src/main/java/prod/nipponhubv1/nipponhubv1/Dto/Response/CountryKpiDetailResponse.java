package prod.nipponhubv1.nipponhubv1.Dto.Response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CountryKpiDetailResponse {
    private Long                countryId;
    private String              countryName;
    private String              currency;
    private BigDecimal          totalRevenue;
    private BigDecimal          affiliateCost;
    private BigDecimal          ownerSharePct;
    private BigDecimal          ownerRevenue;
    private Map<String, Long>   ordersByStatus;
    private List<TopProductKpi> topProducts;
    private LocalDateTime       periodFrom;
    private LocalDateTime       periodTo;
}
