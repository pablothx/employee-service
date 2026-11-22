---

description: "Task list for Employee Management implementation"
---

# Tasks: Employee Management

**Input**: Design documents from `specs/001-employee-management/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/http-api.md, quickstart.md

**Tests**: REQUIRED. Constitution Principle I makes test-first non-negotiable. Within each story,
write the test tasks first, run `./mvnw test`, and confirm the new tests **fail** before you
start the implementation tasks.

**Traceability**: Every test method name starts with its spec scenario id, e.g.
`us1_as2_blankName_returns400`. Edge-case tests use `edge_` followed by a short name. See the
[Acceptance → Test map](#acceptance--test-map).

**Organization**: Tasks are grouped by user story. Phases follow spec priority (P1: US1, US2,
US5; P2: US3, US4).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: The user story this task belongs to (US1–US5)

## Path Conventions

Single Maven module at the repository root (plan.md → Project Structure):

- Production code: `src/main/java/dev/pablopetracini/employees/{api,service,persistence}/`
- Tests: `src/test/java/dev/pablopetracini/employees/{api,service}/` and the package root
- Abbreviations used below: `MAIN` = `src/main/java/dev/pablopetracini/employees`,
  `TEST` = `src/test/java/dev/pablopetracini/employees`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: A Maven project that builds, with the CI gate active from the first commit.

- [X] T001 Create `pom.xml`. Coordinates `dev.pablopetracini:employee-service:0.0.1-SNAPSHOT`;
  parent `spring-boot-starter-parent` at the latest 3.5.x; `java.version` 21. Dependencies:
  `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`,
  `com.h2database:h2` (runtime), `spring-boot-starter-test` (test); plugin `spring-boot-maven-plugin`
  (plan.md → Technical Context)
- [X] T002 Add the Maven wrapper (`mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties`) with
  `mvn -N wrapper:wrapper`, and make `mvnw` executable (research.md R-08)
- [X] T003 [P] Create `.gitignore` covering `target/`, `.idea/`, `*.iml`, `.vscode/`, `.DS_Store`
- [X] T004 [P] Create `src/main/resources/application.properties` with
  `spring.datasource.url=jdbc:h2:mem:employees`, `spring.jpa.hibernate.ddl-auto=create-drop`,
  `spring.jpa.open-in-view=false`
- [X] T005 Create `MAIN/EmployeeServiceApplication.java` (`@SpringBootApplication` with a `main`
  method), then run `./mvnw verify` and confirm `BUILD SUCCESS`
- [X] T006 [P] Create `.github/workflows/ci.yml`. Triggers on `push` and `pull_request`;
  `actions/checkout@v4`, `actions/setup-java@v4` (distribution `temurin`, java 21, `cache: maven`);
  step `./mvnw -B verify` (research.md R-08)

**Checkpoint**: `./mvnw verify` is green locally.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: The entity, repository, and shared records that every story uses.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T007 [P] Create the `Employee` JPA entity in `MAIN/persistence/Employee.java` with fields
  data-model.md `Employee.id`, `Employee.name`, `Employee.department`, `Employee.salary`. It has a
  protected no-arg constructor, a public `(name, department, salary)` constructor, getters only,
  and no setters (data-model.md → Entity)
- [X] T008 [P] Create `EmployeeRepository extends JpaRepository<Employee, Long>` in
  `MAIN/persistence/EmployeeRepository.java`, with no custom methods
- [X] T009 [P] Create the `EmployeeResponse` record in `MAIN/api/EmployeeResponse.java` with
  components data-model.md `EmployeeResponse.id`, `EmployeeResponse.name`,
  `EmployeeResponse.department`, `EmployeeResponse.salary`
- [X] T010 [P] Create the `ErrorResponse` record in `MAIN/api/ErrorResponse.java` with components
  data-model.md `ErrorResponse.timestamp`, `ErrorResponse.status`, `ErrorResponse.error`,
  `ErrorResponse.message`, `ErrorResponse.path`
- [X] T011 Create `GlobalExceptionHandler` (`@RestControllerAdvice`) in
  `MAIN/api/GlobalExceptionHandler.java` with no handlers yet. It has only a private helper
  `ResponseEntity<ErrorResponse> body(HttpStatusCode status, String message, HttpServletRequest req)`
  that fills `timestamp=Instant.now()`, the reason phrase from `HttpStatus.resolve(...)`, and
  `path=req.getRequestURI()`. Handlers are added test-first in each story (research.md R-06)

**Checkpoint**: The project compiles; user story work can start.

---

## Phase 3: User Story 1 - Create an employee (Priority: P1) 🎯 MVP

**Goal**: `POST /employees` validates input, stores the employee, and returns 201 with a `Location`
header and the created employee.

**Independent Test**: POST a valid employee and get 201, a `Location` header, and the body with an
id. POST invalid input and get 400 with the error body naming the invalid field.

### Tests for User Story 1 (write first, confirm they FAIL) ⚠️

- [X] T012 [P] [US1] Create `TEST/service/EmployeeServiceTest.java` (JUnit 5 + Mockito;
  `@Mock EmployeeRepository`, service built with `new EmployeeService(repository)`). Tests:
  - `us1_as1_create_savesAndReturnsResponse`: stub `save` to return the entity with id set via
    `ReflectionTestUtils.setField(e, "id", 1L)`, and assert the response matches
  - `edge_create_trimsNameAndDepartment`: `"  Ana "`/`" Engineering "` are saved as
    `"Ana"`/`"Engineering"` (check with an `ArgumentCaptor`)
  - `edge_create_normalizesSalaryToScaleTwo`: a request salary of `5000` is saved and returned as
    `new BigDecimal("5000.00")` (`isEqualTo`, so scale 2 is checked too)
- [X] T013 [P] [US1] Create `TEST/api/EmployeeControllerTest.java`
  (`@WebMvcTest(EmployeeController.class)`, `@MockitoBean EmployeeService`, MockMvc). Tests:
  - `us1_as1_validEmployee_returns201WithLocationAndBody`: `Location` ends with `/employees/1`
  - `us1_as2_missingOrBlankName_returns400NamingName`: parameterized over missing, `""`, `"   "`;
    `$.message` contains `name`
  - `us1_as3_missingOrBlankDepartment_returns400NamingDepartment`: same pattern for department
  - `us1_as4_invalidSalary_returns400NamingSalary`: parameterized over missing, `0`, `-1`
  - `edge_salaryWithThreeDecimals_returns400`: `1000.123`
  - `edge_clientSuppliedIdIsIgnored_returns201`: a body that includes `"id": 99`
  - Each 400 test also asserts `verify(service, never()).create(any())` (nothing stored)

### Implementation for User Story 1

- [X] T014 [P] [US1] Create the `CreateEmployeeRequest` record in
  `MAIN/api/CreateEmployeeRequest.java` with components and Bean Validation annotations from
  data-model.md `CreateEmployeeRequest.name`, `CreateEmployeeRequest.department`,
  `CreateEmployeeRequest.salary`
- [X] T015 [US1] Create `EmployeeService` (`@Service`, constructor-injected
  `private final EmployeeRepository`) in `MAIN/service/EmployeeService.java`, with
  `@Transactional EmployeeResponse create(CreateEmployeeRequest)`. It trims name and department
  before saving, as data-model.md `Employee.name` and `Employee.department` require, and normalizes
  salary with `setScale(2)` before saving (data-model.md `Employee.salary`). That is exact: no
  rounding is ever needed, because `@Digits(fraction = 2)` already rejects more decimals. It maps
  with a private `toResponse(Employee)` method (data-model.md → Mapping)
- [X] T016 [US1] Create `EmployeeController` (`@RestController @RequestMapping("/employees")`,
  constructor-injected `private final EmployeeService`) in `MAIN/api/EmployeeController.java`, with
  `POST` taking `@Valid @RequestBody CreateEmployeeRequest` and returning
  `ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")…)` plus the
  body (contracts/http-api.md → POST /employees)
