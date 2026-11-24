import { expect, test } from "@playwright/test";

const releasesApi =
  "https://api.github.com/repos/jamhour1g/Azkar-App/releases**";
const releasesPage = "https://github.com/jamhour1g/Azkar-App/releases";
const windowsInstaller = `${releasesPage}/download/v1.2.3/azkar-1.2.3-windows-x64-nsis.exe`;

test("serves pre-rendered content at both published URLs", async ({
  request,
}) => {
  const [home, download] = await Promise.all([
    request.get("/"),
    request.get("/download.html"),
  ]);
  expect(home.ok()).toBe(true);
  expect(download.ok()).toBe(true);
  const html = await download.text();
  expect(await home.text()).toBe(html);
  expect(html).toContain('id="deb-download"');
  expect(html).toContain('id="screen-tab-home"');
  expect(html).toContain('src="./js/download.js"');
  expect(html).toContain('href="./css/download.css"');
});

test("platform tabs support click and arrow navigation after hydration", async ({
  page,
}) => {
  await page.route(releasesApi, (route) =>
    route.fulfill({
      json: [
        {
          draft: false,
          prerelease: false,
          tag_name: "v1.2.3",
          html_url: `${releasesPage}/tag/v1.2.3`,
          assets: [
            {
              name: "azkar-1.2.3-windows-x64-nsis.exe",
              browser_download_url: windowsInstaller,
            },
          ],
        },
      ],
    }),
  );

  await page.goto("/download.html");
  await expect(
    page
      .getByRole("status", { name: "" })
      .filter({ hasText: "missing some installers" }),
  ).toBeVisible();

  const tabs = page.getByRole("tablist", { name: "Platform" });
  const windows = tabs.getByRole("tab", { name: "Windows" });
  const mac = tabs.getByRole("tab", { name: "macOS" });

  await windows.click();
  const windowsPanel = page.getByRole("tabpanel", { name: "Windows" });
  await expect(
    windowsPanel.getByRole("link", { name: /Download Windows/ }),
  ).toHaveAttribute("href", windowsInstaller);

  await windows.press("ArrowRight");
  await expect(mac).toBeFocused();
  await expect(mac).toHaveAttribute("aria-selected", "true");
  await expect(windowsPanel).toBeHidden();
  await expect(
    page
      .getByRole("tabpanel", { name: "macOS" })
      .getByRole("link", { name: /Apple silicon releases/ }),
  ).toHaveAttribute("href", releasesPage);

  await mac.press("End");
  await expect(tabs.getByRole("tab", { name: "Linux" })).toBeFocused();
});

test("shows the release link when GitHub is unavailable", async ({ page }) => {
  await page.route(releasesApi, (route) => route.fulfill({ status: 503 }));
  await page.goto("/download.html");

  await expect(page.getByText("Could not check for releases.")).toBeVisible();
  await expect(
    page.getByRole("link", { name: "Browse all releases" }),
  ).toHaveAttribute("href", releasesPage);
});

test("keeps the terminal section open when switching platforms", async ({
  page,
}) => {
  await page.route(releasesApi, (route) => route.fulfill({ status: 503 }));
  await page.goto("/download.html");
  await expect(page.getByText("Could not check for releases.")).toBeVisible();

  const tabs = page.getByRole("tablist", { name: "Platform" });
  const linux = page.getByRole("tabpanel", { name: "Linux" });
  await tabs.getByRole("tab", { name: "Linux" }).click();
  await linux.getByText("Install from the terminal").press("Enter");
  await expect(linux.getByRole("button", { name: "Copy" })).toBeVisible();

  const mac = tabs.getByRole("tab", { name: "macOS" });
  await mac.click();
  await expect(
    page
      .getByRole("tabpanel", { name: "macOS" })
      .getByRole("button", { name: "Copy" }),
  ).toBeVisible();

  await mac.press("ArrowLeft");
  const windows = page.getByRole("tabpanel", { name: "Windows" });
  await expect(windows.locator("code")).toBeVisible();
  await expect(windows.locator("code")).toHaveText(
    "irm https://jamhour1g.github.io/Azkar-App/install.ps1 | iex",
  );
  await windows.getByText("Install from the terminal").click();
  await tabs.getByRole("tab", { name: "Linux" }).click();
  await expect(linux.getByRole("button", { name: "Copy" })).toBeHidden();
});

