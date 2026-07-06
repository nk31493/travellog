package com.side.travellog.domain.route;

import com.side.travellog.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TravelRouteRepository extends JpaRepository<TravelRoute, Long> {
    List<TravelRoute> findByUserOrderByCreatedAtDesc(User user);
    @Query("SELECT r FROM TravelRoute r WHERE r.user = :user AND " +
           "(r.name LIKE %:keyword% OR r.destination LIKE %:keyword%) " +
           "ORDER BY r.createdAt DESC")
    List<TravelRoute> searchByUser(@Param("user") com.side.travellog.domain.user.User user,
                                    @Param("keyword") String keyword);

    List<TravelRoute> findByUserOrderByStartDateDesc(com.side.travellog.domain.user.User user);

    java.util.Optional<TravelRoute> findByShareToken(String shareToken);
    java.util.Optional<TravelRoute> findByInviteToken(String inviteToken);
}