from PIL import Image
import base64
import io

# 打开图片
img = Image.open('言出法随.jpg')
img = img.convert('RGBA')

# 获取像素
datas = list(img.getdata())
newData = []

for item in datas:
    # 背景变透明
    if item[0] > 180 and item[1] > 180 and item[2] > 180:
        newData.append((255, 255, 255, 0))  # 透明
    else:
        newData.append((255, 255, 255, 255))  # 白色文字

img.putdata(newData)

# 保存为 PNG
output = io.BytesIO()
img.save(output, 'PNG')
print('✅ 透明 PNG 已生成')

# 生成 base64
b64 = 'data:image/png;base64,' + base64.b64encode(output.getvalue()).decode()
print('✅ Base64 长度:', len(b64))

# 保存 base64
with open('calligraphy_b64_new.txt', 'w', encoding='utf-8') as f:
    f.write(b64)

print('✅ 完成！')
