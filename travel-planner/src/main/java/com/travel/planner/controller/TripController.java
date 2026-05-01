package com.travel.planner.controller;

import com.travel.planner.dto.request.CreateTripRequest;
import com.travel.planner.dto.response.ApiResponse;
import com.travel.planner.dto.response.TripResponse;
import com.travel.planner.service.TripService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * UC-01: Create Trip
 */
@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    /** UC-01: 建立新行程，並自動產生逐日行程架構 */
    @PostMapping
    public ResponseEntity<ApiResponse<TripResponse>> createTrip(@Valid @RequestBody CreateTripRequest request) {
        TripResponse response = tripService.createTrip(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("行程建立成功", response));
    }

    @GetMapping("/{tripId}")
    public ResponseEntity<ApiResponse<TripResponse>> getTrip(@PathVariable Long tripId) {
        TripResponse response = tripService.getTrip(tripId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TripResponse>>> getAllTrips() {
        List<TripResponse> responses = tripService.getAllTrips();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
}
