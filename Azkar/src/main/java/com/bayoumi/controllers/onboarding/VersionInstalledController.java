package com.bayoumi.controllers.onboarding;

import com.bayoumi.Launcher;
import com.bayoumi.controllers.settings.azkar.ChooseNotificationColorController;
import com.bayoumi.models.settings.Language;
import com.bayoumi.models.settings.LanguageBundle;
import com.bayoumi.models.settings.NotificationColor;
import com.bayoumi.models.settings.Settings;
import com.bayoumi.services.statistics.StatisticsService;
import com.bayoumi.storage.statistics.StatisticsType;
import com.bayoumi.util.Logger;
import com.bayoumi.util.Utility;
import com.bayoumi.util.gui.load.Loader;
import com.bayoumi.util.gui.load.LoaderComponent;
import com.bayoumi.util.gui.load.Locations;
import com.jfoenix.controls.JFXCheckBox;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.net.URL;
import java.util.ResourceBundle;

public class VersionInstalledController implements Initializable {

    @FXML
    private VBox root;
    @FXML
    private WebView webView;
    @FXML
    private Button closeButton, notificationColorButton;
    @FXML
    private Label mainTitleLabel;
    @FXML
    private JFXCheckBox darkTheme;

    public void updateBundle(ResourceBundle bundle) {
        mainTitleLabel.setText(Utility.toUTF(bundle.getString("changesInNewVersion")));
        darkTheme.setText(Utility.toUTF(bundle.getString("enableDarkTheme")));
        notificationColorButton.setText(Utility.toUTF(bundle.getString("settings.azkar.notificationColor")));
        closeButton.setText(Utility.toUTF(bundle.getString("close")));
        root.setNodeOrientation(NodeOrientation.valueOf(Utility.toUTF(bundle.getString("dir"))));
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            updateBundle(LanguageBundle.getInstance().getResourceBundle());

            String content;
            if (Settings.getInstance().getLanguage().equals(Language.Arabic)) {
                content = "<!DOCTYPE html> <html lang=\"en\"> <head> <meta charset=\"UTF-8\" /> <title>Changelog</title> <style> body { font-family: Tajawal, Arial, sans-serif; line-height: 1.6; margin: 20px; background-color: #2c3f51; color: #ffffff; } body.light { background-color: #fff; color: #000; } body.dark { background-color: #2c3f51; color: #fff; } h3 { margin-block: 10px; } ul { margin: 0 0 20px 20px; } </style> </head> <body class=\"%s\"> <h3 dir=\"rtl\">● الميزات الجديدة:</h3> <ul dir=\"rtl\"> <li>إمكانية التبديل بين <strong>الوضع الفاتح والوضع الداكن</strong>.</li> <li>إضافة خيار تغيير <strong>لون الإشعارات (الخلفية والنص والحدود)</strong>.</li> <li> إضافة إمكانية <strong>رفع ملفات صوتية مخصصة للأذان</strong>، مما يسمح للمستخدمين برفع أي ملف صوتي يرغبون به للأذان. </li> <li> إضافة <strong>زر لتفعيل/إلغاء تذكير الأذان</strong> حسب رغبة المستخدمين. </li> </ul> <h3 dir=\"rtl\">● التحسينات:</h3> <ul dir=\"rtl\"> <li>تغيير تسمية \"مواقيت أخرى\" إلى \"مواقيت الليل\" لتوضيح الغرض.</li> <li> <strong> تم تفعيل جمع بعض البيانات الإحصائية افتراضيًا</strong> للمساعدة في تحسين أداء التطبيق وحل المشكلات. يمكنك إيقاف هذه الميزة في أي وقت من قسم \"الخصوصية\" في الإعدادات. </li> </ul> </body> </html>";
            } else {
                content = "<!DOCTYPE html> <html lang=\"en\"> <head> <meta charset=\"UTF-8\" /> <title>Changelog</title> <style> body { font-family: Tajawal, Arial, sans-serif; line-height: 1.6; margin: 20px; background-color: #2c3f51; color: #ffffff; } body.light { background-color: #fff; color: #000; } body.dark { background-color: #2c3f51; color: #fff; } h3 { margin-block: 10px; } ul { margin: 0 0 20px 20px; } </style> </head> <body class=\"%s\"> <h3>● New Features:</h3> <ul> <li>Ability to switch between <strong>light and dark modes</strong>.</li> <li>Added an option to change <strong>the notification colors (background, text, and borders)</strong>.</li> <li> Added the <strong>ability to upload custom Adhan audio files</strong>, allowing users to upload any desired Adhan audio file. </li> <li> Added a <strong>button to enable/disable Adhan reminders</strong> based on user preference. </li> </ul> <h3>● Enhancements:</h3> <ul> <li>Renamed \"Other Timings\" to \"Night Timings\" for better clarity.</li> <li> <strong>Data collection for analytics is now enabled by default</strong> to help improve performance and resolve issues more efficiently. You can disable this feature at any time in the “Privacy” section under Settings. </li> </ul> </body> </html>";
            }
            webView.getEngine().loadContent(String.format(content, Settings.getInstance().getNightMode() ? "dark" : "light"));

            darkTheme.setSelected(Settings.getInstance().getNightMode());
        } catch (Exception ex) {
            Logger.error(null, ex, getClass().getName() + ".initialize()");
        }
    }

    @FXML
    private void goToNotificationColor() {
        try {
            StatisticsService.getInstance().increment(StatisticsType.SETTINGS_COLORS_OPENED);
            final LoaderComponent popUp = Loader.getInstance().getPopUp(Locations.ChooseNotificationColor);
            ((ChooseNotificationColorController) popUp.getController()).setData();
            popUp.showAndWait();
        } catch (Exception e) {
            Logger.error(null, e, getClass().getName() + ".goToNotificationColor()");
        }
    }


    @FXML
    private void darkThemeSelect() {
        final boolean isDark = darkTheme.isSelected();
        Settings.getInstance().setNightMode(isDark);
        darkTheme.getScene().getStylesheets().setAll(Settings.getInstance().getThemeFilesCSS());

        Launcher.homeController.changeTheme();
        if (isDark) {
            NotificationColor.setDarkTheme();
        } else {
            NotificationColor.setLightTheme();
        }

        updateWebViewCSS(isDark);
    }

    private void updateWebViewCSS(boolean isDark) {
        try {
            final Document doc = webView.getEngine().getDocument();
            final Element body = (Element) doc.getElementsByTagName("body").item(0);
            if (body != null) {
                body.setAttribute("class", isDark ? "dark" : "light");
            }
        } catch (Exception e) {
            Logger.error(null, e, getClass().getName() + ".updateWebViewCSS()");
        }
    }

    @FXML
    private void close() {
        ((Stage) closeButton.getScene().getWindow()).close();
    }

}
