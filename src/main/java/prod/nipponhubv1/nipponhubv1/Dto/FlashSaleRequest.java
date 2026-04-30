package prod.nipponhubv1.nipponhubv1.Dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FlashSaleRequest {
    @NotNull private Long productId;
    @NotNull @DecimalMin("1.0") @DecimalMax("99.0") private BigDecimal discountPct;
    @NotNull private LocalDateTime startsAt;
    @NotNull private LocalDateTime endsAt;
}
