package com.advancedswiftly.pawfit.place;

import jakarta.validation.Valid;
import com.advancedswiftly.pawfit.global.error.ApiErrorResponse;
import com.advancedswiftly.pawfit.global.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/v1/places")
class PlaceController {

    private final PlaceService service;

    PlaceController(PlaceService service) {
        this.service = service;
    }

    @GetMapping
    PlaceService.Search search(Authentication auth,
            @RequestParam(required = false) @Size(max = 100) String keyword,
            @RequestParam(required = false) @Positive Long regionId,
            @RequestParam(required = false) @Size(max = 80) String category,
            @RequestParam(required = false) @Size(max = 5) List<@Positive Long> dogIds,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.search(auth, keyword, regionId, category, dogIds, page, size);
    }

    @GetMapping("/map")
    PlaceService.MapResult map(Authentication auth,
            @RequestParam @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
            @RequestParam @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
            @RequestParam(defaultValue = "5000") @Min(1) @Max(50000) int radiusMeters,
            @RequestParam(required = false) @Size(max = 80) String category,
            @RequestParam(required = false) @Size(max = 5) List<@Positive Long> dogIds) {
        return service.map(auth, latitude, longitude, radiusMeters, category, dogIds);
    }

    @GetMapping("/{placeId}")
    PlaceService.Detail detail(Authentication auth, @PathVariable String placeId,
            @RequestParam(required = false) @Size(max = 5) List<@Positive Long> dogIds) {
        return service.detail(auth, placeId, dogIds);
    }

    @PreAuthorize("hasRole('GUARDIAN')")
    @PostMapping("/{placeId}/entry-judgments")
    PlaceService.Judgment judge(Authentication auth, @PathVariable String placeId,
            @Valid @RequestBody Selection selection) {
        return service.judge(auth, placeId, selection.dogIds());
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiErrorResponse invalidParameter(HttpServletRequest request) {
        var code = ErrorCode.INVALID_REQUEST;
        return ApiErrorResponse.of(code, code.getMessage(), request.getRequestURI(), null, java.util.Map.of());
    }

    record Selection(@NotEmpty @Size(max = 5) List<@NotNull @Positive Long> dogIds) {}
}
