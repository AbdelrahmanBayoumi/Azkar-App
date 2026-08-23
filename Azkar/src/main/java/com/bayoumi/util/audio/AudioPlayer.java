package com.bayoumi.util.audio;

import com.bayoumi.util.Logger;
import javafx.application.Platform;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ServiceConfigurationError;

/**
 * Cross-platform streaming audio player using the {@link javax.sound.sampled} API.
 * <p>
 * Replaces JavaFX {@code MediaPlayer} to avoid native GStreamer/GLib crashes on Linux.
 * Supports WAV natively, and MP3 via SPI providers (mp3spi) which are
 * automatically discovered from the classpath.
 * </p>
 * <p>
 * Streams audio chunks dynamically via {@link SourceDataLine} in a background worker thread.
 * </p>
 */
public class AudioPlayer {

    enum State {READY, PLAYING, STOP_REQUESTED, COMPLETED, FAILED, DISPOSED}

    interface LineProvider {
        SourceDataLine open(AudioFormat format) throws LineUnavailableException;
    }

    interface CallbackDispatcher {
        void dispatch(Runnable callback);
    }

    private static final long MAX_FILE_SIZE = 100 * 1024 * 1024;
    private static final LineProvider SYSTEM_LINE_PROVIDER = new LineProvider() {
        @Override
        public SourceDataLine open(AudioFormat format) throws LineUnavailableException {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            if (!AudioSystem.isLineSupported(info)) {
                throw new LineUnavailableException("Audio line not supported for format: " + format);
            }
            SourceDataLine sourceDataLine = (SourceDataLine) AudioSystem.getLine(info);
            sourceDataLine.open(format);
            return sourceDataLine;
        }
    };
    private static final CallbackDispatcher JAVAFX_DISPATCHER = new CallbackDispatcher() {
        @Override
        public void dispatch(Runnable callback) {
            Platform.runLater(callback);
        }
    };

    private final File audioFile;
    private final LineProvider lineProvider;
    private final CallbackDispatcher callbackDispatcher;
    private final Object lock = new Object();
    private AudioInputStream rawStream;
    private AudioInputStream decodedStream;
    private SourceDataLine line;
    private Runnable onEndOfMedia;
    private Runnable onStopped;
    private float currentVolume = 1.0f;
    private long volumeRevision;
    private boolean callbackClaimed;
    private volatile State state = State.READY;
    private Thread workerThread;

    public AudioPlayer(File audioFile) throws AudioPlayerException {
        this(audioFile, SYSTEM_LINE_PROVIDER, JAVAFX_DISPATCHER);
    }

    AudioPlayer(File audioFile, LineProvider lineProvider, CallbackDispatcher callbackDispatcher) throws AudioPlayerException {
        if (audioFile == null || !audioFile.isFile()) {
            throw new AudioPlayerException("Audio file is invalid or missing", null);
        }
        if (audioFile.length() > MAX_FILE_SIZE) {
            throw new AudioPlayerException("Audio file too large: " + (audioFile.length() / (1024 * 1024))
                    + " MB (max 100 MB)", null);
        }
        this.audioFile = audioFile;
        this.lineProvider = lineProvider;
        this.callbackDispatcher = callbackDispatcher;
        try {
            initStreamAndLine();
        } catch (Exception | LinkageError | ServiceConfigurationError e) {
            dispose();
            throw new AudioPlayerException("Error loading audio file: " + audioFile.getName(), e);
        }
    }

