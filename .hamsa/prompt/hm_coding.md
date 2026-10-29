You are a coding agent. implement the coding task according to the following instructions.

# Feature Implementation

Implement Java Spring Boot features according to feature requirement files.


## Temporary Files

Store temporary files under:
<project-root>/tmp

## Process

1. Read and understand the selected feature requirement(s).
2. If `<project-root>/src` does not exist, create a Java Spring Boot application.
3. Follow the coding guidelines and the local-development-environment document
   reproduced at the end of this prompt. They are binding: the project
   structure, package layout, technology stack and naming rules they define
   override your own preferences, and they override a design or feature
   requirement document that prescribes otherwise. Those documents state what
   each feature must do — implement that inside the structure below, even when
   their example code, database choice or package names differ.
4. Implement all features under directory ```<project-root>/docs/feature/```
5. Related features must be implemented in the same feature package/directory.
6. Reuse existing implementations and dependencies; do not duplicate functionality.
7. Implement only requirements defined by the feature requirements and referenced design.

## Implementation Rules

- Implement required controllers, services, repositories, entities, DTOs, jobs, and Kafka receivers.
- Preserve required public function signatures and DTO definitions.
- API DTO identifiers must use `UUID`.
- Implement required validation, security, transactions, error handling, retries, and idempotency.
- Follow existing project conventions and the coding guidelines reproduced below.
- Do not invent unsupported functionality.
- If a required dependency is missing, implement only the minimum required dependency.
- Name classes, methods, packages and directories as the coding guidelines require
  (PascalCase classes, camelCase methods, lowercase package and directory names).

## Validation

After implementation:
- Compile the project.
- Run available tests.
- Fix implementation-related build/test failures.
- Verify public signatures and DTOs match the requirements.
- Verify the feature package structure against the coding guidelines
  (`feature/<resource>/`, controllers/jobs/consumers directly under the feature root).
- Identify unresolved requirements or `TBD` items.

## Final Step

Configure the local development environment according to the document
reproduced at the end of this prompt under `reference/local_env.md`.
update ```application-local.yml ``` file to use the exact database name, db user name, db user password as defined in ```docker-compose.yml``` in the project root directory.
Verify the application can build and start using the documented environment.

## Final Response

Report:
- Implemented features
- Files/classes created or modified
- Build/test result
- Local environment status
- Remaining issues/TBDs

