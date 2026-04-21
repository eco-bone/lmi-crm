# Prospect Detail Page Enhancements Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enable everyone to view full prospect details while restricting actions to owners only. Fix backend to return full fields for all users, and implement frontend ownership-based action controls.

**Architecture:**
1. **Backend:** Remove limited response pattern - always return full prospect fields. Keep ownership validation in action endpoints.
2. **Frontend:** Fetch current user info, calculate ownership, conditionally enable/disable actions based on ownership + role.
3. **UI Enhancements:** Add protection status visualization, provisional alerts, program type tooltips, and form validation.

**Tech Stack:** Spring Boot (Backend), Next.js 14 (Frontend), React 18, Redux Toolkit, Material-UI v5, Axios

---

## Critical Requirements

### Backend Requirements
1. **Remove ownership check from `getProspectDetail`** - Anyone can view
2. **Always return full fields** - No more `toLimitedResponse()`
3. **Keep action validations** - Edit/Delete/Convert/Extension still require ownership

### Frontend Requirements
1. **Fetch current user info** - Need `userId` from Redux to compare ownership
2. **Calculate ownership** - Compare `prospect.licenseeId`/`associateId` with current user
3. **Show all fields** - Display full prospect information for everyone
4. **Conditional actions** - Enable buttons only if user owns the prospect (or is admin)

---

## File Structure

### Backend Files to Modify:
1. **`src/main/java/com/lmi/crm/service/ProspectServiceImpl.java`**
   - Remove ownership check from `getProspectDetail` (lines 424-434)
   - Always use `toResponse()` instead of `toLimitedResponse()` (lines 436-445)

2. **`src/main/java/com/lmi/crm/mapper/ProspectMapper.java`**
   - Keep both response methods but deprecate `toLimitedResponse()` (optional cleanup)

### Frontend Files to Modify:
1. **`frontend/src/app/dashboard/prospects/[id]/page.js`**
   - Add ownership calculation logic
   - Update all action button conditions to check ownership
   - Add protection status card, provisional banner, tooltips
   - Add form validation and loading states

2. **`frontend/src/services/prospectService.js`**
   - Add missing `requestProtectionExtension` method

3. **`frontend/src/components/dashboard/shared.js`**
   - Add `ProtectionStatusCard` component
   - Add `ProgramTypeWithTooltip` component
   - Add helper functions for protection calculations

---

## Task 1: Backend - Remove Ownership Check from getProspectDetail

**Files:**
- Modify: `src/main/java/com/lmi/crm/service/ProspectServiceImpl.java:410-450`

**Goal:** Allow everyone to view full prospect details without ownership restrictions.

---

- [ ] **Step 1: Remove ownership validation**

Open `src/main/java/com/lmi/crm/service/ProspectServiceImpl.java` and find the `getProspectDetail` method (line 411).

Delete lines 423-434 (ownership check):

```java
// DELETE THIS BLOCK:
// Step 2 — Ownership check for non-admin roles
if (requestingUser.getRole() == UserRole.ASSOCIATE) {
    if (!requestingUserId.equals(prospect.getAssociateId())) {
        throw new RuntimeException("Access denied");
    }
} else if (requestingUser.getRole() == UserRole.LICENSEE) {
    if (!prospectLicenseeRepository.existsByProspectIdAndLicenseeId(prospectId, requestingUserId)) {
        throw new RuntimeException("Access denied");
    }
} else if (requestingUser.getRole() != UserRole.ADMIN && requestingUser.getRole() != UserRole.SUPER_ADMIN) {
    throw new RuntimeException("Access denied");
}
```

- [ ] **Step 2: Always return full response**

Replace lines 436-445 with:

```java
        // Step 2 — Return full response for all users
        Integer licenseeId = prospectLicenseeRepository.findByProspectIdAndIsPrimaryTrue(prospectId)
                .map(ProspectLicensee::getLicenseeId)
                .orElse(null);
        ProspectResponse response = prospectMapper.toResponse(prospect, licenseeId, null);

        log.info("GET /api/prospects/{} — returned for userId: {}", prospectId, requestingUserId);

        return response;
```

Remove the old if/else block that checked role and returned different responses.

- [ ] **Step 3: Update method comment**

Update the method-level comment (if exists) to reflect the new behavior:

```java
/**
 * Get prospect detail by ID.
 * Returns full prospect information for all authenticated users.
 * Ownership validation is performed at action endpoints (update, delete, convert, extension).
 */
```

