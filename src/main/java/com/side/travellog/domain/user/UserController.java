package com.side.travellog.domain.user;

import com.side.travellog.domain.pin.TravelPin;
import com.side.travellog.domain.pin.TravelPinService;
import com.side.travellog.domain.route.TravelRoute;
import com.side.travellog.domain.route.TravelRouteService;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final TravelPinService travelPinService;
    private final TravelRouteService travelRouteService;
    private final com.side.travellog.domain.route.ExpenseService expenseService;
    
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/signup")
    public String signupPage() {
        return "signup";
    }

    @PostMapping("/signup")
    public String signup(@RequestParam String email,
                         @RequestParam String password,
                         @RequestParam String nickname,
                         Model model) {
        try {
            userService.register(email, password, nickname);
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "signup";
        }
    }

    @Value("${google.maps.key}")
    private String googleMapsKey;

    @GetMapping("/mypage")
    public String mypage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByEmail(userDetails.getUsername());
        model.addAttribute("user", user);
        model.addAttribute("googleMapsKey", googleMapsKey);
        return "mypage";
    }

    @PutMapping("/api/mypage/nickname")
    @ResponseBody
    public ResponseEntity<?> updateNickname(@AuthenticationPrincipal UserDetails userDetails,
                                             @RequestBody Map<String, String> body) {
        userService.updateNickname(userDetails.getUsername(), body.get("nickname"));
        return ResponseEntity.ok().build();
    }

    @PutMapping("/api/mypage/password")
    @ResponseBody
    public ResponseEntity<?> updatePassword(@AuthenticationPrincipal UserDetails userDetails,
                                            @RequestBody Map<String, String> body) {
        try {
            userService.updatePassword(userDetails.getUsername(),
                    body.get("currentPassword"), body.get("newPassword"));
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/api/mypage/stats")
    @ResponseBody
    public Map<String, Object> getStats(@AuthenticationPrincipal UserDetails userDetails) {
        List<TravelPin> pins = travelPinService.getPinsByUser(userDetails.getUsername());
        List<TravelRoute> routes = travelRouteService.getAllRoutesByUser(userDetails.getUsername());

        Set<String> countries = new java.util.HashSet<>();
        Set<String> cities = new java.util.HashSet<>();

        // 국가별 도시 맵
        Map<String, Set<String>> countryCityMap = new java.util.LinkedHashMap<>();

        routes.forEach(route -> {
            if (route.getDestination() != null && !route.getDestination().isEmpty()) {
                String[] parts = route.getDestination().split(",");
                if (parts.length >= 2) {
                    String city = parts[0].trim();
                    String country = parts[parts.length - 1].trim();
                    cities.add(city);
                    countries.add(country);
                    countryCityMap.computeIfAbsent(country, k -> new java.util.LinkedHashSet<>()).add(city);
                } else {
                    cities.add(parts[0].trim());
                }
            }
        });

        // 월별 여행 횟수
        Map<String, Integer> monthlyTrips = new java.util.LinkedHashMap<>();
        for (int i = 1; i <= 12; i++) {
            monthlyTrips.put(String.format("%02d", i) + "월", 0);
        }
        routes.forEach(route -> {
            if (route.getStartDate() != null) {
                String month = String.format("%02d", route.getStartDate().getMonthValue()) + "월";
                monthlyTrips.merge(month, 1, Integer::sum);
            }
        });

        // 여행별 총 경비
        List<Map<String, Object>> tripExpenses = routes.stream().map(route -> {
            Map<String, Object> m = new HashMap<>();
            m.put("name", route.getName());
            int total = expenseService.getTotalByRoute(route.getId(), userDetails.getUsername());
            m.put("total", total);
            return m;
        }).filter(m -> (int) m.get("total") > 0).toList();

        // 국가별 방문 횟수
        Map<String, Integer> countryCount = new java.util.LinkedHashMap<>();
        routes.forEach(route -> {
            if (route.getDestination() != null && !route.getDestination().isEmpty()) {
                String[] parts = route.getDestination().split(",");
                String country = parts.length >= 2 ? parts[parts.length - 1].trim() : parts[0].trim();
                countryCount.merge(country, 1, Integer::sum);
            }
        });

        // 중복 장소명 제거한 핀 수
        long uniquePins = pins.stream()
                .map(TravelPin::getTitle)
                .distinct()
                .count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTrips", routes.size());
        stats.put("totalPins", uniquePins);
        stats.put("countries", new java.util.ArrayList<>(countries));
        stats.put("cities", new java.util.ArrayList<>(cities));
        stats.put("monthlyTrips", monthlyTrips);
        stats.put("tripExpenses", tripExpenses);
        stats.put("countryCount", countryCount);
        stats.put("countryCityMap", countryCityMap);
        return stats;
    }

}