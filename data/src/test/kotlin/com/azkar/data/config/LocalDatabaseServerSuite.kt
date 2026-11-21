package com.azkar.data.config

import de.infix.testBalloon.framework.core.testSuite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import java.nio.file.Files
import java.sql.DriverManager
import java.util.Properties

// TestBalloon generates the test entry point, but the IDE does not see that usage.
@Suppress("unused")
val LocalDatabaseServerSuite by testSuite("Local database server") {
    test("AUTO_SERVER advertises loopback and accepts a second local connection") {
        withContext(Dispatchers.IO) {
            val directory = Files.createTempDirectory("azkar-auto-server-")
            val databasePath = directory.resolve("database").toString().replace('\\', '/')
            try {
                DriverManager.getConnection("jdbc:h2:file:$databasePath;AUTO_SERVER=TRUE", "sa", "").use {
                    val lock =
                        Properties().apply {
                            Files.newInputStream(directory.resolve("database.lock.db")).use { input -> load(input) }
                        }
                    val server = lock.getProperty("server")
                    assertThat(server.substringBeforeLast(':')).isEqualTo("localhost")
                    assertThat(lock.getProperty("hostName")).isEqualTo("localhost")
                    val key = lock.getProperty("id")
                    DriverManager.getConnection("jdbc:h2:tcp://$server/$key", "sa", "").use { remote ->
                        remote.createStatement().use { statement ->
                            statement.executeQuery("SELECT 1").use { result ->
                                assertThat(result.next()).isTrue()
                                assertThat(result.getInt(1)).isEqualTo(1)
                            }
                        }
                    }
                }
            } finally {
                Files.walk(directory).use { paths -> paths.sorted(Comparator.reverseOrder()).forEach(Files::delete) }
            }
        }
    }
}
