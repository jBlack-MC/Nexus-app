import express from "express";
import bcrypt from "bcryptjs";
import jwt from "jsonwebtoken";
import { existsSync, readFileSync, writeFileSync } from "node:fs";
import { randomUUID } from "node:crypto";

const app = express();
const port = process.env.PORT || 5263;
const secret = process.env.JWT_SECRET || "change-this-development-secret";
const databaseFile = new URL("./nexus-data.json", import.meta.url);
const database = existsSync(databaseFile) ? JSON.parse(readFileSync(databaseFile, "utf8")) : { users: [], habits: {} };
const users = new Map(database.users.map((user) => [user.id, user]));
const habits = new Map(Object.entries(database.habits));

function saveDatabase() {
  writeFileSync(databaseFile, JSON.stringify({ users: [...users.values()], habits: Object.fromEntries(habits) }, null, 2));
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
  users.delete(req.user.id);
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
