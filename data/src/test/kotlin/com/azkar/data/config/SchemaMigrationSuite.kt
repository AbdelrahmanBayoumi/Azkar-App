package com.azkar.data.config

import com.azkar.testUtils.TestDatabase
import com.zaxxer.hikari.HikariDataSource
import de.infix.testBalloon.framework.core.testSuite
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * Migrating a database that has already been migrated.
 *
 * [TestDatabase.bootstrapWithH2] migrates a fresh in-memory database, which is what every other suite
 * uses. That path cannot fail here: there is nothing in the database yet, so every statement Exposed
 * generates is a `CREATE ... IF NOT EXISTS` and there is no index for it to try to remove. The second
 * run is a different story — H2 has by then created indexes of its own to back the foreign keys, and
 * those are the case that has to keep working, because it is the case every start after the first
 * actually takes.
 *
 * This suite therefore opens a new pool and connection for every migration instead of sharing one,
 * which is what a process restart looks like to Exposed. That is why it does not share the
 * [TestDatabase] fixture: [TestDatabase.reset] assumes a connection it already holds.
 *
 * TestBalloon generates the test entry point, but the IDE does not see that usage.
 */
@Suppress("unused")
val SchemaMigrationSuite by testSuite("Migrating a database that already exists") {
    test("migrating again succeeds, which is what every start after the first does") {
        withMigratedDatabase { name ->
            assertThatCode { migrateAgain(name) }.doesNotThrowAnyException()
        }
    }

    test("a third and fourth migration are still no-ops rather than failures") {
        withMigratedDatabase { name ->
            assertThatCode { migrateAgain(name) }.doesNotThrowAnyException()
            assertThatCode { migrateAgain(name) }.doesNotThrowAnyException()
        }
    }

    test("the rows are still there after a repeat migration") {
        withMigratedDatabase { name ->
            val before = countRemembrances(name)

            migrateAgain(name)
            assertThat(countRemembrances(name)).isEqualTo(before)

            migrateAgain(name)
            assertThat(countRemembrances(name)).isEqualTo(before)
        }
    }

    test("an index the model declares survives a repeat migration") {
        withMigratedDatabase { name ->
            migrateAgain(name)

            // The filter that protects the engine-owned index must not reach across and drop one we
            // declared ourselves, or the index on remembrance.is_favorite would quietly disappear.
            assertThat(remembranceIndexNames(name)).contains("idx_remembrance__is_favorite")
        }
    }

    test("a repeat migration writes no rows at all") {
        // The migration is schema and nothing else. The dataset is written once, by the bootstrapper,
        // on first launch — so whatever a migration does to the schema, it must leave the contents of
        // every table exactly as it found them, or a second launch would be a second library.
        withMigratedDatabase { name ->
            val collectionsBefore = countRows(name, "collection")
            val remembrancesBefore = countRows(name, "remembrance")

            migrateAgain(name)

            assertThat(countRows(name, "collection")).isEqualTo(collectionsBefore)
            assertThat(countRows(name, "remembrance")).isEqualTo(remembrancesBefore)
        }
    }

    test("a custom collection survives a repeat migration") {
        // There is no seed to protect the row from any more, which makes this a statement about the
        // migration itself rather than about an upsert: a row the user wrote has to come through the
        // whole cycle untouched.
        withMigratedDatabase { name ->
            val customId =
                withConnection(name) { database ->
                    transaction(database) {
                        exec("INSERT INTO collection (name, kind) VALUES ('Mine','CUSTOM')")
                        exec("SELECT id FROM collection WHERE name = 'Mine'") { resultSet ->
                            resultSet.next()
                            resultSet.getLong(1)
                        }
                    }
                }

            migrateAgain(name)

            val after =
                withConnection(name) { database ->
                    transaction(database) {
                        var collectionName = ""
                        exec("SELECT name FROM collection WHERE id = $customId") { resultSet ->
                            resultSet.next()
                            collectionName = resultSet.getString(1)
                        }
                        collectionName
                    }
                }
            assertThat(after).isEqualTo("Mine")
        }
    }
}

/** Counts the seeded remembrances through a fresh connection. */
private fun countRemembrances(name: String): Int = countRows(name, "remembrance")

/** Counts the rows of [table] through a fresh connection. */
private fun countRows(
    name: String,
    table: String,
): Int =
    withConnection(name) { database ->
        transaction(database) {
            var count = 0
            exec("SELECT COUNT(*) FROM $table") { resultSet ->
                resultSet.next()
                count = resultSet.getInt(1)
            }
            count
        }
    }

/** The index names H2 holds on the remembrance table, read the same way a fresh start would see them. */
private fun remembranceIndexNames(name: String): Set<String> =
    withConnection(name) { database ->
        transaction(database) {
            val names = mutableSetOf<String>()
            exec("SELECT INDEX_NAME FROM INFORMATION_SCHEMA.INDEXES WHERE TABLE_NAME = 'remembrance'") { resultSet ->
                while (resultSet.next()) names += resultSet.getString(1)
            }
            names
        }
    }

/** Runs [block] against a brand new pool and connection, then releases both. */
private fun <T> withConnection(
    name: String,
    block: (Database) -> T,
): T {
    val dataSource = HikariDataSource(DatabaseManager.poolConfig(jdbcUrl(name)))
    dataSource.use { dataSource ->
        return block(Database.connect(dataSource, { }, ExposedDatabaseConfig.INSTANCE))
    }
}

/**
 * Migrates [name] again, the way the next start of the application would.
 *
 * Each call builds and closes its own pool rather than reusing one. That is not incidental: Exposed
 * reads the existing schema through the connection it is handed, so a second migration over the same
 * pool sees the schema as it looked before the first one ran and tries to recreate every table,
 * which then fails on a constraint that is already there. The application never hits that, because
 * each start is a new process with a new pool — giving every migration its own pool is what makes
 * this test the same code path the application takes.
 */
private fun migrateAgain(name: String) {
    val dataSource = HikariDataSource(DatabaseManager.poolConfig(jdbcUrl(name)))
    dataSource.use { dataSource ->
        Database.connect(dataSource, { }, ExposedDatabaseConfig.INSTANCE).migrateToLatestSchema()
    }
}

/**
 * The H2 URL for the in-memory database called [name].
 *
 * `DB_CLOSE_DELAY=-1` is what makes an in-memory database usable here. Without it H2 discards the
 * database the moment the last connection closes, so the second migration would come up against an
 * empty one and there would be no repeat migration left to test. With it the database outlives every
 * pool below, which is what lets each call open and close its own connection while still seeing the
 * schema the previous one left behind — the same thing a new process does to a database on disk.
 */
private fun jdbcUrl(name: String): String = "jdbc:h2:mem:$name;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"

/** An in-memory database named for this call alone, migrated once, with a body free to migrate it again. */
private fun withMigratedDatabase(block: (name: String) -> Unit) {
    val name = TestDatabase.uniqueName("azkar_migration")
    migrateAgain(name)
    block(name)
}
