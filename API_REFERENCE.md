# LMI CRM — API Reference

All endpoints are prefixed with `/api`. Roles listed are the minimum required to call the endpoint.

---

## Users

| Method | Endpoint | Description | Roles |
|--------|----------|-------------|-------|
| `POST` | `/api/admin/create` | Create a new Admin user | Super Admin |
| `POST` | `/api/admin/licensees` | Add a new Licensee and send invite email | Admin, Super Admin |
| `GET` | `/api/users` | List users with optional `role` and `status` filters | Admin, Super Admin, Licensee |
| `GET` | `/api/users/{id}` | Get a specific user's details | Authenticated |
| `PUT` | `/api/users/{id}` | Update a user's profile fields | Authenticated |
| `PUT` | `/api/users/{id}/password` | Reset a user's password | Authenticated |
| `PUT` | `/api/users/{id}/deactivate` | Deactivate a user (triggers prospect transfer cascade) | Admin, Super Admin |

---

## Associates

| Method | Endpoint | Description | Roles |
|--------|----------|-------------|-------|
| `POST` | `/api/licensees/associates/request` | Licensee submits a request to create an Associate | Licensee |
| `PUT` | `/api/admin/associates/{alertId}/decision` | Approve or reject an Associate creation request (`?approve=true/false`) | Admin, Super Admin |
| `POST` | `/api/users/associates/{id}/deactivation-request` | Licensee submits a deactivation request for one of their Associates | Licensee |
| `PUT` | `/api/users/associates/deactivation-requests/{alertId}` | Approve or reject an Associate deactivation request (`?approve=true/false`) | Admin, Super Admin |

---

## Prospects

| Method | Endpoint | Description | Roles |
|--------|----------|-------------|-------|
| `POST` | `/api/prospects` | Create a new prospect (auto-initiates protection; may be flagged PROVISIONAL) | Licensee, Associate |
| `GET` | `/api/prospects` | List prospects with optional `type`, `licenseeId`, `associateId`, `getAll` filters | Authenticated |
| `GET` | `/api/prospects/{id}` | Get a specific prospect's details | Authenticated |
| `PUT` | `/api/prospects/{id}` | Update a prospect — also handles protection extension, override, and licensee reassignment | Licensee, Admin, Super Admin |
| `DELETE` | `/api/prospects/{id}` | Soft delete a prospect (sets `deletionStatus=true`, `protectionStatus=UNPROTECTED`) | Admin, Super Admin |
| `POST` | `/api/prospects/{id}/convert` | Submit a Prospect → Client conversion request | Licensee, Associate |
| `PUT` | `/api/prospects/conversions/{alertId}` | Approve or reject a conversion request (`?approve=true/false`) | Admin, Super Admin |
| `POST` | `/api/prospects/{id}/extension-request` | Submit a protection extension request | Licensee, Associate |
| `PUT` | `/api/prospects/provisional/{alertId}` | Approve, reject, or ignore a provisional prospect (`?decision=APPROVE/REJECT/IGNORE`) | Admin, Super Admin |

---

## Groups

| Method | Endpoint | Description | Roles |
|--------|----------|-------------|-------|
| `POST` | `/api/groups` | Create a new group linked to a Client (prospect.type must be CLIENT) | Licensee, Associate |
| `GET` | `/api/groups` | List groups with optional `licenseeId` filter | Authenticated |
| `GET` | `/api/groups/{id}` | Get a specific group's details | Authenticated |
| `PUT` | `/api/groups/{id}` | Update a group's details | Licensee, Associate, Admin, Super Admin |
| `POST` | `/api/groups/{id}/deletion-request` | Licensee submits a group deletion request | Licensee |
| `DELETE` | `/api/groups/{id}` | Hard delete a group | Admin, Super Admin |
| `PUT` | `/api/groups/deletion-requests/{alertId}` | Approve or reject a group deletion request (`?approve=true/false`) | Admin, Super Admin |

---

## Alerts

| Method | Endpoint | Description | Roles |
|--------|----------|-------------|-------|
| `GET` | `/api/admin/alerts` | List all alerts paginated, with optional `type` and `status` filters | Admin, Super Admin |
| `GET` | `/api/admin/alerts/{id}` | Get a specific alert's details | Admin, Super Admin |

---

## Tasks

| Method | Endpoint | Description | Roles |
|--------|----------|-------------|-------|
| `POST` | `/api/tasks` | Create a new task with a future due date | Authenticated |
| `GET` | `/api/tasks` | List the current user's tasks with optional `status` filter, ordered by due date | Authenticated |
| `GET` | `/api/tasks/{id}` | Get a specific task's details | Authenticated |
| `PUT` | `/api/tasks/{id}` | Update a task's title, description, due date, or status | Authenticated |
| `DELETE` | `/api/tasks/{id}` | Permanently delete a task | Authenticated |

---

## Notes

| Method | Endpoint | Description | Roles |
|--------|----------|-------------|-------|
| `POST` | `/api/notes` | Create a new note | Authenticated |
| `GET` | `/api/notes` | List the current user's notes ordered by last updated | Authenticated |
| `GET` | `/api/notes/{id}` | Get a specific note's details | Authenticated |
| `PUT` | `/api/notes/{id}` | Update a note's title or description | Authenticated |
| `DELETE` | `/api/notes/{id}` | Permanently delete a note | Authenticated |
