/**
 * 简牍 (Jian Du) - 核心逻辑稳定版 (支持 CSV)
 */

let pyodide = null;
let fileManager = { files: [] };
let isRunning = false;
let fontsOk = false;
const MAX_FIX = 3;

// --- 1. 引擎初始化 (带进度显示) ---
async function initPyodide() {
    const status = document.getElementById('envStatus');
    const dot = document.getElementById('envDot');
    try {
        const timeout = setTimeout(() => {
            status.textContent = "引擎启动过慢，请刷新页面";
            dot.style.background = "#f43f5e";
        }, 45000);

        status.textContent = "加载 Pyodide 核心...";
        pyodide = await loadPyodide({ indexURL: './pyodide/' });
        
        status.textContent = "安装数据科学包...";
        await pyodide.loadPackage(['pandas', 'matplotlib', 'micropip']);
        
        status.textContent = "配置 Excel 支持...";
        await pyodide.runPythonAsync(`
import micropip
await micropip.install(['openpyxl', 'Pillow'])
import matplotlib
matplotlib.use('Agg')
        `);
        
        clearTimeout(timeout);
        status.textContent = "引擎就绪";
        dot.style.background = "#36b37e";
    } catch (e) {
        status.textContent = "引擎启动失败";
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
import matplotlib
matplotlib.use('Agg')
import matplotlib.font_manager as fm
import matplotlib.pyplot as plt
from matplotlib import font_manager

# 添加自定义字体
font_manager.fontManager.addfont("custom_font.ttf")
prop = font_manager.FontProperties(fname="custom_font.ttf")
font_name = prop.get_name()

# 全局设置：字体 + unicode minus 修复
plt.rcParams.update({
    'font.family': font_name,
    'font.sans-serif': [font_name, 'DejaVu Sans'],
    'axes.unicode_minus': False,
    'axes.labelweight': 'normal',
    'axes.titlesize': 11,
    'axes.labelsize': 10,
    'xtick.labelsize': 9,
    'ytick.labelsize': 9,
    'legend.fontsize': 9,
    'figure.titlesize': 12,
    'font.size': 10
})
        `);
        fontsOk = true;
        fontBadge.textContent = "汉字引擎：已就绪";
    } catch (e) {
        fontBadge.textContent = "汉字引擎：不可用";
        console.warn("字体加载失败:", e);
    }
}

// --- 3. 核心执行逻辑 ---
async function handleRequest() {
    if (isRunning) return;
    const query = document.getElementById('userInput').value;
    // 从 localStorage 获取 API Key（设置页保存的）
    const apiKey = localStorage.getItem('deepseek_api_key');
    const loader = document.getElementById('loader');
    
    // 调试：输出 API Key 状态
    console.log("API Key 状态:", apiKey ? "已配置 (前 8 位：" + apiKey.substring(0, 8) + "...)" : "未配置");
    console.log("所有 localStorage 键:", Object.keys(localStorage));
    
    if (!apiKey) {
        return alert("⚠️ 请先在设置页面配置 API Key！\n\n1. 返回首页\n2. 点击左下角'设置'\n3. 填写 DeepSeek API Key 并保存\n\n💡 调试提示：按 F12 打开控制台查看详细日志");
    }
    if (fileManager.files.length === 0) {
        return alert("请先上传文件（Excel 或 CSV）");
    }

    isRunning = true;
    loader.classList.remove('hidden');

    try {
        let lastErr = null;
        let attempt = 0;
        let plan = null;  // 提升到外部作用域，方便错误处理时访问

        while (attempt < MAX_FIX) {
            try {
                const meta = await getMeta();
                plan = await fetchAI(query, meta, apiKey, lastErr);
                
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
                    // 匹配任何 pd.read_excel("file.csv") 调用
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
    # 检查文件类型映射
    file_type = FILE_TYPES.get(filename, 'excel')
    
    # 如果是 CSV 文件，使用 read_csv
    if file_type == 'csv':
        return read_csv(filename)
    
    # Excel 文件的处理逻辑
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
    # 如果都失败，用 pandas 默认
    return pd.read_csv(filename)

def embed_chart(filename, cell, sheet_name="分析报告"):
    """物理插入图片"""
    wb = load_workbook("final.xlsx")
    ws = wb[sheet_name] if sheet_name in wb.sheetnames else wb.create_sheet(sheet_name)
    ws.add_image(ExcelImg(filename), cell)
    wb.save("final.xlsx")

# 批量读取并预处理
${readCode}

# 执行生成的业务逻辑（已预处理，确保文件读取方式正确）
${sanitizedCode}
                `);

                renderOutput(plan);
                break; 
            } catch (e) {
                lastErr = e.message;
                attempt++;
                if (attempt >= MAX_FIX) throw e; 
            }
        }
    } catch (e) {
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
                <small>💡 提示：如果连续报错，请尝试将任务分批次发送，或检查文件格式是否正确。<br>系统已启用智能纠正功能，会自动修正 AI 生成的错误读取代码。</small>
            </div>`;
        document.getElementById('resultContent').innerHTML = errorHtml;
    } finally {
        isRunning = false;
        loader.classList.add('hidden');
    }
}

// --- 4. 增强版 Fetch ---
async function fetchAI(query, meta, key, error = null) {
    const controller = new AbortController();
    const id = setTimeout(() => controller.abort(), 60000); // 1 分钟超时

    try {
        const res = await fetch("https://api.deepseek.com/v1/chat/completions", {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${key}`, 'Content-Type': 'application/json' },
            body: JSON.stringify({
                model: "deepseek-chat",
                messages: [
                    {
                        role: "system", 
                        content: `你是一个专业的 Python 数据分析师，擅长商业数据分析和可视化。

【技术规则 - 必须严格遵守】
1. 文件读取（根据 meta 中的类型标记选择）：
   - (EXCEL) / .xlsx → find_real_data("文件名")
   - (CSV) / .csv → read_csv("文件名")
   - 绝对不能用 pd.read_excel 读 CSV，也不能用 pd.read_csv 读 Excel！
2. 写入 Excel：pd.ExcelWriter('final.xlsx', engine='openpyxl')
3. 图表：plt.savefig('c1.png') 保存后用 embed_chart('c1.png', 'A1') 嵌入
4. 输出格式（必须严格 JSON）：{"code": "python代码", "explanation": "分析说明", "needs_chart": true/false}
5. **答案优先规则**：当用户问"哪个/哪一天/谁/最高/最低/最大/最小"等具体问题时，必须在 explanation 的**最开头**用一句话给出明确答案，格式如：「答案：2024年3月15日（价格 128.5元）」，然后再写分析说明。图表可以有，但答案必须先文字说清楚。

【常见业务指标 - 理解其含义，不要算错】
- 毛利率 = (销售收入 - 成本) / 销售收入 × 100%
- 净利率 = 净利润 / 销售收入 × 100%
- 同比增长率 = (本期 - 去年同期) / 去年同期 × 100%
- 环比增长率 = (本期 - 上期) / 上期 × 100%
- 客单价 = 销售额 / 订单数（或成交笔数）
- 复购率 = 回头客订单数 / 总订单数 × 100%
- SKU = 库存量单位，通常对应 "品名/商品名称/产品名称" 列
- 库存周转率 = 销售成本 / 平均库存

【常用分析模式 - 遇到对应任务时使用】
- 销售分析：按月/季/年汇总，配合同比环比，生成趋势折线图
- 分类排名：groupby + sort_values(ascending=False)，柱状图展示 Top N
- 占比分析：value_counts 或 groupby 后算百分比，饼图展示
- ABC 分类：按销售额排序，累计占比 0-80% 为 A 类，80-95% 为 B 类，95-100% 为 C 类
- 用户分层（RFM）：按最近消费(Recency)、消费频率(Frequency)、消费金额(Monetary)打分
- 相关性分析：两列数值型变量用 df[['col1','col2']].corr()，热力图展示
- 异常值检测：用 3σ 原则（均值±3倍标准差之外为异常）或 IQR 法

【统计学基础 - 数据探索时优先计算】
- 均值（mean）、中位数（median）、众数（mode）
- 标准差（std）：越大说明波动越大
- 变异系数（CV）= 标准差 / 均值：用于比较不同量纲数据离散程度
- 偏度（skew）：>0 右偏（少数高值），<0 左偏（少数低值）
- 峰度（kurtosis）：>0 尖峰（数据集中），<0 平顶（数据分散）

【Python 代码习惯】
- 数据清洗：dropna / fillna / replace / astype 转换类型
- 日期列：pd.to_datetime(df['date'])，然后 dt.year / dt.month / dt.day 提取
- 分组聚合：df.groupby('列名').agg({'数值列': ['sum','mean','count']})
- 透视表：pd.pivot_table(df, values='数值', index='行', columns='列', aggfunc='sum')
- 排序：sort_values(by='列名', ascending=False) 默认降序
- 百分比：df['占比'] = df['数值'] / df['数值'].sum() * 100

遇到问题先思考：数据有没有空值？类型对不对？分组键选对了没有？再动手写代码。
                    },
                    {role: "user", content: `任务：${query}\n\n结构上下文：\n${meta}${error ? `\n修正报错：${error}` : ""}`}
                ],
                response_format: { type: "json_object" }
            }),
            signal: controller.signal
        });

        clearTimeout(id);
        if (res.status === 400) throw new Error("API 报错 (400)：请求过载。请分批提交任务。");
        if (res.status === 401) throw new Error("API Key 无效 (401)！\n\n请检查：\n1. API Key 是否正确填写（无空格）\n2. Key 是否已过期或被删除\n3. 账户是否有足够的余额\n\n获取新 Key: https://platform.deepseek.com");
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
async function getMeta() {
    let ctx = "";
    for (let f of fileManager.files) {
        const readFunc = f.type === 'csv' ? 'pd.read_csv' : 'pd.read_excel';
        const res = await pyodide.runPythonAsync(`
import pandas as pd; import json
try:
    df=${readFunc}("${f.name}", nrows=5)
except:
    df=${readFunc}("${f.name}", nrows=5, encoding='gbk')
json.dumps({"cols":list(df.columns.astype(str)), "rows":len(df)})
`);
        const info = JSON.parse(res);
        // 明确标记文件类型，让 AI 能清楚识别
        const typeLabel = f.type === 'csv' ? 'CSV' : 'EXCEL';
        ctx += `文件【${f.name}】类型:${typeLabel} | ${info.rows}行，列：${info.cols.join(', ')}\n`;
    }
    return ctx;
}

function renderOutput(plan) {
    const area = document.getElementById('resultContent');
    let html = `<div style="padding:15px; background:#f0f9ff; border-radius:12px; line-height:1.6;"><b>分析简报：</b><br>${plan.explanation}</div>`;
    
    for (let i = 1; i <= 10; i++) {
        const name = `c${i}.png`;
        try {
            if(pyodide.FS.analyzePath(name).exists) {
                const b = pyodide.FS.readFile(name);
                html += `<div style="margin-top:15px; text-align:center;"><img src="${URL.createObjectURL(new Blob([b], {type:'image/png'}))}" style="max-width:100%; border-radius:8px; border:1px solid #e2e8f0;"></div>`;
            }
        } catch(e){}
    }
    
    // 添加预览按钮和下载按钮
    html += `<div style="display:flex; gap:12px; margin-top:20px;">
        <button class="btn-run" style="flex:1; background:#3d5a80;" onclick="previewResult()">👁️ 在线预览报告</button>
        <button class="btn-run" style="flex:1; background:#059669;" onclick="downloadFile()">📥 下载 Excel 报告</button>
    </div>`;
    area.innerHTML = html;
}

// 预览结果（使用 SheetJS 读取 Excel 并显示）
window.previewResult = async () => {
    try {
        const data = pyodide.FS.readFile("final.xlsx");
        const blob = new Blob([data], {type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"});
        const arrayBuffer = await blob.arrayBuffer();
        const workbook = XLSX.read(arrayBuffer, {type: 'array'});
        
        let html = '<div style="padding:15px; background:#f8fafc; border-radius:12px;">';
        html += '<h3 style="color:#1e293b; margin-bottom:15px; font-size:16px;">📊 报告预览</h3>';
        
        // 遍历所有 Sheet
        workbook.SheetNames.forEach((sheetName, idx) => {
            const worksheet = workbook.Sheets[sheetName];
            const jsonData = XLSX.utils.sheet_to_json(worksheet, {header: 1});
            
            if (jsonData.length === 0) return;
            
            html += `<div style="margin-bottom:20px;">`;
            html += `<div style="background:#3d5a80; color:white; padding:8px 12px; border-radius:8px; font-weight:600; font-size:14px; margin-bottom:10px;">
                📄 Sheet ${idx + 1}: ${sheetName} (${jsonData.length} 行)</div>`;
            
            // 创建表格预览（最多显示前 50 行）
            const maxRows = Math.min(jsonData.length, 50);
            html += '<div style="max-height:300px; overflow:auto; border:1px solid #e2e8f0; border-radius:8px;">';
            html += '<table style="width:100%; border-collapse:collapse; font-size:12px;">';
            
            // 表头
            const headers = jsonData[0] || [];
            html += '<thead><tr style="background:#f1f5f9; position:sticky; top:0; z-index:10;">';
            headers.forEach(h => {
                html += `<th style="padding:8px 12px; text-align:left; border-bottom:2px solid #cbd5e1; color:#1e293b;">${h || ''}</th>`;
            });
            html += '</tr></thead><tbody>';
            
            // 数据行（跳过第一行表头）
            for (let i = 1; i < maxRows; i++) {
                const row = jsonData[i] || [];
                html += '<tr style="background:white;">';
                row.forEach(cell => {
                    html += `<td style="padding:8px 12px; border-bottom:1px solid #e2e8f0; color:#475569;">${cell !== undefined ? cell : ''}</td>`;
                });
                html += '</tr>';
            }
            
            html += '</tbody></table></div>';
            
            if (jsonData.length > 50) {
                html += `<div style="text-align:center; padding:10px; color:#64748b; font-size:12px;">
                    ... 共 ${jsonData.length} 行，仅显示前 50 行 ...</div>`;
            }
            
            html += '</div>';
        });
        
        html += '</div>';
        
        // 添加关闭按钮
        html += '<button class="btn-run" style="width:100%; margin-top:15px; background:#64748b;" onclick="closePreview()">✕ 关闭预览</button>';
        
        const area = document.getElementById('resultContent');
        area.innerHTML = html;
        
    } catch (e) {
        console.error("预览失败:", e);
        alert("预览失败：" + e.message + "\n\n请尝试下载后查看");
    }
};

// 关闭预览，恢复原始结果
window.closePreview = () => {
    // 重新渲染原始输出
    const query = document.getElementById('userInput').value;
    const apiKey = localStorage.getItem('deepseek_api_key');
    if (!apiKey || !query) {
        document.getElementById('resultContent').innerHTML = '<div class="welcome-box"><h3 style="color: #1e293b; margin-bottom: 15px;">欢迎使用简牍</h3><p style="color: #64748b; line-height: 1.8;">请上传 Excel 或 CSV 文件，并用日常语言描述您的处理逻辑。</p></div>';
        return;
    }
    // 简单处理：显示提示信息
    document.getElementById('resultContent').innerHTML = '<div style="padding:20px; text-align:center; color:#64748b;">预览已关闭<br><br><button class="btn-run" onclick="handleRequest()" style="margin-top:15px;">重新分析</button></div>';
};

window.downloadFile = () => {
    const data = pyodide.FS.readFile("final.xlsx");
    const blob = new Blob([data], {type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"});
    const a = document.createElement("a"); a.href = URL.createObjectURL(blob); a.download = "简牍分析报告.xlsx"; a.click();
};

// --- 6. 事件绑定 ---
document.addEventListener('DOMContentLoaded', initPyodide);

// 渲染文件列表（带移除按钮和预览）
async function renderFileList() {
    if (fileManager.files.length === 0) {
        document.getElementById('fileList').innerHTML = '<div style="color:#64748b; font-size:12px; padding:10px;">暂无文件</div>';
        document.getElementById('analyzeBtn').disabled = true;
        return;
    }
    
    const html = await Promise.all(fileManager.files.map(async (f, idx) => {
        // 获取文件预览数据
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

// 移除文件
window.removeFile = async (idx) => {
    const f = fileManager.files[idx];
    try {
        pyodide.FS.unlink(f.name);
    } catch(e) {}
    fileManager.files.splice(idx, 1);
    await renderFileList();
};

// 清除所有文件
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
        // 检测文件类型，标记读取方式
        const ext = f.name.toLowerCase().split('.').pop();
        fileManager.files.push({ 
            name: f.name, 
            type: ext === 'csv' ? 'csv' : 'excel' 
        });
    }
    await renderFileList();
};
document.getElementById('analyzeBtn').onclick = handleRequest;
