package com.bayoumi.controllers.azkar.timed;

import com.bayoumi.models.azkar.TimedZekrDTO;
import com.bayoumi.models.settings.LanguageBundle;
import com.bayoumi.models.settings.Settings;
import com.bayoumi.util.Logger;
import com.bayoumi.util.Utility;
import com.bayoumi.util.gui.ScrollHandler;
import com.bayoumi.util.gui.load.Locations;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDialog;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.text.Text;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class TimedAzkarController implements Initializable {
    public static MediaPlayer MEDIA_PLAYER;

    public static void stopIfPlaying() {
        if (isMediaPlaying()) {
            MEDIA_PLAYER.stop();
        }
    }

    private static boolean isMediaPlaying() {
        return MEDIA_PLAYER != null && MEDIA_PLAYER.getStatus().equals(MediaPlayer.Status.PLAYING);
    }

    private ResourceBundle bundle;
    private List<TimedZekrDTO> timedAzkarList;
    private Image morningImage, nightImage;
    private FontAwesomeIconView pauseIcon, playIcon;
    private int currentIndex = 0;
    // =============== FXML Elements ===============
    @FXML
    private StackPane sp;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private VBox scrollPaneChild;
    @FXML
    private Text content, fadl;
    @FXML
    private Label title, count, countDescription, paginationText;
    @FXML
    private ImageView image;
    @FXML
    private JFXButton settingsButton, playButton, copyButton, toggleZekrDescriptionButton, previousButton, nextButton;
    @FXML
    private Separator separator;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        morningImage = new Image("/com/bayoumi/images/sun_50px.png");
        nightImage = new Image("/com/bayoumi/images/night_50px.png");

        playIcon = new FontAwesomeIconView(FontAwesomeIcon.PLAY);
        playIcon.setStyle("-fx-fill: -fx-secondary;");
        playIcon.setGlyphSize(30);
        pauseIcon = new FontAwesomeIconView(FontAwesomeIcon.PAUSE);
        pauseIcon.setGlyphSize(30);
        pauseIcon.setStyle("-fx-fill: -fx-secondary;");

        settingsButton.setOnMouseEntered(event -> settingsButton.setContentDisplay(ContentDisplay.LEFT));
        settingsButton.setOnMouseExited(event -> settingsButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY));
        ScrollHandler.init(scrollPaneChild, scrollPane, 1);
        updateBundle(LanguageBundle.getInstance().getResourceBundle());

        scrollPaneChild.setOnMouseClicked(event -> onNextClicked());

        // TODO: PopOverUtil.init(copyButton, bundle.getString("copy"));
    }

    public void setData(String type) {
        currentIndex = 0;
        if (type.toLowerCase().contains("morning")) {
            title.setText(Utility.toUTF(bundle.getString("morningAzkar")));
            image.setImage(morningImage);
            initAzkarContainer(1);
        } else {
            title.setText(Utility.toUTF(bundle.getString("nightAzkar")));
            image.setImage(nightImage);
            initAzkarContainer(2);
        }
    }

    @FXML
    private void onIncreaseCountClicked() {
        final int ZekrCount = timedAzkarList.get(currentIndex).getCount();
        final int currentCount = Integer.parseInt(count.getText());
        if (currentCount < ZekrCount - 1 || (currentIndex == timedAzkarList.size() - 1 && currentCount < ZekrCount)){
            count.setText(String.valueOf(currentCount + 1));
        } else {
            onNextClicked();
        }
    }

    @FXML
    private void onPlayClicked() {
        if (isMediaPlaying()) {
            MEDIA_PLAYER.stop();
            playButton.setGraphic(playIcon);
        } else {
            final String audioPath = timedAzkarList.get(currentIndex).getAudio();
            Logger.debug(audioPath);
            MEDIA_PLAYER = new MediaPlayer(new Media("https://github.com/CodeClinicStartup/test/releases/download/1.0.0/78.mp3"));
            MEDIA_PLAYER.setVolume(100);
            MEDIA_PLAYER.play();
            // playing
            playButton.setGraphic(pauseIcon);
            playButton.setPadding(new Insets(5, 11, 5, 11));
            MEDIA_PLAYER.setOnEndOfMedia(() -> playButton.setGraphic(playIcon));
        }
    }

    @FXML
    private void onPreviousClicked() {
        if (currentIndex > 0) {
            currentIndex--;
            setValuesForCurrentIndex();
        }
    }

    @FXML
    private void onNextClicked() {
        if (currentIndex < timedAzkarList.size() - 1) {
            currentIndex++;
            setValuesForCurrentIndex();
        }
    }

    private void setValuesForCurrentIndex() {
        content.setText(timedAzkarList.get(currentIndex).getContent());
        fadl.setText(timedAzkarList.get(currentIndex).getFadl());
        separator.setVisible(!fadl.getText().isEmpty());
        count.setText("0");
        countDescription.setText(timedAzkarList.get(currentIndex).getCountDescription());
        paginationText.setText((currentIndex + 1) + " من " + timedAzkarList.size());

        previousButton.setDisable(currentIndex == 0);
        countDescription.setCursor(previousButton.isDisable() ? Cursor.DEFAULT : Cursor.HAND);
        nextButton.setDisable(currentIndex == timedAzkarList.size() - 1);
        paginationText.setCursor(nextButton.isDisable() ? Cursor.DEFAULT : Cursor.HAND);

        playButton.setDisable(timedAzkarList.get(currentIndex).getAudio().isEmpty());
    }

    @FXML
    private void onToggleZekrDescription() {
        System.out.println("onToggleZekrDescription");
    }

    private void updateBundle(ResourceBundle bundle) {
        this.bundle = bundle;
        settingsButton.setText(Utility.toUTF(bundle.getString("settings")));
    }


    private void initAzkarContainer(int type) {
        updateFontSize();
        reset();
        try {
            timedAzkarList = TimedZekrDTO.getTimedAzkar(Settings.getInstance().getLanguage().getLocale(), type);
            if (timedAzkarList.isEmpty()) {
                return;
            }
            setValuesForCurrentIndex();
        } catch (Exception ex) {
            Logger.error(null, ex, getClass().getName() + ".initAzkarContainer()");
        }
    }

    private void reset() {
        content.setText(null);
        fadl.setText(null);
        count.setText(null);
        countDescription.setText(null);
        paginationText.setText(null);
    }


    @FXML
    private void onCopyClicked() {
        Utility.copyToClipboard(content.getText());
    }

    @FXML
    private void openSettings() {
        try {
            final FXMLLoader loader = new FXMLLoader(getClass().getResource(Locations.TimedAzkar_Settings.toString()));
            final JFXDialog dialog = new JFXDialog(sp, loader.load(), JFXDialog.DialogTransition.TOP);
            ((SettingsController) loader.getController()).setData(dialog, () -> {
            });
            ((SettingsController) loader.getController()).setData(dialog, this::updateFontSize);
            dialog.show();
        } catch (Exception ex) {
            Logger.error(null, ex, getClass().getName() + ".openSettings()");
        }
    }

    private void updateFontSize() {
        this.content.setStyle("-fx-font-family: 'Noto Naskh Arabic'; -fx-font-weight: BOLD; -fx-text-alignment: center;-fx-font-size: " + Settings.getInstance().getAzkarSettings().getTimedAzkarFontSize() + ";");
        this.fadl.setStyle("-fx-font-family: 'Noto Naskh Arabic'; -fx-font-weight: BOLD; -fx-text-alignment: center;-fx-font-size: " + (Settings.getInstance().getAzkarSettings().getTimedAzkarFontSize() - 4) + ";");
    }
}
