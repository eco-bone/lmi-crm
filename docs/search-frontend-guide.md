# Search APIs — Frontend Integration Guide

This document covers the three search endpoints in the LMI CRM backend. It is intended to give a frontend developer full context to build search UI without needing to read the backend source code.

---

## Overview

There are three search endpoints — one each for prospects, users, and groups. They are separate from the standard list endpoints and are designed for keyword-based lookup across multiple fields simultaneously.

**Key differences from list endpoints:**

| | List (`GET /api/prospects`) | Search (`GET /api/prospects/search`) |
|---|---|---|
| Purpose | Browse records with category filters | Find records by typing a keyword |
| Filtering | By `type`, `licenseeId`, `associateId` | By keyword across multiple text fields |
| Scope | Role-scoped by default | Configurable via `scope` param |

All three search endpoints are available to **every authenticated role** (associate, licensee, admin).

---

## Authentication

Every request must include a valid JWT:

```
Authorization: Bearer <token>
```

If the token is missing or expired, the server returns `403 Forbidden` before any response envelope is produced.

---

## Standard Response Envelope

All responses are wrapped in:

```json
{
  "success": true,
  "message": "Search results retrieved successfully",
  "data": { ... }
}
```

On error, `success` is `false`, `data` is `null`, and `message` explains what went wrong.

---

## The `scope` Parameter

Every search endpoint accepts a `scope` query param that controls how wide the search is:

| Value | Behaviour |
|---|---|
| `own` (default) | Search only within records the user owns or manages |
| `all` | Search across all records in the system |

**What "own" means per role:**

| Entity | Role | `scope=own` returns |
|---|---|---|
| Prospects | Associate | Only their own prospects |
| Prospects | Licensee | All prospects linked to them |
| Prospects | Admin | Same as `all` |
| Users | Associate | All users under the same licensee org (teammates + parent licensee) |
| Users | Licensee | Their own associates + themselves |
| Users | Admin | Same as `all` |
| Groups | Associate | Groups owned by their parent licensee |
| Groups | Licensee | Only their own groups |
| Groups | Admin | Same as `all` |

Admins always see everything regardless of the `scope` value.

---

## Common Query Parameters

These apply to all three endpoints:

| Param | Type | Required | Default | Description |
|---|---|---|---|---|
| `q` | string | yes | — | Keyword to search. Case-insensitive substring match. |
| `scope` | string | no | `own` | `own` or `all` |
| `page` | integer | no | `0` | Zero-based page index |
| `limit` | integer | no | `10` | Number of results per page |

---

## Enums Reference

### ProspectType
```
PROSPECT   — Still being pursued
CLIENT     — Converted to a client
```

### ProspectStatus
```
PROTECTED    — Actively protected
UNPROTECTED  — Protection has lapsed or been removed
PROVISIONAL  — Flagged, pending admin review
```

### ProspectProgramType
```
LI          — Large In-house (12 month protection)
SI          — Small In-house (6 month protection)
ONE_TO_ONE  — Perpetual protection
SHC         — Showcase, perpetual protection
```

### ClassificationType
```
AAA | AA | A | B | C
```

### UserRole
```
ASSOCIATE       — Sales associate under a licensee
LICENSEE        — Licensee who manages associates
MASTER_LICENSEE — Top-level licensee (MLO)
ADMIN           — Internal admin
SUPER_ADMIN     — Full system access
```

### UserStatus
```
ACTIVE   — Can log in and use the system
INACTIVE — Deactivated, cannot log in
PENDING  — Invited but has not set up their account yet
```

### GroupProgramType
```
EPP | ELD | EPL | ECE | EML | ESL | LFW | AIE
```

### DeliveryType
```
ONLINE | HYBRID | IN_PERSON
```

---

## Endpoints

---

### 1. Search Prospects

```
GET /api/prospects/search
```

**Searched fields:** `companyName`, `contactFirstName`, `contactLastName`, `city`, `email`, `phone`

#### Request

```
GET /api/prospects/search?q=acme&scope=all&page=0&limit=10
Authorization: Bearer <token>
```

#### Response

