package com.bayoumi.util.gui.notfication;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.io.File;

public class NotificationAudio {
    private final String fileName;
    private final int volume;
    private MediaPlayer mediaPlayer = null;

    public NotificationAudio(String fileName, int volume) {
        this.fileName = fileName;
        this.volume = volume;
    }


    public String getFileName() {
        return fileName;
    }

    public int getVolume() {
        return volume;
    }

    public void play() {
        if (!fileName.contains("بدون صوت") && !fileName.equals("")) {
            mediaPlayer = new MediaPlayer(new Media(new File("jarFiles/audio/" + fileName).toURI().toString()));
            mediaPlayer.setVolume(this.volume / 100.0);
            mediaPlayer.play();
        }
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    public void stop() {
        this.mediaPlayer.stop();
        this.mediaPlayer.dispose();
        this.mediaPlayer = null;
    }
}
