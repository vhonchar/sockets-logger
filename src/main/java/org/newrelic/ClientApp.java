package org.newrelic;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Scanner;

public class ClientApp {

    public static void main(String[] args) throws IOException {
        try (
                var socket = new Socket("localhost", 4000);
                var os = new OutputStreamWriter(socket.getOutputStream())
        ) {
            socket.setSoTimeout(10 + 1000);
            var cmd = new Scanner(System.in);
            System.out.println("Socket is connected on port " + socket.getLocalPort()
                    + ". Start typing then press enter to send to the server...");
            String line;
            do {
                line = cmd.nextLine();
                os.write(line + System.lineSeparator());
                os.flush();
            } while (!"exit".equalsIgnoreCase(line) && !"terminate".equalsIgnoreCase(line) && !socket.isClosed());
        } finally {
            System.out.println("Connection is terminated");
        }
    }
}
