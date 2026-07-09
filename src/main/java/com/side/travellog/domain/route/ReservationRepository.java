package com.side.travellog.domain.route;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByTravelRouteOrderByStartDateTimeAsc(TravelRoute travelRoute);
    void deleteByTravelRoute(TravelRoute travelRoute);
}