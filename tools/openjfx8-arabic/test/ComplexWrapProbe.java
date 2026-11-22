import com.sun.javafx.scene.text.GlyphList;
import com.sun.javafx.scene.text.TextLayout;
import com.sun.javafx.text.TextRun;
import com.sun.javafx.tk.Toolkit;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public final class ComplexWrapProbe extends Application {
    private static final String MIXED = "🌟🌟🌟🌟🌟أَعُوذُ بِاللَّهِ";

    @Override
    @SuppressWarnings("deprecation")
    public void start(Stage stage) {
        Font font = Font.loadFont(new java.io.File(getParameters().getRaw().get(0)).toURI().toString(), 36);
        if (font == null) throw new AssertionError("Could not load the test font");
        verifyWholeCharacters(font.impl_getNativeFont(), 15);
        verifyWholeCharacters(font.impl_getNativeFont(), 1);
        verifyRtlAdvances(font.impl_getNativeFont());
        verifyWrapAcrossFallbackFont(Font.font("DejaVu Sans", 36).impl_getNativeFont());
        System.out.println("UTF-16 boundaries, RTL advances, and fallback-font wrapping passed");
        Platform.exit();
    }

    private static TextLayout layout(String text, Object font, float wrap) {
        TextLayout layout = Toolkit.getToolkit().getTextLayoutFactory().createLayout();
        layout.setDirection(TextLayout.DIRECTION_RTL);
        layout.setContent(text, font);
        layout.setWrapWidth(wrap);
        return layout;
    }

    private static void verifyWholeCharacters(Object font, float width) {
        int characters = 0;
        for (GlyphList glyphs : layout(MIXED, font, width).getRuns()) {
            TextRun run = (TextRun)glyphs;
            int start = run.getStart();
            int end = run.getEnd();
            if (Character.isLowSurrogate(MIXED.charAt(start))
                    || Character.isHighSurrogate(MIXED.charAt(end - 1))
                    || run.getGlyphCount() == 0) {
                throw new AssertionError("Wrapped UTF-16 span [" + start + "," + end + ") lost glyphs or split a pair");
            }
            characters += run.getLength();
        }
        if (characters != MIXED.length()) throw new AssertionError("Wrapped text lost characters");
    }

    private static void verifyRtlAdvances(Object font) {
        for (GlyphList glyphs : layout("مَرَّةٌ وَاحِدَةٌ", font, 0).getRuns()) {
            TextRun run = (TextRun)glyphs;
            if (run.isLeftToRight()) throw new AssertionError("Expected an RTL glyph run");
            if (run.getWrapIndex(0) != 0) throw new AssertionError("No character fits the remaining width");
            for (int index = 0; index < run.getGlyphCount(); index++) {
                if (run.getAdvance(index) < 0) {
                    throw new AssertionError("Mark drawing offsets must preserve logical advances");
                }
            }
        }
    }

    private static void verifyWrapAcrossFallbackFont(Object font) {
        TextRun run = (TextRun) layout("أب🌟جد", font, 0).getRuns()[0];
        if (run.isLeftToRight() || run.getLength() != 6 || run.getGlyphCount() != 5) {
            throw new AssertionError("Expected a single RTL run with an embedded emoji");
        }
        if (run.getWrapIndex(90) != 4) {
            throw new AssertionError("RTL wrap expected at offset 4, got " + run.getWrapIndex(90));
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
