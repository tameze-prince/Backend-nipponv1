package prod.nipponhubv1.nipponhubv1.Models;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity @Table(name = "loyalty_grades")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class LoyaltyGrade {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50, nullable = false)
    private String name;                  // Ex: Bronze, Silver, Gold, Platinum

    @Column(name = "min_total_spent", precision = 12, scale = 2, nullable = false)
    private BigDecimal minTotalSpent;     // Seuil d'activation

    @Column(name = "color_hex", length = 10)
    private String colorHex;             // Ex: #FFD700

    @Column(name = "badge_image_url", columnDefinition = "TEXT")
    private String badgeImageUrl;

    @OneToMany(mappedBy = "grade")
    @JsonIgnore
    private List<LoyaltyAccount> accounts = new ArrayList<>();
}
