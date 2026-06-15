package com.travel.planner.model;

import com.travel.planner.model.enums.ShareType;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "share_links")
public class ShareLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShareType shareType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_traveler_id")
    private Traveler assignedTo;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean active;

    protected ShareLink() {}

    public ShareLink(Trip trip, ShareType shareType, String baseUrl, Traveler assignedTo) {
        this.trip = trip;
        this.shareType = shareType;
        this.token = UUID.randomUUID().toString().replace("-", "");
        this.createdAt = LocalDateTime.now();
        this.expiresAt = this.createdAt.plusDays(7);
        this.active = true;
        this.assignedTo = assignedTo;
        this.url = baseUrl + "/share/" + this.token;
    }

    public Long getId() { return id; }
    public Trip getTrip() { return trip; }
    public String getToken() { return token; }
    public String getUrl() { return url; }
    public ShareType getShareType() { return shareType; }
    public Traveler getAssignedTo() { return assignedTo; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public boolean isActive() { return active; }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public void deactivate() {
        this.active = false;
    }
}
