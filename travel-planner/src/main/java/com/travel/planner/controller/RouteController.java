package com.travel.planner.controller;

import com.travel.planner.dto.request.PlanRouteRequest;
import com.travel.planner.dto.response.ApiResponse;
import com.travel.planner.dto.response.RouteEstimateResponse;
import com.travel.planner.dto.response.RouteResponse;
import com.travel.planner.service.RouteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * UC-02: Plan Route
 */
@RestController
@RequestMapping("/api/trips/{tripId}/routes")
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    /** UC-02: 取得路線估算（交通時間與費用預覽），不儲存 */
    @PostMapping("/estimate")
    public ResponseEntity<ApiResponse<RouteEstimateResponse>> estimateRoute(
            @PathVariable Long tripId,
            @Valid @RequestBody PlanRouteRequest request) {
        RouteEstimateResponse response = routeService.estimateRoute(tripId, request);
        return ResponseEntity.ok(ApiResponse.success("路線估算完成", response));
    }

    /** UC-02: 使用者確認路線，儲存至行程 */
    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<RouteResponse>> confirmRoute(
            @PathVariable Long tripId,
            @Valid @RequestBody PlanRouteRequest request) {
        RouteResponse response = routeService.confirmRoute(tripId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("路線規劃已確認並儲存", response));
    }

    /** 取得行程中所有已確認的路線 */
    @GetMapping
    public ResponseEntity<ApiResponse<List<RouteResponse>>> getRoutes(@PathVariable Long tripId) {
        List<RouteResponse> responses = routeService.getRoutes(tripId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /** 取得特定路線詳情 */
    @GetMapping("/{routeId}")
    public ResponseEntity<ApiResponse<RouteResponse>> getRoute(
            @PathVariable Long tripId,
            @PathVariable Long routeId) {
        RouteResponse response = routeService.getRoute(tripId, routeId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
