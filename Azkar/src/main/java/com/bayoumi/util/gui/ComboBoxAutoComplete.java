package com.bayoumi.util.gui;


import com.bayoumi.models.location.City;
import com.bayoumi.models.location.Country;
import com.jfoenix.controls.JFXComboBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.geometry.Bounds;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Window;

import java.text.Normalizer;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class ComboBoxAutoComplete<T> {

    String filter = "";
    private final JFXComboBox<T> cmb;
    private ObservableList<T> originalItems;

    public ComboBoxAutoComplete(JFXComboBox<T> cmb) {
        this.cmb = cmb;
//        originalItems = FXCollections.observableArrayList(cmb.getItems());
        setItems(cmb.getItems());

        cmb.setTooltip(new Tooltip());
        cmb.setOnKeyPressed(this::handleOnKeyPressed);
        cmb.setOnHidden(this::handleOnHiding);
    }

    public void setItems(ObservableList<T> items) {
        if (originalItems != null) {
            originalItems.clear();
        }
        originalItems = FXCollections.observableArrayList(items);
        filter = "";
        if (!originalItems.isEmpty() && originalItems.get(0) instanceof Country) {
            cmb.setOnKeyPressed(this::spacificActionForCountires);
        }
        if (!originalItems.isEmpty() && originalItems.get(0) instanceof City) {
            cmb.setOnKeyPressed(this::spacificActionForCities);
        }
    }

    public void spacificActionForCities(KeyEvent e) {
        ObservableList<T> filteredList = FXCollections.observableArrayList();
        KeyCode code = e.getCode();

        if (!code.equals(KeyCode.TAB) && !code.equals(KeyCode.SHIFT)) {
            filter += e.getText();
            if (code == KeyCode.BACK_SPACE && filter.length() > 0) {
                filter = filter.substring(0, filter.length() - 1);
                cmb.getItems().setAll(originalItems);
            }
            if (code == KeyCode.ESCAPE) {
                filter = "";
            }
            if (filter.length() == 0) {
                filteredList = originalItems;
                cmb.getTooltip().hide();
            } else {
                Stream<T> items = cmb.getItems().stream();
                String txtUsr = unaccent(filter.toLowerCase());
                items.filter(city -> unaccent(((City) city).getName().replaceAll("[إأآ]", "ا"))
                        .toLowerCase().contains(txtUsr.replaceAll("[إأآ]", "ا"))).forEach(filteredList::add);
                cmb.getTooltip().setText(txtUsr);
                Window stage = cmb.getScene().getWindow();
                Bounds boundsInScreen = cmb.localToScreen(cmb.getBoundsInLocal());
                double posX = boundsInScreen.getMinX();
                double posY = boundsInScreen.getMinY() - 10;

                cmb.getTooltip().show(stage, posX, posY);
                cmb.show();

            }
            cmb.getItems().setAll(filteredList);
        }
    }

    public void spacificActionForCountires(KeyEvent e) {
        ObservableList<T> filteredList = FXCollections.observableArrayList();
        KeyCode code = e.getCode();

        if (!code.equals(KeyCode.TAB) && !code.equals(KeyCode.SHIFT)) {
            filter += e.getText();
            if (code == KeyCode.BACK_SPACE && filter.length() > 0) {
                filter = filter.substring(0, filter.length() - 1);
                cmb.getItems().setAll(originalItems);
            }
            if (code == KeyCode.ESCAPE) {
                filter = "";
            }
            if (filter.length() == 0) {
                filteredList = originalItems;
                cmb.getTooltip().hide();
            } else {
                Stream<T> items = cmb.getItems().stream();
                String txtUsr = unaccent(filter.toLowerCase());

                items.filter(country -> unaccent(((Country) country).getName().replaceAll("[إأآ]", "ا"))
                        .toLowerCase().contains(txtUsr.replaceAll("[إأآ]", "ا"))).forEach(filteredList::add);
                cmb.getTooltip().setText(txtUsr);
                Window stage = cmb.getScene().getWindow();
                Bounds boundsInScreen = cmb.localToScreen(cmb.getBoundsInLocal());
                double posX = boundsInScreen.getMinX();
                double posY = boundsInScreen.getMinY() - 10;

                cmb.getTooltip().show(stage, posX, posY);
                cmb.show();

            }
            cmb.getItems().setAll(filteredList);
        }
    }

    public void handleOnKeyPressed(KeyEvent e) {
        ObservableList<T> filteredList = FXCollections.observableArrayList();
        KeyCode code = e.getCode();

        if (!code.equals(KeyCode.TAB) && !code.equals(KeyCode.SHIFT)) {
            filter += e.getText();
            if (code == KeyCode.BACK_SPACE && !filter.isEmpty()) {
                filter = filter.substring(0, filter.length() - 1);
                cmb.getItems().setAll(originalItems);
            }
            if (code == KeyCode.ESCAPE) {
                filter = "";
            }
            if (filter.isEmpty()) {
                filteredList = originalItems;
                cmb.getTooltip().hide();
            } else {
                Stream<T> itens = cmb.getItems().stream();
                String txtUsr = unaccent(filter.toLowerCase());
                itens.filter(el -> unaccent(el.toString().toLowerCase().replaceAll("[إأآ]", "ا")).contains(txtUsr.replaceAll("[إأآ]", "ا"))).forEach(filteredList::add);
                cmb.getTooltip().setText(txtUsr);
                Window stage = cmb.getScene().getWindow();
                Bounds boundsInScreen = cmb.localToScreen(cmb.getBoundsInLocal());
                double posX = boundsInScreen.getMinX();
                double posY = boundsInScreen.getMinY() - 10;

                cmb.getTooltip().show(stage, posX, posY);
                cmb.show();

            }
            cmb.getItems().setAll(filteredList);
        }
    }

    public void handleOnHiding(Event e) {
        filter = "";
        cmb.getTooltip().hide();
        T s = cmb.getSelectionModel().getSelectedItem();
        cmb.getItems().setAll(originalItems);
        cmb.getSelectionModel().select(s);
    }

    private String unaccent(String s) {
        String temp = Normalizer.normalize(s, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(temp).replaceAll("");
    }

}