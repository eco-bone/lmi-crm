# CLAUDE.md — LMI Solutions CRM Portal

## Project Overview

The LMI CRM Portal is an internal web application for managing prospects, clients, training groups, associates, and licensees. Its central purpose is **prospect ownership protection** — ensuring only one representative pursues a prospect at a time, preventing conflicts and duplication across the sales organisation.

The lifecycle tracked is: **Prospect → Client → Group Training Program**

---

## Architecture & Build Stages

The backend is organised into 9 sequential stages. Each stage must be completed before moving to the next, as later stages depend on services built earlier.

**Stage 0 — Foundation:** Entities, enums, repositories, mappers. Also build `NotificationService` (email only) and `AlertService.createAlert()` here, as nearly every subsequent stage depends on them.

**Stage 1 — User Management:** Licensee and associate CRUD, invite flow, deactivation, role conversion (associate → licensee), and admin management (Super Admin only).

**Stage 2 — Prospects:** The most complex stage. Includes fuzzy matching, provisional logic, city checks, protection initialisation, and conversion requests.

**Stage 3 — Protection Scheduler:** Four daily background jobs covering first-meeting deadlines, expiry after no first meeting, grace period entry (LI/SI), and final unprotection.

**Stage 4 — Groups:** Group CRUD scoped to clients only (prospect.type must equal `CLIENT`).

**Stage 5 — Alerts:** Admin-only alert dashboard with paginated listing, detail view, and a unified `performAlertAction` endpoint that delegates to the correct service based on alert type.

**Stage 6 — Resources:** Admin uploads (S3 for files, YouTube URL for ZCDC). Read-only access for all authenticated users.

**Stage 7 — Tasks & Notes:** Personal productivity module. Tasks have a 30-minute reminder scheduler. Notes have no reminders.

**Stage 8 — Reports & Data:** Dashboard stats (role-scoped), report generation (TODAY / WEEKLY), and one-time Excel import.

**Stage 9 — Auth:** JWT infrastructure, login, full registration/OTP chain, and `@PreAuthorize` guards on all controllers. Do not add auth guards until this stage is reached.

---

## User Roles & Key Permissions

| Capability | Associate | Licensee | Admin | Super Admin |
|---|---|---|---|---|
| Add prospects | ✓ | ✓ | ✓ | — |
| View prospect list (limited fields) | ✓ | ✓ | Full | — |
| Delete/unprotect prospect | ✗ | ✓ | ✓ | — |
| Request conversion | ✓ | ✓ | Approves | — |
| Add licensees | ✗ | ✗ | ✓ | — |
| Approve associate requests | ✗ | ✗ | ✓ | — |
| Manage admins | ✗ | ✗ | ✗ | ✓ |
| View audit logs | ✗ | ✗ | ✓ | ✓ |

Associates see only their own prospects and clients. Licensees see all prospects and clients linked to them. Admins see everything. Role visibility is enforced inside the service layer using `SecurityContext.currentUser()` — not via a `requestingUserId` parameter.

---

## Core Domain Rules

### Prospect Creation & Duplicate Detection
- Fuzzy match on company name uses 65% threshold (regex-based).
- Dropdown suggestions appear after the first 2 characters; format is `Company Name – City`.
- If `prospect_type != SHC` and `prospect_city != licensee_city`, flag as **PROVISIONAL** and create an admin alert.
- If contact first + last name already exists for the same company, block the entry.
- Admin can Approve / Reject / Ignore provisional entries.

### Protection System
Protection is automatic on prospect creation. Rules by program type:

| Program | Base Duration | Grace Period | Expiry |
|---|---|---|---|
| LI (Large In-house) | 12 months | 3 months | Yes |
| SI (Small In-house) | 6 months | 3 months | Yes |
| O2O (One-to-One) | Perpetual | — | No |
| SHC (Showcase) | Perpetual | — | No |

**First-meeting window:** 45 days from entry. Warning email at day 45. Unprotected at day 75 if still no meeting.

After the first meeting, every new meeting resets the protection timeline. For LI/SI, no activity for the full base duration triggers grace period entry and an alert. No activity during the grace period results in the prospect becoming **UNPROTECTED**.

