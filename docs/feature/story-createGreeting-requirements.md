# Feature Requirements: Create Greeting

| Field              | Value                                                      |
| ------------------ | ---------------------------------------------------------- |
| Feature Name       | Create Greeting                                            |
| Design Section     | §3.1 Resource: Greeting (§3.1.1–§3.1.3)                   |
| Related PRD        | FR-01, FR-02, FR-03, FR-07, FR-08, FR-09, BR-01, BR-02, GO-01 |
| Components/Classes | GreetingEntity, GreetingRepository, GreetingService, GreetingController, CreateGreetingRequest, CreateGreetingResponse |
| APIs               | POST /greetings                                            |
| Kafka Events       | —                                                          |
| Jobs               | —                                                          |
| Dependencies       | story-scaffolding (UUIDEntityBase, ExceptionBase, GlobalExceptionHandler, Flyway migration, application config) |

---

## 1. Summary

**Purpose and Scope:**
Implement the POST /greetings endpoint that accepts a JSON body with a `text` field, validates it, persists a new greeting entity with a service-generated UUID, and returns the UUID in the response. This is the creation half of the Greeting resource.

**Actors/Triggers:**
- Anonymous caller sends an HTTP POST request to `/greetings`.

**Preconditions:**
- The application is running and connected to MySQL.
- The `greeting` table exists (created by Flyway migration from the scaffolding story).
- `UUIDEntityBase`, `ExceptionBase`, and `GlobalExceptionHandler` are available.

**Processing Flow:**
1. Caller sends `POST /greetings` with JSON body `{"text": "<greeting text>"}`.
2. Spring deserializes the body into `CreateGreetingRequest`.
3. Bean Validation validates `text` (`@NotBlank`, `@Size(min=1, max=40)`).
4. If validation fails, `MethodArgumentNotValidException` is thrown → `GlobalExceptionHandler` returns HTTP 400 with `{"error": "<description>"}`.
5. `GreetingController.createGreeting()` delegates to `GreetingService`.
6. `GreetingService` creates a new `GreetingEntity`, generates a UUID, sets the text, and saves via `GreetingRepository`.
7. `GreetingService` returns the generated UUID.
8. `GreetingController` wraps the UUID in `CreateGreetingResponse` and returns HTTP 201.

**Success Behavior:**
- HTTP 201 Created with body `{"id": "<uuid>"}`.

**Error Behavior:**
- HTTP 400 Bad Request with body `{"error": "<validation failure description>"}` when `text` is missing, blank, empty, or exceeds 40 characters.

**Dependencies:**
- Scaffolding story (UUIDEntityBase, GlobalExceptionHandler, Flyway migration).

---

## 2. DTO Definitions

### CreateGreetingRequest

| Aspect      | Detail                                                        |
| ----------- | ------------------------------------------------------------- |
| Class Name  | `CreateGreetingRequest`                                       |
| Purpose     | Request body DTO for POST /greetings.                         |
| Location    | `feature/greeting/dto/CreateGreetingRequest.java`             |

| Field | Type   | Required | Validation                          | Description                          |
| ----- | ------ | -------- | ----------------------------------- | ------------------------------------ |
| text  | String | Yes      | `@NotBlank`, `@Size(min = 1, max = 40)` | The greeting message text. Must be between 1 and 40 characters inclusive. |

**Traces:** FR-07, FR-08, BR-01

### CreateGreetingResponse

| Aspect      | Detail                                                        |
| ----------- | ------------------------------------------------------------- |
| Class Name  | `CreateGreetingResponse`                                      |
| Purpose     | Response body DTO for POST /greetings (HTTP 201).             |
| Location    | `feature/greeting/dto/CreateGreetingResponse.java`            |

| Field | Type | Required | Validation | Description                                      |
| ----- | ---- | -------- | ---------- | ------------------------------------------------ |
| id    | UUID | Yes      | —          | Service-generated unique identifier for the created greeting. |

**Traces:** FR-02, BR-02

### GreetingEntity

| Aspect      | Detail                                                        |
| ----------- | ------------------------------------------------------------- |
| Class Name  | `GreetingEntity`                                              |
| Purpose     | JPA entity representing a persisted greeting.                 |
| Location    | `feature/greeting/entity/GreetingEntity.java`                 |

| Field | Type   | Required | Validation                        | Description                          |
| ----- | ------ | -------- | --------------------------------- | ------------------------------------ |
| id    | UUID   | Yes      | PK, service-generated, VARCHAR(36) | Inherited from `UUIDEntityBase`.     |
| text  | String | Yes      | `@Column(nullable = false, length = 40)` | The greeting message. NOT NULL, max 40 chars. |

**Traces:** FR-02, BR-01, BR-02

---

## 3. Public Function Signatures

### GreetingEntity

```java
@Entity
@Table(name = "greeting")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GreetingEntity extends UUIDEntityBase {

    @Column(name = "text", nullable = false, length = 40)
    private String text;
}
```

### GreetingRepository

```java
public interface GreetingRepository extends JpaRepository<GreetingEntity, UUID> {
}
```

