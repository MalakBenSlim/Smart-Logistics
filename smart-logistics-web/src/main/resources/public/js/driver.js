const DriverApp = (() => {
  let profile = null;
  let deliveries = [];
  let warehouses = [];
  let activateTab = null;

  async function init() {
    activateTab = SL.setupTabs('deliveries');
    const user = await SL.apiFetch('/api/auth/me');
    SL.setupTopbar(user, (type, ev) => {
      if (type === 'POSITION_UPDATED') {
        appendTcpLog(`SSE/JMS POSITION_UPDATED -> lat=${ev.lat}, lng=${ev.lng} (${ev.remainingKm} km)`);
        const d = deliveries.find(x => x.id === ev.deliveryId);
        if (d) {
          d.lastLat = ev.lat;
          d.lastLng = ev.lng;
          renderDriverMap(d);
        }
      } else {
        loadAll();
      }
    });
    await loadAll();
  }

  async function loadAll() {
    [profile, deliveries, warehouses] = await Promise.all([
      SL.apiFetch('/api/driver/profile'),
      SL.apiFetch('/api/driver/deliveries'),
      SL.apiFetch('/api/client/warehouses')
    ]);
    document.getElementById('drv-status-sel').value = profile.status;
    document.getElementById('kpi-veh').textContent = profile.vehicleType;
    document.getElementById('kpi-stat').innerHTML = SL.statusBadge(profile.status);
    document.getElementById('kpi-act').textContent = profile.activeDeliveries;
    document.getElementById('kpi-tot').textContent = profile.totalDeliveries;

    renderDeliveries();
    populateTelSelect();
  }

  function renderDeliveries() {
    document.getElementById('drv-del-tbody').innerHTML = deliveries.map(d => `
      <tr>
        <td><strong>${SL.escapeHtml(d.orderCode)}</strong></td>
        <td>${SL.escapeHtml(d.deliveryAddress)}</td>
        <td>${Number(d.remainingKm || 0).toFixed(2)} km</td>
        <td>${SL.statusBadge(d.status)}</td>
        <td style="display:flex;gap:.35rem;flex-wrap:wrap;">
          ${d.status === 'ASSIGNEE' ? `<button class="btn btn-primary btn-sm" onclick="DriverApp.startDelivery(${d.id})">Démarrer (TCP)</button>` : ''}
          ${d.status !== 'LIVREE' ? `<button class="btn btn-outline btn-sm" onclick="DriverApp.openTelemetry(${d.id})">🚀 Simuler GPS</button>` : ''}
          ${d.status === 'EN_ROUTE' ? `<button class="btn btn-success btn-sm" onclick="DriverApp.completeDelivery(${d.id})">✓ Confirmer livrée</button>` : ''}
        </td>
      </tr>`).join('');
  }

  function populateTelSelect() {
    const sel = document.getElementById('tel-del-select');
    const prev = sel.value;
    sel.innerHTML = deliveries.map(d => `<option value="${d.id}">${SL.escapeHtml(d.orderCode)} — ${d.status}</option>`).join('');
    if (prev) sel.value = prev;
    selectTelemetryDelivery();
  }

  function selectTelemetryDelivery() {
    const id = Number(document.getElementById('tel-del-select').value);
    const d = deliveries.find(x => x.id === id);
    if (!d) return;
    document.getElementById('tel-lat').value = d.lastLat;
    document.getElementById('tel-lng').value = d.lastLng;
    renderDriverMap(d);
  }

  function renderDriverMap(d) {
    SL.renderTunisiaMap('driver-map', {
      warehouses,
      origin: { lat: d.warehouseLat, lng: d.warehouseLng },
      target: { lat: d.targetLat, lng: d.targetLng },
      driver: { lat: d.lastLat, lng: d.lastLng }
    });
  }

  function openTelemetry(deliveryId) {
    activateTab('telemetry');
    document.getElementById('tel-del-select').value = deliveryId;
    selectTelemetryDelivery();
  }

  async function changeStatus(status) {
    await SL.apiFetch('/api/driver/status', { method: 'PUT', body: JSON.stringify({ status }) });
    SL.showToast('Statut mis à jour', `Votre disponibilité est maintenant ${status}.`, 'info');
    loadAll();
  }

  async function startDelivery(id) {
    const res = await SL.apiFetch(`/api/driver/deliveries/${id}/start`, { method: 'POST' });
    appendTcpLog(`TCP DELIVERY_STARTED -> ${res.tcpAck}`);
    loadAll();
  }

  async function sendPosition() {
    const id = document.getElementById('tel-del-select').value;
    if (!id) return;
    const res = await SL.apiFetch(`/api/driver/deliveries/${id}/position`, {
      method: 'POST',
      body: JSON.stringify({
        lat: parseFloat(document.getElementById('tel-lat').value),
        lng: parseFloat(document.getElementById('tel-lng').value)
      })
    });
    appendTcpLog(`TCP POSITION_UPDATE -> ${res.tcpAck}`);
  }

  async function simulateTcpRoute() {
    const id = document.getElementById('tel-del-select').value;
    if (!id) return;
    const res = await SL.apiFetch(`/api/driver/deliveries/${id}/simulate-tcp`, { method: 'POST' });
    appendTcpLog(res.message);
    SL.showToast('Simulateur TCP lancé', res.message, 'success');
  }

  async function completeDelivery(id) {
    if (!await SL.confirmDialog('Confirmer la livraison ?', 'La commande passera au statut DELIVERED.')) return;
    const res = await SL.apiFetch(`/api/driver/deliveries/${id}/complete`, { method: 'POST' });
    appendTcpLog(`TCP DELIVERY_COMPLETED -> ${res.tcpAck}`);
    loadAll();
  }

  function appendTcpLog(line) {
    const box = document.getElementById('tcp-log-box');
    const time = new Date().toLocaleTimeString();
    box.innerHTML = `[${time}] ${SL.escapeHtml(line)}<br>` + box.innerHTML;
  }

  window.addEventListener('DOMContentLoaded', init);
  return { changeStatus, startDelivery, sendPosition, simulateTcpRoute, completeDelivery, openTelemetry, selectTelemetryDelivery };
})();
