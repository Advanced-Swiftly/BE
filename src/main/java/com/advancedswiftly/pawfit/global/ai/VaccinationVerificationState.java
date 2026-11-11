package com.advancedswiftly.pawfit.global.ai;

public record VaccinationVerificationState(
        VaccinationAnalysis aiAnalysis,
        OperatorReviewStatus operatorReviewStatus) {

    public static VaccinationVerificationState awaitingOperator(VaccinationAnalysis analysis) {
        return new VaccinationVerificationState(analysis, OperatorReviewStatus.PENDING);
    }
}
