# SWIYU Trust Management SCS

The service contains the business logic to do the onboardings to the trust registry.

## Getting Started

Before using the service make sure the following services are running:

- [swiu-core-business-service](https://github.com/swiyu-admin-ch/swiyu-core-business-service) for the kafka container
  setup

For local development, the service can be started with the following command:

### Backend with bundled UI

```shell
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### Backend with extra served UI

```shell
mvn spring-boot:run -Dspring-boot.run.profiles=local,local-ui

```

### Backend with extra served UI and local shared services

```shell
./mvnw spring-boot:run -Dspring-boot.run.profiles=local,shared
```

### Backend with extra served UI and shared services from DEV

```shell
./mvnw spring-boot:run -Dspring-boot.run.profiles=local,shared,shared-dev
```

## Configuration

The following environment variables must be set

| Variable Name                                     | Description                                                                               | Default Value       |
|---------------------------------------------------|-------------------------------------------------------------------------------------------|---------------------|
| `AUTH_RESOURCE_ID`                                | The resource ID for the OAuth2 resource server.                                           | `BJ-swiyu-tms`      |
| `AUTH_ISSUER_URI`                                 | The issuer URI for the OAuth2 authorization server.                                       |                     |
| `AUTH_JWK_SET_URI`                                | The JWK set URI for the OAuth2 authorization server.                                      |                     |
| `AUTH_SWIYU_KEYCLOAK_REALM_ECOSYSTEM_ISSUER_URI`  | The issuer URI of the Keycloak ecosystem realm (used for OAuth2 client token retrieval).  |                     |
| `AUTH_SWIYU_KEYCLOAK_REALM_ECOSYSTEM_JWK_SET_URI` | The JWK set URI of the Keycloak ecosystem realm (used for OAuth2 client token retrieval). |                     |
| `AUTH_CORE_BUSINESS_SERVICE_CLIENT_SECRET`        | The OAuth2 client secret used to obtain tokens for the core-business-service REST client. |                     |
| `PROMETHEUS_USER`                                 | The username for accessing /actuator/prometheus endpoint.                                 |                     |
| `PROMETHEUS_PASSWORD`                             | The password for accessing /actuator/prometheus endpoint.                                 |                     |
| `IS_INIT_CONTAINER_EXECUTION`                     | Indicates if the application is running in an init container.                             | false               |
| `REGISTRY_BASE_READ_URL`                          | The URL for base registry for reading entries.                                            |                     |
| `ISSUER_STATUS_LIST_URI`                          | The URI of the status list which should be used for onboardings.                          |                     |
| `ISSUER_MANAGEMENT_URL`                           | The URL for the issuer management API endpoint.                                           |                     |
| `ISSUER_OID4VCI_URL`                              | The URL for the issuer oid4vci API endpoint.                                              |                     |
| `JWT_SIGNING_KEY`                                 | The pem formatted private key with which requests to gov issuer/trust registry are signed |                     |
| `JWT_SIGNING_KEY_ID`                              | The identifier (kid) to be used when signing the JWT                                      |                     |
| `TRUST_REGISTRY_DB_URL`                           | The JDBC url to the trust registry database.                                              |                     |
| `TRUST_REGISTRY_DB_USERNAME`                      | The username to connect to the trust registry database.                                   |                     |
| `TRUST_REGISTRY_DB_PASSWORD`                      | The password to connect to the trust registry database.                                   |                     |
| `TRUST_REGISTRY_DATA_URL`                         | The public endpoint to the trust data registry.                                           |                     |
| `TRUST_REGISTRY_VC_SCHEMA_ENDPOINT`               | The path to the VC schema endpoint on the trust registry.                                 | `/api/v1/vc-schema` |
| `CORE_BUSINESS_SERVICE_BASE_URL`                  | The base URL of the core business service REST API.                                       |                     |
| `ENVIRONMENT`                                     | The environment name exposed to the frontend (e.g. `dev`, `ref`, `prod`).                 |                     |
| `FUNCTIONALITY_AUTOMATIC_APPROVAL_ENABLED`        | Feature flag to enable or disable automatic approval of onboarding submissions.           |                     |

### Secrets config

Due to the necessity of local secrets for development there might be profiles used in the run configurations
which are not part of this repository. Those profiles are generated outside of this repository and provided
to the application als `application-*.yaml` files in the root `/config` directory or directly via environment
variables.

## Generating Keys in order to access Gov Trust Issuer and Trust Registry API

When accessing the APIs of gov trust issuer, the request must be signed with a private key. The private key must be
configured for the trust management scs and the public key must be configured on the gov trust issuer.

** For a detailed setup guide see https://confluence.bit.admin.ch/x/_Cw_N (for FOITT internal usage only)**

To generate a new private and public key for a new stage, the following scripts can be executed:

```shell
mvn -f utils/key-generator/pom.xml compile exec:java
```

This will create 2 files in a `.keys` directory, with those files you can now configure

1. Trust management scs

- in vault put the content of JWT_SIGNING_KEY.pem into a variable JWT_SIGNING_KEY
- in vault put the `kid` value from JWT_PUBLIC_KEY.json into the JWT_SIGNING_KEY_ID

2. Gov Trust Issuer (Register the public key)

- add json of the public key JWT_PUBLIC_KEY.json into the JWKS_ALLOWLIST env variable (gitops or vault)