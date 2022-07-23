# luckperms-rest-api

![](https://img.shields.io/badge/status-beta-important?style=for-the-badge)

A REST API for LuckPerms.

* [API Documentation](https://petstore.swagger.io/?url=https://raw.githubusercontent.com/LuckPerms/rest-api/main/src/main/resources/luckperms-openapi.yml)

## Information

* The REST API is bundled as a LuckPerms "[extension](https://luckperms.net/wiki/Extensions)".
* This means that in theory, you could install it on your Minecraft server/proxy directly. However, this not recommended.
* Instead, consider standing up a standalone instance of LuckPerms and installing the extension there.
* The extention / API is still a work in progress. Submit bugs/suggestions in the issues section please!

### Usage

1. Compile with Gradle -- `./gradlew build`
2. Add `luckperms-rest-api-v1.jar` to the LuckPerms extension folder (`/data/extensions/`).

The API will be available at `http://localhost:8080`.

The port can be configured using the `LUCKPERMS_REST_API_PORT` environment variable.