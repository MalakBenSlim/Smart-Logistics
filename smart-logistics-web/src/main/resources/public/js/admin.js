const AdminApp = (() => {
  let ordersCache = [];

  async function init() {
    SL.setupTabs('overview');
    const user = await SL.apiFetch('/api/auth/me');
    SL.setupTopbar(user, () => refreshAll());
    await refreshAll();

    document.getElementById('wh-form').addEventListener('submit', saveWarehouse);
    document.getElementById('drv-form').addEventListener('submit', saveDriver);
  }

  async function refreshAll() {
    await Promise.all([loadStats(), loadHealth(), loadOrders(), loadWarehouses(), loadDrivers(), loadUsers()]);
  }

  async function loadStats() {
    const s = await SL.apiFetch('/api/admin/stats');
    document.getElementById('k-users').textContent = s.totalUsers;
    document.getElementById('k-orders').textContent = s.totalOrders;
    document.getElementById('k-active').textContent = s.activeOrders;
    document.getElementById('k-deliv').textContent = s.deliveredOrders;
    document.getElementById('k-rev').textContent = SL.formatMoney(s.totalRevenue);
    document.getElementById('k-drv').textContent = `${s.availableDrivers} / ${s.totalDrivers}`;
    document.getElementById('k-wh').textContent = s.totalWarehouses;
    document.getElementById('k-out').textContent = `${s.outOfStockCount} / ${s.lowStockCount}`;

    SLCharts.renderColumnChart('chart-orders', s.ordersByDay, '#2563eb');
    SLCharts.renderColumnChart('chart-deliveries', s.deliveriesByDay, '#10b981');
    SLCharts.renderBarChart('chart-top-prod', s.topProducts, '#7c3aed', ' u.');
    SLCharts.renderBarChart('chart-wh-load', s.warehouseLoads, '#f59e0b', '%');
    SLCharts.renderBarChart('chart-drv-perf', s.driverPerformances, '#0ea5e9', ' liv.');
  }

  async function loadHealth() {
    const h = await SL.apiFetch('/api/admin/health');
    document.getElementById('health-checked-at').textContent = `Dernière vérification : ${h.checkedAt || 'maintenant'}`;
    const items = [
      ['Annuaire JNDI / RMI Registry', h.rmiRegistryOnline, 'Port 1099'],
      ['Broker JMS ActiveMQ', h.jmsBrokerOnline, 'Port 61616'],
      ['Serveur Socket TCP Livreurs', h.tcpSocketOnline, `${h.activeDriverSockets} connecté(s) · Port 9095`],
      ['Base de données H2 TCP', h.databaseOnline, 'Pool HikariCP · Port 9092'],
      ...Object.entries(h.serviceStatuses || {}).map(([name, ok]) => [name, ok, 'Service RMI distant'])
    ];
    document.getElementById('health-cards').innerHTML = items.map(([label, ok, sub]) => `
      <div class="kpi-card">
        <div style="display:flex;justify-content:space-between;align-items:center;">
          <strong style="font-size:.92rem;">${SL.escapeHtml(label)}</strong>
          <span class="badge ${ok ? 'badge-green' : 'badge-red'}">${ok ? '● ONLINE' : '● OFFLINE'}</span>
        </div>
        <div class="kpi-sub">${SL.escapeHtml(sub)}</div>
      </div>`).join('');
  }

  async function loadOrders() {
    ordersCache = await SL.apiFetch('/api/admin/orders');
    document.getElementById('admin-orders-tbody').innerHTML = ordersCache.map(o => `
      <tr>
        <td><strong>${SL.escapeHtml(o.orderCode)}</strong></td>
        <td>${SL.escapeHtml(o.clientName)}</td>
        <td>${SL.escapeHtml(o.warehouseName || '—')}</td>
        <td>${SL.formatMoney(o.totalAmount + o.deliveryCost)}</td>
        <td>${SL.statusBadge(o.status)}</td>
        <td>${o.smartDecision
          ? `<button class="btn btn-outline btn-sm" onclick="AdminApp.inspectDecision(${o.id})">🤖 ${o.smartDecision.warehouseScore}/100</button>`
          : '—'}</td>
      </tr>`).join('');
  }

  function inspectDecision(orderId) {
    const ord = ordersCache.find(o => o.id === orderId);
    if (!ord) return;
    document.getElementById('decision-modal-body').innerHTML = SL.renderSmartDecision(ord.smartDecision);
    document.getElementById('decision-modal').classList.add('open');
  }

  async function loadWarehouses() {
    const list = await SL.apiFetch('/api/admin/warehouses');
    document.getElementById('admin-wh-tbody').innerHTML = list.map(w => `
      <tr>
        <td><strong>${SL.escapeHtml(w.name)}</strong></td>
        <td>${SL.escapeHtml(w.address)}</td>
        <td><small>${w.latitude.toFixed(3)}, ${w.longitude.toFixed(3)}</small></td>
        <td>${w.currentLoad} / ${w.maxCapacity}</td>
        <td>${SL.statusBadge(w.status)}</td>
      </tr>`).join('');
  }

  async function saveWarehouse(e) {
    e.preventDefault();
    await SL.apiFetch('/api/admin/warehouses', {
      method: 'POST',
      body: JSON.stringify({
        name: document.getElementById('w-name').value,
        address: document.getElementById('w-addr').value,
        latitude: parseFloat(document.getElementById('w-lat').value),
        longitude: parseFloat(document.getElementById('w-lng').value),
        maxCapacity: parseInt(document.getElementById('w-cap').value, 10),
        status: 'ACTIF'
      })
    });
    e.target.reset();
    SL.showToast('Dépôt créé', 'Nouveau dépôt enregistré via RMI.', 'success');
    refreshAll();
  }

  async function loadDrivers() {
    const list = await SL.apiFetch('/api/admin/drivers');
    document.getElementById('admin-drv-tbody').innerHTML = list.map(d => `
      <tr>
        <td><strong>${SL.escapeHtml(d.fullName)}</strong></td>
        <td>${SL.escapeHtml(d.phone)}</td>
        <td>${SL.escapeHtml(d.vehicleType)}</td>
        <td>${d.activeDeliveries}</td>
        <td>${d.totalDeliveries}</td>
        <td>${SL.statusBadge(d.status)}</td>
      </tr>`).join('');
  }

  async function saveDriver(e) {
    e.preventDefault();
    await SL.apiFetch('/api/admin/drivers', {
      method: 'POST',
      body: JSON.stringify({
        fullName: document.getElementById('d-name').value,
        phone: document.getElementById('d-phone').value,
        vehicleType: document.getElementById('d-veh').value,
        currentLat: parseFloat(document.getElementById('d-lat').value),
        currentLng: parseFloat(document.getElementById('d-lng').value),
        status: 'DISPONIBLE'
      })
    });
    e.target.reset();
    SL.showToast('Livreur ajouté', 'Flotte mise à jour.', 'success');
    refreshAll();
  }

  async function loadUsers() {
    const list = await SL.apiFetch('/api/admin/users');
    const roles = ['CLIENT', 'GESTIONNAIRE_STOCK', 'LIVREUR', 'ADMINISTRATEUR'];
    document.getElementById('admin-users-tbody').innerHTML = list.map(u => `
      <tr>
        <td><strong>${SL.escapeHtml(u.fullName)}</strong></td>
        <td>${SL.escapeHtml(u.email)}</td>
        <td>
          <select class="form-control" id="u-role-${u.id}" style="padding:.25rem .4rem;">
            ${roles.map(r => `<option value="${r}" ${u.role === r ? 'selected' : ''}>${r}</option>`).join('')}
          </select>
        </td>
        <td><input type="checkbox" id="u-act-${u.id}" ${u.active ? 'checked' : ''}></td>
        <td><button class="btn btn-primary btn-sm" onclick="AdminApp.updateUser(${u.id})">Sauvegarder</button></td>
      </tr>`).join('');
  }

  async function updateUser(id) {
    await SL.apiFetch(`/api/admin/users/${id}`, {
      method: 'PUT',
      body: JSON.stringify({
        role: document.getElementById(`u-role-${id}`).value,
        active: document.getElementById(`u-act-${id}`).checked
      })
    });
    SL.showToast('Utilisateur mis à jour', 'Permissions enregistrées.', 'success');
    loadUsers();
  }

  window.addEventListener('DOMContentLoaded', init);
  return { loadHealth, inspectDecision, updateUser };
})();
