package com.travel.planner.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Trip")
class TripTest {

    private static final String DESTINATION = "東京";
    private static final LocalDate DEPARTURE = LocalDate.of(2026, 7, 1);
    private static final LocalDate RETURN = LocalDate.of(2026, 7, 5);
    private static final int COMPANION_COUNT = 3;

    private Trip trip;

    @BeforeEach
    void setUp() {
        trip = new Trip(DESTINATION, DEPARTURE, RETURN, COMPANION_COUNT);
    }

    @Nested
    @DisplayName("建構子")
    class Constructor {

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
