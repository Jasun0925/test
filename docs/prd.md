# Product Requirements Document: Greeting Service

| Field          | Value                  |
| -------------- | ---------------------- |
| Source         | BRD: Greeting          |
| Version        | 1.0                    |
| Status         | Draft                  |

---

## 1. Product Overview

The Greeting Service is a minimal HTTP API that allows a single anonymous caller to store a short greeting text and retrieve it later by identifier. The service exposes two endpoints — one to create a greeting and one to read it back — with no authentication, no bulk operations, and no integration with external systems.

---

## 2. Business Problem

There is a need for a lightweight service that can accept a greeting message, persist it, and return it on demand. The service must be simple, self-contained, and require no user accounts or login.

---

## 3. Goals and Objectives

| ID    | Goal                                                                 | Traces To |
| ----- | -------------------------------------------------------------------- | --------- |
| GO-01 | Provide an endpoint to store a greeting text and return an identifier | BRD-Goal  |
| GO-02 | Provide an endpoint to retrieve a stored greeting by its identifier   | BRD-Goal  |
| GO-03 | Operate without authentication or user accounts                      | BRD-User  |

---

## 4. Scope

### In Scope

- Creating a greeting via HTTP POST.
- Retrieving a greeting via HTTP GET.
- Input validation on the greeting text.
- Returning appropriate error responses for unknown identifiers and invalid input.

### Out of Scope

| Item                  | Traces To       |
| --------------------- | --------------- |
| User accounts / login | BRD-OutOfScope  |
| Listing greetings     | BRD-OutOfScope  |
| Updating greetings    | BRD-OutOfScope  |
| Deleting greetings    | BRD-OutOfScope  |
| Search                | BRD-OutOfScope  |
| File handling         | BRD-OutOfScope  |
| Message queues        | BRD-OutOfScope  |
| External system calls | BRD-OutOfScope  |

---

## 5. Users and Actors

There is a single actor: an anonymous caller that interacts with the service over HTTP. No login or identity verification is required. The caller may create greetings and retrieve them by identifier.

---

## 6. High-Level Business Process

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

**Flow:**

1. The caller sends a POST request with a greeting text.
2. The service validates the text, stores it, and returns a unique identifier.
3. The caller sends a GET request with the identifier.
4. The service looks up the identifier and returns the stored text, or responds with 404 if the identifier is unknown.

---

## 7. Functional Requirements

| ID    | Requirement                                                                                                  | Traces To      |
| ----- | ------------------------------------------------------------------------------------------------------------ | -------------- |
| FR-01 | The service SHALL accept a POST request to `/greetings` with a JSON body containing a `text` field.          | BRD-Behavior-1 |
| FR-02 | On successful creation, the service SHALL persist the greeting text and return a response containing the `id`.| BRD-Behavior-1 |
| FR-03 | The service SHALL respond to POST `/greetings` with HTTP status 201 on successful creation.                  | BRD-Behavior-1 |
| FR-04 | The service SHALL accept a GET request to `/greetings/{id}` and return the stored greeting text.             | BRD-Behavior-2 |
| FR-05 | The service SHALL respond to GET `/greetings/{id}` with HTTP status 200 and a JSON body containing `id` and `text`. | BRD-Behavior-2 |
| FR-06 | The service SHALL respond with HTTP status 404 when a GET request references an unknown `id`.                | BRD-Behavior-3 |
| FR-07 | The `text` field SHALL be required in the POST request body.                                                 | BRD-Behavior-4 |
| FR-08 | The `text` field SHALL be between 1 and 40 characters inclusive.                                             | BRD-Behavior-4 |
| FR-09 | The service SHALL respond with HTTP status 400 when the `text` field is missing, empty, or exceeds 40 characters. | BRD-Behavior-4 |

---

## 8. Business Rules

| ID    | Rule                                                                                          | Traces To      |
| ----- | --------------------------------------------------------------------------------------------- | -------------- |
| BR-01 | Every greeting must have a non-empty text value between 1 and 40 characters.                  | BRD-Behavior-4 |
| BR-02 | Each stored greeting is assigned a unique identifier by the service; callers do not choose it. | BRD-Behavior-1 |
| BR-03 | No authentication or authorization is required for any operation.                             | BRD-User       |

---

## 9. Business Data Requirements

### Greeting Entity

