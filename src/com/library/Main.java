package com.library;

import com.library.server.LibraryServer;

public class Main {
    public static void main(String[] args) {
        int port = 8080;

        // Check environment variable PORT (standard on Cloud hosts like Render, Railway, Heroku)
        String envPort = System.getenv("PORT");
        if (envPort != null && !envPort.trim().isEmpty()) {
            try {
                port = Integer.parseInt(envPort.trim());
            } catch (NumberFormatException ignored) {}
        } else if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {}
        }

        try {
            LibraryServer server = new LibraryServer(port);
            server.start();

            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down Library Server...");
                server.stop();
            }));

        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
