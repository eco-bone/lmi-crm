# Data Import Templates — Instructions

This folder contains 4 CSV files. Please fill them in with your real company data,
following the rules below. Sample rows are already filled in as examples — replace
them with your actual data (you can add as many rows as needed).

## General Rules

1. **Do not rename, reorder, or delete columns.** Add new rows only.
2. **Leave a cell empty if it doesn't apply.** Do not write "NA", "N/A", "-", or "none".
3. **Dates** must be in `YYYY-MM-DD` format (e.g. `2026-05-15`).
4. **Date + time** must be in `YYYY-MM-DD HH:MM:SS` format, 24-hour clock (e.g. `2026-05-15 14:30:00`).
5. **True/false fields** must be exactly `true` or `false` (lowercase).
6. **Lists** (when one field can hold multiple values): separate values with commas
   and wrap the whole value in double quotes, e.g. `"Mumbai,Pune,Nashik"`.
   If there's only one value, no quotes are needed.
7. **Email addresses are used to link rows across files.** The same person/company
   must use the exact same email everywhere it appears across all 4 files
   (spelling, capitalization, etc. must match exactly).
8. **Enum / fixed-choice fields** must match one of the listed values exactly
   (case-sensitive, as written below).
9. Save files as `.csv` (UTF-8 encoding), keeping the same file names.

---

## File 1: `01_team.csv` — Licensees & Associates

One row per person (licensee or associate).

| Column | Description |
|---|---|
| `role` | `LICENSEE` or `ASSOCIATE` |
| `firstName` | Person's first name |
| `lastName` | Person's last name |
| `email` | Their email — must be unique, used to identify them in other files |
| `phone` | 10-digit phone number |
| `cities` | **Licensees only.** City or cities they operate in. Multiple cities = comma-separated list in quotes, e.g. `"Mumbai,Pune,Nashik"`. Leave blank for associates. |
| `primaryCity` | **Licensees only.** Which one of the `cities` is their main/primary city. Leave blank for associates. |
| `licenseeEmail` | **Associates only.** The email of the licensee (from this same file) that this associate reports to. Leave blank for licensees. |

---

## File 2: `02_prospects_clients.csv` — Prospects & Clients (companies)

One row per company (prospect or client).

| Column | Description |
|---|---|
| `companyName` | Name of the company |
| `city` | City the company is located in |
| `contactFirstName` | First name of the main contact person at the company |
| `contactLastName` | Last name of the main contact person |
| `designation` | Job title of the contact person (e.g. HR Manager, Director) |
| `email` | Contact person's email — must be unique, used to identify this company in other files |
| `phone` | 10-digit phone number for the contact |
| `referredBy` | How/who this prospect was referred by (e.g. "LinkedIn", "Referral", a person's name) |
| `classificationType` | Lead classification. One of: `AAA`, `AA`, `A`, `B`, `C` |
| `programType` | Program type. One of: `LI` (Large In-house), `SI` (Small In-house), `ONE_TO_ONE`, `SHC` (Showcase) |
| `type` | `PROSPECT` or `CLIENT` |
| `associateEmail` | Email of the associate (from File 1) who owns this account |
| `licenseeEmails` | Email(s) of the licensee(s) (from File 1) linked to this company. Multiple = comma-separated list in quotes |
| `primaryLicenseeEmail` | Which one of `licenseeEmails` is the primary licensee |
| `entryDate` | Date this prospect/client was first added (`YYYY-MM-DD`) |
| `firstMeetingDate` | Date of the first meeting (`YYYY-MM-DD`). Leave blank if no meeting has happened yet |
| `lastMeetingDate` | Date of the most recent meeting (`YYYY-MM-DD`). Leave blank if none |

---

## File 3: `03_groups.csv` — Training Groups

One row per training group. Groups can only be linked to companies marked
`type = CLIENT` in File 2 (not `PROSPECT`).

| Column | Description |
|---|---|
| `licenseeEmail` | Email of the licensee (from File 1) this group runs under |
| `facilitatorEmail` | Email of the person (from File 1) facilitating the group |
| `groupSize` | Number of participants in the group (whole number) |
| `groupType` | One of: `EPP`, `ELD`, `EPL`, `ECE`, `EML`, `ESL`, `LFW`, `AIE` |
| `deliveryType` | One of: `ONLINE`, `HYBRID`, `IN_PERSON` |
| `startDate` | Group start date (`YYYY-MM-DD`) |
| `ppmTfeDateSent` | Date the PPM/TFE form was sent (`YYYY-MM-DD`). Leave blank if not applicable |
| `clientEmails` | Email(s) of the client company contact(s) (from File 2, where `type = CLIENT`) in this group. Multiple = comma-separated list in quotes |

---

## File 4: `04_meetings.csv` — Meeting History

One row per meeting held with a prospect or client.

| Column | Description |
|---|---|
| `prospectEmail` | Email of the company contact (from File 2) this meeting was with |
| `prospectCompanyName` | Company name (must match File 2, for readability/double-checking) |
| `pointOfContact` | Name of the person actually met with |
| `description` | Short note on what the meeting was about |
| `meetingAt` | Date and time of the meeting (`YYYY-MM-DD HH:MM:SS`, 24-hour clock) |

---

## Quick Reference — Fixed Values

- `role`: `LICENSEE`, `ASSOCIATE`
- `classificationType`: `AAA`, `AA`, `A`, `B`, `C`
- `programType`: `LI`, `SI`, `ONE_TO_ONE`, `SHC`
- `type` (File 2): `PROSPECT`, `CLIENT`
- `groupType`: `EPP`, `ELD`, `EPL`, `ECE`, `EML`, `ESL`, `LFW`, `AIE`
- `deliveryType`: `ONLINE`, `HYBRID`, `IN_PERSON`
