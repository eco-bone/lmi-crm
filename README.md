# CRM — Customer Relationship Management System

A Spring Boot REST API for managing prospects, licensees, associates, groups, meetings, and related business workflows. The system is designed around a multi-role user hierarchy (Admin, Super Admin, Master Licensee, Licensee, Associate) and supports prospect tracking, group program management, alerting, audit logging, and resource sharing.

**Stack:** Java 17 · Spring Boot 4.0.5 · Spring Data JPA · PostgreSQL (Supabase) · Lombok · Maven

---

## Running the project

```bash
./mvnw spring-boot:run
```

Database credentials go in `src/main/resources/application-local.properties` (gitignored). See the existing file for the required keys.

---

## Work log

### Project setup
- Initialised Spring Boot 4.0.5 project with Java 17 and Maven
- Added dependencies: Spring Web, Spring Data JPA, PostgreSQL driver, Lombok
- Connected to Supabase (PostgreSQL) via `application-local.properties` to keep credentials out of version control
- Configured `spring.jpa.hibernate.ddl-auto=update` — Hibernate auto-creates and updates tables on startup

### Entity layer (`entity/`)
- Created all JPA entities strictly following the agreed database schema
- All foreign keys are stored as plain `Integer` fields (e.g. `licenseeId`, `associateId`) rather than JPA relationships, keeping the model simple and explicit
- Entities use `@CreationTimestamp` / `@UpdateTimestamp` for automatic timestamp management
- `ProspectLicensee` and `LicenseeCity` use `@PrePersist` for timestamp handling
- `AuditLog` uses PostgreSQL `jsonb` columns for `previousState`, `newState`, and `metadata`
- All entities use Lombok `@Data @NoArgsConstructor @AllArgsConstructor @Builder`

Entities: `User`, `Prospect`, `Group`, `Meeting`, `Alert`, `AuditLog`, `Resource`, `UserItem`, `GroupProspect`, `ProspectLicensee`, `LicenseeCity`

### Enum layer (`enums/`)
- Created all enums matching the schema exactly

| Enum | Values |
|---|---|
| `UserRole` | ADMIN, LICENSEE, ASSOCIATE, MASTER_LICENSEE, SUPER_ADMIN |
| `UserStatus` | ACTIVE, INACTIVE |
| `AuditActionType` | 24 domain-specific audit events |
| `ClassificationType` | AAA, AA, A, B, C |
| `ProspectProgramType` | LI, SI, ONE_TO_ONE, SHC |
| `ProspectType` | PROSPECT, CLIENT |
| `ProspectStatus` | NORMAL, PROVISIONAL |
| `ProtectionStatus` | PROTECTED, UNPROTECTED, PROVISIONAL |
| `GroupProgramType` | EPP, ELD, EPL, ECE, EML, ESL, LFW, AIE |
| `DeliveryType` | ONLINE, HYBRID, IN_PERSON |
| `AlertType` | 8 alert types |
| `AlertStatus` | PENDING, RESOLVED, REJECTED |
| `RelatedEntityType` | PROSPECT, USER, GROUP, SYSTEM |
| `UserItemType` | TASK, NOTE |
| `TaskStatus` | PENDING, COMPLETED |
| `ResourceType` | ZCDC, DOCUMENT, PPT |
| `FileType` | YOUTUBE, PDF, DOC, XLS, PPT |

### Repository layer (`dao/`)
- Created one `JpaRepository<Entity, Integer>` interface per entity
- No custom queries yet — these will be added as features require them

Repositories: `UserRepository`, `ProspectRepository`, `GroupRepository`, `MeetingRepository`, `AlertRepository`, `AuditLogRepository`, `ResourceRepository`, `UserItemRepository`, `GroupProspectRepository`, `ProspectLicenseeRepository`, `LicenseeCityRepository`

### Service layer (`service/crud/`)
- Implemented full CRUD for `User` following an interface + implementation pattern
- `UserService` — interface defining the contract
- `UserServiceImpl` — concrete implementation with logging via `@Slf4j`
- Delete is a **soft delete**: sets `status = INACTIVE`, record is retained in the database

### DTO & Mapper (`dto/`, `mapper/`)
- `UserRequestDTO` — fields accepted from the client (excludes `licenseeId`, which will be derived from auth context once Spring Security is added)
- `UserResponseDTO` — fields returned to the client
- `UserMapper` — handles `toEntity()`, `toDTO()`, and `updateEntity()` conversions, keeping mapping logic out of the service layer
