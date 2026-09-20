// Field Officer Module: Task Execution, Resolution Updates & Proof Upload
const Officer = {
    currentActionComplaintId: null,

    async loadOfficerDashboard() {
        if (!Auth.hasRole('ROLE_OFFICER') && !Auth.hasRole('ROLE_ADMIN')) {
            App.showToast('Access restricted to municipal field officers.', 'error');
            App.showView('landing');
            return;
        }

        try {
            const res = await fetch('/api/officer/complaints', {
                headers: { 'Authorization': `Bearer ${Auth.getToken()}` }
            });
            const complaints = await res.json();

            // Summary metrics
            const total = complaints.length;
            const assigned = complaints.filter(c => c.status === 'ASSIGNED').length;
            const inProgress = complaints.filter(c => c.status === 'IN_PROGRESS').length;
            const resolved = complaints.filter(c => c.status === 'RESOLVED' || c.status === 'CLOSED').length;

            document.getElementById('officer-stat-total').textContent = total;
            document.getElementById('officer-stat-assigned').textContent = assigned;
            document.getElementById('officer-stat-progress').textContent = inProgress;
            document.getElementById('officer-stat-resolved').textContent = resolved;

            const tbody = document.getElementById('officer-complaints-tbody');
            if (!tbody) return;

            if (complaints.length === 0) {
                tbody.innerHTML = `
                    <tr>
                        <td colspan="7" class="text-center py-10 text-slate-400 italic">
                            No complaints currently assigned to you. Enjoy your clean desk!
                        </td>
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
                        <span class="px-2 py-0.5 rounded font-bold ${App.getPriorityBadgeClass(c.priority)}">${c.priority}</span>
                    </td>
                    <td class="px-4 py-3">
                        <span class="px-2.5 py-0.5 rounded-full font-semibold ${App.getStatusBadgeClass(c.status)}">${App.formatStatus(c.status)}</span>
                    </td>
                    <td class="px-4 py-3 text-slate-500">${App.formatDate(c.createdAt)}</td>
                    <td class="px-4 py-3">
                        ${c.status === 'RESOLVED' || c.status === 'CLOSED'
                            ? '<span class="text-emerald-700 font-semibold flex items-center gap-1"><i class="fa-solid fa-circle-check"></i> Completed</span>'
                            : '<span class="text-amber-600 font-semibold flex items-center gap-1"><i class="fa-solid fa-clock"></i> Action Needed</span>'}
                    </td>
                    <td class="px-4 py-3 text-right whitespace-nowrap">
                        <div class="flex items-center justify-end gap-2">
                            <button onclick="Officer.openUpdateModal(${c.id}, '${c.complaintId}', '${c.status}')" class="px-3 py-1.5 bg-blue-600 hover:bg-blue-700 text-white rounded-lg font-semibold text-xs transition shadow-sm">
                                <i class="fa-solid fa-pen-to-square mr-1"></i> Update Work
                            </button>
                            <button onclick="Complaints.showDetailsModal('${c.complaintId}')" class="px-2.5 py-1.5 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-lg font-medium text-xs transition">
                                <i class="fa-solid fa-eye"></i>
                            </button>
                        </div>
                    </td>
                </tr>
            `).join('');

        } catch (err) {
            App.showToast('Failed to load assigned tasks', 'error');
        }
    },

    openUpdateModal(id, code, currentStatus) {
        this.currentActionComplaintId = id;
        document.getElementById('officer-update-code').textContent = code;

        const statusSelect = document.getElementById('officer-status-select');
        statusSelect.value = (currentStatus === 'ASSIGNED') ? 'IN_PROGRESS' : (currentStatus === 'IN_PROGRESS' ? 'RESOLVED' : currentStatus);

        document.getElementById('officer-update-form').reset();
        App.openModal('officer-update-modal');
    },

    async submitUpdate(event) {
        event.preventDefault();
        const submitBtn = document.getElementById('officer-submit-btn');
        const originalText = submitBtn.innerHTML;

        try {
            const status = document.getElementById('officer-status-select').value;
            const message = document.getElementById('officer-update-message').value.trim();
            const proofFile = document.getElementById('officer-proof-file').files[0];

            if (!message) {
                App.showToast('Please enter progress or resolution notes.', 'warning');
                return;
            }

            submitBtn.disabled = true;
            submitBtn.innerHTML = '<i class="fa-solid fa-spinner fa-spin mr-2"></i> Saving Update...';

            const formData = new FormData();
            formData.append('status', status);
            formData.append('message', message);
            if (proofFile) {
                formData.append('proofImage', proofFile);
            }

            const res = await fetch(`/api/officer/complaints/${this.currentActionComplaintId}/updates`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${Auth.getToken()}`
                },
                body: formData
            });

            if (!res.ok) {
                const err = await res.json();
                throw new Error(err.message || 'Failed to submit update');
            }

            App.showToast('Field progress updated successfully!', 'success');
            App.closeModal('officer-update-modal');
            this.loadOfficerDashboard();

        } catch (err) {
            App.showToast(err.message, 'error');
        } finally {
            submitBtn.disabled = false;
            submitBtn.innerHTML = originalText;
        }
    }
};
