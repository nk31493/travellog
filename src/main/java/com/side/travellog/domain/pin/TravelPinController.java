package com.side.travellog.domain.pin;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pins")
public class TravelPinController {

    private final PinPhotoRepository pinPhotoRepository;
    private final TravelPinService travelPinService;
    private final PinCommentService pinCommentService;
    private final com.side.travellog.config.TripUpdateService tripUpdateService;
    private final PinPhotoService pinPhotoService;
    private final TravelPinRepository travelPinRepository;
    private final com.side.travellog.domain.notification.NotificationService notificationService;
    private final com.side.travellog.domain.route.TravelRouteService travelRouteService;

        @PostMapping
        public Map<String, Object> createPin(@AuthenticationPrincipal UserDetails userDetails,
                                @RequestBody Map<String, String> body) {
        String email = userDetails.getUsername();
        Long routeId = Long.parseLong(body.get("routeId"));
        String title = body.get("title");
        Double latitude = Double.parseDouble(body.get("latitude"));
        Double longitude = Double.parseDouble(body.get("longitude"));
        LocalDate visitDate = LocalDate.parse(body.get("visitDate"));
        LocalDate endDate = body.get("endDate") != null && !body.get("endDate").isEmpty()
                ? LocalDate.parse(body.get("endDate")) : visitDate;
        LocalTime visitTime = body.get("visitTime") != null && !body.get("visitTime").isEmpty()
                ? LocalTime.parse(body.get("visitTime")) : null;
        String memo = body.get("memo");

        TravelPin pin = travelPinService.createPin(email, routeId, title, latitude, longitude,
                visitDate, endDate, visitTime, memo);
        tripUpdateService.notifyPinChanged(routeId);

        // 협업자들에게 알림
        com.side.travellog.domain.route.TravelRoute route = pin.getTravelRoute();
        com.side.travellog.domain.user.User creator = pin.getUser();
        travelRouteService.getCollaborators(routeId, email).forEach(collaborator -> {
                if (!collaborator.getId().equals(creator.getId())) {
                notificationService.send(
                        collaborator,
                        creator.getNickname() + "님이 '" + route.getName() + "'에 " + pin.getTitle() + "을 추가했어요!",
                        "/trips/" + route.getId()
                );
                }
        });
        // 소유자에게도 알림 (본인이 추가한 게 아닌 경우)
        if (!route.getUser().getId().equals(creator.getId())) {
                notificationService.send(
                route.getUser(),
                creator.getNickname() + "님이 '" + route.getName() + "'에 " + pin.getTitle() + "을 추가했어요!",
                "/trips/" + route.getId()
                );
        }

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("pinId", pin.getId());
        result.put("title", pin.getTitle());
        result.put("latitude", pin.getLatitude());
        result.put("longitude", pin.getLongitude());
        result.put("visitDate", pin.getVisitDate().toString());
        result.put("visitTime", pin.getVisitTime() != null ? pin.getVisitTime().toString() : null);
        result.put("memo", pin.getMemo());
        return result;
        }

        @PutMapping("/{pinId}/memo")
        public ResponseEntity<?> updateMemo(@AuthenticationPrincipal UserDetails userDetails,
                                        @PathVariable Long pinId,
                                        @RequestBody Map<String, String> body) {
        TravelPin pin = travelPinService.getPinById(pinId, userDetails.getUsername());
        Long routeId = pin.getTravelRoute().getId();
        travelPinService.updateMemo(pinId, userDetails.getUsername(), body.get("memo"));
        tripUpdateService.notifyPinChanged(routeId);
        return ResponseEntity.ok().build();
        }

