package com.travel.planner.model;

import com.travel.planner.exception.InvalidInputException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Trip")
class TripTest {

    private static final String DESTINATION = "東京";
    private static final LocalDate DEPARTURE = LocalDate.of(2026, 7, 1);
    private static final LocalDate RETURN = LocalDate.of(2026, 7, 5);
    private static final int COMPANION_COUNT = 3;

    private Trip trip;

    @BeforeEach
    void setUp() {
        trip = new Trip("東京之旅", DESTINATION, DEPARTURE, RETURN, COMPANION_COUNT);
    }

    @Nested
    @DisplayName("建構子")
    class Constructor {

        @Test
        @DisplayName("正確設定行程名稱")
        void setsTitle() {
            assertThat(trip.getTitle()).isEqualTo("東京之旅");
        }

        @Test
        @DisplayName("正確設定目的地")
        void setsDestination() {
            assertThat(trip.getDestination()).isEqualTo(DESTINATION);
        }

        @Test
        @DisplayName("正確設定出發日期")
        void setsDepartureDate() {
            assertThat(trip.getDepartureDate()).isEqualTo(DEPARTURE);
        }

        @Test
        @DisplayName("正確設定回程日期")
        void setsReturnDate() {
            assertThat(trip.getReturnDate()).isEqualTo(RETURN);
        }

        @Test
        @DisplayName("正確設定旅伴人數")
        void setsCompanionCount() {
            assertThat(trip.getCompanionCount()).isEqualTo(COMPANION_COUNT);
        }

        @Test
        @DisplayName("id 初始為 null（由 JPA 產生）")
        void idIsNullBeforePersistence() {
            assertThat(trip.getId()).isNull();
        }

        @Test
        @DisplayName("dailyPlans 初始為空 list")
        void dailyPlansInitiallyEmpty() {
            assertThat(trip.getDailyPlans()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("routes 初始為空 list")
        void routesInitiallyEmpty() {
            assertThat(trip.getRoutes()).isNotNull().isEmpty();
        }
    }

    @Nested
    @DisplayName("Setter")
    class Setters {

        @Test
        @DisplayName("setDestination 更新目的地")
        void setDestination() {
            trip.setDestination("大阪");
            assertThat(trip.getDestination()).isEqualTo("大阪");
        }

        @Test
        @DisplayName("setDepartureDate 更新出發日期")
        void setDepartureDate() {
            LocalDate newDate = LocalDate.of(2026, 8, 1);
            trip.setDepartureDate(newDate);
            assertThat(trip.getDepartureDate()).isEqualTo(newDate);
        }

        @Test
        @DisplayName("setReturnDate 更新回程日期")
        void setReturnDate() {
            LocalDate newDate = LocalDate.of(2026, 8, 10);
            trip.setReturnDate(newDate);
            assertThat(trip.getReturnDate()).isEqualTo(newDate);
        }

        @Test
        @DisplayName("setCompanionCount 更新旅伴人數")
        void setCompanionCount() {
            trip.setCompanionCount(5);
            assertThat(trip.getCompanionCount()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("validateDates")
    class ValidateDates {

        @Test
        @DisplayName("回程日期晚於出發日期時不拋出例外")
        void doesNotThrow_whenReturnAfterDeparture() {
            Trip.validateDates(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 5));
        }

        @Test
        @DisplayName("回程日期與出發日期相同時拋出 InvalidInputException")
        void throwsInvalidInput_whenReturnSameAsDeparture() {
            LocalDate same = LocalDate.of(2026, 7, 1);
            assertThatThrownBy(() -> Trip.validateDates(same, same))
                    .isInstanceOf(InvalidInputException.class)
                    .hasMessageContaining("回程日期必須晚於出發日期");
        }

        @Test
        @DisplayName("回程日期早於出發日期時拋出 InvalidInputException")
        void throwsInvalidInput_whenReturnBeforeDeparture() {
            assertThatThrownBy(() -> Trip.validateDates(
                    LocalDate.of(2026, 7, 5), LocalDate.of(2026, 7, 1)))
                    .isInstanceOf(InvalidInputException.class);
        }
    }

    @Nested
    @DisplayName("generateDailyPlans")
    class GenerateDailyPlans {

        @Test
        @DisplayName("5 天行程產生 5 個每日行程")
        void generatesFiveDailyPlans_forFiveDayTrip() {
            trip.generateDailyPlans();
            assertThat(trip.getDailyPlans()).hasSize(5);
        }

        @Test
        @DisplayName("每日行程的日期從出發日依序遞增")
        void dailyPlanDates_areSequentialFromDeparture() {
            trip.generateDailyPlans();
            List<DailyPlan> plans = trip.getDailyPlans();
            assertThat(plans.get(0).getDate()).isEqualTo(DEPARTURE);
            assertThat(plans.get(1).getDate()).isEqualTo(DEPARTURE.plusDays(1));
            assertThat(plans.get(4).getDate()).isEqualTo(DEPARTURE.plusDays(4));
        }

        @Test
        @DisplayName("dayNumber 從 1 開始遞增")
        void dayNumbers_startAtOneAndIncrement() {
            trip.generateDailyPlans();
            List<DailyPlan> plans = trip.getDailyPlans();
            assertThat(plans.get(0).getDayNumber()).isEqualTo(1);
            assertThat(plans.get(4).getDayNumber()).isEqualTo(5);
        }

        @Test
        @DisplayName("出發日與回程日相同時只產生 1 個每日行程")
        void generatesOneDailyPlan_whenSameDayTrip() {
            Trip oneDayTrip = new Trip("一日遊", "東京",
                    LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 1), 1);
            oneDayTrip.generateDailyPlans();
            assertThat(oneDayTrip.getDailyPlans()).hasSize(1);
        }

        @Test
        @DisplayName("每個 DailyPlan 正確關聯此 Trip")
        void eachDailyPlan_isLinkedToTrip() {
            trip.generateDailyPlans();
            assertThat(trip.getDailyPlans()).allMatch(p -> p.getTrip() == trip);
        }
    }

    @Nested
    @DisplayName("dailyPlans 集合操作")
    class DailyPlansCollection {

        @Test
        @DisplayName("可新增 DailyPlan 至集合")
        void canAddDailyPlan() {
            DailyPlan plan = new DailyPlan(trip, DEPARTURE, 1);
            trip.getDailyPlans().add(plan);
            assertThat(trip.getDailyPlans()).hasSize(1).contains(plan);
        }

        @Test
        @DisplayName("可新增多個 DailyPlan")
        void canAddMultipleDailyPlans() {
            trip.getDailyPlans().add(new DailyPlan(trip, DEPARTURE, 1));
            trip.getDailyPlans().add(new DailyPlan(trip, DEPARTURE.plusDays(1), 2));
            trip.getDailyPlans().add(new DailyPlan(trip, DEPARTURE.plusDays(2), 3));
            assertThat(trip.getDailyPlans()).hasSize(3);
        }

        @Test
        @DisplayName("可從集合中移除 DailyPlan")
        void canRemoveDailyPlan() {
            DailyPlan plan = new DailyPlan(trip, DEPARTURE, 1);
            trip.getDailyPlans().add(plan);
            trip.getDailyPlans().remove(plan);
            assertThat(trip.getDailyPlans()).isEmpty();
        }
    }
}
