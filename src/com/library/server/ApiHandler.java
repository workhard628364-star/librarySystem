package com.library.server;

import com.library.model.*;
import com.library.service.LibraryService;
import com.library.util.JsonHelper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ApiHandler implements HttpHandler {

    private final LibraryService libraryService;

    public ApiHandler(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Enable CORS
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        String query = exchange.getRequestURI().getQuery();
        Map<String, String> queryParams = parseQueryParams(query);

        try {
            // ROUTE 1: Auth / Login
            if (path.equals("/api/auth/login") && "POST".equalsIgnoreCase(method)) {
                handleLogin(exchange);
                return;
            }

            // ROUTE 2: Books
            if (path.equals("/api/books")) {
                if ("GET".equalsIgnoreCase(method)) {
                    handleGetBooks(exchange, queryParams);
                } else if ("POST".equalsIgnoreCase(method)) {
                    handleAddBook(exchange);
                } else {
                    sendError(exchange, 405, "Method not allowed");
                }
                return;
            }
            if (path.startsWith("/api/books/")) {
                String bookId = path.substring("/api/books/".length());
                if ("PUT".equalsIgnoreCase(method)) {
                    handleUpdateBook(exchange, bookId);
                } else if ("DELETE".equalsIgnoreCase(method)) {
                    handleDeleteBook(exchange, bookId);
                } else if ("GET".equalsIgnoreCase(method)) {
                    handleGetBookById(exchange, bookId);
                } else {
                    sendError(exchange, 405, "Method not allowed");
                }
                return;
            }

            // ROUTE 3: Members
            if (path.equals("/api/members")) {
                if ("GET".equalsIgnoreCase(method)) {
                    handleGetMembers(exchange);
                } else if ("POST".equalsIgnoreCase(method)) {
                    handleCreateMember(exchange);
                } else {
                    sendError(exchange, 405, "Method not allowed");
                }
                return;
            }
            if (path.startsWith("/api/members/")) {
                String memberId = path.substring("/api/members/".length());
                if ("PUT".equalsIgnoreCase(method)) {
                    handleUpdateMember(exchange, memberId);
                } else if ("DELETE".equalsIgnoreCase(method)) {
                    handleDeleteMember(exchange, memberId);
                } else {
                    sendError(exchange, 405, "Method not allowed");
                }
                return;
            }

            // ROUTE 4: Transactions
            if (path.equals("/api/transactions") && "GET".equalsIgnoreCase(method)) {
                handleGetTransactions(exchange, queryParams);
                return;
            }
            if (path.equals("/api/transactions/borrow") && "POST".equalsIgnoreCase(method)) {
                handleBorrowBook(exchange);
                return;
            }
            if (path.equals("/api/transactions/return") && "POST".equalsIgnoreCase(method)) {
                handleReturnBook(exchange);
                return;
            }

            // ROUTE 5: Notifications
            if (path.equals("/api/notifications")) {
                if ("GET".equalsIgnoreCase(method)) {
                    handleGetNotifications(exchange, queryParams);
                } else if ("POST".equalsIgnoreCase(method)) {
                    handleSendNotification(exchange);
                } else {
                    sendError(exchange, 405, "Method not allowed");
                }
                return;
            }
            if (path.matches("/api/notifications/[^/]+/read") && "POST".equalsIgnoreCase(method)) {
                String notifId = path.split("/")[3];
                libraryService.markNotificationRead(notifId);
                sendSuccess(exchange, "Notification marked as read");
                return;
            }

            // ROUTE 6: Inventory Reports
            if (path.equals("/api/reports/inventory") && "GET".equalsIgnoreCase(method)) {
                handleGetInventoryReport(exchange);
                return;
            }

            // ROUTE 7: Member Preferences & Profile
            if (path.equals("/api/member/preferences") && "POST".equalsIgnoreCase(method)) {
                handleUpdatePreferences(exchange);
                return;
            }
            if (path.equals("/api/member/search-history")) {
                if ("GET".equalsIgnoreCase(method)) {
                    handleGetSearchHistory(exchange, queryParams);
                } else if ("POST".equalsIgnoreCase(method)) {
                    handleRecordSearch(exchange);
                } else if ("DELETE".equalsIgnoreCase(method)) {
                    handleClearSearchHistory(exchange, queryParams);
                } else {
                    sendError(exchange, 405, "Method not allowed");
                }
                return;
            }

            sendError(exchange, 404, "Endpoint not found: " + path);

        } catch (IllegalArgumentException | NoSuchElementException e) {
            sendError(exchange, 400, e.getMessage());
        } catch (IllegalStateException e) {
            sendError(exchange, 409, e.getMessage());
        } catch (Exception e) {
            sendError(exchange, 500, "Internal Server Error: " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------
    // HANDLER IMPLEMENTATIONS
    // -----------------------------------------------------------------

    private void handleLogin(HttpExchange exchange) throws IOException {
        Map<String, Object> body = readJsonBody(exchange);
        String email = String.valueOf(body.getOrDefault("email", "")).trim();
        String password = String.valueOf(body.getOrDefault("password", "")).trim();
        String role = String.valueOf(body.getOrDefault("role", "MEMBER")).trim();

        if (email.isEmpty()) {
            sendError(exchange, 400, "Email is required");
            return;
        }

        Optional<Member> memberOpt = libraryService.getMemberByEmail(email);
        if (memberOpt.isPresent()) {
            Member m = memberOpt.get();
            if (m.getPassword().equals(password)) {
                Map<String, Object> res = new HashMap<>();
                res.put("success", true);
                res.put("user", m);
                res.put("message", "Welcome back, " + m.getName() + "!");
                sendJsonResponse(exchange, 200, JsonHelper.toJson(res));
                return;
            }
        }

        // Demo fallback for quick testing: if email doesn't match, give helpful feedback
        sendError(exchange, 401, "Invalid email or password. Use demo accounts or register a new account.");
    }

    private void handleGetBooks(HttpExchange exchange, Map<String, String> queryParams) throws IOException {
        String q = queryParams.get("q");
        String author = queryParams.get("author");
        String genre = queryParams.get("genre");

        List<Book> list;
        if (q != null || author != null || (genre != null && !genre.equalsIgnoreCase("all"))) {
            list = libraryService.searchBooks(q, author, genre);
        } else {
            list = libraryService.getAllBooks();
        }
        sendJsonResponse(exchange, 200, JsonHelper.toJson(list));
    }

    private void handleGetBookById(HttpExchange exchange, String id) throws IOException {
        Optional<Book> b = libraryService.getBookById(id);
        if (b.isPresent()) {
            sendJsonResponse(exchange, 200, JsonHelper.toJson(b.get()));
        } else {
            sendError(exchange, 404, "Book not found");
        }
    }

    private void handleAddBook(HttpExchange exchange) throws IOException {
        Map<String, Object> body = readJsonBody(exchange);
        String title = (String) body.get("title");
        String author = (String) body.get("author");
        String isbn = (String) body.get("isbn");
        String genre = (String) body.get("genre");
        int copies = parseInt(body.get("totalCopies"), 1);
        int year = parseInt(body.get("publishedYear"), 2024);

        Book created = libraryService.addBook(title, author, isbn, genre, copies, year);
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "Book '" + created.getTitle() + "' successfully added to catalog!");
        res.put("book", created);
        sendJsonResponse(exchange, 201, JsonHelper.toJson(res));
    }

    private void handleUpdateBook(HttpExchange exchange, String bookId) throws IOException {
        Map<String, Object> body = readJsonBody(exchange);
        String title = (String) body.get("title");
        String author = (String) body.get("author");
        String isbn = (String) body.get("isbn");
        String genre = (String) body.get("genre");
        int copies = parseInt(body.get("totalCopies"), -1);
        int year = parseInt(body.get("publishedYear"), -1);

        Book updated = libraryService.updateBook(bookId, title, author, isbn, genre, copies, year);
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "Book '" + updated.getTitle() + "' updated successfully!");
        res.put("book", updated);
        sendJsonResponse(exchange, 200, JsonHelper.toJson(res));
    }

    private void handleDeleteBook(HttpExchange exchange, String bookId) throws IOException {
        boolean deleted = libraryService.deleteBook(bookId);
        if (deleted) {
            sendSuccess(exchange, "Book ID " + bookId + " deleted successfully from catalog.");
        } else {
            sendError(exchange, 404, "Book not found: " + bookId);
        }
    }

    private void handleGetMembers(HttpExchange exchange) throws IOException {
        List<Member> list = libraryService.getAllMembers();
        sendJsonResponse(exchange, 200, JsonHelper.toJson(list));
    }

    private void handleCreateMember(HttpExchange exchange) throws IOException {
        Map<String, Object> body = readJsonBody(exchange);
        String name = (String) body.get("name");
        String email = (String) body.get("email");
        String membershipId = (String) body.get("membershipId");
        String password = (String) body.get("password");
        String phone = (String) body.get("phone");

        Member created = libraryService.createMember(name, email, membershipId, password, phone);
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "Member account for '" + created.getName() + "' created successfully!");
        res.put("member", created);
        sendJsonResponse(exchange, 201, JsonHelper.toJson(res));
    }

    private void handleUpdateMember(HttpExchange exchange, String memberId) throws IOException {
        Map<String, Object> body = readJsonBody(exchange);
        String name = (String) body.get("name");
        String email = (String) body.get("email");
        String phone = (String) body.get("phone");
        String password = (String) body.get("password");

        Member updated = libraryService.updateMember(memberId, name, email, phone, password);
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "Member profile updated successfully!");
        res.put("member", updated);
        sendJsonResponse(exchange, 200, JsonHelper.toJson(res));
    }

    private void handleDeleteMember(HttpExchange exchange, String memberId) throws IOException {
        boolean deleted = libraryService.deleteMember(memberId);
        if (deleted) {
            sendSuccess(exchange, "Member ID " + memberId + " deleted successfully.");
        } else {
            sendError(exchange, 404, "Member not found: " + memberId);
        }
    }

    private void handleGetTransactions(HttpExchange exchange, Map<String, String> queryParams) throws IOException {
        String memberId = queryParams.get("memberId");
        List<Transaction> list;
        if (memberId != null && !memberId.trim().isEmpty()) {
            list = libraryService.getTransactionsByMember(memberId);
        } else {
            list = libraryService.getAllTransactions();
        }
        sendJsonResponse(exchange, 200, JsonHelper.toJson(list));
    }

    private void handleBorrowBook(HttpExchange exchange) throws IOException {
        Map<String, Object> body = readJsonBody(exchange);
        String bookId = (String) body.get("bookId");
        String memberId = (String) body.get("memberId");
        int loanDays = parseInt(body.get("loanDays"), 14);

        if (bookId == null || memberId == null) {
            sendError(exchange, 400, "Both bookId and memberId are required");
            return;
        }

        Transaction txn = libraryService.borrowBook(bookId, memberId, loanDays);
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "Book successfully borrowed! Due date: " + txn.getDueDate());
        res.put("transaction", txn);
        sendJsonResponse(exchange, 200, JsonHelper.toJson(res));
    }

    private void handleReturnBook(HttpExchange exchange) throws IOException {
        Map<String, Object> body = readJsonBody(exchange);
        String txnId = (String) body.get("transactionId");
        String bookId = (String) body.get("bookId");
        String memberId = (String) body.get("memberId");

        Transaction txn;
        if (txnId != null && !txnId.trim().isEmpty()) {
            txn = libraryService.returnBook(txnId);
        } else if (bookId != null && memberId != null) {
            txn = libraryService.returnBookByBookAndMember(bookId, memberId);
        } else {
            sendError(exchange, 400, "Please provide transactionId or both bookId and memberId");
            return;
        }

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        String fineMsg = txn.getFine() > 0 ? String.format(" (Late fine applied: $%.2f)", txn.getFine()) : "";
        res.put("message", "Book '" + txn.getBookTitle() + "' successfully returned!" + fineMsg);
        res.put("transaction", txn);
        sendJsonResponse(exchange, 200, JsonHelper.toJson(res));
    }

    private void handleGetNotifications(HttpExchange exchange, Map<String, String> queryParams) throws IOException {
        String memberId = queryParams.get("memberId");
        List<Notification> list;
        if (memberId != null && !memberId.trim().isEmpty()) {
            list = libraryService.getNotificationsForMember(memberId);
        } else {
            list = libraryService.getAllNotifications();
        }
        sendJsonResponse(exchange, 200, JsonHelper.toJson(list));
    }

    private void handleSendNotification(HttpExchange exchange) throws IOException {
        Map<String, Object> body = readJsonBody(exchange);
        String recipientId = (String) body.get("recipientId");
        String title = (String) body.get("title");
        String message = (String) body.get("message");
        String type = (String) body.get("type");

        Notification notif = libraryService.sendNotification(recipientId, title, message, type);
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "Notification broadcasted successfully to " + notif.getRecipientName() + "!");
        res.put("notification", notif);
        sendJsonResponse(exchange, 201, JsonHelper.toJson(res));
    }

    private void handleGetInventoryReport(HttpExchange exchange) throws IOException {
        Map<String, Object> report = libraryService.generateInventoryReport();
        sendJsonResponse(exchange, 200, JsonHelper.toJson(report));
    }

    private void handleUpdatePreferences(HttpExchange exchange) throws IOException {
        Map<String, Object> body = readJsonBody(exchange);
        String memberId = (String) body.get("memberId");
        boolean due = parseBool(body.get("prefDueDateAlerts"), true);
        boolean arrivals = parseBool(body.get("prefNewArrivalAlerts"), true);
        boolean news = parseBool(body.get("prefNewsletter"), false);

        Member m = libraryService.updatePreferences(memberId, due, arrivals, news);
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "Notification preferences saved successfully!");
        res.put("member", m);
        sendJsonResponse(exchange, 200, JsonHelper.toJson(res));
    }

    private void handleGetSearchHistory(HttpExchange exchange, Map<String, String> queryParams) throws IOException {
        String memberId = queryParams.get("memberId");
        if (memberId == null || memberId.trim().isEmpty()) {
            sendError(exchange, 400, "memberId query parameter required");
            return;
        }
        List<SearchRecord> records = libraryService.getSearchHistory(memberId);
        sendJsonResponse(exchange, 200, JsonHelper.toJson(records));
    }

    private void handleRecordSearch(HttpExchange exchange) throws IOException {
        Map<String, Object> body = readJsonBody(exchange);
        String memberId = (String) body.get("memberId");
        String query = (String) body.get("query");
        String criteria = (String) body.get("searchCriteria");
        int count = parseInt(body.get("resultCount"), 0);

        if (memberId != null && query != null && !query.trim().isEmpty()) {
            libraryService.recordSearch(memberId, query, criteria, count);
        }
        sendSuccess(exchange, "Search history recorded");
    }

    private void handleClearSearchHistory(HttpExchange exchange, Map<String, String> queryParams) throws IOException {
        String memberId = queryParams.get("memberId");
        if (memberId != null) {
            libraryService.clearSearchHistory(memberId);
        }
        sendSuccess(exchange, "Search history cleared");
    }

    // -----------------------------------------------------------------
    // HELPER METHODS
    // -----------------------------------------------------------------

    private Map<String, Object> readJsonBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        int nRead;
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        String body = new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        return JsonHelper.parseObject(body);
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendSuccess(HttpExchange exchange, String message) throws IOException {
        Map<String, Object> map = new HashMap<>();
        map.put("success", true);
        map.put("message", message);
        sendJsonResponse(exchange, 200, JsonHelper.toJson(map));
    }

    private void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        Map<String, Object> map = new HashMap<>();
        map.put("success", false);
        map.put("error", message);
        sendJsonResponse(exchange, statusCode, JsonHelper.toJson(map));
    }

    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.trim().isEmpty()) return map;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            try {
                if (idx > 0) {
                    map.put(URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8),
                            URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8));
                } else if (!pair.isEmpty()) {
                    map.put(URLDecoder.decode(pair, StandardCharsets.UTF_8), "");
                }
            } catch (Exception ignored) {}
        }
        return map;
    }

    private int parseInt(Object val, int def) {
        if (val == null) return def;
        if (val instanceof Number) return ((Number) val).intValue();
        try {
            return Integer.parseInt(val.toString().trim());
        } catch (Exception e) {
            return def;
        }
    }

    private boolean parseBool(Object val, boolean def) {
        if (val == null) return def;
        if (val instanceof Boolean) return (Boolean) val;
        return Boolean.parseBoolean(val.toString());
    }
}
