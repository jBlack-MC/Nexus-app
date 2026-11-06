#!/usr/bin/env node
// Renders the Play Store icon and feature graphic with headless Chrome/Edge, and crops the
// verified app screenshots (docs/screenshots/part3) to Play's maximum 2:1 aspect ratio.
//   node docs/play-store/build-assets.mjs
// Cropping uses Python + Pillow if available (PYTHONPATH may point at a Pillow install).
import { spawnSync } from "node:child_process";
import { existsSync, mkdirSync, readdirSync, rmSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";
import { tmpdir } from "node:os";

const here = dirname(fileURLToPath(import.meta.url));
const out = join(here, "assets");
mkdirSync(out, { recursive: true });
const browser = [process.env.CHROME_PATH, "C:/Program Files/Google/Chrome/Application/chrome.exe",
  "C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe", "/usr/bin/google-chrome"].find((p) => p && existsSync(p));
if (!browser) throw new Error("Chrome/Edge not found; set CHROME_PATH");

function render(src, png, w, h) {
  const profile = join(tmpdir(), `nexus-play-${process.pid}`);
  const r = spawnSync(browser, ["--headless=new", "--disable-gpu", "--hide-scrollbars", `--user-data-dir=${profile}`,
    `--window-size=${w},${h}`, `--screenshot=${png}`, "--default-background-color=00000000", pathToFileURL(src).href], { encoding: "utf8" });
  rmSync(profile, { recursive: true, force: true });
  if (!existsSync(png)) throw new Error(`render failed: ${src}\n${r.stderr}`);
  console.log("rendered", png);
}
render(join(here, "src", "icon.html"), join(out, "icon-512.png"), 512, 512);
render(join(here, "src", "feature-graphic.html"), join(out, "feature-graphic-1024x500.png"), 1024, 500);

// Phone screenshots: 1080x2400 captures -> 1080x2160 (drop status/navigation bars), max ratio 2:1.
const shots = join(here, "..", "screenshots", "part3");
const pick = ["03-dashboard", "05-projects", "07-tasks", "08-task-detail", "09-habits", "15-dashboard-isizulu", "19-offline-pending", "13-notifications"];
const py = `
import sys
from PIL import Image
src, dst = sys.argv[1], sys.argv[2]
im = Image.open(src)
w, h = im.size
top = 60
im.crop((0, top, w, top + 2 * w)).convert("RGB").save(dst, optimize=True)  # Play: 24-bit, no alpha
`;
mkdirSync(join(out, "phone"), { recursive: true });
pick.forEach((name, i) => {
  const src = join(shots, `${name}.png`);
  if (!existsSync(src)) return console.warn("missing", src);
  const dst = join(out, "phone", `${String(i + 1).padStart(2, "0")}-${name}.png`);
  const r = spawnSync("python", ["-c", py, src, dst], { encoding: "utf8", env: process.env });
  if (r.status !== 0) throw new Error(r.stderr);
  console.log("cropped", dst);
});
console.log(readdirSync(join(out, "phone")).length, "phone screenshots");
// Play requires the hi-res icon as a 32-bit PNG (with alpha channel).
const toRgba = `
import sys
from PIL import Image
Image.open(sys.argv[1]).convert("RGBA").save(sys.argv[1])
`;
const icon = spawnSync("python", ["-c", toRgba, join(out, "icon-512.png")], { encoding: "utf8", env: process.env });
if (icon.status !== 0) throw new Error(icon.stderr);
console.log("icon converted to 32-bit RGBA");
