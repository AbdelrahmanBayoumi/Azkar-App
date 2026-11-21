// Theme controls

const themeToggle = document.getElementById("theme-toggle");
const themeIcon = document.getElementById("theme-icon");
const themeMeta = document.querySelector('meta[name="theme-color"]');
const systemDark = window.matchMedia("(prefers-color-scheme: dark)");
const desktopHero = window.matchMedia("(min-width: 1100px)");

function activeTheme() {
  if (document.documentElement.dataset.theme !== "system")
    return document.documentElement.dataset.theme;

  return systemDark.matches ? "dark" : "light";
}

function updateThemeControl() {
  const dark = activeTheme() === "dark";
  const nextTheme = dark ? "light" : "dark";

  themeIcon.innerHTML = dark
    ? '<circle cx="12" cy="12" r="4"/><path d="M12 2v2m0 16v2M4.93 4.93l1.42 1.42m11.3 11.3 1.42 1.42M2 12h2m16 0h2M4.93 19.07l1.42-1.42m11.3-11.3 1.42-1.42"/>'
    : '<path d="M20.2 15.3A8.5 8.5 0 0 1 8.7 3.8 8.5 8.5 0 1 0 20.2 15.3Z"/>';
  themeToggle.setAttribute("aria-label", `Switch to ${nextTheme} theme`);
  themeToggle.title = `Switch to ${nextTheme} theme`;

  themeMeta.content = dark ? "#0B1220" : "#F8FAFC";

  updateScreenshotImages(dark);
}

function updateScreenshotImages(dark = activeTheme() === "dark") {
  document
    .querySelectorAll("img[data-light-src][data-dark-src]")
    .forEach((image) => {
      if (image.closest(".screenshot-card")?.hidden) return;
      if (image.closest(".hero-preview") && !desktopHero.matches) return;

      const source = dark ? image.dataset.darkSrc : image.dataset.lightSrc;
      if (image.getAttribute("src") !== source) image.src = source;
    });
}

desktopHero.addEventListener("change", () => updateScreenshotImages());

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

// Screenshot gallery

const screenTabs = Array.from(document.querySelectorAll(".screen-tab"));
const screenTabList = document.querySelector(".screen-tabs");
const screenPanels = screenTabs.map((tab) =>
  document.getElementById(tab.getAttribute("aria-controls")),
);
const screenshotWarning = document.getElementById("screenshot-warning");
const screenRotationDuration = 6500;
const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)");
let screenRotationTimer;
let screenRotationPaused = false;
let screenProgressFrame;

document
  .querySelectorAll("img[data-light-src][data-dark-src]")
  .forEach((image) => {
    const warnAboutMissingScreenshot = () => {
      screenshotWarning.hidden = false;
    };

    if (
      image.hasAttribute("src") &&
      image.complete &&
      image.naturalWidth === 0
    ) {
      warnAboutMissingScreenshot();
    }

    image.addEventListener("error", warnAboutMissingScreenshot, {
      once: true,
    });
  });

function restartScreenProgress(index) {
  window.cancelAnimationFrame(screenProgressFrame);
  screenProgressFrame = undefined;
  screenTabs.forEach((tab) => tab.classList.remove("is-counting"));
  if (reduceMotion.matches || screenRotationPaused || document.hidden) return;
  screenProgressFrame = window.requestAnimationFrame(() => {
    screenTabs[index].classList.add("is-counting");
    screenProgressFrame = undefined;
  });
}

function scheduleScreenRotation() {
  window.clearTimeout(screenRotationTimer);
  if (reduceMotion.matches || screenRotationPaused || document.hidden) return;
  screenRotationTimer = window.setTimeout(() => {
    const activeIndex = screenTabs.findIndex(
      (tab) => tab.getAttribute("aria-selected") === "true",
    );
    showScreen(activeIndex + 1);
  }, screenRotationDuration);
}

function setScreenRotationPaused(paused) {
  if (screenRotationPaused === paused) return;
  screenRotationPaused = paused;
  window.clearTimeout(screenRotationTimer);
  screenTabList.classList.toggle("is-paused", paused);
  if (!paused) {
    const activeIndex = screenTabs.findIndex(
      (tab) => tab.getAttribute("aria-selected") === "true",
    );
    restartScreenProgress(activeIndex);
    scheduleScreenRotation();
  }
}

function syncScreenRotationPause() {
  setScreenRotationPaused(
    screenTabs.some((tab) => tab.matches(":focus-visible")),
  );
}

function showScreen(index, moveFocus = false) {
  const selectedIndex = (index + screenTabs.length) % screenTabs.length;
  screenTabs.forEach((tab, tabIndex) => {
    const selected = tabIndex === selectedIndex;
    tab.setAttribute("aria-selected", String(selected));
    tab.tabIndex = selected ? 0 : -1;
    screenPanels[tabIndex].hidden = !selected;
  });

  updateScreenshotImages();
  restartScreenProgress(selectedIndex);
  scheduleScreenRotation();

  if (moveFocus) screenTabs[selectedIndex].focus();
}

