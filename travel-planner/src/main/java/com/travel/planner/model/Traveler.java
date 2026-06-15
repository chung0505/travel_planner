package com.travel.planner.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "travelers")
public class Traveler {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @OneToMany(mappedBy = "organizer")
    private List<Trip> organizedTrips = new ArrayList<>();

    @ManyToMany(mappedBy = "participants")
    private List<Trip> participatingTrips = new ArrayList<>();

    protected Traveler() {}

    public Traveler(String name, String email, String passwordHash) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public List<Trip> getOrganizedTrips() { return organizedTrips; }
    public List<Trip> getParticipatingTrips() { return participatingTrips; }

    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
}
