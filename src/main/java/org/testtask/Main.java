package org.testtask;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {

    private static final int MAX_CONCURRENT_CLIENTS = 5;
    private static final int PORT = 4000;
    private static final File PERSISTED_STORAGE = new File("numbers.txt");
    private static final int REPORT_PERIOD = 10;
    private static final ScheduledExecutorService reportScheduler = Executors
            .newSingleThreadScheduledExecutor();

    public static void main(String[] args) throws IOException, InterruptedException {
        var server = new Server(MAX_CONCURRENT_CLIENTS);
        var metrics = new MetricsService();
        var reporter = new Reporter(metrics);
        var storage = new FileStorage(PERSISTED_STORAGE, metrics);

        try {
            server.start(PORT, new SocketController(storage));
            reportScheduler.scheduleAtFixedRate(() -> System.out.println(reporter.runReport()), 0, REPORT_PERIOD, TimeUnit.SECONDS);

            while (server.isRunning()) {
                TimeUnit.SECONDS.sleep(1);
            }
        } finally {
            server.close();
            storage.shutdown();
            reportScheduler.shutdown();
        }
    }
}