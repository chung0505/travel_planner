package com.travel.planner.model;

import com.travel.planner.exception.InvalidInputException;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "trips")
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String destination;

    @Column(nullable = false)
    private LocalDate departureDate;

    @Column(nullable = false)
    private LocalDate returnDate;

    @Column(nullable = false)
    private int companionCount;

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("date ASC")
    private List<DailyPlan> dailyPlans = new ArrayList<>();

    protected Trip() {}

    public Trip(String title, String destination, LocalDate departureDate, LocalDate returnDate, int companionCount) {
        this.title = title;
        this.destination = destination;
        this.departureDate = departureDate;
        this.returnDate = returnDate;
        this.companionCount = companionCount;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDestination() { return destination; }
    public LocalDate getDepartureDate() { return departureDate; }
    public LocalDate getReturnDate() { return returnDate; }
    public int getCompanionCount() { return companionCount; }
    public List<DailyPlan> getDailyPlans() { return dailyPlans; }

    public void setTitle(String title) { this.title = title; }
    public void setDestination(String destination) { this.destination = destination; }
    public void setDepartureDate(LocalDate departureDate) { this.departureDate = departureDate; }
    public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }
    public void setCompanionCount(int companionCount) { this.companionCount = companionCount; }

    // ── Domain behaviour ────────────────────────────────────────────────────

    /**
     * 驗證出發日期與回程日期的合法性。
     * 回程日期必須晚於出發日期，否則拋出 InvalidInputException。
     */
    public static void validateDates(LocalDate departureDate, LocalDate returnDate) {
        if (!returnDate.isAfter(departureDate)) {
            throw new InvalidInputException("回程日期必須晚於出發日期");
        }
    }

    /**
     * 根據出發日期與回程日期，自動產生每日行程（DailyPlan）並加入此行程。
     * 每天一筆，從出發日到回程日（含）。
     */
    public void generateDailyPlans() {
        LocalDate current = this.departureDate;
        int dayNumber = 1;
        while (!current.isAfter(this.returnDate)) {
            this.dailyPlans.add(new DailyPlan(this, current, dayNumber));
            current = current.plusDays(1);
            dayNumber++;
        }
    }
}
