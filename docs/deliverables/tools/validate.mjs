#!/usr/bin/env node
// Validates the generated documentation package. Exit code 0 = all checks passed.
//   node docs/deliverables/tools/validate.mjs
import { readFileSync, readdirSync, existsSync, statSync } from "node:fs";
import { join, dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const root = resolve(here, "..");
const repo = resolve(root, "..", "..");
const errors = [];
const notes = [];
const fail = (m) => errors.push(m);

const sources = readdirSync(join(root, "src")).filter((f) => /^\d\d-.*\.html$/.test(f)).sort();
const htmlFiles = sources.map((f) => join(root, "html", f));
const pdfFiles = sources.map((f) => join(root, "pdf", f.replace(/\.html$/, ".pdf")));
const combinedHtml = join(root, "combined", "Nexus-Documentation-Package.html");
const combinedPdf = join(root, "combined", "Nexus-Documentation-Package.pdf");
const indexHtml = join(root, "index.html");

// 1. Expected outputs exist and are non-empty.
for (const f of [...htmlFiles, ...pdfFiles, combinedHtml, combinedPdf, indexHtml]) {
  if (!existsSync(f)) fail(`missing output: ${f}`);
  else if (statSync(f).size === 0) fail(`empty output: ${f}`);
}

// 2. PDFs are well-formed enough to open: header, EOF marker, page count.
function pdfPages(buf) {
  const s = buf.toString("latin1");
  const counts = [...s.matchAll(/\/Type\s*\/Pages\b[^>]*?\/Count\s+(\d+)/g)].map((m) => +m[1]);
  const alt = [...s.matchAll(/\/Count\s+(\d+)[^>]*?\/Type\s*\/Pages\b/g)].map((m) => +m[1]);
  const all = [...counts, ...alt];
  if (all.length) return Math.max(...all);
  return (s.match(/\/Type\s*\/Page\b(?!s)/g) || []).length;
}
for (const f of [...pdfFiles, combinedPdf].filter(existsSync)) {
  const buf = readFileSync(f);
  if (buf.subarray(0, 5).toString() !== "%PDF-") fail(`not a PDF header: ${f}`);
  if (!buf.subarray(-1024).toString("latin1").includes("%%EOF")) fail(`no %%EOF trailer: ${f}`);
  const pages = pdfPages(buf);
  if (pages < 1) fail(`could not find any pages: ${f}`);
  notes.push(`${f.slice(root.length + 1).replace(/\\/g, "/")}: ${pages} pages, ${(buf.length / 1024).toFixed(0)} KB`);
}

// 3. Links, anchors and images resolve; no unresolved tokens.
const idCache = new Map();
const idsOf = (file) => {
  if (!idCache.has(file)) idCache.set(file, new Set([...readFileSync(file, "utf8").matchAll(/\sid="([^"]+)"/g)].map((m) => m[1])));
  return idCache.get(file);
};
for (const f of [...htmlFiles, combinedHtml, indexHtml].filter(existsSync)) {
  const html = readFileSync(f, "utf8");
  const rel = f.slice(root.length + 1).replace(/\\/g, "/");
  if (/\[\[[A-Z0-9-]+/.test(html) || html.includes("broken-ref")) fail(`${rel}: unresolved [[ref]]`);
  if (/\{\{[A-Z]\}\}/.test(html)) fail(`${rel}: unresolved {{label}}`);
  const ids = [...html.matchAll(/\sid="([^"]+)"/g)].map((m) => m[1]);
  const dup = ids.filter((id, i) => ids.indexOf(id) !== i);
  if (dup.length) fail(`${rel}: duplicate ids ${[...new Set(dup)].join(", ")}`);
  for (const [, href] of html.matchAll(/\shref="([^"]+)"/g)) {
    if (/^(https?:|mailto:)/.test(href)) continue;
    const [path, frag] = href.split("#");
    const target = path ? resolve(dirname(f), path) : f;
    if (!existsSync(target)) { fail(`${rel}: link target missing ${href}`); continue; }
    if (frag && target.endsWith(".html") && !idsOf(target).has(frag)) fail(`${rel}: anchor missing ${href}`);
  }
  for (const [, src] of html.matchAll(/\ssrc="([^"]+)"/g)) {
    if (!existsSync(resolve(dirname(f), src))) fail(`${rel}: image missing ${src}`);
  }
  for (const [, img] of html.matchAll(/<img\b([^>]*)>/g)) {
    if (!/\salt="[^"]+"/.test(img)) fail(`${rel}: <img> without alt text`);
  }
  for (const [, svg] of html.matchAll(/<svg\b([^>]*)>/g)) {
    if (!/role="img"/.test(svg) || !/aria-labelledby=/.test(svg)) fail(`${rel}: <svg> without role/aria-labelledby`);
  }
  for (const k of ["Document ID", "Version", "Date", "Status"]) {
    if (f !== indexHtml && !html.includes(`>${k}</th>`)) fail(`${rel}: title page lacks ${k}`);
  }
}

// 4. No secrets or personal data copied into the package.
const forbidden = [];
const seed = existsSync(join(repo, "docs", "seed_test_users.ps1")) ? readFileSync(join(repo, "docs", "seed_test_users.ps1"), "utf8") : "";
for (const [, pw] of seed.matchAll(/password\s*=\s*"([^"]+)"/gi)) forbidden.push(pw);
for (const wf of readdirSync(join(repo, ".github", "workflows"))) {
  for (const [, s] of readFileSync(join(repo, ".github", "workflows", wf), "utf8").matchAll(/JWT_SECRET[=:]\s*"([^"]{16,})"/g)) forbidden.push(s);
}
// Release-signing passwords (git-ignored keystore.properties) must never appear in documents.
const keystoreProps = join(repo, "keystore.properties");
if (existsSync(keystoreProps)) {
  for (const [, v] of readFileSync(keystoreProps, "utf8").matchAll(/^(?:storePassword|keyPassword)=(.+)$/gm)) forbidden.push(v.trim());
}
const allOut = [...htmlFiles, combinedHtml, indexHtml, ...sources.map((s) => join(root, "src", s))].filter(existsSync);
for (const f of allOut) {
  const text = readFileSync(f, "utf8");
  const rel = f.slice(root.length + 1).replace(/\\/g, "/");
  for (const s of new Set(forbidden)) if (text.includes(s)) fail(`${rel}: contains a secret/test password from the repository`);
  if (/eyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\./.test(text)) fail(`${rel}: contains a JWT-like token`);
  for (const [email] of text.matchAll(/[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}/g)) {
    if (!/@(example\.com|nexus-app\.com)$/.test(email)) fail(`${rel}: contains email address ${email}`);
  }
  if (/ST\d{8}/.test(text)) fail(`${rel}: contains a student number`);
}
notes.push(`secret scan: ${new Set(forbidden).size} known secret/test-password values checked`);

// 5. Internal consistency: audit summary counts match the findings table.
const audit = sources.find((s) => s.startsWith("08-"));
if (audit) {
  const src = readFileSync(join(root, "src", audit), "utf8");
  const rows = [...src.matchAll(/<tr id="(AUD-\d+)">[\s\S]*?<span class="sev (\w+)">/g)].map((m) => [m[1], m[2]]);
  const bySev = {};
  for (const [, sev] of rows) bySev[sev] = (bySev[sev] || 0) + 1;
  const summary = [...src.matchAll(/<span class="sev (\w+)">\w+<\/span><\/td><td>(\d+)<\/td>/g)];
  for (const [, sev, n] of summary) {
    if ((bySev[sev] || 0) !== +n) fail(`audit summary says ${n} ${sev}, table has ${bySev[sev] || 0}`);
  }
  notes.push(`audit findings: ${rows.length} (${Object.entries(bySev).map(([k, v]) => `${k} ${v}`).join(", ")})`);
}

// 6. Every cited repository path that looks like a file exists.
const cited = new Set();
for (const s of sources) {
  for (const [, p] of readFileSync(join(root, "src", s), "utf8").matchAll(/<code>((?:docs|backend|app|\.github|gradle)\/[A-Za-z0-9_./-]+\.[a-z]{2,4})<\/code>/g)) cited.add(p);
}
// Paths the documents mention on purpose even though they are absent from the checkout.
const expectedAbsent = {
  "backend/nexus-data.json": "runtime data file, git-ignored",
  ".github/dependabot.yml": "documents state it does not exist",
  "app/google-services.json": "Firebase config supplied by the team, git-ignored",
};
for (const p of cited) if (!existsSync(join(repo, p)) && !expectedAbsent[p]) fail(`cited path does not exist: ${p}`);
notes.push(`cited repository paths checked: ${cited.size}`);

for (const n of notes) console.log(`ok   ${n}`);
if (errors.length) {
  for (const e of errors) console.error(`FAIL ${e}`);
  console.error(`\n${errors.length} problem(s) found.`);
  process.exit(1);
}
console.log(`\nAll checks passed: ${htmlFiles.length} HTML, ${pdfFiles.length} PDF, combined HTML+PDF, index.`);