- [ ] **Step 4: Test the endpoint**

```bash
# Start the backend
./mvnw spring-boot:run
```

Test with curl or Postman:
```bash
# Login as any user and get token
# Then test viewing a prospect you DON'T own:
curl -X GET http://localhost:8080/api/prospects/24 \
  -H "Authorization: Bearer YOUR_TOKEN"
```

Expected: Should return full fields (email, phone, programType, etc.) without "Access denied" error.

- [ ] **Step 5: Commit backend changes**

```bash
git add src/main/java/com/lmi/crm/service/ProspectServiceImpl.java
git commit -m "fix: remove ownership check from getProspectDetail - allow everyone to view full prospect details"
```

---

## Task 2: Add Missing Frontend API Service Method

**Files:**
- Modify: `frontend/src/services/prospectService.js:66` (append after `checkDuplicate`)

**Goal:** Add the missing `requestProtectionExtension` API method.

---

- [ ] **Step 1: Add the requestProtectionExtension function**

Open `frontend/src/services/prospectService.js` and add after the `checkDuplicate` function (line 65):

```javascript
/**
 * Request protection period extension for a prospect.
 * @param {number} id - Prospect ID
 * Response envelope: { success: boolean, data: null, message: string }
 */
export const requestProtectionExtension = (id) =>
  api.post(`/api/prospects/${id}/extension-request`);
```

- [ ] **Step 2: Verify the export**

Ensure the function is exported and follows the same pattern as other service methods.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/services/prospectService.js
git commit -m "feat: add requestProtectionExtension API service method"
```

---

## Task 3: Add Protection Calculation Helpers

**Files:**
- Modify: `frontend/src/components/dashboard/shared.js:237` (append at end)

**Goal:** Add utility functions for calculating protection status, days remaining, and status colors.

---

- [ ] **Step 1: Add protection helper functions**

Open `frontend/src/components/dashboard/shared.js` and add at the end of the file (after line 236):

```javascript
// ── Protection Status Helpers ─────────────────────────────────────────────────

/**
 * Calculate days remaining until protection expires
 * @param {string} entryDate - ISO date string (YYYY-MM-DD)
 * @param {number} protectionPeriodMonths - Number of months
 * @returns {number} Days remaining (negative if expired)
 */
export function calculateDaysRemaining(entryDate, protectionPeriodMonths) {
  if (!entryDate || !protectionPeriodMonths) return null;
  const entry = new Date(entryDate);
  const expiry = new Date(entry);
  expiry.setMonth(expiry.getMonth() + protectionPeriodMonths);
  const today = new Date();
  return Math.ceil((expiry - today) / (1000 * 60 * 60 * 24));
}

/**
 * Get protection status color configuration
 * @param {number} daysRemaining - Days until expiry
 * @returns {{ color: string, bgColor: string, status: string }}
 */
export function getProtectionStatusColor(daysRemaining) {
  if (daysRemaining === null) return { color: '#64748b', bgColor: '#f1f5f9', status: 'Unknown' };
  if (daysRemaining < 0) return { color: '#991b1b', bgColor: '#fee2e2', status: 'Expired' };
  if (daysRemaining <= 15) return { color: '#b91c1c', bgColor: '#fef2f2', status: 'Critical' };
  if (daysRemaining <= 30) return { color: '#ea580c', bgColor: '#fff7ed', status: 'Warning' };
  if (daysRemaining <= 60) return { color: '#ca8a04', bgColor: '#fefce8', status: 'Attention' };
  return { color: '#065f46', bgColor: '#ecfdf5', status: 'Protected' };
}

/**
 * Get program type protection rules
 * @param {string} programType - 'LI' | 'SI' | 'ONE_TO_ONE' | 'SHC'
 * @returns {string} Human-readable protection rules
 */
export function getProgramTypeRules(programType) {
  const rules = {
    LI: '12 months base protection + 3 months grace period',
    SI: '6 months base protection + 3 months grace period',
    ONE_TO_ONE: 'Perpetual protection (no expiry)',
    SHC: 'Perpetual protection (no expiry)',
  };
  return rules[programType] || 'Unknown program type';
}
```

- [ ] **Step 2: Verify exports**

Ensure all three functions are exported and can be imported by other components.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/components/dashboard/shared.js
git commit -m "feat: add protection status calculation helpers"
```

---

## Task 4: Add ProtectionStatusCard Component

