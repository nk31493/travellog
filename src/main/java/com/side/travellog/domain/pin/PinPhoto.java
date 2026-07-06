package com.side.travellog.domain.pin;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PinPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pin_id")
    private TravelPin travelPin;

    private String fileName;
    private String filePath;
    private Integer orderNum;
    private boolean representative;

    @Builder
    public PinPhoto(TravelPin travelPin, String fileName, String filePath, Integer orderNum) {
        this.travelPin = travelPin;
        this.fileName = fileName;
        this.filePath = filePath;
        this.orderNum = orderNum;
        this.representative = false;
    }

    public void setRepresentative(boolean representative) {
        this.representative = representative;
    }
}