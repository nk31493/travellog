package com.side.travellog.domain.route;

import com.side.travellog.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByTravelRouteAndUserOrderByExpenseDateAsc(TravelRoute travelRoute, User user);
    List<Expense> findByTravelRouteAndSharedTrueOrderByExpenseDateAsc(TravelRoute travelRoute);
    void deleteByTravelRoute(TravelRoute travelRoute);      
}