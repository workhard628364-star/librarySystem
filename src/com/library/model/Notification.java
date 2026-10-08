package com.library.model;

public class Notification {
    private String id;
    private String recipientId;   // "ALL" or memberId
    private String recipientName; // "All Members" or specific member name
    private String title;
    private String message;
    private String type;          // "DUE_DATE", "NEW_ARRIVAL", "ANNOUNCEMENT"
    private String timestamp;
    private boolean isRead;

    public Notification() {}

    public Notification(String id, String recipientId, String recipientName, String title, String message, String type, String timestamp, boolean isRead) {
        this.id = id;
        this.recipientId = recipientId;
        this.recipientName = recipientName;
        this.title = title;
        this.message = message;
        this.type = type;
        this.timestamp = timestamp;
        this.isRead = isRead;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRecipientId() { return recipientId; }
    public void setRecipientId(String recipientId) { this.recipientId = recipientId; }

    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
}
