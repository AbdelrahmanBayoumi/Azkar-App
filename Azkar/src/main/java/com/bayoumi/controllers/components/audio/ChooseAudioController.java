package com.bayoumi.controllers.components.audio;

import com.bayoumi.models.Muezzin;
import com.bayoumi.models.settings.Language;
import com.bayoumi.models.settings.LanguageBundle;
import com.bayoumi.models.settings.Settings;
import com.bayoumi.util.Logger;
import com.bayoumi.util.Utility;
import com.bayoumi.util.gui.BuilderUI;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class ChooseAudioController implements Initializable {
    public static MediaPlayer MEDIA_PLAYER;
    private FontAwesomeIconView pauseIcon;
    private FontAwesomeIconView playIcon;
    @FXML
    public JFXComboBox<Muezzin> audioBox;
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

    public Muezzin getValue() {
        if (audioBox == null || audioBox.getValue() == null || audioBox.getValue().equals(Muezzin.NO_SOUND)) {
            return Muezzin.NO_SOUND;
        }
        return audioBox.getValue();
    }

    public void setData(String promptText, Muezzin initialValue, List<Muezzin> items) {
        audioBox.setPromptText(promptText);
        audioBox.setValue(initialValue);
        audioBox.setItems(FXCollections.observableArrayList(items));
        audioBox.getItems().add(Muezzin.NO_SOUND);
        playButton.setDisable(audioBox.getValue().equals(Muezzin.NO_SOUND));
        if (Settings.getInstance().getLanguage().equals(Language.Arabic)) {
            audioBox.setConverter(Muezzin.arabicConverter());
        } else {
            audioBox.setConverter(Muezzin.englishConverter());
        }
    }

    public void initFromFirstValue() {
        if (audioBox == null || audioBox.getItems() == null) {
            return;
        }
        audioBox.setValue(audioBox.getItems().size() > 1 ? audioBox.getItems().get(0) : Muezzin.NO_SOUND);
        playButton.setDisable(audioBox.getValue().equals(Muezzin.NO_SOUND));
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
            playButton.setDisable(audioBox.getValue().equals(Muezzin.NO_SOUND));
            if (MEDIA_PLAYER != null && MEDIA_PLAYER.getStatus().equals(MediaPlayer.Status.PLAYING)) {
                MEDIA_PLAYER.stop();
                playButton.setGraphic(playIcon);
                playButton.setPadding(new Insets(5, 14, 5, 8));
            }
            Settings.getInstance().getPrayerTimeSettings().setAdhanAudio(getValue().getFileName());
        });
    }

    @FXML
    private void play() {
        if (isMediaPlaying()) {
            MEDIA_PLAYER.stop();
            playButton.setGraphic(playIcon);
        } else {
            Muezzin muezzin = audioBox.getValue();
            Logger.debug(muezzin);
            if (!muezzin.equals(Muezzin.NO_SOUND)) {
                try {
                    MEDIA_PLAYER = new MediaPlayer(new Media(new File(muezzin.getPath()).toURI().toString()));
                } catch (Exception e) {
                    Logger.error(null, e, getClass().getName() + ".play()");
                    ResourceBundle bundle = LanguageBundle.getInstance().getResourceBundle();
                    BuilderUI.showOkAlert(Alert.AlertType.ERROR, Utility.toUTF(bundle.getString("errorPlayingAudio")), Utility.toUTF(bundle.getString("dir")).equals("rtl"));
                    return;
                }
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
