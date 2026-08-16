# Architecture

## 1. System architecture

The backend is a distributed system composed of several application services plus infrastructure services.

```text
                         ┌─────────────────┐
                         │    Frontend     │
                         └────────┬────────┘
                                  │ HTTP
               ┌──────────────────┼──────────────────┐
               │                  │                  │
               ▼                  ▼                  ▼
       ┌──────────────┐   ┌──────────────┐   ┌──────────────┐
       │Authentication│   │User Service  │   │Event Service │
       │  Spring Boot │   │ ASP.NET Core │   │ ASP.NET Core │
       └──────┬───────┘   └──────▲───────┘   └──────┬───────┘
              │                  │                  │
              │ gRPC             │                  │ gRPC
              └──────────────────┘                  │
                                                   ▼
                                            ┌──────────────┐
                                            │ Reservation  │
                                            │ Spring Boot  │
                                            └──────┬───────┘
                                                   │
                                                   │
                                                   ▼
                                            ┌──────────────┐
                                            │  PostgreSQL  │
                                            └──────────────┘


                    ┌───────────────────────┐
                    │        RabbitMQ       │
                    └───────────┬───────────┘
                                │ AMQP
                                ▼
                    ┌───────────────────────┐
                    │ Notification Service  │
                    │     ASP.NET Core      │
                    └───────────────────────┘
```

## 2. Communication matrix

| Caller | Callee | Protocol | Purpose |
|---|---|---|---|
| Frontend | Authentication | HTTP/REST | Login/signup/token flows |
| Frontend | User Service | HTTP/REST | User operations |
| Frontend | Event Service | HTTP/REST | Events and tickets |
| Frontend | Reservation | HTTP/REST | Reservations |
| Authentication | User Service | gRPC | User lookup and user-management operations |
| Event Service | Reservation | gRPC | Reserved-seat lookup |
| Application producer | RabbitMQ | AMQP | Asynchronous email message delivery |
| RabbitMQ | Notification Service | AMQP | Email queue consumption |
| .NET services | PostgreSQL | PostgreSQL protocol | Persistence |
| Java services | PostgreSQL | PostgreSQL protocol | Persistence |
| Java services | Redis | Redis protocol | Redis-based infrastructure |
| Application services | OTel Collector | OTLP/gRPC | Telemetry |

## 3. Authentication sequence

```text
Client
  |
  | POST /api/auth/...
  v
Authentication Service
  |
  | gRPC user lookup / validation
  v
User Service
  |
  +--------------------+
                       |
  <--------------------+
  |
  | create RS256 JWT
  v
Client
  |
  | Authorization: Bearer <JWT>
  v
Protected Service
  |
  | validate issuer/audience/signature
  v
Authorization policy
  |
  v
Controller
```

The Authentication Service owns token creation.

The .NET services consume the resulting JWT and apply role-based authorization.

## 4. Event-seat lookup sequence

```text
Client
   |
   | GET seat information
   v
Event Service
   |
   | MediatR query
   v
GetSeatsByConfig query handler
   |
   | gRPC
   v
Reservation Service
   |
   | repository query
   v
PostgreSQL
   |
   v
Reservation Service
   |
   | gRPC response
   v
Event Service
   |
   v
HTTP response
```

## 5. Notification sequence

```text
Application Service
       |
       | publish email message
       v
    RabbitMQ
       |
       | emailQueue
       v
Notification Service
       |
       v
Email Service
       |
       v
Email provider
```

The RabbitMQ design makes notification processing asynchronous relative to the producer.

## 6. Database architecture

The current implementation uses a shared PostgreSQL database:

```text
               PostgreSQL
              sportik-backend
                    |
        ┌───────────┼───────────┐
        │           │           │
        ▼           ▼           ▼
     Users    Authentication  Reservation
        │           │           │
        └───────────┴───────────┘
                    │
                    ▼
               Event data
```

The repository therefore has service boundaries at the application/code level, while persistence is currently shared at the database level.

## 7. Internal architecture of the .NET services

### User Service

```text
Users.API
    |
    v
Users.Application
    |
    v
Users.Domain
    |
    v
Users.Infrastructure
    |
    v
PostgreSQL
```

The API layer contains controllers, middleware, validation, authorization, and gRPC endpoints.

The application layer contains use-case-oriented services.

The domain layer contains models/enums/repository abstractions.

The infrastructure layer contains database access and repository implementations.

### Event Service

The Event Service is organized around API, application, domain/common functionality, and infrastructure.

Its application layer uses MediatR commands and queries.

A typical request is:

```text
Controller
   -> MediatR
   -> Command/Query Handler
   -> Repository / Infrastructure
```

## 8. Infrastructure architecture

```text
                         OpenTelemetry
                              |
                              v
                    ┌──────────────────┐
                    │ OTel Collector   │
                    └──────┬───────────┘
                           /|                          / |                          /  |                          v   v   v
                     Tempo Loki Prometheus
                                  |
                                  v
                               Grafana
```

Other infrastructure components:

```text
PostgreSQL -> relational persistence
Redis      -> Redis-based support for Java services
RabbitMQ   -> asynchronous messaging
Docker     -> service packaging
Compose    -> local multi-container orchestration
```

## 9. Container networking

All application containers use the Docker network:

```text
sportik-network
```

Internal service communication therefore uses Docker DNS names.

Examples from the deployment:

```text
postgres:5432
rabbitmq:5672
redis:6379
user-service:8080
user-service:8081
event-service:8080
event-service:8081
reservation:8081
reservation:9091
```

Host port mappings are only for access from outside Docker.

## 10. Documentation scope

The architecture documentation intentionally describes implemented behavior rather than an idealized microservice architecture.

In particular:

- PostgreSQL is shared.
- gRPC is used selectively between services.
- RabbitMQ is used for asynchronous notification processing.
- Authentication is centralized.
- The frontend uses service-specific upstreams defined by Docker Compose.
- Observability is part of the deployed system rather than an external assumption.
