package com.travel.planner.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class CreateTripRequest {

    @NotBlank(message = "行程名稱為必填欄位")
    private String title;

    @NotBlank(message = "目的地為必填欄位")
    private String destination;

    @NotNull(message = "出發日期為必填欄位")
    private LocalDate departureDate;

    @NotNull(message = "回程日期為必填欄位")
    private LocalDate returnDate;

    @Min(value = 1, message = "旅伴人數至少為 1")
    private int companionCount;

    public String getTitle() { return title; }
    public String getDestination() { return destination; }
    public LocalDate getDepartureDate() { return departureDate; }
    public LocalDate getReturnDate() { return returnDate; }
    public int getCompanionCount() { return companionCount; }

    public void setTitle(String title) { this.title = title; }
    public void setDestination(String destination) { this.destination = destination; }
    public void setDepartureDate(LocalDate departureDate) { this.departureDate = departureDate; }
    public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }
    public void setCompanionCount(int companionCount) { this.companionCount = companionCount; }
}
