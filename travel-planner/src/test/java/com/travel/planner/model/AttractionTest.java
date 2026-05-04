package com.travel.planner.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Attraction")
class AttractionTest {

    private static final String NAME = "淺草寺";
    private static final String ADDRESS = "東京都台東區淺草2-3-1";
    private static final LocalTime START = LocalTime.of(9, 0);
    private static final LocalTime END = LocalTime.of(11, 0);

    private DailyPlan dailyPlan;
    private Attraction attraction;

    @BeforeEach
    void setUp() {
        Trip trip = new Trip("東京之旅", "東京", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 3), 2);
        dailyPlan = new DailyPlan(trip, LocalDate.of(2026, 7, 1), 1);
        attraction = new Attraction(dailyPlan, NAME, ADDRESS, START, END);
    }

    @Nested
    @DisplayName("建構子")
    class Constructor {

        @Test
        @DisplayName("正確關聯所屬 DailyPlan")
        void setsDailyPlan() {
            assertThat(attraction.getDailyPlan()).isSameAs(dailyPlan);
        }

        @Test
        @DisplayName("正確設定景點名稱")
        void setsName() {
            assertThat(attraction.getName()).isEqualTo(NAME);
        }

        @Test
        @DisplayName("正確設定地址")
        void setsAddress() {
            assertThat(attraction.getAddress()).isEqualTo(ADDRESS);
        }

        @Test
        @DisplayName("正確設定開始時間")
        void setsStartTime() {
            assertThat(attraction.getStartTime()).isEqualTo(START);
        }

        @Test
        @DisplayName("正確設定結束時間")
        void setsEndTime() {
            assertThat(attraction.getEndTime()).isEqualTo(END);
        }

        @Test
        @DisplayName("id 初始為 null")
        void idIsNullBeforePersistence() {
            assertThat(attraction.getId()).isNull();
        }
    }

    @Nested
    @DisplayName("Setter")
    class Setters {

        @Test
        @DisplayName("setName 更新景點名稱")
        void setName() {
            attraction.setName("上野公園");
            assertThat(attraction.getName()).isEqualTo("上野公園");
        }

        @Test
        @DisplayName("setAddress 更新地址")
        void setAddress() {
            attraction.setAddress("東京都台東區上野公園");
            assertThat(attraction.getAddress()).isEqualTo("東京都台東區上野公園");
        }

        @Test
        @DisplayName("setStartTime 更新開始時間")
        void setStartTime() {
            LocalTime newTime = LocalTime.of(13, 30);
            attraction.setStartTime(newTime);
            assertThat(attraction.getStartTime()).isEqualTo(newTime);
        }

        @Test
        @DisplayName("setEndTime 更新結束時間")
        void setEndTime() {
            LocalTime newTime = LocalTime.of(15, 0);
            attraction.setEndTime(newTime);
            assertThat(attraction.getEndTime()).isEqualTo(newTime);
        }
    }

    @Nested
    @DisplayName("時間語意驗證")
    class TimeSemantics {

        @Test
        @DisplayName("結束時間在開始時間之後（正常情境）")
        void endTimeIsAfterStartTime() {
            assertThat(attraction.getEndTime()).isAfter(attraction.getStartTime());
        }

        @Test
        @DisplayName("整點時間可正確儲存")
        void handlesExactHourTimes() {
            Attraction a = new Attraction(dailyPlan, "某景點", "某地址",
                    LocalTime.of(8, 0), LocalTime.of(10, 0));
            assertThat(a.getStartTime().getMinute()).isZero();
            assertThat(a.getEndTime().getMinute()).isZero();
        }

        @Test
        @DisplayName("帶分鐘的時間可正確儲存")
        void handlesTimesWithMinutes() {
            Attraction a = new Attraction(dailyPlan, "某景點", "某地址",
                    LocalTime.of(9, 30), LocalTime.of(11, 45));
            assertThat(a.getStartTime()).isEqualTo(LocalTime.of(9, 30));
            assertThat(a.getEndTime()).isEqualTo(LocalTime.of(11, 45));
        }

        @Test
        @DisplayName("跨午夜（23:00 ~ 01:00）時間可正確儲存")
        void handlesLateNightTimes() {
            Attraction a = new Attraction(dailyPlan, "跨夜活動", "某地址",
                    LocalTime.of(23, 0), LocalTime.of(1, 0));
            assertThat(a.getStartTime()).isEqualTo(LocalTime.of(23, 0));
            assertThat(a.getEndTime()).isEqualTo(LocalTime.of(1, 0));
        }
    }
}
