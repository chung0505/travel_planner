package com.travel.planner.dto.request;

import com.travel.planner.model.enums.TransportationMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public class PlanRouteRequest {

    @NotNull(message = "請選擇景點清單")
    @Size(min = 2, message = "至少需要選擇兩個景點")
    private List<Long> attractionIds;

    @NotNull(message = "請選擇交通方式")
    private TransportationMethod transportationMethod;

    public List<Long> getAttractionIds() { return attractionIds; }
    public TransportationMethod getTransportationMethod() { return transportationMethod; }

    public void setAttractionIds(List<Long> attractionIds) { this.attractionIds = attractionIds; }
    public void setTransportationMethod(TransportationMethod transportationMethod) {
        this.transportationMethod = transportationMethod;
    }
}
