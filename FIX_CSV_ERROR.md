# CSV 读取错误修复说明

## 问题现象

上传 CSV 文件后，点击分析时报错：
```
ValueError: Excel file format cannot be determined, you must specify an engine manually.
```

## 根本原因

AI 生成的 Python 代码中使用了 `pd.read_excel()` 来读取 CSV 文件，导致 pandas 无法识别文件格式。

示例错误代码：
```python
df1 = pd.read_excel("sales_data.csv")  # ❌ 错误！CSV 文件不能用 read_excel
```

正确代码应该是：
```python
df1 = read_csv("sales_data.csv")  # ✅ 正确
```

## 解决方案（已实施）

### 三层防护机制

#### 1️⃣ AI 提示词增强
在发送给 AI 的系统提示中明确说明：
```
重要规则：
1. 读取文件时必须根据文件类型选择函数：
   - 文件标记为 (EXCEL) 或 (.xlsx) → 使用 find_real_data("文件名")
   - 文件标记为 (CSV) 或 (.csv) → 使用 read_csv("文件名")
   - 绝对不能用 read_excel 读取 CSV 文件！
```

#### 2️⃣ JS 代码预处理（核心修复）
在 AI 生成代码后、执行前，自动扫描并替换错误的读取调用：

```javascript
// 所有 pd.read_excel("file.csv") 都替换为 read_csv("file.csv")
sanitizedCode = sanitizedCode.replace(
    /pd\.read_excel\s*\(\s*["']file\.csv["']/g,
    'read_csv("file.csv"'
);
```

#### 3️⃣ Python 运行时智能路由（双重保险）
完全覆盖 pandas 的读取函数，自动纠正错误调用：

```python
def _smart_read_excel(io, **kwargs):
    """智能 read_excel: 如果文件是 CSV 格式，自动切换到 read_csv"""
    if isinstance(io, str) and io in FILE_TYPES:
        if FILE_TYPES[io] == 'csv':
            print(f"⚠️ 自动纠正：检测到用 read_excel 读取 CSV 文件 '{io}'")
            return read_csv(io)
    return _original_read_excel(io, **kwargs)

pd.read_excel = _smart_read_excel
```

## 测试方法

### 方法 1：使用测试文件
1. 打开 `index.html`
2. 上传 `test_data.csv`（已提供的测试文件）
3. 输入："统计总销售额"
4. 点击执行

### 方法 2：使用自己的 CSV 文件
1. 准备一个 CSV 文件（确保扩展名是 `.csv`）
2. 上传到系统
3. 输入分析需求
4. 执行并查看结果

### 方法 3：混合文件测试
1. 同时上传 `test_data.csv` 和一个 Excel 文件
2. 输入："合并两个文件并比对数据"
3. 执行并查看结果

## 验证修复成功

成功的标志：
- ✅ 不再出现 "Excel file format cannot be determined" 错误
- ✅ 控制台显示 "=== AI 原始代码 ===" 和 "=== 预处理后代码 ==="
- ✅ 如果 AI 生成错误代码，控制台会显示 "⚠️ 自动纠正：..."
- ✅ 正常输出分析结果和图表

## 调试技巧

### 查看 AI 生成的代码
1. 如果报错，错误面板底部会有 "📝 查看 AI 生成的代码" 按钮
2. 点击展开，查看 AI 实际生成的代码
3. 检查是否有 `pd.read_excel("xxx.csv")` 这样的错误调用

### 查看预处理日志
1. 按 F12 打开浏览器开发者工具
2. 切换到 Console 标签
3. 查看 "=== AI 原始代码 ===" 和 "=== 预处理后代码 ===" 的对比
4. 确认错误的读取调用已被替换

### 查看运行时纠正
1. 在 Console 中查找 "⚠️ 自动纠正：" 消息
2. 这表示 Python 运行时检测并纠正了错误的读取调用

## 如果问题仍然存在

### 检查清单
- [ ] 文件扩展名是否正确（`.csv` 还是 `.xlsx`）
- [ ] 文件是否真的损坏（用 Excel 或记事本打开检查）
- [ ] 浏览器控制台是否有 JavaScript 错误
- [ ] Pyodide 引擎是否正常启动（左侧显示"引擎就绪"）

### 获取帮助
1. 截图错误面板
2. 展开 "查看 AI 生成的代码"
3. 复制控制台日志
4. 联系技术支持

---

**修复版本:** 2.0.2  
**修复日期:** 2026-03-16  
**修复者:** 小灵 (Xiao Ling)
