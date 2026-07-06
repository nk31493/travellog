package com.side.travellog.domain.route;

import com.side.travellog.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SettlementCheckRepository extends JpaRepository<SettlementCheck, Long> {
    List<SettlementCheck> findByExpense(Expense expense);
    Optional<SettlementCheck> findByExpenseAndUser(Expense expense, User user);
    void deleteByExpense(Expense expense);
}