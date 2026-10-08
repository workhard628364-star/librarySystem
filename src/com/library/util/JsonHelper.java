package com.library.util;

import com.library.model.*;
import java.util.*;

public class JsonHelper {

    public static String escape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 32 || c > 126) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    public static String toJson(Object obj) {
        if (obj == null) return "null";
        if (obj instanceof String) return "\"" + escape((String) obj) + "\"";
        if (obj instanceof Number || obj instanceof Boolean) return obj.toString();
        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) sb.append(",");
                sb.append("\"").append(escape(String.valueOf(entry.getKey()))).append("\":");
                sb.append(toJson(entry.getValue()));
                first = false;
            }
            sb.append("}");
            return sb.toString();
        }
        if (obj instanceof Collection) {
            Collection<?> col = (Collection<?>) obj;
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object item : col) {
                if (!first) sb.append(",");
                sb.append(toJson(item));
                first = false;
            }
            sb.append("]");
            return sb.toString();
        }
        if (obj instanceof Book) {
            Book b = (Book) obj;
            return String.format(
                "{\"id\":\"%s\",\"title\":\"%s\",\"author\":\"%s\",\"isbn\":\"%s\",\"genre\":\"%s\",\"status\":\"%s\",\"totalCopies\":%d,\"availableCopies\":%d,\"publishedYear\":%d}",
                escape(b.getId()), escape(b.getTitle()), escape(b.getAuthor()), escape(b.getIsbn()),
                escape(b.getGenre()), escape(b.getStatus()), b.getTotalCopies(), b.getAvailableCopies(), b.getPublishedYear()
            );
        }
        if (obj instanceof Member) {
            Member m = (Member) obj;
            return String.format(
                "{\"id\":\"%s\",\"name\":\"%s\",\"email\":\"%s\",\"membershipId\":\"%s\",\"phone\":\"%s\",\"role\":\"%s\",\"joinedDate\":\"%s\",\"prefDueDateAlerts\":%b,\"prefNewArrivalAlerts\":%b,\"prefNewsletter\":%b}",
                escape(m.getId()), escape(m.getName()), escape(m.getEmail()), escape(m.getMembershipId()),
                escape(m.getPhone()), escape(m.getRole()), escape(m.getJoinedDate()),
                m.isPrefDueDateAlerts(), m.isPrefNewArrivalAlerts(), m.isPrefNewsletter()
            );
        }
        if (obj instanceof Transaction) {
            Transaction t = (Transaction) obj;
            return String.format(
                "{\"id\":\"%s\",\"bookId\":\"%s\",\"bookTitle\":\"%s\",\"memberId\":\"%s\",\"memberName\":\"%s\",\"borrowDate\":\"%s\",\"dueDate\":\"%s\",\"returnDate\":%s,\"status\":\"%s\",\"fine\":%.2f}",
                escape(t.getId()), escape(t.getBookId()), escape(t.getBookTitle()), escape(t.getMemberId()), escape(t.getMemberName()),
                escape(t.getBorrowDate()), escape(t.getDueDate()),
                t.getReturnDate() == null ? "null" : "\"" + escape(t.getReturnDate()) + "\"",
                escape(t.getStatus()), t.getFine()
            );
        }
        if (obj instanceof Notification) {
            Notification n = (Notification) obj;
            return String.format(
                "{\"id\":\"%s\",\"recipientId\":\"%s\",\"recipientName\":\"%s\",\"title\":\"%s\",\"message\":\"%s\",\"type\":\"%s\",\"timestamp\":\"%s\",\"isRead\":%b}",
                escape(n.getId()), escape(n.getRecipientId()), escape(n.getRecipientName()), escape(n.getTitle()),
                escape(n.getMessage()), escape(n.getType()), escape(n.getTimestamp()), n.isRead()
            );
        }
        if (obj instanceof SearchRecord) {
            SearchRecord s = (SearchRecord) obj;
            return String.format(
                "{\"id\":\"%s\",\"memberId\":\"%s\",\"query\":\"%s\",\"searchCriteria\":\"%s\",\"resultCount\":%d,\"timestamp\":\"%s\"}",
                escape(s.getId()), escape(s.getMemberId()), escape(s.getQuery()), escape(s.getSearchCriteria()),
                s.getResultCount(), escape(s.getTimestamp())
            );
        }
        return "\"" + escape(obj.toString()) + "\"";
    }

    // Lightweight JSON Parser for HTTP Request Bodies
    public static Map<String, Object> parseObject(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new HashMap<>();
        }
        Object parsed = new Parser(json.trim()).parseValue();
        if (parsed instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) parsed;
            return map;
        }
        return new HashMap<>();
    }

    private static class Parser {
        private final String text;
        private int pos = 0;

        public Parser(String text) {
            this.text = text;
        }

        private void skipWhitespace() {
            while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) {
                pos++;
            }
        }

        private char peek() {
            skipWhitespace();
            if (pos >= text.length()) return '\0';
            return text.charAt(pos);
        }

        private char next() {
            skipWhitespace();
            if (pos >= text.length()) return '\0';
            return text.charAt(pos++);
        }

        public Object parseValue() {
            skipWhitespace();
            if (pos >= text.length()) return null;
            char c = text.charAt(pos);
            if (c == '{') return parseObjectInternal();
            if (c == '[') return parseArrayInternal();
            if (c == '"') return parseStringInternal();
            if (c == 't' || c == 'f') return parseBooleanInternal();
            if (c == 'n') return parseNullInternal();
            return parseNumberInternal();
        }

        private Map<String, Object> parseObjectInternal() {
            Map<String, Object> map = new LinkedHashMap<>();
            next(); // consume '{'
            skipWhitespace();
            if (peek() == '}') {
                next(); // consume '}'
                return map;
            }
            while (pos < text.length()) {
                skipWhitespace();
                if (peek() != '"') {
                    // unexpected or error, break
                    break;
                }
                String key = parseStringInternal();
                skipWhitespace();
                if (peek() == ':') {
                    next(); // consume ':'
                }
                Object val = parseValue();
                map.put(key, val);
                skipWhitespace();
                char delim = peek();
                if (delim == ',') {
                    next(); // consume ','
                } else if (delim == '}') {
                    next(); // consume '}'
                    break;
                } else {
                    break;
                }
            }
            return map;
        }

        private List<Object> parseArrayInternal() {
            List<Object> list = new ArrayList<>();
            next(); // consume '['
            skipWhitespace();
            if (peek() == ']') {
                next();
                return list;
            }
            while (pos < text.length()) {
                Object val = parseValue();
                list.add(val);
                skipWhitespace();
                char delim = peek();
                if (delim == ',') {
                    next();
                } else if (delim == ']') {
                    next();
                    break;
                } else {
                    break;
                }
            }
            return list;
        }

        private String parseStringInternal() {
            next(); // consume opening quote
            StringBuilder sb = new StringBuilder();
            while (pos < text.length()) {
                char c = text.charAt(pos++);
                if (c == '"') {
                    return sb.toString();
                } else if (c == '\\') {
                    if (pos >= text.length()) break;
                    char esc = text.charAt(pos++);
                    switch (esc) {
                        case '"': sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        case '/': sb.append('/'); break;
                        case 'b': sb.append('\b'); break;
                        case 'f': sb.append('\f'); break;
                        case 'n': sb.append('\n'); break;
                        case 'r': sb.append('\r'); break;
                        case 't': sb.append('\t'); break;
                        case 'u':
                            if (pos + 4 <= text.length()) {
                                String hex = text.substring(pos, pos + 4);
                                try {
                                    sb.append((char) Integer.parseInt(hex, 16));
                                } catch (NumberFormatException ignored) {}
                                pos += 4;
                            }
                            break;
                        default: sb.append(esc); break;
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        private Boolean parseBooleanInternal() {
            if (text.startsWith("true", pos)) {
                pos += 4;
                return Boolean.TRUE;
            }
            if (text.startsWith("false", pos)) {
                pos += 5;
                return Boolean.FALSE;
            }
            return Boolean.FALSE;
        }

        private Object parseNullInternal() {
            if (text.startsWith("null", pos)) {
                pos += 4;
            }
            return null;
        }

        private Object parseNumberInternal() {
            int start = pos;
            if (pos < text.length() && (text.charAt(pos) == '-' || text.charAt(pos) == '+')) {
                pos++;
            }
            boolean isFloat = false;
            while (pos < text.length()) {
                char c = text.charAt(pos);
                if (Character.isDigit(c)) {
                    pos++;
                } else if (c == '.' || c == 'e' || c == 'E') {
                    isFloat = true;
                    pos++;
                } else {
                    break;
                }
            }
            String numStr = text.substring(start, pos);
            try {
                if (isFloat) {
                    return Double.parseDouble(numStr);
                } else {
                    long l = Long.parseLong(numStr);
                    if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) {
                        return (int) l;
                    }
                    return l;
                }
            } catch (Exception e) {
                return 0;
            }
        }
    }
}