```json
{
  "success": true,
  "message": "Search results retrieved successfully",
  "data": {
    "overallTotal": 4,
    "prospectCount": 3,
    "clientCount": 1,
    "provisionalCount": 1,
    "unprotectedCount": 0,
    "prospects": {
      "content": [
        {
          "id": 17,
          "companyName": "Acme Corp",
          "city": "Mumbai",
          "contactFirstName": "Rajesh",
          "contactLastName": "Sharma",
          "designation": "HR Manager",
          "type": "PROSPECT",
          "status": "PROTECTED",

          // Fields below are only present when the requester is ADMIN or SUPER_ADMIN
          "email": "rajesh@acme.com",
          "phone": "9876543210",
          "referredBy": "John Doe",
          "classificationType": "A",
          "programType": "LI",
          "protectionPeriodMonths": 12,
          "entryDate": "2026-01-15",
          "firstMeetingDate": "2026-01-28",
          "lastMeetingDate": "2026-03-10",
          "associateId": 5,
          "licenseeId": 2,
          "createdBy": 5,
          "createdAt": "2026-01-15T09:22:00",
          "provisionReason": null
        }
      ],
      "totalElements": 4,
      "totalPages": 1,
      "number": 0,
      "size": 10,
      "first": true,
      "last": true
    }
  }
}
```

#### What non-admins receive

Associates and licensees get a **limited** prospect object. Only these fields are populated:

| Field | Always present |
|---|---|
| `id` | yes |
| `companyName` | yes |
| `city` | yes |
| `contactFirstName` | yes |
| `contactLastName` | yes |
| `designation` | yes |
| `type` | yes |
| `status` | yes |
| All other fields | `null` |

This applies regardless of whether `scope=own` or `scope=all`. Non-admins never receive contact details, protection info, or ownership info for any prospect via search.

---

### 2. Search Users

```
GET /api/users/search
```

**Searched fields:** `firstName`, `lastName`, `email`, `phone`

#### Request

```
GET /api/users/search?q=john&scope=own&page=0&limit=10
Authorization: Bearer <token>
```

#### Response

```json
{
  "success": true,
  "message": "Search results retrieved successfully",
  "data": {
    "overallTotal": 3,
    "activeCount": 2,
    "inactiveCount": 1,
    "countByRole": {
      "LICENSEE": 1,
      "ASSOCIATE": 2
    },
    "users": {
      "content": [
        {
          "id": 5,
          "firstName": "John",
          "lastName": "Mathew",
          "email": "john.mathew@lmi.com",
          "phone": "9123456789",
          "role": "ASSOCIATE",
          "licenseeId": 2,
          "status": "ACTIVE",
          "createdAt": "2025-11-10T08:00:00",
          "cities": null
        },
        {
          "id": 2,
          "firstName": "John",
          "lastName": "Baker",
          "email": "john.baker@lmi.com",
          "phone": "9000000001",
          "role": "LICENSEE",
          "licenseeId": 2,
          "status": "ACTIVE",
          "createdAt": "2025-09-01T10:00:00",
          "cities": [
            { "id": 1, "city": "Mumbai", "isPrimary": true },
            { "id": 2, "city": "Pune", "isPrimary": false }
          ]
        }
      ],
      "totalElements": 3,
      "totalPages": 1,
      "number": 0,
      "size": 10,
      "first": true,
      "last": true
    }
  }
}
```

#### Notes on `cities`

- `cities` is only populated for users with `role = LICENSEE`. It is `null` for associates and admins.
- Each city entry has: `id` (integer), `city` (string), `isPrimary` (boolean).

---

### 3. Search Groups

```
GET /api/groups/search
```

**Searched fields:** `groupType` (enum name), `deliveryType` (enum name), licensee full name, facilitator full name, linked client `companyName`

For example, searching `"LI"` matches groups with `groupType = LI`. Searching `"Smith"` matches groups whose licensee or facilitator is named Smith. Searching `"Tata"` matches groups that have a linked client called Tata.

#### Request

```
GET /api/groups/search?q=LI&scope=all&page=0&limit=10
Authorization: Bearer <token>
```

