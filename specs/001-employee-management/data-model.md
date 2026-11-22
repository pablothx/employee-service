# Data Model: Employee Management

All types live under the base package `dev.pablopetracini.employees`: the entity in `.persistence`,
the records in `.api`.

## Entity: `Employee` (JPA, package `dev.pablopetracini.employees.persistence`)

| Field | Type | Column | Rules |
|---|---|---|---|
| `id` | `Long` | PK, `@GeneratedValue(strategy = IDENTITY)` | Assigned by the system; never taken from client input |
| `name` | `String` | `NOT NULL` | Stored trimmed; non-blank |
| `department` | `String` | `NOT NULL` | Stored trimmed; non-blank; case-sensitive |
| `salary` | `BigDecimal` | `NOT NULL`, `precision = 15, scale = 2` | > 0, at most 2 decimal places; normalized to scale 2 before saving (`5000` → `5000.00`) |

- Has a protected no-arg constructor (for JPA) and a public constructor `(name, department, salary)`.
- Has getters only; nothing changes an employee after it is created (no update in scope).
- Is never exposed through the API (Constitution IV).
- Has no relationships and no state transitions.

## Record: `CreateEmployeeRequest` (API input)

| Component | Type | Validation |
|---|---|---|
| `name` | `String` | `@NotBlank` |
| `department` | `String` | `@NotBlank` |
| `salary` | `BigDecimal` | `@NotNull`, `@Positive`, `@Digits(integer = 13, fraction = 2)` |

Unknown JSON properties (including `id`) are ignored, which is Spring Boot's Jackson default.

## Record: `EmployeeResponse` (API output)

`id: Long`, `name: String`, `department: String`, `salary: BigDecimal`

It is used by get-by-id, the top-earner list, and the raise preview. In the preview, `salary` holds
the computed value; the stored value is not changed.

## Record: `ErrorResponse` (API error output)

| Component | Type | Example |
|---|---|---|
| `timestamp` | `Instant` (serialized as ISO-8601) | `2026-11-22T14:03:11.123Z` |
| `status` | `int` | `404` |
| `error` | `String` | `Not Found` |
| `message` | `String` | `Employee 999 not found` |
| `path` | `String` | `/employees/999` |

## Mapping

The service converts `Employee` to `EmployeeResponse` with one private method, without a mapping
library. A raise preview builds `new EmployeeResponse(e.getId(), e.getName(), e.getDepartment(),
raised(e.getSalary(), percent))` and never calls a setter on the entity.
