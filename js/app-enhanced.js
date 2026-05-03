/**
 * 简牍 (Jian Du) - 核心逻辑增强版 (支持 CSV + 分析历史保存)
 * 基于 app.js 稳定版，添加了与后端 API 的集成
 */

let pyodide = null;
let fileManager = { files: [] };
let isRunning = false;
let fontsOk = false;
const MAX_FIX = 3;

// 性能监控
let analysisStartTime = 0;
let currentApiKey = null;

// --- 1. 引擎初始化 (带进度显示) ---
// ============================================================
// 1. ENGINE INIT - uses micropip to install pandas (no version mismatch)
// ============================================================
async function initPyodide() {
    const status = document.getElementById('envStatus');
    const dot = document.getElementById('envDot');
    try {
        const timeout = setTimeout(() => {
            status.textContent = "Engine slow, please wait or refresh";
            dot.style.background = "#f43f5e";
        }, 60000);

        status.textContent = "Loading Pyodide core...";
        pyodide = await loadPyodide({ indexURL: './pyodide/' });

        status.textContent = "Installing pandas + matplotlib via micropip...";
        await pyodide.runPythonAsync(`
import micropip
await micropip.install(['pandas', 'matplotlib', 'openpyxl', 'Pillow'])
import matplotlib
matplotlib.use('Agg')
        `);

        clearTimeout(timeout);
        status.textContent = "Engine ready";
        dot.style.background = "#36b37e";
    } catch (e) {
        status.textContent = "Engine init failed: " + e.message;
        console.error("Pyodide Init Error:", e);
    }
}


// --- 2. 汉字引擎加载 ---
async function ensureFonts() {
    if (fontsOk) return;
    const fontBadge = document.getElementById('fontStatus');
    try {
        const res = await fetch('STXINWEI.TTF');
        if (!res.ok) throw new Error("未找到字体文件");
        const buffer = await res.arrayBuffer();
        pyodide.FS.writeFile("custom_font.ttf", new Uint8Array(buffer));
        await pyodide.runPythonAsync(`
import matplotlib.font_manager as fm
import matplotlib.pyplot as plt
fm.fontManager.addfont("custom_font.ttf")
plt.rcParams['font.family'] = fm.FontProperties(fname="custom_font.ttf").get_name()
plt.rcParams['axes.unicode_minus'] = False
        `);
        fontsOk = true;
        if (fontBadge) fontBadge.textContent = "汉字引擎：已就绪";
    } catch (e) {
        if (fontBadge) fontBadge.textContent = "汉字引擎：不可用";
        console.warn("字体加载失败:", e);
    }
}

