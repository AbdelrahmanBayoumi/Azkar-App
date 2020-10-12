package com.bayoumi.azkar.timed;

import com.bayoumi.datamodel.TimedZekr;
import com.bayoumi.util.Logger;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDialog;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
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
    private VBox boxContainer;
    @FXML
    private ImageView image;
    @FXML
    private Label title;
    @FXML
    private JFXButton settingsBTN;

    private Image morningImage;
    private Image nightImage;

    public void setData(String type) {
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
        morningImage = new Image("/com/bayoumi/images/icons8_sun_50px.png");
        nightImage = new Image("/com/bayoumi/images/icons8_night_50px.png");
    }

    private void initAzkarContainer(ObservableList<TimedZekr> list) {
        try {
            boxContainer.getChildren().clear();
            ZekrBoxController controller;
            FXMLLoader fxmlLoader;
            for (TimedZekr zekr : list) {
                fxmlLoader = new FXMLLoader(getClass().getResource("/com/bayoumi/fxml/azkar/timed/ZekrBox.fxml"));
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
        ObservableList<Text> list = FXCollections.observableArrayList();
        ObservableList<Node> ZekrBoxNodes = boxContainer.getChildren();
        for (Node zekrBox : ZekrBoxNodes) {
            list.add((Text) ((TextFlow) ((VBox) zekrBox).getChildren().get(0)).getChildren().get(0));
        }
        return list;
    }

    @FXML
    private void openSettings() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/bayoumi/fxml/azkar/timed/Settings.fxml"));
            HBox pane = loader.load();
            SettingsController controller = loader.getController();
            controller.setTextList(getTextList());
            JFXDialog dialog = new JFXDialog(sp, pane, JFXDialog.DialogTransition.TOP);
            dialog.show();
        } catch (Exception ex) {
            ex.printStackTrace();
            Logger.error(null, ex, getClass().getName() + ".initAzkarContainer()");
        }
    }

    @FXML
    private void incrementScrollSpeed(ScrollEvent event) {
        double deltaY = event.getDeltaY() * 4;
        double width = scrollPane.getContent().getBoundsInLocal().getWidth();
        double vvalue = scrollPane.getVvalue();
        scrollPane.setVvalue(vvalue + -deltaY / width);
    }
}
