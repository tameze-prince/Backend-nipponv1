package prod.nipponhubv1.nipponhubv1.Models;

import jakarta.persistence.*;
import lombok.*;
import prod.nipponhubv1.nipponhubv1.Models.Enums.CommissionStatus;

import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.*;



@Entity @Table(name = "affiliate_commissions")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class AffiliateCommission {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "affiliate_id", nullable = false)
    private AffiliateProfile affiliate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "commission_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal commissionAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CommissionStatus status;      // ENUM

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_order_id")
    private AffiliatePaymentOrder paymentOrder;  // nullable jusqu'au paiement

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
