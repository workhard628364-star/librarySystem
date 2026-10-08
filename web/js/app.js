// =========================================================
// Online Library Management System - Frontend Application Logic
// =========================================================

const API_BASE = '/api';

// Application State
const state = {
  currentRole: 'LIBRARIAN', // 'LIBRARIAN' or memberId ('M101', etc.)
  currentMember: null,
  books: [],
  members: [],
  transactions: [],
  notifications: [],
  searchHistory: [],
  genreChart: null,
  trendsChart: null,
  selectedGenrePill: 'ALL'
};

// ---------------------------------------------------------
// Initialization
// ---------------------------------------------------------
document.addEventListener('DOMContentLoaded', async () => {
  await loadMembers();
  await loadBooks();
  
  // Set default view to Librarian
  await switchRole('LIBRARIAN');
});

// Toast notification helper
function showToast(message, type = 'success') {
  const container = document.getElementById('toast-container');
  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  
  let icon = 'fa-circle-check';
  if (type === 'error') icon = 'fa-circle-exclamation';
  if (type === 'info') icon = 'fa-circle-info';
  
  toast.innerHTML = `<i class="fa-solid ${icon}"></i> <span>${message}</span>`;
  container.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateX(100%)';
    toast.style.transition = 'all 0.3s ease';
    setTimeout(() => toast.remove(), 300);
  }, 4000);
}

// ---------------------------------------------------------
// Role Switching (Librarian <-> Member)
// ---------------------------------------------------------
async function switchRole(roleVal) {
  state.currentRole = roleVal;
  const userPill = document.getElementById('current-user-name');
  const libView = document.getElementById('librarian-view');
  const memView = document.getElementById('member-view');

  if (roleVal === 'LIBRARIAN') {
    libView.classList.add('active');
    memView.classList.remove('active');
    userPill.textContent = 'Librarian (Staff)';
    state.currentMember = null;
    await refreshLibrarianData();
  } else {
    libView.classList.remove('active');
    memView.classList.add('active');
    
    // Find current member
    const member = state.members.find(m => m.id === roleVal) || state.members[1];
    state.currentMember = member;
    userPill.textContent = member ? `${member.name} (Member)` : 'Member';
    
    await refreshMemberData();
  }
}

// ---------------------------------------------------------
// LIBRARIAN CONTROLLER & DATA LOADERS
// ---------------------------------------------------------
async function refreshLibrarianData() {
  await Promise.all([
    loadBooks(),
    loadMembers(),
    loadTransactions(),
    loadNotifications(),
    loadInventoryReport()
  ]);
  populateGenreFilter();
  populateNotificationRecipients();
  populateIssueModalOptions();
}

function switchLibTab(tabId) {
  document.querySelectorAll('#librarian-view .nav-item').forEach(b => b.classList.remove('active'));
  document.querySelectorAll('#librarian-view .tab-content').forEach(c => c.classList.remove('active'));

  event.currentTarget.classList.add('active');
  const target = document.getElementById(tabId);
  if (target) target.classList.add('active');

  if (tabId === 'lib-reports') {
    loadInventoryReport();
  }
}

// 1. Book Management
async function loadBooks() {
  try {
    const res = await fetch(`${API_BASE}/books`);
    state.books = await res.json();
    renderLibBooks(state.books);
    updateSidebarStats();
    if (state.currentRole !== 'LIBRARIAN') {
      renderMemberBooks(state.books);
    }
  } catch (err) {
    console.error('Failed to load books', err);
  }
}

function renderLibBooks(books) {
  const tbody = document.getElementById('lib-books-tbody');
  if (!tbody) return;

  if (books.length === 0) {
    tbody.innerHTML = `<tr><td colspan="8" class="text-center text-muted" style="padding:2rem;">No books found matching criteria.</td></tr>`;
    return;
  }

  tbody.innerHTML = books.map(b => {
    const isAvail = b.availableCopies > 0;
    const badgeClass = isAvail ? 'badge-success' : 'badge-danger';
    const statusText = isAvail ? 'Available' : 'All Borrowed';

    return `
      <tr>
        <td><code>${b.id}</code></td>
        <td><strong>${escapeHtml(b.title)}</strong></td>
        <td>${escapeHtml(b.author)}</td>
        <td><span class="badge badge-info">${escapeHtml(b.genre)}</span></td>
        <td><small class="text-muted">${escapeHtml(b.isbn)}</small></td>
        <td><strong>${b.availableCopies}</strong> / ${b.totalCopies}</td>
        <td><span class="badge ${badgeClass}">${statusText}</span></td>
        <td>
          <div style="display:flex; gap:0.4rem;">
            <button class="btn btn-outline btn-sm" onclick="openEditBookModal('${b.id}')" title="Edit Book">
              <i class="fa-solid fa-pen-to-square"></i>
            </button>
            <button class="btn btn-danger btn-sm" onclick="deleteBook('${b.id}', '${escapeHtml(b.title)}')" title="Delete Book">
              <i class="fa-solid fa-trash"></i>
            </button>
          </div>
        </td>
      </tr>
    `;
  }).join('');
}

