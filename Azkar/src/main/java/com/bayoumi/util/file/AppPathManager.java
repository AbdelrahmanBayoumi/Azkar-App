package com.bayoumi.util.file;

import com.bayoumi.util.Constants;
import com.bayoumi.util.Logger;
import io.sentry.Sentry;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Resolves application assets and data paths across platforms (Windows, Linux, macOS)
 * and environments (Development vs. Production), ensuring user data safety and isolation.
 */
public class AppPathManager {

    private static String assetsPath;

    private AppPathManager() {
    }

    static {
        init();
    }

    public enum RuntimeEnvironment { DEVELOPMENT, PRODUCTION }
    public enum OperatingSystem { WINDOWS, LINUX, MAC, UNKNOWN }

    /**
     * Immutable context for path resolution, allowing deterministic unit testing.
     */
    public static class AssetsPathContext {
        public final Path installDir;
        public final String userHome;
        public final String localAppData;
        public final RuntimeEnvironment env;
        public final OperatingSystem os;

        public AssetsPathContext(Path installDir, String userHome, String localAppData,
                                 RuntimeEnvironment env, OperatingSystem os) {
            if (userHome == null || userHome.trim().isEmpty()) {
                throw new IllegalArgumentException("userHome parameter must not be null or empty");
            }
            this.installDir = installDir;
            this.userHome = userHome;
            this.localAppData = localAppData;
            this.env = env;
            this.os = os;
        }
    }

