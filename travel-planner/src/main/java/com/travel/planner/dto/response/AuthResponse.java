package com.travel.planner.dto.response;

public class AuthResponse {

    private String token;
    private TravelerResponse traveler;

    public AuthResponse(String token, TravelerResponse traveler) {
        this.token = token;
        this.traveler = traveler;
    }

    public String getToken() { return token; }
    public TravelerResponse getTraveler() { return traveler; }
}
