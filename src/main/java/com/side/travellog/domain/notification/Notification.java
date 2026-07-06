package com.side.travellog.domain.notification;

import com.side.travellog.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String message;
    private String link;
    private boolean isRead;
    private LocalDateTime createdAt;

    @Builder
    public Notification(User user, String message, String link) {
        this.user = user;
        this.message = message;
        this.link = link;
        this.isRead = false;
        this.createdAt = LocalDateTime.now();
    }

    public void markAsRead() {
        this.isRead = true;
    }
}