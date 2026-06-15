package com.travel.planner.controller;

import com.travel.planner.dto.request.ShareItineraryRequest;
import com.travel.planner.dto.response.ApiResponse;
import com.travel.planner.dto.response.ShareLinkResponse;
import com.travel.planner.dto.response.TripResponse;
import com.travel.planner.service.ShareService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class ShareController {

    private final ShareService shareService;

    public ShareController(ShareService shareService) {
        this.shareService = shareService;
    }

    @PostMapping("/api/trips/{tripId}/share")
    public ResponseEntity<ApiResponse<ShareLinkResponse>> shareItinerary(
            @PathVariable Long tripId,
            @Valid @RequestBody ShareItineraryRequest request) {
        ShareLinkResponse response = shareService.shareItinerary(tripId, request);
        return ResponseEntity.ok(ApiResponse.success("行程分享成功", response));
    }

    @GetMapping("/api/share/{token}")
    public ResponseEntity<ApiResponse<TripResponse>> getSharedItinerary(@PathVariable String token) {
        TripResponse response = shareService.getSharedItinerary(token);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
