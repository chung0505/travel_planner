package com.travel.planner.controller;

import com.travel.planner.dto.request.AddAttractionRequest;
import com.travel.planner.dto.response.ApiResponse;
import com.travel.planner.dto.response.AttractionResponse;
import com.travel.planner.dto.response.DailyPlanResponse;
import com.travel.planner.service.DailyPlanService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * UC-05: Manage Daily Plan
 */
@RestController
@RequestMapping("/api/trips/{tripId}/daily-plans")
public class DailyPlanController {

    private final DailyPlanService dailyPlanService;

    public DailyPlanController(DailyPlanService dailyPlanService) {
        this.dailyPlanService = dailyPlanService;
    }

    /** UC-05: 取得行程的所有每日行程 */
    @GetMapping
    public ResponseEntity<ApiResponse<List<DailyPlanResponse>>> getDailyPlans(@PathVariable Long tripId) {
        List<DailyPlanResponse> responses = dailyPlanService.getDailyPlans(tripId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /** UC-05: 取得特定某日的行程 */
    @GetMapping("/{dailyPlanId}")
    public ResponseEntity<ApiResponse<DailyPlanResponse>> getDailyPlan(
            @PathVariable Long tripId,
            @PathVariable Long dailyPlanId) {
        DailyPlanResponse response = dailyPlanService.getDailyPlan(tripId, dailyPlanId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /** UC-05: 新增景點至指定日期的行程，並進行時間衝突偵測 */
    @PostMapping("/{dailyPlanId}/attractions")
    public ResponseEntity<ApiResponse<DailyPlanResponse>> addAttraction(
            @PathVariable Long tripId,
            @PathVariable Long dailyPlanId,
            @Valid @RequestBody AddAttractionRequest request) {
        DailyPlanResponse response = dailyPlanService.addAttraction(tripId, dailyPlanId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("景點新增成功", response));
    }

    /** 刪除景點 */
    @DeleteMapping("/{dailyPlanId}/attractions/{attractionId}")
    public ResponseEntity<ApiResponse<AttractionResponse>> removeAttraction(
            @PathVariable Long tripId,
            @PathVariable Long dailyPlanId,
            @PathVariable Long attractionId) {
        AttractionResponse response = dailyPlanService.removeAttraction(tripId, dailyPlanId, attractionId);
        return ResponseEntity.ok(ApiResponse.success("景點已移除", response));
    }
}