        @PutMapping("/{pinId}/dates")
        public ResponseEntity<?> updateDates(@AuthenticationPrincipal UserDetails userDetails,
                                        @PathVariable Long pinId,
                                        @RequestBody Map<String, String> body) {
        LocalDate visitDate = LocalDate.parse(body.get("visitDate"));
        LocalDate endDate = body.get("endDate") != null && !body.get("endDate").isEmpty()
                ? LocalDate.parse(body.get("endDate")) : visitDate;
        LocalTime visitTime = body.get("visitTime") != null && !body.get("visitTime").isEmpty()
                ? LocalTime.parse(body.get("visitTime")) : null;
        TravelPin pin = travelPinService.getPinById(pinId, userDetails.getUsername());
        Long routeId = pin.getTravelRoute().getId();
        travelPinService.updateDates(pinId, userDetails.getUsername(), visitDate, endDate, visitTime);
        tripUpdateService.notifyPinChanged(routeId);
        return ResponseEntity.ok().build();
        }

        @PutMapping("/{pinId}/title")
        public ResponseEntity<?> updateTitle(@AuthenticationPrincipal UserDetails userDetails,
                                        @PathVariable Long pinId,
                                        @RequestBody Map<String, String> body) {
        TravelPin pin = travelPinService.getPinById(pinId, userDetails.getUsername());
        Long routeId = pin.getTravelRoute().getId();
        travelPinService.updateTitle(pinId, userDetails.getUsername(), body.get("title"));
        tripUpdateService.notifyPinChanged(routeId);
        return ResponseEntity.ok().build();
        }

        @PutMapping("/order")
        public ResponseEntity<?> updateOrder(@AuthenticationPrincipal UserDetails userDetails,
                                        @RequestBody Map<String, Object> body) {
        Long routeId = Long.parseLong(body.get("routeId").toString());
        List<Long> pinIds = ((List<?>) body.get("pinIds"))
                .stream().map(id -> Long.parseLong(id.toString())).toList();
        travelPinService.updateOrder(routeId, pinIds, userDetails.getUsername());
        tripUpdateService.notifyPinChanged(routeId);
        return ResponseEntity.ok().build();
        }

        @DeleteMapping("/{pinId}")
        public ResponseEntity<?> deletePin(@AuthenticationPrincipal UserDetails userDetails,
                                        @PathVariable Long pinId) {
        TravelPin pin = travelPinService.getPinById(pinId, userDetails.getUsername());
        Long routeId = pin.getTravelRoute().getId();
        travelPinService.deletePin(pinId, userDetails.getUsername());
        tripUpdateService.notifyPinChanged(routeId);
        return ResponseEntity.ok().build();
        }

        @PostMapping("/{pinId}/comments")
        public java.util.Map<String, Object> addComment(@AuthenticationPrincipal UserDetails userDetails,
                                                        @PathVariable Long pinId,
                                                        @RequestBody Map<String, String> body) {
        PinComment comment = pinCommentService.addComment(pinId, userDetails.getUsername(), body.get("content"));
        TravelPin pin = travelPinService.getPinById(pinId, userDetails.getUsername());
        tripUpdateService.notifyCommentChanged(pin.getTravelRoute().getId());

        // 핀 소유자와 협업자들에게 알림 (본인 제외)
        com.side.travellog.domain.route.TravelRoute route = pin.getTravelRoute();
        com.side.travellog.domain.user.User commenter = comment.getUser();
        String msg = commenter.getNickname() + "님이 '" + pin.getTitle() + "'에 댓글을 남겼어요!";
        String link = "/pin/" + pin.getId();

        // 소유자에게 알림
        if (!route.getUser().getId().equals(commenter.getId())) {
                notificationService.send(route.getUser(), msg, link);
        }
        // 협업자들에게 알림
        travelRouteService.getCollaborators(route.getId(), userDetails.getUsername()).forEach(collaborator -> {
                if (!collaborator.getId().equals(commenter.getId())) {
                notificationService.send(collaborator, msg, link);
                }
        });

        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("commentId", comment.getId());
        result.put("nickname", comment.getUser().getNickname());
        result.put("content", comment.getContent());
        result.put("createdAt", comment.getCreatedAt().toString());
        return result;
        }

