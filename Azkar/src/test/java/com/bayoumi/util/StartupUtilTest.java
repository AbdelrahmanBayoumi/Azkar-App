package com.bayoumi.util;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

public class StartupUtilTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void isValidAutostartExecutable_existingJar_accepted() throws IOException {
        File jarFile = tempFolder.newFile("app.jar");
        Assert.assertTrue(StartupUtil.isValidAutostartExecutable(jarFile.getAbsolutePath()));
    }

    @Test
    public void isValidAutostartExecutable_missingJar_rejected() {
        Assert.assertFalse(StartupUtil.isValidAutostartExecutable("/tmp/missing-azkar.jar"));
    }

    @Test
    public void isValidAutostartExecutable_directory_rejected() throws IOException {
        File dirPath = tempFolder.newFolder("target_classes");
        Assert.assertFalse(StartupUtil.isValidAutostartExecutable(dirPath.getAbsolutePath()));
    }

    @Test
    public void isValidAutostartExecutable_null_rejected() {
        Assert.assertFalse(StartupUtil.isValidAutostartExecutable(null));
    }

    @Test
    public void isValidAutostartExecutable_blank_rejected() {
        Assert.assertFalse(StartupUtil.isValidAutostartExecutable("   "));
    }

    @Test
    public void isValidAutostartExecutable_regularNonJarFile_rejected() throws IOException {
        File hosts = tempFolder.newFile("hosts");
        Assert.assertFalse(
                "Non-jar regular files should be rejected",
                StartupUtil.isValidAutostartExecutable(hosts.getAbsolutePath()));
    }

    @Test
    public void isValidAutostartExecutable_nonExistentNonJarFile_rejected() {
        Assert.assertFalse(
                StartupUtil.isValidAutostartExecutable("/tmp/nonexistent-file"));
    }

    @Test
    public void isValidAutostartExecutable_install4jExecutablePath_accepted() {
        String testPath = "/opt/Azkar/Azkar";
        String orig = System.getProperty("executablepath");
        try {
            System.setProperty("executablepath", testPath);
            Assert.assertTrue("install4j executablepath system property should be accepted",
                    StartupUtil.isValidAutostartExecutable(testPath));
        } finally {
            restoreProperty("executablepath", orig);
        }
    }

    @Test
    public void isValidAutostartExecutable_exe4jModuleName_accepted() {
        String testPath = "C:\\Program Files\\Azkar\\Azkar.exe";
        String orig = System.getProperty("exe4j.moduleName");
        try {
            System.setProperty("exe4j.moduleName", testPath);
            Assert.assertTrue("exe4j.moduleName system property should be accepted",
                    StartupUtil.isValidAutostartExecutable(testPath));
        } finally {
            restoreProperty("exe4j.moduleName", orig);
        }
    }

    @Test
    public void setAutostart_devMode_safeWithMockedHome() throws IOException {
        org.junit.Assume.assumeTrue(OSUtil.isLinux());
        File mockHome = tempFolder.newFolder("mockUserHome");
        String origHome = System.getProperty("user.home");
        try {
            System.setProperty("user.home", mockHome.getAbsolutePath());
            // Running in dev mode (target/classes), autostart logs warning and returns safely without creating files
            StartupUtil.setAutostart(true);

            File mockAutostartFile = new File(mockHome, ".config/autostart/Azkar.desktop");
            Assert.assertFalse("Autostart should NOT create desktop file in dev mode", mockAutostartFile.exists());

            File mockAutostartDir = new File(mockHome, ".config/autostart");
            Assert.assertFalse("Autostart directory should NOT be created in dev mode", mockAutostartDir.exists());
        } finally {
            restoreProperty("user.home", origHome);
        }
    }

    private void restoreProperty(String key, String originalValue) {
        if (originalValue != null) {
            System.setProperty(key, originalValue);
        } else {
            System.clearProperty(key);
        }
    }
}