#### Response

```json
{
  "success": true,
  "message": "Search results retrieved successfully",
  "data": {
    "overallTotal": 2,
    "activeCount": 2,
    "groups": {
      "content": [
        {
          "id": 3,
          "licenseeId": 2,
          "licenseeName": "John Baker",
          "facilitatorId": 5,
          "facilitatorName": "John Mathew",
          "groupSize": 20,
          "groupType": "EPP",
          "deliveryType": "IN_PERSON",
          "startDate": "2026-03-01",
          "ppmTfeDateSent": "2026-02-15",
          "createdBy": 2,
          "createdAt": "2026-02-10T11:00:00",
          "prospects": [
            {
              "id": 17,
              "companyName": "Acme Corp",
              "city": "Mumbai",
              "contactFirstName": "Rajesh",
              "contactLastName": "Sharma",
              "designation": "HR Manager",
              "type": "CLIENT",
              "status": "PROTECTED",
              "email": "rajesh@acme.com",
              "phone": "9876543210",
              "referredBy": null,
              "classificationType": "A",
              "programType": "LI",
              "protectionPeriodMonths": 12,
              "entryDate": "2026-01-15",
              "firstMeetingDate": "2026-01-28",
              "lastMeetingDate": "2026-03-10",
              "associateId": 5,
              "licenseeId": 2,
              "createdBy": 5,
              "createdAt": "2026-01-15T09:22:00",
              "provisionReason": null
            }
          ]
        }
      ],
      "totalElements": 2,
      "totalPages": 1,
      "number": 0,
      "size": 10,
      "first": true,
      "last": true
    }
  }
}
```

#### Notes on `prospects` inside groups

- `prospects` is the list of clients linked to the group. Groups can only be linked to records with `type = CLIENT`, never `PROSPECT`.
- The prospects inside group responses always use the **full** response shape (all fields populated), regardless of the requesting user's role.

---

## TypeScript Types

Paste these into your project for type safety:

```typescript
// Common pagination wrapper (Spring Page object)
interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;       // current page (zero-based)
  size: number;         // page size
  first: boolean;
  last: boolean;
}

// Enums
type ProspectType = 'PROSPECT' | 'CLIENT';
type ProspectStatus = 'PROTECTED' | 'UNPROTECTED' | 'PROVISIONAL';
type ProspectProgramType = 'LI' | 'SI' | 'ONE_TO_ONE' | 'SHC';
type ClassificationType = 'AAA' | 'AA' | 'A' | 'B' | 'C';
type UserRole = 'ASSOCIATE' | 'LICENSEE' | 'MASTER_LICENSEE' | 'ADMIN' | 'SUPER_ADMIN';
type UserStatus = 'ACTIVE' | 'INACTIVE' | 'PENDING';
type GroupProgramType = 'EPP' | 'ELD' | 'EPL' | 'ECE' | 'EML' | 'ESL' | 'LFW' | 'AIE';
type DeliveryType = 'ONLINE' | 'HYBRID' | 'IN_PERSON';

// Prospect — full shape (admin only)
interface ProspectResponse {
  id: number;
  companyName: string;
  city: string;
  contactFirstName: string;
  contactLastName: string;
  designation: string | null;
  type: ProspectType;
  status: ProspectStatus;
  // Fields below are null for non-admins
  email: string | null;
  phone: string | null;
  referredBy: string | null;
  classificationType: ClassificationType | null;
  programType: ProspectProgramType | null;
  protectionPeriodMonths: number | null;
  entryDate: string | null;        // ISO date: "2026-01-15"
  firstMeetingDate: string | null;
  lastMeetingDate: string | null;
  associateId: number | null;
  licenseeId: number | null;
  createdBy: number | null;
  createdAt: string | null;        // ISO datetime: "2026-01-15T09:22:00"
  provisionReason: string | null;
}

// City entry (inside licensee user objects)
interface CityInfo {
  id: number;
  city: string;
  isPrimary: boolean;
}

// User
interface UserResponse {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  role: UserRole;
  licenseeId: number | null;
  status: UserStatus;
  createdAt: string;               // ISO datetime
  cities: CityInfo[] | null;       // only populated for LICENSEE role
}

// Group
interface GroupResponse {
  id: number;
  licenseeId: number;
  licenseeName: string | null;
  facilitatorId: number | null;
  facilitatorName: string | null;
  groupSize: number | null;
  groupType: GroupProgramType;
  deliveryType: DeliveryType;
  startDate: string | null;        // ISO date
  ppmTfeDateSent: string | null;   // ISO date
  createdBy: number;
  createdAt: string;               // ISO datetime
  prospects: ProspectResponse[];   // linked clients (always full shape)
}

// Search response wrappers
interface ProspectsSearchResponse {
  overallTotal: number;
  prospectCount: number;
  clientCount: number;
  provisionalCount: number;
  unprotectedCount: number;
  prospects: Page<ProspectResponse>;
}

interface UsersSearchResponse {
  overallTotal: number;
  activeCount: number;
  inactiveCount: number;
  countByRole: Partial<Record<UserRole, number>>;
  users: Page<UserResponse>;
}

interface GroupsSearchResponse {
  overallTotal: number;
  activeCount: number;
  groups: Page<GroupResponse>;
}

// API envelope
interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T | null;
}
```

