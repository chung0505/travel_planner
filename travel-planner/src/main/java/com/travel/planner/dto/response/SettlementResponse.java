package com.travel.planner.dto.response;

import java.math.BigDecimal;
import java.util.List;

public class SettlementResponse {

    private List<TravelerBalance> balances;
    private List<TransferItem> transfers;

    public SettlementResponse(List<TravelerBalance> balances, List<TransferItem> transfers) {
        this.balances = balances;
        this.transfers = transfers;
    }

    public List<TravelerBalance> getBalances() { return balances; }
    public List<TransferItem> getTransfers() { return transfers; }

    public static class TravelerBalance {
        private Long travelerId;
        private String travelerName;
        private BigDecimal totalPaid;
        private BigDecimal totalOwed;
        private BigDecimal balance;

        public TravelerBalance(Long travelerId, String travelerName,
                               BigDecimal totalPaid, BigDecimal totalOwed) {
            this.travelerId = travelerId;
            this.travelerName = travelerName;
            this.totalPaid = totalPaid;
            this.totalOwed = totalOwed;
            this.balance = totalPaid.subtract(totalOwed);
        }

        public Long getTravelerId() { return travelerId; }
        public String getTravelerName() { return travelerName; }
        public BigDecimal getTotalPaid() { return totalPaid; }
        public BigDecimal getTotalOwed() { return totalOwed; }
        public BigDecimal getBalance() { return balance; }
    }

    public static class TransferItem {
        private Long fromTravelerId;
        private String fromTravelerName;
        private Long toTravelerId;
        private String toTravelerName;
        private BigDecimal amount;

        public TransferItem(Long fromTravelerId, String fromTravelerName,
                            Long toTravelerId, String toTravelerName,
                            BigDecimal amount) {
            this.fromTravelerId = fromTravelerId;
            this.fromTravelerName = fromTravelerName;
            this.toTravelerId = toTravelerId;
            this.toTravelerName = toTravelerName;
            this.amount = amount;
        }

        public Long getFromTravelerId() { return fromTravelerId; }
        public String getFromTravelerName() { return fromTravelerName; }
        public Long getToTravelerId() { return toTravelerId; }
        public String getToTravelerName() { return toTravelerName; }
        public BigDecimal getAmount() { return amount; }
    }
}
