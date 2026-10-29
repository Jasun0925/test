# System Design Document: Greeting Service

| Field          | Value                          |
| -------------- | ------------------------------ |
| Source PRD     | docs/prd.md                    |
| Version        | 1.0                            |
| Status         | Draft                          |

---

## 1. Background and Introduction

The Greeting Service is a minimal HTTP API that allows a single anonymous caller to store a short greeting text and retrieve it later by identifier. The service exposes two endpoints — one to create a greeting and one to read it back — with no authentication, no bulk operations, and no integration with external systems.

There is a need for a lightweight service that can accept a greeting message, persist it, and return it on demand. The service must be simple, self-contained, and require no user accounts or login.

```
+--------+         POST /greetings         +-----------+
|        | ------- {"text": "Hello"} ----> |           |
| Caller |                                 |  Service  |
|        | <------ {"id": "<id>"}  ------- |           |
+--------+                                 +-----------+
    |                                           |
    |          GET /greetings/{id}              |
    | ----------------------------------------> |
    |                                           |
    | <--- {"id": "<id>", "text": "Hello"} --- |
    |                                           |
```

---

## 2. System Architecture Diagram

Architecture diagram exported to: `docs/tmp_dsh_w9-architecture.png`

```
+------------------+                +--------------------------------------------------+
|                  |  REST / HTTPS  |          Spring Boot Application                 |
|   Anonymous      | -------------> |                                                  |
|   Caller         |                |  +--------------------------------------------+  |
|                  | <------------- |  |  REST API (GreetingController)              |  |
+------------------+                |  +--------------------------------------------+  |
                                    |                    |                              |
                                    |                    v                              |
                                    |  +--------------------------------------------+  |
                                    |  |  Application Services (GreetingService)     |  |
                                    |  +--------------------------------------------+  |
                                    |                    |                              |
                                    |                    v                              |
                                    |  +--------------------------------------------+  |
                                    |  |  Persistence (GreetingRepository)           |  |
                                    |  +--------------------------------------------+  |
                                    |                    |                              |
                                    +--------------------------------------------------+
                                                         |
                                                         | JDBC
                                                         v
                                                  +--------------+
                                                  |    MySQL     |
                                                  |   Database   |
                                                  +--------------+
```

---

## 3. Feature Design

### 3.1 Resource: Greeting

The Greeting resource represents a short text message stored by the service. It is the sole REST resource in the system.

**Feature directory:** `feature/greeting/`

---

#### 3.1.1 Entity Model

The `GreetingEntity` extends `UUIDEntityBase` since it is exposed through REST APIs. The UUID serves as the public identifier.

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

**Location:** `feature/greeting/entity/GreetingEntity.java`

| Field | Type   | DB Column | Constraints                        | Source        |
| ----- | ------ | --------- | ---------------------------------- | ------------- |
| id    | UUID   | id        | PK, service-generated UUID, VARCHAR | UUIDEntityBase |
| text  | String | text      | NOT NULL, VARCHAR(40)              | PRD §9        |

**Database Table: `greeting`**

| Column | Type         | Constraints       |
| ------ | ------------ | ----------------- |
| id     | VARCHAR(36)  | PRIMARY KEY       |
| text   | VARCHAR(40)  | NOT NULL          |

**Flyway Migration:** `V1__create_greeting_table.sql`

```sql
CREATE TABLE greeting (
    id   VARCHAR(36)  NOT NULL,
    text VARCHAR(40)  NOT NULL,
    PRIMARY KEY (id)
);
```

---

#### 3.1.2 DTOs

**Location:** `feature/greeting/dto/`

##### CreateGreetingRequest

Used for `POST /greetings` request body.

```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateGreetingRequest {

    @NotBlank
    @Size(min = 1, max = 40)
    private String text;
}
```

| Field | Type   | Validation                          | Traces To          |
| ----- | ------ | ----------------------------------- | ------------------ |
| text  | String | @NotBlank, @Size(min = 1, max = 40) | FR-07, FR-08, BR-01 |

##### CreateGreetingResponse

Used for `POST /greetings` success response (HTTP 201).

```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateGreetingResponse {

    private UUID id;
}
```

| Field | Type | Description                     | Traces To     |
| ----- | ---- | ------------------------------- | ------------- |
| id    | UUID | Service-generated identifier    | FR-02, BR-02  |

##### GreetingResponse

Used for `GET /greetings/{id}` success response (HTTP 200).

```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GreetingResponse {

    private UUID id;
    private String text;
}
```

| Field | Type   | Description                  | Traces To |
| ----- | ------ | ---------------------------- | --------- |
| id    | UUID   | Greeting identifier          | FR-05     |
| text  | String | Stored greeting text         | FR-05     |