- [X] T017 [US1] In `MAIN/api/GlobalExceptionHandler.java`, handle `MethodArgumentNotValidException`
  → 400. The message is the field errors as `field: defaultMessage`, sorted by field and joined
  with `"; "` (research.md R-06)

**Checkpoint**: `./mvnw verify` is green, and all US1 tests pass. This is the MVP.

---

## Phase 4: User Story 2 - Get an employee by id (Priority: P1)

**Goal**: `GET /employees/{id}` returns the employee, 404 if it doesn't exist, or 400 if the id
isn't numeric.

**Independent Test**: GET an existing id returns 200. GET `999` returns 404 with
`Employee 999 not found`. GET `abc` returns 400.

### Tests for User Story 2 (write first, confirm they FAIL) ⚠️

- [X] T018 [P] [US2] In `TEST/service/EmployeeServiceTest.java`, add:
  - `us2_as1_findById_returnsEmployee`
  - `us2_as2_findById_unknownId_throwsNotFoundWithMessage`: message `Employee 999 not found`
  - `edge_findById_neverSaves`: `verify(repository, never()).save(any())`
- [X] T019 [P] [US2] In `TEST/api/EmployeeControllerTest.java`, add:
  - `us2_as1_existingId_returns200WithBody`
  - `us2_as2_unknownId_returns404WithMessage`: the service mock throws
    `new EmployeeNotFoundException(999L)`
  - `us2_as3_nonNumericId_returns400`: `GET /employees/abc`