function filterLibBooks() {
  const query = document.getElementById('lib-book-search').value.toLowerCase().trim();
  const genre = document.getElementById('lib-genre-filter').value;

  const filtered = state.books.filter(b => {
    const matchesQuery = !query ||
      b.title.toLowerCase().includes(query) ||
      b.author.toLowerCase().includes(query) ||
      b.isbn.toLowerCase().includes(query);

    const matchesGenre = (genre === 'ALL') || (b.genre.toLowerCase() === genre.toLowerCase());
    return matchesQuery && matchesGenre;
  });

  renderLibBooks(filtered);
}

function populateGenreFilter() {
  const select = document.getElementById('lib-genre-filter');
  if (!select) return;

  const genres = [...new Set(state.books.map(b => b.genre))].sort();
  const currentVal = select.value;
  select.innerHTML = `<option value="ALL">All Genres</option>` +
    genres.map(g => `<option value="${escapeHtml(g)}">${escapeHtml(g)}</option>`).join('');
  if (genres.includes(currentVal)) select.value = currentVal;
}

// Add & Edit Book Modals
function openAddBookModal() {
  document.getElementById('book-modal-title').textContent = 'Add New Book';
  document.getElementById('book-form-id').value = '';
  document.getElementById('book-form').reset();
  document.getElementById('book-copies').value = '5';
  document.getElementById('book-year').value = new Date().getFullYear();
  document.getElementById('book-modal').classList.add('active');
}

function openEditBookModal(bookId) {
  const book = state.books.find(b => b.id === bookId);
  if (!book) return;

  document.getElementById('book-modal-title').textContent = 'Edit Book Details';
  document.getElementById('book-form-id').value = book.id;
  document.getElementById('book-title').value = book.title;
  document.getElementById('book-author').value = book.author;
  document.getElementById('book-isbn').value = book.isbn;
  document.getElementById('book-genre').value = book.genre;
  document.getElementById('book-copies').value = book.totalCopies;
  document.getElementById('book-year').value = book.publishedYear || 2024;
  document.getElementById('book-modal').classList.add('active');
}

function closeBookModal() {
  document.getElementById('book-modal').classList.remove('active');
}

