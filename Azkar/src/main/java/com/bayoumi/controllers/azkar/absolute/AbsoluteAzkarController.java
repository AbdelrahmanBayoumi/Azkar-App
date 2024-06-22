package com.bayoumi.controllers.azkar.absolute;

import com.bayoumi.models.azkar.AbsoluteZekr;
import com.bayoumi.models.settings.LanguageBundle;
import com.bayoumi.util.Utility;
import com.bayoumi.util.gui.button.TableViewButton;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import com.jfoenix.controls.JFXTreeTableView;
import com.jfoenix.controls.RecursiveTreeItem;
import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.layout.AnchorPane;

import java.net.URL;
import java.util.ResourceBundle;

public class AbsoluteAzkarController implements Initializable {
    @FXML
    private AnchorPane root;
    @FXML
    private Label absoluteAzkar;
    @FXML
    private JFXButton returnToDefaultBTN, addButton;
    @FXML
    private JFXTextField newZekr;
    @FXML
    private JFXTreeTableView<AbsoluteZekr> azkarTable;
    @FXML
    private TreeTableColumn<AbsoluteZekr, String> zekrCol;
    @FXML
    private TreeTableColumn<AbsoluteZekr, TableViewButton> editCol;
    @FXML
    private TreeTableColumn<AbsoluteZekr, TableViewButton> deleteCol;

    public void updateBundle(ResourceBundle bundle) {
        returnToDefaultBTN.setText(Utility.toUTF(bundle.getString("retrieveDefaultAzkar")));
        addButton.setText(Utility.toUTF(bundle.getString("add")));
        absoluteAzkar.setText(Utility.toUTF(bundle.getString("absoluteAzkar")));
        newZekr.setPromptText(Utility.toUTF(bundle.getString("zekr")));
        zekrCol.setText(Utility.toUTF(bundle.getString("zekr")));
        editCol.setText(Utility.toUTF(bundle.getString("edit")));
        deleteCol.setText(Utility.toUTF(bundle.getString("delete")));
        root.setNodeOrientation(NodeOrientation.valueOf(Utility.toUTF(bundle.getString("dir"))));
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        updateBundle(LanguageBundle.getInstance().getResourceBundle());
        tableConfiguration();
        AbsoluteZekr.fetchData();
        returnToDefaultBTN.setOnMouseEntered(event -> returnToDefaultBTN.setContentDisplay(ContentDisplay.LEFT));
        returnToDefaultBTN.setOnMouseExited(event -> returnToDefaultBTN.setContentDisplay(ContentDisplay.GRAPHIC_ONLY));
    }

    private void tableConfiguration() {
        zekrCol.setCellValueFactory(new TreeItemPropertyValueFactory<>("text"));
        zekrCol.setStyle("-fx-alignment: center;");
        editCol.setCellValueFactory(new TreeItemPropertyValueFactory<>("edit"));
        editCol.setStyle("-fx-alignment: center;");
        deleteCol.setCellValueFactory(new TreeItemPropertyValueFactory<>("delete"));
        deleteCol.setStyle("-fx-alignment: center;");
        TreeItem<AbsoluteZekr> root = new RecursiveTreeItem<>(AbsoluteZekr.absoluteZekrObservableList, RecursiveTreeObject::getChildren);
        azkarTable.setRoot(root);
        azkarTable.setShowRoot(false);
    }

    @FXML
    private void newZekrAddAction() {
        if (!newZekr.getText().trim().isEmpty()) {
            AbsoluteZekr zekr = new AbsoluteZekr(0, newZekr.getText().trim());
            zekr.insert();
            AbsoluteZekr.fetchData();
            newZekr.setText("");
        }
    }


    @FXML
    private void returnToDefault() {
        AbsoluteZekr.returnDefault();
    }
}
