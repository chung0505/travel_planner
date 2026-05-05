package com.travel.planner.model;

import com.travel.planner.model.enums.TransportationMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Route")
class RouteTest {

    private Trip trip;
    private List<Long> attractionIds;

    @BeforeEach
    void setUp() {
        trip = new Trip("東京之旅", "東京", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 3), 2);
        attractionIds = List.of(1L, 2L, 3L);
    }

    @Nested
    @DisplayName("建構子")
    class Constructor {

        @Test
        @DisplayName("正確關聯所屬 Trip")
        void setsTrip() {
            Route route = new Route(trip, attractionIds, TransportationMethod.WALKING, 60, BigDecimal.ZERO);
            assertThat(route.getTrip()).isSameAs(trip);
        }

        @Test
        @DisplayName("正確設定景點 ID 清單")
        void setsAttractionIds() {
            Route route = new Route(trip, attractionIds, TransportationMethod.WALKING, 60, BigDecimal.ZERO);
            assertThat(route.getAttractionIds()).containsExactly(1L, 2L, 3L);
        }

        @Test
        @DisplayName("正確設定交通方式")
        void setsTransportationMethod() {
            Route route = new Route(trip, attractionIds, TransportationMethod.PUBLIC_TRANSIT, 30, new BigDecimal("30"));
            assertThat(route.getTransportationMethod()).isEqualTo(TransportationMethod.PUBLIC_TRANSIT);
        }

        @Test
        @DisplayName("正確設定預估時間（分鐘）")
        void setsEstimatedDurationMinutes() {
            Route route = new Route(trip, attractionIds, TransportationMethod.TAXI, 20, new BigDecimal("150"));
            assertThat(route.getEstimatedDurationMinutes()).isEqualTo(20);
        }

        @Test
        @DisplayName("正確設定預估費用")
        void setsEstimatedCost() {
            BigDecimal cost = new BigDecimal("250.00");
            Route route = new Route(trip, attractionIds, TransportationMethod.TAXI, 20, cost);
            assertThat(route.getEstimatedCost()).isEqualByComparingTo(cost);
        }

        @Test
        @DisplayName("confirmed 預設為 false")
        void confirmedDefaultsFalse() {
            Route route = new Route(trip, attractionIds, TransportationMethod.WALKING, 60, BigDecimal.ZERO);
            assertThat(route.isConfirmed()).isFalse();
        }

        @Test
        @DisplayName("id 初始為 null")
        void idIsNullBeforePersistence() {
            Route route = new Route(trip, attractionIds, TransportationMethod.WALKING, 60, BigDecimal.ZERO);
            assertThat(route.getId()).isNull();
        }
    }

    @Nested
    @DisplayName("confirm")
    class Confirm {

        @Test
        @DisplayName("confirm() 將路線標記為已確認")
        void confirmSetsConfirmedTrue() {
            Route route = new Route(trip, attractionIds, TransportationMethod.WALKING, 60, BigDecimal.ZERO);
            route.confirm("[[25.0,121.0]]");
            assertThat(route.isConfirmed()).isTrue();
        }

        @Test
        @DisplayName("confirm() 同時儲存路線幾何 JSON")
        void confirmStoresGeometryJson() {
            Route route = new Route(trip, attractionIds, TransportationMethod.WALKING, 60, BigDecimal.ZERO);
            String geometry = "[[25.0,121.0],[25.1,121.1]]";
            route.confirm(geometry);
            assertThat(route.getGeometryJson()).isEqualTo(geometry);
        }

        @Test
        @DisplayName("confirm() 前 confirmed 預設為 false")
        void confirmedDefaultsFalseBeforeConfirm() {
            Route route = new Route(trip, attractionIds, TransportationMethod.WALKING, 60, BigDecimal.ZERO);
            assertThat(route.isConfirmed()).isFalse();
        }
    }

    @Nested
    @DisplayName("TransportationMethod 枚舉")
    class TransportationMethodEnum {

        @ParameterizedTest(name = "TransportationMethod.{0} 可正常指定")
        @EnumSource(TransportationMethod.class)
        @DisplayName("所有交通方式皆可建立路線")
        void allTransportationMethodsAreValid(TransportationMethod method) {
            Route route = new Route(trip, attractionIds, method, 30, new BigDecimal("50"));
            assertThat(route.getTransportationMethod()).isEqualTo(method);
        }

        @Test
        @DisplayName("TransportationMethod 共有四種")
        void hasThreeMethods() {
            assertThat(TransportationMethod.values()).hasSize(3);
        }

        @Test
        @DisplayName("包含 WALKING、PUBLIC_TRANSIT、TAXI")
        void containsExpectedMethods() {
            assertThat(TransportationMethod.values()).containsExactlyInAnyOrder(
                    TransportationMethod.WALKING,
                    TransportationMethod.PUBLIC_TRANSIT,
                    TransportationMethod.TAXI
            );
        }
    }

    @Nested
    @DisplayName("calculateCost")
    class CalculateCost {

        @Test
        @DisplayName("步行費用為 0")
        void walking_isZeroCost() {
            BigDecimal cost = Route.calculateCost(TransportationMethod.WALKING, 5000);
            assertThat(cost).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("大眾運輸費用為固定 30 元")
        void publicTransit_isFixedThirtyNtd() {
            BigDecimal cost = Route.calculateCost(TransportationMethod.PUBLIC_TRANSIT, 10000);
            assertThat(cost).isEqualByComparingTo(new BigDecimal("30"));
        }

        @Test
        @DisplayName("計程車距離在起跳範圍內（≤ 1.25 km）費用為 85 元")
        void taxi_withinFlagFall_isEightyFiveNtd() {
            BigDecimal cost = Route.calculateCost(TransportationMethod.TAXI, 1000);
            assertThat(cost).isEqualByComparingTo(new BigDecimal("85"));
        }

        @Test
        @DisplayName("計程車超過起跳距離後每 200m 加 5 元")
        void taxi_beyondFlagFall_addsPerDistanceFare() {
            // 1.25 km 起跳後 200m → 85 + 5 = 90
            BigDecimal cost = Route.calculateCost(TransportationMethod.TAXI, 1450);
            assertThat(cost).isEqualByComparingTo(new BigDecimal("90"));
        }

        @Test
        @DisplayName("計程車長距離費用正確累加")
        void taxi_longDistance_accumulatesFareCorrectly() {
            // 起跳 1.25km + 1km = 1.25 + 5 段 × 200m = 85 + 25 = 110
            BigDecimal cost = Route.calculateCost(TransportationMethod.TAXI, 2250);
            assertThat(cost).isEqualByComparingTo(new BigDecimal("110"));
        }

        @Test
        @DisplayName("步行不論距離長短費用皆為 0")
        void walking_anyDistance_isAlwaysZero() {
            assertThat(Route.calculateCost(TransportationMethod.WALKING, 0))
                    .isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(Route.calculateCost(TransportationMethod.WALKING, 50000))
                    .isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("attractionIds 清單語意")
    class AttractionIdsSemantics {

        @Test
        @DisplayName("景點順序與輸入順序相同（路線有方向性）")
        void preservesAttractionOrder() {
            List<Long> ordered = List.of(3L, 1L, 2L);
            Route route = new Route(trip, ordered, TransportationMethod.WALKING, 60, BigDecimal.ZERO);
            assertThat(route.getAttractionIds()).containsExactly(3L, 1L, 2L);
        }

        @Test
        @DisplayName("兩個景點可建立路線（最小有效路線）")
        void twoAttractionsIsMinimumValidRoute() {
            Route route = new Route(trip, List.of(1L, 2L), TransportationMethod.WALKING, 60, BigDecimal.ZERO);
            assertThat(route.getAttractionIds()).hasSize(2);
        }

        @Test
        @DisplayName("費用為零（步行）可正常建立")
        void zeroCostIsValid() {
            Route route = new Route(trip, List.of(1L, 2L), TransportationMethod.WALKING, 60, BigDecimal.ZERO);
            assertThat(route.getEstimatedCost()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }
}
