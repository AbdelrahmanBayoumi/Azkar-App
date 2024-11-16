package com.bayoumi.util.file;

import com.bayoumi.models.Muezzin;
import com.bayoumi.util.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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

    public static void main(String[] args) {
        List<Muezzin> muezzinList = new ArrayList<>();
        for (String s : getAdhanFilesNames()) {
            for (Muezzin muezzin : Muezzin.values()) {
                if (s.equals(muezzin.getFileName())) {
                    muezzinList.add(muezzin);
                }
            }
        }
        muezzinList.forEach(Logger::debug);
    }

    public static List<Muezzin> getAdhanList() {
        List<Muezzin> muezzinList = new ArrayList<>();
        Muezzin tempMuezzin;
        for (String s : getAdhanFilesNames()) {
            tempMuezzin = null;
            for (Muezzin muezzin : Muezzin.values()) {
                if (s.equals(muezzin.getFileName())) {
                    tempMuezzin = muezzin;

                }
            }
            if (tempMuezzin != null) {
                muezzinList.add(tempMuezzin);
            } else if (!s.equals("sharawy_doaa.mp3")) {
                muezzinList.add(new Muezzin(s, s, s));
            }
        }
        return muezzinList;
    }

    public static List<String> getAdhanFilesNames() {
        List<String> audioFiles = new ArrayList<>();
        addFilesNameToList(new File(Muezzin.PARENT_PATH), audioFiles);
        addFilesNameToList(new File(Muezzin.PARENT_PATH_UPLOAD), audioFiles);
        return audioFiles;
    }

    public static String getMuezzinPath(Muezzin muezzin) {
        File local = new File(muezzin.getLocalPath());
        if (local.isFile() && local.exists()) {
            return muezzin.getLocalPath();
        } else {
            return muezzin.getUploadedPath();
        }
    }

    public static File getMuezzinPath(String muezzin) {
        File local = new File(Muezzin.PARENT_PATH + muezzin);
        File localZekr = new File(Muezzin.PARENT_PATH_ZEKR + muezzin);

        if (local.isFile() && local.exists()) {
            return local;
        } else if(localZekr.isFile()&&localZekr.exists()) {
            return localZekr;
        }
        else {

            File upload = new File(Muezzin.PARENT_PATH_UPLOAD + muezzin);
            return upload;
        }
    }

}