        @GetMapping("/{pinId}/comments")
        public List<java.util.Map<String, Object>> getComments(@AuthenticationPrincipal UserDetails userDetails,
                                                                @PathVariable Long pinId) {
        String email = userDetails.getUsername();
        return pinCommentService.getComments(pinId, email)
                .stream().map(c -> {
                        java.util.Map<String, Object> map = new java.util.HashMap<>();
                        map.put("commentId", c.getId());
                        map.put("nickname", c.getUser().getNickname());
                        map.put("content", c.getContent());
                        map.put("createdAt", c.getCreatedAt().toString());
                        map.put("isMine", c.getUser().getEmail().equals(email));
                        return map;
                }).toList();
        }

        @DeleteMapping("/comments/{commentId}")
        public ResponseEntity<?> deleteComment(@AuthenticationPrincipal UserDetails userDetails,
                                                @PathVariable Long commentId) {
        Long routeId = pinCommentService.getRouteIdByCommentId(commentId);
        pinCommentService.deleteComment(commentId, userDetails.getUsername());
        tripUpdateService.notifyCommentChanged(routeId);
        return ResponseEntity.ok().build();
        }

        @PostMapping("/{pinId}/photos")
        public Map<String, Object> uploadPhoto(@AuthenticationPrincipal UserDetails userDetails,
                                                @PathVariable Long pinId,
                                                @RequestParam("file") MultipartFile file) throws java.io.IOException {
        PinPhoto photo = pinPhotoService.uploadPhoto(pinId, userDetails.getUsername(), file);
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("photoId", photo.getId());
        result.put("filePath", photo.getFilePath());
        return result;
        }

        @GetMapping("/{pinId}/photos")
        public List<Map<String, Object>> getPhotos(@AuthenticationPrincipal UserDetails userDetails,
                                                @PathVariable Long pinId) {
        return pinPhotoService.getPhotos(pinId, userDetails.getUsername())
                .stream().map(p -> {
                        Map<String, Object> map = new java.util.HashMap<>();
                        map.put("photoId", p.getId());
                        map.put("filePath", p.getFilePath());
                        map.put("fileName", p.getFileName());
                        map.put("representative", p.isRepresentative());
                        return map;
                }).toList();
        }

        @DeleteMapping("/photos/{photoId}")
        public ResponseEntity<?> deletePhoto(@AuthenticationPrincipal UserDetails userDetails,
                                        @PathVariable Long photoId) {
        pinPhotoService.deletePhoto(photoId, userDetails.getUsername());
        return ResponseEntity.ok().build();
        }

        @PutMapping("/photos/{photoId}/representative")
        public ResponseEntity<?> setRepresentative(@AuthenticationPrincipal UserDetails userDetails,
                                                @PathVariable Long photoId) {
        pinPhotoService.setRepresentative(photoId, userDetails.getUsername());
        return ResponseEntity.ok().build();
        }

        @PutMapping("/{pinId}/rating")
        public ResponseEntity<?> updateRating(@AuthenticationPrincipal UserDetails userDetails,
                                        @PathVariable Long pinId,
                                        @RequestBody Map<String, Integer> body) {
        travelPinService.updateRating(pinId, userDetails.getUsername(), body.get("rating"));
        return ResponseEntity.ok().build();
        }

        @GetMapping("/{pinId}/photos/public")
        public List<Map<String, Object>> getPhotosPublic(@PathVariable Long pinId) {
        TravelPin pin = travelPinRepository.findById(pinId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 핀입니다."));
        return pinPhotoRepository.findByTravelPinOrderByOrderNumAsc(pin)
                .stream().map(p -> {
                        Map<String, Object> map = new java.util.HashMap<>();
                        map.put("photoId", p.getId());
                        map.put("filePath", p.getFilePath());
                        map.put("representative", p.isRepresentative());
                        return map;
                }).toList();
        }

        @PutMapping("/comments/{commentId}")
        public ResponseEntity<?> updateComment(@AuthenticationPrincipal UserDetails userDetails,
                                                @PathVariable Long commentId,
                                                @RequestBody Map<String, String> body) {
        pinCommentService.updateComment(commentId, userDetails.getUsername(), body.get("content"));
        return ResponseEntity.ok().build();
        }
}