# User Service — API Reference (for Frontend)

Base path for all REST endpoints: **`/api`**
Content type: **`application/json`**
Auth: **`Authorization: Bearer <JWT>`** header on every endpoint (all controllers require auth — see [Auth & Roles](#auth--roles))

> This service also exposes a gRPC API (`UserGrpcService`), but that's consumed by other backend services (e.g. Auth service), not the frontend. This doc only covers the REST endpoints under `Users.API/Controllers`.

---

## Auth & Roles

Every endpoint requires a valid Bearer JWT. Tokens are RSA-signed and validated against `Jwt:Issuer` / `Jwt:Audience` config.

The JWT must contain:
- `sub` claim → user ID (numeric, parsed as `long`)
- `roles` claim → one or more of `USER`, `SUPPORT`, `ADMIN`

| Policy | Required role(s) | Used on |
|---|---|---|
| `RequireUser` | `USER` | all `/api/user/*` endpoints |
| `RequireAdmin` | `ADMIN` | all `/api/admin/*` endpoints |

If the token is missing/invalid → `401`. If the role doesn't match the policy → `403`.

---

## Error format

All errors come back as `application/problem+json`:

```json
{
  "status": 400,
  "title": "Validation failed",
  "instance": "/api/user/profile",
  "errors": ["Email is not a valid email address."]
}
```

Status codes you'll see:

| Status | Meaning |
|---|---|
| 400 | Validation failed / bad domain rule (e.g. malformed input) |
| 401 | Not authenticated / missing-invalid token |
| 403 | Authenticated but wrong role |
| 404 | Resource not found |
| 409 | Business rule conflict (e.g. duplicate email) |
| 503 | DB / infra unavailable |
| 500 | Unexpected error |

Note: plain validation errors (`ValidationException`) come with an `errors` array as shown above. Other exception types (404/409/etc.) come back with just `title` + `detail`, no `errors` array.

---

## `/api/user` — requires `USER` role

### `GET /api/user/profile`
Returns the logged-in user's profile.

**Response `200`:**
```json
{
  "firstName": "Ali",
  "lastName": "Rezaei",
  "email": "ali@example.com",
  "phoneNumber": "09123456789",
  "city": "Tehran"
}
```
`phoneNumber` and `city` can be `null`.

---

### `PUT /api/user/profile`
Updates the logged-in user's profile. User ID comes from the JWT, not the body.

**Request body:**
```json
{
  "firstName": "Ali",
  "lastName": "Rezaei",
  "email": "ali@example.com",
  "phoneNumber": "09123456789",
  "city": "Tehran"
}
```

| Field | Required? | Rules |
|---|---|---|
| `firstName` | ✅ | not empty, 3–60 chars, must differ from `lastName` |
| `lastName` | ✅ | not empty, 3–60 chars, must differ from `firstName` |
| `email` | ✅ | valid email format |
| `phoneNumber` | ❌ optional (send `null` or omit) | **if provided**: exactly 11 chars, must start with `"09"`, digits only |
| `city` | ❌ optional (send `null` or omit) | **if provided**: must not be an empty string. Must match an existing city name in the DB, or you'll get a `404 "The city not found"` |

**Response:** `200` empty body on success.

⚠️ Note: `phoneNumber` and `city` validation only kicks in when the field is **not null**. Sending `""` (empty string) for city, however, will fail validation (`NotEmpty`). Best practice: omit the field or send `null`, don't send `""`.

---

### `GET /api/user/report/{reportId}`
Get details of a specific report. Only works if the report belongs to the logged-in user (otherwise the backend throws — currently returns as an unhandled exception path, treat as an error).

**Response `200`:**
```json
{
  "reportId": 12,
  "userId": 5,
  "type": "TECHNICAL_BUG",
  "reportedAt": "2026-08-01T10:00:00+00:00",
  "request": "The app crashes on login",
  "response": null,
  "respondedAt": null,
  "status": "OPEN"
}
```

`response` / `respondedAt` are `null` until an admin answers the report.

---

### `GET /api/user/report`
Lists all reports created by the logged-in user (summary view, not full detail).

**Response `200`:**
```json
[
  { "reportId": 12, "status": "OPEN", "reportedAt": "2026-08-01T10:00:00+00:00" },
  { "reportId": 9,  "status": "CLOSED", "reportedAt": "2026-07-20T09:30:00+00:00" }
]
```

---

### `POST /api/user/report`
Creates a new support report/ticket for the logged-in user.

**Request body:**
```json
{
  "requestConent": "The app crashes on login",
  "type": "TECHNICAL_BUG"
}
```

> ⚠️ Field name is literally `requestConent` (typo in backend, not `requestContent`). Send it exactly as-is or you'll get a null/empty value.

| Field | Required? | Rules |
|---|---|---|
| `requestConent` | ✅ | 10–500 chars |
| `type` | ✅ | must exactly match one of the `ReportType` enum names (case-sensitive string match): `PAYMENT_ISSUE`, `RESERVATION_ISSUE`, `CANCEL_RESERVATION`, `TECHNICAL_BUG`, `COMPLAINT`, `OTHER` |

**Response `200`:** the new report's ID (plain integer), e.g. `14`.

---

### `GET /api/user/cities`
Search cities (for populating a city picker/autocomplete, e.g. in the profile form).

**Query params:**

| Param | Required? | Default | Notes |
|---|---|---|---|
| `searchTerm` | ❌ | none | partial match (case-insensitive `ILIKE`); if omitted, returns cities unfiltered |
| `limit` | ❌ | `20` | capped server-side at `40` |
| `offset` | ❌ | `0` | negative values reset to `0` |

Example: `GET /api/user/cities?searchTerm=teh&limit=10`

**Response `200`:**
```json
[
  { "cityId": 1, "name": "Tehran" },
  { "cityId": 4, "name": "Tabriz" }
]
```

---

## `/api/admin` — requires `ADMIN` role

### `GET /api/admin/report`
Lists all **open** reports across all users (for admin dashboard/queue).

**Query params:**

| Param | Required? | Default | Notes |
|---|---|---|---|
| `limit` | ❌ | `20` | capped at `40` |
| `offset` | ❌ | `0` | negative → `0` |

**Response `200`:** same shape as `GET /api/user/report` (array of `{reportId, status, reportedAt}`), but across all users.

---

### `GET /api/admin/report/{reportId}`
Get full details of any open report (by ID, path param).

**Response `200`:** same shape as `GET /api/user/report/{reportId}`.

---

### `PUT /api/admin/report/{reportId}`
Admin answers/closes a report.

**Request body:**
```json
{
  "response": "This has been fixed in the latest release."
}
```

| Field | Required? | Rules |
|---|---|---|
| `response` | ✅ | not empty, max 500 chars |

**Response `200`:** empty body. Report status flips to `CLOSED` and `respondedAt` is set.

⚠️ Will `400` if the report isn't currently `OPEN` (can't answer an already-closed report).

---

### `GET /api/admin/users`
List/search users (for admin user-management screen). **All filters are optional.**

**Query params** (all optional, sent as query string):

| Param | Type | Notes |
|---|---|---|
| `firstName` | string | partial match |
| `lastName` | string | partial match |
| `email` | string | partial match |
| `status` | bool | filter by active/inactive |
| `limit` | int | default `20` (no server-side cap enforced here — unlike report/user endpoints above) |
| `offset` | int | default `0` |

Example: `GET /api/admin/users?firstName=ali&status=true&limit=10`

**Response `200`:**
```json
[
  { "userId": 5, "firstName": "Ali", "lastName": "Rezaei", "email": "ali@example.com", "isActive": true }
]
```

---

### `PUT /api/admin/users/{userId}`
Activate or deactivate a user account.

**Query param:**

| Param | Required? | Notes |
|---|---|---|
| `active` | ✅ | `true` or `true`/`false` boolean query param |

Example: `PUT /api/admin/users/5?active=false`

**Response `200`:** empty body. `404` if user doesn't exist.

---

## Enums reference

**`ReportType`** (string values, used in `type` field):
`PAYMENT_ISSUE`, `RESERVATION_ISSUE`, `CANCEL_RESERVATION`, `TECHNICAL_BUG`, `COMPLAINT`, `OTHER`

**`ReportStatus`** (string values, returned in `status` field):
`OPEN`, `IN_PROGRESS`, `CLOSED`

All enums are serialized/deserialized as **strings** (JSON string enum converter is configured), not numbers.

---

## Quick gotchas for frontend integration

1. **`requestConent` is misspelled** in the create-report request — copy it exactly.
2. **Optional fields in `UserProfileDto`** (`phoneNumber`, `city`) should be sent as `null`/omitted rather than `""` — empty strings fail validation for `city` but are allowed to pass through for `phoneNumber` only if `null`.
3. **Phone number format**: exactly 11 digits, starting with `09` (Iranian mobile format), e.g. `09123456789`.
4. **`limit`/`offset` caps** differ across endpoints: reports and cities cap `limit` at 40 server-side; `GET /api/admin/users` does not enforce a cap.
5. All list/search GET endpoints use **query params**, not request bodies.
6. JWT `sub` claim carries the user ID — the frontend never needs to send a user ID in the body for "my profile" type endpoints; it's derived from the token.