screenTabList.addEventListener("focusin", syncScreenRotationPause);
screenTabList.addEventListener("focusout", (event) => {
  if (!screenTabList.contains(event.relatedTarget)) syncScreenRotationPause();
});
document.addEventListener("visibilitychange", () => {
  if (document.hidden) {
    window.clearTimeout(screenRotationTimer);
    screenTabList.classList.add("is-paused");
  } else {
    screenTabList.classList.toggle("is-paused", screenRotationPaused);
    const activeIndex = screenTabs.findIndex(
      (tab) => tab.getAttribute("aria-selected") === "true",
    );
    restartScreenProgress(activeIndex);
    scheduleScreenRotation();
  }
});
reduceMotion.addEventListener("change", () => {
  if (reduceMotion.matches) {
    window.clearTimeout(screenRotationTimer);
    screenTabs.forEach((tab) => tab.classList.remove("is-counting"));
  } else {
    const activeIndex = screenTabs.findIndex(
      (tab) => tab.getAttribute("aria-selected") === "true",
    );
    restartScreenProgress(activeIndex);
    scheduleScreenRotation();
  }
});

screenTabs.forEach((tab, index) => {
  tab.addEventListener("click", () => showScreen(index));
  tab.addEventListener("keydown", (event) => {
    let nextIndex;
    if (event.key === "ArrowRight") nextIndex = index + 1;
    else if (event.key === "ArrowLeft") nextIndex = index - 1;
    else if (event.key === "Home") nextIndex = 0;
    else if (event.key === "End") nextIndex = screenTabs.length - 1;
    else return;
    event.preventDefault();
    showScreen(nextIndex, true);
  });
});

showScreen(
  screenTabs.findIndex((tab) => tab.getAttribute("aria-selected") === "true"),
);

// Platform installer tabs
const platformTabs = Array.from(document.querySelectorAll(".platform-tab"));
const platformPanels = platformTabs.map((tab) =>
  document.getElementById(tab.getAttribute("aria-controls")),
);

function showPlatform(index, moveFocus = false) {
  const selectedIndex = (index + platformTabs.length) % platformTabs.length;
  platformTabs.forEach((tab, tabIndex) => {
    const selected = tabIndex === selectedIndex;
    tab.setAttribute("aria-selected", String(selected));
    tab.tabIndex = selected ? 0 : -1;
    platformPanels[tabIndex].hidden = !selected;
  });
  if (moveFocus) platformTabs[selectedIndex].focus();
}

platformTabs.forEach((tab, index) => {
  tab.addEventListener("click", () => showPlatform(index));
  tab.addEventListener("keydown", (event) => {
    let nextIndex;
    if (event.key === "ArrowRight") nextIndex = index + 1;
    else if (event.key === "ArrowLeft") nextIndex = index - 1;
    else if (event.key === "Home") nextIndex = 0;
    else if (event.key === "End") nextIndex = platformTabs.length - 1;
    else return;
    event.preventDefault();
    showPlatform(nextIndex, true);
  });
});

/** @type {Navigator & {userAgentData?: {platform?: string}}} */
const visitorNavigator = navigator;
const visitorPlatform = [
  visitorNavigator.userAgentData?.platform,
  visitorNavigator.platform,
  visitorNavigator.userAgent,
]
  .filter(Boolean)
  .join(" ")
  .toLowerCase();
const isLinuxHost =
  /linux/.test(visitorPlatform) && !/android/.test(visitorPlatform);
showPlatform(
  /win/.test(visitorPlatform)
    ? 0
    : /mac|iphone|ipad/.test(visitorPlatform)
      ? 1
      : 2,
);
document.getElementById("linux-repositories").hidden = !isLinuxHost;

// Feature category tabs

const featureTabs = Array.from(
  document.querySelectorAll('[role="tab"][aria-controls^="panel-feature-"]'),
);
const featurePanels = featureTabs.map((tab) =>
  document.getElementById(tab.getAttribute("aria-controls")),
);

function activateFeatureTab(index, moveFocus = false) {
  const selectedIndex = (index + featureTabs.length) % featureTabs.length;
  featureTabs.forEach((tab, tabIndex) => {
    const selected = tabIndex === selectedIndex;
    tab.setAttribute("aria-selected", String(selected));
    tab.tabIndex = selected ? 0 : -1;
    featurePanels[tabIndex].hidden = !selected;
  });

  if (moveFocus) featureTabs[selectedIndex].focus();
}

featureTabs.forEach((tab, index) => {
  tab.addEventListener("click", () => activateFeatureTab(index));
  tab.addEventListener("keydown", (event) => {
    let nextIndex;
    if (event.key === "ArrowRight") nextIndex = index + 1;
    else if (event.key === "ArrowLeft") nextIndex = index - 1;
    else if (event.key === "Home") nextIndex = 0;
    else if (event.key === "End") nextIndex = featureTabs.length - 1;
    else return;
    event.preventDefault();
    activateFeatureTab(nextIndex, true);
  });
});

// Stable release and installer links

