export interface ReleaseAsset {
  name: string;
  browser_download_url: string;
}

export interface Release {
  tag_name: string;
  html_url: string | undefined;
  assets: ReleaseAsset[];
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function isReleaseAsset(value: unknown): value is ReleaseAsset {
  return (
    isRecord(value) &&
    typeof value.name === "string" &&
    typeof value.browser_download_url === "string"
  );
}

export function selectDownloadRelease(
  value: unknown,
): { channel: "stable" | "dev"; release: Release } | undefined {
  if (!Array.isArray(value))
    throw new TypeError("Expected a GitHub release list");

  let dev: Release | undefined;
  let latestDevDate = -Infinity;
  for (const release of value as unknown[]) {
    if (
      !isRecord(release) ||
      release.draft !== false ||
      typeof release.tag_name !== "string"
    )
      continue;

    if (release.prerelease === false) {
      return {
        channel: "stable",
        release: parseRelease(release.tag_name, release),
      };
    }
    if (
      release.prerelease !== true ||
      !/^v\d+\.\d+\.\d+-beta\.\d+(\.\d+)*$/.test(release.tag_name)
    )
      continue;
    const publishedAt =
      typeof release.published_at === "string"
        ? Date.parse(release.published_at)
        : NaN;
    if (publishedAt > latestDevDate) {
      dev = parseRelease(release.tag_name, release);
      latestDevDate = publishedAt;
    }
  }
  return dev && { channel: "dev", release: dev };
}

function parseRelease(tag: string, release: Record<string, unknown>): Release {
  return {
    tag_name: tag,
    html_url:
      typeof release.html_url === "string" ? release.html_url : undefined,
    assets: Array.isArray(release.assets)
      ? release.assets.filter(isReleaseAsset)
      : [],
  };
}

export function findInstaller(
  assets: ReleaseAsset[],
  suffix: string,
): ReleaseAsset | undefined {
  return assets.find(({ name }) => name.endsWith(suffix));
}
