# ProPR Review Instructions

## Project Summary

This repository is a small static blog demo used for pull request review workflows.

- `content/` contains markdown source files and frontmatter.
- `src/sitegen/Main.java` builds markdown content into static HTML pages under `dist/`.
- `build.sh` compiles the Java generator with `javac`, runs it, and copies static assets.
- `static/styles.css` is the editorial and visual baseline stylesheet copied to `dist/styles.css`.

## Review Priorities

Prioritize correctness, regressions, and maintainability over style nits.

Focus most on:

- content pipeline correctness
- route generation and navigation behavior
- consistency between markdown frontmatter and generated output
- user-facing rendering issues
- changes that weaken escaping or HTML safety

Avoid low-value comments about minor wording, formatting, or subjective style unless they affect behavior, clarity, or consistency.

## Important Repo Conventions

### Content Structure

- `content/index.md` maps to `/`.
- `content/<name>.md` maps to `/<name>/`.
- `content/<section>/_index.md` defines a top-level section at `/<section>/`.
- additional markdown files in `content/<section>/` become article pages at `/<section>/<article>/`.

Reviewers should flag changes that break these conventions without updating the generator consistently.

### Build Output Rules

- `dist/` is generated and should normally not be edited by hand.
- if a PR changes `content/`, `src/sitegen/Main.java`, or `build.sh`, check whether generated output or tests should also be refreshed locally.
- if a PR adds generated HTML changes without a matching source change or clear reason, that is likely a problem.

### Sorting and Navigation Invariants

The current generator behavior is intentional:

- pages and sections are ordered by `order`, then `title`
- articles are ordered by `date` descending, then `order`, then `title`
- navigation is derived from root markdown pages and section indexes, not maintained separately
- new user-facing sections should reuse the existing section and article pipeline instead of introducing parallel content models or rendering flows

Flag PRs that accidentally change these ordering rules or introduce duplicated sources of truth.

### Routing Expectations

The generated site uses static routes with trailing slashes.

- `/` resolves from `content/index.md`
- top-level pages and sections render to directory-style routes like `/about/` and `/blog/`
- article pages render below their section, such as `/blog/welcome-to-the-demo/`

Be alert for regressions where route generation, active navigation state, or static file layout stops matching the content tree.

## Risk Areas Worth Extra Attention

### HTML Rendering

`src/sitegen/Main.java` converts markdown to HTML directly during the build.

Reviewers should scrutinize changes that:

- allow raw or unescaped HTML from content sources
- bypass the markdown build pipeline
- expand rendering behavior without preserving escaping rules

### Content Parsing Drift

Frontmatter parsing, route generation, and HTML rendering all live in `src/sitegen/Main.java`.

If a PR changes the expected content shape, verify that:

- frontmatter parsing still accepts the repository's existing markdown files
- route generation still matches the content conventions
- rendered HTML still supports the markdown features currently used in `content/`

### Date Handling

Article dates come from frontmatter strings and are used for ordering.

Flag changes that make article ordering unstable, accept invalid dates silently, or mix sorting rules between listing and route generation.

## Good Review Questions

When relevant, ask yourself and analyze:

- Does this change preserve the content-to-route mapping rules?
- If source markdown changed, does the generator still render every required route?
- If the generator changed, do navigation, article ordering, and page output still match the repo conventions?
- Does this introduce a second source of truth for navigation or routes?
- Does this change broaden the HTML safety surface?
- Does this alter article or navigation order unintentionally?

## Review Tone

Keep comments concrete and actionable. Prefer identifying:

- broken behavior
- parsing or route regressions
- rendering and escaping issues
- maintainability risks from duplicated logic

Prefer not to comment on purely stylistic or syntactic choices unless they obscure intent or increase the chance of future mistakes.
