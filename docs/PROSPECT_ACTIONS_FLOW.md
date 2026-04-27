# Prospect Actions Flow — Product Documentation

## Overview

This document explains all prospect actions from a product perspective, including what happens when users click action buttons, the complete workflow through the system, and how to test each action.

---

## Table of Contents

1. [Role-Based Action Matrix](#role-based-action-matrix)
2. [Action Workflows](#action-workflows)
   - [View Prospect Details](#1-view-prospect-details)
   - [Edit Prospect](#2-edit-prospect)
   - [Delete Prospect](#3-delete-prospect)
   - [Request Conversion (Prospect → Client)](#4-request-conversion-prospect--client)
   - [Request Protection Extension](#5-request-protection-extension)
   - [Handle Provisional Prospect](#6-handle-provisional-prospect)
3. [Testing & Verification Guide](#testing--verification-guide)

---

## Role-Based Action Matrix

| Action | Associate | Licensee | Admin | Frontend Condition | API Endpoint | Creates Alert? |
|--------|-----------|----------|-------|-------------------|--------------|----------------|
| **View Details** | ✓ | ✓ | ✓ | Always visible to authenticated users | `GET /api/prospects/{id}` | No |
| **Edit** | ✗ | ✓ (owner only) | ✓ | `['LICENSEE', 'ADMIN', 'SUPER_ADMIN'].includes(role) && isOwner` | `PUT /api/prospects/{id}` | No |
| **Delete** | ✗ | ✗ | ✓ | `['ADMIN', 'SUPER_ADMIN'].includes(role)` | `DELETE /api/prospects/{id}` | No |
| **Request Conversion** | ✓ (owner only) | ✓ (owner only) | ✗ | `['LICENSEE', 'ASSOCIATE'].includes(role) && prospect.type === 'PROSPECT' && isOwner` | `POST /api/prospects/{id}/convert` | Yes |
| **Request Extension** | ✓ (owner only) | ✓ (owner only) | ✗ | `['LICENSEE', 'ASSOCIATE'].includes(role) && prospect.status === 'PROTECTED' && prospect.protectionPeriodMonths && isOwner && !hasExtensionRequest` | `POST /api/prospects/{id}/extension-request` | Yes |
| **Handle Provisional** | ✗ | ✗ | ✓ | `['ADMIN', 'SUPER_ADMIN'].includes(role) && prospect.status === 'PROVISIONAL'` | `PUT /api/prospects/provisional/{alertId}` | No |

### Ownership Calculation

**Frontend Logic:**
```javascript
// Admin always has ownership privileges
if (role === 'ADMIN' || role === 'SUPER_ADMIN') {
  isOwner = true;
}
// Licensee owns if prospect.licenseeId matches their userId
else if (role === 'LICENSEE' && prospect.licenseeId === currentUserId) {
  isOwner = true;
}
// Associate owns if prospect.associateId matches their userId
else if (role === 'ASSOCIATE' && prospect.associateId === currentUserId) {
  isOwner = true;
}
else {
  isOwner = false;
}
```

---

## Action Workflows

### 1. View Prospect Details

**Applicable Roles:** All authenticated users (Associate, Licensee, Admin, Super Admin)

**What It Does:**
- Displays complete prospect information including contact details, classification, program type, protection status, and ownership information
- No ownership restrictions — everyone can view full details
- Action buttons are conditionally shown based on role and ownership

**Workflow:**

```
┌─────────────────────────────────────────────────────────────┐
│ User navigates to /dashboard/prospects/{id}                 │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ Frontend: useEffect calls getProspectById(id)               │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ API: GET /api/prospects/{id}                                │
│ • No ownership check                                        │
│ • Returns full ProspectResponse (18+ fields)                │
│ • Fields: companyName, email, phone, programType,           │
│   classificationType, status, protectionPeriodMonths, etc.  │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ Frontend: Calculate ownership using currentUserId           │
│ • Compare userId with prospect.licenseeId/associateId       │
│ • Set isOwner state (true/false)                            │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ Frontend: Render page with conditional action buttons       │
│ • Show all prospect details                                 │
│ • Show/hide Edit, Delete, Convert, Extend based on          │
│   role + ownership                                          │
└─────────────────────────────────────────────────────────────┘
```

**Backend Code:** `ProspectServiceImpl.java:419-445`

**Key Points:**
- Removed ownership validation from backend `getProspectDetail()` method
- Always returns `toResponse()` (full fields) instead of role-based `toLimitedResponse()`
- Frontend calculates ownership for action button visibility

---

### 2. Edit Prospect

**Applicable Roles:** Licensee (owner only), Admin, Super Admin

**What It Does:**
- Opens inline edit form with pre-filled prospect data
- Validates required fields (company name, contact name, email, phone, city, classification)
- Updates prospect information
- Refreshes page to show updated data

**Workflow:**

```
┌─────────────────────────────────────────────────────────────┐
│ User clicks "Edit" button                                   │
│ Condition: canEdit = ['LICENSEE', 'ADMIN'].includes(role)  │
│            && isOwner                                       │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ Frontend: Toggle edit mode, show editable TextFields        │
│ • Pre-fill all fields with current prospect data            │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ User modifies fields and clicks "Save"                      │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ Frontend: Run validateForm()                                │
│ • Check required fields not empty                           │
│ • Validate email format (regex)                             │
│ • Show inline errors if validation fails                    │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ API: PUT /api/prospects/{id}                                │
│ Request body: { companyName, contactFirstName,              │
│   contactLastName, email, phone, city, classificationType,  │
│   programType, designation, referredBy }                    │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ Backend: ProspectServiceImpl.updateProspect()               │
│ • Validates ownership (licensee/associate owns prospect)    │
│ • Admins can update any prospect                            │
│ • Updates allowed fields                                    │
│ • Returns updated ProspectResponse                          │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ Frontend: Exit edit mode, refresh prospect data             │
│ • Calls getProspectById(id) to re-fetch                     │
│ • Shows success toast                                       │
└─────────────────────────────────────────────────────────────┘
```

**Backend Code:** `ProspectServiceImpl.java:updateProspect()`

**Frontend Validation Rules:**
- Company Name: Required
- Contact First Name: Required
- Contact Last Name: Required
- Email: Required + must match email regex pattern
- Phone: Required
- City: Required
- Classification Type: Required

**Admin-Only Fields** (can only be updated by Admin/Super Admin):
- `protectionPeriodMonths` — Manual protection extension
- `status` — Change protection status
- Licensee reassignment

---

### 3. Delete Prospect

**Applicable Roles:** Admin, Super Admin only

**What It Does:**
- Soft deletes the prospect (sets `deletionStatus = true`)
- Sets `protectionStatus = UNPROTECTED`
- Does NOT hard delete from database
- Redirects user to prospects list page after deletion

**Workflow:**

```
┌─────────────────────────────────────────────────────────────┐
│ User clicks "Delete" button                                 │
│ Condition: canDelete = ['ADMIN', 'SUPER_ADMIN']            │
│            .includes(role)                                  │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ Frontend: Show confirmation dialog                          │
│ "Are you sure you want to delete this prospect?"            │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼ (User confirms)
┌─────────────────────────────────────────────────────────────┐
│ API: DELETE /api/prospects/{id}                             │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ Backend: ProspectServiceImpl.softDeleteProspect()           │
│ • Validates role (Admin/Super Admin only)                   │
│ • Sets prospect.deletionStatus = true                       │
│ • Sets prospect.protectionStatus = UNPROTECTED              │
│ • Saves to database (soft delete)                           │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ Frontend: Navigate to /dashboard/prospects                  │
│ • Shows success toast                                       │
└─────────────────────────────────────────────────────────────┘
```

**Backend Code:** `ProspectServiceImpl.java:softDeleteProspect()`

**Important Notes:**
- Licensees CANNOT delete prospects (backend implementation blocks this, even though CLAUDE.md suggests otherwise)
- Deletion is reversible (data remains in database with `deletionStatus = true`)
- No alert is created for deletion

---

### 4. Request Conversion (Prospect → Client)

**Applicable Roles:** Associate (owner only), Licensee (owner only)

**What It Does:**
- Submits a request to convert a prospect to a client
- Creates an alert for admin approval
- Alert appears on the Alerts page (to be implemented)
- Admin approves/rejects the request from the Alerts page
- On approval, prospect.type changes from `PROSPECT` to `CLIENT`

**Workflow:**

```
┌─────────────────────────────────────────────────────────────┐
│ User clicks "Request Client Conversion" button              │
│ Condition: canConvert = ['LICENSEE', 'ASSOCIATE']          │
│            .includes(role) && prospect.type === 'PROSPECT'  │
│            && isOwner                                       │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ API: POST /api/prospects/{id}/convert                       │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ Backend: ProspectServiceImpl.requestConversion()            │
│ • Validates role (Associate/Licensee only)                  │
│ • Validates prospect.type === PROSPECT (not already client) │
│ • Checks ownership:                                         │
│   - Associate: prospect.associateId === userId              │
│   - Licensee: prospect.licenseeId === userId                │
│ • Checks no pending PROSPECT_CONVERSION_REQUEST alert       │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ Backend: alertService.createAlert()                         │
│ • Type: PROSPECT_CONVERSION_REQUEST                         │
│ • Title: "Conversion Request — {companyName}"               │
│ • Description: "User id {userId} has requested conversion   │
│   of prospect: {companyName} (id: {prospectId}) to Client"  │
│ • Related Entity: PROSPECT (id: prospectId)                 │
│ • Requires Admin Action: true                               │
│ • Status: PENDING                                           │
│ • Sends email to admin with alert summary                   │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ Frontend: Show success toast                                │
│ "Conversion request submitted successfully"                 │
│ • Button is now hidden (hasConversionRequest = true)        │
└─────────────────────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ === ADMIN APPROVAL FLOW (on Alerts Page) ===                │
│ Admin navigates to /dashboard/alerts                        │
│ • Sees alert with type PROSPECT_CONVERSION_REQUEST          │
│ • Clicks to view alert details                              │
│ • Clicks "Approve" or "Reject"                              │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ API: Called via unified alert action endpoint               │
│ Internal call: approveRejectConversion(alertId, approve)    │
└────────────────────┬────────────────────────────────────────┘
                     │
        ┌────────────┴────────────┐
        │                         │
        ▼ (Reject)                ▼ (Approve)
┌────────────────────┐    ┌────────────────────────────────────┐
│ Alert status =     │    │ Backend: Update prospect            │
│ REJECTED           │    │ • prospect.type = CLIENT            │
│                    │    │ • Save prospect                     │
│ No prospect change │    │ • Alert status = RESOLVED           │
└────────────────────┘    └────────────────┬───────────────────┘
                                           │
                                           ▼
                          ┌────────────────────────────────────┐
                          │ Frontend (Alerts Page):             │
                          │ • Removes alert from pending list   │
                          │ • Shows success message             │
                          │                                    │
                          │ Prospect Detail Page:              │
                          │ • Prospect now shows type: CLIENT   │
                          │ • Convert button no longer visible  │
                          └────────────────────────────────────┘
```

**Backend Code:**
- Request: `ProspectServiceImpl.java:445-499` (requestConversion)
- Approval: `ProspectServiceImpl.java:501-551` (approveRejectConversion)
- Alert Creation: `AlertService.java` (createAlert)

**Frontend Implementation:**
- **Prospect Detail Page:** Button to submit request
- **Alerts Page (To Be Implemented):** Admin views and approves/rejects

**Validation Checks:**
1. User must be Associate or Licensee
2. User must own the prospect
3. Prospect type must be `PROSPECT` (not already CLIENT)
4. No pending conversion request already exists for this prospect

**Error Messages:**
- "Prospect is already a client" (if type === CLIENT)
- "Access denied" (if not owner)
- "A conversion request for this prospect is already pending" (duplicate request)

---

### 5. Request Protection Extension

**Applicable Roles:** Associate (owner only), Licensee (owner only)

**What It Does:**
- Submits a request to extend the protection period for a prospect
- Creates an alert for admin approval
- Alert appears on the Alerts page (to be implemented)
- Admin approves/rejects the request from the Alerts page
- On approval, admin manually extends `protectionPeriodMonths` via prospect edit

**Workflow:**

```
┌─────────────────────────────────────────────────────────────┐
│ User clicks "Request Protection Extension" button           │
│ Condition: canRequestExtension =                            │
│   ['LICENSEE', 'ASSOCIATE'].includes(role)                  │
│   && prospect.status === 'PROTECTED'                        │
│   && prospect.protectionPeriodMonths exists                 │
│   && isOwner                                                │
│   && !hasExtensionRequest                                   │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ API: POST /api/prospects/{id}/extension-request             │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ Backend: ProspectServiceImpl.requestProtectionExtension()   │
│ • Validates role (Associate/Licensee only)                  │
│ • Checks ownership (same logic as conversion)               │
│ • Checks no pending PROTECTION_EXTENSION_REQUEST alert      │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ Backend: alertService.createAlert()                         │
│ • Type: PROTECTION_EXTENSION_REQUEST                        │
│ • Title: "Protection Extension Request — {companyName}"     │
│ • Description: "User id {userId} has requested a protection │
│   period extension for prospect: {companyName} (id: {id})"  │
│ • Related Entity: PROSPECT (id: prospectId)                 │
│ • Requires Admin Action: true                               │
│ • Status: PENDING                                           │
│ • Sends email to admin with alert summary                   │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ Frontend: Show success toast                                │
│ "Protection extension request submitted successfully"       │
│ • Button is now hidden (hasExtensionRequest = true)         │
└─────────────────────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ === ADMIN APPROVAL FLOW (on Alerts Page) ===                │
│ Admin navigates to /dashboard/alerts                        │
│ • Sees alert with type PROTECTION_EXTENSION_REQUEST         │
│ • Clicks to view alert details                              │
│ • Reviews prospect's current protection status              │
│ • Clicks "Approve" or "Reject"                              │
└────────────────────┬────────────────────────────────────────┘
                     │
        ┌────────────┴────────────┐
        │                         │
        ▼ (Reject)                ▼ (Approve)
┌────────────────────┐    ┌────────────────────────────────────┐
│ Alert status =     │    │ Admin manually extends protection:  │
│ REJECTED           │    │                                    │
│                    │    │ 1. Navigate to prospect detail page │
│ No prospect change │    │ 2. Click "Edit"                     │
│                    │    │ 3. Update protectionPeriodMonths    │
└────────────────────┘    │    field (admin-only field)         │
                          │ 4. Save changes                     │
                          │                                    │
                          │ Alert status = RESOLVED             │
                          └────────────────┬───────────────────┘
                                           │
                                           ▼
                          ┌────────────────────────────────────┐
                          │ Frontend (Alerts Page):             │
                          │ • Marks alert as resolved           │
                          │                                    │
                          │ Prospect Detail Page:              │
                          │ • Shows updated protection period   │
                          │ • Progress bar reflects new expiry  │
                          │ • Color-coded status updates        │
                          └────────────────────────────────────┘
```

**Backend Code:**
- Request: `ProspectServiceImpl.java:172-216` (requestProtectionExtension)
- Alert Creation: `AlertService.java` (createAlert)

**Frontend Implementation:**
- **Prospect Detail Page:** Button to submit request (with conditional visibility)
- **Alerts Page (To Be Implemented):** Admin views request and approves/rejects
- **Protection Status Card:** Shows visual progress bar with color-coded status

**Protection Status Colors:**
- **Green** (>60 days): Protected
- **Yellow** (30-60 days): Attention
- **Orange** (15-30 days): Warning
- **Red** (<15 days): Critical
- **Dark Red** (<0 days): Expired

**Validation Checks:**
1. User must be Associate or Licensee
2. User must own the prospect
3. Prospect must have `status = PROTECTED`
4. Prospect must have a `protectionPeriodMonths` value
5. No pending extension request already exists for this prospect

**Error Messages:**
- "You do not have ownership of this prospect" (if not owner)
- "A protection extension request is already pending for this prospect" (duplicate request)

**Important Note:**
Protection extension approval does NOT automatically extend the period. Admin must manually edit the prospect and update the `protectionPeriodMonths` field after approving the alert.

---

### 6. Handle Provisional Prospect

**Applicable Roles:** Admin, Super Admin only

**What It Does:**
- Appears when a prospect is created with `status = PROVISIONAL`
- Admin can Approve (change to PROTECTED), Reject (delete), or Ignore
- Action buttons appear directly on prospect detail page for admins

**Workflow:**

```
┌─────────────────────────────────────────────────────────────┐
│ Prospect created with city mismatch or other provisional    │
│ trigger condition                                           │
│ • prospect.status = PROVISIONAL                             │
│ • prospect.provisionReason = "City mismatch: ..."           │
│ • Alert created: DUPLICATE_PROSPECT type                    │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ Admin views prospect detail page                            │
│ • Yellow warning banner displays:                           │
│   "Provisional Prospect — Awaiting Admin Approval"          │
│   Shows provision reason                                    │
│ • Action buttons: "Approve" | "Reject" | "Ignore"           │
│ Condition: canHandleProvisional =                           │
│   ['ADMIN', 'SUPER_ADMIN'].includes(role)                   │
│   && prospect.status === 'PROVISIONAL'                      │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ Admin clicks "Approve", "Reject", or "Ignore"               │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ API: PUT /api/prospects/provisional/{alertId}               │
│ Query param: ?decision=APPROVE|REJECT|IGNORE                │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ Backend: ProspectServiceImpl.approveRejectProvisional()     │
│ • Validates role (Admin/Super Admin only)                   │
│ • Validates alert type = DUPLICATE_PROSPECT                 │
│ • Validates alert status = PENDING                          │
│ • Validates prospect status = PROVISIONAL                   │
└────────────────────┬────────────────────────────────────────┘
                     │
        ┌────────────┼────────────┐
        │            │            │
        ▼ (APPROVE)  ▼ (REJECT)   ▼ (IGNORE)
┌───────────────┐ ┌───────────┐ ┌─────────────────┐
│ prospect.     │ │ prospect. │ │ prospect.status │
│ status =      │ │ status =  │ │ unchanged       │
│ PROTECTED     │ │ UNPROT-   │ │                 │
│               │ │ ECTED     │ │ Alert status =  │
│ Alert status  │ │           │ │ RESOLVED        │
│ = RESOLVED    │ │ Soft      │ │                 │
│               │ │ delete    │ │ No other action │
└───────┬───────┘ │           │ └────────┬────────┘
        │         │ Alert     │          │
        │         │ status =  │          │
        │         │ REJECTED  │          │
        │         └─────┬─────┘          │
        │               │                │
        └───────────────┴────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│ Frontend: Refresh prospect data                             │
│ • If APPROVED: Banner disappears, status = PROTECTED        │
│ • If REJECTED: Redirect to prospects list (deleted)         │
│ • If IGNORED: Banner remains, no status change             │
│ • Shows success toast with action result                    │
└─────────────────────────────────────────────────────────────┘
```

**Backend Code:**
- Handler: `ProspectServiceImpl.java:553-606` (approveRejectProvisional)

**Frontend Implementation:**
- **Warning Banner:** Shows on prospect detail page when `status = PROVISIONAL`
- **Action Buttons:** Approve, Reject, Ignore (admin only)
- **Alert Type:** `DUPLICATE_PROSPECT`

**Decision Outcomes:**
- **APPROVE:** Changes `prospect.status` to `PROTECTED`, resolves alert
- **REJECT:** Sets `prospect.status` to `UNPROTECTED`, soft deletes prospect (`deletionStatus = true`), rejects alert
- **IGNORE:** No change to prospect status, resolves alert (admin acknowledges but takes no action)

**Validation Checks:**
1. User must be Admin or Super Admin
2. Alert type must be `DUPLICATE_PROSPECT`
3. Alert status must be `PENDING`
4. Prospect status must be `PROVISIONAL`

**Provisional Triggers:**
- City mismatch: Prospect city doesn't match licensee's registered cities (except SHC program type)
- Other business rules (to be defined)

---

## Testing & Verification Guide

### Prerequisites
- Backend running on `http://localhost:8080`
- Frontend running on `http://localhost:3000`
- Test user accounts for each role:
  - Associate account
  - Licensee account
  - Admin account

### Test Scenarios

#### Scenario 1: View Prospect Details (All Roles)

**As Associate (Non-Owner):**
1. Login as Associate user
2. Navigate to a prospect they don't own: `/dashboard/prospects/{id}`
3. **Expected:**
   - All prospect details visible (email, phone, programType, etc.)
   - NO action buttons visible (Edit, Delete, Convert, Extend)
   - Protection status card displays correctly
   - Program type tooltip shows rules

**As Licensee (Owner):**
1. Login as Licensee user
2. Navigate to a prospect they own
3. **Expected:**
   - All prospect details visible
   - "Edit" button visible
   - "Request Client Conversion" visible (if type = PROSPECT)
   - "Request Protection Extension" visible (if status = PROTECTED)
   - NO "Delete" button

**As Admin:**
1. Login as Admin user
2. Navigate to any prospect
3. **Expected:**
   - All prospect details visible
   - "Edit" and "Delete" buttons visible
   - NO "Request Conversion" or "Request Extension" buttons

#### Scenario 2: Edit Prospect (Licensee/Admin)

**As Licensee (Owner):**
1. Login as Licensee, navigate to owned prospect
2. Click "Edit" button
3. **Expected:** Form fields become editable
4. Leave "Company Name" empty and click "Save"
5. **Expected:** Red error message "Company name is required"
6. Enter invalid email format "test@invalid"
7. **Expected:** Error message "Invalid email format"
8. Fill all required fields correctly and click "Save"
9. **Expected:**
   - Success toast "Prospect updated successfully"
   - Page exits edit mode and shows updated data
10. Try to edit a prospect owned by another licensee
11. **Expected:** Edit button not visible

**As Admin:**
1. Login as Admin, navigate to any prospect
2. Click "Edit", update `protectionPeriodMonths` field
3. **Expected:** Can successfully update admin-only field
4. **Note:** Non-admins don't see this field in edit mode

#### Scenario 3: Delete Prospect (Admin Only)

**As Licensee:**
1. Login as Licensee, navigate to owned prospect
2. **Expected:** NO "Delete" button visible

**As Admin:**
1. Login as Admin, navigate to any prospect
2. Click "Delete" button
3. **Expected:** Confirmation dialog appears
4. Click "Cancel"
5. **Expected:** Dialog closes, no deletion
6. Click "Delete" again and confirm
7. **Expected:**
   - Success toast "Prospect deleted successfully"
   - Redirect to `/dashboard/prospects` list
8. Check database: `deletionStatus = true`, `protectionStatus = UNPROTECTED`

#### Scenario 4: Request Conversion (Associate/Licensee)

**As Associate (Owner):**
1. Login as Associate, navigate to owned prospect (type = PROSPECT)
2. Click "Request Client Conversion" button
3. **Expected:**
   - Success toast "Conversion request submitted successfully"
   - Button disappears (replaced with "Conversion request pending" text)
4. Click button again
5. **Expected:** Button is hidden (no duplicate request)

**As Admin:**
1. Login as Admin, navigate to `/dashboard/alerts` (to be implemented)
2. **Expected:** Alert appears with type `PROSPECT_CONVERSION_REQUEST`
3. Click "Approve"
4. **Expected:**
   - Alert status changes to RESOLVED
   - Prospect type changes to CLIENT
5. Return to prospect detail page
6. **Expected:**
   - Type shows "CLIENT"
   - "Request Client Conversion" button no longer visible

**Error Cases:**
1. Try to request conversion on a CLIENT
   - **Expected:** Button not visible (frontend condition)
2. Try to request conversion on non-owned prospect
   - **Expected:** Button not visible

#### Scenario 5: Request Protection Extension (Associate/Licensee)

**As Licensee (Owner):**
1. Login as Licensee, navigate to owned prospect with:
   - `status = PROTECTED`
   - `protectionPeriodMonths` exists
   - No pending extension request
2. Check Protection Status Card displays:
   - Color-coded chip (green/yellow/orange/red)
   - Days remaining
   - Progress bar
3. Click "Request Protection Extension" button
4. **Expected:**
   - Success toast "Protection extension request submitted successfully"
   - Button disappears
5. Check database: Alert created with type `PROTECTION_EXTENSION_REQUEST`

**As Admin:**
1. Login as Admin, navigate to `/dashboard/alerts`
2. **Expected:** Alert appears with type `PROTECTION_EXTENSION_REQUEST`
3. Click "Approve"
4. Navigate to prospect detail page
5. Click "Edit"
6. Update `protectionPeriodMonths` field (e.g., from 12 to 15)
7. Save changes
8. **Expected:**
   - Protection Status Card updates with new expiry date
   - Progress bar reflects new timeline
   - Days remaining recalculated

**Error Cases:**
1. Try to request extension on UNPROTECTED prospect
   - **Expected:** Button not visible
2. Try to request extension twice
   - **Expected:** Button hidden after first request

#### Scenario 6: Handle Provisional Prospect (Admin Only)

**Setup:**
1. Create a prospect with city mismatch to trigger provisional status

**As Admin:**
1. Login as Admin, navigate to provisional prospect
2. **Expected:**
   - Yellow warning banner: "Provisional Prospect — Awaiting Admin Approval"
   - Provision reason displayed
   - Three buttons: "Approve", "Reject", "Ignore"
3. Click "Approve"
4. **Expected:**
   - Success toast "Provisional prospect approved"
   - Banner disappears
   - Status changes to PROTECTED
5. Create another provisional prospect
6. Click "Reject"
7. **Expected:**
   - Success toast "Provisional prospect rejected"
   - Redirect to prospects list
   - Prospect soft deleted in database
8. Create another provisional prospect
9. Click "Ignore"
10. **Expected:**
    - Success toast "Provisional prospect ignored"
    - Banner remains (status unchanged)
    - Alert marked as resolved

**As Licensee:**
1. Login as Licensee, navigate to provisional prospect they own
2. **Expected:** Warning banner visible but NO action buttons

### Validation Checklist

- [ ] All roles can view complete prospect details without ownership restrictions
- [ ] Action buttons conditionally shown based on role + ownership
- [ ] Form validation prevents invalid data submission
- [ ] Ownership calculation correctly compares userId with licenseeId/associateId
- [ ] Protection Status Card shows color-coded visual status
- [ ] Conversion requests create alerts viewable on alerts page
- [ ] Extension requests create alerts viewable on alerts page
- [ ] Admins can approve/reject conversion and extension requests
- [ ] Provisional prospects display warning banner for admins
- [ ] Provisional approval/reject/ignore actions update prospect status correctly
- [ ] Soft delete doesn't remove data from database
- [ ] No duplicate requests allowed (conversion/extension)
- [ ] API returns proper error messages for unauthorized actions

### API Testing (Postman/cURL)

**Get Prospect Details:**
```bash
GET http://localhost:8080/api/prospects/24
Headers: Authorization: Bearer {token}
```

**Edit Prospect:**
```bash
PUT http://localhost:8080/api/prospects/24
Headers: Authorization: Bearer {token}
Body: {
  "companyName": "Updated Company Name",
  "email": "updated@email.com",
  ...
}
```

**Delete Prospect:**
```bash
DELETE http://localhost:8080/api/prospects/24
Headers: Authorization: Bearer {token}
```

**Request Conversion:**
```bash
POST http://localhost:8080/api/prospects/24/convert
Headers: Authorization: Bearer {token}
```

**Request Extension:**
```bash
POST http://localhost:8080/api/prospects/24/extension-request
Headers: Authorization: Bearer {token}
```

**Handle Provisional:**
```bash
PUT http://localhost:8080/api/prospects/provisional/{alertId}?decision=APPROVE
Headers: Authorization: Bearer {token}
```

---

## Related Documentation

- **CLAUDE.md** — Project-level architecture and stage requirements
- **API_REFERENCE.md** — Complete API endpoint documentation
- **docs/superpowers/plans/2026-04-19-prospect-detail-enhancements.md** — Implementation plan for prospect detail page

---

## Future Enhancements (Not Yet Implemented)

1. **Alerts Page:**
   - Unified alert dashboard with filtering by type
   - Direct approve/reject actions from alert list
   - Alert detail view with full context
   - Real-time alert notifications

2. **Meeting Tracking:**
   - Record first meeting date
   - Track subsequent meetings
   - Meeting resets protection timeline
   - 45-day first-meeting warning system

3. **Protection Extension Approval:**
   - Automated extension flow from alerts page
   - Admin specifies extension duration in months
   - Instant protectionPeriodMonths update without manual edit

4. **Advanced Ownership:**
   - Transfer ownership between licensees
   - Multi-licensee ownership support
   - Associate reassignment to different licensee

---

**Document Version:** 1.0
**Last Updated:** 2026-04-19
**Author:** LMI CRM Development Team
