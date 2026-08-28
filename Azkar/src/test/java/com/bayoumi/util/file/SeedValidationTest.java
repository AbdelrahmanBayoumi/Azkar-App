package com.bayoumi.util.file;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SeedValidationTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testSeedValidation_SourceEqualsDestination_FileExists() throws IOException {
        File seedFile = tempFolder.newFile("locations.db");
        try (FileWriter fw = new FileWriter(seedFile)) {
            fw.write("DB DATA");
        }

        Path from = seedFile.toPath();
        Path to = seedFile.toPath();

        boolean result = FileUtils.copySeedIfNotExist(from, to);

        Assert.assertTrue("Target file must exist", result);
        Assert.assertTrue("Target file must exist on disk", Files.exists(to));
    }

    @Test
    public void testSeedValidation_SourceEqualsDestination_FileMissing() throws IOException {
        Path from = tempFolder.getRoot().toPath().resolve("missing.db");
        Path to = tempFolder.getRoot().toPath().resolve("missing.db");

        boolean result = FileUtils.copySeedIfNotExist(from, to);

        Assert.assertFalse("Target file is missing", result);
        Assert.assertFalse("Target file is missing on disk", Files.exists(to));
    }

    @Test
    public void testSeedValidation_SourceMissing_DestinationExists() throws IOException {
        Path from = tempFolder.getRoot().toPath().resolve("missing_source.db");

        File targetFile = tempFolder.newFile("existing_dest.db");
        try (FileWriter fw = new FileWriter(targetFile)) {
            fw.write("EXISTING DEST DATA");
        }
        Path to = targetFile.toPath();

        boolean result = FileUtils.copySeedIfNotExist(from, to);

        Assert.assertTrue("Destination file must remain intact", result);
        Assert.assertTrue("Destination file must exist on disk", Files.exists(to));
        String content = new String(Files.readAllBytes(to));
        Assert.assertEquals("EXISTING DEST DATA", content);
    }

    @Test
    public void testSeedValidation_BothMissing() throws IOException {
        Path from = tempFolder.getRoot().toPath().resolve("missing_from.db");
        Path to = tempFolder.getRoot().toPath().resolve("missing_to.db");

        boolean result = FileUtils.copySeedIfNotExist(from, to);

        Assert.assertFalse("Target file is missing when both missing", result);
        Assert.assertFalse("Target file is missing on disk when both missing", Files.exists(to));
    }

    @Test
    public void testSeedValidation_SourceExists_DestinationExists_NoOverwrite() throws IOException {
        File sourceFile = tempFolder.newFile("source.db");
        try (FileWriter fw = new FileWriter(sourceFile)) {
            fw.write("SOURCE DATA");
        }

        File destFile = tempFolder.newFile("dest.db");
        try (FileWriter fw = new FileWriter(destFile)) {
            fw.write("USER DATA");
        }

        boolean result = FileUtils.copySeedIfNotExist(sourceFile.toPath(), destFile.toPath());

        Assert.assertTrue("Destination file must exist", result);
        String content = new String(Files.readAllBytes(destFile.toPath()));
        Assert.assertEquals("USER DATA", content);
    }
}
