# 📚 Online Library Management System (Java Backend + Web Application)

A full-stack **Online Library Management System** designed and built with **Pure Java 21** (`com.sun.net.httpserver`) and a modern, responsive web dashboard frontend.

---

## 🌟 Key Features Overview

### 1. 👨‍💼 Librarian Portal
* **Book Management**:
  * Add new titles to the catalog (Title, Author, ISBN, Genre, Total Copies, Publication Year).
  * Edit book details and copy counts.
  * Delete books with safety checks (prevents deleting books with active loans).
  * Instant search and genre filters.
* **Member Management**:
  * Register new patron member accounts (Name, Email, Membership ID, Password, Phone).
  * Update member profile and contact details.
  * Remove member accounts (protected against removing patrons with active borrows).
* **Transaction Management**:
  * Issue / loan books to members with configurable due dates.
  * Track borrowed vs returned status.
  * Instant 1-click **Process Return** action.
  * Automatic late fine calculation for overdue loans.
* **Notification Management**:
  * Broadcast notifications to all members or direct alerts to individual members.
  * Select notification types: **Due Date Reminder**, **New Book Arrival**, or **Library Announcement**.
  * History log of sent notifications.
* **Inventory Reports & Analytics**:
  * Real-time metrics: Total Catalog Copies, Available on Shelf, Currently Borrowed, Overdue Loans.
  * Interactive **Genre Distribution Chart** (Doughnut).
  * **Borrowing Circulation Trends Chart** (Bar).
  * Print / Export report functionality.

### 2. 👩‍🎓 Member Portal
* **Book Search & Catalog**:
  * Search by Title, Author, or Genre.
  * Filter quickly with genre pill tags (Computer Science, Classic Literature, Science, etc.).
  * Live clickable **Recent Searches** chips.
  * One-click **Borrow Book** button with real-time stock deduction.
* **My Active Borrowed Books & Returns**:
  * Dedicated dashboard of current loans with countdown badges and overdue warnings.
  * One-click **Return This Book Now** button with instant confirmation.
* **Borrowing History**:
  * Complete audit trail of past and current loans with dates, status, and fee breakdown.
* **Book Search History**:
  * Audit log of all search queries, criteria, match counts, and timestamps.
  * "Re-run" quick search button and "Clear History" option.
* **Profile Management**:
  * Form to edit Name, Email, Phone, and Password with instant save.
* **Alerts & Notification Preferences**:
  * Live inbox of incoming alerts (due dates, new arrivals, announcements).
  * Customizable toggle preferences for email/system alerts.

---

## 🏗️ Architecture & Technology Stack

* **Backend**: Pure Java 21 (Zero external dependencies; uses JDK's high-performance built-in `com.sun.net.httpserver`).
  * `com.library.Main`: Server bootstrap and lifecycle.
  * `com.library.server.LibraryServer`: Multi-threaded HTTP server with static & REST handlers.
  * `com.library.server.ApiHandler`: RESTful API endpoints for Books, Members, Transactions, Notifications, Reports, Profiles.
  * `com.library.server.StaticFileHandler`: Secure static asset serving with MIME type mapping.
  * `com.library.service.LibraryService`: Business rules, validation, inventory analytics, and seed data.
  * `com.library.util.JsonHelper`: Custom recursive-descent JSON parser and serializer.
  * `com.library.model.*`: Domain models (`Book`, `Member`, `Transaction`, `Notification`, `SearchRecord`).
* **Frontend**: HTML5, Modern CSS3 (CSS Grid & Flexbox, Theme variables, Glassmorphism), Vanilla JavaScript ES6+, Font Awesome Icons, Chart.js.

---

## 🚀 How to Run the Project

### Option A: Using Windows Batch Script (Fastest)
Double-click `run.bat` or run:
```cmd
run.bat
```

### Option B: Using PowerShell Script
```powershell
.\run.ps1
```

### Option C: Manual Command Line
```powershell
# 1. Compile Java classes into bin/
javac -encoding UTF-8 -d bin (Get-ChildItem -Path src -Recurse -Filter *.java).FullName

# 2. Run the server
java -cp bin com.library.Main 8080
```

Once started, open your browser and navigate to:
👉 **`http://localhost:8080`**

---

## 👥 Default Demo Accounts

| Role | Name / Email | Password | Details |
| :--- | :--- | :--- | :--- |
| **Librarian (Staff)** | `librarian@library.com` | `admin123` | Full access to Book, Member, Transaction, Notification & Inventory Management |
| **Member** | `alex@example.com` | `password123` | Member with 1 overdue book and 1 active loan to demo returns and alerts |
| **Member** | `sarah@example.com` | `password123` | Member with active book loan |
| **Member** | `rahul@example.com` | `password123` | Member with completed return history |

*Tip: You can seamlessly switch between Librarian and Members directly using the **Role dropdown** in the top navigation bar!*

---

## 🌐 REST API Endpoints

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/books` | Get all books or search (`?q=`, `?author=`, `?genre=`) |
| `POST` | `/api/books` | Add new book to catalog |
| `PUT` | `/api/books/{id}` | Update book details |
| `DELETE` | `/api/books/{id}` | Delete book (validates no active loans) |
| `GET` | `/api/members` | Get all members |
| `POST` | `/api/members` | Create new member account |
| `PUT` | `/api/members/{id}` | Update member profile |
| `DELETE` | `/api/members/{id}` | Delete member account |
| `GET` | `/api/transactions` | Get transactions (`?memberId=` optional) |
| `POST` | `/api/transactions/borrow` | Borrow / Issue a book |
| `POST` | `/api/transactions/return` | Process return & calculate fines |
| `GET` | `/api/notifications` | Get notifications list (`?memberId=` optional) |
| `POST` | `/api/notifications` | Broadcast or send a notification |
| `GET` | `/api/reports/inventory` | Inventory metrics & circulation trends |
| `POST` | `/api/member/preferences` | Update member notification preferences |
| `GET` | `/api/member/search-history` | Get member's book search history |
| `POST` | `/api/member/search-history` | Record search entry |
| `DELETE` | `/api/member/search-history` | Clear search history |
