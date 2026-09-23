import express from "express";
import bcrypt from "bcryptjs";
import jwt from "jsonwebtoken";
import { existsSync, readFileSync, writeFileSync } from "node:fs";
import { randomUUID } from "node:crypto";

const app = express();
const port = process.env.PORT || 5263;
// Never fall back to a hard-coded or empty signing secret: a predictable secret lets anyone
// mint a token for any account. Fail fast instead of starting in an unsafe state.
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
  console.error("Generate one with: node -e \"console.log(require('crypto').randomBytes(48).toString('base64url'))\"");
  process.exit(1);
}
const databaseFile = new URL("./nexus-data.json", import.meta.url);
const database = existsSync(databaseFile) ? JSON.parse(readFileSync(databaseFile, "utf8")) : { users: [], habits: {} };
const users = new Map(database.users.map((user) => [user.id, user]));
const habits = new Map(Object.entries(database.habits || {}));
const projects = new Map(Object.entries(database.projects || {}));
const tasks = new Map(Object.entries(database.tasks || {}));

function saveDatabase() {
  writeFileSync(databaseFile, JSON.stringify({ users: [...users.values()], habits: Object.fromEntries(habits), projects: Object.fromEntries(projects), tasks: Object.fromEntries(tasks) }, null, 2));
}

app.use(express.json());

app.get("/api/app/config", (_req, res) => {
  res.json({
    minimumAppVersion: "1.0.0",
    latestAppVersion: "1.0.0",
    maintenanceMode: false,
    maintenanceMessage: "",
    registrationEnabled: true,
    featureFlags: { habits: true, remoteLocalization: true, announcements: true },
    announcements: []
  });
});

app.get("/api/localization/:language", (req, res) => {
  const bundles = {
    en: { login: "Sign In", register: "Create Account", dashboard: "Dashboard", settings: "Settings", habits: "Habits" },
    zu: { login: "Ngena", register: "Dala i-akhawunti", dashboard: "Ideshibhodi", settings: "Izilungiselelo", habits: "Imikhuba" },
    tn: { login: "Tsena", register: "Tlhama akhaonto", dashboard: "Deshibhodi", settings: "Dipeakanyo", habits: "Mekgwa" }
  };
  const language = bundles[req.params.language] ? req.params.language : "en";
  res.json({ language, strings: bundles[language] });
});

function tokenFor(user) {
  return jwt.sign({ sub: user.id, email: user.email }, secret, { expiresIn: "7d" });
}

function auth(req, res, next) {
  const value = req.headers.authorization || "";
  try {
    const decoded = jwt.verify(value.replace("Bearer ", ""), secret);
    req.user = users.get(decoded.sub);
    if (!req.user) return res.status(401).json({ message: "Session expired" });
    next();
  } catch {
    res.status(401).json({ message: "Authentication required" });
  }
}

app.post("/api/auth/register", async (req, res) => {
  const { email, password, displayName } = req.body;
  if (!email || !password || !displayName) return res.status(400).json({ message: "Missing registration fields" });
  if ([...users.values()].some((user) => user.email === email)) return res.status(409).json({ message: "Email already registered" });
  const user = { id: randomUUID(), email, displayName, passwordHash: await bcrypt.hash(password, 12), language: "en", notificationsEnabled: true };
  users.set(user.id, user);
  saveDatabase();
  res.status(201).json({ token: tokenFor(user) });
});

app.post("/api/auth/login", async (req, res) => {
  const user = [...users.values()].find((candidate) => candidate.email === req.body.email);
  if (!user || !(await bcrypt.compare(req.body.password || "", user.passwordHash))) return res.status(401).json({ message: "Invalid credentials" });
  res.json({ token: tokenFor(user) });
});

app.get("/api/users/me", auth, (req, res) => {
  const { email, displayName, language, notificationsEnabled } = req.user;
  res.json({ email, displayName, language, notificationsEnabled });
});

app.patch("/api/users/me", auth, (req, res) => {
  Object.assign(req.user, {
    displayName: req.body.displayName ?? req.user.displayName,
    language: req.body.language ?? req.user.language,
    notificationsEnabled: req.body.notificationsEnabled ?? req.user.notificationsEnabled
  });
  saveDatabase();
  const { email, displayName, language, notificationsEnabled } = req.user;
  res.json({ email, displayName, language, notificationsEnabled });
});

app.post("/api/users/me/password", auth, async (req, res) => {
  if (!req.body.newPassword || req.body.newPassword.length < 6) return res.status(400).json({ message: "Password must be at least 6 characters" });
  if (!(await bcrypt.compare(req.body.currentPassword || "", req.user.passwordHash))) return res.status(401).json({ message: "Current password is incorrect" });
  req.user.passwordHash = await bcrypt.hash(req.body.newPassword, 12);
  saveDatabase();
  res.status(204).end();
});

