package com.travel.planner.dto.request;

import com.travel.planner.model.enums.ShareType;
import jakarta.validation.constraints.NotNull;

public class ShareItineraryRequest {

    @NotNull(message = "分享方式為必填欄位")
    private ShareType shareType;

    private Long assignedToTravelerId;

    public ShareType getShareType() { return shareType; }
    public Long getAssignedToTravelerId() { return assignedToTravelerId; }

    public void setShareType(ShareType shareType) { this.shareType = shareType; }
    public void setAssignedToTravelerId(Long assignedToTravelerId) { this.assignedToTravelerId = assignedToTravelerId; }
}
