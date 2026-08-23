package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.graphics.SpriteAPI;
import data.methods.shaders.ShaderUtil;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;
import org.lwjgl.util.vector.Vector2f;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

/**
 * Meng_HexShieldPlugin - 六边形护盾渲染插件。
 *
 * <h3>功能概述</h3>
 * 在舰船护盾表面渲染六边形蜂窝纹理，受击时产生从命中点向外扩散的波纹动画。
 * 采用现代管线方案：VAO管理顶点、modelMatrix uniform传递变换矩阵、
 * SSBO传递无限数量的命中事件数据，单次直接渲染到屏幕（无FBO中间步骤）。
 *
 * <h3>依赖的API</h3>
 * <ul>
 *   <li>{@link ShaderUtil} - shader编译、VAO创建、modelMatrix构建、SSBO管理、uniform设置</li>
 *   <li>LWJGL GL11/GL13/GL15/GL20/GL30/GL43 - OpenGL操作</li>
 *   <li>Starsector CombatLayeredRenderingPlugin - 渲染插件生命周期</li>
 * </ul>
 *
 * <h3>渲染管线</h3>
 * <pre>
 *   每帧: advance() 清理过期命中事件 → render() 上传SSBO → 单次直接渲染
 *   顶点着色器: data/shaders/meng/common.vert (modelMatrix + size uniform)
 *   片段着色器: 六边形蜂窝SDF + 球面投影 + 波纹动画(SSBO命中事件) + 遮罩纹理
 * </pre>
 *
 * <h3>提取来源</h3>
 * <ul>
 *   <li>原 Meng_HexShieldPlugin (FBO二步渲染 + 3波槽固定数组)</li>
 *   <li>{@code src/example/PLSP_EventDisturbVisual.java} (VAO+SSBO+modelMatrix范式参考)</li>
 * </ul>
 */
public class Meng_HexShieldPlugin implements CombatLayeredRenderingPlugin {

    private static final org.apache.log4j.Logger LOG = Global.getLogger(Meng_HexShieldPlugin.class);

    /** SSBO中每个命中事件的float数量: uvX, uvY, triggerTime, damage */
    private static final int HIT_EVENT_FLOATS = 4;
    /** SSBO binding point，对应片段着色器 layout(std430, binding = 1) */
    private static final int SSBO_BINDING = 1;
    /** 脉冲事件SSBO中每个事件的float数量: uvX, uvY, triggerTime, intensity */
    private static final int PULSE_EVENT_FLOATS = 4;
    /** 脉冲事件SSBO binding point，对应片段着色器 layout(std430, binding = 2) */
    private static final int PULSE_SSBO_BINDING = 2;
    /** 自动脉冲触发间隔（秒），Java侧常量 */
    private static final float AUTO_PULSE_INTERVAL = 3.0f;
    /** 脉冲波纹生命周期（秒），参数化控制 - 修改此值可调整脉冲波纹持续时间 */
    private static final float AUTO_PULSE_LIFETIME = 2.5f;

    private ShipAPI ship;
    private float timer = 0f;
    private boolean expired = false;
    private boolean initOnce = false;
    private boolean glInitialized = false;
    private final boolean renderEnabled;
    private float shieldRadius;

    // Shader
    private int shaderProgram;
    private ShaderUtil.VAOData vao;
    private int uModelMatrixLoc, uSizeLoc;
    private int uTimeLoc, uHexSizeLoc, uBgColorLoc;
    private int uShieldFacingLoc, uShieldArcLoc;
    private int uMaskTextureLoc;
    private int uPulseMaskTextureLoc;
    private int uHitEventCountLoc;
    /** 护盾关闭渐出透明度 [0,1]，由 advance() 中的 fadeAlpha/shieldOffTimer 驱动 */
    private int uFadeAlphaLoc;
    /** 脉冲事件数量 uniform location */
    private int uPulseEventCountLoc;
    /** 脉冲波纹生命周期（秒）uniform location */
    private int uPulseLifetimeLoc;

