<div align="center">
  <img src="site/public/images/azkar.png" alt="Azkar app icon" width="88">
  <h1>Azkar</h1>
  <p>A desktop companion for daily adhkar and prayer times. Read, keep your place, and receive reminders from the passages you choose—on Windows, macOS, and Linux.</p>
  <p>
    <a href="https://jamhour1g.github.io/Azkar-App/download.html">Download</a> ·
    <a href="https://github.com/jamhour1g/Azkar-App/releases">Releases</a> ·
    <a href="https://github.com/jamhour1g/Azkar-App/issues">Report an issue</a>
  </p>
</div>

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="https://jamhour1g.github.io/Azkar-App/images/screen-home-dark.webp">
  <source media="(prefers-color-scheme: light)" srcset="https://jamhour1g.github.io/Azkar-App/images/screen-home-light.webp">
  <img src="https://jamhour1g.github.io/Azkar-App/images/screen-home-light.webp" alt="Azkar Home with a daily remembrance, today's prayer times, and a countdown to the next prayer" width="1440" height="900">
</picture>

## Start with Azkar

Install the desktop app from the **[download page](https://jamhour1g.github.io/Azkar-App/download.html)**,
or browse [GitHub releases](https://github.com/jamhour1g/Azkar-App/releases).

For Linux repositories, update channels, and installation guidance, see the download page. For
release channels and packaging details, see the [release guide](docs/releasing.md).

## Made for a steady daily practice

- **Find the adhkar you need.** Explore the bundled collections, browse topics, or search for a passage.
- **Understand what you read.** Read Arabic alongside translations, with explanations and source references where available.
- **Pick up where you left off.** Recite a collection one passage at a time, track repetitions, and resume your progress later.
- **Build your own library.** Add remembrances, arrange collections, choose covers, organize tags, and keep favorites close.
- **Receive reminders from your library.** Choose collections or passages, set a cadence, and pause reminders when you need to focus.
- **Keep track of prayer times.** See today's schedule and the next-prayer countdown, with individual prayer alerts and location settings.
- **Read comfortably.** Use English or Arabic, right-to-left layout, light or dark themes, and adjustable text size and typeface.

## A closer look

<table>
  <tr>
    <td width="50%">
      <strong>Find your daily adhkar</strong><br>
      Browse collections and topics, or search for a passage.
      <picture>
        <source media="(prefers-color-scheme: dark)" srcset="https://jamhour1g.github.io/Azkar-App/images/screen-library-dark.webp">
        <img src="https://jamhour1g.github.io/Azkar-App/images/screen-library-light.webp" alt="Azkar library with collections, topic filters, and search">
      </picture>
    </td>
    <td width="50%">
      <strong>Read with context</strong><br>
      Keep the Arabic text, meaning, and references together.
      <picture>
        <source media="(prefers-color-scheme: dark)" srcset="https://jamhour1g.github.io/Azkar-App/images/screen-reader-dark.webp">
        <img src="https://jamhour1g.github.io/Azkar-App/images/screen-reader-light.webp" alt="A remembrance reader with Arabic text, translation, and source details">
      </picture>
    </td>
  </tr>
  <tr>
    <td width="50%">
      <strong>Keep your place</strong><br>
      Follow a recitation session and track your progress.
      <picture>
        <source media="(prefers-color-scheme: dark)" srcset="https://jamhour1g.github.io/Azkar-App/images/screen-session-dark.webp">
        <img src="https://jamhour1g.github.io/Azkar-App/images/screen-session-light.webp" alt="An Azkar recitation session with passage navigation and progress">
      </picture>
    </td>
    <td width="50%">
      <strong>A reminder during your day</strong><br>
      Read a passage on your desktop and save it as a favorite.
      <picture>
        <source media="(prefers-color-scheme: dark)" srcset="https://jamhour1g.github.io/Azkar-App/images/screen-reminder-dark.webp">
        <img src="https://jamhour1g.github.io/Azkar-App/images/screen-reminder-light.webp" alt="An Azkar reminder card with Arabic text, favorite and dismiss controls">
      </picture>
    </td>
  </tr>
</table>

Screenshots follow your light or dark appearance. The
[download page](https://jamhour1g.github.io/Azkar-App/download.html#showcase-title) has the full gallery.

## Contribute

Bug reports and focused improvements are welcome. Include your operating system, app version, and
steps to reproduce. For UI changes, update the Roborazzi light and dark baselines; their image diffs
are included in the pull request. CI comparison artifacts are available when verification fails.

To run from source, install JDK 25, clone the repository, and start the desktop app:

```bash
git clone https://github.com/jamhour1g/Azkar-App.git
cd Azkar-App
./gradlew :app:run
```

The [development guide](docs/development.md) covers local checks, screenshots, and project layout.
Read [CI documentation](docs/ci.md) before opening a pull request.

## Project documentation

- [Development setup and project layout](docs/development.md)
- [Continuous integration and contributor approval](docs/ci.md)
- [Release process and packaging](docs/releasing.md)
- [Installed release validation](docs/release-validation.md)
- [Logging conventions](docs/logging.md)
- [Notification overlay behavior](docs/notification-overlay.md)
- [Bundled seed data notice](data/src/main/resources/db/seed/SEED_DATA_NOTICE.md)

> [!NOTE]
> The bundled remembrance library has separate attribution and licensing. See the
> [seed data notice](data/src/main/resources/db/seed/SEED_DATA_NOTICE.md).
