package com.bayoumi.util.gui.notfication;

import com.bayoumi.util.Constants;
import com.bayoumi.util.Logger;
import com.bayoumi.util.audio.AudioPlayer;
import com.bayoumi.util.file.AppPathManager;
import com.bayoumi.util.file.FileUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class NotificationAudio {

    public static String PARENT_PATH = "jarFiles/audio/";

    private static final List<String> EXPECTED_SEEDS = Collections.unmodifiableList(Arrays.asList(
            "juntos.mp3",
            "notification01.mp3",
            "notification02.mp3",
            "swiftly.mp3"
    ));

    static {
        PARENT_PATH = Constants.assetsPath + "/audio/";
        try {
            copyAudioFilesToAssetsPath();
        } catch (IOException e) {
            Logger.error(e.getLocalizedMessage(), e, NotificationAudio.class.getName() + ".copyAudioFilesToAssetsPath()");
        }
    }

    private static void copyAudioFilesToAssetsPath() throws IOException {
        File sourceAudioDir = AppPathManager.getAppInstallDir().resolve("jarFiles/audio").toFile();
        String[] dirContents = sourceAudioDir.isDirectory() ? sourceAudioDir.list() : null;
        if (!sourceAudioDir.exists() || !sourceAudioDir.isDirectory() || dirContents == null || dirContents.length == 0) {
            Logger.warn("[NotificationAudio] Source audio directory is missing or empty: " + sourceAudioDir.getAbsolutePath());
        }
        for (String audioFile : EXPECTED_SEEDS) {
            final Path from = AppPathManager.getAppInstallDir().resolve("jarFiles/audio/" + audioFile).toAbsolutePath();
            final Path to = Paths.get(Constants.assetsPath + "/audio/" + audioFile).toAbsolutePath();
            if (!FileUtils.copySeedIfNotExist(from, to)) {
                Logger.warn("[NotificationAudio] Required notification audio seed missing: " + to);
            }
        }
    }


    private final String fileName;
    private final int volume;
    private AudioPlayer audioPlayer = null;

    public NotificationAudio(String fileName, int volume) {
        this.fileName = fileName;
        this.volume = volume;
    }

    public static ObservableList<String> getAudioList() {
        ObservableList<String> audioFiles = FXCollections.observableArrayList("بدون صوت");
        FileUtils.addFilesNameToList(new File(Constants.assetsPath + "/audio"), audioFiles);
        return audioFiles;
    }

    public String getFileName() {
        return fileName;
    }

    public int getVolume() {
        return volume;
    }

    public void play() {
        try {
            if (!fileName.contains("بدون صوت") && !fileName.isEmpty()) {
                audioPlayer = new AudioPlayer(new File(Constants.assetsPath + "/audio/" + fileName));
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
            this.audioPlayer.dispose();
            this.audioPlayer = null;
        }
    }
}
