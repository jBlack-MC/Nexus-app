# Nexus

<p align="center">
  <img src="docs/nexus-intro.svg" alt="Nexus logo" width="280" />
</p>

Nexus is a focused Android workspace for people who want to turn projects into clear, manageable next steps. It brings projects, tasks, and progress into one calm mobile experience, so an individual or small team can see what matters, decide what to do next, and keep moving.

Today, Nexus provides a secure foundation: account access, a project dashboard, and task management inside each project. The product direction is to evolve from a simple task tracker into a dependable daily planning companion - useful in a few seconds, not another complicated system to maintain.

## Team

| Student Number | Name | Role |
| --- | --- | --- |
| ST10438928 | Clarity Masuku | Backend/API Integration Android App Developer / UI & Authentication  |
| ST10462532 | Sibusiso Mabena | Testing |

<p align="center">
  <a href="app/release/NexusApp.apk" download>
    <img src="https://img.shields.io/badge/Download-Nexus%20APK-6C4DFF?style=for-the-badge&amp;logo=android&amp;logoColor=white" alt="Download Nexus APK">
  </a>
</p>

The button downloads the prototype APK from `app/release/NexusApp.apk`.

> Status: active development. The default backend is a local service reachable from the Android emulator at `http://10.0.2.2:5263`.

## Contents

