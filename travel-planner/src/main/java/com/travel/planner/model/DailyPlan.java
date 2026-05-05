package com.travel.planner.model;

import com.travel.planner.exception.InvalidInputException;
import com.travel.planner.exception.TimeConflictException;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "daily_plans")
public class DailyPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private int dayNumber;

    @OneToMany(mappedBy = "dailyPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("startTime ASC")
    private List<Attraction> attractions = new ArrayList<>();

    protected DailyPlan() {}

    public DailyPlan(Trip trip, LocalDate date, int dayNumber) {
        this.trip = trip;
        this.date = date;
        this.dayNumber = dayNumber;
    }

    public Long getId() { return id; }
    public Trip getTrip() { return trip; }
    public LocalDate getDate() { return date; }
    public int getDayNumber() { return dayNumber; }
    public List<Attraction> getAttractions() { return attractions; }

    // ── Domain behaviour ────────────────────────────────────────────────────

    /**
     * 驗證景點時間區間的基本合法性：結束時間必須晚於開始時間。
     */
    public static void validateAttractionTimes(LocalTime startTime, LocalTime endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new InvalidInputException("結束時間必須晚於開始時間");
        }
    }

    /**
     * 確認新景點的時間區間不與此每日行程中已有的景點衝突。
     * excludeId 可用於編輯景點時排除自身，傳入 null 表示不排除任何景點。
     */
    public void checkTimeConflict(LocalTime startTime, LocalTime endTime, Long excludeId) {
        attractions.stream()
                .filter(a -> excludeId == null || !a.getId().equals(excludeId))
                .filter(a -> startTime.isBefore(a.getEndTime()) && endTime.isAfter(a.getStartTime()))
                .findFirst()
                .ifPresent(conflict -> {
                    throw new TimeConflictException(
                            String.format("時間衝突：與景點「%s」（%s ~ %s）發生重疊",
                                    conflict.getName(), conflict.getStartTime(), conflict.getEndTime()));
                });
    }

    /**
     * 建立並加入新景點（不含 geocoding）。
     * 呼叫前須先完成時間驗證與衝突偵測。
     */
    public Attraction addAttraction(String name, String address, LocalTime startTime, LocalTime endTime) {
        Attraction attraction = new Attraction(this, name, address, startTime, endTime);
        attractions.add(attraction);
        return attraction;
    }
}
