package com.bayoumi.controllers.feedback;

import com.bayoumi.util.Constants;
import com.bayoumi.util.Logger;
import com.bayoumi.util.forms.Feedback;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextArea;
import com.jfoenix.controls.JFXTextField;
import com.jfoenix.validation.RegexValidator;
import com.jfoenix.validation.RequiredFieldValidator;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import org.controlsfx.control.Notifications;

import java.net.URL;
import java.nio.file.FileSystems;
import java.util.ResourceBundle;

public class FeedbackController implements Initializable {

    @FXML
    private JFXButton suggestion;
    @FXML
    private JFXButton problem;
    @FXML
    private JFXButton other;
    @FXML
    private JFXTextField email;
    @FXML
    private JFXTextField subject;
    @FXML
    private JFXTextArea details;
    @FXML
    private JFXButton sendBtn;
    @FXML
    private ProgressIndicator progress;
    private JFXButton focusedButton;

    private void reset() {
        subject.setText("");
        email.setText("");
        details.setText("");
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        focusedButton = suggestion;
        progress.setVisible(false);
        sendBtn.disableProperty().bindBidirectional(progress.visibleProperty());

        initValidation();
    }

    private FontAwesomeIconView getErrorIcon() {
        FontAwesomeIconView errorIcon = new FontAwesomeIconView(FontAwesomeIcon.EXCLAMATION_TRIANGLE);
        errorIcon.setStyle("-fx-fill: red;-fx-font-size: 20;-fx-font-family: \"FontAwesome\"");
        return errorIcon;
    }

    private RequiredFieldValidator generateRequiredFieldValidator() {
        RequiredFieldValidator requiredFieldValidator = new RequiredFieldValidator();
        requiredFieldValidator.setMessage("هذا الحقل مطلوب");
        requiredFieldValidator.setIcon(getErrorIcon());
        return requiredFieldValidator;
    }

    private void initValidation() {
        subject.setValidators(generateRequiredFieldValidator());
        subject.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                subject.validate();
            }
        });

        email.setValidators(generateRequiredFieldValidator());
        email.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                email.validate();
            }
        });
        RegexValidator emailValidator = new RegexValidator();
        emailValidator.setMessage("البريد الإلكتروني غير صحيح!");
        emailValidator.setRegexPattern("^[_A-Za-z0-9-+]+(\\.[_A-Za-z0-9-]+)*@"
                + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$");
        emailValidator.setIcon(getErrorIcon());
        email.setValidators(emailValidator);
    }


    private String getType() {
        if (focusedButton.equals(suggestion)) {
            return "Suggestion";
        }
        if (focusedButton.equals(problem)) {
            return "Problem";
        }
        return "Other";
    }

    @FXML
    private void send() {
        new Thread(() -> {
            try {
                progress.setVisible(true);
                if (!subject.validate() || !email.validate()) {
                    progress.setVisible(false);
                    return;
                }
                if (!email.validate()) {
                    progress.setVisible(false);
                    return;
                }
                Feedback feedback = new Feedback(
                        getType(),
                        subject.getText(),
                        email.getText(),
                        details.getText(),
                        System.getProperty("os.name"),
                        getAbsolutePath(Constants.assetsPath + "/logs/debug.txt"));
                feedback.submitFeedback();
                Platform.runLater(() -> {
                    VBox vBox = new VBox(10);
                    vBox.setAlignment(Pos.CENTER);
                    vBox.setPadding(new Insets(10));
                    Label label = new Label("تم الإرسال");
                    label.setStyle("-fx-font-size: 25px");
                    FontAwesomeIconView checkIcon = new FontAwesomeIconView(FontAwesomeIcon.CHECK_CIRCLE);
                    checkIcon.setStyle("-fx-fill: green;-fx-font-size: 50;-fx-font-family: \"FontAwesome\"");
                    vBox.getChildren().addAll(checkIcon, label);
                    Notifications.create().graphic(vBox).show();
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                Logger.error(null, ex, getClass().getName() + ".send()");
            }
            progress.setVisible(false);
            reset();
        }).start();
    }

    private String getAbsolutePath(String relativePath) {
        return FileSystems.getDefault().getPath(relativePath).normalize().toAbsolutePath().toString();
    }

    @FXML
    private void toggleButton(ActionEvent event) {
        focusedButton.getStyleClass().remove("secondary-button");
        focusedButton = (JFXButton) event.getSource();
        focusedButton.getStyleClass().add("secondary-button");
    }
}
