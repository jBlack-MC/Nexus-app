import express from "express";
import bcrypt from "bcryptjs";
import jwt from "jsonwebtoken";
import { z } from "zod";
import rateLimit from "express-rate-limit";
import helmet from "helmet";
import cors from "cors";
import { existsSync } from "node:fs";
import { readFile, writeFile, rename } from "node:fs/promises";
import { randomUUID } from "node:crypto";
import path from "node:path";
import { fileURLToPath } from "node:url";
import pino from "pino";
import pinoHttp from "pino-http";

const app = express();
const port = process.env.PORT || 5263;

// Configure structured logger with sensitive field redaction
const logger = pino({
  level: process.env.LOG_LEVEL || "info",
  redact: {
    paths: [
      "req.body.password",
      "req.body.newPassword",
      "req.body.currentPassword",
      "req.headers.authorization",
      "password",
      "passwordHash",
      "token"
    ],
    censor: "[REDACTED]"
  }
});

// 1. JWT_SECRET startup check (refuses to start below 32 chars or known placeholders)
const KNOWN_PLACEHOLDER_SECRETS = new Set([
  "change-this-development-secret",
  "changeme",
  "secret",
  "test",
  "password",
]);
const secret = (process.env.JWT_SECRET || "").trim();
if (secret.length < 32 || KNOWN_PLACEHOLDER_SECRETS.has(secret.toLowerCase())) {
  console.error("FATAL: JWT_SECRET is missing, too short, or set to a known placeholder value.");
  console.error("The API will not start without a strong signing secret of at least 32 characters.");
  console.error("");
  console.error("Set one before starting the server, for example:");
  console.error('  PowerShell:  $env:JWT_SECRET = "<a-long-random-string>"; npm start');
  console.error('  bash:        export JWT_SECRET="<a-long-random-string>" && npm start');
  console.error("");
  console.error('Generate one with: node -e "console.log(require(\'crypto\').randomBytes(48).toString(\'base64url\'))"');
  process.exit(1);
}

class AppError extends Error {
  constructor(message, statusCode = 500, code = undefined) {
    super(message);
    this.statusCode = statusCode;
    this.code = code;
  }
}

// Rate limiter for authentication endpoints (10 requests per 15 minutes per IP)
const authLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 10,
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: { message: "Too many authentication attempts, please try again later." } }
});

// Zod validation schemas matching Android client API models exactly
const registerSchema = z.object({
  email: z.string().email(),
  password: z.string().min(6),
  displayName: z.string().min(1)
});

const loginSchema = z.object({
  email: z.string().email(),
  password: z.string().min(1)
});

const updateProfileSchema = z.object({
  displayName: z.string().min(1).optional(),
  language: z.string().optional(),
  notificationsEnabled: z.boolean().optional()
}).strict();

const changePasswordSchema = z.object({
  currentPassword: z.string().min(1),
  newPassword: z.string().min(6)
});

const projectSchema = z.object({
  name: z.string().min(1),
  description: z.string().nullable().optional()
});

const taskSchema = z.object({
  title: z.string().min(1),
  description: z.string().nullable().optional(),
  isCompleted: z.boolean().optional(),
  dueDate: z.string().regex(/^\d{4}-\d{2}-\d{2}$/, "Invalid ISO date (yyyy-MM-dd)").nullable().optional(),
  priority: z.enum(["NONE", "LOW", "MEDIUM", "HIGH"]).optional(),
  status: z.enum(["TODO", "IN_PROGRESS", "DONE"]).optional(),
  labels: z.array(z.string()).optional(),
  checklist: z.array(z.object({
    id: z.string().optional(),
    title: z.string().min(1),
    isCompleted: z.boolean().optional()
  })).optional()
});

const habitSchema = z.object({
  name: z.string().min(1),
  description: z.string().nullable().optional(),
  frequency: z.enum(["DAILY", "WEEKLY", "CUSTOM"]).optional(),
  targetDays: z.array(z.number()).optional(),
  completedDates: z.array(z.string()).optional()
});

/**
 * Validation middleware: runs before any store access or file I/O.
 * Returns 400 with `{ error: { message: "..." } }` on validation failure via central error handler.
 */
function validate(schema) {
  return (req, res, next) => {
    const result = schema.safeParse(req.body);
    if (!result.success) {
      const message = result.error.errors.map(e => `${e.path.join('.')}: ${e.message}`).join(', ');
      return next(new AppError(message, 400, "VALIDATION_ERROR"));
    }
    req.body = result.data;
    next();
  };
}

// In-process async mutex queue to serialize all read-modify-write and read operations against the JSON data store.
class AsyncMutex {
  constructor() {
    this.queue = Promise.resolve();
  }

  async acquire() {
    let release;
    const nextPromise = new Promise((resolve) => {
      release = resolve;
    });
    const currentPromise = this.queue;
    this.queue = nextPromise;
    await currentPromise;
    return release;
  }
}

