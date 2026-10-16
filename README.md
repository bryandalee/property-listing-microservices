# Property Listing Microservices

A small backend for property listings (rent or sale), split into three Spring Boot services: one stores listings, one stores users, and a public API combines the two for outside clients.

The listing service was given as starter code. I built the user service and the public API, along with their tests and the Docker setup.

**Stack:** Java 17, Spring Boot 3.1, Spring Data JPA, Flyway, H2, Gradle, JUnit, Docker Compose

## Architecture

```
client ──► public-api :8080 ──HTTP──► listing-service :8081 ──► H2 (listings)
                       │
                       └─────HTTP──► user-service :8082 ──► H2 (users)
```

| Service | Port | |
|---|---|---|
| `listing-service` | 8081 | Starter code. Stores listings. |
| `user-service` | 8082 | Stores users. Same structure as the listing service, with its own H2 database. |
| `public-api` | 8080 | The only service meant to be called from outside. It has no database and gets everything from the other two over HTTP. |

Each service owns its data. No service reads another service's database; everything goes through its REST API.

## Running it

With Docker, from the repository root:

```bash
docker compose up --build
```

The first build takes a minute or two. Only the public API is published (port 8080). The listing and user services can only be reached inside the compose network. To call them directly, run them with Gradle instead.

Without Docker, you need Java 17. Each service is a separate Gradle project, so start them in three terminals:

```bash
cd listing-service && ./gradlew bootRun
cd user-service && ./gradlew bootRun
cd public-api && ./gradlew bootRun
```

On Windows, use `gradlew.bat bootRun`. The databases are in-memory, so data is lost on restart.

A quick check once all three are up:

```bash
curl -X POST localhost:8080/public-api/users -H 'Content-Type: application/json' -d '{"name":"Jane Doe"}'
curl -X POST localhost:8080/public-api/listings -H 'Content-Type: application/json' -d '{"userId":1,"listingType":"rent","price":6000}'
curl localhost:8080/public-api/listings
```

## Tests

Run `./gradlew test` inside `user-service` or `public-api` (29 tests in total). The public API tests don't need the other services running. They use `MockRestServiceServer` in place of the listing and user services.

## API

### Public API (port 8080, JSON)

| Method | Path | |
|---|---|---|
| `GET` | `/public-api/listings` | Listings, newest first, each with its owner embedded. Query: `pageNum` (default 1), `pageSize` (default 10, max 100), `userId` (optional). |
| `POST` | `/public-api/listings` | Body: `{"userId": 1, "listingType": "rent", "price": 6000}`. `listingType` is `rent` or `sale`. Returns 400 if the user doesn't exist. |
| `POST` | `/public-api/users` | Body: `{"name": "Jane Doe"}` |

Example response for `GET /public-api/listings` (timestamps are in microseconds):

```json
{
  "result": true,
  "listings": [
    {
      "id": 1,
      "listingType": "rent",
      "price": 6000,
      "createdAt": 1475820997000000,
      "updatedAt": 1475820997000000,
      "user": {
        "id": 1,
        "name": "Jane Doe",
        "createdAt": 1475820997000000,
        "updatedAt": 1475820997000000
      }
    }
  ]
}
```

Errors always use the same body: `{"result": false, "message": "..."}`.

### Internal services (form-encoded requests)

| Service | Endpoints |
|---|---|
| `listing-service` | `GET /listings` (`pageNum`, `pageSize`, `userId`), `POST /listings` (`userId`, `listingType`, `price`) |
| `user-service` | `GET /users` (`pageNum`, `pageSize`, optional `ids=1,2,3`), `GET /users/{id}`, `POST /users` (`name`) |

## How the public API works

- `GET /public-api/listings` gets one page of listings from the listing service, then fetches all of their owners from the user service in a single request (`GET /users?ids=1,2,3`). A page of listings always costs two requests in total, however many owners it has. I added the `ids` filter to the user service for this. It's optional; without it `GET /users` is a normal paginated list.
- `POST /public-api/listings` checks with the user service that the user exists before creating the listing. The listing service doesn't know about users and accepts any `userId`, so the check has to happen here, since this is the only service that knows about both.
- `POST /public-api/users` forwards to the user service.

The public API accepts JSON and converts it to the form-encoded requests that the internal services expect.

## Decisions

- The public API can't touch a database. `public-api/build.gradle` doesn't include JPA, H2 or Flyway at all, so all data has to come through the other services' APIs.
- Service URLs and timeouts are configuration (`services.*` in `public-api/src/main/resources/application.yml`). The connect timeout is 2s and the read timeout 5s, so a stuck service makes the request fail instead of hang.
- Failures from the other services are translated. If a service is down or times out, the public API answers 503. If it returns a 5xx, the public API answers 502. If it rejects the request with a 4xx, its status and message are passed through.
- If a listing points to a user that doesn't exist, the listing is still returned with `"user": null` instead of failing the whole page.
- Nothing is shared between services. The DTOs and error handling are duplicated on purpose, so one service can change without breaking another.
- The user service follows the listing service's layout (controller, service, repository, Flyway migration). Two small differences: constructor injection instead of field `@Autowired`, and a primary key on the `users` table.
- In the user service and the public API, every error uses the same `{"result": false, "message": "..."}` body as the listing service, with readable messages such as `name: must not be blank`. This includes unknown routes and wrong HTTP methods.
- `pageSize` on the public API is capped at 100, and the user service accepts at most 100 ids per request, so the owners of any page fit in one lookup.

## What I'd do next

- Clean up the listing service's validation errors, which currently return the full exception text. I left the starter listing service as it was, apart from adding a Dockerfile.
- Add health checks to docker-compose, so the public API only starts once the other services are ready instead of relying on start order alone.
- Give each service its own real database (for example a Postgres container per service) instead of in-memory H2.
