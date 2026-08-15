# Event Service — API Reference (for Frontend)

Base path for all REST endpoints: **`/api/event`**
Content type: **`application/json`**
Auth: **`Authorization: Bearer <JWT>`** header — required on every endpoint (see [Auth & Roles](#auth--roles))

> This service also talks to a Reservation Service over gRPC internally (to check seat reservation status). That's backend-to-backend only, not exposed to the frontend.

---

## Auth & Roles

Same JWT setup as User Service — RSA-signed token, validated against `Jwt:Issuer` / `Jwt:Audience`, with a `roles` claim.

| Policy | Required role(s) | Used on |
|---|---|---|
| `RequireUser` | `USER` | browsing/read endpoints (matches, venues, leagues, seats, ticket configs) |
| `RequireAdmin` | `ADMIN` | creating matches & ticket configs |

`RequireSupportOrAdmin` policy is registered but **not currently used** on any endpoint in this controller.

If the token is missing/invalid → `401`. Wrong role → `403`.

---

## Error format

Same `application/problem+json` shape as User Service:

```json
{
  "status": 400,
  "title": "Validation failed",
  "instance": "/api/event/match",
  "errors": ["Match time must be in the future."]
}
```

| Status | Meaning |
|---|---|
| 400 | Validation failed / bad domain rule |
| 401 | Not authenticated |
| 403 | Wrong role |
| 404 | Resource not found (e.g. league/venue/team doesn't exist) |
| 409 | Business rule conflict (e.g. seat overlap, duplicate ticket config) |
| 503 | DB / infra unavailable |
| 500 | Unexpected error |

---

## `GET /api/event/match` — requires `USER` role

Lists all matches (unfiltered, paginated). Good for a general "browse all events" list.

**Query params:**

| Param | Required? | Default | Notes |
|---|---|---|---|
| `limit` | ❌ | `20` | `<= 0` resets to `20`; capped at `40` |
| `offset` | ❌ | `0` | negative → `0` |

**Response `200`:**
```json
[
  {
    "matchId": 101,
    "matchTime": "2026-09-10T18:00:00+00:00",
    "leagueId": 3,
    "leagueName": "Premier League",
    "sportId": 1,
    "sportName": "Football",
    "venueId": 7,
    "venueName": "Azadi Stadium",
    "venueCityId": 1,
    "venueCityName": "Tehran",
    "hostTeamId": 12,
    "hostTeamName": "Persepolis",
    "guestTeamId": 15,
    "guestTeamName": "Esteghlal"
  }
]
```

---

## `POST /api/event/match` — requires `USER` role

Search/filter matches. This is `POST` (not `GET`) because the filter object is sent as a JSON body, not query params — **don't be thrown off by the verb, it's a search/read operation, not a create.**

**Request body (all fields optional):**
```json
{
  "sportId": 1,
  "sportName": null,
  "leagueId": null,
  "leagueName": "Premier League",
  "cityId": null,
  "cityName": null,
  "teamId": null,
  "teamName": "Persepolis",
  "venueId": null,
  "venueName": null,
  "fromDate": "2026-08-01T00:00:00Z",
  "toDate": "2026-12-31T00:00:00Z",
  "limit": 20,
  "offset": 0
}
```

| Field | Required? | Rules |
|---|---|---|
| `sportId` | ❌ | if present, must be `> 0` |
| `sportName` | ❌ | if present, not empty (partial match, case-insensitive) |
| `leagueId` | ❌ | if present, must be `> 0` |
| `leagueName` | ❌ | if present, not empty (partial match) |
| `cityId` | ❌ | if present, must be `> 0` |
| `cityName` | ❌ | if present, not empty (partial match) |
| `teamId` | ❌ | if present, must be `> 0` — matches either host **or** guest team |
| `teamName` | ❌ | if present, not empty (partial match) — matches either host **or** guest team name |
| `venueId` | ❌ | if present, must be `> 0` |
| `venueName` | ❌ | if present, not empty (partial match) |
| `fromDate` / `toDate` | ❌ | if both present, `fromDate` must be `<= toDate` |
| `limit` | ❌ | default `20`, capped at `40` server-side (controller clamps it) |
| `offset` | ❌ | default `0`, negative → `0` |

⚠️ **Important nuance**: for each entity (sport, league, city, team, venue) the query handler only applies **one** filter per entity — if you send both `sportId` **and** `sportName`, only `sportId` is used (the SQL uses `if/else if`). Don't send both; pick either the ID or the name filter.

**Response `200`:** same `MatchDto` array shape as `GET /api/event/match` above.

---

## `GET /api/event/match/{matchId}/configs` — requires `USER` role

Gets the ticket configs (ticket types/categories with pricing) available for a specific match.

**Route param:** `matchId` (int, path)

**Response `200`:**
```json
[
  {
    "configId": 5,
    "matchId": 101,
    "categoryId": 2,
    "categoryName": "VIP",
    "price": 1500000,
    "totalSeats": 200,
    "amenities": "{\"parking\": true}"
  }
]
```
`amenities` is a raw JSON string (or `null`) — parse it client-side if you need structured data out of it.

---

## `GET /api/event/match/configs/seats` — requires `USER` role

Gets seats for one or more ticket configs, with reservation status merged in (calls the Reservation Service internally).

⚠️ **Binding gotcha**: this action has **no `[FromQuery]` attribute** on its parameter, unlike the other GET endpoints in this controller. In ASP.NET Core, a complex type on a `GET` without an explicit binding source defaults to **query string binding** anyway, so in practice you should still call it like:

```
GET /api/event/match/configs/seats?ConfigIds=5&ConfigIds=8
```

Repeat the `ConfigIds` param once per value. Test this one specifically once your backend dev confirms — this is the kind of parameter-binding edge case that's easy to get subtly wrong, and it's the same shape of issue you flagged with the Postman quirk on the User Service.

| Param | Required? | Rules |
|---|---|---|
| `ConfigIds` | ✅ | at least one value; can't be empty |

**Response `200`:**
```json
[
  {
    "seatId": 1001,
    "configId": 5,
    "section": 1,
    "rowNo": 1,
    "seatNo": 1,
    "isReserved": false
  }
]
```

---

## `GET /api/event/venues` — requires `USER` role

Lists venues (paginated).

**Query params:**

| Param | Required? | Default |
|---|---|---|
| `limit` | ❌ | `20`, capped at `40` |
| `offset` | ❌ | `0` |

**Response `200`:**
```json
[
  {
    "venueId": 7,
    "venueName": "Azadi Stadium",
    "cityId": 1,
    "cityName": "Tehran",
    "street": "Azadi Ave",
    "postalCode": "1234567890",
    "latitude": 35.7219,
    "longitude": 51.3347
  }
]
```

---

## `GET /api/event/leagues` — requires `USER` role

Lists leagues (paginated).

**Query params:**

| Param | Required? | Default |
|---|---|---|
| `limit` | ❌ | `20`, capped at `40` |
| `offset` | ❌ | `0` |

**Response `200`:**
```json
[
  { "leagueId": 3, "leagueName": "Premier League", "sportId": 1, "sportName": "Football" }
]
```

---

## `POST /api/event/new/match` — requires `ADMIN` role

Creates a new match.

**Request body:**
```json
{
  "leagueId": 3,
  "venueId": 7,
  "matchTime": "2026-09-10T18:00:00Z",
  "hostTeamId": 12,
  "guestTeamId": 15
}
```

| Field | Required? | Rules |
|---|---|---|
| `leagueId` | ✅ | `> 0` |
| `venueId` | ✅ | `> 0` |
| `hostTeamId` | ✅ | `> 0`, must differ from `guestTeamId` |
| `guestTeamId` | ✅ | `> 0`, must differ from `hostTeamId` |
| `matchTime` | ✅ | must be in the future (UTC compared against server time) |

**Response `200`:** the new match ID (plain integer).

**Possible errors:**
- `404` — league doesn't exist
- `404` — venue, host team, or guest team don't exist (from a DB foreign-key check)
- `409` — DB check constraint catches host == guest team at the DB layer too (belt-and-suspenders with the FluentValidation rule above)

---

## `POST /api/event/new/match/ticket` — requires `ADMIN` role

Creates a new ticket config (a ticket category/price tier + seat layout) for a match. This also generates the actual seat rows in one transaction.

**Request body:**
```json
{
  "matchId": 101,
  "categoryId": 2,
  "price": 1500000,
  "amenities": "{\"parking\": true, \"food\": false}",
  "seatBlocks": [
    { "section": 1, "rowStart": 1, "rowCount": 10, "seatsPerRow": 20 },
    { "section": 2, "rowStart": 1, "rowCount": 5, "seatsPerRow": 15 }
  ]
}
```

| Field | Required? | Rules |
|---|---|---|
| `matchId` | ✅ | `> 0` |
| `categoryId` | ✅ | `> 0` |
| `price` | ✅ | `>= 0` |
| `amenities` | ❌ optional | if provided, must be **valid JSON** (sent as a string, not a JSON object — see note below) |
| `seatBlocks` | ✅ | at least 1 block |
| `seatBlocks[].section` | ✅ | `> 0` |
| `seatBlocks[].rowStart` | ✅ | `> 0` (defaults to `1` if omitted client-side, but still validated as `> 0`) |
| `seatBlocks[].rowCount` | ✅ | `> 0` |
| `seatBlocks[].seatsPerRow` | ✅ | `> 0` |

⚠️ **`amenities` is a string field, not a nested object.** You must `JSON.stringify()` it before sending:
```json
"amenities": "{\"parking\": true}"
```
NOT:
```json
"amenities": { "parking": true }
```
(The latter would fail model binding or fail the "valid JSON" check depending on how it's serialized — stick to a stringified JSON blob to match the backend's expectation.)

⚠️ **Seat block overlap checks happen in two places:**
1. Client-side/request-level: blocks *within the same request* can't overlap in row ranges within the same `section` (checked before hitting the DB).
2. Server-side/DB-level: the new blocks also can't overlap with seats from **other existing ticket configs for the same match** — this comes back as a `409` if triggered.

**`TotalSeats`** is computed automatically server-side as `sum(rowCount * seatsPerRow)` across all blocks — don't send it, it's not part of the request DTO.

**Response `200`:** the new ticket config ID (plain integer).

**Possible errors:**
- `400` — validation failures (bad IDs, empty seat blocks, invalid amenities JSON, internal overlap)
- `404` — match or ticket category doesn't exist
- `409` — duplicate ticket config for this match+category, or seat overlap with an existing config

---

## Enums / reference data

This service doesn't expose its own enums to the frontend directly — `sport`, `league`, `city`, `team`, `venue`, and `ticket category` are all DB-backed lookup tables. Use:
- `GET /api/event/leagues` and `GET /api/event/venues` to populate pickers
- There's currently **no dedicated endpoint for sports, teams, or ticket categories** — if your frontend needs those as standalone dropdowns, flag it to the backend dev; right now they're only reachable indirectly through match/venue results.

---

## Quick gotchas for frontend integration

1. **`POST /api/event/match` is a search, not a create** — it's `POST` only because it needs a request body for the filter object.
2. **Don't send both an ID and a name filter for the same entity** in the match search (e.g. `sportId` + `sportName`) — only the ID takes effect.
3. **`amenities` must be a JSON-encoded string**, not a nested object, in ticket config creation.
4. **`GET /api/event/match/configs/seats`** binds `ConfigIds` from the query string despite looking like it might expect a body — repeat the query param per value (`?ConfigIds=1&ConfigIds=2`). Verify this against a real request once, this is a common gotcha spot.
5. **`limit`/`offset` clamps** are consistent across `match`, `venues`, `leagues` (`<=0` → 20 default, `>40` → 40 cap) but this controller does it slightly differently than User Service (it resets non-positive `limit` to the default rather than leaving `0`/negative as-is) — worth knowing if you're replicating pagination UI logic across both services.
6. All timestamps are ISO 8601 with offset (`DateTimeOffset`) — send timezone-aware datetimes.