const dbMutex = new AsyncMutex();
const __dirname = path.dirname(fileURLToPath(import.meta.url));
const databaseFile = process.env.DATABASE_PATH
  ? path.resolve(process.env.DATABASE_PATH)
  : path.join(__dirname, "nexus-data.json");

let users = new Map();
let habits = new Map();
let projects = new Map();
let tasks = new Map();

async function loadDatabase() {
  if (existsSync(databaseFile)) {
    try {
      const content = await readFile(databaseFile, "utf8");
      const database = JSON.parse(content);
      users = new Map((database.users || []).map((user) => [user.id, user]));
      habits = new Map(Object.entries(database.habits || {}));
      projects = new Map(Object.entries(database.projects || {}));
      tasks = new Map(Object.entries(database.tasks || {}));
    } catch (e) {
      logger.error({ err: e }, "Failed to load database, starting with empty state");
    }
  }
}

/**
 * Saves the database atomically and safely:
 * 1. Serializes in-memory Maps to JSON.
 * 2. Writes the JSON to a temporary file (nexus-data.json.tmp) using async file I/O.
 * 3. Atomically renames the temporary file over the target nexus-data.json file.
 * This prevents file truncation or corruption if a crash occurs mid-write.
 */
async function saveDatabase() {
  const data = {
    users: [...users.values()],
    habits: Object.fromEntries(habits),
    projects: Object.fromEntries(projects),
    tasks: Object.fromEntries(tasks)
  };
  const jsonContent = JSON.stringify(data, null, 2);
  const tempFile = databaseFile + ".tmp";
  await writeFile(tempFile, jsonContent, "utf8");
  await rename(tempFile, databaseFile);
}

// 2. Baseline security headers using Helmet (X-Content-Type-Options, X-Frame-Options, etc.)
app.use(helmet());

// 1. Explicit CORS configuration:
// Because Nexus is a mobile-only client (Android app interacting with the API via Retrofit),
// browser CORS policies do not apply. CORS is explicitly kept open (origin: '*')
// to allow local testing, web-based tools, or future multi-platform clients.
app.use(cors({ origin: "*" }));

app.use(express.json({ limit: "1mb" }));

// Unauthenticated health check endpoints for CI, uptime checks, and liveness verification
app.get("/health", (_req, res) => {
  res.json({ status: "ok", uptime: process.uptime() });
});

app.get("/api/health", (_req, res) => {
  res.json({ status: "ok", uptime: process.uptime() });
});

// Request logging middleware with request correlation ID generation
app.use(pinoHttp({
  logger,
  genReqId: (req, res) => req.headers["x-request-id"] || req.headers["x-correlation-id"] || randomUUID(),
  customLogLevel: (req, res, err) => {
    if (res.statusCode >= 500 || err) return "error";
    if (res.statusCode >= 400) return "warn";
    return "info";
  }
}));

function withDb(handler) {
  return async (req, res, next) => {
    const release = await dbMutex.acquire();
    try {
      await handler(req, res, next);
    } catch (err) {
      next(err);
    } finally {
      release();
    }
  };
}

function withAuthAndDb(handler) {
  return async (req, res, next) => {
    const release = await dbMutex.acquire();
    try {
      const value = req.headers.authorization || "";
      let decoded;
      try {
        decoded = jwt.verify(value.replace("Bearer ", ""), secret);
      } catch {
        return next(new AppError("Authentication required", 401, "UNAUTHORIZED"));
      }
      req.user = users.get(decoded.sub);
      if (!req.user) {
        return next(new AppError("Session expired", 401, "SESSION_EXPIRED"));
      }
      await handler(req, res, next);
    } catch (err) {
      next(err);
    } finally {
      release();
    }
  };
}

app.get("/api/app/config", withDb((_req, res) => {
  res.json({
    minimumAppVersion: "1.0.0",
    latestAppVersion: "1.0.0",
    maintenanceMode: false,
    maintenanceMessage: "",
    registrationEnabled: true,
    featureFlags: { habits: true, remoteLocalization: true, announcements: true },
    announcements: []
  });
}));

app.get("/api/localization/:language", withDb((req, res) => {
  const bundles = {
    en: { login: "Sign In", register: "Create Account", dashboard: "Dashboard", settings: "Settings", habits: "Habits" },
    zu: { login: "Ngena", register: "Dala i-akhawunti", dashboard: "Ideshibhodi", settings: "Izilungiselelo", habits: "Imikhuba" },
    tn: { login: "Tsena", register: "Tlhama akhaonto", dashboard: "Deshibhodi", settings: "Dipeakanyo", habits: "Mekgwa" }
  };
  const language = bundles[req.params.language] ? req.params.language : "en";
  res.json({ language, strings: bundles[language] });
}));

