package org.testtask;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FileStorageTest {
    private FileStorage storage;
    private MetricsService metrics;
    private File tempDir;
    private File persistedStorage;

    @BeforeEach
    public void setup() throws IOException {
        this.metrics = new MetricsService();
        tempDir = Files.createTempDirectory(null).toFile();
        persistedStorage = new File(tempDir, "numbers.txt");
        this.storage = new FileStorage(persistedStorage, this.metrics);
    }

    @Test
    void shouldPersistDataToFile() throws InterruptedException, IOException {
        this.storage.add("1");
        this.storage.add("2");
        this.storage.add("3");

        // to await persisting queue is finished
        this.storage.shutdown();

        try (var is = new FileReader(this.persistedStorage)) {
            var lines = IOUtils.readLines(is);
            assertEquals(Arrays.asList("1", "2", "3"), lines);
        }
    }

    @Test
    void shouldPersistOnlyUniqueValues() throws InterruptedException, IOException {
        this.storage.add("1");
        this.storage.add("2");
        this.storage.add("1");
        this.storage.add("2");

        // to await persisting queue is finished
        this.storage.shutdown();

        try (var is = new FileReader(this.persistedStorage)) {
            var lines = IOUtils.readLines(is);
            assertEquals(Arrays.asList("1", "2"), lines);
        }
    }

    @Test
    void shouldCountUniqueAndDuplicateMetrics() throws InterruptedException, IOException {
        this.storage.add("1");
        this.storage.add("2");
        this.storage.add("1");
        this.storage.add("1");
        this.storage.add("1");
        this.storage.add("2");

        // to await persisting queue is finished
        this.storage.shutdown();

        assertEquals(2, this.metrics.unique().get());
        assertEquals(4, this.metrics.duplicates().get());
    }

    @Test
    void shouldRecreateFileOnInitialization() throws InterruptedException, IOException {
        //populate current file
        this.storage.add("1");
        this.storage.shutdown();

        // re-create and write new data
        this.storage = new FileStorage(persistedStorage, metrics);
        this.storage.add("2");
        this.storage.shutdown();

        try (var is = new FileReader(this.persistedStorage)) {
            var lines = IOUtils.readLines(is);
            assertEquals(List.of("2"), lines);
        }
    }

    @AfterEach
    public void teardown() throws IOException {
        this.storage.shutdown();
        FileUtils.deleteDirectory(tempDir);
    }
}