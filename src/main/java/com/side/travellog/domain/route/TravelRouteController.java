package com.side.travellog.domain.route;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.side.travellog.domain.pin.TravelPin;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class TravelRouteController {

    private final TravelRouteService travelRouteService;

    private final com.side.travellog.domain.pin.TravelPinRepository travelPinRepository;

    private final ChecklistService checklistService;
    private final ExpenseService expenseService;
    private final com.side.travellog.config.TripUpdateService tripUpdateService;
    private final com.side.travellog.domain.notification.NotificationService notificationService;
    private final com.side.travellog.domain.user.UserRepository userRepository;
    private final TravelRouteLikeRepository likeRepository;
    private final TravelRouteRepository travelRouteRepository;
    private final ReservationService reservationService;

    @Value("${google.maps.key}")
    private String googleMapsKey;

    // 메인 페이지 - 내 여행 목록
    @GetMapping("/trips")
    public String tripList(@AuthenticationPrincipal UserDetails userDetails,
                            @RequestParam(required = false) String keyword,
                            Model model) {
        List<TravelRoute> allRoutes = (keyword != null && !keyword.isBlank())
                ? travelRouteService.searchRoutes(userDetails.getUsername(), keyword)
                : travelRouteService.getAllRoutesByUser(userDetails.getUsername());

        java.time.LocalDate today = java.time.LocalDate.now();

        // 진행 중 / 예정 여행
        List<TravelRoute> activeRoutes = allRoutes.stream()
                .filter(r -> r.getEndDate() == null || !r.getEndDate().isBefore(today))
                .toList();

        // 완료된 여행
        List<TravelRoute> doneRoutes = allRoutes.stream()
                .filter(r -> r.getEndDate() != null && r.getEndDate().isBefore(today))
                .toList();

        List<Long> collaboratingIds = travelRouteService
                .getCollaboratingRoutesByUser(userDetails.getUsername())
                .stream().map(TravelRoute::getId).toList();

        List<Long> hasCollaboratorIds = travelRouteService
                .getRoutesWithCollaborators(userDetails.getUsername());

        model.addAttribute("activeRoutes", activeRoutes);
        model.addAttribute("doneRoutes", doneRoutes);
        model.addAttribute("collaboratingIds", collaboratingIds);
        model.addAttribute("hasCollaboratorIds", hasCollaboratorIds);
        model.addAttribute("today", today);
        model.addAttribute("keyword", keyword);
        return "trip-list";
    }

    // 여행 상세 페이지
    @GetMapping("/trips/{id}")
    public String tripDetail(@PathVariable Long id,
                            @AuthenticationPrincipal UserDetails userDetails,
                            Model model) {
        TravelRoute route = travelRouteService.getRouteByIdWithAccess(id, userDetails.getUsername());
        boolean isOwner = route.getUser().getEmail().equals(userDetails.getUsername());
        model.addAttribute("route", route);
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("googleMapsKey", googleMapsKey);
        return "trip-detail";
    }

    // 여행 생성 API
    @PostMapping("/api/trips")
    @ResponseBody
    public TravelRoute createTrip(@AuthenticationPrincipal UserDetails userDetails,
                                   @RequestBody Map<String, String> body) {
        LocalDate startDate = LocalDate.parse(body.get("startDate"));
        LocalDate endDate = LocalDate.parse(body.get("endDate"));
        return travelRouteService.createRoute(
                userDetails.getUsername(),
                body.get("name"),
                body.get("destination"),
                startDate,
                endDate
        );
    }

    // 여행 삭제 API
    @DeleteMapping("/api/trips/{routeId}")
    @ResponseBody
    public ResponseEntity<?> deleteTrip(@PathVariable Long routeId,
                                         @AuthenticationPrincipal UserDetails userDetails) {
        travelRouteService.deleteRoute(routeId, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    // 핀 목록 API
    @GetMapping("/api/trips/{routeId}/pins")
    @ResponseBody
    public List<Map<String, Object>> getTripPins(@PathVariable Long routeId,
                                                @AuthenticationPrincipal UserDetails userDetails) {
        TravelRoute route = travelRouteService.getRouteByIdWithAccess(routeId, userDetails.getUsername());
        List<TravelPin> pins = travelPinRepository.findByTravelRouteOrdered(route);
        return pins.stream().map(pin -> {
            Map<String, Object> map = new HashMap<>();
            map.put("pinId", pin.getId());
            map.put("title", pin.getTitle());
            map.put("latitude", pin.getLatitude());
            map.put("longitude", pin.getLongitude());
            map.put("visitDate", pin.getVisitDate().toString());
            map.put("endDate", pin.getEndDate() != null ? pin.getEndDate().toString() : null);
            map.put("visitTime", pin.getVisitTime() != null ? pin.getVisitTime().toString() : null);
            map.put("memo", pin.getMemo());
            map.put("rating", pin.getRating());
            return map;
        }).toList();
    }

    @PutMapping("/api/trips/{routeId}")
    @ResponseBody
    public ResponseEntity<?> updateTrip(@PathVariable Long routeId,
                                        @AuthenticationPrincipal UserDetails userDetails,
                                        @RequestBody Map<String, String> body) {
        LocalDate startDate = body.get("startDate") != null && !body.get("startDate").isEmpty()
                ? LocalDate.parse(body.get("startDate")) : null;
        LocalDate endDate = body.get("endDate") != null && !body.get("endDate").isEmpty()
                ? LocalDate.parse(body.get("endDate")) : null;
        travelRouteService.updateRoute(routeId, userDetails.getUsername(),
                body.get("name"), body.get("destination"), startDate, endDate);
        return ResponseEntity.ok().build();
    }

    @Value("${weather.api.key}")
    private String weatherApiKey;

    @GetMapping("/api/trips/{routeId}/weather")
    @ResponseBody
    public ResponseEntity<?> getWeather(@PathVariable Long routeId,
                                        @AuthenticationPrincipal UserDetails userDetails) {
        TravelRoute route = travelRouteService.getRouteByIdWithAccess(routeId, userDetails.getUsername());
        String destination = route.getDestination().split(",")[0].trim();

        String englishCity = toEnglishCity(destination);
        System.out.println("날씨 검색 도시: " + destination + " -> " + englishCity);

        try {
            String url = "https://api.openweathermap.org/data/2.5/weather?q="
                    + java.net.URLEncoder.encode(englishCity, "UTF-8")
                    + "&appid=" + weatherApiKey
                    + "&units=metric&lang=kr";

            System.out.println("날씨 API 호출: " + url);

            java.net.URL apiUrl = new java.net.URL(url);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) apiUrl.openConnection();
            conn.setRequestMethod("GET");

            int responseCode = conn.getResponseCode();
            System.out.println("날씨 응답 코드: " + responseCode);

            java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(
                        responseCode == 200 ? conn.getInputStream() : conn.getErrorStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();

            System.out.println("날씨 응답: " + sb.toString());
            return ResponseEntity.ok(sb.toString());
        } catch (Exception e) {
            System.out.println("날씨 오류: " + e.getMessage());
            return ResponseEntity.ok("{}");
        }
    }

    // 공유 토큰 생성
    @PostMapping("/api/trips/{routeId}/share")
    @ResponseBody
    public Map<String, String> generateShare(@PathVariable Long routeId,
                                            @AuthenticationPrincipal UserDetails userDetails,
                                            jakarta.servlet.http.HttpServletRequest request) {
        String token = travelRouteService.generateShareToken(routeId, userDetails.getUsername());
        String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
        return Map.of("shareUrl", baseUrl + "/share/" + token);
    }

    // 공유 페이지
    @GetMapping("/share/{token}")
    public String sharePage(@PathVariable String token, Model model) {
        TravelRoute route = travelRouteService.getRouteByShareToken(token);
        model.addAttribute("route", route);
        model.addAttribute("shareToken", token);
        model.addAttribute("googleMapsKey", googleMapsKey);
        return "trip-share";
    }

    // 공유 페이지 핀 목록 API
    @GetMapping("/api/share/{token}/pins")
    @ResponseBody
    public List<Map<String, Object>> getSharedPins(@PathVariable String token) {
        TravelRoute route = travelRouteService.getRouteByShareToken(token);
        List<TravelPin> pins = travelPinRepository.findByTravelRouteOrdered(route);
        return pins.stream().map(pin -> {
            Map<String, Object> map = new HashMap<>();
            map.put("pinId", pin.getId());
            map.put("title", pin.getTitle());
            map.put("latitude", pin.getLatitude());
            map.put("longitude", pin.getLongitude());
            map.put("visitDate", pin.getVisitDate().toString());
            map.put("visitTime", pin.getVisitTime() != null ? pin.getVisitTime().toString() : null);
            map.put("memo", pin.getMemo());
            return map;
        }).toList();
    }

    @GetMapping("/api/trips/{routeId}/forecast")
    @ResponseBody
    public ResponseEntity<?> getForecast(@PathVariable Long routeId,
                                        @AuthenticationPrincipal UserDetails userDetails) {
        TravelRoute route = travelRouteService.getRouteByIdWithAccess(routeId, userDetails.getUsername());
        String destination = route.getDestination().split(",")[0].trim();

        String englishCity = toEnglishCity(destination);

        try {
            String url = "https://api.openweathermap.org/data/2.5/forecast?q="
                    + java.net.URLEncoder.encode(englishCity, "UTF-8")
                    + "&appid=" + weatherApiKey
                    + "&units=metric&lang=kr&cnt=40";

            java.net.URL apiUrl = new java.net.URL(url);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) apiUrl.openConnection();
            conn.setRequestMethod("GET");

            int responseCode = conn.getResponseCode();
            java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(
                        responseCode == 200 ? conn.getInputStream() : conn.getErrorStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();

            return ResponseEntity.ok(sb.toString());
        } catch (Exception e) {
            return ResponseEntity.ok("{}");
        }
    }

    @PostMapping("/api/trips/{routeId}/leave")
    @ResponseBody
    public ResponseEntity<?> leaveCollaboration(@PathVariable Long routeId,
                                                @AuthenticationPrincipal UserDetails userDetails) {
        com.side.travellog.domain.user.User leaver =
                userRepository.findByEmail(userDetails.getUsername());
        com.side.travellog.domain.route.TravelRoute route =
                travelRouteService.getRouteByIdWithAccess(routeId, userDetails.getUsername());

        travelRouteService.leaveCollaboration(routeId, userDetails.getUsername());

        // 소유자에게 알림
        notificationService.send(
                route.getUser(),
                leaver.getNickname() + "님이 '" + route.getName() + "' 여행에서 나갔어요.",
                "/trips/" + routeId
        );

        return ResponseEntity.ok().build();
    }

    private String toEnglishCity(String koreanCity) {
        java.util.Map<String, String> cityMap = new java.util.HashMap<>();
        // 한국
        cityMap.put("서울", "Seoul"); cityMap.put("부산", "Busan");
        cityMap.put("제주", "Jeju"); cityMap.put("인천", "Incheon");
        cityMap.put("대구", "Daegu"); cityMap.put("대전", "Daejeon");
        cityMap.put("광주", "Gwangju"); cityMap.put("수원", "Suwon");
        cityMap.put("경주", "Gyeongju"); cityMap.put("전주", "Jeonju");
        // 일본
        cityMap.put("도쿄", "Tokyo"); cityMap.put("오사카", "Osaka");
        cityMap.put("교토", "Kyoto"); cityMap.put("후쿠오카", "Fukuoka");
        cityMap.put("삿포로", "Sapporo"); cityMap.put("나고야", "Nagoya");
        cityMap.put("나라", "Nara"); cityMap.put("고베", "Kobe");
        cityMap.put("요코하마", "Yokohama"); cityMap.put("히로시마", "Hiroshima");
        cityMap.put("오키나와", "Okinawa"); cityMap.put("가고시마", "Kagoshima");
        // 중국
        cityMap.put("베이징", "Beijing"); cityMap.put("상하이", "Shanghai");
        cityMap.put("광저우", "Guangzhou"); cityMap.put("홍콩", "Hong Kong");
        cityMap.put("마카오", "Macao"); cityMap.put("청두", "Chengdu");
        cityMap.put("시안", "Xian"); cityMap.put("장가계", "Zhangjiajie");
        // 동남아
        cityMap.put("방콕", "Bangkok"); cityMap.put("파타야", "Pattaya");
        cityMap.put("치앙마이", "Chiang Mai"); cityMap.put("푸켓", "Phuket");
        cityMap.put("싱가포르", "Singapore"); cityMap.put("쿠알라룸푸르", "Kuala Lumpur");
        cityMap.put("발리", "Bali"); cityMap.put("자카르타", "Jakarta");
        cityMap.put("마닐라", "Manila"); cityMap.put("세부", "Cebu");
        cityMap.put("하노이", "Hanoi"); cityMap.put("호치민", "Ho Chi Minh City");
        cityMap.put("다낭", "Da Nang"); cityMap.put("나트랑", "Nha Trang");
        cityMap.put("양곤", "Yangon"); cityMap.put("프놈펜", "Phnom Penh");
        cityMap.put("시엠립", "Siem Reap"); cityMap.put("비엔티안", "Vientiane");
        cityMap.put("콸라룸푸르", "Kuala Lumpur"); cityMap.put("조호바루", "Johor Bahru");
        // 유럽
        cityMap.put("파리", "Paris"); cityMap.put("런던", "London");
        cityMap.put("로마", "Rome"); cityMap.put("밀라노", "Milan");
        cityMap.put("베네치아", "Venice"); cityMap.put("피렌체", "Florence");
        cityMap.put("바르셀로나", "Barcelona"); cityMap.put("마드리드", "Madrid");
        cityMap.put("베를린", "Berlin"); cityMap.put("뮌헨", "Munich");
        cityMap.put("프랑크푸르트", "Frankfurt"); cityMap.put("함부르크", "Hamburg");
        cityMap.put("암스테르담", "Amsterdam"); cityMap.put("브뤼셀", "Brussels");
        cityMap.put("빈", "Vienna"); cityMap.put("프라하", "Prague");
        cityMap.put("부다페스트", "Budapest"); cityMap.put("바르샤바", "Warsaw");
        cityMap.put("취리히", "Zurich"); cityMap.put("제네바", "Geneva");
        cityMap.put("리스본", "Lisbon"); cityMap.put("포르투", "Porto");
        cityMap.put("아테네", "Athens"); cityMap.put("이스탄불", "Istanbul");
        cityMap.put("스톡홀름", "Stockholm"); cityMap.put("코펜하겐", "Copenhagen");
        cityMap.put("오슬로", "Oslo"); cityMap.put("헬싱키", "Helsinki");
        cityMap.put("더블린", "Dublin"); cityMap.put("에든버러", "Edinburgh");
        cityMap.put("크라쿠프", "Krakow"); cityMap.put("두브로브니크", "Dubrovnik");
        cityMap.put("자그레브", "Zagreb"); cityMap.put("류블랴나", "Ljubljana");
        cityMap.put("브라티슬라바", "Bratislava"); cityMap.put("탈린", "Tallinn");
        cityMap.put("리가", "Riga"); cityMap.put("빌뉴스", "Vilnius");
        // 미주
        cityMap.put("뉴욕", "New York"); cityMap.put("로스앤젤레스", "Los Angeles");
        cityMap.put("샌프란시스코", "San Francisco"); cityMap.put("라스베가스", "Las Vegas");
        cityMap.put("시카고", "Chicago"); cityMap.put("마이애미", "Miami");
        cityMap.put("시애틀", "Seattle"); cityMap.put("보스턴", "Boston");
        cityMap.put("워싱턴", "Washington"); cityMap.put("올랜도", "Orlando");
        cityMap.put("하와이", "Honolulu"); cityMap.put("괌", "Guam");
        cityMap.put("토론토", "Toronto"); cityMap.put("밴쿠버", "Vancouver");
        cityMap.put("몬트리올", "Montreal"); cityMap.put("캘거리", "Calgary");
        cityMap.put("멕시코시티", "Mexico City"); cityMap.put("칸쿤", "Cancun");
        cityMap.put("상파울루", "Sao Paulo"); cityMap.put("리우데자네이루", "Rio de Janeiro");
        cityMap.put("부에노스아이레스", "Buenos Aires"); cityMap.put("리마", "Lima");
        cityMap.put("보고타", "Bogota"); cityMap.put("산티아고", "Santiago");
        // 중동/아프리카
        cityMap.put("두바이", "Dubai"); cityMap.put("아부다비", "Abu Dhabi");
        cityMap.put("도하", "Doha"); cityMap.put("리야드", "Riyadh");
        cityMap.put("텔아비브", "Tel Aviv"); cityMap.put("예루살렘", "Jerusalem");
        cityMap.put("카이로", "Cairo"); cityMap.put("마라케시", "Marrakesh");
        cityMap.put("나이로비", "Nairobi"); cityMap.put("요하네스버그", "Johannesburg");
        cityMap.put("케이프타운", "Cape Town"); cityMap.put("카사블랑카", "Casablanca");
        // 오세아니아
        cityMap.put("시드니", "Sydney"); cityMap.put("멜버른", "Melbourne");
        cityMap.put("브리즈번", "Brisbane"); cityMap.put("퍼스", "Perth");
        cityMap.put("오클랜드", "Auckland"); cityMap.put("크라이스트처치", "Christchurch");
        // 인도/중앙아시아
        cityMap.put("뭄바이", "Mumbai"); cityMap.put("뉴델리", "New Delhi");
        cityMap.put("방갈로르", "Bangalore"); cityMap.put("콜카타", "Kolkata");
        cityMap.put("알마티", "Almaty"); cityMap.put("타슈켄트", "Tashkent");

        return cityMap.getOrDefault(koreanCity, koreanCity);
    }

    // 초대 링크 생성 (소유자만)
    @PostMapping("/api/trips/{routeId}/invite")
    @ResponseBody
    public Map<String, String> generateInvite(@PathVariable Long routeId,
                                            @AuthenticationPrincipal UserDetails userDetails,
                                            jakarta.servlet.http.HttpServletRequest request) {
        String token = travelRouteService.generateInviteToken(routeId, userDetails.getUsername());
        String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
        return Map.of("inviteUrl", baseUrl + "/invite/" + token);
    }

    @GetMapping("/invite/{token}")
    public String joinByInviteToken(@PathVariable String token,
                                    @AuthenticationPrincipal UserDetails userDetails) {
        com.side.travellog.domain.route.TravelRoute route =
                travelRouteService.joinByInviteToken(token, userDetails.getUsername());

        // 소유자에게 알림 발송
        com.side.travellog.domain.user.User joiner =
                userRepository.findByEmail(userDetails.getUsername());
        notificationService.send(
                route.getUser(),
                joiner.getNickname() + "님이 '" + route.getName() + "' 여행에 참여했어요!",
                "/trips/" + route.getId()
        );

        return "redirect:/trips/" + route.getId();
    }

    // 협업자 목록 조회
    @GetMapping("/api/trips/{routeId}/collaborators")
    @ResponseBody
    public List<Map<String, Object>> getCollaborators(@PathVariable Long routeId,
                                                    @AuthenticationPrincipal UserDetails userDetails) {
        List<com.side.travellog.domain.user.User> collaborators =
                travelRouteService.getCollaborators(routeId, userDetails.getUsername());
        return collaborators.stream().map(u -> {
            Map<String, Object> map = new HashMap<>();
            map.put("userId", u.getId());
            map.put("nickname", u.getNickname());
            map.put("email", u.getEmail());
            return map;
        }).toList();
    }

    // 협업자 내쫓기
    @DeleteMapping("/api/trips/{routeId}/collaborators/{userId}")
    @ResponseBody
    public ResponseEntity<?> removeCollaborator(@PathVariable Long routeId,
                                                @PathVariable Long userId,
                                                @AuthenticationPrincipal UserDetails userDetails) {
        travelRouteService.removeCollaborator(routeId, userId, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/trips/{routeId}/checklist")
    @ResponseBody
    public Map<String, Object> addChecklistItem(@PathVariable Long routeId,
                                                @AuthenticationPrincipal UserDetails userDetails,
                                                @RequestBody Map<String, String> body) {
        com.side.travellog.domain.route.ChecklistItem item =
                checklistService.addItem(routeId, userDetails.getUsername(), body.get("content"));
        tripUpdateService.notifyChecklistChanged(routeId);
        Map<String, Object> result = new HashMap<>();
        result.put("itemId", item.getId());
        result.put("content", item.getContent());
        result.put("checked", item.isChecked());
        return result;
    }

    @GetMapping("/api/trips/{routeId}/checklist")
    @ResponseBody
    public List<Map<String, Object>> getChecklist(@PathVariable Long routeId,
                                                @AuthenticationPrincipal UserDetails userDetails) {
        return checklistService.getItems(routeId, userDetails.getUsername())
                .stream().map(item -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("itemId", item.getId());
                    map.put("content", item.getContent());
                    map.put("checked", item.isChecked());
                    return map;
                }).toList();
    }

    @PutMapping("/api/checklist/{itemId}/toggle")
    @ResponseBody
    public ResponseEntity<?> toggleChecklistItem(@PathVariable Long itemId,
                                                @AuthenticationPrincipal UserDetails userDetails) {
        checklistService.toggleItem(itemId, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/api/checklist/{itemId}")
    @ResponseBody
    public ResponseEntity<?> deleteChecklistItem(@PathVariable Long itemId,
                                                @AuthenticationPrincipal UserDetails userDetails) {
        checklistService.deleteItem(itemId, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/trips/{routeId}/expenses")
    @ResponseBody
    public Map<String, Object> addExpense(@PathVariable Long routeId,
                                        @AuthenticationPrincipal UserDetails userDetails,
                                        @RequestBody Map<String, String> body) {
        boolean shared = "true".equals(body.get("shared"));
        com.side.travellog.domain.route.Expense expense = expenseService.addExpense(
                routeId, userDetails.getUsername(),
                body.get("title"),
                Integer.parseInt(body.get("amount")),
                body.get("category"),
                java.time.LocalDate.parse(body.get("expenseDate")),
                shared
        );
        if (shared) {
            tripUpdateService.notifyExpenseChanged(routeId);
        }
        return toExpenseMap(expense);
    }

    @GetMapping("/api/trips/{routeId}/expenses/mine")
    @ResponseBody
    public List<Map<String, Object>> getMyExpenses(@PathVariable Long routeId,
                                                    @AuthenticationPrincipal UserDetails userDetails) {
        return expenseService.getMyExpenses(routeId, userDetails.getUsername())
                .stream().map(this::toExpenseMap).toList();
    }

    @GetMapping("/api/trips/{routeId}/expenses/shared")
    @ResponseBody
    public List<Map<String, Object>> getSharedExpenses(@PathVariable Long routeId,
                                                        @AuthenticationPrincipal UserDetails userDetails) {
        return expenseService.getSharedExpenses(routeId, userDetails.getUsername())
                .stream().map(this::toExpenseMap).toList();
    }

    @PutMapping("/api/expenses/{expenseId}")
    @ResponseBody
    public ResponseEntity<?> updateExpense(@PathVariable Long expenseId,
                                            @AuthenticationPrincipal UserDetails userDetails,
                                            @RequestBody Map<String, String> body) {
        boolean shared = "true".equals(body.get("shared"));
        expenseService.updateExpense(expenseId, userDetails.getUsername(),
                body.get("title"),
                Integer.parseInt(body.get("amount")),
                body.get("category"),
                java.time.LocalDate.parse(body.get("expenseDate")),
                shared);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/api/expenses/{expenseId}")
    @ResponseBody
    public ResponseEntity<?> deleteExpense(@PathVariable Long expenseId,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        expenseService.deleteExpense(expenseId, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/trips/{routeId}/expenses/total")
    @ResponseBody
    public Map<String, Integer> getMyExpenseTotal(@PathVariable Long routeId,
                                                @AuthenticationPrincipal UserDetails userDetails) {
        return Map.of("total", expenseService.getMyTotal(routeId, userDetails.getUsername()));
    }

    private Map<String, Object> toExpenseMap(com.side.travellog.domain.route.Expense e) {
        Map<String, Object> map = new HashMap<>();
        map.put("expenseId", e.getId());
        map.put("title", e.getTitle());
        map.put("amount", e.getAmount());
        map.put("category", e.getCategory());
        map.put("expenseDate", e.getExpenseDate().toString());
        map.put("nickname", e.getUser().getNickname());
        map.put("shared", e.isShared());
        return map;
    }

    @GetMapping("/api/trips/{routeId}/settlement")
    @ResponseBody
    public List<Map<String, Object>> getSettlement(@PathVariable Long routeId,
                                                    @AuthenticationPrincipal UserDetails userDetails) {
        return expenseService.getSettlementDetails(routeId, userDetails.getUsername());
    }

    @PutMapping("/api/expenses/{expenseId}/settlement/{userId}/toggle")
    @ResponseBody
    public ResponseEntity<?> toggleSettlementCheck(@PathVariable Long expenseId,
                                                @PathVariable Long userId,
                                                @AuthenticationPrincipal UserDetails userDetails) {
        expenseService.toggleSettlementCheck(expenseId, userId, userDetails.getUsername());

        // 결제자에게 정산 알림
        com.side.travellog.domain.route.Expense expense = expenseService.getExpenseById(expenseId);
        com.side.travellog.domain.user.User settler = userRepository.findById(userId)
                .orElse(null);
        if (settler != null) {
            notificationService.send(
                    expense.getUser(),
                    settler.getNickname() + "님이 '" + expense.getTitle() + "' 정산을 완료했어요!",
                    "/trips/" + expense.getTravelRoute().getId()
            );
        }

        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/trips/{routeId}/expenses/shared/total")
    @ResponseBody
    public Map<String, Integer> getSharedExpenseTotal(@PathVariable Long routeId,
                                                    @AuthenticationPrincipal UserDetails userDetails) {
        return Map.of("total", expenseService.getSharedTotal(routeId, userDetails.getUsername()));
    }

    @PostMapping("/api/trips/{routeId}/checklist/default")
    @ResponseBody
    public ResponseEntity<?> fillDefaultChecklist(@PathVariable Long routeId,
                                                @AuthenticationPrincipal UserDetails userDetails) {
        checklistService.fillDefaultItemsIfEmpty(routeId, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/trips/{routeId}/invite/nickname")
    @ResponseBody
    public ResponseEntity<?> inviteByNickname(@PathVariable Long routeId,
                                            @AuthenticationPrincipal UserDetails userDetails,
                                            @RequestBody Map<String, String> body) {
        try {
            travelRouteService.inviteByNickname(routeId, body.get("nickname"), userDetails.getUsername());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/api/trips/{routeId}/public")
    @ResponseBody
    public ResponseEntity<?> updatePublic(@PathVariable Long routeId,
                                        @AuthenticationPrincipal UserDetails userDetails,
                                        @RequestBody Map<String, Boolean> body) {
        boolean isPublic = body.get("isPublic");
        travelRouteService.updatePublic(routeId, userDetails.getUsername(), isPublic);

        // 협업자들에게 알림
        TravelRoute route = travelRouteRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 여행입니다."));
        com.side.travellog.domain.user.User actor =
                userRepository.findByEmail(userDetails.getUsername());
        String statusText = isPublic ? "공개" : "비공개";
        String msg = actor.getNickname() + "님이 '" + route.getName() + "' 여행을 " + statusText + "로 전환했어요!";
        String link = "/trips/" + routeId;

        if (!route.getUser().getId().equals(actor.getId())) {
            notificationService.send(route.getUser(), msg, link);
        }
        travelRouteService.getCollaborators(routeId, userDetails.getUsername()).forEach(collaborator -> {
            if (!collaborator.getId().equals(actor.getId())) {
                notificationService.send(collaborator, msg, link);
            }
        });

        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/trips/{routeId}/like")
    @ResponseBody
    public ResponseEntity<?> toggleLike(@PathVariable Long routeId,
                                        @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body("로그인이 필요해요");
        }
        TravelRoute route = travelRouteRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 여행입니다."));
        com.side.travellog.domain.user.User user =
                userRepository.findByEmail(userDetails.getUsername());

        boolean liked;
        try {
            java.util.Optional<TravelRouteLike> existing = likeRepository.findByTravelRouteAndUser(route, user);
            if (existing.isPresent()) {
                likeRepository.delete(existing.get());
                liked = false;
            } else {
                likeRepository.save(TravelRouteLike.builder()
                        .travelRoute(route)
                        .user(user)
                        .build());
                liked = true;
            }
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // 동시 요청으로 이미 좋아요가 눌린 경우, 현재 상태를 그대로 반환
            liked = likeRepository.findByTravelRouteAndUser(route, user).isPresent();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("liked", liked);
        result.put("count", likeRepository.countByTravelRoute(route));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/api/trips/{routeId}/like/count")
    @ResponseBody
    public Map<String, Integer> getLikeCount(@PathVariable Long routeId) {
        TravelRoute route = travelRouteRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 여행입니다."));
        return Map.of("count", likeRepository.countByTravelRoute(route));
    }

    @PostMapping("/api/trips/{routeId}/copy")
    @ResponseBody
    public ResponseEntity<?> copyRoute(@PathVariable Long routeId,
                                        @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body("로그인이 필요해요");
        }
        TravelRoute copy = travelRouteService.copyRoute(routeId, userDetails.getUsername());
        Map<String, Object> result = new HashMap<>();
        result.put("routeId", copy.getId());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/api/trips/{routeId}/reservations")
    @ResponseBody
    public Map<String, Object> addReservation(@PathVariable Long routeId,
                                            @AuthenticationPrincipal UserDetails userDetails,
                                            @RequestBody Map<String, String> body) {
        java.time.LocalDateTime start = java.time.LocalDateTime.parse(body.get("startDateTime"));
        java.time.LocalDateTime end = body.get("endDateTime") != null && !body.get("endDateTime").isEmpty()
                ? java.time.LocalDateTime.parse(body.get("endDateTime")) : null;

        Reservation r = reservationService.addReservation(routeId, userDetails.getUsername(),
                body.get("type"), body.get("title"), body.get("confirmationNumber"),
                start, end, body.get("memo"));

        return toReservationMap(r);
    }

    @GetMapping("/api/trips/{routeId}/reservations")
    @ResponseBody
    public List<Map<String, Object>> getReservations(@PathVariable Long routeId,
                                                    @AuthenticationPrincipal UserDetails userDetails) {
        return reservationService.getReservations(routeId, userDetails.getUsername())
                .stream().map(this::toReservationMap).toList();
    }

    @PutMapping("/api/reservations/{reservationId}")
    @ResponseBody
    public ResponseEntity<?> updateReservation(@PathVariable Long reservationId,
                                                @AuthenticationPrincipal UserDetails userDetails,
                                                @RequestBody Map<String, String> body) {
        java.time.LocalDateTime start = java.time.LocalDateTime.parse(body.get("startDateTime"));
        java.time.LocalDateTime end = body.get("endDateTime") != null && !body.get("endDateTime").isEmpty()
                ? java.time.LocalDateTime.parse(body.get("endDateTime")) : null;

        reservationService.updateReservation(reservationId, userDetails.getUsername(),
                body.get("type"), body.get("title"), body.get("confirmationNumber"),
                start, end, body.get("memo"));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/api/reservations/{reservationId}")
    @ResponseBody
    public ResponseEntity<?> deleteReservation(@PathVariable Long reservationId,
                                                @AuthenticationPrincipal UserDetails userDetails) {
        reservationService.deleteReservation(reservationId, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    private Map<String, Object> toReservationMap(Reservation r) {
        Map<String, Object> map = new HashMap<>();
        map.put("reservationId", r.getId());
        map.put("type", r.getType());
        map.put("title", r.getTitle());
        map.put("confirmationNumber", r.getConfirmationNumber());
        map.put("startDateTime", r.getStartDateTime().toString());
        map.put("endDateTime", r.getEndDateTime() != null ? r.getEndDateTime().toString() : null);
        map.put("memo", r.getMemo());
        return map;
    }
    
}