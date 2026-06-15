package com.travel.planner.service;

import com.travel.planner.dto.request.ShareItineraryRequest;
import com.travel.planner.dto.response.ShareLinkResponse;
import com.travel.planner.dto.response.TripResponse;
import com.travel.planner.exception.ResourceNotFoundException;
import com.travel.planner.model.ShareLink;
import com.travel.planner.model.Traveler;
import com.travel.planner.model.Trip;
import com.travel.planner.model.enums.ShareType;
import com.travel.planner.repository.ShareLinkRepository;
import com.travel.planner.repository.TravelerRepository;
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
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShareService")
class ShareServiceTest {

    @Mock private ShareLinkRepository shareLinkRepository;
    @Mock private TripService tripService;
    @Mock private TravelerRepository travelerRepository;

    @InjectMocks
    private ShareService shareService;

    private Trip trip;
    private Traveler traveler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(shareService, "baseUrl", "http://localhost:8080");

        trip = new Trip("東京之旅", "東京",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 5), 2);
        trip.generateDailyPlans();
        ReflectionTestUtils.setField(trip, "id", 1L);

        traveler = new Traveler("Alice", "alice@test.com", "hash");
        ReflectionTestUtils.setField(traveler, "id", 10L);
    }

    // ── shareItinerary ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("shareItinerary")
    class ShareItinerary {

        @Test
        @DisplayName("LINK 分享：回傳含 token 且 url 已儲存的分享連結")
        void createsShareLink_withStoredUrl() {
            ShareItineraryRequest request = new ShareItineraryRequest();
            request.setShareType(ShareType.LINK);

            when(tripService.findTripById(1L)).thenReturn(trip);
            when(shareLinkRepository.save(any(ShareLink.class))).thenAnswer(inv -> {
                ShareLink link = inv.getArgument(0);
                ReflectionTestUtils.setField(link, "id", 20L);
                return link;
            });

            ShareLinkResponse response = shareService.shareItinerary(1L, request);

            assertThat(response.getToken()).isNotBlank();
            assertThat(response.getUrl()).startsWith("http://localhost:8080/api/share/");
            assertThat(response.getUrl()).contains(response.getToken());
            assertThat(response.getShareType()).isEqualTo(ShareType.LINK);
            assertThat(response.getTripId()).isEqualTo(1L);
            assertThat(response.getAssignedToTravelerId()).isNull();
        }

        @Test
        @DisplayName("指定 assignedToTravelerId 時，分享連結與旅客關聯")
        void createsShareLink_withAssignedTraveler() {
            ShareItineraryRequest request = new ShareItineraryRequest();
            request.setShareType(ShareType.LINK);
            request.setAssignedToTravelerId(10L);

            when(tripService.findTripById(1L)).thenReturn(trip);
            when(travelerRepository.findById(10L)).thenReturn(Optional.of(traveler));
            when(shareLinkRepository.save(any(ShareLink.class))).thenAnswer(inv -> {
                ShareLink link = inv.getArgument(0);
                ReflectionTestUtils.setField(link, "id", 21L);
                return link;
            });

            ShareLinkResponse response = shareService.shareItinerary(1L, request);

            assertThat(response.getAssignedToTravelerId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("assignedToTravelerId 不存在時拋出 ResourceNotFoundException")
        void throwsNotFound_whenAssignedTravelerMissing() {
            ShareItineraryRequest request = new ShareItineraryRequest();
            request.setShareType(ShareType.LINK);
            request.setAssignedToTravelerId(999L);

            when(tripService.findTripById(1L)).thenReturn(trip);
            when(travelerRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> shareService.shareItinerary(1L, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("SUMMARY 分享：shareType 為 SUMMARY")
        void createsShareLink_withSummaryType() {
            ShareItineraryRequest request = new ShareItineraryRequest();
            request.setShareType(ShareType.SUMMARY);

            when(tripService.findTripById(1L)).thenReturn(trip);
            when(shareLinkRepository.save(any(ShareLink.class))).thenAnswer(inv -> {
                ShareLink link = inv.getArgument(0);
                ReflectionTestUtils.setField(link, "id", 22L);
                return link;
            });

            ShareLinkResponse response = shareService.shareItinerary(1L, request);

            assertThat(response.getShareType()).isEqualTo(ShareType.SUMMARY);
        }

        @Test
        @DisplayName("分享連結有效期限為建立後 7 天")
        void shareLink_expiresInSevenDays() {
            ShareItineraryRequest request = new ShareItineraryRequest();
            request.setShareType(ShareType.LINK);

            when(tripService.findTripById(1L)).thenReturn(trip);
            when(shareLinkRepository.save(any(ShareLink.class))).thenAnswer(inv -> {
                ShareLink link = inv.getArgument(0);
                ReflectionTestUtils.setField(link, "id", 23L);
                return link;
            });

            ShareLinkResponse response = shareService.shareItinerary(1L, request);

            assertThat(response.getExpiresAt())
                    .isAfter(response.getCreatedAt().plusDays(6))
                    .isBefore(response.getCreatedAt().plusDays(8));
        }

        @Test
        @DisplayName("行程不存在時拋出 ResourceNotFoundException")
        void throwsNotFound_whenTripMissing() {
            when(tripService.findTripById(99L))
                    .thenThrow(new ResourceNotFoundException("找不到行程 ID: 99"));

            ShareItineraryRequest request = new ShareItineraryRequest();
            request.setShareType(ShareType.LINK);

            assertThatThrownBy(() -> shareService.shareItinerary(99L, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("每次分享產生不同 token")
        void generatesUniqueToken_eachTime() {
            ShareItineraryRequest request = new ShareItineraryRequest();
            request.setShareType(ShareType.LINK);

            when(tripService.findTripById(1L)).thenReturn(trip);
            when(shareLinkRepository.save(any(ShareLink.class))).thenAnswer(inv -> {
                ShareLink link = inv.getArgument(0);
                ReflectionTestUtils.setField(link, "id", 24L);
                return link;
            });

            ShareLinkResponse first = shareService.shareItinerary(1L, request);
            ShareLinkResponse second = shareService.shareItinerary(1L, request);

            assertThat(first.getToken()).isNotEqualTo(second.getToken());
        }
    }

    // ── getSharedItinerary ──────────────────────────────────────────────────

    @Nested
    @DisplayName("getSharedItinerary")
    class GetSharedItinerary {

        private ShareLink buildActiveLink() {
            ShareLink link = new ShareLink(trip, ShareType.LINK, "http://localhost:8080", null);
            ReflectionTestUtils.setField(link, "id", 20L);
            return link;
        }

        @Test
        @DisplayName("有效 token 回傳對應行程資訊")
        void returnsTripResponse_whenTokenValid() {
            ShareLink link = buildActiveLink();

            when(shareLinkRepository.findByToken(link.getToken())).thenReturn(Optional.of(link));

            TripResponse response = shareService.getSharedItinerary(link.getToken());

            assertThat(response.getDestination()).isEqualTo("東京");
            assertThat(response.getTotalDays()).isEqualTo(5);
        }

        @Test
        @DisplayName("token 不存在時拋出 ResourceNotFoundException")
        void throwsNotFound_whenTokenMissing() {
            when(shareLinkRepository.findByToken("nonexistent"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> shareService.getSharedItinerary("nonexistent"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("連結已停用時拋出 ResourceNotFoundException")
        void throwsNotFound_whenLinkDeactivated() {
            ShareLink link = buildActiveLink();
            link.deactivate();

            when(shareLinkRepository.findByToken(link.getToken())).thenReturn(Optional.of(link));

            assertThatThrownBy(() -> shareService.getSharedItinerary(link.getToken()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("連結已過期時拋出 ResourceNotFoundException")
        void throwsNotFound_whenLinkExpired() {
            ShareLink link = buildActiveLink();
            ReflectionTestUtils.setField(link, "expiresAt", LocalDateTime.now().minusDays(1));

            when(shareLinkRepository.findByToken(link.getToken())).thenReturn(Optional.of(link));

            assertThatThrownBy(() -> shareService.getSharedItinerary(link.getToken()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
