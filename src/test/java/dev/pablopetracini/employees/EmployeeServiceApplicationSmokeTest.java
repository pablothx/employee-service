package dev.pablopetracini.employees;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.pablopetracini.employees.persistence.Employee;
import dev.pablopetracini.employees.persistence.EmployeeRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * End-to-end checks against the real H2 database. Stored state is read straight from the
 * repository, so "reads change nothing" (Constitution V) does not rely on the endpoint under test.
 */
@SpringBootTest
@AutoConfigureMockMvc
class EmployeeServiceApplicationSmokeTest {

    private final MockMvc mockMvc;
    private final EmployeeRepository repository;

    @Autowired
    EmployeeServiceApplicationSmokeTest(MockMvc mockMvc, EmployeeRepository repository) {
        this.mockMvc = mockMvc;
        this.repository = repository;
    }

    /** A comparable snapshot of a stored row. */
    private record Row(Long id, String name, String department, BigDecimal salary) {
        static Row of(Employee e) {
            return new Row(e.getId(), e.getName(), e.getDepartment(), e.getSalary());
        }
    }

    @BeforeEach
    void clearDatabase() {
        repository.deleteAll();
    }

    private long create(String name, String department, String salary) throws Exception {
        String location = mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","department":"%s","salary":%s}""".formatted(name, department, salary)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }

    private List<Row> storedRows() {
        return repository.findAll().stream().map(Row::of).sorted((a, b) -> a.id().compareTo(b.id())).toList();
    }

    @Test
    void us4_as2_endToEnd_previewLeavesStoredSalariesUnchanged() throws Exception {
        long ana = create("Ana", "Engineering", "5000.00");
        long bob = create("Bob", "Engineering", "7000.00");

        mockMvc.perform(post("/employees/raise").param("percent", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Ana')].salary").value(5500.00))
                .andExpect(jsonPath("$[?(@.name == 'Bob')].salary").value(7700.00));

        assertThat(repository.findById(ana).orElseThrow().getSalary()).isEqualTo(new BigDecimal("5000.00"));
        assertThat(repository.findById(bob).orElseThrow().getSalary()).isEqualTo(new BigDecimal("7000.00"));
    }

    @Test
    void us2_as1_endToEnd_getByIdLeavesStoredDataUnchanged() throws Exception {
        long ana = create("Ana", "Engineering", "5000.00");
        List<Row> before = storedRows();

        mockMvc.perform(get("/employees/{id}", ana))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ana"));

        assertThat(storedRows()).isEqualTo(before);
    }

    @Test
    void us3_as4_endToEnd_topByDepartmentLeavesStoredDataUnchanged() throws Exception {
        create("Ana", "Engineering", "5000.00");
        create("Bob", "Engineering", "7000.00");
        create("Cara", "Sales", "4000.00");
        List<Row> before = storedRows();

        mockMvc.perform(get("/employees/stats/top-by-department"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Bob"))
                .andExpect(jsonPath("$[1].name").value("Cara"));

        assertThat(storedRows()).hasSize(3).isEqualTo(before);
    }
}
