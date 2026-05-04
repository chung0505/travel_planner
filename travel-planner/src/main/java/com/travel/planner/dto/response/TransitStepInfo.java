package com.travel.planner.dto.response;

public class TransitStepInfo {

    private final String travelMode;    // "TRANSIT" | "WALKING"
    private final String vehicleName;   // 捷運 / 公車 / 步行
    private final String lineName;      // 板南線 / 297路（TRANSIT 才有）
    private final String departureStop; // 出發站（TRANSIT 才有）
    private final String arrivalStop;   // 抵達站（TRANSIT 才有）
    private final Integer numStops;     // 乘坐站數（TRANSIT 才有）
    private final int durationMinutes;  // 此步驟所需分鐘

    public TransitStepInfo(String travelMode, String vehicleName, String lineName,
                           String departureStop, String arrivalStop,
                           Integer numStops, int durationMinutes) {
        this.travelMode    = travelMode;
        this.vehicleName   = vehicleName;
        this.lineName      = lineName;
        this.departureStop = departureStop;
        this.arrivalStop   = arrivalStop;
        this.numStops      = numStops;
        this.durationMinutes = durationMinutes;
    }

    public String getTravelMode()    { return travelMode; }
    public String getVehicleName()   { return vehicleName; }
    public String getLineName()      { return lineName; }
    public String getDepartureStop() { return departureStop; }
    public String getArrivalStop()   { return arrivalStop; }
    public Integer getNumStops()     { return numStops; }
    public int getDurationMinutes()  { return durationMinutes; }
}
