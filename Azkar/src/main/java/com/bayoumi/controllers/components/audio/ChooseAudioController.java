package com.bayoumi.controllers.components.audio;

import com.bayoumi.models.Muezzin;
import com.bayoumi.models.settings.AzkarSettings;
import com.bayoumi.models.settings.Language;
import com.bayoumi.models.settings.LanguageBundle;
import com.bayoumi.models.settings.Settings;
import com.bayoumi.util.Constants;
import com.bayoumi.util.Logger;
import com.bayoumi.util.Utility;
import com.bayoumi.util.file.FileUtils;
import com.bayoumi.util.gui.BuilderUI;
import com.bayoumi.util.gui.PopOverUtil;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXSlider;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.jensd.fx.glyphs.octicons.OctIcon;
import de.jensd.fx.glyphs.octicons.OctIconView;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.layout.HBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.ResourceBundle;

public class ChooseAudioController implements Initializable {
    public static MediaPlayer MEDIA_PLAYER;
    private AzkarSettings azkarSettings;
    private FontAwesomeIconView pauseIcon;
    private FontAwesomeIconView playIcon;
    private double previousValue = 50;
    private boolean isMuted = false;

    // ======= FXML =======
    @FXML
    private HBox prayerVolumeBox;
    @FXML
    private JFXSlider prayerVolumeSlider;
    @FXML
    private OctIconView volume;
    @FXML
    public JFXComboBox<Muezzin> audioBox;
    @FXML
    private JFXButton playButton;
    @FXML
    private JFXButton uploadButton;

