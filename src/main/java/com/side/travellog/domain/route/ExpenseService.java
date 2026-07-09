package com.side.travellog.domain.route;

import com.side.travellog.domain.user.User;
import com.side.travellog.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final TravelRouteRepository travelRouteRepository;
    private final UserRepository userRepository;
    private final RouteCollaboratorRepository routeCollaboratorRepository;
    private final SettlementCheckRepository settlementCheckRepository;

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

    public Expense addExpense(Long routeId, String email, String title,
                                Integer amount, String category, LocalDate expenseDate, boolean shared) {
        User user = userRepository.findByEmail(email);
        TravelRoute route = checkAccess(routeId, user);

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("항목을 입력해주세요.");
        }
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("금액은 0보다 커야 해요.");
        }
        if (amount > 100_000_000) {
            throw new IllegalArgumentException("금액이 너무 커요.");
        }

        return expenseRepository.save(Expense.builder()
                .travelRoute(route)
                .user(user)
                .title(title)
                .amount(amount)
                .category(category)
                .expenseDate(expenseDate)
                .shared(shared)
                .build());
    }

    // 내 개인 지출만
    public List<Expense> getMyExpenses(Long routeId, String email) {
        User user = userRepository.findByEmail(email);
        TravelRoute route = checkAccess(routeId, user);
        return expenseRepository.findByTravelRouteAndUserOrderByExpenseDateAsc(route, user);
    }

    // 공동 경비 전체
    public List<Expense> getSharedExpenses(Long routeId, String email) {
        User user = userRepository.findByEmail(email);
        TravelRoute route = checkAccess(routeId, user);
        return expenseRepository.findByTravelRouteAndSharedTrueOrderByExpenseDateAsc(route);
    }

    public void updateExpense(Long expenseId, String email, String title,
                                Integer amount, String category, LocalDate expenseDate, boolean shared) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 지출입니다."));
        User user = userRepository.findByEmail(email);
        if (!expense.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("본인이 등록한 지출만 수정할 수 있습니다.");
        }

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("항목을 입력해주세요.");
        }
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("금액은 0보다 커야 해요.");
        }

        expense.updateInfo(title, amount, category, expenseDate, shared);
        expenseRepository.save(expense);
    }

    // 공동 경비 총액
    public int getSharedTotal(Long routeId, String email) {
    return getSharedExpenses(routeId, email).stream().mapToInt(Expense::getAmount).sum();
}

    public void deleteExpense(Long expenseId, String email) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 지출입니다."));
        User user = userRepository.findByEmail(email);
        if (!expense.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("본인이 등록한 지출만 삭제할 수 있습니다.");
        }
        expenseRepository.delete(expense);
    }

    public int getMyTotal(Long routeId, String email) {
        return getMyExpenses(routeId, email).stream().mapToInt(Expense::getAmount).sum();
    }

    // 공동 경비 항목별 정산 정보
    public List<Map<String, Object>> getSettlementDetails(Long routeId, String email) {
        User requester = userRepository.findByEmail(email);
        TravelRoute route = checkAccess(routeId, requester);
        List<Expense> sharedExpenses = expenseRepository.findByTravelRouteAndSharedTrueOrderByExpenseDateAsc(route);

        List<User> participants = new ArrayList<>();
        participants.add(route.getUser());
        routeCollaboratorRepository.findByTravelRoute(route)
                .forEach(c -> participants.add(c.getUser()));
        int participantCount = participants.size();

        List<Map<String, Object>> result = new ArrayList<>();

        for (Expense expense : sharedExpenses) {
            User payer = expense.getUser();
            int amount = expense.getAmount();
            int perPerson = participantCount > 0 ? amount / participantCount : 0;

            List<SettlementCheck> checks = settlementCheckRepository.findByExpense(expense);
            Set<Long> checkedUserIds = checks.stream().map(c -> c.getUser().getId()).collect(Collectors.toSet());

            List<Map<String, Object>> payers = new ArrayList<>();
            for (User p : participants) {
                if (p.getId().equals(payer.getId())) continue;
                Map<String, Object> payerMap = new HashMap<>();
                payerMap.put("userId", p.getId());
                payerMap.put("nickname", p.getNickname());
                payerMap.put("amount", perPerson);
                payerMap.put("checked", checkedUserIds.contains(p.getId()));
                payers.add(payerMap);
            }

            boolean isPayer = payer.getId().equals(requester.getId());

            Map<String, Object> map = new HashMap<>();
            map.put("expenseId", expense.getId());
            map.put("title", expense.getTitle());
            map.put("category", expense.getCategory());
            map.put("totalAmount", amount);
            map.put("perPerson", perPerson);
            map.put("payerNickname", payer.getNickname());
            map.put("payerUserId", payer.getId());
            map.put("isPayer", isPayer);
            map.put("payers", payers);
            result.add(map);
        }

        return result;
    }

    public void toggleSettlementCheck(Long expenseId, Long targetUserId, String email) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 지출입니다."));
        User requester = userRepository.findByEmail(email);
        checkAccess(expense.getTravelRoute().getId(), requester);

        if (!expense.getUser().getId().equals(requester.getId())) {
            throw new IllegalArgumentException("결제자만 정산 여부를 체크할 수 있습니다.");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        try {
            Optional<SettlementCheck> existing = settlementCheckRepository.findByExpenseAndUser(expense, targetUser);
            if (existing.isPresent()) {
                settlementCheckRepository.delete(existing.get());
            } else {
                settlementCheckRepository.save(SettlementCheck.builder()
                        .expense(expense)
                        .user(targetUser)
                        .build());
            }
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // 동시 요청 시 무시 (이미 반영됨)
        }
    }

        public int getTotalByRoute(Long routeId, String email) {
        User user = userRepository.findByEmail(email);
        TravelRoute route = checkAccess(routeId, user);
        List<Expense> myExpenses = expenseRepository.findByTravelRouteAndUserOrderByExpenseDateAsc(route, user);
        List<Expense> sharedExpenses = expenseRepository.findByTravelRouteAndSharedTrueOrderByExpenseDateAsc(route);
        
        int myTotal = myExpenses.stream().mapToInt(Expense::getAmount).sum();
        int sharedTotal = sharedExpenses.stream().mapToInt(Expense::getAmount).sum();
        return myTotal + sharedTotal;
    }

    public Expense getExpenseById(Long expenseId) {
        return expenseRepository.findById(expenseId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 지출입니다."));
    }

}