package org.testtask;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

public class SocketController implements Server.SocketCallback {

    private final Storage storage;

    public SocketController(Storage storage) {
        this.storage = storage;
    }

    @Override
    public boolean call(BufferedReader input, BufferedWriter output) throws IOException {
        while (true) {
            var data = input.readLine();
            // null is returned when input stream or socket is closed
            if (data == null) {
                return false;
            }
            if ("terminate".equalsIgnoreCase(data)) {
                System.out.println("Received command " + data);
                return true;
            }
            if (data.matches("[0-9]{9}")) {
                storage.add(data);
            }
        }
    }
}
