# Quickstart & Validation: Employee Management

## Prerequisites

- JDK 21 on `PATH` (`java -version` shows 21)
- No Maven install needed; use the wrapper `./mvnw`
- `curl`; `jq` is optional

## Build & test

```bash
./mvnw verify
```

Expected: `BUILD SUCCESS`. Service, controller, and smoke tests all pass. This is the same
command CI runs.

## Run

```bash
./mvnw spring-boot:run
```

The service starts on `http://localhost:8080` with an empty in-memory database.

## Validation scenarios

Run these in order against a freshly started service. Endpoint details are in
[contracts/http-api.md](contracts/http-api.md).

1. **Create (US1)**:
   ```bash
   curl -i -X POST localhost:8080/employees -H 'Content-Type: application/json' \
     -d '{"name":"Ana","department":"Engineering","salary":5000.00}'
   curl -s -X POST localhost:8080/employees -H 'Content-Type: application/json' \
     -d '{"name":"Bob","department":"Engineering","salary":7000.00}'
   curl -s -X POST localhost:8080/employees -H 'Content-Type: application/json' \
     -d '{"name":"Cara","department":"Sales","salary":4000.00}'
   ```
   Expect `201`, `Location: http://localhost:8080/employees/1`, and an employee body with id 1.

2. **Invalid create (US1, US5)**:
   ```bash
   curl -s -X POST localhost:8080/employees -H 'Content-Type: application/json' \
     -d '{"name":" ","department":"Sales","salary":0}'
   ```
   Expect `400`, a body with the five error fields, and a message naming `name` and `salary`.

3. **Get by id (US2)**: `curl -s localhost:8080/employees/1` returns Ana.
   `curl -s localhost:8080/employees/999` returns `404` with message `Employee 999 not found`.
   `curl -s localhost:8080/employees/abc` returns `400`.

4. **Top earner per department (US3)**: `curl -s localhost:8080/employees/stats/top-by-department`
   returns `[Bob (Engineering), Cara (Sales)]`.

5. **Raise preview (US4)**: `curl -s -X POST 'localhost:8080/employees/raise?percent=10'` returns
   Ana at 5500.00, Bob at 7700.00, and Cara at 4400.00.
   Then `curl -s localhost:8080/employees/1` must still show `5000.00`.
   `percent=0`, `percent=101`, `percent=2.5`, and a missing percent each return `400`.

If every step matches, the feature meets the spec's acceptance scenarios end to end.
