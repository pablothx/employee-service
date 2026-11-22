# employee-service

A small REST service for managing employees (id, name, department, salary), built to show
spec-driven development with an AI assistant and modern Java (21, records, Streams).

It can create an employee, get one by id, list the highest-paid employee per department, and
preview a salary raise without changing stored data. Every error uses the same JSON body.

**Stack:** Java 21 · Spring Boot 3.5 · Spring Web · Spring Data JPA · Bean Validation · H2 (in-memory)

## Run

Requires JDK 21 or newer. No Maven install is needed; the wrapper downloads it.

```bash
./mvnw spring-boot:run
```

The service listens on `http://localhost:8080`. The database is in-memory and starts empty on
every run.

## Test

```bash
./mvnw verify
```

This is the same command CI runs on every push and pull request
([.github/workflows/ci.yml](.github/workflows/ci.yml)). It runs Mockito unit tests for the
service, `@WebMvcTest` tests for the HTTP layer, and one `@SpringBootTest` end-to-end test.

## API

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/employees` | Create an employee → `201` + `Location` |
| `GET` | `/employees/{id}` | Get one employee → `200`, or `404` |
| `GET` | `/employees/stats/top-by-department` | Highest-paid employee per department (ties → lowest id) |
| `POST` | `/employees/raise?percent=N` | Preview a raise of N% (1–100). **Read-only**: stored salaries don't change |

```bash
# Create
curl -i -X POST localhost:8080/employees -H 'Content-Type: application/json' \
  -d '{"name":"Ana","department":"Engineering","salary":5000.00}'

# Get by id
curl -s localhost:8080/employees/1

# Highest-paid per department
curl -s localhost:8080/employees/stats/top-by-department

# Preview a 10% raise (returns new salaries; nothing is stored)
curl -s -X POST 'localhost:8080/employees/raise?percent=10'
```

Every error returns the same five fields:

```json
{
  "timestamp": "2026-11-22T12:22:06.853Z",
  "status": 404,
  "error": "Not Found",
  "message": "Employee 999 not found",
  "path": "/employees/999"
}
```

The full contract is in [specs/001-employee-management/contracts/http-api.md](specs/001-employee-management/contracts/http-api.md).

## How this was built

The service was built with [Spec Kit](https://github.com/github/spec-kit) and an AI coding
assistant. Every step leaves a reviewable document in the repo:

1. **Constitution**: project rules: test-first, controller/service/repository layers,
   constructor injection, records for API types, `BigDecimal` for money, reads never write, and
   `mvn verify` green in CI. See [.specify/memory/constitution.md](.specify/memory/constitution.md).
2. **Specify**: the feature as user stories with numbered acceptance scenarios, and no technology
   choices yet. See [spec.md](specs/001-employee-management/spec.md).
3. **Plan**: the stack, design decisions with alternatives considered, the data model, and the API
   contract. See [plan.md](specs/001-employee-management/plan.md) and
   [research.md](specs/001-employee-management/research.md).
4. **Tasks**: an ordered, test-first task list. See
   [tasks.md](specs/001-employee-management/tasks.md).
5. **Analyze**: a consistency check across spec, plan, and tasks before writing code. It caught,
   for example, that two read endpoints were only tested against mocks, not the real database.
6. **Implement**: story by story. For each story the tests were written first and seen failing,
   then the code made them pass.

Every test method name starts with the acceptance scenario it covers (e.g.
`us4_as2_endToEnd_previewLeavesStoredSalariesUnchanged`), so each requirement can be traced to a
test. **Every AI-generated diff was reviewed by a human before it was committed.**
