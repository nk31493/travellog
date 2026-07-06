package com.side.travellog.domain.pin;

import com.side.travellog.domain.route.RouteCollaboratorRepository;
import com.side.travellog.domain.user.User;
import com.side.travellog.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PinCommentService {

    private final PinCommentRepository pinCommentRepository;
    private final TravelPinRepository travelPinRepository;
    private final UserRepository userRepository;
    private final RouteCollaboratorRepository routeCollaboratorRepository;

    private void checkAccess(TravelPin pin, User user) {
        boolean isOwner = pin.getTravelRoute().getUser().getId().equals(user.getId());
        boolean isCollaborator = routeCollaboratorRepository
                .existsByTravelRouteAndUser(pin.getTravelRoute(), user);
        if (!isOwner && !isCollaborator) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }
    }

    public PinComment addComment(Long pinId, String email, String content) {
        TravelPin pin = travelPinRepository.findById(pinId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 핀입니다."));
        User user = userRepository.findByEmail(email);
        checkAccess(pin, user);

        return pinCommentRepository.save(PinComment.builder()
                .travelPin(pin)
                .user(user)
                .content(content)
                .build());
    }

    public List<PinComment> getComments(Long pinId, String email) {
        TravelPin pin = travelPinRepository.findById(pinId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 핀입니다."));
        User user = userRepository.findByEmail(email);
        checkAccess(pin, user);
        return pinCommentRepository.findByTravelPinOrderByCreatedAtAsc(pin);
    }

    public void deleteComment(Long commentId, String email) {
        PinComment comment = pinCommentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글입니다."));
        User user = userRepository.findByEmail(email);
        if (!comment.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("본인 댓글만 삭제할 수 있습니다.");
        }
        pinCommentRepository.delete(comment);
    }

    public Long getRouteIdByCommentId(Long commentId) {
        PinComment comment = pinCommentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글입니다."));
        return comment.getTravelPin().getTravelRoute().getId();
    }

    public void updateComment(Long commentId, String email, String content) {
        PinComment comment = pinCommentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글입니다."));
        User user = userRepository.findByEmail(email);
        if (!comment.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("본인 댓글만 수정할 수 있습니다.");
        }
        comment.updateContent(content);
        pinCommentRepository.save(comment);
    }
}