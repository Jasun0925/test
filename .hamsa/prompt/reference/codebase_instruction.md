# Coding Guidelines

This document defines the architectural, coding, dependency, database, security, testing, and project-organization rules for AI coding agents.

## 1. Technology

The project uses:

- Java 25
- Spring Boot
- Maven
- Spring Data JPA
- Flyway
- REST APIs
- Jakarta Persistence (`jakarta.persistence.*`)
- MySQL
- Kafka
- Lombok

Rules:

- Use Maven for dependencies and builds.
- Do not introduce Gradle or another build system.
- Do not introduce another database migration framework.
- Follow existing project dependencies and conventions.

---

# 2. Project Structure

The project uses feature-oriented architecture:

```text
<project-root>/
├── pom.xml
├── docs/
│   └── feature/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── <base-package>/
│   │   │       ├── exception/
│   │   │       ├── advice/
│   │   │       ├── feature/
|   |   |       |── common
│   │   │       └── utility/
│   │   └── resources/
│   │       ├── db/migration/
│   │       └── application.yml
│   └── test/
│       └── java/
│           └── <base-package>/
└── reference/
````

Do not arbitrarily rename the existing base package or reorganize an existing project.

---

# 3. Resource-Based Feature Architecture

Feature-specific code must be organized by the primary REST/business resource:

```text
feature/<resource>/
```

A feature requirement file does **not** necessarily represent a separate code feature directory.

If multiple requirement files manipulate the same resource, their code belongs in the same feature directory.

Example:

```text
docs/feature/
├── createUser-requirements.md
├── updateUser-requirements.md
└── deleteUser-requirements.md
```

All belong to:

```text
feature/user/
```

Do not create:

```text
feature/createUser/
feature/updateUser/
feature/deleteUser/
```

## Determining the Resource

Use this priority:

1. REST resource represented by the API path.
2. Primary business entity/resource.
3. Resource represented by a Kafka event.
4. Resource primarily processed by a job.

Example:

```text
POST   /users
GET    /users/{id}
PUT    /users/{id}
DELETE /users/{id}
```

belongs to:

```text
feature/user/
```

If the resource directory already exists, reuse it.

Each resource should have one primary feature directory.

## Exceptions

Authentication and other cross-cutting capabilities may have their own feature directories when they represent distinct architectural responsibilities.

For example:

```text
feature/
├── user/
├── login/
└── auth/
```

Do not force authentication code into `feature/user/` merely because authentication uses users.

---

# 4. Feature Directory Structure

A resource feature generally follows:

```text
feature/<resource>/
├── repo/
├── entity/
├── dto/
├── service/
├── <Resource>Controller.java
├── <Resource>Job.java
└── <Resource>KafkaConsumer.java
```

Create only required directories/classes.

Feature-specific controllers, jobs, and Kafka consumers must be directly under the feature root.

Do not create:

```text
feature/<resource>/controller/
feature/<resource>/job/
feature/<resource>/kafka/
```

unless explicitly required.

Do not create global:

```text
controller/
service/
repository/
entity/
job/
```

packages for feature-specific code.

---

# 5. Shared Resource Components

When multiple requirements use the same resource:

* Reuse the same entity.
* Reuse the same repository.
* Reuse services where appropriate.
* Reuse controllers where appropriate.
* Add operation-specific DTOs when needed.
* Do not duplicate business logic.

Example:

```text
feature/user/
├── repo/UserRepository.java
├── entity/UserEntity.java
├── dto/
│   ├── CreateUserRequest.java
│   ├── UpdateUserRequest.java
│   └── UserResponse.java
├── service/UserService.java
└── UserController.java
```

Cross-resource functionality should call the owning resource's service rather than duplicate its business logic.

---

# 6. Controllers

Controllers must:

* Use Spring REST annotations.
* Receive HTTP requests.
* Validate request data.
* Delegate business logic to services.
* Return DTOs.
* Avoid direct repository access.
* Avoid substantial business logic.

Preferred flow:

```text
HTTP Request
    ↓
Controller
    ↓
Service
    ↓
Repository
    ↓
