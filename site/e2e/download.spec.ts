import { expect, test } from "@playwright/test";

const releasesApi =
  "https://api.github.com/repos/jamhour1g/Azkar-App/releases**";
const releasesPage = "https://github.com/jamhour1g/Azkar-App/releases";
const windowsInstaller = `${releasesPage}/download/v1.2.3/azkar-1.2.3-windows-x64-nsis.exe`;

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

  await expect(
    page.getByText("Could not check for stable installers."),
  ).toBeVisible();
  await expect(
    page.getByRole("link", { name: "Browse all releases" }),
  ).toHaveAttribute("href", releasesPage);
});
