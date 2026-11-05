// Tests for the Part 3 additions: Google SSO, push device registration and real-time sync
// notifications, case-insensitive email, and the fail-safe data-file load (AUD-01).
// The shared auth rate limiter allows 10 auth requests per test file, so setup that does not need
// the auth endpoints writes users directly.
import request from "supertest";
import jwt from "jsonwebtoken";
import { spawnSync } from "node:child_process";
import { randomUUID } from "node:crypto";
import fs from "node:fs/promises";
import os from "node:os";
import path from "node:path";
import { app, loadDatabase, users, projects, tasks, habits, databaseFile, setGoogleTokenVerifier } from "../server.js";
import { setPushSender } from "../push.js";

const SECRET = process.env.JWT_SECRET;
const tokenFor = (user) => jwt.sign({ sub: user.id }, SECRET);
const addUser = (fields = {}) => {
  const user = { id: randomUUID(), email: `${randomUUID()}@example.com`, displayName: "T", language: "en", notificationsEnabled: true, ...fields };
  users.set(user.id, user);
  return user;
};

afterEach(async () => {
  users.clear(); projects.clear(); tasks.clear(); habits.clear();
  setPushSender(null);
  await fs.rm(databaseFile, { force: true });
  await fs.rm(databaseFile + ".bak", { force: true });
});

describe("AUD-01: unreadable data file", () => {
  test("loadDatabase rejects and leaves the damaged file untouched", async () => {
    const damaged = '{"users":[{"id":"u1","email":"keep@example.com"}], TRUNCATED';
    await fs.writeFile(databaseFile, damaged);
    await expect(loadDatabase()).rejects.toMatchObject({ code: "DATABASE_UNREADABLE" });
    expect(await fs.readFile(databaseFile, "utf8")).toBe(damaged);
  });

  test("the server refuses to start instead of starting empty", async () => {
    const dir = await fs.mkdtemp(path.join(os.tmpdir(), "nexus-"));
    const file = path.join(dir, "nexus-data.json");
    await fs.writeFile(file, "{ not json");
    const result = spawnSync("node", ["server.js"], {
      env: { ...process.env, NODE_ENV: "production", DATABASE_PATH: file, PORT: "0" },
      cwd: path.resolve("."), timeout: 20000,
    });
    expect(result.status).toBe(1);
    expect(result.stdout.toString()).toContain("Refusing to start");
    expect(await fs.readFile(file, "utf8")).toBe("{ not json");
    await fs.rm(dir, { recursive: true, force: true });
  });

  test("each save keeps a .bak copy of the previous file", async () => {
    const user = addUser();
    await request(app).post("/api/projects").set("Authorization", `Bearer ${tokenFor(user)}`).send({ name: "One" });
    await request(app).post("/api/projects").set("Authorization", `Bearer ${tokenFor(user)}`).send({ name: "Two" });
    const backup = JSON.parse(await fs.readFile(databaseFile + ".bak", "utf8"));
    expect(backup.projects[user.id]).toHaveLength(1);
  });
});

describe("Email handling", () => {
  test("registration and sign-in are case-insensitive", async () => {
    const reg = await request(app).post("/api/auth/register").send({ email: "Mixed.Case@Example.com", password: "secret123", displayName: "Mixed" });
    expect(reg.status).toBe(201);
    const login = await request(app).post("/api/auth/login").send({ email: "mixed.case@example.com", password: "secret123" });
    expect(login.status).toBe(200);
    const dup = await request(app).post("/api/auth/register").send({ email: "MIXED.CASE@EXAMPLE.COM", password: "secret123", displayName: "Dup" });
    expect(dup.status).toBe(409);
    expect(dup.body.error.code).toBe("EMAIL_ALREADY_REGISTERED");
  });
});

