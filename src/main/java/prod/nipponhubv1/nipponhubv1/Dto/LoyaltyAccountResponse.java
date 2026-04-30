package prod.nipponhubv1.nipponhubv1.Dto;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class LoyaltyAccountResponse {
    private int pointsBalance;
    private BigDecimal totalSpent;
    private String gradeName;
    private String gradeColorHex;
    private String badgeImageUrl;
    private String cardImageUrl;
    private String qrCode;
}
