package com.side.travellog.domain.pin;

import com.side.travellog.domain.route.RouteCollaboratorRepository;
import com.side.travellog.domain.route.TravelRoute;
import com.side.travellog.domain.route.TravelRouteRepository;
import com.side.travellog.domain.route.TravelRouteService;
import com.side.travellog.domain.user.User;
import com.side.travellog.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TravelPinService {

    private final TravelPinRepository travelPinRepository;
    private final TravelRouteRepository travelRouteRepository;
    private final UserRepository userRepository;
    private final RouteCollaboratorRepository routeCollaboratorRepository;
    private final TravelRouteService travelRouteService;

    // 라우트에 대한 접근 권한(소유자 또는 협업자) 체크
    private TravelRoute checkRouteAccess(Long routeId, String email) {
        TravelRoute route = travelRouteRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 여행입니다."));
        User user = userRepository.findByEmail(email);

        boolean isOwner = route.getUser().getId().equals(user.getId());
        boolean isCollaborator = routeCollaboratorRepository.existsByTravelRouteAndUser(route, user);

        if (!isOwner && !isCollaborator) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }
        return route;
    }

    public TravelPin createPin(String email, Long routeId, String title,
                                Double latitude, Double longitude,
                                LocalDate visitDate, LocalDate endDate,
                                LocalTime visitTime, String memo) {
        User user = userRepository.findByEmail(email);
        TravelRoute route = checkRouteAccess(routeId, email);

        // 현재 같은 날짜의 핀 개수를 orderNum으로 설정
        List<TravelPin> existingPins = travelPinRepository.findByTravelRouteOrdered(route);
        int orderNum = (int) existingPins.stream()
                .filter(p -> p.getVisitDate().equals(visitDate))
                .count();

        TravelPin pin = TravelPin.builder()
            .user(user)
            .travelRoute(route)
            .title(title)
            .latitude(latitude)
            .longitude(longitude)
            .visitDate(visitDate)
            .endDate(endDate)
            .visitTime(visitTime)
            .memo(memo)
            .orderNum(orderNum)
            .build();

        return travelPinRepository.save(pin);
    }

    public TravelPin getPinById(Long pinId, String email) {
        TravelPin pin = travelPinRepository.findById(pinId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 핀입니다."));
        checkRouteAccess(pin.getTravelRoute().getId(), email);
        return pin;
    }

    public void updateMemo(Long pinId, String email, String memo) {
        TravelPin pin = getPinById(pinId, email);
        pin.updateMemo(memo);
        travelPinRepository.save(pin);
    }

    public void updateDates(Long pinId, String email, LocalDate visitDate,
                            LocalDate endDate, LocalTime visitTime) {
        TravelPin pin = getPinById(pinId, email);
        pin.updateDates(visitDate, endDate, visitTime);
        travelPinRepository.save(pin);
    }

    public void deletePin(Long pinId, String email) {
        TravelPin pin = getPinById(pinId, email);
        travelPinRepository.delete(pin);
    }

    public void updateTitle(Long pinId, String email, String title) {
        TravelPin pin = getPinById(pinId, email);
        pin.updateTitle(title);
        travelPinRepository.save(pin);
    }

    public void updateOrder(Long routeId, List<Long> pinIds, String email) {
        checkRouteAccess(routeId, email);

        for (int i = 0; i < pinIds.size(); i++) {
            TravelPin pin = travelPinRepository.findById(pinIds.get(i))
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 핀입니다."));
            pin.updateOrderNum(i);
            travelPinRepository.save(pin);
        }
    }

    public List<TravelPin> getPinsByUser(String email) {
        List<TravelRoute> allRoutes = travelRouteService.getAllRoutesByUser(email);
        return allRoutes.stream()
                .flatMap(route -> travelPinRepository.findByTravelRouteOrdered(route).stream())
                .toList();
    }

    public void updateRating(Long pinId, String email, Integer rating) {
        TravelPin pin = getPinById(pinId, email);
        pin.updateRating(rating == 0 ? null : rating);
        travelPinRepository.save(pin);
    }

    public TravelPin getPinByIdPublic(Long pinId) {
        return travelPinRepository.findById(pinId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 핀입니다."));
    }
}