package com.side.travellog.domain.route;

import com.side.travellog.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TravelRouteLikeRepository extends JpaRepository<TravelRouteLike, Long> {
    List<TravelRouteLike> findByUser(User user);
    Optional<TravelRouteLike> findByTravelRouteAndUser(TravelRoute travelRoute, User user);
    int countByTravelRoute(TravelRoute travelRoute);
}