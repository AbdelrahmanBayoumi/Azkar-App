import { Tabs } from "@base-ui/react/tabs";
import { useEffect, useRef, useState } from "react";
import {
  findInstaller,
  selectDownloadRelease,
  type Release,
} from "./releases.js";
import {
  detectPlatform,
  platforms,
  type Installer,
  type Platform,
} from "./platforms.js";
import { TerminalCommand } from "./terminal-command.js";
import { Icon } from "./ui.js";

const releasesPage = "https://github.com/jamhour1g/Azkar-App/releases";
const releasesApi =
  "https://api.github.com/repos/jamhour1g/Azkar-App/releases?per_page=100";

type ReleaseResult =
  | { status: "loading" | "error" | "missing" }
  | { status: "stable" | "dev"; release: Release };

interface ReleaseStatus {
  message: string;
  href?: string;
  linkLabel?: string;
}

function releaseStatus(result: ReleaseResult): ReleaseStatus {
  switch (result.status) {
    case "loading":
      return { message: "Checking for releases…" };
    case "error":
      return {
        message: "Could not check for releases.",
        href: releasesPage,
      };
    case "missing":
      return { message: "Azkar isn’t available to download yet." };
    case "dev":
      return {
        message: `Azkar ${result.release.tag_name} is available as a preview. The first stable version is still to come.`,
        href:
          result.release.html_url ||
          `${releasesPage}/tag/${result.release.tag_name}`,
        linkLabel: "Release notes",
      };
    case "stable": {
      const { release } = result;
      const incomplete = platforms.some(({ installers }) =>
        installers.some(({ suffix }) => !findInstaller(release.assets, suffix)),
      );
      if (incomplete) {
        return {
          message: `Stable release ${release.tag_name} is missing some installers.`,
          href: release.html_url || releasesPage,
          linkLabel: "View release assets",
        };
      }
      return {
        message: `Stable release ${release.tag_name} is ready. Choose an installer below.`,
      };
    }
  }
}

function InstallerLink({
  installer,
  release,
  primary,
  fallbackHref = releasesPage,
}: {
  installer: Installer;
  release: Release | undefined;
  primary: boolean;
  fallbackHref?: string;
}) {
  const asset = release && findInstaller(release.assets, installer.suffix);
  const label = asset ? installer.label : installer.fallback;
  const className = `platform-link${asset ? "" : " is-release-link"}${primary ? " is-primary" : ""}`;
  return (
    <a
      className={className}
      id={`${installer.id}-download`}
      href={asset?.browser_download_url ?? fallbackHref}
      data-asset={installer.id}
      aria-description={
        asset && release ? `Version ${release.tag_name}` : undefined
      }
    >
      <Icon name={asset ? "download" : "download-release"} />
      <span className="installer-copy">
        <span className="download-label">{label}</span>
        <span className="installer-hint">{installer.hint}</span>
      </span>
    </a>
  );
}

