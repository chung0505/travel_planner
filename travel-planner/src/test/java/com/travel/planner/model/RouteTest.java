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
        trip = new Trip("東京", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 3), 2);
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
    @DisplayName("setConfirmed")
    class SetConfirmed {

        @Test
        @DisplayName("setConfirmed(true) 將路線標記為已確認")
        void setConfirmedTrue() {
            Route route = new Route(trip, attractionIds, TransportationMethod.WALKING, 60, BigDecimal.ZERO);
            route.setConfirmed(true);
            assertThat(route.isConfirmed()).isTrue();
        }

        @Test
        @DisplayName("setConfirmed(false) 可取消確認狀態")
        void setConfirmedFalse() {
            Route route = new Route(trip, attractionIds, TransportationMethod.WALKING, 60, BigDecimal.ZERO);
            route.setConfirmed(true);
            route.setConfirmed(false);
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
        void hasFourMethods() {
            assertThat(TransportationMethod.values()).hasSize(4);
        }

        @Test
        @DisplayName("包含 WALKING、PUBLIC_TRANSIT、TAXI、SELF_DRIVING")
        void containsExpectedMethods() {
            assertThat(TransportationMethod.values()).containsExactlyInAnyOrder(
                    TransportationMethod.WALKING,
                    TransportationMethod.PUBLIC_TRANSIT,
                    TransportationMethod.TAXI,
                    TransportationMethod.SELF_DRIVING
            );
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
