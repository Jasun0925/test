## purpose
 setup the local environment for development. execute the following steps

## steps 
1. create `docker-compose.yml` at `<project-root>` to host redis and mysql database, exposing MySQL on port 3306 and Redis on port 6379 unless the project already uses other ports.
2. seed the mysql database with flyway migration files: put the schema migrations under `src/main/resources/db/migration` and let Flyway apply them when the application starts. Do not create the tables by hand, and do not let Hibernate generate the schema.
3. create a local profile file `src/main/resources/application-local.yml` for the `local` profile. Spring Boot only reads profile files named `application-<profile>.yml`, with a hyphen.
4. in `application-local.yml`, configure the Springboot application to use the redis and mysql hosted in docker-compose, with the same host, port, database name, username and password as in `docker-compose.yml`. Activate the profile by starting the application with `--spring.profiles.active=local`.
