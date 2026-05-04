# LMI CRM — API Reference

**Base URL:** `/api`

All endpoints are prefixed with `/api`. This document provides comprehensive API specifications for frontend developers, including request/response shapes, validation rules, and role-based access controls.

---

## Table of Contents

1. [Common Types](#common-types)
2. [Authentication & Authorization](#authentication--authorization)
3. [User Management](#user-management)
4. [Associate Management](#associate-management)
5. [Prospects](#prospects)
6. [Groups](#groups)
7. [Alerts](#alerts)
8. [Tasks](#tasks)
9. [Notes](#notes)
10. [Search](#search)

---

## Common Types

### ApiResponse<T>

All endpoints (except a few that return raw strings) wrap their responses in this standard format:

```typescript
{
  success: boolean;
  data: T | null;
  message: string;
}
```

**Example Success Response:**
```json
{
  "success": true,
  "data": { ... },
  "message": "Operation completed successfully"
}
```

**Example Error Response:**
```json
{
  "success": false,
  "data": null,
  "message": "Error description"
}
```

### Enums

**UserRole**
- `ADMIN`
- `LICENSEE`
- `ASSOCIATE`
- `MASTER_LICENSEE`
- `SUPER_ADMIN`

**UserStatus**
- `ACTIVE`
- `INACTIVE`
- `PENDING`

**ProspectType**
- `PROSPECT`
- `CLIENT`

**ProspectStatus**
- `PROTECTED`
- `UNPROTECTED`
- `PROVISIONAL`

**ProspectProgramType**
- `LI` (Large In-house)
- `SI` (Small In-house)
- `ONE_TO_ONE`
- `SHC` (Showcase)

**ClassificationType**
- `AAA`
- `AA`
- `A`
- `B`
- `C`

**GroupProgramType**
- `EPP`
- `ELD`
- `EPL`
- `ECE`
- `EML`
- `ESL`
- `LFW`
- `AIE`

**DeliveryType**
- `ONLINE`
- `HYBRID`
- `IN_PERSON`

**TaskStatus**
- `PENDING`
- `COMPLETED`

**AlertType**
- `DUPLICATE_PROSPECT`
- `PROSPECT_CONVERSION_REQUEST`
- `ASSOCIATE_CREATION_REQUEST`
- `ASSOCIATE_DEACTIVATION_REQUEST`
- `LICENSEE_DEACTIVATION_REQUEST`
- `PROSPECT_PROTECTION_WARNING`
- `PROSPECT_UNPROTECTED`
- `OWNERSHIP_CLAIM_REQUEST`
- `PROTECTION_EXTENSION_REQUEST`
- `GROUP_DELETION_REQUEST`
- `TASK_REMINDER`

**AlertStatus**
- `PENDING`
- `RESOLVED`
- `REJECTED`

**ProvisionalDecision**
- `APPROVE`
- `REJECT`
- `IGNORE`

**RelatedEntityType**
- `PROSPECT`
- `USER`
- `GROUP`

---

## Authentication & Authorization

### Role-Based Access Control

Each endpoint specifies minimum role requirements. The authenticated user's role is extracted from the JWT token.

**Role Hierarchy (lowest to highest):**
1. `ASSOCIATE` - Can only see their own data
2. `LICENSEE` - Can see data for themselves and their associates
3. `ADMIN` - Can see all data except admin management
4. `SUPER_ADMIN` - Full system access

---

## User Management

### 1. Create Admin

**Endpoint:** `POST /api/admin/create`
**Roles:** `SUPER_ADMIN`

Creates a new Admin user. Only Super Admins can create other Admins.

**Request Body:**
```typescript
{
  firstName: string;      // Required, not blank
  lastName: string;       // Required, not blank
  email: string;          // Required, valid email format
  phone: string;          // Required, not blank
}
```

**Response:** `UserResponse`
```typescript
{
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  role: "ADMIN";
  licenseeId: null;
  status: "ACTIVE" | "INACTIVE" | "PENDING";
  createdAt: string;      // ISO 8601 datetime
  cities: null;
}
```

---

### 2. Add Licensee

**Endpoint:** `POST /api/admin/licensees`
**Roles:** `ADMIN`, `SUPER_ADMIN`

Creates a new Licensee and sends an invite email.

**Request Body:**
```typescript
{
  firstName: string;      // Required, not blank
  lastName: string;       // Required, not blank
  email: string;          // Required, valid email format
  phone: string;          // Required, not blank
  cities: Array<{
    city: string;         // Required, not blank
    isPrimary: boolean;   // Required
  }>;                     // Required, at least one city
}
```

**Response:** `LicenseeResponse`
```typescript
{
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  role: "LICENSEE";
  status: "ACTIVE" | "INACTIVE" | "PENDING";
  cities: Array<{
    id: number;
    city: string;
    isPrimary: boolean;
  }>;
  createdAt: string;      // ISO 8601 datetime
}
```

---

### 3. List Users

**Endpoint:** `GET /api/users`
**Roles:** `ADMIN`, `SUPER_ADMIN`, `LICENSEE`

Retrieve a filtered list of users.

**Query Parameters:**
- `role` (optional): Filter by `UserRole`
- `status` (optional): Filter by `UserStatus`
- `includeAllStatuses` (optional, default: `false`): Include inactive/pending users

**Response:** `Array<UserResponse>`
```typescript
[
  {
    id: number;
    firstName: string;
    lastName: string;
    email: string;
    phone: string;
    role: "ADMIN" | "LICENSEE" | "ASSOCIATE" | "MASTER_LICENSEE" | "SUPER_ADMIN";
    licenseeId: number | null;
    status: "ACTIVE" | "INACTIVE" | "PENDING";
    createdAt: string;
    cities: Array<{
      id: number;
      city: string;
      isPrimary: boolean;
    }> | null;
  }
]
```

---

### 4. Get User Detail

**Endpoint:** `GET /api/users/{id}`
**Roles:** `Authenticated`

Retrieve details for a specific user.

**Path Parameters:**
- `id`: User ID

**Response:** `UserResponse`
```typescript
{
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  role: "ADMIN" | "LICENSEE" | "ASSOCIATE" | "MASTER_LICENSEE" | "SUPER_ADMIN";
  licenseeId: number | null;
  status: "ACTIVE" | "INACTIVE" | "PENDING";
  createdAt: string;
  cities: Array<{
    id: number;
    city: string;
    isPrimary: boolean;
  }> | null;
}
```

---

### 5. Update User

**Endpoint:** `PUT /api/users/{id}`
**Roles:** `Authenticated`

Update a user's profile. Users can update their own profile. Admins can update any user.

**Path Parameters:**
- `id`: User ID

**Request Body:**
```typescript
{
  firstName?: string;         // Optional, min length 1
  lastName?: string;          // Optional, min length 1
  email?: string;             // Optional, min length 1
  phone?: string;             // Optional, min length 1
  status?: "ACTIVE" | "INACTIVE" | "PENDING";
  role?: "ADMIN" | "LICENSEE" | "ASSOCIATE" | "MASTER_LICENSEE" | "SUPER_ADMIN";
  newLicenseeId?: number;     // Optional, admin-only
  newPrimaryCity?: string;    // Optional, admin-only
  cities?: Array<{
    city: string;             // Required
    delete?: boolean;         // Optional, default false
  }>;
}
```

**Response:** `UserResponse`

---

### 6. Reset Password

**Endpoint:** `PUT /api/users/{id}/password`
**Roles:** `Authenticated`

Reset a user's password.

**Path Parameters:**
- `id`: User ID

**Request Body:**
```typescript
{
  currentPassword?: string;   // Optional (required for non-admins changing own password)
  newPassword: string;        // Required, min length 8
  confirmPassword: string;    // Required, must match newPassword
}
```

**Response:** `string`
```
"Password updated successfully"
```

---

### 7. Deactivate User

**Endpoint:** `PUT /api/users/{id}/deactivate`
**Roles:** `ADMIN`, `SUPER_ADMIN`

Deactivate a user. This triggers prospect transfer cascade:
- **Associate deactivated:** All prospects transfer to parent Licensee
- **Licensee deactivated:** All prospects and clients transfer to MLO (Master Licensee)

**Path Parameters:**
- `id`: User ID

**Response:** `UserResponse`

---

## Associate Management

### 1. Request Associate Creation

**Endpoint:** `POST /api/licensees/associates/request`
**Roles:** `LICENSEE`

Licensee submits a request to create an Associate under them.

**Request Body:**
```typescript
{
  firstName: string;      // Required, not blank
  lastName: string;       // Required, not blank
  email: string;          // Required, valid email format
  phone: string;          // Required, not blank
}
```

**Response:** `string`
```
"Associate creation request submitted successfully"
```

---

### 2. Approve/Reject Associate Creation

**Endpoint:** `PUT /api/admin/associates/{alertId}/decision`
**Roles:** `ADMIN`, `SUPER_ADMIN`

Approve or reject an Associate creation request.

**Path Parameters:**
- `alertId`: Alert ID from the associate creation request

**Query Parameters:**
- `approve`: `true` or `false`

**Response:** `ApiResponse<UserResponse>`
```typescript
{
  success: boolean;
  data: {
    id: number;
    firstName: string;
    lastName: string;
    email: string;
    phone: string;
    role: "ASSOCIATE";
    licenseeId: number;
    status: "ACTIVE" | "PENDING";
    createdAt: string;
    cities: null;
  } | null;
  message: string;
}
```

---

### 3. Request Associate Deactivation

**Endpoint:** `POST /api/users/associates/{id}/deactivation-request`
**Roles:** `LICENSEE`

Licensee submits a request to deactivate one of their Associates.

**Path Parameters:**
- `id`: Associate User ID

**Response:** `string`
```
"Associate deactivation request submitted successfully"
```

---

### 4. Approve/Reject Associate Deactivation

**Endpoint:** `PUT /api/users/associates/deactivation-requests/{alertId}`
**Roles:** `ADMIN`, `SUPER_ADMIN`

Approve or reject an Associate deactivation request.

**Path Parameters:**
- `alertId`: Alert ID from the deactivation request

**Query Parameters:**
- `approve`: `true` or `false`

**Response:** `ApiResponse<UserResponse>`

---

## Prospects

### 1. List Prospects

**Endpoint:** `GET /api/prospects`
**Roles:** `Authenticated`

Retrieve a filtered list of prospects. Role-based visibility:
- **Associates:** Only their own prospects
- **Licensees:** All prospects linked to them and their associates
- **Admins:** All prospects

**Query Parameters:**
- `type` (optional): Filter by `ProspectType` (`PROSPECT` | `CLIENT`)
- `licenseeId` (optional): Filter by licensee ID
- `associateId` (optional): Filter by associate ID
- `getAll` (optional, default: `false`): Get all prospects (respecting role visibility)

**Response:** `ApiResponse<Array<ProspectResponse>>`
```typescript
{
  success: true,
  data: [
    {
      id: number;
      companyName: string;
      city: string;
      contactFirstName: string;
      contactLastName: string;
      designation: string | null;
      email: string;
      phone: string;
      referredBy: string;
      classificationType: "AAA" | "AA" | "A" | "B" | "C";
      programType: "LI" | "SI" | "ONE_TO_ONE" | "SHC";
      type: "PROSPECT" | "CLIENT";
      status: "PROTECTED" | "UNPROTECTED" | "PROVISIONAL";
      protectionPeriodMonths: number | null;
      entryDate: string;           // ISO 8601 date (YYYY-MM-DD)
      associateId: number | null;
      licenseeId: number;
      createdBy: number;
      createdAt: string;           // ISO 8601 datetime
      provisionReason: string | null;
    }
  ],
  message: "Prospects retrieved successfully"
}
```

---

### 2. Add Prospect

**Endpoint:** `POST /api/prospects`
**Roles:** `LICENSEE`, `ASSOCIATE`

Create a new prospect. Auto-initiates protection and may flag as PROVISIONAL if:
- `programType != SHC` AND `city != licensee's city`

**Request Body:**
```typescript
{
  companyName: string;          // Required, not blank
  city: string;                 // Required, not blank
  contactFirstName: string;     // Required, not blank
  contactLastName: string;      // Required, not blank
  designation?: string;         // Optional
  email: string;                // Required, valid email format
  phone: string;                // Required, not blank
  referredBy: string;           // Required, not blank
  classificationType: "AAA" | "AA" | "A" | "B" | "C";  // Required
  programType: "LI" | "SI" | "ONE_TO_ONE" | "SHC";    // Required
}
```

**Response:** `ApiResponse<ProspectResponse>`
```typescript
{
  success: true,
  data: {
    id: number;
    companyName: string;
    city: string;
    contactFirstName: string;
    contactLastName: string;
    designation: string | null;
    email: string;
    phone: string;
    referredBy: string;
    classificationType: "AAA" | "AA" | "A" | "B" | "C";
    programType: "LI" | "SI" | "ONE_TO_ONE" | "SHC";
    type: "PROSPECT";
    status: "PROTECTED" | "PROVISIONAL";
    protectionPeriodMonths: number | null;
    entryDate: string;
    associateId: number | null;
    licenseeId: number;
    createdBy: number;
    createdAt: string;
    provisionReason: string | null;
  },
  message: "Prospect created successfully"
    // OR "Prospect has been flagged as provisional and is awaiting admin approval..."
}
```

---

### 3. Get Prospect Detail

**Endpoint:** `GET /api/prospects/{id}`
**Roles:** `Authenticated`

Retrieve details for a specific prospect.

**Path Parameters:**
- `id`: Prospect ID

**Response:** `ApiResponse<ProspectResponse>`

---

### 4. Update Prospect

**Endpoint:** `PUT /api/prospects/{id}`
**Roles:** `LICENSEE`, `ADMIN`, `SUPER_ADMIN`

Update prospect details. Also handles:
- Protection extension (via `protectionPeriodMonths`)
- Protection override (via `status`)
- Licensee reassignment (via `newLicenseeId`)

**Path Parameters:**
- `id`: Prospect ID

**Request Body:**
```typescript
{
  companyName?: string;                    // Optional, min length 1
  city?: string;                           // Optional, min length 1
  contactFirstName?: string;               // Optional, min length 1
  contactLastName?: string;                // Optional, min length 1
  designation?: string;                    // Optional
  email?: string;                          // Optional, valid email
  phone?: string;                          // Optional
  referredBy?: string;                     // Optional
  classificationType?: "AAA" | "AA" | "A" | "B" | "C";
  status?: "PROTECTED" | "UNPROTECTED" | "PROVISIONAL";  // Admin-only
  protectionPeriodMonths?: number;         // Admin-only
  newLicenseeId?: number;                  // Admin-only
}
```

**Response:** `ApiResponse<ProspectResponse>`

---

### 5. Delete Prospect

**Endpoint:** `DELETE /api/prospects/{id}`
**Roles:** `ADMIN`, `SUPER_ADMIN`

Soft delete a prospect. Sets `deletionStatus = true` and `protectionStatus = UNPROTECTED`.

**Path Parameters:**
- `id`: Prospect ID

**Response:** `ApiResponse<string>`
```typescript
{
  success: true,
  data: null,
  message: "Prospect soft deleted successfully"
}
```

---

### 6. Request Prospect Conversion

**Endpoint:** `POST /api/prospects/{id}/convert`
**Roles:** `LICENSEE`, `ASSOCIATE`

Submit a request to convert a Prospect to Client. Creates an admin alert.

**Path Parameters:**
- `id`: Prospect ID

**Response:** `ApiResponse<string>`
```typescript
{
  success: true,
  data: null,
  message: "Conversion request submitted successfully"
}
```

---

### 7. Approve/Reject Conversion

**Endpoint:** `PUT /api/prospects/conversions/{alertId}`
**Roles:** `ADMIN`, `SUPER_ADMIN`

Approve or reject a prospect conversion request.

**Path Parameters:**
- `alertId`: Alert ID from the conversion request

**Query Parameters:**
- `approve`: `true` or `false`

**Response:** `ApiResponse<ProspectResponse>`
```typescript
{
  success: boolean,
  data: {
    // ProspectResponse with type: "CLIENT" if approved
  } | null,
  message: string
}
```

---

### 8. Handle Provisional Prospect

**Endpoint:** `PUT /api/prospects/provisional/{alertId}`
**Roles:** `ADMIN`, `SUPER_ADMIN`

Approve, reject, or ignore a provisional prospect.

**Path Parameters:**
- `alertId`: Alert ID from the provisional prospect

**Query Parameters:**
- `decision`: `APPROVE` | `REJECT` | `IGNORE`

**Response:** `ApiResponse<ProspectResponse>`

---

### 9. Request Protection Extension

**Endpoint:** `POST /api/prospects/{id}/extension-request`
**Roles:** `LICENSEE`, `ASSOCIATE`

Submit a request to extend protection period for a prospect.

**Path Parameters:**
- `id`: Prospect ID

**Response:** `ApiResponse<string>`
```typescript
{
  success: true,
  data: null,
  message: "Protection extension request submitted successfully"
}
```

---

## Groups

### 1. Add Group

**Endpoint:** `POST /api/groups`
**Roles:** `LICENSEE`, `ASSOCIATE`

Create a new group training program. All linked prospects must have `type = CLIENT`.

**Request Body:**
```typescript
{
  groupSize: number;               // Required, min: 1, max: 100
  groupType: "EPP" | "ELD" | "EPL" | "ECE" | "EML" | "ESL" | "LFW" | "AIE";  // Required
  deliveryType: "ONLINE" | "HYBRID" | "IN_PERSON";  // Required
  startDate: string;               // Required, ISO 8601 date (YYYY-MM-DD)
  ppmTfeDateSent?: string;         // Optional, ISO 8601 date
  facilitatorId?: number;          // Optional
  prospectIds: number[];           // Required, at least one client ID
}
```

**Response:** `ApiResponse<GroupResponse>`
```typescript
{
  success: true,
  data: {
    id: number;
    licenseeId: number;
    licenseeName: string;
    facilitatorId: number | null;
    facilitatorName: string | null;
    groupSize: number;
    groupType: "EPP" | "ELD" | "EPL" | "ECE" | "EML" | "ESL" | "LFW" | "AIE";
    deliveryType: "ONLINE" | "HYBRID" | "IN_PERSON";
    startDate: string;              // ISO 8601 date
    ppmTfeDateSent: string | null;  // ISO 8601 date
    createdBy: number;
    createdAt: string;              // ISO 8601 datetime
    prospects: Array<ProspectResponse>;
  },
  message: "Group created successfully"
}
```

---

### 2. List Groups

**Endpoint:** `GET /api/groups`
**Roles:** `Authenticated`

Retrieve a filtered list of groups.

**Query Parameters:**
- `licenseeId` (optional): Filter by licensee ID

**Response:** `ApiResponse<Array<GroupResponse>>`

---

### 3. Get Group Detail

**Endpoint:** `GET /api/groups/{id}`
**Roles:** `Authenticated`

Retrieve details for a specific group.

**Path Parameters:**
- `id`: Group ID

**Response:** `ApiResponse<GroupResponse>`

---

### 4. Update Group

**Endpoint:** `PUT /api/groups/{id}`
**Roles:** `LICENSEE`, `ASSOCIATE`, `ADMIN`, `SUPER_ADMIN`

Update group details.

**Path Parameters:**
- `id`: Group ID

**Request Body:**
```typescript
{
  groupSize?: number;              // Optional, min: 1, max: 100
  groupType?: "EPP" | "ELD" | "EPL" | "ECE" | "EML" | "ESL" | "LFW" | "AIE";
  deliveryType?: "ONLINE" | "HYBRID" | "IN_PERSON";
  startDate?: string;              // Optional, ISO 8601 date
  ppmTfeDateSent?: string;         // Optional, ISO 8601 date
  facilitatorId?: number;          // Optional
  licenseeId?: number;             // Optional, admin-only
  addProspectIds?: number[];       // Optional, client IDs to add
  removeProspectIds?: number[];    // Optional, client IDs to remove
}
```

**Response:** `ApiResponse<GroupResponse>`

---

### 5. Request Group Deletion

**Endpoint:** `POST /api/groups/{id}/deletion-request`
**Roles:** `LICENSEE`

Submit a request to delete a group. Creates an admin alert.

**Path Parameters:**
- `id`: Group ID

**Response:** `ApiResponse<string>`
```typescript
{
  success: true,
  data: null,
  message: "Group deletion request submitted successfully"
}
```

---

### 6. Delete Group

**Endpoint:** `DELETE /api/groups/{id}`
**Roles:** `ADMIN`, `SUPER_ADMIN`

Hard delete a group (permanent deletion).

**Path Parameters:**
- `id`: Group ID

**Response:** `ApiResponse<string>`
```typescript
{
  success: true,
  data: null,
  message: "Group deleted successfully"
}
```

---

### 7. Approve/Reject Group Deletion

**Endpoint:** `PUT /api/groups/deletion-requests/{alertId}`
**Roles:** `ADMIN`, `SUPER_ADMIN`

Approve or reject a group deletion request.

**Path Parameters:**
- `alertId`: Alert ID from the deletion request

**Query Parameters:**
- `approve`: `true` or `false`

**Response:** `ApiResponse<string>`

---

## Alerts

### 1. List Alerts

**Endpoint:** `GET /api/admin/alerts`
**Roles:** `ADMIN`, `SUPER_ADMIN`

Retrieve paginated list of alerts with optional filters.

**Query Parameters:**
- `type` (optional): Filter by `AlertType`
- `status` (optional): Filter by `AlertStatus`
- `page` (optional, default: `0`): Page number (0-indexed)
- `size` (optional, default: `20`): Items per page

**Response:** `ApiResponse<Page<AlertResponse>>`
```typescript
{
  success: true,
  data: {
    content: [
      {
        id: number;
        alertType: "DUPLICATE_PROSPECT" | "PROSPECT_CONVERSION_REQUEST" | ...; // See AlertType enum
        title: string;
        description: string;
        relatedEntityType: "PROSPECT" | "USER" | "GROUP";
        relatedEntityId: number;
        triggeredBy: number;
        status: "PENDING" | "RESOLVED" | "REJECTED";
        actionRequired: boolean;
        createdAt: string;        // ISO 8601 datetime
      }
    ],
    pageable: {
      pageNumber: number;
      pageSize: number;
      sort: { ... };
      offset: number;
      paged: boolean;
      unpaged: boolean;
    },
    totalPages: number;
    totalElements: number;
    last: boolean;
    size: number;
    number: number;
    sort: { ... };
    first: boolean;
    numberOfElements: number;
    empty: boolean;
  },
  message: "Alerts retrieved successfully"
}
```

---

### 2. Get Alert Detail

**Endpoint:** `GET /api/admin/alerts/{id}`
**Roles:** `ADMIN`, `SUPER_ADMIN`

Retrieve details for a specific alert.

**Path Parameters:**
- `id`: Alert ID

**Response:** `ApiResponse<AlertResponse>`
```typescript
{
  success: true,
  data: {
    id: number;
    alertType: "DUPLICATE_PROSPECT" | "PROSPECT_CONVERSION_REQUEST" | ...;
    title: string;
    description: string;
    relatedEntityType: "PROSPECT" | "USER" | "GROUP";
    relatedEntityId: number;
    triggeredBy: number;
    status: "PENDING" | "RESOLVED" | "REJECTED";
    actionRequired: boolean;
    createdAt: string;
  },
  message: "Alert retrieved successfully"
}
```

**Note:** Alert actions (Approve/Reject) are performed via specific endpoints based on alert type:
- Associate creation/deactivation: `/api/admin/associates/{alertId}/decision`
- Prospect conversion: `/api/prospects/conversions/{alertId}`
- Provisional prospect: `/api/prospects/provisional/{alertId}`
- Group deletion: `/api/groups/deletion-requests/{alertId}`

---

## Tasks

### 1. Create Task

**Endpoint:** `POST /api/tasks`
**Roles:** `Authenticated`

Create a new task with a future due date.

**Request Body:**
```typescript
{
  title: string;          // Required, not blank
  description?: string;   // Optional
  dueDate: string;        // Required, ISO 8601 datetime
}
```

**Response:** `ApiResponse<TaskResponse>`
```typescript
{
  success: true,
  data: {
    id: number;
    userId: number;
    title: string;
    description: string | null;
    dueDate: string;        // ISO 8601 datetime
    status: "PENDING";
    createdAt: string;      // ISO 8601 datetime
    updatedAt: string;      // ISO 8601 datetime
  },
  message: "Task created successfully"
}
```

---

### 2. List Tasks

**Endpoint:** `GET /api/tasks`
**Roles:** `Authenticated`

Retrieve the current user's tasks, ordered by due date.

**Query Parameters:**
- `status` (optional): Filter by `TaskStatus` (`PENDING` | `COMPLETED`)

**Response:** `ApiResponse<Array<TaskResponse>>`
```typescript
{
  success: true,
  data: [
    {
      id: number;
      userId: number;
      title: string;
      description: string | null;
      dueDate: string;
      status: "PENDING" | "COMPLETED";
      createdAt: string;
      updatedAt: string;
    }
  ],
  message: "Tasks retrieved successfully"
}
```

---

### 3. Get Task Detail

**Endpoint:** `GET /api/tasks/{id}`
**Roles:** `Authenticated`

Retrieve details for a specific task.

**Path Parameters:**
- `id`: Task ID

**Response:** `ApiResponse<TaskResponse>`

---

### 4. Update Task

**Endpoint:** `PUT /api/tasks/{id}`
**Roles:** `Authenticated`

Update a task's title, description, due date, or status.

**Path Parameters:**
- `id`: Task ID

**Request Body:**
```typescript
{
  title?: string;               // Optional, min length 1
  description?: string;         // Optional
  dueDate?: string;             // Optional, ISO 8601 datetime
  status?: "PENDING" | "COMPLETED";
}
```

**Response:** `ApiResponse<TaskResponse>`

---

### 5. Delete Task

**Endpoint:** `DELETE /api/tasks/{id}`
**Roles:** `Authenticated`

Permanently delete a task.

**Path Parameters:**
- `id`: Task ID

**Response:** `ApiResponse<string>`
```typescript
{
  success: true,
  data: null,
  message: "Task deleted successfully"
}
```

---

## Notes

### 1. Create Note

**Endpoint:** `POST /api/notes`
**Roles:** `Authenticated`

Create a new note.

**Request Body:**
```typescript
{
  title: string;          // Required, not blank
  description?: string;   // Optional
}
```

**Response:** `ApiResponse<NoteResponse>`
```typescript
{
  success: true,
  data: {
    id: number;
    userId: number;
    title: string;
    description: string | null;
    createdAt: string;      // ISO 8601 datetime
    updatedAt: string;      // ISO 8601 datetime
  },
  message: "Note created successfully"
}
```

---

### 2. List Notes

**Endpoint:** `GET /api/notes`
**Roles:** `Authenticated`

Retrieve the current user's notes, ordered by last updated.

**Response:** `ApiResponse<Array<NoteResponse>>`
```typescript
{
  success: true,
  data: [
    {
      id: number;
      userId: number;
      title: string;
      description: string | null;
      createdAt: string;
      updatedAt: string;
    }
  ],
  message: "Notes retrieved successfully"
}
```

---

### 3. Get Note Detail

**Endpoint:** `GET /api/notes/{id}`
**Roles:** `Authenticated`

Retrieve details for a specific note.

**Path Parameters:**
- `id`: Note ID

**Response:** `ApiResponse<NoteResponse>`

---

### 4. Update Note

**Endpoint:** `PUT /api/notes/{id}`
**Roles:** `Authenticated`

Update a note's title or description.

**Path Parameters:**
- `id`: Note ID

**Request Body:**
```typescript
{
  title?: string;         // Optional, min length 1
  description?: string;   // Optional
}
```

**Response:** `ApiResponse<NoteResponse>`

---

### 5. Delete Note

**Endpoint:** `DELETE /api/notes/{id}`
**Roles:** `Authenticated`

Permanently delete a note.

**Path Parameters:**
- `id`: Note ID

**Response:** `ApiResponse<string>`
```typescript
{
  success: true,
  data: null,
  message: "Note deleted successfully"
}
```

---

## Error Handling

All endpoints may return error responses with appropriate HTTP status codes:

**400 Bad Request:**
```json
{
  "success": false,
  "data": null,
  "message": "Validation error: email must be a valid email address"
}
```

**401 Unauthorized:**
```json
{
  "success": false,
  "data": null,
  "message": "Authentication required"
}
```

**403 Forbidden:**
```json
{
  "success": false,
  "data": null,
  "message": "Access denied: insufficient permissions"
}
```

**404 Not Found:**
```json
{
  "success": false,
  "data": null,
  "message": "Resource not found"
}
```

**500 Internal Server Error:**
```json
{
  "success": false,
  "data": null,
  "message": "An unexpected error occurred"
}
```

---

## Date and Time Formats

- **Date fields** (`entryDate`, `startDate`, `ppmTfeDateSent`): ISO 8601 date format `YYYY-MM-DD`
- **DateTime fields** (`createdAt`, `updatedAt`, `dueDate`): ISO 8601 datetime format `YYYY-MM-DDTHH:mm:ss.sssZ`

**Example:**
```json
{
  "entryDate": "2026-04-18",
  "createdAt": "2026-04-18T10:30:00.000Z",
  "dueDate": "2026-04-25T15:00:00.000Z"
}
```

---

## Important Notes

1. **Soft Deletes:** Prospects use soft delete (`deletionStatus = true`). Groups, tasks, and notes use hard delete (permanent).

2. **Protection System:**
   - Protection is automatic on prospect creation
   - LI (Large In-house): 12 months + 3 months grace
   - SI (Small In-house): 6 months + 3 months grace
   - ONE_TO_ONE & SHC: Perpetual (no expiry)
   - First-meeting window: 45 days (warning) → 75 days (unprotected if no meeting)

3. **Role Visibility:**
   - Associates see only their own prospects/clients
   - Licensees see their own + their associates' data
   - Admins see everything

4. **Alert Workflow:**
   - All approval/rejection actions are performed via specific endpoints (not a unified alert action endpoint)
   - Email notifications are sent automatically on alert creation
   - No action buttons in emails - all actions happen in the dashboard

5. **Validation:**
   - Email fields must be valid email format
   - Phone fields are required but no specific format enforced (string)
   - Group size must be 1-100

---

## Search

All three search endpoints share the same query parameters and follow the same scoping model. Search is distinct from the list endpoints — it performs keyword matching across multiple text fields simultaneously rather than filtering by known category values.

### Common Query Parameters

| Parameter | Type | Required | Default | Description |
|---|---|---|---|---|
| `q` | string | yes | — | Keyword to search for (substring match, case-insensitive) |
| `scope` | string | no | `own` | `own` to search within the user's own records; `all` to search across all records |
| `page` | int | no | `0` | Zero-based page index |
| `limit` | int | no | `10` | Page size |

**Scope behaviour by role:**

| Role | `scope=own` | `scope=all` |
|---|---|---|
| Associate | Their own records only | All records in the system |
| Licensee | Records belonging to them | All records in the system |
| Admin / Super Admin | Always all (scope ignored) | Always all |

---

### Search Prospects

```
GET /api/prospects/search
```

**Access:** All authenticated roles

**Searched fields:** `companyName`, `contactFirstName`, `contactLastName`, `city`, `email`, `phone`

**Scope=own detail:**
- Associate → prospects where `associateId = self`
- Licensee → prospects linked to them via `prospect_licensees`

**Response:**
```json
{
  "success": true,
  "message": "Search results retrieved successfully",
  "data": {
    "overallTotal": 12,
    "prospectCount": 8,
    "clientCount": 4,
    "provisionalCount": 1,
    "unprotectedCount": 2,
    "prospects": {
      "content": [ /* ProspectResponse objects */ ],
      "totalElements": 12,
      "totalPages": 2,
      "number": 0,
      "size": 10
    }
  }
}
```

**Note:** Non-admin roles receive a limited `ProspectResponse` (no protection details) regardless of scope.

---

### Search Users

```
GET /api/users/search
```

**Access:** All authenticated roles

**Searched fields:** `firstName`, `lastName`, `email`, `phone`

**Scope=own detail:**
- Associate → all users with the same `licenseeId` (their teammates and parent licensee)
- Licensee → all users with `licenseeId = self` (their own associates + themselves)

**Response:**
```json
{
  "success": true,
  "message": "Search results retrieved successfully",
  "data": {
    "overallTotal": 5,
    "activeCount": 4,
    "inactiveCount": 1,
    "countByRole": {
      "LICENSEE": 1,
      "ASSOCIATE": 4
    },
    "users": {
      "content": [ /* UserResponse objects */ ],
      "totalElements": 5,
      "totalPages": 1,
      "number": 0,
      "size": 10
    }
  }
}
```

---

### Search Groups

```
GET /api/groups/search
```

**Access:** All authenticated roles

**Searched fields:** `groupType` (e.g. `LI`, `SI`), `deliveryType` (e.g. `IN_PERSON`, `VIRTUAL`), licensee full name, facilitator full name, linked client `companyName`

**Scope=own detail:**
- Associate → groups belonging to their parent licensee
- Licensee → groups they own

**Response:**
```json
{
  "success": true,
  "message": "Search results retrieved successfully",
  "data": {
    "overallTotal": 3,
    "activeCount": 3,
    "groups": {
      "content": [ /* GroupResponse objects */ ],
      "totalElements": 3,
      "totalPages": 1,
      "number": 0,
      "size": 10
    }
  }
}
```

---

### Example Requests

```
# Find prospects matching "acme" within own scope
GET /api/prospects/search?q=acme

# Find prospects matching "mumbai" across all licensees
GET /api/prospects/search?q=mumbai&scope=all

# Find associates named "john" within same licensee org
GET /api/users/search?q=john&scope=own

# Find any user matching "sharma" across the whole system
GET /api/users/search?q=sharma&scope=all

# Find groups of type LI within own scope
GET /api/groups/search?q=LI

# Find groups linked to client "Tata" across all licensees
GET /api/groups/search?q=tata&scope=all&page=0&limit=5
```
   - Password minimum length is 8 characters
