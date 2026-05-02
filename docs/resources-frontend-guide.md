# Resources Module — Frontend Integration Guide

This document covers the complete Resources feature of the LMI CRM backend. It is intended to give a frontend developer (or their AI assistant) full context to build the UI without needing to read the backend source code.

---

## Overview

Resources are files or videos that admins upload for all authenticated users to view and download. There are three types of resources:

| Type | What it stores | File formats allowed |
|---|---|---|
| `DOCUMENT` | Any document | PDF, DOC, DOCX, XLS, XLSX |
| `PPT` | Presentations | PPT, PPTX |
| `ZCDC` | Training videos | YouTube URL only (no file upload) |

Files (`DOCUMENT` and `PPT`) are stored in an **AWS S3 bucket**. The frontend never talks to S3 directly — it always goes through the backend API.

---

## Authentication

Every endpoint requires a valid JWT in the `Authorization` header:

```
Authorization: Bearer <token>
```

Upload, update, and delete endpoints additionally require the user role to be `ADMIN` or `SUPER_ADMIN`. If a lower-privileged user hits these endpoints, the server returns `403 Forbidden`.

---

## Standard Response Envelope

Every API response is wrapped in the same envelope:

```json
{
  "success": true,
  "message": "Human-readable message",
  "data": { ... }
}
```

On error (`success: false`), `data` is `null` and `message` contains the error reason.

---

## Enums Reference

### ResourceType
```
ZCDC       — YouTube video (ZCDC training content)
DOCUMENT   — PDF, Word, or Excel file
PPT        — PowerPoint file
```

### FileType
```
YOUTUBE    — Stored as a YouTube URL (set automatically for ZCDC resources)
PDF
DOC        — Covers both .doc and .docx
XLS        — Covers both .xls and .xlsx
PPT        — Covers both .ppt and .pptx
```

`fileType` is determined server-side from the uploaded file's MIME type. The frontend never sets it directly.

---

## Endpoints

### 1. Upload a Resource

**POST** `/api/resources`

**Access:** ADMIN, SUPER_ADMIN only

**Content-Type:** `multipart/form-data`

#### Form fields

| Field | Type | Required | Notes |
|---|---|---|---|
| `title` | string | Yes | Cannot be blank |
| `description` | string | No | Optional free text |
| `resourceType` | string (enum) | Yes | `ZCDC`, `DOCUMENT`, or `PPT` |
| `videoUrl` | string | Conditional | Required when `resourceType = ZCDC`. Ignored otherwise. |
| `file` | file | Conditional | Required when `resourceType = DOCUMENT` or `PPT`. Ignored for ZCDC. |

#### YouTube URL rules (for ZCDC)
The backend validates the URL strictly. Accepted formats:
- `https://www.youtube.com/watch?v=XXXXXXXXXXX`
- `https://youtu.be/XXXXXXXXXXX`
- `https://www.youtube.com/embed/XXXXXXXXXXX`

The video ID (the 11-character part) must be present and exactly 11 characters. Any other YouTube URL shape (e.g. playlist URLs, channel URLs) will be rejected.

#### File type rules
- `DOCUMENT` → only PDF, DOC, DOCX, XLS, XLSX accepted
- `PPT` → only PPT, PPTX accepted
- Sending a `.pptx` file as a `DOCUMENT` will be rejected

#### Duplicate check
The backend checks whether a resource with the same file key (for S3 files) or the same YouTube URL already exists and is not deleted. If a duplicate is found, the upload is rejected with an error message that includes the title of the conflicting resource.

#### Success response

```json
{
  "success": true,
  "message": "Resource uploaded successfully",
  "data": {
    "id": 12,
    "title": "Sales Training Q1",
    "description": "Quarterly training material",
    "resourceType": "DOCUMENT",
    "fileType": "PDF",
    "fileUrl": "document/a3f1b2c4-uuid-originalname.pdf",
    "uploadedBy": 1,
    "createdAt": "2026-05-01T10:30:00",
    "updatedAt": "2026-05-01T10:30:00"
  }
}
```

