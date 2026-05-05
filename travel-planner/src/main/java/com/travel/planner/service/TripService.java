package com.travel.planner.service;

import com.travel.planner.dto.request.CreateTripRequest;
import com.travel.planner.dto.response.TripResponse;
import com.travel.planner.exception.ResourceNotFoundException;
import com.travel.planner.model.Trip;
import com.travel.planner.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TripService {

    private final TripRepository tripRepository;

    public TripService(TripRepository tripRepository) {
        this.tripRepository = tripRepository;
    }

    @Transactional
    public TripResponse createTrip(CreateTripRequest request) {
        Trip.validateDates(request.getDepartureDate(), request.getReturnDate());

        Trip trip = new Trip(
                request.getTitle(),
                request.getDestination(),
                request.getDepartureDate(),
                request.getReturnDate(),
                request.getCompanionCount()
        );

        trip.generateDailyPlans();

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

    @Transactional
    public void deleteTrip(Long tripId) {
        Trip trip = findTripById(tripId);
        tripRepository.delete(trip);
    }

    Trip findTripById(Long tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到行程 ID: " + tripId));
    }
}
