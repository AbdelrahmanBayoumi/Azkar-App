package com.bayoumi.util.gui.load;

import com.bayoumi.util.gui.BuilderUI;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;

import java.util.HashMap;

public class Loader {
    // ====== SINGLETON ========
    private static Loader instance;
    private final HashMap<Locations, LoaderComponent> map;

    private Loader() {
        map = new HashMap<>();
    }

    public static Loader getInstance() {
        if (instance == null) {
            instance = new Loader();
        }
        return instance;
    }


    public LoaderComponent getPopUp(Locations location) throws Exception {
        if (!map.containsKey(location)) {
            map.put(location, loadPopUp(location));
        }
        return map.get(location);
    }

    public Object getController(Locations location) throws Exception {
        if (!map.containsKey(location)) {
            map.put(location, load(location));
        }
        return map.get(location).getController();
    }

    public Parent getView(Locations location) throws Exception {
        if (!map.containsKey(location)) {
            map.put(location, load(location));
        }
        final LoaderComponent loaderComponent = map.get(location);
        return loaderComponent.getView();
    }

    private LoaderComponent loadPopUp(Locations location) throws Exception {
        final FXMLLoader loader = new FXMLLoader(Loader.class.getResource(location.toString()));
        final Parent view = loader.load();
        final Object controller = loader.getController();
        return new LoaderComponent(location, view, controller, BuilderUI.initStageDecorated(new Scene(view), ""));
    }

    private LoaderComponent load(Locations location) throws Exception {
        FXMLLoader loader = new FXMLLoader(Loader.class.getResource(location.toString()));
        final Parent view = loader.load();
        final Object controller = loader.getController();
        return new LoaderComponent(location, view, controller);
    }

}