| Aspect       | Detail                                                  |
| ------------ | ------------------------------------------------------- |
| Class        | `GreetingRepository`                                    |
| Location     | `feature/greeting/repository/GreetingRepository.java`   |
| Purpose      | Spring Data JPA repository for `GreetingEntity`.        |
| Inherited    | `save()`, `findById()` from `JpaRepository`.            |

### GreetingService

```java
@Service
public class GreetingService {

    /**
     * Create and persist a new greeting.
     *
     * @param request the validated creation request
     * @return the UUID of the newly created greeting
     */
    public UUID createGreeting(CreateGreetingRequest request);
}
```

| Aspect       | Detail                                                  |
| ------------ | ------------------------------------------------------- |
| Class        | `GreetingService`                                       |
| Location     | `feature/greeting/GreetingService.java`                 |
| Method       | `createGreeting(CreateGreetingRequest request)`         |
| Input        | `CreateGreetingRequest request` — validated request DTO |
| Return       | `UUID` — the generated identifier of the new greeting   |
| Purpose      | Generates a UUID, creates a `GreetingEntity`, persists it, and returns the UUID. |
| Error Conds  | None expected at this layer (validation handled by controller). |

### GreetingController

```java
@RestController
@RequestMapping("/greetings")
public class GreetingController {

    /**
     * Create a new greeting.
     *
     * @param request the greeting text payload
     * @return the created greeting's id
     *
     * Traces: FR-01, FR-02, FR-03, FR-07, FR-08, FR-09
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateGreetingResponse createGreeting(
            @Valid @RequestBody CreateGreetingRequest request);
}
```

| Aspect       | Detail                                                  |
| ------------ | ------------------------------------------------------- |
| Class        | `GreetingController`                                    |
| Location     | `feature/greeting/GreetingController.java`              |
| Method       | `createGreeting(@Valid @RequestBody CreateGreetingRequest request)` |
| Input        | `CreateGreetingRequest request` — JSON request body     |
| Return       | `CreateGreetingResponse` — contains the generated UUID  |
| HTTP Method  | POST                                                    |
| Path         | `/greetings`                                            |
| Status       | 201 Created                                             |
| Purpose      | Accepts greeting creation request, delegates to service, returns response. |
| Error Conds  | Validation failure → 400 (handled by GlobalExceptionHandler). |

---

## 4. Implementation Requirements

### Controller Behavior

- `GreetingController` is annotated `@RestController` and `@RequestMapping("/greetings")`.
- `createGreeting()` is annotated `@PostMapping` and `@ResponseStatus(HttpStatus.CREATED)`.
- The `request` parameter is annotated `@Valid @RequestBody` to trigger Bean Validation.
- Delegates to `GreetingService.createGreeting()`.
- Wraps the returned UUID in a `CreateGreetingResponse` and returns it.

### Service Behavior

- `GreetingService` is annotated `@Service`.
- `createGreeting()`:
  1. Generates a new UUID via `UUID.randomUUID()`.
  2. Creates a `GreetingEntity` with the generated UUID and the text from the request.
  3. Saves the entity via `GreetingRepository.save()`.
  4. Returns the generated UUID.

### Repository Behavior

- `GreetingRepository` extends `JpaRepository<GreetingEntity, UUID>`.
- Uses the inherited `save()` method. No custom queries needed.

### Entity/State Changes

- A new row is inserted into the `greeting` table with the generated UUID as `id` and the provided `text`.

### Transactions

- `GreetingService.createGreeting()` runs within Spring's default transactional context. An explicit `@Transactional` annotation is recommended.

### Validation

- `@NotBlank` on `CreateGreetingRequest.text`: rejects null, empty string, and whitespace-only strings.
- `@Size(min = 1, max = 40)` on `CreateGreetingRequest.text`: enforces character length between 1 and 40 inclusive.
- Validation failures produce `MethodArgumentNotValidException`, handled by `GlobalExceptionHandler` → HTTP 400 with `{"error": "<description>"}`.

### Error Handling

- Validation errors: HTTP 400 via `GlobalExceptionHandler.handleValidationException()`.
- No other application-level errors expected for this endpoint.

### Idempotency

- Not required. Each POST creates a new greeting with a new UUID.

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
| AC-1 | A POST to `/greetings` with `{"text": "Hello"}` returns HTTP 201 and a JSON body containing an `id` field with a valid UUID string. |
| AC-2 | After a successful POST, the greeting is persisted in the `greeting` table with the returned `id` and the submitted `text`. |
| AC-3 | The `id` is generated by the service; the caller cannot specify it.                                               |
| AC-4 | A POST to `/greetings` with `{"text": ""}` returns HTTP 400 with `{"error": "<description>"}`.                   |
| AC-5 | A POST to `/greetings` with no `text` field (e.g., `{}`) returns HTTP 400 with `{"error": "<description>"}`.     |
| AC-6 | A POST to `/greetings` with a `text` value of 41 characters returns HTTP 400 with `{"error": "<description>"}`.  |
| AC-7 | A POST to `/greetings` with a `text` value of exactly 1 character returns HTTP 201.                               |
| AC-8 | A POST to `/greetings` with a `text` value of exactly 40 characters returns HTTP 201.                             |
| AC-9 | A POST to `/greetings` with a whitespace-only `text` (e.g., `"   "`) returns HTTP 400.                           |
| AC-10| The response `Content-Type` is `application/json`.                                                                |
