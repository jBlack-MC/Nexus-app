#!/usr/bin/env node
// Checks the Part 3 submission file: anchors, images, unresolved tokens, secrets, PDF integrity.
import { readFileSync, existsSync } from "node:fs";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const repo = resolve(here, "..", "..");
const html = readFileSync(join(here, "Nexus-Part-3-Submission.html"), "utf8");
const errors = [];
const ids = new Set([...html.matchAll(/\sid="([^"]+)"/g)].map((m) => m[1]));
const dupes = [...html.matchAll(/\sid="([^"]+)"/g)].map((m) => m[1]).filter((id, i, a) => a.indexOf(id) !== i);
if (dupes.length) errors.push(`duplicate ids: ${[...new Set(dupes)].join(", ")}`);
for (const [, href] of html.matchAll(/\shref="([^"]+)"/g)) {
  if (href.startsWith("#") && !ids.has(href.slice(1))) errors.push(`broken anchor ${href}`);
  if (!href.startsWith("#") && !/^https?:/.test(href) && !existsSync(resolve(here, href.split("#")[0]))) errors.push(`missing link target ${href}`);
}
for (const [, src] of html.matchAll(/\ssrc="([^"]+)"/g)) if (!existsSync(resolve(here, src))) errors.push(`missing image ${src}`);
for (const [, img] of html.matchAll(/<img\b([^>]*)>/g)) if (!/\salt="[^"]+"/.test(img)) errors.push("img without alt");
if (/\[\[[A-Z0-9-]+|\{\{[A-Z]\}\}|SIGNED_RESULT|broken-ref/.test(html)) errors.push("unresolved token");
const secrets = [];
const seed = readFileSync(join(repo, "docs", "seed_test_users.ps1"), "utf8");
for (const [, pw] of seed.matchAll(/password\s*=\s*"([^"]+)"/gi)) secrets.push(pw);
if (existsSync(join(repo, "keystore.properties"))) {
  for (const [, v] of readFileSync(join(repo, "keystore.properties"), "utf8").matchAll(/^(?:storePassword|keyPassword)=(.+)$/gm)) secrets.push(v.trim());
}
for (const s of secrets) if (html.includes(s)) errors.push("contains a secret or test password");
if (/eyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\./.test(html)) errors.push("contains a JWT-like token");
const pdf = readFileSync(join(here, "Nexus-Part-3-Submission.pdf"));
const pages = Math.max(0, ...[...pdf.toString("latin1").matchAll(/\/Type\s*\/Pages\b[^>]*?\/Count\s+(\d+)/g)].map((m) => +m[1]));
if (pdf.subarray(0, 5).toString() !== "%PDF-" || pages < 1) errors.push("PDF invalid");
console.log(`anchors ${ids.size}, images ${[...html.matchAll(/<img /g)].length}, PDF ${pages} pages, ${(pdf.length / 1048576).toFixed(1)} MB`);
if (errors.length) { errors.forEach((e) => console.error("FAIL", e)); process.exit(1); }
console.log("Submission checks passed.");
