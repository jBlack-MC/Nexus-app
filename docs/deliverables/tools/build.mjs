#!/usr/bin/env node
// Builds the Nexus documentation package from docs/deliverables/src/*.html.
// No third-party dependencies: Node 18+ only. PDF output uses a locally installed
// Chrome or Edge in headless mode.
//
//   node docs/deliverables/tools/build.mjs          -> HTML only
//   node docs/deliverables/tools/build.mjs --pdf    -> HTML + PDF
//
// Source conventions (see docs/deliverables/README.md):
//   <!--meta key: value ... -->  document metadata block at the top of each source file
//   [[ID]] / [[ID|text]]         cross-reference to a stable ID defined anywhere in the package
//   {{F}} {{I}} {{R}} {{Q}} {{U}} {{E}}  evidence labels
import { readFileSync, writeFileSync, mkdirSync, readdirSync, existsSync, rmSync } from "node:fs";
import { spawnSync } from "node:child_process";
import { join, dirname, resolve } from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";
import { tmpdir } from "node:os";

const here = dirname(fileURLToPath(import.meta.url));
const root = resolve(here, "..");
const srcDir = join(root, "src");
const htmlDir = join(root, "html");
const pdfDir = join(root, "pdf");
const combinedDir = join(root, "combined");
const wantPdf = process.argv.includes("--pdf");

export const PACKAGE = {
  project: "Nexus",
  version: "2.0",
  date: "2026-11-05",
  status: "Draft for team review (not approved)",
  baseline: "Branch New-Changes: commit 716bedb plus the uncommitted Part 3 working-tree changes of 2026-11-05 (v1.0 analysed 3a3f275)",
  preparedBy: "AI-assisted (Claude Code) from repository evidence; team review pending",
};

const LABELS = {
  F: ["fact", "Implemented"],
  I: ["inf", "Inferred"],
  R: ["rec", "Recommendation"],
  Q: ["oq", "Open question"],
  U: ["unk", "Unknown"],
  E: ["est", "Estimate"],
};

const STABLE_ID = /^[A-Z][A-Z0-9]*(?:-[A-Z0-9]+)+$/;

function parseSource(file) {
  const raw = readFileSync(join(srcDir, file), "utf8");
  const m = raw.match(/^\s*<!--meta([\s\S]*?)-->/);
  if (!m) throw new Error(`${file}: missing <!--meta --> block`);
  const meta = {};
  for (const line of m[1].split(/\r?\n/)) {
    const kv = line.match(/^\s*([a-zA-Z]+)\s*:\s*(.+?)\s*$/);
    if (kv) meta[kv[1]] = kv[2];
  }
  for (const k of ["id", "title", "subtitle", "audience"]) {
    if (!meta[k]) throw new Error(`${file}: meta '${k}' missing`);
  }
  const num = file.slice(0, 2);
  const slug = file.replace(/\.html$/, "");
  return { file, num, slug, out: `${slug}.html`, meta, body: raw.slice(m[0].length) };
}

const escapeHtml = (s) => s.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
const stripTags = (s) => s.replace(/<[^>]+>/g, "").replace(/\s+/g, " ").trim();
const slugify = (s) => stripTags(s).toLowerCase().replace(/&[a-z]+;/g, "").replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "");

function numberHeadings(doc) {
  // Adds section numbers and anchor ids to <h2>/<h3>; returns TOC entries.
  const toc = [];
  let h2 = 0;
  let h3 = 0;
  const used = new Set();
  doc.body = doc.body.replace(/<(h2|h3)([^>]*)>([\s\S]*?)<\/\1>/g, (all, tag, attrs, inner) => {
    let num;
    if (tag === "h2") { h2 += 1; h3 = 0; num = `${h2}`; } else { h3 += 1; num = `${h2}.${h3}`; }
    let id = (attrs.match(/\sid="([^"]+)"/) || [])[1];
    if (!id) {
      id = `d${doc.num}-${slugify(inner) || num}`;
      while (used.has(id)) id += "-x";
      attrs += ` id="${id}"`;
    }
    used.add(id);
    toc.push({ level: tag === "h2" ? 2 : 3, num, id, text: stripTags(inner) });
    return `<${tag}${attrs}><span class="num">${num}</span>${inner}</${tag}>`;
  });
  return toc;
}

function tocHtml(toc, title = "Contents") {
  let html = `<nav class="toc" aria-label="Table of contents"><h2>${title}</h2><ol>`;
  let open3 = false;
  toc.forEach((t, i) => {
    if (t.level === 2) {
      if (open3) { html += "</ol>"; open3 = false; }
      if (i > 0) html += "</li>";
      html += `<li><a href="#${t.id}">${t.num} ${escapeHtml(t.text)}</a>`;
    } else {
      if (!open3) { html += "<ol>"; open3 = true; }
      html += `<li><a href="#${t.id}">${t.num} ${escapeHtml(t.text)}</a></li>`;
    }
  });
  if (open3) html += "</ol>";
  html += "</li></ol></nav>";
  return html;
}

