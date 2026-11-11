package com.advancedswiftly.pawfit.dog.api.dto;

import com.advancedswiftly.pawfit.dog.domain.VaccinationStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class DogDtos {

    private static final String EQUIPMENT_CODE_PATTERN = "^[A-Z][A-Z0-9_]{0,49}$";

    private DogDtos() {
    }

    public record DogCreateRequest(
            @NotBlank @Size(max = 50) String name,
            @NotNull Long breedId,
            @PastOrPresent LocalDate birthDate,
            @DecimalMin("0.01") @Digits(integer = 4, fraction = 2) BigDecimal weightKg,
            @Size(max = 20) List<
                    @NotBlank
                    @Pattern(regexp = EQUIPMENT_CODE_PATTERN, message = "장비 코드는 영문 대문자 형식이어야 합니다.")
                    String> equipmentCodes,
            Boolean isDefault) {

        public DogCreateRequest {
            equipmentCodes = equipmentCodes == null ? List.of() : List.copyOf(equipmentCodes);
        }
    }

    public record DogUpdateRequest(
            @Size(max = 50) @Pattern(regexp = ".*\\S.*", message = "이름은 공백일 수 없습니다.") String name,
            Long breedId,
            @DecimalMin("0.01") @Digits(integer = 4, fraction = 2) BigDecimal weightKg,
            @Size(max = 20) List<
                    @NotBlank
                    @Pattern(regexp = EQUIPMENT_CODE_PATTERN, message = "장비 코드는 영문 대문자 형식이어야 합니다.")
                    String> equipmentCodes) {
    }

    public record DogCreateResponse(Long dogId, String name, boolean isDefault) {
    }

    public record DogUpdateResponse(Long dogId, String name, BigDecimal weightKg) {
    }

    public record DefaultDogResponse(Long dogId, boolean isDefault) {
    }

    public record DogListItem(Long dogId, String name, String breedName, boolean isDefault) {
    }

    public record DogListResponse(List<DogListItem> dogs) {
    }

    public record DogDetailResponse(
            Long dogId,
            String name,
            Long breedId,
            String breedName,
            LocalDate birthDate,
            BigDecimal weightKg,
            List<String> equipmentCodes,
            boolean isDefault,
            VaccinationStatus vaccinationStatus) {
    }
}