// --- 3. 核心执行逻辑（增强版 - 添加历史保存） ---
async function handleRequest() {
    if (isRunning) return;
    const query = document.getElementById('userInput').value;
    // 优先用用户个人 API Key（管理员分配），其次用全局设置
    let apiKey = localStorage.getItem('deepseek_api_key');
    try {
        const u = JSON.parse(localStorage.getItem('jiandu_current_user') || '{}');
        if (u.api_key) apiKey = u.api_key;
    } catch {}
    currentApiKey = apiKey;
    
    if (!apiKey) {
        return alert("⚠️ 请先在设置页面配置 API Key！\n\n1. 返回首页\n2. 点击左下角'设置'\n3. 填写 DeepSeek API Key 并保存\n\n💡 调试提示：按 F12 打开控制台查看详细日志");
    }
    if (fileManager.files.length === 0) {
        return alert("请先上传文件（Excel 或 CSV）");
    }

    isRunning = true;
    analysisStartTime = Date.now();
    loader.classList.remove('hidden');

    let plan = null;
    let executionTimeMs = 0;
    let apiCost = 0;
    let chartCount = 0;

    try {
        let lastErr = null;
        let attempt = 0;

        while (attempt < MAX_FIX) {
            try {
                const meta = await getMeta();
                const fetchStart = Date.now();
                plan = await fetchAI(query, meta, apiKey, lastErr);
                const fetchEnd = Date.now();
                
                // 估算 API 成本（基于 token 使用，简化计算）
                const inputTokens = query.length / 4 + meta.length / 4;
                const outputTokens = plan.code.length / 4;
                apiCost = (inputTokens * 0.000001 + outputTokens * 0.000002); // 约估算
                
                if (plan.needs_chart) await ensureFonts();

                // 构建文件读取代码，根据文件类型选择读取方式
                const readCode = fileManager.files.map((f, i) => {
                    const readFunc = f.type === 'csv' ? 'read_csv' : 'find_real_data';
                    return `df${i+1} = ${readFunc}("${f.name}")`;
                }).join('\n');

                // 预处理 AI 生成的代码，强制替换所有 pd.read_excel 和 pd.read_csv 调用
                let sanitizedCode = plan.code;
                
                // 第一步：处理 CSV 文件 - 所有 pd.read_excel("xxx.csv") 都替换为 read_csv("xxx.csv")
                fileManager.files.filter(f => f.type === 'csv').forEach(f => {
                    const safeName = f.name.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
                    sanitizedCode = sanitizedCode.replace(
                        new RegExp(`pd\\.read_excel\\s*\\(\\s*["']${safeName}["']`, 'g'),
                        `read_csv("${f.name}"`
                    );
                });
                
                // 第二步：处理 Excel 文件 - 所有 pd.read_csv("xxx.xlsx") 都替换为 find_real_data("xxx.xlsx")
                fileManager.files.filter(f => f.type === 'excel').forEach(f => {
                    const safeName = f.name.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
                    sanitizedCode = sanitizedCode.replace(
                        new RegExp(`pd\\.read_csv\\s*\\(\\s*["']${safeName}["']`, 'g'),
                        `find_real_data("${f.name}"`
                    );
                });

                // 调试：输出原始代码和预处理后的代码
                console.log("=== AI 原始代码 ===", plan.code);
                console.log("=== 预处理后代码 ===", sanitizedCode);

                const execStart = Date.now();
                await pyodide.runPythonAsync(`
import pandas as pd
from openpyxl import load_workbook
from openpyxl.drawing.image import Image as ExcelImg

# 文件类型映射（由 JS 生成）
FILE_TYPES = {
${fileManager.files.map((f, i) => `    "${f.name}": "${f.type}"`).join(',\n')}
}

def find_real_data(filename):
    """自适应表头探测：跳过空行 - 自动检测文件类型"""
    file_type = FILE_TYPES.get(filename, 'excel')
    if file_type == 'csv':
        return read_csv(filename)
    try:
        preview = pd.read_excel(filename, nrows=50, header=None)
        header_row = 0
        keywords = ['ID', 'id', '品名', '名称', '销售额', '利润', '单价', '数量']
        for i, row in preview.iterrows():
            if row.astype(str).str.contains('|'.join(keywords), case=False).any():
                header_row = i
                break
        df = pd.read_excel(filename, header=header_row)
        return df.dropna(how='all').dropna(axis=1, how='all')
    except:
        return pd.read_excel(filename)

def read_csv(filename):
    """读取 CSV 文件，自动检测编码"""
    encodings = ['utf-8', 'gbk', 'gb2312', 'latin1']
    for enc in encodings:
        try:
            df = pd.read_csv(filename, encoding=enc)
            return df.dropna(how='all').dropna(axis=1, how='all')
        except Exception as e:
            continue
    return pd.read_csv(filename)

def embed_chart(filename, cell, sheet_name="分析报告"):
    """物理插入图片"""
    wb = load_workbook("final.xlsx")
    ws = wb[sheet_name] if sheet_name in wb.sheetnames else wb.create_sheet(sheet_name)
    ws.add_image(ExcelImg(filename), cell)
    wb.save("final.xlsx")

# 批量读取并预处理
${readCode}

# 执行生成的业务逻辑
${sanitizedCode}
                `);
                const execEnd = Date.now();
                
                executionTimeMs = (fetchEnd - fetchStart) + (execEnd - execStart);
                
                // 统计图表数量
                
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

for (let i = 1; i <= 10; i++) {
                    try {
                        if(pyodide.FS.analyzePath(`c${i}.png`).exists) chartCount++;
                    } catch(e){}
                }

                renderOutput(plan);
                
                // 触发订阅弹窗事件（延迟由 workspace-auth.html 控制）
                window.dispatchEvent(new CustomEvent('jiandu:analysis-success'));
                
                // 保存分析历史到数据库
                await saveAnalysisHistory({
                    queryText: query,
                    filesJson: fileManager.files.map(f => ({ name: f.name, type: f.type })),
                    generatedCode: plan.code,
                    outputSummary: plan.explanation,
                    outputFile: 'final.xlsx',
                    chartCount: chartCount,
                    status: 'success',
                    executionTimeMs: executionTimeMs,
                    apiCost: apiCost
                });
                
                break; 
            } catch (e) {
                lastErr = e.message;
                attempt++;
                if (attempt >= MAX_FIX) throw e; 
            }
        }
    } catch (e) {
        executionTimeMs = Date.now() - analysisStartTime;
        
        // 详细错误日志
        console.error("详细错误:", e);
        const errorHtml = `
            <div style="padding:20px; color:#f43f5e; background:#fff1f2; border-radius:12px; border:1px solid #fecaca;">
                <b>⚠️ 研读中断：</b><br><br>
                <div style="background:#fef2f2; padding:15px; border-radius:8px; font-family:monospace; font-size:13px; white-space:pre-wrap; max-height:300px; overflow-y:auto;">
${e.message}
                </div>
                <br>
                <details style="font-size:13px; color:#64748b;">
                    <summary style="cursor:pointer;">🔍 查看调试信息</summary>
                    <div style="margin-top:10px; padding:10px; background:#1e293b; color:#e2e8f0; border-radius:8px; font-family:monospace; font-size:12px; overflow-x:auto;">
                        <b>文件列表:</b><br>
                        ${fileManager.files.map(f => `  - ${f.name} (${f.type})`).join('<br>')}<br><br>
                        <b>错误堆栈:</b><br>
                        ${e.stack || 'N/A'}
                    </div>
                </details>
                ${plan ? `<details style="font-size:13px; color:#64748b; margin-top:10px;">
                    <summary style="cursor:pointer;">📝 查看 AI 生成的代码</summary>
                    <div style="margin-top:10px; padding:10px; background:#1e293b; color:#e2e8f0; border-radius:8px; font-family:monospace; font-size:11px; overflow-x:auto; white-space:pre-wrap; max-height:400px; overflow-y:auto;">
${plan.code}
                    </div>
                </details>` : ''}
                <br>
                <small>💡 提示：如果连续报错，请尝试将任务分批次发送，或检查文件格式是否正确。</small>
            </div>`;
        document.getElementById('resultContent').innerHTML = errorHtml;
        
        // 保存失败的分析历史
        await saveAnalysisHistory({
            queryText: query,
            filesJson: fileManager.files.map(f => ({ name: f.name, type: f.type })),
            generatedCode: plan ? plan.code : null,
            outputSummary: null,
            outputFile: null,
            chartCount: 0,
            status: 'failed',
            errorMessage: e.message,
            executionTimeMs: executionTimeMs,
            apiCost: apiCost
        });
    } finally {
        isRunning = false;
        loader.classList.add('hidden');
    }
}

