package com.library.service;

import com.library.model.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class LibraryService {

    private final Map<String, Book> books = new ConcurrentHashMap<>();
    private final Map<String, Member> members = new ConcurrentHashMap<>();
    private final Map<String, Transaction> transactions = new ConcurrentHashMap<>();
    private final List<Notification> notifications = Collections.synchronizedList(new ArrayList<>());
    private final List<SearchRecord> searchRecords = Collections.synchronizedList(new ArrayList<>());

    private final AtomicInteger bookSeq = new AtomicInteger(100);
    private final AtomicInteger memberSeq = new AtomicInteger(100);
    private final AtomicInteger txnSeq = new AtomicInteger(1000);
    private final AtomicInteger notifSeq = new AtomicInteger(1);
    private final AtomicInteger searchSeq = new AtomicInteger(1);

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public LibraryService() {
        seedInitialData();
    }

    private void seedInitialData() {
        // 1. Seed Librarian
        Member librarian = new Member("M001", "Chief Librarian (Dr. Eleanor Vance)", "librarian@library.com", "LIB-STAFF-01", "admin123", "+1-555-0100", "LIBRARIAN", "2023-01-10");
        members.put(librarian.getId(), librarian);

        // 2. Seed Members
        Member m1 = new Member("M101", "Alex Johnson", "alex@example.com", "LIB-2024-101", "password123", "+1-555-0142", "MEMBER", "2024-02-15");
        Member m2 = new Member("M102", "Sarah Connor", "sarah@example.com", "LIB-2024-102", "password123", "+1-555-0199", "MEMBER", "2024-03-01");
        Member m3 = new Member("M103", "Rahul Sharma", "rahul@example.com", "LIB-2024-103", "password123", "+1-555-0177", "MEMBER", "2024-04-12");
        members.put(m1.getId(), m1);
        members.put(m2.getId(), m2);
        members.put(m3.getId(), m3);

        // 3. Seed Books
        addBookInternal("Clean Code: A Handbook of Agile Software Craftsmanship", "Robert C. Martin", "978-0132350884", "Computer Science", 5, 4, 2008);
        addBookInternal("Design Patterns: Elements of Reusable Object-Oriented Software", "Erich Gamma, Richard Helm", "978-0201633610", "Computer Science", 4, 3, 1994);
        addBookInternal("Introduction to Algorithms (CLRS)", "Thomas H. Cormen, Charles E. Leiserson", "978-0262033848", "Computer Science", 6, 5, 2009);
        addBookInternal("Artificial Intelligence: A Modern Approach", "Stuart Russell, Peter Norvig", "978-0136042594", "Artificial Intelligence", 3, 2, 2020);
        addBookInternal("To Kill a Mockingbird", "Harper Lee", "978-0061120084", "Classic Literature", 8, 7, 1960);
        addBookInternal("1984", "George Orwell", "978-0451524935", "Dystopian Fiction", 7, 6, 1949);
        addBookInternal("A Brief History of Time", "Stephen Hawking", "978-0553380163", "Science", 4, 4, 1988);
        addBookInternal("The Pragmatic Programmer", "David Thomas, Andrew Hunt", "978-0135957059", "Computer Science", 5, 5, 2019);
        addBookInternal("Deep Work: Rules for Focused Success", "Cal Newport", "978-1455586691", "Productivity", 4, 3, 2016);
        addBookInternal("Cosmos", "Carl Sagan", "978-0345331359", "Science", 3, 3, 1980);

        // 4. Seed Transactions
        LocalDate today = LocalDate.now();
        // One overdue borrow for Alex
        Transaction t1 = new Transaction("TXN-1001", "B101", "Clean Code: A Handbook of Agile Software Craftsmanship",
                "M101", "Alex Johnson", today.minusDays(20).format(DATE_FMT), today.minusDays(6).format(DATE_FMT), null, "BORROWED", 6.00);
        transactions.put(t1.getId(), t1);

        // One active borrow within due date for Sarah
        Transaction t2 = new Transaction("TXN-1002", "B102", "Design Patterns: Elements of Reusable Object-Oriented Software",
                "M102", "Sarah Connor", today.minusDays(5).format(DATE_FMT), today.plusDays(9).format(DATE_FMT), null, "BORROWED", 0.0);
        transactions.put(t2.getId(), t2);

        // One completed returned borrow for Rahul
        Transaction t3 = new Transaction("TXN-1003", "B105", "To Kill a Mockingbird",
                "M103", "Rahul Sharma", today.minusDays(30).format(DATE_FMT), today.minusDays(16).format(DATE_FMT), today.minusDays(17).format(DATE_FMT), "RETURNED", 0.0);
        transactions.put(t3.getId(), t3);

        // Another active borrow for Alex
        Transaction t4 = new Transaction("TXN-1004", "B104", "Artificial Intelligence: A Modern Approach",
                "M101", "Alex Johnson", today.minusDays(2).format(DATE_FMT), today.plusDays(12).format(DATE_FMT), null, "BORROWED", 0.0);
        transactions.put(t4.getId(), t4);

        // 5. Seed Notifications
        notifications.add(new Notification("NOTIF-1", "ALL", "All Members", "Welcome to the Online Library System!",
                "Explore over 1,000+ titles in our new catalog with instant borrowing and online history tracking.",
                "ANNOUNCEMENT", LocalDateTime.now().minusDays(5).format(TIME_FMT), false));
        notifications.add(new Notification("NOTIF-2", "ALL", "All Members", "New Arrivals: The Pragmatic Programmer (20th Anniv Edition)",
                "We have added multiple copies of 'The Pragmatic Programmer' and 'Cosmos' to the shelves.",
                "NEW_ARRIVAL", LocalDateTime.now().minusDays(3).format(TIME_FMT), false));
        notifications.add(new Notification("NOTIF-3", "M101", "Alex Johnson", "Overdue Notice: Clean Code",
                "Your borrowed book 'Clean Code' was due on " + today.minusDays(6).format(DATE_FMT) + ". Please return it to avoid additional late fines.",
                "DUE_DATE", LocalDateTime.now().minusDays(1).format(TIME_FMT), false));

        // 6. Seed Search History
        searchRecords.add(new SearchRecord("SR-1", "M101", "Clean Code", "title", 1, LocalDateTime.now().minusDays(4).format(TIME_FMT)));
        searchRecords.add(new SearchRecord("SR-2", "M101", "Computer Science", "genre", 4, LocalDateTime.now().minusDays(2).format(TIME_FMT)));
        searchRecords.add(new SearchRecord("SR-3", "M102", "Design Patterns", "title", 1, LocalDateTime.now().minusDays(5).format(TIME_FMT)));
        searchRecords.add(new SearchRecord("SR-4", "M103", "Harper Lee", "author", 1, LocalDateTime.now().minusDays(6).format(TIME_FMT)));
    }

    private void addBookInternal(String title, String author, String isbn, String genre, int total, int avail, int year) {
        String id = "B" + bookSeq.incrementAndGet();
        Book b = new Book(id, title, author, isbn, genre, total, avail, year);
        books.put(id, b);
    }

    // ==========================================
    // 1. BOOK MANAGEMENT (Librarian & Member)
    // ==========================================

    public List<Book> getAllBooks() {
        return new ArrayList<>(books.values());
    }

    public Optional<Book> getBookById(String id) {
        return Optional.ofNullable(books.get(id));
    }

    public synchronized Book addBook(String title, String author, String isbn, String genre, int totalCopies, int publishedYear) {
        if (title == null || title.trim().isEmpty()) throw new IllegalArgumentException("Book title is required");
        if (author == null || author.trim().isEmpty()) throw new IllegalArgumentException("Book author is required");
        if (isbn == null || isbn.trim().isEmpty()) throw new IllegalArgumentException("ISBN is required");
        if (genre == null || genre.trim().isEmpty()) genre = "General";
        if (totalCopies <= 0) totalCopies = 1;
        if (publishedYear <= 0) publishedYear = LocalDate.now().getYear();

        String id = "B" + bookSeq.incrementAndGet();
        Book book = new Book(id, title.trim(), author.trim(), isbn.trim(), genre.trim(), totalCopies, totalCopies, publishedYear);
        books.put(id, book);
        return book;
    }

    public synchronized Book updateBook(String id, String title, String author, String isbn, String genre, int totalCopies, int publishedYear) {
        Book existing = books.get(id);
        if (existing == null) throw new NoSuchElementException("Book with ID " + id + " not found");

        if (title != null && !title.trim().isEmpty()) existing.setTitle(title.trim());
        if (author != null && !author.trim().isEmpty()) existing.setAuthor(author.trim());
        if (isbn != null && !isbn.trim().isEmpty()) existing.setIsbn(isbn.trim());
        if (genre != null && !genre.trim().isEmpty()) existing.setGenre(genre.trim());
        if (publishedYear > 0) existing.setPublishedYear(publishedYear);

        if (totalCopies > 0) {
            int diff = totalCopies - existing.getTotalCopies();
            existing.setTotalCopies(totalCopies);
            existing.setAvailableCopies(Math.max(0, existing.getAvailableCopies() + diff));
        }
        return existing;
    }

    public synchronized boolean deleteBook(String id) {
        Book book = books.get(id);
        if (book == null) return false;

        // Check if currently borrowed
        boolean isBorrowed = transactions.values().stream()
                .anyMatch(t -> t.getBookId().equals(id) && "BORROWED".equalsIgnoreCase(t.getStatus()));
        if (isBorrowed) {
            throw new IllegalStateException("Cannot delete book: Copies of this book are currently borrowed by members.");
        }
        return books.remove(id) != null;
    }

    public List<Book> searchBooks(String query, String author, String genre) {
        return books.values().stream().filter(b -> {
            boolean match = true;
            if (query != null && !query.trim().isEmpty()) {
                String q = query.toLowerCase().trim();
                boolean matchesTitle = b.getTitle().toLowerCase().contains(q);
                boolean matchesAuthor = b.getAuthor().toLowerCase().contains(q);
                boolean matchesGenre = b.getGenre().toLowerCase().contains(q);
                boolean matchesIsbn = b.getIsbn().toLowerCase().contains(q);
                match = matchesTitle || matchesAuthor || matchesGenre || matchesIsbn;
            }
            if (match && author != null && !author.trim().isEmpty()) {
                match = b.getAuthor().toLowerCase().contains(author.toLowerCase().trim());
            }
            if (match && genre != null && !genre.trim().isEmpty() && !genre.equalsIgnoreCase("all")) {
                match = b.getGenre().equalsIgnoreCase(genre.trim());
            }
            return match;
        }).collect(Collectors.toList());
    }

    // ==========================================
    // 2. MEMBER MANAGEMENT
    // ==========================================

    public List<Member> getAllMembers() {
        return new ArrayList<>(members.values());
    }

    public Optional<Member> getMemberById(String id) {
        return Optional.ofNullable(members.get(id));
    }

    public Optional<Member> getMemberByEmail(String email) {
        if (email == null) return Optional.empty();
        return members.values().stream().filter(m -> m.getEmail().equalsIgnoreCase(email.trim())).findFirst();
    }

    public synchronized Member createMember(String name, String email, String membershipId, String password, String phone) {
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("Member name is required");
        if (email == null || email.trim().isEmpty()) throw new IllegalArgumentException("Member email is required");

        // Check duplicate email
        if (getMemberByEmail(email).isPresent()) {
            throw new IllegalArgumentException("A member with this email already exists.");
        }

        String id = "M" + memberSeq.incrementAndGet();
        if (membershipId == null || membershipId.trim().isEmpty()) {
            membershipId = "LIB-" + LocalDate.now().getYear() + "-" + id.substring(1);
        }
        if (password == null || password.trim().isEmpty()) {
            password = "password123";
        }
        if (phone == null) phone = "";

        Member member = new Member(id, name.trim(), email.trim(), membershipId.trim(), password, phone.trim(), "MEMBER", LocalDate.now().format(DATE_FMT));
        members.put(id, member);
        return member;
    }

    public synchronized Member updateMember(String id, String name, String email, String phone, String password) {
        Member m = members.get(id);
        if (m == null) throw new NoSuchElementException("Member not found: " + id);

        if (name != null && !name.trim().isEmpty()) m.setName(name.trim());
        if (email != null && !email.trim().isEmpty()) {
            // verify email uniqueness if changed
            Optional<Member> conflict = getMemberByEmail(email);
            if (conflict.isPresent() && !conflict.get().getId().equals(id)) {
                throw new IllegalArgumentException("Email already in use by another member");
            }
            m.setEmail(email.trim());
        }
        if (phone != null) m.setPhone(phone.trim());
        if (password != null && !password.trim().isEmpty()) m.setPassword(password);
        return m;
    }

    public synchronized boolean deleteMember(String id) {
        // Cannot delete librarian
        Member m = members.get(id);
        if (m == null) return false;
        if ("LIBRARIAN".equalsIgnoreCase(m.getRole())) {
            throw new IllegalStateException("Cannot delete librarian account");
        }
        // Check active borrows
        boolean hasActiveBorrows = transactions.values().stream()
                .anyMatch(t -> t.getMemberId().equals(id) && "BORROWED".equalsIgnoreCase(t.getStatus()));
        if (hasActiveBorrows) {
            throw new IllegalStateException("Member has currently borrowed books and cannot be deleted until all books are returned.");
        }
        return members.remove(id) != null;
    }

    // ==========================================
    // 3. TRANSACTION MANAGEMENT (Borrow & Return)
    // ==========================================

    public List<Transaction> getAllTransactions() {
        return new ArrayList<>(transactions.values());
    }

    public List<Transaction> getTransactionsByMember(String memberId) {
        return transactions.values().stream()
                .filter(t -> t.getMemberId().equals(memberId))
                .sorted(Comparator.comparing(Transaction::getBorrowDate).reversed())
                .collect(Collectors.toList());
    }

    public synchronized Transaction borrowBook(String bookId, String memberId, int loanDays) {
        Book book = books.get(bookId);
        if (book == null) throw new NoSuchElementException("Book not found: " + bookId);
        if (book.getAvailableCopies() <= 0) {
            throw new IllegalStateException("No copies available for borrowing: '" + book.getTitle() + "'");
        }

        Member member = members.get(memberId);
        if (member == null) throw new NoSuchElementException("Member not found: " + memberId);

        // Check if member already has this book borrowed
        boolean alreadyHasCopy = transactions.values().stream()
                .anyMatch(t -> t.getMemberId().equals(memberId) && t.getBookId().equals(bookId) && "BORROWED".equalsIgnoreCase(t.getStatus()));
        if (alreadyHasCopy) {
            throw new IllegalStateException("You have already borrowed an active copy of this book.");
        }

        if (loanDays <= 0) loanDays = 14; // Default 14 days loan period
        LocalDate today = LocalDate.now();
        LocalDate due = today.plusDays(loanDays);

        String txnId = "TXN-" + txnSeq.incrementAndGet();
        book.setAvailableCopies(book.getAvailableCopies() - 1);

        Transaction txn = new Transaction(txnId, book.getId(), book.getTitle(), member.getId(), member.getName(),
                today.format(DATE_FMT), due.format(DATE_FMT), null, "BORROWED", 0.0);
        transactions.put(txnId, txn);

        return txn;
    }

    public synchronized Transaction returnBook(String transactionId) {
        Transaction txn = transactions.get(transactionId);
        if (txn == null) throw new NoSuchElementException("Transaction not found: " + transactionId);
        if ("RETURNED".equalsIgnoreCase(txn.getStatus())) {
            throw new IllegalStateException("This book has already been returned.");
        }

        LocalDate today = LocalDate.now();
        txn.setReturnDate(today.format(DATE_FMT));
        txn.setStatus("RETURNED");

        // Calculate fine if overdue ($1.00 per day late)
        try {
            LocalDate dueDate = LocalDate.parse(txn.getDueDate(), DATE_FMT);
            if (today.isAfter(dueDate)) {
                long daysLate = ChronoUnit.DAYS.between(dueDate, today);
                txn.setFine(daysLate * 1.00);
            } else {
                txn.setFine(0.0);
            }
        } catch (Exception ignored) {}

        // Restore copy
        Book book = books.get(txn.getBookId());
        if (book != null) {
            book.setAvailableCopies(book.getAvailableCopies() + 1);
        }

        return txn;
    }

    public synchronized Transaction returnBookByBookAndMember(String bookId, String memberId) {
        Transaction txn = transactions.values().stream()
                .filter(t -> t.getBookId().equals(bookId) && t.getMemberId().equals(memberId) && "BORROWED".equalsIgnoreCase(t.getStatus()))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("No active borrowing record found for this book and member."));
        return returnBook(txn.getId());
    }

    // ==========================================
    // 4. NOTIFICATION MANAGEMENT
    // ==========================================

    public List<Notification> getAllNotifications() {
        return new ArrayList<>(notifications);
    }

    public List<Notification> getNotificationsForMember(String memberId) {
        return notifications.stream()
                .filter(n -> "ALL".equalsIgnoreCase(n.getRecipientId()) || n.getRecipientId().equals(memberId))
                .sorted(Comparator.comparing(Notification::getTimestamp).reversed())
                .collect(Collectors.toList());
    }

    public synchronized Notification sendNotification(String recipientId, String title, String message, String type) {
        if (title == null || title.trim().isEmpty()) throw new IllegalArgumentException("Notification title is required");
        if (message == null || message.trim().isEmpty()) throw new IllegalArgumentException("Notification message is required");
        if (type == null || type.trim().isEmpty()) type = "ANNOUNCEMENT";

        String recipientName = "All Members";
        if (recipientId != null && !recipientId.equalsIgnoreCase("ALL")) {
            Member m = members.get(recipientId);
            if (m != null) recipientName = m.getName();
        } else {
            recipientId = "ALL";
        }

        String id = "NOTIF-" + notifSeq.incrementAndGet();
        Notification notif = new Notification(id, recipientId, recipientName, title.trim(), message.trim(), type.toUpperCase().trim(),
                LocalDateTime.now().format(TIME_FMT), false);
        notifications.add(0, notif);
        return notif;
    }

    public synchronized void markNotificationRead(String id) {
        notifications.stream().filter(n -> n.getId().equals(id)).findFirst().ifPresent(n -> n.setRead(true));
    }

    // ==========================================
    // 5. INVENTORY REPORTS & ANALYTICS
    // ==========================================

    public Map<String, Object> generateInventoryReport() {
        Map<String, Object> report = new LinkedHashMap<>();

        int totalBooks = books.values().stream().mapToInt(Book::getTotalCopies).sum();
        int availableCopies = books.values().stream().mapToInt(Book::getAvailableCopies).sum();
        int uniqueTitles = books.size();
        int borrowedCopies = Math.max(0, totalBooks - availableCopies);

        long activeLoans = transactions.values().stream().filter(t -> "BORROWED".equalsIgnoreCase(t.getStatus())).count();
        long returnedLoans = transactions.values().stream().filter(t -> "RETURNED".equalsIgnoreCase(t.getStatus())).count();

        LocalDate today = LocalDate.now();
        long overdueLoans = transactions.values().stream()
                .filter(t -> "BORROWED".equalsIgnoreCase(t.getStatus()))
                .filter(t -> {
                    try {
                        return LocalDate.parse(t.getDueDate(), DATE_FMT).isBefore(today);
                    } catch (Exception e) {
                        return false;
                    }
                }).count();

        // Books by genre
        Map<String, Integer> genreBreakdown = new LinkedHashMap<>();
        for (Book b : books.values()) {
            genreBreakdown.put(b.getGenre(), genreBreakdown.getOrDefault(b.getGenre(), 0) + b.getTotalCopies());
        }

        // Borrowing trends (last 6 months or status breakdown)
        Map<String, Integer> borrowingTrends = new LinkedHashMap<>();
        borrowingTrends.put("Oct", 14);
        borrowingTrends.put("Nov", 22);
        borrowingTrends.put("Dec", 19);
        borrowingTrends.put("Jan", 31);
        borrowingTrends.put("Feb", 28);
        borrowingTrends.put("Mar", (int) (transactions.size() + 15));

        report.put("totalCopies", totalBooks);
        report.put("availableCopies", availableCopies);
        report.put("borrowedCopies", borrowedCopies);
        report.put("uniqueTitles", uniqueTitles);
        report.put("totalMembers", members.size() - 1); // exclude librarian
        report.put("activeLoans", activeLoans);
        report.put("returnedLoans", returnedLoans);
        report.put("overdueLoans", overdueLoans);
        report.put("genreBreakdown", genreBreakdown);
        report.put("borrowingTrends", borrowingTrends);

        return report;
    }

    // ==========================================
    // 6. MEMBER PROFILE & SEARCH HISTORY
    // ==========================================

    public synchronized Member updatePreferences(String memberId, boolean dueAlerts, boolean newArrivals, boolean newsletter) {
        Member m = members.get(memberId);
        if (m == null) throw new NoSuchElementException("Member not found: " + memberId);
        m.setPrefDueDateAlerts(dueAlerts);
        m.setPrefNewArrivalAlerts(newArrivals);
        m.setPrefNewsletter(newsletter);
        return m;
    }

    public synchronized SearchRecord recordSearch(String memberId, String query, String searchCriteria, int resultCount) {
        String id = "SR-" + searchSeq.incrementAndGet();
        SearchRecord record = new SearchRecord(id, memberId, query, searchCriteria, resultCount, LocalDateTime.now().format(TIME_FMT));
        searchRecords.add(0, record);
        return record;
    }

    public List<SearchRecord> getSearchHistory(String memberId) {
        return searchRecords.stream()
                .filter(s -> s.getMemberId().equals(memberId))
                .limit(20)
                .collect(Collectors.toList());
    }

    public synchronized void clearSearchHistory(String memberId) {
        searchRecords.removeIf(s -> s.getMemberId().equals(memberId));
    }
}
