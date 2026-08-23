package data.methods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import data.methods.shaders.ShaderUtil;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;
import org.lwjgl.util.vector.Vector2f;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.EnumSet;

/**
 * Meng_BlackHolePlugin - 光线步进黑洞 + 引力透镜 + 背景扭曲 + 吸积盘渲染插件。
 *
 * <h3>功能概述</h3>
 * 在世界坐标指定位置渲染黑洞效果：
 * <ul>
 *   <li>光线步进模拟引力弯曲（每步向黑洞偏折光线方向）</li>
 *   <li>逃逸光线偏折量映射到屏幕UV偏移，扭曲实际游戏背景</li>
 *   <li>多层吸积盘（_Steps=12层噪声纹理 + 红移/蓝移 + 多普勒效果）</li>
 *   <li>辉光累积（沿光线路径累加）</li>
 * </ul>
 * 移植自 Shadertoy 黑洞着色器。
 *
 * <h3>依赖的API</h3>
 * <ul>
 *   <li>{@link ShaderUtil} - shader编译、VAO创建、modelMatrix构建、uniform设置</li>
 *   <li>LWJGL GL11/GL12/GL13/GL20 - OpenGL操作</li>
 * </ul>
 *
 * <h3>渲染管线</h3>
 * <pre>
 *   每帧: advance() 计时器 → render() 捕获屏幕 → 设置uniform → 绘制billboard
 *   顶点着色器: 自定义（输出 v_uv + v_screenUV）
 *   片段着色器: 光线步进(ray march) + 引力弯曲 + 屏幕采样 + 吸积盘 + 辉光
 * </pre>
 *
 * <h3>使用示例</h3>
 * <pre>
 *   Meng_BlackHolePlugin bh = new Meng_BlackHolePlugin(position, 500f);
 *   bh.setLifetime(2f);
 *   Global.getCombatEngine().addLayeredRenderingPlugin(bh);
 * </pre>
 *
 * <h3>生命周期</h3>
 * 调用 {@link #markExpired()} 标记过期，引擎随后调用 {@link #cleanup()} 释放GPU资源。
 */
public class Meng_BlackHolePlugin implements CombatLayeredRenderingPlugin {

    private static final org.apache.log4j.Logger LOG = Global.getLogger(Meng_BlackHolePlugin.class);

    /** 噪声纹理尺寸（吸积盘 + 星云共用） */
    private static final int NOISE_TEX_W = 256;
    private static final int NOISE_TEX_H = 256;


    // ==================== 可调参数 ====================

    /** 黑洞世界坐标位置 */
    private Vector2f position;
    /** 渲染半径（世界坐标单位），四边形边长 = radius * 2 */
    private float radius;
    /** Shader camera scale. Larger values show more of the black hole while keeping v_uv=(0.5, 0.5) as the center. */
    private float viewScale = 4f;
    /** Rendered quad size multiplier. The quad stays centered on position and only reserves room for lensing/glow. */
    private float billboardScale = 2.5f;

    // ==================== 内部状态 ====================

    private float timer = 0f;
    private boolean expired = false;
    private boolean glInitialized = false;
    /** 生命周期（秒），<=0 表示永不过期 */
    private float lifetime = 0f;

    // GPU资源
    private int shaderProgram;
    private ShaderUtil.VAOData vao;
    /** 噪声纹理（吸积盘 + 星云共用） */
    private int noiseTexId;
    /** 屏幕帧缓冲捕获纹理（每帧更新） */
    private int screenTexId;
    /** 屏幕捕获纹理实际宽高（匹配视口，非正方形） */
    private int screenTexW, screenTexH;
    /** 视口缓冲区（LWJGL2 要求用 IntBuffer 接收 glGetInteger） */
    private IntBuffer viewportBuf;

    // Uniform位置缓存
    private int uModelMatrixLoc, uSizeLoc;
    private int uTimeLoc;
    private int uNoiseTexLoc;
    /** 屏幕纹理 uniform 位置 */
    private int uScreenTexLoc;
    /** 屏幕分辨率 uniform 位置 */
    private int uScreenResLoc;
    /** Local shader camera scale uniform location. */
    private int uViewScaleLoc;
    /** Viewport zoom compensation uniform location for screen-background lensing offsets only. */
    private int uZoomScaleLoc;

    /**
     * 创建黑洞渲染插件。
     *
     * @param position 黑洞中心世界坐标
     * @param radius   渲染半径（世界坐标单位），四边形边长 = radius * 2
     */
    public Meng_BlackHolePlugin(Vector2f position, float radius) {
        this.position = new Vector2f(position);
        this.radius = radius;
    }

