package com.side.travellog.domain.notification;

import com.side.travellog.domain.user.User;
import com.side.travellog.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public void send(User user, String message, String link) {
        notificationRepository.save(Notification.builder()
                .user(user)
                .message(message)
                .link(link)
                .build());
    }

    public List<Notification> getNotifications(String email) {
        User user = userRepository.findByEmail(email);
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public int getUnreadCount(String email) {
        User user = userRepository.findByEmail(email);
        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    public void markAllAsRead(String email) {
        User user = userRepository.findByEmail(email);
        List<Notification> notifications = notificationRepository.findByUserOrderByCreatedAtDesc(user);
        notifications.forEach(Notification::markAsRead);
        notificationRepository.saveAll(notifications);
    }
}