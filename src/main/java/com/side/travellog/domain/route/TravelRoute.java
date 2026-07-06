package com.side.travellog.domain.route;

import com.side.travellog.domain.pin.TravelPin;
import com.side.travellog.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "travel_route")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TravelRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 100)
    private String destination;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column
    private LocalDate startDate;

    @Column
    private LocalDate endDate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(unique = true)
    private String shareToken;

    @Column(unique = true)
    private String inviteToken;

    public void updateInviteToken(String inviteToken) {
        this.inviteToken = inviteToken;
    }

    @OneToMany(mappedBy = "travelRoute", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TravelPin> pins = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public void updateShareToken(String shareToken) {
        this.shareToken = shareToken;
    }

    public void updateInfo(String name, String destination, LocalDate startDate, LocalDate endDate) {
        this.name = name;
        this.destination = destination;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void updateOwner(User user) {
        this.user = user;
    }

}