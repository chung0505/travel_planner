package com.travel.planner.service;

import com.travel.planner.dto.request.ShareItineraryRequest;
import com.travel.planner.dto.response.ShareLinkResponse;
import com.travel.planner.dto.response.TripResponse;
import com.travel.planner.exception.ResourceNotFoundException;
import com.travel.planner.model.ShareLink;
import com.travel.planner.model.Traveler;
import com.travel.planner.model.Trip;
import com.travel.planner.repository.ShareLinkRepository;
import com.travel.planner.repository.TravelerRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShareService {

    private final ShareLinkRepository shareLinkRepository;
    private final TripService tripService;
    private final TravelerRepository travelerRepository;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public ShareService(ShareLinkRepository shareLinkRepository,
                        TripService tripService,
                        TravelerRepository travelerRepository) {
        this.shareLinkRepository = shareLinkRepository;
        this.tripService = tripService;
        this.travelerRepository = travelerRepository;
    }

    @Transactional
    public ShareLinkResponse shareItinerary(Long tripId, ShareItineraryRequest request) {
        Trip trip = tripService.findTripById(tripId);

        Traveler assignedTo = null;
        if (request.getAssignedToTravelerId() != null) {
            assignedTo = travelerRepository.findById(request.getAssignedToTravelerId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "找不到旅客 ID: " + request.getAssignedToTravelerId()));
        }

        ShareLink shareLink = new ShareLink(trip, request.getShareType(), baseUrl, assignedTo);
        ShareLink saved = shareLinkRepository.save(shareLink);
        return new ShareLinkResponse(saved);
    }

    @Transactional(readOnly = true)
    public TripResponse getSharedItinerary(String token) {
        ShareLink shareLink = shareLinkRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("分享連結不存在或已失效"));

        if (!shareLink.isActive() || shareLink.isExpired()) {
            throw new ResourceNotFoundException("分享連結已過期或已停用");
        }

        return new TripResponse(shareLink.getTrip());
    }
}
