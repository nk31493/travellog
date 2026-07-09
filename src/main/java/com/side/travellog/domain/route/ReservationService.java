package com.side.travellog.domain.route;

import com.side.travellog.domain.user.User;
import com.side.travellog.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final TravelRouteRepository travelRouteRepository;
    private final UserRepository userRepository;
    private final RouteCollaboratorRepository routeCollaboratorRepository;

    private TravelRoute checkAccess(Long routeId, User user) {
        TravelRoute route = travelRouteRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 여행입니다."));
        boolean isOwner = route.getUser().getId().equals(user.getId());
        boolean isCollaborator = routeCollaboratorRepository.existsByTravelRouteAndUser(route, user);
        if (!isOwner && !isCollaborator) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }
        return route;
    }

    public Reservation addReservation(Long routeId, String email, String type, String title,
                                       String confirmationNumber, LocalDateTime startDateTime,
                                       LocalDateTime endDateTime, String memo) {
        User user = userRepository.findByEmail(email);
        TravelRoute route = checkAccess(routeId, user);

        return reservationRepository.save(Reservation.builder()
                .travelRoute(route)
                .type(type)
                .title(title)
                .confirmationNumber(confirmationNumber)
                .startDateTime(startDateTime)
                .endDateTime(endDateTime)
                .memo(memo)
                .build());
    }

    public List<Reservation> getReservations(Long routeId, String email) {
        User user = userRepository.findByEmail(email);
        TravelRoute route = checkAccess(routeId, user);
        return reservationRepository.findByTravelRouteOrderByStartDateTimeAsc(route);
    }

    public void updateReservation(Long reservationId, String email, String type, String title,
                                   String confirmationNumber, LocalDateTime startDateTime,
                                   LocalDateTime endDateTime, String memo) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예약입니다."));
        User user = userRepository.findByEmail(email);
        checkAccess(reservation.getTravelRoute().getId(), user);
        reservation.updateInfo(type, title, confirmationNumber, startDateTime, endDateTime, memo);
        reservationRepository.save(reservation);
    }

    public void deleteReservation(Long reservationId, String email) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예약입니다."));
        User user = userRepository.findByEmail(email);
        checkAccess(reservation.getTravelRoute().getId(), user);
        reservationRepository.delete(reservation);
    }
}