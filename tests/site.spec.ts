import { expect, test } from '@playwright/test';

test('home page renders sorted primary nav', async ({ page }) => {
  await page.goto('/');

  await expect(page.getByRole('heading', { level: 1, name: 'Propr Review Demo' }).first()).toBeVisible();
  await expect(page.getByRole('link', { name: 'Propr Review Demo' }).first()).toHaveAttribute('href', '/');

  const navLinks = page.getByRole('navigation', { name: 'Primary' }).getByRole('link');
  await expect(navLinks).toHaveText(['Propr Review Demo', 'Blog', 'About']);
  await expect(navLinks.nth(0)).toHaveAttribute('href', '/');
  await expect(navLinks.nth(1)).toHaveAttribute('href', '/blog/');
  await expect(navLinks.nth(2)).toHaveAttribute('href', '/about/');
});

test('about page renders content from root markdown', async ({ page }) => {
  await page.goto('/about/');

  await expect(page).toHaveTitle('About');
  await expect(page.getByRole('heading', { level: 1, name: 'About' }).first()).toBeVisible();
  await expect(page.getByText('The goal is to have a maintainable codebase')).toBeVisible();
});

test('blog listing renders section content and sorted articles', async ({ page }) => {
  await page.goto('/blog/');

  await expect(page).toHaveTitle('Blog');
  await expect(page.getByRole('heading', { level: 1, name: 'Blog' }).first()).toBeVisible();
  await expect(page.getByText('The blog section is driven by file convention.')).toBeVisible();

  const articleHeadings = page.locator('.article-card h2');
  await expect(articleHeadings).toHaveText([
    'Welcome to the Demo',
    'Reviewing Pull Requests Effectively',
  ]);
});

test('article page renders article content', async ({ page }) => {
  await page.goto('/blog/welcome-to-the-demo/');

  await expect(page).toHaveTitle('Welcome to the Demo');
  await expect(page.getByRole('heading', { level: 1, name: 'Welcome to the Demo' }).first()).toBeVisible();
  await expect(page.getByRole('link', { name: 'Back to Blog' })).toHaveAttribute('href', '/blog/');
  await expect(page.getByText('Write markdown, add frontmatter, and the build step')).toBeVisible();
});

test('blog article links preserve descending date order', async ({ page }) => {
  await page.goto('/blog/');

  const articleLinks = page.locator('.article-card h2 a');
  await expect(articleLinks).toHaveCount(2);
  await expect(articleLinks.nth(0)).toHaveAttribute('href', '/blog/welcome-to-the-demo/');
  await expect(articleLinks.nth(1)).toHaveAttribute('href', '/blog/reviewing-pull-requests-effectively/');
});
