package org.newrelic;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Stores all unique data in-memory for quick access.
 * In-memory storage is a set of integers (for little memory optimization),
 * while persisted file stores initial incoming data, to preserve leading 0s
 * <p>
 * Disk IO operations are deferred to another thread, to ensure high throughput of the main operation.
 * Note, this leads to a necessity to wait for the deferred write queue to complete before shutting down the app.
 */
public class FileStorage implements Storage {

    // since we are going to store only numbers with max 9 digits,
    // this will reduce memory consumption a little
    private final Set<Integer> storage = ConcurrentHashMap.newKeySet();
    private final ExecutorService persistQueue = Executors.newSingleThreadExecutor();
    private final MetricsService metrics;
    private final FileWriter os;

    public FileStorage(File file, MetricsService metrics) throws IOException {
        this.metrics = metrics;
        if (file.exists() && !file.delete()) {
            throw new RuntimeException("Can't delete file " + file.getPath());
        }
        file.createNewFile();
        boolean append = true;
        this.os = new FileWriter(file, append);
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
        try {
            this.os.write(number + System.lineSeparator());
        } catch (IOException e) {
            System.out.println("Error while trying to persist number " + number);
            e.printStackTrace();
        }
    }

    public void shutdown() {
        this.persistQueue.shutdown();
        try {
            this.persistQueue.awaitTermination(10, TimeUnit.SECONDS);
            this.os.flush();
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(this.os);
        }
    }
}
