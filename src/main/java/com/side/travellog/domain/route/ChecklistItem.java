package com.side.travellog.domain.route;

import com.side.travellog.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChecklistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    private TravelRoute travelRoute;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String content;
    private boolean checked;
    private Integer orderNum;

    @Builder
    public ChecklistItem(TravelRoute travelRoute, User user, String content, Integer orderNum) {
        this.travelRoute = travelRoute;
        this.user = user;
        this.content = content;
        this.checked = false;
        this.orderNum = orderNum;
    }

    public void toggleChecked() {
        this.checked = !this.checked;
    }

    public void updateContent(String content) {
        this.content = content;
    }
}