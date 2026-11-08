package com.advancedswiftly.pawfit.publicdata;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('OPERATOR')")
class PublicDataController {
    private final PublicDataService service;

    PublicDataController(PublicDataService service) {
        this.service = service;
    }

    @GetMapping("/public-data-sources")
    PublicDataService.Sources sources() {
        return service.sources();
    }

    @PostMapping("/public-data-sources/{sourceId}/sync")
    @ResponseStatus(HttpStatus.ACCEPTED)
    PublicDataService.Started sync(@PathVariable @Positive long sourceId,
            @RequestBody(required = false) SyncRequest request) {
        return service.start(sourceId, request != null && Boolean.TRUE.equals(request.fullSync()));
    }

    @GetMapping("/public-data-sources/{sourceId}/sync-runs")
    PublicDataService.Items<PublicDataService.Run> runs(@PathVariable @Positive long sourceId,
            @RequestParam(defaultValue = "0") @Min(0) int page) {
        return service.runs(sourceId, page);
    }

    @GetMapping("/public-facilities")
    PublicDataService.Items<PublicDataService.Facility> facilities(
            @RequestParam(required = false) @Positive Long sourceId,
            @RequestParam(required = false) @Positive Long regionId,
            @RequestParam(required = false) @Size(max = 150) String keyword,
            @RequestParam(defaultValue = "0") @Min(0) int page) {
        return service.facilities(sourceId, regionId, keyword, page);
    }

    @PatchMapping("/public-facilities/{facilityId}")
    PublicDataService.Corrected correct(@PathVariable @Positive long facilityId,
            @Valid @RequestBody Correction request) {
        return service.correct(facilityId, request);
    }

    record SyncRequest(Boolean fullSync) {}
    record Correction(@Size(max = 150) String name,
                      @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
                      @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
                      @Size(max = 80) String category, Boolean visible) {}
}
