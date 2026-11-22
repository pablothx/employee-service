# Implementation Plan: Employee Management

**Branch**: `001-employee-management` | **Date**: 2026-11-22 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/001-employee-management/spec.md`

## Summary

This is a single Spring Boot REST service with four endpoints: create an employee, get one by id,
list the top earner per department, and preview a salary raise without changing anything. It
uses the controller → service → repository layers, with request/response records kept separate
from one JPA entity. Salaries are `BigDecimal`. Errors come from one `@RestControllerAdvice` that
returns a five-field error body. H2 in-memory is the storage. Development is test-first: Mockito
unit tests for the service, `@WebMvcTest` slices for the controller, and one `@SpringBootTest`
smoke test. GitHub Actions runs `mvn verify`.

## Technical Context

**Language/Version**: Java 21 (LTS)

**Maven Coordinates**: groupId `dev.pablopetracini`, artifactId `employee-service`; base package
`dev.pablopetracini.employees`

**Primary Dependencies**: Spring Boot 3.5.x (latest 3.x patch, pinned in `pom.xml`):
`spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`;
runtime `com.h2database:h2`; test `spring-boot-starter-test` (JUnit 5, Mockito, AssertJ, MockMvc)

**Storage**: H2 in-memory. The schema is generated from the entity
(`ddl-auto=create-drop`) and the data is lost on restart, which matches the spec's assumption.

**Testing**: JUnit 5 + Mockito (service), `@WebMvcTest` + `@MockitoBean` (controller and error
handler), one `@SpringBootTest` + `@AutoConfigureMockMvc` smoke test. All run via `mvn verify`.

**Target Platform**: Any JVM 21 host; run locally with `./mvnw spring-boot:run`

**Project Type**: Web service (single-module REST API)

**Performance Goals**: None beyond normal interactive response times; the data set is in the
hundreds of rows at most (spec Assumptions)

**Constraints**: 2-hour time box (Constitution VI); no auth, pagination, frontend, Docker, or
extra endpoints (FR-012); money is exact (FR-011 → `BigDecimal`)

**Scale/Scope**: 4 endpoints, 1 entity, about 10 production classes

No NEEDS CLARIFICATION items remain; the stack was given in the plan input. Remaining design
choices are recorded in [research.md](research.md).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | How the plan complies | Status |
|---|---|---|
| I. Test-First | Each acceptance scenario gets a test named after its id (e.g. `us1_as2_blankName_returns400`). Tests are written and seen failing before the code. The scenario → test mapping is listed in tasks.md | PASS |
| II. Layered Architecture | `EmployeeController` → `EmployeeService` → `EmployeeRepository`. The controller never touches the repository | PASS |
| III. Constructor Injection | All beans use constructors with `final` fields, and there is no `@Autowired` on fields. Tests use `@MockitoBean` or plain constructors | PASS |
| IV. Immutable API Contracts | `CreateEmployeeRequest`, `EmployeeResponse`, and `ErrorResponse` are records. The `Employee` entity is never serialized, and mapping is explicit in the service | PASS |
| V. Side-Effect-Free Reads | `findById`, `topEarnersByDepartment`, and `previewRaise` are `@Transactional(readOnly = true)` and build new records without touching entities. Unit tests assert `save` is never called. The `@SpringBootTest` smoke test checks, against the real database, that every read endpoint (get-by-id, top-by-department, raise preview) leaves all stored fields unchanged. See the R-01 note on POST | PASS (with note) |
| VI. Minimal Scope | Exactly the 4 endpoints from the spec, and no extra abstractions (no mapper library, no service interface + impl split) | PASS |
| Tech: BigDecimal | Salary is `BigDecimal` in the entity and records, compared with `compareTo`, raise uses `setScale(2, RoundingMode.HALF_UP)` | PASS |
| Quality Gate: `mvn verify` | The GitHub Actions workflow runs `mvn -B verify` on push and pull request | PASS |

**Post-design re-check (after Phase 1)**: PASS. The data model, contracts, and quickstart add no
endpoints, layers, or dependencies beyond those listed above.

## Project Structure

### Documentation (this feature)

```text
specs/001-employee-management/
├── plan.md              # This file
├── research.md          # Phase 0: design decisions
├── data-model.md        # Phase 1: entity, records, validation
├── quickstart.md        # Phase 1: run/validate guide
├── contracts/
│   └── http-api.md      # Phase 1: endpoint contract
└── tasks.md             # Phase 2 (/speckit-tasks — not created here)
```

### Source Code (repository root)

```text
pom.xml
mvnw, mvnw.cmd, .mvn/wrapper/
README.md
.github/workflows/ci.yml

src/main/java/dev/pablopetracini/employees/
├── EmployeeServiceApplication.java
├── api/
│   ├── EmployeeController.java
│   ├── CreateEmployeeRequest.java      # record + Bean Validation
│   ├── EmployeeResponse.java           # record
│   ├── ErrorResponse.java              # record: timestamp, status, error, message, path
│   └── GlobalExceptionHandler.java     # @RestControllerAdvice
├── service/
│   ├── EmployeeService.java
│   └── EmployeeNotFoundException.java
└── persistence/
    ├── Employee.java                   # @Entity
    └── EmployeeRepository.java         # JpaRepository<Employee, Long>

src/main/resources/application.properties

src/test/java/dev/pablopetracini/employees/
├── service/EmployeeServiceTest.java            # JUnit 5 + Mockito
├── api/EmployeeControllerTest.java             # @WebMvcTest (+ error handler)
└── EmployeeServiceApplicationSmokeTest.java    # @SpringBootTest end-to-end
```

**Structure Decision**: A single Maven module with packages by layer (`api`, `service`,
`persistence`), which maps directly to Constitution II. Maven coordinates are
`dev.pablopetracini:employee-service`, and the base package is `dev.pablopetracini.employees`.

## Complexity Tracking

No constitution violations need justifying.
