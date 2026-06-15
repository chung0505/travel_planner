package com.travel.planner.controller;

import com.travel.planner.dto.request.CreateTravelerRequest;
import com.travel.planner.dto.response.ApiResponse;
import com.travel.planner.dto.response.TravelerResponse;
import com.travel.planner.service.TravelerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/travelers")
public class TravelerController {

    private final TravelerService travelerService;

    public TravelerController(TravelerService travelerService) {
        this.travelerService = travelerService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TravelerResponse>> createTraveler(
            @Valid @RequestBody CreateTravelerRequest request) {
        TravelerResponse response = travelerService.createTraveler(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("旅客註冊成功", response));
    }

    @GetMapping("/{travelerId}")
    public ResponseEntity<ApiResponse<TravelerResponse>> getTraveler(@PathVariable Long travelerId) {
        TravelerResponse response = travelerService.getTraveler(travelerId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TravelerResponse>>> getAllTravelers() {
        List<TravelerResponse> responses = travelerService.getAllTravelers();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
}