**Files:**
- Modify: `frontend/src/components/dashboard/shared.js:237` (append after helpers)

**Goal:** Create a reusable component to display protection status with visual indicators.

---

- [ ] **Step 1: Add necessary imports at top of file**

Open `frontend/src/components/dashboard/shared.js` and add these imports after existing imports (around line 14):

```javascript
import LinearProgress from '@mui/material/LinearProgress';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import Tooltip from '@mui/material/Tooltip';
```

- [ ] **Step 2: Add ProtectionStatusCard component**

Add after the helper functions:

```javascript
/**
 * Protection Status Card - Shows detailed protection status with progress bar
 */
export function ProtectionStatusCard({ prospect }) {
  const daysRemaining = calculateDaysRemaining(prospect.entryDate, prospect.protectionPeriodMonths);
  const statusConfig = getProtectionStatusColor(daysRemaining);

  if (prospect.status !== 'PROTECTED' || !prospect.protectionPeriodMonths) {
    return (
      <Box>
        <Typography sx={{ fontSize: '0.6875rem', fontWeight: 600, color: '#94a3b8', mb: 0.5, textTransform: 'uppercase', letterSpacing: '0.05em' }}>
          Protection Status
        </Typography>
        <Typography sx={{ fontSize: '0.8125rem', color: '#334155', lineHeight: 1.5 }}>
          {prospect.status === 'UNPROTECTED' ? 'Not Protected' : prospect.status === 'PROVISIONAL' ? 'Pending Approval' : '—'}
        </Typography>
      </Box>
    );
  }

  const totalDays = prospect.protectionPeriodMonths * 30; // Approximate
  const elapsedDays = totalDays - daysRemaining;
  const progress = Math.min(100, Math.max(0, (elapsedDays / totalDays) * 100));

  return (
    <Box>
      <Typography sx={{ fontSize: '0.6875rem', fontWeight: 600, color: '#94a3b8', mb: 0.5, textTransform: 'uppercase', letterSpacing: '0.05em' }}>
        Protection Status
      </Typography>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 0.5 }}>
        <Chip
          label={statusConfig.status}
          size="small"
          sx={{
            height: 22,
            borderRadius: '6px',
            fontSize: '0.6875rem',
            fontWeight: 600,
            bgcolor: statusConfig.bgColor,
            color: statusConfig.color,
            border: 'none',
            '& .MuiChip-label': { px: '8px' },
          }}
        />
        <Typography sx={{ fontSize: '0.8125rem', fontWeight: 600, color: statusConfig.color }}>
          {daysRemaining < 0 ? 'Expired' : `${daysRemaining} days remaining`}
        </Typography>
      </Box>
      <LinearProgress
        variant="determinate"
        value={progress}
        sx={{
          height: 6,
          borderRadius: '3px',
          bgcolor: '#f1f5f9',
          '& .MuiLinearProgress-bar': {
            bgcolor: statusConfig.color,
            borderRadius: '3px',
          },
        }}
      />
      <Typography sx={{ fontSize: '0.6875rem', color: '#94a3b8', mt: 0.5 }}>
        {prospect.protectionPeriodMonths} month{prospect.protectionPeriodMonths !== 1 ? 's' : ''} protection period
      </Typography>
    </Box>
  );
}

/**
 * Program Type Chip with Tooltip - Shows program type with protection rules
 */
export function ProgramTypeWithTooltip({ programType }) {
  const rules = getProgramTypeRules(programType);

  return (
    <Tooltip
      title={
        <Box sx={{ p: 0.5 }}>
          <Typography sx={{ fontSize: '0.75rem', fontWeight: 600, mb: 0.5 }}>
            {programType} Program Rules
          </Typography>
          <Typography sx={{ fontSize: '0.6875rem', lineHeight: 1.4 }}>
            {rules}
          </Typography>
          <Typography sx={{ fontSize: '0.6875rem', color: '#cbd5e1', mt: 0.5, fontStyle: 'italic' }}>
            First meeting required within 45 days
          </Typography>
        </Box>
      }
      arrow
      placement="top"
    >
      <Box sx={{ display: 'inline-flex', alignItems: 'center', gap: 0.5, cursor: 'help' }}>
        <ProgramChip label={programType} />
        <InfoOutlinedIcon sx={{ fontSize: '0.875rem', color: '#94a3b8' }} />
      </Box>
    </Tooltip>
  );
}
```

- [ ] **Step 3: Verify component syntax**

