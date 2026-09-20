// Complaints Module: Reporting, Duplicate Detection, Public Explorer, and Tracking
const Complaints = {
    selectedFiles: [],
    duplicateDebounceTimer: null,
    currentTrackedComplaint: null,

    // Category list with icons for UI
    categories: [
        { id: 'Potholes', name: 'Potholes', icon: 'fa-road', color: 'orange' },
        { id: 'Waterlogging', name: 'Waterlogging', icon: 'fa-cloud-showers-heavy', color: 'blue' },
        { id: 'Garbage', name: 'Garbage', icon: 'fa-trash-can', color: 'green' },
        { id: 'Streetlights', name: 'Streetlights', icon: 'fa-lightbulb', color: 'yellow' },
        { id: 'Damaged Roads', name: 'Damaged Roads', icon: 'fa-triangle-exclamation', color: 'amber' },
        { id: 'Water Supply', name: 'Water Supply', icon: 'fa-faucet-drip', color: 'sky' },
        { id: 'Drainage', name: 'Drainage', icon: 'fa-water', color: 'purple' },
        { id: 'Fallen Trees', name: 'Fallen Trees', icon: 'fa-tree', color: 'emerald' },
        { id: 'Traffic Signals', name: 'Traffic Signals', icon: 'fa-traffic-light', color: 'red' },
        { id: 'Other', name: 'Other Infrastructure', icon: 'fa-location-dot', color: 'slate' }
    ],

    initReportForm() {
        this.selectedFiles = [];
        this.renderImagePreviews();

        const fileInput = document.getElementById('report-file-input');
        if (fileInput) {
            fileInput.value = '';
            fileInput.onchange = (e) => this.handleFileSelection(e.target.files);
        }

        // Category selection cards click handler
        const categoryCards = document.querySelectorAll('.category-select-card');
        categoryCards.forEach(card => {
            card.onclick = () => {
                categoryCards.forEach(c => c.classList.remove('ring-2', 'ring-blue-600', 'bg-blue-50'));
                card.classList.add('ring-2', 'ring-blue-600', 'bg-blue-50');
                const catInput = document.getElementById('report-category');
                if (catInput) {
                    catInput.value = card.dataset.category;
                    this.triggerDuplicateCheck();
                }
            };
        });

        // Initialize Map
        setTimeout(() => {
            CivicMap.initReportMap();
        }, 200);
    },

    handleFileSelection(files) {
        if (!files) return;
        const validExtensions = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp'];

        for (let i = 0; i < files.length; i++) {
            const file = files[i];
            if (!validExtensions.includes(file.type)) {
                App.showToast(`Invalid file "${file.name}". Only JPG and PNG are supported.`, 'warning');
                continue;
            }
            if (file.size > 15 * 1024 * 1024) {
                App.showToast(`File "${file.name}" exceeds 15MB limit.`, 'warning');
                continue;
            }
            if (this.selectedFiles.length >= 5) {
                App.showToast('You can upload a maximum of 5 images.', 'warning');
                break;
            }
            this.selectedFiles.push(file);
        }
        this.renderImagePreviews();
    },

    removeFile(index) {
        this.selectedFiles.splice(index, 1);
        this.renderImagePreviews();
    },

    renderImagePreviews() {
        const container = document.getElementById('image-previews-container');
        if (!container) return;

        container.innerHTML = '';
        if (this.selectedFiles.length === 0) {
            container.classList.add('hidden');
            return;
        }
        container.classList.remove('hidden');

        this.selectedFiles.forEach((file, idx) => {
            const reader = new FileReader();
            const previewCard = document.createElement('div');
            previewCard.className = 'relative group w-20 h-20 rounded-lg overflow-hidden border border-slate-200 shadow-sm';

            reader.onload = (e) => {
                previewCard.innerHTML = `
                    <img src="${e.target.result}" class="w-full h-full object-cover">
                    <button type="button" onclick="Complaints.removeFile(${idx})" class="absolute top-1 right-1 bg-red-600 text-white w-5 h-5 rounded-full flex items-center justify-center text-xs opacity-90 hover:opacity-100 transition shadow">
                        <i class="fa-solid fa-xmark"></i>
                    </button>
                    <div class="absolute bottom-0 inset-x-0 bg-black/60 text-white text-[9px] px-1 truncate">${file.name}</div>
                `;
            };
            reader.readAsDataURL(file);
            container.appendChild(previewCard);
        });
    },

    // Duplicate Issue Detection
    triggerDuplicateCheck() {
        clearTimeout(this.duplicateDebounceTimer);
        this.duplicateDebounceTimer = setTimeout(() => this.checkDuplicateIssues(), 400);
    },

    async checkDuplicateIssues() {
        const category = document.getElementById('report-category')?.value;
        const lat = parseFloat(document.getElementById('report-latitude')?.value);
        const lng = parseFloat(document.getElementById('report-longitude')?.value);
        const alertBox = document.getElementById('duplicate-alert-banner');

        if (!category || isNaN(lat) || isNaN(lng) || !alertBox) return;

        try {
            const res = await fetch('/api/complaints/check-duplicate', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ category, latitude: lat, longitude: lng, radiusMeters: 120 })
            });

            if (!res.ok) return;
            const duplicates = await res.json();

            if (duplicates && duplicates.length > 0) {
                const nearest = duplicates[0];
                alertBox.classList.remove('hidden');
                alertBox.innerHTML = `
                    <div class="flex items-start gap-3 p-4 bg-amber-50 border border-amber-300 rounded-xl shadow-sm">
                        <i class="fa-solid fa-triangle-exclamation text-amber-600 text-xl mt-0.5"></i>
                        <div class="flex-1">
                            <h4 class="font-bold text-amber-900 text-sm">Similar Issue Detected Nearby!</h4>
                            <p class="text-xs text-amber-800 mt-1">
                                An active <b>${nearest.category}</b> report (<b>${nearest.complaintId}</b>: "${nearest.title}") is located only <b>${nearest.distanceMeters}m away</b>.
                            </p>
                            <div class="flex items-center gap-3 mt-3">
                                <button type="button" onclick="Complaints.showDetailsModal('${nearest.complaintId}')" class="text-xs bg-amber-600 hover:bg-amber-700 text-white font-semibold px-3 py-1.5 rounded-lg shadow-sm transition">
                                    <i class="fa-solid fa-eye mr-1"></i> View Existing Issue (${nearest.upvoteCount} Upvotes)
                                </button>
                                <span class="text-xs text-amber-700">or continue reporting if it's a different problem</span>
                            </div>
                        </div>
                    </div>
                `;
            } else {
                alertBox.classList.add('hidden');
                alertBox.innerHTML = '';
            }
        } catch (e) {
            // Ignore background check failure
        }
    },

    // Submit Report Form
    async submitReport(event) {
        event.preventDefault();

        if (!Auth.isAuthenticated()) {
            App.showToast('Please log in or register to submit a complaint.', 'info');
            App.openAuthModal('login');
            return;
        }

        const submitBtn = document.getElementById('report-submit-btn');
        const originalBtnText = submitBtn.innerHTML;

        try {
            const form = event.target;
            const title = form.title.value.trim();
            const category = form.category.value;
            const priority = form.priority.value;
            const description = form.description.value.trim();
            const latitude = form.latitude.value;
            const longitude = form.longitude.value;
            const address = form.address.value.trim();

            if (!category) {
                App.showToast('Please select an issue category.', 'warning');
                return;
            }
            if (!address) {
                App.showToast('Please specify the location/address.', 'warning');
                return;
            }

            submitBtn.disabled = true;
            submitBtn.innerHTML = '<i class="fa-solid fa-spinner fa-spin mr-2"></i> Submitting Complaint...';

            const formData = new FormData();
            formData.append('title', title);
            formData.append('category', category);
            formData.append('priority', priority);
            formData.append('description', description);
            formData.append('latitude', latitude);
            formData.append('longitude', longitude);
            formData.append('address', address);

            this.selectedFiles.forEach((file) => {
                formData.append('files', file);
            });

            const res = await fetch('/api/complaints', {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${Auth.getToken()}`
                },
                body: formData
            });

            const data = await res.json();
            if (!res.ok) {
                throw new Error(data.message || 'Failed to submit complaint');
            }

            form.reset();
            this.selectedFiles = [];
            this.renderImagePreviews();

            // Show submission success modal
            this.showSuccessModal(data);

        } catch (err) {
            App.showToast(err.message, 'error');
        } finally {
            submitBtn.disabled = false;
            submitBtn.innerHTML = originalBtnText;
        }
    },

    showSuccessModal(complaint) {
        const modal = document.getElementById('report-success-modal');
        if (!modal) return;

        document.getElementById('success-complaint-id').textContent = complaint.complaintId;
        document.getElementById('success-complaint-title').textContent = complaint.title;
        document.getElementById('success-complaint-category').textContent = complaint.category;
        document.getElementById('success-complaint-location').textContent = complaint.address;
        document.getElementById('success-complaint-status').textContent = App.formatStatus(complaint.status);

        const trackBtn = document.getElementById('success-track-btn');
        if (trackBtn) {
            trackBtn.onclick = () => {
                App.closeModal('report-success-modal');
                App.showView('track');
                document.getElementById('track-search-input').value = complaint.complaintId;
                this.trackComplaint(complaint.complaintId);
            };
        }

        App.openModal('report-success-modal');
    },

    // Tracking & Details
    async trackComplaint(code) {
        if (!code || !code.trim()) {
            App.showToast('Please enter a Complaint ID (e.g. CIV-2026-00001)', 'warning');
            return;
        }

        const cleanCode = code.trim().toUpperCase();
        const container = document.getElementById('track-result-container');
        const emptyState = document.getElementById('track-empty-state');
        const loadingState = document.getElementById('track-loading-state');

        if (emptyState) emptyState.classList.add('hidden');
        if (loadingState) loadingState.classList.remove('hidden');
        if (container) container.classList.add('hidden');

        try {
            const headers = {};
            if (Auth.isAuthenticated()) {
                headers['Authorization'] = `Bearer ${Auth.getToken()}`;
            }

            const res = await fetch(`/api/complaints/by-code/${cleanCode}`, { headers });
            const data = await res.json();

            if (!res.ok) {
                throw new Error(data.message || `No complaint found with ID: ${cleanCode}`);
            }

            this.currentTrackedComplaint = data;
            this.renderTrackingView(data);

            if (loadingState) loadingState.classList.add('hidden');
            if (container) container.classList.remove('hidden');

        } catch (err) {
            if (loadingState) loadingState.classList.add('hidden');
            if (emptyState) emptyState.classList.remove('hidden');
            App.showToast(err.message, 'error');
        }
    },

    renderTrackingView(c) {
        document.getElementById('track-id-display').textContent = c.complaintId;
        document.getElementById('track-title-display').textContent = c.title;
        document.getElementById('track-desc-display').textContent = c.description;
        document.getElementById('track-address-display').textContent = c.address;
        document.getElementById('track-date-display').textContent = App.formatDate(c.createdAt);
        document.getElementById('track-category-display').textContent = c.category;
        document.getElementById('track-dept-display').textContent = c.departmentName || 'Department Allocation Pending';
        document.getElementById('track-officer-display').textContent = c.assignedOfficerName || 'Officer to be assigned';

        // Priority and Status Badges
        const statusBadge = document.getElementById('track-status-badge');
        statusBadge.className = `px-3 py-1 rounded-full text-xs font-bold uppercase tracking-wider ${App.getStatusBadgeClass(c.status)}`;
        statusBadge.textContent = App.formatStatus(c.status);

        const priorityBadge = document.getElementById('track-priority-badge');
        priorityBadge.className = `px-2.5 py-0.5 rounded-full text-xs font-bold ${App.getPriorityBadgeClass(c.priority)}`;
        priorityBadge.textContent = c.priority;

        // Render 7-Stage Horizontal Visual Timeline
        this.renderTimelineTrack(c.status, c.updates);

        // Render Upvote Button
        const upvoteBtn = document.getElementById('track-upvote-btn');
        const upvoteCount = document.getElementById('track-upvote-count');
        if (upvoteBtn && upvoteCount) {
            upvoteCount.textContent = c.upvoteCount;
            if (c.hasUpvoted) {
                upvoteBtn.className = 'flex items-center gap-2 px-3 py-1.5 bg-blue-600 text-white rounded-lg font-semibold text-xs transition shadow-sm';
            } else {
                upvoteBtn.className = 'flex items-center gap-2 px-3 py-1.5 bg-slate-100 hover:bg-blue-50 text-slate-700 hover:text-blue-600 rounded-lg font-semibold text-xs border border-slate-200 transition';
            }
            upvoteBtn.onclick = () => this.toggleUpvote(c.id, 'track');
        }

        // Render Photos Gallery
        const gallery = document.getElementById('track-images-gallery');
        if (gallery) {
            gallery.innerHTML = '';
            if (c.images && c.images.length > 0) {
                c.images.forEach(img => {
                    const card = document.createElement('a');
                    card.href = img.filePath;
                    card.target = '_blank';
                    card.className = 'block rounded-lg overflow-hidden border border-slate-200 shadow-sm hover:opacity-90 transition';
                    card.innerHTML = `<img src="${img.filePath}" class="w-full h-32 object-cover" alt="Evidence">`;
                    gallery.appendChild(card);
                });
            } else {
                gallery.innerHTML = '<p class="text-xs text-slate-400 italic">No photographic evidence attached.</p>';
            }
        }

        // Render Timeline Updates List
        const updatesList = document.getElementById('track-updates-list');
        if (updatesList) {
            updatesList.innerHTML = '';
            if (c.updates && c.updates.length > 0) {
                c.updates.forEach(u => {
                    const item = document.createElement('div');
                    item.className = 'flex items-start gap-3 p-3 rounded-xl bg-slate-50 border border-slate-200/70 text-xs';
                    item.innerHTML = `
                        <div class="w-8 h-8 rounded-full bg-blue-100 text-blue-700 flex items-center justify-center font-bold shrink-0 mt-0.5">
                            <i class="fa-solid fa-check"></i>
                        </div>
                        <div class="flex-1">
                            <div class="flex items-center justify-between gap-2">
                                <span class="font-bold text-slate-800">${u.updatedByName}</span>
                                <span class="text-[11px] text-slate-400">${App.formatDate(u.createdAt)}</span>
                            </div>
                            <div class="mt-0.5 font-semibold text-blue-600">${App.formatStatus(u.status)}</div>
                            <p class="text-slate-600 mt-1">${u.message}</p>
                            ${u.proofImagePath ? `<a href="${u.proofImagePath}" target="_blank" class="inline-block mt-2 text-[11px] text-blue-600 font-semibold underline"><i class="fa-solid fa-image mr-1"></i>View Resolution Proof Photo</a>` : ''}
                        </div>
                    `;
                    updatesList.appendChild(item);
                });
            }
        }

        // Render Mini Map
        setTimeout(() => {
            CivicMap.renderDetailMap('track-map', c.latitude, c.longitude, c.title);
        }, 200);
    },

    // Visual Timeline Renderer
    renderTimelineTrack(currentStatus, updates = []) {
        const container = document.getElementById('visual-timeline-container');
        if (!container) return;

        const stages = [
            { key: 'SUBMITTED', label: 'Submitted' },
            { key: 'UNDER_REVIEW', label: 'Under Review' },
            { key: 'VERIFIED', label: 'Verified' },
            { key: 'ASSIGNED', label: 'Assigned' },
            { key: 'IN_PROGRESS', label: 'In Progress' },
            { key: 'RESOLVED', label: 'Resolved' },
            { key: 'CLOSED', label: 'Closed' }
        ];

        const isRejected = currentStatus === 'REJECTED';
        const currentIndex = stages.findIndex(s => s.key === currentStatus);

        let html = '<div class="flex items-center justify-between w-full relative py-4 overflow-x-auto">';

        stages.forEach((stage, idx) => {
            let nodeClass = 'node-pending';
            let stepClass = '';
            let icon = idx + 1;

            if (isRejected) {
                if (idx === 0) {
                    nodeClass = 'node-completed';
                    icon = '<i class="fa-solid fa-check text-xs"></i>';
                } else if (idx === 1) {
                    nodeClass = 'node-rejected';
                    icon = '<i class="fa-solid fa-xmark text-xs"></i>';
                }
            } else {
                if (idx < currentIndex) {
                    nodeClass = 'node-completed';
                    stepClass = 'completed';
                    icon = '<i class="fa-solid fa-check text-xs"></i>';
                } else if (idx === currentIndex) {
                    nodeClass = 'node-active';
                    stepClass = 'active';
                    icon = '<i class="fa-solid fa-circle-dot text-xs"></i>';
                }
            }

            html += `
                <div class="timeline-step-horizontal ${stepClass}">
                    <div class="timeline-node ${nodeClass} mb-2">${icon}</div>
                    <span class="text-[11px] font-semibold text-center text-slate-700 whitespace-nowrap">${stage.label}</span>
                </div>
            `;
        });

        html += '</div>';

        if (isRejected) {
            html += `
                <div class="mt-3 p-3 bg-red-50 border border-red-200 rounded-xl flex items-center gap-2 text-xs text-red-700 font-semibold">
                    <i class="fa-solid fa-circle-xmark text-red-600"></i>
                    This complaint has been rejected by municipal authorities.
                </div>
            `;
        }

        container.innerHTML = html;
    },

    // Public Complaints Explorer
    async loadPublicComplaints() {
        const category = document.getElementById('public-filter-category')?.value || '';
        const status = document.getElementById('public-filter-status')?.value || '';
        const priority = document.getElementById('public-filter-priority')?.value || '';
        const keyword = document.getElementById('public-filter-search')?.value || '';

        const params = new URLSearchParams();
        if (category) params.append('category', category);
        if (status) params.append('status', status);
        if (priority) params.append('priority', priority);
        if (keyword) params.append('keyword', keyword);
        params.append('size', '50');

        try {
            const headers = {};
            if (Auth.isAuthenticated()) {
                headers['Authorization'] = `Bearer ${Auth.getToken()}`;
            }

            const res = await fetch(`/api/complaints/public?${params.toString()}`, { headers });
            const data = await res.json();
            const complaints = data.content || [];

            // Update Map Pins
            CivicMap.initPublicMap(complaints);

            // Update Grid Cards
            this.renderPublicCards(complaints);

        } catch (err) {
            App.showToast('Failed to load public complaints', 'error');
        }
    },

    renderPublicCards(complaints) {
        const container = document.getElementById('public-cards-container');
        if (!container) return;

        if (complaints.length === 0) {
            container.innerHTML = `
                <div class="col-span-full text-center py-12 bg-white rounded-2xl border border-slate-200">
                    <i class="fa-solid fa-clipboard-check text-4xl text-slate-300 mb-3"></i>
                    <p class="text-slate-500 font-medium">No complaints match your selected filters.</p>
                </div>
            `;
            return;
        }

        container.innerHTML = complaints.map(c => `
            <div class="bg-white rounded-xl border border-slate-200 p-5 card-hover shadow-sm flex flex-col justify-between">
                <div>
                    <div class="flex items-center justify-between gap-2 mb-3">
                        <span class="text-xs font-mono font-bold text-blue-600">${c.complaintId}</span>
                        <span class="text-xs px-2.5 py-0.5 rounded-full font-semibold ${App.getStatusBadgeClass(c.status)}">${App.formatStatus(c.status)}</span>
                    </div>
                    <div class="flex items-center gap-2 text-xs font-semibold text-slate-500 mb-1">
                        <span><i class="fa-solid ${CivicMap.getCategoryIcon(c.category).icon} text-blue-500 mr-1"></i>${c.category}</span>
                        <span>•</span>
                        <span class="${App.getPriorityBadgeClass(c.priority)} px-2 py-0.5 rounded font-bold">${c.priority}</span>
                    </div>
                    <h3 class="font-bold text-slate-900 text-base mb-2 line-clamp-2">${c.title}</h3>
                    <p class="text-xs text-slate-600 mb-4 line-clamp-2">${c.description}</p>
                    <p class="text-xs text-slate-500 flex items-center gap-1 mb-4">
                        <i class="fa-solid fa-location-dot text-red-500 shrink-0"></i>
                        <span class="truncate">${c.address}</span>
                    </p>
                </div>
                <div class="flex items-center justify-between border-t border-slate-100 pt-3 mt-2">
                    <button onclick="Complaints.toggleUpvote(${c.id}, 'card')" class="flex items-center gap-1.5 text-xs font-semibold px-2.5 py-1 rounded-lg transition ${c.hasUpvoted ? 'bg-blue-600 text-white' : 'bg-slate-100 hover:bg-blue-50 text-slate-700 hover:text-blue-600'}">
                        <i class="fa-solid fa-thumbs-up"></i>
                        <span>${c.upvoteCount}</span>
                    </button>
                    <button onclick="Complaints.showDetailsModal('${c.complaintId}')" class="text-xs bg-slate-900 hover:bg-blue-600 text-white font-semibold px-3 py-1.5 rounded-lg transition">
                        View Details
                    </button>
                </div>
            </div>
        `).join('');
    },

    async toggleUpvote(complaintId, source = 'card') {
        if (!Auth.isAuthenticated()) {
            App.showToast('Please log in to upvote civic issues.', 'info');
            App.openAuthModal('login');
            return;
        }

        try {
            const res = await fetch(`/api/complaints/${complaintId}/upvote`, {
                method: 'POST',
                headers: { 'Authorization': `Bearer ${Auth.getToken()}` }
            });
            const data = await res.json();
            if (!res.ok) throw new Error(data.message || 'Failed to update upvote');

            App.showToast(data.hasUpvoted ? 'Upvoted! Thank you for raising urgency.' : 'Upvote removed.', 'success');

            if (source === 'track' && this.currentTrackedComplaint) {
                this.trackComplaint(this.currentTrackedComplaint.complaintId);
            } else {
                this.loadPublicComplaints();
            }
        } catch (err) {
            App.showToast(err.message, 'error');
        }
    },

    // Citizen Dashboard
    async loadCitizenDashboard() {
        if (!Auth.isAuthenticated()) return;

        try {
            const res = await fetch('/api/complaints/my', {
                headers: { 'Authorization': `Bearer ${Auth.getToken()}` }
            });
            const complaints = await res.json();

            // Summary metrics
            const total = complaints.length;
            const pending = complaints.filter(c => c.status === 'SUBMITTED' || c.status === 'UNDER_REVIEW').length;
            const inProgress = complaints.filter(c => c.status === 'ASSIGNED' || c.status === 'IN_PROGRESS' || c.status === 'VERIFIED').length;
            const resolved = complaints.filter(c => c.status === 'RESOLVED' || c.status === 'CLOSED').length;

            document.getElementById('citizen-stat-total').textContent = total;
            document.getElementById('citizen-stat-pending').textContent = pending;
            document.getElementById('citizen-stat-progress').textContent = inProgress;
            document.getElementById('citizen-stat-resolved').textContent = resolved;

            // Render Table
            const tbody = document.getElementById('citizen-complaints-tbody');
            if (!tbody) return;

            if (complaints.length === 0) {
                tbody.innerHTML = `
                    <tr>
                        <td colspan="7" class="text-center py-8 text-slate-400 italic">
                            You have not reported any civic issues yet. Click "Report Issue" to get started!
                        </td>
                    </tr>
                `;
                return;
            }

            tbody.innerHTML = complaints.map(c => `
                <tr class="hover:bg-slate-50 transition border-b border-slate-100">
                    <td class="px-4 py-3 font-mono font-bold text-blue-600 text-xs">${c.complaintId}</td>
                    <td class="px-4 py-3">
                        <div class="font-bold text-slate-800 text-xs">${c.title}</div>
                        <div class="text-[11px] text-slate-500">${c.category}</div>
                    </td>
                    <td class="px-4 py-3 text-xs text-slate-600 max-w-xs truncate">${c.address}</td>
                    <td class="px-4 py-3 text-xs text-slate-500">${App.formatDate(c.createdAt)}</td>
                    <td class="px-4 py-3">
                        <span class="text-xs px-2 py-0.5 rounded font-bold ${App.getPriorityBadgeClass(c.priority)}">${c.priority}</span>
                    </td>
                    <td class="px-4 py-3">
                        <span class="text-xs px-2.5 py-0.5 rounded-full font-semibold ${App.getStatusBadgeClass(c.status)}">${App.formatStatus(c.status)}</span>
                    </td>
                    <td class="px-4 py-3 text-right">
                        <button onclick="Complaints.showDetailsModal('${c.complaintId}')" class="text-xs bg-slate-100 hover:bg-blue-50 text-slate-700 hover:text-blue-600 font-semibold px-2.5 py-1 rounded transition border border-slate-200">
                            Details
                        </button>
                    </td>
                </tr>
            `).join('');

        } catch (err) {
            App.showToast('Failed to load your complaints', 'error');
        }
    },

    // Modal Details Popup
    async showDetailsModal(complaintId) {
        App.showView('track');
        document.getElementById('track-search-input').value = complaintId;
        await this.trackComplaint(complaintId);
    }
};
