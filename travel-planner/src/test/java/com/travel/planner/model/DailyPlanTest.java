package com.travel.planner.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DailyPlan")
class DailyPlanTest {

    private static final LocalDate DATE = LocalDate.of(2026, 7, 1);
    private static final int DAY_NUMBER = 1;

    private Trip trip;
    private DailyPlan dailyPlan;

    @BeforeEach
    void setUp() {
        trip = new Trip("東京之旅", "東京", DATE, DATE.plusDays(2), 2);
        dailyPlan = new DailyPlan(trip, DATE, DAY_NUMBER);
    }

    @Nested
    @DisplayName("建構子")
    class Constructor {

        @Test
        @DisplayName("正確關聯所屬 Trip")
        void setsTrip() {
            assertThat(dailyPlan.getTrip()).isSameAs(trip);
        }

        @Test
        @DisplayName("正確設定日期")
        void setsDate() {
            assertThat(dailyPlan.getDate()).isEqualTo(DATE);
        }

        @Test
        @DisplayName("正確設定第幾天編號")
        void setsDayNumber() {
            assertThat(dailyPlan.getDayNumber()).isEqualTo(DAY_NUMBER);
        }

        @Test
        @DisplayName("id 初始為 null")
        void idIsNullBeforePersistence() {
            assertThat(dailyPlan.getId()).isNull();
        }

        @Test
        @DisplayName("attractions 初始為空 list")
        void attractionsInitiallyEmpty() {
            assertThat(dailyPlan.getAttractions()).isNotNull().isEmpty();
        }
    }

    @Nested
    @DisplayName("attractions 集合操作")
    class AttractionsCollection {

        @Test
        @DisplayName("可新增景點至當日行程")
        void canAddAttraction() {
            Attraction a = new Attraction(dailyPlan, "淺草寺", "台東區淺草", LocalTime.of(9, 0), LocalTime.of(11, 0));
            dailyPlan.getAttractions().add(a);
            assertThat(dailyPlan.getAttractions()).hasSize(1).contains(a);
        }

        @Test
        @DisplayName("可新增多個景點")
        void canAddMultipleAttractions() {
            dailyPlan.getAttractions().add(new Attraction(dailyPlan, "景點A", "地址A", LocalTime.of(9, 0), LocalTime.of(10, 0)));
            dailyPlan.getAttractions().add(new Attraction(dailyPlan, "景點B", "地址B", LocalTime.of(11, 0), LocalTime.of(12, 0)));
            assertThat(dailyPlan.getAttractions()).hasSize(2);
        }

        @Test
        @DisplayName("可從集合中移除景點")
        void canRemoveAttraction() {
            Attraction a = new Attraction(dailyPlan, "淺草寺", "台東區淺草", LocalTime.of(9, 0), LocalTime.of(11, 0));
            dailyPlan.getAttractions().add(a);
            dailyPlan.getAttractions().remove(a);
            assertThat(dailyPlan.getAttractions()).isEmpty();
        }

        @Test
        @DisplayName("不同日期的 DailyPlan 彼此獨立")
        void differentDailyPlansAreIndependent() {
            DailyPlan day2 = new DailyPlan(trip, DATE.plusDays(1), 2);
            Attraction a1 = new Attraction(dailyPlan, "景點A", "地址A", LocalTime.of(9, 0), LocalTime.of(10, 0));
            Attraction a2 = new Attraction(day2, "景點B", "地址B", LocalTime.of(9, 0), LocalTime.of(10, 0));

            dailyPlan.getAttractions().add(a1);
            day2.getAttractions().add(a2);

            assertThat(dailyPlan.getAttractions()).containsOnly(a1);
            assertThat(day2.getAttractions()).containsOnly(a2);
        }
    }

    @Nested
    @DisplayName("與 Trip 的關聯")
    class TripRelationship {

        @Test
        @DisplayName("dayNumber 從 1 開始遞增")
        void dayNumberStartsAtOne() {
            DailyPlan day1 = new DailyPlan(trip, DATE, 1);
            DailyPlan day2 = new DailyPlan(trip, DATE.plusDays(1), 2);
            DailyPlan day3 = new DailyPlan(trip, DATE.plusDays(2), 3);

            assertThat(day1.getDayNumber()).isEqualTo(1);
            assertThat(day2.getDayNumber()).isEqualTo(2);
            assertThat(day3.getDayNumber()).isEqualTo(3);
        }

        @Test
        @DisplayName("各 DailyPlan 的日期與 dayNumber 一致")
        void dateCorrespondsWithDayNumber() {
            DailyPlan day2 = new DailyPlan(trip, DATE.plusDays(1), 2);
            assertThat(day2.getDate()).isEqualTo(DATE.plusDays(1));
            assertThat(day2.getDayNumber()).isEqualTo(2);
        }
    }
}
