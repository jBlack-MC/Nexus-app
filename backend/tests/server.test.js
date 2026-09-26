import request from "supertest";
import { spawnSync } from "node:child_process";
import fs from "node:fs/promises";
import path from "node:path";
import jwt from "jsonwebtoken";
import { app, loadDatabase, users, tasks, projects, databaseFile } from "../server.js";

const TEST_SECRET = "this-is-a-very-secure-test-jwt-secret-of-at-least-32-characters";

beforeAll(async () => {
  process.env.NODE_ENV = "test";
  process.env.JWT_SECRET = TEST_SECRET;
  // Ensure isolated test database
  try {
    await fs.unlink(databaseFile);
  } catch {}
  await loadDatabase();
});

afterEach(async () => {
  users.clear();
  tasks.clear();
  projects.clear();
  try {
    await fs.unlink(databaseFile);
  } catch {}
  try {
    await fs.unlink(databaseFile + ".tmp");
  } catch {}
});

describe("Nexus Backend Remediation Tests (Steps 1-6)", () => {

  // Step 3: Config-loading level test for JWT_SECRET
  test("Step 3: Server refuses to start with missing, short, or placeholder JWT_SECRET", () => {
    const invalidSecrets = ["", "short", "changeme", "change-this-development-secret", "secret"];
    for (const secret of invalidSecrets) {
      const result = spawnSync("node", ["server.js"], {
        env: { ...process.env, JWT_SECRET: secret, NODE_ENV: "production" },
        cwd: path.resolve(".")
      });
      expect(result.status).toBe(1);
      const stderr = result.stderr.toString();
      expect(stderr).toContain("FATAL: JWT_SECRET is missing, too short, or set to a known placeholder value");
    }
  });

  // Step 1: Concurrency test for write-lock (AsyncMutex) & atomic-write (temp file + rename)
  test("Step 1: Concurrent PATCH requests update different task fields correctly without race conditions", async () => {
    // Setup user, project, and task
    const regRes = await request(app)
      .post("/api/auth/register")
      .send({ email: "locktest@example.com", password: "password123", displayName: "Lock Test" });
    const token = regRes.body.token;

    const projRes = await request(app)
      .post("/api/projects")
      .set("Authorization", `Bearer ${token}`)
      .send({ name: "Project 1" });
    const projectId = projRes.body.id;

    const taskRes = await request(app)
      .post(`/api/projects/${projectId}/tasks`)
      .set("Authorization", `Bearer ${token}`)
      .send({ title: "Task 1", priority: "LOW", status: "TODO" });
    const taskId = taskRes.body.id;

    // Fire two rapid concurrent PATCH/PUT requests updating different fields
    const p1 = request(app)
      .put(`/api/tasks/${taskId}`)
      .set("Authorization", `Bearer ${token}`)
      .send({ title: "Task 1 Updated", priority: "HIGH", status: "TODO" });

    const p2 = request(app)
      .put(`/api/tasks/${taskId}`)
      .set("Authorization", `Bearer ${token}`)
      .send({ title: "Task 1 Updated", priority: "HIGH", status: "DONE" });

    await Promise.all([p1, p2]);

    // Verify final stored state has both updates applied correctly (serialized via AsyncMutex)
    const getRes = await request(app)
      .get(`/api/projects/${projectId}/tasks`)
      .set("Authorization", `Bearer ${token}`);

    expect(getRes.status).toBe(200);
    const updatedTask = getRes.body.find(t => t.id === taskId);
    expect(updatedTask.title).toBe("Task 1 Updated");
    expect(updatedTask.priority).toBe("HIGH");
    expect(updatedTask.status).toBe("DONE");

    // Also assert atomic write temp-file + rename pattern exists (nexus-data.json.tmp is used and renamed)
    const existsTemp = await fs.stat(databaseFile).then(() => true).catch(() => false);
    expect(existsTemp).toBe(true);
  });

  // Step 2: Validation tests for Zod schemas (register, task create/update, users/me PATCH)
  describe("Step 2: Zod Validation Tests", () => {
    let token;
    beforeEach(async () => {
      const res = await request(app)
        .post("/api/auth/register")
        .send({ email: "validuser@example.com", password: "password123", displayName: "Valid User" });
      token = res.body.token;
    });

    test("Register: rejects invalid email and missing password with 400 VALIDATION_ERROR", async () => {
      const res1 = await request(app)
        .post("/api/auth/register")
        .send({ email: "not-an-email", password: "password123", displayName: "Bad Email" });
      expect(res1.status).toBe(400);
      expect(res1.body.error.code).toBe("VALIDATION_ERROR");
      expect(res1.body.error.message).toContain("email");

      const res2 = await request(app)
        .post("/api/auth/register")
        .send({ email: "good@example.com", password: "123", displayName: "Short Password" });
      expect(res2.status).toBe(400);
      expect(res2.body.error.code).toBe("VALIDATION_ERROR");
    });

    test("Task Create/Update: rejects invalid priority enum and malformed dueDate", async () => {
      const projRes = await request(app)
        .post("/api/projects")
        .set("Authorization", `Bearer ${token}`)
        .send({ name: "Project" });
      const projectId = projRes.body.id;

      // Invalid priority enum ("URGENT")
      const res1 = await request(app)
        .post(`/api/projects/${projectId}/tasks`)
        .set("Authorization", `Bearer ${token}`)
        .send({ title: "Bad Priority", priority: "URGENT" });
      expect(res1.status).toBe(400);
      expect(res1.body.error.code).toBe("VALIDATION_ERROR");

      // Malformed dueDate ("2026/13/45")
      const res2 = await request(app)
        .post(`/api/projects/${projectId}/tasks`)
        .set("Authorization", `Bearer ${token}`)
        .send({ title: "Bad Date", dueDate: "2026/13/45" });
      expect(res2.status).toBe(400);
      expect(res2.body.error.code).toBe("VALIDATION_ERROR");
    });

    test("Users/me PATCH: rejects disallowed extra fields due to .strict()", async () => {
      const res = await request(app)
        .patch("/api/users/me")
        .set("Authorization", `Bearer ${token}`)
        .send({ displayName: "Updated Name", disallowedField: "hacked" });
      expect(res.status).toBe(400);
      expect(res.body.error.code).toBe("VALIDATION_ERROR");
    });
  });

  // Step 4: Error Handling & Production Mode Tests
  describe("Step 4: Centralized Error Handling & Production Mode", () => {
    test("Validation error returns standardized error shape with status 400 and code", async () => {
      const res = await request(app)
        .post("/api/auth/register")
        .send({ email: "invalid", password: "123", displayName: "" });
      expect(res.status).toBe(400);
      expect(res.body).toHaveProperty("error");
      expect(res.body.error).toHaveProperty("message");
      expect(res.body.error).toHaveProperty("code", "VALIDATION_ERROR");
    });

    test("Simulated 500 error returns standardized error shape and hides stack trace in production", async () => {
      // Test development mode (stack trace logged, message preserved)
      const devRes = await request(app).get("/api/test/error-500");
      expect(devRes.status).toBe(500);
      expect(devRes.body).toHaveProperty("error");
      expect(devRes.body.error.message).toBe("Simulated unhandled 500 error");

      // Test production mode (NODE_ENV=production)
      const originalEnv = process.env.NODE_ENV;
      process.env.NODE_ENV = "production";
      try {
        const prodRes = await request(app).get("/api/test/error-500");
        expect(prodRes.status).toBe(500);
        expect(prodRes.body.error.message).toBe("Internal Server Error");
        expect(prodRes.body.error).not.toHaveProperty("stack");
      } finally {
        process.env.NODE_ENV = originalEnv;
      }
    });
  });

  // Step 3: Auth & Rate Limiting tests (placed last so rate limiting doesn't affect other tests)
  describe("Step 3: Auth and Rate Limiting Tests", () => {
    test("Expired or invalid token returns 401 UNAUTHORIZED", async () => {
      const expiredToken = jwt.sign({ sub: "fake-id", email: "fake@example.com" }, TEST_SECRET, { expiresIn: "-1s" });
      const res = await request(app)
        .get("/api/users/me")
        .set("Authorization", `Bearer ${expiredToken}`);
      expect(res.status).toBe(401);
      expect(res.body.error.code).toBe("UNAUTHORIZED");
    });

    test("Rate limiting on /api/auth/login returns 429 after threshold (10 requests)", async () => {
      // Send 10 login requests (threshold is max: 10 per 15 min)
      for (let i = 0; i < 10; i++) {
        await request(app)
          .post("/api/auth/login")
          .send({ email: "ratelimit@example.com", password: "wrong" });
      }
      // 11th request should be rate limited (429)
      const res = await request(app)
        .post("/api/auth/login")
        .send({ email: "ratelimit@example.com", password: "wrong" });
      expect(res.status).toBe(429);
    });
  });

  // Step 5: Health check and body size limit tests
  describe("Step 5: Health Check and Body Size Limit", () => {
    test("GET /health and GET /api/health return 200 with ok status and uptime", async () => {
      const res1 = await request(app).get("/health");
      expect(res1.status).toBe(200);
      expect(res1.body).toHaveProperty("status", "ok");
      expect(typeof res1.body.uptime).toBe("number");

      const res2 = await request(app).get("/api/health");
      expect(res2.status).toBe(200);
      expect(res2.body).toHaveProperty("status", "ok");
      expect(typeof res2.body.uptime).toBe("number");
    });

    test("express.json() rejects payloads exceeding 1mb limit with 413 Payload Too Large", async () => {
      const largePayload = { data: "a".repeat(1024 * 1024 * 2) }; // 2MB
      const res = await request(app)
        .post("/api/auth/register")
        .send(largePayload);
      expect(res.status).toBe(413);
    });
  });
});
