package com.bayoumi.controllers.settings.prayertimes;

import com.bayoumi.controllers.components.audio.ChooseAudioController;
import com.bayoumi.controllers.components.audio.ChooseAudioUtil;
import com.bayoumi.controllers.settings.SettingsInterface;
import com.bayoumi.models.City;
import com.bayoumi.models.Country;
import com.bayoumi.models.settings.PrayerTimeSettings;
import com.bayoumi.models.settings.Settings;
import com.bayoumi.util.Logger;
import com.bayoumi.util.gui.ComboBoxAutoComplete;
import com.bayoumi.util.gui.PopOverUtil;
import com.bayoumi.util.gui.ScrollHandler;
import com.bayoumi.util.web.IpChecker;
import com.bayoumi.util.web.LocationService;
import com.jfoenix.controls.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.controlsfx.control.PopOver;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

public class PrayerTimeSettingsController implements Initializable, SettingsInterface {
    private PrayerTimeSettings prayerTimeSettings;
    private ChooseAudioController chooseAudioController;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private VBox container, adhanContainer;
    @FXML
    private JFXComboBox<Country> countries;
    @FXML
    private JFXComboBox<City> cities;
    @FXML
    private ComboBox<PrayerTimeSettings.Method> methodComboBox;
    @FXML
    private ToggleGroup asrJuristic;
    @FXML
    private JFXRadioButton hanafiRadioButton;
    @FXML
    private JFXRadioButton standardJuristic;
    @FXML
    private JFXCheckBox summerTiming;
    @FXML
    private JFXTextField longitude;
    @FXML
    private JFXTextField latitude;
    @FXML
    private JFXButton autoLocationButton;
    @FXML
    private Label statusLabel;
    private ComboBoxAutoComplete<Country> countryComboBoxAutoComplete;
    private ComboBoxAutoComplete<City> cityComboBoxAutoComplete;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        statusLabel.setVisible(false);
        countryComboBoxAutoComplete = new ComboBoxAutoComplete<>(countries);
        cityComboBoxAutoComplete = new ComboBoxAutoComplete<>(cities);
        initDoubleValidation();
        prayerTimeSettings = Settings.getInstance().getPrayerTimeSettings();
        initCountries();
        initCities();
        PopOverUtil.init(methodComboBox, "الجانب الرياضي لكيفية عمل الحساب متفق عليه بشكل عام في العالم الإسلامي. ثم مرة أخرى ، هذا افتراض أقوم به بناءً على عدد البلدان التي تستخدم الحساب القائم على الزاوية (ويرجى ملاحظة أنني لست مؤهلاً دينياً أو رسمياً وأقدم هذه المعلومات بتواضع مطلق على أمل أن تكون مفيدة ). ومع ذلك ، بناءً على الموقع ، وتفضيلات الحكومة ، و \"عوامل\" أخرى ، هناك اختلافات في الأساليب التي تنتج ، في بعض الأحيان ، تباينًا كبيرًا في التوقيت. إذا كان الجانب الرياضي يثير اهتمامك ، فقم بإلقاء نظرة على هذا الشرح الممتاز:" + "\nhttp://praytimes.org/wiki/Prayer_Times_Calculation.");
        // init Methods
        methodComboBox.setItems(FXCollections.observableArrayList(PrayerTimeSettings.Method.getListOfMethods()));
        methodComboBox.setConverter(PrayerTimeSettings.Method.getStringConverter());
        methodComboBox.setValue(prayerTimeSettings.getMethod());
        // init Asr Juristic
        if (prayerTimeSettings.getAsrJuristic() == 1) {
            hanafiRadioButton.setSelected(true);
        } else {
            standardJuristic.setSelected(true);
        }
        summerTiming.setSelected(prayerTimeSettings.isSummerTiming());
        ScrollHandler.init(container, scrollPane, 4);

