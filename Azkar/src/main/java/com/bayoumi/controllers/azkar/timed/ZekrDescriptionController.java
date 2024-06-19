package com.bayoumi.controllers.azkar.timed;

import com.bayoumi.models.settings.LanguageBundle;
import com.bayoumi.util.Utility;
import com.bayoumi.util.gui.ScrollHandler;
import com.jfoenix.controls.JFXDialog;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.util.ResourceBundle;

public class ZekrDescriptionController {
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private VBox scrollPaneChild;
    @FXML
    private Text hadithText, explanationOfHadithVocabulary;
    @FXML
    private Label hadithTextTitle, explanationOfHadithVocabularyTitle;
    private ResourceBundle bundle;
    private JFXDialog dialog;

    public void setData(JFXDialog dialog, String hadithText, String explanationOfHadithVocabulary) {
        this.dialog = dialog;
        updateBundle(LanguageBundle.getInstance().getResourceBundle());
        if (hadithText == null || hadithText.isEmpty()) {
            this.hadithText.setText(Utility.toUTF(bundle.getString("noData")));
        } else {
            this.hadithText.setText(hadithText);
        }
        if (explanationOfHadithVocabulary == null || explanationOfHadithVocabulary.isEmpty()) {
            this.explanationOfHadithVocabulary.setText(Utility.toUTF(bundle.getString("noData")));
        } else {
            this.explanationOfHadithVocabulary.setText(explanationOfHadithVocabulary);
        }
        ScrollHandler.init(scrollPaneChild, scrollPane, 1);
    }

    private void updateBundle(ResourceBundle bundle) {
        this.bundle = bundle;
        hadithTextTitle.setText(Utility.toUTF(bundle.getString("hadithText")));
        explanationOfHadithVocabularyTitle.setText(Utility.toUTF(bundle.getString("explanationOfHadithVocabulary")));
    }

    @FXML
    private void keyEventAction(KeyEvent keyEvent) {
        if (keyEvent.getCode().equals(KeyCode.ESCAPE)) {
            dialog.close();
        }
    }
}
