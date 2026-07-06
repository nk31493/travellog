package com.side.travellog.domain.pin;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class PinDetailController {

    private final TravelPinService travelPinService;

    @GetMapping("/pin/{id}")
    public String pinDetail(@PathVariable Long id,
                            @AuthenticationPrincipal UserDetails userDetails,
                            Model model) {
        TravelPin pin = travelPinService.getPinById(id, userDetails.getUsername());
        model.addAttribute("pin", pin);
        return "pin-detail";
    }
}