    // SSBO
    private int ssboId = 0;
    private int ssboCapacity = 0;
    private FloatBuffer ssboUploadBuffer = null;
    private int ssboUploadCapacity = 0;

    // Pulse SSBO (separate from hit events, drives gap-glow ripple)
    private int pulseSsboId = 0;
    private int pulseSsboCapacity = 0;
    private FloatBuffer pulseSsboUploadBuffer = null;
    private int pulseSsboUploadCapacity = 0;

    // Hit events (dynamic list replacing fixed 3-slot wave arrays)
    private final List<HitEvent> hitEvents = new ArrayList<>();
    /** 脉冲事件列表 - 驱动缝隙发光波纹，与命中波纹完全独立 */
    private final List<HitEvent> pulseEvents = new ArrayList<>();
    /** 自动脉冲计时器，每隔 AUTO_PULSE_INTERVAL 触发一次中心脉冲 */
    private float autoPulseTimer = 0f;

    // Shield mask sprite
    private SpriteAPI shieldSprite;
    /** 脉冲专用蒙版贴图 Meng_ShieldMask，直接乘脉冲发光强度 */
    private SpriteAPI pulseMaskSprite;

    // Damage modifier
    private float lastHitTime = -0.3f;
    private MyDamageDealtModifier damageModifier;

    private float fadeAlpha = 0f;
    private float shieldOffTimer = 0f;
    private static final float SHIELD_FADE_DURATION = 0.4f;
    private float lastActiveArcRad = 0f;
    private float lastShieldFacingDeg = 0f;

    public Meng_HexShieldPlugin(ShipAPI ship) {
        this(ship, true);
    }

    /** Creates a logic-only instance when renderEnabled is false. */
    public Meng_HexShieldPlugin(ShipAPI ship, boolean renderEnabled) {
        this.ship = ship;
        this.renderEnabled = renderEnabled;
        this.shieldRadius = ship.getShieldRadiusEvenIfNoShield();

        try {
            this.shieldSprite = Global.getSettings().getSprite("fx", "Meng_Shield");
            LOG.info("[HexShield] Sprite loaded successfully for: " + ship.getName());
        } catch (Exception e) {
            LOG.error("[HexShield] Failed to load sprite: " + e.getMessage(), e);
            this.shieldSprite = null;
        }
        try {
            this.pulseMaskSprite = Global.getSettings().getSprite("fx", "Meng_ShieldMask");
            LOG.info("[HexShield] Pulse mask sprite loaded successfully");
        } catch (Exception e) {
            LOG.error("[HexShield] Failed to load pulse mask sprite: " + e.getMessage(), e);
            this.pulseMaskSprite = null;
        }
    }

    private void initGL() {
        if (glInitialized) return;
        createShaderProgram();
        createBuffers();
        cacheUniformLocations();
        ssboId = ShaderUtil.createSSBO();
        pulseSsboId = ShaderUtil.createSSBO();
        glInitialized = true;
    }

    // ==================== SHADER ====================

    /**
     * 从模组资源目录加载并编译顶点、片段着色器。
     * 片段着色器包含六边形蜂窝SDF、球面投影、SSBO命中事件波纹动画和遮罩纹理混合。
     *
     * <p>调用路径: {@link #initGL()} 内部调用，仅在首次render时触发。</p>
     */
    private void createShaderProgram() {
        shaderProgram = ShaderUtil.createShaderProgramFromFiles(
                "data/shaders/meng/common.vert",
                "data/shaders/meng/hex_shield.frag",
                "HexShield");
        if (shaderProgram > 0) {
            LOG.info("[HexShield] Shader compiled and linked successfully");
        }
    }
    /**
     * 创建通用矩形VAO，替代旧的VBO+IBO方案。
     * 调用路径: {@link #initGL()} 内部调用。
     */
    private void createBuffers() {
        vao = ShaderUtil.createUniversalRectVAO();
    }

