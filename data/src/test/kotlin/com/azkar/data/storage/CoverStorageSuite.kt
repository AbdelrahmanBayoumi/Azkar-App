package com.azkar.data.storage

import de.infix.testBalloon.framework.core.testSuite
import org.assertj.core.api.Assertions.assertThat
import java.nio.file.Files
import java.nio.file.Path

/**
 * The cover directory is the one part of the app's state that is not in the database, so nothing
 * else checks these rules for it. Each test gets its own directory rather than the real
 * `~/.azkar/covers`: a suite that swept the user's actual pictures would be a far worse bug than
 * any it is looking for.
 *
 * TestBalloon generates the test entry point, but the IDE does not see that usage.
 */
@Suppress("unused")
val CoverStorageSuite by testSuite("CoverStorage") {
    testSuite("storing a file the user picked") {
        test("the copy lands in the covers directory and keeps the extension") {
            withCovers { storage ->
                val source = imageFile("sunset.jpg")

                val name = storage.store(source)

                assertThat(name).endsWith(".jpg")
                assertThat(storage.resolve(name)).exists()
                assertThat(Files.readAllBytes(storage.resolve(name))).isEqualTo(SUNSET_BYTES)
            }
        }

        test("the stored name is not the path, so the library survives a moved home directory") {
            withCovers { storage ->
                val name = storage.store(imageFile("sunset.jpg"))

                assertThat(name).doesNotContain("/")
                assertThat(storage.resolve(name).parent).isEqualTo(storage.resolve(""))
            }
        }

        test("two stores of one file are two files, because a replacement needs somewhere new to go") {
            withCovers { storage ->
                val source = imageFile("sunset.jpg")

                assertThat(storage.store(source)).isNotEqualTo(storage.store(source))
            }
        }

        test("a file with no usable extension is stored as a png, because the bytes are what matter") {
            withCovers { storage ->
                assertThat(storage.store(imageFile("pasted-image"))).endsWith(".png")
            }
        }
    }

    testSuite("removing a cover") {
        test("deleting takes the file away") {
            withCovers { storage ->
                val name = storage.store(imageFile("sunset.png"))

                storage.delete(name)

                assertThat(storage.resolve(name)).doesNotExist()
            }
        }

        test("deleting a name that is not there is not a failure, because there is nothing to remove") {
            withCovers { storage ->
                storage.delete("never-stored.png")
            }
        }
    }

    testSuite("sweeping what nothing points at any more") {
        test("a cover no collection refers to goes, and the count says how many") {
            withCovers { storage ->
                val source = imageFile("sunset.png")
                val kept = storage.store(source)
                storage.store(source)
                storage.store(source)

                val swept = storage.sweepOrphans(setOf(kept))

                assertThat(swept).isEqualTo(2)
                assertThat(storage.storedNames()).containsExactly(kept)
            }
        }

        test("sweeping with nothing to remove deletes nothing") {
            withCovers { storage ->
                val name = storage.store(imageFile("sunset.png"))

                assertThat(storage.sweepOrphans(setOf(name))).isEqualTo(0)
                assertThat(storage.resolve(name)).exists()
            }
        }

        test("a directory that was never created reads as holding nothing rather than failing") {
            withCovers { storage ->
                assertThat(storage.storedNames()).isEmpty()
                assertThat(storage.sweepOrphans(emptySet())).isEqualTo(0)
            }
        }
    }
}

/** A covers directory of this suite's own, so nothing here can touch the user's real pictures. */
private fun withCovers(block: (CoverStorage) -> Unit) {
    val storage = CoverStorage(Files.createTempDirectory("azkar-cover-suite"))
    block(storage)
}

private fun imageFile(name: String): Path {
    val file = Files.createTempDirectory("azkar-cover-suite-src").resolve(name)
    Files.write(file, SUNSET_BYTES)
    return file
}

private val SUNSET_BYTES = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 1, 2, 3)
