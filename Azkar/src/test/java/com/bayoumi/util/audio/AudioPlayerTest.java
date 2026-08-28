package com.bayoumi.util.audio;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.Control;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.AudioSystem;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class AudioPlayerTest {
    private static final long TIMEOUT_SECONDS = 3;

    @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

    private File createValidWavFile(String fileName, int frames) throws Exception {
        AudioFormat format = new AudioFormat(44100.0f, 16, 1, true, false);
        AudioInputStream stream = new AudioInputStream(new ByteArrayInputStream(new byte[frames * format.getFrameSize()]), format, frames);
        File wavFile = new File(tempFolder.getRoot(), fileName);
        AudioSystem.write(stream, AudioFileFormat.Type.WAVE, wavFile);
        return wavFile;
    }

    private AudioPlayer player(File file, FakeLine line) throws Exception {
        return new AudioPlayer(file, new AudioPlayer.LineProvider() {
            @Override public SourceDataLine open(AudioFormat format) { line.open(format); return line; }
        }, new AudioPlayer.CallbackDispatcher() {
            @Override public void dispatch(Runnable callback) { callback.run(); }
        });
    }

    private static void await(CountDownLatch latch) throws Exception {
        assertTrue("timed out waiting for playback", latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
    }

    @Test(expected = AudioPlayer.AudioPlayerException.class)
    public void missingFileThrowsException() throws Exception { new AudioPlayer(new File(tempFolder.getRoot(), "missing.wav")); }

    @Test(expected = AudioPlayer.AudioPlayerException.class)
    public void oversizedFileThrowsException() throws Exception {
        File largeFile = tempFolder.newFile("large.wav");
        try (RandomAccessFile file = new RandomAccessFile(largeFile, "rw")) { file.setLength(101L * 1024 * 1024); }
        new AudioPlayer(largeFile);
    }

    @Test
    public void validWavPlayThenImmediateManualStopIsSafe() throws Exception {
        FakeLine line = new FakeLine(); line.blockWrite = true;
        AudioPlayer player = player(createValidWavFile("manual.wav", 44100), line);
        player.play();
        player.stop();
        assertFalse(player.isPlaying());
    }

    @Test
    public void manualStopInvokesStoppedExactlyOnce() throws Exception {
        FakeLine line = new FakeLine(); line.blockWrite = true;
        AudioPlayer player = player(createValidWavFile("stop.wav", 44100), line);
        AtomicInteger stopped = new AtomicInteger();
        CountDownLatch callback = new CountDownLatch(1);
        player.setOnStopped(new Runnable() { @Override public void run() { stopped.incrementAndGet(); callback.countDown(); } });
        player.play(); await(line.started); player.stop(); await(callback);
        assertEquals(1, stopped.get());
    }

    @Test
    public void naturalEofInvokesEndExactlyOnceAndNotStopped() throws Exception {
        FakeLine line = new FakeLine();
        AudioPlayer player = player(createValidWavFile("eof.wav", 2), line);
        AtomicInteger ended = new AtomicInteger();
        AtomicInteger stopped = new AtomicInteger();
        CountDownLatch callback = new CountDownLatch(1);
        player.setOnEndOfMedia(new Runnable() { @Override public void run() { ended.incrementAndGet(); callback.countDown(); } });
        player.setOnStopped(new Runnable() { @Override public void run() { stopped.incrementAndGet(); } });
        player.play(); await(callback);
        assertEquals(1, ended.get());
        assertEquals(0, stopped.get());
    }

    @Test
    public void stopWhileDrainBlockedWinsOverNaturalEof() throws Exception {
        FakeLine line = new FakeLine(); line.blockDrain = true;
        AudioPlayer player = player(createValidWavFile("drain.wav", 2), line);
        AtomicInteger ended = new AtomicInteger();
        AtomicInteger stopped = new AtomicInteger();
        CountDownLatch callback = new CountDownLatch(1);
        player.setOnEndOfMedia(new Runnable() { @Override public void run() { ended.incrementAndGet(); } });
        player.setOnStopped(new Runnable() { @Override public void run() { stopped.incrementAndGet(); callback.countDown(); } });
        player.play(); await(line.drainEntered); player.stop(); line.releaseDrain.countDown(); await(callback);
        assertEquals(1, stopped.get());
        assertEquals(0, ended.get());
    }

    @Test
    public void writeFailureInvokesStoppedExactlyOnce() throws Exception {
        FakeLine line = new FakeLine(); line.failWrite = true;
        AudioPlayer player = player(createValidWavFile("failure.wav", 44100), line);
        AtomicInteger stopped = new AtomicInteger();
        CountDownLatch callback = new CountDownLatch(1);
        player.setOnStopped(new Runnable() { @Override public void run() { stopped.incrementAndGet(); callback.countDown(); } });
        player.play(); await(callback);
        assertEquals(1, stopped.get());
        assertFalse(player.isPlaying());
        assertEquals(AudioPlayer.State.FAILED, player.getState());
    }

    @Test
    public void cachedVolumeIsAppliedBeforeStart() throws Exception {
        FakeLine line = new FakeLine(); line.blockWrite = true;
        AudioPlayer player = player(createValidWavFile("volume.wav", 44100), line);
        player.setVolume(0.5);
        player.play(); await(line.started); player.stop();
        assertTrue(line.gainSetBeforeStart);
        assertTrue(line.gain.getValue() < 0.0f);
    }

    @Test
    public void unsignedEightBitWavIsNormalizedBeforeOpeningLine() throws Exception {
        AudioFormat unsignedFormat = new AudioFormat(44100.0f, 8, 1, false, false);
        File wavFile = new File(tempFolder.getRoot(), "unsigned.wav");
        AudioInputStream stream = new AudioInputStream(new ByteArrayInputStream(new byte[16]), unsignedFormat, 16);
        AudioSystem.write(stream, AudioFileFormat.Type.WAVE, wavFile);
        FakeLine line = new FakeLine();

        AudioPlayer player = player(wavFile, line);

        assertEquals(AudioFormat.Encoding.PCM_SIGNED, line.format.getEncoding());
        assertEquals(16, line.format.getSampleSizeInBits());
        assertEquals(2, line.format.getFrameSize());
        assertFalse(line.format.isBigEndian());
        player.dispose();
    }

    @Test
    public void newerVolumeWinsWhenPlayTimeGainApplicationRacesSetVolume() throws Exception {
        FakeLine line = new FakeLine(); line.blockWrite = true; line.gain.blockFirstSet = true;
        AudioPlayer player = player(createValidWavFile("volume-race.wav", 44100), line);
        Thread playThread = new Thread(new Runnable() { @Override public void run() { player.play(); } });
        playThread.start();
        await(line.gain.firstSetEntered);
        player.setVolume(0.25);
        line.gain.releaseFirstSet.countDown();
        playThread.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
        assertFalse("play did not finish", playThread.isAlive());
        await(line.started);
        player.stop();
        assertEquals((float) (20.0 * Math.log10(0.25)), line.startGain, 0.1f);
    }

    @Test
    public void stopBeforeWorkerStartsInvokesStoppedExactlyOnce() throws Exception {
        FakeLine line = new FakeLine(); line.gain.blockFirstSet = true;
        AudioPlayer player = player(createValidWavFile("stop-before-start.wav", 44100), line);
        AtomicInteger stopped = new AtomicInteger();
        AtomicInteger ended = new AtomicInteger();
        CountDownLatch callback = new CountDownLatch(1);
        player.setOnStopped(new Runnable() { @Override public void run() { stopped.incrementAndGet(); callback.countDown(); } });
        player.setOnEndOfMedia(new Runnable() { @Override public void run() { ended.incrementAndGet(); } });
        Thread playThread = new Thread(new Runnable() { @Override public void run() { player.play(); } });
        playThread.start();
        await(line.gain.firstSetEntered);
        player.stop();
        line.gain.releaseFirstSet.countDown();
        playThread.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
        assertFalse("play did not finish", playThread.isAlive());
        await(callback);
        assertEquals(1, stopped.get());
        assertEquals(0, ended.get());
    }

    @Test
    public void mp3FileOpensThroughSpiAsSignedPcm() throws Exception {
        File mp3File = new File("jarFiles/audio/notification01.mp3");
        if (!mp3File.exists()) {
            mp3File = new File("Azkar/jarFiles/audio/notification01.mp3");
        }
        assertTrue("Bundled MP3 file should exist", mp3File.exists());

        try (InputStream in = new BufferedInputStream(new FileInputStream(mp3File));
             AudioInputStream rawStream = AudioSystem.getAudioInputStream(in)) {
            AudioFormat baseFormat = rawStream.getFormat();
            AudioFormat pcmFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    baseFormat.getSampleRate(),
                    16,
                    baseFormat.getChannels(),
                    baseFormat.getChannels() * 2,
                    baseFormat.getSampleRate(),
                    false
            );
            assertTrue("Conversion to signed PCM must be supported by SPI", AudioSystem.isConversionSupported(pcmFormat, baseFormat));
            try (AudioInputStream pcmStream = AudioSystem.getAudioInputStream(pcmFormat, rawStream)) {
                assertNotNull("Decoded PCM stream should not be null", pcmStream);
                assertEquals(AudioFormat.Encoding.PCM_SIGNED, pcmStream.getFormat().getEncoding());
                assertEquals(16, pcmStream.getFormat().getSampleSizeInBits());
                byte[] buf = new byte[4096];
                int bytesRead = pcmStream.read(buf);
                assertTrue("Should be able to decode initial MP3 frames to PCM", bytesRead > 0);
            }
        }
    }

    private static final class FakeLine implements SourceDataLine {
        final CountDownLatch started = new CountDownLatch(1);
        final CountDownLatch drainEntered = new CountDownLatch(1);
        final CountDownLatch releaseDrain = new CountDownLatch(1);
        final CountDownLatch releaseWrite = new CountDownLatch(1);
        final Gain gain = new Gain();
        volatile boolean open;
        volatile boolean running;
        volatile boolean failWrite;
        volatile boolean blockDrain;
        volatile boolean blockWrite;
        volatile boolean gainSetBeforeStart;
        AudioFormat format;

        @Override public void open(AudioFormat value) { format = value; open = true; }
        @Override public void open(AudioFormat value, int bufferSize) { open(value); }
        @Override public int write(byte[] data, int offset, int length) {
            if (failWrite) throw new IllegalStateException("write failed");
            if (blockWrite) {
                try {
                    releaseWrite.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return length;
        }
        volatile float startGain;
        @Override public void start() { gainSetBeforeStart = gain.wasSet; startGain = gain.getValue(); running = true; started.countDown(); }
        @Override public void drain() {
            drainEntered.countDown();
            if (blockDrain) {
                boolean interrupted = false;
                while (releaseDrain.getCount() != 0) {
                    try {
                        releaseDrain.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        interrupted = true;
                    }
                }
                if (interrupted) Thread.currentThread().interrupt();
            }
        }
        @Override public void stop() { running = false; }
        @Override public void flush() { releaseWrite.countDown(); }
        @Override public boolean isRunning() { return running; }
        @Override public boolean isActive() { return running; }
        @Override public AudioFormat getFormat() { return format; }
        @Override public int getBufferSize() { return 8192; }
        @Override public int available() { return 8192; }
        @Override public int getFramePosition() { return 0; }
        @Override public long getLongFramePosition() { return 0; }
        @Override public long getMicrosecondPosition() { return 0; }
        @Override public float getLevel() { return 0; }
        @Override public Line.Info getLineInfo() { return new DataLine.Info(SourceDataLine.class, format); }
        @Override public void open() { open = true; }
        @Override public void close() { open = false; running = false; }
        @Override public boolean isOpen() { return open; }
        @Override public Control[] getControls() { return new Control[] { gain }; }
        @Override public boolean isControlSupported(Control.Type type) { return FloatControl.Type.MASTER_GAIN.equals(type); }
        @Override public Control getControl(Control.Type type) { if (isControlSupported(type)) return gain; throw new IllegalArgumentException(); }
        @Override public void addLineListener(LineListener listener) { }
        @Override public void removeLineListener(LineListener listener) { }
    }

    private static final class Gain extends FloatControl {
        final CountDownLatch firstSetEntered = new CountDownLatch(1);
        final CountDownLatch releaseFirstSet = new CountDownLatch(1);
        boolean blockFirstSet;
        boolean firstSetBlocked;
        boolean wasSet;
        Gain() { super(FloatControl.Type.MASTER_GAIN, -80.0f, 6.0f, 0.1f, 1, 0.0f, "dB"); }
        @Override public void setValue(float value) {
            if (blockFirstSet && !firstSetBlocked) {
                firstSetBlocked = true;
                firstSetEntered.countDown();
                try {
                    releaseFirstSet.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            wasSet = true;
            super.setValue(value);
        }
    }
}
