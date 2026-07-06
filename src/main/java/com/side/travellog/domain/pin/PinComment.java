package com.side.travellog.domain.pin;

import com.side.travellog.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PinComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pin_id")
    private TravelPin travelPin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(length = 500)
    private String content;

    private LocalDateTime createdAt;

    @Builder
    public PinComment(TravelPin travelPin, User user, String content) {
        this.travelPin = travelPin;
        this.user = user;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }

    public void updateContent(String content) {
        this.content = content;
    }
}