Database
```

Example:

```java
@RestController
@RequestMapping("/users")
public class UserController {
}
```

JPA entities should not normally be exposed directly through REST APIs.

---

# 7. Jobs

Feature-specific jobs must be directly under the feature root.

Correct:

```text
feature/user/UserCleanupJob.java
```

Incorrect:

```text
feature/user/job/UserCleanupJob.java
```

Jobs should orchestrate scheduled/background processing.

Business logic belongs in services.

---

# 8. Kafka

Kafka consumers belong directly under the owning feature root.

Example:

```text
feature/payment/PaymentKafkaConsumer.java
```

Kafka processing should:

* Deserialize events into DTOs.
* Delegate business logic to services.
* Handle failures according to requirements.
* Implement retry/error handling where required.
* Implement idempotency where required.

Do not place feature-specific consumers in a global Kafka package.

---

# 9. Repository

Repositories belong under:

```text
feature/<resource>/repo/
```

Use Spring Data JPA where appropriate.

Repositories are responsible for database access, not business logic.

Example:

```java
public interface UserRepository
        extends JpaRepository<UserEntity, UUID> {
}
```

---

# 10. Entity

Entities belong under:

```text
feature/<resource>/entity/
```

Use the existing entity base classes.

## REST-Exposed Entity

Use:

```text
UUIDEntityBase
```

for entities exposed through REST APIs.

The database ID must be stored as a string-compatible UUID column, not binary.

In application.yml and application-local.yml, the property ```preferred_uuid_jdbc_type``` should be set to ```VARCHAR```.

## Internal Entity

Use:

```text
IDEntityBase
```

for entities not exposed through REST APIs.

Use:

```text
REST exposed → UUIDEntityBase → UUID
Internal     → IDEntityBase    → Long
```

Do not create duplicate ID base classes.

If entity exposure is ambiguous and affects the design, ask for clarification.

---

# 11. DTO

DTOs belong under:

```text
feature/<resource>/dto/
```

Use DTOs for REST API contracts.

Preferred names:

```text
CreateUserRequest
UpdateUserRequest
UserResponse
LoginRequest
LoginResponse
```

Avoid generic names such as:

```text
UserDto
UserData
UserObject
```

unless justified.

API DTO identifiers must use `UUID`.

Do not expose JPA entities directly unless explicitly required.

---

# 12. Service

Business logic belongs under:

```text
feature/<resource>/service/
```

Services are responsible for:

* Business rules
* Business workflows
* Repository coordination
* Business validation
* Application exceptions
* Transaction boundaries

Controllers should delegate business operations to services.

---

# 13. Exceptions

All application-specific exceptions must extend:

```text
ExceptionBase
```

`ExceptionBase` extends `RuntimeException` and contains an `HttpStatus`.

Example:

```java
public class UserNotFoundException extends ExceptionBase {

    public UserNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
```

Application exceptions belong under:

```text
exception/
```

Do not return `ResponseEntity` from services.

Expected application errors should be represented by `ExceptionBase` subclasses.

---

# 14. Global Exception Handler

Global REST exception handling belongs under:

```text
advice/GlobalExceptionHandler.java
```

Use:

```java
@RestControllerAdvice
```

The preferred flow is:

```text
Service
    ↓
ExceptionBase
    ↓
GlobalExceptionHandler
    ↓
HTTP Response
```

HTTP response construction belongs to the controller/advice layer.

---

# 15. Database and Flyway

Flyway is the source of truth for schema changes.

Migrations belong under:

```text
src/main/resources/db/migration/
```

Use versioned names:

```text
V1__create_user_table.sql
V2__add_user_status.sql
```

Rules:

* Migration versions must be unique.
* Never modify an already-applied migration.
* Create a new migration for schema changes.
* Keep entities and database schema consistent.
* Do not use Hibernate automatic schema generation as the production migration mechanism.

When creating/modifying an entity, determine whether a Flyway migration is required.

---

# 16. Transactions

Transactions should normally be managed at the service layer.

Example:

```java
@Transactional
public void updateUser(...) {
    ...
}
```

Transaction boundaries should correspond to business operations.

Do not arbitrarily add `@Transactional` to every method.

---

# 17. Validation

Use Jakarta/Spring validation for API input constraints.

Example:

```java
public class CreateUserRequest {

    @NotBlank
    private String username;

