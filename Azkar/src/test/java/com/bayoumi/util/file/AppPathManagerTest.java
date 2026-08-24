package com.bayoumi.util.file;

import org.junit.Assert;
import org.junit.Assume;
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
    public void resolveAssetsPath_production_linux_returnsUserDataDir() {
        AppPathManager.AssetsPathContext ctx = new AppPathManager.AssetsPathContext(
                mockInstallDir, "/home/testuser", null,
                AppPathManager.RuntimeEnvironment.PRODUCTION, AppPathManager.OperatingSystem.LINUX);
        String resolved = AppPathManager.resolveAssetsPath(ctx);

        String expected = Paths.get("/home/testuser/.Azkar/jarFiles").toAbsolutePath().normalize().toString();
        Assert.assertEquals(expected, resolved);
        Assert.assertTrue("Must be absolute", Paths.get(resolved).isAbsolute());
    }

    @Test
    public void resolveAssetsPath_development_returnsInstallDirJarFiles() {
        AppPathManager.AssetsPathContext ctx = new AppPathManager.AssetsPathContext(
                mockInstallDir, "/home/testuser", null,
                AppPathManager.RuntimeEnvironment.DEVELOPMENT, AppPathManager.OperatingSystem.LINUX);
        String resolved = AppPathManager.resolveAssetsPath(ctx);

        String expected = mockInstallDir.resolve("jarFiles").toAbsolutePath().normalize().toString();
        Assert.assertEquals(expected, resolved);
    }

    @Test
    public void resolveAssetsPath_production_windows_withLocalAppData_usesLocalAppData() {
        String userHome = "/users/testuser";
        String localAppData = "/users/testuser/appdata/local";
        AppPathManager.AssetsPathContext ctx = new AppPathManager.AssetsPathContext(
                mockInstallDir, userHome, localAppData,
                AppPathManager.RuntimeEnvironment.PRODUCTION, AppPathManager.OperatingSystem.WINDOWS);
        String resolved = AppPathManager.resolveAssetsPath(ctx);

        String expected = Paths.get(localAppData, "Azkar", "jarFiles").toAbsolutePath().normalize().toString();
        Assert.assertEquals(expected, resolved);
    }

    @Test
    public void resolveAssetsPath_production_windows_withoutLocalAppData_usesUserHomeAppDataLocal() {
        String userHome = "/users/testuser";
        AppPathManager.AssetsPathContext ctx = new AppPathManager.AssetsPathContext(
                mockInstallDir, userHome, null,
                AppPathManager.RuntimeEnvironment.PRODUCTION, AppPathManager.OperatingSystem.WINDOWS);
        String resolved = AppPathManager.resolveAssetsPath(ctx);

        String expected = Paths.get(userHome, "AppData", "Local", "Azkar", "jarFiles").toAbsolutePath().normalize().toString();
        Assert.assertEquals(expected, resolved);
    }

    @Test
    public void resolveAssetsPath_production_legacyDbExistsAndWritable_returnsLegacyPathWhenCanonicalDbMissing() throws IOException {
        Path legacyJarFiles = mockInstallDir.resolve("jarFiles");
        Files.createDirectories(legacyJarFiles.resolve("db"));
        Files.write(legacyJarFiles.resolve("db/data.db"), new byte[]{1, 2, 3});

        String userHome = tempFolder.newFolder("freshUserHome").getAbsolutePath();
        AppPathManager.AssetsPathContext ctx = new AppPathManager.AssetsPathContext(
                mockInstallDir, userHome, null,
                AppPathManager.RuntimeEnvironment.PRODUCTION, AppPathManager.OperatingSystem.LINUX);
        String resolved = AppPathManager.resolveAssetsPath(ctx);

        Assert.assertEquals(legacyJarFiles.toAbsolutePath().normalize().toString(), resolved);
    }

    @Test
    public void resolveAssetsPath_production_canonicalDbExists_returnsCanonicalEvenIfLegacyDbExists() throws IOException {
        Path legacyJarFiles = mockInstallDir.resolve("jarFiles");
        Files.createDirectories(legacyJarFiles.resolve("db"));
        Files.write(legacyJarFiles.resolve("db/data.db"), new byte[]{1, 2, 3});

        File customUserHome = tempFolder.newFolder("existingUserHome");
        Path canonicalJarFiles = Paths.get(customUserHome.getAbsolutePath(), ".Azkar", "jarFiles");
        Files.createDirectories(canonicalJarFiles.resolve("db"));
        Files.write(canonicalJarFiles.resolve("db/data.db"), new byte[]{4, 5, 6});

        AppPathManager.AssetsPathContext ctx = new AppPathManager.AssetsPathContext(
                mockInstallDir, customUserHome.getAbsolutePath(), null,
                AppPathManager.RuntimeEnvironment.PRODUCTION, AppPathManager.OperatingSystem.LINUX);
        String resolved = AppPathManager.resolveAssetsPath(ctx);

        Assert.assertEquals(canonicalJarFiles.toAbsolutePath().normalize().toString(), resolved);
    }

    @Test
    public void resolveAssetsPath_production_legacyDbIsDirectory_returnsCanonical() throws IOException {
        Path legacyJarFiles = mockInstallDir.resolve("jarFiles");
        Files.createDirectories(legacyJarFiles.resolve("db/data.db")); // directory instead of file

        String userHome = tempFolder.newFolder("dirUserHome").getAbsolutePath();
        AppPathManager.AssetsPathContext ctx = new AppPathManager.AssetsPathContext(
                mockInstallDir, userHome, null,
                AppPathManager.RuntimeEnvironment.PRODUCTION, AppPathManager.OperatingSystem.LINUX);
        String resolved = AppPathManager.resolveAssetsPath(ctx);

        String expectedCanonical = Paths.get(userHome, ".Azkar", "jarFiles").toAbsolutePath().normalize().toString();
        Assert.assertEquals(expectedCanonical, resolved);
    }

    // ===== computeUserDataAssetsPath =====

    @Test
    public void computeUserDataAssetsPath_alwaysAbsolute() {
        String result = AppPathManager.computeUserDataAssetsPath("/home/user", null, false);
        Assert.assertTrue(Paths.get(result).isAbsolute());
    }

    @Test
    public void computeUserDataAssetsPath_linux_ignoresLocalAppData() {
        String result = AppPathManager.computeUserDataAssetsPath("/home/user", "/fake/local/appdata", false);
        Assert.assertEquals(Paths.get("/home/user/.Azkar/jarFiles").toAbsolutePath().normalize().toString(), result);
    }

    @Test
    public void computeUserDataAssetsPath_windows_usesLocalAppDataWhenPresent() {
        String userHome = "/home/user";
        String localAppData = "/home/user/custom/localappdata";
        String result = AppPathManager.computeUserDataAssetsPath(userHome, localAppData, true);
        Assert.assertEquals(Paths.get(localAppData, "Azkar", "jarFiles").toAbsolutePath().normalize().toString(), result);
    }

    @Test
    public void computeUserDataAssetsPath_windows_fallbackWhenLocalAppDataNull() {
        String userHome = "/home/user";
        String result = AppPathManager.computeUserDataAssetsPath(userHome, null, true);
        Assert.assertEquals(Paths.get(userHome, "AppData", "Local", "Azkar", "jarFiles").toAbsolutePath().normalize().toString(), result);
    }

    @Test
    public void computeUserDataAssetsPath_realWindowsPath_onWindows() {
        Assume.assumeTrue(com.bayoumi.util.OSUtil.isWindows());
        String localAppData = "C:\\Users\\user\\AppData\\Local";
        String userHome = "C:\\Users\\user";
        String result = AppPathManager.computeUserDataAssetsPath(userHome, localAppData, true);
        Assert.assertEquals(Paths.get(localAppData, "Azkar", "jarFiles").toAbsolutePath().normalize().toString(), result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void computeUserDataAssetsPath_nullUserHome_throwsIllegalArgumentException() {
        AppPathManager.computeUserDataAssetsPath(null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void computeUserDataAssetsPath_emptyUserHome_throwsIllegalArgumentException() {
        AppPathManager.computeUserDataAssetsPath("   ", null);
    }

    // ===== Dev Layout Detection & Routing =====

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

    @Test
    public void detectEnvironment_targetClasses_returnsDevelopment() throws IOException {
        File projectRoot = tempFolder.newFolder("DevProject");
        new File(projectRoot, "pom.xml").createNewFile();
        File classesDir = new File(projectRoot, "target/classes");
        classesDir.mkdirs();

        AppPathManager.RuntimeEnvironment env = AppPathManager.detectEnvironment(classesDir.toPath());
        Assert.assertEquals(AppPathManager.RuntimeEnvironment.DEVELOPMENT, env);
    }

    @Test
    public void detectEnvironment_nonDevPath_returnsProduction() throws IOException {
        File prodDir = tempFolder.newFolder("opt", "Azkar");
        AppPathManager.RuntimeEnvironment env = AppPathManager.detectEnvironment(prodDir.toPath());
        Assert.assertEquals(AppPathManager.RuntimeEnvironment.PRODUCTION, env);
    }

    @Test
    public void resolveAppInstallDir_targetClasses_returnsProjectRoot() throws IOException {
        File projectRoot = tempFolder.newFolder("DevProjectDir");
        new File(projectRoot, "pom.xml").createNewFile();
        File classesDir = new File(projectRoot, "target/classes");
        classesDir.mkdirs();

        Path resolved = AppPathManager.resolveAppInstallDir(classesDir.toPath());
        Assert.assertEquals(projectRoot.toPath().toAbsolutePath().normalize(), resolved);
    }

    @Test
    public void resolveAppInstallDir_nonDevPath_returnsInstallDir() throws IOException {
        File prodDir = tempFolder.newFolder("opt", "AzkarApp");
        Path resolved = AppPathManager.resolveAppInstallDir(prodDir.toPath());
        Assert.assertEquals(prodDir.toPath().toAbsolutePath().normalize(), resolved);
    }

    @Test
    public void resolveAssetsPath_realTargetLayout_resolvesToProjectJarFiles_withoutTouchingRealUserData() throws IOException {
        File projectRoot = tempFolder.newFolder("RealLayoutProject");
        new File(projectRoot, "pom.xml").createNewFile();
        File projectJarFiles = new File(projectRoot, "jarFiles");
        projectJarFiles.mkdirs();
        File classesDir = new File(projectRoot, "target/classes");
        classesDir.mkdirs();

        Path installDir = AppPathManager.resolveAppInstallDir(classesDir.toPath());
        AppPathManager.RuntimeEnvironment env = AppPathManager.detectEnvironment(classesDir.toPath());
        AppPathManager.OperatingSystem os = AppPathManager.OperatingSystem.LINUX;

        String syntheticHome = tempFolder.newFolder("syntheticHome").getAbsolutePath();
        AppPathManager.AssetsPathContext ctx = new AppPathManager.AssetsPathContext(
                installDir, syntheticHome, null, env, os);
        String resolved = AppPathManager.resolveAssetsPath(ctx);

        Assert.assertEquals(projectJarFiles.toPath().toAbsolutePath().normalize().toString(), resolved);
        Assert.assertFalse("Must not create or use synthetic user data directory for dev",
                Files.exists(Paths.get(syntheticHome, ".Azkar")));
    }

    // ===== Writability Probe =====

    @Test
    public void isDirectoryActuallyWritable_writableDirectory_returnsTrue() throws IOException {
        Path tempDir = tempFolder.newFolder("writableDir").toPath();
        Assert.assertTrue(AppPathManager.isDirectoryActuallyWritable(tempDir));
    }

    @Test
    public void isDirectoryActuallyWritable_nonExistentDirectory_returnsFalse() {
        Path nonExistent = mockInstallDir.resolve("does_not_exist");
        Assert.assertFalse(AppPathManager.isDirectoryActuallyWritable(nonExistent));
    }

    @Test
    public void isDirectoryActuallyWritable_nullDirectory_returnsFalse() {
        Assert.assertFalse(AppPathManager.isDirectoryActuallyWritable(null));
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
