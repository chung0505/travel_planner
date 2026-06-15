package com.travel.planner.service;

import com.travel.planner.dto.request.CreateTripRequest;
import com.travel.planner.dto.response.TripResponse;
import com.travel.planner.exception.InvalidInputException;
import com.travel.planner.exception.ResourceNotFoundException;
import com.travel.planner.model.Traveler;
import com.travel.planner.model.Trip;
import com.travel.planner.repository.TravelerRepository;
import com.travel.planner.repository.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TripService")
class TripServiceTest {

    @Mock
    private TripRepository tripRepository;

    @Mock
    private TravelerRepository travelerRepository;

    @InjectMocks
    private TripService tripService;

    private static final Long ORGANIZER_ID = 1L;

    private CreateTripRequest validRequest;
    private Traveler organizer;

    @BeforeEach
    void setUp() {
        validRequest = new CreateTripRequest();
        validRequest.setTitle("東京之旅");
        validRequest.setDestination("東京");
        validRequest.setDepartureDate(LocalDate.of(2026, 7, 1));
        validRequest.setReturnDate(LocalDate.of(2026, 7, 5));
        validRequest.setCompanionCount(2);

        organizer = new Traveler("主辦人", "organizer@example.com", "hash");
    }

    @Nested
    @DisplayName("createTrip")
    class CreateTrip {

        @Test
        @DisplayName("成功建立行程並自動產生每日行程")
        void createsTrip_withGeneratedDailyPlans() {
            when(travelerRepository.findById(ORGANIZER_ID)).thenReturn(Optional.of(organizer));
            when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> inv.getArgument(0));

            TripResponse response = tripService.createTrip(validRequest, ORGANIZER_ID);

            assertThat(response.getTitle()).isEqualTo("東京之旅");
            assertThat(response.getDestination()).isEqualTo("東京");
            assertThat(response.getDailyPlans()).hasSize(5);
            assertThat(response.getTotalDays()).isEqualTo(5);
        }

        @Test
        @DisplayName("每日行程依日期順序排列且 dayNumber 從 1 開始遞增")
        void dailyPlans_haveCorrectDayNumbersAndDates() {
            when(travelerRepository.findById(ORGANIZER_ID)).thenReturn(Optional.of(organizer));
            when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> inv.getArgument(0));

            TripResponse response = tripService.createTrip(validRequest, ORGANIZER_ID);

            assertThat(response.getDailyPlans().get(0).getDayNumber()).isEqualTo(1);
            assertThat(response.getDailyPlans().get(0).getDate()).isEqualTo(LocalDate.of(2026, 7, 1));
            assertThat(response.getDailyPlans().get(4).getDayNumber()).isEqualTo(5);
            assertThat(response.getDailyPlans().get(4).getDate()).isEqualTo(LocalDate.of(2026, 7, 5));
        }

        @Test
        @DisplayName("兩天行程產生兩個每日行程")
        void createsTwoDailyPlans_forTwoDayTrip() {
            validRequest.setDepartureDate(LocalDate.of(2026, 7, 1));
            validRequest.setReturnDate(LocalDate.of(2026, 7, 2));
            when(travelerRepository.findById(ORGANIZER_ID)).thenReturn(Optional.of(organizer));
            when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> inv.getArgument(0));

            TripResponse response = tripService.createTrip(validRequest, ORGANIZER_ID);

            assertThat(response.getDailyPlans()).hasSize(2);
        }

        @Test
        @DisplayName("回程日期與出發日期相同時拋出 InvalidInputException")
        void throwsInvalidInput_whenReturnSameAsDeparture() {
            validRequest.setReturnDate(LocalDate.of(2026, 7, 1));

            assertThatThrownBy(() -> tripService.createTrip(validRequest, ORGANIZER_ID))
                    .isInstanceOf(InvalidInputException.class);
        }

        @Test
        @DisplayName("回程日期早於出發日期時拋出 InvalidInputException")
        void throwsInvalidInput_whenReturnBeforeDeparture() {
            validRequest.setReturnDate(LocalDate.of(2026, 6, 30));

            assertThatThrownBy(() -> tripService.createTrip(validRequest, ORGANIZER_ID))
                    .isInstanceOf(InvalidInputException.class);
        }
    }

    @Nested
    @DisplayName("getTrip")
    class GetTrip {

        @Test
        @DisplayName("存在的行程 ID 回傳對應的 TripResponse")
        void returnsTrip_whenFound() {
            Trip trip = new Trip("東京之旅", "東京",
                    LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 5), 2);
            when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

            TripResponse response = tripService.getTrip(1L);

            assertThat(response.getDestination()).isEqualTo("東京");
            assertThat(response.getCompanionCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("不存在的行程 ID 拋出 ResourceNotFoundException")
        void throwsNotFound_whenTripMissing() {
            when(tripRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tripService.getTrip(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getAllTrips")
    class GetAllTrips {

        @Test
        @DisplayName("回傳自己建立與參與的行程清單")
        void returnsAllTrips() {
            when(tripRepository.findByOrganizerId(ORGANIZER_ID)).thenReturn(List.of(
                    new Trip("東京之旅", "東京", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 5), 2)
            ));
            when(tripRepository.findByParticipantsId(ORGANIZER_ID)).thenReturn(List.of(
                    new Trip("大阪之旅", "大阪", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3), 1)
            ));

            List<TripResponse> responses = tripService.getAllTrips(ORGANIZER_ID);

            assertThat(responses).hasSize(2);
            assertThat(responses.get(0).getDestination()).isEqualTo("東京");
            assertThat(responses.get(1).getDestination()).isEqualTo("大阪");
        }

        @Test
        @DisplayName("無行程時回傳空清單")
        void returnsEmptyList_whenNoTrips() {
            when(tripRepository.findByOrganizerId(ORGANIZER_ID)).thenReturn(List.of());
            when(tripRepository.findByParticipantsId(ORGANIZER_ID)).thenReturn(List.of());

            List<TripResponse> responses = tripService.getAllTrips(ORGANIZER_ID);

            assertThat(responses).isEmpty();
        }
    }

    @Nested
    @DisplayName("deleteTrip")
    class DeleteTrip {

        @Test
        @DisplayName("存在的行程正常刪除")
        void deletesTrip_whenFound() {
            Trip trip = new Trip("東京之旅", "東京",
                    LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 5), 2);
            when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

            tripService.deleteTrip(1L);

            verify(tripRepository).delete(trip);
        }

        @Test
        @DisplayName("行程不存在時拋出 ResourceNotFoundException")
        void throwsNotFound_whenTripMissing() {
            when(tripRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tripService.deleteTrip(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
