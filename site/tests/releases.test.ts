import { expect, test } from "bun:test";
import {
  findInstaller,
  selectStableRelease,
} from "../src/download/releases.js";

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
    selectStableRelease([
      { ...release, tag_name: "v1.2.5", draft: true },
      { ...release, tag_name: "v1.2.4-beta.1", prerelease: true },
      release,
      { ...release, tag_name: "v1.2.2" },
    ]),
  ).toEqual({
    tag_name: release.tag_name,
    html_url: release.html_url,
    assets: release.assets,
  });
});

test("finds the installer for the requested architecture", () => {
  expect(findInstaller(release.assets, "-macos-arm64.dmg")).toEqual(
    release.assets[1],
  );
  expect(findInstaller(release.assets, "-macos-x64.dmg")).toBeUndefined();
});

test("retains download fallbacks when release data is missing", () => {
  expect(selectStableRelease([])).toBeUndefined();
  expect(selectStableRelease([{ ...release, assets: null }])?.assets).toEqual(
    [],
  );
  expect(
    selectStableRelease([
      { ...release, assets: [null, { name: "broken" }, ...release.assets] },
    ])?.assets,
  ).toEqual(release.assets);
  expect(() => selectStableRelease({ message: "Rate limit exceeded" })).toThrow(
    TypeError,
  );
});