    /**
     * 缓存所有uniform变量的location，包括新增的modelMatrix、size和u_hitEventCount。
     * 调用路径: {@link #initGL()} 内部调用。
     */
    private void cacheUniformLocations() {
        if (shaderProgram <= 0) return;
        int[] locs = ShaderUtil.getUniformLocations(shaderProgram,
                "modelMatrix", "size",
                "u_time", "u_hexSize", "u_bgColor",
                "u_shieldFacing", "u_shieldArc", "u_maskTexture",
                "u_hitEventCount",
                "u_fadeAlpha",
                "u_pulseEventCount", "u_pulseLifetime",
                "u_pulseMaskTexture");
        uModelMatrixLoc = locs[0];
        uSizeLoc = locs[1];
        uTimeLoc = locs[2];
        uHexSizeLoc = locs[3];
        uBgColorLoc = locs[4];
        uShieldFacingLoc = locs[5];
        uShieldArcLoc = locs[6];
        uMaskTextureLoc = locs[7];
        uHitEventCountLoc = locs[8];
        uFadeAlphaLoc = locs[9];
        uPulseEventCountLoc = locs[10];
        uPulseLifetimeLoc = locs[11];
        uPulseMaskTextureLoc = locs[12];
    }

    // ==================== CombatLayeredRenderingPlugin ====================

    @Override
    public void init(CombatEntityAPI entity) {}

    /**
     * 清理所有GPU资源: shader program、VAO、SSBO、damage listener。
     * 调用路径: 引擎在插件过期或被移除时自动调用。
     */
    @Override
    public void cleanup() {
        ShaderUtil.cleanupAll(shaderProgram, vao, ssboId);
        if (pulseSsboId > 0) {
            GL15.glDeleteBuffers(pulseSsboId);
        }
        shaderProgram = 0;
        vao = null;
        ssboId = 0;
        pulseSsboId = 0;
        if (ship != null && damageModifier != null) {
            ship.removeListener(damageModifier);
            damageModifier = null;
        }
    }

    @Override
    public boolean isExpired() {
        return expired;
    }

    public void markExpired() {
        expired = true;
    }

    @Override
    public void advance(float amount) {
        if (expired) return;
        if (!ship.isAlive()) { expired = true; return; }

        timer += amount;

        // Clear expired hit events
        Iterator<HitEvent> iter = hitEvents.iterator();
        while (iter.hasNext()) {
            HitEvent event = iter.next();
            float age = timer - event.triggerTime;
            float dmgScale = 0.3f + Math.min(event.damage / 100f, 1f) * 0.6f
                    + Math.min(Math.max(event.damage - 100f, 0f) / 400f, 1f) * 0.3f;
            if (age >= dmgScale * 0.5f) {
                iter.remove();
            }
        }

        // Clear expired pulse events (lifetime = u_pulseLifetime equivalent, default 2.0s)
        float pulseLifetime = AUTO_PULSE_LIFETIME;
        Iterator<HitEvent> pIter = pulseEvents.iterator();
        while (pIter.hasNext()) {
            HitEvent event = pIter.next();
            if (timer - event.triggerTime >= pulseLifetime) {
                pIter.remove();
            }
        }

        // Auto-pulse: trigger center ripple at fixed interval
        autoPulseTimer += amount;
        if (autoPulseTimer >= AUTO_PULSE_INTERVAL) {
            autoPulseTimer -= AUTO_PULSE_INTERVAL;
            pulseEvents.add(new HitEvent(0.5f, 0.5f, timer, 1.0f));
        }

        ShieldAPI shield = ship.getShield();
        if (shield != null && shield.isOn()) {
            fadeAlpha = 1f;
            shieldOffTimer = 0f;
            lastActiveArcRad = (float) Math.toRadians(shield.getActiveArc());
            lastShieldFacingDeg = shield.getFacing();
        } else if (fadeAlpha > 0f) {
            shieldOffTimer += amount;
            fadeAlpha = Math.max(0f, 1f - shieldOffTimer / SHIELD_FADE_DURATION);
        }

        if (!initOnce) {
            initOnce = true;
        }

        if (damageModifier == null) {
            damageModifier = new MyDamageDealtModifier(this);
            ship.addListener(damageModifier);
        }
    }

