package com.bayoumi.util.gui.load;

import javafx.scene.Parent;
import javafx.stage.Stage;

public class LoaderComponent {
    private Locations location;
    private Parent view;
    private Object controller;
    private Stage stage;

    public LoaderComponent(Locations location, Parent view, Object controller) {
        this.location = location;
        this.view = view;
        this.controller = controller;
    }

    public LoaderComponent(Locations location, Parent view, Object controller, Stage stage) {
        this.location = location;
        this.view = view;
        this.controller = controller;
        this.stage = stage;
    }

    public Locations getLocation() {
        return location;
    }

    public void setLocation(Locations location) {
        this.location = location;
    }

    public Parent getView() {
        return view;
    }

    public void setView(Parent view) {
        this.view = view;
    }

    public Object getController() {
        return controller;
    }

    public void setController(Object controller) {
        this.controller = controller;
    }

    public Stage getStage() {
        return stage;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void showAndWait() {
        stage.centerOnScreen();
        stage.showAndWait();
    }


}
