package com.side.travellog.domain.route;

import com.side.travellog.domain.user.User;
import com.side.travellog.domain.user.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChecklistService {

    private final ChecklistItemRepository checklistItemRepository;
    private final TravelRouteRepository travelRouteRepository;
    private final UserRepository userRepository;
    private final RouteCollaboratorRepository routeCollaboratorRepository;

    private static final List<String> DEFAULT_ITEMS = List.of(
            "여권", "환전", "충전기", "보조 배터리", "세면도구", "화장품", "상비약", "상의", "하의", "속옷, 양말", "어댑터"
    );

    private TravelRoute checkAccess(Long routeId, User user) {
        TravelRoute route = travelRouteRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 여행입니다."));

        boolean isOwner = route.getUser().getId().equals(user.getId());
        boolean isCollaborator = routeCollaboratorRepository.existsByTravelRouteAndUser(route, user);
        if (!isOwner && !isCollaborator) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }
        return route;
    }

    @Transactional
    public void createDefaultItems(TravelRoute route, User user) {
        for (int i = 0; i < DEFAULT_ITEMS.size(); i++) {
            checklistItemRepository.save(ChecklistItem.builder()
                    .travelRoute(route)
                    .user(user)
                    .content(DEFAULT_ITEMS.get(i))
                    .orderNum(i)
                    .build());
        }
    }

    public ChecklistItem addItem(Long routeId, String email, String content) {
        User user = userRepository.findByEmail(email);
        TravelRoute route = checkAccess(routeId, user);

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("내용을 입력해주세요.");
        }
        if (content.length() > 100) {
            throw new IllegalArgumentException("100자 이하로 입력해주세요.");
        }

        int nextOrder = checklistItemRepository.findByTravelRouteAndUserOrderByOrderNumAsc(route, user).size();
        return checklistItemRepository.save(ChecklistItem.builder()
                .travelRoute(route)
                .user(user)
                .content(content)
                .orderNum(nextOrder)
                .build());
    }

    public List<ChecklistItem> getItems(Long routeId, String email) {
        User user = userRepository.findByEmail(email);
        TravelRoute route = checkAccess(routeId, user);
        return checklistItemRepository.findByTravelRouteAndUserOrderByOrderNumAsc(route, user);
    }

    public void toggleItem(Long itemId, String email) {
        ChecklistItem item = checklistItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 항목입니다."));
        User user = userRepository.findByEmail(email);
        if (!item.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("본인 항목만 수정할 수 있습니다.");
        }
        item.toggleChecked();
        checklistItemRepository.save(item);
    }

    public void deleteItem(Long itemId, String email) {
        ChecklistItem item = checklistItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 항목입니다."));
        User user = userRepository.findByEmail(email);
        if (!item.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("본인 항목만 삭제할 수 있습니다.");
        }
        checklistItemRepository.delete(item);
    }

    public void fillDefaultItemsIfEmpty(Long routeId, String email) {
        User user = userRepository.findByEmail(email);
        TravelRoute route = checkAccess(routeId, user);
        List<ChecklistItem> existing = checklistItemRepository.findByTravelRouteAndUserOrderByOrderNumAsc(route, user);
        if (existing.isEmpty()) {
            createDefaultItems(route, user);
        }
    }
}