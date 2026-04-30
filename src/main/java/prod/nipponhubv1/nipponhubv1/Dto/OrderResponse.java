package prod.nipponhubv1.nipponhubv1.Dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Data;
import prod.nipponhubv1.nipponhubv1.Models.Enums.OrderStatus;
import prod.nipponhubv1.nipponhubv1.Models.Enums.OrderType;

@Data @Builder
public class OrderResponse {
    private Long id;
    private OrderStatus status;
    private OrderType orderType;
    private String cityName;
    private String countryName;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private int pointsUsed;
    private List<OrderItemResponse> items;
    private String invoiceUrl;
    private LocalDateTime createdAt;
    private LocalDateTime deliveredAt;
}
