import java.io.File;

import javax.imageio.ImageIO;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

public final class BidiPunctuationProbe extends Application {
    private static final int IMAGE_WIDTH = 820;
    private static final int IMAGE_HEIGHT = 430;

    private static final String FADL =
            "[سورة البقرة، الآية: 255] من قالها حين يصبح أجير من الجن حتى يمسي، "
            + "ومن قالها حين يمسي أجير منهم حتى يصبح";
    private static final String DESCRIPTION =
            "(والمعوذتين) أي: {قل أعوذ برب الفلق} و {قل أعوذ برب الناس}.";
    private static final String AYAH =
            "﴿وَمِنَ اللَّيْلِ فَتَهَجَّدْ بِهِ نَافِلَةً لَّكَ عَسَىٰ "
            + "أَن يَبْعَثَكَ رَبُّكَ مَقَامًا مَّحْمُودًا﴾";

    @Override
    public void start(Stage stage) throws Exception {
        if (getParameters().getRaw().size() != 2) {
            throw new IllegalArgumentException(
                    "Usage: BidiPunctuationProbe <NotoNaskhArabic-Bold.ttf> <output.png>");
        }

        assertMirroringMetadata();

        File fontFile = new File(getParameters().getRaw().get(0));
        File outputFile = new File(getParameters().getRaw().get(1));
        Font loadedFont = Font.loadFont(fontFile.toURI().toURL().toExternalForm(), 26);
        if (loadedFont == null) {
            throw new IllegalStateException("Could not load font: " + fontFile);
        }
        Font font = Font.font(loadedFont.getFamily(), FontWeight.BOLD, 26);

        VBox root = new VBox(20);
        root.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: white;");
        root.getChildren().add(createRow("FADL", FADL, font));
        root.getChildren().add(createRow("DESCRIPTION", DESCRIPTION, font));
        root.getChildren().add(createRow("AYAH", AYAH, font));

        stage.setScene(new Scene(root, IMAGE_WIDTH, IMAGE_HEIGHT, Color.WHITE));
        root.applyCss();
        root.layout();

        WritableImage image = new WritableImage(IMAGE_WIDTH, IMAGE_HEIGHT);
        root.snapshot(null, image);
        ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", outputFile);

        System.out.println("font=" + font.getName()
                + ", asciiMirrored=" + Character.isMirrored('[')
                + ", ornateMirrored=" + Character.isMirrored('﴿'));
        Platform.exit();
    }

    private static VBox createRow(String name, String value, Font font) {
        Text title = new Text(name + ":");
        title.setFont(Font.font("Sans", FontWeight.BOLD, 14));

        Text text = new Text(value);
        text.setFont(font);

        TextFlow flow = new TextFlow(text);
        flow.setPrefWidth(720);
        flow.setMaxWidth(720);
        flow.setTextAlignment(TextAlignment.CENTER);

        VBox row = new VBox(4, title, flow);
        row.setPadding(new Insets(12));
        row.setStyle("-fx-border-color: #aaaaaa;");
        return row;
    }

    private static void assertMirroringMetadata() {
        if (!Character.isMirrored('(')
                || !Character.isMirrored('[')
                || !Character.isMirrored('{')) {
            throw new AssertionError("ASCII paired punctuation must have Unicode mirroring metadata");
        }
        if (Character.isMirrored('﴿') || Character.isMirrored('﴾')) {
            throw new AssertionError("Ornate Arabic parentheses must not be mirrored automatically");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