**Important:** `fileUrl` for S3-backed resources is an **S3 object key**, not a downloadable URL. Do not attempt to use it as a URL directly. Use the `/download` endpoint to get an actual URL.

For ZCDC resources, `fileUrl` contains the YouTube URL as-is and can be used directly.

---

### 2. List Resources

**GET** `/api/resources`

**Access:** All authenticated users

**Query parameters:**

| Param | Type | Default | Notes |
|---|---|---|---|
| `getAll` | boolean | `false` | Controls which response shape is returned (see below) |
| `type` | string (enum) | none | Optional filter: `ZCDC`, `DOCUMENT`, or `PPT` |
| `page` | integer | `0` | Zero-based page index. Only used when `getAll=false` |
| `limit` | integer | `5` | Page size. Used in both modes |

Results are always sorted by `createdAt` descending (newest first).

#### When `getAll=false` — paginated listing

Use this for the main resources list page. Returns a page of results plus counts.

```json
{
  "success": true,
  "message": "Resources retrieved successfully",
  "data": {
    "totalCount": 42,
    "countByType": {
      "ZCDC": 10,
      "DOCUMENT": 25,
      "PPT": 7
    },
    "resources": {
      "content": [
        {
          "id": 12,
          "title": "Sales Training Q1",
          "description": "...",
          "resourceType": "DOCUMENT",
          "fileType": "PDF",
          "fileUrl": "document/uuid-filename.pdf",
          "uploadedBy": 1,
          "createdAt": "2026-05-01T10:30:00",
          "updatedAt": "2026-05-01T10:30:00"
        }
      ],
      "totalElements": 42,
      "totalPages": 9,
      "number": 0,
      "size": 5,
      "first": true,
      "last": false
    }
  }
}
```

#### When `getAll=true` — summary + first page

Use this for a dashboard or landing view where you want counts and a preview simultaneously. The `limit` param controls how many items appear in `firstPage`. Pagination beyond the first page is not available in this mode — switch to `getAll=false` if the user wants to scroll further.

```json
{
  "success": true,
  "message": "Resources retrieved successfully",
  "data": {
    "totalCount": 42,
    "countByType": {
      "ZCDC": 10,
      "DOCUMENT": 25,
      "PPT": 7
    },
    "firstPage": {
      "content": [ ... ],
      "totalElements": 42,
      "totalPages": 9,
      "number": 0,
      "size": 5,
      "first": true,
      "last": false
    }
  }
}
```

---

### 3. Get Resource Detail

**GET** `/api/resources/{id}`

**Access:** All authenticated users

Returns the full metadata for a single resource. Does **not** return a downloadable URL — call `/download` for that.

#### Success response

```json
{
  "success": true,
  "message": "Resource retrieved successfully",
  "data": {
    "id": 12,
    "title": "Sales Training Q1",
    "description": "Quarterly training material",
    "resourceType": "DOCUMENT",
    "fileType": "PDF",
    "fileUrl": "document/uuid-filename.pdf",
    "uploadedBy": 1,
    "createdAt": "2026-05-01T10:30:00",
    "updatedAt": "2026-05-01T10:30:00"
  }
}
```

---

### 4. Download a Resource

**GET** `/api/resources/{id}/download`

**Access:** All authenticated users

This is the correct endpoint for opening or downloading a file. What it returns depends on the resource type:

| `fileType` | What is returned in `data` |
|---|---|
| `YOUTUBE` | The YouTube URL directly (e.g. `https://www.youtube.com/watch?v=...`) |
| `PDF`, `DOC`, `XLS`, `PPT` | A **pre-signed S3 URL** valid for **24 hours** |

#### Success response

```json
{
  "success": true,
  "message": "Download URL generated successfully",
  "data": "https://lmi-crm-bucket.s3.ap-south-1.amazonaws.com/document/uuid-file.pdf?X-Amz-Algorithm=..."
}
```

#### Frontend behaviour

