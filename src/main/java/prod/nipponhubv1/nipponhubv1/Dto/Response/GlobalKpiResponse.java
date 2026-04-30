package prod.nipponhubv1.nipponhubv1.Dto.Response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

import lombok.Data;

@Data
@Builder
public class GlobalKpiResponse {
    private BigDecimal       totalRevenue;
    private long             totalOrders;
    private long             totalCustomers;
    private LocalDateTime    periodFrom;
    private LocalDateTime    periodTo;
    private List<CountryKpi> byCountry;
}