    public static synchronized void init() {
        if (assetsPath != null) {
            return;
        }
        try {
            Path rawCodeSource = getRawCodeSourceLocation();
            Path installDir = resolveAppInstallDir(rawCodeSource);
            RuntimeEnvironment env = detectEnvironment(rawCodeSource);

            OperatingSystem os = com.bayoumi.util.OSUtil.isWindows() ? OperatingSystem.WINDOWS :
                                 com.bayoumi.util.OSUtil.isLinux() ? OperatingSystem.LINUX :
                                 com.bayoumi.util.OSUtil.isMac() ? OperatingSystem.MAC : OperatingSystem.UNKNOWN;

            String userHome = System.getProperty("user.home");
            String localAppData = System.getenv("LOCALAPPDATA");

            AssetsPathContext ctx = new AssetsPathContext(installDir, userHome, localAppData, env, os);
            assetsPath = resolveAssetsPath(ctx);

            Path resolvedAssetsPath = Paths.get(assetsPath);
            ensureWritableDirectory(resolvedAssetsPath);
        } catch (Exception ex) {
            Sentry.captureException(ex);
            Logger.error("Failed to initialize AppPathManager: " + ex.getLocalizedMessage(), ex, AppPathManager.class.getName() + " -> init");
            throw new IllegalStateException("AppPathManager initialization failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * Tests if a directory is actually writable by creating and deleting a temporary probe file.
     * Avoids unreliable {@link Files#isWritable(Path)} results on Windows NTFS ACLs.
     */
    public static boolean isDirectoryActuallyWritable(Path dir) {
        if (dir == null || !Files.exists(dir)) {
            return false;
        }
        Path testFile = null;
        try {
            testFile = Files.createTempFile(dir, ".azkar_probe_", ".tmp");
            Files.write(testFile, new byte[]{1});
            Files.delete(testFile);
            return true;
        } catch (IOException | SecurityException e) {
            return false;
        } finally {
            if (testFile != null) {
                try {
                    Files.deleteIfExists(testFile);
                } catch (IOException | SecurityException e) {
                    Logger.warn("Failed to delete write probe temp file: " + testFile + " (" + e.getMessage() + ")");
                }
            }
        }
    }

    static Path getRawCodeSourceLocation() {
        try {
            URI uri = AppPathManager.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            return Paths.get(uri).toAbsolutePath().normalize();
        } catch (Exception e) {
            String install4jExe = System.getProperty("executablepath");
            if (install4jExe != null && !install4jExe.trim().isEmpty()) {
                return Paths.get(install4jExe).toAbsolutePath().normalize();
            }

            String exe4jModule = System.getProperty("exe4j.moduleName");
            if (exe4jModule != null && !exe4jModule.trim().isEmpty()) {
                return Paths.get(exe4jModule).toAbsolutePath().normalize();
            }

            Logger.error("Failed to determine application raw code source: " + e.getLocalizedMessage(), e, AppPathManager.class.getName());
            throw new IllegalStateException("Failed to determine application code source location", e);
        }
    }

    static Path resolveAppInstallDir(Path rawLocation) {
        if (rawLocation == null) {
            return null;
        }
        Path path = Files.isRegularFile(rawLocation) ? rawLocation.getParent() : rawLocation;
        Path devRoot = findDevelopmentProjectRoot(path);
        if (devRoot != null) {
            return devRoot;
        }
        return path;
    }

    static RuntimeEnvironment detectEnvironment(Path rawLocation) {
        if (rawLocation == null) {
            return RuntimeEnvironment.PRODUCTION;
        }
        Path path = Files.isRegularFile(rawLocation) ? rawLocation.getParent() : rawLocation;
        return findDevelopmentProjectRoot(path) != null ? RuntimeEnvironment.DEVELOPMENT : RuntimeEnvironment.PRODUCTION;
    }

    public static Path getAppInstallDir() {
        return resolveAppInstallDir(getRawCodeSourceLocation());
    }

    /**
     * Resolves the assets path using the following decision rules:
     * 1. Development: uses {@code <projectRoot>/jarFiles} without touching user data.
     * 2. Production: uses canonical user data path (e.g. %LOCALAPPDATA%/Azkar or ~/.Azkar),
     *    falling back to legacy install-local data only if canonical DB does not exist
     *    and the legacy DB is confirmed writable.
     */
    public static String resolveAssetsPath(AssetsPathContext ctx) {
        if (ctx.env == RuntimeEnvironment.DEVELOPMENT) {
            return ctx.installDir.resolve("jarFiles").toAbsolutePath().normalize().toString();
        }
        String canonicalPath = computeUserDataAssetsPath(ctx.userHome, ctx.localAppData, ctx.os == OperatingSystem.WINDOWS);
        if (ctx.installDir != null) {
            Path canonicalDb = Paths.get(canonicalPath).resolve("db/data.db");
            if (Files.exists(canonicalDb)) {
                return canonicalPath;
            }
            Path legacyJarFilesDir = ctx.installDir.resolve("jarFiles").toAbsolutePath().normalize();
            Path legacyDb = legacyJarFilesDir.resolve("db/data.db");
            if (Files.isRegularFile(legacyDb)
                    && Files.isWritable(legacyDb)
                    && isDirectoryActuallyWritable(legacyDb.getParent())
                    && isDirectoryActuallyWritable(legacyJarFilesDir)) {
                return legacyJarFilesDir.toString();
            }
        }
        return canonicalPath;
    }

    public static String computeUserDataAssetsPath(String userHome, String localAppData, boolean isWindows) {
        if (userHome == null || userHome.trim().isEmpty()) {
            throw new IllegalArgumentException("userHome parameter must not be null or empty");
        }
        String baseDir;
        if (isWindows) {
            if (localAppData != null && !localAppData.trim().isEmpty()) {
                baseDir = localAppData + "/" + Constants.APP_NAME + "/jarFiles";
            } else {
                baseDir = userHome + "/AppData/Local/" + Constants.APP_NAME + "/jarFiles";
            }
        } else {
            baseDir = userHome + "/." + Constants.APP_NAME + "/jarFiles";
        }
        return Paths.get(baseDir).toAbsolutePath().normalize().toString();
    }

    public static String computeUserDataAssetsPath(String userHome, String localAppData) {
        return computeUserDataAssetsPath(userHome, localAppData, com.bayoumi.util.OSUtil.isWindows());
    }

    private static void ensureWritableDirectory(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            Logger.error("Failed to create assets directory: " + dir, e, AppPathManager.class.getName());
            throw new IllegalStateException("Cannot create assets directory: " + dir, e);
        }
        if (!isDirectoryActuallyWritable(dir)) {
            throw new IllegalStateException("Assets directory exists but is not actually writable: " + dir);
        }
    }

    static Path findDevelopmentProjectRoot(Path current) {
        Path curr = current;
        while (curr != null) {
            if (curr.getFileName() != null && curr.getFileName().toString().equals("target")) {
                Path projectRoot = curr.getParent();
                if (projectRoot != null && (Files.exists(projectRoot.resolve("pom.xml")) || Files.exists(projectRoot.resolve("jarFiles")))) {
                    return projectRoot.toAbsolutePath().normalize();
                }
            }
            curr = curr.getParent();
        }
        return null;
    }

    public static String getAssetsPath() {
        if (assetsPath == null) {
            init();
        }
        return assetsPath;
    }
}
