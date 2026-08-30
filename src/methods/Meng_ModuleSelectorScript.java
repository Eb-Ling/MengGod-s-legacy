package data.methods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.loading.WeaponGroupSpec;
import com.fs.starfarer.api.loading.WeaponGroupType;
import com.fs.starfarer.api.loading.WeaponSlotAPI;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * OldEmpire浮动模块选择器（纯静态工具类）。
 * <p>
 * 核心职责：
 * <ul>
 *   <li>管理浮动模块（STATION_MODULE）的安装与切换</li>
 *   <li>支持A/B两组独立槽位，每组多种模块模式（轻/重型模块由CSV配置）</li>
 *   <li>切换模块时通过标签系统缓存/恢复武器、船插、武器组等配置</li>
 *   <li>缓存以variant的tag形式存储，使用Base64编码序列化</li>
 *   <li>模块数据从CSV文件(data/config/Meng_OldEmpire_floating_modules.csv)加载，支持热配置</li>
 * </ul>
 */
public final class Meng_ModuleSelectorScript {

    // ====== 槽位ID常量（A组浮动模块） ======
    public static final String FLOAT_LEFT = "OLDEMPIRE_FLOAT_L";
    public static final String FLOAT_RIGHT = "OLDEMPIRE_FLOAT_R";
    public static final String FLOAT_CENTER = "OLDEMPIRE_FLOAT_C";
    // ====== 槽位ID常量（B组浮动模块，第二组） ======
    public static final String FLOAT_B_LEFT = "OLDEMPIRE_FLOAT_B_L";
    public static final String FLOAT_B_RIGHT = "OLDEMPIRE_FLOAT_B_R";
    public static final String FLOAT_B_CENTER = "OLDEMPIRE_FLOAT_B_C";
    // ====== 固定槽位（不可切换的固定浮动模块） ======
    public static final String FIXED_LEFT = "OLDEMPIRE_FIXED_L";
    public static final String FIXED_RIGHT = "OLDEMPIRE_FIXED_R";

    /** A组模式标签前缀，完整标签形如 Meng_OldEmpire_float_mode_0 ~ Meng_OldEmpire_float_mode_3 */
    private static final String MODE_PREFIX = "Meng_OldEmpire_float_mode_";
    /** B组模式标签前缀 */
    private static final String MODE_B_PREFIX = "Meng_OldEmpire_float_b_mode_";
    /** 标记variant已完成模块对象同步的tag */
    private static final String MODULE_OBJECTS_TAG = "Meng_OldEmpire_module_objects_v2";
    /** 模块配置缓存tag前缀，格式：prefix.{base64_key}.{base64_payload} */
    private static final String MODULE_CACHE_PREFIX = "Meng_OldEmpire_module_cache_v1.";

    // ====== CSV驱动的模块数据 ======

    /**
     * 浮动模块数据条目（从CSV加载）。
     * 每条记录直接存储完整的variant ID，代码中直接查表使用，不做字符串拼接。
     */
    public static class MengFloatingModule {
        /** 左侧浮动模块的完整variantId（如 Meng_OldEmpire_float_02_l_Standard） */
        public final String leftVariant;
        /** 右侧浮动模块的完整variantId（如 Meng_OldEmpire_float_02_r_Standard） */
        public final String rightVariant;
        /** true=轻型舰可用，false=重型舰可用 */
        public final boolean isLight;
        /** 显示名称（如 "1 侦察节点"） */
        public final String name;
        /** 模式索引（0~3），在同类型（轻/重）中的排序 */
        public final int mode;
        /** 固定绑定的舰船hullId（为空字符串时表示非固定模块） */
        public final String fixedHull;
        /** 固定模块安装方向：L=仅左侧，R=仅右侧，BOTH=两侧 */
        public final String fixedSide;

        public MengFloatingModule(String leftVariant, String rightVariant, boolean isLight,
                                  String name, int mode, String fixedHull, String fixedSide) {
            this.leftVariant = leftVariant != null ? leftVariant : "";
            this.rightVariant = rightVariant != null ? rightVariant : "";
            this.isLight = isLight;
            this.name = name;
            this.mode = mode;
            this.fixedHull = fixedHull != null ? fixedHull : "";
            this.fixedSide = fixedSide != null ? fixedSide : "";
        }
    }

    /** 轻型模块列表，按mode索引排序（从CSV加载） */
    private static final List<MengFloatingModule> LIGHT_MODULES = new ArrayList<MengFloatingModule>();
    /** 重型模块列表，按mode索引排序（从CSV加载） */
    private static final List<MengFloatingModule> HEAVY_MODULES = new ArrayList<MengFloatingModule>();
    /** 特殊舰船固定模块映射：hullId → 固定模块列表（从CSV加载） */
    private static final Map<String, List<MengFloatingModule>> FIXED_MODULES = new HashMap<String, List<MengFloatingModule>>();

    /** 空模块variant ID，用于无实际模块时占位 */
    private static final String EMPTY_MODULE = "Meng_OldEmpire_float_empty_Standard";
    /** CSV配置文件路径 */
    private static final String MODULE_CSV_PATH = "data/config/Meng_OldEmpire_floating_modules.csv";

    /** 纯静态工具类，禁止实例化 */
    private Meng_ModuleSelectorScript() {
    }

    // ===================================================================
    //                     CSV数据加载（onApplicationLoad时调用）
    // ===================================================================

