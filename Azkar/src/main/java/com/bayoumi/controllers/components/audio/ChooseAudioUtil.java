package com.bayoumi.controllers.components.audio;

import com.bayoumi.Launcher;
import com.bayoumi.models.settings.Settings;
import com.bayoumi.util.Logger;
import com.bayoumi.util.file.FileUtils;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.Pane;

public class ChooseAudioUtil {

    public static ChooseAudioController adhan(Pane container) {
        try {
            FXMLLoader loader = new FXMLLoader(ChooseAudioUtil.class.getResource("/com/bayoumi/views/components/ChooseAudio.fxml"));
            container.getChildren().add(loader.load());
            ChooseAudioController chooseAudioController = loader.getController();
            chooseAudioController.setData("المؤذن",
                    Settings.getInstance().getPrayerTimeSettings().getAdhanAudio(),
                    FileUtils.getAdhanList(),
                    "jarFiles/audio/adhan/",
                    ".mp3");

            return chooseAudioController;
        } catch (Exception ex) {
            ex.printStackTrace();
            Logger.error("loading ChooseAudio", ex, ChooseAudioUtil.class.getName() + ".adhan()");
            Launcher.workFine.setValue(false);
        }
        return null;
    }
}