package data.scripts.campaign;

import java.util.HashMap;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.JumpPointAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;

/**
 * 圣殿跳跃点特效状态管理器
 *
 * 通过 Sector 级别的 MemoryAPI（$Meng_embers_jump_state）持久化存储，
 * 以跳跃点 ID 为 key 记录每个跳跃点对应的特效半径值。
 *
 * 调用方式：
 * - 写入半径：Meng_EmbersJumpState.get().setRadius(jumpPoint, radius)
 * - 读取半径：Meng_EmbersJumpState.get().getRadius(jumpPoint, defaultRadius)
 * - 由 Meng_EmbersJumpPlugin 在生成渲染实体时设置初始半径
 * - 由 Meng_EmbersJumpRenderPlugin 在每帧渲染时读取当前半径
 * - 外部模块可随时调用 setRadius 动态修改任意跳跃点的特效大小
 */
public class Meng_EmbersJumpState {

    /** Sector Memory 中存储本状态实例的 key，通过 Global.getSector().getMemoryWithoutUpdate() 访问 */
    private static final String MEMORY_KEY = "$Meng_embers_jump_state";

    /** 跳跃点 ID → 特效半径的映射表，key 为 JumpPointAPI.getId()，value 为像素半径 */
    private Map<String, Float> radiusMap = new HashMap<>();

    /**
     * 获取全局唯一的状态实例。
     * 优先从 Sector Memory 中读取已有实例，若不存在则创建新实例并写入 Memory。
     *
     * @return 当前 Sector 的跳跃点特效状态实例
     */
    public static Meng_EmbersJumpState get() {
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        Object obj = mem.get(MEMORY_KEY);
        if (obj instanceof Meng_EmbersJumpState) {
            return (Meng_EmbersJumpState) obj;
        }
        Meng_EmbersJumpState state = new Meng_EmbersJumpState();
        mem.set(MEMORY_KEY, state);
        return state;
    }

    /**
     * 设置指定跳跃点的特效半径。
     *
     * @param jumpPoint  目标跳跃点，取其 getId() 作为 key
     * @param radius     特效起始半径（像素）
     */
    public void setRadius(JumpPointAPI jumpPoint, float radius) {
        if (jumpPoint == null) return;
        radiusMap.put(jumpPoint.getId(), radius);
    }

    /**
     * 获取指定跳跃点的特效半径。
     * 若该跳跃点未记录半径，返回传入的默认值。
     *
     * @param jumpPoint    目标跳跃点
     * @param defaultRadius 未记录时使用的默认半径
     * @return 该跳跃点当前配置的特效半径（像素）
     */
    public float getRadius(JumpPointAPI jumpPoint, float defaultRadius) {
        if (jumpPoint == null) return defaultRadius;
        Float r = radiusMap.get(jumpPoint.getId());
        return r != null ? r : defaultRadius;
    }

    /**
     * 通过跳跃点 ID 直接设置特效半径。
     * 适用于无法直接获取 JumpPointAPI 引用的场景。
     *
     * @param jumpPointId  跳跃点 ID（JumpPointAPI.getId()）
     * @param radius       特效起始半径（像素）
     */
    public void setRadiusById(String jumpPointId, float radius) {
        radiusMap.put(jumpPointId, radius);
    }

    /**
     * 通过跳跃点 ID 直接获取特效半径。
     *
     * @param jumpPointId   跳跃点 ID
     * @param defaultRadius 未记录时使用的默认半径
     * @return 该跳跃点当前配置的特效半径（像素）
     */
    public float getRadiusById(String jumpPointId, float defaultRadius) {
        Float r = radiusMap.get(jumpPointId);
        return r != null ? r : defaultRadius;
    }

    /**
     * 移除指定跳跃点的半径记录。
     * 移除后该跳跃点将使用默认半径。
     *
     * @param jumpPoint  目标跳跃点
     */
    public void removeRadius(JumpPointAPI jumpPoint) {
        if (jumpPoint == null) return;
        radiusMap.remove(jumpPoint.getId());
    }
}
