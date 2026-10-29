# Feature Requirements: Get Greeting

| Field              | Value                                                      |
| ------------------ | ---------------------------------------------------------- |
| Feature Name       | Get Greeting                                               |
| Design Section     | §3.1 Resource: Greeting (§3.1.3, §3.1.4)                  |
| Related PRD        | FR-04, FR-05, FR-06, GO-02                                 |
| Components/Classes | GreetingService, GreetingController, GreetingResponse, GreetingNotFoundException |
| APIs               | GET /greetings/{id}                                        |
| Kafka Events       | —                                                          |
| Jobs               | —                                                          |
| Dependencies       | story-scaffolding (UUIDEntityBase, ExceptionBase, GlobalExceptionHandler), story-createGreeting (GreetingEntity, GreetingRepository) |

---

## 1. Summary

**Purpose and Scope:**
Implement the GET /greetings/{id} endpoint that retrieves a previously stored greeting by its UUID. Returns the greeting's `id` and `text` on success, or HTTP 404 if the greeting does not exist. This is the retrieval half of the Greeting resource.

**Actors/Triggers:**
- Anonymous caller sends an HTTP GET request to `/greetings/{id}`.

**Preconditions:**
- The application is running and connected to MySQL.
- The `greeting` table exists and may contain previously created greetings.
- `GreetingEntity`, `GreetingRepository`, `ExceptionBase`, and `GlobalExceptionHandler` are available.

**Processing Flow:**
1. Caller sends `GET /greetings/{id}` where `{id}` is a UUID string.
2. `GreetingController.getGreeting()` receives the `id` as a `UUID` path variable.
3. Controller delegates to `GreetingService.getGreeting(UUID id)`.
4. `GreetingService` queries `GreetingRepository.findById(id)`.
5. If found: maps the entity to `GreetingResponse` and returns it.
6. If not found: throws `GreetingNotFoundException` with message `"Greeting not found"`.
7. `GlobalExceptionHandler` catches `GreetingNotFoundException` (via `ExceptionBase`) and returns HTTP 404 with `{"error": "Greeting not found"}`.

**Success Behavior:**
- HTTP 200 OK with body `{"id": "<uuid>", "text": "<greeting text>"}`.

**Error Behavior:**
- HTTP 404 Not Found with body `{"error": "Greeting not found"}` when the `id` does not match any stored greeting.

**Dependencies:**
- Scaffolding story (ExceptionBase, GlobalExceptionHandler).
- Create Greeting story (GreetingEntity, GreetingRepository — shared, not duplicated).

---

## 2. DTO Definitions

### GreetingResponse

| Aspect      | Detail                                                        |
| ----------- | ------------------------------------------------------------- |
| Class Name  | `GreetingResponse`                                            |
| Purpose     | Response body DTO for GET /greetings/{id} (HTTP 200).         |
| Location    | `feature/greeting/dto/GreetingResponse.java`                  |

| Field | Type   | Required | Validation | Description                          |
| ----- | ------ | -------- | ---------- | ------------------------------------ |
| id    | UUID   | Yes      | —          | The greeting's unique identifier.    |
| text  | String | Yes      | —          | The stored greeting message text.    |

**Traces:** FR-05

### GreetingNotFoundException

| Aspect      | Detail                                                        |
| ----------- | ------------------------------------------------------------- |
| Class Name  | `GreetingNotFoundException`                                   |
| Purpose     | Exception thrown when a greeting lookup by ID finds no match. |
| Location    | `exception/GreetingNotFoundException.java`                    |

| Field   | Type       | Required | Description                          |
| ------- | ---------- | -------- | ------------------------------------ |
| status  | HttpStatus | Yes      | Always `HttpStatus.NOT_FOUND` (404). |
| message | String     | Yes      | `"Greeting not found"`               |

**Traces:** FR-06

**Note:** `GreetingNotFoundException` extends `ExceptionBase` (defined in the scaffolding story). It is not duplicated here; it is referenced.

---

## 3. Public Function Signatures

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
| Purpose      | Creates a NOT_FOUND exception for missing greetings.    |
| Error Conds  | N/A — this is the error itself.                         |

### GreetingService (getGreeting method)

```java
@Service
public class GreetingService {

    /**
     * Retrieve a greeting by its identifier.
     *
     * @param id the greeting UUID
     * @return the greeting response DTO
     * @throws GreetingNotFoundException if no greeting exists with the given id
     */
    public GreetingResponse getGreeting(UUID id);
}
```

