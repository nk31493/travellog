package com.side.travellog.domain.route;

import com.side.travellog.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RouteCollaboratorRepository extends JpaRepository<RouteCollaborator, Long> {
    List<RouteCollaborator> findByTravelRoute(TravelRoute travelRoute);
    List<RouteCollaborator> findByTravelRouteOrderByJoinedAtAsc(TravelRoute travelRoute);
    Optional<RouteCollaborator> findByTravelRouteAndUser(TravelRoute travelRoute, User user);
    boolean existsByTravelRouteAndUser(TravelRoute travelRoute, User user);
    List<RouteCollaborator> findByUser(User user);
    void deleteByTravelRoute(TravelRoute travelRoute);
}