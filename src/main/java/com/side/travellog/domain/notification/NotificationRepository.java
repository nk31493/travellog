package com.side.travellog.domain.notification;

import com.side.travellog.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserOrderByCreatedAtDesc(User user);
    int countByUserAndIsReadFalse(User user);
}