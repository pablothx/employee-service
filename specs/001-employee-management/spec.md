# Feature Specification: Employee Management

**Feature Branch**: `001-employee-management` (no branch created; work is on `main`)

**Created**: 2026-11-22

**Status**: Draft

**Input**: User description: "A small public portfolio service to manage employees (id, name,
department, salary). Purpose: show how I build with AI using spec-driven development, and practice
modern Java. Users can: create an employee with validation (name and department required,
salary > 0), get an employee by id (clear error if not found), see the highest-paid employee per
department, and preview a salary raise by a percent (1 to 100) that returns a new list without
changing stored data. Errors use a consistent body (timestamp, status, error, message, path).
Out of scope: auth, pagination, frontend, Docker, extra endpoints."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Create an employee (Priority: P1)

A client registers a new employee by giving a name, a department, and a salary. The service
checks the input, stores the employee, assigns an id, and returns the stored employee.

**Why this priority**: No other capability is useful without employees in the system.

**Independent Test**: Submit a valid employee and confirm the response contains a new id and the
submitted values; submit invalid input and confirm it is rejected with the standard error body.

**Acceptance Scenarios**:

1. **Given** no employees exist, **When** a client creates an employee with name "Ana",
   department "Engineering", and salary 5000.00, **Then** the service confirms creation and returns
   the employee with a system-assigned id and the same name, department, and salary.
2. **Given** any state, **When** a client creates an employee with a missing or blank name,
   **Then** the request is rejected as invalid, nothing is stored, and the error body names the
   name field as the problem.
3. **Given** any state, **When** a client creates an employee with a missing or blank department,
   **Then** the request is rejected as invalid, nothing is stored, and the error body names the
   department field as the problem.
4. **Given** any state, **When** a client creates an employee with a salary that is missing, zero,
   or negative, **Then** the request is rejected as invalid, nothing is stored, and the error body
   names the salary field as the problem.

---

### User Story 2 - Get an employee by id (Priority: P1)

A client looks up one employee by id and gets that employee's details, or a clear "not found"
error if no employee has that id.

**Why this priority**: Retrieval proves creation worked and is the simplest read in the service.

**Independent Test**: Create an employee, fetch it by its id, and compare; fetch an unknown id and
check the not-found error.

**Acceptance Scenarios**:

1. **Given** an employee with id X exists, **When** a client requests employee X, **Then** the
   service returns that employee's id, name, department, and salary.
2. **Given** no employee with id 999 exists, **When** a client requests employee 999, **Then** the
   service responds with a not-found error whose message states that employee 999 was not found.
3. **Given** any state, **When** a client requests an employee with an id that is not a valid id
   (e.g., "abc"), **Then** the service responds with an invalid-request error using the standard
   error body.

---

### User Story 3 - Highest-paid employee per department (Priority: P2)

A client asks which employee earns the most in each department and gets one employee for each
department that has employees.

**Why this priority**: This is the first reporting feature built on stored data, but it depends on
creation already working.

**Independent Test**: Create employees across two or more departments with known salaries and
check that the result holds exactly the top earner of each department.

**Acceptance Scenarios**:

1. **Given** Engineering has Ana (5000) and Bob (7000), and Sales has Cara (4000), **When** a
   client requests the highest-paid employee per department, **Then** the result holds exactly two
   entries: Engineering → Bob and Sales → Cara.
2. **Given** no employees exist, **When** a client requests the highest-paid employee per
   department, **Then** the result is empty and no error is returned.
3. **Given** two employees in the same department share the highest salary, **When** a client
   requests the highest-paid employee per department, **Then** exactly one of them is returned for
   that department: the one with the lowest id.
4. **Given** any stored employees, **When** a client requests the highest-paid employee per
   department, **Then** the stored employee data is unchanged afterwards.

---

### User Story 4 - Preview a salary raise (Priority: P2)

A client gives a raise percentage and gets back a list of all employees with their salaries as they
would be after that raise. Nothing stored changes; this is a "what-if" preview only.

**Why this priority**: It shows a calculation on money values and the rule that reads never change
data, but it is not needed for basic employee management.

**Independent Test**: Create employees, request a preview with a known percentage, check the
calculated salaries, then fetch the employees again and confirm their stored salaries are unchanged.

**Acceptance Scenarios**:

1. **Given** Ana earns 5000.00 and Bob earns 7000.00, **When** a client previews a 10 percent
   raise, **Then** the result lists Ana at 5500.00 and Bob at 7700.00.
2. **Given** the preview in scenario 1 has been requested, **When** a client then fetches Ana and
   Bob by id, **Then** their stored salaries are still 5000.00 and 7000.00.
3. **Given** any state, **When** a client previews a raise of 1 or 100 percent, **Then** the request
   is accepted (both limits are inclusive).
