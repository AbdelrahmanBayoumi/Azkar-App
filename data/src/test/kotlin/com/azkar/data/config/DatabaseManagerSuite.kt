package com.azkar.data.config

import com.azkar.testUtils.TestDatabase
import com.zaxxer.hikari.HikariDataSource
import de.infix.testBalloon.framework.core.testSuite
import org.assertj.core.api.Assertions.assertThat

/**
 * Guards the connection-pool wiring that [DatabaseManager] performs before it ever hands a
 * `DataSource` to Exposed. Nothing else in the suite builds a pool — the repositories go
 * straight to a fixture's in-memory database — so a mistake in here would only surface
 * when the desktop app is launched.
 *
 * TestBalloon generates the test entry point, but the IDE does not see that usage.
 */
@Suppress("unused")
val DatabaseManagerSuite by testSuite("DatabaseManager") {
    test("poolConfig treats its argument as a JDBC URL, not a properties file path") {
        val config = DatabaseManager.poolConfig(H2_URL)

        assertThat(config.jdbcUrl).isEqualTo(H2_URL)
        assertThat(config.driverClassName).isEqualTo("org.h2.Driver")
        assertThat(config.isAutoCommit).isFalse()

        // The real proof: a pool built from this config hands out a usable connection.
        HikariDataSource(config).use { dataSource ->
            dataSource.connection.use { connection ->
                connection.createStatement().use { statement ->
                    statement.executeQuery("SELECT 1").use { resultSet ->
                        assertThat(resultSet.next()).isTrue()
                        assertThat(resultSet.getInt(1)).isEqualTo(1)
                    }
                }
            }
        }
    }
}

// A name of its own, so this pool can never be handed a database some other test is using.
private val H2_URL = "jdbc:h2:mem:${TestDatabase.uniqueName("pool_config")};DB_CLOSE_DELAY=-1"
