package com.side.travellog.domain.pin;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PinPhotoRepository extends JpaRepository<PinPhoto, Long> {
    List<PinPhoto> findByTravelPinOrderByOrderNumAsc(TravelPin travelPin);
    int countByTravelPin(TravelPin travelPin);

    List<PinPhoto> findByTravelPinAndRepresentativeTrue(TravelPin travelPin);
    void deleteByTravelPin(TravelPin travelPin);
}