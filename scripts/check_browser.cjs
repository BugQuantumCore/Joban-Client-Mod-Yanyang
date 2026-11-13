/* Browser regressions run by .github/workflows/docs-check.yml, never at page load. */
const assert = require("node:assert/strict");
const fs = require("node:fs/promises");
const http = require("node:http");
const path = require("node:path");
const { chromium } = require("playwright");

const root = path.resolve(__dirname, "..");
const locales = ["zh-CN", "zh-HK", "zh-TW", "lzh", "en-US", "en-GB", "fr-FR"];
const mime = { ".html": "text/html; charset=utf-8", ".css": "text/css", ".js": "text/javascript" };

async function main() {
  const server = http.createServer(async (request, response) => {
    try {
      const pathname = decodeURIComponent(new URL(request.url, "http://localhost").pathname);
      const file = path.resolve(root, "." + (pathname.endsWith("/") ? pathname + "index.html" : pathname));
      if (!file.startsWith(root + path.sep)) throw new Error("Outside site root");
      const data = await fs.readFile(file);
      response.writeHead(200, { "Content-Type": mime[path.extname(file)] || "application/octet-stream" });
      response.end(data);
    } catch (_) {
      response.writeHead(404);
      response.end("Not found");
    }
  });
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
  const origin = `http://127.0.0.1:${server.address().port}`;
  let browser;
  let page;
  const errors = [];
  try {
    browser = await chromium.launch();
    page = await browser.newPage({ viewport: { width: 1280, height: 900 } });
    page.on("pageerror", (error) => errors.push(`${page.url()}: ${error.message}`));
    const pages = ["index.html"];
    for (const locale of locales) {
      for (const name of (await fs.readdir(path.join(root, locale))).filter((name) => name.endsWith(".html"))) {
        pages.push(`${locale}/${name}`);
      }
    }
    for (const name of pages) {
      const response = await page.goto(`${origin}/${name}`);
      assert.equal(response.status(), 200, name);
      assert.equal(await page.locator("h1").count(), 1, `${name}: expected one title`);
      // Highlighting must preserve the code copied by readers, including angle brackets.
      const codePreserved = await page.evaluate(async () => {
        const html = await (await fetch(location.href)).text();
        const source = new DOMParser().parseFromString(html, "text/html");
        const before = [...source.querySelectorAll("pre > code")].map((code) => code.textContent);
        const after = [...document.querySelectorAll("pre > code")].map((code) => code.textContent);
        return JSON.stringify(before) === JSON.stringify(after);
      });
      assert.ok(codePreserved, `${name}: highlighting changed example source`);
    }

    for (const locale of ["zh-CN", "lzh"]) {
      await page.setViewportSize({ width: 390, height: 844 });
      await page.goto(`${origin}/${locale}/js-api.html?ci=1#render`);
      assert.ok(await page.locator(".sidebar").evaluate((sidebar) => sidebar.inert));
      await page.locator(".menu-btn").click();
      const drawerFocus = await page.locator(".sidebar").evaluate((sidebar) => ({
        inside: sidebar.contains(document.activeElement),
        active: document.activeElement.outerHTML.slice(0, 180),
        visibility: getComputedStyle(sidebar).visibility,
        inert: sidebar.inert,
        open: sidebar.classList.contains("open"),
      }));
      assert.ok(drawerFocus.inside, `${locale}: opening drawer must move focus inside: ${JSON.stringify(drawerFocus)}`);
      await page.keyboard.press("Shift+Tab");
      assert.ok(await page.locator(".lang-select").evaluate((select) => select === document.activeElement));
      await page.keyboard.press("Tab");
      assert.ok(await page.locator(".brand").evaluate((brand) => brand === document.activeElement));
      await page.keyboard.press("Escape");
      assert.ok(await page.locator(".menu-btn").evaluate((button) => button === document.activeElement));
      assert.ok(await page.locator(".sidebar").evaluate((sidebar) => sidebar.inert));

      await page.locator(".menu-btn").click();
      // A resize during a swipe must also remove the transient inline transform.
      await page.locator(".sidebar").evaluate((sidebar) => { sidebar.style.transform = "translateX(40px)"; });
      await page.setViewportSize({ width: 1280, height: 900 });
      await page.waitForFunction(() => !document.body.classList.contains("nav-open"));
      assert.ok(await page.locator(".sidebar").evaluate((sidebar) => !sidebar.inert && !sidebar.style.transform));
      await page.setViewportSize({ width: 390, height: 844 });
      await page.waitForFunction(() => document.querySelector(".sidebar").inert);
      await page.locator(".menu-btn").click();
      await page.locator(".lang-select").selectOption("en-US");
      await page.waitForURL(`${origin}/en-US/js-api.html?ci=1#render`);
    }

    await page.goto(`${origin}/lzh/js-types.html`);
    const scroll = await page.evaluate(() => {
      const surface = document.querySelector(".content");
      const pre = [...document.querySelectorAll("pre")].find((element) => element.scrollHeight > element.clientHeight + 20);
      if (!pre) throw new Error("Expected a vertically scrollable code example");
      surface.scrollLeft = 0;
      pre.scrollTop = 1;
      const nested = new WheelEvent("wheel", { bubbles: true, cancelable: true, deltaY: 40 });
      pre.dispatchEvent(nested);
      const nestedPreserved = !nested.defaultPrevented && surface.scrollLeft === 0;
      const zoom = new WheelEvent("wheel", { bubbles: true, cancelable: true, deltaY: 40, ctrlKey: true });
      surface.dispatchEvent(zoom);
      const zoomPreserved = !zoom.defaultPrevented && surface.scrollLeft === 0;
      const turn = new WheelEvent("wheel", { bubbles: true, cancelable: true, deltaY: 40 });
      surface.dispatchEvent(turn);
      return { nestedPreserved, zoomPreserved, turnsPage: turn.defaultPrevented && surface.scrollLeft < 0 };
    });
    assert.deepEqual(scroll, { nestedPreserved: true, zoomPreserved: true, turnsPage: true });

    await page.goto(`${origin}/zh-CN/js-api.html`);
    await page.evaluate(() => {
      Object.defineProperty(navigator, "clipboard", { configurable: true, value: undefined });
      document.execCommand = () => { throw new Error("Clipboard unavailable"); };
    });
    const copy = page.locator(".copy-btn").first();
    await copy.click();
    await page.waitForFunction(() => document.querySelector(".copy-btn").classList.contains("err"));
    assert.equal(await page.locator("textarea").count(), 0, "failed fallback left its hidden textarea");
    assert.ok(await copy.evaluate((button) => button === document.activeElement), "copy fallback lost keyboard focus");
    assert.deepEqual(errors, [], "uncaught page JavaScript errors");
    console.log(`Checked ${pages.length} pages, mobile navigation, locale anchors, scroll and clipboard regressions.`);
  } catch (error) {
    await fs.mkdir(path.join(root, "artifacts"), { recursive: true });
    if (page) await page.screenshot({ path: path.join(root, "artifacts", "failure.png"), fullPage: true }).catch(() => {});
    throw error;
  } finally {
    if (browser) await browser.close();
    await new Promise((resolve) => server.close(resolve));
  }
}

main().catch((error) => { console.error(error); process.exitCode = 1; });
