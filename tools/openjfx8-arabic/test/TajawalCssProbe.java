import java.io.File;

import javax.imageio.ImageIO;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public final class TajawalCssProbe extends Application {
    private static final String EXPECTED_FAMILY = "Azkar Tajawal Medium";

    @Override
    public void start(Stage stage) throws Exception {
        if (getParameters().getRaw().size() != 4) {
            throw new IllegalArgumentException(
                    "Usage: TajawalCssProbe <base.css> <theme.css> <Tajawal-Medium.ttf> <output.png>");
        }

        File baseStylesheet = new File(getParameters().getRaw().get(0));
        File themeStylesheet = new File(getParameters().getRaw().get(1));
        File fontFile = new File(getParameters().getRaw().get(2));
        File outputFile = new File(getParameters().getRaw().get(3));
        Font loadedFont = Font.loadFont(fontFile.toURI().toURL().toExternalForm(), 25);
        if (loadedFont == null || !EXPECTED_FAMILY.equals(loadedFont.getFamily())) {
            throw new AssertionError("Unexpected embedded font family: " + loadedFont);
        }

        Label label = new Label("الفجر — الشروق — أذكار الصباح والمساء");
        label.getStyleClass().add("font-text-medium");
        VBox root = new VBox(label);
        root.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        root.setPadding(new Insets(20));

        Scene scene = new Scene(root, 900, 120);
        scene.getStylesheets().addAll(
                baseStylesheet.toURI().toURL().toExternalForm(),
                themeStylesheet.toURI().toURL().toExternalForm());
        stage.setScene(scene);
        root.applyCss();
        root.layout();

        String selectedFamily = label.getFont().getFamily();
        System.out.println("Tajawal CSS family=" + selectedFamily);
        if (!EXPECTED_FAMILY.equals(selectedFamily)) {
            throw new AssertionError(
                    "CSS selected " + selectedFamily + " instead of " + EXPECTED_FAMILY);
        }

        WritableImage image = new WritableImage(900, 120);
        root.snapshot(null, image);
        ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", outputFile);
        Platform.exit();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
