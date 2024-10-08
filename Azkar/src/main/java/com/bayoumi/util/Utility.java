package com.bayoumi.util;

import com.bayoumi.Launcher;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;

/**
 * Helper methods
 */
public class Utility {

    public static void printAllRunningThreads() {
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        threadSet.forEach(thread -> Logger.debug(thread.getName()));
    }

    public static String toUTF(String val) {
        try {
            return new String(val.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            Logger.error(null, ex, com.bayoumi.util.time.Utilities.class.getName() + ".toUTF()");
        }
        return val;
    }

    /**
     * this method is used in exiting the program
     */
    public static void exitProgramAction() {
//        printAllRunningThreads();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Long exitTime = System.currentTimeMillis();
            Logger.info("App closed - Used for "
                    + (exitTime - Launcher.startTime) + " ms\n");
        }));
        System.exit(0);
    }

    /**
     * copy text to Clipboard
     *
     * @param text to be copied
     */
    public static void copyToClipboard(String text) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(text), null);
    }

    /**
     * @param path where directory needed to be created
     */
    public static void createDirectory(String path) {
        try {
            Files.createDirectories(Paths.get(path));
        } catch (Exception e) {
            Logger.error(null, e, Utility.class.getName() + ".createDirectory()");
        }
    }

    /**
     * Copy file from source to destination if the destination file is not exist or not a directory
     * if the destination file is exist and not a directory the file will not be copied
     *
     * @param from source path to copy from
     * @param to   destination path to copy to
     * @throws IOException if the file is not found or can't be read
     */
    public static void copyIfNotExist(Path from, Path to) throws IOException {
        File f = new File(to.toString());
        if (f.exists() && !f.isDirectory()) {
            return;
        }
        Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * @param i number
     * @return number as String formatted as two digits
     */
    public static String formatIntToTwoDigit(int i) {
        return String.format("%02d", i);
    }

    public static double formatNum(double num) {
        double returnedVal = Double.parseDouble(String.format("%.3f", num));
        return Math.abs(returnedVal) == 0 ? 0 : returnedVal;
    }

}
