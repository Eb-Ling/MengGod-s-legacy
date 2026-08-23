package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

/**
 * Meng_Fire_DissPlugin - 排散特效常驻渲染插件。
 *
 * <h3>功能概述</h3>
 * 管理舰船排散（vent）时的环形粒子特效。采用常驻模式，一个 plugin 实例管理所有排散圈，
 * 替代旧的每圈创建独立 plugin 方案，减少引擎遍历和 addLayeredRenderingPlugin 开销。
 *
 * <h3>依赖的API</h3>
 * <ul>
 *   <li>Starsector CombatLayeredRenderingPlugin - 渲染插件生命周期</li>
 *   <li>LWJGL GL11 - 旧管线渲染调用</li>
 * </ul>
 *
 * <h3>使用方式</h3>
 * <pre>
 *   初始化时创建并注册:
 *     Meng_Fire_DissPlugin dissPlugin = new Meng_Fire_DissPlugin(ship);
 *     Global.getCombatEngine().addLayeredRenderingPlugin(dissPlugin);
 *     ship.setCustomData(DISS_KEY, dissPlugin);
 *
 *   每次排散触发时:
 *     dissPlugin.addRing(radius);
 * </pre>
 *
 * <h3>来源</h3>
 * 从 Meng_fire_core 和 Meng_fire_core_li 中的内部静态类提取合并为独立类。
 */
public class Meng_Fire_DissPlugin implements CombatLayeredRenderingPlugin {

    /** 每圈粒子的生命周期（秒） */
    private static final float RING_LIFETIME = 1.0f;
    /** 每圈的粒子数量 */
    private static final int PARTICLES_PER_RING = 16;

    private final ShipAPI ship;
    private WeaponAPI coverWeapon;
    private final List<DissRing> rings = new ArrayList<>();
    private float timer = 0f;

    /**
     * 创建排散特效常驻渲染插件。
     *
     * @param ship 所属舰船
     */
    public Meng_Fire_DissPlugin(ShipAPI ship) {
        this.ship = ship;
    }

    /**
     * 添加一圈新的排散特效。由 hullmod 的 advanceInCombat 在 venttime-- 时调用。
     *
     * @param radius 该圈的初始半径（世界坐标单位）
     */
    public void addRing(float radius) {
        DissRing ring = new DissRing();
        ring.radius = radius;
        ring.startTime = timer;
        ring.angleOffset = (float) Math.random() * 360f;
        ring.textureIndices = new int[PARTICLES_PER_RING];
        for (int i = 0; i < PARTICLES_PER_RING; i++) {
            ring.textureIndices[i] = Math.round((float) Math.floor(Math.random() * 16.9999f));
        }
        rings.add(ring);
    }

    @Override
    public void init(CombatEntityAPI entity) {
        for (WeaponAPI w : ship.getAllWeapons()) {
            if (w.getSpec().getWeaponId().equals("Meng_cover")) {
                coverWeapon = w;
                break;
            }
        }
    }

    @Override
    public void cleanup() {
    }

    /**
     * 舰船死亡时过期，引擎自动移除。
     */
    @Override
    public boolean isExpired() {
        return !ship.isAlive();
    }

    /**
     * 推进计时器并清理过期的排散圈。
     */
    @Override
    public void advance(float amount) {
        timer += amount;
        Iterator<DissRing> iter = rings.iterator();
        while (iter.hasNext()) {
            DissRing ring = iter.next();
            if (timer - ring.startTime >= RING_LIFETIME) {
                iter.remove();
            }
        }
    }

    @Override
    public EnumSet<CombatEngineLayers> getActiveLayers() {
        return EnumSet.of(CombatEngineLayers.CONTRAILS_LAYER);
    }

    @Override
    public float getRenderRadius() {
        return 10000000f;
    }

    /**
     * 渲染所有活跃的排散圈。每圈由16个粒子组成环形排列，随时间向外扩散并淡出。
     */
    @Override
    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        if (layer != CombatEngineLayers.CONTRAILS_LAYER) return;
        if (rings.isEmpty()) return;

        Vector2f loc;
        if (coverWeapon != null) {
            loc = coverWeapon.getLocation();
        } else {
            loc = ship.getLocation();
        }

        SpriteAPI sprite = Global.getSettings().getSprite("fx", "Meng_flux_diss");

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        sprite.bindTexture();

        for (DissRing ring : rings) {
            float age = timer - ring.startTime;
            float level = age / RING_LIFETIME;
            float expandFactor = 1f + level * 0.75f;
            float particleSize = ring.radius * 0.25f * (1f + level);
            float alpha = 0.75f * (1f - level * level);

            for (int i = 0; i < PARTICLES_PER_RING; i++) {
                float arg = ring.angleOffset + i * 360f / PARTICLES_PER_RING;
                float radArg = (float) Math.toRadians(arg);
                float px = loc.x + ring.radius * expandFactor * (float) Math.cos(radArg);
                float py = loc.y + ring.radius * expandFactor * (float) Math.sin(radArg);

                int texIdx = ring.textureIndices[i];
                int row = texIdx % 4;
                int column = texIdx / 4;

                GL11.glPushMatrix();
                GL11.glTranslatef(px, py, 0f);

                GL11.glColor4f(0.66f, 0.56f, 1f, alpha);
                GL11.glBegin(GL11.GL_QUAD_STRIP);
                GL11.glTexCoord2f(row * 0.25f, column * 0.25f);
                GL11.glVertex2f(-particleSize, particleSize);
                GL11.glTexCoord2f((row + 1) * 0.25f, column * 0.25f);
                GL11.glVertex2f(particleSize, particleSize);
                GL11.glTexCoord2f(row * 0.25f, (column + 1) * 0.25f);
                GL11.glVertex2f(-particleSize, -particleSize);
                GL11.glTexCoord2f((row + 1) * 0.25f, (column + 1) * 0.25f);
                GL11.glVertex2f(particleSize, -particleSize);
                GL11.glEnd();

                GL11.glPopMatrix();
            }
        }
    }

    /**
     * 单圈排散粒子数据容器。
     * 存储一圈粒子的半径、起始时间、随机角度偏移和每个粒子的贴图索引。
     */
    private static class DissRing {
        float radius;
        float startTime;
        float angleOffset;
        int[] textureIndices;
    }
}
