package com.advancedswiftly.pawfit.dog.api;

import com.advancedswiftly.pawfit.dog.api.dto.DogDtos.DefaultDogResponse;
import com.advancedswiftly.pawfit.dog.api.dto.DogDtos.DogCreateRequest;
import com.advancedswiftly.pawfit.dog.api.dto.DogDtos.DogCreateResponse;
import com.advancedswiftly.pawfit.dog.api.dto.DogDtos.DogDetailResponse;
import com.advancedswiftly.pawfit.dog.api.dto.DogDtos.DogListResponse;
import com.advancedswiftly.pawfit.dog.api.dto.DogDtos.DogUpdateRequest;
import com.advancedswiftly.pawfit.dog.api.dto.DogDtos.DogUpdateResponse;
import com.advancedswiftly.pawfit.dog.application.DogService;
import com.advancedswiftly.pawfit.global.error.BusinessException;
import com.advancedswiftly.pawfit.global.error.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "반려견", description = "보호자의 반려견 프로필 관리")
@Validated
@RestController
@RequestMapping("/api/v1/dogs")
public class DogController {

    private final DogService dogService;

    public DogController(DogService dogService) {
        this.dogService = dogService;
    }

    @Operation(summary = "반려견 등록")
    @PostMapping
    public ResponseEntity<DogCreateResponse> create(
            Authentication authentication,
            @Valid @RequestBody DogCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dogService.create(currentUserId(authentication), request));
    }

    @Operation(summary = "내 반려견 목록 조회")
    @GetMapping
    public DogListResponse findMine(Authentication authentication) {
        return dogService.findMine(currentUserId(authentication));
    }

    @Operation(summary = "반려견 상세 조회")
    @GetMapping("/{dogId}")
    public DogDetailResponse findMine(
            Authentication authentication, @PathVariable @Positive Long dogId) {
        return dogService.findMine(currentUserId(authentication), dogId);
    }

    @Operation(summary = "반려견 정보 수정")
    @PatchMapping("/{dogId}")
    public DogUpdateResponse update(
            Authentication authentication,
            @PathVariable @Positive Long dogId,
            @Valid @RequestBody DogUpdateRequest request) {
        return dogService.update(currentUserId(authentication), dogId, request);
    }

    @Operation(summary = "대표 반려견 설정")
    @PatchMapping("/{dogId}/default")
    public DefaultDogResponse designateDefault(
            Authentication authentication, @PathVariable @Positive Long dogId) {
        return dogService.designateDefault(currentUserId(authentication), dogId);
    }

    @Operation(summary = "반려견 삭제")
    @DeleteMapping("/{dogId}")
    public ResponseEntity<Void> delete(
            Authentication authentication, @PathVariable @Positive Long dogId) {
        dogService.delete(currentUserId(authentication), dogId);
        return ResponseEntity.noContent().build();
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }
}