    public static boolean stopIfPlaying() {
        final boolean isMediaPlaying = isMediaPlaying();
        if (isMediaPlaying) {
            MEDIA_PLAYER.stop();
            MEDIA_PLAYER.dispose(); // Release the resources
            MEDIA_PLAYER = null;   // Remove reference
        }
        return isMediaPlaying;
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

    private void setMuezzins() {
        audioBox.setItems(FXCollections.observableArrayList(FileUtils.getAdhanList()));
        audioBox.getItems().add(Muezzin.NO_SOUND);
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

        prayerVolumeSlider.setValue(azkarSettings.getPrayerVolume());
        prayerVolumeBox.setDisable(audioBox.getValue().equals(Muezzin.NO_SOUND));
        if (azkarSettings.getPrayerVolume() == 0) {
            volume.setIcon(OctIcon.MUTE);
        } else {
            volume.setIcon(OctIcon.UNMUTE);
        }
    }

    public void initFromFirstValue() {
        if (audioBox == null || audioBox.getItems() == null) {
            return;
        }
        audioBox.setValue(audioBox.getItems().size() > 1 ? audioBox.getItems().get(0) : Muezzin.NO_SOUND);
        Settings.getInstance().getPrayerTimeSettings().setAdhanAudio(getValue().getFileName());
        playButton.setDisable(audioBox.getValue().equals(Muezzin.NO_SOUND));
        prayerVolumeBox.setDisable(audioBox.getValue().equals(Muezzin.NO_SOUND));
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        azkarSettings = Settings.getInstance().getAzkarSettings();
        playIcon = new FontAwesomeIconView(FontAwesomeIcon.PLAY);
        playIcon.setStyle("-fx-fill: -fx-reverse-secondary;");
        playIcon.setGlyphSize(30);
        pauseIcon = new FontAwesomeIconView(FontAwesomeIcon.PAUSE);
        pauseIcon.setGlyphSize(30);
        pauseIcon.setStyle("-fx-fill: -fx-reverse-secondary;");
        audioBox.setOnAction(event -> {
            playButton.setDisable(Muezzin.NO_SOUND.equals(audioBox.getValue()));
            prayerVolumeBox.setDisable(Muezzin.NO_SOUND.equals(audioBox.getValue()));
            if (stopIfPlaying()) {
                setPlayIcon();
            }
            Settings.getInstance().getPrayerTimeSettings().setAdhanAudio(getValue().getFileName());
        });

        PopOverUtil.init(uploadButton, Utility.toUTF(LanguageBundle.getInstance().getResourceBundle().getString("uploadNewAudioTooltip")));

        prayerVolumeSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            azkarSettings.setPrayerVolume((int) prayerVolumeSlider.getValue());
            playButton.requestFocus();
            previousValue = (double) oldValue;
            // multiply duration by percentage calculated by
            // slider position
            if (prayerVolumeSlider.getValue() > 0) {
                volume.setIcon(OctIcon.UNMUTE);
            } else if (prayerVolumeSlider.getValue() == 0) {
                volume.setIcon(OctIcon.MUTE);
            }
            if (null != MEDIA_PLAYER) {
                MEDIA_PLAYER.setVolume(azkarSettings.getPrayerVolume() / 100.0);
            }
        });
    }

    @FXML
    private void uploadAudio() {
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        // TODO: support & test other audio formats
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Audio Files", "*.mp3")
        );
        final File selectedFile = fileChooser.showOpenDialog(uploadButton.getScene().getWindow());
        if (selectedFile != null && selectedFile.isFile()) {
            Path audioSourcePath = selectedFile.toPath();
            System.out.println("Path:" + audioSourcePath);
            String audioTargetPath = Constants.assetsPath + "/audio/adhan/" + selectedFile.getName();
            System.out.println("audioTargetPath:" + audioTargetPath);
            Path path = Paths.get(audioTargetPath);
            System.out.println("audioTargetPath path:" + path);

            try {
                Files.copy(audioSourcePath, path, StandardCopyOption.REPLACE_EXISTING);
                setMuezzins();
                Muezzin newMuezzin = audioBox.getItems().stream()
                        .filter(muezzin -> muezzin.getFileName().equals(selectedFile.getName())).findAny()
                        .orElse(Muezzin.NO_SOUND);
                audioBox.setValue(newMuezzin);
            } catch (IOException e) {
                Logger.error(null, e, getClass().getName() + ".uploadAudio()");
                final ResourceBundle bundle = LanguageBundle.getInstance().getResourceBundle();
                BuilderUI.showOkAlert(Alert.AlertType.ERROR, Utility.toUTF(bundle.getString("errorUploadAudio")), bundle);
            }
        }
    }

    @FXML
    private void play() {
        if (stopIfPlaying()) {
            setPlayIcon();
        } else {
            final Muezzin muezzin = audioBox.getValue();
            Logger.debug(muezzin);
            if (!muezzin.equals(Muezzin.NO_SOUND)) {
                try {
                    MEDIA_PLAYER = new MediaPlayer(new Media(new File(muezzin.getPath()).toURI().toString()));
                } catch (Exception e) {
                    Logger.error(null, e, getClass().getName() + ".play()");
                    final ResourceBundle bundle = LanguageBundle.getInstance().getResourceBundle();
                    BuilderUI.showOkAlert(Alert.AlertType.ERROR, Utility.toUTF(bundle.getString("errorPlayingAudio")), bundle);
                    return;
                }
                MEDIA_PLAYER.setVolume(prayerVolumeSlider.getValue() / 100.0);
                MEDIA_PLAYER.setOnEndOfMedia(() -> playButton.setGraphic(playIcon));
                MEDIA_PLAYER.setOnStopped(() -> playButton.setGraphic(playIcon));
                MEDIA_PLAYER.play();
                setPauseIcon();
            }
        }
    }

    private void setPlayIcon() {
        playButton.setGraphic(playIcon);
        playButton.setPadding(new Insets(5, 14, 5, 8));
    }

    private void setPauseIcon() {
        playButton.setGraphic(pauseIcon);
        playButton.setPadding(new Insets(5, 11, 5, 11));
    }

    @FXML
    private void muteUnmute() {
        if (isMuted) {
            // umMute
            isMuted = false;
            prayerVolumeSlider.setValue(previousValue);
        } else {
            // mute
            isMuted = true;
            previousValue = prayerVolumeSlider.getValue();
            prayerVolumeSlider.setValue(0);
        }
    }
}
