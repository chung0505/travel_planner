package com.travel.planner.service;

import com.travel.planner.dto.request.AddExpenseRequest;
import com.travel.planner.dto.request.SetBudgetRequest;
import com.travel.planner.dto.response.BudgetSummaryResponse;
import com.travel.planner.dto.response.SettlementResponse;
import com.travel.planner.dto.response.SettlementResponse.TransferItem;
import com.travel.planner.dto.response.SettlementResponse.TravelerBalance;
import com.travel.planner.exception.InvalidInputException;
import com.travel.planner.exception.ResourceNotFoundException;
import com.travel.planner.model.Budget;
import com.travel.planner.model.Expense;
import com.travel.planner.model.ExpenseSharing;
import com.travel.planner.model.Traveler;
import com.travel.planner.model.Trip;
import com.travel.planner.repository.BudgetRepository;
import com.travel.planner.repository.TravelerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final TripService tripService;
    private final TravelerRepository travelerRepository;

    public BudgetService(BudgetRepository budgetRepository,
                         TripService tripService,
                         TravelerRepository travelerRepository) {
        this.budgetRepository = budgetRepository;
        this.tripService = tripService;
        this.travelerRepository = travelerRepository;
    }

    @Transactional
    public BudgetSummaryResponse setBudget(Long tripId, SetBudgetRequest request) {
        Trip trip = tripService.findTripById(tripId);
        Budget budget = budgetRepository.findByTripId(tripId)
                .orElseGet(() -> new Budget(trip, request.getTotalBudget(), request.getCurrency()));
        budget.setTotalBudget(request.getTotalBudget());
        budget.setCurrency(request.getCurrency());
        Budget saved = budgetRepository.save(budget);
        return new BudgetSummaryResponse(saved);
    }

    @Transactional(readOnly = true)
    public BudgetSummaryResponse getBudget(Long tripId, Long travelerId) {
        tripService.findTripById(tripId);
        Budget budget = findBudgetByTripId(tripId);
        return new BudgetSummaryResponse(budget, travelerId);
    }

    @Transactional
    public BudgetSummaryResponse addExpense(Long tripId, AddExpenseRequest request, Long travelerId) {
        tripService.findTripById(tripId);
        Budget budget = findBudgetByTripId(tripId);

        Traveler paidByTraveler = null;
        if (request.getPaidByTravelerId() != null) {
            paidByTraveler = travelerRepository.findById(request.getPaidByTravelerId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "找不到旅客 ID: " + request.getPaidByTravelerId()));
        }

        Expense expense = new Expense(
                budget,
                request.getExpenseType(),
                request.getAmount(),
                request.getDate(),
                request.getNote(),
                request.getCurrency(),
                request.getExchangeRate(),
                request.getOriginalAmount(),
                request.getPaidBy(),
                paidByTraveler
        );

        List<Long> splitIds = request.getSplitAmongTravelerIds();
        if (!splitIds.isEmpty()) {
            List<Traveler> travelers = travelerRepository.findAllById(splitIds);
            if (travelers.size() != splitIds.size()) {
                throw new ResourceNotFoundException("部分旅客 ID 不存在，請確認分攤名單");
            }

            Map<Long, BigDecimal> ratios = request.getSplitRatios();
            boolean useRatios = ratios != null && !ratios.isEmpty();

            if (useRatios) {
                BigDecimal totalRatio = ratios.values().stream()
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                if (totalRatio.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new InvalidInputException("比例總和必須大於 0");
                }

                BigDecimal totalAmount = request.getAmount();
                BigDecimal runningTotal = BigDecimal.ZERO;
                List<Traveler> sortedTravelers = new ArrayList<>(travelers);

                for (int i = 0; i < sortedTravelers.size(); i++) {
                    Traveler t = sortedTravelers.get(i);
                    BigDecimal ratio = ratios.getOrDefault(t.getId(), BigDecimal.ONE);
                    BigDecimal share;
                    if (i == sortedTravelers.size() - 1) {
                        // 最後一人補足尾差，避免四捨五入誤差
                        share = totalAmount.subtract(runningTotal);
                    } else {
                        share = totalAmount.multiply(ratio)
                                .divide(totalRatio, 2, RoundingMode.HALF_UP);
                        runningTotal = runningTotal.add(share);
                    }
                    expense.getSharings().add(new ExpenseSharing(expense, t, share));
                }
            } else {
                BigDecimal sharePerPerson = request.getAmount()
                        .divide(BigDecimal.valueOf(travelers.size()), 2, RoundingMode.HALF_UP);
                for (Traveler t : travelers) {
                    expense.getSharings().add(new ExpenseSharing(expense, t, sharePerPerson));
                }
            }
        }

        budget.getExpenses().add(expense);
        Budget saved = budgetRepository.save(budget);
        return new BudgetSummaryResponse(saved, travelerId);
    }

    @Transactional(readOnly = true)
    public SettlementResponse getSettlement(Long tripId) {
        tripService.findTripById(tripId);
        Budget budget = findBudgetByTripId(tripId);

        // 收集所有出現的旅客：付款人 + 分攤對象
        Map<Long, String> travelerNames = new HashMap<>();
        Map<Long, BigDecimal> totalPaid = new HashMap<>();
        Map<Long, BigDecimal> totalOwed = new HashMap<>();

        for (Expense expense : budget.getExpenses()) {
            if (expense.getPaidByTraveler() != null) {
                Traveler payer = expense.getPaidByTraveler();
                travelerNames.put(payer.getId(), payer.getName());
                totalPaid.merge(payer.getId(), expense.getAmount(), BigDecimal::add);
            }
            for (ExpenseSharing sharing : expense.getSharings()) {
                Traveler t = sharing.getTraveler();
                travelerNames.put(t.getId(), t.getName());
                totalOwed.merge(t.getId(), sharing.getAmountPerPerson(), BigDecimal::add);
            }
        }

        // 建立餘額清單
        List<TravelerBalance> balances = new ArrayList<>();
        for (Long id : travelerNames.keySet()) {
            BigDecimal paid = totalPaid.getOrDefault(id, BigDecimal.ZERO);
            BigDecimal owed = totalOwed.getOrDefault(id, BigDecimal.ZERO);
            balances.add(new TravelerBalance(id, travelerNames.get(id), paid, owed));
        }

        // Greedy min-cash-flow：算出最少轉帳筆數
        List<TransferItem> transfers = computeTransfers(balances, travelerNames);

        return new SettlementResponse(balances, transfers);
    }

    private List<TransferItem> computeTransfers(List<TravelerBalance> balances,
                                                Map<Long, String> travelerNames) {
        // balance > 0：別人欠他（債主）；balance < 0：他欠別人（債務人）
        LinkedList<TravelerBalance> creditors = new LinkedList<>(
                balances.stream()
                        .filter(b -> b.getBalance().compareTo(BigDecimal.ZERO) > 0)
                        .sorted(Comparator.comparing(TravelerBalance::getBalance).reversed())
                        .toList());
        LinkedList<TravelerBalance> debtors = new LinkedList<>(
                balances.stream()
                        .filter(b -> b.getBalance().compareTo(BigDecimal.ZERO) < 0)
                        .sorted(Comparator.comparing(TravelerBalance::getBalance))
                        .toList());

        List<TransferItem> transfers = new ArrayList<>();
        Map<Long, BigDecimal> creditLeft = new HashMap<>();
        Map<Long, BigDecimal> debtLeft = new HashMap<>();
        creditors.forEach(c -> creditLeft.put(c.getTravelerId(), c.getBalance()));
        debtors.forEach(d -> debtLeft.put(d.getTravelerId(), d.getBalance().negate()));

        LinkedList<Long> creditorIds = new LinkedList<>(creditors.stream().map(TravelerBalance::getTravelerId).toList());
        LinkedList<Long> debtorIds = new LinkedList<>(debtors.stream().map(TravelerBalance::getTravelerId).toList());

        while (!creditorIds.isEmpty() && !debtorIds.isEmpty()) {
            Long creditorId = creditorIds.peek();
            Long debtorId = debtorIds.peek();
            BigDecimal credit = creditLeft.get(creditorId);
            BigDecimal debt = debtLeft.get(debtorId);

            BigDecimal transfer = credit.min(debt);
            transfers.add(new TransferItem(
                    debtorId, travelerNames.get(debtorId),
                    creditorId, travelerNames.get(creditorId),
                    transfer.setScale(2, RoundingMode.HALF_UP)
            ));

            credit = credit.subtract(transfer);
            debt = debt.subtract(transfer);
            creditLeft.put(creditorId, credit);
            debtLeft.put(debtorId, debt);

            if (credit.compareTo(BigDecimal.ZERO) == 0) creditorIds.poll();
            if (debt.compareTo(BigDecimal.ZERO) == 0) debtorIds.poll();
        }

        return transfers;
    }

    Budget findBudgetByTripId(Long tripId) {
        return budgetRepository.findByTripId(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("尚未設定預算，行程 ID: " + tripId));
    }
}
