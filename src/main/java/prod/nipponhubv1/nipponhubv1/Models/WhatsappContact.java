package prod.nipponhubv1.nipponhubv1.Models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity @Table(name = "whatsapp_contacts")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class WhatsappContact {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id", nullable = false)
    private City city;

    @Column(name = "whatsapp_number", length = 20, nullable = false)
    private String whatsappNumber;

    @Column(length = 100)
    private String label;

    @Column(name = "is_active")
    private boolean active = true;
}
