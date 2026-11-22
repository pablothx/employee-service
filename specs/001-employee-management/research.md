# Research & Design Decisions: Employee Management

The stack was fixed by the plan input, so there were no open unknowns. This file records the
design choices made while turning the spec into that stack.

## R-01: Raise preview uses `POST` although it changes nothing

- **Decision**: Keep `POST /employees/raise?percent=N` as requested. The handler only reads data
  and is `@Transactional(readOnly = true)`.
- **Rationale**: Constitution V forbids read endpoints from mutating data; it does not require the
  `GET` verb. POST also signals "this is a computation, not a resource fetch".
- **Trade-off**: HTTP treats POST as unsafe, so caches and clients won't treat the call as a
  safe, repeatable read. If that matters later, `GET /employees/raise-preview?percent=N` is the
  drop-in alternative. Switching costs one annotation and one contract line.
- **Alternatives considered**: `GET /employees/raise-preview` (semantically purest, but differs
  from the requested contract).

## R-02: Percent validation

- **Decision**: `@RequestParam @Min(1) @Max(100) int percent`, relying on Spring 6.1+ built-in
  method validation (no class-level `@Validated` needed).
- **Rationale**: A primitive `int` rejects `2.5` and `abc` as type mismatches. A missing parameter
  raises `MissingServletRequestParameterException`. A value out of range raises
  `HandlerMethodValidationException`. All three map to 400 with the standard body (FR-009).
- **Alternatives considered**: a `BigDecimal` percent (rejected: the spec says whole numbers only).

## R-03: Raise arithmetic

- **Decision**:
  `salary.multiply(BigDecimal.valueOf(100 + percent).movePointLeft(2)).setScale(2, RoundingMode.HALF_UP)`.
  The multiplier is an exact decimal (e.g. 1.03), so no division and no intermediate rounding.
- **Rationale**: Exact decimal maths, with a single rounding step at the end (spec US4-AS5:
  1234.57 × 1.03 = 1271.6071 → 1271.61).
- **Alternatives considered**: `double` (prohibited by the constitution).

## R-04: Top earner per department

- **Decision**:
  ```text
  Comparator<Employee> bySalaryThenLowestId =
      Comparator.comparing(Employee::getSalary)
                .thenComparing(Employee::getId, Comparator.reverseOrder());
  groupingBy(Employee::getDepartment, TreeMap::new, maxBy(bySalaryThenLowestId))
  ```
  The map values are unwrapped from `Optional` (never empty within a group). The result is
  returned as a JSON array of `EmployeeResponse`, sorted by department.
- **Rationale**: `BigDecimal`'s natural order uses `compareTo`, so 7000.0 and 7000.00 tie
  correctly. Reversing the id order inside `maxBy` makes the lowest id win a tie (US3-AS3).
  `TreeMap` gives a deterministic output order for tests.
- **Alternatives considered**: returning a `{department: employee}` JSON object (rejected: each
  employee already carries its department, so an array is simpler); a JPQL query (rejected: the
  plan input asks for Streams, and the data is small).

## R-05: Input normalization

- **Decision**: `@NotBlank` on name and department; the service trims both before saving.
  Salary: `@NotNull @Positive @Digits(integer = 13, fraction = 2)`.
- **Rationale**: Trimming once at write time makes department grouping exact (spec Edge Cases).
  `@Digits(fraction = 2)` rejects values with 3 or more decimals. 13 integer digits is a generous
  upper bound that matches the column `precision = 15, scale = 2`.
- **Alternatives considered**: trimming in a Jackson deserializer (more machinery, no benefit).

## R-06: Error body and exception mapping

- **Decision**: An `ErrorResponse` record with exactly `timestamp` (ISO-8601 `Instant`), `status`
  (int), `error` (reason phrase), `message`, and `path`. A single `GlobalExceptionHandler` maps:

  | Exception | Status | Message |
  |---|---|---|
  | `EmployeeNotFoundException` | 404 | `Employee 999 not found` |
  | `MethodArgumentNotValidException` | 400 | Field errors joined, e.g. `name: must not be blank; salary: must be greater than 0` |
  | `HandlerMethodValidationException` | 400 | Parameter errors, e.g. `percent: must be less than or equal to 100` |
  | `MethodArgumentTypeMismatchException` | 400 | `Invalid value 'abc' for parameter 'id'` |
  | `MissingServletRequestParameterException` | 400 | `Required parameter 'percent' is missing` |
  | `HttpMessageNotReadableException` | 400 | `Malformed request body` |
  | Any other `org.springframework.web.ErrorResponse` (404 no route, 405, 415…) | its status | its reason |
  | `Exception` (fallback) | 500 | `Unexpected error` (no internal details) |

- **Rationale**: Covers every error path in the spec (FR-010, US5) with one class. The last two
  rows make sure even framework errors keep the same shape.
- **Alternatives considered**: RFC 7807 `ProblemDetail` (rejected: its field names differ from the
  required five).

## R-07: Testing strategy & traceability

- **Decision**:
  - `EmployeeServiceTest` (Mockito): creation and trimming, not-found, top earners including the
    tie and empty cases, raise maths and rounding, and `verify(repository, never()).save(...)` on
    the read paths.
  - `EmployeeControllerTest` (`@WebMvcTest`, `@MockitoBean EmployeeService`): HTTP status codes,
    the `Location` header, JSON shapes, every validation case, and the error body shape.
  - `EmployeeServiceApplicationSmokeTest` (`@SpringBootTest` + MockMvc + real H2): create, then
    fetch, then preview, then fetch again to prove salaries are unchanged (SC-002 end to end).
  - Test method names start with the scenario id (`us4_as2_…`) so that every acceptance criterion
    can be traced to a test (Constitution I).
- **Rationale**: Three test layers with clear responsibilities; one full-context test keeps the
  suite fast.
- **Note**: Boot 3.4+ deprecates `@MockBean` in favour of `@MockitoBean`.

## R-08: CI

- **Decision**: `.github/workflows/ci.yml` triggers on `push` and `pull_request`, uses
  `actions/setup-java` (Temurin 21, Maven cache), and runs `./mvnw -B verify`.
- **Rationale**: This is the constitution's quality gate. The Maven wrapper pins the Maven version
  so local and CI builds behave the same.

## R-09: README

- **Decision**: A short README covering what the service is, the prerequisites (JDK 21), run, test,
  one `curl` per endpoint (taken from the quickstart), and a "How this was built" section
  (constitution → specify → plan → tasks → implement with Spec Kit, every AI-generated diff
  reviewed by a human before commit, with links to `specs/001-employee-management/`).
