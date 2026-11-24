import AxeBuilder from "@axe-core/playwright";
import { expect, test } from "@playwright/test";

for (const colorScheme of ["light", "dark"] as const) {
  test(`download page accessibility in ${colorScheme} theme`, async ({
    page,
  }) => {
    await page.emulateMedia({ colorScheme, reducedMotion: "reduce" });
    await page.route(
      "https://api.github.com/repos/jamhour1g/Azkar-App/releases**",
      (route) => route.fulfill({ json: [] }),
    );
    await page.goto("/download.html");
    await expect(
      page.getByText("No stable release is available yet."),
    ).toBeVisible();

    const results = await new AxeBuilder({ page })
      .withTags(["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"])
      .analyze();
    expect(results.violations).toEqual([]);
  });
}
