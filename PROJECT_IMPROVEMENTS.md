# Project Improvements Log

This file records meaningful project changes and keeps planned work separate from features that are already delivered. Add a new dated section whenever a feature, technical improvement, or release-quality change is completed.

## 2026-09-24

### Android Structural Remediation Pass Delivered

Full structural remediation of the Android codebase covering architecture, repository separation, dependency injection, domain logic isolation, package organization, navigation safety, and testing infrastructure:

- **AuthViewModel Repository Bypass Resolution:** Fixed the repository bypass in `AuthViewModel` by routing all authentication workflows (`login`, `register`, `updateProfile`) through `UserRepository` instead of making direct network or token calls.
- **Monolithic Repository Separation:** Disassembled the monolithic repository into six single-responsibility repositories (`ProjectRepository`, `TaskRepository`, `HabitRepository`, `DashboardRepository`, `UserRepository`, `ConfigRepository`) in `com.example.nexus.data`, cleanly decoupling data access concerns across domain entities.
- **Dependency Injection Standardization & Hilt Evaluation:** Conducted an architectural evaluation of Hilt vs. Manual DI (Step 3b). Retained the lightweight, application-level manual DI container in `NexusApp` (`projectRepository`, `taskRepository`, `habitRepository`, `dashboardRepository`, `userRepository`, `configRepository`) and standardized constructor injection with default parameter fallbacks across all ViewModels, eliminating direct instance instantiations in ViewModels while enabling deterministic test overrides.
- **Business Logic Relocation:** Relocated streak scoring, bonus points, level calculations, and badge entitlement rules out of ViewModels/UI into a pure domain layer (`com.example.nexus.domain.HabitScoring`), backed by dedicated unit test coverage (`HabitScoringTest`).
- **Feature-Based Package Reorganization:** Restructured the flat UI package layout into feature-focused subpackages under `com.example.nexus.ui` (`auth`, `components`, `dashboard`, `habits`, `navigation`, `profile`, `projects`, `settings`, `splash`, `tasks`, `theme`), establishing clean structural boundaries between user interface components and core architectural layers (`api`, `auth`, `data`, `domain`, `settings`, `util`).
- **Type-Safe Navigation Migration:** Replaced legacy string-based route paths with Navigation Compose 2.8.0 Kotlin Serialization `@Serializable` type-safe routes (`Route.Splash`, `Route.Login`, `Route.Register`, `Route.Dashboard`, `Route.Projects`, `Route.ProjectDetail`, `Route.Tasks`, `Route.TaskDetail`, `Route.Habits`, `Route.Settings`, `Route.Profile`) defined in `NexusNavHost.kt`.
- **Consolidated Shared Test Fakes:** Standardized testing infrastructure by consolidating ad-hoc test doubles into a central set of shared fakes under `app/src/test/java/com/example/nexus/fakes/` (`FakeProjectRepository`, `FakeTaskRepository`, `FakeHabitRepository`, `FakeDashboardRepository`, `FakeUserRepository`, `FakeConfigRepository`, `FakeAuthSession`, `FakeSettingsSession`), eliminating duplication across unit tests.

## 2026-09-23

### Backend Remediation Pass Delivered

- **Write-Locking & Atomic Persistence:** Implemented an in-process `AsyncMutex` queue to serialize all read/write operations against `nexus-data.json` and added atomic file persistence (writing to `.tmp` then performing `rename()`) to eliminate data-loss race conditions and file corruption during concurrent operations or process crashes.
- **Strict Input Validation:** Integrated strict Zod validation schemas for all user registration, profile update, password change, project, task, and habit endpoints, rejecting malformed or unvalidated payloads with clear validation error codes before store access.
- **Authentication Security & Rate Limiting:** Enforced strict startup validation for `JWT_SECRET` (requiring at least 32 non-placeholder characters), configured 24-hour JWT expiry, secured password hashing with bcrypt (work factor 12), and added `express-rate-limit` throttling (10 requests per 15 minutes per IP) on `/auth/register` and `/auth/login` to prevent brute-force attacks.
- **Observability and Liveness Probes:** Added unauthenticated `GET /health` and `GET /api/health` endpoints returning process uptime, and integrated structured logging via Pino with automatic redaction of sensitive fields (passwords, tokens, auth headers) and request correlation ID tracking.

## 2026-09-22

### Delivered today

