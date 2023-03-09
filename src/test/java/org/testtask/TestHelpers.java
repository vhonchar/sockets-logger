package org.testtask;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;

public class TestHelpers {

    public static CompletableFuture<String> ping(int port) {
        return CompletableFuture.supplyAsync(() -> {
            try (var socket = new Socket("localhost", port);
                 var input = new BufferedReader(new InputStreamReader(socket.getInputStream()))
            ) {
                return input.readLine();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static CompletableFuture<String> sendRequest(int port, String text) {
        return CompletableFuture.supplyAsync(() -> {
            try (var socket = new Socket("localhost", port);
                 var input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 var output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))
            ) {
                output.write(text + System.lineSeparator());
                output.flush();
                return input.readLine();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
