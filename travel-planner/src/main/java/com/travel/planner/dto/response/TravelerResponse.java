package com.travel.planner.dto.response;

import com.travel.planner.model.Traveler;

public class TravelerResponse {

    private final Long id;
    private final String name;
    private final String email;

    public TravelerResponse(Traveler traveler) {
        this.id = traveler.getId();
        this.name = traveler.getName();
        this.email = traveler.getEmail();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
}
