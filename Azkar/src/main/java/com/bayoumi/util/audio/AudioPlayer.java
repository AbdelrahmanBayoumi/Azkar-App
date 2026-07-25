package com.bayoumi.util.audio;

import com.bayoumi.util.Logger;
import javafx.application.Platform;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ServiceConfigurationError;

/**
 * Cross-platform audio player using javax.sound.sampled API.
 * <p>
 * Replaces JavaFX MediaPlayer to avoid GStreamer/GLib native crashes on Linux.
 * Supports WAV natively, and MP3/OGG via SPI providers (mp3spi, vorbisspi)
 * which are automatically discovered from the classpath.
 * </p>
 */
public class AudioPlayer {

    private Clip clip;
    private Runnable onEndOfMedia;
    private Runnable onStopped;
    private volatile boolean playing = false;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

    /**
     * Creates an AudioPlayer for the given audio file.
     * Supports WAV, MP3 (via mp3spi), and OGG (via vorbisspi).
     *
     * @param audioFile the audio file to play
     * @throws AudioPlayerException if the file cannot be loaded
     */
    public AudioPlayer(File audioFile) throws AudioPlayerException {
        if (audioFile.length() > MAX_FILE_SIZE) {
            throw new AudioPlayerException(
                    "Audio file too large: " + (audioFile.length() / (1024 * 1024)) + " MB (max 10 MB)",
                    null);
        }
        AudioInputStream rawStream = null;
        AudioInputStream decodedStream = null;
        try {
            // BufferedInputStream is required for mp3spi to handle mark/reset
            rawStream = AudioSystem.getAudioInputStream(
                    new BufferedInputStream(new FileInputStream(audioFile))
            );

            // Decode compressed formats (MP3/OGG) to PCM
            AudioFormat baseFormat = rawStream.getFormat();
            AudioFormat decodedFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    baseFormat.getSampleRate(),
                    16,
                    baseFormat.getChannels(),
                    baseFormat.getChannels() * 2,
                    baseFormat.getSampleRate(),
                    false
            );

            if (AudioSystem.isConversionSupported(decodedFormat, baseFormat)) {
                decodedStream = AudioSystem.getAudioInputStream(decodedFormat, rawStream);
            } else {
                // Already in a compatible format (e.g., WAV PCM)
                decodedStream = rawStream;
            }

            clip = AudioSystem.getClip();
            clip.open(decodedStream);

            final Clip finalClip = clip;
            clip.addLineListener(event -> {
                try {
                    if (event.getType() == LineEvent.Type.STOP) {
                        boolean isOpen = finalClip.isOpen();
                        if (playing && isOpen && finalClip.getMicrosecondPosition() >= finalClip.getMicrosecondLength()) {
                            // Reached end of media
                            playing = false;
                            if (onEndOfMedia != null) {
                                Platform.runLater(onEndOfMedia);
                            }
                            dispose();
                        } else if (!playing) {
                            // Manually stopped
                            if (onStopped != null) {
                                Platform.runLater(onStopped);
                            }
                        }
                    }
                } catch (Exception e) {
                    Logger.error("Exception in AudioPlayer LineListener", e, "AudioPlayer");
                }
            });
        } catch (Exception | LinkageError | ServiceConfigurationError e) {
            dispose();
            // Wrap SPI decoding, native linkage, and I/O exceptions into AudioPlayerException
            throw new AudioPlayerException("Error loading audio file: " + audioFile.getName(), e);
        } finally {
            closeQuietly(decodedStream);
            if (decodedStream != rawStream) {
                closeQuietly(rawStream);
            }
        }
    }

    private static void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Starts playback from the beginning.
     */
    public void play() {
        if (clip != null) {
            clip.setFramePosition(0);
            playing = true;
            clip.start();
        }
    }

    /**
     * Stops playback and releases all resources.
     */
    public void stop() {
        dispose();
    }

    /**
     * Stops playback and releases all resources.
     * After calling dispose(), this AudioPlayer cannot be reused.
     */
    public void dispose() {
        if (clip != null) {
            playing = false;
            try {
                if (clip.isRunning()) {
                    clip.stop();
                }
                clip.close();
            } catch (Exception ignored) {
            }
            clip = null;
        }
    }

    /**
     * @return true if audio is currently playing
     */
    public boolean isPlaying() {
        return clip != null && playing && clip.isRunning();
    }

    /**
     * Sets the volume level.
     *
     * @param volume value between 0.0 (mute) and 1.0 (max)
     */
    public void setVolume(double volume) {
        if (clip == null) return;
        try {
            FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            if (volume <= 0.0) {
                gainControl.setValue(gainControl.getMinimum());
            } else {
                // Convert linear 0.0-1.0 to dB scale
                float dB = (float) (20.0 * Math.log10(Math.max(volume, 0.0001)));
                // Clamp to the control's range
                dB = Math.max(dB, gainControl.getMinimum());
                dB = Math.min(dB, gainControl.getMaximum());
                gainControl.setValue(dB);
            }
        } catch (IllegalArgumentException e) {
            // MASTER_GAIN not supported on this platform — ignore silently
            Logger.debug("Volume control not available: " + e.getMessage());
        }
    }

    /**
     * Sets a callback to run (on the JavaFX Application Thread) when playback finishes naturally.
     */
    public void setOnEndOfMedia(Runnable callback) {
        this.onEndOfMedia = callback;
    }

    /**
     * Sets a callback to run (on the JavaFX Application Thread) when playback is stopped manually.
     */
    public void setOnStopped(Runnable callback) {
        this.onStopped = callback;
    }


    /**
     * Custom exception for audio player errors.
     */
    public static class AudioPlayerException extends Exception {
        public AudioPlayerException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
