#!/usr/bin/env node
// Builds the single Part 3 submission file:
//   docs/submission/Nexus-Part-3-Submission.html and .pdf
// = docs/submission/src/part3.html + the combined documentation package (docs/deliverables).
// Run the deliverables build first:  node docs/deliverables/tools/build.mjs
// Then:                              node docs/submission/build.mjs
import { readFileSync, writeFileSync, existsSync, rmSync } from "node:fs";
import { spawnSync } from "node:child_process";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";
import { tmpdir } from "node:os";

const here = dirname(fileURLToPath(import.meta.url));
const combined = resolve(here, "..", "deliverables", "combined", "Nexus-Documentation-Package.html");
if (!existsSync(combined)) throw new Error("Build docs/deliverables first (node docs/deliverables/tools/build.mjs)");

const LABELS = { F: ["fact", "Implemented"], I: ["inf", "Inferred"], R: ["rec", "Recommendation"], Q: ["oq", "Open question"], U: ["unk", "Unknown"], E: ["est", "Estimate"] };
const pkg = readFileSync(combined, "utf8");
const pkgBody = pkg.slice(pkg.indexOf('<main class="doc">') + 18, pkg.lastIndexOf("</main>"))
  .replace(/\.\.\/\.\.\/screenshots\//g, "../screenshots/");
const ids = new Set([...pkgBody.matchAll(/\sid="([^"]+)"/g)].map((m) => m[1]));

let part3 = readFileSync(join(here, "src", "part3.html"), "utf8");
const signed = existsSync(join(here, "signing-verification.txt")) ? readFileSync(join(here, "signing-verification.txt"), "utf8").trim() : "see docs/submission/signing-verification.txt";
const signedHtml = signed.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/`([^`]+)`/g, "<code>$1</code>");
part3 = part3.replace("SIGNED_RESULT", signedHtml);
for (const [, id] of part3.matchAll(/\sid="([^"]+)"/g)) ids.add(id);
part3 = part3.replace(/\{\{([FIRQUE])\}\}/g, (_, k) => `<span class="tag ${LABELS[k][0]}">${LABELS[k][1]}</span>`);
const missing = [];
part3 = part3.replace(/\[\[([A-Z0-9-]+)\]\]/g, (_, id) => {
  if (!ids.has(id)) missing.push(id);
  return `<a href="#${id}">${id}</a>`;
});
if (missing.length) throw new Error(`Unresolved references: ${missing.join(", ")}`);

// Number the Part 3 sections (the package keeps its own numbering).
let n = 0;
part3 = part3.replace(/<h2 id="([^"]+)">/g, (_, id) => `<h2 id="${id}"><span class="num">${++n}</span>`);
const toc = [...part3.matchAll(/<h2 id="([^"]+)"><span class="num">(\d+)<\/span>([^<]+)/g)]
  .map(([, id, num, text]) => `<li><a href="#${id}">${num} ${text}</a></li>`).join("");
part3 = part3.replace("</header>", `</header>\n<nav class="toc" aria-label="Contents"><h2>Contents</h2><ol>${toc}</ol></nav>`);

const html = `<!DOCTYPE html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Nexus – Part 3 Final Submission (OPSC6312)</title>
<link rel="stylesheet" href="../deliverables/tools/style.css">
<style>@media print { @page { @top-left { content: "Nexus · OPSC6312 Part 3 final submission · v2.0.0"; } } }</style>
</head><body><main class="doc">
${part3}
<section class="combined-break">${pkgBody}</section>
</main></body></html>
`;
const outHtml = join(here, "Nexus-Part-3-Submission.html");
writeFileSync(outHtml, html);
console.log("wrote", outHtml);

const browser = [process.env.CHROME_PATH, "C:/Program Files/Google/Chrome/Application/chrome.exe",
  "C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe", "/usr/bin/google-chrome"].find((p) => p && existsSync(p));
if (!browser) { console.error("No Chrome/Edge: HTML written, PDF skipped"); process.exit(2); }
const profile = join(tmpdir(), `nexus-sub-${process.pid}`);
const outPdf = join(here, "Nexus-Part-3-Submission.pdf");
spawnSync(browser, ["--headless=new", "--disable-gpu", "--no-pdf-header-footer", "--allow-file-access-from-files",
  `--user-data-dir=${profile}`, "--virtual-time-budget=8000", `--print-to-pdf=${outPdf}`, pathToFileURL(outHtml).href], { timeout: 180000 });
rmSync(profile, { recursive: true, force: true });
if (!existsSync(outPdf)) throw new Error("PDF not produced");
console.log("wrote", outPdf);
