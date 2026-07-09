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

        // 필수 값 검증
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("장소 이름을 입력해주세요.");
        }
        if (title.length() > 100) {
            throw new IllegalArgumentException("장소 이름은 100자 이하로 입력해주세요.");
        }
        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException("위치 정보가 없어요. 장소를 다시 검색해주세요.");
        }
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("올바르지 않은 위치 정보예요.");
        }
        if (visitDate == null) {
            throw new IllegalArgumentException("방문 날짜를 입력해주세요.");
        }
        // 여행 날짜 범위 검증 (여행에 날짜가 설정된 경우만)
        if (route.getStartDate() != null && route.getEndDate() != null) {
            if (visitDate.isBefore(route.getStartDate()) || visitDate.isAfter(route.getEndDate())) {
                throw new IllegalArgumentException("여행 기간 안의 날짜만 선택할 수 있어요.");
            }
        }

        // 현재 같은 날짜의 핀 개수를 orderNum으로 설정
        List<TravelPin> existingPins = travelPinRepository.findByTravelRouteOrdered(route);
        int orderNum = (int) existingPins.stream()
                .filter(p -> p.getVisitDate() != null && p.getVisitDate().equals(visitDate))
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