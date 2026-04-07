# LMI Solutions CRM Portal — Backend (Spring Boot)

## About This Document

This is the primary reference document for the LMI CRM backend project. It serves as persistent context for the entire development process — covering architecture decisions, conventions, business logic, and the full API surface. When working on any part of this project, this document is the source of truth.

## Project Overview

Internal CRM for LMI Solutions. Manages Prospects, Clients, Training Groups, Associates, and Licensees. Core business logic revolves around **prospect ownership protection**, **duplicate prevention**, and **role-based access control**.

The backend exposes a REST API only. The frontend is a separate project handled by a different developer.

---

## Tech Stack

- **Framework:** Spring Boot (Maven, NOT Gradle)
- **Language:** Java 17+
- **Database:** PostgreSQL via Supabase (free tier) — for development
- **ORM:** Spring Data JPA (Hibernate)
- **Security:** Spring Security + JWT
- **Email:** Spring Mail (SMTP)
- **File Storage:** AWS S3 (for resource uploads)
- **Build Tool:** Maven (`pom.xml`)
- **Testing:** JUnit 5 + Mockito

---

## Lombok Conventions

Lombok is used throughout this project in place of all boilerplate. Getters, setters, constructors, builders, and loggers are all annotation-driven.

**Entities:** `@Getter` + `@Setter` + `@Builder` + `@NoArgsConstructor` + `@AllArgsConstructor`
- `@Data` is avoided on entities with relationships, as it causes infinite loops in `toString`/`hashCode`

**DTOs (request + response):** `@Data` + `@Builder` + `@NoArgsConstructor` + `@AllArgsConstructor`

**Services / components:** `@RequiredArgsConstructor` — dependencies are declared as `private final` fields and Lombok generates the constructor. `@Autowired` is not used anywhere in this project.

**Logging:** `@Slf4j` — provides a `log` variable without any manual declaration.



---

## Maven Dependencies (`pom.xml`)

