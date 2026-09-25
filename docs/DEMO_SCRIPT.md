# Nexus Part 2 demo narration script

Use this as a short, narrated recording plan. Replace bracketed details with the account and project you use in the recording.

1. **Introduction (0:00-0:15).** “Nexus is an Android project and task workspace. This demo shows authentication, project and task management, settings, and the data saved by the custom REST API.”
2. **Register (0:15-0:45).** Register `[email]` with a display name. “The app validates blank fields, malformed email addresses, and short passwords before sending the request. A successful response returns a JWT, which is stored securely for the session.”
3. **Log in (0:45-1:00).** Log out, then log back in. “The bearer token is attached automatically to protected API calls.” *(Note: Authentication endpoints are rate-limited to 10 requests per 15 minutes per IP address; avoid rapid-fire repeated login or registration tests during live recording to prevent temporary HTTP 429 rate-limiting responses).*
4. **Create a project (1:00-1:25).** Create `[project name]`. “Projects are loaded into the dashboard and cached locally for offline-aware reads.”
5. **Create and complete tasks (1:25-2:05).** Add two tasks. Give the first a due date and a priority, and give the second a label and a checklist. Mark one complete. “Tasks carry a due date, a priority, a status, labels and a checklist. Because due dates are real, the dashboard separates overdue, due-today and upcoming work.”
6. **Settings (2:05-2:30).** Open Settings from the Dashboard, change display name/language/notification preference, and choose a theme. “Account preferences are sent in a partial `PATCH /users/me`; the visual theme is intentionally device-local.”
7. **Data evidence (2:30-3:00).** Show the API's JSON data file (`backend/nexus-data.json`) and point out the `users`, `projects` and `tasks` entries for the demonstrated account. The server keeps everything in this one JSON document; there is no PostgreSQL database in this project, so do not present one. Do not expose a real token or password in the recording.
8. **Close (3:00-3:10).** “The next PoE scope is Google SSO, a visible offline write queue with conflict handling, push notifications, and complete UI localization.”

Before upload, verify that no passwords, JWTs, database connection strings, or personally identifying test data are visible. Upload as unlisted if the link should only be shared with the lecturer, then replace the README placeholder with that URL.
