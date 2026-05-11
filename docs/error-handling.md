# Error Handling

## Architecture

Error handling has two layers:

1. **Per-endpoint try-catch** in every controller method — logs full context (endpoint, `requestingUserId`, key params) and stacktrace at `ERROR` level before re-throwing.
2. **`GlobalExceptionHandler`** (`@RestControllerAdvice`) — catches all re-thrown exceptions and maps them to a consistent `ApiResponse` JSON body.

### Response format

All error responses use the same envelope as successful responses:

```json
{
  "success": false,
  "data": null,
  "message": "<reason string>"
}
```

---

## HTTP Status Codes

### 400 Bad Request
Input is structurally or semantically invalid. The request cannot be processed as-is.

| Situation | Example message |
|---|---|
| Missing required field | `File is required for DOCUMENT and PPT resources` |
| Field fails a business rule | `Due date must be in the future` |
| Wrong type for an operation | `Alert is not a conversion request` |
| Password rules violated | `Current password is incorrect`, `New password cannot be same as current password`, `Passwords do not match`, `Password must be at least 8 characters` |
| Entity is in wrong state for operation | `Prospect is not provisional`, `Item is not a task`, `Target user is not an Associate`, `Target user is not a licensee` |
| Invalid file type | `DOCUMENT resources only allow PDF, DOC, DOCX, XLS, XLSX files`, `Unsupported file type. Allowed: PDF, DOC, DOCX, XLS, XLSX, PPT, PPTX` |
| Invalid URL | `Invalid YouTube URL format. Must be a valid YouTube watch, share, or embed link` |
| Invalid OTP/token | `OTP has expired`, `Invalid OTP`, `Invalid or expired invitation token`, `No pending OTP found` |
| Constraint violated | `Group size (N) cannot be less than the number of clients in the group (M)`, `extensionMonths must be a positive number when approving` |
| Missing linkage | `Associate is not linked to a licensee`, `licenseeId is required when creating a group as admin` |
| City operations | `Cannot delete primary city` |
| Role change constraints | `A licenseeId must be provided when setting role to ASSOCIATE` |

---

### 401 Unauthorized
The request carries no valid identity.

| Situation | Example message |
|---|---|
| No authenticated user in security context | `No authenticated user found` |
| Wrong credentials at login | `Invalid credentials` |

---

### 403 Forbidden
The caller is authenticated but not permitted to perform this action.

| Situation | Example message |
|---|---|
| Generic permission failure | `Access denied` |
| Role-specific restriction | `Access denied: Associates cannot update prospects`, `Only a Licensee can request associate creation`, `Only an Admin can approve or reject associate creation requests` |
| Cross-entity ownership | `You do not have ownership of this prospect`, `This Associate does not belong to your licensee`, `You can only update your own meetings`, `You can only delete your own meetings` |
| Admin-on-admin restriction | `Admin cannot deactivate another Admin` |
| Role change restriction | `Admin can only change role of Associates`, `Licensee role cannot be changed` |
| Account blocked | `Account setup not complete. Please check your invitation email.`, `Account has been deactivated. Contact your administrator.` |

---

### 404 Not Found
The requested resource does not exist or has been soft-deleted.

| Resource | Example message |
|---|---|
| User | `User not found`, `User not found with id: {id}` |
| Prospect | `Prospect not found` |
| Alert | `Alert not found`, `Alert not found with id: {id}` |
| Group | `Group not found` |
| Meeting | `Meeting not found` |
| Resource | `Resource not found` |
| Task | `Task not found` |
| Note | `Note not found` |
| Licensee | `Licensee not found with id: {id}` |
| City | `City not found: {name}`, `No primary city found for licensee: {id}` |

---

### 409 Conflict
The request is valid but conflicts with existing state.

| Situation | Example message |
|---|---|
| Duplicate email/phone | `A user with this email already exists`, `A user with this phone number already exists`, `Email already in use` |
| Duplicate prospect | `A prospect with email '...' already exists in the system`, `Duplicate Prospect Detected: {name} at {company} already exists in the system` |
| Already inactive | `User is already inactive` |
| Already a client | `Prospect is already a client` |
| Invitation already used | `This invitation has already been used` |
| Account already activated | `Account is already active or deactivated` |
| Pending request already exists | `A deactivation request for this Associate is already pending`, `A deletion request for this group is already pending`, `A conversion request for this prospect is already pending`, `A protection extension request is already pending for this prospect` |
| Alert already resolved | `Alert has already been acted on`, `Alert is no longer pending` |
| Duplicate resource file/URL | `A resource with this file already exists: {title}`, `A resource with this YouTube URL already exists: {title}` |
| Duplicate city | `City already exists: {name}` |

---

### 500 Internal Server Error
An unexpected error that is not a known business failure.

Two sub-cases:

**Known infrastructure failures** (thrown explicitly):

| Situation | Message |
|---|---|
| S3 upload failure | `Failed to upload file to S3` |

**Unknown/unclassified exceptions** (caught by global handler fallback):

| Situation | Message |
|---|---|
| Any unhandled `RuntimeException` or `Exception` | `An unexpected error occurred. Please try again later.` |

Internal failures that are bugs rather than user errors (e.g. JSON serialisation of alert payload) are also left as bare `RuntimeException` and produce a 500.

---

## Controller Logging

Every endpoint logs the full context when an exception is caught, before re-throwing:

```
ERROR POST /api/prospects — failed — requestingUserId: 42, company: Acme Corp — Prospect not found
<full stacktrace>
```

```
ERROR POST /api/prospects — unexpected error — requestingUserId: 42, company: Acme Corp
<full stacktrace>
```

The distinction is:
- `failed` — a `RuntimeException` (known business or infrastructure error)
- `unexpected error` — a checked `Exception` (true unknown)

---

## GlobalExceptionHandler mapping

```
ResponseStatusException  →  status from exception, body: { message: reason }
RuntimeException         →  500,  body: { message: "An unexpected error occurred. Please try again later." }
Exception                →  500,  body: { message: "An unexpected error occurred. Please try again later." }
```

`ResponseStatusException` is always tried first. A bare `RuntimeException` reaching the handler means something slipped through that should have been classified — it is logged at `ERROR` with the note *"this is likely a bug"*.
