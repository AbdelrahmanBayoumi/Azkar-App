import { expect, test } from "vitest";
import {
  findInstaller,
  selectDownloadRelease,
} from "../../src/download/releases.js";

const release = {
  tag_name: "v1.2.3",
  draft: false,
  prerelease: false,
  html_url: "https://github.com/jamhour1g/Azkar-App/releases/tag/v1.2.3",
  assets: [
    {
      name: "azkar-1.2.3-windows-x64-nsis.exe",
      browser_download_url: "https://example.test/windows.exe",
    },
    {
      name: "azkar-1.2.3-macos-arm64.dmg",
      browser_download_url: "https://example.test/arm64.dmg",
    },
  ],
};

test("uses the first published stable release", () => {
  expect(
    selectDownloadRelease([
      { ...release, tag_name: "v1.2.5", draft: true },
      {
        ...release,
        tag_name: "v1.2.4-beta.1",
        prerelease: true,
        published_at: "2026-11-24T12:00:00Z",
      },
      release,
      { ...release, tag_name: "v1.2.2" },
    ]),
  ).toEqual({
    channel: "stable",
    release: {
      tag_name: release.tag_name,
      html_url: release.html_url,
      assets: release.assets,
    },
  });
});

test("uses the latest published Dev build when Stable is absent", () => {
  const dev = {
    ...release,
    tag_name: "v1.2.4-beta.2",
    prerelease: true,
    published_at: "2026-11-24T12:00:00Z",
  };
  expect(
    selectDownloadRelease([
      {
        ...dev,
        tag_name: "v1.2.4-beta.1",
        published_at: "2026-11-23T12:00:00Z",
      },
      { ...dev, tag_name: "v1.2.4-beta.3", draft: true },
      { ...dev, tag_name: "v1.2.4-rc.1" },
      dev,
    ]),
  ).toMatchObject({ channel: "dev", release: { tag_name: dev.tag_name } });
});

test("finds the installer for the requested architecture", () => {
  expect(findInstaller(release.assets, "-macos-arm64.dmg")).toEqual(
    release.assets[1],
  );
  expect(findInstaller(release.assets, "-macos-x64.dmg")).toBeUndefined();
});

test("retains download fallbacks when release data is missing", () => {
  expect(selectDownloadRelease([])).toBeUndefined();
  expect(
    selectDownloadRelease([{ ...release, assets: null }])?.release.assets,
  ).toEqual([]);
  expect(
    selectDownloadRelease([
      { ...release, assets: [null, { name: "broken" }, ...release.assets] },
    ])?.release.assets,
  ).toEqual(release.assets);
  expect(() =>
    selectDownloadRelease({ message: "Rate limit exceeded" }),
  ).toThrow(TypeError);
});