    /**
     * 从CSV加载浮动模块配置数据。
     * 使用 getMergedSpreadsheetDataForMod API 解析CSV，支持多mod合并扩展。
     * 应在 Mengplugin.onApplicationLoad() 中调用。
     * <p>
     * CSV列：mode, leftVariant, rightVariant, isLight, name, fixedHull, fixedSide
     * - 常规模块行：填写mode/leftVariant/rightVariant/isLight/name，fixedHull和fixedSide留空
     * - 固定模块行：填写leftVariant/rightVariant/fixedHull/fixedSide，mode和name留空
     */
    public static void loadModuleData() {
        LIGHT_MODULES.clear();
        HEAVY_MODULES.clear();
        FIXED_MODULES.clear();

        try {
            // 使用游戏API加载CSV（支持多mod合并，其他mod可通过同名CSV追加行）
            JSONArray rows = Global.getSettings().getMergedSpreadsheetDataForMod(
                    "leftVariant", MODULE_CSV_PATH, "Meng");

            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.getJSONObject(i);

                String leftVariant = row.optString("leftVariant", "").trim();
                String rightVariant = row.optString("rightVariant", "").trim();
                if (leftVariant.isEmpty() && rightVariant.isEmpty()) continue;

                String modeStr = row.optString("mode", "").trim();
                boolean isLight = row.optBoolean("isLight", false);
                String name = row.optString("name", "").trim();
                String fixedHull = row.optString("fixedHull", "").trim();
                String fixedSide = row.optString("fixedSide", "").trim();
                int mode = modeStr.isEmpty() ? -1 : row.optInt("mode", -1);

                MengFloatingModule entry = new MengFloatingModule(leftVariant, rightVariant,
                        isLight, name, mode, fixedHull, fixedSide);

                if (!fixedHull.isEmpty()) {
                    if (leftVariant.isEmpty() || rightVariant.isEmpty()
                            || !isValidFixedSide(fixedSide)) {
                        Global.getLogger(Meng_ModuleSelectorScript.class).warn(
                                "Skipping invalid fixed floating module row " + i);
                        continue;
                    }
                    // 固定模块行：按hullId分组存储
                    if (!FIXED_MODULES.containsKey(fixedHull)) {
                        FIXED_MODULES.put(fixedHull, new ArrayList<MengFloatingModule>());
                    }
                    FIXED_MODULES.get(fixedHull).add(entry);
                } else {
                    if (mode < 0 || leftVariant.isEmpty() || rightVariant.isEmpty()) {
                        Global.getLogger(Meng_ModuleSelectorScript.class).warn(
                                "Skipping invalid floating module row " + i);
                        continue;
                    }
                    // 常规模块行：按轻/重型分组，mode索引排序插入
                    List<MengFloatingModule> list = isLight ? LIGHT_MODULES : HEAVY_MODULES;
                    while (list.size() <= mode) {
                        list.add(null);
                    }
                    if (list.get(mode) != null) {
                        Global.getLogger(Meng_ModuleSelectorScript.class).warn(
                                "Skipping duplicate floating module mode " + mode + " in row " + i);
                        continue;
                    }
                    list.set(mode, entry);
                }
            }

            Global.getLogger(Meng_ModuleSelectorScript.class)
                    .info("Loaded floating module data: " + LIGHT_MODULES.size() + " light, "
                            + HEAVY_MODULES.size() + " heavy, " + FIXED_MODULES.size() + " fixed hulls");
        } catch (Exception ex) {
            Global.getLogger(Meng_ModuleSelectorScript.class)
                    .error("Failed to load floating module CSV: " + MODULE_CSV_PATH, ex);
        }
    }

    // ===================================================================
    //                     CSV数据查询辅助方法
    // ===================================================================

    /**
     * 获取指定类型和模式的模块数据条目。
     * @param light true=轻型，false=重型
     * @param mode  模式索引（从0开始）
     */
    private static MengFloatingModule getModuleForMode(boolean light, int mode) {
        List<MengFloatingModule> list = light ? LIGHT_MODULES : HEAVY_MODULES;
        if (list.isEmpty()) {
            return null;
        }
        int safeMode = Math.max(0, Math.min(mode, list.size() - 1));
        if (safeMode < list.size()) {
            return list.get(safeMode);
        }
        return null;
    }

    /**
     * 获取指定类型当前注册的模式数量（从CSV加载的条目数）。
     * 新增CSV行后此值自动增大，按钮注册和标签读写均依赖此值。
     * @param light true=轻型，false=重型
     */
    public static int getModeCount(boolean light) {
        return (light ? LIGHT_MODULES : HEAVY_MODULES).size();
    }

    /** 判断某个模式是否由当前CSV配置实际提供。 */
    public static boolean isModeAvailable(boolean light, int mode) {
        return mode >= 0 && mode < getModeCount(light)
                && getModuleForMode(light, mode) != null;
    }

    /**
     * 获取两种类型中最大的模式数量（用于标签清理等需要覆盖全部范围的场景）。
     */
    private static int getMaxModeCount() {
        return Math.max(LIGHT_MODULES.size(), HEAVY_MODULES.size());
    }

    /**
     * 获取指定类型的默认模块数据（mode=0的模块）。
     * @param light true=轻型，false=重型
     */
    private static MengFloatingModule getDefaultModule(boolean light) {
        List<MengFloatingModule> list = light ? LIGHT_MODULES : HEAVY_MODULES;
        if (!list.isEmpty() && list.get(0) != null) {
            return list.get(0);
        }
        return null; // 兜底返回null，调用方需处理
    }

    // ===================================================================
    //                        公共查询方法
    // ===================================================================

    /**
     * 判断variant是否为OldEmpire父级舰（拥有可切换浮动模块的舰船）。
     * 条件：hullId以"Meng_OldEmpire_"开头，且FLOAT_LEFT和FLOAT_RIGHT槽位类型均为STATION_MODULE。
     */
    public static boolean isOldEmpireParent(ShipVariantAPI variant) {
        if (variant == null || variant.getHullSpec() == null
                || !variant.getHullSpec().getHullId().startsWith("Meng_OldEmpire_")) {
            return false;
        }
        WeaponSlotAPI left = variant.getHullSpec().getWeaponSlotAPI(FLOAT_LEFT);
        WeaponSlotAPI right = variant.getHullSpec().getWeaponSlotAPI(FLOAT_RIGHT);
        return left != null && right != null
                && left.getWeaponType() == WeaponAPI.WeaponType.STATION_MODULE
                && right.getWeaponType() == WeaponAPI.WeaponType.STATION_MODULE;
    }

    /**
     * 判断variant是否支持B组（双组浮动模块）。
     * 需要FLOAT_B_LEFT和FLOAT_B_RIGHT两个额外槽位存在且类型正确。
     */
    public static boolean hasSecondaryGroup(ShipVariantAPI variant) {
        if (variant == null || variant.getHullSpec() == null) {
            return false;
        }
        WeaponSlotAPI left = variant.getHullSpec().getWeaponSlotAPI(FLOAT_B_LEFT);
        WeaponSlotAPI right = variant.getHullSpec().getWeaponSlotAPI(FLOAT_B_RIGHT);
        return left != null && right != null
                && left.getWeaponType() == WeaponAPI.WeaponType.STATION_MODULE
                && right.getWeaponType() == WeaponAPI.WeaponType.STATION_MODULE;
    }

    /** 判断舰船尺寸是否为轻型（护卫舰/驱逐舰） */
    public static boolean isLightHull(ShipAPI.HullSize size) {
        return size == ShipAPI.HullSize.FRIGATE || size == ShipAPI.HullSize.DESTROYER;
    }

    /** 判断舰船尺寸是否为重型（巡洋舰/主力舰） */
    public static boolean isHeavyHull(ShipAPI.HullSize size) {
        return size == ShipAPI.HullSize.CRUISER || size == ShipAPI.HullSize.CAPITAL_SHIP;
    }

    // ===================================================================
    //                        模式读取/写入
    // ===================================================================

    /**
     * 获取A组当前模式索引（默认group=0）。
     */
    public static int getMode(ShipVariantAPI variant) {
        return getMode(variant, 0);
    }

    /**
     * 获取指定组当前激活的模式索引（0~3）。
     * <p>
     * 读取优先级：
     * 1. variant标签中的模式记录（最可靠，由selectMode写入）
     * 2. 从当前安装的浮动模块反推模式（兼容旧存档）
     * 3. 兜底返回0
     */
    public static int getMode(ShipVariantAPI variant, int group) {
        int safeGroup = group > 0 ? 1 : 0;
        boolean light = variant != null
                && variant.getHullSpec() != null
                && isLightHull(variant.getHullSpec().getHullSize());
        // 优先从tag中读取模式
        int tagged = readTagMode(variant, modePrefix(safeGroup), getMaxModeCount());
        if (isModeAvailable(light, tagged)) {
            return tagged;
        }
        // 回退：从当前安装的模块ID推断模式
        int fromModule = readModeFromModule(variant, light, leftSlot(safeGroup));
        if (isModeAvailable(light, fromModule)) {
            return fromModule;
        }
        // 兜底
        return 0;
    }

    /**
     * 获取模块模式的显示名称。
     * @param light true=轻型舰模块名，false=重型舰模块名
     * @param mode  模式索引（0~3）
     */
    public static String getModuleName(boolean light, int mode) {
        MengFloatingModule module = getModuleForMode(light, mode);
        return module != null ? module.name : "Mode " + mode;
    }

    // ===================================================================
    //                        模式切换
    // ===================================================================

    /**
     * 切换A组模块模式（默认group=0）。
     */
    public static void selectMode(ShipVariantAPI variant, boolean light, int mode) {
        selectMode(variant, light, mode, 0);
    }

    /**
     * 执行模块模式切换的核心方法。
     * <p>
     * 流程：
     * 1. 清理非法的station module条目
     * 2. 缓存当前激活模块的武器/船插/武器组配置到tag中
     * 3. 写入新模式标签
     * 4. 安装新模式对应的浮动模块（从缓存恢复之前的配置）
     * 5. 标记variant已同步，设置来源为REFIT
     */
    public static void selectMode(ShipVariantAPI variant, boolean light, int mode, int group) {
        if (variant == null || !isModeAvailable(light, mode)) {
            return;
        }
        int safeGroup = group > 0 ? 1 : 0;
        int safeMode = mode;

        // 第1步：清理不合法的station module槽位
        sanitizeStationModules(variant);

        // 第2步：缓存当前模块的武器/船插/武器组配置
        cacheActiveModule(variant, leftSlot(safeGroup));
        cacheActiveModule(variant, rightSlot(safeGroup));
        cacheActiveModule(variant, centerSlot(safeGroup));

        // 第3步：写入新模式标签（先清除旧标签再写新的）
        writeTagMode(variant, safeMode, modePrefix(safeGroup), getMaxModeCount());

        // 第4步：安装新模式对应的浮动模块（直接使用CSV中注册的variantId）
        MengFloatingModule target = getModuleForMode(light, safeMode);
        setFloatingModules(variant, target, true, safeGroup);

        // 第5步：标记variant已完成模块对象同步
        if (variant != null) {
            variant.addTag(MODULE_OBJECTS_TAG);
            variant.setSource(VariantSource.REFIT);
        }
    }

    // ===================================================================
    //                        variant同步
    // ===================================================================

    /**
     * 同步variant的浮动模块状态（确保模块安装正确且标签一致）。
     * <p>
     * 在以下场景调用：
     * - 舰船首次加载时确保默认模块
     * - 检测到模块对象未同步时强制安装
     * - 特殊舰船（Meng_OldEmpire_002/004）的固定浮动模块安装
     *
     * @param variant  舰船variant
     * @param hullSize 舰船尺寸（用于判断轻/重型）
     * @return 是否发生了任何变更
     */
    public static boolean syncVariant(ShipVariantAPI variant, ShipAPI.HullSize hullSize) {
        if (!isOldEmpireParent(variant)) {
            return false;
        }

        boolean light = isLightHull(hullSize);
        // 如果variant尚未标记完成同步，则需要强制安装所有模块对象
        boolean forceObjectSync = !variant.hasTag(MODULE_OBJECTS_TAG);
        boolean changed = sanitizeStationModules(variant);

        // 同步A组
        int mode = getMode(variant, 0);
        writeTagMode(variant, mode, MODE_PREFIX, getMaxModeCount());
        changed |= ensureDefaultModules(variant, light, 0);
        MengFloatingModule modeModule = getModuleForMode(light, mode);
        changed |= setFloatingModules(variant, modeModule, forceObjectSync, 0);

        // 同步B组（如果存在）
        if (hasSecondaryGroup(variant)) {
            int secondaryMode = getMode(variant, 1);
            writeTagMode(variant, secondaryMode, MODE_B_PREFIX, getMaxModeCount());
            changed |= ensureDefaultModules(variant, light, 1);
            MengFloatingModule secModule = getModuleForMode(light, secondaryMode);
            changed |= setFloatingModules(variant, secModule, forceObjectSync, 1);
        }

        // 特殊舰船的固定浮动模块安装（从CSV数据驱动，直接使用注册的variantId）
        String hullId = variant.getHullSpec().getHullId();
        List<MengFloatingModule> fixedList = FIXED_MODULES.get(hullId);
        if (fixedList != null) {
            for (MengFloatingModule fixed : fixedList) {
            String side = fixed.fixedSide.toUpperCase(java.util.Locale.ROOT);
                if ("BOTH".equals(side) || "L".equals(side)) {
                    changed |= setStationModule(variant, FIXED_LEFT,
                            fixed.leftVariant, forceObjectSync);
                }
                if ("BOTH".equals(side) || "R".equals(side)) {
                    changed |= setStationModule(variant, FIXED_RIGHT,
                            fixed.rightVariant, forceObjectSync);
                }
            }
        }

        // 首次同步完成后打标记，避免后续重复安装
        if (forceObjectSync) {
            variant.addTag(MODULE_OBJECTS_TAG);
            changed = true;
        }

        if (changed) {
            variant.setSource(VariantSource.REFIT);
        }
        return changed;
    }

    // ===================================================================
    //                        模块安装
    // ===================================================================

    /**
     * 确保指定组拥有默认浮动模块（若当前槽位为空则安装默认模块）。
     * 默认模块为CSV中mode=0的条目（轻型默认02，重型默认01）。
     */
    private static boolean ensureDefaultModules(ShipVariantAPI variant, boolean light, int group) {
        MengFloatingModule defaultMod = getDefaultModule(light);
        boolean changed = false;
        if (defaultMod != null) {
            changed |= ensureStationModule(variant, leftSlot(group), defaultMod.leftVariant);
            changed |= ensureStationModule(variant, rightSlot(group), defaultMod.rightVariant);
        }
        // 中心槽位仅在舰船拥有该槽位时才处理
        String center = centerSlot(group);
        if (hasStationModuleSlot(variant, center)) {
            changed |= ensureStationModule(variant, center, EMPTY_MODULE);
        }
        return changed;
    }

    /**
     * 确保指定槽位有模块（仅在槽位为空时安装，已有则跳过）。
     */
    private static boolean ensureStationModule(ShipVariantAPI variant, String slot, String variantId) {
        if (variant.getStationModules().get(slot) != null) {
            return false;
        }
        return installModuleVariant(variant, slot, variantId);
    }

    /**
     * 批量设置浮动模块（左+右+中心）。
     * 直接使用模块数据中注册的variantId，不做字符串拼接。
     * <p>
     * 中心槽位特殊处理：当模块的rightVariant为空时（如09号视界节点），
     * 中心槽位使用该模块的leftVariant；其他情况使用空模块占位。
     *
     * @param variant       舰船variant
     * @param module        模块数据条目（可为null，null时不安装任何模块）
     * @param forceInstall  是否强制重新安装（忽略已安装的相同variant）
     * @param group         槽位组（0=A组，1=B组）
     */
    private static boolean setFloatingModules(ShipVariantAPI variant, MengFloatingModule module,
                                              boolean forceInstall, int group) {
        if (variant == null || module == null) {
            return false;
        }
        boolean changed = false;
        // 直接使用CSV中注册的完整variantId
        changed |= setStationModule(variant, leftSlot(group), module.leftVariant, forceInstall);
        changed |= setStationModule(variant, rightSlot(group), module.rightVariant, forceInstall);
        // 中心槽位：rightVariant为空时使用leftVariant，否则用空模块占位
        String center = centerSlot(group);
        if (hasStationModuleSlot(variant, center)) {
            String centerVariant = module.rightVariant.isEmpty()
                    ? module.leftVariant
                    : EMPTY_MODULE;
            changed |= setStationModule(variant, center, centerVariant, forceInstall);
        }
        return changed;
    }

    /**
     * 判断variant的指定槽位是否为STATION_MODULE类型。
     */
    private static boolean hasStationModuleSlot(ShipVariantAPI variant, String slotId) {
        if (variant == null || variant.getHullSpec() == null || slotId == null) {
            return false;
        }
        WeaponSlotAPI slot = variant.getHullSpec().getWeaponSlotAPI(slotId);
        return slot != null && slot.getWeaponType() == WeaponAPI.WeaponType.STATION_MODULE;
    }

    /**
     * 设置单个浮动模块槽位。
     * 如果当前已有相同variantId且不需要强制安装，则跳过。
     */
    private static boolean setStationModule(ShipVariantAPI variant, String slot, String variantId,
                                            boolean forceInstall) {
        if (variant == null) {
            return false;
        }
        String current = variant.getStationModules().get(slot);
        ShipVariantAPI currentModule = getModuleVariantSafely(variant, slot);
        // 不强制安装时，若variantId和模块对象都匹配则跳过
        if (!forceInstall && variantId.equals(current) && moduleMatches(currentModule, variantId)) {
            return false;
        }
        // 不强制安装时，仅variantId匹配也跳过
        if (!forceInstall && variantId.equals(current)) {
            return false;
        }
        return installModuleVariant(variant, slot, variantId);
    }

    /**
     * 安装浮动模块到指定槽位。
     * 从全局设置中读取模块variant模板，克隆并恢复缓存的配置后安装。
     */
    private static boolean installModuleVariant(ShipVariantAPI parent, String slotId, String variantId) {
        if (parent == null || parent.getHullSpec() == null) {
            return false;
        }
        WeaponSlotAPI slot = parent.getHullSpec().getWeaponSlotAPI(slotId);
        if (slot == null || slot.getWeaponType() != WeaponAPI.WeaponType.STATION_MODULE) {
            return false;
        }

        // 从游戏设置中读取模块variant模板
        ShipVariantAPI template = Global.getSettings().getVariant(variantId);
        if (template == null || template.getHullSpec() == null) {
            Global.getLogger(Meng_ModuleSelectorScript.class)
                    .error("Missing floating module variant: " + variantId);
            return false;
        }

        // 克隆模板variant，并从缓存恢复之前的武器/船插配置
        ShipVariantAPI module = createDefaultModuleVariant(template, variantId);
        restoreCachedModule(parent, slotId, variantId, module);
        parent.setModuleVariant(slotId, module);
        Global.getLogger(Meng_ModuleSelectorScript.class)
                .info("Installed floating module " + variantId + " into " + slotId
                        + " on " + parent.getHullVariantId());
        return true;
    }

    /**
     * 克隆模板variant并设置基础属性。
     * 设置来源为REFIT，关闭自动分配武器。
     */
    private static ShipVariantAPI createDefaultModuleVariant(ShipVariantAPI template, String variantId) {
        ShipVariantAPI module = template.clone();
        module.setSource(VariantSource.REFIT);
        module.setHullVariantId(variantId);
        module.setMayAutoAssignWeapons(false);
        return module;
    }

    /**
     * 清除模块variant中所有非内置武器槽的武器配置。
     * 保留内置武器不受影响。
     */
    private static void clearNonBuiltInWeapons(ShipVariantAPI module) {
        if (module == null || module.getHullSpec() == null) {
            return;
        }
        for (WeaponSlotAPI slot : module.getHullSpec().getAllWeaponSlotsCopy()) {
            if (slot != null && slot.isWeaponSlot() && !module.getHullSpec().isBuiltIn(slot.getId())) {
                module.clearSlot(slot.getId());
            }
        }
    }

    /**
     * 安全获取模块variant，捕获可能的运行时异常。
     */
    private static ShipVariantAPI getModuleVariantSafely(ShipVariantAPI parent, String slotId) {
        if (parent == null || slotId == null) {
            return null;
        }
        try {
            return parent.getModuleVariant(slotId);
        } catch (RuntimeException ex) {
            Global.getLogger(Meng_ModuleSelectorScript.class)
                    .warn("Unable to read floating module in slot " + slotId, ex);
            return null;
        }
    }

    /** 判断模块variant的hullVariantId是否与期望的variantId一致 */
    private static boolean moduleMatches(ShipVariantAPI module, String variantId) {
        return module != null && variantId != null && variantId.equals(module.getHullVariantId());
    }

    // ===================================================================
    //                    模块配置缓存（tag序列化系统）
    // ===================================================================

    /**
     * 缓存指定槽位当前激活模块的配置到variant的tag中。
     * 缓存包含：通风口、电容、武器、船插、武器组等全部可编辑配置。
     */
    private static void cacheActiveModule(ShipVariantAPI parent, String slotId) {
        if (!hasStationModuleSlot(parent, slotId)) {
            return;
        }
        ShipVariantAPI module = getModuleVariantSafely(parent, slotId);
        if (module == null) {
            return;
        }
        String variantId = resolveModuleVariantId(parent, slotId, module);
        if (!isCacheableModuleVariant(variantId)) {
            return;
        }
        writeModuleCache(parent, slotId, variantId, module);
    }

    /**
     * 解析模块的variantId。
     * 优先从模块variant对象获取，回退到parent的stationModules映射。
     */
    private static String resolveModuleVariantId(ShipVariantAPI parent, String slotId, ShipVariantAPI module) {
        if (module != null && isCacheableModuleVariant(module.getHullVariantId())) {
            return module.getHullVariantId();
        }
        if (parent != null && parent.getStationModules() != null) {
            String current = parent.getStationModules().get(slotId);
            if (isCacheableModuleVariant(current)) {
                return current;
            }
        }
        return null;
    }

    /**
     * 判断variantId是否为可缓存的标准模块。
     * 标准格式：Meng_OldEmpire_float_{XX}_Standard
     */
    private static boolean isCacheableModuleVariant(String variantId) {
        return variantId != null
                && variantId.startsWith("Meng_OldEmpire_float_")
                && variantId.endsWith("_Standard");
    }

    /**
     * 将模块配置序列化为JSON并写入parent的tag中。
     * tag格式：Meng_OldEmpire_module_cache_v1.{cacheKey}.{base64(json)}
     * cacheKey = slotId|variantId（明文，仅含安全字符）
     */
    private static void writeModuleCache(ShipVariantAPI parent, String slotId, String variantId,
                                         ShipVariantAPI module) {
        if (parent == null || slotId == null || variantId == null || module == null) {
            return;
        }
        String cacheKey = cacheKey(slotId, variantId);
        // 先移除同key的旧缓存
        removeModuleCache(parent, cacheKey);
        // 序列化模块配置为JSON，Base64编码后写入tag
        String json = serializeModuleLoadout(module).toString();
        String encoded = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        parent.addTag(MODULE_CACHE_PREFIX + cacheKey + "." + encoded);
    }

    /**
     * 从parent的tag缓存中恢复指定模块的配置。
     * 在新模块安装后调用，将之前缓存的武器/船插/武器组等应用到新模块。
     */
    private static void restoreCachedModule(ShipVariantAPI parent, String slotId, String variantId,
                                            ShipVariantAPI module) {
        String payload = readModuleCache(parent, cacheKey(slotId, variantId));
        if (payload == null || payload.isEmpty()) {
            return;
        }
        applyModuleLoadout(module, payload);
    }

    /** 构造缓存key：槽位ID + "|" + variantId */
    private static String cacheKey(String slotId, String variantId) {
        return slotId + "|" + variantId;
    }

    /** 从tag列表中移除指定cacheKey的所有缓存tag */
    private static void removeModuleCache(ShipVariantAPI parent, String cacheKey) {
        if (parent == null || cacheKey == null) {
            return;
        }
        // 复制tag列表避免ConcurrentModificationException
        List<String> copy = new ArrayList<String>(parent.getTags());
        for (String tag : copy) {
            if (cacheKey.equals(readCacheKey(tag))) {
                parent.removeTag(tag);
            }
        }
    }

    /** 从tag列表中读取指定cacheKey的缓存payload（JSON字符串） */
    private static String readModuleCache(ShipVariantAPI parent, String cacheKey) {
        if (parent == null || cacheKey == null) {
            return null;
        }
        for (String tag : parent.getTags()) {
            if (!cacheKey.equals(readCacheKey(tag))) {
                continue;
            }
            // tag格式：prefix.cacheKey.base64(json)，取第3段解码
            String[] parts = tag.split("\\.", 3);
            if (parts.length == 3) {
                try {
                    return new String(Base64.getUrlDecoder().decode(parts[2]),
                            java.nio.charset.StandardCharsets.UTF_8);
                } catch (IllegalArgumentException ex) {
                    return null;
                }
            }
        }
        return null;
    }

    /** 从缓存tag中提取cacheKey（第2段，明文） */
    private static String readCacheKey(String tag) {
        if (tag == null || !tag.startsWith(MODULE_CACHE_PREFIX)) {
            return null;
        }
        String[] parts = tag.split("\\.", 3);
        if (parts.length < 3) {
            return null;
        }
        return parts[1];
    }

    // ===================================================================
    //                    模块配置序列化/反序列化
    // ===================================================================

    /**
     * 将模块variant的可编辑配置序列化为JSON对象。
     * 包含：vents、caps、weapons、mods（普通/perma/smod）、weapon groups
     */
    private static JSONObject serializeModuleLoadout(ShipVariantAPI module) {
        JSONObject json = new JSONObject();
        try {
            if (module.getNumFluxVents() > 0) {
                json.put("vents", module.getNumFluxVents());
            }
            if (module.getNumFluxCapacitors() > 0) {
                json.put("caps", module.getNumFluxCapacitors());
            }

        // 武器：{slotId: weaponId}
            JSONObject weapons = new JSONObject();
            for (String slotId : module.getNonBuiltInWeaponSlots()) {
                String weaponId = module.getWeaponId(slotId);
                if (weaponId != null) {
                    weapons.put(slotId, weaponId);
                }
            }
            if (weapons.length() > 0) {
                json.put("weapons", weapons);
            }

        // 船插（区分普通/永久/S-mod三类，跳过内置的）
            JSONArray mods = new JSONArray();
            JSONArray permaMods = new JSONArray();
            JSONArray smods = new JSONArray();
            Set<String> perma = new LinkedHashSet<String>(module.getPermaMods());
            Set<String> smodSet = new LinkedHashSet<String>(module.getSMods());
            for (String hullmod : module.getNonBuiltInHullmods()) {
                if (!perma.contains(hullmod) && !smodSet.contains(hullmod)) {
                    mods.put(hullmod);
                }
            }
            for (String hullmod : perma) {
                if (!smodSet.contains(hullmod) && !isBuiltInHullmod(module, hullmod)) {
                    permaMods.put(hullmod);
                }
            }
            for (String hullmod : smodSet) {
                if (!isBuiltInHullmod(module, hullmod)) {
                    smods.put(hullmod);
                }
            }
            if (mods.length() > 0) json.put("mods", mods);
            if (permaMods.length() > 0) json.put("permaMods", permaMods);
            if (smods.length() > 0) json.put("smods", smods);

        // 武器组：[{type, autofire, slots}]
            JSONArray groups = new JSONArray();
            for (WeaponGroupSpec group : module.getWeaponGroups()) {
                if (group == null || group.getSlots().isEmpty()) continue;
                JSONObject g = new JSONObject();
                g.put("type", group.getType().name());
                g.put("autofire", group.isAutofireOnByDefault());
                JSONArray slots = new JSONArray();
                for (String slotId : group.getSlots()) {
                    slots.put(slotId);
                }
                g.put("slots", slots);
                groups.put(g);
            }
                if (groups.length() > 0) {
                    json.put("groups", groups);
                }
        } catch (JSONException e) {
            // JSON序列化异常不应阻断模块切换，返回部分构建的结果
        }

        return json;
    }

    /**
     * 将JSON配置应用到模块variant。
     * 先清除可编辑配置，再从 JSON 中恢复。
     */
    private static void applyModuleLoadout(ShipVariantAPI module, String payload) {
        if (module == null || payload == null) {
            return;
        }
        JSONObject json;
        try {
            json = new JSONObject(payload);
        } catch (Exception ex) {
            return;
        }

        try {
            // 先清空模块上的可编辑配置
            clearEditableLoadout(module);

        // 通风口、电容
            module.setNumFluxVents(json.optInt("vents", 0));
            module.setNumFluxCapacitors(json.optInt("caps", 0));

        // 武器
            boolean restoredWeapon = false;
            JSONObject weapons = json.optJSONObject("weapons");
            if (weapons != null) {
                for (Iterator<String> keys = weapons.keys(); keys.hasNext();) {
                    String slotId = keys.next();
                    module.addWeapon(slotId, weapons.getString(slotId));
                    restoredWeapon = true;
                }
            }

        // 船插（普通/永久/S-mod）
            JSONArray mods = json.optJSONArray("mods");
            if (mods != null) {
                for (int i = 0; i < mods.length(); i++) {
                    addHullMod(module, mods.getString(i));
                }
            }
            JSONArray permaMods = json.optJSONArray("permaMods");
            if (permaMods != null) {
                for (int i = 0; i < permaMods.length(); i++) {
                    addPermaHullMod(module, permaMods.getString(i), false);
                }
            }
            JSONArray smods = json.optJSONArray("smods");
            if (smods != null) {
                for (int i = 0; i < smods.length(); i++) {
                    addPermaHullMod(module, smods.getString(i), true);
                }
            }

        // 武器组
            boolean restoredGroup = false;
            JSONArray groups = json.optJSONArray("groups");
            if (groups != null) {
                module.getWeaponGroups().clear();
                for (int i = 0; i < groups.length(); i++) {
                    JSONObject g = groups.getJSONObject(i);
                    WeaponGroupType type;
                    try {
                        type = WeaponGroupType.valueOf(g.optString("type", "LINKED"));
                    } catch (IllegalArgumentException ex) {
                        type = WeaponGroupType.LINKED;
                    }
                    WeaponGroupSpec group = new WeaponGroupSpec(type);
                    group.setAutofireOnByDefault(g.optBoolean("autofire", false));
                    JSONArray slots = g.optJSONArray("slots");
                    if (slots != null) {
                        for (int j = 0; j < slots.length(); j++) {
                            group.addSlot(slots.getString(j));
                        }
                    }
                    if (!group.getSlots().isEmpty()) {
                        module.addWeaponGroup(group);
                        restoredGroup = true;
                    }
                }
            }

        // 如果恢复了武器但没有武器组信息，自动生成武器组
            if (restoredWeapon && !restoredGroup) {
                module.autoGenerateWeaponGroups();
            }
        } catch (JSONException e) {
            // JSON解析异常时模块配置可能不完整，保留已应用的部分
        }
    }

    /**
     * 清除模块variant上所有可编辑的配置（通风口、电容、非内置武器、武器组、非内置船插）。
     * 内置武器和内置船插不受影响。
     */
    private static void clearEditableLoadout(ShipVariantAPI module) {
        if (module == null) {
            return;
        }
        module.setNumFluxVents(0);
        module.setNumFluxCapacitors(0);
        clearNonBuiltInWeapons(module);
        module.getWeaponGroups().clear();

        // 移除非内置船插
        for (String hullmod : new ArrayList<String>(module.getNonBuiltInHullmods())) {
            module.removeMod(hullmod);
        }
        // 移除非内置的永久船插
        for (String hullmod : new ArrayList<String>(module.getPermaMods())) {
            if (!isBuiltInHullmod(module, hullmod)) {
                module.removePermaMod(hullmod);
            }
        }
    }

    // ===================================================================
    //                        辅助方法
    // ===================================================================

    /** 添加普通船插（空值安全检查） */
    private static void addHullMod(ShipVariantAPI module, String hullmod) {
        if (hullmod != null && !hullmod.isEmpty()) {
            module.addMod(hullmod);
        }
    }

    /** 添加永久/S-mod船插（空值安全检查） */
    private static void addPermaHullMod(ShipVariantAPI module, String hullmod, boolean sMod) {
        if (hullmod != null && !hullmod.isEmpty()) {
            module.addPermaMod(hullmod, sMod);
        }
    }

    /** 判断船插是否为内置船插 */
    private static boolean isBuiltInHullmod(ShipVariantAPI module, String hullmod) {
        return module != null
                && module.getHullSpec() != null
                && hullmod != null
                && module.getHullSpec().isBuiltInMod(hullmod);
    }

    /**
     * 清理variant中不合法的station module条目。
     * 移除所有槽位类型不是STATION_MODULE的stationModule映射。
     */
    private static boolean sanitizeStationModules(ShipVariantAPI variant) {
        if (variant == null || variant.getHullSpec() == null || variant.getStationModules() == null) {
            return false;
        }
        boolean changed = false;
        Iterator<Map.Entry<String, String>> iterator = variant.getStationModules().entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            WeaponSlotAPI slot = variant.getHullSpec().getWeaponSlotAPI(entry.getKey());
            // 槽位不存在或类型不是STATION_MODULE，移除
            if (slot == null || slot.getWeaponType() != WeaponAPI.WeaponType.STATION_MODULE) {
                iterator.remove();
                changed = true;
            }
        }
        return changed;
    }

    // ===================================================================
    //                        标签读写
    // ===================================================================

    /**
     * 从variant的标签中读取当前模式索引。
     * 依次检查prefix+0到prefix+maxCount-1，找到第一个存在的标签即返回对应索引。
     * @param maxCount 最大模式数量（由CSV条目数决定）
     * @return 模式索引（从0开始），未找到返回-1
     */
    private static int readTagMode(ShipVariantAPI variant, String prefix, int maxCount) {
        if (variant == null) {
            return -1;
        }
        for (int i = 0; i < maxCount; i++) {
            if (variant.hasTag(prefix + i)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 将模式索引写入variant标签。
     * 先清除prefix+0到prefix+maxCount-1的旧标签，再添加新的。
     * @param maxCount 最大模式数量（由CSV条目数决定，确保清理所有可能的旧标签）
     */
    private static void writeTagMode(ShipVariantAPI variant, int mode, String prefix, int maxCount) {
        if (variant == null) {
            return;
        }
        // 清除所有同前缀标签，包括CSV缩减前遗留的越界模式。
        for (String tag : new ArrayList<String>(variant.getTags())) {
            if (tag.startsWith(prefix)) {
                variant.removeTag(tag);
            }
        }
        // 写入新模式标签
        variant.addTag(prefix + Math.max(0, mode));
    }

    /**
     * 从当前安装的浮动模块ID反推模式索引。
     * 用于兼容没有模式标签的旧存档。
     * @return 模式索引（0~3），未找到返回-1
     */
    private static int readModeFromModule(ShipVariantAPI variant, boolean light, String leftSlot) {
        if (variant == null) {
            return -1;
        }
        String hullId = variant.getStationModules().get(leftSlot);
        if (hullId == null) {
            return -1;
        }
        // 直接匹配CSV中注册的完整variantId
        List<MengFloatingModule> modules = light ? LIGHT_MODULES : HEAVY_MODULES;
        for (int i = 0; i < modules.size(); i++) {
            MengFloatingModule m = modules.get(i);
            if (m != null && (hullId.equals(m.leftVariant) || hullId.equals(m.rightVariant))) {
                return i;
            }
        }
        return -1;
    }

    // ===================================================================
    //                        槽位映射
    // ===================================================================

    /** 根据组索引获取对应的前缀 */
    private static String modePrefix(int group) {
        return group > 0 ? MODE_B_PREFIX : MODE_PREFIX;
    }

    /** 根据组索引获取左侧槽位ID */
    private static String leftSlot(int group) {
        return group > 0 ? FLOAT_B_LEFT : FLOAT_LEFT;
    }

    /** 根据组索引获取右侧槽位ID */
    private static String rightSlot(int group) {
        return group > 0 ? FLOAT_B_RIGHT : FLOAT_RIGHT;
    }

    /** 根据组索引获取中心槽位ID */
    private static String centerSlot(int group) {
        return group > 0 ? FLOAT_B_CENTER : FLOAT_CENTER;
    }

    /** 将模式索引限制在有效范围（已废弃，改用内联 Math.max/Math.min + getModeCount()） */
    @Deprecated
    private static int clampMode(int mode) {
        return Math.max(0, mode);
    }

    private static boolean isValidFixedSide(String side) {
        String normalized = side == null ? "" : side.trim().toUpperCase(java.util.Locale.ROOT);
        return "L".equals(normalized) || "R".equals(normalized) || "BOTH".equals(normalized);
    }
}