async function handleSaveBook(event) {
  event.preventDefault();
  const id = document.getElementById('book-form-id').value;
  const payload = {
    title: document.getElementById('book-title').value.trim(),
    author: document.getElementById('book-author').value.trim(),
    isbn: document.getElementById('book-isbn').value.trim(),
    genre: document.getElementById('book-genre').value.trim(),
    totalCopies: parseInt(document.getElementById('book-copies').value),
    publishedYear: parseInt(document.getElementById('book-year').value)
  };

  try {
    let res;
    if (id) {
      res = await fetch(`${API_BASE}/books/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });
    } else {
      res = await fetch(`${API_BASE}/books`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });
    }

    const data = await res.json();
    if (res.ok) {
      showToast(data.message || 'Book saved successfully!');
      closeBookModal();
      await loadBooks();
      populateGenreFilter();
    } else {
      showToast(data.error || 'Failed to save book', 'error');
    }
  } catch (err) {
    showToast('Network error while saving book', 'error');
  }
}

async function deleteBook(id, title) {
  if (!confirm(`Are you sure you want to delete book "${title}" (ID: ${id})?`)) {
    return;
  }

  try {
    const res = await fetch(`${API_BASE}/books/${id}`, { method: 'DELETE' });
    const data = await res.json();
    if (res.ok) {
      showToast(data.message || 'Book deleted successfully!');
      await loadBooks();
    } else {
      showToast(data.error || 'Could not delete book', 'error');
    }
  } catch (err) {
    showToast('Failed to connect to server', 'error');
  }
}

// 2. Member Management
async function loadMembers() {
  try {
    const res = await fetch(`${API_BASE}/members`);
    state.members = await res.json();
    renderLibMembers(state.members);

    // Update role select options
    const roleSelect = document.getElementById('role-select');
    if (roleSelect) {
      const regularMembers = state.members.filter(m => m.role === 'MEMBER');
      let html = `<option value="LIBRARIAN">Librarian (Staff: Dr. Eleanor Vance)</option>`;
      regularMembers.forEach(m => {
        html += `<option value="${m.id}">Member: ${m.name} (${m.email})</option>`;
      });
      const curr = roleSelect.value;
      roleSelect.innerHTML = html;
      if (curr) roleSelect.value = curr;
    }
  } catch (err) {
    console.error('Failed to load members', err);
  }
}

function renderLibMembers(members) {
  const tbody = document.getElementById('lib-members-tbody');
  if (!tbody) return;

  tbody.innerHTML = members.map(m => {
    const isLibrarian = m.role === 'LIBRARIAN';
    const roleBadge = isLibrarian ? 'badge-primary' : 'badge-info';

    return `
      <tr>
        <td><code>${m.id}</code></td>
        <td><strong>${escapeHtml(m.name)}</strong></td>
        <td>${escapeHtml(m.email)}</td>
        <td><span class="badge ${roleBadge}">${escapeHtml(m.membershipId)}</span></td>
        <td>${escapeHtml(m.phone || 'N/A')}</td>
        <td>${escapeHtml(m.role)}</td>
        <td><small class="text-muted">${m.joinedDate}</small></td>
        <td>
          <div style="display:flex; gap:0.4rem;">
            <button class="btn btn-outline btn-sm" onclick="openEditMemberModal('${m.id}')" title="Edit Member">
              <i class="fa-solid fa-user-pen"></i>
            </button>
            ${!isLibrarian ? `
              <button class="btn btn-danger btn-sm" onclick="deleteMember('${m.id}', '${escapeHtml(m.name)}')" title="Delete Member">
                <i class="fa-solid fa-trash"></i>
              </button>
            ` : ''}
          </div>
        </td>
      </tr>
    `;
  }).join('');
}

function openAddMemberModal() {
  document.getElementById('member-modal-title').textContent = 'Create New Member';
  document.getElementById('member-form-id').value = '';
  document.getElementById('member-form').reset();
  document.getElementById('mem-modal-pass-group').style.display = 'block';
  document.getElementById('member-modal').classList.add('active');
}

function openEditMemberModal(id) {
  const m = state.members.find(x => x.id === id);
  if (!m) return;

  document.getElementById('member-modal-title').textContent = 'Edit Member Profile';
  document.getElementById('member-form-id').value = m.id;
  document.getElementById('mem-modal-name').value = m.name;
  document.getElementById('mem-modal-email').value = m.email;
  document.getElementById('mem-modal-card').value = m.membershipId;
  document.getElementById('mem-modal-phone').value = m.phone || '';
  document.getElementById('mem-modal-pass-group').style.display = 'none';
  document.getElementById('member-modal').classList.add('active');
}

function closeMemberModal() {
  document.getElementById('member-modal').classList.remove('active');
}

async function handleSaveMember(event) {
  event.preventDefault();
  const id = document.getElementById('member-form-id').value;
  const payload = {
    name: document.getElementById('mem-modal-name').value.trim(),
    email: document.getElementById('mem-modal-email').value.trim(),
    membershipId: document.getElementById('mem-modal-card').value.trim(),
    phone: document.getElementById('mem-modal-phone').value.trim(),
    password: document.getElementById('mem-modal-pass').value.trim()
  };

  try {
    let res;
    if (id) {
      res = await fetch(`${API_BASE}/members/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });
    } else {
      res = await fetch(`${API_BASE}/members`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });
    }

    const data = await res.json();
    if (res.ok) {
      showToast(data.message || 'Member saved successfully!');
      closeMemberModal();
      await loadMembers();
    } else {
      showToast(data.error || 'Failed to save member', 'error');
    }
  } catch (err) {
    showToast('Network error while saving member', 'error');
  }
}

async function deleteMember(id, name) {
  if (!confirm(`Are you sure you want to delete member account for "${name}" (ID: ${id})?`)) {
    return;
  }

  try {
    const res = await fetch(`${API_BASE}/members/${id}`, { method: 'DELETE' });
    const data = await res.json();
    if (res.ok) {
      showToast(data.message || 'Member account deleted successfully!');
      await loadMembers();
    } else {
      showToast(data.error || 'Could not delete member', 'error');
    }
  } catch (err) {
    showToast('Failed to connect to server', 'error');
  }
}

// 3. Transaction Management
async function loadTransactions() {
  try {
    const res = await fetch(`${API_BASE}/transactions`);
    state.transactions = await res.json();
    renderLibTransactions(state.transactions);
    updateSidebarStats();
  } catch (err) {
    console.error('Failed to load transactions', err);
  }
}

function renderLibTransactions(txns) {
  const tbody = document.getElementById('lib-transactions-tbody');
  if (!tbody) return;

  if (txns.length === 0) {
    tbody.innerHTML = `<tr><td colspan="9" class="text-center text-muted" style="padding:2rem;">No transactions recorded.</td></tr>`;
    return;
  }

  tbody.innerHTML = txns.map(t => {
    const isBorrowed = t.status === 'BORROWED';
    const isOverdue = isBorrowed && new Date(t.dueDate) < new Date();
    let badgeClass = 'badge-success';
    let statusText = 'Returned';

    if (isBorrowed) {
      badgeClass = isOverdue ? 'badge-danger' : 'badge-warning';
      statusText = isOverdue ? 'Overdue' : 'Borrowed';
    }

    return `
      <tr>
        <td><code>${t.id}</code></td>
        <td><strong>${escapeHtml(t.bookTitle)}</strong></td>
        <td>${escapeHtml(t.memberName)}</td>
        <td><small>${t.borrowDate}</small></td>
        <td><small class="${isOverdue ? 'text-danger fw-bold' : ''}">${t.dueDate}</small></td>
        <td><small>${t.returnDate || '-'}</small></td>
        <td><span class="badge ${badgeClass}">${statusText}</span></td>
        <td>${t.fine > 0 ? `<span class="text-danger fw-bold">$${t.fine.toFixed(2)}</span>` : '$0.00'}</td>
        <td>
          ${isBorrowed ? `
            <button class="btn btn-outline btn-sm" onclick="processReturn('${t.id}')">
              <i class="fa-solid fa-rotate-left"></i> Process Return
            </button>
          ` : `<span class="text-muted"><i class="fa-solid fa-check"></i> Complete</span>`}
        </td>
      </tr>
    `;
  }).join('');
}

