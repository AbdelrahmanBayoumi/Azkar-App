package com.bayoumi.controllers.components;

import com.bayoumi.models.location.City;
import com.bayoumi.models.location.Country;
import com.bayoumi.models.settings.Language;
import com.bayoumi.models.settings.LanguageBundle;
import com.bayoumi.models.settings.PrayerTimeSettings;
import com.bayoumi.models.settings.Settings;
import com.bayoumi.util.Logger;
import com.bayoumi.util.Utility;
import com.bayoumi.util.gui.ComboBoxAutoComplete;
import com.bayoumi.util.web.IpChecker;
import com.bayoumi.util.web.LocationService;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXTextField;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.net.URL;
import java.util.ResourceBundle;

public class SelectLocationController implements Initializable {
    private PrayerTimeSettings prayerTimeSettings;
    private ResourceBundle bundle;
    @FXML
    private Label enterCountryAndCity, statusLabel;
    @FXML
    public JFXComboBox<Country> countries;
    @FXML
    public JFXComboBox<City> cities;
    @FXML
    public JFXTextField manualLongitude, manualLatitude, autoCountry, autoCity, autoLongitude, autoLatitude;
    @FXML
    public JFXButton autoLocationButton, autoSelectButton, manualSelectButton;
    public JFXButton selectedButton;
    private ComboBoxAutoComplete<City> cityComboBoxAutoComplete;
    private ComboBoxAutoComplete<Country> countryComboBoxAutoComplete;
    @FXML
    private VBox manualContainer, autoContainer;

    public void setData() {
        updateBundle(LanguageBundle.getInstance().getResourceBundle());
        final boolean isManualSelected = prayerTimeSettings.isManualLocationSelected();
        // init auto location text fields
        if (!isManualSelected) {
            autoCountry.setText(prayerTimeSettings.getCountry());
            autoCity.setText(prayerTimeSettings.getCity());
            autoLatitude.setText(String.valueOf(prayerTimeSettings.getLatitude()));
            autoLongitude.setText(String.valueOf(prayerTimeSettings.getLongitude()));
        }
        // init manual location text fields
        initCountriesAndCities();

        // init selected button
        if (isManualSelected) {
            manualSelect(null);
        } else {
            autoSelect(null);
        }
    }