- Added GitHub Actions CI to run unit tests and build debug APK, release APK, and release AAB artifacts.
- Added Gradle wrapper validation, JDK 17 setup, Gradle caching, artifact retention, and a restricted CI permission scope.
- Updated Git ignore rules so generated release output and Kotlin build state are not committed.
- Reworked the README into a structured project document with setup, build, CI, API, security, contribution, and licensing guidance.
- Added a product-focused app introduction, product principles, roadmap, changelog, local-cache documentation, and release testing checklist.
- Documented the Room cache used for dashboard, project, and task reads when the backend is unavailable.
- Added a repository-owned animated SVG preview of the Nexus splash sequence and linked it from the README. The preview is documentation only; the production splash remains the Compose implementation.
- Added delivery briefs for the first roadmap items so each one starts with a user problem, smallest useful version, and measurable outcome.

### Current app foundation

- JWT account registration, sign-in, sign-out, and persisted session state.
- Project and task CRUD with task completion.
- Dashboard totals for projects, tasks, and activity.
- Compose navigation, Material 3 UI, loading, retry, and empty states.
- Retrofit API access with bearer-token authorization and debug-only HTTP logging.
- Room-backed fallback cache for read operations.

### Next implementation milestone: task planning

The first product milestone should be delivered as a vertical slice: API contract, database migration, repository mapping, ViewModel state, Compose UI, and tests in one pull request.

| Feature | Backend/data requirement | Android work | Definition of done |
| --- | --- | --- | --- |
| Due date | Add nullable `dueAt` timestamp to task create, update, and response payloads. | Add date picker, formatting, overdue calculation, and Room migration. | A due date survives refresh and is clearly marked when overdue. |
| Priority | Add `priority` enum: `low`, `medium`, `high`. | Add priority selector, accessible label, sort/filter support, and Room migration. | Users can set and filter priority; high-priority tasks are distinguishable without colour alone. |
| Status | Add a task status enum or retain completion plus a nullable `blockedReason`. | Add status chips and a filter. | Open, completed, and blocked tasks are visibly different. |
| Labels | Add label entity or a `labels` collection in task payloads. | Add label picker and filter chips. | Labels can be added, removed, and used to narrow a task list. |
| Today view | Support due-date and priority queries, or return all task metadata. | Add a dashboard section for overdue, today, and upcoming tasks. | The view loads quickly and always explains why a task appears there. |

### Planned after the task-planning milestone

1. Subtasks, checklists, search, sorting, and list/board views.
2. Reminders, recurring tasks, notes, attachments, templates, archive, and progress reviews.
3. Calendar and timeline views, theme controls, and accessibility audit.
4. Shared projects, roles, comments, mentions, activity history, and notifications.
5. Offline write queue, sync status, conflict resolution, account recovery, import/export, widgets, tablet layouts, and privacy-respecting analytics.

### Important constraints before implementation

- The current task API payload only contains title, description, and completion state. Due dates, priority, labels, subtasks, reminders, collaboration, and sync cannot be implemented reliably in the Android app until the backend accepts and returns their data.
- Room schema changes require a proper migration for existing users. Do not rely on destructive migration for a production release.
- Push notifications require a notification provider, backend device-token registration, notification permissions, and user preferences.
- Collaboration requires server-side authorization and audit rules; it must not be implemented only in the client.

### Database Persistence Roadmap Evaluation

Although Nexus's JSON-file persistence (`nexus-data.json`) has been successfully hardened with thread-safe write-locking (`AsyncMutex`) and atomic file writes (`.tmp` + `rename`), it remains an in-memory document store bounded by single-threaded write serialization. For the project's current test volume (<50 users, <10,000 total tasks), this approach is genuinely sufficient and operationally zero-config. However, as advanced task-planning features (due dates, priorities, labels, and complex queries) are introduced in the next milestone, file-based persistence should be migrated to embedded SQLite via `better-sqlite3` to ensure ACID compliance, relational integrity, and scalable indexing without introducing a separate database server process.

### Quality checklist for every feature

- Define the user problem and one measurable success outcome.
- Update API models and backend validation first.
- Add a database migration when persistent local data changes.
- Cover happy path, loading, offline, retry, unauthorized, and validation states.
- Add unit tests for ViewModel logic and instrumentation coverage for the main user path.
- Check accessibility: labels, touch targets, keyboard navigation, contrast, and reduced motion.
- Update `README.md` and this log when the feature is complete.
