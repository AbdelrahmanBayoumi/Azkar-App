package com.bayoumi.util.file;

import com.bayoumi.util.Constants;
import com.bayoumi.util.Logger;
import io.sentry.Sentry;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AppPathManager {

    private static String assetsPath;
    private static boolean assetsPathChanged = false;

    private AppPathManager() {
    }

    static {
        init();
    }

    public static class AssetsPathContext {
        public final Path installDir;
        public final boolean jarFilesExists;
        public final boolean isJarFilesWritable;
        public final boolean isInstallDirWritable;
        public final String userHome;
        public final String localAppData;

        public AssetsPathContext(Path installDir, boolean jarFilesExists, boolean isJarFilesWritable, boolean isInstallDirWritable, String userHome, String localAppData) {
            if (userHome == null || userHome.trim().isEmpty()) {
                throw new IllegalArgumentException("userHome parameter must not be null or empty");
            }
            this.installDir = installDir;
            this.jarFilesExists = jarFilesExists;
            this.isJarFilesWritable = isJarFilesWritable;
            this.isInstallDirWritable = isInstallDirWritable;
            this.userHome = userHome;
            this.localAppData = localAppData;
        }
    }

    public static synchronized void init() {
        if (assetsPath != null) {
            return;
        }
        try {
            Path installDir = getAppInstallDir();
            Path jarFilesDir = installDir.resolve("jarFiles").toAbsolutePath().normalize();

            boolean jarFilesExists = Files.exists(jarFilesDir);
            boolean isJarFilesWritable = jarFilesExists && Files.isWritable(jarFilesDir);
            boolean isInstallDirWritable = Files.isWritable(installDir);

            String userHome = System.getProperty("user.home");
            String localAppData = System.getenv("LOCALAPPDATA");

            AssetsPathContext ctx = new AssetsPathContext(installDir, jarFilesExists, isJarFilesWritable, isInstallDirWritable, userHome, localAppData);
            assetsPath = resolveAssetsPath(ctx);

            Path resolvedPath = Paths.get(assetsPath);
            if (!resolvedPath.equals(jarFilesDir)) {
                assetsPathChanged = true;
                ensureWritableDirectory(resolvedPath);
            } else {
                assetsPathChanged = false;
                if (!jarFilesExists) {
                    ensureWritableDirectory(jarFilesDir);
                }
            }
        } catch (Exception ex) {
            Sentry.captureException(ex);
            Logger.error("Failed to initialize AppPathManager: " + ex.getLocalizedMessage(), ex, AppPathManager.class.getName() + " -> init");
            throw new IllegalStateException("AppPathManager initialization failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * Pure decision function for resolving the assets path using AssetsPathContext.
     * Performs NO I/O and accesses NO global state.
     * Guaranteed to return an ABSOLUTE path string.
     */
    public static String resolveAssetsPath(AssetsPathContext ctx) {
        Path jarFilesDir = ctx.installDir.resolve("jarFiles").toAbsolutePath().normalize();

        if (ctx.jarFilesExists && ctx.isJarFilesWritable) {
            return jarFilesDir.toString();
        }
        if (!ctx.jarFilesExists && ctx.isInstallDirWritable) {
            return jarFilesDir.toString();
        }

        return computeUserDataAssetsPath(ctx.userHome, ctx.localAppData);
    }

    /**
     * Computes the User Data assets path without performing any I/O or accessing global state.
     * Throws IllegalArgumentException if userHome is null or empty.
     */
    public static String computeUserDataAssetsPath(String userHome, String localAppData) {
        if (userHome == null || userHome.trim().isEmpty()) {
            throw new IllegalArgumentException("userHome parameter must not be null or empty");
        }
        String baseDir;
        if (localAppData != null && !localAppData.trim().isEmpty()) {
            baseDir = localAppData + "/" + Constants.APP_NAME + "/jarFiles";
        } else {
            baseDir = userHome + "/." + Constants.APP_NAME + "/jarFiles";
        }
        return Paths.get(baseDir).toAbsolutePath().normalize().toString();
    }

    private static void ensureWritableDirectory(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            Logger.error("Failed to create assets directory: " + dir, e, AppPathManager.class.getName());
            throw new IllegalStateException("Cannot create assets directory: " + dir, e);
        }
        if (!Files.isWritable(dir)) {
            throw new IllegalStateException("Assets directory exists but is not writable: " + dir);
        }
    }

    public static Path getAppInstallDir() {
        try {
            URI uri = AppPathManager.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            Path path = Paths.get(uri).toAbsolutePath().normalize();
            if (Files.isRegularFile(path)) {
                path = path.getParent();
            }

            Path devPath = findDevelopmentProjectRoot(path);
            if (devPath != null) {
                return devPath;
            }

            return path;
        } catch (Exception e) {
            String install4jExe = System.getProperty("executablepath");
            if (install4jExe != null && !install4jExe.trim().isEmpty()) {
                Path p = Paths.get(install4jExe).toAbsolutePath().normalize();
                return Files.isRegularFile(p) ? p.getParent() : p;
            }

            String exe4jModule = System.getProperty("exe4j.moduleName");
            if (exe4jModule != null && !exe4jModule.trim().isEmpty()) {
                Path p = Paths.get(exe4jModule).toAbsolutePath().normalize();
                return Files.isRegularFile(p) ? p.getParent() : p;
            }

            Logger.error("Failed to determine application install dir: " + e.getLocalizedMessage(), e, AppPathManager.class.getName());
            throw new IllegalStateException("Failed to determine application installation directory", e);
        }
    }

    public static Path findDevelopmentProjectRoot(Path current) {
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

    public static boolean isAssetsPathChanged() {
        return assetsPathChanged;
    }
}
