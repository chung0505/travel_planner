package com.travel.planner.service;

import com.travel.planner.dto.request.AddExpenseRequest;
import com.travel.planner.dto.request.SetBudgetRequest;
import com.travel.planner.dto.response.BudgetSummaryResponse;
import com.travel.planner.exception.ResourceNotFoundException;
import com.travel.planner.model.Budget;
import com.travel.planner.model.Traveler;
import com.travel.planner.model.Trip;
import com.travel.planner.model.enums.ExpenseType;
import com.travel.planner.repository.BudgetRepository;
import com.travel.planner.repository.TravelerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BudgetService")
class BudgetServiceTest {

    @Mock private BudgetRepository budgetRepository;
    @Mock private TripService tripService;
    @Mock private TravelerRepository travelerRepository;

    @InjectMocks
    private BudgetService budgetService;

    private static final Long VIEWER_ID = 1L;

    private Trip trip;

    @BeforeEach
    void setUp() {
        trip = new Trip("東京之旅", "東京",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 5), 3);
        ReflectionTestUtils.setField(trip, "id", 1L);
    }

    // ── setBudget ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("setBudget")
    class SetBudget {

        @Test
        @DisplayName("行程無預算時新建 Budget 並回傳摘要（含幣別）")
        void createsBudget_whenNoneExists() {
            SetBudgetRequest request = new SetBudgetRequest();
            request.setTotalBudget(new BigDecimal("50000"));
            request.setCurrency("TWD");

            when(tripService.findTripById(1L)).thenReturn(trip);
            when(budgetRepository.findByTripId(1L)).thenReturn(Optional.empty());
            when(budgetRepository.save(any(Budget.class))).thenAnswer(inv -> {
                Budget b = inv.getArgument(0);
                ReflectionTestUtils.setField(b, "id", 10L);
                return b;
            });

            BudgetSummaryResponse response = budgetService.setBudget(1L, request);

            assertThat(response.getTotalBudget()).isEqualByComparingTo("50000");
            assertThat(response.getCurrency()).isEqualTo("TWD");
            assertThat(response.getTotalSpent()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(response.getRemainingBudget()).isEqualByComparingTo("50000");
            assertThat(response.isOverBudget()).isFalse();
            verify(budgetRepository).save(any(Budget.class));
        }

        @Test
        @DisplayName("行程已有預算時更新 totalBudget 與 currency")
        void updatesBudget_whenAlreadyExists() {
            Budget existingBudget = new Budget(trip, new BigDecimal("30000"), "TWD");
            ReflectionTestUtils.setField(existingBudget, "id", 10L);

            SetBudgetRequest request = new SetBudgetRequest();
            request.setTotalBudget(new BigDecimal("60000"));
            request.setCurrency("JPY");

            when(tripService.findTripById(1L)).thenReturn(trip);
            when(budgetRepository.findByTripId(1L)).thenReturn(Optional.of(existingBudget));
            when(budgetRepository.save(any(Budget.class))).thenAnswer(inv -> inv.getArgument(0));

            BudgetSummaryResponse response = budgetService.setBudget(1L, request);

            assertThat(response.getTotalBudget()).isEqualByComparingTo("60000");
            assertThat(response.getCurrency()).isEqualTo("JPY");
        }

        @Test
        @DisplayName("行程不存在時拋出 ResourceNotFoundException")
        void throwsNotFound_whenTripMissing() {
            when(tripService.findTripById(99L))
                    .thenThrow(new ResourceNotFoundException("找不到行程 ID: 99"));

            SetBudgetRequest request = new SetBudgetRequest();
            request.setTotalBudget(new BigDecimal("50000"));
            request.setCurrency("TWD");

            assertThatThrownBy(() -> budgetService.setBudget(99L, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── getBudget ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getBudget")
    class GetBudget {

        @Test
        @DisplayName("回傳預算摘要，包含幣別、已花費與剩餘金額")
        void returnsBudgetSummary_whenExists() {
            Budget budget = new Budget(trip, new BigDecimal("50000"), "TWD");
            ReflectionTestUtils.setField(budget, "id", 10L);

            when(tripService.findTripById(1L)).thenReturn(trip);
            when(budgetRepository.findByTripId(1L)).thenReturn(Optional.of(budget));

            BudgetSummaryResponse response = budgetService.getBudget(1L, VIEWER_ID);

            assertThat(response.getTotalBudget()).isEqualByComparingTo("50000");
            assertThat(response.getCurrency()).isEqualTo("TWD");
            assertThat(response.getTotalSpent()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(response.getExpenses()).isEmpty();
        }

        @Test
        @DisplayName("尚未設定預算時拋出 ResourceNotFoundException")
        void throwsNotFound_whenBudgetNotSet() {
            when(tripService.findTripById(1L)).thenReturn(trip);
            when(budgetRepository.findByTripId(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> budgetService.getBudget(1L, VIEWER_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("行程不存在時拋出 ResourceNotFoundException")
        void throwsNotFound_whenTripMissing() {
            when(tripService.findTripById(99L))
                    .thenThrow(new ResourceNotFoundException("找不到行程 ID: 99"));

            assertThatThrownBy(() -> budgetService.getBudget(99L, VIEWER_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── addExpense ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("addExpense")
    class AddExpense {

        private AddExpenseRequest buildRequest(String amount, ExpenseType type) {
            AddExpenseRequest req = new AddExpenseRequest();
            req.setExpenseType(type);
            req.setAmount(new BigDecimal(amount));
            req.setOriginalAmount(new BigDecimal(amount));
            req.setDate(LocalDate.of(2026, 7, 2));
            req.setNote("測試備註");
            req.setCurrency("TWD");
            req.setExchangeRate(BigDecimal.ONE);
            req.setPaidBy("王小明");
            return req;
        }

        @Test
        @DisplayName("新增費用後，totalSpent 與 remainingBudget 正確更新")
        void addsExpense_andUpdatesSummary() {
            Budget budget = new Budget(trip, new BigDecimal("50000"), "TWD");
            ReflectionTestUtils.setField(budget, "id", 10L);

            when(tripService.findTripById(1L)).thenReturn(trip);
            when(budgetRepository.findByTripId(1L)).thenReturn(Optional.of(budget));
            when(budgetRepository.save(any(Budget.class))).thenAnswer(inv -> inv.getArgument(0));

            BudgetSummaryResponse response = budgetService.addExpense(1L,
                    buildRequest("3000", ExpenseType.ACCOMMODATION), VIEWER_ID);

            assertThat(response.getTotalSpent()).isEqualByComparingTo("3000");
            assertThat(response.getRemainingBudget()).isEqualByComparingTo("47000");
            assertThat(response.getExpenses()).hasSize(1);
            assertThat(response.getExpenses().get(0).getExpenseType()).isEqualTo(ExpenseType.ACCOMMODATION);
            assertThat(response.getExpenses().get(0).getCurrency()).isEqualTo("TWD");
            assertThat(response.getExpenses().get(0).getPaidBy()).isEqualTo("王小明");
        }

        @Test
        @DisplayName("有分攤名單時，每人金額正確計算並建立 ExpenseSharing")
        void createsExpenseSharing_whenSplitIdsProvided() {
            Budget budget = new Budget(trip, new BigDecimal("50000"), "TWD");
            ReflectionTestUtils.setField(budget, "id", 10L);

            Traveler t1 = new Traveler("Alice", "alice@test.com", "hash");
            Traveler t2 = new Traveler("Bob", "bob@test.com", "hash");
            ReflectionTestUtils.setField(t1, "id", 101L);
            ReflectionTestUtils.setField(t2, "id", 102L);

            AddExpenseRequest req = buildRequest("3000", ExpenseType.FOOD);
            req.setSplitAmongTravelerIds(List.of(101L, 102L));

            when(tripService.findTripById(1L)).thenReturn(trip);
            when(budgetRepository.findByTripId(1L)).thenReturn(Optional.of(budget));
            when(travelerRepository.findAllById(List.of(101L, 102L))).thenReturn(List.of(t1, t2));
            when(budgetRepository.save(any(Budget.class))).thenAnswer(inv -> inv.getArgument(0));

            BudgetSummaryResponse response = budgetService.addExpense(1L, req, VIEWER_ID);

            assertThat(response.getExpenses().get(0).getSharings()).hasSize(2);
            assertThat(response.getExpenses().get(0).getSharings().get(0).getAmountPerPerson())
                    .isEqualByComparingTo("1500.00");
        }

        @Test
        @DisplayName("paidByTravelerId 存在時，正確設定付款旅客")
        void setsPaidByTraveler_whenIdProvided() {
            Budget budget = new Budget(trip, new BigDecimal("50000"), "TWD");
            ReflectionTestUtils.setField(budget, "id", 10L);

            Traveler payer = new Traveler("Alice", "alice@test.com", "hash");
            ReflectionTestUtils.setField(payer, "id", 101L);

            AddExpenseRequest req = buildRequest("2000", ExpenseType.TRANSPORTATION);
            req.setPaidByTravelerId(101L);

            when(tripService.findTripById(1L)).thenReturn(trip);
            when(budgetRepository.findByTripId(1L)).thenReturn(Optional.of(budget));
            when(travelerRepository.findById(101L)).thenReturn(Optional.of(payer));
            when(budgetRepository.save(any(Budget.class))).thenAnswer(inv -> inv.getArgument(0));

            BudgetSummaryResponse response = budgetService.addExpense(1L, req, VIEWER_ID);

            assertThat(response.getExpenses().get(0).getPaidByTravelerId()).isEqualTo(101L);
        }

        @Test
        @DisplayName("paidByTravelerId 不存在時拋出 ResourceNotFoundException")
        void throwsNotFound_whenPaidByTravelerMissing() {
            Budget budget = new Budget(trip, new BigDecimal("50000"), "TWD");
            ReflectionTestUtils.setField(budget, "id", 10L);

            AddExpenseRequest req = buildRequest("2000", ExpenseType.TRANSPORTATION);
            req.setPaidByTravelerId(999L);

            when(tripService.findTripById(1L)).thenReturn(trip);
            when(budgetRepository.findByTripId(1L)).thenReturn(Optional.of(budget));
            when(travelerRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> budgetService.addExpense(1L, req, VIEWER_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("總花費超出預算時，isOverBudget 為 true")
        void isOverBudget_true_whenTotalExceedsBudget() {
            Budget budget = new Budget(trip, new BigDecimal("1000"), "TWD");
            ReflectionTestUtils.setField(budget, "id", 10L);

            when(tripService.findTripById(1L)).thenReturn(trip);
            when(budgetRepository.findByTripId(1L)).thenReturn(Optional.of(budget));
            when(budgetRepository.save(any(Budget.class))).thenAnswer(inv -> inv.getArgument(0));

            BudgetSummaryResponse response = budgetService.addExpense(1L,
                    buildRequest("5000", ExpenseType.TICKET), VIEWER_ID);

            assertThat(response.isOverBudget()).isTrue();
            assertThat(response.getRemainingBudget()).isEqualByComparingTo("-4000");
        }

        @Test
        @DisplayName("分攤名單中有不存在的旅客 ID 時拋出 ResourceNotFoundException")
        void throwsNotFound_whenSplitTravelerNotFound() {
            Budget budget = new Budget(trip, new BigDecimal("50000"), "TWD");
            ReflectionTestUtils.setField(budget, "id", 10L);

            Traveler t1 = new Traveler("Alice", "alice@test.com", "hash");
            ReflectionTestUtils.setField(t1, "id", 101L);

            AddExpenseRequest req = buildRequest("3000", ExpenseType.FOOD);
            req.setSplitAmongTravelerIds(List.of(101L, 999L));

            when(tripService.findTripById(1L)).thenReturn(trip);
            when(budgetRepository.findByTripId(1L)).thenReturn(Optional.of(budget));
            when(travelerRepository.findAllById(List.of(101L, 999L))).thenReturn(List.of(t1));

            assertThatThrownBy(() -> budgetService.addExpense(1L, req, VIEWER_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("尚未設定預算時拋出 ResourceNotFoundException")
        void throwsNotFound_whenBudgetNotSet() {
            when(tripService.findTripById(1L)).thenReturn(trip);
            when(budgetRepository.findByTripId(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> budgetService.addExpense(1L, buildRequest("3000", ExpenseType.FOOD), VIEWER_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
