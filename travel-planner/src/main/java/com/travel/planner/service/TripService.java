package com.travel.planner.service;

import com.travel.planner.dto.request.CreateTripRequest;
import com.travel.planner.dto.response.TripResponse;
import com.travel.planner.exception.InvalidInputException;
import com.travel.planner.exception.ResourceNotFoundException;
import com.travel.planner.model.Traveler;
import com.travel.planner.model.Trip;
import com.travel.planner.repository.TravelerRepository;
import com.travel.planner.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TripService {

    private final TripRepository tripRepository;
    private final TravelerRepository travelerRepository;

    public TripService(TripRepository tripRepository, TravelerRepository travelerRepository) {
        this.tripRepository = tripRepository;
        this.travelerRepository = travelerRepository;
    }

    @Transactional
    public TripResponse createTrip(CreateTripRequest request, Long organizerId) {
        Trip.validateDates(request.getDepartureDate(), request.getReturnDate());

        Traveler organizer = travelerRepository.findById(organizerId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到旅客 ID: " + organizerId));

        Trip trip = new Trip(
                request.getTitle(),
                request.getDestination(),
                request.getDepartureDate(),
                request.getReturnDate(),
                request.getCompanionCount()
        );
        trip.setOrganizer(organizer);
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
    public List<TripResponse> getAllTrips(Long travelerId) {
        // 自己建立的行程 + 被加入為旅伴的行程（去重）
        List<Trip> organized = tripRepository.findByOrganizerId(travelerId);
        List<Trip> participating = tripRepository.findByParticipantsId(travelerId);

        return java.util.stream.Stream.concat(organized.stream(), participating.stream())
                .distinct()
                .map(TripResponse::new)
                .toList();
    }

    @Transactional
    public void deleteTrip(Long tripId) {
        Trip trip = findTripById(tripId);
        tripRepository.delete(trip);
    }

    @Transactional
    public TripResponse addParticipant(Long tripId, Long travelerId) {
        Trip trip = findTripById(tripId);

        // organizer 佔 1 位，加上現有 participants，不能超過 companionCount
        int currentTotal = 1 + trip.getParticipants().size();
        if (currentTotal >= trip.getCompanionCount()) {
            throw new InvalidInputException(
                    "已達行程人數上限（" + trip.getCompanionCount() + " 人），無法再新增旅伴");
        }

        Traveler traveler = travelerRepository.findById(travelerId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到旅客 ID: " + travelerId));
        trip.addParticipant(traveler);
        return new TripResponse(tripRepository.save(trip));
    }

    @Transactional
    public TripResponse removeParticipant(Long tripId, Long travelerId) {
        Trip trip = findTripById(tripId);
        trip.getParticipants().removeIf(t -> t.getId().equals(travelerId));
        return new TripResponse(tripRepository.save(trip));
    }

    Trip findTripById(Long tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到行程 ID: " + tripId));
    }
}
