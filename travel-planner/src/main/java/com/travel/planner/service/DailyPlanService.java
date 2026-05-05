package com.travel.planner.service;

import com.travel.planner.dto.request.AddAttractionRequest;
import com.travel.planner.dto.response.AttractionResponse;
import com.travel.planner.dto.response.DailyPlanResponse;
import com.travel.planner.exception.ResourceNotFoundException;
import com.travel.planner.model.Attraction;
import com.travel.planner.model.DailyPlan;
import com.travel.planner.repository.DailyPlanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * UC-05: Manage Daily Plan
 * 負責每日行程景點的新增與時間衝突偵測
 */
@Service
public class DailyPlanService {

    private static final Logger log = LoggerFactory.getLogger(DailyPlanService.class);

    private final DailyPlanRepository dailyPlanRepository;
    private final TripService tripService;
    private final OrsService orsService;

    public DailyPlanService(DailyPlanRepository dailyPlanRepository,
                             TripService tripService,
                             OrsService orsService) {
        this.dailyPlanRepository = dailyPlanRepository;
        this.tripService = tripService;
        this.orsService = orsService;
    }

    @Transactional(readOnly = true)
    public List<DailyPlanResponse> getDailyPlans(Long tripId) {
        tripService.findTripById(tripId);
        return dailyPlanRepository.findByTripIdOrderByDateAsc(tripId).stream()
                .map(DailyPlanResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public DailyPlanResponse getDailyPlan(Long tripId, Long dailyPlanId) {
        DailyPlan dailyPlan = findDailyPlan(tripId, dailyPlanId);
        return new DailyPlanResponse(dailyPlan);
    }

    @Transactional
    public DailyPlanResponse addAttraction(Long tripId, Long dailyPlanId, AddAttractionRequest request) {
        DailyPlan.validateAttractionTimes(request.getStartTime(), request.getEndTime());

        DailyPlan dailyPlan = findDailyPlan(tripId, dailyPlanId);

        dailyPlan.checkTimeConflict(request.getStartTime(), request.getEndTime(), null);

        // 委派給 DailyPlan 建立並加入景點（純領域邏輯）
        Attraction attraction = dailyPlan.addAttraction(
                request.getName(),
                request.getAddress(),
                request.getStartTime(),
                request.getEndTime()
        );

        // Geocoding 屬於基礎設施關注點，保留在 Service 層
        double[] latLng = orsService.geocode(request.getAddress());
        if (latLng != null) {
            attraction.setLatitude(latLng[0]);
            attraction.setLongitude(latLng[1]);
        } else {
            log.warn("景點「{}」地址無法 Geocoding，路線規劃將無法使用此景點", request.getName());
        }

        DailyPlan saved = dailyPlanRepository.save(dailyPlan);
        return new DailyPlanResponse(saved);
    }

    @Transactional
    public AttractionResponse removeAttraction(Long tripId, Long dailyPlanId, Long attractionId) {
        DailyPlan dailyPlan = findDailyPlan(tripId, dailyPlanId);

        Attraction attraction = dailyPlan.getAttractions().stream()
                .filter(a -> a.getId().equals(attractionId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("找不到景點 ID: " + attractionId));

        AttractionResponse response = new AttractionResponse(attraction);
        dailyPlan.getAttractions().remove(attraction);
        dailyPlanRepository.save(dailyPlan);
        return response;
    }

    private DailyPlan findDailyPlan(Long tripId, Long dailyPlanId) {
        return dailyPlanRepository.findByIdAndTripId(dailyPlanId, tripId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "找不到行程 ID " + tripId + " 中的每日行程 ID: " + dailyPlanId));
    }
}
