package com.travel.planner.model;

import com.travel.planner.exception.InvalidInputException;
import com.travel.planner.exception.TimeConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    @DisplayName("validateAttractionTimes")
    class ValidateAttractionTimes {

        @Test
        @DisplayName("結束時間晚於開始時間時不拋出例外")
        void doesNotThrow_whenEndAfterStart() {
            DailyPlan.validateAttractionTimes(LocalTime.of(9, 0), LocalTime.of(11, 0));
        }

        @Test
        @DisplayName("結束時間等於開始時間時拋出 InvalidInputException")
        void throwsInvalidInput_whenEndEqualsStart() {
            LocalTime same = LocalTime.of(9, 0);
            assertThatThrownBy(() -> DailyPlan.validateAttractionTimes(same, same))
                    .isInstanceOf(InvalidInputException.class)
                    .hasMessageContaining("結束時間必須晚於開始時間");
        }

        @Test
        @DisplayName("結束時間早於開始時間時拋出 InvalidInputException")
        void throwsInvalidInput_whenEndBeforeStart() {
            assertThatThrownBy(() -> DailyPlan.validateAttractionTimes(
                    LocalTime.of(11, 0), LocalTime.of(9, 0)))
                    .isInstanceOf(InvalidInputException.class);
        }
    }

    @Nested
    @DisplayName("checkTimeConflict")
    class CheckTimeConflict {

        @BeforeEach
        void addExistingAttraction() {
            // 既有景點：10:00 ~ 12:00
            Attraction existing = new Attraction(dailyPlan, "東京鐵塔", "地址",
                    LocalTime.of(10, 0), LocalTime.of(12, 0));
            dailyPlan.getAttractions().add(existing);
        }

        @Test
        @DisplayName("時間不重疊時不拋出例外")
        void doesNotThrow_whenNoOverlap() {
            // 13:00 ~ 15:00，在既有景點之後
            dailyPlan.checkTimeConflict(LocalTime.of(13, 0), LocalTime.of(15, 0), null);
        }

        @Test
        @DisplayName("新景點時間與既有景點部分重疊時拋出 TimeConflictException")
        void throwsTimeConflict_whenPartialOverlap() {
            // 09:00 ~ 11:00，與 10:00 ~ 12:00 重疊
            assertThatThrownBy(() ->
                    dailyPlan.checkTimeConflict(LocalTime.of(9, 0), LocalTime.of(11, 0), null))
                    .isInstanceOf(TimeConflictException.class)
                    .hasMessageContaining("東京鐵塔");
        }

        @Test
        @DisplayName("新景點時間完全包含既有景點時拋出 TimeConflictException")
        void throwsTimeConflict_whenFullyContains() {
            // 09:00 ~ 13:00，完全包含 10:00 ~ 12:00
            assertThatThrownBy(() ->
                    dailyPlan.checkTimeConflict(LocalTime.of(9, 0), LocalTime.of(13, 0), null))
                    .isInstanceOf(TimeConflictException.class);
        }

        @Test
        @DisplayName("excludeId 符合衝突景點 ID 時不拋出例外（編輯自身用）")
        void doesNotThrow_whenConflictingAttractionIsExcluded() {
            Attraction existing = dailyPlan.getAttractions().get(0);
            org.springframework.test.util.ReflectionTestUtils.setField(existing, "id", 99L);
            // 相同時間但排除自身，不應拋出
            dailyPlan.checkTimeConflict(LocalTime.of(10, 0), LocalTime.of(12, 0), 99L);
        }
    }

    @Nested
    @DisplayName("addAttraction")
    class AddAttractionDomain {

        @Test
        @DisplayName("新增景點後加入 attractions 集合")
        void addsAttractionToList() {
            Attraction result = dailyPlan.addAttraction("淺草寺", "台東區淺草",
                    LocalTime.of(9, 0), LocalTime.of(11, 0));
            assertThat(dailyPlan.getAttractions()).hasSize(1).contains(result);
        }

        @Test
        @DisplayName("回傳的 Attraction 欄位與輸入相符")
        void returnedAttraction_hasCorrectFields() {
            Attraction result = dailyPlan.addAttraction("淺草寺", "台東區淺草",
                    LocalTime.of(9, 0), LocalTime.of(11, 0));
            assertThat(result.getName()).isEqualTo("淺草寺");
            assertThat(result.getAddress()).isEqualTo("台東區淺草");
            assertThat(result.getStartTime()).isEqualTo(LocalTime.of(9, 0));
            assertThat(result.getEndTime()).isEqualTo(LocalTime.of(11, 0));
        }

        @Test
        @DisplayName("回傳的 Attraction 正確關聯此 DailyPlan")
        void returnedAttraction_isLinkedToDailyPlan() {
            Attraction result = dailyPlan.addAttraction("淺草寺", "台東區淺草",
                    LocalTime.of(9, 0), LocalTime.of(11, 0));
            assertThat(result.getDailyPlan()).isSameAs(dailyPlan);
        }

        @Test
        @DisplayName("座標初始為 null（geocoding 由 Service 負責）")
        void newAttraction_hasNullCoordinates() {
            Attraction result = dailyPlan.addAttraction("淺草寺", "台東區淺草",
                    LocalTime.of(9, 0), LocalTime.of(11, 0));
            assertThat(result.getLatitude()).isNull();
            assertThat(result.getLongitude()).isNull();
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
