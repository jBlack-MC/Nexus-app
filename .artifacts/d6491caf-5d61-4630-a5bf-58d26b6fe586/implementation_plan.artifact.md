# Implementation Plan: Offline Authentication and Local User Persistence

The user wants to be able to log in with pre-seeded test credentials and create new accounts locally without a network connection. Since the app currently relies on a backend for registration and login, it shows "no internet connection" when the server is unreachable.

We will implement an `OfflineInterceptor` in the OkHttp client. This interceptor will catch network errors and fulfill authentication and data-modification requests locally using a mock JSON response and a local `SharedPreferences`-backed user store. This allows the entire app to function fully standalone (offline) while still leveraging the existing Room database for caching and local persistence.

## User Review Required

> [!IMPORTANT]
> The app will now have a local fallback for authentication. If the server is unreachable, the app will simulate a successful login or registration. Registered accounts will be stored in `SharedPreferences` on the device.

## Proposed Changes

### [Core Application]

#### [MODIFY] [NexusApp.kt](file:///C:/Users/Clari/AndroidStudioProjects/Nexus/app/src/main/java/com/example/nexus/NexusApp.kt)
- Expose the global application `instance` to allow access to the application context from the networking layer.

### [Networking & Offline Fallback]

#### [NEW] [LocalAccountManager.kt](file:///C:/Users/Clari/AndroidStudioProjects/Nexus/app/src/main/java/com/example/nexus/auth/LocalAccountManager.kt)
- Create a manager to handle the 3 pre-seeded test users and store new local registrations in `SharedPreferences`.

#### [NEW] [OfflineInterceptor.kt](file:///C:/Users/Clari/AndroidStudioProjects/Nexus/app/src/main/java/com/example/nexus/api/OfflineInterceptor.kt)
- Implement an OkHttp `Interceptor` that:
    - Catches `java.io.IOException` (no network).
    - Intercepts `auth/login` and `auth/register` to handle them via `LocalAccountManager`.
    - Simulates successful responses for data-writing endpoints (`POST`/`PUT`/`DELETE` for projects and tasks) when offline, allowing the app's `NexusRepository` to update the local Room database successfully.
    - Provides mock data for `users/me` and `dashboard` when the network is unavailable.

#### [MODIFY] [RetrofitClient.kt](file:///C:/Users/Clari/AndroidStudioProjects/Nexus/app/src/main/java/com/example/nexus/api/RetrofitClient.kt)
- Add the `OfflineInterceptor` to the `OkHttpClient`.

## Verification Plan

### Manual Verification
- **Test Pre-seeded Users:** Attempt to log in with `admin@nexus-app.com` / `nexusAdmin123` while the backend is NOT running (or in Airplane mode). The app should log in successfully and show the dashboard.
- **Test Offline Registration:** Attempt to register a new account while offline. The app should succeed, store the user locally, and log in.
- **Test Persistence:** Close and re-open the app. Log in again with the newly created local account. It should succeed.
- **Test Data Creation:** While offline, create a new Project and a new Task. They should be saved to the local Room database and appear in the UI immediately.
