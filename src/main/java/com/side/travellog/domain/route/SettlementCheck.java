package com.side.travellog.domain.route;

import com.side.travellog.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(uniqueConstraints = {
    @UniqueConstraint(columnNames = {"expense_id", "user_id"})
})
public class SettlementCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id")
    private Expense expense;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Builder
    public SettlementCheck(Expense expense, User user) {
        this.expense = expense;
        this.user = user;
    }
}