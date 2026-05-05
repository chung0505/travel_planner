package com.travel.planner.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.planner.dto.request.PlanRouteRequest;
import com.travel.planner.dto.response.RouteEstimateResponse;
import com.travel.planner.dto.response.RouteResponse;
import com.travel.planner.exception.InvalidInputException;
import com.travel.planner.exception.ResourceNotFoundException;
import com.travel.planner.model.Attraction;
import com.travel.planner.model.DailyPlan;
import com.travel.planner.model.Route;
import com.travel.planner.model.Trip;
import com.travel.planner.model.enums.TransportationMethod;
import com.travel.planner.repository.AttractionRepository;
import com.travel.planner.repository.RouteRepository;
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
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RouteService")
class RouteServiceTest {

    @Mock
    private RouteRepository routeRepository;

    @Mock
    private AttractionRepository attractionRepository;

    @Mock
    private TripService tripService;

    @Mock
    private OrsService orsService;

    @InjectMocks
    private RouteService routeService;

    // ObjectMapper is a real instance (not mocked) since it's a value type
    private final ObjectMapper objectMapper = new ObjectMapper();

    private Trip trip;
    private Attraction attractionA;
    private Attraction attractionB;
    private PlanRouteRequest request;

    @BeforeEach
    void setUp() {
        // Inject the real ObjectMapper via field injection
        ReflectionTestUtils.setField(routeService, "objectMapper", objectMapper);

        trip = new Trip("東京之旅", "東京",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 5), 2);
        ReflectionTestUtils.setField(trip, "id", 1L);

        DailyPlan dailyPlan = new DailyPlan(trip, LocalDate.of(2026, 7, 1), 1);
        ReflectionTestUtils.setField(dailyPlan, "id", 10L);

        attractionA = new Attraction(dailyPlan, "淺草寺",
                "東京都台東區淺草 2-3-1", LocalTime.of(9, 0), LocalTime.of(11, 0));
        attractionA.setLatitude(35.7148);
        attractionA.setLongitude(139.7967);
        ReflectionTestUtils.setField(attractionA, "id", 101L);

        attractionB = new Attraction(dailyPlan, "東京鐵塔",
                "東京都港區芝公園 4-2-8", LocalTime.of(13, 0), LocalTime.of(15, 0));
        attractionB.setLatitude(35.6586);
        attractionB.setLongitude(139.7454);
        ReflectionTestUtils.setField(attractionB, "id", 102L);

