import java.io.File;
import java.nio.IntBuffer;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import com.sun.javafx.font.FontResource;
import com.sun.javafx.font.FontStrike;
import com.sun.javafx.font.PGFont;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.text.GlyphList;
import com.sun.javafx.scene.text.TextLayout;
import com.sun.javafx.tk.RenderJob;
import com.sun.javafx.tk.Toolkit;
import com.sun.prism.Graphics;
import com.sun.prism.GraphicsPipeline;
import com.sun.prism.RTTexture;
import com.sun.prism.Texture;
import com.sun.prism.impl.ps.BaseShaderGraphics;
import com.sun.prism.paint.Color;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public final class GlyphCacheClippingProbe extends Application {
    private static final int WIDTH = 64;
    private static final int HEIGHT = 64;
    private static final int TEXT_X = 12;
    private static final int CLIP_RIGHT = TEXT_X + 9;

    @Override
    @SuppressWarnings("deprecation")
    public void start(Stage stage) throws Exception {
        Font font = Font.loadFont(new File(getParameters().getRaw().get(0)).toURI().toString(), 36);
        if (font == null) throw new AssertionError("Could not load the test font");

        Toolkit toolkit = Toolkit.getToolkit();
        TextLayout layout = toolkit.getTextLayoutFactory().createLayout();
        layout.setDirection(TextLayout.DIRECTION_RTL);
        layout.setContent("بَ", font.impl_getNativeFont());
        GlyphList run = layout.getRuns()[0];
        if (run.getPosX(0) <= CLIP_RIGHT - TEXT_X
                || run.getPosX(run.getGlyphCount() - 1) >= CLIP_RIGHT - TEXT_X) {
            throw new AssertionError("The combining mark must precede the visible base across the clip");
        }
        PGFont nativeFont = (PGFont)font.impl_getNativeFont();
        FontStrike strike = nativeFont.getStrike(BaseTransform.IDENTITY_TRANSFORM, FontResource.AA_GREYSCALE);
        FutureTask<Void> check = new FutureTask<>(() -> {
            verifyClip(run, strike);
            return null;
        });
        toolkit.addRenderJob(new RenderJob(check));
        check.get(15, TimeUnit.SECONDS);
        Platform.exit();
    }

    private static void verifyClip(GlyphList run, FontStrike strike) {
        int[] full = draw(run, strike, WIDTH);
        int[] clipped = draw(run, strike, CLIP_RIGHT);
        int ink = 0;
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int index = y * WIDTH + x;
                if (x < CLIP_RIGHT && full[index] != 0xffffffff) ink++;
                int expected = x < CLIP_RIGHT ? full[index] : 0xffffffff;
                if (clipped[index] != expected) {
                    throw new AssertionError("Incorrect clipped pixel at " + x + ", " + y);
                }
            }
        }
        if (ink == 0) throw new AssertionError("Clip must intersect the base glyph");
        System.out.println("Accelerated Arabic glyph clipping preserved " + ink + " ink pixels");
    }

    private static int[] draw(GlyphList run, FontStrike strike, int right) {
        RTTexture texture = GraphicsPipeline.getDefaultResourceFactory()
                .createRTTexture(WIDTH, HEIGHT, Texture.WrapMode.CLAMP_NOT_NEEDED);
        try {
            Graphics graphics = texture.createGraphics();
            if (!(graphics instanceof BaseShaderGraphics)) {
                throw new AssertionError("Prism shader pipeline required for glyph-cache clipping");
            }
            graphics.clear(Color.WHITE);
            graphics.setClipRect(new Rectangle(0, 0, right, HEIGHT));
            graphics.setPaint(Color.BLACK);
            graphics.drawString(run, strike, TEXT_X, 42, null, 0, 0);
            graphics.sync();
            IntBuffer pixels = IntBuffer.allocate(WIDTH * HEIGHT);
            if (!texture.readPixels(pixels)) throw new AssertionError("Could not read Prism output");
            return pixels.array();
        } finally {
            texture.dispose();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
