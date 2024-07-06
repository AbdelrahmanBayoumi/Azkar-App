package com.bayoumi.util;

public class VersionComparator {
    public static boolean isNewerVersion(String latestVersion, String storedVersion) {
        latestVersion = stripPrefix(latestVersion, "v");
        storedVersion = stripPrefix(storedVersion, "v");

        String[] latestParts = latestVersion.split("-", 2);
        String[] storedParts = storedVersion.split("-", 2);

        String[] latestNumbers = latestParts[0].split("\\.");
        String[] storedNumbers = storedParts[0].split("\\.");

        for (int i = 0; i < Math.max(latestNumbers.length, storedNumbers.length); i++) {
            int latestNumber = (i < latestNumbers.length) ? Integer.parseInt(latestNumbers[i]) : 0;
            int storedNumber = (i < storedNumbers.length) ? Integer.parseInt(storedNumbers[i]) : 0;

            if (latestNumber > storedNumber) {
                return true;
            } else if (latestNumber < storedNumber) {
                return false;
            }
        }

        // If numeric parts are equal, compare pre-release labels
        if (latestParts.length == 1 && storedParts.length == 1) {
            return false; // Both are release versions and equal
        } else if (latestParts.length == 1) {
            return true; // latestVersion is a release version, storedVersion is a pre-release
        } else if (storedParts.length == 1) {
            return false; // latestVersion is a pre-release, storedVersion is a release version
        } else {
            // Both are pre-releases, compare lexicographically
            return latestParts[1].compareTo(storedParts[1]) > 0;
        }
    }

    private static String stripPrefix(String version, String prefix) {
        if (version.startsWith(prefix)) {
            return version.substring(prefix.length());
        }
        return version;
    }
}
