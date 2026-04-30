package prod.nipponhubv1.nipponhubv1.Dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Builder;
import lombok.Data;


@Data @Builder
public class AffiliateStatsResponse {
    private String referralCode;
    private BigDecimal commissionPct;
    private BigDecimal totalEarned;
    private BigDecimal balance;
    private long totalClicks;
    private long totalOrders;
    private List<AffiliateCommissionResponse> recentCommissions;
}
