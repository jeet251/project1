// Leaflet Map Manager for CivicFix
const CivicMap = {
    reportMap: null,
    reportMarker: null,
    publicMap: null,
    publicMarkersLayer: null,
    detailMap: null,
    detailMarker: null,

    // Default center: Bangalore downtown (12.9716, 77.5946)
    defaultCoords: [12.9716, 77.5946],

    getCategoryIcon(category) {
        const cat = (category || '').toLowerCase();
        if (cat.includes('pothole') || cat.includes('road')) return { icon: 'fa-road', color: '#ea580c' };
        if (cat.includes('waterlog') || cat.includes('rain')) return { icon: 'fa-cloud-showers-heavy', color: '#0284c7' };
        if (cat.includes('garbage') || cat.includes('waste')) return { icon: 'fa-trash-can', color: '#16a34a' };
        if (cat.includes('light') || cat.includes('electric')) return { icon: 'fa-lightbulb', color: '#eab308' };
        if (cat.includes('water supply') || cat.includes('tap')) return { icon: 'fa-faucet-drip', color: '#2563eb' };
        if (cat.includes('drain')) return { icon: 'fa-water', color: '#7c3aed' };
        if (cat.includes('tree')) return { icon: 'fa-tree', color: '#15803d' };
        if (cat.includes('traffic') || cat.includes('signal')) return { icon: 'fa-traffic-light', color: '#dc2626' };
        return { icon: 'fa-location-dot', color: '#4b5563' };
    },

    createCustomMarker(category) {
        const info = this.getCategoryIcon(category);
        return L.divIcon({
            className: 'custom-leaflet-pin',
            html: `<div class="custom-pin" style="background-color: ${info.color};">
                     <i class="fa-solid ${info.icon}"></i>
                   </div>`,
            iconSize: [32, 32],
            iconAnchor: [16, 16],
            popupAnchor: [0, -18]
        });
    },

    // 1. Report Location Picker Map
    initReportMap() {
        const container = document.getElementById('report-map');
        if (!container) return;

        if (this.reportMap) {
            this.reportMap.remove();
            this.reportMap = null;
        }

        const latInput = document.getElementById('report-latitude');
        const lngInput = document.getElementById('report-longitude');
        const initLat = parseFloat(latInput.value) || this.defaultCoords[0];
        const initLng = parseFloat(lngInput.value) || this.defaultCoords[1];

        this.reportMap = L.map('report-map').setView([initLat, initLng], 14);

        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '© OpenStreetMap contributors'
        }).addTo(this.reportMap);

        this.reportMarker = L.marker([initLat, initLng], {
            draggable: true,
            icon: this.createCustomMarker('pothole')
        }).addTo(this.reportMap);

        // Update inputs on drag
        this.reportMarker.on('dragend', (e) => {
            const pos = e.target.getLatLng();
            this.setReportCoords(pos.lat, pos.lng);
            this.reverseGeocode(pos.lat, pos.lng);
            Complaints.triggerDuplicateCheck();
        });

        // Click anywhere to relocate marker
        this.reportMap.on('click', (e) => {
            this.reportMarker.setLatLng(e.latlng);
            this.setReportCoords(e.latlng.lat, e.latlng.lng);
            this.reverseGeocode(e.latlng.lat, e.latlng.lng);
            Complaints.triggerDuplicateCheck();
        });

        setTimeout(() => {
            this.reportMap.invalidateSize();
        }, 300);
    },

    setReportCoords(lat, lng) {
        const latInput = document.getElementById('report-latitude');
        const lngInput = document.getElementById('report-longitude');
        const display = document.getElementById('report-coords-display');

        if (latInput) latInput.value = lat.toFixed(6);
        if (lngInput) lngInput.value = lng.toFixed(6);
        if (display) display.textContent = `Lat: ${lat.toFixed(5)}, Lng: ${lng.toFixed(5)}`;
    },

    // HTML5 Geolocation "Use My Location"
    locateUser() {
        if (!navigator.geolocation) {
            App.showToast('Geolocation is not supported by your browser.', 'warning');
            return;
        }

        App.showToast('Detecting your location...', 'info');
        navigator.geolocation.getCurrentPosition(
            (pos) => {
                const lat = pos.coords.latitude;
                const lng = pos.coords.longitude;
                if (this.reportMap && this.reportMarker) {
                    this.reportMap.setView([lat, lng], 16);
                    this.reportMarker.setLatLng([lat, lng]);
                    this.setReportCoords(lat, lng);
                    this.reverseGeocode(lat, lng);
                    Complaints.triggerDuplicateCheck();
                    App.showToast('Location updated from GPS!', 'success');
                }
            },
            (err) => {
                App.showToast('Could not retrieve GPS location: ' + err.message, 'warning');
            },
            { enableHighAccuracy: true, timeout: 8000 }
        );
    },

    // Reverse geocode lat/lng to readable street address using OpenStreetMap Nominatim
    async reverseGeocode(lat, lng) {
        const addressInput = document.getElementById('report-address');
        if (!addressInput) return;

        try {
            const url = `https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lng}&zoom=18&addressdetails=1`;
            const res = await fetch(url, { headers: { 'Accept-Language': 'en' } });
            if (res.ok) {
                const data = await res.json();
                if (data && data.display_name) {
                    addressInput.value = data.display_name;
                }
            }
        } catch (e) {
            // Silently fall back if offline or rate limited
        }
    },

    // 2. Public Explorer Map
    initPublicMap(complaints = []) {
        const container = document.getElementById('public-map');
        if (!container) return;

        if (this.publicMap) {
            this.publicMap.remove();
            this.publicMap = null;
        }

        this.publicMap = L.map('public-map').setView(this.defaultCoords, 13);

        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '© OpenStreetMap contributors'
        }).addTo(this.publicMap);

        this.publicMarkersLayer = L.layerGroup().addTo(this.publicMap);
        this.renderPublicMarkers(complaints);

        setTimeout(() => {
            this.publicMap.invalidateSize();
        }, 300);
    },

    renderPublicMarkers(complaints) {
        if (!this.publicMap || !this.publicMarkersLayer) return;
        this.publicMarkersLayer.clearLayers();

        if (!complaints || complaints.length === 0) return;

        const bounds = [];

        complaints.forEach((c) => {
            if (c.latitude && c.longitude) {
                const marker = L.marker([c.latitude, c.longitude], {
                    icon: this.createCustomMarker(c.category)
                });

                const popupHtml = `
                    <div class="p-1 max-w-xs">
                        <div class="flex items-center justify-between gap-2 mb-1">
                            <span class="text-xs font-mono font-bold text-blue-600">${c.complaintId}</span>
                            <span class="text-[11px] px-2 py-0.5 rounded font-medium ${App.getStatusBadgeClass(c.status)}">${App.formatStatus(c.status)}</span>
                        </div>
                        <h4 class="font-bold text-slate-800 text-sm mb-1 line-clamp-2">${c.title}</h4>
                        <p class="text-xs text-slate-500 mb-2"><i class="fa-solid fa-location-dot text-red-500 mr-1"></i>${c.address}</p>
                        <div class="flex items-center justify-between border-t pt-2 mt-2">
                            <span class="text-xs text-slate-600 font-semibold"><i class="fa-solid fa-thumbs-up text-blue-500 mr-1"></i>${c.upvoteCount} Upvotes</span>
                            <button onclick="Complaints.showDetailsModal('${c.complaintId}')" class="text-xs bg-blue-600 hover:bg-blue-700 text-white font-medium px-2.5 py-1 rounded transition">
                                View Details
                            </button>
                        </div>
                    </div>
                `;

                marker.bindPopup(popupHtml);
                this.publicMarkersLayer.addLayer(marker);
                bounds.push([c.latitude, c.longitude]);
            }
        });

        if (bounds.length > 0) {
            this.publicMap.fitBounds(bounds, { padding: [40, 40], maxZoom: 15 });
        }
    },

    // 3. Single Complaint Details Map
    renderDetailMap(containerId, lat, lng, title) {
        const container = document.getElementById(containerId);
        if (!container) return;

        if (this.detailMap) {
            this.detailMap.remove();
            this.detailMap = null;
        }

        this.detailMap = L.map(containerId).setView([lat, lng], 15);

        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '© OpenStreetMap'
        }).addTo(this.detailMap);

        this.detailMarker = L.marker([lat, lng]).addTo(this.detailMap);
        if (title) {
            this.detailMarker.bindPopup(`<b>Location:</b><br>${title}`).openPopup();
        }

        setTimeout(() => {
            if (this.detailMap) this.detailMap.invalidateSize();
        }, 300);
    }
};