| Field | Type   | Constraints                          | Description                          |
| ----- | ------ | ------------------------------------ | ------------------------------------ |
| id    | string | Unique, assigned by the service      | Identifier returned upon creation    |
| text  | string | Required, 1 to 40 characters         | The greeting message                 |

### API Contracts

#### POST /greetings

**Request:**

```
Content-Type: application/json

{
  "text": "<string, 1-40 chars>"
}
```

**Success Response — 201 Created:**

```
Content-Type: application/json

{
  "id": "<string>"
}
```

**Validation Error Response — 400 Bad Request:**

```
Content-Type: application/json

{
  "error": "<description of the validation failure>"
}
```

#### GET /greetings/{id}

**Success Response — 200 OK:**

```
Content-Type: application/json

{
  "id": "<string>",
  "text": "<string>"
}
```

**Not Found Response — 404 Not Found:**

```
Content-Type: application/json

{
  "error": "Greeting not found"
}
```

---

## 10. Non-Functional Requirements

| ID     | Requirement                                                                                         | Traces To |
| ------ | --------------------------------------------------------------------------------------------------- | --------- |
| NFR-01 | The service SHALL return responses within a reasonable time for single-record operations.            | Implied   |
| NFR-02 | The service SHALL use JSON as the request and response content type for all endpoints.               | Implied   |
| NFR-03 | The service SHALL be self-contained with no dependencies on external systems, message queues, or file storage. | BRD-OutOfScope |

---

## 11. Acceptance Criteria

| ID    | Criterion                                                                                                          | Traces To |
| ----- | ------------------------------------------------------------------------------------------------------------------ | --------- |
| AC-01 | A POST to `/greetings` with `{"text": "Hello"}` returns HTTP 201 and a JSON body containing an `id` field.        | FR-01, FR-02, FR-03 |
| AC-02 | A GET to `/greetings/{id}` using the `id` from AC-01 returns HTTP 200 and `{"id": "<id>", "text": "Hello"}`.      | FR-04, FR-05 |
| AC-03 | A GET to `/greetings/nonexistent` returns HTTP 404.                                                                | FR-06 |
| AC-04 | A POST to `/greetings` with `{"text": ""}` returns HTTP 400.                                                      | FR-07, FR-08, FR-09 |
| AC-05 | A POST to `/greetings` with no `text` field returns HTTP 400.                                                      | FR-07, FR-09 |
| AC-06 | A POST to `/greetings` with a `text` value of 41 characters returns HTTP 400.                                      | FR-08, FR-09 |
| AC-07 | A POST to `/greetings` with a `text` value of exactly 1 character returns HTTP 201.                                | FR-08 |
| AC-08 | A POST to `/greetings` with a `text` value of exactly 40 characters returns HTTP 201.                              | FR-08 |

---

## 12. Dependencies and Assumptions

### Dependencies

There are no external dependencies. The service is self-contained.

### Assumptions

| ID    | Assumption                                                                                                   |
| ----- | ------------------------------------------------------------------------------------------------------------ |
| AS-01 | The `id` is a server-generated opaque string (e.g., UUID). The BRD does not prescribe a format.              |
| AS-02 | Greetings are persisted for the lifetime of the service process. Durability across restarts is not specified. |
| AS-03 | Character length for the `text` field refers to Unicode characters, not bytes.                                |
| AS-04 | The service handles one request at a time or a small number of concurrent requests; high-throughput scaling is not required. |

---

## 13. Open Questions

| ID    | Question                                                                                              |
| ----- | ----------------------------------------------------------------------------------------------------- |
| OQ-01 | Should the `id` follow a specific format (e.g., UUID, integer, short code)?                           |
| OQ-02 | Is persistence across service restarts required, or is in-memory storage acceptable?                  |
| OQ-03 | Should the 400 error response include structured field-level validation details?                      |

---

## Traceability Matrix

| BRD Reference     | PRD Requirement(s)                  |
| ------------------ | ----------------------------------- |
| BRD-Goal           | GO-01, GO-02                        |
| BRD-User           | GO-03, BR-03                        |
| BRD-Behavior-1     | FR-01, FR-02, FR-03, BR-02          |
| BRD-Behavior-2     | FR-04, FR-05                        |
| BRD-Behavior-3     | FR-06                               |
| BRD-Behavior-4     | FR-07, FR-08, FR-09, BR-01          |
| BRD-OutOfScope     | Scope — Out of Scope table, NFR-03  |