Core: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-security`, `spring-boot-starter-validation`, `spring-boot-starter-mail`

Database: `postgresql` (runtime scope)

JWT: `jjwt-api`, `jjwt-impl`, `jjwt-jackson`

AWS S3: `aws-java-sdk-s3`

Excel import: `apache-poi` (`poi-ooxml`)

Utilities: `lombok`, `mapstruct`, `jackson-databind`

---

## Project Package Structure

All code lives under `com.lmi.crm` with the following sub-packages:

- `config` — Security, CORS, S3, Mail, JWT configuration classes
- `enums` — All enums, mirroring the DB schema exactly
- `entity` — JPA entities, one per DB table
- `repository` — Spring Data JPA repositories
- `dto/request` — Incoming request body classes
- `dto/response` — Outgoing response body classes
- `service/crud` — Thin CRUD services, one per entity
- `service/business` — Business logic services; each is split into an interface and an `Impl` class (see Service / Implementation Pattern below)
- `controller` — REST controllers, thin layer delegating to business services
- `security` — JWT filter and `UserDetailsService` implementation
- `exception` — Custom exception classes and `GlobalExceptionHandler`
- `scheduler` — Scheduled jobs for protection expiry and task reminders
- `util` — Utility classes (`FuzzyMatchUtil`, `OtpUtil`, `DateUtil`, etc.)

---

## Service / Implementation Pattern

All business services (Phase 5) follow a strict interface + implementation split:

- The interface (e.g. `ProspectService`) lives in `service/business/` and declares all public method signatures
- The implementation (e.g. `ProspectServiceImpl`) lives in `service/business/impl/` and carries the `@Service` annotation
- CRUD services (Phase 4) follow the same pattern: interface in `service/crud/`, implementation in `service/crud/impl/`
- Controllers and other services always depend on the interface type, never on the `Impl` class directly

This keeps dependencies loosely coupled and makes the contracts explicit.

---

## Build Order

### Phase 1 — Enums

All enums live in `com.lmi.crm.enums`. Each enum maps directly to the DB schema.

1. `UserRole` — `ADMIN, LICENSEE, ASSOCIATE, MASTER_LICENSEE, SUPER_ADMIN`
2. `UserStatus` — `ACTIVE, INACTIVE`
3. `ClassificationType` — `AAA, AA, A, B, C`
4. `ProspectProgramType` — `LI, SI, ONE_TO_ONE, SHC`
5. `ProspectType` — `PROSPECT, CLIENT`
6. `ProspectStatus` — `NORMAL, PROVISIONAL`
7. `ProtectionStatus` — `PROTECTED, UNPROTECTED, PROVISIONAL`
8. `GroupProgramType` — `EPP, ELD, EPL, ECE, EML, ESL, LFW, AIE`
9. `DeliveryType` — `ONLINE, HYBRID, IN_PERSON`
10. `AlertStatus` — `PENDING, RESOLVED, REJECTED`
11. `AlertType` — `DUPLICATE_PROSPECT, PROSPECT_CONVERSION_REQUEST, ASSOCIATE_CREATION_REQUEST, ASSOCIATE_DEACTIVATION_REQUEST, LICENSEE_DEACTIVATION_REQUEST, PROSPECT_PROTECTION_WARNING, PROSPECT_UNPROTECTED, OWNERSHIP_CLAIM_REQUEST`
12. `RelatedEntityType` — `PROSPECT, USER, GROUP, SYSTEM`
13. `TaskStatus` — `PENDING, COMPLETED`
14. `ResourceType` — `ZCDC, DOCUMENT, PPT`
15. `FileType` — `YOUTUBE, PDF, DOC, XLS, PPT`
16. `AuditActionType` — (full list below, use exact names from schema)

**`AuditActionType` values:**
```
USER_REGISTERED,
PROSPECT_CREATED, PROSPECT_UPDATED, PROSPECT_DUPLICATE_ATTEMPT,
PROSPECT_UNPROTECTED, PROSPECT_CONVERSION_REQUESTED,
PROSPECT_CONVERSION_APPROVED, PROSPECT_CONVERSION_REJECTED,
PROVISIONAL_APPROVED, PROVISIONAL_REJECTED,
GROUP_CREATED, GROUP_UPDATED, GROUP_DELETED,
LICENSEE_CREATED, LICENSEE_DEACTIVATED,
ASSOCIATE_CREATED, ASSOCIATE_DEACTIVATED,
ASSOCIATE_REQUEST_APPROVED, ASSOCIATE_REQUEST_REJECTED,
ADMIN_CREATED, ADMIN_UPDATED, ADMIN_DEACTIVATED,
RESOURCE_UPDATED, RESOURCE_DELETED
```

---

### Phase 2 — Entities

JPA entities live in `com.lmi.crm.entity`. Enums are mapped with `@Enumerated(EnumType.STRING)`. `@CreationTimestamp` / `@UpdateTimestamp` handle audit fields. Entities with relationships use `@Getter` + `@Setter` rather than `@Data` to avoid `toString`/`hashCode` loops — see the Lombok Conventions section.

#### 1. `User`
```
Table: users
Fields: id, firstName, lastName, email (unique), phone (unique), password, role (UserRole), licenseeId (FK → users.id, nullable), status (UserStatus), createdAt, updatedAt
Note: Self-referential FK for licenseeId. Associates point to their Licensee's user ID.
```

#### 2. `LicenseeCity`
```
Table: licensee_cities
Fields: id, licenseeId (FK → users.id), city, isPrimary (default false), createdAt
Note: Unique constraint on (licenseeId, city, isPrimary)
```

#### 3. `Prospect`
```
Table: prospects
Fields: id, companyName, city, contactFirstName, contactLastName, designation, email (unique), phone, referredBy, classificationType (ClassificationType), programType (ProspectProgramType), type (ProspectType), associateId (FK → users.id), firstMeetingDate (Date), lastMeetingDate (Date), entryDate (Date), protectionStatus (ProtectionStatus), protectionPeriodMonths (int), deletionStatus (boolean, default false), createdBy (FK → users.id), createdAt, updatedAt
```

#### 4. `Group`
```
Table: groups
Fields: id, licenseeId (FK → users.id), facilitatorId (FK → users.id), groupSize (int), groupType (GroupProgramType), deliveryType (DeliveryType), startDate (Date), ppmTfeDateSent (Date), createdBy (FK → users.id), deletionStatus (boolean, default false), createdAt, updatedAt
```

#### 5. `Meeting`
```
Table: meetings
Fields: id, prospectId (FK → prospects.id), pointOfContact (varchar), description (text), meetingAt (timestamp), createdBy (FK → users.id), createdAt, updatedAt
```

#### 6. `ProspectLicensee`
```
Table: prospect_licensees
Fields: id, prospectId (FK → prospects.id), licenseeId (FK → users.id), isPrimary (boolean, default false), assignedAt (timestamp)
Unique constraint: (prospectId, licenseeId)
```

#### 7. `GroupProspect`
```
Table: group_prospects
Fields: id, groupId (FK → groups.id), prospectId (FK → prospects.id), createdAt
Unique constraint: (groupId, prospectId)
```

#### 8. `Alert`
```
Table: alerts
Fields: id, alertType (AlertType), title, description, relatedEntityType (RelatedEntityType), relatedEntityId (int), triggeredBy (FK → users.id, nullable for SYSTEM), status (AlertStatus, default PENDING), actionRequired (boolean), createdAt
```

#### 9. `UserItem` (Tasks + Notes combined)
```
Table: user_items
Fields: id, userId (FK → users.id), type (UserItemType enum: TASK/NOTE), title, description, dueDate (timestamp, nullable — only for tasks), taskStatus (TaskStatus, nullable — only for tasks), createdAt, updatedAt
```

#### 10. `Resource`
```
Table: resources
Fields: id, title, description, resourceType (ResourceType), fileType (FileType), fileUrl, uploadedBy (FK → users.id), createdAt, updatedAt
```

#### 11. `AuditLog`
```
Table: audit_logs
Fields: id, actionType (AuditActionType), entityType (RelatedEntityType), entityId (int), performedBy (int, nullable), previousState (JSON → String), newState (JSON → String), metadata (JSON → String), createdAt
Note: immutable — no updatedAt. No cascade deletes. Store JSON as String or use @Column(columnDefinition="jsonb").
```

---

### Phase 3 — Repositories

One repository per entity, all in `com.lmi.crm.repository`, extending `JpaRepository<Entity, Integer>`. Custom query methods are added only where needed. `@Query` (JPQL or native) is used where Spring Data method naming is not expressive enough.

#### `UserRepository`
Custom queries: look up by email, by phone, or by either (for login); filter users by licenseeId + status; filter by role + status; find all by role.

#### `LicenseeCityRepository`
Custom queries: fetch all cities for a licensee; fetch the primary city for a licensee; find licensees by city; check if a (licenseeId, city) pair already exists.

#### `ProspectRepository`
Custom queries: find by associateId; find by protectionStatus; find by type (PROSPECT/CLIENT); find by exact companyName + city; find companies whose name starts with a given prefix (case-insensitive, for the duplicate dropdown); find all prospects linked to a licensee via the `prospect_licensees` join table; find protected prospects with no first meeting older than a given cutoff date; find protected LI/SI prospects with no meeting activity since a given cutoff date.

#### `GroupRepository`
Custom queries: find by licenseeId; find by facilitatorId; find all non-deleted groups.

#### `MeetingRepository`
Custom queries: find all meetings for a prospect; find the most recent meeting for a prospect; find all meetings for a prospect in chronological order.

#### `ProspectLicenseeRepository`
Custom queries: find all licensee assignments for a prospect; find all prospects assigned to a licensee; find the primary licensee for a prospect; check whether a (prospectId, licenseeId) assignment already exists.

#### `GroupProspectRepository`
Custom queries: find all prospects in a group; find all groups a prospect belongs to; check whether a (groupId, prospectId) link already exists.

#### `AlertRepository`
Custom queries: find all alerts by status, newest first; find alerts by type + status; find all alerts newest first; paginated filtering by type + status.

#### `UserItemRepository`
Custom queries: find all items for a user by type (TASK or NOTE); find tasks for a user filtered by status; find pending tasks with a due date falling within a given time window (used by the reminder scheduler).

#### `ResourceRepository`
Custom queries: find resources by type; find all resources sorted newest first.

#### `AuditLogRepository`
Custom queries: find all logs for a specific entity (by type + id); find all actions performed by a specific user; paginated lookup by action type.

---

### Phase 4 — CRUD Services

One CRUD service per entity in `com.lmi.crm.service.crud`. These services are intentionally thin — no business logic lives here. Business services (Phase 5) call into these.

Each CRUD service covers:
- `save(entity)` / `saveAll(list)`
- `findById(id)` — throws `ResourceNotFoundException` if not found
- `findAll()` with any relevant filters
- `delete(id)` / soft-delete where applicable
- `existsById(id)`
- Wrappers around custom repository queries from Phase 3

**CRUD Services to create:**
1. `UserCrudService`
2. `LicenseeCityCrudService`
3. `ProspectCrudService`
4. `GroupCrudService`
5. `MeetingCrudService`
6. `ProspectLicenseeCrudService`
7. `GroupProspectCrudService`
8. `AlertCrudService`
9. `UserItemCrudService`
10. `ResourceCrudService`
11. `AuditLogCrudService`

---

### Phase 5 — Business Services

Business services live in `com.lmi.crm.service.business`. This is where the actual application logic lives. Each method maps to one or more API endpoints. Repositories are not injected directly here — all data access goes through the CRUD services.

---

#### `AuthService`

```
login(email/phone, password)
  → validate credentials, generate JWT, return token + user info

