# Employee Service Constitution

## Core Principles

### I. Test-First (NON-NEGOTIABLE)

- Tests MUST be written before the production code they exercise, and MUST be observed
  failing before the implementation is written (red → green → refactor).
- Every acceptance criterion in a feature spec MUST map to at least one automated test, and
  that mapping MUST be traceable (e.g., test name or comment referencing the criterion).
- A change that adds behavior without a corresponding failing-first test MUST NOT be merged.

Rationale: tests written first prove the spec is understood and keep acceptance criteria
verifiable rather than aspirational.

### II. Layered Architecture

- Code MUST be organized into three layers: controller → service → repository.
- Controllers handle HTTP concerns only (routing, validation, DTO mapping, status codes) and
  MUST NOT call repositories directly.
- Services hold business logic and transaction boundaries.
- Repositories handle persistence only and MUST NOT contain business rules.
- Dependencies MUST point downward only; no layer may depend on a layer above it.

Rationale: clear boundaries make each layer independently testable and keep changes local.

### III. Constructor Injection Only

- Dependencies MUST be supplied via constructor injection.
- Field injection and setter injection (`@Autowired` on fields or setters) are prohibited.
- Injected dependencies MUST be declared `final`.

Rationale: constructor injection makes dependencies explicit, enables immutability, and allows
plain unit tests without a Spring context.

### IV. Immutable API Contracts

- Request and response DTOs exposed by the API MUST be Java `record` types.
- DTOs MUST be separate from JPA entities; entities MUST NOT be serialized in API responses or
  bound from API requests.
- Mapping between DTOs and entities MUST happen explicitly at the controller/service boundary.

Rationale: decoupling the wire contract from the persistence model prevents accidental schema
leaks, lazy-loading surprises, and unintended mass assignment.

### V. Side-Effect-Free Reads

- Any endpoint whose purpose is to read data (e.g., `GET`) MUST NOT create, update, or delete
  persisted state.
- Read paths in services SHOULD be marked read-only transactions (`@Transactional(readOnly = true)`)
  where transactions are used.
- Tests for read endpoints MUST verify that persisted state is unchanged after the call.

Rationale: safe, idempotent reads are a core HTTP guarantee clients and caches rely on.

### VI. Minimal Scope

- Work MUST be limited to what the current feature spec requires; anything outside the spec
  MUST be rejected or recorded as a separate future spec.
- Each feature is time-boxed to 2 hours of implementation effort; if the time box is at risk,
  scope MUST be cut rather than the time box extended.
- Speculative abstractions, unused configuration, and "nice to have" endpoints are prohibited.

Rationale: a tight scope and time box keep delivery predictable and prevent gold-plating.

## Technical Constraints

- Monetary values (e.g., salary) MUST use `java.math.BigDecimal`; `float` and `double` are
  prohibited for money in code, DTOs, and entities.
- Monetary comparisons MUST use `compareTo`, not `equals`, and scale/rounding MUST be explicit
  wherever arithmetic is performed.
- The service is built with Maven; `mvn verify` is the canonical build and test command.
- Persistence uses JPA; entities are internal to the service and governed by Principle IV.

## Quality Gates & Development Workflow

- Every change MUST keep `mvn verify` green in CI; a red build blocks merge.
- Tests MUST NOT be disabled, skipped, or deleted to make the build pass.
- Each pull request MUST show: tests added before implementation (Principle I), the
  acceptance-criterion-to-test mapping, and no out-of-spec additions (Principle VI).
- Feature work follows the Spec Kit flow: specify → plan → tasks → implement; plans MUST include
  a Constitution Check against these principles.

## Governance

- This constitution supersedes all other project practices and conventions. Where guidance
  conflicts, this document wins.
- Amendments MUST be made via a pull request that updates this file, states the rationale, and
  bumps the version.
- Versioning follows semantic versioning:
  - MAJOR: a principle is removed or redefined in a backward-incompatible way.
  - MINOR: a principle or section is added or materially expanded.
  - PATCH: clarifications, wording, or typo fixes with no semantic change.
- Every pull request review MUST verify compliance with the Core Principles and Quality Gates;
  any deviation MUST be justified in writing in the PR and approved by a reviewer.

**Version**: 1.0.0 | **Ratified**: 2026-11-22 | **Last Amended**: 2026-11-22
