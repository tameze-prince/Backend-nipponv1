package prod.nipponhubv1.nipponhubv1.Services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import prod.nipponhubv1.nipponhubv1.Exception.OtakuException;
import prod.nipponhubv1.nipponhubv1.Models.Notification;
import prod.nipponhubv1.nipponhubv1.Models.Notification.NotificationType;
import prod.nipponhubv1.nipponhubv1.Models.OurUser;
import prod.nipponhubv1.nipponhubv1.Repository.NotificationRepository;
import prod.nipponhubv1.nipponhubv1.Repository.UserRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepo;
    private final UserRepository userRepo;

    // ── Créer une notification ────────────────────────────────────────────────

    /**
     * Crée une notification pour un utilisateur donné.
     */
    @Transactional
    public Notification createNotification(Long recipientId,
                                            NotificationType type,
                                            String title,
                                            String message,
                                            Long relatedOrderId,
                                            Long relatedProductId,
                                            Long relatedUserId,
                                            String actionUrl) {
        OurUser recipient = userRepo.findById(recipientId)
            .orElseThrow(() -> OtakuException.notFound("Utilisateur", recipientId));

        Notification notification = Notification.builder()
            .recipient(recipient)
            .type(type)
            .title(title)
            .message(message)
            .relatedOrderId(relatedOrderId)
            .relatedProductId(relatedProductId)
            .relatedUserId(relatedUserId)
            .actionUrl(actionUrl)
            .read(false)
            .build();

        Notification saved = notificationRepo.save(notification);
        log.info("✓ Notification créée — type={} recipient={} title={}",
            type, recipient.getEmail(), title);
        return saved;
    }

    // ── Récupérer les notifications ───────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<Notification> getNotifications(Long userId, Pageable pageable) {
        return notificationRepo.findByRecipientIdOrderByReadAscCreatedAtDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepo.countByRecipientIdAndReadFalse(userId);
    }

    @Transactional(readOnly = true)
    public List<Notification> getUnreadSince(Long userId, LocalDateTime since) {
        return notificationRepo.findUnreadSince(userId, since);
    }

    @Transactional(readOnly = true)
    public Page<Notification> getNotificationsByType(Long userId,
                                                      NotificationType type,
                                                      Pageable pageable) {
        return notificationRepo.findByRecipientIdAndTypeOrderByCreatedAtDesc(userId, type, pageable);
    }

    @Transactional(readOnly = true)
    public Notification getNotification(Long notificationId) {
        return notificationRepo.findById(notificationId)
            .orElseThrow(() -> OtakuException.notFound("Notification", notificationId));
    }

    // ── Marquer comme lue ────────────────────────────────────────────────────

    @Transactional
    public Notification markAsRead(Long notificationId) {
        Notification notification = getNotification(notificationId);
        notification.setRead(true);
        return notificationRepo.save(notification);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepo.findByRecipientIdOrderByReadAscCreatedAtDesc(userId, Pageable.unpaged())
            .stream()
            .filter(n -> !n.isRead())
            .forEach(n -> {
                n.setRead(true);
                notificationRepo.save(n);
            });
        log.info("✓ Toutes les notifications marquées comme lues — userId={}", userId);
    }

    // ── Supprimer les anciennes notifications ────────────────────────────────

    @Transactional
    public void deleteOldReadNotifications(Long userId, int daysOld) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(daysOld);
        notificationRepo.deleteByRecipientIdAndReadTrueAndCreatedAtBefore(userId, cutoff);
        log.info("✓ Anciennes notifications supprimées — userId={} before={}",
            userId, cutoff);
    }

    // ── Notifier multiple utilisateurs d'un événement ───────────────────────

    @Transactional
    public void notifyUsers(List<Long> userIds,
                           NotificationType type,
                           String title,
                           String message,
                           Long relatedOrderId,
                           Long relatedProductId,
                           String actionUrl) {
        userIds.forEach(userId ->
            createNotification(userId, type, title, message,
                relatedOrderId, relatedProductId, null, actionUrl)
        );
    }

    // ── Événements systématiques ─────────────────────────────────────────────

    /**
     * Notifier le créateur du produit d'une nouvelle commande.
     */
    public void notifyProductOwnerNewOrder(Long productCreatorId,
                                          Long orderId,
                                          Long productId,
                                          String customerName,
                                          String price) {
        String title = "Nouvelle commande sur votre produit";
        String message = String.format(
            "Un client (%s) a passé une commande incluant votre produit pour %s",
            customerName, price
        );
        String actionUrl = "/admin/orders/" + orderId;

        createNotification(productCreatorId, NotificationType.NEW_ORDER,
            title, message, orderId, productId, null, actionUrl);
    }

    /**
     * Notifier l'admin d'une nouvelle inscription.
     */
    public void notifyAdminNewUser(Long adminId, Long newUserId, String userEmail) {
        String title = "Nouvel utilisateur inscrit";
        String message = String.format("Un nouveau client s'est inscrit : %s", userEmail);
        String actionUrl = "/admin/users/" + newUserId;

        createNotification(adminId, NotificationType.NEW_USER,
            title, message, null, null, newUserId, actionUrl);
    }

    /**
     * Notifier l'admin d'une nouvelle commande (en général).
     */
    public void notifyAdminNewOrder(Long adminId, Long orderId, String customerName, BigDecimal total) {
        String title = "Nouvelle commande";
        String message = String.format(
            "Commande de %s pour un montant de %s XAF",
            customerName, total
        );
        String actionUrl = "/admin/orders/" + orderId;

        createNotification(adminId, NotificationType.NEW_ORDER,
            title, message, orderId, null, null, actionUrl);
    }
}
