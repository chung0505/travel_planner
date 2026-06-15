package com.travel.planner.controller;

import com.travel.planner.dto.request.AddExpenseRequest;
import com.travel.planner.dto.request.SetBudgetRequest;
import com.travel.planner.dto.response.ApiResponse;
import com.travel.planner.dto.response.BudgetSummaryResponse;
import com.travel.planner.dto.response.SettlementResponse;
import com.travel.planner.service.BudgetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trips/{tripId}/budget")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @PutMapping
    public ResponseEntity<ApiResponse<BudgetSummaryResponse>> setBudget(
            @PathVariable Long tripId,
            @Valid @RequestBody SetBudgetRequest request) {
        BudgetSummaryResponse response = budgetService.setBudget(tripId, request);
        return ResponseEntity.ok(ApiResponse.success("預算設定成功", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<BudgetSummaryResponse>> getBudget(
            @PathVariable Long tripId,
            Authentication auth) {
        Long travelerId = (Long) auth.getPrincipal();
        BudgetSummaryResponse response = budgetService.getBudget(tripId, travelerId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/expenses")
    public ResponseEntity<ApiResponse<BudgetSummaryResponse>> addExpense(
            @PathVariable Long tripId,
            @Valid @RequestBody AddExpenseRequest request,
            Authentication auth) {
        Long travelerId = (Long) auth.getPrincipal();
        BudgetSummaryResponse response = budgetService.addExpense(tripId, request, travelerId);
        String message = response.isOverBudget() ? "費用新增成功（注意：您的個人花費已超出預算）" : "費用新增成功";
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(message, response));
    }

    @GetMapping("/settlement")
    public ResponseEntity<ApiResponse<SettlementResponse>> getSettlement(@PathVariable Long tripId) {
        SettlementResponse response = budgetService.getSettlement(tripId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
