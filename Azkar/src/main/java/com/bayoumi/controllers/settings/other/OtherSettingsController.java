package com.bayoumi.controllers.settings.other;

import com.bayoumi.controllers.settings.SettingsInterface;
import com.bayoumi.models.settings.Language;
import com.bayoumi.models.settings.LanguageBundle;
import com.bayoumi.models.settings.OtherSettings;
import com.bayoumi.models.settings.Settings;
import com.bayoumi.util.Constants;
import com.bayoumi.util.Logger;
import com.bayoumi.util.Utility;
import com.bayoumi.util.gui.BuilderUI;
import com.bayoumi.util.gui.HelperMethods;
import com.bayoumi.util.gui.load.Locations;
import com.bayoumi.util.time.HijriDate;
import com.bayoumi.util.update.UpdateHandler;
import com.jfoenix.controls.JFXCheckBox;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.awt.*;
import java.net.URI;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

public class OtherSettingsController implements Initializable, SettingsInterface {

    private OtherSettings otherSettings;
    private ResourceBundle bundle;
    @FXML
    public Label hijriDateLabel;
    @FXML
    private ComboBox<Language> languageComboBox;
    @FXML
    private JFXCheckBox format24;
    @FXML
    private JFXCheckBox minimizeAtStart;
    @FXML
    private Spinner<Integer> hijriDateOffset;
    @FXML
    private JFXCheckBox darkTheme, autoUpdateCheckBox;
    @FXML
    private Label version, adjustingTheHijriDateText, languageText;
    @FXML
    private VBox loadingBox;
    @FXML
    private Text adjustingTheHijriDateNote;
    @FXML
    private Button checkForUpdateButton, forProblemsAndSuggestionsButton;
    @FXML
    private StackPane root;

    public void updateBundle(ResourceBundle bundle) {
        this.bundle = bundle;
        languageText.setText(Utility.toUTF(bundle.getString("language")));
        format24.setText(Utility.toUTF(bundle.getString("hour24System")));
        darkTheme.setText(Utility.toUTF(bundle.getString("darkTheme")));
        minimizeAtStart.setText(Utility.toUTF(bundle.getString("minimizeAtStart")));
        adjustingTheHijriDateText.setText(Utility.toUTF(bundle.getString("adjustingTheHijriDateText")));
        adjustingTheHijriDateNote.setText(Utility.toUTF(bundle.getString("adjustingTheHijriDateNote")));
        checkForUpdateButton.setText(Utility.toUTF(bundle.getString("checkForUpdate")));
        forProblemsAndSuggestionsButton.setText(Utility.toUTF(bundle.getString("forProblemsAndSuggestions")));
        autoUpdateCheckBox.setText(Utility.toUTF(bundle.getString("checkForUpdatesAutomatically")));
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        otherSettings = Settings.getInstance().getOtherSettings();
        updateBundle(LanguageBundle.getInstance().getResourceBundle());

        hijriDateLabel.setText(new HijriDate(otherSettings.getHijriOffset()).getString(otherSettings.getLanguageLocal()));

        hijriDateOffset.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(-20, 20, 0));
        hijriDateOffset.getValueFactory().setValue(otherSettings.getHijriOffset());

        hijriDateOffset.valueProperty().addListener((observable, oldValue, newValue) ->
                hijriDateLabel.setText(new HijriDate(hijriDateOffset.getValue()).getString(otherSettings.getLanguageLocal())));


        languageComboBox.setConverter(Language.stringConvertor(languageComboBox));
        languageComboBox.setItems(FXCollections.observableArrayList(Language.values()));
        languageComboBox.setValue(otherSettings.getLanguage());

        format24.setSelected(otherSettings.isEnable24Format());

        minimizeAtStart.setSelected(otherSettings.isMinimized());

        darkTheme.setSelected(otherSettings.isEnableDarkMode());
        darkTheme.setDisable(true);

        version.setText(Constants.VERSION);

        autoUpdateCheckBox.setSelected(otherSettings.isAutomaticCheckForUpdates());
    }

    @FXML
    private void openWebsite() {
        try {
            Desktop.getDesktop().browse(new URI("https://azkar-site.web.app/"));
        } catch (Exception e) {
            Logger.error(null, e, getClass().getName() + ".openWebsite()");
        }
    }

    @Override
    public void saveToDB() {
        otherSettings.setLanguage(languageComboBox.getValue());
        otherSettings.setEnable24Format(format24.isSelected());
        otherSettings.setEnableDarkMode(darkTheme.isSelected());
        otherSettings.setHijriOffset(hijriDateOffset.getValue());
        otherSettings.setMinimized(minimizeAtStart.isSelected());
        otherSettings.setAutomaticCheckForUpdates(autoUpdateCheckBox.isSelected());
        otherSettings.save();
    }

    @FXML
    private void openFeedback() {
        try {
            final Stage stage = BuilderUI.initStageDecorated(new Scene(FXMLLoader.load(Objects.requireNonNull(getClass().getResource(Locations.Feedback.toString())))), "");
            HelperMethods.ExitKeyCodeCombination(stage.getScene(), stage);
            stage.show();
            ((Stage) version.getScene().getWindow()).close();
        } catch (Exception e) {
            Logger.error(null, e, getClass().getName() + ".openFeedback()");
        }
    }

    @FXML
    private void checkForUpdate() {
        loadingBox.setVisible(true);
        new Thread(() -> {
            switch (UpdateHandler.getInstance().checkUpdate()) {
                case 0:
                    Logger.info(OtherSettings.class.getName() + ".checkForUpdate(): " + "No Update Found");
                    Platform.runLater(() -> BuilderUI.showOkAlert(Alert.AlertType.INFORMATION, Utility.toUTF(this.bundle.getString("thereAreNoNewUpdates")), Utility.toUTF(this.bundle.getString("dir")).equals("RIGHT_TO_LEFT")));
                    break;
                case 1:
                    UpdateHandler.getInstance().showInstallPrompt();
                    break;
                case -1:
                    Logger.info(OtherSettings.class.getName() + ".checkForUpdate(): " + "error => only installers and single bundle archives on macOS are supported for background updates");
                    Platform.runLater(() -> BuilderUI.showOkAlert(Alert.AlertType.ERROR, Utility.toUTF(this.bundle.getString("problemInSearchingForUpdates")), Utility.toUTF(this.bundle.getString("dir")).equals("RIGHT_TO_LEFT")));
                    break;
            }
            Platform.runLater(() -> loadingBox.setVisible(false));
        }).start();
    }
}