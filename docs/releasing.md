# Releasing the desktop app

Nucleus 2.5.18 packages the existing Compose Desktop JVM application. The Compose UI, AWT windows, tray and ProGuard rules remain in the app; only installation and update packaging moved from Conveyor.

| Platform | Installer and update path | Initial architecture |
|---|---|---|
| Windows | NSIS `.exe`; Nucleus checks, downloads and installs after approval | x64 |
| macOS | DMG for installation; ZIP is the Nucleus replacement payload | Intel and Apple Silicon |
| Linux | `.deb` and `.rpm` through signed apt/dnf repositories; AppImage through Nucleus | x64 |

Linux repositories have Stable and Dev suites and retain only the newest package in each suite. Historical installers remain on GitHub Releases. apt/dnf owns package transactions; Azkar shows package-manager commands for updates and channel changes. AppImage, Windows and macOS use Nucleus in-app updates. The app checks on startup and from Settings, shows release notes, and waits for approval before downloading and installing. A channel change is an explicit request and can offer an older version; Azkar warns that the installer does not roll back database changes.

## Local packaging

The Linux DEB and RPM install and removal hooks live in `app/packaging/linux/gnomeExtension/scripts/`. The release shrinker rules live in `app/packaging/proguard-rules.pro`.

Use JDK 25 (Zulu in CI; ProGuard needs `jmods`). Nucleus packages on the matching host OS:

On Ubuntu/Debian, install the RPM build tool before building the full Linux format set:

```bash
sudo apt install rpm
```

The build checks for `rpmbuild` before packaging and gives the installation command when it is missing.

```bash
./gradlew :app:packageCurrentOS
```

For a local Dev package, use a unique SemVer prerelease with a `-beta.N` suffix:

```bash
./gradlew :app:packageCurrentOS -PappVersion=0.1.2-beta.1
```

Final packages, update metadata and `SHA256SUMS` are staged under `build/installers/<platform>/`, where platform is `linux-x64`, `windows-x64`, `macos-x64`, or `macos-arm64`. For example, a Dev AppImage is `build/installers/linux-x64/AppImage/azkar-0.1.2-beta.1-linux-x64.AppImage`. The staging task removes stale outputs for that platform. Intermediate Nucleus files stay under `app/build/compose/binaries/main-release/`.

`packageCurrentOS` also runs `smokeTestPackagedRuntime`: it launches the bundled release runtime in headless mode, opens/migrates a database in a fresh temporary profile, closes it, and requires a success marker within 90 seconds. It does not access the normal user profile, register startup tasks, or check for updates. The log is `app/build/reports/packaging/smoke-test.log`. Run `./gradlew :app:smokeTestPackagedRuntime` to check the runtime without rebuilding installer formats.

Linux CI packages DEB, RPM and AppImage; Windows CI packages NSIS; macOS CI packages DMG and ZIP on both supported architectures. Windows installer/uninstaller and macOS DMG icons are configured separately from the app icons. An AppImage embeds its icon, but file-manager thumbnails still depend on desktop support.

The release merger requires all eight platform/format artifacts and checks their exact versioned filenames, channel, manifest coverage, sizes and SHA-512 hashes. Duplicate filenames, missing architectures, unsafe URLs and unexpected files fail validation before output is written. Windows NSIS filenames include the Nucleus `-nsis` suffix, for example `azkar-0.1.2-windows-x64-nsis.exe`. Native EXE metadata may contain the numeric base version; macOS bundle versions offset the major by one (app 0.1.2 → bundle 1.1.2) because jpackage requires a positive major. App UI/JVM version and artifact names keep the original version; final update manifests always carry the full release version. Run its fixture tests with `./scripts/release/merge_nucleus_release.main.kts self-test` (requires `kotlinc`).