Check for any syntax errors and ensure all MUI components are imported.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/components/dashboard/shared.js
git commit -m "feat: add ProtectionStatusCard and ProgramTypeWithTooltip components"
```

---

## Task 5: Add Ownership Calculation Logic to Detail Page

**Files:**
- Modify: `frontend/src/app/dashboard/prospects/[id]/page.js:39-50` (state section)

**Goal:** Calculate if current user owns the prospect to enable/disable actions.

---

- [ ] **Step 1: Import useSelector to get userId**

The file already imports `useSelector` and gets `role` (line 39). We need to also get `userId`.

Update line 39:

```javascript
  const role = useSelector((state) => state.auth.role);
  const currentUserId = useSelector((state) => state.auth.userId);
```

- [ ] **Step 2: Add ownership calculation state**

Add after the `prospect` state (around line 41):

```javascript
  const [isOwner, setIsOwner] = useState(false);
```

- [ ] **Step 3: Add ownership calculation function**

Add after the `showToast` function (around line 53):

```javascript
  const calculateOwnership = (prospectData) => {
    if (!currentUserId || !prospectData) {
      setIsOwner(false);
      return;
    }

    // Admins always have ownership privileges
    if (role === 'ADMIN' || role === 'SUPER_ADMIN') {
      setIsOwner(true);
      return;
    }

    // Check licensee ownership
    if (role === 'LICENSEE' && prospectData.licenseeId === currentUserId) {
      setIsOwner(true);
      return;
    }

    // Check associate ownership
    if (role === 'ASSOCIATE' && prospectData.associateId === currentUserId) {
      setIsOwner(true);
      return;
    }

    // Not owner
    setIsOwner(false);
  };
```

- [ ] **Step 4: Call calculateOwnership when prospect loads**

Update the `loadProspect` function (around line 55-87) to call `calculateOwnership`:

Find the line where `setProspect(prospectData)` is called (line 61) and add after it:

```javascript
      setProspect(prospectData);
      calculateOwnership(prospectData); // Add this line
```

- [ ] **Step 5: Verify ownership is recalculated**

Ensure that whenever prospect changes, ownership is recalculated. The useEffect on line 89 already calls `loadProspect()`, so ownership will be recalculated on mount and when ID changes.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/app/dashboard/prospects/[id]/page.js
git commit -m "feat: add ownership calculation logic based on licenseeId and associateId"
```

---

## Task 6: Update Action Button Conditions with Ownership Check

**Files:**
- Modify: `frontend/src/app/dashboard/prospects/[id]/page.js:212-216` (action conditions)

**Goal:** Update all action button conditions to check ownership, not just role.

---

- [ ] **Step 1: Update canEdit condition**

Find the action conditions (around line 212-216) and replace:

```javascript
  const canEdit = (['LICENSEE', 'ADMIN', 'SUPER_ADMIN'].includes(role)) && isOwner;
  const canDelete = ['ADMIN', 'SUPER_ADMIN'].includes(role); // Admins can delete anything
  const canConvert = (['LICENSEE', 'ASSOCIATE'].includes(role)) && prospect.type === 'PROSPECT' && isOwner;
  const canRequestExtension = (['LICENSEE', 'ASSOCIATE'].includes(role))
    && prospect.status === 'PROTECTED'
    && prospect.protectionPeriodMonths
    && isOwner;
  const canHandleProvisional = ['ADMIN', 'SUPER_ADMIN'].includes(role) && prospect.status === 'PROVISIONAL';
  const isAdmin = ['ADMIN', 'SUPER_ADMIN'].includes(role);
```

- [ ] **Step 2: Add hasExtensionRequest state**

Add state to track pending extension request (around line 49):

```javascript
  const [hasExtensionRequest, setHasExtensionRequest] = useState(false);
```

- [ ] **Step 3: Update canRequestExtension to include pending check**

Update the condition:

```javascript
  const canRequestExtension = (['LICENSEE', 'ASSOCIATE'].includes(role))
    && prospect.status === 'PROTECTED'
    && prospect.protectionPeriodMonths
    && isOwner
    && !hasExtensionRequest;
```

- [ ] **Step 4: Test ownership-based visibility**

When testing:
1. Login as Licensee A who owns prospect #24
2. View prospect #24 - should see Edit, Convert, Extension buttons
3. Login as Licensee B who doesn't own prospect #24
4. View prospect #24 - should NOT see Edit, Convert, Extension buttons
5. Should still see all prospect fields (no hidden data)

