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
    @JoinColumn(name = "daily_plan_id", nullable = false)
    private DailyPlan dailyPlan;

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

    public Route(DailyPlan dailyPlan, List<Long> attractionIds, TransportationMethod transportationMethod,
                 int estimatedDurationMinutes, BigDecimal estimatedCost) {
        this.dailyPlan = dailyPlan;
        this.attractionIds = attractionIds;
        this.transportationMethod = transportationMethod;
        this.estimatedDurationMinutes = estimatedDurationMinutes;
        this.estimatedCost = estimatedCost;
        this.confirmed = false;
    }

    public Long getId() { return id; }
    public DailyPlan getDailyPlan() { return dailyPlan; }
    public List<Long> getAttractionIds() { return attractionIds; }
    public TransportationMethod getTransportationMethod() { return transportationMethod; }
    public int getEstimatedDurationMinutes() { return estimatedDurationMinutes; }
    public BigDecimal getEstimatedCost() { return estimatedCost; }
    public boolean isConfirmed() { return confirmed; }
    public String getGeometryJson() { return geometryJson; }

    // ── Domain behaviour ────────────────────────────────────────────────────

    /**
     * 將路線標記為已確認，並儲存路線幾何（序列化後的 JSON 字串）。
     * 將兩個相關狀態變更封裝為單一領域操作。
     */
    public void confirm(String geometryJson) {
        this.confirmed = true;
        this.geometryJson = geometryJson;
    }

    /**
     * 依交通方式與距離計算費用（台灣費率）。
     * <ul>
     *   <li>步行：免費</li>
     *   <li>大眾運輸：固定估算 30 元（API 回傳實際票價時由呼叫端覆蓋）</li>
     *   <li>計程車：起跳 85 元，超過 1.25 km 後每 200m 加 5 元</li>
     * </ul>
     */
    public static BigDecimal calculateCost(TransportationMethod method, double distanceMeters) {
        double km = distanceMeters / 1000.0;
        double fare = switch (method) {
            case WALKING -> 0;
            case PUBLIC_TRANSIT -> 30;
            case TAXI -> 85 + Math.max(0, (km - 1.25) / 0.2 * 5);
        };
        return BigDecimal.valueOf(Math.round(fare));
    }
}
