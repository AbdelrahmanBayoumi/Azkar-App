package com.bayoumi.controllers.azkar.timed;

import com.bayoumi.models.TimedZekrOld;
import com.bayoumi.models.settings.LanguageBundle;
import com.bayoumi.util.Logger;
import com.bayoumi.util.Utility;
import com.bayoumi.util.gui.ScrollHandler;
import com.bayoumi.util.gui.load.Locations;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDialog;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.util.ResourceBundle;

public class TimedAzkarController implements Initializable {

    private ResourceBundle bundle;
    private Image morningImage, nightImage;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private StackPane sp;
    @FXML
    private FlowPane boxContainer;
    @FXML
    private ImageView image;
    @FXML
    private Label title;
    @FXML
    private JFXButton settingsButton;

    private void updateBundle(ResourceBundle bundle) {
        this.bundle = bundle;
        settingsButton.setText(Utility.toUTF(bundle.getString("settings")));
    }

    public void setData(String type) {
        if (type.toLowerCase().contains("morning")) {
            title.setText(Utility.toUTF(bundle.getString("morningAzkar")));
            image.setImage(morningImage);
            TimedZekrOld.fetchMorningAzkar();
            initAzkarContainer(TimedZekrOld.MORNING_LIST);
        } else {
            title.setText(Utility.toUTF(bundle.getString("nightAzkar")));
            image.setImage(nightImage);
            TimedZekrOld.fetchNightAzkar();
            initAzkarContainer(TimedZekrOld.NIGHT_LIST);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        morningImage = new Image("/com/bayoumi/images/sun_50px.png");
        nightImage = new Image("/com/bayoumi/images/night_50px.png");
        settingsButton.setOnMouseEntered(event -> settingsButton.setContentDisplay(ContentDisplay.LEFT));
        settingsButton.setOnMouseExited(event -> settingsButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY));
        ScrollHandler.init(boxContainer, scrollPane, 1);
        updateBundle(LanguageBundle.getInstance().getResourceBundle());
    }

    private void initAzkarContainer(ObservableList<TimedZekrOld> list) {
        try {
            boxContainer.getChildren().clear();
            ZekrBoxController controller;
            FXMLLoader fxmlLoader;
            for (TimedZekrOld zekr : list) {
                fxmlLoader = new FXMLLoader(getClass().getResource(Locations.ZekrBox.toString()));
                final Node zekrBox = fxmlLoader.load();
                zekrBox.setUserData(fxmlLoader.getController());
                boxContainer.getChildren().add(zekrBox);
                controller = fxmlLoader.getController();
                controller.setData(zekr.getText(), zekr.getRepeat());
            }
        } catch (Exception ex) {
            Logger.error(null, ex, getClass().getName() + ".initAzkarContainer()");
        }
    }

    @FXML
    private void openSettings() {
        try {
            final FXMLLoader loader = new FXMLLoader(getClass().getResource(Locations.TimedAzkar_Settings.toString()));
            final JFXDialog dialog = new JFXDialog(sp, loader.load(), JFXDialog.DialogTransition.TOP);
            ((SettingsController) loader.getController()).setData(dialog,
                    () -> boxContainer.getChildren().forEach(node -> ((ZekrBoxController) node.getUserData()).updateFontSize()));
            dialog.show();
        } catch (Exception ex) {
            Logger.error(null, ex, getClass().getName() + ".openSettings()");
        }
    }
}