    // ==================== 参数设置 ====================

    /** 更新世界坐标位置 */
    public void setPosition(Vector2f pos) { this.position = new Vector2f(pos); }

    /** 更新渲染半径 */
    public void setRadius(float r) { this.radius = r; }

    /**
     * Updates the shader camera scale without moving the black hole center.
     * Larger values make the black hole appear smaller inside the same centered billboard.
     */
    public void setViewScale(float scale) { this.viewScale = Math.max(0.1f, scale); }

    /**
     * Updates the rendered quad size multiplier without changing the shader-space center.
     * Use this when lensing or glow needs more room around the black hole.
     */
    public void setBillboardScale(float scale) { this.billboardScale = Math.max(1f, scale); }

    /** 标记为过期，引擎将调用 cleanup() 释放资源 */
    public void markExpired() { this.expired = true; }

    /** 设置自动过期时间（秒），<=0 表示永不过期 */
    public void setLifetime(float seconds) { this.lifetime = seconds; }

    // ==================== 插件生命周期 ====================

    @Override
    public void init(CombatEntityAPI entity) {}

    @Override
    public void cleanup() {
        ShaderUtil.deleteProgram(shaderProgram);
        ShaderUtil.deleteVAO(vao);
        if (noiseTexId != 0) GL11.glDeleteTextures(noiseTexId);
        if (screenTexId != 0) GL11.glDeleteTextures(screenTexId);
        shaderProgram = 0;
        vao = null;
        noiseTexId = 0;
        screenTexId = 0;
    }

    @Override
    public boolean isExpired() { return expired; }

    @Override
    public void advance(float amount) {
        timer += amount;
        if (lifetime > 0f && timer >= lifetime) {
            expired = true;
        }
    }

