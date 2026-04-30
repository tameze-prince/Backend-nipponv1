package prod.nipponhubv1.nipponhubv1.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import prod.nipponhubv1.nipponhubv1.Models.WishlistNotification;

@Repository
public interface WishlistNotificationRepository extends JpaRepository<WishlistNotification, Long> {

    // Vérifier si une notif a déjà été envoyée (éviter doublons)
    boolean existsByUserIdAndVariantId(Long userId, Long variantId);

    List<WishlistNotification> findByUserId(Long userId);
}
