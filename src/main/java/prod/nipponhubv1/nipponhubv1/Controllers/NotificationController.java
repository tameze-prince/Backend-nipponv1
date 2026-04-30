package prod.nipponhubv1.nipponhubv1.Controllers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import prod.nipponhubv1.nipponhubv1.Models.Notification;
import prod.nipponhubv1.nipponhubv1.Models.OurUser;
import prod.nipponhubv1.nipponhubv1.Services.NotificationService;

/**
 * GET    /api/v1/notifications              → mes notifications (paginé)
 * GET    /api/v1/notifications/{id}         → détail notification
 * GET    /api/v1/notifications/unread/count → nombre non-lues
 * PUT    /api/v1/notifications/{id}/read    → marquer comme lue
 * PUT    /api/v1/notifications/read-all     → tout marquer comme lu
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notifications utilisateur")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<Notification>> getNotifications(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt",
                            direction = Sort.Direction.DESC) Pageable pageable) {
        OurUser user = (OurUser) userDetails;
        return ResponseEntity.ok(
            notificationService.getNotifications(user.getId(), pageable)
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Notification> getNotification(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        OurUser user = (OurUser) userDetails;
        Notification notification = notificationService.getNotification(id);

        // Vérifier que c'est la sienne
        if (!notification.getRecipient().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(notification);
    }

    @GetMapping("/unread/count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Long> getUnreadCount(
            @AuthenticationPrincipal UserDetails userDetails) {
        OurUser user = (OurUser) userDetails;
        long count = notificationService.getUnreadCount(user.getId());
        return ResponseEntity.ok(count);
    }

    @PutMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Notification> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        OurUser user = (OurUser) userDetails;
        Notification notification = notificationService.getNotification(id);

        // Vérifier que c'est la sienne
        if (!notification.getRecipient().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(notificationService.markAsRead(id));
    }

    @PutMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markAllAsRead(
            @AuthenticationPrincipal UserDetails userDetails) {
        OurUser user = (OurUser) userDetails;
        notificationService.markAllAsRead(user.getId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        OurUser user = (OurUser) userDetails;
        Notification notification = notificationService.getNotification(id);

        // Vérifier que c'est la sienne
        if (!notification.getRecipient().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        notificationService.deleteOldReadNotifications(user.getId(), 0);
        return ResponseEntity.noContent().build();
    }
}
