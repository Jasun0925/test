# Code Review Report: Greeting Service

| Field       | Value                  |
| ----------- | ---------------------- |
| Date        | 2026-10-29             |
| Result      | **PASS** (after fixes) |
| Tests       | 15/15 passing          |
| Compilation | ✅ Success             |

---

## 1. Review Scope

Reviewed all source code against:
- `docs/prd.md` (PRD v1.0)
- `docs/design.md` (System Design v1.0)
- `docs/feature/story-scaffolding-requirements.md`
- `docs/feature/story-createGreeting-requirements.md`
- `docs/feature/story-getGreeting-requirements.md`
- `docs/feature/story-errorHandling-requirements.md`
- `reference/codebase_instruction.md` (Coding Guidelines)

---

## 2. Issues Found and Fixed

### Issue 1: `MethodArgumentTypeMismatchException` not handled by `GlobalExceptionHandler`

| Aspect     | Detail |
| ---------- | ------ |
| Severity   | Medium |
| File       | `src/main/java/com/greeting/advice/GlobalExceptionHandler.java` |
| Requirement | Error Handling story §4: "All error responses follow the uniform format: `{"error": "<description>"}`" |
| Problem    | When an invalid UUID was passed to `GET /greetings/{id}` (e.g., `/greetings/abc`), Spring's `DefaultHandlerExceptionResolver` handled the `MethodArgumentTypeMismatchException` instead of the `GlobalExceptionHandler`, producing a non-uniform error response format. |
| Fix        | Added `handleTypeMismatchException(MethodArgumentTypeMismatchException)` handler returning HTTP 400 with `{"error": "Invalid parameter: <name>"}`. |

### Issue 2: `HttpMessageNotReadableException` not handled by `GlobalExceptionHandler`

| Aspect     | Detail |
| ---------- | ------ |
| Severity   | Medium |
| File       | `src/main/java/com/greeting/advice/GlobalExceptionHandler.java` |
| Requirement | Error Handling story §4: "All error responses follow the uniform format: `{"error": "<description>"}`" |
| Problem    | When a malformed/unparseable JSON body was sent to `POST /greetings`, Spring threw `HttpMessageNotReadableException` which was not caught by the `GlobalExceptionHandler`, producing a non-uniform error response. |
| Fix        | Added `handleMessageNotReadableException(HttpMessageNotReadableException)` handler returning HTTP 400 with `{"error": "Malformed request body"}`. |

### Test Updates

| Test File | Change |
| --------- | ------ |
| `GreetingControllerTest.java` | Renamed `getGreeting_invalidUuid_returns400` → `getGreeting_invalidUuid_returns400WithErrorBody` and added assertion on `$.error` field existence. |
| `GreetingControllerTest.java` | Added new test `createGreeting_malformedJson_returns400WithErrorBody` verifying malformed JSON returns `{"error": "Malformed request body"}`. |

---

## 3. Verification Checklist

### Functional Requirements (PRD)

| ID    | Requirement | Status |
| ----- | ----------- | ------ |
| FR-01 | POST /greetings accepts JSON body with `text` field | ✅ Pass |
| FR-02 | Returns response containing `id` on creation | ✅ Pass |
| FR-03 | POST returns HTTP 201 | ✅ Pass |
| FR-04 | GET /greetings/{id} returns stored greeting | ✅ Pass |
| FR-05 | GET returns HTTP 200 with `id` and `text` | ✅ Pass |
| FR-06 | Unknown `id` returns HTTP 404 | ✅ Pass |
| FR-07 | `text` field required | ✅ Pass |
| FR-08 | `text` between 1 and 40 characters | ✅ Pass |
| FR-09 | Invalid `text` returns HTTP 400 | ✅ Pass |

### Business Rules

| ID    | Rule | Status |
| ----- | ---- | ------ |
| BR-01 | Non-empty text, 1-40 chars | ✅ `@NotBlank` + `@Size(min=1, max=40)` |
| BR-02 | Service-generated UUID | ✅ `UUID.randomUUID()` in service |
| BR-03 | No authentication | ✅ No security filters configured |

### Architecture & Coding Guidelines

| Check | Status |
| ----- | ------ |
| Feature-oriented architecture (`feature/greeting/`) | ✅ |
| Controller directly under feature root | ✅ |
| Service under `service/` | ✅ |
| Repository under `repo/` | ✅ |
| Entity under `entity/` | ✅ |
| DTOs under `dto/` | ✅ |
| `UUIDEntityBase` for REST-exposed entity | ✅ |
| UUID stored as VARCHAR (`preferred_uuid_jdbc_type: VARCHAR`) | ✅ |
| `ExceptionBase` extends `RuntimeException` with `HttpStatus` | ✅ |
| `GreetingNotFoundException` extends `ExceptionBase` | ✅ |
| `GlobalExceptionHandler` with `@RestControllerAdvice` | ✅ |
| Lombok `@Builder` on entities and DTOs | ✅ |
| Flyway migration `V1__create_greeting_table.sql` | ✅ |
| `@Transactional` on service write methods | ✅ |
| `@Transactional(readOnly = true)` on service read methods | ✅ |
| `@Valid @RequestBody` on controller | ✅ |
| No direct repository access from controller | ✅ |
| `docker-compose.yml` with MySQL and Redis | ✅ |
| `application.yml` with JPA validate mode | ✅ |
| `application-test.yml` with H2 for tests | ✅ |
| No hardcoded secrets (credentials in config files only) | ✅ |
| No `System.out.println()` | ✅ |

### Error Response Format

| Condition | Expected | Status |
| --------- | -------- | ------ |
| Missing `text` | 400 `{"error": "<description>"}` | ✅ |
| Empty `text` | 400 `{"error": "<description>"}` | ✅ |
| `text` > 40 chars | 400 `{"error": "<description>"}` | ✅ |
| Whitespace-only `text` | 400 `{"error": "<description>"}` | ✅ |
| Unknown `id` | 404 `{"error": "Greeting not found"}` | ✅ |
| Invalid UUID format | 400 `{"error": "Invalid parameter: id"}` | ✅ (fixed) |
| Malformed JSON body | 400 `{"error": "Malformed request body"}` | ✅ (fixed) |

### Test Coverage

| Test Class | Tests | Status |
| ---------- | ----- | ------ |
| `ApplicationTest` | 1 (context loads) | ✅ |
| `GreetingControllerTest` | 11 tests | ✅ |
| `GreetingServiceTest` | 3 tests | ✅ |
| **Total** | **15** | **All passing** |

---

## 4. Files Modified

| File | Change |
| ---- | ------ |
| `src/main/java/com/greeting/advice/GlobalExceptionHandler.java` | Added handlers for `MethodArgumentTypeMismatchException` and `HttpMessageNotReadableException` |
| `src/test/java/com/greeting/feature/greeting/GreetingControllerTest.java` | Updated invalid UUID test with body assertion; added malformed JSON test |

---

## 5. Result

**PASS** — All requirements are met, all 15 tests pass, compilation succeeds, and the two identified issues have been fixed.
