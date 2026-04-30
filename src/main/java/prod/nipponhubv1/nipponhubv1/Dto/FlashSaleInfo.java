package prod.nipponhubv1.nipponhubv1.Dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class FlashSaleInfo {
    private BigDecimal discountPct;
    private BigDecimal discountedPrice;
    private LocalDateTime endsAt;
}