test("offers direct preview downloads when Stable is absent", async ({
  page,
}) => {
  const tag = "v1.2.4-beta.2";
  const devRelease = `${releasesPage}/tag/${tag}`;
  const download = (suffix: string) =>
    `${releasesPage}/download/${tag}/azkar-${tag.slice(1)}-${suffix}`;
  const installers = [
    ["Windows", "windows", "windows-x64-nsis.exe"],
    ["macOS", "macos-arm64", "macos-arm64.dmg"],
    ["macOS", "macos-x64", "macos-x64.dmg"],
    ["Linux", "deb", "linux-x64.deb"],
    ["Linux", "rpm", "linux-x64.rpm"],
    ["Linux", "appimage", "linux-x64.AppImage"],
  ] as const;
  await page.route(releasesApi, (route) =>
    route.fulfill({
      json: [
        {
          draft: false,
          prerelease: true,
          tag_name: tag,
          published_at: "2026-11-24T12:00:00Z",
          html_url: devRelease,
          assets: installers.map(([, , suffix]) => ({
            name: `azkar-${tag.slice(1)}-${suffix}`,
            browser_download_url: download(suffix),
          })),
        },
      ],
    }),
  );

  await page.goto("/download.html");

  await expect(page.locator("#release-status")).toContainText(
    `Azkar ${tag} is available as a preview.`,
  );
  await expect(
    page.getByRole("link", { name: "Release notes" }),
  ).toHaveAttribute("href", devRelease);
  for (const [platform, id, suffix] of installers) {
    await page.getByRole("tab", { name: platform, exact: true }).click();
    const panel = page.getByRole("tabpanel", { name: platform });
    const link = panel.locator(`#${id}-download`);
    await expect(link).toBeVisible();
    await expect(link).toHaveAttribute("href", download(suffix));
  }

  const linux = page.getByRole("tabpanel", { name: "Linux" });
  await expect(linux.getByRole("button", { name: "Copy" })).toBeHidden();
  await linux.getByText("Install from the terminal").click();
  await expect(
    linux.getByText("Run this command and choose Dev", { exact: false }),
  ).toBeVisible();
  await expect(linux.locator("code")).toHaveText(
    "curl -fsSL https://jamhour1g.github.io/Azkar-App/install.sh | sh",
  );
  await page.context().grantPermissions(["clipboard-read", "clipboard-write"]);
  await linux.getByRole("button", { name: "Copy" }).click();
  await expect(linux.getByRole("button", { name: "Copied" })).toBeVisible();
  expect(await page.evaluate(() => navigator.clipboard.readText())).toBe(
    "curl -fsSL https://jamhour1g.github.io/Azkar-App/install.sh | sh",
  );
});

test("explains when there are no published builds", async ({ page }) => {
  await page.route(releasesApi, (route) => route.fulfill({ json: [] }));
  await page.goto("/download.html");
  await expect(page.locator("#release-status")).toContainText(
    "Azkar isn’t available to download yet.",
  );
  await expect(page.getByRole("tablist", { name: "Platform" })).toHaveCount(0);
});

test("copies the screenshot generation command from the warning", async ({
  page,
}) => {
  await page.route(releasesApi, (route) => route.fulfill({ json: [] }));
  await page.route("**/images/screen-*.webp", (route) =>
    route.fulfill({ status: 404 }),
  );
  await page.context().grantPermissions(["clipboard-read", "clipboard-write"]);
  await page.goto("/download.html");

  const warning = page.locator("#screenshot-warning");
  const command = "./gradlew :app:captureDownloadScreenshots";
  await expect(warning).toBeVisible();
  const message = warning.locator("p");
  await expect(message.locator("code")).toHaveText(command);
  await message.getByRole("button", { name: "Copy" }).press("Enter");
  await expect(warning.getByRole("button", { name: "Copied" })).toBeVisible();
  expect(await page.evaluate(() => navigator.clipboard.readText())).toBe(
    command,
  );
});
