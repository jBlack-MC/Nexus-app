# Nexus documentation package

Twelve numbered deliverables built from repository evidence at commit `3a3f275` (`New-Changes`; same tree as `main` at `9f8e82a`). Status: **draft for team review**. Nobody has approved these documents yet.

Open `index.html` to browse. For a single submission file, use `combined/Nexus-Documentation-Package.pdf`.

| ID | Deliverable | HTML | PDF |
| --- | --- | --- | --- |
| NX-01-PRD | Product requirements | `html/01-product-requirements.html` | `pdf/01-product-requirements.pdf` |
| NX-02-TRD | Technical requirements | `html/02-technical-requirements.html` | `pdf/02-technical-requirements.pdf` |
| NX-03-FLOW | Application flow | `html/03-application-flow.html` | `pdf/03-application-flow.pdf` |
| NX-04-DES | Design brief | `html/04-design-brief.html` | `pdf/04-design-brief.pdf` |
| NX-05-DATA | Backend and data schema | `html/05-data-schema.html` | `pdf/05-data-schema.pdf` |
| NX-06-PLAN | Implementation plan | `html/06-implementation-plan.html` | `pdf/06-implementation-plan.pdf` |
| NX-07-CICD | CI/CD and release | `html/07-cicd-release.html` | `pdf/07-cicd-release.pdf` |
| NX-08-AUD | Engineering audit (+ remediation addendum) | `html/08-engineering-audit.html` | `pdf/08-engineering-audit.pdf` |
| NX-09-TDH | Testing, deployment and handover | `html/09-testing-deployment-handover.html` | `pdf/09-testing-deployment-handover.pdf` |
| NX-10-UM | User manual | `html/10-user-manual.html` | `pdf/10-user-manual.pdf` |
| NX-11-DEP | Deployment guide | `html/11-deployment-guide.html` | `pdf/11-deployment-guide.pdf` |
| NX-12-TCR | Team contribution report | `html/12-team-contribution.html` | `pdf/12-team-contribution.pdf` |

## Building

Requires Node 18+ and, for PDFs, a local Chrome or Edge (set `CHROME_PATH` if it is not in a standard location). There are no npm dependencies.

```sh
node docs/deliverables/tools/build.mjs --pdf   # HTML + PDF (omit --pdf for HTML only)
node docs/deliverables/tools/validate.mjs      # exit 0 = all checks pass
```

The validator checks:

- every expected HTML and PDF file exists;
- each PDF has a valid header, a `%%EOF` trailer and at least one page;
- every link, anchor and image resolves, and every `[[ref]]` and label token has been resolved;
- images have alt text and SVG diagrams have accessible names;
- no repository test password, CI secret, JWT, non-example email or student number appears in the output;
- the audit summary counts match the findings table;
- every cited repository path exists (two intentional exceptions are listed in the script).

No DOCX is produced, because the project has no DOCX tooling (no pandoc, LibreOffice or docx library). If Word is required, open the combined HTML in Word and save it as `.docx`.

## Editing

Edit only `src/NN-*.html`. Everything in `html/`, `pdf/`, `combined/` and `index.html` is generated.

- **Metadata:** each source file starts with a `<!--meta ... -->` block (id, title, subtitle, audience). The package version, date, status and baseline are set in `PACKAGE` in `tools/build.mjs`.
- **Stable IDs:** declare them as `id="FR-01"` on a row or heading. Reference them anywhere with `[[FR-01]]`, or `[[FR-01|custom text]]` to change the link text. IDs must be unique across the whole package.
- **Evidence labels:** `{{F}}` Implemented, `{{I}}` Inferred, `{{R}}` Recommendation, `{{Q}}` Open question, `{{U}}` Unknown, `{{E}}` Estimate.
- **Numbering and contents:** section numbers and the table of contents are generated from `<h2>`/`<h3>`.

When the code changes, update the baseline in `PACKAGE`, re-check the affected statements, then rebuild and re-validate.
