package prod.nipponhubv1.nipponhubv1.Dto.Response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OwnerShareCalculation {
    private Long          countryId;
    private BigDecimal    totalRevenue;
    private BigDecimal    sharePct;
    private BigDecimal    grossShare;     // CA × %Part Owner
    private BigDecimal    affiliateCost;  // Commissions affiliés payées
    private BigDecimal    netShare;       // Part nette réelle = grossShare - affiliateCost
    private LocalDateTime periodFrom;
    private LocalDateTime periodTo;
}