sendInvitationEmail(targetEmail, role, invitedByUserId)
  → generate unique registration token, store it, send email with link + temp password

registerUser(token, firstName, lastName, phone)
  → validate token, create user with INACTIVE status until OTP verified

sendEmailOtp(email)
  → generate 6-digit OTP, store with expiry, send via Spring Mail

sendPhoneOtp(phone)
  → generate 6-digit OTP, store with expiry, send via SMS gateway

verifyOtp(identifier, otp, type)
  → validate OTP, mark email/phone as verified
  → if both verified, activate user account

changePassword(userId, oldPassword, newPassword)
  → validate old password, hash and store new password
```

---

#### `ProspectService`

```
addProspect(request, currentUser)
  → Run duplicate check (fuzzy match on company name, first 2 letters → dropdown)
  → Check: same contact firstName + lastName + same company → reject
  → Check city vs licensee city (if programType != SHC) → mark PROVISIONAL + create Alert
  → Check fuzzy match ≥ 65% → mark PROVISIONAL + create Alert
  → Set protectionStatus = PROTECTED, entryDate = today
  → Calculate protectionPeriodMonths from programType:
      LI → 12, SI → 6, ONE_TO_ONE → null (perpetual), SHC → null (perpetual)
  → Save, create AuditLog (PROSPECT_CREATED)

