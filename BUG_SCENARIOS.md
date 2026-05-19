# Bug Scenarios

These feature branches are intentionally defective review targets built from a clean `main` branch.

- `feature/bug_1`: add reading-time metadata but compute it from rendered HTML instead of markdown text
- `feature/bug_2`: add a latest-posts panel but sort posts in ascending date order
- `feature/bug_3`: render formatted article summaries as raw HTML and widen the HTML injection surface
- `feature/bug_4`: add related-post navigation but exclude the current article incorrectly when dates are missing or titles collide
- `feature/bug_5`: make article cards fully clickable using nested interactive elements
- `feature/bug_6`: generate a sitemap but omit article pages from the output
- `feature/bug_7`: add draft preview output but allow writing generated HTML to an arbitrary path from `SITE_PREVIEW_PATH`
- `feature/bug_8`: render inline HTML snippets from markdown and bypass normal HTML escaping
- `feature/bug_9`: add a content include filter but silently drop pages based on an unchecked regex from `SITE_INCLUDE_FILTER`
- `feature/bug_10`: add a post-build hook but execute arbitrary shell from `SITE_POST_BUILD_HOOK`
- `feature/bug_11`: log the build root path and leak filesystem location details during generation
- `feature/bug_12`: cache parsed markdown metadata using a broken key equality check that can mix up documents
- `feature/bug_13`: add a stable article sort tie-breaker that compares each title against itself and never breaks ties
- `feature/bug_14`: add section entries to nav data but use the section title as the href instead of the section path
- `feature/bug_15`: read markdown files through streams but never close the input stream
- `feature/bug_16`: add date parsing fallback but swallow `IllegalArgumentException` and turn invalid values into missing dates
- `feature/bug_17`: reuse a description visibility flag but redundantly check the same condition and add dead logic
- `feature/bug_18`: show article count on section pages but undercount by subtracting one from the actual total
- `feature/bug_19`: use a compact article date format but output month/day/day instead of month/day/year
- `feature/bug_20`: support repeated frontmatter keys but concatenate duplicate values without any separator
- `feature/bug_21`: thread page title through the layout but replace the site title in the header with the current page title
- `feature/bug_22`: show a section accent label but crash when `accent` is missing and leak note-like summaries into article pages
- `feature/bug_23`: stream generated page output but never close the output stream
- `feature/bug_24`: add highlighted inline markdown but copy raw content into a `data-source` attribute
- `feature/bug_25`: allow overriding the content import directory but read from an arbitrary path via `SITE_IMPORT_DIR`
- `feature/bug_26`: keep a draft date recovery hook as unreachable dead code in date parsing
- `feature/semantic_bug_1`: document that first-class content must live under `content/` but add a hardcoded handbook section directly in code
