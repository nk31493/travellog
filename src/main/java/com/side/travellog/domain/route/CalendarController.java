package com.side.travellog.domain.route;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

@Controller
@RequiredArgsConstructor
public class CalendarController {

    private final TravelRouteService travelRouteService;
    private final com.side.travellog.domain.pin.PinPhotoRepository pinPhotoRepository;

    @GetMapping("/calendar")
    public String calendarPage() {
        return "calendar";
    }

    @GetMapping("/api/calendar/trips")
    @ResponseBody
    @Transactional
    public List<Map<String, Object>> getCalendarTrips(@AuthenticationPrincipal UserDetails userDetails) {
        List<TravelRoute> all = travelRouteService.getAllRoutesByUser(userDetails.getUsername());

        return all.stream().map(route -> {
            Map<String, Object> map = new HashMap<>();
            map.put("routeId", route.getId());
            map.put("name", route.getName());
            map.put("destination", route.getDestination());
            map.put("startDate", route.getStartDate() != null ? route.getStartDate().toString() : null);
            map.put("endDate", route.getEndDate() != null ? route.getEndDate().toString() : null);

            // 핀별 첫 번째 사진 날짜 + 경로 목록
            Map<String, String> photoDateMap = new java.util.LinkedHashMap<>();
            route.getPins().forEach(pin -> {
                List<com.side.travellog.domain.pin.PinPhoto> photos =
                        pinPhotoRepository.findByTravelPinOrderByOrderNumAsc(pin);
                if (!photos.isEmpty() && pin.getVisitDate() != null) {
                    // 대표 사진이 있으면 그걸 쓰고, 없으면 첫 번째 사진
                    String path = photos.stream()
                            .filter(p -> p.isRepresentative())
                            .findFirst()
                            .map(p -> p.getFilePath())
                            .orElse(photos.get(0).getFilePath());
                    photoDateMap.putIfAbsent(pin.getVisitDate().toString(), path);
                }
            });
            map.put("photoDateMap", photoDateMap);
                        return map;
                    }).toList();
    }
}