package dev.pablopetracini.employees.service;

import dev.pablopetracini.employees.api.CreateEmployeeRequest;
import dev.pablopetracini.employees.api.EmployeeResponse;
import dev.pablopetracini.employees.persistence.Employee;
import dev.pablopetracini.employees.persistence.EmployeeRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeService {

    private static final int SALARY_SCALE = 2;

    /** Highest salary wins; on a tie the lowest id wins (reversed id order inside maxBy). */
    private static final Comparator<Employee> BY_SALARY_THEN_LOWEST_ID =
            Comparator.comparing(Employee::getSalary)
                    .thenComparing(Employee::getId, Comparator.reverseOrder());

    private final EmployeeRepository repository;

    public EmployeeService(EmployeeRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public EmployeeResponse create(CreateEmployeeRequest request) {
        Employee employee = new Employee(
                request.name().trim(),
                request.department().trim(),
                // Exact: validation already rejects more than 2 decimal places.
                request.salary().setScale(SALARY_SCALE));
        return toResponse(repository.save(employee));
    }

    @Transactional(readOnly = true)
    public EmployeeResponse findById(Long id) {
        return repository.findById(id)
                .map(EmployeeService::toResponse)
                .orElseThrow(() -> new EmployeeNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> topEarnersByDepartment() {
        return repository.findAll().stream()
                .collect(Collectors.groupingBy(
                        Employee::getDepartment,
                        TreeMap::new,
                        Collectors.maxBy(BY_SALARY_THEN_LOWEST_ID)))
                .values().stream()
                .flatMap(Optional::stream)
                .map(EmployeeService::toResponse)
                .toList();
    }

    /**
     * Read-only "what-if": builds new response records and never touches the entities, so nothing
     * can be flushed back to the database.
     */
    @Transactional(readOnly = true)
    public List<EmployeeResponse> previewRaise(int percent) {
        // e.g. 10 -> 1.10, exact; the only rounding happens once, at the end.
        BigDecimal multiplier = BigDecimal.valueOf(100L + percent).movePointLeft(2);
        return repository.findAll().stream()
                .map(e -> new EmployeeResponse(
                        e.getId(),
                        e.getName(),
                        e.getDepartment(),
                        e.getSalary().multiply(multiplier).setScale(SALARY_SCALE, RoundingMode.HALF_UP)))
                .toList();
    }

    private static EmployeeResponse toResponse(Employee employee) {
        return new EmployeeResponse(
                employee.getId(), employee.getName(), employee.getDepartment(), employee.getSalary());
    }
}