// --- 4. 增强版 Fetch ---
async function fetchAI(query, meta, key, error = null) {
    const controller = new AbortController();
    const id = setTimeout(() => controller.abort(), 60000);

    try {
        const res = await fetch("https://api.deepseek.com/v1/chat/completions", {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${key}`, 'Content-Type': 'application/json' },
            body: JSON.stringify({
                model: (() => {
                    try {
                        const u = JSON.parse(localStorage.getItem('jiandu_current_user') || '{}');
                        return u.model || localStorage.getItem('deepseek_model') || 'deepseek-chat';
                    } catch { return localStorage.getItem('deepseek_model') || 'deepseek-chat'; }
                })(),
                messages: [
                    {
                        role: "system", 
                        content: `You are a Python data analysis expert.
IMPORTANT RULES:
1. FILE READING: Use the correct function based on file type:
   - Files marked (EXCEL) or (.xlsx) -> find_real_data("filename")
   - Files marked (CSV) or (.csv) -> read_csv("filename")
   - NEVER use read_excel for CSV files.
2. MULTI-SHEET: Use pd.ExcelWriter('final.xlsx', engine='openpyxl').
3. TEXT ANSWERS FIRST: If user asks for a date, single value, count, sum, or simple fact -> set needs_chart=false and put the plain answer in the explanation field. Do NOT make a chart.
   - Only generate charts when user explicitly asks for trends, comparisons, distributions, or patterns.
4. CHART OUTPUT: Save charts as c1.png, c2.png and call embed_chart("c1.png", "A1").
5. OUTPUT FORMAT: Always JSON: {"code": "python code (English comments only)", "explanation": "answer in Chinese", "needs_chart": true/false}
                    },
                    {role: "user", content: `任务：${query}\n\n结构上下文：\n${meta}${error ? `\n修正报错：${error}` : ""}`}
                ],
                response_format: { type: "json_object" }
            }),
            signal: controller.signal
        });

        clearTimeout(id);
        if (res.status === 400) throw new Error("API 报错 (400)：请求过载。请分批提交任务。");
        if (res.status === 401) throw new Error("API Key 无效 (401)！");
        if (!res.ok) throw new Error(`云端响应异常 (${res.status})`);

        const d = await res.json();
        let txt = d.choices[0].message.content.replace(/```json/g, '').replace(/```/g, '').trim();
        return JSON.parse(txt);
    } catch (e) {
        if (e.name === 'AbortError') throw new Error("云端大脑研读超时，请检查网络或精简任务。");
        throw e;
    }
}