function TerminalInstall({
  platform,
  devOnly,
  open,
  onOpenChange,
}: {
  platform: Platform["id"];
  devOnly: boolean;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const site = "https://jamhour1g.github.io/Azkar-App";
  const command =
    platform === "windows"
      ? `irm ${site}/install.ps1 | iex`
      : `curl -fsSL ${site}/install.sh | sh`;

  return (
    <details
      className="terminal-install"
      open={open}
      onToggle={(event) => {
        const nextOpen = event.currentTarget.open;
        if (nextOpen !== open) onOpenChange(nextOpen);
      }}
    >
      <summary>Install from the terminal</summary>
      <div className="detail-body">
        <p>
          {devOnly
            ? "Run this command and choose Dev when prompted."
            : "Choose Stable, Dev, or Cancel when prompted."}
          {platform === "linux" &&
            " Sets up APT on Debian and Ubuntu or DNF on Fedora for future updates. Other x86-64 distributions get an AppImage."}
        </p>
        <TerminalCommand
          command={command}
          label={platform === "windows" ? "PowerShell" : "Shell"}
        />
      </div>
    </details>
  );
}

export function Installers() {
  const [selected, setSelected] = useState<Platform["id"]>("linux");
  const [terminalOpen, setTerminalOpen] = useState(false);
  const platformDetected = useRef(false);
  const [result, setResult] = useState<ReleaseResult>({ status: "loading" });
  const release = "release" in result ? result.release : undefined;
  const devOnly = result.status === "dev";
  const status = releaseStatus(result);

  useEffect(() => {
    // Fast Refresh reruns effects while preserving the selected tab.
    if (!platformDetected.current) {
      platformDetected.current = true;
      const visitor: Navigator & { userAgentData?: { platform?: string } } =
        navigator;
      const platform = [
        visitor.userAgentData?.platform,
        visitor.platform,
        visitor.userAgent,
      ]
        .filter(Boolean)
        .join(" ")
        .toLowerCase();
      setSelected(detectPlatform(platform));
    }

    const request = new AbortController();
    async function loadRelease(): Promise<void> {
      try {
        const response = await fetch(releasesApi, {
          headers: { Accept: "application/vnd.github+json" },
          signal: request.signal,
        });
        if (!response.ok) throw new Error(`GitHub returned ${response.status}`);
        const payload: unknown = await response.json();
        const available = selectDownloadRelease(payload);
        if (request.signal.aborted) return;
        setResult(
          available
            ? { status: available.channel, release: available.release }
            : { status: "missing" },
        );
      } catch {
        if (!request.signal.aborted) setResult({ status: "error" });
      }
    }
    void loadRelease();
    return () => request.abort();
  }, []);

  return (
    <Tabs.Root
      className="download-options"
      id="downloads"
      value={selected}
      onValueChange={setSelected}
    >
      <h2 className="visually-hidden">Choose your platform</h2>
      <p
        className="release-status"
        id="release-status"
        role="status"
        aria-live="polite"
      >
        <Icon name="info" />
        <span className="release-status-message" id="release-status-message">
          {status.message}
        </span>
        <a
          id="release-overview"
          href={status.href ?? releasesPage}
          hidden={!status.href}
        >
          {status.linkLabel ?? "Browse all releases"}
        </a>
      </p>
      {result.status !== "missing" && (
        <>
          <Tabs.List
            className="platform-tabs"
            aria-label="Platform"
            activateOnFocus
          >
            {platforms.map(({ id, label }) => (
              <Tabs.Tab
                key={id}
                className="platform-tab"
                id={`tab-${id}`}
                value={id}
                aria-controls={`panel-${id}`}
              >
                {label}
              </Tabs.Tab>
            ))}
          </Tabs.List>
          <div className="platform-list">
            {platforms.map(({ id, label, detail, note, icon, installers }) => (
              <Tabs.Panel
                className={
                  id === "linux" ? "platform platform-linux" : "platform"
                }
                id={`panel-${id}`}
                key={id}
                value={id}
                keepMounted
                aria-labelledby={`tab-${id}`}
              >
                <h3 className="platform-name">
                  <Icon name={icon} className="platform-icon" />
                  {label}
                </h3>
                <p className="platform-detail">{detail}</p>
                <div className="platform-actions">
                  {installers.map((installer, index) => (
                    <InstallerLink
                      key={installer.id}
                      installer={installer}
                      release={release}
                      primary={index === 0}
                      fallbackHref={devOnly ? status.href : undefined}
                    />
                  ))}
                </div>
                <p className="platform-note">
                  {note}
                  {id === "linux" && (
                    <span
                      className="platform-version"
                      id="linux-version-line"
                      hidden={result.status !== "stable"}
                    >
                      Latest stable:{" "}
                      <span id="linux-version">{release?.tag_name}</span>
                    </span>
                  )}
                </p>
                <TerminalInstall
                  platform={id}
                  devOnly={devOnly}
                  open={terminalOpen}
                  onOpenChange={setTerminalOpen}
                />
              </Tabs.Panel>
            ))}
          </div>
        </>
      )}
    </Tabs.Root>
  );
}
