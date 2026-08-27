import java.io.File;

import javax.imageio.ImageIO;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.scene.Scene;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

public final class ArabicWrappingProbe extends Application {
    private static final int IMAGE_WIDTH = 800;
    private static final int IMAGE_HEIGHT = 350;
    private static final int EXPECTED_LINE_COUNT = 4;
    private static final int MINIMUM_BLANK_ROWS_BETWEEN_LINES = 4;
    private static final double INK_THRESHOLD = 0.85;

    private static final String TEXT =
            "أَعُوذُ بِاللَّهِ مِنَ الشَّيطَانِ الرَّجِيمِ ﴿اللَّهُ لاَ إِلَهَ إِلاَّ هُوَ الْحَيُّ الْقَيُّومُ "
            + "لاَ تَأْخُذُهُ سِنَةٌ وَلاَ نَوْمٌ لَّهُ مَا فِي السَّمَوَاتِ وَمَا فِي الأَرْضِ مَن ذَا الَّذِي "
            + "يَشْفَعُ عِنْدَهُ إِلاَّ بِإِذْنِهِ يَعْلَمُ مَا بَيْنَ أَيْدِيهِمْ وَمَا خَلْفَهُمْ وَلاَ يُحِيطُونَ "
            + "بِشَيْءٍ مِّنْ عِلْمِهِ إِلاَّ بِمَا شَاء وَسِعَ كُرْسِيُّهُ السَّمَوَاتِ وَالْأَرْضَ وَلاَ يَؤُودُهُ "
            + "حِفْظُهُمَا وَهُوَ الْعَلِيُّ الْعَظِيمُ﴾";

    @Override
    public void start(Stage stage) throws Exception {
        if (getParameters().getRaw().size() != 2) {
            throw new IllegalArgumentException(
                    "Usage: ArabicWrappingProbe <NotoNaskhArabic-Bold.ttf> <output.png>");
        }

        File fontFile = new File(getParameters().getRaw().get(0));
        File outputFile = new File(getParameters().getRaw().get(1));
        Font font = Font.loadFont(fontFile.toURI().toURL().toExternalForm(), 22);
        if (font == null) {
            throw new IllegalStateException("Could not load font: " + fontFile);
        }

        Text text = new Text(TEXT);
        text.setFont(font);
        text.setCache(true);

        TextFlow flow = new TextFlow(text);
        flow.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        flow.setTextAlignment(TextAlignment.CENTER);
        flow.setPrefWidth(720);
        flow.setMaxWidth(720);
        flow.setPadding(new Insets(20));

        StackPane root = new StackPane(flow);
        root.setPadding(new Insets(20));
        stage.setScene(new Scene(root, IMAGE_WIDTH, IMAGE_HEIGHT, Color.WHITE));
        root.applyCss();
        root.layout();

        WritableImage image = new WritableImage(IMAGE_WIDTH, IMAGE_HEIGHT);
        root.snapshot(null, image);
        ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", outputFile);

        int lineCount = countInkLines(image);
        System.out.println("font=" + font.getName() + ", visualLines=" + lineCount);
        Platform.exit();
        if (lineCount != EXPECTED_LINE_COUNT) {
            throw new AssertionError(
                    "Expected " + EXPECTED_LINE_COUNT + " Arabic lines, found " + lineCount
                    + ". Inspect " + outputFile);
        }
    }

    private static int countInkLines(WritableImage image) {
        PixelReader pixels = image.getPixelReader();
        int lineCount = 0;
        int blankRows = MINIMUM_BLANK_ROWS_BETWEEN_LINES;

        for (int y = 0; y < IMAGE_HEIGHT; y++) {
            boolean inkInRow = false;
            for (int x = 0; x < IMAGE_WIDTH && !inkInRow; x++) {
                Color color = pixels.getColor(x, y);
                inkInRow = color.getOpacity() > 0.5
                        && color.getRed() < INK_THRESHOLD
                        && color.getGreen() < INK_THRESHOLD
                        && color.getBlue() < INK_THRESHOLD;
            }

            if (inkInRow && blankRows >= MINIMUM_BLANK_ROWS_BETWEEN_LINES) {
                lineCount++;
            }
            blankRows = inkInRow ? 0 : blankRows + 1;
        }
        return lineCount;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