searchCompanyName(prefix)
  → Query prospects where companyName starts with first 2 chars of prefix
  → Return list of {companyName, city} (for duplicate dropdown)

getProspects(filters, currentUser)
  → ADMIN/SUPER_ADMIN: all prospects
  → LICENSEE: prospects in prospect_licensees where licenseeId = currentUser
  → ASSOCIATE: prospects where associateId = currentUser
  → Filter by type (PROSPECT/CLIENT/ALL)
  → Return limited fields for ASSOCIATE/LICENSEE: firstName, lastName, designation, company, city, status

getProspectDetail(id, currentUser)
  → Enforce visibility: associate sees own only, licensee sees own only, admin sees all

updateProspect(id, request, currentUser)
  → Validate permissions
  → Admin-only fields: protectionStatus override, protectionPeriodMonths extension, licensee replacement
  → Create AuditLog (PROSPECT_UPDATED) with previousState and newState

softDeleteProspect(id, currentUser)
  → LICENSEE only (not associate, not admin directly)
  → Set deletionStatus = true, protectionStatus = UNPROTECTED
  → AuditLog (PROSPECT_UNPROTECTED)

requestConversion(prospectId, currentUser)
  → Only ASSOCIATE or LICENSEE
  → Create Alert (PROSPECT_CONVERSION_REQUEST)
  → Send email to all admins
  → AuditLog (PROSPECT_CONVERSION_REQUESTED)

approveRejectConversion(alertId, decision, currentUser)
  → ADMIN only
  → If APPROVE: update prospect.type = CLIENT, resolve alert
  → If REJECT: reject alert
  → AuditLog (PROSPECT_CONVERSION_APPROVED or REJECTED)

approveRejectProvisional(alertId, decision, currentUser)
  → ADMIN only
  → APPROVE → prospect.protectionStatus = PROTECTED, status = NORMAL
  → REJECT → soft delete prospect
  → IGNORE → leave as PROVISIONAL
  → AuditLog (PROVISIONAL_APPROVED or REJECTED)

reassignProspectOwnership(prospectId, newLicenseeId, currentUser)
  → ADMIN only
  → Update prospect_licensees: remove old primary, set new
  → AuditLog (PROSPECT_UPDATED with metadata: ownership transfer)
```

---

#### `ProtectionService`

```
// Called by scheduler, not directly by API

checkFirstMeetingDeadlines()
  → Find all PROTECTED prospects where firstMeetingDate IS NULL and entryDate <= today - 45 days
  → Send warning email to licensee + associate
  → Create Alert (PROSPECT_PROTECTION_WARNING)

expireFirstMeetingProtection()
  → Find all PROTECTED prospects where firstMeetingDate IS NULL and entryDate <= today - 75 days
  → Set protectionStatus = UNPROTECTED
  → Create Alert (PROSPECT_UNPROTECTED)
  → AuditLog (PROSPECT_UNPROTECTED)

checkActivityDeadlines()
  → For LI: find prospects where lastMeetingDate <= today - 12 months and protectionStatus = PROTECTED
  → For SI: find prospects where lastMeetingDate <= today - 6 months and protectionStatus = PROTECTED
  → Send grace period warning, create Alert

expireAfterGracePeriod()
  → For LI: lastMeetingDate <= today - 15 months (12 + 3)
  → For SI: lastMeetingDate <= today - 9 months (6 + 3)
  → Set protectionStatus = UNPROTECTED
  → AuditLog, Alert

extendProtectionPeriod(prospectId, extensionMonths, currentUser)
  → ADMIN only
  → Update protectionPeriodMonths
  → AuditLog (PROSPECT_UPDATED)

overrideProtectionStatus(prospectId, newStatus, currentUser)
  → ADMIN only
  → Update protectionStatus directly
  → AuditLog

requestOwnershipClaim(prospectId, claimingLicenseeId)
  → Prospect must be UNPROTECTED
  → Create Alert (OWNERSHIP_CLAIM_REQUEST)
  → Notify admin

approveRejectOwnershipClaim(alertId, decision, currentUser)
  → ADMIN only
  → If approve: reassign prospect_licensees
  → Resolve alert, AuditLog
```

---

#### `GroupService`

```
addGroup(clientProspectId, request, currentUser)
  → Validate prospect.type == CLIENT (groups only for clients)
  → Save group, link via group_prospects
  → AuditLog (GROUP_CREATED)

getGroups(filters)
  → Filter by licenseeId if LICENSEE/ASSOCIATE
  → Return list with pagination

getGroupDetail(id)
  → Return full group + linked prospects

updateGroup(id, request, currentUser)
  → Validate permissions, update fields
  → AuditLog (GROUP_UPDATED)

deleteGroup(id, currentUser)
  → Soft delete (deletionStatus = true)
  → AuditLog (GROUP_DELETED)
```

---

#### `UserManagementService`

```
addLicensee(request, currentUser)
  → ADMIN/SUPER_ADMIN only
  → Create user with role=LICENSEE, send invitation email
  → AuditLog (LICENSEE_CREATED)

