// Saves a screenshot of every page at phone, tablet and desktop width, and prints axe accessibility violations.
// Needs the website running (npm run dev) and the backend for the room data. Run: npm run screenshots
import { chromium } from "playwright";
import { AxeBuilder } from "@axe-core/playwright";
import { mkdirSync } from "node:fs";

const BASE_URL = process.env.BASE_URL ?? "http://localhost:5173";
const OUTPUT_DIR = "../../screenshots";
const BROWSER_CHANNEL = "msedge"; // shortcut: uses the installed Edge to skip a browser download, switch to the bundled chromium on CI
const PAGES = ["/", "/rooms", "/booking", "/not-found"];
const VIEWPORTS = { phone: { width: 375, height: 812 }, tablet: { width: 768, height: 1024 }, desktop: { width: 1280, height: 800 } };
const SETTLE_MS = 1000;

mkdirSync(OUTPUT_DIR, { recursive: true });
const browser = await chromium.launch({ channel: BROWSER_CHANNEL });

for (const path of PAGES) {
    for (const [viewportName, viewport] of Object.entries(VIEWPORTS)) {
        const context = await browser.newContext({ viewport }); // axe requires a page made from a context
        const page = await context.newPage();
        await page.goto(BASE_URL + path);
        await page.waitForTimeout(SETTLE_MS);
        const pageName = path === "/" ? "home" : path.slice(1);
        await page.screenshot({ path: `${OUTPUT_DIR}/${pageName}-${viewportName}.png`, fullPage: true });
        if (viewportName === "desktop") {
            const { violations } = await new AxeBuilder({ page }).analyze();
            console.log(`${path}: ${violations.length} accessibility violations`, violations.map(v => v.id));
        }
        await context.close();
    }
}
await browser.close();