function titlePage(doc) {
  const m = doc.meta;
  return `<header class="titlepage" id="${m.id}">
  <p class="project">${PACKAGE.project} &middot; Documentation package &middot; Deliverable ${doc.num}</p>
  <h1>${escapeHtml(m.title)}</h1>
  <p class="subtitle">${escapeHtml(m.subtitle)}</p>
  <table class="meta"><caption>Document control</caption><tbody>
    <tr><th scope="row">Document ID</th><td>${m.id}</td></tr>
    <tr><th scope="row">Version</th><td>${PACKAGE.version}</td></tr>
    <tr><th scope="row">Date</th><td>${PACKAGE.date}</td></tr>
    <tr><th scope="row">Status</th><td>${PACKAGE.status}</td></tr>
    <tr><th scope="row">Intended readers</th><td>${escapeHtml(m.audience)}</td></tr>
    <tr><th scope="row">Evidence baseline</th><td>${PACKAGE.baseline}</td></tr>
    <tr><th scope="row">Prepared by</th><td>${PACKAGE.preparedBy}</td></tr>
    <tr><th scope="row">Approved by</th><td><span class="tag unk">Unknown</span> No approval recorded</td></tr>
  </tbody></table>
  <p class="legend"><strong>Evidence labels used in this document:</strong>
    <span class="tag fact">Implemented</span> verified in code, configuration or test output;
    <span class="tag inf">Inferred</span> reasoned from evidence but not executed;
    <span class="tag rec">Recommendation</span> proposed, not implemented;
    <span class="tag oq">Open question</span> needs a decision;
    <span class="tag unk">Unknown</span> no evidence available;
    <span class="tag est">Estimate</span> planning figure, not a measurement.</p>
</header>`;
}

function pageStyle(doc) {
  const header = `${doc.meta.id} \\00B7  ${doc.meta.title} \\00B7  v${PACKAGE.version} \\00B7  ${PACKAGE.status.split(" (")[0]}`;
  return `<style>@media print { @page { @top-left { content: "${header}"; } } }</style>`;
}

function page(title, styleHref, extraHead, body) {
  return `<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>${escapeHtml(title)}</title>
<link rel="stylesheet" href="${styleHref}">
${extraHead}
</head>
<body>
<main class="doc">
${body}
</main>
</body>
</html>
`;
}

function applyTokens(html, resolveRef) {
  html = html.replace(/\{\{([FIRQUE])\}\}/g, (_, k) => `<span class="tag ${LABELS[k][0]}">${LABELS[k][1]}</span>`);
  html = html.replace(/\[\[([A-Z0-9-]+)(?:\|([^\]]+))?\]\]/g, (all, id, text) => {
    const href = resolveRef(id);
    if (!href) return `<span class="broken-ref">[[${id}]]</span>`;
    return `<a href="${href}">${text ?? id}</a>`;
  });
  return html;
}

