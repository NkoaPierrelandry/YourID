package com.yourid.api.notifications.service;

import com.yourid.api.notifications.dto.NotificationResponse;
import com.yourid.api.notifications.entity.Notification;
import com.yourid.api.notifications.repository.NotificationRepository;
import com.yourid.api.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    // Utilise en interne par d autres services (Orders, Withdrawals...) pour prevenir
    // un utilisateur d un evenement -- pas expose directement via un controller.
    @Transactional
    public void notify(User user, String type, String title, String message) {
        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .build();
        notificationRepository.save(notification);
    }

    // Construit le DTO A L INTERIEUR de la transaction, avant que la session Hibernate
    // ne se ferme -- contrairement a une version precedente qui retournait l entite
    // Notification brute et plantait a la serialisation (LazyInitializationException
    // sur le champ "user", charge en LAZY).
    @Transactional(readOnly = true)
    public Page<NotificationResponse> listForUser(UUID userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(n -> NotificationResponse.builder()
                        .id(n.getId())
                        .type(n.getType())
                        .title(n.getTitle())
                        .message(n.getMessage())
                        .isRead(n.isRead())
                        .createdAt(n.getCreatedAt())
                        .build());
    }

    public long countUnread(UUID userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAsRead(UUID notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.markAsRead();
            notificationRepository.save(n);
        });
    }
}