package com.bayoumi.util.gui.notfication;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class NotificationAudioTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testGetAudioListMergeAndDeduplicate() throws IOException {
        File bundledDir = tempFolder.newFolder("bundled_audio");
        File userDir = tempFolder.newFolder("user_audio");

        new File(bundledDir, "sound1.mp3").createNewFile();
        new File(bundledDir, "sound2.mp3").createNewFile();

        new File(userDir, "sound2.mp3").createNewFile();
        new File(userDir, "custom.mp3").createNewFile();

        List<String> list = NotificationAudio.getAudioList(bundledDir.toPath(), userDir.toPath());

        Assert.assertEquals("بدون صوت", list.get(0));
        Assert.assertTrue(list.contains("sound1.mp3"));
        Assert.assertTrue(list.contains("sound2.mp3"));
        Assert.assertTrue(list.contains("custom.mp3"));

        long sound2Count = list.stream().filter(s -> s.equals("sound2.mp3")).count();
        Assert.assertEquals(1, sound2Count);
        Assert.assertEquals(4, list.size());
    }

    @Test
    public void testBundledAudioWinsOnDuplicateFilename() throws IOException {
        File bundledDir = tempFolder.newFolder("bundled_audio");
        File userDir = tempFolder.newFolder("user_audio");

        File bundledSound = new File(bundledDir, "sound.mp3");
        bundledSound.createNewFile();

        File userSound = new File(userDir, "sound.mp3");
        userSound.createNewFile();

        File resolved = NotificationAudio.resolveAudioFile("sound.mp3", bundledDir.toPath(), userDir.toPath());
        Assert.assertNotNull(resolved);
        Assert.assertEquals(bundledSound.getCanonicalPath(), resolved.getCanonicalPath());
    }

    @Test
    public void testUserCustomAudioResolves() throws IOException {
        File bundledDir = tempFolder.newFolder("bundled_audio");
        File userDir = tempFolder.newFolder("user_audio");

        File userSound = new File(userDir, "user_only.mp3");
        userSound.createNewFile();

        File resolved = NotificationAudio.resolveAudioFile("user_only.mp3", bundledDir.toPath(), userDir.toPath());
        Assert.assertNotNull(resolved);
        Assert.assertEquals(userSound.getCanonicalPath(), resolved.getCanonicalPath());
    }

    @Test
    public void testMissingFileReturnsNull() throws IOException {
        File bundledDir = tempFolder.newFolder("bundled_audio");
        File userDir = tempFolder.newFolder("user_audio");

        File resolved = NotificationAudio.resolveAudioFile("nonexistent.mp3", bundledDir.toPath(), userDir.toPath());
        Assert.assertNull(resolved);
    }

    @Test
    public void testPathTraversalAndAbsolutePathRejected() throws IOException {
        File bundledDir = tempFolder.newFolder("bundled_audio");
        File userDir = tempFolder.newFolder("user_audio");

        File outsideFile = tempFolder.newFile("outside.mp3");

        File resolvedTraversal = NotificationAudio.resolveAudioFile("../outside.mp3", bundledDir.toPath(), userDir.toPath());
        Assert.assertNull(resolvedTraversal);

        File resolvedAbsolute = NotificationAudio.resolveAudioFile(outsideFile.getAbsolutePath(), bundledDir.toPath(), userDir.toPath());
        Assert.assertNull(resolvedAbsolute);
    }

    @Test
    public void testSymlinkEscapeRejected() throws IOException {
        File bundledDir = tempFolder.newFolder("bundled_audio");
        File userDir = tempFolder.newFolder("user_audio");
        File outsideFile = tempFolder.newFile("outside_secret.mp3");

        Path symlink = bundledDir.toPath().resolve("symlink_escape.mp3");
        try {
            Files.createSymbolicLink(symlink, outsideFile.toPath());
        } catch (UnsupportedOperationException | IOException | SecurityException ignored) {
            return;
        }

        File resolved = NotificationAudio.resolveAudioFile("symlink_escape.mp3", bundledDir.toPath(), userDir.toPath());
        Assert.assertNull(resolved);
    }

    @Test
    public void testSilentOrEmptyReturnsNull() throws IOException {
        File bundledDir = tempFolder.newFolder("bundled_audio");
        File userDir = tempFolder.newFolder("user_audio");

        Assert.assertNull(NotificationAudio.resolveAudioFile("بدون صوت", bundledDir.toPath(), userDir.toPath()));
        Assert.assertNull(NotificationAudio.resolveAudioFile(null, bundledDir.toPath(), userDir.toPath()));
        Assert.assertNull(NotificationAudio.resolveAudioFile("", bundledDir.toPath(), userDir.toPath()));
    }
}
