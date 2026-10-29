# Feature Requirements: Error Handling

| Field              | Value                                                      |
| ------------------ | ---------------------------------------------------------- |
| Feature Name       | Error Handling                                             |
| Design Section     | §3.1.4 Exception                                           |
| Related PRD        | FR-06, FR-09                                               |
| Components/Classes | ExceptionBase, GreetingNotFoundException, GlobalExceptionHandler |
| APIs               | Cross-cutting (applies to all endpoints)                   |
| Kafka Events       | —                                                          |
| Jobs               | —                                                          |
| Dependencies       | story-scaffolding (ExceptionBase, GlobalExceptionHandler are defined there) |

---

## 1. Summary

**Purpose and Scope:**
Define the unified error handling strategy for the Greeting Service. This feature covers the `ExceptionBase` abstract exception, the `GreetingNotFoundException` concrete exception, and the `GlobalExceptionHandler` controller advice that translates exceptions into consistent JSON error responses. This story documents the error handling contract and behavior that spans all endpoints.

**Actors/Triggers:**
- Any request that results in a validation failure, a not-found condition, or an application error.

**Preconditions:**
- Spring Boot application is running with `spring-boot-starter-web` and `spring-boot-starter-validation`.

**Processing Flow:**

*Validation Error (HTTP 400):*
1. A request with invalid input triggers Bean Validation.
2. Spring throws `MethodArgumentNotValidException`.
3. `GlobalExceptionHandler.handleValidationException()` catches it.
4. Extracts the first field error's default message.
5. Returns HTTP 400 with `{"error": "<description>"}`.

*Not Found Error (HTTP 404):*
1. A service method determines the requested resource does not exist.
2. Throws `GreetingNotFoundException("Greeting not found")`.
3. `GlobalExceptionHandler.handleExceptionBase()` catches it (as `ExceptionBase`).
4. Extracts `status` (404) and `message` from the exception.
5. Returns HTTP 404 with `{"error": "Greeting not found"}`.

**Success Behavior:**
- All error responses follow the uniform format: `{"error": "<description>"}`.
- HTTP status codes match the error type (400 for validation, 404 for not found).

**Error Behavior:**
- N/A — this feature *is* the error handling mechanism.

**Dependencies:**
- Scaffolding story provides the base classes. This story documents the complete error handling contract and the `GreetingNotFoundException` integration.

---

## 2. DTO Definitions

### Error Response Body

| Aspect      | Detail                                                        |
| ----------- | ------------------------------------------------------------- |
| Class Name  | (Inline `Map<String, String>` — no dedicated DTO class)      |
| Purpose     | Uniform JSON error response for all error conditions.         |

| Field | Type   | Required | Validation | Description                                      |
| ----- | ------ | -------- | ---------- | ------------------------------------------------ |
| error | String | Yes      | —          | Human-readable description of the error.         |

**Traces:** FR-06, FR-09

### ExceptionBase

*Defined in scaffolding story — referenced here, not duplicated.*

| Aspect      | Detail                                                        |
| ----------- | ------------------------------------------------------------- |
| Class Name  | `ExceptionBase`                                               |
| Purpose     | Abstract base exception with HTTP status and message.         |
| Location    | `common/exception/ExceptionBase.java`                         |

| Field   | Type       | Required | Description                          |
| ------- | ---------- | -------- | ------------------------------------ |
| status  | HttpStatus | Yes      | HTTP status code for the error.      |
| message | String     | Yes      | Error description.                   |

### GreetingNotFoundException

| Aspect      | Detail                                                        |
| ----------- | ------------------------------------------------------------- |
| Class Name  | `GreetingNotFoundException`                                   |
| Purpose     | Thrown when a greeting is not found by ID.                     |
| Location    | `exception/GreetingNotFoundException.java`                    |

| Field   | Type       | Required | Description                          |
| ------- | ---------- | -------- | ------------------------------------ |
| status  | HttpStatus | Yes      | Always `HttpStatus.NOT_FOUND` (404). |
| message | String     | Yes      | Error message (e.g., `"Greeting not found"`). |

**Traces:** FR-06

---

## 3. Public Function Signatures

### ExceptionBase

```java
public abstract class ExceptionBase extends RuntimeException {

    public ExceptionBase(HttpStatus status, String message) {
        super(message);
    }

    public HttpStatus getStatus();

    @Override
    public String getMessage();
}
```

| Aspect       | Detail                                                  |
| ------------ | ------------------------------------------------------- |
| Class        | `ExceptionBase`                                         |
| Location     | `common/exception/ExceptionBase.java`                   |
| Constructor  | `ExceptionBase(HttpStatus status, String message)`      |
| Purpose      | Base class for all application-specific exceptions.     |

### GreetingNotFoundException

```java
public class GreetingNotFoundException extends ExceptionBase {

    public GreetingNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
```