function build() {
  const files = readdirSync(srcDir).filter((f) => /^\d\d-.*\.html$/.test(f)).sort();
  const docs = files.map(parseSource);

  // Registry of stable IDs -> owning document.
  const registry = new Map();
  for (const doc of docs) {
    registry.set(doc.meta.id, doc);
    for (const [, id] of doc.body.matchAll(/\sid="([^"]+)"/g)) {
      if (!STABLE_ID.test(id)) continue;
      if (registry.has(id) && registry.get(id) !== doc) throw new Error(`Duplicate stable id ${id} in ${doc.file} and ${registry.get(id).file}`);
      if (registry.has(id) && registry.get(id) === doc && id !== doc.meta.id) throw new Error(`Duplicate stable id ${id} in ${doc.file}`);
      registry.set(id, doc);
    }
  }

  for (const d of [htmlDir, combinedDir]) mkdirSync(d, { recursive: true });
  const tocs = new Map();
  for (const doc of docs) tocs.set(doc, numberHeadings(doc));

  // Individual documents.
  for (const doc of docs) {
    const resolveRef = (id) => {
      const owner = registry.get(id);
      if (!owner) return null;
      return owner === doc ? `#${id}` : `${owner.out}#${id}`;
    };
    const body = applyTokens(
      `${titlePage(doc)}\n${tocHtml(tocs.get(doc))}\n${doc.body}\n<footer class="docfoot">${doc.meta.id} &middot; v${PACKAGE.version} &middot; ${PACKAGE.date} &middot; Part of the <a href="../index.html">Nexus documentation package</a>. Generated by docs/deliverables/tools/build.mjs.</footer>`,
      resolveRef,
    );
    writeFileSync(join(htmlDir, doc.out), page(`${doc.meta.id} ${doc.meta.title} - Nexus`, "../tools/style.css", pageStyle(doc), body));
  }

  // Combined document.
  const combinedResolve = (id) => (registry.has(id) ? `#${id}` : null);
  const indexRows = docs.map((d) => `<tr><td><a href="#${d.meta.id}">${d.meta.id}</a></td><td>${escapeHtml(d.meta.title)}</td><td>${escapeHtml(d.meta.subtitle)}</td></tr>`).join("\n");
  let combinedBody = `<header class="titlepage" id="NX-00-PKG">
  <p class="project">${PACKAGE.project} &middot; Combined submission</p>
  <h1>Nexus Documentation Package</h1>
  <p class="subtitle">Twelve numbered deliverables for the Nexus Android app and Node.js API, compiled from repository evidence.</p>
  <table class="meta"><caption>Package control</caption><tbody>
    <tr><th scope="row">Version</th><td>${PACKAGE.version}</td></tr>
    <tr><th scope="row">Date</th><td>${PACKAGE.date}</td></tr>
    <tr><th scope="row">Status</th><td>${PACKAGE.status}</td></tr>
    <tr><th scope="row">Evidence baseline</th><td>${PACKAGE.baseline}</td></tr>
    <tr><th scope="row">Prepared by</th><td>${PACKAGE.preparedBy}</td></tr>
  </tbody></table>
</header>
<nav class="toc" aria-label="Package contents"><h2>Package contents</h2>
<table class="package-index"><thead><tr><th scope="col">ID</th><th scope="col">Deliverable</th><th scope="col">Scope</th></tr></thead><tbody>
${indexRows}
</tbody></table></nav>`;
  for (const doc of docs) {
    const toc = tocs.get(doc).filter((t) => t.level === 2);
    combinedBody += `\n<section class="combined-break">${titlePage(doc)}\n${tocHtml(toc, `${doc.meta.id} contents`)}\n${doc.body}</section>`;
  }
  combinedBody = applyTokens(combinedBody, combinedResolve);
  writeFileSync(join(combinedDir, "Nexus-Documentation-Package.html"), page("Nexus Documentation Package", "../tools/style.css", "", combinedBody));

  // Package index (landing page).
  const listRows = docs.map((d) => `<tr><td>${d.meta.id}</td><td><a href="html/${d.out}">${escapeHtml(d.meta.title)}</a></td><td><a href="pdf/${d.slug}.pdf">PDF</a></td><td>${escapeHtml(d.meta.subtitle)}</td></tr>`).join("\n");
  const indexBody = `<header class="titlepage" style="break-after:auto">
  <p class="project">${PACKAGE.project}</p><h1>Documentation package</h1>
  <p class="subtitle">Version ${PACKAGE.version} &middot; ${PACKAGE.date} &middot; ${PACKAGE.status}</p></header>
<table><caption>Deliverables</caption><thead><tr><th scope="col">ID</th><th scope="col">Document (HTML)</th><th scope="col">PDF</th><th scope="col">Scope</th></tr></thead><tbody>
${listRows}
</tbody></table>
<p>Combined submission: <a href="combined/Nexus-Documentation-Package.html">HTML</a> &middot; <a href="combined/Nexus-Documentation-Package.pdf">PDF</a>. Build and validation instructions: <code>docs/deliverables/README.md</code>.</p>`;
  writeFileSync(join(root, "index.html"), page("Nexus documentation package", "tools/style.css", "", indexBody));

  console.log(`Built ${docs.length} documents, combined package and index; ${registry.size} stable IDs registered.`);
  return docs;
}

function findBrowser() {
  const candidates = [
    process.env.CHROME_PATH,
    "C:/Program Files/Google/Chrome/Application/chrome.exe",
    "C:/Program Files (x86)/Google/Chrome/Application/chrome.exe",
    "C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe",
    "/usr/bin/google-chrome",
    "/usr/bin/chromium",
    "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
  ].filter(Boolean);
  return candidates.find((p) => existsSync(p));
}

function printPdf(browser, htmlFile, pdfFile) {
  const profile = join(tmpdir(), `nexus-docs-chrome-${process.pid}`);
  const args = [
    "--headless=new", "--disable-gpu", "--no-first-run", "--no-default-browser-check",
    "--allow-file-access-from-files", `--user-data-dir=${profile}`,
    "--no-pdf-header-footer", "--virtual-time-budget=5000",
    `--print-to-pdf=${pdfFile}`, pathToFileURL(htmlFile).href,
  ];
  const r = spawnSync(browser, args, { encoding: "utf8", timeout: 120000 });
  rmSync(profile, { recursive: true, force: true });
  if (r.status !== 0 || !existsSync(pdfFile)) {
    throw new Error(`PDF generation failed for ${htmlFile}: ${r.stderr || r.error}`);
  }
}

const docs = build();
if (wantPdf) {
  const browser = findBrowser();
  if (!browser) {
    console.error("No Chrome/Edge found; set CHROME_PATH. HTML sources were built; PDFs were not.");
    process.exit(2);
  }
  mkdirSync(pdfDir, { recursive: true });
  for (const doc of docs) {
    printPdf(browser, join(htmlDir, doc.out), join(pdfDir, `${doc.slug}.pdf`));
    console.log(`PDF  pdf/${doc.slug}.pdf`);
  }
  printPdf(browser, join(combinedDir, "Nexus-Documentation-Package.html"), join(combinedDir, "Nexus-Documentation-Package.pdf"));
  console.log("PDF  combined/Nexus-Documentation-Package.pdf");
}