function filterLibTxns() {
  const query = document.getElementById('lib-txn-search').value.toLowerCase().trim();
  const status = document.getElementById('lib-txn-status-filter').value;

  const filtered = state.transactions.filter(t => {
    const matchesQuery = !query ||
      t.bookTitle.toLowerCase().includes(query) ||
      t.memberName.toLowerCase().includes(query) ||
      t.id.toLowerCase().includes(query);

    const matchesStatus = (status === 'ALL') || (t.status === status);
    return matchesQuery && matchesStatus;
  });

  renderLibTransactions(filtered);
}

function openIssueBookModal() {
  populateIssueModalOptions();
  document.getElementById('issue-modal').classList.add('active');
}

function closeIssueModal() {
  document.getElementById('issue-modal').classList.remove('active');
}

function populateIssueModalOptions() {
  const bookSelect = document.getElementById('issue-book-select');
  const memSelect = document.getElementById('issue-member-select');
  if (!bookSelect || !memSelect) return;

  const availBooks = state.books.filter(b => b.availableCopies > 0);
  bookSelect.innerHTML = availBooks.length > 0 ?
    availBooks.map(b => `<option value="${b.id}">${escapeHtml(b.title)} (${b.availableCopies} available)</option>`).join('') :
    `<option disabled>No books currently available</option>`;

  const regMembers = state.members.filter(m => m.role === 'MEMBER');
  memSelect.innerHTML = regMembers.map(m => `<option value="${m.id}">${escapeHtml(m.name)} (${m.membershipId})</option>`).join('');
}

async function handleIssueBook(event) {
  event.preventDefault();
  const bookId = document.getElementById('issue-book-select').value;
  const memberId = document.getElementById('issue-member-select').value;
  const loanDays = parseInt(document.getElementById('issue-days').value) || 14;

  try {
    const res = await fetch(`${API_BASE}/transactions/borrow`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ bookId, memberId, loanDays })
    });

    const data = await res.json();
    if (res.ok) {
      showToast(data.message || 'Book issued successfully!');
      closeIssueModal();
      await refreshLibrarianData();
    } else {
      showToast(data.error || 'Failed to issue book', 'error');
    }
  } catch (err) {
    showToast('Network error while issuing book', 'error');
  }
}

async function processReturn(txnId) {
  try {
    const res = await fetch(`${API_BASE}/transactions/return`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ transactionId: txnId })
    });

    const data = await res.json();
    if (res.ok) {
      showToast(data.message || 'Book returned successfully!');
      await refreshLibrarianData();
      if (state.currentRole !== 'LIBRARIAN') {
        await refreshMemberData();
      }
    } else {
      showToast(data.error || 'Failed to process return', 'error');
    }
  } catch (err) {
    showToast('Network error while processing return', 'error');
  }
}

// 4. Notification Management
async function loadNotifications() {
  try {
    const res = await fetch(`${API_BASE}/notifications`);
    state.notifications = await res.json();
    renderSentNotifications(state.notifications);
  } catch (err) {
    console.error('Failed to load notifications', err);
  }
}

function renderSentNotifications(notifs) {
  const container = document.getElementById('sent-notifs-list');
  if (!container) return;

  if (notifs.length === 0) {
    container.innerHTML = `<p class="text-muted text-center" style="padding:1.5rem;">No notifications sent yet.</p>`;
    return;
  }

  container.innerHTML = notifs.map(n => {
    let typeClass = 'notif-announcement';
    if (n.type === 'DUE_DATE') typeClass = 'notif-due';
    if (n.type === 'NEW_ARRIVAL') typeClass = 'notif-arrival';

    return `
      <div class="notif-item ${typeClass}">
        <div class="notif-header">
          <strong>${escapeHtml(n.title)}</strong>
          <span class="notif-meta">${n.timestamp}</span>
        </div>
        <p>${escapeHtml(n.message)}</p>
        <span class="badge badge-info" style="align-self:flex-start;">Recipient: ${escapeHtml(n.recipientName)}</span>
      </div>
    `;
  }).join('');
}

function populateNotificationRecipients() {
  const select = document.getElementById('notif-recipient');
  if (!select) return;

  const regMembers = state.members.filter(m => m.role === 'MEMBER');
  let html = `<option value="ALL">📢 All Registered Members (Broadcast)</option>`;
  regMembers.forEach(m => {
    html += `<option value="${m.id}">Patron: ${escapeHtml(m.name)} (${m.email})</option>`;
  });
  select.innerHTML = html;
}

