package org.newrelic;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FileStorage implements Storage {

    // since we are going to store only numbers with max 9 digits,
    // this will reduce memory consumption a little
    private final Set<Integer> storage = new HashSet<>();
    private final ExecutorService persistQueue = Executors.newSingleThreadExecutor();
    private final File file;
    private final MetricsService metrics;

    public FileStorage(File file, MetricsService metrics) throws IOException {
        this.metrics = metrics;
        this.file = file;
        if (file.exists()) {
            file.delete();
        }
        file.createNewFile();
    }

    public void add(String number) {
        var asInt = Integer.valueOf(number);
        if (storage.add(asInt)) {
            this.metrics.unique().incrementAndGet();
            // deferring IO operation to another thread,
            // since writing to a disk is the longest operation
            // and is not required to be completed right now
            this.persistQueue.submit(() -> this.persist(number));
        } else {
            this.metrics.duplicates().incrementAndGet();
        }
    }

    private void persist(String number) {
        boolean append = true;
        try (var os = new FileWriter(this.file, append)) {
            os.write(number + System.lineSeparator());
        } catch (IOException e) {
            System.out.println("Error while trying to persist number " + number);
            e.printStackTrace();
        }
    }

    public void shutdown() throws InterruptedException {
        this.persistQueue.shutdown();
        this.persistQueue.awaitTermination(10, TimeUnit.SECONDS);
    }
}
