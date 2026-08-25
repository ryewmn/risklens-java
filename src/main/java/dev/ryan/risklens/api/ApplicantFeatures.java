package dev.ryan.risklens.api;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ApplicantFeatures(
        @NotNull @Positive @DecimalMax("10000000") Double creditLimit,
        @NotNull @Min(18) @Max(100) Integer age,
        @NotNull @Min(-2) @Max(8) Integer repaymentStatusCurrent,
        @NotNull @Min(-2) @Max(8) Integer repaymentStatusPrior,
        @NotNull @DecimalMin("-1000000") @DecimalMax("10000000") Double billAmountCurrent,
        @NotNull @DecimalMin("-1000000") @DecimalMax("10000000") Double billAmountPrior,
        @NotNull @DecimalMin("0") @DecimalMax("10000000") Double paymentAmountCurrent,
        @NotNull @DecimalMin("0") @DecimalMax("10000000") Double paymentAmountPrior) {
}