async function handleSendNotification(event) {
  event.preventDefault();
  const payload = {
    recipientId: document.getElementById('notif-recipient').value,
    type: document.getElementById('notif-type').value,
    title: document.getElementById('notif-title').value.trim(),
    message: document.getElementById('notif-message').value.trim()
  };

  try {
    const res = await fetch(`${API_BASE}/notifications`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    const data = await res.json();
    if (res.ok) {
      showToast(data.message || 'Notification sent successfully!');
      document.getElementById('send-notification-form').reset();
      await loadNotifications();
    } else {
      showToast(data.error || 'Failed to send notification', 'error');
    }
  } catch (err) {
    showToast('Network error while sending notification', 'error');
  }
}

// 5. Inventory Reports & Analytics
async function loadInventoryReport() {
  try {
    const res = await fetch(`${API_BASE}/reports/inventory`);
    const report = await res.json();

    document.getElementById('report-total-books').textContent = report.totalCopies;
    document.getElementById('report-avail-books').textContent = report.availableCopies;
    document.getElementById('report-borrowed-books').textContent = report.borrowedCopies;
    document.getElementById('report-overdue-books').textContent = report.overdueLoans;

    renderInventoryCharts(report);
  } catch (err) {
    console.error('Failed to load inventory report', err);
  }
}

function renderInventoryCharts(report) {
  if (typeof Chart === 'undefined') return;

  // Chart 1: Genres Doughnut
  const genreCtx = document.getElementById('genreChart');
  if (genreCtx) {
    if (state.genreChart) state.genreChart.destroy();

    const genres = Object.keys(report.genreBreakdown || {});
    const counts = Object.values(report.genreBreakdown || {});

    state.genreChart = new Chart(genreCtx, {
      type: 'doughnut',
      data: {
        labels: genres,
        datasets: [{
          data: counts,
          backgroundColor: [
            '#2563eb', '#10b981', '#f59e0b', '#8b5cf6', '#ec4899', '#06b6d4', '#64748b'
          ]
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { position: 'bottom', labels: { boxWidth: 12 } }
        }
      }
    });
  }

  // Chart 2: Borrowing Trends Line
  const trendsCtx = document.getElementById('trendsChart');
  if (trendsCtx) {
    if (state.trendsChart) state.trendsChart.destroy();

    const months = Object.keys(report.borrowingTrends || {});
    const values = Object.values(report.borrowingTrends || {});

    state.trendsChart = new Chart(trendsCtx, {
      type: 'bar',
      data: {
        labels: months,
        datasets: [{
          label: 'Book Checkouts',
          data: values,
          backgroundColor: '#3b82f6',
          borderRadius: 6
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          y: { beginAtZero: true, ticks: { precision: 0 } }
        }
      }
    });
  }
}

function updateSidebarStats() {
  const totalBooks = state.books.reduce((acc, b) => acc + b.totalCopies, 0);
  const activeLoans = state.transactions.filter(t => t.status === 'BORROWED').length;
  const overdueLoans = state.transactions.filter(t => t.status === 'BORROWED' && new Date(t.dueDate) < new Date()).length;

  const totalEl = document.getElementById('sidebar-total-books');
  const activeEl = document.getElementById('sidebar-active-loans');
  const overdueEl = document.getElementById('sidebar-overdue-loans');

  if (totalEl) totalEl.textContent = totalBooks;
  if (activeEl) activeEl.textContent = activeLoans;
  if (overdueEl) overdueEl.textContent = overdueLoans;
}

// ---------------------------------------------------------
// MEMBER CONTROLLER & FEATURES
// ---------------------------------------------------------
async function refreshMemberData() {
  if (!state.currentMember) return;
  const memId = state.currentMember.id;

  await Promise.all([
    loadBooks(),
    loadMemberLoans(memId),
    loadMemberSearchHistory(memId),
    loadMemberNotifications(memId)
  ]);

  renderMemberProfile();
  renderMemberPreferences();
  buildGenrePillFilters();
}

function switchMemberTab(tabId) {
  document.querySelectorAll('#member-view .nav-item').forEach(b => b.classList.remove('active'));
  document.querySelectorAll('#member-view .tab-content').forEach(c => c.classList.remove('active'));

  event.currentTarget.classList.add('active');
  const target = document.getElementById(tabId);
  if (target) target.classList.add('active');
}

// 1. Member Book Catalog & Search
function renderMemberBooks(books) {
  const grid = document.getElementById('mem-books-grid');
  if (!grid) return;

  if (books.length === 0) {
    grid.innerHTML = `<div class="card" style="grid-column:1/-1; text-align:center; padding:3rem;">
      <i class="fa-solid fa-book-open" style="font-size:2.5rem; color:#94a3b8; margin-bottom:1rem;"></i>
      <p style="color:#64748b;">No books found matching your search criteria. Try a different query or genre filter.</p>
    </div>`;
    return;
  }

  grid.innerHTML = books.map(b => {
    const isAvail = b.availableCopies > 0;
    const badgeClass = isAvail ? 'badge-success' : 'badge-danger';
    const statusText = isAvail ? `${b.availableCopies} Copies Available` : 'All Copies Borrowed';

    return `
      <div class="book-card">
        <div class="book-card-header">
          <span class="book-genre-tag">${escapeHtml(b.genre)}</span>
          <h4 class="book-card-title">${escapeHtml(b.title)}</h4>
        </div>
        <div class="book-card-body">
          <p class="book-author"><i class="fa-solid fa-pen-nib"></i> ${escapeHtml(b.author)}</p>
          <div class="book-meta">
            <span><strong>ISBN:</strong> ${escapeHtml(b.isbn)}</span>
            <span><strong>Published:</strong> ${b.publishedYear || 'N/A'}</span>
          </div>
          <div class="book-stock-bar">
            <span class="badge ${badgeClass}">${statusText}</span>
            <button class="btn btn-primary btn-sm" ${!isAvail ? 'disabled' : ''} onclick="borrowBookAsMember('${b.id}', '${escapeHtml(b.title)}')">
              <i class="fa-solid fa-bookmark"></i> Borrow
            </button>
          </div>
        </div>
      </div>
    `;
  }).join('');
}

function buildGenrePillFilters() {
  const container = document.getElementById('mem-genre-pills');
  if (!container) return;

  const genres = ['ALL', ...new Set(state.books.map(b => b.genre))].sort();
  container.innerHTML = genres.map(g => `
    <button class="genre-pill ${state.selectedGenrePill === g ? 'active' : ''}" onclick="filterMemberByGenre('${g}')">
      ${g === 'ALL' ? 'All Genres' : escapeHtml(g)}
    </button>
  `).join('');
}

function filterMemberByGenre(genre) {
  state.selectedGenrePill = genre;
  buildGenrePillFilters();
  executeMemberSearch(false);
}

async function executeMemberSearch(recordHistory = true) {
  const input = document.getElementById('mem-search-input');
  const criteria = document.getElementById('mem-search-criteria').value;
  const query = input.value.trim();

  let author = '';
  let genre = state.selectedGenrePill;
  let q = query;

  if (criteria === 'author') {
    author = query;
    q = '';
  } else if (criteria === 'genre' && query) {
    genre = query;
    q = '';
  }

  const params = new URLSearchParams();
  if (q) params.set('q', q);
  if (author) params.set('author', author);
  if (genre && genre !== 'ALL') params.set('genre', genre);

  try {
    const res = await fetch(`${API_BASE}/books?${params.toString()}`);
    const results = await res.json();
    renderMemberBooks(results);

    // Save search history
    if (recordHistory && state.currentMember && query) {
      await fetch(`${API_BASE}/member/search-history`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          memberId: state.currentMember.id,
          query: query,
          searchCriteria: criteria,
          resultCount: results.length
        })
      });
      await loadMemberSearchHistory(state.currentMember.id);
    }
  } catch (err) {
    showToast('Failed to execute search', 'error');
  }
}

