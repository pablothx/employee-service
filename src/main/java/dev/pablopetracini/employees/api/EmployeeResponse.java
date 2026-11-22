package dev.pablopetracini.employees.api;

import java.math.BigDecimal;

public record EmployeeResponse(Long id, String name, String department, BigDecimal salary) {
}