- For **ZCDC** resources: open the returned URL in a new tab or embed it in an `<iframe>` / YouTube player.
- For **file** resources: open the presigned URL in a new tab. The browser will either download the file or render it inline (e.g. PDFs render natively in most browsers).
- The presigned URL expires after **24 hours**. Do not cache or store it — always call this endpoint fresh when the user wants to open the file.

---

### 5. Update a Resource

**PUT** `/api/resources/{id}`

**Access:** ADMIN, SUPER_ADMIN only

**Content-Type:** `multipart/form-data`

All fields are optional. Only the fields you send will be updated.

| Field | Type | Notes |
|---|---|---|
| `title` | string | Must be at least 1 character if provided |
| `description` | string | Replaces existing description |
| `videoUrl` | string | Only processed if the resource is `ZCDC` type. Same YouTube validation applies. |
| `file` | file | Only processed if the resource is `DOCUMENT` or `PPT` type. A new file is uploaded to S3 and replaces the old key in the database. The old S3 file is **not** deleted from storage. |

**You cannot change the `resourceType` of an existing resource.** For example, you cannot turn a `DOCUMENT` into a `ZCDC`. Create a new resource and delete the old one instead.

#### Success response

Same shape as the upload response — returns the full updated `ResourceResponse`.

---

### 6. Delete a Resource

**DELETE** `/api/resources/{id}`

**Access:** ADMIN, SUPER_ADMIN only

This is a **soft delete**. The record remains in the database with `deletionStatus = true`. It will no longer appear in any listing or detail endpoint. The S3 file is **not** removed from the bucket.

#### Success response

```json
{
  "success": true,
  "message": "Resource deleted successfully",
  "data": null
}
```

---

## S3 Interaction — How It Works

The frontend never communicates with S3 directly. Here is exactly what happens inside the backend for each operation:

### Upload flow (DOCUMENT / PPT)

```
Frontend                    Backend                        AWS S3
   |                           |                              |
   |-- POST /api/resources -->>|                              |
   |   multipart/form-data     |                              |
   |   (file attached)         |                              |
   |                           |-- validate file type ------->|
   |                           |-- generate S3 key:           |
   |                           |   {type}/{uuid}-{filename}   |
   |                           |-- PutObject (binary stream)->|
   |                           |<- 200 OK --------------------|
   |                           |-- save key to DB             |
   |<-- ResourceResponse ------|                              |
   |   (fileUrl = S3 key,      |                              |
   |    NOT a URL)             |                              |
```

The S3 key format is: `{resourcetype}/{uuid}-{originalfilename}`

Examples:
- `document/3a7f1b2c-4d5e-Sales-Q1.pdf`
- `ppt/9c8b7a6d-2e3f-Training-Deck.pptx`

### Download flow (DOCUMENT / PPT)

```
Frontend                    Backend                        AWS S3
   |                           |                              |
   |-- GET /api/resources/12/download -->                     |
   |                           |                              |
   |                           |-- read S3 key from DB        |
   |                           |-- GeneratePresignedUrl ---->>|
   |                           |   (key, expiry = 1440 min)   |
   |                           |<-- signed URL ---------------|
   |<-- { data: "https://..." }|                              |
   |                           |                              |
   |-- GET presigned URL --------------------------->>        |
   |<-- file binary (direct from S3) ----------------------- |
```

The presigned URL is a temporary, signed direct link to the S3 object. The file transfer happens directly between the user's browser and S3 — the backend is not in that path.

### Upload flow (ZCDC)

```
Frontend                    Backend                  YouTube
   |                           |                        |
   |-- POST /api/resources -->>|                        |
   |   videoUrl=https://...    |                        |
   |                           |-- validate URL format  |
   |                           |-- validate video ID    |
   |                           |   (11 chars, present)  |
   |                           |-- save URL to DB       |
   |<-- ResourceResponse ------|                        |
   |   (fileUrl = YouTube URL) |                        |
```

No S3 interaction occurs for ZCDC resources. The YouTube URL is stored as-is.

### Download flow (ZCDC)

