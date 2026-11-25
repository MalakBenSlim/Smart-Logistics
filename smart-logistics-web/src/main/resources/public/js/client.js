const ClientApp = (() => {
  let products = [];
  let warehouses = [];
  let orders = [];
  let cart = []; // [{product, quantity}]
  let trackedOrderId = null;
  let activateTab = null;

  const STEPS = ['CREATED', 'PREPARED', 'DRIVER_ASSIGNED', 'SHIPPED', 'IN_DELIVERY', 'DELIVERED'];
  const STEP_LABELS = ['Créée', 'Préparée', 'Livreur affecté', 'Expédiée', 'En livraison', 'Livrée'];

  async function init() {
    activateTab = SL.setupTabs('overview');
    const user = await SL.apiFetch('/api/auth/me');
    SL.setupTopbar(user, handleSseEvent);
    await loadAll();
    document.getElementById('search-prod').addEventListener('input', renderCatalog);
    document.getElementById('cat-filter').addEventListener('change', renderCatalog);
  }

  async function loadAll() {
    [products, warehouses, orders] = await Promise.all([
      SL.apiFetch('/api/client/products'),
      SL.apiFetch('/api/client/warehouses'),
      SL.apiFetch('/api/client/orders')
    ]);
    populateCategories();
    renderOverview();
    renderCatalog();
    renderCart();
    renderOrdersTable();
    populateTrackSelect();
  }

  function handleSseEvent(type, ev) {
    if (type === 'POSITION_UPDATED' && trackedOrderId && ev.orderId === trackedOrderId) {
      const order = orders.find(o => o.id === trackedOrderId);
      if (order) {
        if (!order.delivery) order.delivery = {};
        order.delivery.lastLat = ev.lat;
        order.delivery.lastLng = ev.lng;
        order.delivery.remainingKm = ev.remainingKm;
        order.delivery.estimatedMinutesRemaining = ev.estimatedMinutes;
        renderTrackingPanel(order);
      }
    } else if (ev && ev.orderId) {
      loadAll();
    }
  }

  function populateCategories() {
    const sel = document.getElementById('cat-filter');
    const current = sel.value;
    const cats = [...new Set(products.map(p => p.category))];
    sel.innerHTML = '<option value="">Toutes catégories</option>' +
      cats.map(c => `<option value="${SL.escapeHtml(c)}">${SL.escapeHtml(c)}</option>`).join('');
    sel.value = current;
  }

  function renderOverview() {
    const active = orders.filter(o => o.status !== 'DELIVERED' && o.status !== 'CANCELLED').length;
    const delivered = orders.filter(o => o.status === 'DELIVERED').length;
    const amount = orders.filter(o => o.status !== 'CANCELLED').reduce((s, o) => s + o.totalAmount + o.deliveryCost, 0);

    document.getElementById('kpi-total').textContent = orders.length;
    document.getElementById('kpi-active').textContent = active;
    document.getElementById('kpi-delivered').textContent = delivered;
    document.getElementById('kpi-amount').textContent = SL.formatMoney(amount);

    const latest = orders[0];
    const box = document.getElementById('latest-order-box');
    if (!latest) {
      box.innerHTML = '<p style="color:var(--text-muted);">Aucune commande passée pour le moment.</p>';
      return;
    }
    box.innerHTML = `
      <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:.5rem;margin-bottom:1rem;">
        <div>
          <strong style="font-size:1.1rem;">${SL.escapeHtml(latest.orderCode)}</strong> · ${SL.statusBadge(latest.status)}
          <div style="font-size:.86rem;color:var(--text-muted);">${SL.escapeHtml(latest.deliveryAddress)} · Total : ${SL.formatMoney(latest.totalAmount + latest.deliveryCost)}</div>
        </div>
        <button class="btn btn-outline btn-sm" onclick="ClientApp.trackOrder(${latest.id})">📍 Suivre en direct</button>
      </div>
      ${renderTimeline(latest.status)}
      ${SL.renderSmartDecision(latest.smartDecision)}
    `;
  }

  function renderTimeline(status) {
    if (status === 'CANCELLED') {
      return '<div class="alert alert-error">❌ Commande annulée</div>';
    }
    const idx = STEPS.indexOf(status);
    return `<div class="timeline">${STEPS.map((st, i) => {
      const cls = i < idx || status === 'DELIVERED' ? 'done' : (i === idx ? 'active' : '');
      const icon = cls === 'done' ? '✓' : (cls === 'active' ? '●' : (i + 1));
      return `<div class="timeline-step ${cls}"><div class="timeline-dot">${icon}</div><div>${STEP_LABELS[i]}</div></div>`;
    }).join('')}</div>`;
  }

  function renderCatalog() {
    const q = document.getElementById('search-prod').value.toLowerCase();
    const cat = document.getElementById('cat-filter').value;
    const filtered = products.filter(p =>
      (!cat || p.category === cat) &&
      (!q || p.name.toLowerCase().includes(q) || p.sku.toLowerCase().includes(q))
    );
    document.getElementById('catalog-tbody').innerHTML = filtered.map(p => `
      <tr>
        <td><strong>${SL.escapeHtml(p.name)}</strong><br><small style="color:var(--text-muted);">${SL.escapeHtml(p.sku)}</small></td>
        <td><span class="badge badge-gray">${SL.escapeHtml(p.category)}</span></td>
        <td><strong>${SL.formatMoney(p.unitPrice)}</strong></td>
        <td>${p.totalAvailableStock > 0
          ? `<span class="badge badge-green">${p.totalAvailableStock} dispo</span>`
          : `<span class="badge badge-red">Rupture</span>`}</td>
        <td><button class="btn btn-primary btn-sm" ${p.totalAvailableStock <= 0 ? 'disabled' : ''} onclick="ClientApp.addToCart(${p.id})">+ Ajouter</button></td>
      </tr>`).join('');
  }

  function addToCart(productId) {
    const prod = products.find(p => p.id === productId);
    if (!prod) return;
    const existing = cart.find(c => c.product.id === productId);
    if (existing) existing.quantity++;
    else cart.push({ product: prod, quantity: 1 });
    renderCart();
    SL.showToast('Panier mis à jour', `${prod.name} ajouté.`, 'info');
  }

  function updateQty(productId, delta) {
    const idx = cart.findIndex(c => c.product.id === productId);
    if (idx < 0) return;
    cart[idx].quantity += delta;
    if (cart[idx].quantity <= 0) cart.splice(idx, 1);
    renderCart();
  }

  function renderCart() {
    const count = cart.reduce((s, c) => s + c.quantity, 0);
    document.getElementById('cart-count').textContent = count;
    const box = document.getElementById('cart-items');
    const totalBox = document.getElementById('cart-total-row');
    if (cart.length === 0) {
      box.innerHTML = '<div class="alert alert-info">Panier vide. Ajoutez des produits depuis le catalogue.</div>';
      totalBox.textContent = '';
      return;
    }
    box.innerHTML = cart.map(c => `
      <div style="display:flex;justify-content:space-between;align-items:center;padding:.5rem 0;border-bottom:1px solid var(--border);font-size:.88rem;">
        <div><strong>${SL.escapeHtml(c.product.name)}</strong><br><small>${SL.formatMoney(c.product.unitPrice)}</small></div>
        <div style="display:flex;align-items:center;gap:.4rem;">
          <button class="btn btn-outline btn-sm" onclick="ClientApp.updateQty(${c.product.id},-1)">-</button>
          <strong>${c.quantity}</strong>
          <button class="btn btn-outline btn-sm" onclick="ClientApp.updateQty(${c.product.id},1)">+</button>
        </div>
      </div>`).join('');
    const subtotal = cart.reduce((s, c) => s + c.product.unitPrice * c.quantity, 0);
    totalBox.textContent = `Sous-total produits : ${SL.formatMoney(subtotal)}`;
  }

  function applyDest() {
    const [lat, lng, ...addr] = document.getElementById('dest-preset').value.split(',');
    document.getElementById('ord-lat').value = lat;
    document.getElementById('ord-lng').value = lng;
    document.getElementById('ord-address').value = addr.join(',');
  }

  function toggleCardInput() {
    const m = document.getElementById('ord-payment').value;
    document.getElementById('card-group').style.display = m === 'CARTE_SIMULEE' ? 'block' : 'none';
  }

  async function previewSmart() {
    if (cart.length === 0) return SL.showToast('Panier vide', 'Ajoutez au moins un produit.', 'warning');
    const box = document.getElementById('preview-decision-box');
    try {
      const decision = await SL.apiFetch('/api/client/optimize-preview', {
        method: 'POST',
        body: JSON.stringify({
          lat: parseFloat(document.getElementById('ord-lat').value),
          lng: parseFloat(document.getElementById('ord-lng').value),
          items: cart.map(c => ({ productId: c.product.id, quantity: c.quantity }))
        })
      });
      box.innerHTML = SL.renderSmartDecision(decision);
    } catch (err) {
      box.innerHTML = `<div class="alert alert-error">${SL.escapeHtml(err.message)}</div>`;
    }
  }

  async function submitOrder() {
    if (cart.length === 0) return SL.showToast('Panier vide', 'Ajoutez au moins un produit.', 'warning');
    const box = document.getElementById('preview-decision-box');
    try {
      const created = await SL.apiFetch('/api/client/orders', {
        method: 'POST',
        body: JSON.stringify({
          deliveryAddress: document.getElementById('ord-address').value,
          deliveryLat: parseFloat(document.getElementById('ord-lat').value),
          deliveryLng: parseFloat(document.getElementById('ord-lng').value),
          paymentMethod: document.getElementById('ord-payment').value,
          simulatedCardNumber: document.getElementById('ord-card').value,
          items: cart.map(c => ({ productId: c.product.id, quantity: c.quantity }))
        })
      });
      cart = [];
      box.innerHTML = '';
      SL.showToast('Commande validée !', `${created.orderCode} assignée au ${created.warehouseName}`, 'success');
      await loadAll();
      trackOrder(created.id);
    } catch (err) {
      box.innerHTML = `<div class="alert alert-error">${SL.escapeHtml(err.message)}</div>`;
    }
  }

  function renderOrdersTable() {
    document.getElementById('orders-tbody').innerHTML = orders.map(o => `
      <tr>
        <td><strong>${SL.escapeHtml(o.orderCode)}</strong></td>
        <td>${SL.escapeHtml(o.warehouseName || '—')}</td>
        <td>${SL.formatMoney(o.totalAmount + o.deliveryCost)}</td>
        <td>${o.payment ? SL.statusBadge(o.payment.status) : '—'}</td>
        <td>${SL.statusBadge(o.status)}</td>
        <td style="display:flex;gap:.4rem;">
          <button class="btn btn-outline btn-sm" onclick="ClientApp.trackOrder(${o.id})">Suivre</button>
          ${['CREATED', 'PREPARED', 'DRIVER_ASSIGNED'].includes(o.status)
            ? `<button class="btn btn-danger btn-sm" onclick="ClientApp.cancelOrder(${o.id})">Annuler</button>` : ''}
        </td>
      </tr>`).join('');
  }

  async function cancelOrder(orderId) {
    const ok = await SL.confirmDialog('Annuler la commande ?', 'Le stock réservé sera libéré immédiatement.');
    if (!ok) return;
    await SL.apiFetch(`/api/client/orders/${orderId}/cancel`, { method: 'POST' });
    SL.showToast('Commande annulée', 'Votre commande a bien été annulée.', 'info');
    loadAll();
  }

  function populateTrackSelect() {
    const sel = document.getElementById('track-select');
    sel.innerHTML = orders.map(o => `<option value="${o.id}">${SL.escapeHtml(o.orderCode)} (${o.status})</option>`).join('');
    if (!trackedOrderId && orders.length > 0) trackedOrderId = orders[0].id;
    if (trackedOrderId) {
      sel.value = trackedOrderId;
      const ord = orders.find(o => o.id === Number(trackedOrderId));
      if (ord) renderTrackingPanel(ord);
    }
  }

  function trackOrder(orderId) {
    trackedOrderId = Number(orderId);
    activateTab('tracking');
    populateTrackSelect();
  }

  function selectTrackedOrder(id) {
    trackedOrderId = Number(id);
    const ord = orders.find(o => o.id === trackedOrderId);
    if (ord) renderTrackingPanel(ord);
  }

  function renderTrackingPanel(order) {
    document.getElementById('track-details').innerHTML = `
      <div style="margin-bottom:.75rem;">
        <strong>${SL.escapeHtml(order.orderCode)}</strong> · ${SL.statusBadge(order.status)}
        <div style="font-size:.85rem;color:var(--text-muted);margin-top:.25rem;">
          Destination : ${SL.escapeHtml(order.deliveryAddress)} · Délai estimé : ~${order.estimatedMinutes} min
        </div>
      </div>
      ${renderTimeline(order.status)}
      ${SL.renderSmartDecision(order.smartDecision)}
    `;

    const wh = warehouses.find(w => w.id === order.warehouseId);
    const driverPos = order.delivery
      ? { lat: order.delivery.lastLat, lng: order.delivery.lastLng }
      : null;

    SL.renderTunisiaMap('client-map', {
      warehouses,
      origin: wh ? { lat: wh.latitude, lng: wh.longitude } : null,
      target: { lat: order.deliveryLat, lng: order.deliveryLng },
      driver: driverPos
    });

    const info = document.getElementById('driver-telemetry-info');
    const etaBadge = document.getElementById('eta-badge');
    if (order.delivery) {
      etaBadge.textContent = order.delivery.status === 'LIVREE'
        ? 'Livrée ✓'
        : `ETA ~${order.delivery.estimatedMinutesRemaining || 15} min`;
      info.innerHTML = `
        <strong>Livreur :</strong> ${SL.escapeHtml(order.delivery.driverName)} (${SL.escapeHtml(order.delivery.vehicleType)}) ·
        <strong>Distance restante :</strong> ${Number(order.delivery.remainingKm || 0).toFixed(2)} km ·
        <strong>GPS :</strong> ${Number(order.delivery.lastLat).toFixed(4)}, ${Number(order.delivery.lastLng).toFixed(4)}
      `;
    } else {
      etaBadge.textContent = 'En attente livreur';
      info.textContent = 'Aucun livreur encore affecté à cette commande.';
    }
  }

  function openCartTab() {
    activateTab('catalog');
  }

  window.addEventListener('DOMContentLoaded', init);
  return { addToCart, updateQty, applyDest, toggleCardInput, previewSmart, submitOrder, cancelOrder, trackOrder, selectTrackedOrder, openCartTab };
})();
