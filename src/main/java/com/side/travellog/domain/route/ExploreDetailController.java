package com.side.travellog.domain.route;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class ExploreDetailController {

    private final TravelRouteRepository travelRouteRepository;
    private final TravelRouteLikeRepository likeRepository;
    private final com.side.travellog.domain.user.UserRepository userRepository;

    @Value("${google.maps.key}")
    private String googleMapsKey;

    @GetMapping("/explore/{routeId}")
    @Transactional
    public String exploreDetail(@PathVariable Long routeId,
                                @AuthenticationPrincipal UserDetails userDetails,
                                Model model) {
        TravelRoute route = travelRouteRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 여행입니다."));

        if (!route.isPublic()) {
            return "redirect:/explore";
        }

        boolean liked = false;
        int likeCount = likeRepository.countByTravelRoute(route);

        if (userDetails != null) {
            com.side.travellog.domain.user.User user =
                    userRepository.findByEmail(userDetails.getUsername());
            liked = likeRepository.findByTravelRouteAndUser(route, user).isPresent();
        }

        model.addAttribute("route", route);
        model.addAttribute("liked", liked);
        model.addAttribute("likeCount", likeCount);
        model.addAttribute("googleMapsKey", googleMapsKey);
        return "explore-detail";
    }
}