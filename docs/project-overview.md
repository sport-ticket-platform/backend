# Project Overview

## 1. What this project is

The repository implements the backend of a sports ticketing platform.

The application is split into multiple services rather than being implemented as one application. The services are written in two different technology stacks:

- ASP.NET Core / .NET 9
- Spring Boot / Java

The services cooperate through HTTP/REST, gRPC, and RabbitMQ.

The current codebase contains:

```text
src/
├── Events/
├── Notification/
├── Users/
├── auth/
├── protos/
└── reservation/
```

This structure represents the major application domains currently present in the repository.

## 2. Service responsibilities

### User Service

Location:

```text
src/Users
```

Implementation:

```text
ASP.NET Core / .NET 9
```

Responsibilities visible in the source:

- retrieve a user's profile
- update a user's profile
- create and retrieve user reports
- search cities
- allow administrators to inspect and answer reports
- allow administrators to retrieve users and change account status
- expose gRPC methods required by Authentication Service

The service is internally divided into:

```text
Users.API
Users.Application
Users.Domain
Users.Infrastructure
```

This is the clearest layered architecture in the repository.

### Event Service

Location:

```text
src/Events/EventService
```

Implementation:

```text
ASP.NET Core / .NET 9
```

Responsibilities:

- retrieve matches
- retrieve filtered matches
- retrieve ticket configurations for matches
- retrieve seats
- retrieve venues
- retrieve leagues
- create matches
- create ticket configurations

The API layer delegates operations through MediatR commands and queries.

### Authentication Service

Location:

```text
src/auth
```

Implementation:

```text
Spring Boot / Java
```

Responsibilities:

- password login
- email OTP login
- phone OTP login
- OTP verification
- signup
- token refresh
- logout
- password reset

Authentication is not implemented inside the User Service. Instead, the Authentication Service communicates with User Service through a gRPC contract.

### Reservation Service

Location:

```text
src/reservation
```

Implementation:

```text
Spring Boot / Java
```

Responsibilities visible in the source:

- create reservations
- retrieve reservation history
- retrieve reservation details
- cancel reservations
- expose reservation information through gRPC
- order, payment, wallet, and related reservation functionality

### Notification Service

Location:

```text
src/Notification/NotificationService
```

Implementation:

```text
ASP.NET Core / .NET 9
```

Responsibilities:

- consume email messages from RabbitMQ
- process email-related work
- provide notification-related services, including a gRPC service in the repository

## 3. How the services communicate

### Frontend to backend

The frontend container is configured with upstream addresses for:

```text
Authentication
User Service
Event Service
Reservation Service
```

The public-facing path therefore goes through HTTP requests to the individual APIs.

### Authentication to User Service

The Authentication Service uses gRPC to access user information and perform user operations required by authentication flows.

The shared `user.proto` contract contains operations such as:

```text
GetUserByEmail
GetUserByPhone
GetUserById
CheckEmailExists
CreateUser
ChangeUserPassword
```

The response used by authentication contains fields such as:

```text
id
role
email
phone
password
status
is_two_factor_enabled
```

### Event Service to Reservation Service

Event Service uses gRPC to ask Reservation Service for reserved seats.

The contract is:

```text
ReservationService
    └── GetReservedSeatsByConfigIds
```

The request contains a list of ticket configuration IDs.

The response contains:

```text
seat_id
config_id
```

This information is used by Event Service when returning seat-related information.

### RabbitMQ and notifications

RabbitMQ provides asynchronous communication for email notification processing.

The deployment configures:

```text
queue = emailQueue
```

The Notification Service contains the corresponding RabbitMQ consumer.

## 4. Authentication model

Authentication is centralized in the Authentication Service.

The service generates JWT access tokens using RSA/RS256.

The token contains:

```text
sub
roles
iss
aud
iat
exp
```

The `sub` value represents the user's ID.

The .NET services validate the token using the RSA public key and then apply application authorization policies.

The User Service currently defines:

```text
RequireUser
RequireAdmin
RequireSupportOrAdmin
```

## 5. Database model

The PostgreSQL database is created from:

```text
deployment/data/init.sql
```

The schema currently contains tables covering the following areas.

### Identity

```text
city
users
refresh_token
app_setting
```

### Wallet

```text
wallet
wallet_transaction
```

### Sports and events

```text
sport
team
venue
league
match
```

### Ticketing

```text
ticket_category
ticket_config
seat
sold_ticket
```

### Reservations

```text
reservation
reservation_seat
```

### Orders and payments

```text
ticket_order
payment_methods
payment
```

### Support

```text
report
```

The reservation schema contains an active-seat uniqueness constraint so an active seat cannot be assigned to multiple active reservations at the same time.

## 6. Deployment topology

The main deployment is described by:

```text
deployment/compose.yaml
```

The Compose deployment contains the following important components:

```text
user-service
event-service
authentication
reservation
notification-service
frontend
postgres
rabbitmq
redis
otel-collector
tempo
loki
prometheus
grafana
```

All of them are connected to the `sportik-network` Docker bridge network.

Inside the network, services communicate using Compose service names.

For example:

```text
postgres:5432
rabbitmq:5672
redis:6379
reservation:9091
user-service:8081
event-service:8081
```

Host ports are mapped separately for local development.

## 7. Persistence and infrastructure

PostgreSQL uses the Docker volume:

```text
postgres_data
```

RabbitMQ uses:

```text
rabbitmq_data
```

The database initialization script is mounted read-only into PostgreSQL.

The Notification Service receives its Gmail application password through a Docker Compose secret instead of placing the secret directly into the environment variables.

## 8. Observability

The .NET services use OpenTelemetry for:

- traces
- metrics
- logs

Telemetry is sent to the OpenTelemetry Collector.

The deployment then routes telemetry to:

```text
Tempo       -> traces
Loki        -> logs
Prometheus  -> metrics
Grafana     -> visualization
```

The relevant configuration is under:

```text
deployment/data/
```

## 9. Source-of-truth rule

This documentation intentionally describes the system from the current repository state.

When documentation and implementation differ, the source code, protobuf contracts, database initialization SQL, and deployment configuration should be treated as authoritative.

In particular:

- REST endpoints should be derived from controllers.
- gRPC APIs should be derived from `.proto` contracts and implementations.
- Database structure should be derived from `deployment/data/init.sql`.
- Runtime topology should be derived from `deployment/compose.yaml`.
- Configuration values should be derived from the service configuration and Compose environment.

This prevents the documentation from describing architecture that is not actually implemented.