### Implementation for User Story 2

- [X] T020 [P] [US2] Create `EmployeeNotFoundException extends RuntimeException` (constructor
  `(Long id)`, message `"Employee " + id + " not found"`) in
  `MAIN/service/EmployeeNotFoundException.java`
- [X] T021 [US2] In `MAIN/service/EmployeeService.java`, add
  `@Transactional(readOnly = true) EmployeeResponse findById(Long id)`, which throws
  `EmployeeNotFoundException` when the employee is absent
- [X] T022 [US2] In `MAIN/api/EmployeeController.java`, add `GET /{id}` with
  `@PathVariable Long id`
- [X] T023 [US2] In `MAIN/api/GlobalExceptionHandler.java`, handle `EmployeeNotFoundException` → 404
  with the exception message, and `MethodArgumentTypeMismatchException` → 400 with
  `Invalid value '<value>' for parameter '<name>'`

**Checkpoint**: US1 and US2 both pass; `./mvnw verify` is green.

---

## Phase 5: User Story 5 - Consistent error responses (Priority: P1)

**Goal**: Every error, including framework errors and unexpected failures, returns exactly the
five-field body.

**Independent Test**: Trigger a validation error, a not-found error, a malformed body, an unknown
route, a wrong method, and an unexpected exception; every body has exactly `timestamp`, `status`,
`error`, `message`, and `path`.

**Depends on**: US1 and US2 endpoints (it uses them to trigger errors).

### Tests for User Story 5 (write first, confirm they FAIL) ⚠️

- [X] T024 [US5] In `TEST/api/EmployeeControllerTest.java`, add:
  - A private helper `assertStandardErrorBody(ResultActions, int status)` that asserts
    `jsonPath("$.*", hasSize(5))`, that `timestamp`, `status`, `error`, `message`, and `path` exist,
    and that `$.status` equals the given status. Every error test in this file uses it from here on
  - `us5_as1_allErrorBodiesHaveExactlyFiveFields`: parameterized over the 400 (invalid create),
    404 (unknown id), 400 (`abc` id), and 400 (malformed JSON) cases, each checked with
    `assertStandardErrorBody`
  - `us5_as2_notFoundBody_hasStatusErrorMessagePath`: `status=404`, `error="Not Found"`,
    `message="Employee 999 not found"`, `path="/employees/999"`
  - `us5_as3_malformedBody_returns400StandardBody`: body `{"name":`
  - `edge_unknownRoute_returns404StandardBody`: `GET /nope`
  - `edge_wrongMethod_returns405StandardBody`: `DELETE /employees/1`
  - `edge_unexpectedException_returns500WithoutInternalDetails`: the service mock throws
    `RuntimeException("db password leaked")`; assert `message="Unexpected error"` and that the body
    doesn't contain `"leaked"`

### Implementation for User Story 5

- [X] T025 [US5] In `MAIN/api/GlobalExceptionHandler.java`, handle `HttpMessageNotReadableException`
  → 400 `Malformed request body`, and add a fallback `@ExceptionHandler(Exception.class)`. If the
  exception `instanceof org.springframework.web.ErrorResponse`, use its `getStatusCode()` and
  reason phrase (this covers 404 no route, 405, and 415). Otherwise return 500
  `Unexpected error` (research.md R-06)

**Checkpoint**: Every error path returns the standard body; `./mvnw verify` is green.

---

## Phase 6: User Story 3 - Highest-paid employee per department (Priority: P2)

**Goal**: `GET /employees/stats/top-by-department` returns one top earner per department, ties go
to the lowest id, and it changes nothing.

