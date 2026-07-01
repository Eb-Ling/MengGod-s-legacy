package data.scripts.campaign;

import java.awt.Color;
import java.util.Random;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.impl.campaign.BaseCustomEntityPlugin;
import com.fs.starfarer.api.util.Misc;

/**
 * 圣殿跳跃点能量涌动渲染插件
 *
 * 继承 BaseCustomEntityPlugin，实现 CustomCampaignEntityPlugin 接口。
 * 在 campaign 层以 GL11 叠层绘制从外向内收缩的半透明亮红色波纹环，模拟能量涌动效果。
 *
 * 调用方式：由 custom_entities.json 中 "Meng_embers_jump_render" 类型通过 pluginClass 字段自动挂载，
 * 隐形实体由 Meng_EmbersJumpPlugin 在每个已标记跳跃点位置生成。
 *
 * 动态半径：通过 Meng_EmbersJumpState 读取关联跳跃点的当前特效半径，
 * 外部可随时调用 Meng_EmbersJumpState.get().setRadius(jumpPoint, newRadius) 动态调整。
 */
public class Meng_EmbersJumpRenderPlugin extends BaseCustomEntityPlugin {

    /** 单次收缩循环持续时间（秒），2.5s 较慢 */
    private static final float CYCLE_DURATION = 2.5f;

    /** 默认波纹起始半径（像素），当 Meng_EmbersJumpState 中无记录时使用此值 */
    private static final float DEFAULT_RADIUS_START = 90f;

    /** 波纹终止半径（像素），收缩到 0（中心点） */
    private static final float RADIUS_END = 0f;

    /** 基础透明度 0.4 */
    private static final float BASE_ALPHA = 0.4f;

    /** 波纹层数，3 层产生连续涌动感 */
    private static final int LAYER_COUNT = 3;

    /** 每层相位偏移 */
    private static final float PHASE_OFFSET = 1f / LAYER_COUNT;

    /** 环段数，48 段近似圆形 */
    private static final int SEGMENTS = 48;

    /** 基础颜色：亮红色 */
    private static final Color BASE_COLOR = new Color(255, 60, 40);

    /** 最大渲染距离（像素），超过此距离不绘制 */
    private static final float MAX_RENDER_DIST = 3000f;

    /**
     * 渲染实体 Memory 中存储关联跳跃点 ID 的 key。
     * 由 Meng_EmbersJumpPlugin.spawnRenderEntity() 在生成实体时写入，
     * 本插件通过此 ID 从 Meng_EmbersJumpState 查询当前特效半径。
     */
    public static final String JUMP_POINT_ID_KEY = "$Meng_embers_jump_point_id";

    /** 动画计时器，在 advance 中通过 amount 累加，单位秒 */
    private float animTimer = 0f;

    /** 随机数生成器，用于细微闪烁和位置抖动 */
    private transient Random random;

    /**
     * 初始化方法，由引擎在实体创建后调用。初始化随机数生成器。
     * @param entity  所挂载的 SectorEntityToken 实体
     * @param pluginParams  插件参数，未使用
     */
    @Override
    public void init(SectorEntityToken entity, Object pluginParams) {
        super.init(entity, pluginParams);
        random = new Random();
    }

    /**
     * 每帧推进逻辑。将时间累加到 animTimer 用于计算波纹收缩进度。
     * @param amount  本帧时间增量（秒）
     */
    @Override
    public void advance(float amount) {
        if(Global.getSector().isPaused()) return;
        animTimer += amount;
    }

    /**
     * 从 Meng_EmbersJumpState 获取当前关联跳跃点的动态特效半径。
     * 通过实体 Memory 中存储的跳跃点 ID 查询，若无记录则返回 DEFAULT_RADIUS_START。
     *
     * @return 当前跳跃点对应的特效起始半径（像素）
     */
    private float getCurrentRadiusStart() {
        Object jpId = entity.getMemoryWithoutUpdate().get(JUMP_POINT_ID_KEY);
        if (jpId instanceof String) {
            return Meng_EmbersJumpState.get().getRadiusById((String) jpId, DEFAULT_RADIUS_START);
        }
        return DEFAULT_RADIUS_START;
    }

    /**
     * 返回渲染范围（像素），引擎据此判断实体是否在视口内。
     * 使用动态半径计算，确保半径增大时渲染判定范围同步扩大。
     *
     * @return 渲染判定范围
     */
    @Override
    public float getRenderRange() {
        return entity.getRadius() + getCurrentRadiusStart() + 200f;
    }

