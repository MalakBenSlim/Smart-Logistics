const SL = (() => {
  async function apiFetch(url, options = {}) {
    const cfg = {
      headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
      credentials: 'same-origin',
      ...options
    };
    const res = await fetch(url, cfg);
    let data = null;
    const ct = res.headers.get('content-type') || '';
    if (ct.includes('application/json')) {
      data = await res.json();
    } else {
      data = await res.text();
    }
    if (!res.ok) {
      if (res.status === 401 && !window.location.pathname.includes('/login')) {
        window.location.href = '/login';
      }
      const msg = (data && data.message) ? data.message : `Erreur HTTP ${res.status}`;
      const err = new Error(msg);
      err.status = res.status;
      err.data = data;
      throw err;
    }
    return data;
  }

  function showToast(title, message, type = 'info') {
    let container = document.getElementById('toast-container');
    if (!container) {
      container = document.createElement('div');
      container.id = 'toast-container';
      container.className = 'toast-container';
      document.body.appendChild(container);
    }
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.innerHTML = `<div style="font-weight:700;margin-bottom:2px;">${escapeHtml(title)}</div><div style="opacity:.9">${escapeHtml(message)}</div>`;
    container.appendChild(toast);
    setTimeout(() => toast.remove(), 4500);
  }

  function escapeHtml(str) {
    if (str == null) return '';
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }

  function formatMoney(v) {
    return `${Number(v || 0).toFixed(2)} DT`;
  }

  function statusBadge(status) {
    const map = {
      CREATED: ['Créée', 'badge-blue'],
      PREPARED: ['Préparée', 'badge-purple'],
      DRIVER_ASSIGNED: ['Livreur affecté', 'badge-amber'],
      SHIPPED: ['Expédiée', 'badge-blue'],
      IN_DELIVERY: ['En livraison', 'badge-amber'],
      DELIVERED: ['Livrée', 'badge-green'],
      CANCELLED: ['Annulée', 'badge-red'],
      DISPONIBLE: ['Disponible', 'badge-green'],
      EN_LIVRAISON: ['En livraison', 'badge-amber'],
      HORS_SERVICE: ['Hors service', 'badge-red'],
      ASSIGNEE: ['Assignée', 'badge-blue'],
      EN_ROUTE: ['En route', 'badge-amber'],
      LIVREE: ['Livrée', 'badge-green'],
      ACCEPTE: ['Accepté', 'badge-green'],
      EN_ATTENTE: ['En attente', 'badge-amber'],
      REFUSE: ['Refusé', 'badge-red'],
      ACTIF: ['Actif', 'badge-green'],
      MAINTENANCE: ['Maintenance', 'badge-amber'],
      SATURE: ['Saturé', 'badge-red']
    };
    const [label, cls] = map[status] || [status, 'badge-gray'];
    return `<span class="badge ${cls}">${escapeHtml(label)}</span>`;
  }

  function confirmDialog(title, message) {
    return new Promise((resolve) => {
      let modal = document.getElementById('sl-confirm-modal');
      if (!modal) {
        modal = document.createElement('div');
        modal.id = 'sl-confirm-modal';
        modal.className = 'modal-backdrop';
        modal.innerHTML = `
          <div class="modal-box" style="max-width:420px;">
            <h3 id="sl-confirm-title" style="margin-bottom:.5rem;"></h3>
            <p id="sl-confirm-msg" style="color:var(--text-muted);font-size:.92rem;margin-bottom:1.25rem;"></p>
            <div style="display:flex;justify-content:flex-end;gap:.6rem;">
              <button class="btn btn-outline" id="sl-confirm-no">Annuler</button>
              <button class="btn btn-primary" id="sl-confirm-yes">Confirmer</button>
            </div>
          </div>`;
        document.body.appendChild(modal);
      }
      modal.querySelector('#sl-confirm-title').textContent = title;
      modal.querySelector('#sl-confirm-msg').textContent = message;
      modal.classList.add('open');

      const cleanup = (res) => {
        modal.classList.remove('open');
        resolve(res);
      };
      modal.querySelector('#sl-confirm-no').onclick = () => cleanup(false);
      modal.querySelector('#sl-confirm-yes').onclick = () => cleanup(true);
    });
  }

  function renderSmartDecision(decision) {
    if (!decision) {
      return '<div class="alert alert-info">Décision Smart Optimizer en attente.</div>';
    }
    const reasons = (decision.warehouseReasons || [])
      .map(r => `<li>✓ ${escapeHtml(r)}</li>`)
      .join('');
    const candidates = (decision.warehouseCandidates || []).map(c => `
      <div class="candidate-row">
        <div><strong>${escapeHtml(c.warehouseName)}</strong></div>
        <div>
          ${c.eligible
            ? `<div class="progress-track"><div class="progress-fill" style="width:${Math.min(100, c.totalScore)}%"></div></div>
               <small style="color:var(--text-muted);">${c.distanceKm} km · charge ${c.loadPercent}% · ~${c.estimatedMinutes} min</small>`
            : `<span class="badge badge-red">${escapeHtml(c.ineligibilityReason)}</span>`}
        </div>
        <div><strong>${c.eligible ? c.totalScore + '/100' : 'Écarté'}</strong></div>
      </div>`).join('');

    let driverHtml = '';
    if (decision.selectedDriverName) {
      const drvReasons = (decision.driverReasons || []).map(r => `<li>🚚 ${escapeHtml(r)}</li>`).join('');
      driverHtml = `
        <div style="margin-top:1rem;padding-top:.85rem;border-top:1px solid #bfdbfe;">
          <div style="display:flex;justify-content:space-between;align-items:center;">
            <strong>Livreur sélectionné : ${escapeHtml(decision.selectedDriverName)}</strong>
            <span class="smart-score-badge" style="background:var(--success);">${decision.driverScore}/100</span>
          </div>
          <ul class="reason-list">${drvReasons}</ul>
        </div>`;
    }

    return `
      <div class="smart-panel">
        <div style="display:flex;justify-content:space-between;align-items:center;gap:.5rem;">
          <div>
            <div style="font-size:.78rem;font-weight:700;color:var(--primary);text-transform:uppercase;">🤖 Smart Route & Warehouse Optimizer</div>
            <div style="font-size:1.08rem;font-weight:700;margin-top:2px;">Dépôt sélectionné : ${escapeHtml(decision.selectedWarehouseName)}</div>
          </div>
          <span class="smart-score-badge">${decision.warehouseScore}/100</span>
        </div>
        <ul class="reason-list">${reasons}</ul>
        ${candidates ? `<details style="margin-top:.5rem;"><summary style="cursor:pointer;font-size:.84rem;font-weight:600;color:var(--primary);">Comparer les dépôts évalués (${decision.warehouseCandidates.length})</summary><div style="margin-top:.5rem;">${candidates}</div></details>` : ''}
        ${driverHtml}
      </div>`;
  }

  /**
   * Carte vectorielle interactive de la Tunisie (100% hors-ligne)
   * Projetant les coordonnées GPS (lat 32.5..37.5, lng 8.0..11.8)
   */
  function renderTunisiaMap(containerId, { warehouses = [], target = null, driver = null, origin = null } = {}) {
    const el = document.getElementById(containerId);
    if (!el) return;
    const W = 560, H = 340;
    const minLat = 32.6, maxLat = 37.4, minLng = 8.4, maxLng = 11.6;
    const projX = (lng) => ((lng - minLng) / (maxLng - minLng)) * (W - 60) + 30;
    const projY = (lat) => H - (((lat - minLat) / (maxLat - minLat)) * (H - 40) + 20);

    let routeSvg = '';
    if (origin && target) {
      const x1 = projX(origin.lng), y1 = projY(origin.lat);
      const x2 = projX(target.lng), y2 = projY(target.lat);
      routeSvg += `<line x1="${x1}" y1="${y1}" x2="${x2}" y2="${y2}" stroke="#2563eb" stroke-width="2.5" stroke-dasharray="6,5" />`;
    }
    if (driver && target) {
      const xd = projX(driver.lng), yd = projY(driver.lat);
      const xt = projX(target.lng), yt = projY(target.lat);
      routeSvg += `<line x1="${xd}" y1="${yd}" x2="${xt}" y2="${yt}" stroke="#10b981" stroke-width="3" />`;
    }

    const whSvg = warehouses.map(w => {
      const x = projX(w.longitude), y = projY(w.latitude);
      return `<g transform="translate(${x},${y})">
        <circle r="7" fill="#1e3a8a" stroke="#fff" stroke-width="2"></circle>
        <text x="10" y="4" font-size="11" font-weight="600" fill="#1e293b">${escapeHtml(w.name.replace('Dépôt ', ''))}</text>
      </g>`;
    }).join('');

    let targetSvg = '';
    if (target) {
      const x = projX(target.lng), y = projY(target.lat);
      targetSvg = `<g transform="translate(${x},${y})">
        <circle r="8" fill="#ef4444" stroke="#fff" stroke-width="2"></circle>
        <text x="10" y="4" font-size="11" font-weight="700" fill="#b91c1c">Client</text>
      </g>`;
    }

    let driverSvg = '';
    if (driver) {
      const x = projX(driver.lng), y = projY(driver.lat);
      driverSvg = `<g transform="translate(${x},${y})">
        <circle r="14" fill="rgba(16,185,129,0.25)"></circle>
        <circle r="7" fill="#10b981" stroke="#fff" stroke-width="2"></circle>
        <text x="11" y="-6" font-size="11" font-weight="700" fill="#047857">🚚 Livreur</text>
      </g>`;
    }

    el.innerHTML = `
      <svg viewBox="0 0 ${W} ${H}" style="width:100%;height:auto;display:block;background:linear-gradient(180deg,#eef6ff 0%,#f8fafc 100%);">
        <defs>
          <pattern id="grid" width="28" height="28" patternUnits="userSpaceOnUse">
            <path d="M 28 0 L 0 0 0 28" fill="none" stroke="#e2e8f0" stroke-width="0.8"/>
          </pattern>
        </defs>
        <rect width="${W}" height="${H}" fill="url(#grid)" />
        <path d="M235,28 L290,34 L310,60 L290,85 L315,125 L302,165 L322,205 L292,250 L315,285 L270,318 L210,305 L180,245 L155,190 L170,115 L195,55 Z"
              fill="#ffffff" stroke="#cbd5e1" stroke-width="2" />
        ${routeSvg}
        ${whSvg}
        ${targetSvg}
        ${driverSvg}
      </svg>`;
  }

  function setupTopbar(user, onNotificationEvent) {
    const nameEl = document.getElementById('user-name');
    const roleEl = document.getElementById('user-role');
    if (nameEl) nameEl.textContent = user.fullName;
    if (roleEl) roleEl.textContent = user.role;

    const logoutBtn = document.getElementById('logout-btn');
    if (logoutBtn) {
      logoutBtn.onclick = async () => {
        await apiFetch('/api/auth/logout', { method: 'POST' });
        window.location.href = '/login';
      };
    }

    const menuToggle = document.getElementById('menu-toggle');
    const sidebar = document.querySelector('.sidebar');
    if (menuToggle && sidebar) {
      menuToggle.onclick = () => sidebar.classList.toggle('open');
    }

    // Notifications bell
    const bell = document.getElementById('notif-bell');
    const dropdown = document.getElementById('notif-dropdown');
    if (bell && dropdown) {
      bell.onclick = (e) => {
        e.stopPropagation();
        dropdown.classList.toggle('open');
      };
      document.addEventListener('click', () => dropdown.classList.remove('open'));
      loadNotifications();
    }

    // SSE real-time stream
    const sse = new EventSource('/api/stream/events');
    const sseBadge = document.getElementById('sse-status');
    sse.addEventListener('connected', () => {
      if (sseBadge) sseBadge.innerHTML = '<span class="pulse-dot"></span> Temps réel JMS/SSE actif';
    });
    const eventTypes = [
      'ORDER_CREATED', 'WAREHOUSE_SELECTED', 'ORDER_PREPARED',
      'DRIVER_ASSIGNED', 'ORDER_SHIPPED', 'DELIVERY_STARTED',
      'POSITION_UPDATED', 'DELIVERY_COMPLETED', 'ORDER_CANCELLED', 'LOW_STOCK'
    ];
    eventTypes.forEach(t => {
      sse.addEventListener(t, (e) => {
        try {
          const payload = JSON.parse(e.data);
          if (t !== 'POSITION_UPDATED') {
            showToast(payload.title || t, payload.message || '', t === 'LOW_STOCK' ? 'warning' : 'success');
            loadNotifications();
          }
          if (onNotificationEvent) onNotificationEvent(t, payload);
        } catch (err) {
          console.error('SSE parse error', err);
        }
      });
    });
    sse.onerror = () => {
      if (sseBadge) sseBadge.innerHTML = 'Reconnexion SSE...';
    };
  }

  async function loadNotifications() {
    const listEl = document.getElementById('notif-list');
    const countEl = document.getElementById('notif-count');
    if (!listEl) return;
    try {
      const notifs = await apiFetch('/api/notifications');
      const unread = notifs.filter(n => !n.read).length;
      if (countEl) {
        countEl.textContent = unread;
        countEl.style.display = unread > 0 ? 'inline-flex' : 'none';
      }
      if (notifs.length === 0) {
        listEl.innerHTML = '<div style="padding:1rem;color:var(--text-muted);text-align:center;">Aucune notification</div>';
        return;
      }
      listEl.innerHTML = notifs.map(n => `
        <div class="notif-item ${n.read ? '' : 'unread'}" onclick="SL.markNotifRead(${n.id})">
          <div style="font-weight:600;">${escapeHtml(n.title)}</div>
          <div style="color:var(--text-muted);margin-top:2px;">${escapeHtml(n.message)}</div>
        </div>`).join('');
    } catch (e) {
      // ignore silently
    }
  }

  async function markNotifRead(id) {
    await apiFetch(`/api/notifications/${id}/read`, { method: 'POST' });
    loadNotifications();
  }

  function setupTabs(defaultTab) {
    const navButtons = document.querySelectorAll('.nav-item[data-tab]');
    const activate = (tab) => {
      navButtons.forEach(b => b.classList.toggle('active', b.dataset.tab === tab));
      document.querySelectorAll('.section-view').forEach(s => s.classList.toggle('active', s.id === `tab-${tab}`));
    };
    navButtons.forEach(b => b.addEventListener('click', () => {
      activate(b.dataset.tab);
      const sidebar = document.querySelector('.sidebar');
      if (sidebar) sidebar.classList.remove('open');
    }));
    if (defaultTab) activate(defaultTab);
    return activate;
  }

  return {
    apiFetch, showToast, escapeHtml, formatMoney, statusBadge,
    confirmDialog, renderSmartDecision, renderTunisiaMap,
    setupTopbar, loadNotifications, markNotifRead, setupTabs
  };
})();