---

#### 3.1.3 REST Controller

**Location:** `feature/greeting/GreetingController.java`

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

| Method           | HTTP         | Path              | Request DTO              | Response DTO            | Status | Traces                              |
| ---------------- | ------------ | ----------------- | ------------------------ | ----------------------- | ------ | ----------------------------------- |
| createGreeting   | POST         | /greetings        | CreateGreetingRequest    | CreateGreetingResponse  | 201    | FR-01, FR-02, FR-03, FR-07–FR-09   |
| getGreeting      | GET          | /greetings/{id}   | —                        | GreetingResponse        | 200    | FR-04, FR-05, FR-06                |

**Error Responses:**

| Condition                          | HTTP Status | Response Body                                      | Traces    |
| ---------------------------------- | ----------- | -------------------------------------------------- | --------- |
| `text` missing, empty, or > 40    | 400         | `{"error": "<validation failure description>"}`    | FR-09     |
| `id` not found                     | 404         | `{"error": "Greeting not found"}`                  | FR-06     |

---

#### 3.1.4 Exception

**Location:** `exception/GreetingNotFoundException.java`

```java
public class GreetingNotFoundException extends ExceptionBase {

    public GreetingNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
```

The `GlobalExceptionHandler` (under `advice/`) translates `ExceptionBase` subclasses into the appropriate HTTP error response.

For validation errors (HTTP 400), Spring's built-in `MethodArgumentNotValidException` handling is used, mapped by the `GlobalExceptionHandler` to return `{"error": "<description>"}`.

---

#### 3.1.5 Requirement Traceability

| PRD Requirement | Design Element                                                                 |
| --------------- | ------------------------------------------------------------------------------ |
| FR-01           | `GreetingController.createGreeting()` — POST /greetings with JSON body         |
| FR-02           | `GreetingController.createGreeting()` returns `CreateGreetingResponse` with id  |
| FR-03           | `@ResponseStatus(HttpStatus.CREATED)` on createGreeting                        |
| FR-04           | `GreetingController.getGreeting()` — GET /greetings/{id}                       |
| FR-05           | `GreetingController.getGreeting()` returns `GreetingResponse` with id and text  |
| FR-06           | `GreetingNotFoundException` → 404 via GlobalExceptionHandler                   |
| FR-07           | `@NotBlank` on `CreateGreetingRequest.text`                                    |
| FR-08           | `@Size(min = 1, max = 40)` on `CreateGreetingRequest.text`                    |
| FR-09           | Validation failure → 400 via GlobalExceptionHandler                            |
| BR-01           | `@NotBlank` + `@Size(min = 1, max = 40)` on text; DB column VARCHAR(40) NOT NULL |
| BR-02           | UUID generated by service (UUIDEntityBase); not accepted from caller           |
| BR-03           | No authentication/authorization configured                                     |
| GO-01           | POST /greetings endpoint                                                       |
| GO-02           | GET /greetings/{id} endpoint                                                   |
| GO-03           | No security filters or login endpoints                                         |
| NFR-01          | Single-record DB operations; no complex queries                                |
| NFR-02          | JSON content type on all endpoints (Spring default)                            |
| NFR-03          | No external system dependencies in design                                      |

---

## 4. Open Questions

| ID    | Question / Assumption                                                                                          | Decision / Status                                                                                      |
| ----- | -------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------ |
| OQ-01 | Should the `id` follow a specific format? (PRD OQ-01)                                                          | **Decision:** UUID (v4) — aligns with `UUIDEntityBase` from coding guidelines for REST-exposed entities. |
| OQ-02 | Is persistence across service restarts required? (PRD OQ-02)                                                   | **Decision:** Yes — MySQL with Flyway migration provides durable persistence across restarts.           |
| OQ-03 | Should the 400 error response include structured field-level validation details? (PRD OQ-03)                   | **TBD** — Current design returns `{"error": "<description>"}` matching PRD §9 contract. Structured field-level details can be added later if needed. |
| AS-01 | PRD AS-01: The `id` is a server-generated opaque string.                                                       | Implemented as UUID stored as VARCHAR(36).                                                              |
| AS-02 | PRD AS-02: Greetings persisted for lifetime of service process.                                                | Exceeded — MySQL provides persistence beyond process lifetime.                                          |
| AS-03 | PRD AS-03: Character length refers to Unicode characters, not bytes.                                           | Handled by Java `@Size` annotation which counts characters; MySQL VARCHAR(40) with utf8mb4 supports this. |
| AS-04 | PRD AS-04: Service handles small number of concurrent requests.                                                | No special concurrency or scaling mechanisms designed; standard Spring Boot thread pool is sufficient.   |