**Independent Test**: Seed Engineering (Ana 5000, Bob 7000) and Sales (Cara 4000); the result is
`[Bob, Cara]`. An empty store gives `[]`.

### Tests for User Story 3 (write first, confirm they FAIL) ⚠️

- [X] T026 [P] [US3] In `TEST/service/EmployeeServiceTest.java` (stub `repository.findAll()`;
  set ids with `ReflectionTestUtils`), add:
  - `us3_as1_topEarners_returnsOnePerDepartmentSortedByDepartment`
  - `us3_as2_topEarners_emptyStore_returnsEmptyList`
  - `us3_as3_topEarners_tie_returnsLowestId`: use salaries `7000.0` and `7000.00` to prove the
    comparison ignores scale
  - `us3_as4_topEarners_neverSaves`
  - `edge_topEarners_departmentsAreCaseSensitive`: `Engineering` and `engineering` each get their
    own entry
- [X] T027 [P] [US3] In `TEST/api/EmployeeControllerTest.java`, add
  `us3_as1_topByDepartment_returns200Array` and `us3_as2_topByDepartment_empty_returnsEmptyArray`

### Implementation for User Story 3

- [X] T028 [US3] In `MAIN/service/EmployeeService.java`, add
  `@Transactional(readOnly = true) List<EmployeeResponse> topEarnersByDepartment()`, using
  `groupingBy(Employee::getDepartment, TreeMap::new, maxBy(comparing(Employee::getSalary).thenComparing(Employee::getId, reverseOrder())))`
  and unwrapping the `Optional`s (research.md R-04)
- [X] T029 [US3] In `MAIN/api/EmployeeController.java`, add `GET /stats/top-by-department`
  (contracts/http-api.md)

**Checkpoint**: US3 passes; `./mvnw verify` is green.

---

## Phase 7: User Story 4 - Preview a salary raise (Priority: P2)

**Goal**: `POST /employees/raise?percent=N` (N a whole number from 1 to 100) returns every employee
with the raised salary, rounded to 2 decimals half-up, and leaves stored data unchanged.

**Independent Test**: Seed Ana 5000.00 and Bob 7000.00; `percent=10` returns 5500.00 and 7700.00;
a later GET of each still shows the original salary. Out-of-range or non-integer percents return
400.

### Tests for User Story 4 (write first, confirm they FAIL) ⚠️

- [X] T030 [P] [US4] In `TEST/service/EmployeeServiceTest.java`, add:
  - `us4_as1_previewRaise_tenPercent`: assert `isEqualTo(new BigDecimal("5500.00"))`, which also
    checks scale 2
  - `us4_as2_previewRaise_doesNotMutateEntitiesOrSave`: entity salaries are unchanged and `save`
    is never called
  - `us4_as5_previewRaise_roundsHalfUp`: `1234.57` at 3% gives `1271.61`
  - `us4_as6_previewRaise_emptyStore_returnsEmptyList`
- [X] T031 [P] [US4] In `TEST/api/EmployeeControllerTest.java`, add:
  - `us4_as3_percentBoundaries_accepted`: parameterized over `1` and `100`, returns 200
  - `us4_as4_invalidPercent_returns400`: parameterized over `0`, `101`, `-5`, `2.5`, `abc`, and
    missing; check each with `assertStandardErrorBody(…, 400)` (from T024, so exactly five fields,
    as US5-AS1 and SC-003 require) and `verify(service, never()).previewRaise(anyInt())`
- [X] T032 [P] [US4] Create `TEST/EmployeeServiceApplicationSmokeTest.java` (`@SpringBootTest`,
  `@AutoConfigureMockMvc`, real H2). Each test inserts its own data and reads ids from `Location`.
  It reads stored state straight from `EmployeeRepository` (constructor-injected into the test
  class), so the checks go to the database instead of trusting the endpoint under test.
  Constitution V requires a real-database check for every read endpoint:
  - `us4_as2_endToEnd_previewLeavesStoredSalariesUnchanged`: POST Ana and Bob, POST
    `/employees/raise?percent=10` and assert 5500.00/7700.00, then assert from the repository that
    the stored salaries are still 5000.00/7000.00 (SC-002)
  - `us2_as1_endToEnd_getByIdLeavesStoredDataUnchanged`: POST Ana, record the repository's
    name/department/salary, GET `/employees/{id}`, and assert the repository values are identical
  - `us3_as4_endToEnd_topByDepartmentLeavesStoredDataUnchanged`: POST Ana, Bob, and Cara, record
    every stored row, GET `/employees/stats/top-by-department`, and assert all rows are identical
    (count and every field)
  - Call `repository.deleteAll()` in `@BeforeEach` so no data carries over between tests

