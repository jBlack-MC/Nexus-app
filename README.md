# Nexus

A task & project manager for Android built with Kotlin and Jetpack Compose.
Nexus lets users authenticate with a backend via JWT, view a dashboard of
project/task activity, and create / edit / delete projects and tasks in a
nested navigation flow.

> **Status:** Active development. The app currently targets a local emulator
> backend (`http://10.0.2.2:5263`); see [Local backend](#local-backend) below.

## Features

- **JWT authentication** — login & register, with session persistence.
- **Dashboard** — at-a-glance view of project, task, and activity counts.
- **Projects & Tasks** — full CRUD for projects, and nested CRUD for a project's
  tasks (create, edit, complete, delete).
- **Theming** — Material 3 with dynamic color on Android 12+ and a custom
  brand theme otherwise.
- **Intro animation** — an animated splash logo sequence that respects
  `prefers-reduced-motion`.

## Screenshots

| Splash | Login | Dashboard |
| --- | --- | --- |
| _screenshots/splash.png_ | _screenshots/login.png_ | _screenshots/dashboard.png_ |

*(Screenshot assets are not yet committed. Drop them under `screenshots/` and
update the links above.)*

## Tech stack

| Layer | Library / API |
| --- | --- |
| Language | Kotlin 2.2.10 |
| UI | Jetpack Compose (Material 3) |
| Build | Android Gradle Plugin 9.3.1, Compose BOM 2026.02.01 |
| Architecture | Single-activity, MVVM (`ViewModel` + `StateFlow` + `runCatching`) |
| Navigation | `androidx.navigation:navigation-compose` |
| Network | Retrofit 3.0 + Gson + OkHttp (auth + logging interceptors) |
| Persistence | AndroidX `security-crypto` — `EncryptedSharedPreferences` |
| Min SDK | 24 |
| Target / Compile SDK | 37 |

## Project structure

```
app/src/main/
├── AndroidManifest.xml            # Single activity, network-security config
├── java/com/example/nexus/
│   ├── NexusApp.kt                # Application; initializes auth session
│   ├── MainActivity.kt            # setContent { NexusTheme { NexusNavHost } }
│   ├── api/                       # Network layer
│   │   ├── RetrofitClient.kt      # OkHttp client + auth/logging interceptors
│   │   ├── ApiService.kt          # Typed REST endpoints
│   │   ├── AuthModels.kt          # register/login request + auth response
│   │   ├── DashboardData.kt
│   │   └── ProjectAndTaskModels.kt
│   ├── auth/
│   │   ├── AuthSession.kt        # In-process auth/session state holder
│   │   └── SecureTokenStore.kt   # Local token persistence
│   ├── ui/
│   │   ├── AuthViewModel.kt       # login/register state machine
│   │   ├── DashboardScreen/ViewModel
│   │   ├── LoginScreen, RegisterScreen, SplashIntroScreen
│   │   ├── components/            # NexusLogo, EmptyState
│   │   ├── navigation/NexusNavHost.kt
│   │   ├── projects/              # Project list + detail screens + ViewModels
│   │   ├── tasks/                 # Task list screen + ViewModel
│   │   └── theme/                 # Color, Type, Theme
│   └── util/
│       └── TokenManager.kt        # EncryptedSharedPreferences helper
└── res/
    ├── values/      # strings, colors, themes
    ├── xml/         # backup rules, data-extraction rules, network-security config
    └── mipmap-*/    # launcher icons
```

### Navigation graph

```
splash  ──(token?)──▶  dashboard
        └──(no token)──▶  login  ──► register
                             │
                             ▼
                          dashboard  ──►  projects
                                            │
                                            ├──▶ project detail (tasks)
                                            │       │
                                            │       ├── create/edit project
                                            │       ├── create/edit task
                                            │       └── delete
                                            └──► tasks (by project)
```

## Build & run

**Requirements**
- Android Studio (Giraffe or later, matching AGP 9.3.1)
- JDK 17+
- A running backend reachable at `10.0.2.2:5263` from the emulator
  (see [Local backend](#local-backend)).

**Steps**
1. Open the project in Android Studio.
2. `./gradlew build` to compile, or press ▶ in Studio to run on a device/emulator
   (API 24+ recommended).
3. Register or log in to obtain a session.

> **Note for production:** the shipped `BASE_URL` is an HTTP emulator-local
> address and cleartext is explicitly permitted **only** for `10.0.2.2` in
> `res/xml/network_security_config.xml`. Before shipping to a real backend,
> switch the base URL to your HTTPS endpoint and remove the cleartext config.

### Continuous integration

GitHub Actions builds the unit tests, debug APK, and unsigned release APK/AAB
on pushes to `main`, `master`, or `release/**`, and pull requests to `main` or
`master`. You can also start it from
the repository's **Actions** tab. Download the resulting files from the
workflow run's artifacts: `nexus-debug-apk` and `nexus-release-artifacts`.

Release files are intentionally unsigned. To publish to Google Play, configure
a release signing key through GitHub repository secrets and add a signing step;
never commit a keystore or its passwords to the repository.

### Local backend

Nexus expects a JSON API at `http://10.0.2.2:5263/api/`. Endpoints (see
`ApiService.kt`):

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `auth/register` | register (`email`, `password`, `displayName`) → `{ token }` |
| `POST` | `auth/login` | login → `{ token }` |
| `GET` | `dashboard` | `{ projects, tasks, activity }` (auth required) |
| `GET` | `projects` | list projects (auth required) |
| `GET` | `projects/{projectId}` | project by id |
| `POST` | `projects` | create project |
| `PUT` | `projects/{projectId}` | update project |
| `DELETE` | `projects/{projectId}` | delete project |
| `GET` | `projects/{projectId}/tasks` | list tasks for a project |
| `POST` | `projects/{projectId}/tasks` | create task |
| `PUT` | `tasks/{taskId}` | update task |
| `DELETE` | `tasks/{taskId}` | delete task |

The auth token is sent as `Authorization: Bearer <jwt>` via an OkHttp
interceptor (`RetrofitClient.kt`).

## Security notes

- **Secrets:** do not commit any keystore, API key, or production credentials.
  Backend credentials must live in environment-driven gradle properties or a
  CI secrets store, not in source.
- **Logging:** HTTP logging is intended for **debug** builds only. Ensure the
  logging interceptor level is gated on `BuildConfig.DEBUG` before release so
  auth tokens and credentials are never written to logcat.
- **Backup:** review `res/xml/backup_rules.xml` and `data_extraction_rules.xml`
  to ensure anything sensitive (token storage) is excluded from cloud/device
  backup.
- **TLS:** all production traffic must use HTTPS; the cleartext exception is
  scoped to the local emulator host only.

## Testing

```
./gradlew test                  # unit tests (host JVM)
./gradlew connectedAndroidTest  # instrumentation tests (device/emulator)
```

The project currently ships only the default template tests
(`ExampleUnitTest.kt`, `ExampleInstrumentedTest.kt`). Contributions adding
coverage for `AuthViewModel`, the dashboard / projects / tasks ViewModels, and
the auth-gated navigation flow are encouraged.

## Contributing

1. Open an issue or describe scope before large changes.
2. Follow the existing architecture: single-activity + MVVM
   (`ViewModel` + `StateFlow` + `runCatching`).
3. Match the code style (`kotlin.code.style=official`, set in `gradle.properties`).
4. When introducing reflection-based types (e.g. Gson) and re-enabling R8,
   add matching keep rules to `app/src/main/keepRules/rules.keep`.

## License

Specify a license here (e.g. MIT or Apache-2.0). Until one is chosen, all rights
remain with the project authors.
