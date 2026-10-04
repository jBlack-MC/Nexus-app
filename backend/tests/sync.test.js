import request from "supertest";
import jwt from "jsonwebtoken";
import { randomUUID } from "node:crypto";
import fs from "node:fs/promises";
import { app, users, projects, tasks, habits, databaseFile, loadDatabase } from "../server.js";

let token;
let userId;
const operation = (resource, entityId, action, payload = {}) => ({ operationId: randomUUID(), resource, entityId, action, payload });
const send = body => request(app).post("/api/sync").set("Authorization", `Bearer ${token}`).send(body);
beforeEach(() => {
  users.clear(); projects.clear(); tasks.clear(); habits.clear();
  userId = randomUUID();
  users.set(userId, { id: userId, email: "sync@example.com" });
  token = jwt.sign({ sub: userId }, process.env.JWT_SECRET);
});
afterAll(async () => { await fs.rm(databaseFile, { force: true }); });

test("offline project and task creation, edits and deletes replay in order", async () => {
  const projectId = randomUUID(), taskId = randomUUID();
  expect((await send(operation("projects", projectId, "create", { name: "Offline project" }))).status).toBe(204);
  expect((await send(operation("tasks", taskId, "create", { projectId, title: "Plan", status: "DONE", labels: ["work"] }))).status).toBe(204);
  expect(tasks.get(userId)[0].isCompleted).toBe(true);
  expect((await send(operation("tasks", taskId, "update", { projectId, title: "Updated", status: "TODO" }))).status).toBe(204);
  expect(tasks.get(userId)[0].isCompleted).toBe(false);
  expect((await send(operation("projects", projectId, "delete"))).status).toBe(204);
  expect(tasks.get(userId)).toEqual([]);
  expect(projects.get(userId)).toEqual([]);
});

test("a lost response and restart do not duplicate or resurrect an operation", async () => {
  const id = randomUUID();
  const create = operation("habits", id, "create", { name: "Walk", completedDates: ["2026-10-04"] });
  expect((await send(create)).status).toBe(204);
  await loadDatabase();
  expect((await send(create)).status).toBe(204);
  expect(habits.get(userId)).toHaveLength(1);
  expect(habits.get(userId)[0].completedDates).toEqual(["2026-10-04"]);
  expect((await send(operation("habits", id, "delete"))).status).toBe(204);
  expect((await send(create)).status).toBe(204);
  expect(habits.get(userId)).toEqual([]);
});

test("sync cannot attach a task to another account's project", async () => {
  const id = randomUUID();
  projects.set("other-account", [{ id, name: "Private" }]);
  const response = await send(operation("tasks", randomUUID(), "create", { projectId: id, title: "Intrusion" }));
  expect(response.status).toBe(409);
  expect(tasks.get(userId)).toBeUndefined();
});

test("invalid payloads and missing authentication are rejected", async () => {
  expect((await send(operation("tasks", randomUUID(), "create", { title: "Invalid", priority: "URGENT" }))).status).toBe(400);
  expect((await request(app).post("/api/sync").send(operation("projects", randomUUID(), "create", { name: "No auth" }))).status).toBe(401);
});

test("an edit never resurrects an item deleted on another device", async () => {
  const response = await send(operation("projects", randomUUID(), "update", { name: "Deleted elsewhere" }));
  expect(response.status).toBe(409);
  expect(response.body.error.code).toBe("SYNC_CONFLICT");
});
