const SLCharts = (() => {
  function renderBarChart(containerId, dataMap, color = '#2563eb', unit = '') {
    const el = document.getElementById(containerId);
    if (!el) return;
    const entries = Object.entries(dataMap || {});
    if (entries.length === 0) {
      el.innerHTML = '<div style="color:var(--text-muted);padding:1.5rem;text-align:center;">Aucune donnée disponible</div>';
      return;
    }
    const maxVal = Math.max(1, ...entries.map(([, v]) => Number(v)));
    el.innerHTML = `
      <div style="display:flex;flex-direction:column;gap:.65rem;">
        ${entries.map(([label, val]) => {
          const pct = Math.min(100, Math.round((Number(val) / maxVal) * 100));
          return `
            <div>
              <div style="display:flex;justify-content:space-between;font-size:.82rem;margin-bottom:3px;">
                <span style="font-weight:600;">${SL.escapeHtml(label)}</span>
                <span style="color:var(--text-muted);">${val}${unit}</span>
              </div>
              <div class="progress-track" style="height:10px;">
                <div class="progress-fill" style="width:${pct}%;background:${color};"></div>
              </div>
            </div>`;
        }).join('')}
      </div>`;
  }

  function renderColumnChart(containerId, dataMap, color = '#2563eb') {
    const el = document.getElementById(containerId);
    if (!el) return;
    const entries = Object.entries(dataMap || {});
    if (entries.length === 0) {
      el.innerHTML = '<div style="color:var(--text-muted);padding:1.5rem;text-align:center;">Aucune donnée</div>';
      return;
    }
    const W = 440, H = 190, padL = 28, padB = 28, padT = 18;
    const maxVal = Math.max(1, ...entries.map(([, v]) => Number(v)));
    const barW = Math.max(14, Math.min(44, ((W - padL - 20) / entries.length) - 10));
    const step = (W - padL - 20) / entries.length;

    const bars = entries.map(([label, val], idx) => {
      const h = ((Number(val) / maxVal) * (H - padB - padT));
      const x = padL + idx * step + (step - barW) / 2;
      const y = H - padB - h;
      const shortLabel = label.length > 10 ? label.slice(5) : label;
      return `
        <rect x="${x}" y="${y}" width="${barW}" height="${Math.max(2, h)}" rx="4" fill="${color}"></rect>
        <text x="${x + barW / 2}" y="${y - 4}" text-anchor="middle" font-size="10" font-weight="700" fill="#1e293b">${val}</text>
        <text x="${x + barW / 2}" y="${H - 10}" text-anchor="middle" font-size="10" fill="#64748b">${SL.escapeHtml(shortLabel)}</text>`;
    }).join('');

    el.innerHTML = `
      <svg viewBox="0 0 ${W} ${H}" style="width:100%;height:auto;display:block;">
        <line x1="${padL}" y1="${H - padB}" x2="${W - 10}" y2="${H - padB}" stroke="#cbd5e1" stroke-width="1"/>
        ${bars}
      </svg>`;
  }

  return { renderBarChart, renderColumnChart };
})();