function tokenFor(user) {
  return jwt.sign({ sub: user.id, email: user.email }, secret, { expiresIn: "24h" });
}

app.post("/api/auth/register", authLimiter, validate(registerSchema), withDb(async (req, res, next) => {
  const { email, password, displayName } = req.body;
  if ([...users.values()].some((user) => user.email === email)) {
    return next(new AppError("Email already registered", 409, "EMAIL_ALREADY_REGISTERED"));
  }
  // Passwords hashed with bcrypt, work factor (cost) 12
  const user = { id: randomUUID(), email, displayName, passwordHash: await bcrypt.hash(password, 12), language: "en", notificationsEnabled: true };
  users.set(user.id, user);
  await saveDatabase();
  res.status(201).json({ token: tokenFor(user) });
}));

app.post("/api/auth/login", authLimiter, validate(loginSchema), withDb(async (req, res, next) => {
  const user = [...users.values()].find((candidate) => candidate.email === req.body.email);
  if (!user) return next(new AppError("Invalid credentials", 401, "INVALID_CREDENTIALS"));

  let passwordMatch = false;
  if (user.passwordHash && user.passwordHash.startsWith("$2")) {
    passwordMatch = await bcrypt.compare(req.body.password || "", user.passwordHash);
  } else {
    // Transparent migration path for legacy or non-bcrypt password hashes
    passwordMatch = (user.passwordHash === (req.body.password || ""));
    if (passwordMatch) {
      user.passwordHash = await bcrypt.hash(req.body.password || "", 12);
      await saveDatabase();
    }
  }

  if (!passwordMatch) return next(new AppError("Invalid credentials", 401, "INVALID_CREDENTIALS"));
  res.json({ token: tokenFor(user) });
}));

app.get("/api/users/me", withAuthAndDb((req, res) => {
  const { email, displayName, language, notificationsEnabled } = req.user;
  res.json({ email, displayName, language, notificationsEnabled });
}));

app.patch("/api/users/me", validate(updateProfileSchema), withAuthAndDb(async (req, res) => {
  Object.assign(req.user, {
    displayName: req.body.displayName ?? req.user.displayName,
    language: req.body.language ?? req.user.language,
    notificationsEnabled: req.body.notificationsEnabled ?? req.user.notificationsEnabled
  });
  await saveDatabase();
  const { email, displayName, language, notificationsEnabled } = req.user;
  res.json({ email, displayName, language, notificationsEnabled });
}));

app.post("/api/users/me/password", validate(changePasswordSchema), withAuthAndDb(async (req, res, next) => {
  if (!(await bcrypt.compare(req.body.currentPassword || "", req.user.passwordHash))) {
    return next(new AppError("Current password is incorrect", 401, "INCORRECT_CURRENT_PASSWORD"));
  }
  req.user.passwordHash = await bcrypt.hash(req.body.newPassword, 12);
  await saveDatabase();
  res.status(204).end();
}));

app.delete("/api/users/me", withAuthAndDb(async (req, res) => {
  habits.delete(req.user.id);
  projects.delete(req.user.id);
  tasks.delete(req.user.id);
  users.delete(req.user.id);
  await saveDatabase();
  res.status(204).end();
}));

app.get("/api/dashboard", withAuthAndDb((req, res) => {
  const userProjects = projects.get(req.user.id) || [];
  const userTasks = tasks.get(req.user.id) || [];
  res.json({ projects: userProjects.length, tasks: userTasks.length, activity: userTasks.filter((task) => task.isCompleted).length });
}));

app.get("/api/projects", withAuthAndDb((req, res) => res.json(projects.get(req.user.id) || [])));
app.post("/api/projects", validate(projectSchema), withAuthAndDb(async (req, res) => {
  const userProjects = projects.get(req.user.id) || [];
  const project = { id: randomUUID(), name: req.body.name, description: req.body.description || null, createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() };
  userProjects.push(project);
  projects.set(req.user.id, userProjects);
  await saveDatabase();
  res.status(201).json(project);
}));
app.get("/api/projects/:projectId", withAuthAndDb((req, res, next) => {
  const project = (projects.get(req.user.id) || []).find((item) => item.id === req.params.projectId);
  if (!project) return next(new AppError("Project not found", 404, "PROJECT_NOT_FOUND"));
  res.json(project);
}));
app.put("/api/projects/:projectId", validate(projectSchema), withAuthAndDb(async (req, res, next) => {
  const userProjects = projects.get(req.user.id) || [];
  const index = userProjects.findIndex((item) => item.id === req.params.projectId);
  if (index < 0) return next(new AppError("Project not found", 404, "PROJECT_NOT_FOUND"));
  userProjects[index] = { ...userProjects[index], name: req.body.name, description: req.body.description || null, updatedAt: new Date().toISOString() };
  await saveDatabase();
  res.json(userProjects[index]);
}));
app.delete("/api/projects/:projectId", withAuthAndDb(async (req, res) => {
  projects.set(req.user.id, (projects.get(req.user.id) || []).filter((item) => item.id !== req.params.projectId));
  tasks.set(req.user.id, (tasks.get(req.user.id) || []).filter((item) => item.projectId !== req.params.projectId));
  await saveDatabase();
  res.status(204).end();
}));

