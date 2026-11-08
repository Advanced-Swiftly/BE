package com.advancedswiftly.pawfit.global.ai;

public record VaccinationAnalysis(
        AiProvider provider,
        VaccinationAnalysisStatus status,
        VaccinationExtraction extraction,
        String failureReason) {

    public static VaccinationAnalysis analyzed(AiProvider provider, VaccinationExtraction extraction) {
        return new VaccinationAnalysis(provider, VaccinationAnalysisStatus.ANALYZED, extraction, null);
    }

    public static VaccinationAnalysis needsManualReview(AiProvider provider, String failureReason) {
        return new VaccinationAnalysis(
                provider,
                VaccinationAnalysisStatus.NEEDS_MANUAL_REVIEW,
                null,
                failureReason);
    }
}
