package org.newrelic;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

// basically, integration test
@Timeout(60)
class MainTest {

    private static final int AMOUNT_OF_DATA = 3_000_000;

    @Test
    void shouldSupportHighThroughput() throws InterruptedException, ExecutionException, IOException {
        var serverFeature = CompletableFuture.runAsync(() -> {
            try {
                Main.main(new String[0]);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        var loadGenerator = new LoadGenerator();
        var data = generateData(AMOUNT_OF_DATA);
        loadGenerator.generateLoad(data.stream(), 5, 4000);

        // Need to give time for server to remove completed handlers from the thread pool
        TimeUnit.SECONDS.sleep(5);
        Client.sendRequest(4000, "terminate");
        serverFeature.get();

        assertEquals(
                data.stream().distinct().count(),
                Files.lines(Path.of("numbers.txt")).count()
        );
    }

    private Collection<String> generateData(int amountOfData) {
        int stringLength = 9;
        boolean useLetters = false;
        boolean useNumbers = true;
        return IntStream
                .range(0, amountOfData)
                .parallel()
                .mapToObj((i) -> RandomStringUtils.random(stringLength, useLetters, useNumbers))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @AfterEach
    public void tearDown() {
//        FileUtils.deleteQuietly(new File("numbers.txt"));
    }
}