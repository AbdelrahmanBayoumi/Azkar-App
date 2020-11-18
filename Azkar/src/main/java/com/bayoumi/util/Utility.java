package com.bayoumi.util;

import com.bayoumi.main.Launcher;
import com.github.msarhan.ummalqura.calendar.UmmalquraCalendar;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.text.SimpleDateFormat;
import java.util.*;

public class Utility {


    //========= Helper Objects =========
    private static final String CLASS_NAME = Utility.class.getName();

    public static void ExitKeyCodeCombination(Scene scene, Stage stage) {
        HashMap<KeyCombination, Runnable> hashMap = new HashMap<>();
        // CTRL + Q
        KeyCombination kc1 = new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN);
        Runnable rn1 = () -> {
            exitProgramAction();
            stage.close();
        };
        hashMap.put(kc1, rn1);
        scene.getAccelerators().putAll(hashMap);
    }


    public static void setStageToBottom(Stage s, double w, double h) {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        s.setX(bounds.getWidth() - (w));
        s.setY(bounds.getHeight() - (h));
    }

    public static void SetAppDecoration(Stage stage) {
        stage.setTitle("Azkar");
        SetIcon(stage);
    }

    public static void SetIcon(Stage stage) {
        stage.getIcons().clear();
        stage.getIcons().add(new Image("/com/bayoumi/images/icons8_mosque_50px.png"));
    }

    public static void exitProgramAction() {
        // code
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Long exitTime = System.currentTimeMillis();
            Logger.info("App closed - Used for "
                    + (exitTime - Launcher.startTime) + " ms\n");
        }));
        System.exit(0);
    }

    public static void copyToClipboard(String text) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(text), null);
    }

    public static String getTime(String language, Date date) {
        return new SimpleDateFormat("hh:mm:ss a", new Locale(language)).format(date);
    }

    public static String getDateAndTime(String language, Date date) {
        return getGregorianDate(language) + " - " + getHijriDate(language) + " - " + new SimpleDateFormat("hh:mm:ss a", new Locale(language)).format(date);
    }

    public static String getGregorianDate(String language) {
        Calendar cal = new GregorianCalendar();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM y", new Locale(language));
        dateFormat.setCalendar(cal);
        return dateFormat.format(cal.getTime());
    }

    public static String getDay(String language, Date date) {
        // get the day of week from the Date based on specific locale.
        return new SimpleDateFormat("EEEE", new Locale(language)).format(date);
    }

    public static String getHijriDate(String language) {
        Calendar cal = new UmmalquraCalendar();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM y", new Locale(language));
        dateFormat.setCalendar(cal);
        return dateFormat.format(cal.getTime());
    }

    public static void main(String[] args) {
        Date date = new Date();
        System.out.println(getHijriDate("ar"));
        System.out.println(getHijriDate("en"));
        System.out.println("==================");
        System.out.println(getGregorianDate("ar"));
        System.out.println(getGregorianDate("en"));
        System.out.println("==================");
        System.out.println(getDay("ar", date));
        System.out.println(getDay("en", date));
        System.out.println("==================");
        System.out.println(getTime("en", date));
        System.out.println(getTime("ar", date));
        System.out.println("==================");
        System.out.println(getDateAndTime("en", date));
        System.out.println(getDateAndTime("ar", date));
    }
}