deactivateLicensee(licenseeId, currentUser)
  → ADMIN only
  → Find all prospects via prospect_licensees → reassign to MLO
  → Set user.status = INACTIVE
  → AuditLog (LICENSEE_DEACTIVATED)

requestAssociateCreation(request, requestingLicenseeId)
  → LICENSEE only
  → Create Alert (ASSOCIATE_CREATION_REQUEST)
  → Send email to admin

approveRejectAssociateCreation(alertId, decision, currentUser)
  → ADMIN only
  → If approve: create user with role=ASSOCIATE, send invitation
  → AuditLog (ASSOCIATE_REQUEST_APPROVED or REJECTED)

deactivateAssociate(associateId, currentUser)
  → ADMIN only (direct), or triggered via approved deactivation request
  → Transfer all prospects to associate's parent licensee
  → Set user.status = INACTIVE
  → AuditLog (ASSOCIATE_DEACTIVATED)

requestAssociateDeactivation(associateId, requestingLicenseeId)
  → LICENSEE only
  → Create Alert (ASSOCIATE_DEACTIVATION_REQUEST)
  → Send email to admin

approveRejectAssociateDeactivation(alertId, decision, currentUser)
  → ADMIN only
  → If approve: call deactivateAssociate()
  → AuditLog

convertAssociateToLicensee(associateId, currentUser)
  → ADMIN only
  → Update user.role = LICENSEE, clear licenseeId
  → AuditLog (ADMIN_UPDATED)

getUsers(roleFilter, statusFilter, currentUser)
  → ADMIN: all
  → LICENSEE: own associates only
  → Return paginated list

getUserDetail(userId, currentUser)
  → Enforce access rules

resetPassword(userId, newPassword, currentUser)
  → ADMIN only
  → Hash and store new password

createAdmin(request, currentUser)
  → SUPER_ADMIN only
  → Create user with role=ADMIN
  → AuditLog (ADMIN_CREATED)

deactivateAdmin(adminId, currentUser)
  → SUPER_ADMIN only
  → AuditLog (ADMIN_DEACTIVATED)

getAdmins(currentUser)
  → SUPER_ADMIN only

updateAdmin(adminId, request, currentUser)
  → SUPER_ADMIN only
  → AuditLog (ADMIN_UPDATED)
```

---

#### `ResourceService`

```
uploadResource(request, file, currentUser)
  → ADMIN only
  → If file (PDF/DOC/XLS/PPT): upload to S3, get URL
  → If ZCDC: store YouTube URL directly
  → Save resource entity
  → AuditLog: Note — schema has RESOURCE_UPDATED and RESOURCE_DELETED but not RESOURCE_CREATED; log as RESOURCE_UPDATED for creation

getResources(typeFilter)
  → All authenticated users can access
  → Filter by resourceType if provided

getResourceDetail(id)
  → Return full resource entity

downloadResource(id)
  → Generate presigned S3 URL for file resources
  → Return YouTube link for ZCDC resources

updateResource(id, request, currentUser)
  → ADMIN only
  → AuditLog (RESOURCE_UPDATED)

deleteResource(id, currentUser)
  → ADMIN only
  → Remove from S3 if applicable
  → AuditLog (RESOURCE_DELETED)
```

---

#### `TaskNoteService`

```
createTask(request, currentUser)
  → Validate dueDate is future
  → Save UserItem with type=TASK, taskStatus=PENDING

updateTask(id, request, currentUser)
  → Validate ownership (userId must match)
  → Update fields including markAsCompleted (taskStatus=COMPLETED)

deleteTask(id, currentUser)
  → Validate ownership, delete

getTasks(currentUser, statusFilter)
  → Return tasks for currentUser only

getTaskDetail(id, currentUser)
  → Validate ownership

createNote(request, currentUser)
  → Save UserItem with type=NOTE

updateNote(id, request, currentUser)
  → Validate ownership

deleteNote(id, currentUser)
  → Validate ownership

getNotes(currentUser)
  → Return notes for currentUser only

getNoteDetail(id, currentUser)
  → Validate ownership
```

---

#### `AlertService`

```
createAlert(alertType, title, description, entityType, entityId, triggeredBy)
  → Internal utility called by other services — not a direct API
  → Save Alert entity with status=PENDING
  → Send notification email to all admins: "New alert in LMI CRM: [title] — log in to review"

getAlerts(typeFilter, statusFilter)
  → ADMIN only
  → Return paginated, sorted by createdAt desc

getAlertDetail(id)
  → ADMIN only

resolveAlert(id, decision, currentUser)
  → Generic resolver — delegates to specific approve/reject handlers based on alertType
  → Update alert.status = RESOLVED or REJECTED
```

---

#### `ReportService`

```
generateReport(type, currentUser)
  → ADMIN only
  → type: WEEKLY or TODAY
  → Count new licensees, associates, prospects, client conversions, users in period
  → Return structured report DTO

