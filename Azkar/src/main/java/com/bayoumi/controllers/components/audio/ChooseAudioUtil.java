package com.bayoumi.controllers.components.audio;

import com.bayoumi.Launcher;
import com.bayoumi.models.Muezzin;
import com.bayoumi.models.settings.Settings;
import com.bayoumi.util.Logger;
import com.bayoumi.util.Utility;
import com.bayoumi.util.gui.load.Locations;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.Pane;

import java.util.List;
import java.util.ResourceBundle;

public class ChooseAudioUtil {

    public static ChooseAudioController adhan(ResourceBundle bundle, Pane container) {
        try {
            List<Muezzin> muezzinList = Muezzin.getAdhanList();
            FXMLLoader loader = new FXMLLoader(ChooseAudioUtil.class.getResource(Locations.ChooseAudio.getName()));
            container.getChildren().add(loader.load());
            ChooseAudioController chooseAudioController = loader.getController();
            chooseAudioController.setData(Utility.toUTF(bundle.getString("muezzin")),
                    Muezzin.getFromFileName(muezzinList, Settings.getInstance().getPrayerTimeSettings().getAdhanAudio()),
                    muezzinList);

            return chooseAudioController;
        } catch (Exception ex) {
            Logger.error("Loading ChooseAudio", ex, ChooseAudioUtil.class.getName() + ".adhan()");
            Launcher.workFine.setValue(false);
        }
        return null;
    }
}
