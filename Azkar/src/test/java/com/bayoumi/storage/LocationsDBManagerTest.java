package com.bayoumi.storage;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class LocationsDBManagerTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private Path createTestDatabasePath(String filename, boolean createTables) throws Exception {
        File file = tempFolder.newFile(filename);
        String url = "jdbc:sqlite:" + file.getAbsolutePath();
        try (Connection con = DriverManager.getConnection(url);
             Statement st = con.createStatement()) {
            if (createTables) {
                st.execute("CREATE TABLE Countries (id INTEGER PRIMARY KEY, name TEXT);");
                st.execute("CREATE TABLE cityd (id INTEGER PRIMARY KEY, name TEXT);");
            } else {
                st.execute("CREATE TABLE dummy (id INTEGER PRIMARY KEY);");
            }
        }
        return file.toPath();
    }

    @Test
    public void testBundledPreferredWhenBothValid() throws Exception {
        Path bundled = createTestDatabasePath("bundled.db", true);
        Path fallback = createTestDatabasePath("fallback.db", true);

        Connection con = LocationsDBManager.openDatabaseConnection(bundled, fallback);
        Assert.assertNotNull("Connection should be established", con);
        Assert.assertTrue("Should connect to bundled DB", con.getMetaData().getURL().contains("bundled.db"));
        con.close();
    }

    @Test
    public void testFallbackSelectedWhenBundledMissing() throws Exception {
        Path bundled = tempFolder.getRoot().toPath().resolve("nonexistent.db");
        Path fallback = createTestDatabasePath("fallback.db", true);

        Connection con = LocationsDBManager.openDatabaseConnection(bundled, fallback);
        Assert.assertNotNull("Connection should be established", con);
        Assert.assertTrue("Should connect to fallback DB", con.getMetaData().getURL().contains("fallback.db"));
        con.close();
    }

    @Test
    public void testFallbackSelectedWhenBundledInvalidSchema() throws Exception {
        Path bundled = createTestDatabasePath("invalid_bundled.db", false);
        Path fallback = createTestDatabasePath("fallback.db", true);

        Connection con = LocationsDBManager.openDatabaseConnection(bundled, fallback);
        Assert.assertNotNull("Connection should be established", con);
        Assert.assertTrue("Should connect to fallback DB", con.getMetaData().getURL().contains("fallback.db"));
        Assert.assertTrue("Bundled invalid file must NOT be deleted", Files.exists(bundled));
        con.close();
    }

    @Test
    public void testFallbackSelectedWhenBundledCorruptNonSqlite() throws Exception {
        File corruptFile = tempFolder.newFile("corrupt_bundled.db");
        Files.write(corruptFile.toPath(), "NOT_A_VALID_SQLITE_FILE_HEADER".getBytes(StandardCharsets.UTF_8));
        Path bundled = corruptFile.toPath();
        Path fallback = createTestDatabasePath("fallback.db", true);

        Connection con = LocationsDBManager.openDatabaseConnection(bundled, fallback);
        Assert.assertNotNull("Connection should be established", con);
        Assert.assertTrue("Should connect to fallback DB when bundled is corrupt", con.getMetaData().getURL().contains("fallback.db"));
        Assert.assertTrue("Corrupt bundled file must NOT be deleted", Files.exists(bundled));
        con.close();
    }

    @Test
    public void testRejectsWhenBothInvalid() throws Exception {
        Path bundled = createTestDatabasePath("invalid_bundled.db", false);
        Path fallback = createTestDatabasePath("invalid_fallback.db", false);

        Connection con = LocationsDBManager.openDatabaseConnection(bundled, fallback);
        Assert.assertNull("Connection should be null when both invalid", con);
        Assert.assertTrue("Bundled file must NOT be deleted", Files.exists(bundled));
        Assert.assertTrue("Fallback file must NOT be deleted", Files.exists(fallback));
    }

    @Test
    public void testEqualPathsHandledSafely() throws Exception {
        Path dbPath = createTestDatabasePath("single.db", false);

        Connection con = LocationsDBManager.openDatabaseConnection(dbPath, dbPath);
        Assert.assertNull("Should return null for invalid DB path without double evaluation error", con);
        Assert.assertTrue("File must NOT be deleted", Files.exists(dbPath));
    }

    @Test
    public void testAcceptedConnectionIsReadOnlyAndNoSidecarsCreated() throws Exception {
        Path bundled = createTestDatabasePath("bundled_readonly.db", true);

        Connection con = LocationsDBManager.openDatabaseConnection(bundled, null);
        Assert.assertNotNull("Connection should be established", con);

        boolean writeFailed = false;
        try (Statement st = con.createStatement()) {
            st.execute("CREATE TABLE read_only_test (id INTEGER);");
        } catch (SQLException ex) {
            writeFailed = true;
        } finally {
            con.close();
        }

        Assert.assertTrue("Write attempt must fail on read-only connection", writeFailed);

        File dir = bundled.getParent().toFile();
        String name = bundled.getFileName().toString();
        Assert.assertFalse("Sidecar -journal must not exist", new File(dir, name + "-journal").exists());
        Assert.assertFalse("Sidecar -wal must not exist", new File(dir, name + "-wal").exists());
        Assert.assertFalse("Sidecar -shm must not exist", new File(dir, name + "-shm").exists());
    }
}
