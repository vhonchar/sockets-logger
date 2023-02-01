package org.newrelic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@Timeout(5)
class SocketControllerTest {

    private SocketController controller;
    private StorageMock storage;

    @BeforeEach
    void setUp() {
        this.storage = new StorageMock();
        this.controller = new SocketController(this.storage);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "not a number",
            "123",
            "1234567890",
            ""
    })
    void shouldNotStore(String data) throws IOException {
        String input = data + System.lineSeparator() + "terminate" + System.lineSeparator();
        this.controller.call(
                new BufferedReader(new CharArrayReader(input.toCharArray())),
                new BufferedWriter(new CharArrayWriter())
        );

        assertEquals(Collections.emptyList(), this.storage.received);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "123456789",
            "000000001",
            "100000000",
    })
    void shouldStore(String data) throws IOException {
        String input = data + System.lineSeparator() + "terminate" + System.lineSeparator();
        this.controller.call(
                new BufferedReader(new CharArrayReader(input.toCharArray())),
                new BufferedWriter(new CharArrayWriter())
        );

        assertEquals(Collections.singletonList(data), this.storage.received);
    }

    @Test
    void shouldNotStopUntilReceiveTerminateCommand() throws IOException, InterruptedException, ExecutionException {
        var is = new PipedReader();
        var os = new PipedWriter(is);

        var controllerFeature = CompletableFuture.runAsync(() -> {
            try {
                this.controller.call(
                        new BufferedReader(is),
                        new BufferedWriter(new CharArrayWriter())
                );
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        assertFalse(controllerFeature.isDone(), "Controller hasn't started");

        os.write("some random data" + System.lineSeparator());
        os.write("123456789" + System.lineSeparator());

        // give controller time to process data
        TimeUnit.MILLISECONDS.sleep(100);

        assertFalse(controllerFeature.isDone(), "Controller should still be running");

        os.write("terminate" + System.lineSeparator());
        controllerFeature.get();
    }


    private static class StorageMock implements Storage {
        List<String> received = new ArrayList<>();

        @Override
        public void add(String number) {
            this.received.add(number);
        }
    }
}