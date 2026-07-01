package data.scripts.campaign;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.JumpPointAPI;
import com.fs.starfarer.api.campaign.JumpPointAPI.JumpDestination;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.campaign.StarSystemAPI;
/**
 * 圣殿跳跃点标记与渲染实体生成脚本
 *
 * 实现 EveryFrameScript，在 Mengplugin.onGameLoad 中注册。
 * 扫描 Embers 星系及超空间中与之关联的跳跃点，
 * 为它们添加 $tag:Meng_embers_jump 和 HAS_INTERACTION_DIALOG 标签，
 * 并在每个已标记跳跃点位置生成一个隐形自定义实体（Meng_embers_jump_render），
 * 该实体挂载 Meng_EmbersJumpRenderPlugin 渲染能量涌动波纹效果。
 */
public class Meng_EmbersJumpPlugin implements EveryFrameScript {

    /** 星系名称 */
    private static final String EMBERS_SYSTEM_NAME = "Embers";

    /** rules 系统识别圣殿跳跃点的 memory key */
    private static final String EMBERS_JUMP_TAG = "$tag:Meng_embers_jump";

    /** custom_entities.json 中定义的渲染实体类型 id */
    private static final String RENDER_ENTITY_TYPE = "Meng_embers_jump_render";

    /** 生成的渲染实体 id 前缀，与跳跃点 id 拼接 */
    private static final String RENDER_ENTITY_ID_PREFIX = "meng_embers_render_";

    /** 重扫描间隔（秒） */
    private final IntervalUtil interval = new IntervalUtil(10f, 10f);

    /** 是否已完成首次扫描 */
    private boolean initialized = false;

    /**
     * 每帧推进。首次调用立即扫描，之后每 10 秒重扫一次以捕获新生成的跳跃点。
     * @param amount  本帧时间增量（秒）
     */
    @Override
    public void advance(float amount) {
        if (!initialized) {
            scanAndMarkJumpPoints();
            initialized = true;
        }

        interval.advance(amount);
        if (interval.intervalElapsed()) {
            scanAndMarkJumpPoints();
        }
    }

    /**
     * 扫描 Embers 星系及超空间，标记关联跳跃点并生成渲染实体。
     */
    private void scanAndMarkJumpPoints() {
        StarSystemAPI embers = Global.getSector().getStarSystem(EMBERS_SYSTEM_NAME);
        if (embers == null) {
            return;
        }

        // 星系内端：手动跳跃点和自动生成的系统内端
        for (SectorEntityToken entity : embers.getJumpPoints()) {
            if (entity instanceof JumpPointAPI) {
                JumpPointAPI jumpPoint = (JumpPointAPI) entity;
                markJumpPoint(jumpPoint);
                spawnRenderEntity(jumpPoint);
                markJumpDestinations(jumpPoint);
            }
        }

        // 超空间端：自动生成的入口在 hyperspace 中
        LocationAPI hyperspace = Global.getSector().getHyperspace();
        if (hyperspace == null) {
            return;
        }

        for (SectorEntityToken entity : hyperspace.getJumpPoints()) {
            if (entity instanceof JumpPointAPI) {
                JumpPointAPI jumpPoint = (JumpPointAPI) entity;
                if (isLinkedToSystem(jumpPoint, embers)) {
                    markJumpPoint(jumpPoint);
                    spawnRenderEntity(jumpPoint);
                    markJumpDestinations(jumpPoint);
                }
            }
        }
    }

    /**
     * 为跳跃点添加 rules 识别标签和交互对话框标签。
     * @param jumpPoint  待标记的跳跃点
     */
    private void markJumpPoint(JumpPointAPI jumpPoint) {
        jumpPoint.getMemoryWithoutUpdate().set(EMBERS_JUMP_TAG, true);
        jumpPoint.addTag(Tags.HAS_INTERACTION_DIALOG);
    }

    /**
     * 递归标记该跳跃点的目的地跳跃点，确保连线两端都被覆盖。
     * @param jumpPoint  其目的地需被标记的跳跃点
     */
    private void markJumpDestinations(JumpPointAPI jumpPoint) {
        for (JumpDestination destination : jumpPoint.getDestinations()) {
            SectorEntityToken destinationToken = destination.getDestination();
            if (destinationToken instanceof JumpPointAPI) {
                markJumpPoint((JumpPointAPI) destinationToken);
            }
        }
    }

    /**
     * 在跳跃点所在位置生成一个隐形渲染实体，
     * 通过零半径圆形轨道绑定到跳跃点，使渲染实体始终跟随跳跃点移动。
     * 若已存在同 id 实体则跳过，避免重复生成。
     *
     * 生成后执行两项初始化：
     * 1. 将跳跃点 ID 写入渲染实体 Memory（key: Meng_EmbersJumpRenderPlugin.JUMP_POINT_ID_KEY），
     *    供渲染插件查询 Meng_EmbersJumpState 获取动态半径。
     * 2. 在 Meng_EmbersJumpState 中为该跳跃点设置默认特效半径（90f），
     *    后续可由外部模块调用 setRadius 覆盖。
     *
     * @param jumpPoint  需要挂载渲染实体的跳跃点
     */
    private void spawnRenderEntity(JumpPointAPI jumpPoint) {
        String renderId = RENDER_ENTITY_ID_PREFIX + jumpPoint.getId();
        LocationAPI loc = jumpPoint.getContainingLocation();
        if (loc == null) return;

        SectorEntityToken existing = loc.getEntityById(renderId);
        if (existing != null) return;

        CustomCampaignEntityAPI renderEntity = loc.addCustomEntity(
                renderId, null, RENDER_ENTITY_TYPE, null
        );
        if (renderEntity != null) {
            renderEntity.setCircularOrbit(jumpPoint, 0f, 0f, 100000f);
            // 将跳跃点 ID 写入渲染实体 Memory，供 Meng_EmbersJumpRenderPlugin 查询动态半径
            renderEntity.getMemoryWithoutUpdate().set(
                    Meng_EmbersJumpRenderPlugin.JUMP_POINT_ID_KEY, jumpPoint.getId()
            );
            // 在状态类中设置默认特效半径，外部可随时覆盖
            Meng_EmbersJumpState.get().setRadius(jumpPoint, jumpPoint.getRadius()*1.2f);
        }
    }

    /**
     * 判断超空间跳跃点是否与 Embers 星系关联，
     * 通过检查其目标星系或目标 token 的所在位置。
     * @param jumpPoint  待检查的跳跃点
     * @param system  Embers 星系
     * @return true 表示该跳跃点通往 Embers 星系
     */
    private boolean isLinkedToSystem(JumpPointAPI jumpPoint, StarSystemAPI system) {
        if (jumpPoint.getDestinationStarSystem() == system) {
            return true;
        }

        for (JumpDestination destination : jumpPoint.getDestinations()) {
            SectorEntityToken destinationToken = destination.getDestination();
            if (destinationToken == null) {
                continue;
            }
            if (destinationToken.getContainingLocation() == system) {
                return true;
            }
            if (destinationToken instanceof JumpPointAPI) {
                JumpPointAPI destinationJumpPoint = (JumpPointAPI) destinationToken;
                if (destinationJumpPoint.getDestinationStarSystem() == system) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 脚本永不结束，持续运行以捕获新跳跃点。
     * @return 始终 false
     */
    @Override
    public boolean isDone() {
        return false;
    }

    /**
     * 是否在游戏暂停时运行。返回 false 使渲染随暂停停止。
     * @return 始终 false
     */
    @Override
    public boolean runWhilePaused() {
        return false;
    }
}