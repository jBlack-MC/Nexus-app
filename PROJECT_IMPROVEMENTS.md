# Project Improvements Log

This file records meaningful project changes and keeps planned work separate from features that are already delivered. Add a new dated section whenever a feature, technical improvement, or release-quality change is completed.

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

### Quality checklist for every feature

- Define the user problem and one measurable success outcome.
- Update API models and backend validation first.
- Add a database migration when persistent local data changes.
- Cover happy path, loading, offline, retry, unauthorized, and validation states.
- Add unit tests for ViewModel logic and instrumentation coverage for the main user path.
- Check accessibility: labels, touch targets, keyboard navigation, contrast, and reduced motion.
- Update `README.md` and this log when the feature is complete.
