package com.bayoumi.controllers.components.audio;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class ChooseAudioController implements Initializable {
    public static MediaPlayer MEDIA_PLAYER;
    private FontAwesomeIconView pauseIcon;
    private FontAwesomeIconView playIcon;
    private String path, fileExtension;
    @FXML
    public JFXComboBox<String> audioBox;
    @FXML
    private JFXButton playButton;

    public static void stopIfPlaying() {
        if (isMediaPlaying()) {
            MEDIA_PLAYER.stop();
        }
    }

    private static boolean isMediaPlaying() {
        return MEDIA_PLAYER != null && MEDIA_PLAYER.getStatus().equals(MediaPlayer.Status.PLAYING);
    }

    public String getValue() {
        if (audioBox == null || audioBox.getValue() == null || audioBox.getValue().equals("")) {
            return "بدون صوت";
        }
        return audioBox.getValue();
    }

    public void setData(String promptText, String initialValue, ObservableList<String> items, String path, String fileExtension) {
        this.path = path;
        this.fileExtension = fileExtension;
        audioBox.setPromptText(promptText);
        audioBox.setValue(initialValue);
        audioBox.setItems(items);
        playButton.setDisable(audioBox.getValue().equals("بدون صوت"));
    }

    public void initFromFirstValue() {
        if (audioBox == null || audioBox.getItems() == null) {
            return;
        }
        audioBox.setValue(audioBox.getItems().size() > 1 ? audioBox.getItems().get(1) : "بدون صوت");
        playButton.setDisable(audioBox.getValue().equals("بدون صوت"));
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        playIcon = new FontAwesomeIconView(FontAwesomeIcon.PLAY);
        playIcon.setStyle("-fx-fill: -fx-secondary;");
        playIcon.setGlyphSize(30);
        pauseIcon = new FontAwesomeIconView(FontAwesomeIcon.PAUSE);
        pauseIcon.setGlyphSize(30);
        pauseIcon.setStyle("-fx-fill: -fx-secondary;");
        audioBox.setOnAction(event -> {
            playButton.setDisable(audioBox.getValue().equals("بدون صوت"));
            if (MEDIA_PLAYER != null && MEDIA_PLAYER.getStatus().equals(MediaPlayer.Status.PLAYING)) {
                MEDIA_PLAYER.stop();
                playButton.setGraphic(playIcon);
                playButton.setPadding(new Insets(5, 14, 5, 8));
            }
        });
    }

    @FXML
    private void play() {
        if (isMediaPlaying()) {
            MEDIA_PLAYER.stop();
            playButton.setGraphic(playIcon);
        } else {
            String fileName = audioBox.getValue();
            System.out.println(fileName);
            if (!fileName.equals("بدون صوت")) {
                MEDIA_PLAYER = new MediaPlayer(new Media(new File(path + fileName + fileExtension).toURI().toString()));
                MEDIA_PLAYER.setVolume(100);
                MEDIA_PLAYER.play();
                // playing
                playButton.setGraphic(pauseIcon);
                playButton.setPadding(new Insets(5, 11, 5, 11));
                MEDIA_PLAYER.setOnEndOfMedia(() -> playButton.setGraphic(playIcon));
            }
        }
    }
}
