package com.side.travellog.domain.route;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.transaction.Transactional;

import java.util.*;

@Controller
@RequiredArgsConstructor
public class ExploreController {

    private final TravelRouteService travelRouteService;
    private final TravelRouteLikeRepository likeRepository;
    private final com.side.travellog.domain.user.UserRepository userRepository;

    @GetMapping("/explore")
    @Transactional
    public String explore(@RequestParam(required = false) String keyword,
                        @RequestParam(required = false) String sort,
                        @RequestParam(required = false) String destination,
                        @AuthenticationPrincipal UserDetails userDetails,
                        Model model) {
        String email = userDetails != null ? userDetails.getUsername() : null;

        List<TravelRoute> routes;
        if (keyword != null && !keyword.isBlank()) {
            routes = travelRouteService.searchPublicRoutes(keyword, email);
        } else if (destination != null && !destination.isBlank()) {
            routes = travelRouteService.getPublicRoutesSorted(sort, email).stream()
                    .filter(r -> r.getDestination() != null && r.getDestination().contains(destination))
                    .toList();
        } else {
            routes = travelRouteService.getPublicRoutesSorted(sort, email);
        }

        com.side.travellog.domain.user.User user = userDetails != null
                ? userRepository.findByEmail(email) : null;

        List<Long> likedIds = user != null
                ? likeRepository.findByUser(user).stream()
                    .map(l -> l.getTravelRoute().getId()).toList()
                : new java.util.ArrayList<>();
                
        Map<Long, Integer> likeCounts = new HashMap<>();
        routes.forEach(r -> likeCounts.put(r.getId(), likeRepository.countByTravelRoute(r)));

        model.addAttribute("routes", routes);
        model.addAttribute("likedIds", likedIds);
        model.addAttribute("likeCounts", likeCounts);
        model.addAttribute("keyword", keyword);
        model.addAttribute("sort", sort);
        model.addAttribute("destination", destination);
        model.addAttribute("destinations", travelRouteService.getPublicDestinations());
        model.addAttribute("today", java.time.LocalDate.now());
        return "explore";
    }
}