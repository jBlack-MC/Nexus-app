# Running Nexus online and offline

## Start the included API

From `backend` in PowerShell:

```powershell
npm.cmd ci
$env:JWT_SECRET = node -e "console.log(require('crypto').randomBytes(48).toString('base64url'))"
npm.cmd start
```

Keep the same secret between restarts if existing sessions should remain valid. The API listens on port 5263; `GET /health` checks it. The Android emulator reaches your computer at `http://10.0.2.2:5263/api/`, which is the debug default.

For a phone on your LAN, build with your computer's reachable LAN address:

```powershell
.\gradlew.bat :app:assembleDebug -PNEXUS_API_URL=http://192.168.1.20:5263/api/
```

Replace the example address. Allow the development server port through your local firewall if needed. HTTP is permitted only in debug builds.

## Connect a public API

Deploy `backend` on a Node-capable host or use its Dockerfile. Set a persistent `JWT_SECRET`, expose the service behind HTTPS, and mount persistent storage at `/data` for the container. `DATABASE_PATH` sets the data file for a non-container host. The included `compose.yaml` creates a persistent volume:

```powershell
cd backend
docker compose up --build -d
```

Use a single API instance with this JSON-file store; multiple replicas must not share it. Back up the data volume. TLS termination and the domain are supplied by your host.

Build Android against the actual deployed URL:

```powershell
.\gradlew.bat :app:assembleRelease -PNEXUS_API_URL=https://your-host/api/
```

`NEXUS_API_URL` can also be an environment variable. Release builds reject an HTTP endpoint. Configure the GitHub repository variable `NEXUS_API_URL` to enable release artifacts in CI; debug builds and tests run without it. Release signing remains a separate publishing step. No public API has been provisioned by these changes.

## Offline behavior

- Register or sign in once with the API available. Keep that session to work offline. Registration, fresh sign-in, account deletion and password changes require the API.
- Projects, tasks and habits save into Room together with an ordered upload record in one transaction. A save survives closing and reopening the app.
- Dashboard refresh downloads projects and their tasks for offline access. Habits and profile details are cached when opened. Data never downloaded to a device cannot appear offline.
- The app uploads queued changes during refresh, on network availability, via **Sync**, and every 30 seconds while its process is alive. If Android stops the process, syncing resumes when Nexus next opens; this implementation does not promise uploads while the app is terminated.
- The banner shows queued changes, network failures, expired sessions and conflicts. Saved changes remain on the device after sign-out and are uploaded only when the same account signs in again. Explicit account deletion removes that account's local data.
- Failed or unauthorized uploads stay queued. Each operation has a durable server receipt, so retrying a request after a lost response does not duplicate it.
- Changes to an existing item use last-upload-wins semantics. An edit to an item deleted on another device stops the queue with a conflict. **Review** lets you keep the edits or explicitly discard that item's queued edits. Refresh afterwards to reload server data. This is not automatic field-level merging.
- Room migration 4 → 5 adds the upload queue without deleting existing cached rows. Existing migration 3 → 4 remains in place.

## Acceptance check

1. Start the API; sign in; create a project and task; check **Sync** clears pending changes.
2. Stop the API. Create another project, a task under it and a habit; edit and complete items.
3. Force-close and reopen Nexus. Confirm the saved items and pending count remain.
4. Restart the API. Tap **Sync**, then refresh. Confirm there are no duplicates.
5. Sign out while offline changes exist. A different account must not see or upload them; signing into the original account must resume them.
6. Delete an item through another client, then upload an offline edit to it. Confirm the conflict is visible and its edit is retained until explicitly discarded.

Automated coverage includes API operation replay, durable receipts, account ownership, delete conflicts, Android queue retry behavior, and an instrumented Room persistence test.
