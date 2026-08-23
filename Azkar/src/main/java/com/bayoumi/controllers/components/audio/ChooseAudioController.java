package com.bayoumi.controllers.components.audio;

import com.bayoumi.models.Muezzin;
import com.bayoumi.models.settings.AzkarSettings;
import com.bayoumi.models.settings.Language;
import com.bayoumi.models.settings.LanguageBundle;
import com.bayoumi.models.settings.Settings;
import com.bayoumi.util.Constants;
import com.bayoumi.util.Logger;
import com.bayoumi.util.Utility;
import com.bayoumi.util.gui.BuilderUI;
import com.bayoumi.util.gui.PopOverUtil;
import com.bayoumi.util.gui.button.TableViewButton;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXSlider;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.jensd.fx.glyphs.octicons.OctIcon;
import de.jensd.fx.glyphs.octicons.OctIconView;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
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
    private HBox prayerVolumeBox, root;
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
        audioBox.setItems(FXCollections.observableArrayList(Muezzin.getAdhanList()));
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
        configureAudioBoxCells();

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

        root.setDisable(Settings.getInstance().getPrayerTimeSettings().isPrayersReminderStopped());
        Settings.getInstance().getPrayerTimeSettings().addObserver((o, arg) ->
                root.setDisable(Settings.getInstance().getPrayerTimeSettings().isPrayersReminderStopped()));
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
            String audioTargetPath = Constants.assetsPath + "/audio/adhan/" + selectedFile.getName();
            Path path = Paths.get(audioTargetPath);

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

    private void configureAudioBoxCells() {
        audioBox.setCellFactory(listView -> new ListCell<Muezzin>() {
            private final Label nameLabel = new Label();
            private final Region spacer = new Region();
            private final TableViewButton deleteBtn = new TableViewButton("", new FontAwesomeIconView(FontAwesomeIcon.TRASH));
            private final HBox row = new HBox(8, nameLabel, spacer, deleteBtn);

            {
                HBox.setHgrow(spacer, Priority.ALWAYS);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setMaxWidth(Double.MAX_VALUE);
                row.setPadding(new Insets(0, 4, 0, 8));
                deleteBtn.setFocusTraversable(false);
                deleteBtn.getStyleClass().add("adhan-delete-btn");
                deleteBtn.setRipplerFill(Color.web("#c91a29"));
                deleteBtn.setTooltip(createDeleteTooltip());
                deleteBtn.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                    event.consume();
                    final Muezzin item = getItem();
                    if (item != null && item.isCustom()) {
                        deleteAudio(item);
                    }
                });
                deleteBtn.addEventFilter(MouseEvent.MOUSE_RELEASED, Event::consume);
                deleteBtn.addEventFilter(MouseEvent.MOUSE_CLICKED, Event::consume);
            }

            @Override
            protected void updateItem(Muezzin item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                nameLabel.setText(muezzinDisplayName(item));
                final boolean custom = item.isCustom();
                deleteBtn.setVisible(custom);
                deleteBtn.setManaged(custom);
                setText(null);
                setGraphic(row);
            }
        });
    }

    private Tooltip createDeleteTooltip() {
        final Tooltip tooltip = new Tooltip(Utility.toUTF(
                LanguageBundle.getInstance().getResourceBundle().getString("deleteAudioTooltip")));
        tooltip.getStyleClass().add("adhan-delete-tooltip");
        tooltip.setOnShown(event -> {
            if (tooltip.getScene() != null && audioBox.getScene() != null) {
                tooltip.getScene().getStylesheets().setAll(audioBox.getScene().getStylesheets());
            }
        });
        return tooltip;
    }

    private String muezzinDisplayName(Muezzin muezzin) {
        if (muezzin == null) {
            return "";
        }
        if (Settings.getInstance().getLanguage().equals(Language.Arabic)) {
            return muezzin.getArabicName();
        }
        return muezzin.getEnglishName();
    }

    private void deleteAudio(Muezzin target) {
        if (target == null || !target.isCustom()) {
            return;
        }
        audioBox.hide();
        Platform.runLater(() -> {
            final ResourceBundle bundle = LanguageBundle.getInstance().getResourceBundle();
            if (!BuilderUI.showConfirmAlert(true, String.format(Utility.toUTF(bundle.getString("deleteAudioConfirm")), muezzinDisplayName(target)))) {
                return;
            }
            final Muezzin current = audioBox.getValue();
            final boolean deletingSelected = current != null && target.getFileName().equals(current.getFileName());
            if (MEDIA_PLAYER != null && deletingSelected) {
                MEDIA_PLAYER.stop();
                MEDIA_PLAYER.dispose();
                MEDIA_PLAYER = null;
                setPlayIcon();
            }
            try {
                Files.deleteIfExists(Paths.get(target.getPath()));
            } catch (IOException e) {
                Logger.error(null, e, getClass().getName() + ".deleteAudio()");
                BuilderUI.showOkAlert(Alert.AlertType.ERROR, Utility.toUTF(bundle.getString("errorDeleteAudio")), bundle);
                return;
            }
            final String previousFileName = current == null ? null : current.getFileName();
            setMuezzins();
            if (deletingSelected || previousFileName == null) {
                audioBox.setValue(fallbackMuezzin());
            } else {
                audioBox.setValue(Muezzin.getFromFileName(audioBox.getItems(), previousFileName));
            }
        });
    }

    private Muezzin fallbackMuezzin() {
        if (audioBox.getItems() == null || audioBox.getItems().isEmpty()) {
            return Muezzin.NO_SOUND;
        }
        return audioBox.getItems().stream()
                .filter(muezzin -> !muezzin.isCustom()
                        && muezzin.getFileName() != null
                        && !muezzin.getFileName().isEmpty())
                .findFirst()
                .orElse(Muezzin.NO_SOUND);
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
