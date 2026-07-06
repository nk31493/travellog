package com.side.travellog.dto;

import com.side.travellog.domain.pin.TravelPin;
import lombok.Getter;

@Getter
public class PinResponseDto {

    private Long id;
    private String title;
    private Double latitude;
    private Double longitude;
    private String visitDate;
    private String endDate;
    private String thumbnailUrl;

    public PinResponseDto(TravelPin pin) {
        this.id = pin.getId();
        this.title = pin.getTitle();
        this.latitude = pin.getLatitude();
        this.longitude = pin.getLongitude();
        this.visitDate = pin.getVisitDate().toString();
        this.endDate = pin.getEndDate() != null ? pin.getEndDate().toString() : null;
    }
}