app.get("/api/projects/:projectId/tasks", withAuthAndDb((req, res) => {
  res.json((tasks.get(req.user.id) || []).filter((task) => task.projectId === req.params.projectId));
}));
app.post("/api/projects/:projectId/tasks", validate(taskSchema), withAuthAndDb(async (req, res) => {
  const userTasks = tasks.get(req.user.id) || [];
  const task = { ...req.body, id: randomUUID(), projectId: req.params.projectId, isCompleted: req.body.isCompleted || false, createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() };
  userTasks.push(task);
  tasks.set(req.user.id, userTasks);
  await saveDatabase();
  res.status(201).json(task);
}));
app.put("/api/tasks/:taskId", validate(taskSchema), withAuthAndDb(async (req, res, next) => {
  const userTasks = tasks.get(req.user.id) || [];
  const index = userTasks.findIndex((task) => task.id === req.params.taskId);
  if (index < 0) return next(new AppError("Task not found", 404, "TASK_NOT_FOUND"));
  const { id: _id, projectId: _projectId, createdAt: _createdAt, ...updates } = req.body;
  userTasks[index] = { ...userTasks[index], ...updates, updatedAt: new Date().toISOString() };
  await saveDatabase();
  res.json(userTasks[index]);
}));
app.delete("/api/tasks/:taskId", withAuthAndDb(async (req, res) => {
  tasks.set(req.user.id, (tasks.get(req.user.id) || []).filter((task) => task.id !== req.params.taskId));
  await saveDatabase();
  res.status(204).end();
}));

app.get("/api/habits", withAuthAndDb((req, res) => res.json(habits.get(req.user.id) || [])));
app.post("/api/habits", validate(habitSchema), withAuthAndDb(async (req, res) => {
  const userHabits = habits.get(req.user.id) === undefined ? [] : habits.get(req.user.id);
  const habit = { id: randomUUID(), name: req.body.name, description: req.body.description || null, frequency: req.body.frequency || "DAILY", targetDays: req.body.targetDays || [], completedDates: [], createdAt: new Date().toISOString() };
  userHabits.push(habit);
  habits.set(req.user.id, userHabits);
  await saveDatabase();
  res.status(201).json(habit);
}));
app.patch("/api/habits/:habitId", validate(habitSchema), withAuthAndDb(async (req, res, next) => {
  const userHabits = habits.get(req.user.id) || [];
  const index = userHabits.findIndex((habit) => habit.id === req.params.habitId);
  if (index < 0) return next(new AppError("Habit not found", 404, "HABIT_NOT_FOUND"));
  userHabits[index] = { ...userHabits[index], ...req.body };
  await saveDatabase();
  res.json(userHabits[index]);
}));
app.delete("/api/habits/:habitId", withAuthAndDb(async (req, res) => {
  habits.set(req.user.id, (habits.get(req.user.id) || []).filter((habit) => habit.id !== req.params.habitId));
  await saveDatabase();
  res.status(204).end();
}));

if (process.env.NODE_ENV === "test") {
  app.get("/api/test/error-500", (_req, _res, next) => {
    next(new Error("Simulated unhandled 500 error"));
  });
}

// Central Error-Handling Middleware (registered last, after all routes)
app.use((err, req, res, next) => {
  const statusCode = err.statusCode || err.status || 500;
  const isProduction = process.env.NODE_ENV === "production";
  const requestId = req.id || req.headers?.["x-request-id"] || "-";

  const logData = {
    err,
    requestId,
    statusCode,
    method: req.method,
    url: req.url
  };

  if (statusCode >= 500) {
    (req.log || logger).error(logData, `[Error] ${statusCode} - ${err.message}`);
  } else {
    (req.log || logger).warn(logData, `[ClientError] ${statusCode} - ${err.message}`);
  }

  let message = err.message || "Internal Server Error";
  if (isProduction && statusCode === 500) {
    message = "Internal Server Error";
  }

  const errorResponse = {
    error: {
      message,
      requestId
    }
  };

  if (err.code) {
    errorResponse.error.code = err.code;
  }

  res.status(statusCode).json(errorResponse);
});

if (process.env.NODE_ENV !== "test") {
  loadDatabase().then(() => {
    app.listen(port, () => logger.info(`Nexus API listening on http://localhost:${port}/api`));
  });
}

export { app, loadDatabase, saveDatabase, users, tasks, projects, habits, databaseFile, AppError, logger };