    /**
     * 核心渲染方法，由引擎在 ABOVE 层调用。
     * 绘制 3 个相位错开的收缩波纹环，从外向内收缩，逐层变暗。
     * 使用 additive blend 产生发光效果，alpha 随收缩进度和 viewport 透明度调节。
     * 每帧从 Meng_EmbersJumpState 读取动态半径，实现特效大小实时可调。
     *
     * @param layer  当前渲染层
     * @param viewport  视口对象，提供 alphaMult
     */
    @Override
    public void render(CampaignEngineLayers layer, ViewportAPI viewport) {
        float alphaMult = viewport.getAlphaMult();
        if (alphaMult <= 0f) return;

        Vector2f loc = entity.getLocation();
        if (loc == null) return;

        // 距离玩家舰队过远时不渲染，节省性能
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();

        if (playerFleet != null) {
            float dist = Misc.getDistance(loc, playerFleet.getLocation());
            if (dist > MAX_RENDER_DIST) return;
        }

        // 每帧读取动态半径，支持外部实时调整
        float radiusStart = getCurrentRadiusStart();

        for (int i = 0; i < LAYER_COUNT; i++) {
            float phase = ((animTimer / CYCLE_DURATION) + i * PHASE_OFFSET) % 1f;
            renderRipple(loc.x, loc.y, phase, alphaMult, i, radiusStart);
        }
    }

    /**
     * 绘制单个收缩波纹环，含内圈柔光形成叠层深度。
     *
     * @param cx  实体世界坐标 X
     * @param cy  实体世界坐标 Y
     * @param phase  当前收缩进度（0 = 外环起始，1 = 内环终止）
     * @param alphaMult  viewport 透明度乘数
     * @param layerIndex  层索引（0/1/2），用于颜色微调和随机种子
     * @param radiusStart  当前帧的波纹起始半径（像素），由 Meng_EmbersJumpState 动态提供
     */
    private void renderRipple(float cx, float cy, float phase, float alphaMult, int layerIndex, float radiusStart) {
        // smoothstep 缓动控制收缩速度曲线
        float eased = phase * phase * (3f - 2f * phase);
        // 半径从 radiusStart 收缩到 RADIUS_END(0)，真正收拢到中心
        float radius = radiusStart + (RADIUS_END - radiusStart) * eased;

        // 三段式透明度：渐入(0~0.15) → 持续(0.15~0.85) → 渐出(0.85~1.0)
        float alpha;
        if (phase < 0.15f) {
            // 渐入：从 0 升到 BASE_ALPHA
            alpha = BASE_ALPHA * (phase / 0.15f);
        } else if (phase > 0.85f) {
            // 渐出：从 BASE_ALPHA 降到 0
            alpha = BASE_ALPHA * (1f - (phase - 0.85f) / 0.15f);
        } else {
            // 持续阶段：保持 BASE_ALPHA，轻微衰减
            alpha = BASE_ALPHA * (1f - eased * 0.2f);
        }
        alpha *= alphaMult;
        if (alpha <= 0.01f) return;

        // 细微随机闪烁（正负 0.1）
        float flicker = 1f + (random.nextFloat() - 0.5f) * 0.2f;
        alpha *= flicker;
        if (alpha > 1f) alpha = 1f;
        if (alpha < 0f) alpha = 0f;

        // 线宽：外环粗、内环细
        float lineWidth = 3f - 2f * eased;
        if (lineWidth < 0.5f) lineWidth = 0.5f;
        // 细微位置抖动（正负 2px），模拟能量不稳定
        float jitterX = (random.nextFloat() - 0.5f) * 2f;
        float jitterY = (random.nextFloat() - 0.5f) * 2f;

        // 颜色：亮红色，收缩到中心时略偏紫
        float r = BASE_COLOR.getRed() / 255f;
        float g = BASE_COLOR.getGreen() / 255f * (1f - eased * 0.3f);
        float b = BASE_COLOR.getBlue() / 255f * (1f + eased * 0.5f);

        GL11.glPushMatrix();
        GL11.glTranslatef(cx + jitterX, cy + jitterY, 0f);

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glLineWidth(lineWidth);
        GL11.glColor4f(r, g, b, alpha);

        // 外环主线
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int s = 0; s < SEGMENTS; s++) {
            float angle = (s / (float) SEGMENTS) * (float) Math.PI * 2f;
            float wave = 1f + (float) Math.sin(angle * 6f + phase * 10f + layerIndex) * 0.05f;
            float px = (float) Math.cos(angle) * radius * wave;
            float py = (float) Math.sin(angle) * radius * wave;
            GL11.glVertex2f(px, py);
        }
        GL11.glEnd();

        // 内圈柔光：更细更暗的第二层环，增加叠层感
        float innerRadius = radius * 0.7f;
        float innerAlpha = alpha * 0.3f;
        GL11.glLineWidth(lineWidth * 0.5f);
        GL11.glColor4f(r, g, b, innerAlpha);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int s = 0; s < SEGMENTS; s++) {
            float angle = (s / (float) SEGMENTS) * (float) Math.PI * 2f;
            float wave = 1f + (float) Math.sin(angle * 4f - phase * 8f + layerIndex) * 0.04f;
            float px = (float) Math.cos(angle) * innerRadius * wave;
            float py = (float) Math.sin(angle) * innerRadius * wave;
            GL11.glVertex2f(px, py);
        }
        GL11.glEnd();

        GL11.glColor4f(1f, 1f, 1f, 1f);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glPopMatrix();
    }

    @Override
    public boolean hasCustomMapTooltip() {
        return false;
    }
}
