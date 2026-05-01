package com.travel.planner.service;

import com.travel.planner.dto.request.AddAttractionRequest;
import com.travel.planner.dto.response.AttractionResponse;
import com.travel.planner.dto.response.DailyPlanResponse;
import com.travel.planner.exception.InvalidInputException;
import com.travel.planner.exception.ResourceNotFoundException;
import com.travel.planner.exception.TimeConflictException;
import com.travel.planner.model.Attraction;
import com.travel.planner.model.DailyPlan;
import com.travel.planner.repository.DailyPlanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
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
        validateAttractionTimes(request.getStartTime(), request.getEndTime());

        DailyPlan dailyPlan = findDailyPlan(tripId, dailyPlanId);

        checkTimeConflict(dailyPlan, request.getStartTime(), request.getEndTime(), null);

        Attraction attraction = new Attraction(
                dailyPlan,
                request.getName(),
                request.getAddress(),
                request.getStartTime(),
                request.getEndTime()
        );

        // 自動將地址轉換為經緯度（失敗時不阻擋新增，僅記錄 log）
        double[] latLng = orsService.geocode(request.getAddress());
        if (latLng != null) {
            attraction.setLatitude(latLng[0]);
            attraction.setLongitude(latLng[1]);
        } else {
            log.warn("景點「{}」地址無法 Geocoding，路線規劃將無法使用此景點", request.getName());
        }

        dailyPlan.getAttractions().add(attraction);

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

    private void validateAttractionTimes(LocalTime startTime, LocalTime endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new InvalidInputException("結束時間必須晚於開始時間");
        }
    }

    private void checkTimeConflict(DailyPlan dailyPlan, LocalTime startTime, LocalTime endTime, Long excludeId) {
        dailyPlan.getAttractions().stream()
                .filter(a -> excludeId == null || !a.getId().equals(excludeId))
                .filter(a -> isTimeOverlap(a.getStartTime(), a.getEndTime(), startTime, endTime))
                .findFirst()
                .ifPresent(conflict -> {
                    throw new TimeConflictException(
                            String.format("時間衝突：與景點「%s」（%s ~ %s）發生重疊",
                                    conflict.getName(), conflict.getStartTime(), conflict.getEndTime()));
                });
    }

    private boolean isTimeOverlap(LocalTime existStart, LocalTime existEnd,
                                   LocalTime newStart, LocalTime newEnd) {
        return newStart.isBefore(existEnd) && newEnd.isAfter(existStart);
    }
}
