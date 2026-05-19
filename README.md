# Propr Review Demo Java

Small static blog review demo built with a native Java static-site generator.

## Project Layout

- `content/` contains markdown source files and frontmatter.
- `src/sitegen/Main.java` builds pages from content.
- `static/styles.css` is copied into `dist/styles.css` during build.
- `dist/` is generated output.
- `tests/site.spec.ts` contains Playwright end-to-end coverage.

## Content Model

All first-class site content should be content-driven under `content/` so editorial structure stays visible in the repository tree.

## Build

```bash
./build.sh
```

## Test

Install Node dependencies once:

```bash
npm install
```

Run the end-to-end suite:

```bash
npm run test:e2e
```

The Playwright `webServer` command rebuilds the site and serves `dist/` with `python3 -m http.server`.

## Review branches

- `BUG_SCENARIOS.md` lists the intentionally defective feature branches that should be reviewed against `main`.
