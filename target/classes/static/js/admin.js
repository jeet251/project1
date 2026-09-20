// Municipal Admin Module: Metrics, Charts, and Operational Workflow
const Admin = {
    categoryChart: null,
    statusChart: null,
    priorityChart: null,
    departmentsCache: [],
    officersCache: [],
    currentActionComplaintId: null,

    async loadAdminDashboard() {
        if (!Auth.hasRole('ROLE_ADMIN')) {
            App.showToast('Access denied: Municipal Admin privileges required.', 'error');
            App.showView('landing');
            return;
        }

        await Promise.all([
            this.loadStatistics(),
            this.loadAdminComplaints(),
            this.loadDepartmentsAndOfficers()
        ]);
    },

    async loadStatistics() {
        try {
            const res = await fetch('/api/admin/statistics', {
                headers: { 'Authorization': `Bearer ${Auth.getToken()}` }
            });
            const stats = await res.json();

            // Populate Summary Metric Cards
            document.getElementById('admin-stat-total').textContent = stats.totalComplaints;
            document.getElementById('admin-stat-new').textContent = stats.newComplaints;
            document.getElementById('admin-stat-review').textContent = stats.underReviewComplaints;
            document.getElementById('admin-stat-progress').textContent = stats.inProgressComplaints;
            document.getElementById('admin-stat-resolved').textContent = stats.resolvedComplaints;
            document.getElementById('admin-stat-rejected').textContent = stats.rejectedComplaints;

            // Render Chart.js Analytics
            this.renderCategoryChart(stats.complaintsByCategory);
            this.renderStatusChart(stats.complaintsByStatus);
            this.renderPriorityChart(stats.complaintsByPriority);

        } catch (err) {
            App.showToast('Failed to load admin statistics', 'error');
        }
    },

    renderCategoryChart(byCategory = {}) {
        const ctx = document.getElementById('admin-chart-category')?.getContext('2d');
        if (!ctx) return;

        if (this.categoryChart) this.categoryChart.destroy();

        const labels = Object.keys(byCategory);
        const data = Object.values(byCategory);
        const colors = [
            '#2563eb', '#059669', '#d97706', '#dc2626', '#7c3aed',
            '#0891b2', '#4f46e5', '#db2777', '#16a34a', '#64748b'
        ];

        this.categoryChart = new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels,
                datasets: [{
                    data,
                    backgroundColor: colors.slice(0, labels.length),
                    borderWidth: 2,
                    borderColor: '#ffffff'
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { position: 'right', labels: { boxWidth: 12, font: { size: 11 } } }
                }
            }
        });
    },

    renderStatusChart(byStatus = {}) {
        const ctx = document.getElementById('admin-chart-status')?.getContext('2d');
        if (!ctx) return;

        if (this.statusChart) this.statusChart.destroy();

        const labels = Object.keys(byStatus).map(s => App.formatStatus(s));
        const data = Object.values(byStatus);

        this.statusChart = new Chart(ctx, {
            type: 'bar',
            data: {
                labels,
                datasets: [{
                    label: 'Complaints',
                    data,
                    backgroundColor: '#3b82f6',
                    borderRadius: 6
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { display: false } },
                scales: {
                    y: { beginAtZero: true, ticks: { precision: 0 } },
                    x: { ticks: { font: { size: 10 } } }
                }
            }
        });
    },

    renderPriorityChart(byPriority = {}) {
        const ctx = document.getElementById('admin-chart-priority')?.getContext('2d');
        if (!ctx) return;

        if (this.priorityChart) this.priorityChart.destroy();

        const labels = Object.keys(byPriority);
        const data = Object.values(byPriority);
        const colors = {
            'LOW': '#94a3b8',
            'MEDIUM': '#38bdf8',
            'HIGH': '#fb923c',
            'CRITICAL': '#ef4444'
        };

        this.priorityChart = new Chart(ctx, {
            type: 'pie',
            data: {
                labels,
                datasets: [{
                    data,
                    backgroundColor: labels.map(l => colors[l] || '#94a3b8'),
                    borderWidth: 2,
                    borderColor: '#ffffff'
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { position: 'bottom', labels: { boxWidth: 12, font: { size: 11 } } }
                }
            }
        });
    },

    async loadAdminComplaints() {
        const status = document.getElementById('admin-filter-status')?.value || '';
        const priority = document.getElementById('admin-filter-priority')?.value || '';
        const keyword = document.getElementById('admin-filter-keyword')?.value || '';

        const params = new URLSearchParams();
        if (status) params.append('status', status);
        if (priority) params.append('priority', priority);
        if (keyword) params.append('keyword', keyword);
        params.append('size', '50');

        try {
            const res = await fetch(`/api/admin/complaints?${params.toString()}`, {
                headers: { 'Authorization': `Bearer ${Auth.getToken()}` }
            });
            const data = await res.json();
            const complaints = data.content || [];

            const tbody = document.getElementById('admin-complaints-tbody');
            if (!tbody) return;

            if (complaints.length === 0) {
                tbody.innerHTML = `
                    <tr>
                        <td colspan="8" class="text-center py-8 text-slate-400 italic">No complaints found.</td>
                    </tr>
                `;
                return;
            }

            tbody.innerHTML = complaints.map(c => `
                <tr class="hover:bg-slate-50 transition border-b border-slate-100 text-xs">
                    <td class="px-4 py-3 font-mono font-bold text-blue-600">${c.complaintId}</td>
                    <td class="px-4 py-3">
                        <div class="font-bold text-slate-800">${c.title}</div>
                        <div class="text-[11px] text-slate-500">${c.category} • ${c.address}</div>
                    </td>
                    <td class="px-4 py-3">
                        <div class="font-medium text-slate-700">${c.citizenName || 'Citizen'}</div>
                        <div class="text-[11px] text-slate-400">${c.citizenPhone || ''}</div>
                    </td>
                    <td class="px-4 py-3">
                        <span class="px-2 py-0.5 rounded font-bold ${App.getPriorityBadgeClass(c.priority)}">${c.priority}</span>
                    </td>
                    <td class="px-4 py-3">
                        <span class="px-2.5 py-0.5 rounded-full font-semibold ${App.getStatusBadgeClass(c.status)}">${App.formatStatus(c.status)}</span>
                    </td>
                    <td class="px-4 py-3">
                        <div class="font-medium text-slate-700">${c.departmentName || '<span class="text-amber-600 italic">Unassigned</span>'}</div>
                        <div class="text-[11px] text-slate-400">${c.assignedOfficerName ? 'Officer: ' + c.assignedOfficerName : ''}</div>
                    </td>
                    <td class="px-4 py-3 text-slate-500">${App.formatDate(c.createdAt)}</td>
                    <td class="px-4 py-3 text-right whitespace-nowrap">
                        <div class="flex items-center justify-end gap-1.5">
                            ${c.status === 'SUBMITTED' ? `
                                <button onclick="Admin.verifyComplaint(${c.id})" class="px-2 py-1 bg-emerald-600 hover:bg-emerald-700 text-white rounded font-medium text-[11px] transition" title="Verify Complaint">
                                    Verify
                                </button>
                            ` : ''}
                            <button onclick="Admin.openAssignModal(${c.id}, '${c.complaintId}')" class="px-2 py-1 bg-indigo-600 hover:bg-indigo-700 text-white rounded font-medium text-[11px] transition" title="Assign Department">
                                Assign
                            </button>
                            <button onclick="Admin.openStatusModal(${c.id}, '${c.complaintId}', '${c.status}')" class="px-2 py-1 bg-blue-600 hover:bg-blue-700 text-white rounded font-medium text-[11px] transition" title="Update Status">
                                Status
                            </button>
                            <button onclick="Complaints.showDetailsModal('${c.complaintId}')" class="px-2 py-1 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded font-medium text-[11px] transition" title="View Full Details">
                                <i class="fa-solid fa-eye"></i>
                            </button>
                        </div>
                    </td>
                </tr>
            `).join('');

        } catch (err) {
            App.showToast('Failed to load admin complaints', 'error');
        }
    },

    async loadDepartmentsAndOfficers() {
        try {
            const [deptRes, officerRes] = await Promise.all([
                fetch('/api/departments'),
                fetch('/api/admin/officers', { headers: { 'Authorization': `Bearer ${Auth.getToken()}` } })
            ]);

            if (deptRes.ok) this.departmentsCache = await deptRes.json();
            if (officerRes.ok) this.officersCache = await officerRes.json();
        } catch (e) {
            // Silently handle
        }
    },

    async verifyComplaint(id) {
        try {
            const res = await fetch(`/api/admin/complaints/${id}/status`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${Auth.getToken()}`
                },
                body: JSON.stringify({ status: 'VERIFIED', note: 'Complaint verified by Municipal Admin.' })
            });

            if (!res.ok) throw new Error('Failed to verify complaint');
            App.showToast('Complaint verified successfully!', 'success');
            this.loadAdminDashboard();
        } catch (err) {
            App.showToast(err.message, 'error');
        }
    },

    openAssignModal(id, code) {
        this.currentActionComplaintId = id;
        document.getElementById('assign-complaint-code').textContent = code;

        // Populate departments dropdown
        const deptSelect = document.getElementById('assign-dept-select');
        deptSelect.innerHTML = '<option value="">Select Department...</option>' +
            this.departmentsCache.map(d => `<option value="${d.id}">${d.name}</option>`).join('');

        // Populate officers dropdown
        this.updateOfficerDropdown();

        deptSelect.onchange = () => this.updateOfficerDropdown(deptSelect.value);

        App.openModal('admin-assign-modal');
    },

    updateOfficerDropdown(selectedDeptId = null) {
        const officerSelect = document.getElementById('assign-officer-select');
        let officers = this.officersCache;
        if (selectedDeptId) {
            officers = officers.filter(o => !o.departmentId || o.departmentId == selectedDeptId);
        }

        officerSelect.innerHTML = '<option value="">Assign to any available officer</option>' +
            officers.map(o => `<option value="${o.id}">${o.name} (${o.departmentName || 'General'})</option>`).join('');
    },

    async submitAssign(event) {
        event.preventDefault();
        const deptId = document.getElementById('assign-dept-select').value;
        const officerId = document.getElementById('assign-officer-select').value;
        const note = document.getElementById('assign-note').value;

        if (!deptId) {
            App.showToast('Please select a department', 'warning');
            return;
        }

        try {
            const res = await fetch(`/api/admin/complaints/${this.currentActionComplaintId}/assign`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${Auth.getToken()}`
                },
                body: JSON.stringify({
                    departmentId: parseInt(deptId),
                    officerId: officerId ? parseInt(officerId) : null,
                    note
                })
            });

            if (!res.ok) throw new Error('Failed to assign complaint');
            App.showToast('Department & Officer assigned successfully!', 'success');
            App.closeModal('admin-assign-modal');
            this.loadAdminDashboard();
        } catch (err) {
            App.showToast(err.message, 'error');
        }
    },

    openStatusModal(id, code, currentStatus) {
        this.currentActionComplaintId = id;
        document.getElementById('status-complaint-code').textContent = code;
        const statusSelect = document.getElementById('admin-status-select');
        statusSelect.value = currentStatus;

        this.toggleRejectionField(currentStatus === 'REJECTED');
        statusSelect.onchange = (e) => this.toggleRejectionField(e.target.value === 'REJECTED');

        App.openModal('admin-status-modal');
    },

    toggleRejectionField(show) {
        const field = document.getElementById('rejection-reason-container');
        if (field) {
            show ? field.classList.remove('hidden') : field.classList.add('hidden');
        }
    },

    async submitStatusUpdate(event) {
        event.preventDefault();
        const status = document.getElementById('admin-status-select').value;
        const note = document.getElementById('admin-status-note').value;
        const rejectionReason = document.getElementById('admin-rejection-reason')?.value;

        if (status === 'REJECTED' && (!rejectionReason || !rejectionReason.trim())) {
            App.showToast('Please provide a reason for rejecting the complaint.', 'warning');
            return;
        }

        try {
            const res = await fetch(`/api/admin/complaints/${this.currentActionComplaintId}/status`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${Auth.getToken()}`
                },
                body: JSON.stringify({ status, note, rejectionReason })
            });

            if (!res.ok) throw new Error('Failed to update status');
            App.showToast('Complaint status updated!', 'success');
            App.closeModal('admin-status-modal');
            this.loadAdminDashboard();
        } catch (err) {
            App.showToast(err.message, 'error');
        }
    }
};
