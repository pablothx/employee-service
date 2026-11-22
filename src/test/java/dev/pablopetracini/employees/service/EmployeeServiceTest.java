package dev.pablopetracini.employees.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.pablopetracini.employees.api.CreateEmployeeRequest;
import dev.pablopetracini.employees.api.EmployeeResponse;
import dev.pablopetracini.employees.persistence.Employee;
import dev.pablopetracini.employees.persistence.EmployeeRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository repository;

    private EmployeeService service;

    @BeforeEach
    void setUp() {
        service = new EmployeeService(repository);
    }

    private static Employee employee(long id, String name, String department, String salary) {
        Employee employee = new Employee(name, department, new BigDecimal(salary));
        ReflectionTestUtils.setField(employee, "id", id);
        return employee;
    }

    private void saveAssignsId(long id) {
        when(repository.save(any(Employee.class))).thenAnswer(invocation -> {
            Employee saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", id);
            return saved;
        });
    }

    // --- US1: create ---

    @Test
    void us1_as1_create_savesAndReturnsResponse() {
        saveAssignsId(1L);

        EmployeeResponse response = service.create(
                new CreateEmployeeRequest("Ana", "Engineering", new BigDecimal("5000.00")));

        assertThat(response).isEqualTo(
                new EmployeeResponse(1L, "Ana", "Engineering", new BigDecimal("5000.00")));
    }

    @Test
    void edge_create_trimsNameAndDepartment() {
        saveAssignsId(1L);

        service.create(new CreateEmployeeRequest("  Ana ", " Engineering ", new BigDecimal("5000.00")));

        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Ana");
        assertThat(captor.getValue().getDepartment()).isEqualTo("Engineering");
    }

    @Test
    void edge_create_normalizesSalaryToScaleTwo() {
        saveAssignsId(1L);

        EmployeeResponse response = service.create(
                new CreateEmployeeRequest("Ana", "Engineering", new BigDecimal("5000")));

        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getSalary()).isEqualTo(new BigDecimal("5000.00"));
        assertThat(response.salary()).isEqualTo(new BigDecimal("5000.00"));
    }

    // --- US2: get by id ---

    @Test
    void us2_as1_findById_returnsEmployee() {
        when(repository.findById(1L)).thenReturn(Optional.of(employee(1L, "Ana", "Engineering", "5000.00")));

        assertThat(service.findById(1L)).isEqualTo(
                new EmployeeResponse(1L, "Ana", "Engineering", new BigDecimal("5000.00")));
    }

    @Test
    void us2_as2_findById_unknownId_throwsNotFoundWithMessage() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(999L))
                .isInstanceOf(EmployeeNotFoundException.class)
                .hasMessage("Employee 999 not found");
    }

    @Test
    void edge_findById_neverSaves() {
        when(repository.findById(1L)).thenReturn(Optional.of(employee(1L, "Ana", "Engineering", "5000.00")));

        service.findById(1L);

        verify(repository, never()).save(any());
    }

    // --- US3: highest-paid per department ---

    @Test
    void us3_as1_topEarners_returnsOnePerDepartmentSortedByDepartment() {
        when(repository.findAll()).thenReturn(List.of(
                employee(3L, "Cara", "Sales", "4000.00"),
                employee(1L, "Ana", "Engineering", "5000.00"),
                employee(2L, "Bob", "Engineering", "7000.00")));

        assertThat(service.topEarnersByDepartment()).containsExactly(
                new EmployeeResponse(2L, "Bob", "Engineering", new BigDecimal("7000.00")),
                new EmployeeResponse(3L, "Cara", "Sales", new BigDecimal("4000.00")));
    }

    @Test
    void us3_as2_topEarners_emptyStore_returnsEmptyList() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(service.topEarnersByDepartment()).isEmpty();
    }

    @Test
    void us3_as3_topEarners_tie_returnsLowestId() {
        // 7000.0 and 7000.00 are equal by compareTo but not by equals: proves scale is ignored.
        when(repository.findAll()).thenReturn(List.of(
                employee(5L, "Eve", "Engineering", "7000.0"),
                employee(4L, "Dan", "Engineering", "7000.00")));

        assertThat(service.topEarnersByDepartment())
                .extracting(EmployeeResponse::id)
                .containsExactly(4L);
    }

    @Test
    void us3_as4_topEarners_neverSaves() {
        when(repository.findAll()).thenReturn(List.of(employee(1L, "Ana", "Engineering", "5000.00")));

        service.topEarnersByDepartment();

        verify(repository, never()).save(any());
    }

    @Test
    void edge_topEarners_departmentsAreCaseSensitive() {
        when(repository.findAll()).thenReturn(List.of(
                employee(1L, "Ana", "Engineering", "5000.00"),
                employee(2L, "Bob", "engineering", "7000.00")));

        assertThat(service.topEarnersByDepartment())
                .extracting(EmployeeResponse::department)
                .containsExactly("Engineering", "engineering");
    }

    // --- US4: raise preview ---

    @Test
    void us4_as1_previewRaise_tenPercent() {
        when(repository.findAll()).thenReturn(List.of(
                employee(1L, "Ana", "Engineering", "5000.00"),
                employee(2L, "Bob", "Engineering", "7000.00")));

        assertThat(service.previewRaise(10)).containsExactly(
                new EmployeeResponse(1L, "Ana", "Engineering", new BigDecimal("5500.00")),
                new EmployeeResponse(2L, "Bob", "Engineering", new BigDecimal("7700.00")));
    }

    @Test
    void us4_as2_previewRaise_doesNotMutateEntitiesOrSave() {
        Employee ana = employee(1L, "Ana", "Engineering", "5000.00");
        when(repository.findAll()).thenReturn(List.of(ana));

        service.previewRaise(10);

        assertThat(ana.getSalary()).isEqualTo(new BigDecimal("5000.00"));
        verify(repository, never()).save(any());
        verify(repository, never()).saveAll(any());
    }

    @Test
    void us4_as5_previewRaise_roundsHalfUp() {
        when(repository.findAll()).thenReturn(List.of(employee(1L, "Ana", "Engineering", "1234.57")));

        // 1234.57 * 1.03 = 1271.6071 -> 1271.61
        assertThat(service.previewRaise(3))
                .extracting(EmployeeResponse::salary)
                .containsExactly(new BigDecimal("1271.61"));
    }

    @Test
    void us4_as6_previewRaise_emptyStore_returnsEmptyList() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(service.previewRaise(10)).isEmpty();
    }
}
