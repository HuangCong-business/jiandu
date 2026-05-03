// ============================================================
// META - reads file headers WITHOUT pandas (uses openpyxl + csv)
// ============================================================
async function getMeta() {
    let ctx = "";
    for (let f of fileManager.files) {
        let res;
        if (f.type === 'csv') {
            res = await pyodide.runPythonAsync(`
import csv, json
try:
    with open("${f.name}", newline="", encoding="utf-8") as fh:
        reader = list(csv.reader(fh))
        cols = reader[0] if reader else []
        rows = max(0, len(reader) - 1)
except:
    with open("${f.name}", newline="", encoding="gbk") as fh:
        reader = list(csv.reader(fh))
        cols = reader[0] if reader else []
        rows = max(0, len(reader) - 1)
json.dumps({"cols": cols, "rows": rows})
`);
        } else {
            res = await pyodide.runPythonAsync(`
import openpyxl, json
try:
    wb = openpyxl.load_workbook("${f.name}", read_only=True, data_only=True)
    ws = wb.active
    rows = ws.max_row or 0
    cols = [str(c.value) if c.value is not None else "" for c in next(iter(ws.iter_rows(min_row=1, max_row=1)), [])]
    wb.close()
except:
    cols = []
    rows = 0
json.dumps({"cols": cols, "rows": rows})
`);
        }
        const info = JSON.parse(res);
        const typeLabel = f.type === 'csv' ? 'CSV' : 'EXCEL';
        ctx += `File "${f.name}" Type=${typeLabel} | ${info.rows} rows, cols: ${info.cols.join(', ')}\n`;
    }
    return ctx;
}