    @Override
    public EnumSet<CombatEngineLayers> getActiveLayers() {
        return EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_LAYER);
    }

    @Override
    public float getRenderRadius() {
        return radius * 2f + 500f;
    }

    // ==================== 渲染 ====================

    @Override
    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        if (layer != CombatEngineLayers.ABOVE_SHIPS_LAYER) return;
        if (expired) return;

        initGL();
        if (shaderProgram <= 0) return;

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

        // 捕获当前帧缓冲到屏幕纹理（在绘制billboard之前）
        captureScreen();

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL20.glUseProgram(shaderProgram);

        // 模型矩阵：定位到世界坐标
        FloatBuffer modelMat = ShaderUtil.buildModelMatrix(position.x, position.y, 0f);
        GL20.glUniformMatrix4(uModelMatrixLoc, false, modelMat);

        // 尺寸：正方形广告牌，需要足够大以容纳引力透镜效果
        // billboard 尺寸应为半径的 3-4 倍，确保能看到完整的扭曲背景
        float fullSize = radius * billboardScale;
        GL20.glUniform2f(uSizeLoc, fullSize, fullSize);
        GL20.glUniform1f(uViewScaleLoc, viewScale);
        GL20.glUniform1f(uZoomScaleLoc, 1f / Math.max(0.001f, viewport.getViewMult()));

        // 时间
        GL20.glUniform1f(uTimeLoc, timer);

        // 屏幕分辨率（LWJGL2 用 IntBuffer 接收 glGetInteger）
        GL11.glGetInteger(GL11.GL_VIEWPORT, viewportBuf);
        int vpW = viewportBuf.get(2);
        int vpH = viewportBuf.get(3);
        GL20.glUniform2f(uScreenResLoc, (float) vpW, (float) vpH);

        // 首帧创建屏幕捕获纹理（匹配实际视口尺寸）
        if (screenTexId == 0) {
            createScreenTexture(vpW, vpH);
        }

        // 噪声纹理（纹理单元 0）- 吸积盘 + 星云共用
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, noiseTexId);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL20.glUniform1i(uNoiseTexLoc, 0);

        // 屏幕捕获纹理（纹理单元 1）- 背景扭曲
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, screenTexId);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL20.glUniform1i(uScreenTexLoc, 1);

        // 绘制
        ShaderUtil.drawVAOQuad(vao.vaoId);

        // 清理GL状态
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL20.glUseProgram(0);

        GL11.glPopAttrib();
    }

    // ==================== GL初始化 ====================

    /**
     * 延迟初始化 shader、VAO 和纹理（首帧 render 时调用）。
     * 不能在构造函数中初始化，因为 GL 上下文可能尚未就绪。
     */
    private void initGL() {
        if (glInitialized) return;
        glInitialized = true;

        shaderProgram = ShaderUtil.createShaderProgramFromFiles(
                "data/shaders/meng/black_hole.vert",
                "data/shaders/meng/black_hole.frag",
                "BlackHole");
        if (shaderProgram <= 0) {
            LOG.warn("[BlackHole] shader compile failed");
            return;
        }

        // 缓存 uniform 位置
        int[] locs = ShaderUtil.getUniformLocations(shaderProgram,
                "modelMatrix", "size", "u_viewScale", "u_zoomScale",
                "u_time", "noise_texture", "screen_texture", "screen_resolution");
        uModelMatrixLoc = locs[0];
        uSizeLoc = locs[1];
        uViewScaleLoc = locs[2];
        uZoomScaleLoc = locs[3];
        uTimeLoc = locs[4];
        uNoiseTexLoc = locs[5];
        uScreenTexLoc = locs[6];
        uScreenResLoc = locs[7];

        vao = ShaderUtil.createUniversalRectVAO();
        viewportBuf = BufferUtils.createIntBuffer(16); // LWJGL2 glGetInteger requires at least 16 elements
        createNoiseTexture();
        // 屏幕纹理在首次render()时根据实际视口尺寸创建，此处不创建
    }

    // ==================== 屏幕捕获 ====================

    /**
     * 创建屏幕捕获纹理（匹配视口宽高，每帧 glCopyTexSubImage2D 更新）。
     * 使用实际视口尺寸而非正方形POT，避免宽高比失真和位置偏移。
     * GL_CLAMP_TO_EDGE 包裹，避免采样到未定义区域。
     */
    private void createScreenTexture(int w, int h) {
        screenTexW = w;
        screenTexH = h;
        screenTexId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, screenTexId);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA,
                w, h, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    /**
     * 将当前帧缓冲内容复制到屏幕纹理。
     * 必须在绘制 billboard 之前调用，否则会捕获到 billboard 自身。
     */
    private void captureScreen() {
        if (screenTexId == 0) return;
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, screenTexId);
        GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0,
                screenTexW, screenTexH);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    // ==================== 噪声纹理生成 ====================

    /**
     * 创建 256x256 程序化值噪声纹理，吸积盘和星云背景共用。
     * X/Y轴均使用 GL_REPEAT 包裹以支持任意UV采样。
     */
    private void createNoiseTexture() {
        int w = NOISE_TEX_W;
        int h = NOISE_TEX_H;
        ByteBuffer data = BufferUtils.createByteBuffer(w * h * 4);

        for (int y = 0; y < h; y++) {
            float fy = (float) y / h;
            for (int x = 0; x < w; x++) {
                float fx = (float) x / w;
                // 多 octave 叠加值噪声
                float val = 0f;
                float amp = 1f;
                float freq = 4f;
                float totalAmp = 0f;
                for (int oct = 0; oct < 5; oct++) {
                    val += amp * wrappedNoise(fx * freq, fy * freq, w, h);
                    totalAmp += amp;
                    amp *= 0.5f;
                    freq *= 2f;
                }
                val /= totalAmp;
                int b = Math.max(0, Math.min(255, (int)(val * 255)));
                data.put((byte) b);
                data.put((byte) b);
                data.put((byte) b);
                data.put((byte) 255);
            }
        }
        data.flip();

        noiseTexId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, noiseTexId);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, w, h, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, data);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
    }

    /**
     * 2D值噪声，X/Y轴无缝环绕。
     */
    private static float wrappedNoise(float fx, float fy, int wrapW, int wrapH) {
        int ix = (int) Math.floor(fx);
        int iy = (int) Math.floor(fy);
        float fracX = fx - ix;
        float fracY = fy - iy;
        fracX = fracX * fracX * (3f - 2f * fracX);
        fracY = fracY * fracY * (3f - 2f * fracY);

        float a = wrappedHash(ix, iy, wrapW, wrapH);
        float b = wrappedHash(ix + 1, iy, wrapW, wrapH);
        float c = wrappedHash(ix, iy + 1, wrapW, wrapH);
        float d = wrappedHash(ix + 1, iy + 1, wrapW, wrapH);
        return mix(mix(a, b, fracX), mix(c, d, fracX), fracY);
    }

    private static float wrappedHash(int ix, int iy, int wrapW, int wrapH) {
        ix = ((ix % wrapW) + wrapW) % wrapW;
        iy = ((iy % wrapH) + wrapH) % wrapH;
        int n = ix + iy * 57;
        n = (n << 13) ^ n;
        return 1f - (float)((n * (n * n * 15731 + 789221) + 1376312589) & 0x7fffffff) / 1073741824f;
    }

    private static float mix(float a, float b, float t) { return a + (b - a) * t; }

}
