package com.bayoumi.controllers.settings.prayertimes;

import com.bayoumi.controllers.settings.SettingsInterface;
import com.bayoumi.models.PrayerTimes;
import com.bayoumi.util.Logger;
import com.bayoumi.util.prayertimes.PrayerTimesDBManager;
import com.bayoumi.util.prayertimes.PrayerTimesValidation;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXRadioButton;
import com.jfoenix.controls.JFXTextField;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleGroup;
import org.controlsfx.control.PopOver;

import java.net.URL;
import java.util.ResourceBundle;

public class PrayerTimeSettingsController implements Initializable, SettingsInterface {
    private PrayerTimes.PrayerTimeSettings prayerTimeSettings;
    @FXML
    private JFXTextField country;
    @FXML
    private JFXTextField city;
    @FXML
    private ComboBox<PrayerTimes.PrayerTimeSettings.Method> methodComboBox;
    @FXML
    private ToggleGroup asrJuristic;
    @FXML
    private JFXRadioButton hanafiRadioButton;
    @FXML
    private JFXRadioButton standardJuristic;
    @FXML
    private JFXCheckBox summerTiming;
    @FXML
    private Label statusLabel;
    @FXML
    private JFXButton reloadButton;
    private ChangeListener<Number> changeListener;

    private void initChangeListener() {
        changeListener = (observable, oldValue, newValue) -> {
            System.out.println(observable);
            Platform.runLater(() -> changeStatusLabel(newValue.intValue()));
        };
    }

    private void changeStatusLabel(int i) {
        if (i == 1) {
            statusLabel.setText("تم تحميل مواقيت الصلاة بنجاح");
            statusLabel.setStyle("-fx-text-fill: green;");
        } else if (i == 0) {
            statusLabel.setText("جاري تحميل مواقيت الصلاة..");
            statusLabel.setStyle("-fx-text-fill: red;");
        } else {
            statusLabel.setText("خطأ في تحميل مواقيت الصلاة!");
            statusLabel.setStyle("-fx-text-fill: red;");
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initChangeListener();
        PrayerTimesValidation.PRAYERTIMES_STATUS.addListener(changeListener);
        changeStatusLabel(PrayerTimesValidation.PRAYERTIMES_STATUS.intValue());
        initPopOver();
        prayerTimeSettings = new PrayerTimes.PrayerTimeSettings();
        country.setText(prayerTimeSettings.getCountry());
        city.setText(prayerTimeSettings.getCity());
        methodComboBox.setItems(FXCollections.observableArrayList(
                PrayerTimes.PrayerTimeSettings.Method.getListOfMethods()
        ));
        methodComboBox.setConverter(PrayerTimes.PrayerTimeSettings.Method.getStringConverter());
        methodComboBox.setValue(prayerTimeSettings.getMethod());

        if (prayerTimeSettings.getAsrJuristic() == 1) {
            hanafiRadioButton.setSelected(true);
        } else {
            standardJuristic.setSelected(true);
        }

        summerTiming.setSelected(prayerTimeSettings.isSummerTiming());

        reloadButton.disableProperty().bind(PrayerTimesValidation.PRAYERTIMES_STATUS.isEqualTo(0));
//        reloadButton.sceneProperty().addListener((observableScene, oldScene, newScene) -> {
//            if (oldScene == null && newScene != null) {
//                newScene.getWindow().setOnCloseRequest(event -> {
//
//                });
//            }
//        });
    }

    private void initPopOver() {
        //Build PopOver look and feel
        Label label = new Label("الجانب الرياضي لكيفية عمل الحساب متفق عليه بشكل عام في العالم الإسلامي. ثم مرة أخرى ، هذا افتراض أقوم به بناءً على عدد البلدان التي تستخدم الحساب القائم على الزاوية (ويرجى ملاحظة أنني لست مؤهلاً دينياً أو رسمياً وأقدم هذه المعلومات بتواضع مطلق على أمل أن تكون مفيدة ). ومع ذلك ، بناءً على الموقع ، وتفضيلات الحكومة ، و \"عوامل\" أخرى ، هناك اختلافات في الأساليب التي تنتج ، في بعض الأحيان ، تباينًا كبيرًا في التوقيت. إذا كان الجانب الرياضي يثير اهتمامك ، فقم بإلقاء نظرة على هذا الشرح الممتاز:" + "\nhttp://praytimes.org/wiki/Prayer_Times_Calculation.");
        label.setStyle("-fx-padding: 10;-fx-background-color: #E9C46A;-fx-text-fill: #000000; -fx-font-weight: bold; -fx-text-alignment: right; -fx-max-width: 400; -fx-wrap-text: true;");
        //Create PopOver and add look and feel
        PopOver popOver = new PopOver(label);
        methodComboBox.setOnMouseEntered(mouseEvent -> {
            //Show PopOver when mouse enters label
            popOver.show(methodComboBox);
        });
        methodComboBox.setOnMouseExited(mouseEvent -> {
            //Hide PopOver when mouse exits label
            popOver.hide();
        });
    }

    @Override
    public void saveToDB() {
        PrayerTimesValidation.PRAYERTIMES_STATUS.removeListener(changeListener);

        prayerTimeSettings.setCountry(country.getText());
        prayerTimeSettings.setCity(city.getText());
        prayerTimeSettings.setMethod(methodComboBox.getValue());
        prayerTimeSettings.setAsrJuristic(hanafiRadioButton.isSelected() ? 1 : 0);
        prayerTimeSettings.setSummerTiming(summerTiming.isSelected());
        prayerTimeSettings.save();
    }


    @FXML
    private void reload() {
        new Thread(() -> {
            try {
                PrayerTimesDBManager.deleteAll();
                new PrayerTimesValidation().start();
            } catch (Exception ex) {
                Logger.error(null, ex, getClass().getName() + ".reload()");
            }
        }).start();
    }

}