getDashboardStats(currentUser)
  → ASSOCIATE: own prospects count, own clients count
  → LICENSEE: own prospects, own clients, own associates count
  → ADMIN: all prospects, clients, associates, licensees counts
```

---

#### `DataImportService`

```
importExcel(file, currentUser)
  → ADMIN only (one-time use)
  → Parse Excel using Apache POI
  → Sheets expected: Licensees, Associates, Prospects, Clients, Groups
  → For each sheet: validate required fields, create entities in order:
      1. Licensees (create users)
      2. Associates (create users, link to licensee)
      3. Prospects (create, link licensee)
      4. Clients (same as prospects with type=CLIENT)
      5. Groups (create, link to prospect/client)
  → Return import summary: {inserted, skipped, errors[]}
```

---

### Phase 6 — Schedulers

Scheduled jobs live in `com.lmi.crm.scheduler`.

The `ProtectionScheduler` component runs the following jobs:

- Daily at 9 AM: `checkFirstMeetingDeadlines`, `expireFirstMeetingProtection`, `checkActivityDeadlines`, `expireAfterGracePeriod`
- Weekly on Monday at 8 AM: `sendWeeklyReportToAdmins`
- Every 5 minutes: `sendTaskReminders` — finds pending tasks with a due date in the next 30 minutes and sends an email + creates an alert for each

The main application class has `@EnableScheduling` to activate these jobs.

---

### Phase 7 — Controllers

Controllers live in `com.lmi.crm.controller`. They are intentionally thin — auth is validated, the relevant business service is called, and a `ResponseEntity` is returned. Role enforcement is handled via `@PreAuthorize`.

| Controller | Base Path |
|---|---|
| `AuthController` | `/api/auth` |
| `ProspectController` | `/api/prospects` |
| `GroupController` | `/api/groups` |
| `UserController` | `/api/users` |
| `AlertController` | `/api/admin/alerts` |
| `ResourceController` | `/api/resources` |
| `TaskController` | `/api/tasks` |
| `NoteController` | `/api/notes` |
| `ReportController` | `/api/reports` |
| `ImportController` | `/api/admin/import` |

---

## API Endpoint Reference

### Authentication (`/api/auth`)

| # | Method | Endpoint | Description | Roles |
|---|---|---|---|---|
| 1 | POST | `/login` | Login with email/phone + password | PUBLIC |
| 2 | POST | `/invite` | Send invitation email | ADMIN, LICENSEE |
| 3 | POST | `/register` | Register via invitation link | PUBLIC (token-gated) |
| 4 | POST | `/otp/email` | Send email OTP | PUBLIC |
| 5 | POST | `/otp/phone` | Send phone OTP | PUBLIC |
| 6 | POST | `/otp/verify` | Verify OTP | PUBLIC |
| 7 | POST | `/password` | Change or set password | AUTH |

### Prospects & Clients (`/api/prospects`)

| # | Method | Endpoint | Description | Roles |
|---|---|---|---|---|
| 8 | POST | `/` | Add prospect | ASSOCIATE, LICENSEE, ADMIN |
| 9 | GET | `/search?q=` | Company name search (duplicate check dropdown) | AUTH |
| 10 | GET | `/` | Get all prospects/clients with filters | AUTH |
| 11 | GET | `/{id}` | Get prospect/client detail | AUTH |
| 12 | PUT | `/{id}` | Update prospect (includes protection extend, override, reassign for ADMIN) | AUTH |
| 13 | DELETE | `/{id}` | Soft delete / unprotect prospect | LICENSEE, ADMIN |
| 14 | POST | `/{id}/convert` | Request prospect → client conversion | ASSOCIATE, LICENSEE |
| 15 | PUT | `/conversions/{alertId}` | Approve/reject conversion | ADMIN |
| 16 | PUT | `/provisional/{alertId}` | Approve/reject provisional entry | ADMIN |

### Groups (`/api/groups`)

| # | Method | Endpoint | Description | Roles |
|---|---|---|---|---|
| 20 | POST | `/` | Add group to client | AUTH |
| 21 | GET | `/` | Get groups list | AUTH |
| 22 | GET | `/{id}` | Get group detail | AUTH |
| 23 | PUT | `/{id}` | Update group | AUTH |
| 24 | DELETE | `/{id}` | Delete group | AUTH |

### Users (`/api/users`)

| # | Method | Endpoint | Description | Roles |
|---|---|---|---|---|
| 25 | POST | `/licensees` | Add licensee | ADMIN |
| 26 | PUT | `/licensees/{id}/deactivate` | Deactivate licensee | ADMIN |
| 27 | POST | `/associates/request` | Request associate creation | LICENSEE |
| 28 | PUT | `/associates/requests/{alertId}` | Approve/reject associate request | ADMIN |
| 29 | PUT | `/associates/{id}/deactivate` | Deactivate associate (direct) | ADMIN |
| 30 | POST | `/associates/{id}/deactivation-request` | Request associate deactivation | LICENSEE |
| 31 | PUT | `/associates/deactivation-requests/{alertId}` | Approve/reject deactivation request | ADMIN |
| 32 | PUT | `/associates/{id}/convert-to-licensee` | Convert associate to licensee | ADMIN |
| 33 | GET | `/` | Get users list with role/status filter | ADMIN, LICENSEE |
| 34 | GET | `/{id}` | Get user detail | ADMIN, LICENSEE |
| 35 | PUT | `/{id}/reset-password` | Reset password | ADMIN |

### Admin Management (`/api/admin/users`)

| # | Method | Endpoint | Description | Roles |
|---|---|---|---|---|
| 36 | POST | `/` | Create admin | SUPER_ADMIN |
| 37 | PUT | `/{id}/deactivate` | Deactivate admin | SUPER_ADMIN |
| 38 | GET | `/` | Get admins list | SUPER_ADMIN |
| 39 | PUT | `/{id}` | Update admin | SUPER_ADMIN |

### Resources (`/api/resources`)

| # | Method | Endpoint | Description | Roles |
|---|---|---|---|---|
| 40 | POST | `/` | Upload resource | ADMIN |
| 41 | GET | `/` | Get resources list | AUTH |
| 42 | GET | `/{id}` | Get resource detail | AUTH |
| 43 | GET | `/{id}/download` | Download / get presigned URL | AUTH |
| 44 | DELETE | `/{id}` | Delete resource | ADMIN |
| 45 | PUT | `/{id}` | Update resource details | ADMIN |

### Tasks (`/api/tasks`)

| # | Method | Endpoint | Description | Roles |
|---|---|---|---|---|
| 46 | POST | `/` | Create task | AUTH |
| 47 | PUT | `/{id}` | Update task (includes mark complete) | AUTH (owner only) |
| 48 | DELETE | `/{id}` | Delete task | AUTH (owner only) |
| 49 | GET | `/` | Get tasks list | AUTH |
| 50 | GET | `/{id}` | Get task detail | AUTH (owner only) |

### Notes (`/api/notes`)

| # | Method | Endpoint | Description | Roles |
|---|---|---|---|---|
| 52 | POST | `/` | Create note | AUTH |
| 53 | PUT | `/{id}` | Update note | AUTH (owner only) |
| 54 | DELETE | `/{id}` | Delete note | AUTH (owner only) |
| 55 | GET | `/` | Get notes list | AUTH |
| 56 | GET | `/{id}` | Get note detail | AUTH (owner only) |

### Reports & Data (`/api`)

| # | Method | Endpoint | Description | Roles |
|---|---|---|---|---|
| 57 | GET | `/reports` | Generate report (query param: type=WEEKLY\|TODAY) | ADMIN |
| 58 | POST | `/admin/import` | Import Excel data | ADMIN |
| 59 | GET | `/dashboard/stats` | Get dashboard stats | AUTH |

### Alerts (`/api/admin/alerts`)

| Method | Endpoint | Description | Roles |
|---|---|---|---|
| GET | `/` | Get all alerts (filter by type, status) | ADMIN |
| GET | `/{id}` | Get alert detail | ADMIN |
| PUT | `/{id}/action` | Perform action (approve/reject/ignore) | ADMIN |

---

## Security Configuration

Public endpoints (no JWT required):
- `POST /api/auth/login`
- `POST /api/auth/register`
- `POST /api/auth/otp/email`
- `POST /api/auth/otp/phone`
- `POST /api/auth/otp/verify`

All other endpoints require a valid JWT. Role enforcement is via `@PreAuthorize`. JWT claims carry `userId` and `role`. Token expiry is 24 hours, configurable in `application.properties`.

---

## Key Business Rules

### Duplicate / Provisional Logic
1. On prospect creation, companies starting with the first 2 characters of the input are queried and returned as a dropdown for duplicate awareness (API #9)
2. If a user selects an existing company or enters one with a fuzzy match ≥ 65%, the entry is flagged as PROVISIONAL
3. If `programType != SHC` and `prospect.city` is not in `licensee_cities` for that licensee, the entry is flagged as PROVISIONAL
4. If the same `contactFirstName + contactLastName` already exists for the same company, the entry is rejected outright
5. PROVISIONAL entries generate an Alert and trigger an admin notification email

### Protection Timeline
- On creation: `protectionStatus = PROTECTED`, `entryDate = today`
- Set `protectionPeriodMonths`: LI=12, SI=6, ONE_TO_ONE=null, SHC=null
- Scheduler checks daily for:
    - No first meeting within 45 days → warning email
    - No first meeting within 75 days → UNPROTECTED
    - For LI/SI: no activity for base duration → grace period alert
    - No activity within grace period (3 months) → UNPROTECTED
- O2O and SHC: perpetual — schedulers skip these

### Prospect Visibility Rules
- ASSOCIATE: sees only own prospects (`associateId = currentUser.id`), limited fields
- LICENSEE: sees prospects linked to them via `prospect_licensees`, limited fields
- ADMIN/SUPER_ADMIN: sees all, all fields

### Soft Delete
- Prospects are never hard deleted. `deletionStatus = true` + `protectionStatus = UNPROTECTED`
- Groups: `deletionStatus = true`
- Users: `status = INACTIVE`

### Audit Log Policy
- Write audit log for every significant action (see AuditActionType enum)
- Store `previousState` and `newState` as JSON strings (serialize entity before/after update)
- `performedBy` is null for system-triggered events (scheduler)
- Audit logs are immutable: no UPDATE or DELETE on `audit_logs` table

---

## Utility Classes

### `FuzzyMatchUtil`
Implements a string similarity algorithm (e.g. Levenshtein distance). Exposes a similarity score between 0.0 and 1.0, and a convenience method that accepts a threshold to return a boolean match result. The threshold used for duplicate detection is 0.65.

### `OtpUtil`
Generates 6-digit numeric OTPs. Handles storage with expiry (5 minutes default) — either in-memory or via a DB table — and validates a submitted OTP against the stored value and expiry time.

### `JwtUtil`
Handles JWT generation from a `User` object, and extraction of `userId` and `role` from a token. Also exposes a validity check method.

### `S3Util`
Wraps AWS S3 operations: uploading a file to a given folder, generating a presigned download URL with a configurable expiry, and deleting a file by key.

---

## Exception Handling

A `GlobalExceptionHandler` annotated with `@RestControllerAdvice` handles all exceptions centrally:

```
ResourceNotFoundException → 404
AccessDeniedException → 403
DuplicateEntryException → 409
ValidationException → 400
OtpExpiredException → 400
OtpInvalidException → 400
InvitationTokenInvalidException → 400
BusinessRuleException → 422 (for rule violations like "groups only for clients")
```

The standard error response shape is: `status` (HTTP code), `error` (exception class name), `message` (human-readable detail), and `timestamp`.

---

## `application.properties` Reference

```properties
# DB — Supabase (free tier, development)
# Connection string from: Supabase Dashboard → Project → Settings → Database → Connection string → JDBC
# Use the "Transaction pooler" (port 6543) for dev, NOT the direct connection (port 5432) — free tier has connection limits
# Format: jdbc:postgresql://aws-0-<region>.pooler.supabase.com:6543/postgres?pgbouncer=true
spring.datasource.url=jdbc:postgresql://<your-supabase-host>:6543/postgres?pgbouncer=true
spring.datasource.username=postgres.<your-project-ref>
spring.datasource.password=<your-db-password>
spring.datasource.driver-class-name=org.postgresql.Driver

