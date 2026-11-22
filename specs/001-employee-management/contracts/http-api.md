# HTTP API Contract: Employee Management

Base URL: `http://localhost:8080`. All bodies are `application/json`. Salaries are JSON numbers
with up to 2 decimal places. Every error uses the [error body](#error-body).

## POST /employees — create an employee

Request:

```json
{ "name": "Ana", "department": "Engineering", "salary": 5000.00 }
```

| Outcome | Status | Headers | Body |
|---|---|---|---|
| Created | `201 Created` | `Location: <base>/employees/{id}` (absolute URL) | `EmployeeResponse` |
| Name/department missing or blank; salary missing, ≤ 0, or > 2 decimals | `400 Bad Request` | | Error body; `message` names each invalid field |
| Malformed JSON | `400 Bad Request` | | Error body |

```json
{ "id": 1, "name": "Ana", "department": "Engineering", "salary": 5000.00 }
```

Spec: US1, FR-001–FR-003.

## GET /employees/{id} — get an employee

| Outcome | Status | Body |
|---|---|---|
| Found | `200 OK` | `EmployeeResponse` |
| No employee with that id | `404 Not Found` | Error body, `message: "Employee 999 not found"` |
| Non-numeric id (e.g. `abc`) | `400 Bad Request` | Error body |

Spec: US2, FR-004–FR-005.

## GET /employees/stats/top-by-department — top earner per department

| Outcome | Status | Body |
|---|---|---|
| Always | `200 OK` | Array of `EmployeeResponse`, one per department, sorted by department; `[]` when there are no employees |

Ties within a department go to the lowest `id`. This call does not change stored data.

```json
[
  { "id": 2, "name": "Bob",  "department": "Engineering", "salary": 7000.00 },
  { "id": 3, "name": "Cara", "department": "Sales",       "salary": 4000.00 }
]
```

Spec: US3, FR-006, FR-008.

## POST /employees/raise?percent={N} — preview a raise

`N` is a whole number from 1 to 100 inclusive. There is no request body.

| Outcome | Status | Body |
|---|---|---|
| Valid `N` | `200 OK` | Array of `EmployeeResponse` for all employees, with `salary = round_half_up(salary × (1 + N/100), 2)`; `[]` when there are no employees |
| `N` missing, not a whole number, < 1 or > 100 | `400 Bad Request` | Error body |

This call does not change stored data: a get-by-id afterwards returns the original salary.

Spec: US4, FR-007–FR-009.

## Error body

```json
{
  "timestamp": "2026-11-22T14:03:11.123Z",
  "status": 404,
  "error": "Not Found",
  "message": "Employee 999 not found",
  "path": "/employees/999"
}
```

The body has exactly these five fields for every error, including unknown routes (404),
unsupported methods (405), and unexpected failures (500, with a generic message). Spec: US5,
FR-010.

No other endpoints exist (FR-012).
