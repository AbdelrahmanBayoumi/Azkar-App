import type { IconName } from "./ui.js";

type PlatformId = "windows" | "macos" | "linux";

export interface Installer {
  id: string;
  suffix: string;
  label: string;
  fallback: string;
  hint: string;
}

export interface Platform {
  id: PlatformId;
  label: string;
  detail: string;
  note: string;
  icon: IconName;
  installers: readonly Installer[];
}

export const platforms = [
  {
    id: "windows",
    label: "Windows",
    detail: "Check for updates in the app.",
    note: "Run the downloaded installer to get started.",
    icon: "windows",
    installers: [
      {
        id: "windows",
        suffix: "-windows-x64-nsis.exe",
        label: "Download Windows",
        fallback: "Windows releases",
        hint: ".exe · 64-bit",
      },
    ],
  },
  {
    id: "macos",
    label: "macOS",
    detail: "Choose the chip used by your Mac.",
    note: "Find your chip in the Apple menu’s About This Mac.",
    icon: "macos",
    installers: [
      {
        id: "macos-arm64",
        suffix: "-macos-arm64.dmg",
        label: "Download Apple silicon",
        fallback: "Apple silicon releases",
        hint: ".dmg · M-series Macs",
      },
      {
        id: "macos-x64",
        suffix: "-macos-x64.dmg",
        label: "Download Intel",
        fallback: "Intel releases",
        hint: ".dmg · Intel Macs",
      },
    ],
  },
  {
    id: "linux",
    label: "Linux",
    detail: "Choose your distribution’s package.",
    note: "Open the DEB or RPM in your software installer.",
    icon: "linux",
    installers: [
      {
        id: "deb",
        suffix: "-linux-x64.deb",
        label: "DEB",
        fallback: "DEB",
        hint: "Debian & Ubuntu",
      },
      {
        id: "rpm",
        suffix: "-linux-x64.rpm",
        label: "RPM",
        fallback: "RPM",
        hint: "Fedora",
      },
      {
        id: "appimage",
        suffix: "-linux-x64.AppImage",
        label: "AppImage",
        fallback: "AppImage",
        hint: "Other distributions",
      },
    ],
  },
] satisfies readonly Platform[];

export function detectPlatform(platform: string): PlatformId {
  const name = platform.toLowerCase();
  if (/mac|darwin|iphone|ipad/.test(name)) return "macos";
  if (/windows|win32|win64/.test(name)) return "windows";
  return "linux";
}