    @Email
    @NotBlank
    private String email;
}
```

Use:

* DTO validation for input constraints.
* Services for business rules requiring application state or database access.

---

# 18. Authentication and Authorization

Authentication and authorization belong to their appropriate features.

For example:

```text
feature/
├── login/
├── auth/
└── user/
```

Do not duplicate authentication logic across resource features.

The login endpoint must remain accessible without JWT authentication when it is the authentication entry point.

Follow the feature requirements and existing security architecture.

Do not invent a new authentication mechanism without explicit requirements.

---

# 19. shared entity class
enums or model classes shared across multiple features , for exmaple `IDEntityBase` and `UUIDEntityBase`, may be placed under:

```text
common/
```

# 20. Utility

Shared technical utilities may be placed under:

```text
utility/
```

Examples:

```text
DateTimeUtility
JsonUtility
ValidationUtility
```

Do not use `utility` for feature-specific business logic.

Feature-specific functionality belongs to the owning feature.

---

# 21. Dependency Direction

Preferred dependency flow:

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
Entity / Database
```

Typical REST flow:

```text
Request DTO
    ↓
Controller
    ↓
Service
    ↓
Repository
    ↓
Entity
    ↓
Database
    ↓
Response DTO
```

Controllers must not directly access repositories for business operations.

---

# 22. Naming

Use standard Java naming conventions.

Classes:

```text
UserController
UserService
UserRepository
UserEntity
CreateUserRequest
UserResponse
UserCleanupJob
```

Methods and variables use camelCase:

```text
createUser()
getUserById()
userId
createdAt
```

Constants use upper snake case:

```text
MAX_LOGIN_ATTEMPTS
DEFAULT_PAGE_SIZE
```

---

# 23. Configuration and Secrets

Use existing Spring configuration:

```text
application.yml
application.properties
```

Do not hard-code environment-specific values.

Never commit secrets, credentials, tokens, or passwords.

Do not log:

* Passwords
* Tokens
* Credentials
* Secrets
* Sensitive personal information

Do not use `System.out.println()` for application logging.

---

# 24. Testing

Tests should follow the feature structure where practical:

```text
src/test/java/<base-package>/feature/user/
├── UserControllerTest.java
└── UserServiceTest.java
```

Test, where applicable:

* Normal behavior
* Validation failures
* Business errors
* Not-found conditions
* Duplicate conditions
* Authentication/authorization
* Exception handling
* REST responses
* Repository behavior

---

# 25. Feature Implementation Process

Before implementing a feature:

1. Read the feature requirement.
2. Identify its primary resource.
3. Check whether `feature/<resource>/` already exists.
4. Inspect existing implementation.
5. Reuse existing classes and infrastructure.
6. Determine required code and database changes.
7. Implement the smallest appropriate change.
8. Add or update tests.
9. Run Maven build/tests.

Do not create a new feature directory merely because a new requirement file exists.

---

# 26. Ambiguity

Use existing project conventions when ambiguity does not affect architecture.

Ask for clarification when ambiguity affects:

* Public REST API behavior
* Database schema
* Entity identity
* Resource ownership
* Authentication
* Authorization
* Security
* Data privacy
* Major architecture
* Backward compatibility

Do not silently make major architectural decisions.

---

# 27. Existing Code Takes Precedence

Before creating a class, inspect the existing project.

Do not create duplicate:

* Base classes
* Exceptions
* Services
* Repositories
* Utilities
* Configuration
* Infrastructure

When existing code differs from examples in this document, preserve the existing implementation unless a change is explicitly required.

---

# 28. Scope Control

When implementing a feature:

* Do not refactor unrelated features.
* Do not rename unrelated classes.
* Do not reorganize the entire project.
* Do not change unrelated database tables.
* Do not upgrade dependencies without a reason.
* Do not change unrelated public APIs.
* Do not add unsupported functionality.

Keep changes focused on the requested feature.

---

# 29. Validation Checklist

Before considering an implementation complete:

* [ ] Feature requirement was read.
* [ ] Primary resource was identified.
* [ ] Existing resource feature directory was reused when applicable.
* [ ] Feature-specific code follows feature-oriented structure.
* [ ] Controllers are under the feature root.
* [ ] Jobs are under the feature root.
* [ ] Kafka consumers are under the feature root.
* [ ] DTOs are used for REST contracts.
* [ ] API DTO IDs use `UUID`.
* [ ] REST entities use `UUIDEntityBase`.
* [ ] Internal entities use `IDEntityBase`.
* [ ] Business logic is in services.
* [ ] Database access is in repositories.
* [ ] Exceptions extend `ExceptionBase`.
* [ ] Database changes have Flyway migrations.
* [ ] Existing migrations were not modified.
* [ ] Tests were added or updated.
* [ ] Maven build succeeds.
* [ ] Tests pass.
* [ ] No unrelated code was unnecessarily modified.
* [ ] Entity classes and dto classes should be created using loombok builder.
* [ ] Enums should be stored as String in database table.
* [ ] For frequently queried table fields, upate table schema to create indexes. 
* [ ] All job spring scheduling annotations have default values defined in application.yml and application-local.yml

---

# 30. Rule Priority

When rules conflict, use this priority:

1. Explicit user instruction
2. Existing project implementation and conventions
3. Feature requirements
4. This coding guideline
5. Framework/library defaults

Feature requirements define **what** the feature must do.

This document defines **how** the project should generally implement it.

If a conflict affects architecture, security, database schema, or public APIs, ask for clarification.
