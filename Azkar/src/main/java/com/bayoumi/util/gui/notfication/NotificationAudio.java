package com.bayoumi.util.gui.notfication;

import com.bayoumi.util.Constants;
import com.bayoumi.util.Logger;
import com.bayoumi.util.audio.AudioPlayer;
import com.bayoumi.util.file.AppPathManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class NotificationAudio {

    private final String fileName;
    private final int volume;
    private AudioPlayer audioPlayer = null;

    public NotificationAudio(String fileName, int volume) {
        this.fileName = fileName;
        this.volume = volume;
    }

    private static Path getBundledAudioDir() {
        return AppPathManager.getAppInstallDir().resolve("jarFiles/audio");
    }

    private static Path getUserAudioDir() {
        return Paths.get(Constants.assetsPath + "/audio");
    }

    public static ObservableList<String> getAudioList() {
        return FXCollections.observableArrayList(getAudioList(getBundledAudioDir(), getUserAudioDir()));
    }

    static List<String> getAudioList(Path bundledDir, Path userDir) {
        List<String> audioFiles = new ArrayList<>();
        audioFiles.add("بدون صوت");
        addAudioFilesFromDir(bundledDir, audioFiles);
        addAudioFilesFromDir(userDir, audioFiles);
        return audioFiles;
    }

    private static void addAudioFilesFromDir(Path dir, List<String> list) {
        if (dir != null && Files.isDirectory(dir)) {
            File[] listOfFiles = dir.toFile().listFiles();
            if (listOfFiles != null) {
                for (File file : listOfFiles) {
                    if (file.isFile() && !list.contains(file.getName())) {
                        list.add(file.getName());
                    }
                }
            }
        }
    }

    public static File resolveAudioFile(String fileName) {
        return resolveAudioFile(fileName, getBundledAudioDir(), getUserAudioDir());
    }

    static File resolveAudioFile(String fileName, Path bundledDir, Path userDir) {
        if (fileName == null || fileName.trim().isEmpty() || fileName.contains("بدون صوت")) {
            return null;
        }
        File bundledFile = resolveFileFromDir(fileName, bundledDir);
        if (bundledFile != null) {
            return bundledFile;
        }
        return resolveFileFromDir(fileName, userDir);
    }

    private static File resolveFileFromDir(String fileName, Path dir) {
        if (dir == null || fileName == null || fileName.isEmpty()) {
            return null;
        }
        try {
            if (Paths.get(fileName).isAbsolute()) {
                return null;
            }
            Path normalizedDir = dir.toAbsolutePath().normalize();
            Path candidate = normalizedDir.resolve(fileName).normalize();
            if (!candidate.startsWith(normalizedDir)) {
                return null;
            }
            if (!Files.isRegularFile(candidate)) {
                return null;
            }
            Path realDir = normalizedDir.toRealPath();
            Path realCandidate = candidate.toRealPath();
            if (!realCandidate.startsWith(realDir)) {
                return null;
            }
            return realCandidate.toFile();
        } catch (InvalidPathException | IOException | SecurityException e) {
            return null;
        }
    }

    public String getFileName() {
        return fileName;
    }

    public int getVolume() {
        return volume;
    }

    public void play() {
        try {
            File audioFile = resolveAudioFile(this.fileName, getBundledAudioDir(), getUserAudioDir());
            if (audioFile != null) {
                audioPlayer = new AudioPlayer(audioFile);
                audioPlayer.setVolume(this.volume / 100.0);
                audioPlayer.play();
            }
        } catch (Exception e) {
            Logger.error(null, e, getClass().getName() + ".play()");
        }
    }

    public boolean isPlaying() {
        return audioPlayer != null && audioPlayer.isPlaying();
    }

    public void stop() {
        if (this.audioPlayer != null) {
            this.audioPlayer.stop();
            this.audioPlayer = null;
        }
    }
}
