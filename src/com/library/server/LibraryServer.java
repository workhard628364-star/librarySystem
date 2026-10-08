package com.library.server;

import com.library.service.LibraryService;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;

public class LibraryServer {

    private final int port;
    private final LibraryService libraryService;
    private HttpServer server;

    public LibraryServer(int port) {
        this.port = port;
        this.libraryService = new LibraryService();
    }

    public void start() throws IOException {
        Path webDir = Paths.get("web");
        if (!webDir.toFile().exists()) {
            // Check if run from parent directory
            Path alt = Paths.get("library-management-system", "web");
            if (alt.toFile().exists()) {
                webDir = alt;
            }
        }

        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api", new ApiHandler(libraryService));
        server.createContext("/", new StaticFileHandler(webDir));
        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();

        System.out.println("==================================================================");
        System.out.println("  ONLINE LIBRARY MANAGEMENT SYSTEM - JAVA HTTP SERVER");
        System.out.println("==================================================================");
        System.out.println("  -> Web Application URL : http://localhost:" + port);
        System.out.println("  -> API Base Endpoint   : http://localhost:" + port + "/api");
        System.out.println("  -> Serving Web Dir     : " + webDir.toAbsolutePath());
        System.out.println("  -> Default Librarian   : librarian@library.com / admin123");
        System.out.println("  -> Default Member      : alex@example.com / password123");
        System.out.println("==================================================================");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }
}
