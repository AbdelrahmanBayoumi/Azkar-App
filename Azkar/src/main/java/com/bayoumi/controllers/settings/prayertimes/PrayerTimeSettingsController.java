package com.bayoumi.controllers.settings.prayertimes;

import com.bayoumi.controllers.components.audio.ChooseAudioController;
import com.bayoumi.controllers.components.audio.ChooseAudioUtil;
import com.bayoumi.controllers.settings.SettingsInterface;
import com.bayoumi.models.City;
import com.bayoumi.models.Country;
import com.bayoumi.models.settings.LanguageBundle;
import com.bayoumi.models.settings.PrayerTimeSettings;
import com.bayoumi.models.settings.Settings;
import com.bayoumi.util.Logger;
import com.bayoumi.util.Utility;
import com.bayoumi.util.gui.ComboBoxAutoComplete;
import com.bayoumi.util.gui.ScrollHandler;
import com.bayoumi.util.web.IpChecker;
import com.bayoumi.util.web.LocationService;
import com.jfoenix.controls.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

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
    private JFXRadioButton hanafiRadioButton, standardJuristic;
    @FXML
    private JFXCheckBox summerTiming;
    @FXML
    private JFXTextField longitude, latitude;
    @FXML
    private JFXButton autoLocationButton;
    @FXML
    private Label statusLabel, adhanLabel, calculationMethodText, asrMadhabText, daylightSavingNote;
    private ComboBoxAutoComplete<Country> countryComboBoxAutoComplete;
    private ComboBoxAutoComplete<City> cityComboBoxAutoComplete;
    private ResourceBundle bundle;

    public void updateBundle(ResourceBundle bundle) {
        this.bundle = bundle;
        adhanLabel.setText(Utility.toUTF(bundle.getString("adhan")));
        countries.setPromptText(Utility.toUTF(bundle.getString("country")));
        cities.setPromptText(Utility.toUTF(bundle.getString("city")));
        longitude.setPromptText(Utility.toUTF(bundle.getString("longitude")));
        latitude.setPromptText(Utility.toUTF(bundle.getString("latitude")));
        autoLocationButton.setText(Utility.toUTF(bundle.getString("autoLocate")));
        calculationMethodText.setText(Utility.toUTF(bundle.getString("calculationMethod")));
        asrMadhabText.setText(Utility.toUTF(bundle.getString("asrMadhab")));
        daylightSavingNote.setText(Utility.toUTF(bundle.getString("daylightSavingNote")));
        summerTiming.setText(Utility.toUTF(bundle.getString("extraOneHourDayLightSaving")));
        standardJuristic.setText(Utility.toUTF(bundle.getString("asrMadhabJumhoor")));
        hanafiRadioButton.setText(Utility.toUTF(bundle.getString("hanafi")));
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        updateBundle(LanguageBundle.getInstance().getResourceBundle());
        statusLabel.setVisible(false);
        countryComboBoxAutoComplete = new ComboBoxAutoComplete<>(countries);
        cityComboBoxAutoComplete = new ComboBoxAutoComplete<>(cities);
        initDoubleValidation();
        prayerTimeSettings = Settings.getInstance().getPrayerTimeSettings();
        initCountries();
        initCities();
        // init Methods
        methodComboBox.setItems(FXCollections.observableArrayList(PrayerTimeSettings.Method.getListOfMethods()));
        methodComboBox.setConverter(PrayerTimeSettings.Method.getStringConverter(Settings.getInstance().getOtherSettings().getLanguageLocal().equals("ar")));
        methodComboBox.setValue(prayerTimeSettings.getMethod());
        // init Asr Juristic
        if (prayerTimeSettings.getAsrJuristic() == 1) {
            hanafiRadioButton.setSelected(true);
        } else {
            standardJuristic.setSelected(true);
        }
        summerTiming.setSelected(prayerTimeSettings.isSummerTiming());
        ScrollHandler.init(container, scrollPane, 4);

        chooseAudioController = ChooseAudioUtil.adhan(bundle, adhanContainer);
    }

    private void initCountries() {
        // getAllData
        final String local = Settings.getInstance().getOtherSettings().getLanguageLocal();
        countries.getItems().addAll(Country.getAll(local));
        countryComboBoxAutoComplete.setItems(countries.getItems());
        // StringConverter
        countries.setConverter(new StringConverter<Country>() {
            @Override
            public String toString(Country object) {
                if (local.equals("ar")) {
                    return object != null ? object.getArabicName() : "";
                }
                return object != null ? object.getEnglishName() : "";
            }

            @Override
            public Country fromString(String string) {
                if (local.equals("ar")) {
                    return countries.getItems().stream().filter(object ->
                            object.getArabicName().equals(string)).findFirst().orElse(null);
                }
                return countries.getItems().stream().filter(object ->
                        object.getEnglishName().equals(string)).findFirst().orElse(null);
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
        final String local = Settings.getInstance().getOtherSettings().getLanguageLocal();
        cities.setConverter(new StringConverter<City>() {
            @Override
            public String toString(City object) {
                if (local.equals("ar")) {
                    if (object.getArabicName() == null || object.getArabicName().trim().equals("")) {
                        return object.getEnglishName();
                    }
                    return object.getArabicName();
                }
                return object.getEnglishName();
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
        statusLabel.setText(Utility.toUTF(bundle.getString("loading")) + "..");
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
                    statusLabel.setText(Utility.toUTF(bundle.getString("autoSelectionError")));
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
