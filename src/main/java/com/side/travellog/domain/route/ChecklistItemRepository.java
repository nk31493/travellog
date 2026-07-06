package com.side.travellog.domain.route;

import com.side.travellog.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChecklistItemRepository extends JpaRepository<ChecklistItem, Long> {
    List<ChecklistItem> findByTravelRouteAndUserOrderByOrderNumAsc(TravelRoute travelRoute, User user);
    void deleteByTravelRoute(TravelRoute travelRoute);
}