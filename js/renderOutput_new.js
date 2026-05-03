// ============================================================
// RENDER OUTPUT - with Excel preview table + download buttons
// ============================================================
function renderOutput(plan) {
    const area = document.getElementById('resultContent');

    // Explanation box
    let html = `<div style="padding:16px 20px; background:#f0f9ff; border-radius:12px; line-height:1.8; margin-bottom:16px; border-left:4px solid #0ea5e9;">
        <div style="font-size:13px; font-weight:600; color:#0369a1; margin-bottom:8px;">Analysis Result:</div>
        <div style="font-size:15px; color:#1e293b; white-space:pre-wrap;">` + escapeHtml(plan.explanation) + `</div>
    </div>`;

    // Excel preview + download
    try {
        if (pyodide && pyodide.FS.analyzePath('final.xlsx').exists) {
            const xlsxBytes = pyodide.FS.readFile('final.xlsx');
            const blob = new Blob([new Uint8Array(xlsxBytes)], {type:'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'});
            const xlsxUrl = URL.createObjectURL(blob);
            window.__lastXlsxUrl = xlsxUrl;

            html += `<div style="background:#fff; border:1px solid #e2e8f0; border-radius:12px; padding:16px; margin-bottom:16px;">
                <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:12px;">
                    <span style="font-size:13px;font-weight:600;color:#1e293b;">Excel Preview (max 20 rows)</span>
                    <button onclick="previewXlsx()" style="background:#0ea5e9;color:#fff;border:none;padding:4px 12px;border-radius:6px;cursor:pointer;font-size:12px;">Refresh</button>
                </div>
                <div id="xlsxPreview" style="max-height:300px;overflow:auto;font-size:12px;border:1px solid #e2e8f0;border-radius:6px;"></div>
            </div>`;
        }
    } catch(e) {}

    // Charts
    for (let i = 1; i <= 10; i++) {
        const name = 'c' + i + '.png';
        try {
            if (pyodide && pyodide.FS.analyzePath(name).exists) {
                const b = pyodide.FS.readFile(name);
                const blob = new Blob([new Uint8Array(b)], {type:'image/png'});
                const url = URL.createObjectURL(blob);
                window.__lastChartUrl = url;
                html += `<div style="margin-top:16px; text-align:center; background:#fff; border:1px solid #e2e8f0; border-radius:12px; padding:12px;">
                    <div style="font-size:12px; color:#64748b; margin-bottom:8px;">Chart ` + i + `</div>
                    <img src="` + url + `" style="max-width:100%; border-radius:6px;" />
                </div>`;
            }
        } catch(e) {}
    }

    // Download buttons
    const dlBtns = [];
    if (window.__lastXlsxUrl) {
        dlBtns.push('<button onclick="downloadFile(window.__lastXlsxUrl, \'result.xlsx\')" style="background:#10b981;color:#fff;border:none;padding:8px 16px;border-radius:8px;cursor:pointer;font-size:13px;">Download Excel (.xlsx)</button>');
    }
    if (window.__lastChartUrl) {
        dlBtns.push('<button onclick="downloadFile(window.__lastChartUrl, \'chart.png\')" style="background:#8b5cf6;color:#fff;border:none;padding:8px 16px;border-radius:8px;cursor:pointer;font-size:13px;">Download Chart (.png)</button>');
    }
    if (dlBtns.length > 0) {
        html += '<div style="margin-top:16px;display:flex;gap:10px;flex-wrap:wrap;">' + dlBtns.join('') + '</div>';
    }

    area.innerHTML = html;
    if (window.__lastXlsxUrl) { setTimeout(previewXlsx, 200); }
}

// Preview Excel file as HTML table (openpyxl, no pandas needed)
window.previewXlsx = async function() {
    const el = document.getElementById('xlsxPreview');
    if (!el || !pyodide) return;
    try {
        const html = await pyodide.runPythonAsync(`
import openpyxl
try:
    wb = openpyxl.load_workbook('final.xlsx', read_only=True, data_only=True)
    ws = wb.active
    rows_data = [list(row) for row in ws.iter_rows(values_only=True)]
    wb.close()
    n_rows = min(len(rows_data), 20)
    n_cols = len(rows_data[0]) if rows_data else 0
    h = '<table style="border-collapse:collapse;width:100%;">'
    for ri in range(n_rows):
        bg = '#f1f5f9' if ri % 2 == 0 else '#ffffff'
        h += '<tr>'
        for ci in range(n_cols):
            val = rows_data[ri][ci]
            val_str = str(val) if val is not None else ''
            cell_bg = '#0ea5e9' if ri == 0 else bg
            txt_color = '#ffffff' if ri == 0 else '#334155'
            h += '<td style="padding:5px 10px;border:1px solid #cbd5e1;background:' + cell_bg + ';color:' + txt_color + ';font-size:12px;">' + val_str + '</td>'
        h += '</tr>'
    h += '</table>'
    h
except Exception as e:
    '<div style="color:#ef4444;font-size:13px;">Preview error: ' + str(e) + '</div>'
`);
        el.innerHTML = html;
    } catch(e) {
        el.innerHTML = '<div style="color:#ef4444;font-size:13px;">Error: ' + e.message + '</div>';
    }
};

window.downloadFile = function(url, filename) {
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
};

function escapeHtml(text) {
    if (!text) return '';
    const d = document.createElement('div');
    d.textContent = text;
    return d.innerHTML;
}