        request = new PlanRouteRequest();
        request.setAttractionIds(List.of(101L, 102L));
        request.setTransportationMethod(TransportationMethod.WALKING);
    }

    @Nested
    @DisplayName("estimateRoute")
    class EstimateRoute {

        @Test
        @DisplayName("步行路線：正確計算分鐘數，費用為 0")
        void estimatesRoute_walking_zeroCost() {
            when(tripService.findTripById(1L)).thenReturn(trip);
            when(attractionRepository.findByTripIdOrderByDateAndTime(1L))
                    .thenReturn(List.of(attractionA, attractionB));
            OrsRouteResult orsResult = new OrsRouteResult(
                    2000, 1200,
                    List.of(), List.of(new double[]{2000, 1200}),
                    null, null
            );
            when(orsService.getDirections(any(), any(), any())).thenReturn(orsResult);

            RouteEstimateResponse response = routeService.estimateRoute(1L, request);

            assertThat(response.getTotalEstimatedMinutes()).isEqualTo(20);
            assertThat(response.getTotalEstimatedCost()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(response.getSegments()).hasSize(1);
            assertThat(response.getSegments().get(0).getFromAttraction()).isEqualTo("淺草寺");
            assertThat(response.getSegments().get(0).getToAttraction()).isEqualTo("東京鐵塔");
        }

        @Test
        @DisplayName("計程車路線：依距離正確估算費用")
        void estimatesRoute_taxi_correctFare() {
            request.setTransportationMethod(TransportationMethod.TAXI);
            when(tripService.findTripById(1L)).thenReturn(trip);
            when(attractionRepository.findByTripIdOrderByDateAndTime(1L))
                    .thenReturn(List.of(attractionA, attractionB));
            // 距離 3000m → 85 + (3 - 1.25) / 0.2 * 5 = 85 + 43.75 = 128.75 → round = 129
            OrsRouteResult orsResult = new OrsRouteResult(
                    3000, 600,
                    List.of(), List.of(new double[]{3000, 600}),
                    null, null
            );
            when(orsService.getDirections(any(), any(), any())).thenReturn(orsResult);

            RouteEstimateResponse response = routeService.estimateRoute(1L, request);

            assertThat(response.getTotalEstimatedCost()).isEqualByComparingTo(new BigDecimal("129"));
        }

        @Test
        @DisplayName("大眾運輸：使用 API 回傳的實際票價")
        void estimatesRoute_transit_usesApiFare() {
            request.setTransportationMethod(TransportationMethod.PUBLIC_TRANSIT);
            when(tripService.findTripById(1L)).thenReturn(trip);
            when(attractionRepository.findByTripIdOrderByDateAndTime(1L))
                    .thenReturn(List.of(attractionA, attractionB));
            OrsRouteResult orsResult = new OrsRouteResult(
                    5000, 900,
                    List.of(), List.of(new double[]{5000, 900}),
                    List.of(180.0), List.of(List.of())
            );
            when(orsService.getDirections(any(), any(), any())).thenReturn(orsResult);

            RouteEstimateResponse response = routeService.estimateRoute(1L, request);

            assertThat(response.getTotalEstimatedCost()).isEqualByComparingTo(new BigDecimal("180"));
        }

        @Test
        @DisplayName("景點少於兩個時拋出 InvalidInputException")
        void throwsInvalidInput_whenLessThanTwoAttractions() {
            request.setAttractionIds(List.of(101L));
            when(tripService.findTripById(1L)).thenReturn(trip);
            when(attractionRepository.findByTripIdOrderByDateAndTime(1L))
                    .thenReturn(List.of(attractionA));

            assertThatThrownBy(() -> routeService.estimateRoute(1L, request))
                    .isInstanceOf(InvalidInputException.class);
        }

        @Test
        @DisplayName("景點無座標時拋出 InvalidInputException")
        void throwsInvalidInput_whenAttractionMissingCoordinates() {
            attractionA.setLatitude(null);
            attractionA.setLongitude(null);
            when(tripService.findTripById(1L)).thenReturn(trip);
            when(attractionRepository.findByTripIdOrderByDateAndTime(1L))
                    .thenReturn(List.of(attractionA, attractionB));

            assertThatThrownBy(() -> routeService.estimateRoute(1L, request))
                    .isInstanceOf(InvalidInputException.class);
        }

        @Test
        @DisplayName("景點 ID 不屬於此行程時拋出 InvalidInputException")
        void throwsInvalidInput_whenAttractionNotBelongToTrip() {
            request.setAttractionIds(List.of(101L, 999L));
            when(tripService.findTripById(1L)).thenReturn(trip);
            // 只回傳 attractionA，999L 不存在
            when(attractionRepository.findByTripIdOrderByDateAndTime(1L))
                    .thenReturn(List.of(attractionA));

            assertThatThrownBy(() -> routeService.estimateRoute(1L, request))
                    .isInstanceOf(InvalidInputException.class);
        }
    }

    @Nested
    @DisplayName("confirmRoute")
    class ConfirmRoute {

        @Test
        @DisplayName("成功儲存路線，confirmed 為 true")
        void confirmsRoute_savesAsConfirmed() {
            when(tripService.findTripById(1L)).thenReturn(trip);
            when(attractionRepository.findByTripIdOrderByDateAndTime(1L))
                    .thenReturn(List.of(attractionA, attractionB));
            OrsRouteResult orsResult = new OrsRouteResult(
                    2000, 1200,
                    List.of(), List.of(new double[]{2000, 1200}),
                    null, null
            );
            when(orsService.getDirections(any(), any(), any())).thenReturn(orsResult);
            when(routeRepository.save(any(Route.class))).thenAnswer(inv -> {
                Route r = inv.getArgument(0);
                ReflectionTestUtils.setField(r, "id", 50L);
                return r;
            });

            RouteResponse response = routeService.confirmRoute(1L, request);

            assertThat(response.isConfirmed()).isTrue();
            assertThat(response.getTransportationMethod()).isEqualTo(TransportationMethod.WALKING);
            assertThat(response.getEstimatedDurationMinutes()).isEqualTo(20);
        }
    }

    @Nested
    @DisplayName("getRoutes")
    class GetRoutes {

        @Test
        @DisplayName("回傳行程下所有路線")
        void returnsRoutes_forTrip() {
            Route route = new Route(trip, List.of(101L, 102L),
                    TransportationMethod.WALKING, 20, BigDecimal.ZERO);
            ReflectionTestUtils.setField(route, "id", 50L);
            route.confirm("[]");

            when(tripService.findTripById(1L)).thenReturn(trip);
            when(routeRepository.findByTripId(1L)).thenReturn(List.of(route));

            List<RouteResponse> result = routeService.getRoutes(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTransportationMethod()).isEqualTo(TransportationMethod.WALKING);
        }

        @Test
        @DisplayName("無路線時回傳空清單")
        void returnsEmpty_whenNoRoutes() {
            when(tripService.findTripById(1L)).thenReturn(trip);
            when(routeRepository.findByTripId(1L)).thenReturn(List.of());

            List<RouteResponse> result = routeService.getRoutes(1L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getRoute")
    class GetRoute {

        @Test
        @DisplayName("存在的路線 ID 回傳對應路線")
        void returnsRoute_whenFound() {
            Route route = new Route(trip, List.of(101L, 102L),
                    TransportationMethod.TAXI, 15, new BigDecimal("129"));
            ReflectionTestUtils.setField(route, "id", 50L);
            route.confirm("[]");

            when(tripService.findTripById(1L)).thenReturn(trip);
            when(routeRepository.findById(50L)).thenReturn(Optional.of(route));

            RouteResponse result = routeService.getRoute(1L, 50L);

            assertThat(result.getTransportationMethod()).isEqualTo(TransportationMethod.TAXI);
        }

        @Test
        @DisplayName("路線 ID 不存在時拋出 ResourceNotFoundException")
        void throwsNotFound_whenRouteMissing() {
            when(tripService.findTripById(1L)).thenReturn(trip);
            when(routeRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> routeService.getRoute(1L, 99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("路線不屬於指定行程時拋出 ResourceNotFoundException")
        void throwsNotFound_whenRouteBelongsToDifferentTrip() {
            Trip otherTrip = new Trip("大阪之旅", "大阪",
                    LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3), 1);
            ReflectionTestUtils.setField(otherTrip, "id", 2L);

            Route route = new Route(otherTrip, List.of(101L, 102L),
                    TransportationMethod.WALKING, 20, BigDecimal.ZERO);
            ReflectionTestUtils.setField(route, "id", 50L);

            when(tripService.findTripById(1L)).thenReturn(trip);
            when(routeRepository.findById(50L)).thenReturn(Optional.of(route));

            assertThatThrownBy(() -> routeService.getRoute(1L, 50L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
