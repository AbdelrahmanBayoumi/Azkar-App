import java.io.File;

import javax.imageio.ImageIO;

import com.sun.javafx.scene.text.GlyphList;
import com.sun.javafx.scene.text.TextLayout;
import com.sun.javafx.tk.Toolkit;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public final class NotoRasterizationProbe extends Application {
    private static final int IMAGE_WIDTH = 900;
    private static final int IMAGE_HEIGHT = 140;
    private static final double POSITION_EPSILON = 0.01;
    private static final String SAMPLE =
            "مَرَّةٌ وَاحِدَةٌ — أَعُوذُ بِاللَّهِ مِنَ الشَّيْطَانِ الرَّجِيمِ";

    @Override
    public void start(Stage stage) throws Exception {
        if (getParameters().getRaw().size() != 2) {
            throw new IllegalArgumentException(
                    "Usage: NotoRasterizationProbe <NotoNaskhArabic-Bold.ttf> <output.png>");
        }

        File fontFile = new File(getParameters().getRaw().get(0));
        File outputFile = new File(getParameters().getRaw().get(1));
        Font font = Font.loadFont(fontFile.toURI().toURL().toExternalForm(), 36);
        if (font == null) {
            throw new IllegalStateException("Could not load font: " + fontFile);
        }
        assertPositionedGlyphOffsets(font);

        Text text = new Text(20, 90, SAMPLE);
        text.setFont(font);
        text.setFill(Color.BLACK);

        Group root = new Group(text);
        stage.setScene(new Scene(root, IMAGE_WIDTH, IMAGE_HEIGHT, Color.WHITE));
        root.applyCss();
        root.layout();

        WritableImage image = new WritableImage(IMAGE_WIDTH, IMAGE_HEIGHT);
        root.snapshot(null, image);
        ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", outputFile);

        System.out.println("fontFamily=" + font.getFamily()
                + ", fontName=" + font.getName()
                + ", fontStyle=" + font.getStyle()
                + ", fontSize=" + font.getSize());
        Platform.exit();
    }

    @SuppressWarnings("deprecation")
    private static void assertPositionedGlyphOffsets(Font font) {
        TextLayout layout = Toolkit.getToolkit().getTextLayoutFactory().createLayout();
        layout.setContent(SAMPLE, font.impl_getNativeFont());
        for (GlyphList glyphs : layout.getRuns()) {
            for (int glyphIndex = 0; glyphIndex < glyphs.getGlyphCount(); glyphIndex++) {
                if (Math.abs(glyphs.getPosY(glyphIndex)) > POSITION_EPSILON) {
                    System.out.println("positionedGlyphOffsets=true");
                    return;
                }
            }
        }
        throw new AssertionError("Diacritized Arabic has no positioned glyph offsets");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
