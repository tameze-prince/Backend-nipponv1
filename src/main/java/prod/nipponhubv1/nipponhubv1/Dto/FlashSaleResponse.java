package prod.nipponhubv1.nipponhubv1.Dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FlashSaleResponse {
    private Long id;
    private ProductSummary product;
    private BigDecimal discountPct;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private boolean active;
    private UserSummary createdBy;

    @Data
    @Builder
    public static class ProductSummary {
        private Long id;
        private String name;
        private String slug;
    }

    @Data
    @Builder
    public static class UserSummary {
        private Long id;
        private String firstName;
        private String lastName;
        private String email;
    }
}
