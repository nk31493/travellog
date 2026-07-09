package com.side.travellog.domain.route;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    private TravelRoute travelRoute;

    private String type; // 항공, 숙소, 렌터카, 기타
    private String title; // 예: 대한항공 KE123
    private String confirmationNumber; // 예약번호
    private LocalDateTime startDateTime; // 출발/체크인
    private LocalDateTime endDateTime; // 도착/체크아웃
    private String memo;

    @Builder
    public Reservation(TravelRoute travelRoute, String type, String title,
                        String confirmationNumber, LocalDateTime startDateTime,
                        LocalDateTime endDateTime, String memo) {
        this.travelRoute = travelRoute;
        this.type = type;
        this.title = title;
        this.confirmationNumber = confirmationNumber;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.memo = memo;
    }

    public void updateInfo(String type, String title, String confirmationNumber,
                            LocalDateTime startDateTime, LocalDateTime endDateTime, String memo) {
        this.type = type;
        this.title = title;
        this.confirmationNumber = confirmationNumber;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.memo = memo;
    }
}