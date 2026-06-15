package com.travel.planner.dto.response;

import com.travel.planner.model.ShareLink;
import com.travel.planner.model.enums.ShareType;

import java.time.LocalDateTime;

public class ShareLinkResponse {

    private final Long id;
    private final Long tripId;
    private final String token;
    private final String url;
    private final ShareType shareType;
    private final Long assignedToTravelerId;
    private final LocalDateTime createdAt;
    private final LocalDateTime expiresAt;

    public ShareLinkResponse(ShareLink shareLink) {
        this.id = shareLink.getId();
        this.tripId = shareLink.getTrip().getId();
        this.token = shareLink.getToken();
        this.url = shareLink.getUrl();
        this.shareType = shareLink.getShareType();
        this.assignedToTravelerId = shareLink.getAssignedTo() != null
                ? shareLink.getAssignedTo().getId() : null;
        this.createdAt = shareLink.getCreatedAt();
        this.expiresAt = shareLink.getExpiresAt();
    }

    public Long getId() { return id; }
    public Long getTripId() { return tripId; }
    public String getToken() { return token; }
    public String getUrl() { return url; }
    public ShareType getShareType() { return shareType; }
    public Long getAssignedToTravelerId() { return assignedToTravelerId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
}
