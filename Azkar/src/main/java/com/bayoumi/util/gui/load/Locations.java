package com.bayoumi.util.gui.load;

public enum Locations {
    Home("/com/bayoumi/views/home/home.fxml"),
    AbsoluteAzkar("/com/bayoumi/views/azkar/absolute/AbsoluteAzkar.fxml"),
    PrayerTimes("/com/bayoumi/views/home/prayertimes/PrayerTimes.fxml"),
    AzkarPeriods("/com/bayoumi/views/home/periods/AzkarPeriods.fxml"),
    ZekrBox("/com/bayoumi/views/azkar/timed/ZekrBox.fxml"),
    TimedAzkar_Settings("/com/bayoumi/views/azkar/timed/Settings.fxml"),
    TimedAzkar("/com/bayoumi/views/azkar/timed/TimedAzkar.fxml"),
    Settings("/com/bayoumi/views/settings/Settings.fxml"),
    OtherSettings("/com/bayoumi/views/settings/other/OtherSettings.fxml"),
    PrayerTimeSettings("/com/bayoumi/views/settings/prayertimes/PrayerTimeSettings.fxml"),
    AzkarSettings("/com/bayoumi/views/settings/azkar/AzkarSettings.fxml"),
    Feedback("/com/bayoumi/views/feedback/Feedback.fxml");

    private final String name;

    Locations(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

}
