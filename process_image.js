const fs = require('fs');
const { createCanvas, loadImage } = require('canvas');

async function processImage() {
    try {
        // 读取原图
        const jpgBuffer = fs.readFileSync('言出法随.jpg');
        const img = await loadImage(jpgBuffer);
        
        console.log('📏 原图尺寸:', img.width, 'x', img.height);
        
        // 创建画布
        const canvas = createCanvas(img.width, img.height);
        const ctx = canvas.getContext('2d');
        
        // 绘制图片
        ctx.drawImage(img, 0, 0);
        
        // 获取像素数据
        const imageData = ctx.getImageData(0, 0, img.width, img.height);
        const data = imageData.data;
        
        // 处理像素：背景变透明
        for (let i = 0; i < data.length; i += 4) {
            const r = data[i];
            const g = data[i + 1];
            const b = data[i + 2];
            
            // 如果像素较亮（背景），变透明
            if (r > 200 && g > 200 && b > 200) {
                data[i + 3] = 0; // 完全透明
            }
            // 如果像素较暗（文字），保持
        }
        
        // 放回处理后的像素
        ctx.putImageData(imageData, 0, 0);
        
        // 导出为 PNG
        const pngBuffer = canvas.toBuffer('image/png');
        fs.writeFileSync('言出法随_transparent.png', pngBuffer);
        console.log('✅ 已生成透明 PNG: 言出法随_transparent.png');
        
        // 生成 base64
        const b64 = 'data:image/png;base64,' + pngBuffer.toString('base64');
        fs.writeFileSync('calligraphy_base64_new.txt', b64);
        console.log('✅ Base64 长度:', b64.length);
        
        // 更新 HTML
        let html = fs.readFileSync('index.html', 'utf8');
        const oldSrc = /src="data:image\/[^"]*"/;
        html = html.replace(oldSrc, `src="${b64}"`);
        fs.writeFileSync('index.html', html);
        console.log('✅ 已更新 index.html');
        
        console.log('✨ 完成！');
    } catch (err) {
        console.error('❌ 错误:', err.message);
        console.log('使用简单方法...');
        
        // 简单方法：直接读取 JPG 转 base64
        const jpgBuffer = fs.readFileSync('言出法随.jpg');
        const b64 = 'data:image/jpeg;base64,' + jpgBuffer.toString('base64');
        fs.writeFileSync('calligraphy_base64_simple.txt', b64);
        
        let html = fs.readFileSync('index.html', 'utf8');
        const oldSrc = /src="data:image\/[^"]*"/;
        html = html.replace(oldSrc, `src="${b64}"`);
        fs.writeFileSync('index.html', html);
        
        console.log('✅ 已更新（简单方法）');
    }
}

processImage();
