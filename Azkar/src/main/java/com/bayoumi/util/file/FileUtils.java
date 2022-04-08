package com.bayoumi.util.file;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;
import java.util.List;

public class FileUtils {

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

    public static ObservableList<String> getAdhanList() {
        // TODO: localization for adhan name
        ObservableList<String> audioFiles = FXCollections.observableArrayList("بدون صوت");
        addFilesNameToListRemoveExtension(new File("jarFiles/audio/adhan"), audioFiles, ".mp3");
        return audioFiles;
    }
}
