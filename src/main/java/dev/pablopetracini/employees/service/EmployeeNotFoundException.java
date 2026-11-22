package dev.pablopetracini.employees.service;

public class EmployeeNotFoundException extends RuntimeException {

    public EmployeeNotFoundException(Long id) {
        super("Employee " + id + " not found");
    }
}