### Implementation for User Story 4

- [X] T033 [US4] In `MAIN/service/EmployeeService.java`, add
  `@Transactional(readOnly = true) List<EmployeeResponse> previewRaise(int percent)`. It builds new
  `EmployeeResponse` records with
  `salary.multiply(BigDecimal.valueOf(100 + percent).movePointLeft(2)).setScale(2, RoundingMode.HALF_UP)`
  and never calls an entity setter or `save` (research.md R-03; data-model.md `Employee.salary`,
  `EmployeeResponse.salary`)
- [X] T034 [US4] In `MAIN/api/EmployeeController.java`, add `POST /raise` with
  `@RequestParam @Min(1) @Max(100) int percent` (research.md R-01, R-02)
- [X] T035 [US4] In `MAIN/api/GlobalExceptionHandler.java`, handle `HandlerMethodValidationException`
  → 400 (message `percent: <constraint message>`) and `MissingServletRequestParameterException`
  → 400 `Required parameter 'percent' is missing`

**Checkpoint**: All five stories pass; `./mvnw verify` is green, including the smoke test.

---

## Phase 8: Polish & Cross-Cutting Concerns

- [X] T036 [P] Write `README.md`: a one-paragraph purpose, prerequisites (JDK 21), run
  (`./mvnw spring-boot:run`), test (`./mvnw verify`), one `curl` per endpoint (taken from
  quickstart.md), and a "How this was built" section (constitution → specify → plan → tasks →
  implement with Spec Kit; every AI-generated diff reviewed by a human before commit; links to
  `.specify/memory/constitution.md` and `specs/001-employee-management/`) (research.md R-09)
