package com.bayoumi.util.file;

import java.io.File;

public class DirectoryDeleter {

    public static boolean deleteDirectory(String dirPath) {
        File dir = new File(dirPath);

        // Check if the directory exists
        if (!dir.exists()) {
            System.out.println("Directory does not exist: " + dirPath);
            return false;
        }

        // Check if it's a directory
        if (!dir.isDirectory()) {
            System.out.println("Path is not a directory: " + dirPath);
            return false;
        }

        // Recursively delete directory contents
        if (!deleteContents(dir)) {
            System.out.println("Failed to delete contents of the directory: " + dirPath);
            return false;
        }

        // Delete the directory itself
        boolean deleted = dir.delete();
        if (!deleted) {
            System.out.println("Failed to delete the directory: " + dirPath);
        }

        return deleted;
    }

    private static boolean deleteContents(File file) {
        File[] files = file.listFiles();

        if (files != null) { // Check for null in case of an I/O error
            for (File f : files) {
                if (f.isDirectory()) {
                    // Recursively delete subdirectories
                    if (!deleteContents(f)) {
                        return false;
                    }
                }
                // Attempt to delete the file or directory
                if (!f.delete()) {
                    System.out.println("Failed to delete file (possibly write-protected): " + f.getAbsolutePath());
                    return false;
                }
            }
        }

        return true;
    }
}
