package com.side.travellog.domain.pin;

import com.side.travellog.domain.route.TravelRoute;
import com.side.travellog.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface TravelPinRepository extends JpaRepository<TravelPin, Long> {
    List<TravelPin> findByUser(User user);

    @Query("SELECT p FROM TravelPin p WHERE p.travelRoute = :route ORDER BY COALESCE(p.orderNum, 0) ASC, p.visitDate ASC, p.visitTime ASC")
    List<TravelPin> findByTravelRouteOrdered(@Param("route") TravelRoute route);
    void deleteByTravelRoute(TravelRoute travelRoute);
}
