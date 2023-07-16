package com.bayoumi.controllers.azkar.timed;

import com.bayoumi.models.TimedZekr;
import com.bayoumi.util.Logger;
import com.bayoumi.util.gui.ScrollHandler;
import com.bayoumi.util.gui.load.Locations;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDialog;
import javafx.collections.FXCollections;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.net.URL;
import java.util.ResourceBundle;

public class TimedAzkarController implements Initializable {

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

    private Image morningImage, nightImage;

    public void setData(String type) {
        // TODO: handle localization for this view
        if (type.toLowerCase().contains("morning")) {
            title.setText("أذكار الصباح");
            image.setImage(morningImage);
            TimedZekr.fetchMorningAzkar();
            initAzkarContainer(TimedZekr.MORNING_LIST);
        } else {
            title.setText("أذكار المساء");
            image.setImage(nightImage);
            TimedZekr.fetchNightAzkar();
            initAzkarContainer(TimedZekr.NIGHT_LIST);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        morningImage = new Image("/com/bayoumi/images/sun_50px.png");
        nightImage = new Image("/com/bayoumi/images/night_50px.png");
        settingsButton.setOnMouseEntered(event -> settingsButton.setContentDisplay(ContentDisplay.LEFT));
        settingsButton.setOnMouseExited(event -> settingsButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY));
        ScrollHandler.init(boxContainer, scrollPane, 1);
    }

    private void initAzkarContainer(ObservableList<TimedZekr> list) {
        try {
            boxContainer.getChildren().clear();
            ZekrBoxController controller;
            FXMLLoader fxmlLoader;
            for (TimedZekr zekr : list) {
                fxmlLoader = new FXMLLoader(getClass().getResource(Locations.ZekrBox.toString()));
                boxContainer.getChildren().add(fxmlLoader.load());
                controller = fxmlLoader.getController();
                controller.setData(zekr.getText(), zekr.getRepeat());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Logger.error(null, ex, getClass().getName() + ".initAzkarContainer()");
        }
    }

    private ObservableList<Text> getTextList() {
        final ObservableList<Text> list = FXCollections.observableArrayList();
        final ObservableList<Node> ZekrBoxNodes = boxContainer.getChildren();
        for (Node zekrBox : ZekrBoxNodes) {
            list.add((Text) ((TextFlow) ((HBox) ((VBox) zekrBox).getChildren().get(0)).getChildren().get(0)).getChildren().get(0));
        }
        return list;
    }

    @FXML
    private void openSettings() {
        try {
            final FXMLLoader loader = new FXMLLoader(getClass().getResource(Locations.TimedAzkar_Settings.toString()));
            final JFXDialog dialog = new JFXDialog(sp, loader.load(), JFXDialog.DialogTransition.TOP);
            ((SettingsController) loader.getController()).setData(getTextList(), dialog);
            dialog.show();
        } catch (Exception ex) {
            Logger.error(null, ex, getClass().getName() + ".openSettings()");
        }
    }
}
