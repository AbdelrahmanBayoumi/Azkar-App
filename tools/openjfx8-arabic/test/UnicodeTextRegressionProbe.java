import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

/**
 * Exercises the Unicode range used by the reproducers attached to JDK-8246348.
 */
public final class UnicodeTextRegressionProbe extends Application {
    private static final int UNICODE_LIMIT = 10_000;

    @Override
    public void start(Stage stage) {
        VBox root = new VBox(10);
        root.getChildren().addAll(
                new Text(createUnicodeText(0)),
                new Text(createUnicodeText(1)));

        stage.setScene(new Scene(root, 800, 600));
        stage.show();
        root.applyCss();
        root.layout();
        root.snapshot(null, new WritableImage(800, 600));
        System.out.println("JDK-8246348 Unicode ranges rendered without a native crash");
        Platform.exit();
    }

    private static String createUnicodeText(int firstCodePoint) {
        StringBuilder text = new StringBuilder();
        for (int codePoint = firstCodePoint; codePoint < UNICODE_LIMIT; codePoint++) {
            text.append(Character.toChars(codePoint));
        }
        return text.toString();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
