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
