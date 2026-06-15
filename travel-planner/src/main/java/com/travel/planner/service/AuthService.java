package com.travel.planner.service;

import com.travel.planner.dto.request.CreateTravelerRequest;
import com.travel.planner.dto.request.LoginRequest;
import com.travel.planner.dto.response.AuthResponse;
import com.travel.planner.dto.response.TravelerResponse;
import com.travel.planner.exception.InvalidInputException;
import com.travel.planner.model.Traveler;
import com.travel.planner.repository.TravelerRepository;
import com.travel.planner.security.JwtUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class AuthService {

    private final TravelerRepository travelerRepository;
    private final TravelerService travelerService;
    private final JwtUtil jwtUtil;

    public AuthService(TravelerRepository travelerRepository,
                       TravelerService travelerService,
                       JwtUtil jwtUtil) {
        this.travelerRepository = travelerRepository;
        this.travelerService = travelerService;
        this.jwtUtil = jwtUtil;
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Traveler traveler = travelerRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidInputException("Email 或密碼錯誤"));

        String hashed = hashPassword(request.getPassword());
        if (!hashed.equals(traveler.getPasswordHash())) {
            throw new InvalidInputException("Email 或密碼錯誤");
        }

        String token = jwtUtil.generateToken(traveler.getId());
        return new AuthResponse(token, new TravelerResponse(traveler));
    }

    @Transactional
    public AuthResponse register(CreateTravelerRequest request) {
        TravelerResponse travelerResponse = travelerService.createTraveler(request);
        String token = jwtUtil.generateToken(travelerResponse.getId());
        return new AuthResponse(token, travelerResponse);
    }

    private String hashPassword(String plainPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(plainPassword.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("密碼雜湊失敗", e);
        }
    }
}