    private void initStreamAndLine() throws IOException, UnsupportedAudioFileException, LineUnavailableException {
        AudioInputStream openedRawStream;
        InputStream inputStream = null;
        try {
            inputStream = new BufferedInputStream(new FileInputStream(audioFile));
            openedRawStream = AudioSystem.getAudioInputStream(inputStream);
        } catch (UnsupportedAudioFileException | IOException e) {
            closeQuietly(inputStream);
            throw e;
        } catch (LinkageError | ServiceConfigurationError e) {
            closeQuietly(inputStream);
            throw e;
        }

        rawStream = openedRawStream;
        AudioFormat baseFormat = rawStream.getFormat();
        AudioFormat pcmFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(), 16,
                baseFormat.getChannels(), baseFormat.getChannels() * 2, baseFormat.getSampleRate(), false);
        AudioFormat targetFormat = baseFormat;
        if (AudioSystem.isConversionSupported(pcmFormat, baseFormat)) {
            decodedStream = AudioSystem.getAudioInputStream(pcmFormat, rawStream);
            targetFormat = pcmFormat;
        } else {
            decodedStream = rawStream;
        }
        line = lineProvider.open(targetFormat);
    }

    public void play() {
        Thread threadToStart;
        synchronized (lock) {
            if (state != State.READY) {
                return;
            }
            state = State.PLAYING;
            threadToStart = new Thread(this::playbackLoop, "AudioPlayer-Worker-" + audioFile.getName());
            threadToStart.setDaemon(true);
            workerThread = threadToStart;
        }
        while (true) {
            float volumeToApply;
            long revisionToApply;
            synchronized (lock) {
                if (state != State.PLAYING || workerThread != threadToStart) {
                    return;
                }
                volumeToApply = currentVolume;
                revisionToApply = volumeRevision;
            }
            applyVolumeToLine(volumeToApply);
            synchronized (lock) {
                if (state != State.PLAYING || workerThread != threadToStart) {
                    return;
                }
                if (volumeRevision == revisionToApply) {
                    threadToStart.start();
                    return;
                }
            }
        }
    }

    private void playbackLoop() {
        AudioInputStream streamSnapshot;
        SourceDataLine lineSnapshot;
        synchronized (lock) {
            streamSnapshot = decodedStream;
            lineSnapshot = line;
        }
        boolean naturalEof = false;
        if (streamSnapshot != null && lineSnapshot != null && state == State.PLAYING) {
            try {
                AudioFormat format = streamSnapshot.getFormat();
                int frameSize = format.getFrameSize();
                int bufferSize = frameSize > 0 ? Math.max(frameSize, (8192 / frameSize) * frameSize) : 8192;
                byte[] buffer = new byte[bufferSize];
                lineSnapshot.start();
                int bytesRead = -1;
                while (state == State.PLAYING && (bytesRead = streamSnapshot.read(buffer, 0, buffer.length)) != -1) {
                    if (bytesRead > 0) {
                        lineSnapshot.write(buffer, 0, bytesRead);
                    }
                }
                if (state == State.PLAYING && bytesRead == -1) {
                    lineSnapshot.drain();
                    synchronized (lock) {
                        if (state == State.PLAYING) {
                            state = State.COMPLETED;
                            naturalEof = true;
                        }
                    }
                }
            } catch (Exception | LinkageError | ServiceConfigurationError e) {
                boolean playbackFailed = false;
                synchronized (lock) {
                    if (state == State.PLAYING) {
                        state = State.FAILED;
                        playbackFailed = true;
                    }
                }
                if (playbackFailed) {
                    Logger.error("Error during audio playback: " + audioFile.getName(), e, "AudioPlayer");
                }
            }
        }
        cleanupLineAndStreams();
        triggerCallbacks(naturalEof);
    }

    private void triggerCallbacks(boolean naturalEof) {
        Runnable callback = null;
        synchronized (lock) {
            if (!callbackClaimed) {
                callbackClaimed = true;
                if (naturalEof) {
                    callback = onEndOfMedia;
                } else {
                    callback = onStopped;
                }
            }
        }
        if (callback != null) {
            callbackDispatcher.dispatch(callback);
        }
    }

    public void stop() {
        dispose();
    }

    public void dispose() {
        SourceDataLine lineToStop = null;
        Thread threadToInterrupt = null;
        boolean stoppedBeforeWorkerStarted = false;
        synchronized (lock) {
            if (state == State.DISPOSED) {
                return;
            }
            if (state == State.READY) {
                state = State.DISPOSED;
            } else {
                stoppedBeforeWorkerStarted = state == State.PLAYING && workerThread != null
                        && workerThread.getState() == Thread.State.NEW;
                state = State.STOP_REQUESTED;
                lineToStop = line;
                threadToInterrupt = workerThread;
            }
        }
        stopAndFlush(lineToStop);
        if (threadToInterrupt != null && Thread.currentThread() != threadToInterrupt) {
            threadToInterrupt.interrupt();
        }
        cleanupLineAndStreams();
        synchronized (lock) {
            state = State.DISPOSED;
        }
        if (stoppedBeforeWorkerStarted) {
            triggerCallbacks(false);
        }
    }

    private static void stopAndFlush(SourceDataLine lineToStop) {
        if (lineToStop == null) {
            return;
        }
        try {
            if (lineToStop.isRunning()) {
                lineToStop.stop();
            }
            lineToStop.flush();
        } catch (Exception | LinkageError | ServiceConfigurationError e) {
            Logger.debug("Error stopping audio line: " + e.getMessage());
        }
    }

    private void cleanupLineAndStreams() {
        SourceDataLine lineToClose;
        AudioInputStream decodedToClose;
        AudioInputStream rawToClose;
        synchronized (lock) {
            lineToClose = line;
            decodedToClose = decodedStream;
            rawToClose = rawStream;
            line = null;
            decodedStream = null;
            rawStream = null;
        }
        if (lineToClose != null) {
            try {
                lineToClose.close();
            } catch (Exception | LinkageError | ServiceConfigurationError e) {
                Logger.debug("Error closing audio line: " + e.getMessage());
            }
        }
        closeQuietly(decodedToClose);
        if (decodedToClose != rawToClose) {
            closeQuietly(rawToClose);
        }
    }

    private static void closeQuietly(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception | LinkageError | ServiceConfigurationError e) {
            Logger.debug("Error closing resource: " + e.getMessage());
        }
    }

    public boolean isPlaying() {
        synchronized (lock) {
            return state == State.PLAYING && line != null;
        }
    }

    public void setVolume(double volume) {
        float normalizedVolume = (float) Math.max(0.0, Math.min(1.0, volume));
        synchronized (lock) {
            currentVolume = normalizedVolume;
            volumeRevision++;
        }
        applyVolumeToLine(normalizedVolume);
    }

    private void applyVolumeToLine(float volume) {
        SourceDataLine lineSnapshot;
        synchronized (lock) {
            lineSnapshot = line;
        }
        if (lineSnapshot == null || !lineSnapshot.isOpen()) {
            return;
        }
        try {
            if (lineSnapshot.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gain = (FloatControl) lineSnapshot.getControl(FloatControl.Type.MASTER_GAIN);
                float decibels = volume <= 0.0f ? gain.getMinimum()
                        : (float) Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), 20.0 * Math.log10(volume)));
                gain.setValue(decibels);
            }
        } catch (Exception | LinkageError | ServiceConfigurationError e) {
            Logger.debug("Volume control not available: " + e.getMessage());
        }
    }

    public void setOnEndOfMedia(Runnable callback) {
        synchronized (lock) {
            onEndOfMedia = callback;
        }
    }

    public void setOnStopped(Runnable callback) {
        synchronized (lock) {
            onStopped = callback;
        }
    }

    State getState() {
        return state;
    }

    public static class AudioPlayerException extends Exception {
        public AudioPlayerException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
