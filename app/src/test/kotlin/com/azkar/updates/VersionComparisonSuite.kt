package com.azkar.updates

import de.infix.testBalloon.framework.core.testSuite
import org.assertj.core.api.Assertions.assertThat

// TestBalloon generates the test entry point, but the IDE does not see that usage.
@Suppress("unused")
val VersionComparisonSuite by testSuite("Update version ordering") {
    test("the runtime prerelease version wins over package metadata without its suffix") {
        assertThat(resolveApplicationVersion("0.1.2", "0.1.2-beta.3.1")).isEqualTo("0.1.2-beta.3.1")
    }

    test("the embedded app version is a fallback when runtime metadata is absent") {
        assertThat(resolveApplicationVersion("0.1.2", null)).isEqualTo("0.1.2")
    }

    test("numeric prerelease identifiers compare numerically") {
        assertThat(compareVersions("0.1.2-beta.10.1", "0.1.2-beta.9.8")).isPositive()
    }

    test("a stable release is newer than its prerelease") {
        assertThat(compareVersions("0.1.2", "0.1.2-beta.99.1")).isPositive()
    }

    test("switching to stable detects a lower core version") {
        assertThat(compareVersions("0.1.1", "0.1.2-beta.3.1")).isNegative()
    }

    test("Debian prerelease package versions use tilde ordering") {
        assertThat(debianPackageVersion("0.1.2-beta.84.1")).isEqualTo("0.1.2~beta.84.1")
    }

    test("the dev track shows the newest published prerelease, excluding drafts") {
        assertThat(
            latestChannelVersion(
                listOf(
                    ChannelReleaseVersion("0.1.2-beta.9", isPrerelease = true),
                    ChannelReleaseVersion("0.1.2-beta.24", isPrerelease = true),
                    ChannelReleaseVersion("0.1.2-beta.99", isPrerelease = true, isDraft = true),
                    ChannelReleaseVersion("0.1.1", isPrerelease = false),
                ),
                UpdateChannel.DEV,
            ),
        ).isEqualTo("0.1.2-beta.24")
    }

    test("the stable track excludes prereleases") {
        assertThat(
            latestChannelVersion(
                listOf(
                    ChannelReleaseVersion("0.1.2-beta.24", isPrerelease = true),
                    ChannelReleaseVersion("0.1.1", isPrerelease = false),
                ),
                UpdateChannel.STABLE,
            ),
        ).isEqualTo("0.1.1")
    }
}
