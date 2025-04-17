package com.bayoumi.controllers.settings.other;

import com.bayoumi.Launcher;
import com.bayoumi.controllers.settings.SettingsInterface;
import com.bayoumi.models.settings.Language;
import com.bayoumi.models.settings.LanguageBundle;
import com.bayoumi.models.settings.NotificationColor;
import com.bayoumi.models.settings.Settings;
import com.bayoumi.util.Constants;
import com.bayoumi.util.Logger;
import com.bayoumi.util.Utility;
import com.bayoumi.util.gui.BuilderUI;
import com.bayoumi.util.gui.HelperMethods;
import com.bayoumi.util.gui.ScrollHandler;
import com.bayoumi.util.gui.load.Locations;
import com.bayoumi.util.time.HijriDate;
import com.bayoumi.services.update.UpdateHandler;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXToggleButton;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.awt.*;
import java.net.URI;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

public class OtherSettingsController implements Initializable, SettingsInterface {

    private ResourceBundle bundle;
    @FXML
    private ComboBox<Language> languageComboBox;
    @FXML
    private JFXCheckBox autoUpdateCheckBox, usageDataCheckBox;
    @FXML
    private Spinner<Integer> hijriDateOffset;
    @FXML
    private Label minimizeAtStart, format24, darkTheme, hijriDateLabel, version, adjustingTheHijriDateText, languageText, adjustingTheHijriDateNote, usageStatsLabel,
            versionNumberLabel, website, termsOfUse, privacyPolicy;
    @FXML
    private VBox scrollChild, loadingBox;
    @FXML
    private Button checkForUpdateButton, forProblemsAndSuggestionsButton;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private JFXToggleButton minimizeAtStartToggle, format24Toggle, darkThemeToggle;


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
        usageDataCheckBox.setText(Utility.toUTF(bundle.getString("usageDataCheckBox")));
        usageStatsLabel.setText(Utility.toUTF(bundle.getString("usageStats")));
        versionNumberLabel.setText(Utility.toUTF(bundle.getString("versionNumber")));
        website.setText(Utility.toUTF(bundle.getString("website")));
        termsOfUse.setText(Utility.toUTF(bundle.getString("termsOfUse")));
        privacyPolicy.setText(Utility.toUTF(bundle.getString("privacyPolicy")));

        if (hijriDateOffset.getValue() != null) {
            hijriDateLabel.setText(new HijriDate(hijriDateOffset.getValue()).getString(this.bundle.getLocale().toString()));
        }

        toggleAction(format24Toggle);
        toggleAction(minimizeAtStartToggle);
        toggleAction(darkThemeToggle);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            ScrollHandler.init(scrollChild, scrollPane, 4);
            final Settings settings = Settings.getInstance();