- [Features](#features)
- [Splash animation](#splash-animation)
- [Product direction](#product-direction)
- [Roadmap](#roadmap)
- [Tech stack](#tech-stack)
- [Requirements](#requirements)
- [Getting started](#getting-started)
- [Test credentials](#test-credentials)
- [Build and test](#build-and-test)
- [Continuous integration](#continuous-integration)
- [Changelog](#changelog)
- [Project improvement log](#project-improvement-log)
- [Backend API](#backend-api)
- [Testing](#testing)
- [Part 2 evidence](#part-2-evidence)
- [Project structure](#project-structure)
- [Security and release notes](#security-and-release-notes)
- [Contributing](#contributing)
- [License](#license)

## Features

- Register and sign in with a persisted JWT session.
- View dashboard counts for projects, tasks, and activity.
- Create, edit, and delete projects and project tasks.
- Plan tasks with an optional due date, priority, status, labels, and checklist steps.
- Search, filter, sort, and switch between task list and status-board views.
- Mark tasks as complete.
- Navigate through a single-activity Jetpack Compose UI.
- Use Material 3 theming, including dynamic color on Android 12 and later.

## Splash animation

View the interactive preview in [`docs/nexus-intro.html`](docs/nexus-intro.html). It recreates the Nexus intro: a spinner resolves into connected nodes, then the final monogram and wordmark. The Android implementation lives in [`SplashIntroScreen.kt`](app/src/main/java/com/example/nexus/ui/SplashIntroScreen.kt) and respects Android's system animation setting by showing the completed mark when animations are disabled.

## Product direction

Nexus should earn a place on a user's home screen by being clear, quick, and trustworthy.

| Principle | What it means in Nexus |
| --- | --- |
| Focus over clutter | The home screen should answer: what needs attention now? |
| Fast capture | Adding a task should take seconds, with sensible defaults. |
| Progress with context | Projects should show what is moving, blocked, and complete - not just a task count. |
| Calm by default | Notifications, visual hierarchy, and empty states should help users act without creating pressure. |
| Trustworthy data | Sessions, backups, sync, and offline behaviour must be predictable before advanced features are added. |

## Roadmap

These are proposed improvements, not features currently available in the app. Build them in this order so the product becomes more useful before it becomes more complex.

### Now: make the core experience excellent

- Add due dates, priority, status, and optional labels to tasks.
- Add subtasks and checklists for work that needs more than one step.
- Offer list and board views so users can choose the clearest way to work.
- Improve the dashboard with overdue, due today, and upcoming task sections.
- Add search, filtering, sorting, and a useful empty state for every list.
- Support pull-to-refresh, clear loading states, retry actions, and human-friendly error messages.
- Add onboarding that explains projects, tasks, and the first useful action.
- Write ViewModel, navigation, and API error-handling tests for the current flows.

### Next: make Nexus a daily habit

- Add reminders and notifications for due and overdue tasks, with user-controlled schedules.
- Introduce a "Today" view that helps users choose a small, realistic set of tasks.
- Add recurring tasks, task notes, attachments or links, and project templates.
- Add a calendar view for deadlines and a timeline view for project progress.
- Let users archive projects and review completed work or weekly progress.
- Add light and dark theme controls plus accessibility improvements: larger touch targets, content descriptions, contrast checks, and reduced-motion support.

### Later: make it collaborative and dependable everywhere

- Add shared projects, roles, comments, activity history, and mentions.
- Add offline-first storage with a visible sync state and conflict handling.
- Provide push notifications, secure account recovery, and configurable privacy controls.
- Add calendar integration, CSV import/export, home-screen widgets, and a tablet-friendly layout.
- Add product analytics that measure successful outcomes (for example, tasks completed or time to first project) while respecting user privacy.

Before starting a roadmap item, define the user problem, the smallest useful version, and one success measure. For example: "Can a user capture a task in under 10 seconds?" This keeps Nexus purposeful rather than feature-heavy.

### First delivery brief

| Item | User problem | Smallest useful version | Success measure |
| --- | --- | --- | --- |
| Task planning | People cannot tell which task needs attention first. | Let a user set a due date and priority when creating or editing a task, then show overdue, today, and upcoming tasks on the dashboard. | A user can capture a dated task in under 10 seconds and correctly identify every overdue task. |
| Task clarity | Larger tasks are easy to lose halfway through. | Add a checklist to a task with completion progress. | A user can finish a multi-step task without creating separate placeholder tasks. |
| Find work | A growing task list becomes difficult to scan. | Search task titles and filter by completion, due date, and priority. | A user can find a known task in under 15 seconds. |

Feature status is intentional: the roadmap is proposed work, while [the improvement log](PROJECT_IMPROVEMENTS.md) records delivered changes and implementation prerequisites.

## Tech stack

| Area | Technology |
| --- | --- |
| Language | Kotlin 2.2.10 |
| UI | Jetpack Compose and Material 3 |
| Architecture | Single activity with MVVM, `ViewModel`, and `StateFlow` |
| Navigation | Navigation Compose |
| Network | Retrofit, Gson, and OkHttp |
| Session storage | AndroidX Security Crypto |
| Local cache | Room database for projects, tasks, and dashboard data |
| Build | Gradle 9.5 and Android Gradle Plugin 9.3.1 |
| Android support | Min SDK 24; target and compile SDK 37 |

## Key design decisions

**Offline-aware reads.** Nexus keeps a Room cache of dashboards, projects, and tasks. A user can still review the most recently loaded work when connectivity is unavailable. Writes deliberately remain online-only for this milestone: silently queuing edits without a visible sync state or conflict policy would make task data less trustworthy. The final PoE will add a durable write queue, sync status, and a documented conflict-resolution rule.

**A custom REST API.** The app uses a small Nexus API rather than a third-party task service so its project, task, dashboard, and account rules match the assessment requirements and remain under the product team's control. Retrofit models make that contract explicit; authenticated requests carry a bearer token and the client does not embed third-party service credentials.

**JWT session authentication.** Registration and login return a JWT. The token is stored with AndroidX Security Crypto and attached by the networking layer to protected requests. The app clears the session when the user logs out or the API reports that a session has expired. Google SSO remains a planned extension because it needs a Google OAuth client ID, server-side ID-token verification, and an API endpoint that issues the same Nexus JWT as password login.

## Requirements

- Android Studio with Android SDK Platform 37 installed.
- JDK 17 or later for Gradle.
- An Android device or emulator running API 24 or later.
- A Nexus-compatible backend available at `10.0.2.2:5263` when running on an emulator.

## Getting started

1. Clone the repository and open it in Android Studio.
2. Allow Gradle sync to finish and install any missing Android SDK components.
3. Start the local backend so the emulator can reach `http://10.0.2.2:5263/api/`.
4. Select a device or emulator, then run the `app` configuration.
5. Register an account or sign in.

The `10.0.2.2` address is Android Emulator's alias for the development machine. A physical device cannot use this address; configure an accessible HTTPS backend before using one.

## Test credentials

The following accounts are reserved for development and verification. You can add them to your local system by running [`docs/seed_test_users.ps1`](docs/seed_test_users.ps1) while the backend is active.

| Name | Email | Password | Role / Purpose |
| --- | --- | --- | --- |
| Nexus Admin | `admin@nexus-app.com` | `nexusAdmin123` | System administration and global oversight. |
| Jane Doe | `jane.doe@nexus-app.com` | `janeDoe789` | Power user with multiple active projects. |
| Test User | `test.user@nexus-app.com` | `testUser456` | Standard user for regression testing. |

## Build and test

Run commands from the repository root.

```sh
# Linux or macOS
./gradlew test
./gradlew assembleDebug
./gradlew assembleRelease bundleRelease
```

```powershell
# Windows PowerShell
.\gradlew.bat test
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRelease bundleRelease
```

The debug APK is written to `app/build/outputs/apk/debug/`. Release APK and App Bundle outputs are written under `app/build/outputs/apk/release/` and `app/build/outputs/bundle/release/`.

Instrumented tests require a connected device or running emulator:

```sh
./gradlew connectedAndroidTest
```

## Continuous integration

[GitHub Actions](.github/workflows/android-ci.yml) runs unit tests and builds the debug APK, release APK, and release AAB on pushes to `main`, `master`, and `release/**`; on pull requests to `main` and `master`; and when manually started from the Actions tab.

Each run publishes two downloadable artifacts:

- `nexus-debug-apk`
- `nexus-release-artifacts`

The release artifacts are unsigned. Configure release signing with GitHub Actions secrets before distributing the app or publishing it to Google Play. Never commit a keystore or its passwords to the repository.

## Changelog

### Version 1.0 - current foundation

#### Added

- JWT registration, sign-in, sign-out, and persisted session handling.
- Animated splash experience and Compose navigation for authentication, dashboard, projects, and tasks.
- Dashboard counts for projects, tasks, and activity.
- Project and task creation, editing, deletion, and task completion.
- Retrofit API client with authorization headers and debug-only HTTP logging.
- Room-backed cache for projects, tasks, and dashboard data when reads cannot reach the backend.
- Material 3 interface, dynamic color support, loading states, retry actions, and empty states.
- GitHub Actions CI that tests the project and publishes APK/AAB build artifacts.
- A connected Settings screen with local system/light/dark preference, account display-name updates, language selection, and notification preference controls.
- Dashboard and project-list ViewModels now accept test fakes and have success, empty, and failure unit-test coverage.

#### Next improvements

The [roadmap](#roadmap) describes the planned user-facing improvements. The first release-quality milestone is task due dates, priority, a useful Today view, and a production HTTPS backend.

## Project improvement log

See [PROJECT_IMPROVEMENTS.md](PROJECT_IMPROVEMENTS.md) for a dated record of delivered improvements, the current implementation status, and the technical prerequisites for the next features.

## Backend API

The Android client uses `http://10.0.2.2:5263/api/` by default. The base URL is defined in `RetrofitClient.kt`.

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/auth/register` | Create an account with email, password, and display name. |
| `POST` | `/auth/login` | Sign in and receive a JWT. |
| `GET`, `PATCH` | `/users/me` | Read or partially update the signed-in user's display name, language, and notification preference. |
| `GET` | `/dashboard` | Get project, task, and activity counts. |
| `GET`, `POST` | `/projects` | List or create projects. |
| `GET`, `PUT`, `DELETE` | `/projects/{projectId}` | Read, update, or delete a project. |
| `GET`, `POST` | `/projects/{projectId}/tasks` | List or create a project's tasks. |
| `PUT`, `DELETE` | `/tasks/{taskId}` | Update or delete a task. |

Authenticated requests send `Authorization: Bearer <jwt>` automatically.

### Local data and offline behaviour

Nexus stores a local Room cache for dashboard data, projects, and tasks. When a read request fails, the app can show its cached data when available. Creating, updating, and deleting data still requires a connection; queued offline changes and conflict resolution are planned improvements.

## Testing

Run unit tests on your development machine:

```sh
./gradlew test
```

Run instrumentation tests on a connected device or emulator:

```sh
./gradlew connectedAndroidTest
```

Before a release, test sign-up, sign-in, sign-out, project and task CRUD, task completion, retry behaviour, cached reads, and navigation on both a small phone and a larger screen.

The unit suite covers authentication validation/network outcomes plus dashboard and project-list loading for populated, empty, and failed responses. The ViewModels accept loader functions in their constructors, so tests use deterministic fakes instead of a live API.

## Part 2 evidence

### Screenshots

Add real emulator/device screenshots to `docs/screenshots/` before submission: Dashboard, project creation, task completion, and Settings. Do not use mockups as assessment evidence.

### Demo video

**Submission link:** _add unlisted video URL after recording_. The narrated sequence should show registration, login, project creation, task creation/completion, Settings, and matching auth-store/PostgreSQL data. A ready-to-read script is in [docs/DEMO_SCRIPT.md](docs/DEMO_SCRIPT.md).

### Delivered in this Part 2 pass

- Settings is reachable from the Dashboard and supports a server-backed display name, language, and notification preference update; its theme preference is persisted locally.
- Task planning includes due dates, priorities, statuses, labels, checklists, list/board presentation, filtering, and dashboard attention sections.
- Unit tests cover populated, empty, and failed dashboard/project loading states without a network dependency.
- CI verifies unit tests and produces debug/release APK and AAB artifacts for each qualifying push or pull request.

### Final PoE scope

- Google SSO after server-side Google ID-token verification is available.
- Offline write queue with visible sync status and conflict resolution.
- Push-notification delivery and user-controlled reminder scheduling.
- Complete localized UI strings once the selected-language contract is supported throughout the client.

## Project structure

```text
app/src/main/
|- AndroidManifest.xml
|- java/com/example/nexus/
|  |- api/           # Retrofit service, API models, and client
|  |- auth/          # Session state and secure token storage
|  |- data/          # Room entities, DAOs, database, and repository
|  |- ui/            # Compose screens, ViewModels, navigation, and theme
|  |- util/          # Shared utilities
|  |- MainActivity.kt
|  `- NexusApp.kt
`- res/              # Resources, icons, themes, and XML configuration

.github/workflows/
`- android-ci.yml    # GitHub Actions build and artifact workflow
```

## Security and release notes

- The development endpoint uses HTTP only for `10.0.2.2`; replace it with an HTTPS endpoint for production and remove the cleartext exception in `res/xml/network_security_config.xml`.
- HTTP request-body logging is enabled only in debug builds. Keep it disabled in release builds to prevent credentials or tokens reaching logcat.
- Review the backup and data-extraction rules before release to ensure tokens are excluded from device and cloud backups.
- Do not commit API keys, keystores, passwords, or other production secrets.

## Contributing

1. Keep changes focused and include tests where practical.
2. Follow the existing single-activity MVVM structure.
3. Run `./gradlew test` before opening a pull request.
4. Add ProGuard/R8 keep rules for reflection-based types when minification is enabled.

## License

No license has been selected yet. Until one is added, all rights are reserved by the project authors.