# Use 'update' during development so Hibernate auto-creates/alters tables.
# Switch to 'validate' before going to production.
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# Required for Supabase pgBouncer (transaction mode) — disables prepared statements
spring.datasource.hikari.connection-init-sql=SET application_name='lmi-crm'
spring.jpa.properties.hibernate.jdbc.lob.non_contextual_creation=true

# JWT
jwt.secret=
jwt.expiry.hours=24

# Mail
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=
spring.mail.password=
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# AWS S3
aws.s3.bucket=
aws.s3.region=
aws.access.key=
aws.secret.key=

# OTP
otp.expiry.minutes=5

# Protection rules (days)
protection.first.meeting.warning.days=45
protection.first.meeting.expiry.days=75
protection.grace.period.months=3
```

---

## Response Conventions

- All responses are wrapped: `{ "success": true, "data": {...}, "message": "..." }`
- Paginated lists follow: `{ "success": true, "data": { "content": [...], "page": 0, "size": 20, "total": 100 } }`
- Controllers return `ResponseEntity<ApiResponse<T>>`
- A generic `ApiResponse<T>` wrapper class is defined in `dto/response/`

---

## Notes and Clarifications from PRD

- **Prospect `email` field is marked unique** in schema — confirm with client if this is intentional (two contacts at same company cannot share email)
- **MLO (Master Licensee/Owner)** must be pre-created as a LICENSEE user in the DB before licensee deactivation is used
- **Associate invitation**: Licensee sends *request*, Admin creates + invites. Licensee cannot directly invite.
- **Admin can directly add licensee or associate** without going through request flow
- **Prospect deletion** (soft): LICENSEE can soft-delete own prospects. Admin does not need to approve.
- **Protection extension** and **status override** are ADMIN-only and can be done via the standard `PUT /prospects/{id}` endpoint with admin-only fields
- **`prospect_status` (NORMAL/PROVISIONAL)** vs **`protection_status` (PROTECTED/UNPROTECTED/PROVISIONAL)** — these are distinct. A PROVISIONAL prospect can also have PROTECTED protection_status until admin acts.
- **Dashboard stats** (`/dashboard/stats`) implementation is TBD per the API list — implement basic counts per role for now
- **Weekly report** is also sent automatically by scheduler on Mondays; the button on frontend calls `/reports?type=WEEKLY` to generate on demand
- **Resource `RESOURCE_CREATED`** is missing from `AuditActionType` enum in schema — use `RESOURCE_UPDATED` for creation until schema is updated, or add the enum value