| Aspect       | Detail                                                  |
| ------------ | ------------------------------------------------------- |
| Class        | `GreetingNotFoundException`                              |
| Location     | `exception/GreetingNotFoundException.java`               |
| Constructor  | `GreetingNotFoundException(String message)`              |
| Input        | `String message` — error description                    |
| Purpose      | Signals that a greeting was not found.                  |

### GlobalExceptionHandler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle all ExceptionBase subclasses.
     *
     * @param ex the application exception
     * @return JSON error response with appropriate HTTP status
     */
    @ExceptionHandler(ExceptionBase.class)
    public ResponseEntity<Map<String, String>> handleExceptionBase(ExceptionBase ex);

    /**
     * Handle Bean Validation failures.
     *
     * @param ex the validation exception
     * @return JSON error response with HTTP 400
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(
            MethodArgumentNotValidException ex);
}
```

| Method                    | Input                              | Return                                | HTTP Status       | Purpose                                      |
| ------------------------- | ---------------------------------- | ------------------------------------- | ----------------- | --------------------------------------------- |
| handleExceptionBase       | `ExceptionBase ex`                 | `ResponseEntity<Map<String, String>>` | From `ex.getStatus()` | Translates application exceptions to `{"error": "<message>"}`. |
| handleValidationException | `MethodArgumentNotValidException`  | `ResponseEntity<Map<String, String>>` | 400               | Translates validation failures to `{"error": "<description>"}`. |

---

## 4. Implementation Requirements

### GlobalExceptionHandler Behavior

- Annotated `@RestControllerAdvice` to apply globally to all controllers.

#### handleExceptionBase(ExceptionBase ex)
- Annotated `@ExceptionHandler(ExceptionBase.class)`.
- Extracts `ex.getStatus()` for the HTTP status code.
- Extracts `ex.getMessage()` for the error description.
- Returns `ResponseEntity` with body `{"error": "<message>"}` and the extracted status.
- This handles `GreetingNotFoundException` and any future `ExceptionBase` subclasses.

#### handleValidationException(MethodArgumentNotValidException ex)
- Annotated `@ExceptionHandler(MethodArgumentNotValidException.class)`.
- Extracts field error messages from `ex.getBindingResult().getFieldErrors()`.
- Uses the first field error's `getDefaultMessage()` as the error description.
- Returns HTTP 400 with body `{"error": "<description>"}`.

### ExceptionBase Behavior

- Extends `RuntimeException` (unchecked).
- Stores `HttpStatus status` and passes `message` to `RuntimeException` superclass.
- Provides `getStatus()` accessor.
- Annotated with `@Getter` (Lombok) or explicit getter methods.

### GreetingNotFoundException Behavior

- Extends `ExceptionBase`.
- Constructor calls `super(HttpStatus.NOT_FOUND, message)`.
- Typical usage: `throw new GreetingNotFoundException("Greeting not found")`.

### Error Response Format

All error responses follow this uniform JSON structure:

```json
{
  "error": "<human-readable error description>"
}
```

| Error Condition                     | HTTP Status | Error Message                          | Traces |
| ----------------------------------- | ----------- | -------------------------------------- | ------ |
| `text` missing, empty, or > 40     | 400         | Validation failure description         | FR-09  |
| `text` is whitespace-only           | 400         | Validation failure description         | FR-09  |
| Greeting `id` not found             | 404         | `"Greeting not found"`                 | FR-06  |

### Transactions

- Not applicable to error handling.

### Security/Audit

- Error responses do not leak internal details (stack traces, class names).
- No authentication required (BR-03).

### External Integrations

- None.

### Kafka/Job Processing

- None.

---

## 5. Acceptance Criteria

| ID   | Criterion                                                                                                         |
| ---- | ----------------------------------------------------------------------------------------------------------------- |
| AC-1 | When `GreetingNotFoundException("Greeting not found")` is thrown, the response is HTTP 404 with body `{"error": "Greeting not found"}`. |
| AC-2 | When a POST to `/greetings` has an empty `text` field, the response is HTTP 400 with body `{"error": "<description>"}` where `<description>` is a meaningful validation message. |
| AC-3 | When a POST to `/greetings` has no `text` field, the response is HTTP 400 with body `{"error": "<description>"}`. |
| AC-4 | When a POST to `/greetings` has `text` exceeding 40 characters, the response is HTTP 400 with body `{"error": "<description>"}`. |
| AC-5 | All error responses have `Content-Type: application/json`.                                                        |
| AC-6 | Error responses contain only the `error` field — no stack traces, no internal class names, no additional fields.  |
| AC-7 | Any future `ExceptionBase` subclass with a different `HttpStatus` is automatically handled by `GlobalExceptionHandler` with the correct status code and `{"error": "<message>"}` format. |
| AC-8 | The `GlobalExceptionHandler` handles `MethodArgumentNotValidException` specifically (not just generic `Exception`), ensuring validation errors always return HTTP 400. |
