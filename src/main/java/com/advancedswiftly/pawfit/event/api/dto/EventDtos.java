package com.advancedswiftly.pawfit.event.api.dto;

import com.advancedswiftly.pawfit.event.domain.DrawEntryStatus;
import com.advancedswiftly.pawfit.event.domain.EventStatus;
import com.advancedswiftly.pawfit.event.domain.FulfillmentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class EventDtos {
    private EventDtos() { }
    public record CurrentEventResponse(Long eventId, String title, int year, int quarter, EventStatus status) { }
    public record PrizeResponse(int rank, String name, int quantity) { }
    public record EventDetailResponse(Long eventId, String title, LocalDate startsOn, LocalDate endsOn,
                                      List<Long> regions, List<PrizeResponse> prizes, String notice) { }
    public record EventStoreItem(Long storeId, String name, Long distanceMeters) { }
    public record EventStoreListResponse(List<EventStoreItem> items) { }
    public record QrVisitRequest(@NotBlank @Size(max = 500) String qrToken,
                                 @Size(max = 5) List<@NotNull @Positive Long> dogIds) {
        public QrVisitRequest { dogIds = dogIds == null ? List.of() : List.copyOf(dogIds); }
    }
    public record QrVisitResponse(Long visitId, Long storeId, boolean stampAwarded, long stampCount) { }
    public record VisitItem(Long visitId, String storeName, Instant visitedAt) { }
    public record VisitListResponse(List<VisitItem> items) { }
    public record StampStoreItem(Long storeId, boolean awarded) { }
    public record StampStatusResponse(Long eventId, long stampCount, int maxStampCount,
                                      List<StampStoreItem> stores) { }
    public record DrawEntryRequest(
            @NotBlank @Pattern(regexp = "^01[016789][0-9]{7,8}$") String phoneNumber,
            @NotBlank @Size(max = 300) String address,
            @NotNull Boolean privacyAgreed) { }
    public record DrawEntryCreateResponse(Long drawEntryId, DrawEntryStatus status, long stampCount) { }
    public record PrizeNameResponse(String name) { }
    public record DrawEntryResponse(Long drawEntryId, DrawEntryStatus status, PrizeNameResponse prize,
                                    FulfillmentStatus fulfillmentStatus) { }
}