- [X] T037 Constitution compliance sweep across `src/`:
  - `grep -rn "@Autowired" src/main` is empty (III)
  - `grep -rnE "\b(double|float|Double|Float)\b" src/main` is empty (Tech: BigDecimal)
  - No `Employee` type appears in a controller method signature (IV)
  - Every scenario id in the [map below](#acceptance--test-map) has a matching test method:
    `grep -rhoE "us[1-5]_as[0-9]+" src/test | sort -u` lists all 20 (I)
- [X] T038 Run every scenario in `specs/001-employee-management/quickstart.md` against
  `./mvnw spring-boot:run` and confirm the expected outputs
- [X] T039 Run `./mvnw verify` one last time, push, and confirm the GitHub Actions `ci` workflow is
  green (Quality Gate)

---

## Acceptance → Test map

| Scenario | Test method(s) | File(s) | Task |
|---|---|---|---|
| US1-AS1 | `us1_as1_*` | EmployeeServiceTest, EmployeeControllerTest | T012, T013 |
| US1-AS2 | `us1_as2_*` | EmployeeControllerTest | T013 |
| US1-AS3 | `us1_as3_*` | EmployeeControllerTest | T013 |
| US1-AS4 | `us1_as4_*` | EmployeeControllerTest | T013 |
| US2-AS1 | `us2_as1_*` | EmployeeServiceTest, EmployeeControllerTest, EmployeeServiceApplicationSmokeTest | T018, T019, T032 |
| US2-AS2 | `us2_as2_*` | EmployeeServiceTest, EmployeeControllerTest | T018, T019 |
| US2-AS3 | `us2_as3_*` | EmployeeControllerTest | T019 |
| US3-AS1 | `us3_as1_*` | EmployeeServiceTest, EmployeeControllerTest | T026, T027 |
| US3-AS2 | `us3_as2_*` | EmployeeServiceTest, EmployeeControllerTest | T026, T027 |
| US3-AS3 | `us3_as3_*` | EmployeeServiceTest | T026 |
| US3-AS4 | `us3_as4_*` | EmployeeServiceTest, EmployeeServiceApplicationSmokeTest | T026, T032 |
| US4-AS1 | `us4_as1_*` | EmployeeServiceTest | T030 |
| US4-AS2 | `us4_as2_*` | EmployeeServiceTest, EmployeeServiceApplicationSmokeTest | T030, T032 |
| US4-AS3 | `us4_as3_*` | EmployeeControllerTest | T031 |
| US4-AS4 | `us4_as4_*` | EmployeeControllerTest | T031 |
| US4-AS5 | `us4_as5_*` | EmployeeServiceTest | T030 |
| US4-AS6 | `us4_as6_*` | EmployeeServiceTest | T030 |
| US5-AS1 | `us5_as1_*` | EmployeeControllerTest | T024 |
| US5-AS2 | `us5_as2_*` | EmployeeControllerTest | T024 |
| US5-AS3 | `us5_as3_*` | EmployeeControllerTest | T024 |

Spec edge cases are covered by `edge_*` tests and the scenario tests: blank/whitespace input
(T013), salary with more than 2 decimals (T013), client-supplied id ignored (T013), trimming for
department grouping (T012), case-sensitive department grouping (T026), half-up rounding (T030,
`us4_as5`), and no internal details in 500 errors (T024). Salary scale normalization (T012) has no
spec edge case; it keeps the create response consistent with later reads.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: no dependencies
- **Foundational (Phase 2)**: depends on Setup; blocks all stories
- **US1 (Phase 3)**: depends on Foundational
- **US2 (Phase 4)**: depends on Foundational. It shares `EmployeeService`, `EmployeeController`, and
  their test files with US1, so do it after US1
- **US5 (Phase 5)**: depends on US1 and US2, whose endpoints it uses to trigger errors
- **US3 (Phase 6)**: depends on Foundational and on US1's service and controller classes existing
- **US4 (Phase 7)**: depends on US1 (create, used by the smoke test), US2 (GET used by the smoke
  test, and the type-mismatch handler that covers `percent=2.5` and `abc`)
- **Polish (Phase 8)**: depends on all stories

```text
Setup → Foundational → US1 → US2 → US5
                              └──→ US3 → US4 → Polish
```

### Within Each Story

1. Test tasks first, then run `./mvnw test` and **confirm they fail**
2. Records and exceptions, then service, then controller, then exception handler
3. `./mvnw verify` green at the checkpoint before moving on

### Parallel Opportunities

- Setup: T003, T004, T006 in parallel after T001
- Foundational: T007–T010 in parallel; T011 after T010
- In each story, the service test and controller test tasks are [P] (different files)
- US1: T014 can run in parallel with the tests; US2: T020 can run in parallel with the tests
- US4: T030, T031, and T032 are three different test files and can run in parallel. T032 also
  holds the real-database "reads change nothing" checks for US2 and US3, which need those
  endpoints (already built by then)
- Across stories, little runs in parallel: stories share `EmployeeService`, `EmployeeController`,
  and `GlobalExceptionHandler`, so they run in sequence

---

## Parallel Example: User Story 1

```bash
# Tests first, in parallel (different files):
Task: "T012 Service tests for create + trimming in TEST/service/EmployeeServiceTest.java"
Task: "T013 Controller tests for POST /employees in TEST/api/EmployeeControllerTest.java"
Task: "T014 CreateEmployeeRequest record in MAIN/api/CreateEmployeeRequest.java"

# Then, in order: T015 service → T016 controller → T017 validation handler
```

## Parallel Example: User Story 4

```bash
Task: "T030 Raise preview service tests in TEST/service/EmployeeServiceTest.java"
Task: "T031 Raise preview controller tests in TEST/api/EmployeeControllerTest.java"
Task: "T032 End-to-end smoke test in TEST/EmployeeServiceApplicationSmokeTest.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1 Setup, then Phase 2 Foundational
2. Phase 3 US1 (test first)
3. **Stop and validate**: `./mvnw verify` is green, and quickstart step 1 works

### Incremental Delivery (fits the 2-hour time box)

| Increment | Phases | Approx. time |
|---|---|---|
| Foundation | 1–2 | 15 min |
| MVP: create | 3 (US1) | 25 min |
| Read + errors | 4–5 (US2, US5) | 25 min |
| Reporting | 6 (US3) | 15 min |
| Raise preview | 7 (US4) | 20 min |
| Polish | 8 | 20 min |

If the time box is at risk, cut from the bottom (US4, then US3) rather than skipping tests
(Constitution VI).

---

## Notes

- [P] = different files, no dependency on incomplete tasks
- Never disable or delete a test to get a green build
- Commit after each story checkpoint; every diff is reviewed by a human before commit
- Nothing outside these tasks gets built (FR-012, Constitution VI)
