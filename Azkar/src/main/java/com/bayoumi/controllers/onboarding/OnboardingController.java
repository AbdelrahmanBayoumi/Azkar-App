package com.bayoumi.controllers.onboarding;

import com.bayoumi.models.Onboarding;
import com.bayoumi.models.OtherSettings;
import com.bayoumi.models.PrayerTimes;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXRadioButton;
import com.jfoenix.controls.JFXTextField;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;
import org.controlsfx.control.PopOver;

import java.net.URL;
import java.util.ResourceBundle;

public class OnboardingController implements Initializable {

    @FXML
    private JFXTextField country;
    @FXML
    private JFXTextField city;
    @FXML
    private ComboBox<PrayerTimes.PrayerTimeSettings.Method> methodComboBox;
    @FXML
    private JFXRadioButton standardJuristic;
    @FXML
    private ToggleGroup asrJuristic;
    @FXML
    private JFXRadioButton hanafiRadioButton;
    @FXML
    private JFXCheckBox format24;
    @FXML
    private JFXCheckBox minimizeAtStart;
    private PrayerTimes.PrayerTimeSettings prayerTimeSettings;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
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
    }

    @FXML
    private void finish() {
        // save prayerTimes settings
        prayerTimeSettings.setCountry(country.getText());
        prayerTimeSettings.setCity(city.getText());
        prayerTimeSettings.setMethod(methodComboBox.getValue());
        prayerTimeSettings.setAsrJuristic(hanafiRadioButton.isSelected() ? 1 : 0);
        prayerTimeSettings.setSummerTiming(false);
        prayerTimeSettings.save();
        // save other settings
        OtherSettings otherSettings = new OtherSettings();
        otherSettings.setEnable24Format(format24.isSelected());
        otherSettings.setMinimized(minimizeAtStart.isSelected());
        otherSettings.save();

        Onboarding.setFirstTimeOpened(0);

        ((Stage) country.getScene().getWindow()).close();
    }

    private void initPopOver(){
        //Build PopOver look and feel
        Label label = new Label("الجانب الرياضي لكيفية عمل الحساب متفق عليه بشكل عام في العالم الإسلامي. ثم مرة أخرى ، هذا افتراض أقوم به بناءً على عدد البلدان التي تستخدم الحساب القائم على الزاوية (ويرجى ملاحظة أنني لست مؤهلاً دينياً أو رسمياً وأقدم هذه المعلومات بتواضع مطلق على أمل أن تكون مفيدة ). ومع ذلك ، بناءً على الموقع ، وتفضيلات الحكومة ، و \"عوامل\" أخرى ، هناك اختلافات في الأساليب التي تنتج ، في بعض الأحيان ، تباينًا كبيرًا في التوقيت. إذا كان الجانب الرياضي يثير اهتمامك ، فقم بإلقاء نظرة على هذا الشرح الممتاز:"+"\nhttp://praytimes.org/wiki/Prayer_Times_Calculation.");
        label.setStyle("-fx-padding: 10;-fx-background-color: #E9C46A;-fx-text-fill: #000000; -fx-font-weight: bold; -fx-text-alignment: right; -fx-max-width: 400; -fx-wrap-text: true;");
        //Create PopOver and add look and feel
        PopOver popOver = new PopOver(label);
        methodComboBox.setOnMouseEntered(mouseEvent -> {
            //Show PopOver when mouse enters label
            popOver.show(methodComboBox);
        });
//        popOver.setCloseButtonEnabled(true);
        methodComboBox.setOnMouseExited(mouseEvent -> {
            //Hide PopOver when mouse exits label
            popOver.hide();
        });
    }

}