function resetMemberSearch() {
  document.getElementById('mem-search-input').value = '';
  document.getElementById('mem-search-criteria').value = 'all';
  state.selectedGenrePill = 'ALL';
  buildGenrePillFilters();
  renderMemberBooks(state.books);
}

// 2. Member Borrow & Return Operations
async function borrowBookAsMember(bookId, bookTitle) {
  if (!state.currentMember) {
    showToast('Please select a member profile first', 'error');
    return;
  }

  try {
    const res = await fetch(`${API_BASE}/transactions/borrow`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        bookId: bookId,
        memberId: state.currentMember.id,
        loanDays: 14
      })
    });

    const data = await res.json();
    if (res.ok) {
      showToast(`Success! You have borrowed "${bookTitle}". Due date: ${data.transaction.dueDate}`);
      await refreshMemberData();
    } else {
      showToast(data.error || 'Could not borrow book', 'error');
    }
  } catch (err) {
    showToast('Network error during borrowing', 'error');
  }
}

async function returnBookAsMember(txnId, title) {
  try {
    const res = await fetch(`${API_BASE}/transactions/return`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ transactionId: txnId })
    });

    const data = await res.json();
    if (res.ok) {
      showToast(data.message || `Book "${title}" returned successfully!`);
      await refreshMemberData();
    } else {
      showToast(data.error || 'Failed to return book', 'error');
    }
  } catch (err) {
    showToast('Network error during book return', 'error');
  }
}