| Aspect       | Detail                                                  |
| ------------ | ------------------------------------------------------- |
| Class        | `GreetingService`                                       |
| Location     | `feature/greeting/GreetingService.java`                 |
| Method       | `getGreeting(UUID id)`                                  |
| Input        | `UUID id` — the greeting identifier                     |
| Return       | `GreetingResponse` — DTO with `id` and `text`          |
| Purpose      | Looks up a greeting by UUID; returns DTO or throws 404. |
| Error Conds  | `GreetingNotFoundException` if no entity found for `id`.|

### GreetingController (getGreeting method)

```java
@RestController
@RequestMapping("/greetings")
public class GreetingController {

    /**
     * Retrieve a greeting by its identifier.
     *
     * @param id the greeting UUID
     * @return the greeting id and text
     *
     * Traces: FR-04, FR-05, FR-06
     */
    @GetMapping("/{id}")
    public GreetingResponse getGreeting(@PathVariable UUID id);
}
```

| Aspect       | Detail                                                  |
| ------------ | ------------------------------------------------------- |
| Class        | `GreetingController`                                    |
| Location     | `feature/greeting/GreetingController.java`              |
| Method       | `getGreeting(@PathVariable UUID id)`                    |
| Input        | `UUID id` — path variable                               |
| Return       | `GreetingResponse` — JSON body with `id` and `text`    |
| HTTP Method  | GET                                                     |
| Path         | `/greetings/{id}`                                       |
| Status       | 200 OK (default)                                        |
| Purpose      | Retrieves a greeting by UUID, delegates to service.     |
| Error Conds  | 404 if greeting not found (via GreetingNotFoundException). |

---

## 4. Implementation Requirements

### Controller Behavior

- `GreetingController.getGreeting()` is annotated `@GetMapping("/{id}")`.
- The `id` parameter is annotated `@PathVariable` and typed as `UUID`.
- Spring automatically parses the path variable string to `UUID`. If the format is invalid, Spring returns a 400 error.
- Delegates to `GreetingService.getGreeting(id)`.
- Returns the `GreetingResponse` directly; Spring serializes it to JSON with HTTP 200.

### Service Behavior

- `GreetingService.getGreeting(UUID id)`:
  1. Calls `GreetingRepository.findById(id)`.
  2. If the `Optional` is present: maps `GreetingEntity` to `GreetingResponse` (setting `id` and `text`).
  3. If the `Optional` is empty: throws `new GreetingNotFoundException("Greeting not found")`.

### Repository Behavior

- Uses `GreetingRepository.findById(UUID id)` — inherited from `JpaRepository`. No custom query needed.
- `GreetingRepository` is shared with the Create Greeting story; it is not duplicated.

### Entity/State Changes

- None. This is a read-only operation.

### Transactions

- Read-only operation. An explicit `@Transactional(readOnly = true)` annotation is recommended but not required.

### Validation

- No request body validation needed.
- Path variable `id` is typed as `UUID`; Spring handles format validation.

### Error Handling

- `GreetingNotFoundException` extends `ExceptionBase` with `HttpStatus.NOT_FOUND`.
- `GlobalExceptionHandler.handleExceptionBase()` catches it and returns HTTP 404 with `{"error": "Greeting not found"}`.

### Idempotency

- GET is inherently idempotent. Multiple identical requests return the same result.

### Security/Audit

- No authentication or authorization (BR-03).
- No audit logging required.

### External Integrations

- None (NFR-03).

### Kafka/Job Processing

- None.

---

## 5. Acceptance Criteria

| ID   | Criterion                                                                                                         |
| ---- | ----------------------------------------------------------------------------------------------------------------- |
| AC-1 | A GET to `/greetings/{id}` using a valid, existing greeting UUID returns HTTP 200 with `{"id": "<uuid>", "text": "<text>"}`. |
| AC-2 | The returned `id` matches the UUID used in the request path.                                                      |
| AC-3 | The returned `text` matches the text that was stored when the greeting was created.                               |
| AC-4 | A GET to `/greetings/{id}` with a UUID that does not exist in the database returns HTTP 404 with `{"error": "Greeting not found"}`. |
| AC-5 | A GET to `/greetings/{id}` with a non-UUID string (e.g., `"abc"`) returns HTTP 400.                              |
| AC-6 | The response `Content-Type` is `application/json`.                                                                |
| AC-7 | The endpoint is idempotent: repeated GET requests for the same `id` return the same result.                       |
