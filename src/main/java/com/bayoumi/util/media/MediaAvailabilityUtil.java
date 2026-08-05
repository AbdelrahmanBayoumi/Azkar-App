package com.bayoumi.util.media;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

public class MediaAvailabilityUtil {
    private static Boolean mediaAvailable = null;
    
    public static boolean isMediaAvailable() {
        if (mediaAvailable == null) {
            try {
                // Attempt to initialize a minimal media object
                String silentAudioBase64 = "data:audio/wav;base64,UklGRjIAAABXQVZFZm10IBIAAAABAAEAQB8AAEAfAAABAAgAAABMSVNUAgAAAAEAAGRhdGEAAAAA";
                Media media = new Media(silentAudioBase64);
                MediaPlayer player = new MediaPlayer(media);
                player.dispose(); // Clean up immediately
                mediaAvailable = true;
            } catch (Throwable e) {
                // Catch any error indicating native media failure
                mediaAvailable = false;
            }
        }
        return mediaAvailable;
    }
}