const releaseListUrl =
  "https://api.github.com/repos/jamhour1g/Azkar-App/releases?per_page=100";
const releasePageUrl = "https://github.com/jamhour1g/Azkar-App/releases";
const statusMessage = document.getElementById("release-status-message");
const releaseOverview = document.getElementById("release-overview");
const linuxVersion = document.getElementById("linux-version");
const linuxVersionLine = document.getElementById("linux-version-line");

function setReleaseStatus(
  message,
  {
    href = releasePageUrl,
    linkLabel = "Browse all releases",
    showLink = false,
  } = {},
) {
  statusMessage.textContent = message;

  releaseOverview.href = href;
  releaseOverview.textContent = linkLabel;
  releaseOverview.hidden = !showLink;
}

function pointToAsset(element, assets, suffix, label, accessibleLabel) {
  const asset = assets.find(({ name }) => name.endsWith(suffix));

  if (!asset) return false;

  element.href = asset.browser_download_url;
  element.querySelector(".download-label").textContent = label;
  element.querySelector("svg").innerHTML =
    '<path d="M12 3v12m-5-5 5 5 5-5M5 17v3h14v-3"/>';
  element.classList.remove("is-release-link");
  element.setAttribute("aria-label", accessibleLabel);

  return true;
}

async function connectStableDownloads() {
  try {
    const response = await fetch(releaseListUrl, {
      headers: { Accept: "application/vnd.github+json" },
    });
    if (!response.ok) {
      setReleaseStatus("Could not check for stable installers.", { showLink: true });
      return;
    }

    const releases = await response.json();
    const release = releases.find((item) => !item.draft && !item.prerelease);

    if (!release) {
      setReleaseStatus("No stable release is available yet.", {
        showLink: true,
      });
      return;
    }

    const assets = release.assets || [];
    const version = release.tag_name;
    linuxVersion.textContent = version;
    linuxVersionLine.hidden = false;
    const found = [
      pointToAsset(
        document.getElementById("windows-download"),
        assets,
        "-windows-x64-nsis.exe",
        "Download Windows",
        `Download Windows .exe installer, version ${version}`,
      ),
      pointToAsset(
        document.getElementById("macos-arm64-download"),
        assets,
        "-macos-arm64.dmg",
        "Download Apple silicon",
        `Download macOS installer for Apple silicon, version ${version}`,
      ),
      pointToAsset(
        document.getElementById("macos-x64-download"),
        assets,
        "-macos-x64.dmg",
        "Download Intel",
        `Download macOS installer for Intel, version ${version}`,
      ),
      pointToAsset(
        document.getElementById("deb-download"),
        assets,
        "-linux-x64.deb",
        "DEB",
        `Download DEB package, version ${version}`,
      ),
      pointToAsset(
        document.getElementById("rpm-download"),
        assets,
        "-linux-x64.rpm",
        "RPM",
        `Download RPM package, version ${version}`,
      ),
      pointToAsset(
        document.getElementById("appimage-download"),
        assets,
        "-linux-x64.AppImage",
        "AppImage",
        `Download AppImage, version ${version}`,
      ),
    ];
    const missing = found.some((available) => !available);

    if (missing) {
      setReleaseStatus(
        `Stable release ${release.tag_name} is missing some installers.`,
        {
          href: release.html_url || releasePageUrl,
          linkLabel: "View release assets",
          showLink: true,
        },
      );
    } else {
      setReleaseStatus(
        `Stable release ${release.tag_name} is ready. Choose an installer below.`,
      );
    }
  } catch {
    setReleaseStatus("Could not check for stable installers.", {
      showLink: true,
    });
  }
}

void connectStableDownloads();

// Linux repository command switcher
const repositoryTabs = Array.from(document.querySelectorAll(".repository-tab"));
const repositoryCopy = document.getElementById("repository-copy");
repositoryTabs.forEach((tab) => {
  tab.addEventListener("click", () => {
    repositoryCopy.dataset.copy = `${tab.dataset.repository}-commands`;
    repositoryCopy.textContent = "Copy";
    repositoryTabs.forEach((candidate) => {
      const selected = candidate === tab;
      candidate.setAttribute("aria-pressed", String(selected));
      document.querySelector(
        `[data-repository-panel="${candidate.dataset.repository}"]`,
      ).hidden = !selected;
    });
  });
});

// Copyable Linux repository commands

document.querySelectorAll("[data-copy]").forEach((button) => {
  button.addEventListener("click", async () => {
    const code = document
      .getElementById(button.dataset.copy)
      .querySelector("code").textContent;

    try {
      await navigator.clipboard.writeText(code);
      button.textContent = "Copied";
      window.setTimeout(() => {
        button.textContent = "Copy";
      }, 1800);
    } catch {
      const selection = window.getSelection();
      const range = document.createRange();
      range.selectNodeContents(
        document.getElementById(button.dataset.copy).querySelector("code"),
      );
      selection.removeAllRanges();
      selection.addRange(range);
      button.textContent = "Copy manually";
    }
  });
});
