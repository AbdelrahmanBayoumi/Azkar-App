import { expect, test } from "bun:test";
import { createElement } from "react";
import { renderToString } from "react-dom/server";
import { Gallery } from "../src/download/gallery.js";
import { Installers } from "../src/download/installers.js";
import { detectPlatform } from "../src/download/platforms.js";
import { tabIndexForKey } from "../src/download/ui.js";

test("static installer markup retains fallback download links", () => {
  const html = renderToString(createElement(Installers));
  expect(html).toContain('id="deb-download"');
  expect(html).toContain(
    'href="https://github.com/jamhour1g/Azkar-App/releases"',
  );
  expect(html).toContain('aria-controls="panel-linux"');
  expect(html).toContain('id="apt-commands"');
  expect(html).toContain('id="dnf-commands"');
});

test("static gallery markup includes captions and accessible tab targets", () => {
  const html = renderToString(createElement(Gallery));
  expect(html).toContain('aria-controls="screen-panel-home"');
  expect(html).toContain('aria-labelledby="screen-tab-home"');
  expect(html).toContain("Your day at a glance");
  expect(html).toContain("A reminder during your day");
});

test("tab keys wrap and select the first and last tabs", () => {
  expect(tabIndexForKey("ArrowLeft", 0, 3)).toBe(2);
  expect(tabIndexForKey("ArrowRight", 2, 3)).toBe(0);
  expect(tabIndexForKey("Home", 1, 3)).toBe(0);
  expect(tabIndexForKey("End", 1, 3)).toBe(2);
  expect(tabIndexForKey("Enter", 1, 3)).toBeUndefined();
});

test("detects Darwin as macOS and recognizes Windows platform names", () => {
  expect(detectPlatform("Darwin")).toBe("macos");
  expect(detectPlatform("Win32")).toBe("windows");
  expect(detectPlatform("Linux x86_64")).toBe("linux");
});
