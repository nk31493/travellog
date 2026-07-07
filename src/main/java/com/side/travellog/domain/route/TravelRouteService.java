package com.side.travellog.domain.route;

import com.side.travellog.domain.notification.NotificationService;
import com.side.travellog.domain.pin.PinCommentRepository;
import com.side.travellog.domain.pin.PinPhotoRepository;
import com.side.travellog.domain.pin.TravelPin;
import com.side.travellog.domain.pin.TravelPinRepository;
import com.side.travellog.domain.user.User;
import com.side.travellog.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TravelRouteService {

    private final NotificationService notificationService;
    private final ExpenseRepository expenseRepository;
    private final SettlementCheckRepository settlementCheckRepository;
    private final PinPhotoRepository pinPhotoRepository;
    private final PinCommentRepository pinCommentRepository;
    private final TravelRouteRepository travelRouteRepository;
    private final TravelPinRepository travelPinRepository;
    private final UserRepository userRepository;
    private final RouteCollaboratorRepository routeCollaboratorRepository;
    private final ChecklistItemRepository checklistItemRepository;

    public List<TravelRoute> getRoutesByUser(String email) {
        User user = userRepository.findByEmail(email);
        return travelRouteRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public TravelRoute getRouteById(Long routeId, String email) {
        TravelRoute route = travelRouteRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 여행입니다."));
        if (!route.getUser().getEmail().equals(email)) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }
        return route;
    }

    public List<TravelRoute> getAllRoutesByUser(String email) {
        User user = userRepository.findByEmail(email);
        List<TravelRoute> owned = travelRouteRepository.findByUserOrderByStartDateDesc(user);
        List<TravelRoute> collaborating = routeCollaboratorRepository.findByUser(user)
                .stream()
                .map(RouteCollaborator::getTravelRoute)
                .toList();

        List<TravelRoute> all = new java.util.ArrayList<>(owned);
        all.addAll(collaborating);
        all.sort((a, b) -> {
            if (a.getStartDate() == null) return 1;
            if (b.getStartDate() == null) return -1;
            return b.getStartDate().compareTo(a.getStartDate());
        });
        return all;
    }

    public TravelRoute createRoute(String email, String name, String destination,
                                    LocalDate startDate, LocalDate endDate) {
        User user = userRepository.findByEmail(email);
        TravelRoute route = TravelRoute.builder()
                .user(user)
                .name(name)
                .destination(destination)
                .startDate(startDate)
                .endDate(endDate)
                .build();
        return travelRouteRepository.save(route);
    }

    @Transactional
    public void deleteRoute(Long routeId, String email) {
        TravelRoute route = travelRouteRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 여행입니다."));
        if (!route.getUser().getEmail().equals(email)) {
            throw new IllegalArgumentException("삭제 권한이 없습니다.");
        }

        List<RouteCollaborator> collaborators = routeCollaboratorRepository.findByTravelRouteOrderByJoinedAtAsc(route);

        if (collaborators.isEmpty()) {
            // 핀 관련 자식 데이터 먼저 삭제
            List<TravelPin> pins = travelPinRepository.findByTravelRouteOrdered(route);
            for (TravelPin pin : pins) {
                pinCommentRepository.deleteByTravelPin(pin);
                pinPhotoRepository.deleteByTravelPin(pin);
            }
            // 경비 정산 체크 먼저 삭제
            List<com.side.travellog.domain.route.Expense> expenses =
                    expenseRepository.findByTravelRouteAndSharedTrueOrderByExpenseDateAsc(route);
            for (com.side.travellog.domain.route.Expense expense : expenses) {
                settlementCheckRepository.deleteByExpense(expense);
            }
            expenseRepository.deleteByTravelRoute(route);
            checklistItemRepository.deleteByTravelRoute(route);
            routeCollaboratorRepository.deleteByTravelRoute(route);
            travelPinRepository.deleteByTravelRoute(route);
            travelRouteRepository.delete(route);
        } else {
            // 협업자 중 가장 먼저 참여한 사람에게 소유권 이전
            RouteCollaborator newOwnerCollaborator = collaborators.get(0);
            User newOwner = newOwnerCollaborator.getUser();

            // 기존 소유자 체크리스트 삭제
            User oldOwner = route.getUser();
            List<ChecklistItem> oldOwnerChecklist = checklistItemRepository
                    .findByTravelRouteAndUserOrderByOrderNumAsc(route, oldOwner);
            checklistItemRepository.deleteAll(oldOwnerChecklist);

            // 소유권 이전
            route.updateOwner(newOwner);
            travelRouteRepository.save(route);

            // 새 소유자는 협업자 목록에서 제거
            routeCollaboratorRepository.delete(newOwnerCollaborator);
        }
    }

    @Transactional
    public void updateRoute(Long routeId, String email, String name,
                        String destination, LocalDate startDate, LocalDate endDate) {
        TravelRoute route = getRouteByIdWithAccess(routeId, email);
        route.updateInfo(name, destination, startDate, endDate);
        travelRouteRepository.save(route);

        // 날짜 범위 벗어난 핀 자동 삭제
        List<TravelPin> pins = travelPinRepository.findByTravelRouteOrdered(route);
        for (TravelPin pin : pins) {
            boolean outOfRange = pin.getVisitDate().isBefore(startDate) ||
                    pin.getVisitDate().isAfter(endDate);
            if (outOfRange) {
                pinCommentRepository.deleteByTravelPin(pin);
                pinPhotoRepository.deleteByTravelPin(pin);
                travelPinRepository.delete(pin);
            }
        }
    }

    public void leaveCollaboration(Long routeId, String email) {
        TravelRoute route = travelRouteRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 여행입니다."));
        User user = userRepository.findByEmail(email);

        if (route.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("여행 소유자는 나갈 수 없습니다. 삭제를 이용해주세요.");
        }

        RouteCollaborator collaborator = routeCollaboratorRepository.findByTravelRouteAndUser(route, user)
                .orElseThrow(() -> new IllegalArgumentException("참여 중인 여행이 아닙니다."));
        routeCollaboratorRepository.delete(collaborator);
    }

    public TravelRoute getRouteByShareToken(String shareToken) {
    return travelRouteRepository.findByShareToken(shareToken)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공유 링크입니다."));
    }

    public String generateShareToken(Long routeId, String email) {
        TravelRoute route = getRouteByIdWithAccess(routeId, email);
        String token = java.util.UUID.randomUUID().toString().replace("-", "");

        route.updateShareToken(token);
        travelRouteRepository.save(route);
        return token;
    }

    public List<TravelPin> getPinsByRoute(Long routeId, String email) {
        TravelRoute route = getRouteById(routeId, email);
        return travelPinRepository.findByTravelRouteOrdered(route);
    }

    // 본인 소유 또는 협업자인지 확인하고 라우트 반환
    public TravelRoute getRouteByIdWithAccess(Long routeId, String email) {
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

    // 초대 토큰 생성 (소유자만 가능)
    public String generateInviteToken(Long routeId, String email) {
        TravelRoute route = getRouteById(routeId, email);
        String token = java.util.UUID.randomUUID().toString().replace("-", "");
        route.updateInviteToken(token);
        travelRouteRepository.save(route);
        return token;
    }

    // 초대 링크로 참여하기
    public TravelRoute joinByInviteToken(String inviteToken, String email) {
        TravelRoute route = travelRouteRepository.findByInviteToken(inviteToken)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 초대 링크입니다."));

        User user = userRepository.findByEmail(email);

        boolean isOwner = route.getUser().getId().equals(user.getId());
        boolean already = routeCollaboratorRepository.existsByTravelRouteAndUser(route, user);

        if (!isOwner && !already) {
            RouteCollaborator collaborator = RouteCollaborator.builder()
                    .travelRoute(route)
                    .user(user)
                    .build();
            routeCollaboratorRepository.save(collaborator);
        }
        return route;
    }

    // 협업자 목록 조회
    public List<User> getCollaborators(Long routeId, String email) {
        TravelRoute route = getRouteByIdWithAccess(routeId, email);
        return routeCollaboratorRepository.findByTravelRoute(route)
                .stream()
                .map(RouteCollaborator::getUser)
                .toList();
    }

    // 협업자 내쫓기 (소유자만)
    public void removeCollaborator(Long routeId, Long collaboratorUserId, String email) {
        TravelRoute route = getRouteById(routeId, email);
        User targetUser = userRepository.findById(collaboratorUserId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        routeCollaboratorRepository.findByTravelRouteAndUser(route, targetUser)
                .ifPresent(routeCollaboratorRepository::delete);
    }

    // 여행 검색
    public List<TravelRoute> searchRoutes(String email, String keyword) {
        User user = userRepository.findByEmail(email);
        if (keyword == null || keyword.isBlank()) {
            return getAllRoutesByUser(email);
        }
        return travelRouteRepository.searchByUser(user, keyword);
    }

    public List<TravelRoute> getCollaboratingRoutesByUser(String email) {
        User user = userRepository.findByEmail(email);
        return routeCollaboratorRepository.findByUser(user)
                .stream()
                .map(RouteCollaborator::getTravelRoute)
                .toList();
    }

    public List<Long> getRoutesWithCollaborators(String email) {
        User user = userRepository.findByEmail(email);
        List<TravelRoute> owned = travelRouteRepository.findByUserOrderByStartDateDesc(user);
        return owned.stream()
                .filter(route -> !routeCollaboratorRepository.findByTravelRoute(route).isEmpty())
                .map(TravelRoute::getId)
                .toList();
    }

    public void inviteByNickname(Long routeId, String nickname, String email) {
        TravelRoute route = travelRouteRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 여행입니다."));
        User requester = userRepository.findByEmail(email);

        // 소유자만 초대 가능
        if (!route.getUser().getId().equals(requester.getId())) {
            throw new IllegalArgumentException("소유자만 초대할 수 있습니다.");
        }

        User invitee = userRepository.findByNickname(nickname);
        if (invitee == null) {
            throw new IllegalArgumentException("존재하지 않는 닉네임입니다.");
        }
        if (invitee.getId().equals(requester.getId())) {
            throw new IllegalArgumentException("본인은 초대할 수 없습니다.");
        }
        if (routeCollaboratorRepository.existsByTravelRouteAndUser(route, invitee)) {
            throw new IllegalArgumentException("이미 협업 중인 사용자입니다.");
        }

        routeCollaboratorRepository.save(RouteCollaborator.builder()
                .travelRoute(route)
                .user(invitee)
                .build());

        // 초대받은 사람에게 알림
        notificationService.send(
                invitee,
                requester.getNickname() + "님이 '" + route.getName() + "' 여행에 초대했어요!",
                "/trips/" + route.getId()
        );
    }

    public void updatePublic(Long routeId, String email, boolean isPublic) {
        TravelRoute route = getRouteByIdWithAccess(routeId, email);
        route.updatePublic(isPublic);
        // 공개 설정 시 shareToken 자동 생성
        if (isPublic && (route.getShareToken() == null || route.getShareToken().isBlank())) {
            String token = java.util.UUID.randomUUID().toString().replace("-", "");
            route.updateShareToken(token);
        }
        travelRouteRepository.save(route);
    }

    public List<TravelRoute> getPublicRoutes() {
        return travelRouteRepository.findByIsPublicTrueOrderByCreatedAtDesc();
    }

    public List<TravelRoute> getPublicRoutesSorted(String sort, String email) {
        if (email != null) {
            User user = userRepository.findByEmail(email);
            if ("likes".equals(sort)) {
                return travelRouteRepository.findPublicRoutesOrderByLikesExcludeUser(user);
            }
            return travelRouteRepository.findPublicRoutesOrderByCreatedAtExcludeUser(user);
        }
        if ("likes".equals(sort)) {
            return travelRouteRepository.findPublicRoutesOrderByLikes();
        }
        return travelRouteRepository.findPublicRoutesOrderByCreatedAt();
    }

    public List<TravelRoute> searchPublicRoutes(String keyword, String email) {
        if (email != null) {
            User user = userRepository.findByEmail(email);
            return travelRouteRepository.searchPublicRoutesExcludeUser(keyword, user);
        }
        return travelRouteRepository.searchPublicRoutes(keyword);
    }

    public List<String> getPublicDestinations() {
        return travelRouteRepository.findPublicDestinations();
    }

    @Transactional
    public TravelRoute copyRoute(Long routeId, String email) {
        TravelRoute original = travelRouteRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 여행입니다."));
        if (!original.isPublic()) {
            throw new IllegalArgumentException("공개된 여행만 복사할 수 있습니다.");
        }
        User user = userRepository.findByEmail(email);

        // 날짜는 오늘부터 원본 기간만큼
        long days = 0;
        if (original.getStartDate() != null && original.getEndDate() != null) {
            days = java.time.temporal.ChronoUnit.DAYS.between(original.getStartDate(), original.getEndDate());
        }
        java.time.LocalDate newStart = java.time.LocalDate.now();
        java.time.LocalDate newEnd = newStart.plusDays(days);

        TravelRoute copy = TravelRoute.builder()
                .user(user)
                .name("[복사] " + original.getName())
                .destination(original.getDestination())
                .startDate(newStart)
                .endDate(newEnd)
                .build();
        travelRouteRepository.save(copy);

        // 핀도 복사 (날짜 오프셋 적용)
        List<TravelPin> pins = travelPinRepository.findByTravelRouteOrdered(original);
        for (TravelPin pin : pins) {
            long pinOffset = original.getStartDate() != null && pin.getVisitDate() != null
                    ? java.time.temporal.ChronoUnit.DAYS.between(original.getStartDate(), pin.getVisitDate()) : 0;
            TravelPin copiedPin = TravelPin.builder()
                    .user(user)
                    .travelRoute(copy)
                    .title(pin.getTitle())
                    .latitude(pin.getLatitude())
                    .longitude(pin.getLongitude())
                    .visitDate(newStart.plusDays(pinOffset))
                    .endDate(newStart.plusDays(pinOffset))
                    .visitTime(pin.getVisitTime())
                    .memo(pin.getMemo())
                    .orderNum(pin.getOrderNum())
                    .build();
            travelPinRepository.save(copiedPin);
        }

        return copy;
    }
}