package com.side.travellog.domain.pin;

import com.side.travellog.domain.route.TravelRoute;
import com.side.travellog.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;


@Entity
@Table(name = "travel_pin")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TravelPin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    private TravelRoute travelRoute;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private LocalDate visitDate;

    @Column
    private LocalDate endDate;

    @Column
    private LocalTime visitTime;

    @Column(columnDefinition = "TEXT")
    private String memo;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private Integer orderNum;

    private Integer rating; // 1~5점

    public void updateRating(Integer rating) {
        this.rating = rating;
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public void updateMemo(String memo) {
        this.memo = memo;
    }

    public void updateDates(LocalDate visitDate, LocalDate endDate, LocalTime visitTime) {
        this.visitDate = visitDate;
        this.endDate = endDate;
        this.visitTime = visitTime;
    }

    public void updateTitle(String title) {
        this.title = title;
    }

    public void updateOrderNum(Integer orderNum) {
        this.orderNum = orderNum;
    }
}