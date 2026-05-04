package com.travel.planner.service;

import com.travel.planner.dto.request.AddAttractionRequest;
import com.travel.planner.dto.response.AttractionResponse;
import com.travel.planner.dto.response.DailyPlanResponse;
import com.travel.planner.exception.InvalidInputException;
import com.travel.planner.exception.ResourceNotFoundException;
import com.travel.planner.exception.TimeConflictException;
import com.travel.planner.model.Attraction;
import com.travel.planner.model.DailyPlan;
import com.travel.planner.model.Trip;
import com.travel.planner.repository.DailyPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DailyPlanService")
class DailyPlanServiceTest {

    @Mock
    private DailyPlanRepository dailyPlanRepository;

    @Mock
    private TripService tripService;

    @Mock
    private OrsService orsService;

    @InjectMocks
    private DailyPlanService dailyPlanService;

    private Trip trip;
    private DailyPlan dailyPlan;

    @BeforeEach
    void setUp() {
        trip = new Trip("東京之旅", "東京",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 5), 2);
        ReflectionTestUtils.setField(trip, "id", 1L);

        dailyPlan = new DailyPlan(trip, LocalDate.of(2026, 7, 1), 1);
        ReflectionTestUtils.setField(dailyPlan, "id", 10L);
    }

    @Nested
    @DisplayName("getDailyPlans")
    class GetDailyPlans {

        @Test
        @DisplayName("行程存在時回傳每日行程清單")
        void returnsDailyPlans_whenTripExists() {
            DailyPlan day2 = new DailyPlan(trip, LocalDate.of(2026, 7, 2), 2);
            when(tripService.findTripById(1L)).thenReturn(trip);
            when(dailyPlanRepository.findByTripIdOrderByDateAsc(1L))
                    .thenReturn(List.of(dailyPlan, day2));

            List<DailyPlanResponse> result = dailyPlanService.getDailyPlans(1L);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getDayNumber()).isEqualTo(1);
            assertThat(result.get(1).getDayNumber()).isEqualTo(2);
        }

        @Test
        @DisplayName("行程不存在時拋出 ResourceNotFoundException")
        void throwsNotFound_whenTripMissing() {
            when(tripService.findTripById(99L))
                    .thenThrow(new ResourceNotFoundException("找不到行程 ID: 99"));

            assertThatThrownBy(() -> dailyPlanService.getDailyPlans(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getDailyPlan")
    class GetDailyPlan {

        @Test
        @DisplayName("存在的每日行程回傳對應 DailyPlanResponse")
        void returnsDailyPlan_whenFound() {
            when(dailyPlanRepository.findByIdAndTripId(10L, 1L))
                    .thenReturn(Optional.of(dailyPlan));

            DailyPlanResponse result = dailyPlanService.getDailyPlan(1L, 10L);

            assertThat(result.getId()).isEqualTo(10L);
            assertThat(result.getDate()).isEqualTo(LocalDate.of(2026, 7, 1));
        }

        @Test
        @DisplayName("每日行程不存在時拋出 ResourceNotFoundException")
        void throwsNotFound_whenDailyPlanMissing() {
            when(dailyPlanRepository.findByIdAndTripId(99L, 1L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> dailyPlanService.getDailyPlan(1L, 99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("addAttraction")
    class AddAttraction {

        private AddAttractionRequest request;

        @BeforeEach
        void setUp() {
            request = new AddAttractionRequest();
            request.setName("淺草寺");
            request.setAddress("東京都台東區淺草 2-3-1");
            request.setStartTime(LocalTime.of(9, 0));
            request.setEndTime(LocalTime.of(11, 0));
        }

        @Test
        @DisplayName("Geocoding 成功時景點帶有座標")
        void addsAttraction_withCoordinates_whenGeocodeSucceeds() {
            when(dailyPlanRepository.findByIdAndTripId(10L, 1L))
                    .thenReturn(Optional.of(dailyPlan));
            when(orsService.geocode(request.getAddress()))
                    .thenReturn(new double[]{35.7148, 139.7967});
            when(dailyPlanRepository.save(any(DailyPlan.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            DailyPlanResponse result = dailyPlanService.addAttraction(1L, 10L, request);

            assertThat(result.getAttractions()).hasSize(1);
            AttractionResponse attraction = result.getAttractions().get(0);
            assertThat(attraction.getName()).isEqualTo("淺草寺");
            assertThat(attraction.getLatitude()).isEqualTo(35.7148);
            assertThat(attraction.getLongitude()).isEqualTo(139.7967);
        }

        @Test
        @DisplayName("Geocoding 失敗時景點仍可新增（座標為 null）")
        void addsAttraction_withoutCoordinates_whenGeocodeFails() {
            when(dailyPlanRepository.findByIdAndTripId(10L, 1L))
                    .thenReturn(Optional.of(dailyPlan));
            when(orsService.geocode(request.getAddress())).thenReturn(null);
            when(dailyPlanRepository.save(any(DailyPlan.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            DailyPlanResponse result = dailyPlanService.addAttraction(1L, 10L, request);

            assertThat(result.getAttractions()).hasSize(1);
            assertThat(result.getAttractions().get(0).getLatitude()).isNull();
            assertThat(result.getAttractions().get(0).getLongitude()).isNull();
        }

        @Test
        @DisplayName("結束時間早於開始時間時拋出 InvalidInputException")
        void throwsInvalidInput_whenEndBeforeStart() {
            request.setStartTime(LocalTime.of(11, 0));
            request.setEndTime(LocalTime.of(9, 0));

            assertThatThrownBy(() -> dailyPlanService.addAttraction(1L, 10L, request))
                    .isInstanceOf(InvalidInputException.class);
        }

        @Test
        @DisplayName("結束時間等於開始時間時拋出 InvalidInputException")
        void throwsInvalidInput_whenEndEqualsStart() {
            request.setStartTime(LocalTime.of(10, 0));
            request.setEndTime(LocalTime.of(10, 0));

            assertThatThrownBy(() -> dailyPlanService.addAttraction(1L, 10L, request))
                    .isInstanceOf(InvalidInputException.class);
        }

        @Test
        @DisplayName("時間與既有景點衝突時拋出 TimeConflictException")
        void throwsTimeConflict_whenOverlapsExistingAttraction() {
            Attraction existing = new Attraction(dailyPlan, "東京鐵塔",
                    "東京都港區芝公園 4-2-8",
                    LocalTime.of(10, 0), LocalTime.of(12, 0));
            dailyPlan.getAttractions().add(existing);

            when(dailyPlanRepository.findByIdAndTripId(10L, 1L))
                    .thenReturn(Optional.of(dailyPlan));

            // request: 09:00 ~ 11:00，與既有 10:00 ~ 12:00 重疊
            assertThatThrownBy(() -> dailyPlanService.addAttraction(1L, 10L, request))
                    .isInstanceOf(TimeConflictException.class);
        }
    }

    @Nested
    @DisplayName("removeAttraction")
    class RemoveAttraction {

        @Test
        @DisplayName("成功移除景點並回傳被移除的景點資訊")
        void removesAttraction_andReturnsResponse() {
            Attraction attraction = new Attraction(dailyPlan, "淺草寺",
                    "東京都台東區淺草 2-3-1",
                    LocalTime.of(9, 0), LocalTime.of(11, 0));
            ReflectionTestUtils.setField(attraction, "id", 100L);
            dailyPlan.getAttractions().add(attraction);

            when(dailyPlanRepository.findByIdAndTripId(10L, 1L))
                    .thenReturn(Optional.of(dailyPlan));
            when(dailyPlanRepository.save(any(DailyPlan.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            AttractionResponse result = dailyPlanService.removeAttraction(1L, 10L, 100L);

            assertThat(result.getName()).isEqualTo("淺草寺");
            assertThat(dailyPlan.getAttractions()).isEmpty();
        }

        @Test
        @DisplayName("景點 ID 不存在時拋出 ResourceNotFoundException")
        void throwsNotFound_whenAttractionMissing() {
            when(dailyPlanRepository.findByIdAndTripId(10L, 1L))
                    .thenReturn(Optional.of(dailyPlan));

            assertThatThrownBy(() -> dailyPlanService.removeAttraction(1L, 10L, 999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
