const fs = require('fs');

// 读取 base64 编码
const b64 = fs.readFileSync('calligraphy_base64.txt', 'utf8');
console.log('✅ Base64 长度:', b64.length);

// 读取 HTML
let html = fs.readFileSync('index.html', 'utf8');

// 替换图片 src
const oldSrc = /src="data:image\/[^"]*"/;
const newSrc = `src="${b64}"`;

if (oldSrc.test(html)) {
    html = html.replace(oldSrc, newSrc);
    fs.writeFileSync('index.html', html);
    console.log('✅ 已更新 index.html');
} else {
    console.log('❌ 未找到要替换的图片 src');
}

console.log('✨ 完成！');
