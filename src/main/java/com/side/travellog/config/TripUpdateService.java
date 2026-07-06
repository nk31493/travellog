package com.side.travellog.config;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TripUpdateService {

    private final SimpMessagingTemplate messagingTemplate;

    public void notifyPinChanged(Long routeId) {
        messagingTemplate.convertAndSend("/topic/trip/" + routeId, "PIN_CHANGED");
    }

    public void notifyCommentChanged(Long routeId) {
        messagingTemplate.convertAndSend("/topic/trip/" + routeId, "COMMENT_CHANGED");
    }

    public void notifyChecklistChanged(Long routeId) {
        messagingTemplate.convertAndSend("/topic/trip/" + routeId, "CHECKLIST_CHANGED");
    }

    public void notifyExpenseChanged(Long routeId) {
        messagingTemplate.convertAndSend("/topic/trip/" + routeId, "EXPENSE_CHANGED");
    }
}