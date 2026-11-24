import { activeTheme } from "./theme.js";
import { tabIndexForKey } from "./ui.js";

function requireElement<T extends Element>(
  selector: string,
  elementType: abstract new () => T,
): T {
  const element = document.querySelector(selector);
  if (!(element instanceof elementType))
    throw new Error(`Missing download page element: ${selector}`);
  return element;
}

function controlledPanel(tab: HTMLElement): HTMLElement {
  const id = tab.getAttribute("aria-controls");
  if (!id) throw new Error("Missing aria-controls on download page tab");
  return requireElement(`#${CSS.escape(id)}`, HTMLElement);
}

const themeToggle = requireElement("#theme-toggle", HTMLButtonElement);
const themeIcon = requireElement("#theme-icon use", SVGUseElement);
const themeMeta = requireElement('meta[name="theme-color"]', HTMLMetaElement);
const systemDark = window.matchMedia("(prefers-color-scheme: dark)");
const desktopHero = window.matchMedia("(min-width: 1100px)");

function updateThemeControl() {
  const dark = activeTheme() === "dark";
  const nextTheme = dark ? "light" : "dark";

  themeIcon.setAttribute(
    "href",
    `images/icons/${dark ? "sun" : "moon"}.svg#icon`,
  );
  themeToggle.setAttribute("aria-label", `Switch to ${nextTheme} theme`);
  themeToggle.title = `Switch to ${nextTheme} theme`;

  themeMeta.content = dark ? "#0B1220" : "#F8FAFC";

  updateHeroImage(dark);
}

function updateHeroImage(dark = activeTheme() === "dark") {
  const image = document.querySelector<HTMLImageElement>(".hero-preview img");
  if (!image || !desktopHero.matches) return;
  const source = dark ? image.dataset.darkSrc : image.dataset.lightSrc;
  if (source && image.getAttribute("src") !== source) image.src = source;
}

desktopHero.addEventListener("change", () => updateHeroImage());

themeToggle.addEventListener("click", () => {
  document.documentElement.dataset.theme =
    activeTheme() === "dark" ? "light" : "dark";
  try {
    localStorage.setItem(
      "azkar-download-theme",
      document.documentElement.dataset.theme,
    );
  } catch {
    // Theme switching still works for this page view without browser storage.
  }

  updateThemeControl();
});

systemDark.addEventListener("change", () => {
  if (document.documentElement.dataset.theme === "system") updateThemeControl();
});
updateThemeControl();

const featureTabs = Array.from(
  document.querySelectorAll<HTMLButtonElement>(
    '[role="tab"][aria-controls^="panel-feature-"]',
  ),
);
const features = featureTabs.map((tab) => ({
  tab,
  panel: controlledPanel(tab),
}));

function activateFeatureTab(index: number, moveFocus = false) {
  features.forEach(({ tab, panel }, tabIndex) => {
    const selected = tabIndex === index;
    tab.setAttribute("aria-selected", String(selected));
    tab.tabIndex = selected ? 0 : -1;
    panel.hidden = !selected;
  });

  if (moveFocus) features[index]?.tab.focus();
}

featureTabs.forEach((tab, index) => {
  tab.addEventListener("click", () => activateFeatureTab(index));
  tab.addEventListener("keydown", (event) => {
    const nextIndex = tabIndexForKey(event.key, index, featureTabs.length);
    if (nextIndex === undefined) return;
    event.preventDefault();
    activateFeatureTab(nextIndex, true);
  });
});
