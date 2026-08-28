package com.bayoumi.util.file;

import com.bayoumi.util.Constants;
import com.bayoumi.util.Logger;
import io.sentry.Sentry;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

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
    public enum DistributionMode { INSTALL4J, STANDALONE_JAR }

    public static class RuntimeProfile {
        public final RuntimeEnvironment environment;
        public final OperatingSystem operatingSystem;
        public final DistributionMode distributionMode;

        public RuntimeProfile(RuntimeEnvironment environment, OperatingSystem operatingSystem,
                              DistributionMode distributionMode) {
            this.environment = environment;
            this.operatingSystem = operatingSystem;
            this.distributionMode = distributionMode;
        }
    }

    /**
     * Immutable context for path resolution, allowing deterministic unit testing.
     */
    public static class AssetsPathContext {
        public final Path installDir;
        public final String userHome;
        public final String localAppData;
        public final RuntimeProfile runtimeProfile;

        public AssetsPathContext(Path installDir, String userHome, String localAppData,
                                 RuntimeProfile runtimeProfile) {
            if (userHome == null || userHome.trim().isEmpty()) {
                throw new IllegalArgumentException("userHome parameter must not be null or empty");
            }
            this.installDir = installDir;
            this.userHome = userHome;
            this.localAppData = localAppData;
            this.runtimeProfile = runtimeProfile;
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
            DistributionMode distributionMode = detectDistributionMode(
                    rawCodeSource,
                    env,
                    System.getProperty("executablepath"),
                    System.getProperty("exe4j.moduleName"));

            OperatingSystem os = com.bayoumi.util.OSUtil.isWindows() ? OperatingSystem.WINDOWS :
                                 com.bayoumi.util.OSUtil.isLinux() ? OperatingSystem.LINUX :
                                 com.bayoumi.util.OSUtil.isMac() ? OperatingSystem.MAC : OperatingSystem.UNKNOWN;

            String userHome = System.getProperty("user.home");
            String localAppData = System.getenv("LOCALAPPDATA");

            RuntimeProfile runtimeProfile = new RuntimeProfile(env, os, distributionMode);
            AssetsPathContext ctx = new AssetsPathContext(installDir, userHome, localAppData, runtimeProfile);
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

    static DistributionMode detectDistributionMode(Path rawLocation, RuntimeEnvironment environment,
                                                   String executablePath, String moduleName) {
        if (environment == RuntimeEnvironment.DEVELOPMENT) {
            return DistributionMode.INSTALL4J;
        }
        if (hasText(executablePath) || hasText(moduleName)) {
            return DistributionMode.INSTALL4J;
        }
        if (rawLocation != null && Files.isRegularFile(rawLocation)
                && rawLocation.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar")) {
            return DistributionMode.STANDALONE_JAR;
        }
        return DistributionMode.INSTALL4J;
    }

    public static Path getAppInstallDir() {
        return resolveAppInstallDir(getRawCodeSourceLocation());
    }

    /**
     * Resolves the assets path using the following decision rules:
     * 1. Development: uses {@code <projectRoot>/jarFiles} without touching user data.
     * 2. Existing writable install-local databases remain in place for compatibility.
     * 3. New standalone JARs use install-local data when the JAR directory is writable.
     * 4. install4j launchers and non-writable standalone JARs use canonical user data.
     */
    public static String resolveAssetsPath(AssetsPathContext ctx) {
        if (ctx.runtimeProfile.environment == RuntimeEnvironment.DEVELOPMENT) {
            return ctx.installDir.resolve("jarFiles").toAbsolutePath().normalize().toString();
        }
        Path canonicalDir = Paths.get(computeUserDataAssetsPath(
                ctx.userHome,
                ctx.localAppData,
                ctx.runtimeProfile.operatingSystem == OperatingSystem.WINDOWS));
        return resolveProductionAssetsPath(ctx, canonicalDir);
    }

    private static String resolveProductionAssetsPath(AssetsPathContext ctx, Path canonicalDir) {
        if (ctx.installDir == null) {
            return canonicalAssetsPath(canonicalDir);
        }

        Path legacyDir = ctx.installDir.resolve("jarFiles").toAbsolutePath().normalize();
        Path legacyDb = legacyDir.resolve("db/data.db");
        if (Files.exists(legacyDb)) {
            return resolveExistingLegacyDatabase(legacyDb, legacyDir, canonicalDir);
        }
        if (ctx.runtimeProfile.distributionMode == DistributionMode.STANDALONE_JAR
                && canUseInstallLocalDirectory(ctx.installDir, legacyDir)) {
            return legacyDir.toString();
        }
        return canonicalAssetsPath(canonicalDir);
    }

    private static String resolveExistingLegacyDatabase(Path legacyDb, Path legacyDir,
                                                        Path canonicalDir) {
        if (Files.isRegularFile(legacyDb) && isLegacyDatabaseWritable(legacyDb, legacyDir)) {
            return legacyDir.toString();
        }
        return canonicalAssetsPath(canonicalDir);
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

    private static boolean isLegacyDatabaseWritable(Path legacyDb, Path legacyDir) {
        return Files.isWritable(legacyDb)
                && isDirectoryActuallyWritable(legacyDb.getParent())
                && isDirectoryActuallyWritable(legacyDir);
    }

    private static boolean canUseInstallLocalDirectory(Path installDir, Path legacyDir) {
        if (Files.exists(legacyDir)) {
            return Files.isDirectory(legacyDir) && isDirectoryActuallyWritable(legacyDir);
        }
        return isDirectoryActuallyWritable(installDir);
    }

    private static String canonicalAssetsPath(Path canonicalDir) {
        Path canonicalDb = canonicalDir.resolve("db/data.db");
        if (Files.exists(canonicalDb) && !Files.isRegularFile(canonicalDb)) {
            throw new IllegalStateException("Canonical database path exists but is not a regular file");
        }
        return canonicalDir.toString();
    }

    private static boolean hasText(String text) {
        return text != null && !text.trim().isEmpty();
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
