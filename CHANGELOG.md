<h1 align=center>📜 Changelog - سجل التغيير</h1>
<p align=center>سجل التغيير هو ملف يحتوي على قائمة مرتبة ترتيبًا زمنيًا بالتغييرات الملحوظة لكل إصدار من المشروع.</p>

## [Unreleased](https://github.com/AbdelrahmanBayoumi/Azkar-App/compare/1.3.0...HEAD)

### Fix

- 🔊 Replace JavaFX `MediaPlayer` with a JDK `javax.sound.sampled` player so Adhan and Azkar audio no longer crash the JVM on Linux via GStreamer [#109](https://github.com/AbdelrahmanBayoumi/Azkar-App/issues/109)

## [1.3.0](https://github.com/AbdelrahmanBayoumi/Azkar-App/releases/tag/1.3.0) - 2025-05-06

### Add

- 🌙 Ability to switch between light and dark modes [#4](https://github.com/AbdelrahmanBayoumi/Azkar-App/issues/4)
  - 🎨 Add options to restore default notification colors
- 🎨 Add Option to change notification color (background, text, and border)
- 🔊 Ability to upload custom Adhan (أذان) audio files [#41](https://github.com/AbdelrahmanBayoumi/Azkar-App/issues/41) and this also closes [#49](https://github.com/AbdelrahmanBayoumi/Azkar-App/issues/49) because the user now able to upload any Adhan (أذان) audio file he want.
- 🕌 Add Toggle to Enable/Disable Prayer Reminders [#79](https://github.com/AbdelrahmanBayoumi/Azkar-App/issues/79)

### Enhancement

- 🛠️ Enhanced code structure and performance.
- 🛠️ Refactor Sentry integration
- 🛠️ Update install4j: delete `jarFiles` when uninstall
- 🛠️ Change Other Timings `مواقيت اخرى` to Night Timings `مواقيت الليل`

[Full Changelog](https://github.com/AbdelrahmanBayoumi/Azkar-App/compare/1.2.9...1.3.0)

## [1.2.9](https://github.com/AbdelrahmanBayoumi/Azkar-App/releases/tag/1.2.9) - 2024-10-08

### Add

- 🔊 Ability to change Adhan Volume [#39](https://github.com/AbdelrahmanBayoumi/Azkar-App/issues/39)
- ⏳ Ability to configure the duration for which the notification stays on the screen. [#57](https://github.com/AbdelrahmanBayoumi/Azkar-App/issues/57)
- 🕰️ Added the ability to adjust prayer times by specifying an adjustment value (in minutes), allowing users to add or subtract minutes from the calculated prayer times before displaying the final result [#30](https://github.com/AbdelrahmanBayoumi/Azkar-App/issues/30)

### Fix

- 🔄 Fix repeated Azkar notifications that were shown in a random order by implementing a sequential tracking mechanism, ensuring notifications display in the correct order and loop back after the last item [#70](https://github.com/AbdelrahmanBayoumi/Azkar-App/issues/70)
- 🌍 Fix wrong selection of Cities like in Damascus, Syria [#72](https://github.com/AbdelrahmanBayoumi/Azkar-App/issues/72)

[Full Changelog](https://github.com/AbdelrahmanBayoumi/Azkar-App/compare/1.2.8...1.2.9)

## [1.2.8](https://github.com/AbdelrahmanBayoumi/Azkar-App/releases/tag/1.2.8) - 2024-07-06

### Add

- 📅 Display elapsed time since the current prayer for up to 30 minutes from the Adhan.
- 📜 Added Morning and Evening Adhkar in English and Arabic, with audio, Fadl, frequency, translation, and transliteration. [#33](https://github.com/AbdelrahmanBayoumi/Azkar-App/issues/33)

### Enhancement

- 🛠️ Enhanced code structure and performance.
- 🔄 Timed Adhkar settings now save and reflect font size changes instantly. [#51](https://github.com/AbdelrahmanBayoumi/Azkar-App/issues/51)
- 📌 Notification now is shown above the taskbar in Windows. [#56](https://github.com/AbdelrahmanBayoumi/Azkar-App/issues/56)

### Fix

- 🎨 Resolved notification color change errors. [#46](https://github.com/AbdelrahmanBayoumi/Azkar-App/issues/46)
- 🌍 Improved automatic location detection accuracy in some countries. [#45](https://github.com/AbdelrahmanBayoumi/Azkar-App/issues/45)
- 🔊 Fixed playing notification even if sound driver is not available. [#53](https://github.com/AbdelrahmanBayoumi/Azkar-App/issues/53)

[Full Changelog](https://github.com/AbdelrahmanBayoumi/Azkar-App/compare/1.2.7...1.2.8)

## [1.2.7](https://github.com/AbdelrahmanBayoumi/Azkar-App/releases/tag/1.2.7) - 2023-07-17

- Bundle EXE with 1.8_271 not 1.8_221
- Fix Timed Azkar window delay
- Exception: Error in getCityFromCoordinates() && getCityFromEngName() [#43](https://github.com/AbdelrahmanBayoumi/Azkar-App/issues/43)
- Fix NullPointerException [#42](https://github.com/AbdelrahmanBayoumi/Azkar-App/issues/42)

[Full Changelog](https://github.com/AbdelrahmanBayoumi/Azkar-App/compare/1.2.6...1.2.7)

## [1.2.6](https://github.com/AbdelrahmanBayoumi/Azkar-App/releases/tag/1.2.6) - 2023-06-06

### Fix

- [#40](https://github.com/AbdelrahmanBayoumi/Azkar-App/issues/40) Add one hour to time anyway if user wants to Fix Daylight saving time
- [`4274d17`](https://github.com/AbdelrahmanBayoumi/Azkar-App/commit/4274d173c90b9aecfe56d69d6ef41be065d84e3b) Now when first notification is shown it doesn't steal focus from the current running application
- [#38](https://github.com/AbdelrahmanBayoumi/Azkar-App/issues/38) Fix Spelling errors

[Full Changelog](https://github.com/AbdelrahmanBayoumi/Azkar-App/compare/1.2.5...1.2.6)

## [1.2.5](https://github.com/AbdelrahmanBayoumi/Azkar-App/releases/tag/1.2.5) - 2022-10-18

### Added

- ❎ Display close button in notification [#22](https://github.com/AbdelrahmanBayoumi/Azkar-App/issues/22)
  - Close Notification onClick only not onDrag [`c741f21`](https://github.com/AbdelrahmanBayoumi/Azkar-App/commit/c741f21788ed5827b1cae56a8edbf3607a034517)
- ⏳ Show elapsed time for each Prayer [#35](https://github.com/AbdelrahmanBayoumi/Azkar-App/issues/35)
- 🎨 Ability to change notification's border color [#28](https://github.com/AbdelrahmanBayoumi/Azkar-App/issues/28)
- ⬇️ Ability to Download LocationsDB if not exist [#27](https://github.com/AbdelrahmanBayoumi/Azkar-App/issues/27)

[Full Changelog](https://github.com/AbdelrahmanBayoumi/Azkar-App/compare/1.1.1...1.2.5)

## [1.1.1](https://github.com/AbdelrahmanBayoumi/Azkar-App/releases/tag/1.1.1) - 2022-04-12

### Added

- 📜 Add error tracker `Sentry`

[Full Changelog](https://github.com/AbdelrahmanBayoumi/Azkar-App/compare/1.1.0...1.1.1)

## [1.1.0](https://github.com/AbdelrahmanBayoumi/Azkar-App/releases/tag/1.1.0) - 2022-04-12

### Added

- Change font for the user interface
- Enable changing Language between [Arabic - English]
- Code enhancements

[Full Changelog](https://github.com/AbdelrahmanBayoumi/Azkar-App/compare/1.0.0...1.1.0)

## [1.0.0](https://github.com/AbdelrahmanBayoumi/Azkar-App/releases/tag/1.0.0) - 2021-11-16

### Added

- Check for Update inside the app
- Update the appearance of the home page by adding prayer times and a countdown to the next prayer time
- Reminder at Prayer Times and Play Adhan
- Code enhancement

[Full Changelog](https://github.com/AbdelrahmanBayoumi/Azkar-App/compare/0.9.4...1.0.0)

## [0.9.4](https://github.com/AbdelrahmanBayoumi/Azkar-App/releases/tag/0.9.4) - 2021-04-25

### Added

- Prayer times will be calculated without Internet
- User choose his location from a set of countries and cities
- Location can be detected with IP address
- Code enhancement

[Full Changelog](https://github.com/AbdelrahmanBayoumi/Azkar-App/compare/0.9.3...0.9.4)

## [0.9.3](https://github.com/AbdelrahmanBayoumi/Azkar-App/releases/tag/0.9.3) - 2021-04-18

### Added

- Hijri date can be increased and decreased from settings.
- Notifications location in screen can be changed (Top-Right, Top-Left, Bottom-Right, Bottom-Left, Center).
- Support Feedback & Bug reporting
- Code enhancement

[Full Changelog](https://github.com/AbdelrahmanBayoumi/Azkar-App/compare/0.9.2-beta...0.9.3)

## [0.9.2-beta](https://github.com/AbdelrahmanBayoumi/Azkar-App/releases/tag/0.9.2-beta) - 2021-04-05

- Calculating Muslim prayer times
- Morning and Nights Azkar with reminder
- With notification for random Azkar that pops-up in specific time

---

<h6 align="center">سبحَانَكَ اللَّهُمَّ وَبِحَمْدِكَ، أَشْهَدُ أَنْ لا إِلهَ إِلأَ انْتَ أَسْتَغْفِرُكَ وَأَتْوبُ إِلَيْكَ</h6>
