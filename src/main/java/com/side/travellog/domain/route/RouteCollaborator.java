package com.side.travellog.domain.route;

import com.side.travellog.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "route_collaborator", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"route_id", "user_id"})
})
public class RouteCollaborator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    private TravelRoute travelRoute;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private java.time.LocalDateTime joinedAt;

    @Builder
    public RouteCollaborator(TravelRoute travelRoute, User user) {
        this.travelRoute = travelRoute;
        this.user = user;
        this.joinedAt = java.time.LocalDateTime.now();
    }
}