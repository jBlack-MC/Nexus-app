# Google Play publication kit: Nexus 2.0.0

This kit has everything needed to create the Play Console listing. Every Data safety answer below comes from what the code actually does. Items marked **TEAM ACTION** need your accounts or your decisions.

## Files

| File | Play Console field | Spec check |
| --- | --- | --- |
| `assets/icon-512.png` | App icon | 512×512, 32-bit PNG with alpha, same artwork as the launcher icon |
| `assets/feature-graphic-1024x500.png` | Feature graphic | 1024×500, 24-bit PNG |
| `assets/phone/*.png` (8) | Phone screenshots | 1080×2160 (2:1), 24-bit PNG, captured from the running app on an Android 7.0 emulator on 2026-11-05 with fictional demo data |
| `privacy-policy.html` | Privacy policy URL | **TEAM ACTION:** publish it (e.g. GitHub Pages) and fill in the contact email |

Regenerate the assets with `node docs/play-store/build-assets.mjs` after recapturing `docs/screenshots/part3/`.

## Store listing (English)

- **App name:** Nexus
- **Short description (79/80 characters):** Plan projects, tasks and habits, even offline, in English, isiZulu or Setswana.
- **Category:** Productivity
- **Full description:**

> Nexus brings your projects, tasks and daily habits together in one calm workspace.
>
> • Today view: see what is overdue, due today and coming up next.
> • Projects and tasks: due dates, priority, status, labels and checklists, with search and filters.
> • Habits: daily or weekly check-ins, streaks, points, levels and badges.
> • Works offline: everything you change is saved on your phone first and syncs automatically when you reconnect.
> • Reminders: a notification when tasks are overdue or due today, and real-time alerts when your account changes on another device.
> • Your language: English, isiZulu and Setswana.
> • Sign in with email and password or with your Google account.
> • Light, dark or system theme. Export your habit history as CSV. Delete your account and data at any time from Settings.

## Data safety form (answers derived from the code)

| Question | Answer | Evidence |
| --- | --- | --- |
| Does the app collect or share user data? | Collects; does not share | API stores account and content; no analytics or ad SDKs in `app/build.gradle.kts` |
| Personal info → Email address | Collected, required, for account management | `RegisterRequest`, `/api/auth/*` |
| Personal info → Name | Collected (display name), required, for app functionality | `displayName` |
| Personal info → User IDs | Collected (account ID; Google account ID when signing in with Google) | `user.id`, `googleSub` |
| App activity → Other user-generated content | Collected (projects, tasks, habits), for app functionality | `/api/sync`, `/api/projects`, `/api/habits` |
| Device or other IDs | Collected (FCM registration token) only when push is enabled, for notifications | `POST /users/me/devices` |
| Is data encrypted in transit? | Yes. Release builds refuse plain HTTP | `network_security_config.xml`, `preReleaseBuild` check |
| Can users request deletion? | Yes, in-app: Settings → Delete account and data | `DELETE /api/users/me` |
| Data shared with third parties | None. Google processes FCM tokens and Google sign-in as a service provider | `push.js`, `GoogleSignIn.kt` |

## Content rating and audience (recommendations)

- **Content rating questionnaire:** no violence, sexual content, profanity, gambling or controlled substances. Users cannot communicate with each other, and content is private to the account. The expected rating is "Everyone" / PEGI 3; Play assigns the final rating.
- **Target audience:** 18+ is recommended, which avoids the Families policy requirements. **TEAM ACTION:** confirm.
- **Ads:** No ads.

## Release steps

1. **TEAM ACTION:** Deploy the API over HTTPS (`docs/deliverables`, NX-11-DEP) with `JWT_SECRET`, optional `GOOGLE_CLIENT_ID` and optional `FIREBASE_SERVICE_ACCOUNT_BASE64`.
2. Create the upload key once with `scripts/create-upload-keystore.ps1` (already done on the development PC on 2026-11-05; back up `%USERPROFILE%\.nexus\nexus-upload.jks` and `keystore.properties`).
3. Build the signed bundle: `.\gradlew.bat :app:bundleRelease -PNEXUS_API_URL=https://<host>/api/ [-PGOOGLE_WEB_CLIENT_ID=<id>.apps.googleusercontent.com]`. The output is `app/build/outputs/bundle/release/app-release.aab`.
4. **TEAM ACTION:** In Play Console, create the app, complete App content (privacy policy URL, Data safety, content rating, target audience, ads), upload the AAB to an **Internal testing** track, add the listing assets above, and take a screenshot of the Console as PoE evidence.
