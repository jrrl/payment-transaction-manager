# payment-transaction-manager HOWTO

Used to demonstrate best practices and NFR Pull Requests.

## General config

Application is developed using JDK 17. Gradle is a part of the repository. So the only tool to dev/run/test app
is [JDK for your platform](https://jdk.java.net/17/).

Integration testing phase needs postgreSQL up & running

* default values are:
    * user: postgres
    * password: postgres
    * dbName: template
    * port: 15432
* Ideally it can be run using
  docker: `docker run --name postgres -e POSTGRES_PASSWORD=postgres -e POSTGRES_USER=postgres -e POSTGRES_DB=template -p 15432:5432 -d postgres`

### Project structure

Code is structured as follows:

* `src/main` - for module code
* `src/test` - for unit testing
* `src/itest` - for integration testing

### Running

For running application locally or within container, environment variables need to be set up:

* `DB_URL` pointing to DB. Default for integration db run locally: `jdbc:postgresql://localhost:15432/template`
* `DB_USER` user name for DB access. Default for integration db run locally: `postgres`
* `DB_PASSWORD` password for access database. Default for integration db run locally: `postgres`
* `SAFI_ACCOUNT_MANAGER_URL` URL for Account Manager Api (this is purely an example of generated client from another microservice). Default value should
  be: `https://account-manager.apps.dev.safibank.online`
* All development environment variables are provided in `.env.dev` file

#### As Java app

* to run application use command `./gradlew run` with all env entries needed for application

#### Using containerisation

To obtain image, there are two options:

* Get TAR file: `./gradlew jibBuildTar` This command generates TAR file within build folder: `build/jib-image.tar`
* Get image within local docker instance: `./gradlew jibDockerBuild`
  To run image there are needed environment entries above. Example command to run in local docker instance is:
* `docker run --name payment-transaction-manager -p 8080:8080 -e DB_URL=jdbc:postgresql://<PC_IP_ADDRESS>:15432/template -e DB_USER=postgres -e DB_PASSWORD=postgres -d template:0.1`

#### InitContainer

* to run [InitContainer](../../common/utils/README.md) phase, application needs to be run with all the environment variables as in normal case with additional variable:
```
MICRONAUT_ENVIRONMENTS=init
```
* this will run all the initContainer implementations (e.g. flyway DB migration) that are enabled for current application

### Unit testing

Unit testing needs docker available as postgresql test container is built up during run

## Commands

* Build and test all: `./gradlew clean build`
* Build and skip unit tests: `./gradlew clean build -x test`
* Build and skip integration tests: `./gradlew clean build -x integrationTest` -> No running postgres is needed
* Build and skip all tests: `./gradlew clean build -x test -x integrationTest`

## OpenApi

### Swagger

* Run in browser:
    * Local: `http://localhost:8080/swagger/views/swagger-ui/#/`
    * Remote:`<SERVICE_URL>/swagger/views/swagger-ui/#/`

### Using generated REST client library

Example of usage found in `AccountAdapter`. Simulates integration to `account-manager` via REST API.

1. Add dependency to generated library published to maven repository in `build.gradle.kts`
   * artifact-id (name) formatted as `{service-name}-api-client` e.g.: `account-manager-api-client`
   * artifact-group is the same as module, for which the client is generated (found in `build.gradle.kts`)
2. Add single property to `application.yml` that sets the URL base-path for client, e.g.
   ```yml
   account-manager-api-client-base-path: ${SAFI_ACCOUNT_MANAGER_URL}`
   ```
3. Add the environment variable (e.g. `SAFI_ACCOUNT_MANAGER_URL`) values for all environments
   * `/devops/argocd/environments/dev/apps/{your-service}/values.yaml`
   * `/devops/argocd/environments/stage/apps/{your-service}/values.yaml`
4. Inject `DefaultApi` interface and use - works out of the box