---

## Example Fetch Calls

```typescript
const BASE_URL = 'http://localhost:8080/api';

async function searchProspects(token: string, q: string, scope: 'own' | 'all' = 'own', page = 0, limit = 10) {
  const params = new URLSearchParams({ q, scope, page: String(page), limit: String(limit) });
  const res = await fetch(`${BASE_URL}/prospects/search?${params}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  const json: ApiResponse<ProspectsSearchResponse> = await res.json();
  if (!json.success) throw new Error(json.message);
  return json.data!;
}

async function searchUsers(token: string, q: string, scope: 'own' | 'all' = 'own', page = 0, limit = 10) {
  const params = new URLSearchParams({ q, scope, page: String(page), limit: String(limit) });
  const res = await fetch(`${BASE_URL}/users/search?${params}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  const json: ApiResponse<UsersSearchResponse> = await res.json();
  if (!json.success) throw new Error(json.message);
  return json.data!;
}

async function searchGroups(token: string, q: string, scope: 'own' | 'all' = 'own', page = 0, limit = 10) {
  const params = new URLSearchParams({ q, scope, page: String(page), limit: String(limit) });
  const res = await fetch(`${BASE_URL}/groups/search?${params}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  const json: ApiResponse<GroupsSearchResponse> = await res.json();
  if (!json.success) throw new Error(json.message);
  return json.data!;
}
```

---

## UI Recommendations

### Search input behaviour

- Trigger the API call after the user has typed at least **2 characters** (matches the duplicate-check convention used elsewhere in the app).
- Debounce the input by **300–500ms** to avoid a request on every keystroke.

### Scope toggle

Provide a toggle or dropdown near the search bar with two options:
- **"Within my organisation"** → `scope=own`
- **"Search everyone"** → `scope=all`

Default to `scope=own`. The distinction is most meaningful for licensees (own = their associates, all = every user in the system).

### Rendering prospect results for non-admins

Since `email`, `phone`, and ownership fields are `null` for non-admins, do not render those columns/fields in the search results table for associate and licensee users. Show only: company name, city, contact name, designation, type, and status.

### Pagination

Use `data.prospects.totalPages` (or `users` / `groups` equivalent) to render pagination controls. Increment `page` and re-call the endpoint — the `q` and `scope` params must be included on every page request.

### Empty state

When `overallTotal === 0`, show a message like `"No results found for '<query>'"`. If `scope=own` returned nothing, suggest switching to `scope=all`.

### Error handling

| Scenario | Likely cause | What to show |
|---|---|---|
| `success: false`, message `"User not found"` | Invalid/expired token | Redirect to login |
| `success: false`, message `"Access denied"` | Role not permitted | Generic error toast |
| HTTP `403` | Token missing or expired | Redirect to login |
| `q` param missing | API call made without a query | Client-side guard — never send empty `q` |
