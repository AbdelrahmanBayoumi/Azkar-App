package com.bayoumi.controllers.onboarding;

import com.bayoumi.models.*;
import com.bayoumi.util.Logger;
import com.bayoumi.util.gui.ComboBoxAutoComplete;
import com.bayoumi.util.web.IpChecker;
import com.bayoumi.util.web.LocationService;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXRadioButton;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.controlsfx.control.PopOver;

import java.net.URL;
import java.util.ResourceBundle;

public class OnboardingController implements Initializable {

    @FXML
    private JFXComboBox<Country> countries;
    @FXML
    private JFXComboBox<City> cities;
    @FXML
    private ComboBox<PrayerTimeSettings.Method> methodComboBox;
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
    @FXML
    private JFXButton autoLocationButton;
    @FXML
    private Label statusLabel;
    private PrayerTimeSettings prayerTimeSettings;
    private ComboBoxAutoComplete<Country> countryComboBoxAutoComplete;
    private ComboBoxAutoComplete<City> cityComboBoxAutoComplete;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        countryComboBoxAutoComplete = new ComboBoxAutoComplete<>(countries);
        cityComboBoxAutoComplete = new ComboBoxAutoComplete<>(cities);
        initPopOver();
        prayerTimeSettings = new PrayerTimeSettings();
        initCountries();
        initCities();
        methodComboBox.setItems(FXCollections.observableArrayList(
                PrayerTimeSettings.Method.getListOfMethods()
        ));
        methodComboBox.setConverter(PrayerTimeSettings.Method.getStringConverter());
        methodComboBox.setValue(prayerTimeSettings.getMethod());

        if (prayerTimeSettings.getAsrJuristic() == 1) {
            hanafiRadioButton.setSelected(true);
        } else {
            standardJuristic.setSelected(true);
        }
        autoLocationButton.fire();
    }

    @FXML
    private void finish() {
        // save prayerTimes settings
        prayerTimeSettings.setCountry(countries.getValue().getCode());
        prayerTimeSettings.setCity(cities.getValue().getEnglishName());
        prayerTimeSettings.setMethod(methodComboBox.getValue());
        prayerTimeSettings.setAsrJuristic(hanafiRadioButton.isSelected() ? 1 : 0);
        prayerTimeSettings.setSummerTiming(false);
        prayerTimeSettings.setLatitude(cities.getValue().getLatitude());
        prayerTimeSettings.setLongitude(cities.getValue().getLongitude());
        prayerTimeSettings.save();
        // save other settings
        OtherSettings otherSettings = new OtherSettings();
        otherSettings.setEnable24Format(format24.isSelected());
        otherSettings.setMinimized(minimizeAtStart.isSelected());
        otherSettings.save();

        Onboarding.setFirstTimeOpened(0);

        ((Stage) cities.getScene().getWindow()).close();
    }

    private void initCountries() {
        // getAllData
        countries.getItems().addAll(Country.getAllData());
        countryComboBoxAutoComplete.setItems(countries.getItems());
        // StringConverter
        countries.setConverter(new StringConverter<Country>() {
            @Override
            public String toString(Country object) {
                return object != null ? object.getArabicName() : "";
            }

            @Override
            public Country fromString(String string) {
                return countries.getItems().stream().filter(object ->
                        object.getArabicName().equals(string)).findFirst().orElse(null);
            }
        });
        countries.setOnAction(this::changeCountry);
        if (!countries.getItems().isEmpty()) {
            countries.setValue(countries.getItems().get(0));
        }
        Country countryFormCode = Country.getCountryFormCode(prayerTimeSettings.getCountry());
        if (countryFormCode != null) {
            countries.setValue(countryFormCode);
        }
    }

    private void changeCountry(ActionEvent event) {
        cities.getItems().clear();
        cities.getItems().addAll(City.getCitiesInCountry(countries.getValue().getCode()));
        cityComboBoxAutoComplete.setItems(cities.getItems());
        if (!cities.getItems().isEmpty()) {
            cities.setValue(cities.getItems().get(0));
        }
        statusLabel.setVisible(false);
    }

    private void initCities() {
        cities.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
            if (newValue != null) {
                statusLabel.setVisible(false);
            }
        });
        if (!countries.getItems().isEmpty()) {
            cities.getItems().clear();
            if (countries.getValue() != null) {
                cities.getItems().addAll(City.getCitiesInCountry(countries.getValue().getCode()));
                cityComboBoxAutoComplete.setItems(cities.getItems());
            }
            if (!cities.getItems().isEmpty()) {
                cities.setValue(cities.getItems().get(0));
            }
        }
        cities.setConverter(new StringConverter<City>() {
            @Override
            public String toString(City object) {
                if (object.getArabicName() == null || object.getArabicName().trim().equals("")) {
                    return object.getEnglishName();
                }
                return object.getArabicName();
            }

            @Override
            public City fromString(String string) {
                return null;
            }
        });
        City cityFromEngName = City.getCityFromEngName(prayerTimeSettings.getCity(), prayerTimeSettings.getCountry());
        if (cityFromEngName != null) {
            cities.setValue(cityFromEngName);
            cities.getSelectionModel().select(cityFromEngName);
        }
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
//        popOver.setCloseButtonEnabled(true);
        methodComboBox.setOnMouseExited(mouseEvent -> {
            //Hide PopOver when mouse exits label
            popOver.hide();
        });
    }

    @FXML
    private void getAutoLocation() {
        countries.setDisable(true);
        cities.setDisable(true);
        autoLocationButton.setDisable(true);
        statusLabel.setVisible(true);
        statusLabel.setText("جاري التحميل...");
        statusLabel.setStyle("-fx-text-fill: green");
        new Thread(() -> {
            try {
                City city = LocationService.getCity(IpChecker.getIp());
                Logger.info("LocationService.getCity(IP): " + city);
                Country countryFormCode = Country.getCountryFormCode(city.getCountryCode());
                System.out.println("countryFormCode: " + countryFormCode);
                if (countryFormCode != null) {
                    Platform.runLater(() -> {
                        countries.setValue(countryFormCode);
                    });
                } else {
                    throw new Exception("Error in fetching city => cannot getCountryFormCode()!");
                }
                City cityFromEngName = City.getCityFromEngName(city.getEnglishName(), city.getCountryCode());
                System.out.println("cityFromEngName: " + cityFromEngName);
                if (cityFromEngName != null) {
                    Platform.runLater(() -> {
                        cities.setValue(cityFromEngName);
                        cities.getSelectionModel().select(cityFromEngName);
                    });
                } else {
                    City cityFromCoordinates = City.getCityFromCoordinates(city.getLongitude(), city.getLatitude(), city.getCountryCode());
                    if (cityFromCoordinates != null) {
                        Platform.runLater(() -> {
                            cities.setValue(cityFromCoordinates);
                            cities.getSelectionModel().select(cityFromCoordinates);
                        });
                    } else {
                        throw new Exception("Error in getCityFromCoordinates() && getCityFromEngName()");
                    }
                }
                statusLabel.setVisible(false);
            } catch (Exception ex) {
                ex.printStackTrace();
                Logger.error(null, ex, getClass().getName() + ".getAutoLocation()");
                Platform.runLater(() -> {
                    statusLabel.setVisible(true);
                    statusLabel.setText("خطأ في التحديد التلقائي للموقع.. برجاء المحاولة مرة أخرى!");
                    statusLabel.setStyle("-fx-text-fill: red");
                });
            }
            countries.setDisable(false);
            cities.setDisable(false);
            autoLocationButton.setDisable(false);
        }).start();
    }
}
