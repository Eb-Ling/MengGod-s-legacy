package data;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.nio.ByteBuffer;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class MaskGradientRender implements MyRenderTool.MyRender, MyRenderTool.InitializableEffect {
    // 存储蒙版贴图和渐变贴图的 OpenGL 纹理 ID
    private int maskTextureId;
    private int gradientTextureId;
    // 存储两张贴图的原始尺寸，用于计算渲染范围
    private int maskWidth;
    private int maskHeight;
    private int gradientWidth;
    private int gradientHeight;
    // 计时器，用于驱动流水动画
    private float timer = 0f;
    private Vector2f location;
    // 定义贴图的绝对路径
    private static final String MASK_PATH = "D:\\Starsector\\mods\\MengGod's legacy\\graphics\\fx\\Meng_logo1.png";
    private static final String GRADIENT_PATH = "D:\\Starsector\\mods\\MengGod's legacy\\graphics\\fx\\Meng_MBoss_Beam.png";

    public MaskGradientRender() {
        // 在构造时加载两张贴图
        loadTexture(MASK_PATH, true);
        loadTexture(GRADIENT_PATH, false);
        this.location = new Vector2f(0f, 0f);
    }

    @Override
    public void initialize(float x, float y) {
        location.x = x;
        location.y = y;
        timer = 0f;
    }

    private void loadTexture(String path, boolean isMask) {
        try {
            java.io.File textureFile = new java.io.File(path);
            if (!textureFile.exists()) return;
            
            // 使用 ImageIO 读取 PNG 文件到内存
            BufferedImage image = ImageIO.read(textureFile);
            if (image == null) return;
            
            int width = image.getWidth();
            int height = image.getHeight();
            int[] pixels = new int[width * height];
            // 获取所有像素的 ARGB 数据
            image.getRGB(0, 0, width, height, pixels, 0, width);

            // 创建一个字节缓冲区，用于存放 OpenGL 需要的 RGBA 格式数据
            ByteBuffer buffer = org.lwjgl.BufferUtils.createByteBuffer(width * height * 4);
            
            // 【关键】垂直翻转循环：因为图片文件的 (0,0) 在左上角，而 OpenGL 的 (0,0) 在左下角
            for (int y = height - 1; y >= 0; y--) {
                for (int x = 0; x < width; x++) {
                    int pixel = pixels[y * width + x];
                    // 将 ARGB 拆解为 R, G, B, A 四个字节存入缓冲区
                    buffer.put((byte) ((pixel >> 16) & 0xFF));
                    buffer.put((byte) ((pixel >> 8) & 0xFF));
                    buffer.put((byte) (pixel & 0xFF));
                    buffer.put((byte) ((pixel >> 24) & 0xFF));
                }
            }
            buffer.flip(); // 切换缓冲区模式为读取模式
            
            // 生成一个唯一的纹理 ID
            int textureId = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
            // 设置纹理过滤方式：线性插值，让缩放更平滑
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            // 设置环绕模式：REPEAT 允许我们通过偏移 UV 坐标实现无缝滚动
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
            // 将像素数据上传到显存
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
            
            // 根据类型保存 ID 和尺寸
            if (isMask) {
                maskTextureId = textureId;
                maskWidth = width;
                maskHeight = height;
            } else {
                gradientTextureId = textureId;
                gradientWidth = width;
                gradientHeight = height;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void advance(float amount) {
        timer += amount; // 累加时间，用于动画计算
    }


    @Override
    public boolean isExpired() {
        return false; // 该特效永久存在
    }
    
    @Override
    public void render() {
        if (maskTextureId <= 0 || gradientTextureId <= 0) return;

        // 计算渲染中心点和半宽/半高
        float x = location.x;
        float y = location.y;
        float w = maskWidth * 0.5f;
        float h = maskHeight * 0.5f;

        // === 核心步骤 1: 渲染蒙版到模板缓冲区 (Stencil) ===
        GL11.glEnable(GL11.GL_STENCIL_TEST); // 开启模板测试功能

        // 【预处理】先画一个比 Logo 稍大的透明矩形，把该区域的 Stencil 强制清零
        // 这样可以防止上一帧残留的数据干扰当前形状
        GL11.glColorMask(false, false, false, false); // 关闭颜色写入，只写 Stencil
        GL11.glStencilFunc(GL11.GL_ALWAYS, 0, 0xFF);  // 无论何时都通过
        GL11.glStencilOp(GL11.GL_REPLACE, GL11.GL_REPLACE, GL11.GL_REPLACE); // 将通过的区域设为 0
        GL11.glDisable(GL11.GL_TEXTURE_2D); // 纯色矩形不需要纹理
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x - w - 10, y - h - 10); GL11.glVertex2f(x + w + 10, y - h - 10);
        GL11.glVertex2f(x + w + 10, y + h + 10); GL11.glVertex2f(x - w - 10, y + h + 10);
        GL11.glEnd();

        // 【正式绘制蒙版】配置：将通过测试的像素 Stencil 值设为 1
        GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_REPLACE, GL11.GL_REPLACE, GL11.GL_REPLACE);

        // 开启 Alpha 测试：只有透明度大于 0.05 的像素才会被绘制（即写入 Stencil）
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.05f);

        // 绑定并绘制蒙版图
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, maskTextureId);
        GL11.glBegin(GL11.GL_QUADS);
        // 注意：这里的坐标顺序必须与纹理坐标一一对应，否则会导致翻转
        GL11.glTexCoord2f(0f, 0f); GL11.glVertex2f(x - w, y - h);  // 左下
        GL11.glTexCoord2f(1f, 0f); GL11.glVertex2f(x + w, y - h);  // 右下
        GL11.glTexCoord2f(1f, 1f); GL11.glVertex2f(x + w, y + h);  // 右上
        GL11.glTexCoord2f(0f, 1f); GL11.glVertex2f(x - w, y + h);  // 左上
        GL11.glEnd();

        GL11.glDisable(GL11.GL_ALPHA_TEST); // 关闭 Alpha 测试

        // === 核心步骤 2: 根据模板缓冲区渲染渐变图 ===
        
        GL11.glColorMask(true, true, true, true); // 恢复颜色写入
        
        // 配置：只有当 Stencil 值为 1 时（即刚才蒙版图形的形状内）才允许绘制
        GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP); // 保持 Stencil 值不变

        GL11.glEnable(GL11.GL_BLEND); // 开启混合，让渐变色看起来自然
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, gradientTextureId);
        GL11.glColor4f(1f, 1f, 1f, 1f); // 使用白色以完全保留渐变图的原始色彩

        // 计算流水偏移量：随时间不断增大，配合 REPEAT 模式实现循环流动
        float flowOffset = timer * 0.5f;

        GL11.glBegin(GL11.GL_QUADS);
        // 同样保持严格的坐标对应，并在 S 轴（横向）加上 flowOffset
        GL11.glTexCoord2f(0f + flowOffset, 0f); GL11.glVertex2f(x - w, y - h);  // 左下
        GL11.glTexCoord2f(1f + flowOffset, 0f); GL11.glVertex2f(x + w, y - h);  // 右下
        GL11.glTexCoord2f(1f + flowOffset, 1f); GL11.glVertex2f(x + w, y + h);  // 右上
        GL11.glTexCoord2f(0f + flowOffset, 1f); GL11.glVertex2f(x - w, y + h);  // 左上
        GL11.glEnd();

        // 清理 OpenGL 状态，避免影响后续其他渲染类的绘制
        GL11.glDisable(GL11.GL_STENCIL_TEST);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
    }
}
