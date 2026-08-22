package com.bayoumi.util.gui;

import com.bayoumi.Launcher;
import com.bayoumi.models.settings.NotificationColor;
import com.bayoumi.models.settings.Settings;
import com.bayoumi.models.settings.Theme;
import com.bayoumi.util.Logger;
import com.bayoumi.util.SystemThemeUtil;
import javafx.application.Platform;
import javafx.stage.Window;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public final class ThemeUtil {
    private static Timer watchTimer;

    private ThemeUtil() {
    }

    public static void applyUserTheme(Theme theme) {
        final Theme previous = Settings.getInstance().getTheme();
        final boolean resolvedBefore = Settings.getInstance().getNightMode();
        Settings.getInstance().setTheme(theme);
        applyToOpenWindows();
        final boolean resolvedAfter = Settings.getInstance().getNightMode();
        if (previous != theme || resolvedBefore != resolvedAfter) {
            applyNotificationColors(resolvedAfter);
        }
    }

    public static void applyResolvedTheme() {
        applyToOpenWindows();
        applyNotificationColors(Settings.getInstance().getNightMode());
    }

    public static void applyToOpenWindows() {
        final String[] css = Settings.getInstance().getThemeFilesCSS();
        boolean applied = applyStylesheets(css);
        if (!applied && Launcher.homeController != null) {
            Launcher.homeController.changeTheme();
        }
    }

    public static synchronized void startSystemThemeWatcher() {
        if (watchTimer != null) {
            return;
        }
        watchTimer = new Timer("system-theme-watch", true);
        watchTimer.scheduleAtFixedRate(new TimerTask() {
            private Boolean lastResolvedDark;

            @Override
            public void run() {
                try {
                    if (Settings.getInstance().getTheme() != Theme.SYSTEM) {
                        lastResolvedDark = null;
                        return;
                    }
                    SystemThemeUtil.invalidateCache();
                    final boolean isDark = Settings.getInstance().getNightMode();
                    if (lastResolvedDark != null && lastResolvedDark != isDark) {
                        lastResolvedDark = isDark;
                        Platform.runLater(ThemeUtil::applyResolvedTheme);
                    } else {
                        lastResolvedDark = isDark;
                    }
                } catch (Exception ex) {
                    Logger.error(null, ex, ThemeUtil.class.getName() + ".watch");
                }
            }
        }, 2000, 2000);
    }

    private static void applyNotificationColors(boolean isDark) {
        if (isDark) {
            NotificationColor.setDarkTheme();
        } else {
            NotificationColor.setLightTheme();
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean applyStylesheets(String[] css) {
        boolean applied = false;
        try {
            final Method getWindows = Window.class.getMethod("getWindows");
            final List<Window> windows = (List<Window>) getWindows.invoke(null);
            for (Window window : windows) {
                if (applyToWindow(window, css)) {
                    applied = true;
                }
            }
            return applied;
        } catch (Exception ignored) {
        }
        try {
            final Method implGetWindows = Window.class.getMethod("impl_getWindows");
            final Iterator<Window> windows = (Iterator<Window>) implGetWindows.invoke(null);
            while (windows.hasNext()) {
                if (applyToWindow(windows.next(), css)) {
                    applied = true;
                }
            }
        } catch (Exception ex) {
            Logger.error(null, ex, ThemeUtil.class.getName() + ".applyStylesheets()");
        }
        return applied;
    }

    private static boolean applyToWindow(Window window, String[] css) {
        if (window == null || window.getScene() == null) {
            return false;
        }
        window.getScene().getStylesheets().setAll(css);
        return true;
    }
}
