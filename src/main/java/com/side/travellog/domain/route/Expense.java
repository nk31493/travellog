package com.side.travellog.domain.route;

import com.side.travellog.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    private TravelRoute travelRoute;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String title;
    private Integer amount;
    private String category;
    private LocalDate expenseDate;
    private boolean shared;

    @Builder
    public Expense(TravelRoute travelRoute, User user, String title,
                    Integer amount, String category, LocalDate expenseDate, boolean shared) {
        this.travelRoute = travelRoute;
        this.user = user;
        this.title = title;
        this.amount = amount;
        this.category = category;
        this.expenseDate = expenseDate;
        this.shared = shared;
    }

    public void updateInfo(String title, Integer amount, String category, LocalDate expenseDate, boolean shared) {
        this.title = title;
        this.amount = amount;
        this.category = category;
        this.expenseDate = expenseDate;
        this.shared = shared;
    }
}