const fs = require('fs');

let html = fs.readFileSync('index.html', 'utf8');

// 检查是否有底部链接
if (!html.includes('工作空间') || !html.includes('设置')) {
    // 添加底部链接
    const bottomLinks = `
        <!-- 底部链接 -->
        <div style="margin-top:auto; padding-top:20px; border-top:1px solid #3f414d">
            <a href="workspace.html" style="color:#94a3b8; text-decoration:none; font-size:13px; padding:8px 12px; border-radius:8px; display:flex; align-items:center; gap:8px; margin-bottom:10px; transition:all 0.2s">
                <span>←</span>
                <span>工作空间</span>
            </a>
            <a href="settings.html" style="color:#94a3b8; text-decoration:none; font-size:13px; padding:8px 12px; border-radius:8px; display:flex; align-items:center; gap:8px; transition:all 0.2s">
                <span>⚙️</span>
                <span>设置</span>
            </a>
        </div>
    </aside>`;
    
    html = html.replace('</aside>', bottomLinks);
    fs.writeFileSync('index.html', html);
    console.log('✅ 已添加底部链接');
} else {
    console.log('✅ 底部链接已存在');
}