```
Frontend                    Backend
   |                           |
   |-- GET /api/resources/7/download -->
   |                           |
   |                           |-- read fileUrl from DB
   |                           |   (it's a YouTube URL)
   |<-- { data: "https://www.youtube.com/watch?v=..." }
   |
   |-- open URL in new tab / embed in player
```

---

## Error Responses

All errors return HTTP `200` with `success: false` (the backend uses a global exception handler that wraps all `RuntimeException` into this envelope).

Common error messages you'll need to handle in the UI:

| Scenario | `message` value |
|---|---|
| Not authenticated | `403 Forbidden` (HTTP level, before envelope) |
| Role insufficient | `"Access denied"` |
| Resource not found or deleted | `"Resource not found"` |
| ZCDC missing video URL | `"Video URL is required for ZCDC resources"` |
| Bad YouTube URL format | `"Invalid YouTube URL format. Must be a valid YouTube watch, share, or embed link"` |
| Bad YouTube video ID | `"Invalid YouTube video ID"` |
| File missing for DOCUMENT/PPT | `"File is required for DOCUMENT and PPT resources"` |
| Wrong file type for resource | `"DOCUMENT resources only allow PDF, DOC, DOCX, XLS, XLSX files"` or `"PPT resources only allow PPT, PPTX files"` |
| Duplicate YouTube URL | `"A resource with this YouTube URL already exists: {title}"` |
| Unsupported file format | `"Unsupported file type. Allowed: PDF, DOC, DOCX, XLS, XLSX, PPT, PPTX"` |

---

## UI Workflow Recommendations

### Upload form logic

```
User selects resourceType
  ├── ZCDC     → show videoUrl text field, hide file input
  ├── DOCUMENT → hide videoUrl, show file input (accept: .pdf,.doc,.docx,.xls,.xlsx)
  └── PPT      → hide videoUrl, show file input (accept: .ppt,.pptx)
```

### Rendering the resource list

- Use `GET /api/resources?getAll=true&limit=5` on first load to get counts and a preview simultaneously.
- When the user switches to a dedicated list view, switch to `getAll=false` with pagination controls.
- To filter by type, append `&type=DOCUMENT` (or `ZCDC` / `PPT`).

### Opening / downloading a resource

Never use `fileUrl` from the list or detail response as a link. Always call `GET /api/resources/{id}/download` first, then:
- ZCDC (`fileType = YOUTUBE`): open the URL in a YouTube player or new tab.
- Files (`fileType = PDF/DOC/XLS/PPT`): open the presigned URL in a new tab. Presigned URLs expire after 24 hours so do not persist them — fetch fresh on each click.

### Edit form logic

```
On load: show current title, description
  ├── ZCDC     → show videoUrl field pre-filled, no file input
  ├── DOCUMENT → show file input for replacement (optional), no videoUrl
  └── PPT      → show file input for replacement (optional), no videoUrl

On submit:
  Only send fields the user actually changed.
  If no new file is selected, omit the file field entirely.
  resourceType cannot be changed — do not show that field as editable.
```

### Delete

Show a confirmation dialog before calling `DELETE /api/resources/{id}`. After a successful delete, remove the item from the local list state immediately (no need to re-fetch).

---

## Multipart Form Submission Notes

All upload and update requests use `multipart/form-data`. When calling these endpoints with `fetch` or `axios`:

**Do not set `Content-Type` manually.** Let the browser set it automatically so the boundary is included.

```js
// Correct
const formData = new FormData();
formData.append('title', 'Sales Training Q1');
formData.append('resourceType', 'DOCUMENT');
formData.append('file', fileInputRef.current.files[0]);

fetch('/api/resources', {
  method: 'POST',
  headers: { Authorization: `Bearer ${token}` }, // only auth header
  body: formData,
});
```

For ZCDC, append `videoUrl` instead of `file`:

```js
formData.append('title', 'ZCDC Session 3');
formData.append('resourceType', 'ZCDC');
formData.append('videoUrl', 'https://www.youtube.com/watch?v=dQw4w9WgXcQ');
```
