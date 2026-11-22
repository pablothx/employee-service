package dev.pablopetracini.employees.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.pablopetracini.employees.service.EmployeeNotFoundException;
import dev.pablopetracini.employees.service.EmployeeService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(EmployeeController.class)
class EmployeeControllerTest {

    private final MockMvc mockMvc;

    @MockitoBean
    private EmployeeService service;

    @Autowired
    EmployeeControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    /** The five-field error body every error response must use (US5-AS1, SC-003). */
    private static void assertStandardErrorBody(ResultActions result, int expectedStatus) throws Exception {
        result.andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.*", hasSize(5)))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(expectedStatus))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").exists());
    }

    private ResultActions postEmployee(String json) throws Exception {
        return mockMvc.perform(post("/employees").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    // --- US1: create ---

    @Test
    void us1_as1_validEmployee_returns201WithLocationAndBody() throws Exception {
        when(service.create(any())).thenReturn(
                new EmployeeResponse(1L, "Ana", "Engineering", new BigDecimal("5000.00")));

        postEmployee("""
                {"name":"Ana","department":"Engineering","salary":5000.00}""")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/employees/1")))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Ana"))
                .andExpect(jsonPath("$.department").value("Engineering"))
                .andExpect(jsonPath("$.salary").value(5000.00));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            """
            {"department":"Engineering","salary":5000.00}""",
            """
            {"name":"","department":"Engineering","salary":5000.00}""",
            """
            {"name":"   ","department":"Engineering","salary":5000.00}"""})
    void us1_as2_missingOrBlankName_returns400NamingName(String json) throws Exception {
        postEmployee(json)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("name")));
        verify(service, never()).create(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            """
            {"name":"Ana","salary":5000.00}""",
            """
            {"name":"Ana","department":"","salary":5000.00}""",
            """
            {"name":"Ana","department":"   ","salary":5000.00}"""})
    void us1_as3_missingOrBlankDepartment_returns400NamingDepartment(String json) throws Exception {
        postEmployee(json)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("department")));
        verify(service, never()).create(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            """
            {"name":"Ana","department":"Engineering"}""",
            """
            {"name":"Ana","department":"Engineering","salary":0}""",
            """
            {"name":"Ana","department":"Engineering","salary":-1}"""})
    void us1_as4_invalidSalary_returns400NamingSalary(String json) throws Exception {
        postEmployee(json)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("salary")));
        verify(service, never()).create(any());
    }

    @Test
    void edge_salaryWithThreeDecimals_returns400() throws Exception {
        postEmployee("""
                {"name":"Ana","department":"Engineering","salary":1000.123}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("salary")));
        verify(service, never()).create(any());
    }

    @Test
    void edge_clientSuppliedIdIsIgnored_returns201() throws Exception {
        when(service.create(any())).thenReturn(
                new EmployeeResponse(1L, "Ana", "Engineering", new BigDecimal("5000.00")));

        postEmployee("""
                {"id":99,"name":"Ana","department":"Engineering","salary":5000.00}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    // --- US2: get by id ---

    @Test
    void us2_as1_existingId_returns200WithBody() throws Exception {
        when(service.findById(1L)).thenReturn(
                new EmployeeResponse(1L, "Ana", "Engineering", new BigDecimal("5000.00")));

        mockMvc.perform(get("/employees/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Ana"))
                .andExpect(jsonPath("$.department").value("Engineering"))
                .andExpect(jsonPath("$.salary").value(5000.00));
    }

    @Test
    void us2_as2_unknownId_returns404WithMessage() throws Exception {
        when(service.findById(999L)).thenThrow(new EmployeeNotFoundException(999L));

        mockMvc.perform(get("/employees/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Employee 999 not found"));
    }

    @Test
    void us2_as3_nonNumericId_returns400() throws Exception {
        mockMvc.perform(get("/employees/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("abc")));
        verify(service, never()).findById(any());
    }

    // --- US5: consistent error responses ---

    static Stream<Arguments> errorCases() {
        return Stream.of(
                Arguments.of("invalid create", post("/employees").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"department\":\"Sales\",\"salary\":0}"), 400),
                Arguments.of("unknown id", get("/employees/999"), 404),
                Arguments.of("non-numeric id", get("/employees/abc"), 400),
                Arguments.of("malformed JSON", post("/employees").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":"), 400));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("errorCases")
    void us5_as1_allErrorBodiesHaveExactlyFiveFields(String description, RequestBuilder request, int expectedStatus)
            throws Exception {
        when(service.findById(999L)).thenThrow(new EmployeeNotFoundException(999L));

        assertStandardErrorBody(mockMvc.perform(request), expectedStatus);
    }

    @Test
    void us5_as2_notFoundBody_hasStatusErrorMessagePath() throws Exception {
        when(service.findById(999L)).thenThrow(new EmployeeNotFoundException(999L));

        mockMvc.perform(get("/employees/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Employee 999 not found"))
                .andExpect(jsonPath("$.path").value("/employees/999"));
    }

    @Test
    void us5_as3_malformedBody_returns400StandardBody() throws Exception {
        assertStandardErrorBody(postEmployee("{\"name\":"), 400);
        verify(service, never()).create(any());
    }

    @Test
    void edge_unknownRoute_returns404StandardBody() throws Exception {
        assertStandardErrorBody(mockMvc.perform(get("/nope")), 404);
    }

    @Test
    void edge_wrongMethod_returns405StandardBody() throws Exception {
        assertStandardErrorBody(mockMvc.perform(delete("/employees/1")), 405);
    }

    @Test
    void edge_unexpectedException_returns500WithoutInternalDetails() throws Exception {
        when(service.findById(1L)).thenThrow(new RuntimeException("db password leaked"));

        ResultActions result = mockMvc.perform(get("/employees/1"));

        assertStandardErrorBody(result, 500);
        result.andExpect(jsonPath("$.message").value("Unexpected error"))
                .andExpect(content().string(not(containsString("leaked"))));
    }

    // --- US3: highest-paid per department ---

    @Test
    void us3_as1_topByDepartment_returns200Array() throws Exception {
        when(service.topEarnersByDepartment()).thenReturn(List.of(
                new EmployeeResponse(2L, "Bob", "Engineering", new BigDecimal("7000.00")),
                new EmployeeResponse(3L, "Cara", "Sales", new BigDecimal("4000.00"))));

        mockMvc.perform(get("/employees/stats/top-by-department"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("Bob"))
                .andExpect(jsonPath("$[0].department").value("Engineering"))
                .andExpect(jsonPath("$[1].name").value("Cara"))
                .andExpect(jsonPath("$[1].department").value("Sales"));
    }

    @Test
    void us3_as2_topByDepartment_empty_returnsEmptyArray() throws Exception {
        when(service.topEarnersByDepartment()).thenReturn(List.of());

        mockMvc.perform(get("/employees/stats/top-by-department"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // --- US4: raise preview ---

    @ParameterizedTest
    @ValueSource(ints = {1, 100})
    void us4_as3_percentBoundaries_accepted(int percent) throws Exception {
        when(service.previewRaise(percent)).thenReturn(List.of());

        mockMvc.perform(post("/employees/raise").param("percent", String.valueOf(percent)))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "101", "-5", "2.5", "abc", "<missing>"})
    void us4_as4_invalidPercent_returns400(String percent) throws Exception {
        var request = post("/employees/raise");
        if (!percent.equals("<missing>")) {
            request.param("percent", percent);
        }

        assertStandardErrorBody(mockMvc.perform(request), 400);
        verify(service, never()).previewRaise(anyInt());
    }
}