- [ ] **Step 5: Commit**

```bash
git add frontend/src/app/dashboard/prospects/[id]/page.js
git commit -m "feat: add ownership-based action button visibility"
```

---

## Task 7: Add Provisional Reason Alert Banner

**Files:**
- Modify: `frontend/src/app/dashboard/prospects/[id]/page.js:218` (after header)

**Goal:** Display a prominent alert banner when prospect is in PROVISIONAL status.

---

- [ ] **Step 1: Add WarningAmberRounded icon import**

Add to imports (around line 22):

```javascript
import WarningAmberRoundedIcon from '@mui/icons-material/WarningAmberRounded';
```

- [ ] **Step 2: Add provisional alert banner**

Find the return statement (around line 218) and add the alert banner right after the header Box (around line 269, before the main content Box):

```javascript
      {/* Provisional Alert Banner */}
      {prospect.status === 'PROVISIONAL' && prospect.provisionReason && (
        <Alert
          severity="warning"
          icon={<WarningAmberRoundedIcon />}
          sx={{
            mb: 3,
            borderRadius: '12px',
            bgcolor: '#fffbeb',
            border: '1px solid #fde68a',
            '& .MuiAlert-icon': { color: '#d97706' },
            '& .MuiAlert-message': { width: '100%' },
          }}
        >
          <Typography sx={{ fontSize: '0.875rem', fontWeight: 600, color: '#92400e', mb: 0.5 }}>
            Provisional Prospect — Awaiting Admin Approval
          </Typography>
          <Typography sx={{ fontSize: '0.8125rem', color: '#78350f', lineHeight: 1.5 }}>
            {prospect.provisionReason}
          </Typography>
        </Alert>
      )}
```

- [ ] **Step 3: Test with provisional prospect**

Navigate to a provisional prospect and verify the banner appears with the correct reason.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/app/dashboard/prospects/[id]/page.js
git commit -m "feat: add provisional reason alert banner"
```

---

## Task 8: Replace Protection Display with ProtectionStatusCard

**Files:**
- Modify: `frontend/src/app/dashboard/prospects/[id]/page.js:23` (import), `page.js:373` (component usage)

**Goal:** Replace the simple protection display with enhanced ProtectionStatusCard.

---

- [ ] **Step 1: Update imports from shared components**

Update line 23:

```javascript
import {
  StatusChip,
  ClassChip,
  ProgramChip,
  ProtectionStatusCard,
  ProgramTypeWithTooltip,
} from '@/components/dashboard/shared';
```

- [ ] **Step 2: Remove old calculateExpiry function**

Delete the `calculateExpiry` function (lines 102-111) as it's now handled by ProtectionStatusCard.

- [ ] **Step 3: Replace Protection Expiry field**

Find the "Protection Expiry" InfoField (around line 373) and replace:

```javascript
              <ProtectionStatusCard prospect={prospect} />
```

Remove the old:
```javascript
<InfoField label="Protection Expiry" value={calculateExpiry()} />
```

- [ ] **Step 4: Replace program type chip in header**

Find the header section (around line 228) and update:

```javascript
            {prospect.programType && <ProgramTypeWithTooltip programType={prospect.programType} />}
```

Replace the old:
```javascript
{prospect.programType && <ProgramChip label={prospect.programType} />}
```

- [ ] **Step 5: Test visual rendering**

1. Navigate to a PROTECTED prospect
2. Verify ProtectionStatusCard shows:
   - Color-coded chip (green/yellow/orange/red)
   - Days remaining
   - Progress bar
3. Hover over program type chip and verify tooltip shows rules

- [ ] **Step 6: Commit**

```bash
git add frontend/src/app/dashboard/prospects/[id]/page.js
git commit -m "feat: replace protection display with ProtectionStatusCard and add program type tooltip"
```

---

## Task 9: Add Protection Extension Request Handler

**Files:**
- Modify: `frontend/src/app/dashboard/prospects/[id]/page.js:30` (import), `page.js:145` (handler), `page.js:471` (button)

**Goal:** Add button and handler for protection extension requests (ownership-based).

---

- [ ] **Step 1: Add requestProtectionExtension import**

Update the import from prospectService (line 24-30):

```javascript
import {
  getProspectById,
  updateProspect,
  deleteProspect,
  requestConversion,
  handleProvisional,
  requestProtectionExtension,
} from '@/services/prospectService';
```

- [ ] **Step 2: Add handler for extension request**

Add after `handleProvisionalAction` (around line 182):

```javascript
  const handleExtensionRequest = async () => {
    try {
      await requestProtectionExtension(unwrappedParams.id);
      showToast('Protection extension request submitted successfully');
      setHasExtensionRequest(true);
    } catch (err) {
      showToast(err.response?.data?.message || 'Failed to request protection extension', 'error');
    }
  };
