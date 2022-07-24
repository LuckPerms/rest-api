# luckperms-rest-api

![](https://img.shields.io/badge/status-beta-important?style=for-the-badge)

A REST API for LuckPerms.

* [API Specification](https://petstore.swagger.io/?url=https://raw.githubusercontent.com/LuckPerms/rest-api/main/src/main/resources/luckperms-openapi.yml)

## Information

* The REST API is bundled as a LuckPerms "[extension](https://luckperms.net/wiki/Extensions)".
* We suggest that you run the rest-api as a standalone application within a Docker container. (see below)
* The API is still a work in progress. Please submit bugs/suggestions in the issues section!

## Usage (Docker)

1. Copy the example [docker-compose.yml](docker/docker-compose.yml) file to somewhere sensible.
2. Open the file and configure your database host/credentials
3. Run `docker compose up -d`
4. The API is now accessible (by default) at `http://127.0.0.1:8080`.

## Usage (Manual)

1. Clone the repository
2. Compile with Gradle (`./gradlew build`)
3. Add `luckperms-rest-api-v1.jar` to the LuckPerms extension folder (`/data/extensions/`).
4. The API is now accessible (by default) at `http://localhost:8080`.

## Configuration

The app can be configured using Java system properties or environment variables.

| Environment Variable       | Description                                      | Default Value |
|----------------------------|--------------------------------------------------|---------------|
| `LUCKPERMS_REST_HTTP_PORT` | The port that the HTTP server should listen on   | `8080`        | 
| `LUCKPERMS_REST_AUTH`      | If API key authorization is enabled              | `false`       |
| `LUCKPERMS_REST_AUTH_KEYS` | A comma-separated list of accepted API keys      | *none*        |

## Security

By default, the example Docker Compose setup only makes the API available to applications running on the host machine.
For this reason, authentication is disabled by default.

However, if you decide to make the API available over a wider network (e.g. the internet), then it is crucial that **you configure authentication using API keys** and enable HTTPS by exposing the API behind a reverse proxy (e.g. nginx).

You enable auth by setting `LUCKPERMS_REST_AUTH` to true, and setting `LUCKPERMS_REST_AUTH_KEYS` to a comma separated list of allowed API keys.

e.g.
```yml
LUCKPERMS_REST_AUTH: "true"
LUCKPERMS_REST_AUTH_KEYS: "myverysecureapikey,anotherverysecurekey"
```

Once enabled, API keys should be sent as a `Bearer` token inside the `Authorization` header of API requests.
e.g. 
```
Authorization: Bearer myverysecureapikey
```