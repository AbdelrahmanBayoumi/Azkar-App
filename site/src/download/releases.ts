export interface ReleaseAsset {
  name: string;
  browser_download_url: string;
}

export interface StableRelease {
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

export function selectStableRelease(value: unknown): StableRelease | undefined {
  if (!Array.isArray(value))
    throw new TypeError("Expected a GitHub release list");

  for (const release of value as unknown[]) {
    if (
      !isRecord(release) ||
      release.draft !== false ||
      release.prerelease !== false ||
      typeof release.tag_name !== "string"
    )
      continue;

    return {
      tag_name: release.tag_name,
      html_url:
        typeof release.html_url === "string" ? release.html_url : undefined,
      assets: Array.isArray(release.assets)
        ? release.assets.filter(isReleaseAsset)
        : [],
    };
  }
}

export function findInstaller(
  assets: ReleaseAsset[],
  suffix: string,
): ReleaseAsset | undefined {
  return assets.find(({ name }) => name.endsWith(suffix));
}