```

- [ ] **Step 3: Add extension request button**

Find the Actions section (around line 469-533) and add the button after the conversion button (around line 486):

```javascript
            {canRequestExtension && (
              <Button
                variant="outlined"
                startIcon={<CheckCircleRoundedIcon />}
                onClick={handleExtensionRequest}
                sx={{
                  borderRadius: '8px',
                  fontWeight: 600,
                  fontSize: '0.875rem',
                  borderColor: '#2563eb',
                  color: '#2563eb',
                  '&:hover': { borderColor: '#1d4ed8', bgcolor: '#eff6ff' },
                }}
              >
                Request Protection Extension
              </Button>
            )}
```

- [ ] **Step 4: Update Actions section wrapper condition**

Update line 469:

```javascript
        {!editMode && (canConvert || canRequestExtension || canDelete || canHandleProvisional) && (
```

- [ ] **Step 5: Test extension button**

1. Login as owner (Licensee/Associate)
2. Navigate to PROTECTED prospect they own
3. Verify "Request Protection Extension" button appears
4. Click button and verify success toast
5. Verify button disappears after request

- [ ] **Step 6: Commit**

```bash
git add frontend/src/app/dashboard/prospects/[id]/page.js
git commit -m "feat: add protection extension request button with ownership check"
```

---

## Task 10: Add Form Validation to Edit Mode

**Files:**
- Modify: `frontend/src/app/dashboard/prospects/[id]/page.js:48` (state), `page.js:118` (handleSave)

**Goal:** Add validation for required fields and email format before saving.

---

- [ ] **Step 1: Add validation error state**

Add state (around line 49):

```javascript
  const [validationErrors, setValidationErrors] = useState({});
  const [saving, setSaving] = useState(false);
```

- [ ] **Step 2: Add validation function**

Add after state declarations (around line 53):

```javascript
  const validateForm = () => {
    const errors = {};

    if (!editForm.companyName?.trim()) {
      errors.companyName = 'Company name is required';
    }
    if (!editForm.contactFirstName?.trim()) {
      errors.contactFirstName = 'First name is required';
    }
    if (!editForm.contactLastName?.trim()) {
      errors.contactLastName = 'Last name is required';
    }
    if (!editForm.email?.trim()) {
      errors.email = 'Email is required';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(editForm.email)) {
      errors.email = 'Invalid email format';
    }
    if (!editForm.phone?.trim()) {
      errors.phone = 'Phone is required';
    }
    if (!editForm.city?.trim()) {
      errors.city = 'City is required';
    }
    if (!editForm.classificationType) {
      errors.classificationType = 'Classification is required';
    }

    return errors;
  };
```

- [ ] **Step 3: Update handleSave with validation**

Replace the `handleSave` function (around line 118):

```javascript
  const handleSave = async () => {
    const errors = validateForm();
    if (Object.keys(errors).length > 0) {
      setValidationErrors(errors);
      showToast('Please fix validation errors before saving', 'error');
      return;
    }

    setValidationErrors({});
    setSaving(true);
    try {
      await updateProspect(unwrappedParams.id, editForm);
      showToast('Prospect updated successfully');
      setEditMode(false);
      loadProspect();
    } catch (err) {
      showToast(err.response?.data?.message || 'Failed to update prospect', 'error');
    } finally {
      setSaving(false);
    }
  };
```

- [ ] **Step 4: Clear errors on edit start**

Update `handleEdit` (around line 113):

```javascript
  const handleEdit = () => {
    setEditForm({ ...prospect });
    setValidationErrors({});
    setEditMode(true);
  };
```

- [ ] **Step 5: Add error display to fields**

Update TextField components to show errors. Example for email (around line 316):

```javascript
              <InfoField
                label="Email"
                value={prospect.email}
                editMode={editMode}
                renderEdit={() => (
                  <TextField
                    size="small"
                    type="email"
                    value={editForm.email ?? ''}
                    onChange={(e) => setEditForm({ ...editForm, email: e.target.value })}
                    error={!!validationErrors.email}
                    helperText={validationErrors.email}
                  />
                )}
              />
```

Apply similar pattern to: contactFirstName, contactLastName, phone, city, classificationType.

- [ ] **Step 6: Update Save button with loading state**

Find the Save button (around line 252) and update:

```javascript
              <Button
                variant="contained"
                startIcon={saving ? <CircularProgress size={16} sx={{ color: 'white' }} /> : <SaveRoundedIcon />}
                onClick={handleSave}
                disabled={saving}
                sx={{
                  bgcolor: '#2563eb',
                  borderRadius: '8px',
                  fontWeight: 600,
                  fontSize: '0.875rem',
                  '&:hover': { bgcolor: '#1d4ed8' },
                  '&:disabled': { bgcolor: '#94a3b8' },
                }}
              >
                {saving ? 'Saving...' : 'Save'}
              </Button>
```

- [ ] **Step 7: Test validation**

1. Enter edit mode as owner
2. Clear email field
3. Click Save
4. Verify error appears under field
5. Fix error and verify save succeeds

- [ ] **Step 8: Commit**

```bash
git add frontend/src/app/dashboard/prospects/[id]/page.js
git commit -m "feat: add form validation and loading state to edit mode"
```

---

## Task 11: Final Testing - Ownership-Based Actions

**Files:**
- Test: All modified files

**Goal:** Comprehensive testing of ownership-based actions across different roles and scenarios.

---

- [ ] **Step 1: Test viewing as non-owner**

1. Login as Licensee A (userId: 2)
2. Navigate to prospect owned by Licensee B (licenseeId: 5)
3. Verify:
   - ✅ Page loads without "Access denied" error
   - ✅ All fields are visible (email, phone, programType, classification, etc.)
   - ✅ ProtectionStatusCard shows correctly
   - ✅ Program type tooltip works
   - ❌ Edit button is NOT visible/disabled
   - ❌ "Request Extension" button is NOT visible
   - ❌ "Request Conversion" button is NOT visible

- [ ] **Step 2: Test viewing as owner - Licensee**

1. Login as Licensee A (userId: 2)
2. Navigate to prospect owned by Licensee A (licenseeId: 2)
3. Verify:
   - ✅ All fields visible
   - ✅ Edit button IS visible
   - ✅ Can click Edit and enter edit mode
   - ✅ "Request Extension" button visible (if PROTECTED)
   - ✅ "Request Conversion" button visible (if PROSPECT)
   - ✅ Can click buttons and perform actions

- [ ] **Step 3: Test viewing as owner - Associate**

1. Login as Associate A (userId: 4)
2. Navigate to prospect owned by Associate A (associateId: 4)
3. Verify:
   - ✅ All fields visible
   - ❌ Edit button is NOT visible (Associates can't edit)
   - ✅ "Request Extension" button visible
   - ✅ "Request Conversion" button visible
   - ✅ Can perform extension and conversion requests

- [ ] **Step 4: Test viewing as non-owner - Associate**

1. Login as Associate A (userId: 4)
2. Navigate to prospect owned by Associate B (associateId: 7)
3. Verify:
   - ✅ All fields visible
   - ❌ Edit button NOT visible
   - ❌ "Request Extension" NOT visible
   - ❌ "Request Conversion" NOT visible

- [ ] **Step 5: Test viewing as Admin**

1. Login as Admin (role: ADMIN)
2. Navigate to any prospect
3. Verify:
   - ✅ All fields visible
   - ✅ Edit button IS visible (admins can edit anything)
   - ✅ Can edit all fields including admin-only fields
   - ✅ Delete button IS visible
   - ❌ "Request Extension" NOT visible (admins handle extensions via alerts)
   - ❌ "Request Conversion" NOT visible (admins approve via alerts)
   - ✅ Provisional buttons visible if status = PROVISIONAL

- [ ] **Step 6: Test protection status display**

Test with prospects in different states:
1. PROTECTED with > 60 days - should show green chip and progress bar
2. PROTECTED with 30-60 days - should show yellow
3. PROTECTED with < 15 days - should show red
4. UNPROTECTED - should show "Not Protected" text
5. PROVISIONAL - should show alert banner + "Pending Approval"

- [ ] **Step 7: Test edit mode as owner**

1. Login as owner (Licensee)
2. Click Edit
3. Clear email field
4. Click Save
5. Verify validation error appears
6. Fix email and save successfully
7. Verify data updates and edit mode exits

- [ ] **Step 8: Test backend endpoint directly**

```bash
# Test that backend returns full fields for all users:
curl -X GET http://localhost:8080/api/prospects/24 \
  -H "Authorization: Bearer ASSOCIATE_TOKEN"
```

Expected response should include:
- email
- phone
- programType
- classificationType
- protectionPeriodMonths
- entryDate
- associateId
- licenseeId
- createdBy
- createdAt

- [ ] **Step 9: Verify all commits**

```bash
git log --oneline -11
```

Expected commits:
1. Backend: Remove ownership check from getProspectDetail
2. Frontend: Add requestProtectionExtension API method
3. Frontend: Add protection status calculation helpers
4. Frontend: Add ProtectionStatusCard and tooltip components
5. Frontend: Add ownership calculation logic
6. Frontend: Add ownership-based action button visibility
7. Frontend: Add provisional reason alert banner
8. Frontend: Replace protection display with enhanced components
9. Frontend: Add protection extension request button
10. Frontend: Add form validation and loading state

- [ ] **Step 10: Final documentation commit**

```bash
git add docs/superpowers/plans/2026-04-19-prospect-detail-enhancements.md
git commit -m "docs: update prospect detail enhancements plan with ownership-based actions"
```

---

## Verification Checklist

After completing all tasks, verify:

- [ ] **Backend changes:**
  - [ ] `getProspectDetail` returns full fields for all users
  - [ ] No "Access denied" error when viewing others' prospects
  - [ ] Action endpoints still validate ownership (update, delete, convert, extension)

- [ ] **Frontend ownership logic:**
  - [ ] Ownership calculated correctly for Licensees (licenseeId match)
  - [ ] Ownership calculated correctly for Associates (associateId match)
  - [ ] Admins always treated as owners
  - [ ] Non-owners can view but can't act

- [ ] **Action buttons:**
  - [ ] Edit: visible only for owners (Licensee/Admin)
  - [ ] Delete: visible only for Admins
  - [ ] Request Conversion: visible only for owners (Licensee/Associate)
  - [ ] Request Extension: visible only for owners (Licensee/Associate)
  - [ ] Handle Provisional: visible only for Admins

- [ ] **UI components:**
  - [ ] ProtectionStatusCard shows correct colors
  - [ ] Program type tooltip displays protection rules
  - [ ] Provisional alert banner appears with reason
  - [ ] Form validation prevents invalid submissions
  - [ ] Loading states provide feedback

- [ ] **Data display:**
  - [ ] All fields visible for everyone (no hidden data)
  - [ ] Email, phone, programType, classification all display
  - [ ] Protection period and entry date display
  - [ ] Associate and licensee info display

- [ ] **No console errors in browser dev tools**
- [ ] **All commits follow conventional commit format**

---

## Architecture Notes

### Why This Design?

**1. Open View Model:**
- Product requirement: "Everyone should be able to view prospect details"
- Implementation: Removed ownership check from `getProspectDetail`
- Benefit: Transparency, easier collaboration, no access denied errors

**2. Ownership-Based Actions:**
- Requirement: "Actions should be role-based AND ownership-based"
- Implementation: Frontend calculates ownership by comparing IDs
- Benefit: Clear visual feedback, backend still validates for security

**3. Backend Validation:**
- Even though frontend hides buttons, backend still checks ownership
- This prevents API abuse via curl/Postman
- Security in depth: UI convenience + API security

**4. No API Response Fields for Ownership:**
- We could add `isOwner: boolean` to API response
- But calculating in frontend is simpler and doesn't require backend changes
- Frontend has all data needed: `currentUserId`, `prospect.licenseeId`, `prospect.associateId`

### Common Pitfalls Avoided

- ❌ Don't rely on role alone - must check ownership
- ❌ Don't hide data based on ownership - show all fields
- ❌ Don't skip backend validation - frontend buttons can be bypassed
- ❌ Don't forget to recalculate ownership when prospect changes
- ✅ Calculate ownership in frontend using ID comparisons
- ✅ Show all fields, hide only action buttons
- ✅ Backend validates ownership for all actions
- ✅ Clear visual feedback when viewing others' prospects

### Testing Strategy

1. **Test each role independently** (Associate, Licensee, Admin)
2. **Test ownership scenarios** (owner vs non-owner)
3. **Test state transitions** (PROTECTED → expired, etc.)
4. **Test validation** on all required fields
5. **Test async operations** (save, delete, requests)
6. **Test backend** directly with curl to verify full fields returned
