# Nexus Part 2 submission addendum

This is source text for updating the Part 2 PDF. It separates implemented work from scope deliberately scheduled for the final PoE.

## Improvements completed

| Area | Improvement | Evidence |
| --- | --- | --- |
| Navigation | Settings is available from the Dashboard and returns safely with back navigation. | `NexusNavHost.kt`, `DashboardScreen.kt` |
| Account settings | Display name, language, and notification preference are read/updated through `GET`/`PATCH /users/me`. | `ApiService.kt`, `SettingsViewModel.kt` |
| Device settings | System/light/dark theme is stored in SharedPreferences and applied at the activity root. | `SettingsPreferences.kt`, `MainActivity.kt` |
| Project planning | Tasks can be created and edited with a due date, priority, status, labels and checklist steps, and the dashboard surfaces overdue, due-today and upcoming work. | `TaskEditorDialog.kt`, `TaskListViewModel.kt`, `DashboardScreen.kt` |
| Quality | Dashboard and project-list ViewModels have populated, empty, and failure tests using injected fakes. | `DashboardViewModelTest.kt`, `ProjectListViewModelTest.kt` |
| Delivery | GitHub Actions runs unit tests and creates debug/release APK/AAB artifacts. | `.github/workflows/android-ci.yml` |

## Planned final PoE features

1. Google SSO: verify Google ID tokens on the server, then exchange them for the existing Nexus JWT.
2. Offline write queue: local pending operations, visible sync state, retry, and a documented conflict policy.
3. Push notifications: backend device-token registration, Android permission handling, and user-controlled reminders.
4. Complete localization: selected language will drive translated resources once the preference contract is finalised.

## Evidence to insert in the PDF

Insert four real running-app screenshots: Dashboard, project creation, task completion, and Settings. Add the unlisted narrated demo link after recording. The narration must cover registration, login, project/task creation, completion, Settings, secure session storage at a high level, and the matching rows in the API's JSON store (`backend/nexus-data.json`). This project has no PostgreSQL database, so do not present one. See `DEMO_SCRIPT.md` for the sequence.

Update the feature-status table so shipped features are labelled **implemented**, while the four final-PoE features above are labelled **planned**. Do not state that Google SSO, offline writes, push delivery, or full language switching are complete until their backend and end-to-end tests exist.
