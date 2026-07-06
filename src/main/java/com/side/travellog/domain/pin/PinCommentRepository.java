package com.side.travellog.domain.pin;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PinCommentRepository extends JpaRepository<PinComment, Long> {
    List<PinComment> findByTravelPinOrderByCreatedAtAsc(TravelPin travelPin);
    void deleteByTravelPin(TravelPin travelPin);
}