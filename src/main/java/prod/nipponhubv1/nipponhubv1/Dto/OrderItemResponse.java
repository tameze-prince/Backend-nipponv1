package prod.nipponhubv1.nipponhubv1.Dto;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class OrderItemResponse {
    private Long variantId;
    private String productName;
    private String variantLabel;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
}