// 3. Member Loans & History
async function loadMemberLoans(memberId) {
  try {
    const res = await fetch(`${API_BASE}/transactions?memberId=${memberId}`);
    const txns = await res.json();

    // Render Active Loans
    const active = txns.filter(t => t.status === 'BORROWED');
    renderMemberActiveLoans(active);

    // Render History Table
    renderMemberHistoryTable(txns);

    // Update Member Sidebar Stats
    document.getElementById('mem-stat-active').textContent = active.length;
    document.getElementById('mem-stat-total').textContent = txns.length;
    const totalFine = txns.reduce((sum, t) => sum + (t.fine || 0), 0);
    document.getElementById('mem-stat-fine').textContent = `$${totalFine.toFixed(2)}`;
  } catch (err) {
    console.error('Failed to load member loans', err);
  }
}

function renderMemberActiveLoans(loans) {
  const container = document.getElementById('mem-active-loans-list');
  if (!container) return;

  if (loans.length === 0) {
    container.innerHTML = `<div class="card" style="grid-column:1/-1; text-align:center; padding:3rem;">
      <i class="fa-solid fa-circle-check" style="font-size:2.5rem; color:#10b981; margin-bottom:1rem;"></i>
      <p style="color:#64748b;">You have no active loans right now. Head over to the Book Search tab to borrow books!</p>
    </div>`;
    return;
  }

  const today = new Date();
  container.innerHTML = loans.map(l => {
    const dueDate = new Date(l.dueDate);
    const diffDays = Math.ceil((dueDate - today) / (1000 * 60 * 60 * 24));
    const isOverdue = diffDays < 0;

    return `
      <div class="active-loan-card ${isOverdue ? 'overdue' : ''}">
        <div style="display:flex; justify-content:space-between; align-items:flex-start;">
          <h4 style="font-size:1.05rem;">${escapeHtml(l.bookTitle)}</h4>
          <span class="badge ${isOverdue ? 'badge-danger' : 'badge-warning'}">
            ${isOverdue ? `Overdue by ${Math.abs(diffDays)} days` : `${diffDays} days remaining`}
          </span>
        </div>
        <p class="subtitle"><i class="fa-solid fa-calendar-day"></i> Borrowed on: ${l.borrowDate} | Due: <strong>${l.dueDate}</strong></p>
        <button class="btn btn-outline btn-block" onclick="returnBookAsMember('${l.id}', '${escapeHtml(l.bookTitle)}')">
          <i class="fa-solid fa-rotate-left"></i> Return This Book Now
        </button>
      </div>
    `;
  }).join('');
}

function renderMemberHistoryTable(txns) {
  const tbody = document.getElementById('mem-history-tbody');
  if (!tbody) return;

  if (txns.length === 0) {
    tbody.innerHTML = `<tr><td colspan="7" class="text-center text-muted" style="padding:2rem;">No borrowing history yet.</td></tr>`;
    return;
  }

  tbody.innerHTML = txns.map(t => {
    const isReturned = t.status === 'RETURNED';
    const badgeClass = isReturned ? 'badge-success' : 'badge-warning';

    return `
      <tr>
        <td><code>${t.id}</code></td>
        <td><strong>${escapeHtml(t.bookTitle)}</strong></td>
        <td>${t.borrowDate}</td>
        <td>${t.dueDate}</td>
        <td>${t.returnDate || '-'}</td>
        <td><span class="badge ${badgeClass}">${t.status}</span></td>
        <td>${t.fine > 0 ? `<span class="text-danger fw-bold">$${t.fine.toFixed(2)}</span>` : '$0.00'}</td>
      </tr>
    `;
  }).join('');
}

// 4. Member Search History
async function loadMemberSearchHistory(memberId) {
  try {
    const res = await fetch(`${API_BASE}/member/search-history?memberId=${memberId}`);
    state.searchHistory = await res.json();
    renderMemberSearchHistoryTable(state.searchHistory);
    renderRecentSearchChips(state.searchHistory);
  } catch (err) {
    console.error('Failed to load search history', err);
  }
}

function renderMemberSearchHistoryTable(records) {
  const tbody = document.getElementById('mem-search-history-tbody');
  if (!tbody) return;

  if (records.length === 0) {
    tbody.innerHTML = `<tr><td colspan="5" class="text-center text-muted" style="padding:2rem;">No previous searches recorded.</td></tr>`;
    return;
  }

  tbody.innerHTML = records.map(r => `
    <tr>
      <td><strong>${escapeHtml(r.query)}</strong></td>
      <td><span class="badge badge-info">${escapeHtml(r.searchCriteria)}</span></td>
      <td>${r.resultCount} books</td>
      <td><small class="text-muted">${r.timestamp}</small></td>
      <td>
        <button class="btn btn-outline btn-sm" onclick="rerunSearch('${escapeHtml(r.query)}', '${escapeHtml(r.searchCriteria)}')">
          <i class="fa-solid fa-arrow-rotate-right"></i> Re-run
        </button>
      </td>
    </tr>
  `).join('');
}

