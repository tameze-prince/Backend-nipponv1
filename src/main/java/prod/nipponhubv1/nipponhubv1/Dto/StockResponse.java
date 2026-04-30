package prod.nipponhubv1.nipponhubv1.Dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class StockResponse {
    private Long variantId;
    private String variantLabel;
    private String productName;
    private String countryCode;
    private int quantity;
    private LocalDateTime updatedAt;
}
