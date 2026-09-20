// Core Application Controller for CivicFix
const App = {
    currentView: 'landing',
    notificationPollInterval: null,

    init() {
        Auth.updateUI();
        this.setupNavigation();
        this.loadHeroStatistics();

        // Check URL hash for direct routing (e.g. #track=CIV-2026-00001)
        this.handleHashRoute();
        window.addEventListener('hashchange', () => this.handleHashRoute());

        // Notifications auto-polling every 25 seconds when logged in
        this.notificationPollInterval = setInterval(() => {
            if (Auth.isAuthenticated()) {
                this.loadNotifications();
            }
        }, 25000);
    },

    setupNavigation() {
        document.querySelectorAll('[data-nav]').forEach(el => {
            el.addEventListener('click', (e) => {
                e.preventDefault();
                const view = el.getAttribute('data-nav');
                this.showView(view);
            });
        });
    },

    handleHashRoute() {
        const hash = window.location.hash.substring(1);
        if (!hash) {
            this.showView('landing');
            return;
        }

        if (hash.startsWith('track=')) {
            const code = hash.replace('track=', '');
            this.showView('track');
            document.getElementById('track-search-input').value = code;
            Complaints.trackComplaint(code);
        } else if (['landing', 'report', 'track', 'public', 'citizen-dashboard', 'admin-dashboard', 'officer-dashboard'].includes(hash)) {
            this.showView(hash);
        }
    },

    showView(viewName) {
        // Enforce Authentication for dashboards & reporting if needed
        if (viewName === 'citizen-dashboard' && !Auth.isAuthenticated()) {
            this.showToast('Please sign in to view your dashboard.', 'info');
            this.openAuthModal('login');
            return;
        }
        if (viewName === 'admin-dashboard' && !Auth.hasRole('ROLE_ADMIN')) {
            this.showToast('Access restricted to municipal administrators.', 'error');
            return;
        }
        if (viewName === 'officer-dashboard' && !Auth.hasRole('ROLE_OFFICER') && !Auth.hasRole('ROLE_ADMIN')) {
            this.showToast('Access restricted to field officers.', 'error');
            return;
        }

        // Hide all views
        document.querySelectorAll('.app-view').forEach(v => v.classList.add('hidden'));

        // Show target view
        const target = document.getElementById(`view-${viewName}`);
        if (target) {
            target.classList.remove('hidden');
            window.scrollTo({ top: 0, behavior: 'smooth' });
            this.currentView = viewName;
            window.location.hash = viewName;
        }

        // View-specific initializations
        if (viewName === 'report') {
            Complaints.initReportForm();
        } else if (viewName === 'public') {
            Complaints.loadPublicComplaints();
        } else if (viewName === 'citizen-dashboard') {
            Complaints.loadCitizenDashboard();
        } else if (viewName === 'admin-dashboard') {
            Admin.loadAdminDashboard();
        } else if (viewName === 'officer-dashboard') {
            Officer.loadOfficerDashboard();
        } else if (viewName === 'landing') {
            this.loadHeroStatistics();
        }
    },

    // Hero Statistics & Animated Counters
    async loadHeroStatistics() {
        try {
            const res = await fetch('/api/statistics/summary');
            if (!res.ok) return;
            const stats = await res.json();

            this.animateCounter('stat-hero-total', stats.totalReported || 0);
            this.animateCounter('stat-hero-resolved', stats.resolved || 0);
            this.animateCounter('stat-hero-progress', stats.inProgress || 0);
            this.animateCounter('stat-hero-active', stats.activeIssues || 0);
        } catch (e) {
            // Fail silently
        }
    },

    animateCounter(elementId, targetValue) {
        const el = document.getElementById(elementId);
        if (!el) return;

        let current = 0;
        const duration = 1200;
        const steps = 30;
        const increment = targetValue / steps;
        const stepTime = duration / steps;

        const timer = setInterval(() => {
            current += increment;
            if (current >= targetValue) {
                el.textContent = targetValue;
                clearInterval(timer);
            } else {
                el.textContent = Math.floor(current);
            }
        }, stepTime);
    },

    // In-App Notification System
    async loadNotifications() {
        if (!Auth.isAuthenticated()) return;

        try {
            const [countRes, notifRes] = await Promise.all([
                fetch('/api/notifications/unread-count', { headers: { 'Authorization': `Bearer ${Auth.getToken()}` } }),
                fetch('/api/notifications', { headers: { 'Authorization': `Bearer ${Auth.getToken()}` } })
            ]);

            if (countRes.ok) {
                const countData = await countRes.json();
                const badge = document.getElementById('notif-badge');
                if (badge) {
                    if (countData.unreadCount > 0) {
                        badge.textContent = countData.unreadCount > 9 ? '9+' : countData.unreadCount;
                        badge.classList.remove('hidden');
                    } else {
                        badge.classList.add('hidden');
                    }
                }
            }

            if (notifRes.ok) {
                const notifs = await notifRes.json();
                this.renderNotificationsList(notifs);
            }
        } catch (e) {
            // Fail silently
        }
    },

    renderNotificationsList(notifs = []) {
        const list = document.getElementById('notifications-list');
        if (!list) return;

        if (notifs.length === 0) {
            list.innerHTML = '<p class="text-xs text-slate-400 text-center py-6">No notifications yet.</p>';
            return;
        }

        list.innerHTML = notifs.slice(0, 8).map(n => `
            <div onclick="App.handleNotificationClick(${n.id}, '${n.complaint ? n.complaint.complaintId : ''}')" class="p-3 hover:bg-slate-50 border-b border-slate-100 cursor-pointer transition flex items-start gap-2.5 ${n.read ? 'opacity-70' : 'bg-blue-50/40 font-medium'}">
                <div class="w-2 h-2 rounded-full mt-1.5 ${n.read ? 'bg-transparent' : 'bg-blue-600'} shrink-0"></div>
                <div class="flex-1 text-xs">
                    <p class="text-slate-800">${n.message}</p>
                    <span class="text-[10px] text-slate-400 mt-1 block">${this.formatDate(n.createdAt)}</span>
                </div>
            </div>
        `).join('');
    },

    async handleNotificationClick(id, complaintCode) {
        try {
            await fetch(`/api/notifications/${id}/read`, {
                method: 'PUT',
                headers: { 'Authorization': `Bearer ${Auth.getToken()}` }
            });
            this.toggleNotificationDropdown(false);
            this.loadNotifications();

            if (complaintCode) {
                this.showView('track');
                document.getElementById('track-search-input').value = complaintCode;
                Complaints.trackComplaint(complaintCode);
            }
        } catch (e) {
            // Silently proceed
        }
    },

    async markAllNotificationsRead() {
        try {
            await fetch('/api/notifications/read-all', {
                method: 'PUT',
                headers: { 'Authorization': `Bearer ${Auth.getToken()}` }
            });
            this.showToast('All notifications marked as read', 'success');
            this.loadNotifications();
        } catch (e) {
            // Fail silently
        }
    },

    toggleNotificationDropdown(forceState = null) {
        const dropdown = document.getElementById('notifications-dropdown');
        if (!dropdown) return;
        if (forceState !== null) {
            forceState ? dropdown.classList.remove('hidden') : dropdown.classList.add('hidden');
        } else {
            dropdown.classList.toggle('hidden');
        }
    },

    // Modal Operations
    openModal(modalId) {
        const m = document.getElementById(modalId);
        if (m) {
            m.classList.remove('hidden');
            document.body.classList.add('overflow-hidden');
        }
    },

    closeModal(modalId) {
        const m = document.getElementById(modalId);
        if (m) {
            m.classList.add('hidden');
            document.body.classList.remove('overflow-hidden');
        }
    },

    openAuthModal(mode = 'login') {
        const loginTab = document.getElementById('tab-btn-login');
        const regTab = document.getElementById('tab-btn-register');
        const loginForm = document.getElementById('auth-form-login');
        const regForm = document.getElementById('auth-form-register');

        if (mode === 'login') {
            loginTab.className = 'w-1/2 py-2.5 font-bold text-sm text-blue-600 border-b-2 border-blue-600 transition';
            regTab.className = 'w-1/2 py-2.5 font-semibold text-sm text-slate-500 hover:text-slate-800 transition';
            loginForm.classList.remove('hidden');
            regForm.classList.add('hidden');
        } else {
            regTab.className = 'w-1/2 py-2.5 font-bold text-sm text-blue-600 border-b-2 border-blue-600 transition';
            loginTab.className = 'w-1/2 py-2.5 font-semibold text-sm text-slate-500 hover:text-slate-800 transition';
            regForm.classList.remove('hidden');
            loginForm.classList.add('hidden');
        }

        this.openModal('auth-modal');
    },

    // Toast Notifications
    showToast(message, type = 'info') {
        const container = document.getElementById('toast-container');
        if (!container) return;

        const toast = document.createElement('div');
        toast.className = `toast ${type}`;

        const icons = {
            success: 'fa-circle-check text-emerald-600',
            error: 'fa-circle-exclamation text-rose-600',
            warning: 'fa-triangle-exclamation text-amber-500',
            info: 'fa-circle-info text-blue-600'
        };

        toast.innerHTML = `
            <i class="fa-solid ${icons[type] || icons.info} text-lg"></i>
            <span class="text-xs font-semibold text-slate-800 flex-1">${message}</span>
            <button onclick="this.parentElement.remove()" class="text-slate-400 hover:text-slate-600 text-xs">
                <i class="fa-solid fa-xmark"></i>
            </button>
        `;

        container.appendChild(toast);
        setTimeout(() => {
            if (toast.parentElement) toast.remove();
        }, 4500);
    },

    // Formatting Utilities
    formatStatus(status) {
        if (!status) return 'Unknown';
        const map = {
            'SUBMITTED': 'Submitted',
            'UNDER_REVIEW': 'Under Review',
            'VERIFIED': 'Verified',
            'ASSIGNED': 'Assigned',
            'IN_PROGRESS': 'In Progress',
            'RESOLVED': 'Resolved',
            'CLOSED': 'Closed',
            'REJECTED': 'Rejected'
        };
        return map[status] || status;
    },

    getStatusBadgeClass(status) {
        if (!status) return 'badge-submitted';
        return `badge-${status.toLowerCase()}`;
    },

    getPriorityBadgeClass(priority) {
        if (!priority) return 'priority-medium';
        return `priority-${priority.toLowerCase()}`;
    },

    formatDate(dateStr) {
        if (!dateStr) return '';
        const d = new Date(dateStr);
        if (isNaN(d.getTime())) return dateStr;
        return d.toLocaleDateString('en-US', {
            month: 'short',
            day: 'numeric',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    }
};

// Bootstrap application on load
document.addEventListener('DOMContentLoaded', () => {
    App.init();
});
