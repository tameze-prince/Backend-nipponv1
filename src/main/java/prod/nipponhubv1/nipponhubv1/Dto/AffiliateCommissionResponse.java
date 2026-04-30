package prod.nipponhubv1.nipponhubv1.Dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;
import prod.nipponhubv1.nipponhubv1.Models.Enums.CommissionStatus;

@Data @Builder
public class AffiliateCommissionResponse {
    private Long id;
    private Long orderId;
    private BigDecimal commissionAmount;
    private CommissionStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
}
