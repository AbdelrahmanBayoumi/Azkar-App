package com.bayoumi.controllers.components.audio;

import com.bayoumi.models.Muezzin;
import com.bayoumi.models.settings.AzkarSettings;
import com.bayoumi.models.settings.Language;
import com.bayoumi.models.settings.LanguageBundle;
import com.bayoumi.models.settings.Settings;
import com.bayoumi.util.Constants;
import com.bayoumi.util.Logger;
import com.bayoumi.util.Utility;
import com.bayoumi.util.audio.AudioPlayer;
import com.bayoumi.util.gui.BuilderUI;
import com.bayoumi.util.gui.PopOverUtil;
import com.bayoumi.util.gui.button.TableViewButton;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXSlider;
import com.sun.javafx.scene.control.skin.ComboBoxListViewSkin;
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
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
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
    private static AudioPlayer audioPlayer = null;
    private AzkarSettings azkarSettings;
    private FontAwesomeIconView pauseIcon;
    private FontAwesomeIconView playIcon;
    private double previousValue = 50;
    private boolean isMuted = false;
    private static final double ADHAN_ROW_HEIGHT = 36;
    private static final int ADHAN_MAX_VISIBLE_ROWS = 10;

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

    public static synchronized boolean stopIfPlaying() {
        if (audioPlayer != null && audioPlayer.isPlaying()) {
            audioPlayer.stop();
            audioPlayer = null;
            return true;
        }
        return false;
    }

    public Muezzin getValue() {
        if (audioBox == null || audioBox.getValue() == null || audioBox.getValue().equals(Muezzin.NO_SOUND)) {
            return copyOfNoSound();
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
        audioBox.setOnShown(event -> fitAdhanPopupList());

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
            if (null != audioPlayer) {
                audioPlayer.setVolume(azkarSettings.getPrayerVolume() / 100.0);
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
                new FileChooser.ExtensionFilter("Audio Files", "*.mp3", "*.wav")
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
            private final FontAwesomeIconView deleteIcon = new FontAwesomeIconView(FontAwesomeIcon.TRASH);
            private final TableViewButton deleteBtn = new TableViewButton("", deleteIcon);
            private final HBox row = new HBox(8, nameLabel, deleteBtn);

            {
                nameLabel.setMinWidth(0);
                nameLabel.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(nameLabel, Priority.ALWAYS);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setMinWidth(0);
                row.setMaxWidth(Double.MAX_VALUE);
                deleteIcon.setGlyphSize(18);
                deleteBtn.setFocusTraversable(false);
                deleteBtn.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
                deleteBtn.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
                deleteBtn.getStyleClass().add("adhan-delete-btn");
                deleteBtn.setRipplerFill(Color.TRANSPARENT);
                deleteBtn.addEventFilter(MouseEvent.MOUSE_ENTERED, event -> deleteIcon.setFill(Color.web("#c91a29")));
                deleteBtn.addEventFilter(MouseEvent.MOUSE_EXITED, event -> deleteIcon.setFill(null));
                deleteBtn.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                    event.consume();
                    final Muezzin item = getItem();
                    if (item != null && item.isCustom()) {
                        deleteAudio(item);
                    }
                });
                deleteBtn.addEventFilter(MouseEvent.MOUSE_RELEASED, Event::consume);
                deleteBtn.addEventFilter(MouseEvent.MOUSE_CLICKED, Event::consume);
                setPrefWidth(0);
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

    private void fitAdhanPopupList() {
        if (!(audioBox.getSkin() instanceof ComboBoxListViewSkin)) {
            return;
        }
        final Node popupContent = ((ComboBoxListViewSkin<?>) audioBox.getSkin()).getPopupContent();
        if (!(popupContent instanceof ListView)) {
            return;
        }
        @SuppressWarnings("unchecked")
        final ListView<Muezzin> listView = (ListView<Muezzin>) popupContent;
        final int count = audioBox.getItems() == null ? 0 : audioBox.getItems().size();
        final int visible = Math.max(1, Math.min(count, ADHAN_MAX_VISIBLE_ROWS));
        final double height = visible * ADHAN_ROW_HEIGHT + 2;
        listView.setFixedCellSize(ADHAN_ROW_HEIGHT);
        listView.setPrefHeight(height);
        listView.setMinHeight(height);
        listView.setMaxHeight(height);
        audioBox.setVisibleRowCount(visible);
        listView.getStyleClass().removeAll("adhan-combo-no-scroll", "adhan-combo-scroll");
        listView.getStyleClass().add(count > ADHAN_MAX_VISIBLE_ROWS ? "adhan-combo-scroll" : "adhan-combo-no-scroll");
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
            if (audioPlayer != null && deletingSelected) {
                audioPlayer.stop();
                audioPlayer = null;
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
            return copyOfNoSound();
        }
        return audioBox.getItems().stream()
                .filter(muezzin -> !muezzin.isCustom()
                        && muezzin.getFileName() != null
                        && !muezzin.getFileName().isEmpty())
                .findFirst()
                .orElseGet(ChooseAudioController::copyOfNoSound);
    }

    private static Muezzin copyOfNoSound() {
        return new Muezzin(Muezzin.NO_SOUND.getEnglishName(), Muezzin.NO_SOUND.getArabicName(), Muezzin.NO_SOUND.getFileName());
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
                    audioPlayer = new AudioPlayer(new File(muezzin.getPath()));
                } catch (Exception e) {
                    Logger.error(null, e, getClass().getName() + ".play()");
                    final ResourceBundle bundle = LanguageBundle.getInstance().getResourceBundle();
                    BuilderUI.showOkAlert(Alert.AlertType.ERROR, Utility.toUTF(bundle.getString("errorPlayingAudio")), bundle);
                    return;
                }
                audioPlayer.setVolume(prayerVolumeSlider.getValue() / 100.0);
                audioPlayer.setOnEndOfMedia(() -> playButton.setGraphic(playIcon));
                audioPlayer.setOnStopped(() -> playButton.setGraphic(playIcon));
                audioPlayer.play();
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
