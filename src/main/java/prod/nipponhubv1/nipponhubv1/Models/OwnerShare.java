package prod.nipponhubv1.nipponhubv1.Models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity @Table(name = "owner_shares",
    uniqueConstraints = @UniqueConstraint(columnNames = "country_id"))
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class OwnerShare {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id", nullable = false, unique = true)
    private Country country;

    @Column(name = "share_pct", precision = 5, scale = 2, nullable = false)
    private BigDecimal sharePct;          // Part Owner par pays (Ex: 15.00 = 15%)

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
