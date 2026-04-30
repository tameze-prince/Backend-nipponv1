package prod.nipponhubv1.nipponhubv1.Repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import prod.nipponhubv1.nipponhubv1.Models.Notification;
import prod.nipponhubv1.nipponhubv1.Models.Notification.NotificationType;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Récupérer les notifications d'un utilisateur (paginé, non lues en premier)
    Page<Notification> findByRecipientIdOrderByReadAscCreatedAtDesc(Long userId, Pageable pageable);

    // Compter les notifications non lues
    long countByRecipientIdAndReadFalse(Long userId);

    // Récupérer les non-lues d'un utilisateur depuis une date
    @Query("SELECT n FROM Notification n WHERE n.recipient.id = :userId AND n.read = false " +
           "AND n.createdAt >= :since ORDER BY n.createdAt DESC")
    List<Notification> findUnreadSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    // Marquer comme lue
    void deleteByRecipientIdAndReadTrueAndCreatedAtBefore(Long userId, LocalDateTime before);

    // Notifications de type spécifique pour un utilisateur
    Page<Notification> findByRecipientIdAndTypeOrderByCreatedAtDesc(
            Long userId,
            NotificationType type,
            Pageable pageable);
}
