package com.advancedswiftly.pawfit.global.ai;

import java.util.List;

public record VaccinationExtraction(
        String dogName,
        String clinicName,
        List<VaccinationItem> vaccinations,
        double confidence) {

    public VaccinationExtraction {
        vaccinations = vaccinations == null ? List.of() : List.copyOf(vaccinations);
    }
}