            hijriDateLabel.setText(new HijriDate(settings.getHijriOffset()).getString(settings.getLanguage().getLocale()));
            hijriDateOffset.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(-20, 20, 0));
            hijriDateOffset.getValueFactory().setValue(settings.getHijriOffset());
            hijriDateOffset.valueProperty().addListener((observable, oldValue, newValue) ->
                    hijriDateLabel.setText(new HijriDate(hijriDateOffset.getValue()).getString(settings.getLanguage().getLocale())));


            languageComboBox.setConverter(Language.stringConvertor(languageComboBox));
            languageComboBox.setItems(FXCollections.observableArrayList(Language.values()));
            languageComboBox.setValue(settings.getLanguage());

            format24Toggle.setSelected(settings.getEnable24Format());
            minimizeAtStartToggle.setSelected(settings.getMinimized());
            darkThemeToggle.setSelected(settings.getNightMode());

            version.setText(Constants.VERSION);

            autoUpdateCheckBox.setSelected(settings.getAutomaticCheckForUpdates());
            usageDataCheckBox.setSelected(settings.getSendUsageData());

            updateBundle(LanguageBundle.getInstance().getResourceBundle());
            LanguageBundle.getInstance().addObserver((o, arg) -> updateBundle(LanguageBundle.getInstance().getResourceBundle()));
        } catch (Exception e) {
            Logger.error(null, e, getClass().getName() + ".initialize()");
        }
    }

    @FXML
    private void openWebsite() {
        try {
            Desktop.getDesktop().browse(new URI("https://azkar-site.web.app/"));
        } catch (Exception e) {
            Logger.error(null, e, getClass().getName() + ".openWebsite()");
        }
    }

    @FXML
    private void openUsageDataSite() {
        try {
            Desktop.getDesktop().browse(new URI("https://azkar-site.web.app/desktop/usage-data/"));
        } catch (Exception e) {
            Logger.error(null, e, getClass().getName() + ".openUsageDataSite()");
        }
    }

    @FXML
    private void onTermsOfUseClick() {
        try {
            Desktop.getDesktop().browse(new URI("https://azkar-site.web.app/policies/terms-of-use/"));
        } catch (Exception e) {
            Logger.error(null, e, getClass().getName() + ".onTermsOfUseClick()");
        }
    }

    @FXML
    private void onPrivacyPolicyClicked() {
        try {
            Desktop.getDesktop().browse(new URI("https://azkar-site.web.app/policies/privacy-policy/"));
        } catch (Exception e) {
            Logger.error(null, e, getClass().getName() + ".onPrivacyPolicyClicked()");
        }
    }

    @Override
    public void saveToDB() {
    }

    @FXML
    private void openFeedback() {
        try {
            final Scene scene = new Scene(FXMLLoader.load(Objects.requireNonNull(getClass().getResource(Locations.Feedback.toString()))));
            scene.getStylesheets().setAll(Settings.getInstance().getThemeFilesCSS());
            final Stage stage = BuilderUI.initStageDecorated(scene, "");
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
                    Logger.info(getClass().getName() + ".checkForUpdate(): " + "No Update Found");
                    Platform.runLater(() -> BuilderUI.showOkAlert(Alert.AlertType.INFORMATION, Utility.toUTF(this.bundle.getString("thereAreNoNewUpdates")), bundle));
                    break;
                case 1:
                    UpdateHandler.getInstance().showInstallPrompt();
                    break;
                case -1:
                    Logger.info(getClass().getName() + ".checkForUpdate(): " + "error => only installers and single bundle archives on macOS are supported for background updates");
                    Platform.runLater(() -> BuilderUI.showOkAlert(Alert.AlertType.ERROR, Utility.toUTF(this.bundle.getString("problemInSearchingForUpdates")), bundle));
                    break;
            }
            Platform.runLater(() -> loadingBox.setVisible(false));
        }).start();
    }

    @FXML
    private void saveLanguage() {
        Settings.getInstance().setLanguage(languageComboBox.getValue().getLocale());
    }

    @FXML
    private void autoUpdateCheck() {
        Settings.getInstance().setAutomaticCheckForUpdates(autoUpdateCheckBox.isSelected());
    }

    @FXML
    private void onUsageDataCheck() {
        Settings.getInstance().setSendUsageData(usageDataCheckBox.isSelected());
    }

    @FXML
    private void hijriDateOffsetUpdate() {
        Settings.getInstance().setHijriOffset(hijriDateOffset.getValue());
    }

    @FXML
    private void darkThemeSelect() {
        toggleAction(darkThemeToggle);
        Settings.getInstance().setNightMode(darkThemeToggle.isSelected());
        darkTheme.getScene().getStylesheets().setAll(Settings.getInstance().getThemeFilesCSS());
        Launcher.homeController.changeTheme();
        if (darkThemeToggle.isSelected()) {
            NotificationColor.setDarkTheme();
        } else {
            NotificationColor.setLightTheme();
        }
    }

    @FXML
    private void format24Select() {
        toggleAction(format24Toggle);
        Settings.getInstance().setEnable24Format(format24Toggle.isSelected());
    }

    @FXML
    private void minimizeAtStartSelect() {
        toggleAction(minimizeAtStartToggle);
        Settings.getInstance().setMinimized(minimizeAtStartToggle.isSelected());
    }

    private void toggleAction(JFXToggleButton toggleButton) {
        if (toggleButton.isSelected()) {
            toggleButton.setText(Utility.toUTF(bundle.getString("enabled")));
        } else {
            toggleButton.setText(Utility.toUTF(bundle.getString("disabled")));
        }
    }
}
