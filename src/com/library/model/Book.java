package com.library.model;

public class Book {
    private String id;
    private String title;
    private String author;
    private String isbn;
    private String genre;
    private String status; // AVAILABLE, BORROWED
    private int totalCopies;
    private int availableCopies;
    private int publishedYear;

    public Book() {}

    public Book(String id, String title, String author, String isbn, String genre, int totalCopies, int availableCopies, int publishedYear) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.genre = genre;
        this.totalCopies = totalCopies;
        this.availableCopies = availableCopies;
        this.status = availableCopies > 0 ? "AVAILABLE" : "BORROWED";
        this.publishedYear = publishedYear;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public String getStatus() { return availableCopies > 0 ? "AVAILABLE" : "BORROWED"; }
    public void setStatus(String status) { this.status = status; }

    public int getTotalCopies() { return totalCopies; }
    public void setTotalCopies(int totalCopies) { this.totalCopies = totalCopies; }

    public int getAvailableCopies() { return availableCopies; }
    public void setAvailableCopies(int availableCopies) {
        this.availableCopies = availableCopies;
        this.status = availableCopies > 0 ? "AVAILABLE" : "BORROWED";
    }

    public int getPublishedYear() { return publishedYear; }
    public void setPublishedYear(int publishedYear) { this.publishedYear = publishedYear; }
}
