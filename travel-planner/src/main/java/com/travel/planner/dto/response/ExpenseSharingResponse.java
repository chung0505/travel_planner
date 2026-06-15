package com.travel.planner.dto.response;

import com.travel.planner.model.ExpenseSharing;

import java.math.BigDecimal;

public class ExpenseSharingResponse {

    private final Long id;
    private final Long travelerId;
    private final String travelerName;
    private final BigDecimal amountPerPerson;

    public ExpenseSharingResponse(ExpenseSharing sharing) {
        this.id = sharing.getId();
        this.travelerId = sharing.getTraveler().getId();
        this.travelerName = sharing.getTraveler().getName();
        this.amountPerPerson = sharing.getAmountPerPerson();
    }

    public Long getId() { return id; }
    public Long getTravelerId() { return travelerId; }
    public String getTravelerName() { return travelerName; }
    public BigDecimal getAmountPerPerson() { return amountPerPerson; }
}