4. **Given** any state, **When** a client previews a raise of 0, 101, a negative number, a
   non-number, or omits the percentage, **Then** the request is rejected as invalid with the
   standard error body.
5. **Given** an employee earns 1234.57, **When** a client previews a 3 percent raise, **Then** the
   previewed salary is 1271.61 (1271.6071 rounded to 2 decimal places, half-up).
6. **Given** no employees exist, **When** a client previews a valid raise, **Then** the result is an
   empty list.

---

### User Story 5 - Consistent error responses (Priority: P1)

Every error the service returns uses the same body, so clients can handle every failure the
same way.

**Why this priority**: It applies across every other story and is part of each of their
acceptance criteria.

**Independent Test**: Trigger a validation error, a not-found error, and a malformed request,
and check that each body has the same five fields.

**Acceptance Scenarios**:

1. **Given** any error case in stories 1–4, **When** the service responds, **Then** the body
   contains exactly these fields: timestamp, status, error, message, and path.
2. **Given** a not-found error for employee 999, **When** the client reads the body, **Then**
   status is the not-found status code, error is its standard reason phrase, message describes the
   missing employee, and path is the requested path.
3. **Given** a request body that cannot be read (e.g., malformed content), **When** the service
   responds, **Then** it uses the standard error body with an invalid-request status.

### Edge Cases

- Name or department containing only whitespace counts as blank and is rejected.
- Salary with more than 2 decimal places is rejected as invalid.
- A client-supplied id on creation is ignored; the system always assigns the id.
- Department names are compared exactly (case-sensitive, after trimming leading/trailing spaces)
  when grouping for the highest-paid report.
- Raise previews are rounded to 2 decimal places using half-up rounding.
- An unexpected internal failure still returns the standard error body, without exposing internal
  details in the message.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST let clients create an employee with name, department, and salary, and
  MUST assign a unique id to each created employee.
- **FR-002**: System MUST reject creation when name is missing or blank, department is missing or
  blank, or salary is missing, zero, negative, or has more than 2 decimal places. Nothing is stored
  in these cases.
- **FR-003**: System MUST return the created employee (id, name, department, salary) after a
  successful creation.
- **FR-004**: System MUST let clients retrieve a single employee by id.
- **FR-005**: System MUST respond with a not-found error that names the requested id when no
  employee has that id.
- **FR-006**: System MUST return, for each department that has at least one employee, the single
  employee with the highest salary in it. Ties are broken by lowest id.
- **FR-007**: System MUST let clients preview a salary raise for a percentage from 1 to 100
  (inclusive), returning every employee with salary × (1 + percent/100), rounded to 2 decimal places
  half-up.
- **FR-008**: The raise preview, the highest-paid report, and get-by-id MUST NOT change any
  stored employee data.
- **FR-009**: System MUST reject raise previews whose percentage is missing, not a whole number,
  or outside the range 1–100.
- **FR-010**: Every error response MUST use one body containing timestamp, status, error, message,
  and path.
- **FR-011**: Salary values MUST be represented exactly, with no floating-point rounding error.
- **FR-012**: The service MUST NOT expose any capability beyond the four listed above (create, get
  by id, highest-paid per department, raise preview).

### Key Entities

- **Employee**: A person employed by the organization. Attributes: id (system-assigned, unique),
  name (required text), department (required text), salary (required positive monetary amount, at
  most 2 decimal places, single implicit currency).
- **Raise Preview Entry**: A computed, non-stored view of an employee with the salary they would
  have after a given raise percentage.
- **Error Body**: The standard failure description: timestamp (when the error occurred), status
  (numeric status code), error (short reason phrase), message (human-readable explanation), path
  (the requested path).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of acceptance scenarios in this spec are covered by at least one automated test,
  and all of those tests pass.
- **SC-002**: After any number of raise previews, 100% of stored salaries are identical to their
  values before the previews.
- **SC-003**: 100% of error responses across all error scenarios contain all five standard error
  fields.
- **SC-004**: A reviewer new to the project can create an employee and run each of the other
  three capabilities within 10 minutes, using only the project's README.
- **SC-005**: The feature is delivered within the project's 2-hour time box with the full build and
  test suite passing in CI.

## Assumptions

- The service is a public demo for a portfolio: there are no users, roles, or authentication.
  All clients have equal access.
- The raise percentage is a whole number. Fractional percentages such as 2.5 are treated as invalid.
- The raise preview covers all stored employees; filtering by department is not supported.
- All salaries are in one implicit currency; currency codes are not stored or shown.
- Data does not need to survive a service restart; an empty store at startup is acceptable.
- The number of employees is small (hundreds at most), so results are returned in full without
  pagination.
- Updating and deleting employees, and listing all employees, are out of scope, along with auth,
  pagination, frontend, and containerization.
