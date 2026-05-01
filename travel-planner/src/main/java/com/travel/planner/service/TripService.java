package com.travel.planner.service;

import com.travel.planner.dto.request.CreateTripRequest;
import com.travel.planner.dto.response.TripResponse;
import com.travel.planner.exception.InvalidInputException;
import com.travel.planner.exception.ResourceNotFoundException;
import com.travel.planner.model.DailyPlan;
import com.travel.planner.model.Trip;
import com.travel.planner.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * UC-01: Create Trip
 * 負責建立行程並自動產生逐日行程架構
 */
@Service
public class TripService {

    private final TripRepository tripRepository;

    public TripService(TripRepository tripRepository) {
        this.tripRepository = tripRepository;
    }

    @Transactional
    public TripResponse createTrip(CreateTripRequest request) {
        validateTripDates(request.getDepartureDate(), request.getReturnDate());

        Trip trip = new Trip(
                request.getDestination(),
                request.getDepartureDate(),
                request.getReturnDate(),
                request.getCompanionCount()
        );

        generateDailyPlans(trip);

        Trip saved = tripRepository.save(trip);
        return new TripResponse(saved);
    }

    @Transactional(readOnly = true)
    public TripResponse getTrip(Long tripId) {
        Trip trip = findTripById(tripId);
        return new TripResponse(trip);
    }

    @Transactional(readOnly = true)
    public List<TripResponse> getAllTrips() {
        return tripRepository.findAll().stream()
                .map(TripResponse::new)
                .toList();
    }

    Trip findTripById(Long tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到行程 ID: " + tripId));
    }

    private void validateTripDates(LocalDate departureDate, LocalDate returnDate) {
        if (!returnDate.isAfter(departureDate)) {
            throw new InvalidInputException("回程日期必須晚於出發日期");
        }
    }

    private void generateDailyPlans(Trip trip) {
        LocalDate current = trip.getDepartureDate();
        int dayNumber = 1;
        while (!current.isAfter(trip.getReturnDate())) {
            DailyPlan dailyPlan = new DailyPlan(trip, current, dayNumber);
            trip.getDailyPlans().add(dailyPlan);
            current = current.plusDays(1);
            dayNumber++;
        }
    }
}
