package prod.nipponhubv1.nipponhubv1.Models;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

@Entity @Table(name = "notifications")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {

    public enum NotificationType {
        NEW_ORDER,              // Nouvelle commande
        NEW_USER,               // Nouvel utilisateur inscrit
        ORDER_STATUS_CHANGE,    // Changement de statut commande
        PRODUCT_REVIEW,         // Avis produit
        FLASH_SALE_CREATED,     // Flash sale créée
        LOW_STOCK               // Stock faible
    }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private OurUser recipient;          // Destinataire de la notification

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(length = 200, nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "related_order_id")
    private Long relatedOrderId;

    @Column(name = "related_product_id")
    private Long relatedProductId;

    @Column(name = "related_user_id")
    private Long relatedUserId;

    @Column(name = "is_read")
    private boolean read = false;

    @Column(name = "action_url")
    private String actionUrl;           // URL vers laquelle cliquer

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
