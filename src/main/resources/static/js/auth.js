// Authentication & User State Manager
const Auth = {
    tokenKey: 'civicfix_jwt_token',
    userKey: 'civicfix_user_data',

    getToken() {
        return localStorage.getItem(this.tokenKey);
    },

    getUser() {
        const raw = localStorage.getItem(this.userKey);
        try {
            return raw ? JSON.parse(raw) : null;
        } catch (e) {
            return null;
        }
    },

    isAuthenticated() {
        return !!this.getToken();
    },

    hasRole(role) {
        const user = this.getUser();
        return user && user.role === role;
    },

    setAuth(token, user) {
        localStorage.setItem(this.tokenKey, token);
        localStorage.setItem(this.userKey, JSON.stringify(user));
        this.updateUI();
    },

    logout() {
        localStorage.removeItem(this.tokenKey);
        localStorage.removeItem(this.userKey);
        this.updateUI();
        App.showToast('You have been logged out.', 'info');
        App.showView('landing');
    },

    async login(email, password) {
        try {
            const res = await fetch('/api/auth/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email, password })
            });

            const data = await res.json();
            if (!res.ok) {
                throw new Error(data.message || 'Login failed');
            }

            this.setAuth(data.token, data.user);
            App.showToast(`Welcome back, ${data.user.name}!`, 'success');
            App.closeModal('auth-modal');

            // Route based on role
            if (data.user.role === 'ROLE_ADMIN') {
                App.showView('admin-dashboard');
            } else if (data.user.role === 'ROLE_OFFICER') {
                App.showView('officer-dashboard');
            } else {
                App.showView('citizen-dashboard');
            }
            return data;
        } catch (err) {
            App.showToast(err.message, 'error');
            throw err;
        }
    },

    async register(name, email, phone, password, role = 'ROLE_CITIZEN', departmentId = null) {
        try {
            const body = { name, email, phone, password, role };
            if (departmentId) body.departmentId = departmentId;

            const res = await fetch('/api/auth/register', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(body)
            });

            const data = await res.json();
            if (!res.ok) {
                let errorMsg = data.message || 'Registration failed';
                if (data.errors) {
                    errorMsg = Object.values(data.errors).join(', ');
                }
                throw new Error(errorMsg);
            }

            this.setAuth(data.token, data.user);
            App.showToast(`Registration successful! Welcome to CivicFix, ${data.user.name}.`, 'success');
            App.closeModal('auth-modal');
            App.showView('citizen-dashboard');
            return data;
        } catch (err) {
            App.showToast(err.message, 'error');
            throw err;
        }
    },

    // Fast Demo Quick-Login for presentations
    async quickLogin(role) {
        const credentials = {
            admin: { email: 'admin@civicfix.gov', pass: 'Admin@123' },
            officer: { email: 'officer.road@civicfix.gov', pass: 'Officer@123' },
            citizen: { email: 'citizen@example.com', pass: 'Citizen@123' }
        };

        const cred = credentials[role];
        if (cred) {
            await this.login(cred.email, cred.pass);
        }
    },

    // Updates navigation buttons and badges based on login state
    updateUI() {
        const user = this.getUser();
        const unauthNav = document.getElementById('nav-unauthenticated');
        const authNav = document.getElementById('nav-authenticated');
        const userNameEl = document.getElementById('nav-user-name');
        const userRoleBadge = document.getElementById('nav-user-role-badge');
        const notifBell = document.getElementById('nav-notifications-btn');
        const citizenDashboardLink = document.getElementById('nav-link-citizen-dashboard');
        const adminDashboardLink = document.getElementById('nav-link-admin-dashboard');
        const officerDashboardLink = document.getElementById('nav-link-officer-dashboard');

        if (this.isAuthenticated() && user) {
            if (unauthNav) unauthNav.classList.add('hidden');
            if (authNav) authNav.classList.remove('hidden');
            if (notifBell) notifBell.classList.remove('hidden');
            if (userNameEl) userNameEl.textContent = user.name.split(' ')[0];

            let roleName = 'Citizen';
            let roleClass = 'bg-blue-100 text-blue-800';
            if (user.role === 'ROLE_ADMIN') {
                roleName = 'Admin';
                roleClass = 'bg-purple-100 text-purple-800';
            } else if (user.role === 'ROLE_OFFICER') {
                roleName = 'Officer';
                roleClass = 'bg-emerald-100 text-emerald-800';
            }

            if (userRoleBadge) {
                userRoleBadge.textContent = roleName;
                userRoleBadge.className = `text-xs px-2 py-0.5 rounded-full font-semibold ${roleClass}`;
            }

            // Role specific menu items
            if (citizenDashboardLink) {
                user.role === 'ROLE_CITIZEN' ? citizenDashboardLink.classList.remove('hidden') : citizenDashboardLink.classList.add('hidden');
            }
            if (adminDashboardLink) {
                user.role === 'ROLE_ADMIN' ? adminDashboardLink.classList.remove('hidden') : adminDashboardLink.classList.add('hidden');
            }
            if (officerDashboardLink) {
                (user.role === 'ROLE_OFFICER' || user.role === 'ROLE_ADMIN') ? officerDashboardLink.classList.remove('hidden') : officerDashboardLink.classList.add('hidden');
            }

            // Start polling notifications
            App.loadNotifications();
        } else {
            if (unauthNav) unauthNav.classList.remove('hidden');
            if (authNav) authNav.classList.add('hidden');
            if (notifBell) notifBell.classList.add('hidden');
            if (citizenDashboardLink) citizenDashboardLink.classList.add('hidden');
            if (adminDashboardLink) adminDashboardLink.classList.add('hidden');
            if (officerDashboardLink) officerDashboardLink.classList.add('hidden');
        }
    }
};
