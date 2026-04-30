package prod.nipponhubv1.nipponhubv1.Models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity @Table(name = "affiliate_profiles")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class AffiliateProfile {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private OurUser user;

    @Column(name = "referral_code", length = 50, unique = true, nullable = false)
    private String referralCode;

    @Column(name = "commission_pct", precision = 5, scale = 2)
    private BigDecimal commissionPct;     // Ex: 5.00 = 5%

    @Column(name = "total_earned", precision = 12, scale = 2)
    private BigDecimal totalEarned = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "is_active")
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "affiliate", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<AffiliateCommission> commissions = new ArrayList<>();

    @OneToMany(mappedBy = "affiliate", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<AffiliateClick> clicks = new ArrayList<>();
}
