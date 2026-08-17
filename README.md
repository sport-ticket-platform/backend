# Sport Ticket Platform Backend

### Frontend: [GitHub Repository](https://github.com/sport-ticket-platform/frontend)

Backend platform for a sports ticketing system implemented as a set of cooperating microservices.

The current repository is a hybrid .NET and Spring Boot system:

- **ASP.NET Core / .NET 9**
  - User Service
  - Event Service
  - Notification Service
- **Spring Boot / Java**
  - Authentication Service
  - Reservation Service
- **PostgreSQL 17** for relational persistence
- **Redis 7** for Redis-based infrastructure used by the Java services
- **RabbitMQ 3** for asynchronous notification messaging
- **OpenTelemetry + Grafana stack** for traces, logs, and metrics
- **Docker Compose** for local multi-service deployment

The source tree is organized under `src/` and currently contains the `Users`, `Events`, `auth`, `reservation`, `Notification`, and shared `protos` areas.

## System overview

At runtime, the major request flow is:

```text
Frontend
   |
   +----> Authentication Service
   |          |
   |          +---- gRPC ----> User Service
   |
   +----> User Service
   |
   +----> Event Service
   |          |
   |          +---- gRPC ----> Reservation Service
   |
   +----> Reservation Service
```

Notification delivery is asynchronous:

```text
Application Service
       |
       | AMQP
       v
    RabbitMQ
       |
       v
Notification Service
       |
       v
      Email
```

The services run on a shared Docker Compose network and use container DNS names for internal communication.

## Main services

### User Service

`src/Users`

ASP.NET Core / .NET 9 service responsible for:

- user profile operations
- user reports
- city search
- administrator report management
- administrator user management
- user-related gRPC operations consumed by the Authentication Service

The REST API is rooted at:

```text
/api/user
/api/Admin
```

The service also exposes the `UserService` gRPC contract.

### Event Service

`src/Events/EventService`

ASP.NET Core / .NET 9 service responsible for:

- match lookup and filtering
- ticket configurations
- seats
- venues
- leagues
- creation of matches by administrators
- creation of ticket configurations by administrators

The REST API is rooted at:

```text
/api/event
```

The service uses MediatR for command/query dispatching and contains a gRPC client for Reservation Service.

### Authentication Service

`src/auth`

Spring Boot service responsible for:

- password login
- email OTP login
- phone OTP login
- signup
- refresh-token handling
- logout
- password reset
- JWT generation

The REST API is rooted at:

```text
/api/auth
```

JWTs are signed with **RS256** and contain the user ID in `sub` and user roles in the `roles` claim.

Authentication also communicates with User Service through gRPC for user lookup and user-management operations.

### Reservation Service

`src/reservation`

Spring Boot service responsible for:

- seat reservation
- reservation history
- reservation details
- reservation cancellation
- wallet/payment/order functionality present in the module
- exposing the gRPC reservation contract consumed by Event Service

The REST API is rooted at:

```text
/api/reservations
```

### Notification Service

`src/Notification/NotificationService`

ASP.NET Core / .NET 9 service responsible for notification processing, including email delivery.

It contains RabbitMQ consumer functionality and email-related services.

## Communication

The repository currently uses three communication mechanisms.

### REST

REST is used by the frontend-facing application APIs.

Examples include:

```text
/api/auth
/api/user
/api/Admin
/api/event
/api/reservations
```

### gRPC

gRPC is used for synchronous service-to-service communication.

Important contracts include:

- `UserService`
- `ReservationService`

The shared protobuf definitions can be found under `src/protos` and service-specific `proto` directories.

### RabbitMQ

RabbitMQ is used for asynchronous notification processing.

The notification service is configured with the `emailQueue` queue.

## Authentication

The Authentication Service issues JWT access tokens using RSA/RS256.

The downstream .NET services validate the JWT with the configured RSA public key and enforce role-based authorization policies such as:

```text
RequireUser
RequireAdmin
RequireSupportOrAdmin
```

The User Service obtains the authenticated user's ID from the JWT `sub` claim.

## Database

The deployment uses PostgreSQL 17 with database:

```text
sportik-backend
```

The database is initialized by:

```text
deployment/data/init.sql
```

The schema includes domains for:

- users and cities
- roles and application settings
- wallets and wallet transactions
- sports, teams, leagues, venues, and matches
- ticket categories and configurations
- seats
- reservations
- ticket orders
- payments
- sold tickets
- support reports
- refresh tokens

The current deployment uses a shared PostgreSQL database for several services.

## Docker Compose

The main deployment definition is:

```text
deployment/compose.yaml
```

It starts the application services together with:

- PostgreSQL
- RabbitMQ
- Redis
- OpenTelemetry Collector
- Tempo
- Loki
- Prometheus
- Grafana
- frontend

The Compose file also defines persistent volumes for PostgreSQL and RabbitMQ and a Docker secret for the Gmail application password used by the Notification Service.

## Observability

The .NET services are configured with OpenTelemetry instrumentation for application and infrastructure telemetry.

The deployment contains:

```text
OpenTelemetry Collector
        |
        +----> Tempo       (traces)
        +----> Loki        (logs)
        +----> Prometheus  (metrics)
                         |
                         v
                      Grafana
```

Configuration files are located under:

```text
deployment/data/
```

## Documentation

Project documentation is kept under `docs/`.

Start with:

- [`docs/project-overview.md`](docs/project-overview.md)
- [`docs/architecture.md`](docs/architecture.md)

These documents describe the system from a developer's point of view without replacing the source code as the final source of truth.
