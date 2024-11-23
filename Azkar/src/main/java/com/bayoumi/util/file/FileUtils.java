package com.bayoumi.util.file;

import com.bayoumi.models.Muezzin;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    public static void addFilesNameToListRemoveExtension(File folder, List<String> list, String extension) {
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                if (file.isFile()) {
                    list.add(file.getName().replace(extension, ""));
                }
            }
        }
    }

    public static ObservableList<String> getAudioList() {
        ObservableList<String> audioFiles = FXCollections.observableArrayList("بدون صوت");
        addFilesNameToList(new File("jarFiles/audio"), audioFiles);
        return audioFiles;
    }

    public static String removeExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return fileName;
        }
        return fileName.substring(0, fileName.lastIndexOf('.'));
    }


    public static List<Muezzin> getAdhanList() {
        // Index Muezzin instances by fileName for quick lookup
        Map<String, Muezzin> muezzinMap = Muezzin.values().stream()
                .collect(Collectors.toMap(Muezzin::getFileName, muezzin -> muezzin));

        // Build the list of Muezzin objects
        return getAdhanFilesNames().stream()
                .map(fileName -> muezzinMap.getOrDefault(fileName, new Muezzin(removeExtension(fileName), removeExtension(fileName), fileName)))
                .collect(Collectors.toList());
    }


    public static List<String> getAdhanFilesNames() {
        List<String> audioFiles = new ArrayList<>();
        addFilesNameToList(new File(Muezzin.PARENT_PATH), audioFiles);
        return audioFiles;
    }
}
