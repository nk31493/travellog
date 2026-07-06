package com.side.travellog.domain.route;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class OnboardingController {

    private final TravelRouteService travelRouteService;
    private final ChecklistService checklistService;
    private final com.side.travellog.domain.user.UserRepository userRepository;

    @Value("${google.maps.key}")
    private String googleMapsKey;

    @GetMapping("/onboarding/destination")
    public String destinationPage(Model model) {
        model.addAttribute("googleMapsKey", googleMapsKey);
        return "onboarding-destination";
    }

    @GetMapping("/onboarding/dates")
    public String datesPage() {
        return "onboarding-dates";
    }

    @GetMapping("/onboarding/name")
    public String namePage() {
        return "onboarding-name";
    }

    @PostMapping("/api/onboarding/complete")
    @ResponseBody
    public Map<String, Long> complete(@AuthenticationPrincipal UserDetails userDetails,
                                    @RequestBody Map<String, String> body) {
        LocalDate startDate = LocalDate.parse(body.get("startDate"));
        LocalDate endDate = LocalDate.parse(body.get("endDate"));

        TravelRoute route = travelRouteService.createRoute(
                userDetails.getUsername(),
                body.get("name"),
                body.get("destination"),
                startDate,
                endDate
        );

        com.side.travellog.domain.user.User user = userRepository.findByEmail(userDetails.getUsername());
        checklistService.createDefaultItems(route, user);

        return Map.of("routeId", route.getId());
    }
}