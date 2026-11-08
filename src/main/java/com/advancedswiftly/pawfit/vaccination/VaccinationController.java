package com.advancedswiftly.pawfit.vaccination;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import com.advancedswiftly.pawfit.global.error.BusinessException;
import com.advancedswiftly.pawfit.global.error.ErrorCode;

@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasRole('GUARDIAN')")
class VaccinationController {

    private final VaccinationService service;

    VaccinationController(VaccinationService service) {
        this.service = service;
    }

    @PostMapping(value = "/dogs/{dogId}/vaccination-documents", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.ACCEPTED)
    VaccinationService.Upload upload(Authentication auth, @PathVariable long dogId,
            @RequestPart("files") List<MultipartFile> files) {
        return service.upload(userId(auth), dogId, null, files);
    }

    @GetMapping("/dogs/{dogId}/vaccination-record")
    VaccinationService.Result current(Authentication auth, @PathVariable long dogId) {
        return service.current(userId(auth), dogId);
    }

    @GetMapping("/vaccination-records/{recordId}")
    VaccinationService.Result analysis(Authentication auth, @PathVariable long recordId) {
        return service.analysis(userId(auth), recordId);
    }

    @PatchMapping("/vaccination-records/{recordId}")
    VaccinationService.State edit(Authentication auth, @PathVariable long recordId,
            @Valid @RequestBody Correction correction) {
        return service.edit(userId(auth), recordId, correction.items());
    }

    @PostMapping(value = "/vaccination-records/{recordId}/resubmit", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.ACCEPTED)
    VaccinationService.State resubmit(Authentication auth, @PathVariable long recordId,
            @RequestPart("files") List<MultipartFile> files) {
        service.upload(userId(auth), null, recordId, files);
        return new VaccinationService.State(recordId, "ANALYZING");
    }

    private long userId(Authentication auth) {
        return Long.parseLong(auth.getName());
    }

    @ExceptionHandler({MaxUploadSizeExceededException.class,
            org.springframework.web.multipart.support.MissingServletRequestPartException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    java.util.Map<String, String> invalidDocument() {
        ErrorCode code = ErrorCode.INVALID_DOCUMENT_FORMAT;
        return java.util.Map.of("code", code.getCode(), "message", code.getMessage());
    }

    record Correction(@NotEmpty @Size(max = 50) List<@NotNull @Valid Item> items) {}
    record Item(@NotBlank @Pattern(regexp = "[A-Z][A-Z0-9_]{0,79}") String vaccineCode,
                @NotNull @PastOrPresent LocalDate vaccinatedAt) {}
}