The Linux packages include the Azkar GNOME Shell extension. DEB and RPM register the bundled files system-wide. AppImage installs a user copy when the prayer panel is enabled. If GNOME has yet to discover the files, sign out and back in, then launch Azkar. The panel uses AppIndicator while the extension is unavailable and respects an explicit disable in GNOME. Linux packaging requires Bun and pnpm to build the extension from `app/packaging/linux/gnomeExtension`. Gradle installs the locked dependencies with pnpm, runs TypeScript with Bun, and bundles the generated `extension.js`. The extension and website use the shared [TypeScript workspace](development.md#typescript-workspace) for local checks and formatting.

Linux staging also checks the actual DEB/RPM internal versions and architectures. Both use `~beta.N` internally so Dev sorts below the next Stable version. The RPM task receives the full SemVer through Nucleus's public task API because its DSL validator rejects prereleases before electron-builder can convert them. Packaging regenerates each format's working directory because Nucleus 2.5.18 otherwise reuses an existing `package.json` and fallback manifest across versions.

Local `act` runs never upload GitHub artifacts or publish releases. In a disposable checkout, run a Linux-only packaging rehearsal with a Dev event payload:

```bash
printf '%s\n' '{"ref":"refs/heads/dev","repository":{"default_branch":"dev"}}' > /tmp/azkar-act-event.json
act workflow_dispatch -W .github/workflows/release.yml -e /tmp/azkar-act-event.json -j package --matrix id:linux-x64 --bind
```

Local preparation uses `0.0.0-beta.1` without GitHub credentials. Outputs stay in that checkout's `build/installers/`; the publish job explicitly rejects `ACT=true`. Container-created files may have container-user ownership, so prefer the direct Gradle task in your working checkout. Linux containers cannot validate native Windows/macOS installers.

Nucleus 2.5.18 was selected as a released, pinned baseline. It has Compose Gradle integration through reflection and an internal Compose JDK-probe dependency. The Linux DEB package was built successfully against this checkout's Kotlin 2.4.20, Compose 1.12.1 and Zulu JDK 25. Re-run package and installed-update checks when upgrading Compose or Nucleus. Review any future Compose bytecode patch before enabling it; the released 2.5.18 packaging path does not enable the later default text patch.

## Stable releases

Stable publication is an explicit version tag on `production`. First bump the default app version in `app/build.gradle.kts`, commit it to production, then tag that commit:

```bash
git tag -a v0.1.2 -m "Azkar 0.1.2"
git push origin v0.1.2
```

The workflow verifies the tag is on `production`, checks the recorded [installed-release evidence](release-validation.md), runs source checks and packaged-runtime smoke tests, and builds on Linux, Windows, macOS Intel and Apple Silicon runners. It validates the complete artifact set, prepares a draft release, builds and verifies a fresh signed repository site, explicitly deploys it through Pages Actions, checks every live file against the build checksums, then exposes the release. Already published assets cannot be overwritten by a rerun. The release tag and Gradle package version must match. This is ordered publication across two services, not an atomic transaction: if final release publication fails after deployment, the Linux repositories may already contain the new version. Rerun failed jobs for the same run while its artifacts remain available; inspect the draft and deployed version markers first. Do not overwrite a published version. After artifact expiry, rerun the full Dev workflow (which selects a new version), or rebuild an unpublished Stable tag only after checking its existing draft. A failed build/signature check leaves the previously deployed site untouched.

## Maintaining the release workflow

`.github/workflows/release.yml` defines triggers, job dependencies, permissions, and step-scoped
credentials. Release logic lives under `scripts/release/`:

- `metadata.main.kts`: validated API records, Dev batch eligibility, version numbering, and Stable source ancestry.
- `github-release.sh`: release notes, draft creation/resumption, and final publication.
- `repository-site.sh`: signing-key checks, package collection, site creation, and deployed-file verification.
- `release-rules.main.kts`: pure Kotlin rules shared by metadata and tests. The same selectors drive batching,
  notes, and package collection. Stable versions are ordered numerically, not by API response order.
- `github-client.main.kts`: Ktor HTTP client with typed `@Serializable` response models, pagination,
  timeouts, and explicit authentication. Redirects are rejected instead of forwarding credentials.
- `kotlin.sh`: loads the serialization plugin bundled with the selected compiler for metadata
  commands and HTTP tests; their shebangs use this launcher automatically from the repository root.

Run commands from the repository root. `./scripts/tests/test_release_workflow.main.kts` tests the scripts
directly using temporary files and mocked external commands, without publishing. The workflow
also runs `./scripts/tests/test_github_client.main.kts` with Ktor's MockEngine; no GitHub HTTP calls
are made by either suite. The workflow linter also runs ShellCheck over the shell entry points.
Changes remain covered by the installed-release
evidence check because it tracks the entire `scripts/` directory.
See the [scripts index](../scripts/README.md) for the folder layout, Kotlin tools, and local checks.

## Dev releases

PR checks run for each approved PR. Dev publication collects merged PRs into batches: the Release workflow checks `dev` at minutes 8 and 38 of each hour (UTC), and publishes only when there are new commits and at least six hours have passed since the last successful Dev release. Scheduled workflows use the default branch, which must remain `dev`. An unchanged, older, or diverged source revision skips packaging and publication without consuming a version number. The decision is checked after entering the shared release queue, so queued schedules do not publish duplicate batches.

To publish accumulated changes sooner, manually dispatch from `dev`:

```bash
gh workflow run release.yml --ref dev
```

Manual dispatch bypasses the six-hour wait but still requires new commits. Stable tag releases bypass Dev batching. Dev and Stable publication remain serialized because both deploy the same signed repository site.

The workflow selects the next patch after the latest Stable version and adds a sequential Beta suffix, for example `0.1.2-beta.24`. Numbering starts at `beta.1` for each new patch. Published releases, drafts, and existing tags reserve their numbers; legacy `beta.23.1` reserves number 23, so the next version is `beta.24`. There is no workflow-attempt suffix. This suffix is what Nucleus's released GitHub provider uses to select the Dev feed. Dev packages use the same application identity and data directory as Stable.

The workflow automatically publishes a versioned GitHub prerelease after deploying and verifying the signed Dev apt and RPM repositories. Dev releases carry GitHub's **Pre-release** label, including resumed drafts. The active repository replaces its previous Dev packages; GitHub prereleases retain their unique versioned assets. Dev release notes collect changes since the most recently published Dev prerelease, including every PR merged during the batch interval. A quiet interval can still contain only one PR. Full reruns of an unpublished batch select a new version if a draft or tag already reserves the previous one; rerun failed jobs to resume that existing draft. Stable tags remain explicit `vMAJOR.MINOR.PATCH` versions without Beta or attempt suffixes and are accepted only from `production`.

## Signed Linux repositories

Repository metadata is signed by a dedicated GPG key, separate from any Windows or Apple signing identity. Before publishing, configure these GitHub Actions secrets:

- `LINUX_REPO_SIGNING_KEY`: ASCII-armored private GPG key used only by the repository publication job.
- `LINUX_REPO_SIGNING_PASSPHRASE`: its passphrase (may be empty for an unprotected automation key).

The public key, repository metadata and newest Stable/Dev DEB/RPM pairs are generated into a disposable site directory and deployed as a Pages artifact. They are not committed to `gh-pages` or any other Git branch. The public identity is pinned in `assets/keys/azkar-repository.asc`; the workflow rejects a secret key with a different fingerprint. Back up the private key and its passphrase outside Git before rotating or removing either Actions secret. The public key URL is `https://jamhour1g.github.io/Azkar-App/keys/azkar-repo.asc`. User-facing apt/dnf setup examples are maintained in [the download page](../site/src/download.html). RPM repository metadata is signed; its package checksums are covered by that signed metadata. APT verifies package checksums through its signed Release metadata.

The `publish_linux_repositories.main.kts` script accepts an input directory with optional `stable/` and `dev/` subdirectories, an empty output site directory, an exported public key and a fingerprint. Each channel must have exactly one matching versioned DEB/RPM pair and its release `SHA256SUMS`. It validates hashes and channels, builds/verifies signed metadata, and produces the download page and `<channel>-version.txt` markers. It never commits, pushes or deploys anything itself. The workflow supplies the current release packages and downloads the latest published counterpart channel from GitHub Releases. If that channel has never been published, it remains absent until its first release.

The generated site must stay below 900 MiB, leaving space below Pages' 1 GB site limit. Only DEB/RPM files belong in the repositories; AppImage and other formats remain GitHub Release assets. The old `gh-pages` branch is left untouched and is no longer the deployment source.

A repository release requires GitHub Pages **Source: GitHub Actions**, with the `github-pages` environment allowing deployments from `dev` and `v*` tags. The deployment job alone has `pages: write` and `id-token: write`; contributor CI receives neither. Linux packages use the same package name in Stable and Dev, so switching suites replaces the selected package source rather than adding a second application.

Windows/macOS Stable installers may remain unsigned during development. Linux repository signatures are still required; this policy does not imply Apple notarization or Windows code signing exists.

## Activation

Land the workflow definitions on the default branch before relying on normal automatic dispatch. Owner-authored PRs can rehearse their own CI definitions from their same-repository head branch with manual dispatch; the workflow verifies the selected branch and SHA match that owner PR. Contributor approvals must use the default branch.

```bash
gh workflow run ci.yml --ref <owner-feature-branch> \
  -f pr_number=<PR-number> -f reviewed_sha=<full-reviewed-SHA> -f validate_installers=true
```

Before enabling Dev publication, configure the Linux signing secrets and switch Pages to Actions. Require `Azkar CI / Linux` on `dev`; require it plus `Azkar CI / Installers` on `production`. Keep strict/up-to-date checks, PRs, administrator enforcement, zero required approvals and no force pushes/deletion. Enable approval for all external fork contributors and keep default workflow permissions read-only, without PR review approval permission. Change the required contexts when the new workflow is available; the old `Production promotion gate` job has been replaced.

A successful hosted four-platform CI rehearsal is still necessary. It may run before publication because it needs no repository signing secrets. No Stable release can pass the initially pending installed-release record.

## Package and runtime details

- H2 AUTO_SERVER binds to `127.0.0.1` in the packaged app and source launcher. This keeps the local database off network interfaces and avoids machine-hostname reverse DNS during startup; tests exercise a real local AUTO_SERVER connection.

- The Linux desktop entry sets `StartupWMClass=com-azkar-MainKt`, matching the main-thread AWT initialization in `Main.kt`. ProGuard keeps class names unchanged for the same reason.
- The Nucleus AutoLaunch runtime identity is pinned to `com.azkar.app`; the macOS bundle identifier uses that same identity. macOS keeps its macOS 13 minimum for the AutoLaunch native bridge.
- Linux `.deb` and `.rpm` package versions order Dev below the next Stable version. The app's settings show the current version and selected channel; the package manager decides when the repository transaction is applied.
- Existing Conveyor migration is out of scope while Azkar is still in development. The new Windows installer has no in-place MSIX upgrade claim.

## Verification boundary

A successful Gradle package task proves build-time packaging only. Before public production release, install each artifact and check launch, shortcuts, tray behavior, update replacement and restart. Test the AppImage on Arch and test apt/dnf install, signed metadata verification, updates and both channel switches in disposable supported distributions. Verify macOS ZIP replacement separately from DMG installation. Real login-session checks remain necessary for start-at-login behavior.
