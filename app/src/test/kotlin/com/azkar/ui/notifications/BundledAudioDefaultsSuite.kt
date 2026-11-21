package com.azkar.ui.notifications

import de.infix.testBalloon.framework.core.testSuite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path

// TestBalloon generates the test entry point, but the IDE does not see that usage.
@Suppress("unused")
val BundledAudioDefaultsSuite by testSuite("Bundled audio installation") {
    test("a failed copy leaves no partial sound and the next installation succeeds") {
        withContext(Dispatchers.IO) {
            val directory = Files.createTempDirectory("azkar-audio")
            try {
                val failure =
                    runCatching {
                        BundledAudioDefaults.install(directory) {
                            object : InputStream() {
                                private var reads = 0

                                override fun read(): Int {
                                    if (reads++ < 32) return 1
                                    throw IOException("interrupted copy")
                                }
                            }
                        }
                    }.exceptionOrNull()
                assertThat(failure).isInstanceOf(IOException::class.java)
                Files.list(directory).use { assertThat(it.count()).isZero() }

                val installed = BundledAudioDefaults.install(directory)
                assertThat(Files.size(Path.of(installed.prayer))).isEqualTo(98_541L)
                assertThat(Files.size(Path.of(installed.notification))).isEqualTo(88_607L)
                Files.list(directory).use { assertThat(it.count()).isEqualTo(2) }
            } finally {
                Files.list(directory).use { entries -> entries.forEach { Files.delete(it) } }
                Files.delete(directory)
            }
        }
    }
}
