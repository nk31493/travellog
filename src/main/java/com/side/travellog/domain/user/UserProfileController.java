package com.side.travellog.domain.user;

import com.side.travellog.domain.route.TravelRoute;
import com.side.travellog.domain.route.TravelRouteLikeRepository;
import com.side.travellog.domain.route.TravelRouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class UserProfileController {

    private final UserRepository userRepository;
    private final TravelRouteRepository travelRouteRepository;
    private final TravelRouteLikeRepository likeRepository;

    @GetMapping("/user/{userId}")
    @Transactional
    public String userProfile(@PathVariable Long userId,
                              @AuthenticationPrincipal UserDetails userDetails,
                              Model model) {
        User profileUser = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        List<TravelRoute> publicRoutes = travelRouteRepository
                .findByUserOrderByStartDateDesc(profileUser).stream()
                .filter(TravelRoute::isPublic)
                .toList();

        Map<Long, Integer> likeCounts = new HashMap<>();
        publicRoutes.forEach(r -> likeCounts.put(r.getId(), likeRepository.countByTravelRoute(r)));

        boolean isMe = userDetails != null && userDetails.getUsername().equals(profileUser.getEmail());

        model.addAttribute("profileUser", profileUser);
        model.addAttribute("routes", publicRoutes);
        model.addAttribute("likeCounts", likeCounts);
        model.addAttribute("isMe", isMe);
        model.addAttribute("today", java.time.LocalDate.now());
        return "user-profile";
    }
}