app.delete("/api/users/me", auth, (req, res) => {
  habits.delete(req.user.id);
  projects.delete(req.user.id);
  tasks.delete(req.user.id);
  users.delete(req.user.id);
  saveDatabase();
  res.status(204).end();
});

app.get("/api/dashboard", auth, (req, res) => {
  const userProjects = projects.get(req.user.id) || [];
  const userTasks = tasks.get(req.user.id) || [];
  res.json({ projects: userProjects.length, tasks: userTasks.length, activity: userTasks.filter((task) => task.isCompleted).length });
});

app.get("/api/projects", auth, (req, res) => res.json(projects.get(req.user.id) || []));
app.post("/api/projects", auth, (req, res) => {
  const userProjects = projects.get(req.user.id) || [];
  const project = { id: randomUUID(), name: req.body.name, description: req.body.description || null, createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() };
  userProjects.push(project);
  projects.set(req.user.id, userProjects);
  saveDatabase();
  res.status(201).json(project);
});
app.get("/api/projects/:projectId", auth, (req, res) => {
  const project = (projects.get(req.user.id) || []).find((item) => item.id === req.params.projectId);
  project ? res.json(project) : res.status(404).json({ message: "Project not found" });
});
app.put("/api/projects/:projectId", auth, (req, res) => {
  const userProjects = projects.get(req.user.id) || [];
  const index = userProjects.findIndex((item) => item.id === req.params.projectId);
  if (index < 0) return res.status(404).json({ message: "Project not found" });
  userProjects[index] = { ...userProjects[index], name: req.body.name, description: req.body.description || null, updatedAt: new Date().toISOString() };
  saveDatabase();
  res.json(userProjects[index]);
});
app.delete("/api/projects/:projectId", auth, (req, res) => {
  projects.set(req.user.id, (projects.get(req.user.id) || []).filter((item) => item.id !== req.params.projectId));
  tasks.set(req.user.id, (tasks.get(req.user.id) || []).filter((item) => item.projectId !== req.params.projectId));
  saveDatabase();
  res.status(204).end();
});

app.get("/api/projects/:projectId/tasks", auth, (req, res) => {
  res.json((tasks.get(req.user.id) || []).filter((task) => task.projectId === req.params.projectId));
});
app.post("/api/projects/:projectId/tasks", auth, (req, res) => {
  const userTasks = tasks.get(req.user.id) || [];
  // Spread the body first so a client can never override the server-owned id or projectId.
  const task = { ...req.body, id: randomUUID(), projectId: req.params.projectId, isCompleted: req.body.isCompleted || false, createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() };
  userTasks.push(task);
  tasks.set(req.user.id, userTasks);
  saveDatabase();
  res.status(201).json(task);
});
app.put("/api/tasks/:taskId", auth, (req, res) => {
  const userTasks = tasks.get(req.user.id) || [];
  const index = userTasks.findIndex((task) => task.id === req.params.taskId);
  if (index < 0) return res.status(404).json({ message: "Task not found" });
  // Server-owned fields (id, projectId, createdAt) are never taken from the body.
  const { id: _id, projectId: _projectId, createdAt: _createdAt, ...updates } = req.body;
  userTasks[index] = { ...userTasks[index], ...updates, updatedAt: new Date().toISOString() };
  saveDatabase();
  res.json(userTasks[index]);
});
app.delete("/api/tasks/:taskId", auth, (req, res) => {
  tasks.set(req.user.id, (tasks.get(req.user.id) || []).filter((task) => task.id !== req.params.taskId));
  saveDatabase();
  res.status(204).end();
});

app.get("/api/habits", auth, (req, res) => res.json(habits.get(req.user.id) || []));
app.post("/api/habits", auth, (req, res) => {
  const userHabits = habits.get(req.user.id) || [];
  const habit = { id: randomUUID(), name: req.body.name, description: req.body.description || null, frequency: req.body.frequency || "DAILY", targetDays: req.body.targetDays || [], completedDates: [], createdAt: new Date().toISOString() };
  userHabits.push(habit);
  habits.set(req.user.id, userHabits);
  saveDatabase();
  res.status(201).json(habit);
});
app.patch("/api/habits/:habitId", auth, (req, res) => {
  const userHabits = habits.get(req.user.id) || [];
  const index = userHabits.findIndex((habit) => habit.id === req.params.habitId);
  if (index < 0) return res.status(404).json({ message: "Habit not found" });
  userHabits[index] = { ...userHabits[index], ...req.body };
  saveDatabase();
  res.json(userHabits[index]);
});
app.delete("/api/habits/:habitId", auth, (req, res) => {
  habits.set(req.user.id, (habits.get(req.user.id) || []).filter((habit) => habit.id !== req.params.habitId));
  saveDatabase();
  res.status(204).end();
});

app.listen(port, () => console.log(`Nexus API listening on http://localhost:${port}/api`));
