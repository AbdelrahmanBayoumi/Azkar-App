package com.bayoumi.util.gui.notfication;

import com.bayoumi.util.Logger;
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
        try {
            if (!fileName.contains("بدون صوت") && !fileName.isEmpty()) {
                mediaPlayer = new MediaPlayer(new Media(new File("jarFiles/audio/" + fileName).toURI().toString()));
                mediaPlayer.setVolume(this.volume / 100.0);
                mediaPlayer.play();
            }
        } catch (Exception e) {
            Logger.error(null, e, getClass().getName() + ".play()");
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
