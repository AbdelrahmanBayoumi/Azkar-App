package com.bayoumi.controllers.dialog;

import com.bayoumi.models.settings.LanguageBundle;
import com.bayoumi.util.ByteUtil;
import com.bayoumi.util.Utility;
import com.bayoumi.util.gui.BuilderUI;
import com.bayoumi.util.web.WebUtilities;
import com.jfoenix.controls.JFXProgressBar;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.util.Locale;
import java.util.ResourceBundle;

public class DownloadResourcesController {
    @FXML
    private Label title, fileName, progressText;
    @FXML
    private JFXProgressBar progressbar;
    private final Locale locale = Locale.getDefault();
    private boolean isFinished;

    public void updateBundle(ResourceBundle bundle) {
        title.setText(Utility.toUTF(bundle.getString("downloadingProgramResources")));
    }

    public void setData(String url, String downloadedFilePath, String errorMsgKey, Stage stage, Runnable onClosedAndNotFinished) {
        isFinished = false;
        final ResourceBundle bundle = LanguageBundle.getInstance().getResourceBundle();
        updateBundle(bundle);

        progressbar.setProgress(0);
        fileName.setText("");
        progressText.setText("");

        stage.setOnCloseRequest(event -> {
            if (!isFinished && onClosedAndNotFinished != null) {
                onClosedAndNotFinished.run();
            }
        });
        new Thread(() -> {
            try {
                WebUtilities.downloadFile(url, downloadedFilePath, (field, fileName, bytesWritten, totalBytes) ->
                        Platform.runLater(() -> {
                            this.fileName.setText(fileName);
                            this.progressbar.setProgress(bytesWritten / (totalBytes * 2.0));
                            progressText.setText(ByteUtil.format((bytesWritten / 2), locale) + "/" + ByteUtil.format(totalBytes, locale)
                                    + " (" + Utility.formatNum((bytesWritten / (totalBytes * 2.0)) * 100) + "%)");
                        }));
                Platform.runLater(stage::close);
            } catch (Exception e) {
                Platform.runLater(() -> BuilderUI.showOkAlert(Alert.AlertType.ERROR, Utility.toUTF(bundle.getString(errorMsgKey))
                        , Utility.toUTF(bundle.getString("dir")).equals("RIGHT_TO_LEFT")));
                Platform.runLater(stage::close);
            }
        }).start();
    }
}
