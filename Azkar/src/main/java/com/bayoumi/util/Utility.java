package com.bayoumi.util;

import com.bayoumi.main.Launcher;
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
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

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
        s.setX(bounds.getWidth() - (w) - 30);
        s.setY(bounds.getHeight() - (h) - 50);
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

    public static String getTimeNow() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss a");
        LocalTime time = LocalTime.now();
        return formatter.format(time);
    }

    public static String getDateNow() {
        return LocalDate.now().toString();
    }

    public static void copyToClipboard(String text) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(text), null);
    }
}

