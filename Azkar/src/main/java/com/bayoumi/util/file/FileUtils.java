package com.bayoumi.util.file;

import com.bayoumi.util.Logger;
import com.bayoumi.util.Utility;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Properties;

public class FileUtils {

    /**
     * Add files names From the given folder to the given list
     *
     * @param folder the folder that contains the files
     * @param list   the list that will contain the files names
     */
    public static void addFilesNameToList(File folder, List<String> list) {
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                if (file.isFile()) {
                    list.add(file.getName());
                }
            }
        }
    }

    public static String removeExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return fileName;
        }
        return fileName.substring(0, fileName.lastIndexOf('.'));
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
     * Shared seed validation rule: when source and destination differ and source exists,
     * copy without overwriting.
     *
     * @param from source path
     * @param to   destination path
     * @return true if destination exists after copy attempt, false otherwise
     * @throws IOException if copy fails
     */
    public static boolean copySeedIfNotExist(Path from, Path to) throws IOException {
        if (!from.equals(to) && Files.exists(from)) {
            copyIfNotExist(from, to);
        }
        return Files.exists(to);
    }

    public static Properties getConfig() throws Exception {
        final Properties properties = new Properties();
        try (InputStream input = FileUtils.class.getResourceAsStream("/config.properties")) {
            if (input == null) {
                throw new Exception("Could not find config.properties");
            }
            properties.load(input);
            return properties;
        }
    }
}