        chooseAudioController = ChooseAudioUtil.adhan(adhanContainer);
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
        if (countries.getValue() != null) {
            cities.getItems().addAll(City.getCitiesInCountry(countries.getValue().getCode()));
            cityComboBoxAutoComplete.setItems(cities.getItems());
        }
        if (!cities.getItems().isEmpty()) {
            cities.setValue(cities.getItems().get(0));
        }
        statusLabel.setVisible(false);
    }

    private void initCities() {
        cities.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
            if (newValue != null) {
                latitude.setText(String.valueOf(cities.getValue().getLatitude()));
                longitude.setText(String.valueOf(cities.getValue().getLongitude()));
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

    private void initDoubleValidation() {
        Pattern validEditingState = Pattern.compile("-?(([1-9][0-9]*)|0)?(\\.[0-9]*)?");
        UnaryOperator<TextFormatter.Change> filter = c -> {
            String text = c.getControlNewText();
            if (validEditingState.matcher(text).matches()) {
                return c;
            } else {
                return null;
            }
        };

        StringConverter<Double> converter = new StringConverter<Double>() {

            @Override
            public Double fromString(String s) {
                if (s.isEmpty() || "-".equals(s) || ".".equals(s) || "-.".equals(s)) {
                    return 0.0;
                } else {
                    return Double.valueOf(s);
                }
            }

            @Override
            public String toString(Double d) {
                return d.toString();
            }
        };
        latitude.setTextFormatter(new TextFormatter<>(converter, 0.0, filter));
        longitude.setTextFormatter(new TextFormatter<>(converter, 0.0, filter));
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
        prayerTimeSettings.setCountry(countries.getValue().getCode());
        prayerTimeSettings.setCity(cities.getValue().getEnglishName());
        prayerTimeSettings.setMethod(methodComboBox.getValue());
        prayerTimeSettings.setAsrJuristic(hanafiRadioButton.isSelected() ? 1 : 0);
        prayerTimeSettings.setSummerTiming(summerTiming.isSelected());
        prayerTimeSettings.setLatitude(Double.parseDouble(latitude.getText()));
        prayerTimeSettings.setLongitude(Double.parseDouble(longitude.getText()));

        prayerTimeSettings.setAdhanAudio(chooseAudioController.getValue());
        prayerTimeSettings.save();
        ChooseAudioController.stopIfPlaying();
    }


    @FXML
    private void getAutoLocation() {
        countries.setDisable(true);
        cities.setDisable(true);
        latitude.setDisable(true);
        longitude.setDisable(true);
        autoLocationButton.setDisable(true);
        statusLabel.setVisible(true);
        statusLabel.setText("جاري التحميل...");
        statusLabel.setStyle("-fx-text-fill: green");
        new Thread(() -> {
            try {
                City city = LocationService.getCity(IpChecker.getIp());
                Logger.info("LocationService.getCity(IP): " + city);
                Country countryFormCode = Country.getCountryFormCode(city.getCountryCode());
                if (countryFormCode != null) {
                    Platform.runLater(() -> countries.setValue(countryFormCode));
                } else {
                    throw new Exception("Error in fetching city => cannot getCountryFormCode()!");
                }
                City cityFromEngName = City.getCityFromEngName(city.getEnglishName(), city.getCountryCode());
                if (cityFromEngName != null) {
                    Platform.runLater(() -> {
                        cities.setValue(cityFromEngName);
                        cities.getSelectionModel().select(cityFromEngName);
                        latitude.setText(String.valueOf(cityFromEngName.getLatitude()));
                        latitude.setText(String.valueOf(cityFromEngName.getLongitude()));
                    });
                } else {
                    City cityFromCoordinates = City.getCityFromCoordinates(city.getLongitude(), city.getLatitude(), city.getCountryCode());
                    if (cityFromCoordinates != null) {
                        Platform.runLater(() -> {
                            cities.setValue(cityFromCoordinates);
                            cities.getSelectionModel().select(cityFromCoordinates);
                            latitude.setText(String.valueOf(cityFromCoordinates.getLatitude()));
                            latitude.setText(String.valueOf(cityFromCoordinates.getLongitude()));
                        });
                    } else {
                        throw new Exception("Error in getCityFromCoordinates() && getCityFromEngName()");
                    }
                }
                statusLabel.setVisible(false);
            } catch (Exception ex) {
                Logger.error(null, ex, getClass().getName() + ".getAutoLocation()");
                Platform.runLater(() -> {
                    statusLabel.setVisible(true);
                    statusLabel.setText("خطأ في التحديد التلقائي للموقع.. برجاء المحاولة مرة أخرى!");
                    statusLabel.setStyle("-fx-text-fill: red");
                });
            }
            countries.setDisable(false);
            cities.setDisable(false);
            latitude.setDisable(false);
            longitude.setDisable(false);
            autoLocationButton.setDisable(false);
        }).start();
    }

}
