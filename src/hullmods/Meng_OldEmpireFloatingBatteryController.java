package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponGroupAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import data.methods.Meng_ModuleSelectorScript;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * OldEmpire 浮游炮的总入口。
 *
 * <p>战役/改装阶段负责让模块配置与母舰槽位保持一致；进入战斗后仅创建
 * {@link Controller}。实际目标选择、编队计算和子舰操作分别由独立内部类完成。</p>
 */
public final class Meng_OldEmpireFloatingBatteryController extends BaseHullMod {
    private static final String KEY = "Meng_OldEmpire_floating_battery_controller";
    private static final float ORBIT_ACQUIRE_RANGE = 700f;
    private static final float ORBIT_RELEASE_RANGE = 825f;
    private static final float ORBIT_SPEED = 68f;
    private static final float MODULE_FORWARD_HALF_ARC = 60f;
    private static final float MODULE_AIM_TURN_RATE = 120f;
    private static final float MODULE_TARGET_SCAN_INTERVAL = 0.16f;

    @Override
    /**
     * 在战斗实体生成前同步 variant，保证 STATION_MODULE 槽位与模块选择器一致。
     *
     * @param hullSize 母舰船体尺寸，供选择器决定可安装模块数
     * @param stats 即将生成实体的属性，其中包含待同步的 variant
     * @param id hullmod 实例标识；本控制器不使用该参数
     */
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        ShipVariantAPI variant = stats.getVariant();
        syncVariant(variant, hullSize);
    }

    @Override
    /**
     * 战役帧同步入口。舰队管理或改装造成 variant 改变时，重新补齐模块槽位。
     *
     * @param member 当前舰队成员；为空或没有船体规格时不执行
     * @param amount 战役帧时长；同步为即时操作，不使用该参数
     */
    public void advanceInCampaign(FleetMemberAPI member, float amount) {
        if (member == null || member.getHullSpec() == null) {
            return;
        }
        syncVariant(member.getVariant(), member.getHullSpec().getHullSize());
    }

    @Override
    /**
     * 战斗实体创建完成后的入口。
     *
     * @param ship 已创建的母舰实体
     * @param id hullmod 实例标识；监听器通过自定义数据键去重
     */
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        String listenerKey = KEY + "_listener";
        if (!ship.getCustomData().containsKey(listenerKey)) {
            Controller controller = new Controller(ship);
            ship.setCustomData(listenerKey, controller);
            ship.addListener(controller);
        }
    }

    /**
     * 每艘母舰各有一个的战斗编排器。
     *
     * <p>调用链为：{@code advance -> FleetTargeting -> updateMode ->
     * FormationStrategy -> ModuleRuntime}。本类只维护生命周期和模块列表，
     * 不直接承担常规的移动、相位或通量结算。</p>
     */
    private static final class Controller implements AdvanceableListener {
        private final ShipAPI parent;
        private final FleetTargeting targeting;
        private final List<ModuleRuntime> modules = new ArrayList<ModuleRuntime>();
        private final List<ModuleRuntime> orbitNodes = new ArrayList<ModuleRuntime>();
        private final List<ModuleRuntime> viewNodes = new ArrayList<ModuleRuntime>();
        private final FormationStrategy formation;
        private boolean initialized;
        private boolean modulesRemoved;
        private boolean slotsRestored;
        private float elapsed;
        private float targetScan;
        private float orbitAngle;
        private ShipAPI orbitTarget;

        /**
         * 创建母舰级协调器，并让所有协作者共享同一艘母舰与模块列表。
         *
         * @param parent 装有本 hullmod 的母舰
         */
        private Controller(ShipAPI parent) {
            this.parent = parent;
            this.targeting = new FleetTargeting(parent);
            this.formation = new FormationStrategy(parent, orbitNodes, viewNodes);
        }

        @Override
        /**
         * 战斗每帧入口。
         *
         * <p>先处理初始化/结束/死亡等生命周期，再刷新共享目标快照；随后为每个模块
         * 选择目标、更新模式、计算姿态并执行运行时写入。</p>
         *
         * @param amount 本帧经过的秒数，用于转向、部署和轨道推进
         */
        public void advance(float amount) {
            if (Global.getCombatEngine() == null) {
                return;
            }
            if (!initialized) {
                initialize();
                if (!initialized) {
                    return;
                }
            }
            if (Global.getCombatEngine().isCombatOver()) {
                restoreModuleSlots();
                return;
            }
            if (Global.getCombatEngine().isPaused()) {
                return;
            }
            if (!parent.isAlive() || parent.isHulk()) {
                removeModulesFromCombat();
                return;
            }

            elapsed += amount;
            float deploy = smooth(Math.min(1f, elapsed / 1.1f));
            updateOrbitTarget(amount, deploy);
            targeting.advance(amount);
            if (orbitTarget != null) {
                orbitAngle = normalizeAngle(orbitAngle - ORBIT_SPEED * amount);
                formation.advanceOrbit(amount);
            }
            ShipSystemAPI parentCloak = parent.getPhaseCloak();
            boolean desiredPhaseOn = parentCloak != null
                    && parentCloak.isOn();
            for (ModuleRuntime state : modules) {
                updateModule(state, deploy, amount, desiredPhaseOn);
            }
        }

        /** 母舰失效时停止模块火力、复位槽位并从战场移除所有子舰。 */
        private void removeModulesFromCombat() {
            if (modulesRemoved || Global.getCombatEngine() == null) {
                return;
            }
            restoreModuleSlots();
            for (ModuleRuntime state : modules) {
                state.mode = ModuleMode.DISABLED;
            }
            for (ShipAPI child : parent.getChildModulesCopy()) {
                if (child == null) {
                    continue;
                }
                child.setCollisionClass(CollisionClass.NONE);
                for (WeaponAPI weapon : child.getAllWeapons()) {
                    if (weapon != null) {
                        weapon.stopFiring();
                        weapon.setForceNoFireOneFrame(true);
                    }
                }
                if (Global.getCombatEngine().isEntityInPlay(child)) {
                    Global.getCombatEngine().removeEntity(child);
                }
            }
            modulesRemoved = true;
        }

        // Station-slot specs are cloned by the combat entity but survive a simulator return.
        // Always put them back before the refit screen reuses this ship display.
        /**
         * 恢复战斗期间改写的 station slot，避免改装/舰队管理预览读取到战斗姿态。
         * 此方法可被监听器和战斗结束路径重复调用，内部保证只执行一次。
         */
        private void restoreModuleSlots() {
            if (slotsRestored) {
                return;
            }
            for (ModuleRuntime state : modules) {
                resetStationSlot(state);
                state.visualLocation.set(state.anchor);
                state.visualVelocity.set(0f, 0f);
            }
            slotsRestored = true;
        }

        /**
         * 从真实子舰建立不可变布局和可变运行时记录。
         *
         * <p>该方法会收集武器、分配环绕编号并安装战斗结束看门狗；没有子舰时保持
         * 未初始化，以适应 Starsector 分帧创建 station module 的时序。</p>
         */
        private void initialize() {
            List<ShipAPI> children = parent.getChildModulesCopy();
            if (children == null || children.isEmpty()) {
                return;
            }
            for (ShipAPI child : children) {
                if (child == null || child.getStationSlot() == null) {
                    continue;
                }
                // Station-module 朝向必须写入其挂点才能在引擎结算后保留。
                // 先克隆规格，确保本场战斗的角度不会污染母舰 variant/改装界面。
                child.ensureClonedStationSlotSpec();
                WeaponSlotAPI stationSlot = child.getStationSlot();
                WeaponSlotAPI baseSlot = parent.getHullSpec().getWeaponSlotAPI(stationSlot.getId());
                Vector2f anchor = baseSlot == null
                        ? new Vector2f(stationSlot.getLocation())
                        : new Vector2f(baseSlot.getLocation());
                float baseAngle = baseSlot == null ? stationSlot.getAngle() : baseSlot.getAngle();
                float side = anchor.y >= 0f ? 1f : -1f;
                float outward = 18f + Math.min(24f, child.getCollisionRadius() * 0.18f);
                Vector2f floatAnchor = new Vector2f(anchor.x, anchor.y + side * outward);
                float phase = Math.abs((child.getHullSpec().getHullId() + stationSlot.getId()).hashCode() % 1000) * 0.0137f;
                ModuleLayout layout = new ModuleLayout(child, stationSlot, anchor, floatAnchor,
                        baseAngle, phase, side);
                ModuleRuntime state = new ModuleRuntime(child, layout);
                modules.add(state);
                resetStationSlot(state);
                if (state.orbitNode) {
                    state.orbitIndex = orbitNodes.size();
                    orbitNodes.add(state);
                }
                if (state.viewNode) {
                    viewNodes.add(state);
                }
                ModuleRuntime.protect(child);
                collectModuleWeapons(child, state.weapons);
                setModuleAutofire(child, true);
            }
            initialized = !modules.isEmpty();
            if (initialized && Global.getCombatEngine() != null) {
                Global.getCombatEngine().addPlugin(
                        new SlotResetWatchdog(Global.getCombatEngine(), this));
            }
        }

        /**
         * 收集能参与射程判断和停火的实际武器。
         *
         * @param child 模块子舰
         * @param collected 接收结果的列表；装饰武器会被排除，BUILT_IN 武器保留
         */
        private void collectModuleWeapons(ShipAPI child, List<WeaponAPI> collected) {
            for (WeaponAPI weapon : child.getAllWeapons()) {
                if (weapon == null || weapon.isDecorative()) {
                    continue;
                }
                collected.add(weapon);
            }
        }

        /** 设置模块全部武器组的自动开火状态。@param child 模块子舰 @param enabled 是否开启。 */
        private static void setModuleAutofire(ShipAPI child, boolean enabled) {
            if (child == null) {
                return;
            }
            for (WeaponGroupAPI group : child.getWeaponGroupsCopy()) {
                if (enabled) {
                    group.toggleOn();
                } else {
                    group.toggleOff();
                }
            }
        }

        /**
         * 编排单个模块的一帧工作。
         *
         * @param state 模块的可变运行时记录
         * @param deploy 0 至 1 的开场部署插值
         * @param amount 本帧秒数
         * @param desiredPhaseOn 母舰相位装置当前期望状态
         */
        private void updateModule(ModuleRuntime state, float deploy, float amount,
                boolean desiredPhaseOn) {
            ShipAPI child = state.child;
            if (child == null || !child.isAlive()) {
                return;
            }
            updateModuleTarget(state);
            updateMode(state);
            ModulePose pose = formation.poseFor(state, state.mode, orbitTarget,
                    deploy, elapsed, amount);
            state.advance(parent, pose, amount, desiredPhaseOn, state.target, orbitTarget);
        }

        /** 使用本帧共享的 FleetTargeting 快照，为模块写入射程内的优先目标。 */
        private void updateModuleTarget(ModuleRuntime state) {
            state.target = targeting.select(state);
        }

        /**
         * 根据模块类型和环绕目标更新状态机。
         * ORBIT 丢失目标后先进入 RETURNING，抵达待机编队后才回到 FOLLOW。
         *
         * @param state 需要转换状态的模块
         */
        private void updateMode(ModuleRuntime state) {
            if (state.mode == ModuleMode.DISABLED) {
                return;
            }
            if (state.layout.orbitNode && orbitTarget != null) {
                state.mode = ModuleMode.ORBIT;
            } else if (state.mode == ModuleMode.ORBIT) {
                state.mode = ModuleMode.RETURNING;
            } else if (state.mode == ModuleMode.RETURNING
                    && formation.isNearFollowPosition(state)) {
                state.mode = ModuleMode.FOLLOW;
            } else if (state.mode != ModuleMode.RETURNING) {
                state.mode = ModuleMode.FOLLOW;
            }
        }

        /**
         * 维护母舰共享的环绕目标，带有独立节流以避免每帧扫描全场。
         *
         * @param amount 本帧秒数
         * @param deploy 当前部署插值，用于离开环绕时计算回归位置
         */
        private void updateOrbitTarget(float amount, float deploy) {
            if (orbitNodes.isEmpty()) {
                orbitTarget = null;
                return;
            }

            if (isValidOrbitTarget(orbitTarget, ORBIT_RELEASE_RANGE)) {
                return;
            }

            targetScan -= amount;
            if (targetScan > 0f) {
                return;
            }
            targetScan = 0.12f;

            ShipAPI next = findOrbitTarget();
            if (next == orbitTarget) {
                return;
            }
            if (next == null) {
                leaveOrbit(deploy);
            } else {
                enterOrbit(next);
            }
        }

        /** 优先使用母舰当前目标，否则在获取距离内挑选最近有效目标。 */
        private ShipAPI findOrbitTarget() {
            ShipAPI selected = parent.getShipTarget();
            if (isValidOrbitTarget(selected, ORBIT_ACQUIRE_RANGE)) {
                return selected;
            }

            ShipAPI nearest = null;
            float nearestDistanceSquared = ORBIT_ACQUIRE_RANGE * ORBIT_ACQUIRE_RANGE;
            for (ShipAPI candidate : Global.getCombatEngine().getShips()) {
                if (!isValidOrbitTarget(candidate, ORBIT_ACQUIRE_RANGE)) {
                    continue;
                }
                float distanceSquared = distanceSquared(parent.getLocation(), candidate.getLocation());
                if (distanceSquared < nearestDistanceSquared) {
                    nearest = candidate;
                    nearestDistanceSquared = distanceSquared;
                }
            }
            return nearest;
        }

        /** 检查环绕目标是否仍敌对、在场、未相位且位于指定距离内。 */
        private boolean isValidOrbitTarget(ShipAPI target, float range) {
            if (target == null || target == parent || !target.isAlive() || target.isHulk()
                    || target.isFighter() || target.isDrone() || target.isStationModule()
                    || target.isPhased() || target.getOwner() == parent.getOwner()
                    || target.getOwner() == 100 || Global.getCombatEngine() == null
                    || !Global.getCombatEngine().isEntityInPlay(target)) {
                return false;
            }
            return distanceSquared(parent.getLocation(), target.getLocation()) <= range * range;
        }

        /**
         * 切入环绕：记录初始相位、立即摆放环绕节点并播放转场粒子。
         *
         * @param target 新获得的有效环绕目标
         */
        private void enterOrbit(ShipAPI target) {
            orbitTarget = target;
            orbitAngle = angleDegrees(target.getLocation(), parent.getLocation());
            formation.setOrbitAngle(orbitAngle);
            for (ModuleRuntime state : orbitNodes) {
                Vector2f oldWorld = new Vector2f(state.child.getLocation());
                Vector2f newWorld = getOrbitWorldLocation(state, target);
                Vector2f local = worldToParentLocal(newWorld);
                state.visualLocation.set(local);
                state.visualVelocity.set(0f, 0f);
                float targetFacing = angleDegrees(newWorld, target.getLocation());
                moveChildToLocal(state, local, normalizeAngle(targetFacing - parent.getFacing()));
                emitBlink(oldWorld, state.viewNode);
                emitBlink(newWorld, state.viewNode);
            }
        }

        /**
         * 退出环绕：将环绕节点放回待机位置。后续平滑移动由 FormationStrategy 接管。
         *
         * @param deploy 当前部署插值
         */
        private void leaveOrbit(float deploy) {
            if (orbitTarget == null) {
                return;
            }
            orbitTarget = null;
            for (ModuleRuntime state : orbitNodes) {
                Vector2f oldWorld = new Vector2f(state.child.getLocation());
                Vector2f idle = state.viewNode
                        ? getViewFormationLocation(state)
                        : new Vector2f(
                                lerp(state.anchor.x, state.floatAnchor.x, deploy),
                                lerp(state.anchor.y, state.floatAnchor.y, deploy));
                state.visualLocation.set(idle);
                state.visualVelocity.set(0f, 0f);
                moveChildToLocal(state, idle, state.baseAngle);
                emitBlink(oldWorld, state.viewNode);
                emitBlink(parentLocalToWorld(idle), state.viewNode);
            }
        }

        /**
         * 将局部坐标和相对角度写入真实子舰与其战斗专用挂点副本。
         * 初始化已调用 ensureClonedStationSlotSpec()，因此这里不会污染改装数据。
         */
        private void moveChildToLocal(ModuleRuntime state, Vector2f local, float relativeFacing) {
            ShipAPI child = state.child;
            if (child == null) {
                return;
            }
            state.stationSlot.setAngle(relativeFacing);
            // 局部坐标转回战场坐标，只改变战斗实体的位置。
            child.getLocation().set(parentLocalToWorld(local));
            // 继承母舰速度，避免模块被移动后留下静止的速度残差。
            child.getVelocity().set(parent.getVelocity());
            // 主舰朝向加相对角度就是模块的最终朝向。
            child.setFacing(normalizeAngle(parent.getFacing() + relativeFacing));
            child.setAngularVelocity(parent.getAngularVelocity());
        }

        /** 恢复模块的初始槽位锚点与角度，供战斗结束和异常离场使用。 */
        private void resetStationSlot(ModuleRuntime state) {
            // 战斗实体、实体 variant 与 FleetMember variant 可能各自持有 HullSpec。
            // 舰队管理切换舰船时会重新从后两者创建预览，所以三个层级都必须恢复。
            state.stationSlot.getLocation().set(state.anchor);
            state.stationSlot.setAngle(state.baseAngle);
            String slotId = state.stationSlot.getId();
            restoreHullSpecSlot(parent.getHullSpec(), slotId, state);
            if (parent.getVariant() != null) {
                restoreHullSpecSlot(parent.getVariant().getHullSpec(), slotId, state);
            }
            FleetMemberAPI member = parent.getFleetMember();
            if (member != null && member.getVariant() != null) {
                restoreHullSpecSlot(member.getVariant().getHullSpec(), slotId, state);
            }
            // 同时把当前战斗实体放回该锚点，供战斗结束和异常移除使用。
            moveChildToLocal(state, state.anchor, state.baseAngle);
        }

        /**
         * 复原一个可能被 UI 或战斗实体复制出来的 HullSpec 槽位。
         *
         * @param hullSpec 要复原的规格副本；为空时忽略
         * @param slotId station module 槽位 ID
         * @param state 保存初始锚点和初始角度的模块布局
         */
        private void restoreHullSpecSlot(ShipHullSpecAPI hullSpec, String slotId,
                                         ModuleRuntime state) {
            if (hullSpec == null) {
                return;
            }
            WeaponSlotAPI slot = hullSpec.getWeaponSlotAPI(slotId);
            if (slot != null) {
                slot.getLocation().set(state.anchor);
                slot.setAngle(state.baseAngle);
            }
        }

        /** 为环绕切换动画按模块索引计算目标周围的世界坐标。 */
        private Vector2f getOrbitWorldLocation(ModuleRuntime state, ShipAPI target) {
            int count = Math.max(1, orbitNodes.size());
            float angle = orbitAngle + 360f * state.orbitIndex / count;
            float radius = target.getCollisionRadius()
                    + Math.max(92f, state.child.getCollisionRadius() + 68f);
            double radians = Math.toRadians(angle);
            return new Vector2f(
                    target.getLocation().x + (float) Math.cos(radians) * radius,
                    target.getLocation().y + (float) Math.sin(radians) * radius);
        }

        /** 依据 09 模块所属组和角色计算待机编队局部坐标。 */
        private Vector2f getViewFormationLocation(ModuleRuntime state) {
            float radius = parent.getCollisionRadius()
                    + Math.max(92f, state.child.getCollisionRadius() + 58f);
            boolean dualView = viewNodes.size() >= 6;
            float angle;
            if (!dualView || state.group == 0) {
                angle = state.role == 0 ? 0f : state.role == 1 ? 120f : 240f;
            } else {
                angle = state.role == 0 ? 180f : state.role == 1 ? 60f : 300f;
            }
            double radians = Math.toRadians(angle);
            return new Vector2f(
                    (float) Math.cos(radians) * radius,
                    (float) Math.sin(radians) * radius);
        }

        /** 将世界坐标投影到母舰的前向/左向局部坐标系。 */
        private Vector2f worldToParentLocal(Vector2f world) {
            float radians = (float) Math.toRadians(parent.getFacing());
            float forwardX = (float) Math.cos(radians);
            float forwardY = (float) Math.sin(radians);
            float leftX = -forwardY;
            float leftY = forwardX;
            float dx = world.x - parent.getLocation().x;
            float dy = world.y - parent.getLocation().y;
            return new Vector2f(
                    dx * forwardX + dy * forwardY,
                    dx * leftX + dy * leftY);
        }

        /** 将母舰局部坐标转换成世界坐标。 */
        private Vector2f parentLocalToWorld(Vector2f local) {
            float radians = (float) Math.toRadians(parent.getFacing());
            float forwardX = (float) Math.cos(radians);
            float forwardY = (float) Math.sin(radians);
            float leftX = -forwardY;
            float leftY = forwardX;
            return new Vector2f(
                    parent.getLocation().x + local.x * forwardX + local.y * leftX,
                    parent.getLocation().y + local.x * forwardY + local.y * leftY);
        }

        /** 在进出环绕时生成颜色区分 08/09 的纯视觉粒子。 */
        private void emitBlink(Vector2f location, boolean viewNode) {
            if (Global.getCombatEngine() == null || location == null) {
                return;
            }
            Color color = viewNode
                    ? new Color(90, 205, 255, 165)
                    : new Color(255, 55, 75, 165);
            Global.getCombatEngine().addHitParticle(
                    location, new Vector2f(), 42f, 0.65f, 0.11f, color);
            for (int i = 0; i < 4; i++) {
                float angle = (float) Math.random() * 360f;
                float speed = 28f + (float) Math.random() * 46f;
                double radians = Math.toRadians(angle);
                Vector2f velocity = new Vector2f(
                        (float) Math.cos(radians) * speed,
                        (float) Math.sin(radians) * speed);
                Global.getCombatEngine().addSmoothParticle(
                        location, velocity, 9f + (float) Math.random() * 8f,
                        0.65f, 0.22f, color);
            }
        }

        /** 标量线性插值；level 为 0 至 1 时结果落在 from 和 to 之间。 */
        private static float lerp(float from, float to, float level) {
            return from + (to - from) * level;
        }

        /** 部署期 smoothstep 插值，避免开场模块突然加速。 */
        private static float smooth(float level) {
            return level * level * (3f - 2f * level);
        }

        /** 将任意角度归一化到 [0, 360)。 */
        private static float normalizeAngle(float angle) {
            // 归一化到 0 至 360 度，避免长期环绕导致角度无限累积。
            float normalized = angle % 360f;
            return normalized < 0f ? normalized + 360f : normalized;
        }

        /** 返回从 from 指向 to 的战场世界角度，单位为度。 */
        private static float angleDegrees(Vector2f from, Vector2f to) {
            // atan2 返回 from 指向 to 的二维方向角，随后由弧度转成 Starsector 使用的角度。
            return (float) Math.toDegrees(Math.atan2(to.y - from.y, to.x - from.x));
        }

        private static float distanceSquared(Vector2f first, Vector2f second) {
            float dx = first.x - second.x;
            float dy = first.y - second.y;
            return dx * dx + dy * dy;
        }

        /**
         * 战斗结束保险：母舰监听器不再推进时仍确保 station slot 被复位。
         * 它只观察引擎状态，不处理输入或模块行为。
         */
        private static final class SlotResetWatchdog extends BaseEveryFrameCombatPlugin {
            private final CombatEngineAPI engine;
            private final Controller controller;
            private boolean done;

            /** @param engine 当前战斗引擎 @param controller 要复位的母舰控制器 */
            private SlotResetWatchdog(CombatEngineAPI engine, Controller controller) {
                this.engine = engine;
                this.controller = controller;
            }

            @Override
            /** @param amount 本帧秒数 @param events 输入事件，本插件不消费它们。 */
            public void advance(float amount, List<InputEventAPI> events) {
                if (done || engine == null || controller == null) {
                    done = true;
                    return;
                }
                if (engine.isCombatOver() || !engine.isEntityInPlay(controller.parent)) {
                    controller.restoreModuleSlots();
                    done = true;
                }
            }
        }
    }

    /**
     * 仅为 OldEmpire 母舰同步模块 variant；其他船只直接返回。
     *
     * @param variant 待检查和可能改写的配置
     * @param hullSize 母舰尺寸，传递给模块选择器
     */
    private static void syncVariant(ShipVariantAPI variant, ShipAPI.HullSize hullSize) {
        if (!Meng_ModuleSelectorScript.isOldEmpireParent(variant)) {
            return;
        }

        boolean changed = Meng_ModuleSelectorScript.syncVariant(variant, hullSize);

        if (changed) {
            variant.setSource(VariantSource.REFIT);
        }
    }

    /** 模块的位置状态机。目标、朝向和火力控制不依赖额外布尔状态。 */
    private enum ModuleMode {
        FOLLOW,
        ORBIT,
        RETURNING,
        DISABLED
    }

    /**
     * FormationStrategy 交给 ModuleRuntime 的不可变姿态结果。
     * local 使用母舰局部坐标；desiredWorldFacing 使用战场世界角度。
     */
    private static final class ModulePose {
        private final Vector2f local;
        private final float desiredWorldFacing;

        /** @param local 模块应到达的局部位置 @param desiredWorldFacing 模块期望世界朝向 */
        private ModulePose(Vector2f local, float desiredWorldFacing) {
            this.local = local;
            this.desiredWorldFacing = desiredWorldFacing;
        }
    }

    /**
     * Computes formation only. It never changes a combat entity, flux tracker,
     * phase cloak, weapon, or station slot.
     */
    private static final class FormationStrategy {
        private final ShipAPI parent;
        private final List<ModuleRuntime> orbitNodes;
        private final List<ModuleRuntime> viewNodes;
        private float orbitAngle;

        /** @param parent 母舰 @param orbitNodes 参与环绕的模块 @param viewNodes 09 编队模块 */
        private FormationStrategy(ShipAPI parent, List<ModuleRuntime> orbitNodes,
                                  List<ModuleRuntime> viewNodes) {
            this.parent = parent;
            this.orbitNodes = orbitNodes;
            this.viewNodes = viewNodes;
        }

        /**
         * 计算一帧姿态，不直接改变任何 ShipAPI。
         *
         * @param runtime 模块的当前运动与目标状态
         * @param mode FOLLOW、RETURNING 或 ORBIT 位置策略
         * @param orbitTarget 存在时作为 ORBIT 圆心
         * @param deploy 开场部署插值
         * @param elapsed 战斗已运行秒数，用于漂浮相位
         * @param amount 本帧秒数，用于平滑移动
         */
        private ModulePose poseFor(ModuleRuntime runtime, ModuleMode mode,
                                   ShipAPI orbitTarget, float deploy, float elapsed,
                                   float amount) {
            if (mode == ModuleMode.ORBIT && orbitTarget != null) {
                Vector2f world = orbitLocation(runtime, orbitTarget);
                runtime.visualLocation.set(worldToLocal(world));
                runtime.visualVelocity.set(0f, 0f);
                return new ModulePose(new Vector2f(runtime.visualLocation),
                        angle(world, orbitTarget.getLocation()));
            }

            float driftForward = (float) Math.sin(elapsed * 1.1f + runtime.phase) * 5.5f;
            float driftSide = (float) Math.sin(elapsed * 1.55f + runtime.phase * 1.31f)
                    * 4f * runtime.side;
            Vector2f deployed = runtime.viewNode ? viewLocation(runtime) : runtime.floatAnchor;
            float targetX = lerp(runtime.anchor.x, deployed.x, deploy) + driftForward;
            float targetY = lerp(runtime.anchor.y, deployed.y, deploy) + driftSide;
            float radians = (float) Math.toRadians(parent.getFacing());
            float forwardX = (float) Math.cos(radians);
            float forwardY = (float) Math.sin(radians);
            Vector2f velocity = parent.getVelocity();
            targetX -= clamp((velocity.x * forwardX + velocity.y * forwardY) * .055f, -22f, 22f);
            targetY -= clamp((velocity.x * -forwardY + velocity.y * forwardX) * .075f, -26f, 26f);
            targetY -= clamp(parent.getAngularVelocity() * .18f, -20f, 20f);
            float follow = 1f - (float) Math.exp(-amount * 4.5f);
            runtime.visualLocation.x += (targetX - runtime.visualLocation.x) * follow;
            runtime.visualLocation.y += (targetY - runtime.visualLocation.y) * follow;
            runtime.visualVelocity.x += (targetX - runtime.visualLocation.x) * amount * 9f;
            runtime.visualVelocity.y += (targetY - runtime.visualLocation.y) * amount * 9f;
            runtime.visualVelocity.scale(Math.max(0f, 1f - amount * 5.5f));
            runtime.visualLocation.x += runtime.visualVelocity.x * amount;
            runtime.visualLocation.y += runtime.visualVelocity.y * amount;

            float facing = parent.getFacing();
            if (runtime.target != null && isInForwardSector(runtime.target)) {
                facing = angle(localToWorld(runtime.visualLocation), runtime.target.getLocation());
            }
            return new ModulePose(new Vector2f(runtime.visualLocation), facing);
        }

        /** 判断返航模块是否已经足够接近其普通待机位置。 */
        private boolean isNearFollowPosition(ModuleRuntime runtime) {
            Vector2f follow = runtime.viewNode ? viewLocation(runtime) : runtime.floatAnchor;
            return distanceSquared(runtime.visualLocation, follow) < 64f;
        }

        /** 按固定环绕速度推进共享相位。@param amount 本帧秒数。 */
        private void advanceOrbit(float amount) {
            orbitAngle = normalize(orbitAngle - ORBIT_SPEED * amount);
        }

        /** 进入环绕时以母舰相对目标的方位初始化共享环绕相位。 */
        private void setOrbitAngle(float angle) {
            orbitAngle = normalize(angle);
        }

        /** 计算一个环绕节点在目标圆周上的世界坐标。 */
        private Vector2f orbitLocation(ModuleRuntime runtime, ShipAPI target) {
            int count = Math.max(1, orbitNodes.size());
            float angle = orbitAngle + 360f * runtime.orbitIndex / count;
            float radius = target.getCollisionRadius()
                    + Math.max(92f, runtime.child.getCollisionRadius() + 68f);
            double radians = Math.toRadians(angle);
            return new Vector2f(target.getLocation().x + (float) Math.cos(radians) * radius,
                    target.getLocation().y + (float) Math.sin(radians) * radius);
        }

        /** 计算 09 模块三角或双三角编队的局部坐标。 */
        private Vector2f viewLocation(ModuleRuntime runtime) {
            float radius = parent.getCollisionRadius()
                    + Math.max(92f, runtime.child.getCollisionRadius() + 58f);
            boolean dual = viewNodes.size() >= 6;
            float angle = !dual || runtime.group == 0
                    ? runtime.role == 0 ? 0f : runtime.role == 1 ? 120f : 240f
                    : runtime.role == 0 ? 180f : runtime.role == 1 ? 60f : 300f;
            double radians = Math.toRadians(angle);
            return new Vector2f((float) Math.cos(radians) * radius,
                    (float) Math.sin(radians) * radius);
        }

        /** 判断目标是否位于母舰前方正负 60 度扇区内。 */
        private boolean isInForwardSector(ShipAPI target) {
            return Math.abs(rotation(parent.getFacing(), angle(parent.getLocation(), target.getLocation())))
                    <= MODULE_FORWARD_HALF_ARC;
        }

        /** 将世界位置映射到母舰局部前向/左向坐标。 */
        private Vector2f worldToLocal(Vector2f world) {
            float radians = (float) Math.toRadians(parent.getFacing());
            float forwardX = (float) Math.cos(radians), forwardY = (float) Math.sin(radians);
            float dx = world.x - parent.getLocation().x, dy = world.y - parent.getLocation().y;
            return new Vector2f(dx * forwardX + dy * forwardY, dx * -forwardY + dy * forwardX);
        }

        /** 将局部前向/左向坐标映射回世界位置。 */
        private Vector2f localToWorld(Vector2f local) {
            float radians = (float) Math.toRadians(parent.getFacing());
            float forwardX = (float) Math.cos(radians), forwardY = (float) Math.sin(radians);
            return new Vector2f(parent.getLocation().x + local.x * forwardX - local.y * forwardY,
                    parent.getLocation().y + local.x * forwardY + local.y * forwardX);
        }

        /** 返回二维向量 from 指向 to 的角度。 */
        private static float angle(Vector2f from, Vector2f to) {
            return (float) Math.toDegrees(Math.atan2(to.y - from.y, to.x - from.x));
        }
        /** 返回 from 转到 to 的最短有符号角差，范围为 [-180, 180)。 */
        private static float rotation(float from, float to) { return ((to - from + 540f) % 360f) - 180f; }
        /** 将角度限制在 [0, 360)。 */
        private static float normalize(float value) { value %= 360f; return value < 0f ? value + 360f : value; }
        /** 在 a 与 b 间线性插值。 */
        private static float lerp(float a, float b, float t) { return a + (b - a) * t; }
        /** 将数值限制于闭区间 [min, max]。 */
        private static float clamp(float v, float min, float max) { return Math.max(min, Math.min(max, v)); }
        /** 返回二维平方距离。 */
        private static float distanceSquared(Vector2f a, Vector2f b) { float x = a.x-b.x, y = a.y-b.y; return x*x+y*y; }
    }

    /**
     * Owns only module combat target selection. Positioning, facing, and firing
     * remain in the controller, so changes to target priorities cannot affect
     * formation or station-slot restoration.
     */
    /**
     * 母舰级目标快照。
     *
     * <p>每 0.16 秒只扫描一次全场；模块在该快照中按自己最长武器射程取目标，
     * 避免各个模块独立扫描造成性能浪费和锁敌分裂。</p>
     */
    private static final class FleetTargeting {
        private final ShipAPI parent;
        private final List<ShipAPI> nonFighterCandidates = new ArrayList<ShipAPI>();
        private final List<ShipAPI> otherCandidates = new ArrayList<ShipAPI>();
        private ShipAPI parentTarget;
        private float refreshTimer;

        /** @param parent 候选敌我关系与优先目标的判定基准。 */
        private FleetTargeting(ShipAPI parent) {
            this.parent = parent;
        }

        /**
         * 必要时刷新候选快照。
         *
         * @param amount 本帧秒数；用于累计扫描间隔
         */
        private void advance(float amount) {
            refreshTimer -= amount;
            if (refreshTimer > 0f) {
                return;
            }
            refreshTimer = MODULE_TARGET_SCAN_INTERVAL;
            parentTarget = parent.getShipTarget();
            nonFighterCandidates.clear();
            otherCandidates.clear();
            if (Global.getCombatEngine() == null) {
                return;
            }
            for (ShipAPI candidate : Global.getCombatEngine().getShips()) {
                if (!isCandidate(candidate)) {
                    continue;
                }
                if (candidate.isFighter() || candidate.isDrone()) {
                    otherCandidates.add(candidate);
                } else {
                    nonFighterCandidates.add(candidate);
                }
            }
        }

        /**
         * 从共享快照选择一个模块可攻击的目标：母舰目标、最近非舰载机、其他目标。
         *
         * @param state 请求目标的模块，其最长武器射程决定可选范围
         * @return 目标；没有射程内候选时返回 null
         */
        private ShipAPI select(ModuleRuntime state) {
            float range = longestWeaponRange(state);
            if (range <= 0f) {
                return null;
            }
            if (isInRange(state, parentTarget, range)) {
                return parentTarget;
            }
            ShipAPI nonFighter = findNearest(state, nonFighterCandidates, range);
            return nonFighter != null
                    ? nonFighter : findNearest(state, otherCandidates, range);
        }

        /** 在指定候选列表中查找距模块最近且仍在射程内的实体。 */
        private ShipAPI findNearest(ModuleRuntime state, List<ShipAPI> candidates, float range) {
            ShipAPI nearest = null;
            float nearestNonFighterDistance = range * range;
            for (ShipAPI candidate : candidates) {
                if (!isInRange(state, candidate, range)) {
                    continue;
                }
                float distance = distanceSquared(state.child.getLocation(), candidate.getLocation());
                if (distance < nearestNonFighterDistance) {
                    nearestNonFighterDistance = distance;
                    nearest = candidate;
                }
            }
            return nearest;
        }

        /** 统一排除不可交战的目标，例如同阵营、残骸、相位和 station module。 */
        private boolean isCandidate(ShipAPI target) {
            return target != null && target != parent && target.isAlive() && !target.isHulk()
                    && !target.isStationModule() && !target.isPhased()
                    && target.getOwner() != parent.getOwner() && target.getOwner() != 100
                    && Global.getCombatEngine().isEntityInPlay(target);
        }

        /** 将目标有效性与模块到目标的实际平方距离合并判断。 */
        private boolean isInRange(ModuleRuntime state, ShipAPI target, float range) {
            return state != null && state.child != null && isCandidate(target)
                    && distanceSquared(state.child.getLocation(), target.getLocation()) <= range * range;
        }

        /** 返回模块全部非装饰武器中的最大实际射程。 */
        private static float longestWeaponRange(ModuleRuntime state) {
            float longestRange = 0f;
            for (WeaponAPI weapon : state.weapons) {
                if (weapon != null) {
                    longestRange = Math.max(longestRange, weapon.getRange());
                }
            }
            return longestRange;
        }

        /** 返回平方距离，供最近目标比较使用以避免开方。 */
        /** 供距离排序使用的平方距离工具。 */
        private static float distanceSquared(Vector2f first, Vector2f second) {
            float dx = first.x - second.x;
            float dy = first.y - second.y;
            return dx * dx + dy * dy;
        }
    }

    /**
     * 模块的不可变布局数据，只在 Controller.initialize() 中创建一次。
     *
     * <p>其中包括 station slot、初始锚点、待机偏移、08/09 分类和 09 队列角色；
     * 战斗帧不应修改这些数据。</p>
     */
    private static final class ModuleLayout {
        private final WeaponSlotAPI stationSlot;
        private final Vector2f anchor;
        private final Vector2f floatAnchor;
        private final float baseAngle;
        private final float phase;
        private final float side;
        private final boolean orbitNode;
        private final boolean viewNode;
        private final int group;
        private final int role;

        /**
         * 从子舰和母舰槽位建立布局快照。
         *
         * @param child 模块实体，用其 hull id 识别 08/09 类型
         * @param stationSlot 模块挂接的母舰槽位
         * @param anchor 初始局部锚点
         * @param floatAnchor 悬停待机局部锚点
         * @param baseAngle 初始槽位相对角度
         * @param phase 视觉漂浮的确定性相位
         * @param side 模块位于母舰左侧或右侧的符号
         */
        private ModuleLayout(ShipAPI child, WeaponSlotAPI stationSlot, Vector2f anchor,
                             Vector2f floatAnchor, float baseAngle, float phase, float side) {
            this.stationSlot = stationSlot;
            this.anchor = new Vector2f(anchor);
            this.floatAnchor = new Vector2f(floatAnchor);
            this.baseAngle = baseAngle;
            this.phase = phase;
            this.side = side;
            String hullId = child.getHullSpec().getHullId();
            this.viewNode = hullId.startsWith("Meng_OldEmpire_float_09_");
            this.orbitNode = viewNode || hullId.startsWith("Meng_OldEmpire_float_08_");
            String slotId = stationSlot.getId();
            this.group = slotId.contains("_B_") ? 1 : 0;
            this.role = slotId.endsWith("_C") ? 0 : slotId.endsWith("_L") ? 1 : 2;
        }
    }

    /**
     * 单个模块的可变战斗状态，也是唯一允许写入子舰 ShipAPI 的层。
     *
     * <p>调用流程为 Controller 先写 target/mode，再由 FormationStrategy 产生 ModulePose，
     * 最后调用本类 advance() 完成实体移动、转向、相位、通量和开火抑制。</p>
     */
    private static final class ModuleRuntime {
        private final ModuleLayout layout;
        private final ShipAPI child;
        private final WeaponSlotAPI stationSlot;
        private final Vector2f anchor;
        private final Vector2f floatAnchor;
        private final Vector2f visualLocation;
        private final Vector2f visualVelocity = new Vector2f();
        private final float baseAngle;
        private final float phase;
        private final float side;
        private final boolean orbitNode;
        private final boolean viewNode;
        private final int group;
        private final int role;
        private int orbitIndex;
        private ShipAPI target;
        private final List<WeaponAPI> weapons = new ArrayList<WeaponAPI>();
        private ModuleMode mode = ModuleMode.FOLLOW;

        /** @param child 真实 station-module 子舰 @param layout 初始化期间建立的不可变布局 */
        private ModuleRuntime(ShipAPI child, ModuleLayout layout) {
            this.layout = layout;
            this.child = child;
            this.stationSlot = layout.stationSlot;
            this.anchor = layout.anchor;
            this.floatAnchor = layout.floatAnchor;
            this.visualLocation = new Vector2f(layout.anchor);
            this.baseAngle = layout.baseAngle;
            this.phase = layout.phase;
            this.side = layout.side;
            this.orbitNode = layout.orbitNode;
            this.viewNode = layout.viewNode;
            this.group = layout.group;
            this.role = layout.role;
        }

        /**
         * 将已经计算好的姿态应用到真实子舰，并同步母舰相关的战斗状态。
         *
         * @param parent 母舰，提供位置、朝向、速度、相位和通量状态
         * @param pose FormationStrategy 计算的目标局部位置与世界朝向
         * @param amount 本帧秒数，决定普通模式最大 120 度/秒转向
         * @param desiredPhaseOn 母舰相位装置是否开启
         * @param selectedTarget FleetTargeting 为本模块选中的目标
         * @param orbitTarget 当前共享环绕目标，作为无独立目标时的后备
         */
        private void advance(ShipAPI parent, ModulePose pose, float amount, boolean desiredPhaseOn,
                             ShipAPI selectedTarget, ShipAPI orbitTarget) {
            if (child == null || !child.isAlive() || mode == ModuleMode.DISABLED) {
                return;
            }
            protect(child);
            ShipSystemAPI cloak = child.getPhaseCloak();
            if (cloak != null) {
                if (cloak.isOn() != desiredPhaseOn) {
                    child.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
                } else {
                    child.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK);
                }
            }
            child.setPhased(parent.isPhased());
            child.setOwner(parent.getOwner());
            child.setAlly(parent.isAlly());
            child.setShipTarget(selectedTarget != null ? selectedTarget
                    : orbitNode && orbitTarget != null ? orbitTarget : parent.getShipTarget());

            float current = normalize(child.getFacing());
            float turn = clamp(rotation(current, pose.desiredWorldFacing),
                    -MODULE_AIM_TURN_RATE * amount, MODULE_AIM_TURN_RATE * amount);
            // Orbit modules intentionally snap to the target; follow/return modules
            // retain the verified 120 degree-per-second smoothing.
            float facing = mode == ModuleMode.ORBIT ? pose.desiredWorldFacing : current + turn;
            // 该 slot 已在初始化时克隆，仅用于本战斗的 station-module 朝向结算。
            stationSlot.setAngle(normalize(facing - parent.getFacing()));
            child.getLocation().set(toWorld(parent, pose.local));
            child.getVelocity().set(parent.getVelocity());
            child.setFacing(normalize(facing));
            child.setAngularVelocity(parent.getAngularVelocity());

            float flux = child.getFluxTracker().getCurrFlux();
            float moved = Math.min(Math.max(0f, parent.getMaxFlux() - parent.getCurrFlux()), flux);
            if (moved > 0f) {
                parent.getFluxTracker().increaseFlux(moved, false);
                child.getFluxTracker().decreaseFlux(moved);
            }
            if (parent.isPhased() || parent.getFluxTracker().isOverloadedOrVenting()
                    || parent.getCurrFlux() >= parent.getMaxFlux() * .985f) {
                for (WeaponAPI weapon : weapons) {
                    weapon.stopFiring();
                    weapon.setForceNoFireOneFrame(true);
                }
            }
        }

        /** 将子舰配置为不可碰撞、不可受伤且不会自行泄能/使用系统的浮游炮实体。 */
        private static void protect(ShipAPI child) {
            child.setCollisionClass(CollisionClass.NONE);
            child.setInvalidTransferCommandTarget(true);
            child.setDrone(true);
            child.setLayer(CombatEngineLayers.FIGHTERS_LAYER);
            child.setHitpoints(child.getMaxHitpoints());
            child.blockCommandForOneFrame(ShipCommand.VENT_FLUX);
            child.blockCommandForOneFrame(ShipCommand.USE_SYSTEM);
            MutableShipStatsAPI stats = child.getMutableStats();
            stats.getHullDamageTakenMult().modifyMult(KEY, 0f);
            stats.getArmorDamageTakenMult().modifyMult(KEY, 0f);
            stats.getShieldDamageTakenMult().modifyMult(KEY, 0f);
            stats.getEngineDamageTakenMult().modifyMult(KEY, 0f);
            stats.getWeaponDamageTakenMult().modifyMult(KEY, 0f);
            stats.getEmpDamageTakenMult().modifyMult(KEY, 0f);
        }

        /** 按母舰当前朝向将局部坐标转换为世界坐标。 */
        private static Vector2f toWorld(ShipAPI parent, Vector2f local) {
            double radians = Math.toRadians(parent.getFacing());
            float forwardX = (float) Math.cos(radians), forwardY = (float) Math.sin(radians);
            return new Vector2f(parent.getLocation().x + local.x * forwardX - local.y * forwardY,
                    parent.getLocation().y + local.x * forwardY + local.y * forwardX);
        }
        /** 将数值限制到给定闭区间。 */
        private static float clamp(float v, float min, float max) { return Math.max(min, Math.min(max, v)); }
        /** 返回两个角度的最短有符号差。 */
        private static float rotation(float from, float to) { return ((to - from + 540f) % 360f) - 180f; }
        /** 将角度归一化到 [0, 360)。 */
        private static float normalize(float value) { value %= 360f; return value < 0f ? value + 360f : value; }
    }
}
