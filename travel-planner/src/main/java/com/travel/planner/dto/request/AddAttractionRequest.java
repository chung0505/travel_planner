package com.travel.planner.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public class AddAttractionRequest {

    @NotBlank(message = "景點名稱為必填欄位")
    private String name;

    @NotBlank(message = "景點地址為必填欄位")
    private String address;

    @NotNull(message = "開始時間為必填欄位")
    private LocalTime startTime;

    @NotNull(message = "結束時間為必填欄位")
    private LocalTime endTime;

    public String getName() { return name; }
    public String getAddress() { return address; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }

    public void setName(String name) { this.name = name; }
    public void setAddress(String address) { this.address = address; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
}