    private void updateBundle(ResourceBundle bundle) {
        this.bundle = bundle;
        countries.setPromptText(Utility.toUTF(bundle.getString("country")));
        cities.setPromptText(Utility.toUTF(bundle.getString("city")));
        manualLongitude.setPromptText(Utility.toUTF(bundle.getString("longitude")));
        manualLatitude.setPromptText(Utility.toUTF(bundle.getString("latitude")));
        autoCountry.setPromptText(Utility.toUTF(bundle.getString("country")));
        autoCity.setPromptText(Utility.toUTF(bundle.getString("city")));
        autoLongitude.setPromptText(Utility.toUTF(bundle.getString("longitude")));
        autoLatitude.setPromptText(Utility.toUTF(bundle.getString("latitude")));
        autoLocationButton.setText(Utility.toUTF(bundle.getString("autoLocate")));
        enterCountryAndCity.setText(Utility.toUTF(bundle.getString("enterCountryAndCity")));
        autoSelectButton.setText(Utility.toUTF(bundle.getString("autoLocate")));
        manualSelectButton.setText(Utility.toUTF(bundle.getString("manualLocate")));
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        LanguageBundle.getInstance().addObserver((o, arg) -> updateBundle(LanguageBundle.getInstance().getResourceBundle()));
        prayerTimeSettings = Settings.getInstance().getPrayerTimeSettings();
        statusLabel.setVisible(false);
        countryComboBoxAutoComplete = new ComboBoxAutoComplete<>(countries);
        cityComboBoxAutoComplete = new ComboBoxAutoComplete<>(cities);
        // ===== Set Converters =====
        countries.setConverter(new StringConverter<Country>() {
            @Override
            public String toString(Country object) {
                if (Settings.getInstance().getLanguage().equals(Language.Arabic)) {
                    return object != null ? object.getArabicName() : "";
                }
                return object != null ? object.getEnglishName() : "";
            }

            @Override
            public Country fromString(String string) {
                if (Settings.getInstance().getLanguage().equals(Language.Arabic)) {
                    return countries.getItems().stream().filter(object ->
                            object.getArabicName().equals(string)).findFirst().orElse(null);
                }
                return countries.getItems().stream().filter(object ->
                        object.getEnglishName().equals(string)).findFirst().orElse(null);
            }
        });
        cities.setConverter(new StringConverter<City>() {
            @Override
            public String toString(City object) {
                if (Settings.getInstance().getLanguage().equals(Language.Arabic)) {
                    if (object.getArabicName() == null || object.getArabicName().trim().isEmpty()) {
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
    }

    private void initCountriesAndCities() {
        cities.setOnAction(null);
        initCountries();
        initCities();
        cities.setOnAction((e) -> onManualLocationUpdate());
    }

    private void initCountries() {
        countries.setOnAction(null);
        final String local = Settings.getInstance().getLanguage().getLocale();
        countries.getItems().setAll(Country.getAll(local));
        countryComboBoxAutoComplete.setItems(countries.getItems());
        Country countryFormCode = Country.getCountryFromCodeOrName(prayerTimeSettings.getCountry());
        if (countryFormCode != null) {
            countries.setValue(countryFormCode);
        } else if (!countries.getItems().isEmpty()) {
            countries.setValue(countries.getItems().get(0));
        }
        countries.setOnAction((e) -> changeCountry(e, false));
    }

    private void changeCountry(ActionEvent event, boolean isInit) {
        cities.setOnAction(null);
        cities.getItems().clear();
        if (countries.getValue() != null) {
            cities.getItems().setAll(City.getCitiesInCountry(countries.getValue().getCode()));
            cityComboBoxAutoComplete.setItems(cities.getItems());
        }
        if (!cities.getItems().isEmpty() && event != null) {
            setCityComboBoxValue(cities.getItems().get(0));
        }
        if (!isInit) {
            onManualLocationUpdate();
        }
        if (event != null) {
            cities.setOnAction((e) -> onManualLocationUpdate());
        }
    }

    private void initCities() {
        if (!countries.getItems().isEmpty()) {
            changeCountry(null, true);
        }
        City cityFromEngName = City.getCityFromEngName(prayerTimeSettings.getCity(), prayerTimeSettings.getCountry());
        if (cityFromEngName != null) {
            setCityComboBoxValue(cityFromEngName);

        } else {
            setCityComboBoxValue(cities.getItems().get(0));
        }
    }

    private void setCityComboBoxValue(City city) {
        if (city != null) {
            cities.setValue(city);
            manualLatitude.setText(String.valueOf(cities.getValue().getLatitude()));
            manualLongitude.setText(String.valueOf(cities.getValue().getLongitude()));
        }
    }

    @FXML
    private void getAutoLocation() {
        countries.setDisable(true);
        cities.setDisable(true);
        manualLatitude.setDisable(true);
        manualLongitude.setDisable(true);
        autoLocationButton.setDisable(true);
        statusLabel.setVisible(true);
        statusLabel.setText(Utility.toUTF(bundle.getString("loading")) + "...");
        statusLabel.setStyle("-fx-text-fill: green");
        new Thread(() -> {
            try {
                City city = LocationService.getCity(IpChecker.getIp());
                Logger.debug("LocationService.getCity(IP): " + city);
                Platform.runLater(() -> {
                    autoCountry.setText(city.getCountryName());
                    autoCity.setText(city.getEnglishName());
                    autoLatitude.setText(String.valueOf(city.getLatitude()));
                    autoLongitude.setText(String.valueOf(city.getLongitude()));
                    statusLabel.setVisible(false);
                    onAutoLocationUpdate();
                });
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
            manualLatitude.setDisable(false);
            manualLongitude.setDisable(false);
            autoLocationButton.setDisable(false);
        }).start();
    }

    @FXML
    private void manualSelect(ActionEvent event) {
        if (selectedButton != null && selectedButton.equals(manualSelectButton)) {
            return;
        }
        toggleSelectButtonStyle(manualSelectButton);
        manualContainer.setVisible(true);
        autoContainer.setVisible(false);

        // if action is from UI not from setData() => save in settings
        if (event != null) {
            onManualLocationUpdate();
        }
    }

    @FXML
    private void autoSelect(ActionEvent event) {
        if (selectedButton != null && selectedButton.equals(autoSelectButton)) {
            return;
        }
        toggleSelectButtonStyle(autoSelectButton);
        manualContainer.setVisible(false);
        autoContainer.setVisible(true);

        // if action is from UI not from setData() => save in settings
        if (event != null) {
            onAutoLocationUpdate();
        }
    }

    @FXML
    private void onManualLocationUpdate() {
        prayerTimeSettings.setManualLocationSelected(true);
        if (countries.getValue() == null && cities.getValue() == null) {
            return;
        }
        manualLatitude.setText(String.valueOf(cities.getValue().getLatitude()));
        manualLongitude.setText(String.valueOf(cities.getValue().getLongitude()));
        prayerTimeSettings.setCountry(countries.getValue().getCode());
        prayerTimeSettings.setCity(cities.getValue().getEnglishName());
        prayerTimeSettings.setLatitude(Double.parseDouble(manualLatitude.getText()));
        prayerTimeSettings.setLongitude(Double.parseDouble(manualLongitude.getText()));
        prayerTimeSettings.handleNotifyObservers();
    }

    private void onAutoLocationUpdate() {
        if (!isAutoDetectionValid()) return;
        prayerTimeSettings.setManualLocationSelected(false);
        prayerTimeSettings.setCountry(autoCountry.getText());
        prayerTimeSettings.setCity(autoCity.getText());
        prayerTimeSettings.setLatitude(Double.parseDouble(autoLatitude.getText()));
        prayerTimeSettings.setLongitude(Double.parseDouble(autoLongitude.getText()));
        prayerTimeSettings.handleNotifyObservers();
    }

    private void toggleSelectButtonStyle(JFXButton newButton) {
        if (selectedButton != null) {
            selectedButton.getStyleClass().remove("secondary-button");
        }
        selectedButton = newButton;
        if (selectedButton != null) {
            selectedButton.getStyleClass().add("secondary-button");
        }
    }

    public boolean isAutoDetectionValid() {
        return !autoCountry.getText().trim().isEmpty() && !autoCity.getText().trim().isEmpty() && !autoLatitude.getText().trim().isEmpty() && !autoLongitude.getText().trim().isEmpty();
    }
}
