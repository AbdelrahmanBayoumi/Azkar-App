/*package com.bayoumi;

import com.bayoumi.util.gui.Draggable;
import com.bayoumi.util.gui.notfication.Notifications;
import com.bayoumi.util.gui.tray.TrayUtil;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.stage.*;
import javafx.util.Duration;

import java.util.Random;

public class TestMain extends Application {
    private static final Popup popup = new Popup();
    private static Window window;
    ;

    public static void main(String[] args) {
        launch(args);
    }

    private static HBox createGraphic(String msg, Image image) {
        Text text = new Text(msg);
        text.getStyleClass().add("message");

        TextFlow textFlow = new TextFlow(text);
        textFlow.setTextAlignment(TextAlignment.JUSTIFY);
        textFlow.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

        VBox vBox = new VBox(textFlow);
        vBox.setAlignment(Pos.CENTER);

        HBox hBox = new HBox(vBox);
        hBox.setPadding(new Insets(30));
        hBox.setSpacing(20);
        AnchorPane.setRightAnchor(hBox, 20.0);
        AnchorPane.setLeftAnchor(hBox, 20.0);
        AnchorPane.setTopAnchor(hBox, 20.0);
        AnchorPane.setBottomAnchor(hBox, 20.0);
        hBox.setMaxWidth(500);
        hBox.setMinWidth(330);
        HBox.setHgrow(vBox, Priority.ALWAYS);

        if (image != null) {
            ImageView imageView = new ImageView(image);
            imageView.setFitHeight(50);
            imageView.setFitWidth(50);
            hBox.getChildren().add(0, imageView);
        }

        hBox.sceneProperty().addListener((observableScene, oldScene, newScene) -> {
            if (oldScene == null && newScene != null) {
                if (hBox.getScene().getStylesheets().isEmpty()) {
                    hBox.getScene().getStylesheets().add(0, "/com/bayoumi/css/controlsfx-notification.css");
                }
            }
        });

        new Draggable(hBox);
        return hBox;
    }

    public static void goodNotification() {
        Platform.runLater(() -> Notifications.create()
                .graphic(createGraphic("قُلْ هُوَ ٱللَّهُ أَحَدٌ ١ ٱللَّهُ ٱلصَّمَدُ ٢ لَمْ يَلِدْ وَلَمْ يُولَدْ ٣ وَلَمْ يَكُن لَّهُۥ كُفُوًا أَحَدٌۢ ٤" + "\n" + "text test" + new Random().nextInt(999), new Image("/com/bayoumi/images/Kaaba.png")))
                .hideAfter(Duration.seconds(10))
                .position(Pos.BOTTOM_RIGHT).show());
    }

    private void showNotification() {
        System.out.println("1: " + popup.getOwnerWindow());
        if (window == null) {
            Stage fakeStage = new Stage(StageStyle.UTILITY);
            fakeStage.setOpacity(0);
            fakeStage.show();
            fakeStage.toBack();
            window = fakeStage;
        }
        HBox node = createGraphic("text test" + new Random().nextInt(999), new Image("/com/bayoumi/images/Kaaba.png"));
        popup.getContent().add(node);
        System.out.println("2: " + popup.getOwnerWindow());
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        popup.show(window, bounds.getWidth() - (node.getWidth()), bounds.getHeight() - (node.getHeight()));
    }

    @Override
    public void start(Stage stage) {
        Button button = new Button("button");
        button.setOnAction(event -> {
            System.out.println("clicked");
            PauseTransition transition = new PauseTransition();
            transition.setDuration(Duration.seconds(1));
            transition.setOnFinished(event1 -> {
                System.out.println("HHHHHHHHHHH");
                if (popup.isShowing()) {
                    popup.hide();
                }
//                showNotification();
//                Notification.createNew("اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ ۚ لَّهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الْأَرْضِ ۗ مَن ذَا الَّذِي يَشْفَعُ عِندَهُ إِلَّا بِإِذْنِهِ ۚ يَعْلَمُ مَا بَيْنَ أَيْدِيهِمْ وَمَا خَلْفَهُمْ ۖ وَلَا يُحِيطُونَ بِشَيْءٍ مِّنْ عِلْمِهِ إِلَّا بِمَا شَاءَ ۚ وَسِعَ كُرْسِيُّهُ السَّمَاوَاتِ وَالْأَرْضَ ۖ وَلَا يَئُودُهُ حِفْظُهُمَا ۚ وَهُوَ الْعَلِيُّ الْعَظِيمُ ٥٥٢"+"\n"+"text test" + new Random().nextInt(999), new Image("/com/bayoumi/images/Kaaba.png"));
                goodNotification();
                transition.play();
            });
            transition.play();
        });


        TilePane tilepane = new TilePane();
        tilepane.getChildren().add(button);
        Scene scene = new Scene(tilepane, 200, 200);
        stage.setScene(scene);
        stage.show();
        System.out.println(stage);
        new TrayUtil(stage);
    }
}
*/