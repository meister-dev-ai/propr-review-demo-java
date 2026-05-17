package sitegen;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Main {
    private static final Pattern INLINE_CODE = Pattern.compile("`([^`]+)`");
    private static final Pattern STRONG_TEXT = Pattern.compile("\\*\\*([^*]+)\\*\\*");

    private Main() {
    }

    public static void main(String[] args) throws IOException {
        Path root = args.length > 0 ? Path.of(args[0]) : Path.of(".").toAbsolutePath().normalize();
        Site site = loadSite(root);
        renderSite(root, site);
    }

    private static Site loadSite(Path root) throws IOException {
        Path contentDir = root.resolve("content");
        List<Page> pages = new ArrayList<>();
        List<Section> sections = new ArrayList<>();

        try (Stream<Path> stream = Files.list(contentDir)) {
            List<Path> entries = stream.sorted().toList();
            for (Path entry : entries) {
                String name = entry.getFileName().toString();
                if (Files.isRegularFile(entry) && name.endsWith(".md")) {
                    pages.add(loadPage(entry));
                } else if (Files.isDirectory(entry)) {
                    Path indexPath = entry.resolve("_index.md");
                    if (Files.exists(indexPath)) {
                        sections.add(loadSection(entry));
                    }
                }
            }
        }

        pages.sort(pageComparator());
        sections.sort(pageComparator());

        List<NavItem> navItems = new ArrayList<>();
        pages.forEach(page -> navItems.add(new NavItem(page.title(), page.path(), page.order())));
        sections.forEach(section -> navItems.add(new NavItem(section.title(), section.title(), section.order())));
        navItems.sort(Comparator.comparingInt(NavItem::order).thenComparing(item -> item.title().toLowerCase(Locale.ROOT)));

        String siteTitle = pages.stream()
            .filter(page -> "/".equals(page.path()))
            .map(Page::title)
            .findFirst()
            .orElse("Site");
        String siteDescription = pages.stream()
            .filter(page -> "/".equals(page.path()))
            .map(Page::description)
            .filter(description -> !description.isBlank())
            .findFirst()
            .orElse("");

        return new Site(siteTitle, siteDescription, pages, sections, navItems);
    }

    private static Page loadPage(Path file) throws IOException {
        MarkdownDocument document = parseMarkdownFile(file);
        String slug = file.getFileName().toString().replaceFirst("\\.md$", "");
        String path = slug.equals("index") ? "/" : "/" + slug + "/";
        return new Page(
            document.frontmatter().getOrDefault("title", slug),
            document.frontmatter().getOrDefault("description", ""),
            parseOrder(document.frontmatter().get("order")),
            path,
            renderMarkdown(document.body())
        );
    }

    private static Section loadSection(Path directory) throws IOException {
        MarkdownDocument indexDocument = parseMarkdownFile(directory.resolve("_index.md"));
        String slug = directory.getFileName().toString();
        List<Article> articles = new ArrayList<>();

        try (Stream<Path> stream = Files.list(directory)) {
            List<Path> files = stream
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(".md"))
                .filter(path -> !path.getFileName().toString().equals("_index.md"))
                .sorted()
                .toList();

            for (Path file : files) {
                MarkdownDocument document = parseMarkdownFile(file);
                String articleSlug = file.getFileName().toString().replaceFirst("\\.md$", "");
                articles.add(new Article(
                    document.frontmatter().getOrDefault("title", articleSlug),
                    document.frontmatter().getOrDefault("description", ""),
                    document.frontmatter().getOrDefault("summary", ""),
                    parseOrder(document.frontmatter().get("order")),
                    parseDate(document.frontmatter().get("date")),
                    "/" + slug + "/" + articleSlug + "/",
                    renderMarkdown(document.body())
                ));
            }
        }

        articles.sort(articleComparator());

        return new Section(
            indexDocument.frontmatter().getOrDefault("title", slug),
            indexDocument.frontmatter().getOrDefault("description", ""),
            parseOrder(indexDocument.frontmatter().get("order")),
            "/" + slug + "/",
            renderMarkdown(indexDocument.body()),
            articles
        );
    }

    private static Comparator<PageLike> pageComparator() {
        return Comparator.comparingInt(PageLike::order).thenComparing(item -> item.title().toLowerCase(Locale.ROOT));
    }

    private static Comparator<Article> articleComparator() {
        return Comparator.comparing(Article::date, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparingInt(Article::order)
            .thenComparing(article -> article.title().toLowerCase(Locale.ROOT));
    }

    private static void renderSite(Path root, Site site) throws IOException {
        Path distDir = root.resolve("dist");
        Files.createDirectories(distDir);
        Files.copy(root.resolve("static/styles.css"), distDir.resolve("styles.css"), StandardCopyOption.REPLACE_EXISTING);

        for (Page page : site.pages()) {
            writePage(distDir, page.path(), renderStandardPage(site, page.title(), page.description(), page.path(), page.contentHtml()));
        }

        for (Section section : site.sections()) {
            writePage(distDir, section.path(), renderSectionPage(site, section));
            for (Article article : section.articles()) {
                writePage(distDir, article.path(), renderArticlePage(site, section, article));
            }
        }
    }

    private static void writePage(Path distDir, String routePath, String html) throws IOException {
        Path target = routePath.equals("/")
            ? distDir.resolve("index.html")
            : distDir.resolve(routePath.substring(1)).resolve("index.html");
        Files.createDirectories(Objects.requireNonNull(target.getParent()));
        Files.writeString(target, html, StandardCharsets.UTF_8);
    }

    private static String renderStandardPage(Site site, String title, String description, String activePath, String bodyHtml) {
        String content = "<article class=\"panel stack-gap\">"
            + renderPanelHeader(title, description)
            + "<div class=\"markdown\">" + bodyHtml + "</div>"
            + "</article>";
        return renderDocument(site, title, activePath, content);
    }

    private static String renderSectionPage(Site site, Section section) {
        String articleCards = section.articles().stream()
            .map(article -> "<article class=\"article-card\">"
                + "<div class=\"article-card-meta\"><span>" + escapeHtml(formatDate(article.date())) + "</span></div>"
                + "<h2><a href=\"" + article.path() + "\">" + escapeHtml(article.title()) + "</a></h2>"
                + (!article.summary().isBlank() ? "<p>" + escapeHtml(article.summary()) + "</p>" : "")
                + "</article>")
            .collect(Collectors.joining());

        String content = "<section class=\"panel stack-gap\">"
            + renderPanelHeader(section.title(), section.description())
            + "<div class=\"markdown\">" + section.contentHtml() + "</div>"
            + "<div class=\"article-list\">" + articleCards + "</div>"
            + "</section>";

        return renderDocument(site, section.title(), section.path(), content);
    }

    private static String renderArticlePage(Site site, Section section, Article article) {
        String description = article.description().isBlank() ? section.description() : article.description();
        String content = "<article class=\"panel stack-gap\">"
            + "<a class=\"back-link\" href=\"" + section.path() + "\">Back to " + escapeHtml(section.title()) + "</a>"
            + renderPanelHeader(article.title(), description)
            + "<div class=\"article-card-meta\"><span>" + escapeHtml(formatDate(article.date())) + "</span></div>"
            + "<div class=\"markdown\">" + article.contentHtml() + "</div>"
            + "</article>";

        return renderDocument(site, article.title(), section.path(), content);
    }

    private static String renderPanelHeader(String title, String description) {
        return "<header class=\"panel-header\">"
            + "<h1>" + escapeHtml(title) + "</h1>"
            + (!description.isBlank() ? "<p>" + escapeHtml(description) + "</p>" : "")
            + "</header>";
    }

    private static String renderDocument(Site site, String title, String activePath, String content) {
        String nav = site.navItems().stream()
            .map(item -> {
                boolean active = item.path().equals(activePath);
                String className = active ? "nav-link nav-link-active" : "nav-link";
                return "<a class=\"" + className + "\" href=\"" + item.path() + "\">" + escapeHtml(item.title()) + "</a>";
            })
            .collect(Collectors.joining());

        return "<!DOCTYPE html>"
            + "<html lang=\"en\">"
            + "<head>"
            + "<meta charset=\"utf-8\">"
            + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
            + "<title>" + escapeHtml(title) + "</title>"
            + "<link rel=\"stylesheet\" href=\"/styles.css\">"
            + "</head>"
            + "<body>"
            + "<div class=\"app-shell\">"
            + "<header class=\"site-header\">"
            + "<div><a class=\"site-title\" href=\"/\">" + escapeHtml(site.title()) + "</a>"
            + (!site.description().isBlank() ? "<p class=\"site-tagline\">" + escapeHtml(site.description()) + "</p>" : "")
            + "</div>"
            + "<nav class=\"site-nav\" aria-label=\"Primary\">" + nav + "</nav>"
            + "</header>"
            + "<main>" + content + "</main>"
            + "</div>"
            + "</body>"
            + "</html>";
    }

    private static MarkdownDocument parseMarkdownFile(Path file) throws IOException {
        String source = Files.readString(file, StandardCharsets.UTF_8).replace("\r\n", "\n");
        if (!source.startsWith("---\n")) {
            throw new IllegalArgumentException("Missing frontmatter in " + file);
        }

        int boundary = source.indexOf("\n---\n", 4);
        if (boundary < 0) {
            throw new IllegalArgumentException("Unclosed frontmatter in " + file);
        }

        String frontmatterBlock = source.substring(4, boundary);
        String body = source.substring(boundary + 5).trim();
        Map<String, String> frontmatter = new LinkedHashMap<>();
        for (String line : frontmatterBlock.split("\n")) {
            int separator = line.indexOf(':');
            if (separator <= 0) {
                continue;
            }
            String key = line.substring(0, separator).trim();
            String value = line.substring(separator + 1).trim();
            frontmatter.put(key, value);
        }

        return new MarkdownDocument(frontmatter, body);
    }

    private static String renderMarkdown(String markdown) {
        List<String> lines = markdown.isBlank() ? List.of() : List.of(markdown.split("\n"));
        StringBuilder html = new StringBuilder();
        List<String> paragraph = new ArrayList<>();
        List<String> listItems = new ArrayList<>();

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                flushParagraph(html, paragraph);
                flushList(html, listItems);
                continue;
            }
            if (line.startsWith("# ")) {
                flushParagraph(html, paragraph);
                flushList(html, listItems);
                html.append("<h1>").append(renderInlineMarkdown(line.substring(2).trim())).append("</h1>");
                continue;
            }
            if (line.startsWith("- ")) {
                flushParagraph(html, paragraph);
                listItems.add(line.substring(2).trim());
                continue;
            }
            flushList(html, listItems);
            paragraph.add(line);
        }

        flushParagraph(html, paragraph);
        flushList(html, listItems);
        return html.toString();
    }

    private static void flushParagraph(StringBuilder html, List<String> paragraph) {
        if (paragraph.isEmpty()) {
            return;
        }
        String text = String.join(" ", paragraph);
        html.append("<p>").append(renderInlineMarkdown(text)).append("</p>");
        paragraph.clear();
    }

    private static void flushList(StringBuilder html, List<String> listItems) {
        if (listItems.isEmpty()) {
            return;
        }
        html.append("<ul>");
        for (String item : listItems) {
            html.append("<li>").append(renderInlineMarkdown(item)).append("</li>");
        }
        html.append("</ul>");
        listItems.clear();
    }

    private static String renderInlineMarkdown(String text) {
        String html = escapeHtml(text);
        html = replacePattern(html, STRONG_TEXT, "strong");
        html = replacePattern(html, INLINE_CODE, "code");
        return html;
    }

    private static String replacePattern(String input, Pattern pattern, String tag) {
        Matcher matcher = pattern.matcher(input);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(result, "<" + tag + ">" + Matcher.quoteReplacement(matcher.group(1)) + "</" + tag + ">");
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static int parseOrder(String value) {
        if (value == null || value.isBlank()) {
            return Integer.MAX_VALUE;
        }
        return Integer.parseInt(value);
    }

    private static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Invalid date: " + value, exception);
        }
    }

    private static String formatDate(LocalDate date) {
        return date == null ? "" : date.toString();
    }

    private static String escapeHtml(String text) {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }

    private record MarkdownDocument(Map<String, String> frontmatter, String body) {
    }

    private interface PageLike {
        String title();
        int order();
    }

    private record Page(String title, String description, int order, String path, String contentHtml) implements PageLike {
    }

    private record Section(String title, String description, int order, String path, String contentHtml, List<Article> articles) implements PageLike {
    }

    private record Article(String title, String description, String summary, int order, LocalDate date, String path, String contentHtml) {
    }

    private record NavItem(String title, String path, int order) {
    }

    private record Site(String title, String description, List<Page> pages, List<Section> sections, List<NavItem> navItems) {
    }
}
