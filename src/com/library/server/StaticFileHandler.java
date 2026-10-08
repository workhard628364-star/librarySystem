package com.library.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

public class StaticFileHandler implements HttpHandler {

    private final Path rootDir;
    private static final Map<String, String> MIME_TYPES = new HashMap<>();

    static {
        MIME_TYPES.put("html", "text/html; charset=UTF-8");
        MIME_TYPES.put("htm", "text/html; charset=UTF-8");
        MIME_TYPES.put("css", "text/css; charset=UTF-8");
        MIME_TYPES.put("js", "application/javascript; charset=UTF-8");
        MIME_TYPES.put("json", "application/json; charset=UTF-8");
        MIME_TYPES.put("png", "image/png");
        MIME_TYPES.put("jpg", "image/jpeg");
        MIME_TYPES.put("jpeg", "image/jpeg");
        MIME_TYPES.put("svg", "image/svg+xml");
        MIME_TYPES.put("ico", "image/x-icon");
        MIME_TYPES.put("woff2", "font/woff2");
        MIME_TYPES.put("txt", "text/plain; charset=UTF-8");
    }

    public StaticFileHandler(Path rootDir) {
        this.rootDir = rootDir.toAbsolutePath().normalize();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String requestPath = exchange.getRequestURI().getPath();
        if (requestPath == null || requestPath.equals("/") || requestPath.isEmpty()) {
            requestPath = "/index.html";
        }

        // Sanitize path against directory traversal
        String cleanPath = requestPath.replace('/', File.separatorChar);
        if (cleanPath.startsWith(File.separator)) {
            cleanPath = cleanPath.substring(1);
        }

        Path target = rootDir.resolve(cleanPath).normalize();
        if (!target.startsWith(rootDir) || !Files.exists(target) || Files.isDirectory(target)) {
            // Check index.html fallback for client routing
            Path fallback = rootDir.resolve("index.html");
            if (Files.exists(fallback)) {
                target = fallback;
            } else {
                byte[] notFound = "<h1>404 Not Found</h1>".getBytes();
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(404, notFound.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(notFound);
                }
                return;
            }
        }

        String ext = getFileExtension(target.getFileName().toString());
        String contentType = MIME_TYPES.getOrDefault(ext.toLowerCase(), "application/octet-stream");

        byte[] content = Files.readAllBytes(target);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");
        exchange.sendResponseHeaders(200, content.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(content);
        }
    }

    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < filename.length() - 1) {
            return filename.substring(dotIndex + 1);
        }
        return "";
    }
}
