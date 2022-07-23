# luckperms-rest-api

![](https://img.shields.io/badge/status-beta-important?style=for-the-badge)

A REST API for LuckPerms.

* [API Specification](https://petstore.swagger.io/?url=https://raw.githubusercontent.com/LuckPerms/rest-api/main/src/main/resources/luckperms-openapi.yml)

## Information

* The REST API is bundled as a LuckPerms "[extension](https://luckperms.net/wiki/Extensions)".
* This means that in theory, you could install it on your Minecraft server/proxy, but this not recommended.
* Instead, consider creating a standalone instance of LuckPerms and installing the extension there.
* The extension / REST API is still a work in progress. Submit bugs/suggestions in the issues section please!

## Usage

1. Compile with Gradle -- `./gradlew build`
2. Add `luckperms-rest-api-v1.jar` to the LuckPerms extension folder (`/data/extensions/`).

By default, the API will be available at `http://localhost:8080`.

## Configuration

The extension can be configured using Java system properties or environment variables.

| Property                   | Description                                      | Default Value |
|----------------------------|--------------------------------------------------|---------------|
| `luckperms.rest.http.port` | The port that the HTTP server should listen on   | `8080`        | 
| `luckperms.rest.auth`      | If API key authorization is enabled              | `false`       |
| `luckperms.rest.auth.keys` | A comma-separated list of accepted API keys      | *none*        |

When enabled, API keys should be sent as a Bearer token inside the Authorization header:
```
Authorization: Bearer <api key>
```

You can configure using environment variables by taking the property, converting to upper-case and replacing `.` with `_`.   
e.g. `luckperms.rest.http.port` becomes `LUCKPERMS_REST_HTTP_PORT`