function renderRecentSearchChips(records) {
  const container = document.getElementById('recent-search-chips');
  if (!container) return;

  const uniqueQueries = [...new Set(records.map(r => r.query))].slice(0, 5);
  if (uniqueQueries.length === 0) {
    container.innerHTML = `<span class="text-muted" style="font-size:0.8rem;">None</span>`;
    return;
  }

  container.innerHTML = uniqueQueries.map(q => `
    <span class="search-chip" onclick="rerunSearch('${escapeHtml(q)}', 'all')">
      <i class="fa-solid fa-magnifying-glass"></i> ${escapeHtml(q)}
    </span>
  `).join('');
}

function rerunSearch(query, criteria) {
  document.getElementById('mem-search-input').value = query;
  document.getElementById('mem-search-criteria').value = criteria || 'all';
  switchMemberTab('mem-search');
  executeMemberSearch(false);
}

async function clearMySearchHistory() {
  if (!state.currentMember) return;
  try {
    await fetch(`${API_BASE}/member/search-history?memberId=${state.currentMember.id}`, { method: 'DELETE' });
    showToast('Search history cleared!');
    await loadMemberSearchHistory(state.currentMember.id);
  } catch (err) {
    showToast('Failed to clear search history', 'error');
  }
}

// 5. Member Profile Management
function renderMemberProfile() {
  if (!state.currentMember) return;
  const m = state.currentMember;

  document.getElementById('prof-display-name').textContent = m.name;
  document.getElementById('prof-display-meta').textContent = `Membership ID: ${m.membershipId} | Joined: ${m.joinedDate}`;
  document.getElementById('prof-name').value = m.name;
  document.getElementById('prof-email').value = m.email;
  document.getElementById('prof-phone').value = m.phone || '';
  document.getElementById('prof-password').value = '';
}

async function handleUpdateProfile(event) {
  event.preventDefault();
  if (!state.currentMember) return;

  const payload = {
    name: document.getElementById('prof-name').value.trim(),
    email: document.getElementById('prof-email').value.trim(),
    phone: document.getElementById('prof-phone').value.trim(),
    password: document.getElementById('prof-password').value.trim()
  };

  try {
    const res = await fetch(`${API_BASE}/members/${state.currentMember.id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    const data = await res.json();
    if (res.ok) {
      showToast(data.message || 'Profile updated successfully!');
      state.currentMember = data.member;
      await loadMembers();
      renderMemberProfile();
      document.getElementById('current-user-name').textContent = `${data.member.name} (Member)`;
    } else {
      showToast(data.error || 'Failed to update profile', 'error');
    }
  } catch (err) {
    showToast('Network error while updating profile', 'error');
  }
}

// 6. Member Notifications & Alerts
async function loadMemberNotifications(memberId) {
  try {
    const res = await fetch(`${API_BASE}/notifications?memberId=${memberId}`);
    const notifs = await res.json();
    renderMemberAlerts(notifs);
  } catch (err) {
    console.error('Failed to load member notifications', err);
  }
}

function renderMemberAlerts(notifs) {
  const container = document.getElementById('mem-notifs-stream');
  if (!container) return;

  if (notifs.length === 0) {
    container.innerHTML = `<p class="text-muted text-center" style="padding:2rem;">No alerts at this time.</p>`;
    return;
  }

  container.innerHTML = notifs.map(n => {
    let typeClass = 'notif-announcement';
    let icon = 'fa-bullhorn';
    if (n.type === 'DUE_DATE') { typeClass = 'notif-due'; icon = 'fa-clock'; }
    if (n.type === 'NEW_ARRIVAL') { typeClass = 'notif-arrival'; icon = 'fa-sparkles'; }

    return `
      <div class="notif-item ${typeClass}">
        <div class="notif-header">
          <strong><i class="fa-solid ${icon}"></i> ${escapeHtml(n.title)}</strong>
          <span class="notif-meta">${n.timestamp}</span>
        </div>
        <p>${escapeHtml(n.message)}</p>
      </div>
    `;
  }).join('');
}

function renderMemberPreferences() {
  if (!state.currentMember) return;
  const m = state.currentMember;
  document.getElementById('pref-due-date').checked = m.prefDueDateAlerts !== false;
  document.getElementById('pref-new-arrivals').checked = m.prefNewArrivalAlerts !== false;
  document.getElementById('pref-newsletter').checked = !!m.prefNewsletter;
}

async function handleSavePreferences(event) {
  event.preventDefault();
  if (!state.currentMember) return;

  const payload = {
    memberId: state.currentMember.id,
    prefDueDateAlerts: document.getElementById('pref-due-date').checked,
    prefNewArrivalAlerts: document.getElementById('pref-new-arrivals').checked,
    prefNewsletter: document.getElementById('pref-newsletter').checked
  };

  try {
    const res = await fetch(`${API_BASE}/member/preferences`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    const data = await res.json();
    if (res.ok) {
      showToast(data.message || 'Notification preferences saved!');
      state.currentMember = data.member;
    } else {
      showToast(data.error || 'Failed to save preferences', 'error');
    }
  } catch (err) {
    showToast('Network error while saving preferences', 'error');
  }
}

// ---------------------------------------------------------
// Helper Utilities
// ---------------------------------------------------------
function escapeHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}
