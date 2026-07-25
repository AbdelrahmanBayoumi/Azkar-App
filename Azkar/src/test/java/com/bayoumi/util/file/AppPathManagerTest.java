package com.bayoumi.util.file;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AppPathManagerTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private Path mockInstallDir;

    @Before
    public void setUp() throws IOException {
        mockInstallDir = tempFolder.newFolder("installDir").toPath();
    }

    // ===== AssetsPathContext & Decision Table =====

    @Test
    public void resolveAssetsPath_jarFilesExistsAndWritable_returnsInstallDirJarFiles() throws IOException {
        Path jarFiles = mockInstallDir.resolve("jarFiles");
        Files.createDirectories(jarFiles);

        AppPathManager.AssetsPathContext ctx = new AppPathManager.AssetsPathContext(
                mockInstallDir, true, true, true, "/home/testuser", null);
        String resolved = AppPathManager.resolveAssetsPath(ctx);

        Assert.assertEquals(jarFiles.toAbsolutePath().normalize().toString(), resolved);
        Assert.assertTrue("Must be absolute", Paths.get(resolved).isAbsolute());
    }

    @Test
    public void resolveAssetsPath_jarFilesExistsButNotWritable_returnsUserDataDir() throws IOException {
        Path jarFiles = mockInstallDir.resolve("jarFiles");
        Files.createDirectories(jarFiles);

        AppPathManager.AssetsPathContext ctx = new AppPathManager.AssetsPathContext(
                mockInstallDir, true, false, true, "/home/testuser", null);
        String resolved = AppPathManager.resolveAssetsPath(ctx);

        String expected = Paths.get("/home/testuser/.Azkar/jarFiles").toAbsolutePath().normalize().toString();
        Assert.assertEquals(expected, resolved);
        Assert.assertTrue("Must be absolute", Paths.get(resolved).isAbsolute());
    }

    @Test
    public void resolveAssetsPath_jarFilesNotExistAndInstallDirWritable_returnsInstallDirJarFiles() {
        AppPathManager.AssetsPathContext ctx = new AppPathManager.AssetsPathContext(
                mockInstallDir, false, false, true, "/home/testuser", null);
        String resolved = AppPathManager.resolveAssetsPath(ctx);

        String expected = mockInstallDir.resolve("jarFiles").toAbsolutePath().normalize().toString();
        Assert.assertEquals(expected, resolved);
    }

    @Test
    public void resolveAssetsPath_jarFilesNotExistAndInstallDirNotWritable_returnsUserDataDir() {
        AppPathManager.AssetsPathContext ctx = new AppPathManager.AssetsPathContext(
                mockInstallDir, false, false, false, "/home/testuser", null);
        String resolved = AppPathManager.resolveAssetsPath(ctx);

        String expected = Paths.get("/home/testuser/.Azkar/jarFiles").toAbsolutePath().normalize().toString();
        Assert.assertEquals(expected, resolved);
    }

    @Test
    public void resolveAssetsPath_neverReturnsRelativeJarFiles() {
        AppPathManager.AssetsPathContext ctx = new AppPathManager.AssetsPathContext(
                mockInstallDir, true, true, true, "/home/testuser", null);
        String resolved = AppPathManager.resolveAssetsPath(ctx);

        Assert.assertNotEquals("jarFiles", resolved);
        Assert.assertTrue("Must be absolute", Paths.get(resolved).isAbsolute());
    }

    @Test
    public void resolveAssetsPath_withLocalAppData_usesLocalAppDataPath() {
        AppPathManager.AssetsPathContext ctx = new AppPathManager.AssetsPathContext(
                mockInstallDir, false, false, false, "/home/testuser", "/mock/appdata/local");
        String resolved = AppPathManager.resolveAssetsPath(ctx);

        String expected = Paths.get("/mock/appdata/local/Azkar/jarFiles").toAbsolutePath().normalize().toString();
        Assert.assertEquals(expected, resolved);
    }

    @Test
    public void resolveAssetsPath_withoutLocalAppData_usesUserHomeDotAzkar() {
        AppPathManager.AssetsPathContext ctx = new AppPathManager.AssetsPathContext(
                mockInstallDir, false, false, false, "/home/testuser", null);
        String resolved = AppPathManager.resolveAssetsPath(ctx);

        Assert.assertTrue("Should contain user home", resolved.contains("/home/testuser"));
        Assert.assertTrue("Should contain .Azkar", resolved.contains(".Azkar"));
    }

    // ===== computeUserDataAssetsPath =====

    @Test
    public void computeUserDataAssetsPath_alwaysAbsolute() {
        String result = AppPathManager.computeUserDataAssetsPath("/home/user", null);
        Assert.assertTrue(Paths.get(result).isAbsolute());
    }

    @Test(expected = IllegalArgumentException.class)
    public void computeUserDataAssetsPath_nullUserHome_throwsIllegalArgumentException() {
        AppPathManager.computeUserDataAssetsPath(null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void computeUserDataAssetsPath_emptyUserHome_throwsIllegalArgumentException() {
        AppPathManager.computeUserDataAssetsPath("   ", null);
    }

    // ===== Dev Layout Detection =====

    @Test
    public void findDevelopmentProjectRoot_targetClasses_returnsProjectRoot() throws IOException {
        File projectRoot = tempFolder.newFolder("ProjectRoot");
        new File(projectRoot, "pom.xml").createNewFile();
        File classesDir = new File(projectRoot, "target/classes");
        classesDir.mkdirs();

        Path result = AppPathManager.findDevelopmentProjectRoot(classesDir.toPath());
        Assert.assertNotNull(result);
        Assert.assertEquals(projectRoot.toPath().toAbsolutePath().normalize(), result);
    }

    @Test
    public void findDevelopmentProjectRoot_targetJfxApp_returnsProjectRoot() throws IOException {
        File projectRoot = tempFolder.newFolder("ProjectRoot2");
        new File(projectRoot, "pom.xml").createNewFile();
        File jfxAppDir = new File(projectRoot, "target/jfx/app");
        jfxAppDir.mkdirs();

        Path result = AppPathManager.findDevelopmentProjectRoot(jfxAppDir.toPath());
        Assert.assertNotNull(result);
        Assert.assertEquals(projectRoot.toPath().toAbsolutePath().normalize(), result);
    }

    @Test
    public void findDevelopmentProjectRoot_targetDirect_returnsProjectRoot() throws IOException {
        File projectRoot = tempFolder.newFolder("ProjectRoot3");
        new File(projectRoot, "jarFiles").mkdirs();
        File targetDir = new File(projectRoot, "target");
        targetDir.mkdirs();

        Path result = AppPathManager.findDevelopmentProjectRoot(targetDir.toPath());
        Assert.assertNotNull(result);
        Assert.assertEquals(projectRoot.toPath().toAbsolutePath().normalize(), result);
    }

    @Test
    public void findDevelopmentProjectRoot_nonDevPath_returnsNull() throws IOException {
        File installDir = tempFolder.newFolder("opt", "Azkar");

        Path result = AppPathManager.findDevelopmentProjectRoot(installDir.toPath());
        Assert.assertNull("Non-dev path should return null", result);
    }

    @Test
    public void findDevelopmentProjectRoot_targetWithoutMarker_returnsNull() throws IOException {
        File fakeRoot = tempFolder.newFolder("NoMarker");
        File fakeTarget = new File(fakeRoot, "target/classes");
        fakeTarget.mkdirs();

        Path result = AppPathManager.findDevelopmentProjectRoot(fakeTarget.toPath());
        Assert.assertNull("Path named target but without pom.xml or jarFiles should return null", result);
    }

    // ===== copyIfNotExist =====

    @Test
    public void copyIfNotExist_preservesExistingFile() throws IOException {
        File source = tempFolder.newFile("source.txt");
        try (FileWriter fw = new FileWriter(source)) {
            fw.write("NEW CONTENT");
        }

        File target = tempFolder.newFile("target.txt");
        try (FileWriter fw = new FileWriter(target)) {
            fw.write("ORIGINAL CONTENT");
        }

        FileUtils.copyIfNotExist(source.toPath(), target.toPath());

        String content = new String(java.nio.file.Files.readAllBytes(target.toPath()));
        Assert.assertEquals("ORIGINAL CONTENT", content);
    }
}
