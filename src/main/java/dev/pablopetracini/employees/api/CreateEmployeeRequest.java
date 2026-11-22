package dev.pablopetracini.employees.api;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record CreateEmployeeRequest(
        @NotBlank String name,
        @NotBlank String department,
        @NotNull @Positive @Digits(integer = 13, fraction = 2) BigDecimal salary) {
}