describe("Google single sign-on", () => {
  const google = (idToken) => request(app).post("/api/auth/google").send({ idToken });
  const stub = (claims) => setGoogleTokenVerifier(async (token) => {
    if (token.startsWith("bad")) throw new Error("Invalid token signature");
    return claims;
  });

  test("first sign-in creates an account, later sign-ins reuse it, and a password login is refused", async () => {
    stub({ sub: "google-123", email: "Sso.User@Gmail.com", email_verified: true, name: "Sso User" });
    const first = await google("valid-google-id-token-0001");
    expect(first.status).toBe(201);
    const second = await google("valid-google-id-token-0002");
    expect(second.status).toBe(200);
    const id1 = jwt.verify(first.body.token, SECRET).sub;
    expect(jwt.verify(second.body.token, SECRET).sub).toBe(id1);
    const user = users.get(id1);
    expect(user.email).toBe("sso.user@gmail.com");
    expect(user.passwordHash).toBeUndefined();

    const passwordLogin = await request(app).post("/api/auth/login").send({ email: "sso.user@gmail.com", password: "anything" });
    expect(passwordLogin.status).toBe(401);
    const change = await request(app).post("/api/users/me/password").set("Authorization", `Bearer ${first.body.token}`)
      .send({ currentPassword: "x", newPassword: "newpass1" });
    expect(change.status).toBe(409);
    expect(change.body.error.code).toBe("PASSWORD_NOT_SET");
  });

  test("links to an existing email/password account with the same email", async () => {
    const existing = addUser({ email: "linked@example.com", passwordHash: "$2a$12$abcdefghijklmnopqrstuuJ6v9JmQyQyQyQyQyQyQyQyQyQyQyQyQy" });
    stub({ sub: "google-456", email: "linked@example.com", email_verified: true });
    const res = await google("valid-google-id-token-0003");
    expect(res.status).toBe(200);
    expect(jwt.verify(res.body.token, SECRET).sub).toBe(existing.id);
    expect(users.get(existing.id).googleSub).toBe("google-456");
  });

  test("rejects invalid tokens and unverified emails", async () => {
    stub({ sub: "google-789", email: "unverified@example.com", email_verified: false });
    expect((await google("bad-google-id-token-000")).status).toBe(401);
    const unverified = await google("valid-google-id-token-0004");
    expect(unverified.status).toBe(401);
    expect(unverified.body.error.code).toBe("INVALID_GOOGLE_TOKEN");
  });
});

describe("Push notifications", () => {
  test("devices register, deduplicate and unregister", async () => {
    const user = addUser();
    const auth = `Bearer ${tokenFor(user)}`;
    expect((await request(app).post("/api/users/me/devices").set("Authorization", auth).send({ token: "device-token-A-123" })).status).toBe(204);
    await request(app).post("/api/users/me/devices").set("Authorization", auth).send({ token: "device-token-A-123" });
    await request(app).post("/api/users/me/devices").set("Authorization", auth).send({ token: "device-token-B-456" });
    expect(users.get(user.id).deviceTokens).toEqual(["device-token-A-123", "device-token-B-456"]);
    expect((await request(app).delete("/api/users/me/devices/device-token-A-123").set("Authorization", auth)).status).toBe(204);
    expect(users.get(user.id).deviceTokens).toEqual(["device-token-B-456"]);
    expect((await request(app).post("/api/users/me/devices").send({ token: "device-token-C-789" })).status).toBe(401);
  });

  test("test notification reports when push is not configured", async () => {
    const user = addUser({ deviceTokens: ["device-token-A-123"] });
    const res = await request(app).post("/api/users/me/notifications/test").set("Authorization", `Bearer ${tokenFor(user)}`);
    expect(res.status).toBe(200);
    expect(res.body).toEqual({ configured: false, sent: 0, devices: 1 });
  });

  test("a synced change notifies the user's other devices and prunes dead tokens", async () => {
    const sent = [];
    setPushSender(async (message) => {
      sent.push(message);
      return { sent: message.tokens.length, invalidTokens: ["dead-token-000000"] };
    });
    const user = addUser({ deviceTokens: ["phone-token-111111", "tablet-token-22222", "dead-token-000000"] });
    const res = await request(app).post("/api/sync").set("Authorization", `Bearer ${tokenFor(user)}`)
      .set("X-Nexus-Device-Token", "phone-token-111111")
      .send({ operationId: randomUUID(), resource: "projects", entityId: randomUUID(), action: "create", payload: { name: "Thesis" } });
    expect(res.status).toBe(204);
    await new Promise((resolve) => setTimeout(resolve, 20)); // notification is sent after the response
    expect(sent).toHaveLength(1);
    expect(sent[0].tokens).toEqual(["tablet-token-22222", "dead-token-000000"]);
    expect(sent[0].body).toBe('Project "Thesis" added on another device');
    expect(users.get(user.id).deviceTokens).not.toContain("dead-token-000000");
  });

  test("app config advertises which optional features are enabled", async () => {
    const res = await request(app).get("/api/app/config");
    expect(res.body.featureFlags).toMatchObject({ googleSignIn: false, pushNotifications: false });
  });
});

describe("AUD-03: validation messages", () => {
  test("invalid input returns 400 with a field message (Zod 3 and 4 compatible)", async () => {
    const user = addUser();
    const res = await request(app).post("/api/projects").set("Authorization", `Bearer ${tokenFor(user)}`).send({ name: "" });
    expect(res.status).toBe(400);
    expect(res.body.error.code).toBe("VALIDATION_ERROR");
    expect(res.body.error.message).toContain("name");
  });
});
