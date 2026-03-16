"""
书法图片处理工具
将 言出法随.jpg 转换成最优的 PNG 格式
"""

from PIL import Image, ImageFilter, ImageOps
import os

def process_calligraphy():
    # 检查文件是否存在
    jpg_file = "言出法随.jpg"
    png_file = "言出法随.png"
    
    if not os.path.exists(jpg_file):
        print(f"❌ 找不到文件：{jpg_file}")
        print("请确保 言出法随.jpg 在当前目录下")
        return
    
    print(f"✅ 找到文件：{jpg_file}")
    
    # 打开图片
    img = Image.open(jpg_file)
    
    # 获取原始尺寸
    original_width, original_height = img.size
    print(f"📏 原始尺寸：{original_width} x {original_height}")
    
    # 1. 转换为 RGBA（支持透明通道）
    img = img.convert('RGBA')
    
    # 2. 增强对比度，让文字更清晰
    # 获取像素数据
    datas = img.getdata()
    newData = []
    
    for item in datas:
        # item = (R, G, B, A)
        # 如果像素较暗（文字部分），保持不透明
        # 如果像素较亮（背景部分），变成透明
        if item[0] < 200 and item[1] < 200 and item[2] < 200:
            # 文字部分 - 保持
            newData.append((255, 255, 255, 255))  # 白色文字
        else:
            # 背景部分 - 透明
            newData.append((255, 255, 255, 0))  # 透明
    
    img.putdata(newData)
    
    # 3. 裁剪掉多余的透明边缘
    img = ImageOps.crop(img, border=0)
    
    # 4. 调整到合适大小（宽度 400px，高度自适应）
    target_width = 400
    aspect_ratio = img.height / img.width
    new_height = int(target_width * aspect_ratio)
    img = img.resize((target_width, new_height), Image.Resampling.LANCZOS)
    
    print(f"📏 处理后尺寸：{target_width} x {new_height}")
    
    # 5. 保存为 PNG
    img.save(png_file, 'PNG', quality=95)
    print(f"✅ 已保存：{png_file}")
    
    # 6. 生成 base64 编码（用于直接嵌入 HTML）
    import base64
    with open(png_file, 'rb') as f:
        img_data = f.read()
        base64_str = base64.b64encode(img_data).decode('utf-8')
        
        # 保存 base64 到文本文件
        with open("calligraphy_base64.txt", 'w', encoding='utf-8') as f2:
            f2.write(f'data:image/png;base64,{base64_str}')
        
        print("✅ 已生成 base64 编码：calligraphy_base64.txt")
        print(f"📊 base64 长度：{len(base64_str)} 字符")
    
    print("\n" + "="*50)
    print("✨ 处理完成！")
    print("="*50)
    print(f"\n文件列表:")
    print(f"  输入：{jpg_file}")
    print(f"  输出：{png_file}")
    print(f"  Base64: calligraphy_base64.txt")
    print(f"\n使用方法:")
    print(f"  1. 将 {png_file} 放到 D:\\简牍_V2.0\\ 目录")
    print(f"  2. 或者复制 calligraphy_base64.txt 中的内容")
    print(f"     替换 index.html 中的 src 属性")

if __name__ == "__main__":
    process_calligraphy()
