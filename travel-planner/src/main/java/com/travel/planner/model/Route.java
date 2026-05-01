package com.travel.planner.model;

import com.travel.planner.model.enums.TransportationMethod;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "routes")
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ElementCollection
    @CollectionTable(name = "route_attraction_ids", joinColumns = @JoinColumn(name = "route_id"))
    @Column(name = "attraction_id")
    @OrderColumn(name = "order_index")
    private List<Long> attractionIds = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransportationMethod transportationMethod;

    @Column(nullable = false)
    private int estimatedDurationMinutes;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal estimatedCost;

    private boolean confirmed;

    // Serialized as JSON: [[lat,lng],[lat,lng],...]
    @Column(columnDefinition = "TEXT")
    private String geometryJson;

    protected Route() {}

    public Route(Trip trip, List<Long> attractionIds, TransportationMethod transportationMethod,
                 int estimatedDurationMinutes, BigDecimal estimatedCost) {
        this.trip = trip;
        this.attractionIds = attractionIds;
        this.transportationMethod = transportationMethod;
        this.estimatedDurationMinutes = estimatedDurationMinutes;
        this.estimatedCost = estimatedCost;
        this.confirmed = false;
    }

    public Long getId() { return id; }
    public Trip getTrip() { return trip; }
    public List<Long> getAttractionIds() { return attractionIds; }
    public TransportationMethod getTransportationMethod() { return transportationMethod; }
    public int getEstimatedDurationMinutes() { return estimatedDurationMinutes; }
    public BigDecimal getEstimatedCost() { return estimatedCost; }
    public boolean isConfirmed() { return confirmed; }

    public String getGeometryJson() { return geometryJson; }
    public void setConfirmed(boolean confirmed) { this.confirmed = confirmed; }
    public void setGeometryJson(String geometryJson) { this.geometryJson = geometryJson; }
}
