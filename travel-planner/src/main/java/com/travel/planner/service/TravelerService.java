package com.travel.planner.service;

import com.travel.planner.dto.request.CreateTravelerRequest;
import com.travel.planner.dto.response.TravelerResponse;
import com.travel.planner.exception.InvalidInputException;
import com.travel.planner.exception.ResourceNotFoundException;
import com.travel.planner.model.Traveler;
import com.travel.planner.repository.TravelerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TravelerService {

    private final TravelerRepository travelerRepository;

    public TravelerService(TravelerRepository travelerRepository) {
        this.travelerRepository = travelerRepository;
    }

    @Transactional
    public TravelerResponse createTraveler(CreateTravelerRequest request) {
        if (travelerRepository.existsByEmail(request.getEmail())) {
            throw new InvalidInputException("此 Email 已被註冊：" + request.getEmail());
        }
        Traveler traveler = new Traveler(
                request.getName(),
                request.getEmail(),
                hashPassword(request.getPassword())
        );
        Traveler saved = travelerRepository.save(traveler);
        return new TravelerResponse(saved);
    }

    @Transactional(readOnly = true)
    public TravelerResponse getTraveler(Long travelerId) {
        return new TravelerResponse(findTravelerById(travelerId));
    }

    @Transactional(readOnly = true)
    public List<TravelerResponse> getAllTravelers() {
        return travelerRepository.findAll().stream()
                .map(TravelerResponse::new)
                .toList();
    }

    Traveler findTravelerById(Long travelerId) {
        return travelerRepository.findById(travelerId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到旅客 ID: " + travelerId));
    }

    private String hashPassword(String plainPassword) {
        // SHA-256 hash without external dependency
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(plainPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("密碼雜湊失敗", e);
        }
    }
}
