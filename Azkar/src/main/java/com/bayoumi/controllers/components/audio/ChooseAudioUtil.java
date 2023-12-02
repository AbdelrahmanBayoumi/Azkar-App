package com.bayoumi.controllers.components.audio;

import com.bayoumi.Launcher;
import com.bayoumi.models.Muezzin;
import com.bayoumi.models.settings.Settings;
import com.bayoumi.util.Logger;
import com.bayoumi.util.Utility;
import com.bayoumi.util.file.FileUtils;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.Pane;

import java.util.ResourceBundle;

public class ChooseAudioUtil {

    public static ChooseAudioController adhan(ResourceBundle bundle, Pane container) {
        try {
            FXMLLoader loader = new FXMLLoader(ChooseAudioUtil.class.getResource("/com/bayoumi/views/components/ChooseAudio.fxml"));
            container.getChildren().add(loader.load());
            ChooseAudioController chooseAudioController = loader.getController();
            chooseAudioController.setData(Utility.toUTF(bundle.getString("muezzin")),
                    Muezzin.getFromFileName(Settings.getInstance().getPrayerTimeSettings().getAdhanAudio()),
                    FileUtils.getAdhanList());

            return chooseAudioController;
        } catch (Exception ex) {
            Logger.error("loading ChooseAudio", ex, ChooseAudioUtil.class.getName() + ".adhan()");
            Launcher.workFine.setValue(false);
        }
        return null;
    }
}
