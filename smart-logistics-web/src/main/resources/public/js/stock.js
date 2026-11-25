const StockApp = (() => {
  let warehouses = [];
  let products = [];

  async function init() {
    SL.setupTabs('warehouses');
    const user = await SL.apiFetch('/api/auth/me');
    SL.setupTopbar(user, () => refreshAll());
    await refreshAll();

    document.getElementById('transfer-form').addEventListener('submit', submitTransfer);
    document.getElementById('prod-form').addEventListener('submit', submitProduct);
  }

  async function refreshAll() {
    warehouses = await SL.apiFetch('/api/stock/warehouses');
    products = await SL.apiFetch('/api/stock/products');
    renderWarehouses();
    populateSelectors();
    await Promise.all([loadInventory(), loadProducts(), loadAlerts(), loadOrders()]);
  }

  function renderWarehouses() {
    document.getElementById('wh-cards').innerHTML = warehouses.map(w => {
      const pct = Math.min(100, Math.round((w.currentLoad / w.maxCapacity) * 100));
      return `
        <div class="kpi-card">
          <div style="display:flex;justify-content:space-between;align-items:center;">
            <strong>${SL.escapeHtml(w.name)}</strong> ${SL.statusBadge(w.status)}
          </div>
          <div style="font-size:.8rem;color:var(--text-muted);">${SL.escapeHtml(w.address)}</div>
          <div class="kpi-value" style="font-size:1.35rem;margin-top:.3rem;">${pct}% charge</div>
          <div class="progress-track"><div class="progress-fill" style="width:${pct}%;"></div></div>
          <div class="kpi-sub">${w.currentLoad} / ${w.maxCapacity} unités</div>
        </div>`;
    }).join('');
  }

  function populateSelectors() {
    const whFilter = document.getElementById('wh-filter');
    const current = whFilter.value;
    whFilter.innerHTML = '<option value="">Tous les dépôts</option>' +
      warehouses.map(w => `<option value="${w.id}">${SL.escapeHtml(w.name)}</option>`).join('');
    whFilter.value = current;

    const whOpts = warehouses.map(w => `<option value="${w.id}">${SL.escapeHtml(w.name)}</option>`).join('');
    document.getElementById('tr-from').innerHTML = whOpts;
    document.getElementById('tr-to').innerHTML = whOpts;
    if (warehouses.length > 1) document.getElementById('tr-to').selectedIndex = 1;

    document.getElementById('tr-prod').innerHTML = products
      .map(p => `<option value="${p.id}">${SL.escapeHtml(p.name)}</option>`).join('');
  }

  async function loadInventory() {
    const whId = document.getElementById('wh-filter').value;
    const items = await SL.apiFetch('/api/stock/inventory' + (whId ? `?warehouseId=${whId}` : ''));
    document.getElementById('inv-tbody').innerHTML = items.map(s => `
      <tr>
        <td>${SL.escapeHtml(s.warehouseName)}</td>
        <td><strong>${SL.escapeHtml(s.productName)}</strong></td>
        <td><strong style="color:${s.availableQuantity <= 0 ? 'var(--danger)' : (s.availableQuantity <= s.lowStockThreshold ? 'var(--warning)' : 'var(--success)')};">${s.availableQuantity}</strong> (${s.quantity})</td>
        <td>${s.reservedQuantity}</td>
        <td>${s.lowStockThreshold}</td>
        <td style="display:flex;gap:.3rem;">
          <input type="number" min="0" id="qty-${s.warehouseId}-${s.productId}" value="${s.quantity}" class="form-control" style="width:72px;padding:.25rem .4rem;">
          <button class="btn btn-primary btn-sm" onclick="StockApp.saveQty(${s.warehouseId},${s.productId})">OK</button>
        </td>
      </tr>`).join('');
  }

  async function saveQty(warehouseId, productId) {
    const qty = parseInt(document.getElementById(`qty-${warehouseId}-${productId}`).value, 10);
    await SL.apiFetch('/api/stock/adjust', {
      method: 'PUT',
      body: JSON.stringify({ warehouseId, productId, quantity: qty })
    });
    SL.showToast('Stock mis à jour', 'Inventaire actualisé via RMI.', 'success');
    refreshAll();
  }

  async function submitTransfer(e) {
    e.preventDefault();
    try {
      await SL.apiFetch('/api/stock/transfer', {
        method: 'POST',
        body: JSON.stringify({
          fromWarehouseId: Number(document.getElementById('tr-from').value),
          toWarehouseId: Number(document.getElementById('tr-to').value),
          productId: Number(document.getElementById('tr-prod').value),
          quantity: Number(document.getElementById('tr-qty').value)
        })
      });
      SL.showToast('Transfert réussi', 'Stock transféré atomiquement entre dépôts.', 'success');
      refreshAll();
    } catch (err) {
      SL.showToast('Erreur transfert', err.message, 'error');
    }
  }

  function loadProducts() {
    document.getElementById('prod-tbody').innerHTML = products.map(p => `
      <tr>
        <td><code>${SL.escapeHtml(p.sku)}</code></td>
        <td><strong>${SL.escapeHtml(p.name)}</strong></td>
        <td>${SL.escapeHtml(p.category)}</td>
        <td>${SL.formatMoney(p.unitPrice)}</td>
        <td>${p.lowStockThreshold}</td>
        <td><button class="btn btn-danger btn-sm" onclick="StockApp.deleteProduct(${p.id})">Supprimer</button></td>
      </tr>`).join('');
  }

  async function submitProduct(e) {
    e.preventDefault();
    try {
      await SL.apiFetch('/api/stock/products', {
        method: 'POST',
        body: JSON.stringify({
          sku: document.getElementById('p-sku').value,
          name: document.getElementById('p-name').value,
          category: document.getElementById('p-cat').value,
          unitPrice: parseFloat(document.getElementById('p-price').value),
          lowStockThreshold: parseInt(document.getElementById('p-thresh').value, 10)
        })
      });
      e.target.reset();
      SL.showToast('Produit ajouté', 'Initialisé dans tous les dépôts.', 'success');
      refreshAll();
    } catch (err) {
      SL.showToast('Erreur produit', err.message, 'error');
    }
  }

  async function deleteProduct(id) {
    if (!await SL.confirmDialog('Désactiver ce produit ?', 'Il ne sera plus commandable par les clients.')) return;
    await SL.apiFetch(`/api/stock/products/${id}`, { method: 'DELETE' });
    refreshAll();
  }

  async function loadAlerts() {
    const alerts = await SL.apiFetch('/api/stock/alerts');
    const renderList = (arr, cls) => arr.length === 0
      ? '<div style="color:var(--text-muted);">Aucune alerte</div>'
      : arr.map(a => `<div class="alert ${cls}" style="display:flex;justify-content:space-between;">
          <span><strong>${SL.escapeHtml(a.productName)}</strong> · ${SL.escapeHtml(a.warehouseName)}</span>
          <strong>${a.availableQuantity} dispo</strong>
        </div>`).join('');
    document.getElementById('out-stock-list').innerHTML = renderList(alerts.outOfStock, 'alert-error');
    document.getElementById('low-stock-list').innerHTML = renderList(alerts.lowStock, 'alert-info');
  }

  async function loadOrders() {
    const orders = await SL.apiFetch('/api/stock/orders');
    document.getElementById('stock-orders-tbody').innerHTML = orders.map(o => `
      <tr>
        <td><strong>${SL.escapeHtml(o.orderCode)}</strong></td>
        <td>${SL.escapeHtml(o.clientName)}</td>
        <td>${SL.escapeHtml(o.warehouseName || '—')}</td>
        <td>${SL.statusBadge(o.status)}</td>
        <td>${o.delivery ? SL.escapeHtml(o.delivery.driverName) : '—'}</td>
        <td>
          ${o.status === 'CREATED' ? `<button class="btn btn-primary btn-sm" onclick="StockApp.prepareOrder(${o.id})">Préparer & Affecter Livreur</button>` : ''}
          ${o.status === 'DRIVER_ASSIGNED' ? `<button class="btn btn-success btn-sm" onclick="StockApp.shipOrder(${o.id})">Expédier</button>` : ''}
        </td>
      </tr>`).join('');
  }

  async function prepareOrder(id) {
    try {
      const updated = await SL.apiFetch(`/api/stock/orders/${id}/prepare`, { method: 'POST' });
      SL.showToast('Commande préparée', `Livreur affecté : ${updated.delivery ? updated.delivery.driverName : 'OK'}`, 'success');
      refreshAll();
    } catch (err) {
      SL.showToast('Erreur préparation', err.message, 'error');
    }
  }

  async function shipOrder(id) {
    await SL.apiFetch(`/api/stock/orders/${id}/ship`, { method: 'POST' });
    SL.showToast('Commande expédiée', 'Stock physique déduit.', 'success');
    refreshAll();
  }

  window.addEventListener('DOMContentLoaded', init);
  return { loadInventory, saveQty, deleteProduct, prepareOrder, shipOrder };
})();