// --- 5. 渲染与元数据 ---
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


// --- 6. 事件绑定 ---
document.addEventListener('DOMContentLoaded', initPyodide);

async function renderFileList() {
    if (fileManager.files.length === 0) {
        document.getElementById('fileList').innerHTML = '<div style="color:#64748b; font-size:12px; padding:10px;">暂无文件</div>';
        document.getElementById('analyzeBtn').disabled = true;
        return;
    }
    
    const html = await Promise.all(fileManager.files.map(async (f, idx) => {
        let preview = '';
        try {
            const readFunc = f.type === 'csv' ? 'pd.read_csv' : 'pd.read_excel';
            const res = await pyodide.runPythonAsync(`
import pandas as pd; import json
try:
    df=${readFunc}("${f.name}", nrows=3)
except:
    df=${readFunc}("${f.name}", nrows=3, encoding='gbk')
json.dumps({"cols":list(df.columns.astype(str))[:5], "shape":list(df.shape)})
`);
            const info = JSON.parse(res);
            preview = `${info.shape[0]}行×${info.shape[1]}列 | ${info.cols.slice(0, 3).join(', ')}${info.cols.length > 3 ? '...' : ''}`;
        } catch(e) {}
        
        return `<div class="file-item">
            <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:6px;">
                <span style="font-weight:600;">${f.type === 'csv' ? '📄' : '📜'} ${f.name}</span>
                <button onclick="removeFile(${idx})" style="background:none; border:none; color:#ef4444; cursor:pointer; font-size:18px; padding:0 5px;" title="移除文件">×</button>
            </div>
            ${preview ? `<div style="font-size:11px; color:#94a3b8; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;">${preview}</div>` : ''}
        </div>`;
    }));
    
    document.getElementById('fileList').innerHTML = html.join('');
    document.getElementById('analyzeBtn').disabled = false;
}

window.removeFile = async (idx) => {
    const f = fileManager.files[idx];
    try {
        pyodide.FS.unlink(f.name);
    } catch(e) {}
    fileManager.files.splice(idx, 1);
    await renderFileList();
};

window.clearAllFiles = async () => {
    if (!confirm('确定要清除所有已上传的文件吗？')) return;
    for (let f of fileManager.files) {
        try {
            pyodide.FS.unlink(f.name);
        } catch(e) {}
    }
    fileManager.files = [];
    await renderFileList();
};

document.getElementById('fileInput').onchange = async (e) => {
    for (let f of e.target.files) {
        const b = await f.arrayBuffer();
        pyodide.FS.writeFile(f.name, new Uint8Array(b));
        const ext = f.name.toLowerCase().split('.').pop();
        fileManager.files.push({ 
            name: f.name, 
            type: ext === 'csv' ? 'csv' : 'excel' 
        });
    }
    await renderFileList();
};
document.getElementById('analyzeBtn').onclick = handleRequest;