Admin can manually extend protection (via `protectionPeriodMonths` field on `updateProspect`).

### Prospect → Client Conversion
Associates and Licensees cannot convert directly. They submit a conversion request which creates an alert. Admin approves or rejects via `/admin/alerts`. On approval, `prospects.type` changes from `PROSPECT` to `CLIENT`.

### Deactivation Cascade
- **Associate deactivated:** All prospects transfer to the parent Licensee.
- **Licensee deactivated:** All prospects and clients transfer to MLO (Master Licensee), and the licensee name field on all records is replaced with "MLO". MLO is added by Admin as a licensee.

---

## API Conventions

- All endpoints are prefixed `/api/`.
- Standard REST patterns: `POST` to create, `GET` to read, `PUT` to update, `DELETE` to delete.
- Soft deletes are used throughout (`deletionStatus = true`). No hard deletes except for notes and tasks.
- Role-scoped endpoints enforce visibility in the service layer, not the controller.
- Admin-only fields (e.g. `protectionStatus`, `protectionPeriodMonths`, licensee reassignment) are validated inside `updateProspect` — they are not separate endpoints.
- The three protection management operations (extend, override, reassign) are all handled via `PUT /api/prospects/{id}`.

---

## Notification Service

`NotificationService` is built in Stage 0 and called throughout. All methods:

| Method | Triggered By |
|---|---|
| `sendInviteEmail(user, token)` | `addLicensee`, `approveAssociateCreation`, `Auth.sendInvite` |
| `sendOtpEmail(email, otp)` | `Auth.sendEmailOtp` |
| `sendOtpSms(phone, otp)` | `Auth.sendPhoneOtp` |
| `sendAdminAlertEmail(alert)` | `AlertService.createAlert` — fires on every new alert |
| `sendProtectionWarningEmail(...)` | `ProtectionScheduler` |
| `sendTaskReminderEmail(user, task)` | `TaskReminderScheduler` (every 5 min, 30-min lookahead) |
| `sendWeeklyReportEmail(admins, report)` | `ReportScheduler` (Monday 8 AM) |

Email notifications to admin on alerts contain only a summary and a link to `/admin/alerts`. **No action buttons in email** — all approval actions happen inside the dashboard.

---

## Alert System

Alerts are created by `AlertService.createAlert()` for the following events: duplicate/provisional prospect, conversion request, associate creation/deactivation request, protection warnings, grace period entry, unprotection, and ownership claim requests.

All alert actions (Approve / Reject / Ignore) are performed via `performAlertAction` on `PUT /api/alerts/{alertId}/action`, which delegates internally based on `alertType`. Alert lifecycle: `Pending → Resolved / Rejected`.

---

## Scheduler Jobs

| Job | Schedule | Action |
|---|---|---|
| `checkFirstMeetingDeadlines` | Daily | Warn at day 45 (no first meeting) |
| `expireFirstMeetingProtection` | Daily | Unprotect at day 75 (still no meeting) |
| `checkActivityDeadlines` (LI/SI) | Daily | No activity past base duration → grace period |
| `expireAfterGracePeriod` (LI/SI) | Daily | No activity in grace period → UNPROTECTED |
| `TaskReminderScheduler` | Every 5 min | Email + alert for tasks due within 30 min |
| `ReportScheduler` | Monday 8 AM | Weekly summary email to all admins |

---

## What Not To Do

- Do not add `@PreAuthorize` guards or pull from `SecurityContext` until Stage 9 is reached. Use a `requestingUserId` parameter as a placeholder in earlier stages.
- Do not allow groups to be linked to prospects — only clients (`prospect.type == CLIENT`).
- Do not allow associates to invite other associates or approve any requests.
- Do not hard-delete prospects. Deletion sets `deletionStatus = true` and `protectionStatus = UNPROTECTED`.
- Do not expose admin-only fields (protection override, licensee reassignment) to associates or licensees.
- The Excel import (`POST /api/admin/import`) is a one-time operation — do not build it as a recurring workflow.