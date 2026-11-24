# Development

This guide covers running Azkar from source, checking changes locally, generating gallery images, and
finding the main parts of the project. For branch protections and contributor CI approval, see
[CI](ci.md). For release builds, see [Releasing](releasing.md).

## Requirements and first run

Azkar is a Kotlin desktop application built with
[Compose Multiplatform](https://www.jetbrains.com/compose-multiplatform/). Install **JDK 25**
(Zulu is used in CI) and clone the repository:

```bash
git clone https://github.com/jamhour1g/Azkar-App.git
cd Azkar-App
./gradlew :app:run
```

On Windows, use `.\gradlew.bat :app:run` in PowerShell. The Gradle wrapper downloads Gradle and
dependencies on the first run. Gradle 9.8 also supports running on JDK 27; when JDK 25 is not
installed, Gradle can download a matching Java 25 compilation toolchain via Foojay. This requires
network access and an available JDK for your platform. Alternatively, install JDK 25 and select it
with `JAVA_HOME` (and as the Gradle JVM in your IDE). Release shrinking uses the Gradle JVM's
`jmods` directory, so use Zulu JDK 25 for release builds; the Gradle checks report an error if
`jmods` is missing.

## Local checks

Run the test suites and a CI-style build before submitting changes:

```bash
./gradlew test
CI=true ./gradlew build
```

`test` runs the module test suites. With `CI=true`, `build` checks formatting and static analysis;
without it, the build applies Spotless formatting locally. In PowerShell, set the environment first:

```powershell
$env:CI = "true"
.\gradlew.bat build
```

CI's full workflows, approval model, and installer boundaries are documented in [CI](ci.md).

## Gallery images

Render the download page and README images independently of tests:

```bash
./gradlew :app:captureDownloadScreenshots
```

`app/src/gallery/kotlin/com/azkar/Gallery.kt` renders six scenes in both themes directly into
`site/public/images/` using Roborazzi's Compose Desktop capture API. Gallery-only fixtures live under
`app/src/preview/galleryFixtures/`. The generator uses Compose's UI-test renderer without running a test
suite. The release workflow uploads these freshly rendered images and publishes them to the site;
the README uses the same hosted files.

## TypeScript workspace

The download site and GNOME extension share Bun, pnpm, TypeScript, ESLint, and Prettier.
From the repository root, install once and check both packages:

```bash
pnpm install --frozen-lockfile --ignore-scripts
bun run check
```

Use `pnpm --filter azkar-download-site run build` to render the React installer and gallery
components into static HTML and build the browser bundle. Run `bun run dev` from the repo root to
build and serve the page at `http://127.0.0.1:3000/download.html`. GitHub Pages publishes the built files.

The browser tests build and serve the site automatically. Install Chromium once after
installing workspace dependencies, then run the suite.

```bash
pnpm --filter azkar-download-site exec bun --bun playwright install --only-shell chromium
pnpm --filter azkar-download-site run test:browser
```

Gradle builds the GNOME extension on Linux. Use `pnpm --filter azkar-gnome-extension run check`
for the extension checks alone.

## Preview images

Capture all Compose previews outside the IDE and update the committed images:

```bash
./gradlew :app:recordRoborazziJvm -Pazkar.previewScanner=true
```

The scanner covers private previews in `com.azkar.ui`, applies each preview's theme, locale, and
size, and writes WebP images under `app/src/preview/images/`. Commit those generated images
with UI changes so their visual differences appear in GitHub. This is capture-only; no task compares
the preview images against a baseline. Previews that open native desktop windows are included too.

Standard previews use `@AzkarPreview` to combine `@Preview` with the fixed English, light Azkar theme
and opaque app background. Use `@AzkarDarkPreview`, `@AzkarArabicPreview`, `@AzkarCompactPreview`, or
`@AzkarMediumPreview` to inspect those variants. These annotations have fixed configurations because
Kotlin cannot forward custom annotation arguments into `@Preview`; one-off dimensions and settings
can still use direct `@Preview` plus `AzkarThemePreview(width = ..., height = ...)`.

Preview annotations remain available for IDE rendering.

Capture the GNOME extension's schedule and actions menus on Linux with GNOME Shell installed:

```bash
./gradlew :app:captureGnomePanelPreviews
```

The task uses the bundled extension and fixture prayer data in an isolated GNOME Shell session.
It writes `gnome-prayer-schedule.png` and `gnome-prayer-actions.png` to `app/src/preview/images/`.

## Project layout

| Path | Responsibility |
| --- | --- |
| `app/` | Desktop UI, startup, settings, prayer times, and reminders |
| `domain/` | Library models, repository contracts, and services |
| `data/` | H2 persistence, schema, repositories, and bundled seed data |
| `build-logic/` | Shared Gradle conventions and packaging tasks |
| `site/` | Download page, generated gallery images, and icons |
| `docs/` | Development and operational documentation |

The bundled library is maintained in `library.json`; its source, editing workflow, and attribution
are described in the [seed data notice](../data/src/main/resources/db/seed/SEED_DATA_NOTICE.md).
