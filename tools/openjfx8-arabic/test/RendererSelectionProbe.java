import com.bayoumi.util.gui.ArabicTextSupport;

public final class RendererSelectionProbe {
    public static void main(String[] args) {
        ArabicTextSupport.fix();
        boolean patched = "native".equals(args[0]);
        if (!args[0].equals(System.getProperty("prism.text"))
                || !Boolean.toString(patched).equals(System.getProperty("prism.lcdtext"))) {
            throw new AssertionError("Unexpected renderer: " + System.getProperty("prism.text"));
        }
        System.out.println("Application renderer settings: " + args[0]);
    }
}