    @Override
    public EnumSet<CombatEngineLayers> getActiveLayers() {
        return EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_LAYER);
    }

    @Override
    public float getRenderRadius() {
        return 10000000f;
    }

    // ==================== RENDER ====================

    /**
     * 单次直接渲染六边形护盾到屏幕。
     * 替代旧的FBO二步渲染: 无需中间纹理，直接通过modelMatrix定位到世界坐标。
     *
     * <p>调用路径: {@link #render(CombatEngineLayers, ViewportAPI)} 内部调用。</p>
     *
     * @param shieldR         护盾当前半径（世界坐标单位）
     * @param shieldFacingDeg 护盾朝向角度（度）
     * @param shieldArcRad    护盾弧面角度（弧度）
     */
    private void renderShield(float shieldR, float shieldFacingDeg, float shieldArcRad) {
        uploadHitEvents();

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL20.glUseProgram(shaderProgram);

        // Model matrix: position at ship location, rotate to shield facing
        Vector2f shipLoc = ship.getLocation();
        FloatBuffer modelMat = ShaderUtil.buildModelMatrix(shipLoc.x, shipLoc.y, shieldFacingDeg - 90f);
        GL20.glUniformMatrix4(uModelMatrixLoc, false, modelMat);

        // Size: shield render area (slightly larger than shield radius)
        // 2.125 : 补偿 BoxUtil UBO gameViewport 与旧固定管线矩阵栈的微小缩放差异
        float fullSize = shieldR * 2.125f;
        GL20.glUniform2f(uSizeLoc, fullSize, fullSize);

        // Standard uniforms
        GL20.glUniform1f(uTimeLoc, timer);
        GL20.glUniform1f(uHexSizeLoc, 0.05f);
        GL20.glUniform3f(uBgColorLoc, 0.56f, 0.0f, 1.0f);

        // Shield arc uniforms (in local rotated frame, facing is always 90 degrees)
        GL20.glUniform1f(uShieldFacingLoc, 1.5707963f);
        GL20.glUniform1f(uShieldArcLoc, shieldArcRad);

        // Mask texture
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, shieldSprite.getTextureId());
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL20.glUniform1i(uMaskTextureLoc, 0);

        // Pulse mask texture (binding = 1)
        if (pulseMaskSprite != null) {
            GL13.glActiveTexture(GL13.GL_TEXTURE1);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, pulseMaskSprite.getTextureId());
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL20.glUniform1i(uPulseMaskTextureLoc, 1);
        }

        // Hit event count (SSBO already bound by uploadHitEvents)
        GL20.glUniform1i(uHitEventCountLoc, hitEvents.size());

        // Pulse events: upload and set uniforms
        uploadPulseEvents();
        GL20.glUniform1i(uPulseEventCountLoc, pulseEvents.size());
        GL20.glUniform1f(uPulseLifetimeLoc, AUTO_PULSE_LIFETIME);

        // 护盾关闭渐出（旧代码通过 glColor4f 实现，新代码改为 uniform）
        GL20.glUniform1f(uFadeAlphaLoc, fadeAlpha);

        // Draw
        ShaderUtil.drawVAOQuad(vao.vaoId);

        // Cleanup state
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL20.glUseProgram(0);
        ShaderUtil.unbindSSBO(SSBO_BINDING);
        ShaderUtil.unbindSSBO(PULSE_SSBO_BINDING);
    }

    @Override
    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        if (layer != CombatEngineLayers.ABOVE_SHIPS_LAYER) return;
        if (expired || !ship.isAlive()) return;
        if (!renderEnabled) return;
        initGL();
        if (shaderProgram <= 0) return;
        if (shieldSprite == null) return;

        ShieldAPI shield = ship.getShield();
        if (shield == null) return;
        if (!shield.isOn() && fadeAlpha <= 0f) return;

        float shieldR = shield.isOn() ? shield.getRadius() : shieldRadius;
        if (shieldR <= 0f) return;

        float shieldFacingDeg = shield.isOn() ? shield.getFacing() : lastShieldFacingDeg;
        float shieldArcRad = shield.isOn() ? (float) Math.toRadians(shield.getActiveArc()) : lastActiveArcRad;

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        renderShield(shieldR, shieldFacingDeg, shieldArcRad);
        GL11.glPopAttrib();
    }

    // ==================== HIT EVENTS ====================

    /**
     * 触发新的波纹动画。将命中事件添加到动态列表，由SSBO在下一帧上传。
     * 替代旧的3波槽固定数组轮转方案，支持无限数量的同时波纹。
     *
     * <p>调用路径: {@link #onShieldHit(Vector2f, float)} 内部调用。</p>
     *
     * @param uvX    命中点在护盾UV空间的X坐标 [0,1]
     * @param uvY    命中点在护盾UV空间的Y坐标 [0,1]
     * @param damage 本次命中造成的护盾伤害值
     */
    public void triggerWave(float uvX, float uvY, float damage) {
        hitEvents.add(new HitEvent(uvX, uvY, timer, damage));
    }

    /**
     * 处理护盾受击事件: 将世界坐标命中点转换为护盾局部UV坐标，触发波纹。
     * 包含最小伤害阈值(20)和触发间隔(0.166秒)过滤。
     *
     * <p>调用路径: {@link MyDamageDealtModifier#modifyDamageTaken} 回调触发。</p>
     *
     * @param hitWorldPos 命中点世界坐标
     * @param shieldDmg   护盾伤害值
     */
    private void onShieldHit(Vector2f hitWorldPos, float shieldDmg) {
        if (shieldDmg < 20f) return;
        if (timer - lastHitTime < 0f) return;
        lastHitTime = timer;

        ShieldAPI shield = ship.getShield();
        if (shield == null) return;
        float sr = shield.getRadius();
        if (sr <= 0f) return;

        Vector2f shipLoc = ship.getLocation();
        float dx = hitWorldPos.x - shipLoc.x;
        float dy = hitWorldPos.y - shipLoc.y;

        float facingRad = (float) Math.toRadians(shield.getFacing() - 90f);
        float cosA = (float) Math.cos(-facingRad);
        float sinA = (float) Math.sin(-facingRad);
        float localX = dx * cosA - dy * sinA;
        float localY = dx * sinA + dy * cosA;

        float uvX = (localX / sr) * 0.5f + 0.5f;
        float uvY = (localY / sr) * 0.5f + 0.5f;

        triggerWave(uvX, uvY, shieldDmg);
    }

    /**
     * 将命中事件列表上传到SSBO，并绑定到binding point 1。
     * 采用智能分配策略: 首次或容量不足时使用glBufferData重新分配，
     * 否则使用glBufferSubData仅更新数据。
     *
     * <p>调用路径: {@link #renderShield(float, float, float)} 内部调用，每帧执行。</p>
     */
    private void uploadHitEvents() {
        int eventCount = hitEvents.size();
        int floatCount = Math.max(HIT_EVENT_FLOATS, eventCount * HIT_EVENT_FLOATS);

        // Ensure upload buffer capacity
        if (ssboUploadBuffer == null || ssboUploadCapacity < floatCount) {
            ssboUploadBuffer = BufferUtils.createFloatBuffer(floatCount);
            ssboUploadCapacity = floatCount;
        }

        ssboUploadBuffer.clear();
        if (eventCount > 0) {
            for (HitEvent event : hitEvents) {
                ssboUploadBuffer.put(event.uvX);
                ssboUploadBuffer.put(event.uvY);
                ssboUploadBuffer.put(event.triggerTime);
                ssboUploadBuffer.put(event.damage);
            }
        } else {
            // Upload at least one dummy event to avoid zero-size buffer
            ssboUploadBuffer.put(0f);
            ssboUploadBuffer.put(0f);
            ssboUploadBuffer.put(-999f);
            ssboUploadBuffer.put(0f);
        }
        ssboUploadBuffer.flip();

        ssboCapacity = ShaderUtil.uploadSSBO(ssboId, ssboUploadBuffer, floatCount, ssboCapacity);
        ShaderUtil.bindSSBO(ssboId, SSBO_BINDING);
    }

    /**
     * 将脉冲事件列表上传到独立的SSBO (binding=2)。
     * 脉冲事件与命中事件完全独立，仅驱动缝隙发光效果。
     *
     * <p>调用路径: {@link #renderShield(float, float, float)} 内部调用，每帧执行。</p>
     */
    private void uploadPulseEvents() {
        int eventCount = pulseEvents.size();
        int floatCount = Math.max(PULSE_EVENT_FLOATS, eventCount * PULSE_EVENT_FLOATS);

        if (pulseSsboUploadBuffer == null || pulseSsboUploadCapacity < floatCount) {
            pulseSsboUploadBuffer = BufferUtils.createFloatBuffer(floatCount);
            pulseSsboUploadCapacity = floatCount;
        }

        pulseSsboUploadBuffer.clear();
        if (eventCount > 0) {
            for (HitEvent event : pulseEvents) {
                pulseSsboUploadBuffer.put(event.uvX);
                pulseSsboUploadBuffer.put(event.uvY);
                pulseSsboUploadBuffer.put(event.triggerTime);
                pulseSsboUploadBuffer.put(event.damage);
            }
        } else {
            pulseSsboUploadBuffer.put(0f);
            pulseSsboUploadBuffer.put(0f);
            pulseSsboUploadBuffer.put(-999f);
            pulseSsboUploadBuffer.put(0f);
        }
        pulseSsboUploadBuffer.flip();

        pulseSsboCapacity = ShaderUtil.uploadSSBO(pulseSsboId, pulseSsboUploadBuffer, floatCount, pulseSsboCapacity);
        ShaderUtil.bindSSBO(pulseSsboId, PULSE_SSBO_BINDING);
    }

    /**
     * 命中事件数据容器，存储单次护盾受击的UV坐标、触发时间和伤害值。
     * 数据通过SSBO传递给片段着色器，用于计算波纹动画。
     */
    private static class HitEvent {
        final float uvX;
        final float uvY;
        final float triggerTime;
        final float damage;

        HitEvent(float uvX, float uvY, float triggerTime, float damage) {
            this.uvX = uvX;
            this.uvY = uvY;
            this.triggerTime = triggerTime;
            this.damage = damage;
        }
    }

    // ==================== DAMAGE LISTENER ====================

    private static class MyDamageDealtModifier implements DamageTakenModifier {
        private Meng_HexShieldPlugin plugin;

        public MyDamageDealtModifier(Meng_HexShieldPlugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage,
                                        Vector2f point, boolean shieldHit) {
            if (plugin == null || plugin.expired) return null;
            if (!shieldHit) return null;
            float shieldDmg;
            if(param instanceof BeamAPI) {
                shieldDmg = damage.getDamage() * 0.6f;
            }
            else {
                shieldDmg = damage.getDamage();
            }
            if (shieldDmg <= 0f) return null;

            plugin.onShieldHit(point, shieldDmg);
            return null;
        }
    }
}
