package data.hullmods;

import java.awt.Color;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseCombatLayeredRenderingPlugin;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.FighterWingAPI;
import com.fs.starfarer.api.combat.ShipAIConfig;
import com.fs.starfarer.api.combat.ShipAIPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import data.scripts.kernel.shader.PC_FighterCatapultShaderCore;

/**
 * 黑曜石引擎模块专用弹射跑道。
 * 舰桥系统只切换起飞/整备模式，实际战机生成、弹射、集结和返航都在这里推进。
 */
public class PC_FighterCatapultDeck extends BaseHullMod {

    // 固定配置：本插件只服务左右引擎模块的 LB 1 后向跑道。
    public static final String HULLMOD_ID = "PC_FighterCatapultDeck";
    public static final String LUOXIA_WING_ID = "PC_Luoxia_wing";
    protected static final String LUOXIA_VARIANT_ID = "PC_Luoxia_Interceptor";
    protected static final String LAUNCH_SLOT_ID = "LB 1";

    // 主舰广播模式；战机阶段标记用于阻止其他控制器抢 AI。
    public static final String BRIDGE_MODE_KEY = "$PC_fighterCatapult_bridgeMode";
    public static final String FIGHTER_STATE_KEY = "$PC_fighterCatapult_state";

    protected static final String STATUS_KEY = "PC_FIGHTER_CATAPULT_DECK";
    protected static final String STATUS_ICON = "graphics/icons/hullsys/reserve_deployment.png";
    protected static final String CATAPULT_TAG = "pc_fighter_catapult";

    protected static final int MAX_GROUPS = 2;
    protected static final int FIGHTERS_PER_GROUP = 3;
    protected static final float DAMAGED_HULL_FRACTION = 0.25f;
    protected static final float GROUP_READY_DELAY = 3f;
    protected static final float REFIT_PER_FIGHTER_TIME = 5f;
    protected static final float LIFTING_DURATION = 1f;
    protected static final float CHARGING_DURATION = 0.5f;
    protected static final float ACCELERATING_DURATION = 3f;
    protected static final float TAKEOFF_BURN_DURATION = 1f;
    protected static final float RUNWAY_LENGTH = 180f;
    protected static final float STEAM_FADE_DURATION = 3f;
    protected static final float STEAM_WIDTH = 16f;
    protected static final float PREP_FOG_INTERVAL = 0.11f;
    protected static final float PREP_FOG_BLEND_DURATION = 0.4f;
    protected static final float RUNWAY_WAKE_INTERVAL = 0.045f;
    protected static final float TAKEOFF_TRAIL_INTERVAL = 0.035f;
    protected static final float LANDING_WAKE_INTERVAL = 0.055f;
    protected static final float LAUNCH_START_SCALE = 0.5f;
    protected static final float LAUNCH_READY_SCALE = 0.78f;
    protected static final float LANDING_END_SCALE = 0.12f;
    protected static final float ASSEMBLY_DELAY = 0.55f;
    protected static final float LANDING_START_DISTANCE = 160f;
    protected static final float LANDING_REMOVE_TIME = 1.8f;
    protected static final float RETURNING_FORCE_LAND_TIME = 24f;
    protected static final float RALLY_ORBIT_RATE = 18f;
    protected static final Color LAUNCH_PARTICLE_COLOR = new Color(160, 220, 255, 190);
    protected static final Color TAKEOFF_STEAM_COLOR = new Color(235, 240, 245, 175);

    // 任一引擎模块进场时确保全场只有一个推进插件。
    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ensureCombatPlugin();
    }

    @Override
    public void applyEffectsAfterShipAddedToCombatEngine(ShipAPI ship, String id) {
        ensureCombatPlugin();
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        ensureCombatPlugin();
    }

    @Override
    public boolean shouldAddDescriptionToTooltip(ShipAPI.HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
        return false;
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship,
                                          float width, boolean isForModSpec) {
        float opad = 10f;
        tooltip.addPara("接收弹射命令，弹射截击机。", opad);
    }

    @Override
    public boolean showInRefitScreenModPickerFor(ShipAPI ship) {
        return false;
    }

    public static void ensureCombatPlugin() {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine != null && !engine.hasPluginOfClass(FighterCatapultCombatPlugin.class)) {
            engine.addPlugin(new FighterCatapultCombatPlugin());
        }
    }

    public static boolean isCatapultManaged(ShipAPI fighter) {
        return fighter != null && fighter.getCustomData().containsKey(FIGHTER_STATE_KEY);
    }

    // 舰桥模块写模式，左右引擎模块读同一份主舰 custom data。
    public static void setBridgeLaunchMode(ShipAPI ship, boolean launchMode) {
        getCommandRoot(ship).setCustomData(BRIDGE_MODE_KEY, Boolean.valueOf(launchMode));
    }

    public static boolean isBridgeLaunchMode(ShipAPI ship) {
        Object mode = getCommandRoot(ship).getCustomData().get(BRIDGE_MODE_KEY);
        return mode instanceof Boolean && ((Boolean) mode).booleanValue();
    }

    public static ShipAPI getCommandRoot(ShipAPI ship) {
        ShipAPI current = ship;
        while (current != null && current.getParentStation() != null && current.getParentStation() != current) {
            current = current.getParentStation();
        }
        return current;
    }

    protected static boolean hasCatapultDeck(ShipAPI ship) {
        return ship.getHullSpec().isBuiltInMod(HULLMOD_ID);
    }

    protected static boolean isUsableShip(ShipAPI ship, CombatEngineAPI engine) {
        return ship != null
                && !ship.isFighter()
                && ship.isAlive()
                && !ship.isHulk()
                && !ship.wasRemoved()
                && (engine == null || engine.isInPlay(ship));
    }

    protected static float normalizeAngle(float angle) {
        angle %= 360f;
        if (angle < 0f) {
            angle += 360f;
        }
        return angle;
    }

    protected static float getAccelerationCurve(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t * t * t * t;
    }

    public static class FighterCatapultCombatPlugin extends BaseEveryFrameCombatPlugin {
        protected CombatEngineAPI engine;
        protected final Map<ShipAPI, DeckState> states = new LinkedHashMap<ShipAPI, DeckState>();

        @Override
        public void init(CombatEngineAPI engine) {
            this.engine = engine;
        }

        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            if (engine == null) {
                engine = Global.getCombatEngine();
            }
            if (engine == null) {
                states.clear();
                return;
            }

            // 每个安装了本 hullmod 的引擎模块各维护一套弹射队列。
            Set<ShipAPI> seen = new HashSet<ShipAPI>();
            for (ShipAPI ship : engine.getShips()) {
                if (!isManagedDeck(ship)) {
                    continue;
                }
                DeckState state = getState(ship);
                if (engine.isPaused()) {
                    state.maintainStatus(engine);
                } else {
                    state.advance(Math.max(0f, amount), engine);
                }
                seen.add(ship);
            }
            prune(seen, engine.isPaused() ? 0f : Math.max(0f, amount));
        }

        protected boolean isManagedDeck(ShipAPI ship) {
            return isUsableShip(ship, engine) && hasCatapultDeck(ship);
        }

        protected DeckState getState(ShipAPI ship) {
            DeckState state = states.get(ship);
            if (state == null) {
                state = new DeckState(ship);
                states.put(ship, state);
            }
            return state;
        }

        // 模块消失或被摧毁时，不再继续生成新机组。
        protected void prune(Set<ShipAPI> seen, float amount) {
            Iterator<Map.Entry<ShipAPI, DeckState>> iter = states.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<ShipAPI, DeckState> entry = iter.next();
                ShipAPI ship = entry.getKey();
                if (ship == null || !seen.contains(ship) || !isUsableShip(ship, engine)) {
                    DeckState state = entry.getValue();
                    state.retire(amount, engine);
                    if (state.isRetiredAndClear()) {
                        iter.remove();
                    }
                }
            }
        }
    }

    // 单个引擎模块的跑道状态：两个机组上限、整备计时、当前活动 wing。
    protected static class DeckState {
        protected final ShipAPI deck;
        protected final WeaponSlotAPI launchSlot;
        protected final List<GroupState> groups = new ArrayList<GroupState>();
        protected final List<RefitRequest> refitQueue = new ArrayList<RefitRequest>();
        protected int readyGroups = MAX_GROUPS;
        protected int readyFighterCredits = 0;
        protected float refitTimer = 0f;
        protected float groupReadyDelay = 0f;
        protected boolean retired = false;

        protected DeckState(ShipAPI deck) {
            this.deck = deck;
            this.launchSlot = deck.getHullSpec().getWeaponSlot(LAUNCH_SLOT_ID);
        }

        // 起飞模式自动补机；整备模式召回所有已生成机组。
        protected void advance(float amount, CombatEngineAPI engine) {
            if (retired) {
                retire(amount, engine);
                return;
            }

            advanceRefitQueue(amount, engine);
            advanceGroups(amount, engine);

            boolean launchMode = isBridgeLaunchMode(deck);
            if (!launchMode) {
                orderAllReturn();
            } else {
                maybeLaunchGroup(amount, engine);
            }

            maintainStatus(engine);
        }

        // 单模块整备队列：无论补机还是整组整备，都按 5 秒一架串行处理。
        protected void advanceRefitQueue(float amount, CombatEngineAPI engine) {
            if (refitQueue.isEmpty()) {
                refitTimer = 0f;
                return;
            }

            refitTimer += amount;
            if (refitTimer < REFIT_PER_FIGHTER_TIME) {
                return;
            }

            RefitRequest request = refitQueue.get(0);
            if (request.isReplacement() && hasGroupInLaunchSequence()) {
                return;
            }
            refitTimer -= REFIT_PER_FIGHTER_TIME;
            refitQueue.remove(0);

            if (request.isReplacement()) {
                request.group.replaceFighter(request.index, engine);
            } else {
                readyFighterCredits++;
                if (readyFighterCredits >= FIGHTERS_PER_GROUP && readyGroups < MAX_GROUPS) {
                    readyFighterCredits -= FIGHTERS_PER_GROUP;
                    readyGroups++;
                }
            }
        }

        // 完成返航/损失的机组进入逐架整备队列。
        protected void advanceGroups(float amount, CombatEngineAPI engine) {
            Iterator<GroupState> iter = groups.iterator();
            while (iter.hasNext()) {
                GroupState group = iter.next();
                group.advance(amount, engine);
                if (group.isComplete(engine) && !hasQueuedRefitFor(group)) {
                    group.cleanup(engine);
                    if (!retired) {
                        queueGroupRefit(group.getRefitFighterCount());
                    }
                    iter.remove();
                }
            }
        }

        protected void queueGroupRefit(int fighters) {
            for (int i = 0; i < fighters; i++) {
                refitQueue.add(RefitRequest.readyStock());
            }
        }

        protected void queueFighterReplacement(GroupState group, int index) {
            for (RefitRequest request : refitQueue) {
                if (request.group == group && request.index == index) {
                    return;
                }
            }
            refitQueue.add(RefitRequest.replacement(group, index));
        }

        protected boolean hasQueuedRefitFor(GroupState group) {
            for (RefitRequest request : refitQueue) {
                if (request.group == group) {
                    return true;
                }
            }
            return false;
        }

        // 同一模块一次只弹射一个机组，避免两组同时挤在跑道上。
        protected void maybeLaunchGroup(float amount, CombatEngineAPI engine) {
            if (groupReadyDelay > 0f) {
                groupReadyDelay -= amount;
                return;
            }
            if (readyGroups <= 0 || hasGroupInLaunchSequence()) {
                return;
            }
            if (getLiveGroupCount(engine) >= MAX_GROUPS) {
                return;
            }

            GroupState group = spawnGroup(engine);
            if (group != null) {
                readyGroups--;
                groups.add(group);
            }
        }

        protected boolean hasGroupInLaunchSequence() {
            for (GroupState group : groups) {
                if (group != null && group.isLaunchSequenceActive()) {
                    return true;
                }
            }
            return false;
        }

        protected int getLiveGroupCount(CombatEngineAPI engine) {
            int count = 0;
            for (GroupState group : groups) {
                if (group != null && !group.isComplete(engine)) {
                    count++;
                }
            }
            return count;
        }

        // 生成完整三机 wing，先全部隐藏，后续逐架从跑道释放。
        protected GroupState spawnGroup(CombatEngineAPI engine) {
            float launchHeading = getLaunchHeading();
            Vector2f location = getRunwayStartLocation();
            CombatFleetManagerAPI manager = engine.getFleetManager(deck.getOwner());

            boolean wasSuppressing = manager.isSuppressDeploymentMessages();
            ShipAPI leader = null;
            try {
                manager.setSuppressDeploymentMessages(true);
                leader = manager.spawnShipOrWing(LUOXIA_WING_ID, location, launchHeading, 0f, null);
            } finally {
                manager.setSuppressDeploymentMessages(wasSuppressing);
            }

            FighterWingAPI wing = leader.getWing();
            wing.setWingOwner(deck.getOwner());
            wing.setSourceShip(getCommandRoot(deck));
            List<ShipAPI> members = new ArrayList<ShipAPI>(wing.getWingMembers());
            if (!members.contains(leader)) {
                members.add(leader);
            }
            return new GroupState(this, wing, members, launchHeading, engine);
        }

        protected Vector2f getRunwayStartLocation() {
            return launchSlot.computePosition(deck);
        }

        protected Vector2f getRunwayEndLocation() {
            Vector2f end = getRunwayStartLocation();
            Vector2f dir = Misc.getUnitVectorAtDegreeAngle(getLaunchHeading());
            end.x += dir.x * RUNWAY_LENGTH;
            end.y += dir.y * RUNWAY_LENGTH;
            return end;
        }

        protected Vector2f getLandingLocation() {
            return getRunwayStartLocation();
        }

        protected Vector2f getLandingApproachLocation() {
            return getRunwayEndLocation();
        }

        // 后向跑道，方向与模块面向相反。
        protected float getLaunchHeading() {
            return normalizeAngle(deck.getFacing() + 180f);
        }

        protected float getLandingHeading() {
            return normalizeAngle(getLaunchHeading() + 180f);
        }

        protected void orderAllReturn() {
            for (GroupState group : groups) {
                if (group != null) {
                    group.startReturn();
                }
            }
        }

        protected void forceReturn(CombatEngineAPI engine) {
            orderAllReturn();
            for (GroupState group : groups) {
                if (group != null) {
                    group.advance(0f, engine);
                }
            }
        }

        protected void retire(float amount, CombatEngineAPI engine) {
            retired = true;
            readyGroups = 0;
            readyFighterCredits = 0;
            refitQueue.clear();
            refitTimer = 0f;
            groupReadyDelay = 0f;
            Iterator<GroupState> iter = groups.iterator();
            while (iter.hasNext()) {
                GroupState group = iter.next();
                if (group != null) {
                    group.forceRemove(engine);
                }
                iter.remove();
            }
        }

        protected boolean isRetiredAndClear() {
            return retired && groups.isEmpty();
        }

        protected void maintainStatus(CombatEngineAPI engine) {
            if (engine == null) {
                return;
            }
            ShipAPI root = getCommandRoot(deck);
            if (root == null || root != engine.getPlayerShip()) {
                return;
            }
            String mode = isBridgeLaunchMode(deck) ? "launch" : "refit";
            String detail = mode + ", groups " + getLiveGroupCount(engine) + "/" + MAX_GROUPS
                    + ", ready " + readyGroups + ", refit " + refitQueue.size();
            engine.maintainStatusForPlayerShip(STATUS_KEY + ":" + System.identityHashCode(deck),
                    STATUS_ICON, "Catapult deck", detail, !isBridgeLaunchMode(deck));
        }

        protected static class RefitRequest {
            protected final GroupState group;
            protected final int index;

            protected RefitRequest(GroupState group, int index) {
                this.group = group;
                this.index = index;
            }

            protected static RefitRequest readyStock() {
                return new RefitRequest(null, -1);
            }

            protected static RefitRequest replacement(GroupState group, int index) {
                return new RefitRequest(group, index);
            }

            protected boolean isReplacement() {
                return group != null;
            }
        }
    }

    // 一个三机落霞机组的生命周期。
    protected static class GroupState {
        protected final DeckState deckState;
        protected final FighterWingAPI wing;
        protected final List<FighterState> fighters = new ArrayList<FighterState>();
        protected final float launchHeading;
        protected final float orbitOffset;
        protected int nextLaunchIndex = 0;
        protected float assemblyTimer = 0f;
        protected boolean returning = false;
        protected boolean released = false;
        protected boolean cleaned = false;

        protected GroupState(DeckState deckState, FighterWingAPI wing, List<ShipAPI> members,
                             float launchHeading, CombatEngineAPI engine) {
            this.deckState = deckState;
            this.wing = wing;
            this.launchHeading = launchHeading;
            this.orbitOffset = (float) (Math.random() * 360f);

            int count = Math.max(FIGHTERS_PER_GROUP, members == null ? 0 : members.size());
            removeInitialSpawnedMembers(members, engine);
            for (int i = 0; i < count; i++) {
                FighterState state = new FighterState(this, null, i);
                fighters.add(state);
            }
        }

        protected void removeInitialSpawnedMembers(List<ShipAPI> members, CombatEngineAPI engine) {
            if (members == null) {
                return;
            }
            for (ShipAPI member : members) {
                if (member == null) {
                    continue;
                }
                clearRemovedMember(member, engine);
            }
        }

        // 三架都到集结点后，交还原生 fighter AI。
        protected void advance(float amount, CombatEngineAPI engine) {
            if (cleaned) {
                return;
            }

            if (!returning) {
                returnDamagedFighters(engine);
            }

            for (FighterState fighter : fighters) {
                fighter.advance(amount, engine);
            }

            if (!returning && !released) {
                advanceLaunchQueue();
            }

            if (!returning && !released && isAssembled(engine)) {
                assemblyTimer += amount;
                if (assemblyTimer >= ASSEMBLY_DELAY) {
                    releaseWing();
                }
            }
        }

        // 上一架完成起飞后，下一架立刻开始弹射。
        protected void advanceLaunchQueue() {
            if (nextLaunchIndex >= fighters.size() || hasFighterInTakeoffSequence()) {
                return;
            }

            FighterState fighter = fighters.get(nextLaunchIndex);
            fighter.launch();
            nextLaunchIndex++;
        }

        protected boolean hasFighterInTakeoffSequence() {
            for (FighterState fighter : fighters) {
                if (fighter.phase == FighterPhase.LAUNCHING || fighter.phase == FighterPhase.TAKEOFF_BURN) {
                    return true;
                }
            }
            return false;
        }

        protected void returnDamagedFighters(CombatEngineAPI engine) {
            for (FighterState fighter : fighters) {
                if (fighter.shouldReturnForDamage(engine)) {
                    fighter.startReturnForReplacement();
                }
            }
        }

        // 弹射完成但尚未出击时，战机会绕主舰等待集结。
        protected boolean isAssembled(CombatEngineAPI engine) {
            if (nextLaunchIndex < fighters.size()) {
                return false;
            }
            if (deckState.hasQueuedRefitFor(this)) {
                return false;
            }
            boolean hasLive = false;
            for (FighterState fighter : fighters) {
                if (!fighter.isLive(engine)) {
                    continue;
                }
                hasLive = true;
                if (fighter.phase != FighterPhase.RALLY) {
                    return false;
                }
            }
            return hasLive;
        }

        protected boolean isLaunchSequenceActive() {
            return !returning && (!released || hasFighterInTakeoffSequence());
        }

        protected void releaseWing() {
            released = true;
            deckState.groupReadyDelay = GROUP_READY_DELAY;
            for (FighterState fighter : fighters) {
                fighter.release();
            }
        }

        protected void startReturn() {
            if (returning) {
                return;
            }
            returning = true;
            released = false;
            for (FighterState fighter : fighters) {
                fighter.startReturn();
            }
        }

        protected boolean isComplete(CombatEngineAPI engine) {
            if (deckState.hasQueuedRefitFor(this)) {
                return false;
            }
            for (FighterState fighter : fighters) {
                if (fighter.isLive(engine) || (!fighter.gone && fighter.phase == FighterPhase.LANDING)) {
                    return false;
                }
            }
            return true;
        }

        // 清理临时部署的 wing，避免已落舰机组继续占用部署对象。
        protected void cleanup(CombatEngineAPI engine) {
            if (cleaned) {
                return;
            }
            cleaned = true;
            if (engine != null) {
                engine.getFleetManager(deckState.deck.getOwner()).removeDeployed(wing, false);
            }
        }

        protected void clearRemovedMember(ShipAPI member, CombatEngineAPI engine) {
            if (member == null) {
                return;
            }
            member.removeCustomData(FIGHTER_STATE_KEY);
            FighterWingAPI memberWing = member.getWing();
            if (memberWing != null && memberWing != wing) {
                memberWing.removeMember(member);
            }
            if (wing != null) {
                wing.removeMember(member);
            }
            member.setDoNotRender(false);
            member.setCollisionClass(CollisionClass.NONE);
            member.setHoldFire(false);
            member.setHoldFireOneFrame(false);
            if (engine != null && engine.isInPlay(member)) {
                engine.removeEntity(member);
            }
        }

        protected void forceRemove(CombatEngineAPI engine) {
            if (cleaned) {
                return;
            }
            for (FighterState fighter : fighters) {
                if (fighter != null) {
                    fighter.remove(engine);
                }
            }
            cleanup(engine);
        }

        protected int getRefitFighterCount() {
            return Math.max(FIGHTERS_PER_GROUP, fighters.size());
        }

        protected void queueReplacement(FighterState state) {
            if (returning || cleaned) {
                return;
            }
            deckState.queueFighterReplacement(this, state.index);
        }

        // 单架补机：整备完成后生成 variant，加入原 wing，并走同一条弹射流程。
        protected void replaceFighter(int index, CombatEngineAPI engine) {
            if (engine == null || returning || cleaned) {
                return;
            }

            FighterState old = index < fighters.size() ? fighters.get(index) : null;
            if (old != null && old.fighter != null) {
                clearRemovedMember(old.fighter, engine);
            }
            ShipAPI replacement = spawnFighterEntity(engine);
            if (replacement == null) {
                return;
            }

            FighterState state = new FighterState(this, replacement, index);
            if (index < fighters.size()) {
                fighters.set(index, state);
            } else {
                fighters.add(state);
            }
            state.configureWaiting();
            state.launch();
            assemblyTimer = 0f;
        }

        protected ShipAPI spawnFighterEntity(CombatEngineAPI engine) {
            if (engine == null || cleaned) {
                return null;
            }

            Vector2f location = deckState.getRunwayStartLocation();
            float heading = deckState.getLaunchHeading();
            CombatFleetManagerAPI manager = engine.getFleetManager(deckState.deck.getOwner());

            boolean wasSuppressing = manager.isSuppressDeploymentMessages();
            ShipAPI fighter = null;
            try {
                manager.setSuppressDeploymentMessages(true);
                fighter = manager.spawnShipOrWing(LUOXIA_VARIANT_ID, location, heading, 0f, null);
            } finally {
                manager.setSuppressDeploymentMessages(wasSuppressing);
            }

            if (fighter == null) {
                return null;
            }
            if (wing != null) {
                wing.addMember(fighter);
                if (wing.getLeader() == null
                        || !wing.getLeader().isAlive()
                        || wing.getLeader().isHulk()
                        || wing.getLeader().wasRemoved()) {
                    wing.setLeader(fighter);
                }
            }
            return fighter;
        }

        // 集结点绕主舰后方环绕，避免刚弹射就进入原生 AI。
        protected Vector2f getRallyPoint(int index) {
            ShipAPI anchor = getCommandRoot(deckState.deck);
            CombatEngineAPI engine = Global.getCombatEngine();
            float time = engine.getTotalElapsedTime(false);
            float radius = Math.max(520f, anchor.getCollisionRadius() + 330f);
            float angle = deckState.deck.getFacing() + 180f + orbitOffset + time * RALLY_ORBIT_RATE + index * 120f;
            Vector2f dir = Misc.getUnitVectorAtDegreeAngle(angle);
            Vector2f point = new Vector2f(anchor.getLocation());
            point.x += dir.x * radius;
            point.y += dir.y * radius;
            return point;
        }
    }

    // WAITING 到 ACTIVE 之间的阶段会打标，手动战机控制器会跳过。
    protected enum FighterPhase {
        WAITING,
        LAUNCHING,
        TAKEOFF_BURN,
        RALLY,
        ACTIVE,
        RETURNING,
        LANDING
    }

    // 单架战机的弹射/集结/返航状态。
    protected static class FighterState {
        protected final GroupState group;
        protected ShipAPI fighter;
        protected final int index;
        protected FighterPhase phase = FighterPhase.WAITING;
        protected float phaseElapsed = 0f;
        protected boolean gone = false;
        protected boolean landingStarted = false;
        protected boolean queueReplacementAfterLanding = false;
        protected boolean catapultTrackCompleted = false;
        protected float takeoffSpeed = 0f;
        protected float takeoffHeading = 0f;
        protected float lastCatapultProgress = 0f;
        protected Vector2f takeoffStartLocation = new Vector2f();
        protected Vector2f takeoffVelocity = new Vector2f();
        protected Vector2f landingStartLocation = new Vector2f();
        protected float trailElapsed = 0f;
        protected float prepFogElapsed = 0f;

        protected FighterState(GroupState group, ShipAPI fighter, int index) {
            this.group = group;
            this.fighter = fighter;
            this.index = index;
        }

        // 新生成的 wing 成员先藏在跑道点，禁碰撞、禁火、禁 AI 出击。
        protected void configureWaiting() {
            if (fighter == null) {
                return;
            }
            fighter.setOwner(group.deckState.deck.getOwner());
            fighter.setOriginalOwner(group.deckState.deck.getOriginalOwner());
            fighter.addTag(CATAPULT_TAG);
            fighter.setDoNotRender(true);
            fighter.setCollisionClass(CollisionClass.NONE);
            fighter.setHoldFire(true);
            fighter.setHoldFireOneFrame(true);
            fighter.setPhased(false);
            moveToDeck();
            setPhase(FighterPhase.WAITING);
            fighter.setShipAI(new CatapultFighterAI(this));
        }

        // 弹射开始：起飞点立刻出现半尺寸假机，同时启动蒸汽渲染。
        protected void launch() {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (fighter == null || fighter.wasRemoved() || (engine != null && !engine.isInPlay(fighter))) {
                fighter = group.spawnFighterEntity(engine);
                if (fighter != null) {
                    gone = false;
                    landingStarted = false;
                    queueReplacementAfterLanding = false;
                    configureWaiting();
                }
            }
            if (!isLive(engine)) {
                gone = true;
                return;
            }

            moveToDeck();
            takeoffSpeed = computeTakeoffSpeed();
            takeoffHeading = group.deckState.getLaunchHeading();
            lastCatapultProgress = 0f;
            catapultTrackCompleted = false;
            trailElapsed = 0f;
            prepFogElapsed = 0f;
            fighter.setDoNotRender(true);
            fighter.setCollisionClass(CollisionClass.NONE);
            fighter.setFacing(takeoffHeading);
            fighter.setAngularVelocity(0f);
            fighter.setHoldFire(true);
            fighter.setHoldFireOneFrame(true);
            setPhase(FighterPhase.LAUNCHING);
            moveAlongCatapultTrack();
            fighter.setShipAI(new CatapultFighterAI(this));

            if (engine != null) {
                engine.addLayeredRenderingPlugin(new CatapultFighterRenderer(this));
                engine.addLayeredRenderingPlugin(new CatapultSteamRenderer(this));
            }
        }

        // 阶段转换只在这里做，AI 只负责移动控制。
        protected void advance(float amount, CombatEngineAPI engine) {
            if (gone) {
                return;
            }
            if (!isLive(engine) && phase != FighterPhase.LANDING) {
                if (shouldQueueReplacement()) {
                    group.queueReplacement(this);
                }
                gone = true;
                clearCatapultState();
                return;
            }

            phaseElapsed += amount;
            if (phase == FighterPhase.WAITING) {
                moveToDeck();
            } else if (phase == FighterPhase.LAUNCHING) {
                moveAlongCatapultTrack();
                lastCatapultProgress = Math.max(lastCatapultProgress, getAccelerationProgress());
                if (isPreparationFogActive()) {
                    emitPreparationFog(engine, amount);
                }
                if (!isPreparingLaunch()) {
                    emitRunwayWake(engine, amount);
                }
                if (phaseElapsed >= getCatapultDuration()) {
                    finishCatapult(phaseElapsed - getCatapultDuration());
                }
            } else if (phase == FighterPhase.TAKEOFF_BURN) {
                updateTakeoffBurnKinematics();
                emitTakeoffTrail(engine, amount);
                if (phaseElapsed >= TAKEOFF_BURN_DURATION) {
                    if (group.released) {
                        release();
                    } else {
                        enterRally();
                    }
                }
            } else if (phase == FighterPhase.RETURNING) {
                if (Misc.getDistance(fighter.getLocation(), group.deckState.getLandingApproachLocation()) <= LANDING_START_DISTANCE
                        || phaseElapsed >= RETURNING_FORCE_LAND_TIME) {
                    beginLanding();
                }
            } else if (phase == FighterPhase.LANDING) {
                moveAlongLandingTrack();
                emitLandingWake(engine, amount);
                if (phaseElapsed >= LANDING_REMOVE_TIME) {
                    if (queueReplacementAfterLanding && !group.returning && !group.cleaned) {
                        group.queueReplacement(this);
                        queueReplacementAfterLanding = false;
                    }
                    remove(engine);
                }
            }
        }

        protected void finishCatapult(float overshoot) {
            takeoffStartLocation.set(group.deckState.getRunwayEndLocation());
            fighter.setDoNotRender(true);
            fighter.setCollisionClass(CollisionClass.NONE);
            takeoffHeading = group.deckState.getLaunchHeading();
            fighter.setFacing(takeoffHeading);
            lastCatapultProgress = 1f;
            catapultTrackCompleted = true;

            Vector2f velocity = Misc.getUnitVectorAtDegreeAngle(takeoffHeading);
            velocity.scale(takeoffSpeed);
            Vector2f.add(velocity, group.deckState.deck.getVelocity(), takeoffVelocity);

            setPhase(FighterPhase.TAKEOFF_BURN);
            phaseElapsed = Math.max(0f, Math.min(0.08f, overshoot));
            updateTakeoffBurnKinematics();
            trailElapsed = TAKEOFF_TRAIL_INTERVAL;

            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine != null) {
                engine.addSmoothParticle(new Vector2f(fighter.getLocation()), new Vector2f(takeoffVelocity),
                        55f, 0.75f, 0.35f, LAUNCH_PARTICLE_COLOR);
                emitTakeoffTrail(engine, 0f);
            }
        }

        protected void enterRally() {
            fighter.setDoNotRender(false);
            fighter.setCollisionClass(CollisionClass.FIGHTER);
            fighter.setFacing(takeoffHeading);
            setPhase(FighterPhase.RALLY);
        }

        // 未弹出的隐藏机收到整备命令时直接回库；已弹出的才飞回跑道。
        protected void startReturn() {
            startReturn(false);
        }

        protected void startReturnForReplacement() {
            startReturn(true);
        }

        protected void startReturn(boolean replaceAfterLanding) {
            if (gone || phase == FighterPhase.LANDING) {
                return;
            }
            if (!isLive(Global.getCombatEngine())) {
                gone = true;
                clearCatapultState();
                return;
            }
            if (phase == FighterPhase.WAITING) {
                remove(Global.getCombatEngine());
                return;
            }
            queueReplacementAfterLanding |= replaceAfterLanding && !group.returning && !group.cleaned;
            fighter.setDoNotRender(false);
            fighter.setCollisionClass(CollisionClass.FIGHTER);
            fighter.setHoldFire(true);
            fighter.setHoldFireOneFrame(true);
            setPhase(FighterPhase.RETURNING);
            fighter.setShipAI(new CatapultFighterAI(this));
        }

        // 集结完成后清掉标记，恢复原版舰载机行为。
        protected void release() {
            if (!isLive(Global.getCombatEngine())) {
                gone = true;
                clearCatapultState();
                return;
            }

            clearCatapultState();
            phase = FighterPhase.ACTIVE;
            fighter.setDoNotRender(false);
            fighter.setCollisionClass(CollisionClass.FIGHTER);
            fighter.setHoldFire(false);
            fighter.setHoldFireOneFrame(false);
            fighter.setPhased(false);
            if (fighter.getWing() != null && fighter.getWing().isReturning(fighter)) {
                fighter.getWing().stopReturning(fighter);
            }
            fighter.resetDefaultAI();
        }

        // 到 LB 1 港口点后自绘缩小，避免原生落舰动画把位置拉到母舰后方。
        protected void beginLanding() {
            if (landingStarted) {
                return;
            }
            landingStarted = true;
            landingStartLocation.set(fighter.getLocation());
            fighter.setCollisionClass(CollisionClass.NONE);
            fighter.setDoNotRender(true);
            fighter.setHoldFire(true);
            fighter.setHoldFireOneFrame(true);
            setPhase(FighterPhase.LANDING);
            trailElapsed = 0f;
            prepFogElapsed = 0f;
            moveAlongLandingTrack();

            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine != null) {
                engine.addLayeredRenderingPlugin(new CatapultLandingRenderer(this));
            }
        }

        protected void remove(CombatEngineAPI engine) {
            clearCatapultState();
            gone = true;
            if (fighter != null) {
                group.clearRemovedMember(fighter, engine);
                fighter = null;
            }
        }

        protected void moveToDeck() {
            Vector2f location = group.deckState.getRunwayStartLocation();
            fighter.getLocation().set(location);
            fighter.getVelocity().set(group.deckState.deck.getVelocity());
            fighter.setFacing(group.deckState.getLaunchHeading());
        }

        protected void moveAlongCatapultTrack() {
            fighter.getLocation().set(getRenderedCatapultLocation());
            fighter.getVelocity().set(group.deckState.deck.getVelocity());
            fighter.setFacing(group.deckState.getLaunchHeading());
        }

        protected void updateTakeoffBurnKinematics() {
            Vector2f location = new Vector2f(takeoffStartLocation);
            location.x += takeoffVelocity.x * phaseElapsed;
            location.y += takeoffVelocity.y * phaseElapsed;
            fighter.getLocation().set(location);
            fighter.getVelocity().set(takeoffVelocity);
            fighter.setFacing(takeoffHeading);
        }

        protected void moveAlongLandingTrack() {
            fighter.getLocation().set(getRenderedLandingLocation());
            fighter.getVelocity().set(group.deckState.deck.getVelocity());
            fighter.setFacing(group.deckState.getLandingHeading());
        }

        protected Vector2f getMoveTarget() {
            if (phase == FighterPhase.RALLY) {
                return group.getRallyPoint(index);
            }
            if (phase == FighterPhase.RETURNING) {
                return group.deckState.getLandingApproachLocation();
            }
            if (phase == FighterPhase.LANDING) {
                return group.deckState.getLandingLocation();
            }
            return group.deckState.getRunwayStartLocation();
        }

        // 准备弹射时只在起飞点堆团状雾，避免还没滑跑就拉出线状尾迹。
        protected void emitPreparationFog(CombatEngineAPI engine, float amount) {
            if (engine == null) {
                return;
            }
            prepFogElapsed += amount;
            if (prepFogElapsed < PREP_FOG_INTERVAL) {
                return;
            }
            prepFogElapsed = 0f;

            float duration = LIFTING_DURATION + CHARGING_DURATION;
            float progress = Math.max(0f, Math.min(1f, phaseElapsed / duration));
            float fadeOut = 1f;
            if (phaseElapsed > duration) {
                fadeOut = 1f - Math.max(0f, Math.min(1f, (phaseElapsed - duration) / PREP_FOG_BLEND_DURATION));
            }
            float strength = (0.45f + smoothStep(progress) * 0.35f) * fadeOut;
            float radius = Math.max(8f, fighter.getCollisionRadius());
            Vector2f dir = Misc.getUnitVectorAtDegreeAngle(group.deckState.getLaunchHeading());
            Vector2f side = new Vector2f(-dir.y, dir.x);
            Vector2f base = group.deckState.deck.getVelocity();
            Vector2f origin = group.deckState.getRunwayStartLocation();

            for (int i = 0; i < 2; i++) {
                float along = ((float) Math.random() - 0.65f) * radius * (1.45f + progress * 0.55f);
                float lateral = ((float) Math.random() - 0.5f) * radius * 2.05f;
                Vector2f loc = new Vector2f(origin);
                loc.x += dir.x * along + side.x * lateral;
                loc.y += dir.y * along + side.y * lateral;

                Vector2f velocity = new Vector2f(base);
                velocity.x += dir.x * (-14f - (float) Math.random() * 20f);
                velocity.y += dir.y * (-14f - (float) Math.random() * 20f);
                velocity.x += side.x * (((float) Math.random() - 0.5f) * 46f);
                velocity.y += side.y * (((float) Math.random() - 0.5f) * 46f);

                float size = Math.max(18f, radius * (1.25f + strength * 0.65f + (float) Math.random() * 0.35f));
                engine.addNebulaSmokeParticle(loc, velocity, size, 1.85f, 0.03f, 0.24f,
                        0.62f + strength * 0.22f,
                        new Color(248, 250, 252, Math.round((85f + strength * 95f) * fadeOut)));
            }

            engine.addSmoothParticle(new Vector2f(origin), new Vector2f(base),
                    Math.max(12f, radius * (0.72f + strength * 0.28f)),
                    0.18f + strength * 0.1f, 0.12f,
                    new Color(250, 252, 255, Math.round((55f + strength * 60f) * fadeOut)));
        }

        // 跑道段的白烟跟随假机位置，补上 shader 蒸汽和出膛尾迹之间的过渡。
        protected void emitRunwayWake(CombatEngineAPI engine, float amount) {
            if (engine == null || isPreparingLaunch()) {
                return;
            }
            trailElapsed += amount;
            if (trailElapsed < RUNWAY_WAKE_INTERVAL) {
                return;
            }
            trailElapsed = 0f;

            float progress = Math.max(0f, Math.min(1f,
                    (phaseElapsed - LIFTING_DURATION - CHARGING_DURATION) / ACCELERATING_DURATION));
            float strength = 0.5f + smoothStep(progress) * 0.65f;
            float radius = Math.max(6f, fighter.getCollisionRadius());
            Vector2f dir = Misc.getUnitVectorAtDegreeAngle(group.deckState.getLaunchHeading());
            Vector2f side = new Vector2f(-dir.y, dir.x);
            Vector2f base = group.deckState.deck.getVelocity();
            Vector2f origin = getRenderedCatapultLocation();
            float laneOffset = Math.max(5f, radius * 0.38f);
            float backLength = radius * (0.7f + getAccelerationProgress() * 1.1f);

            for (int sideSign = -1; sideSign <= 1; sideSign += 2) {
                Vector2f loc = new Vector2f(origin);
                loc.x -= dir.x * (backLength + (float) Math.random() * radius * 0.75f);
                loc.y -= dir.y * (backLength + (float) Math.random() * radius * 0.75f);
                loc.x += side.x * sideSign * (laneOffset + (float) Math.random() * 5f);
                loc.y += side.y * sideSign * (laneOffset + (float) Math.random() * 5f);

                Vector2f velocity = new Vector2f(base);
                velocity.x -= dir.x * (55f + strength * 70f + (float) Math.random() * 25f);
                velocity.y -= dir.y * (55f + strength * 70f + (float) Math.random() * 25f);
                velocity.x += side.x * sideSign * (10f + (float) Math.random() * 22f);
                velocity.y += side.y * sideSign * (10f + (float) Math.random() * 22f);

                float size = Math.max(11f, radius * (0.85f + strength * 0.55f + (float) Math.random() * 0.25f));
                engine.addNebulaSmokeParticle(loc, velocity, size, 2f, 0.02f, 0.2f,
                        0.55f + strength * 0.2f,
                        new Color(248, 250, 252, Math.round(115f + strength * 105f)));
            }

            Vector2f coreLoc = new Vector2f(origin);
            coreLoc.x -= dir.x * radius * 0.45f;
            coreLoc.y -= dir.y * radius * 0.45f;
            engine.addSmoothParticle(coreLoc, new Vector2f(base), Math.max(7f, radius * 0.45f),
                    0.2f + strength * 0.12f, 0.1f, TAKEOFF_STEAM_COLOR);
        }

        protected void emitTakeoffTrail(CombatEngineAPI engine, float amount) {
            if (engine == null) {
                return;
            }
            trailElapsed += amount;
            if (trailElapsed < TAKEOFF_TRAIL_INTERVAL) {
                return;
            }
            trailElapsed = 0f;

            float radius = Math.max(6f, fighter.getCollisionRadius());
            float progress = Math.max(0f, Math.min(1f, phaseElapsed / TAKEOFF_BURN_DURATION));
            float strength = Math.max(0.35f, 1f - progress * 0.45f);
            Vector2f dir = Misc.getUnitVectorAtDegreeAngle(takeoffHeading);
            Vector2f side = new Vector2f(-dir.y, dir.x);
            Vector2f baseVel = fighter.getVelocity() == null ? group.deckState.deck.getVelocity() : fighter.getVelocity();

            for (int sideSign = -1; sideSign <= 1; sideSign += 2) {
                Vector2f loc = new Vector2f(fighter.getLocation());
                loc.x -= dir.x * (radius * (0.75f + (float) Math.random() * 0.5f));
                loc.y -= dir.y * (radius * (0.75f + (float) Math.random() * 0.5f));
                loc.x += side.x * sideSign * (radius * (0.35f + (float) Math.random() * 0.25f));
                loc.y += side.y * sideSign * (radius * (0.35f + (float) Math.random() * 0.25f));

                Vector2f vel = new Vector2f(baseVel);
                vel.x -= dir.x * (80f + 65f * strength + (float) Math.random() * 35f);
                vel.y -= dir.y * (80f + 65f * strength + (float) Math.random() * 35f);
                vel.x += side.x * sideSign * (16f + (float) Math.random() * 22f);
                vel.y += side.y * sideSign * (16f + (float) Math.random() * 22f);

                float size = Math.max(12f, radius * (0.95f + strength * 0.55f + (float) Math.random() * 0.35f));
                engine.addNebulaSmokeParticle(loc, vel, size, 2.05f, 0.03f, 0.2f,
                        0.62f + strength * 0.22f,
                        new Color(248, 250, 252, Math.round(105f + strength * 95f)));
            }

            Vector2f coreLoc = new Vector2f(fighter.getLocation());
            coreLoc.x -= dir.x * radius * 0.55f;
            coreLoc.y -= dir.y * radius * 0.55f;
            Vector2f coreVel = new Vector2f(baseVel);
            coreVel.x -= dir.x * 55f;
            coreVel.y -= dir.y * 55f;
            engine.addSmoothParticle(coreLoc, coreVel, Math.max(7f, radius * 0.45f),
                    0.24f + strength * 0.18f, 0.12f, TAKEOFF_STEAM_COLOR);
        }

        // 落舰段沿起飞路线反向收回，弱尾流让缩小过程不显得突然。
        protected void emitLandingWake(CombatEngineAPI engine, float amount) {
            if (engine == null || phaseElapsed < 0.08f) {
                return;
            }
            trailElapsed += amount;
            if (trailElapsed < LANDING_WAKE_INTERVAL) {
                return;
            }
            trailElapsed = 0f;

            float t = Math.max(0f, Math.min(1f, phaseElapsed / LANDING_REMOVE_TIME));
            float strength = Math.max(0.2f, 1f - t * 0.65f);
            float radius = Math.max(6f, fighter.getCollisionRadius()) * Math.max(0.45f, getLandingScale());
            Vector2f dir = Misc.getUnitVectorAtDegreeAngle(group.deckState.getLandingHeading());
            Vector2f side = new Vector2f(-dir.y, dir.x);
            Vector2f base = group.deckState.deck.getVelocity();

            Vector2f loc = getRenderedLandingLocation();
            loc.x -= dir.x * (radius * (0.7f + (float) Math.random() * 0.45f));
            loc.y -= dir.y * (radius * (0.7f + (float) Math.random() * 0.45f));
            loc.x += side.x * (((float) Math.random() - 0.5f) * radius * 0.55f);
            loc.y += side.y * (((float) Math.random() - 0.5f) * radius * 0.55f);

            Vector2f velocity = new Vector2f(base);
            velocity.x -= dir.x * (35f + strength * 45f);
            velocity.y -= dir.y * (35f + strength * 45f);
            velocity.x += side.x * (((float) Math.random() - 0.5f) * 22f);
            velocity.y += side.y * (((float) Math.random() - 0.5f) * 22f);

            engine.addNebulaSmokeParticle(loc, velocity, Math.max(8f, radius * (0.8f + strength * 0.35f)),
                    1.35f, 0.02f, 0.18f, 0.35f + strength * 0.2f,
                    new Color(245, 248, 252, Math.round(55f + strength * 80f)));
        }

        protected float getCatapultDuration() {
            return LIFTING_DURATION + CHARGING_DURATION + ACCELERATING_DURATION;
        }

        protected boolean isPreparingLaunch() {
            return phase == FighterPhase.LAUNCHING && phaseElapsed <= LIFTING_DURATION + CHARGING_DURATION;
        }

        protected boolean isPreparationFogActive() {
            return phase == FighterPhase.LAUNCHING
                    && phaseElapsed <= LIFTING_DURATION + CHARGING_DURATION + PREP_FOG_BLEND_DURATION;
        }

        protected float computeTakeoffSpeed() {
            float x1 = getAccelerationCurve(0.99f);
            float x2 = getAccelerationCurve(1f);
            return RUNWAY_LENGTH * (x2 - x1) / (0.01f * ACCELERATING_DURATION);
        }

        protected Vector2f getRenderedCatapultLocation() {
            if (phase == FighterPhase.TAKEOFF_BURN) {
                return new Vector2f(fighter.getLocation());
            }
            if (phaseElapsed <= LIFTING_DURATION + CHARGING_DURATION) {
                return group.deckState.getRunwayStartLocation();
            }
            float accelElapsed = phaseElapsed - LIFTING_DURATION - CHARGING_DURATION;
            float t = Math.max(0f, Math.min(1f, accelElapsed / ACCELERATING_DURATION));
            return Misc.interpolateVector(group.deckState.getRunwayStartLocation(),
                    group.deckState.getRunwayEndLocation(), getAccelerationCurve(t));
        }

        protected float getAccelerationProgress() {
            float accelElapsed = phaseElapsed - LIFTING_DURATION - CHARGING_DURATION;
            float t = Math.max(0f, Math.min(1f, accelElapsed / ACCELERATING_DURATION));
            return getAccelerationCurve(t);
        }

        protected float getRenderedCatapultScale() {
            if (phase == FighterPhase.TAKEOFF_BURN) {
                return 1f;
            }
            if (phaseElapsed <= LIFTING_DURATION) {
                return LAUNCH_START_SCALE + (LAUNCH_READY_SCALE - LAUNCH_START_SCALE)
                        * smoothStep(phaseElapsed / LIFTING_DURATION);
            }
            if (phaseElapsed <= LIFTING_DURATION + CHARGING_DURATION) {
                return LAUNCH_READY_SCALE;
            }
            return LAUNCH_READY_SCALE + (1f - LAUNCH_READY_SCALE) * getAccelerationProgress();
        }

        protected float getRenderedCatapultAlpha() {
            if (phase == FighterPhase.TAKEOFF_BURN) {
                return 1f;
            }
            if (phase == FighterPhase.LAUNCHING && phaseElapsed <= LIFTING_DURATION) {
                return smoothStep(phaseElapsed / LIFTING_DURATION);
            }
            return 1f;
        }

        protected float getRenderedCatapultFacing() {
            return phase == FighterPhase.TAKEOFF_BURN ? takeoffHeading : group.deckState.getLaunchHeading();
        }

        protected boolean isCatapultVisualPhase() {
            return phase == FighterPhase.LAUNCHING || phase == FighterPhase.TAKEOFF_BURN;
        }

        protected float getLandingScale() {
            float t = Math.max(0f, Math.min(1f, phaseElapsed / LANDING_REMOVE_TIME));
            float shrink = smoothStep(Math.max(0f, (t - 0.2f) / 0.8f));
            return 1f + (LANDING_END_SCALE - 1f) * shrink;
        }

        protected Vector2f getRenderedLandingLocation() {
            float t = Math.max(0f, Math.min(1f, phaseElapsed / LANDING_REMOVE_TIME));
            float slide = smoothStep(t);
            return Misc.interpolateVector(landingStartLocation,
                    group.deckState.getLandingLocation(), slide);
        }

        protected float smoothStep(float t) {
            t = Math.max(0f, Math.min(1f, t));
            return t * t * (3f - 2f * t);
        }

        protected boolean isLive(CombatEngineAPI engine) {
            return fighter != null
                    && !gone
                    && fighter.isAlive()
                    && !fighter.isHulk()
                    && !fighter.wasRemoved()
                    && (engine == null || engine.isInPlay(fighter));
        }

        protected boolean shouldQueueReplacement() {
            return !group.returning
                    && !group.cleaned
                    && phase != FighterPhase.WAITING
                    && phase != FighterPhase.LANDING;
        }

        protected boolean shouldReturnForDamage(CombatEngineAPI engine) {
            return !group.returning
                    && !group.cleaned
                    && isLive(engine)
                    && fighter.getHullLevel() <= DAMAGED_HULL_FRACTION
                    && (phase == FighterPhase.RALLY || phase == FighterPhase.ACTIVE);
        }

        protected void setPhase(FighterPhase phase) {
            this.phase = phase;
            this.phaseElapsed = 0f;
            if (fighter != null && phase != FighterPhase.ACTIVE) {
                fighter.setCustomData(FIGHTER_STATE_KEY, phase.name());
            }
        }

        protected void clearCatapultState() {
            if (fighter != null) {
                fighter.removeCustomData(FIGHTER_STATE_KEY);
            }
        }
    }

    // 起飞全程只画假机；真实 fighter 到环绕/出击前才显示，避免两段视觉硬切。
    protected static class CatapultFighterRenderer extends BaseCombatLayeredRenderingPlugin {
        protected final FighterState state;
        protected final SpriteAPI sprite;
        protected final float width;
        protected final float height;

        protected CatapultFighterRenderer(FighterState state) {
            this.state = state;
            this.sprite = Global.getSettings().getSprite(state.fighter.getHullSpec().getSpriteName());
            this.width = sprite.getWidth();
            this.height = sprite.getHeight();
        }

        @Override
        public void render(CombatEngineLayers layer, ViewportAPI viewport) {
            if (state.gone || !state.isCatapultVisualPhase()) {
                return;
            }

            Vector2f location = state.getRenderedCatapultLocation();
            float scale = state.getRenderedCatapultScale();
            sprite.setSize(width * scale, height * scale);
            sprite.setAlphaMult(state.getRenderedCatapultAlpha());
            sprite.setAngle(state.getRenderedCatapultFacing() - 90f);
            sprite.renderAtCenter(location.x, location.y);
            sprite.setSize(width, height);
            sprite.setAlphaMult(1f);
            sprite.setAngle(0f);
        }

        @Override
        public EnumSet<CombatEngineLayers> getActiveLayers() {
            return EnumSet.of(CombatEngineLayers.FIGHTERS_LAYER);
        }

        @Override
        public float getRenderRadius() {
            return Float.MAX_VALUE;
        }

        @Override
        public boolean isExpired() {
            CombatEngineAPI engine = Global.getCombatEngine();
            return state.gone
                    || !state.isCatapultVisualPhase()
                    || state.fighter.wasRemoved()
                    || engine == null
                    || !engine.isInPlay(state.fighter);
        }
    }

    // 自定义落舰缩小：锁在 LB 1 港口点，不交给原生落舰动画拉位置。
    protected static class CatapultLandingRenderer extends BaseCombatLayeredRenderingPlugin {
        protected final FighterState state;
        protected final SpriteAPI sprite;
        protected final float width;
        protected final float height;

        protected CatapultLandingRenderer(FighterState state) {
            this.state = state;
            this.sprite = Global.getSettings().getSprite(state.fighter.getHullSpec().getSpriteName());
            this.width = sprite.getWidth();
            this.height = sprite.getHeight();
        }

        @Override
        public void render(CombatEngineLayers layer, ViewportAPI viewport) {
            if (state.gone || state.phase != FighterPhase.LANDING) {
                return;
            }

            Vector2f location = state.getRenderedLandingLocation();
            float scale = state.getLandingScale();
            sprite.setSize(width * scale, height * scale);
            sprite.setAlphaMult(Math.max(0f, Math.min(1f, scale)));
            sprite.setAngle(state.group.deckState.getLandingHeading() - 90f);
            sprite.renderAtCenter(location.x, location.y);
            sprite.setSize(width, height);
            sprite.setAlphaMult(1f);
            sprite.setAngle(0f);
        }

        @Override
        public EnumSet<CombatEngineLayers> getActiveLayers() {
            return EnumSet.of(CombatEngineLayers.FIGHTERS_LAYER);
        }

        @Override
        public float getRenderRadius() {
            return Float.MAX_VALUE;
        }

        @Override
        public boolean isExpired() {
            return state.gone || state.phase != FighterPhase.LANDING;
        }
    }

    // 照 revenge 参考：蒸汽在 CONTRAILS_LAYER 沿起飞点到终点展开，结束后淡出。
    protected static class CatapultSteamRenderer extends BaseCombatLayeredRenderingPlugin {
        protected final FighterState state;
        protected float fadeElapsed = 0f;
        protected float maxProgress = 0f;
        protected int timeLoc = -1;
        protected int progressLoc = -1;
        protected int dissipationLoc = -1;

        protected CatapultSteamRenderer(FighterState state) {
            this.state = state;
        }

        @Override
        public void advance(float amount) {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine != null && engine.isPaused()) {
                return;
            }
            if (state.phase != FighterPhase.LAUNCHING) {
                fadeElapsed += amount;
            }
        }

        @Override
        public void render(CombatEngineLayers layer, ViewportAPI viewport) {
            if (layer != CombatEngineLayers.CONTRAILS_LAYER || state.gone) {
                return;
            }
            if (state.isPreparingLaunch()) {
                return;
            }

            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine == null) {
                return;
            }

            int shader = PC_FighterCatapultShaderCore.getSteamProgram();
            if (shader == 0) {
                return;
            }

            Vector2f start = state.group.deckState.getRunwayStartLocation();
            Vector2f end = state.group.deckState.getRunwayEndLocation();
            Vector2f dir = Vector2f.sub(end, start, new Vector2f());
            float length = dir.length();
            if (length < 1f) {
                return;
            }
            dir.scale(1f / length);
            Vector2f perp = new Vector2f(-dir.y, dir.x);

            float progress = getRenderProgress();
            if (progress <= 0f) {
                return;
            }
            float dissipation = Math.max(0f, Math.min(1f, fadeElapsed / STEAM_FADE_DURATION));

            GL11.glPushAttrib(GL11.GL_CURRENT_BIT | GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT);
            GL11.glPushMatrix();
            try {
                GL20.glUseProgram(shader);

                if (timeLoc < 0) {
                    timeLoc = GL20.glGetUniformLocation(shader, "u_time");
                    progressLoc = GL20.glGetUniformLocation(shader, "u_progress");
                    dissipationLoc = GL20.glGetUniformLocation(shader, "u_dissipation");
                }

                GL20.glUniform1f(progressLoc, progress);
                GL20.glUniform1f(dissipationLoc, dissipation);
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

                float time = engine.getTotalElapsedTime(false);
                GL20.glUniform1f(timeLoc, time);
                renderSteamQuad(start, end, perp, 0f, STEAM_WIDTH);
                GL20.glUniform1f(timeLoc, time + 10f);
                renderSteamQuad(start, end, perp, 0f, -STEAM_WIDTH);
            } finally {
                GL20.glUseProgram(0);
                GL11.glPopMatrix();
                GL11.glPopAttrib();
            }
        }

        protected float getRenderProgress() {
            if (state.catapultTrackCompleted) {
                maxProgress = 1f;
                return 1f;
            }
            if (state.phase == FighterPhase.LAUNCHING) {
                maxProgress = Math.max(maxProgress, state.lastCatapultProgress);
                maxProgress = Math.max(maxProgress, state.getAccelerationProgress());
                return Math.max(0.025f, maxProgress);
            }
            maxProgress = Math.max(maxProgress, state.lastCatapultProgress);
            return maxProgress <= 0f ? 0f : Math.max(0.025f, maxProgress);
        }

        protected void renderSteamQuad(Vector2f start, Vector2f end, Vector2f perp, float offset, float width) {
            Vector2f p1 = new Vector2f(start.x + perp.x * offset, start.y + perp.y * offset);
            Vector2f p2 = new Vector2f(end.x + perp.x * offset, end.y + perp.y * offset);
            Vector2f p3 = new Vector2f(end.x + perp.x * (offset + width), end.y + perp.y * (offset + width));
            Vector2f p4 = new Vector2f(start.x + perp.x * (offset + width), start.y + perp.y * (offset + width));

            GL11.glBegin(GL11.GL_QUADS);
            GL11.glTexCoord2f(0f, 0f);
            GL11.glVertex2f(p1.x, p1.y);
            GL11.glTexCoord2f(1f, 0f);
            GL11.glVertex2f(p2.x, p2.y);
            GL11.glTexCoord2f(1f, 1f);
            GL11.glVertex2f(p3.x, p3.y);
            GL11.glTexCoord2f(0f, 1f);
            GL11.glVertex2f(p4.x, p4.y);
            GL11.glEnd();
        }

        @Override
        public EnumSet<CombatEngineLayers> getActiveLayers() {
            return EnumSet.of(CombatEngineLayers.CONTRAILS_LAYER);
        }

        @Override
        public float getRenderRadius() {
            return Float.MAX_VALUE;
        }

        @Override
        public boolean isExpired() {
            return state.gone || fadeElapsed >= STEAM_FADE_DURATION;
        }
    }

    // 临时 AI：只管弹射、集结、返航移动，并全程禁火。
    protected static class CatapultFighterAI implements ShipAIPlugin {
        protected final FighterState state;
        protected final ShipwideAIFlags flags = new ShipwideAIFlags();

        protected CatapultFighterAI(FighterState state) {
            this.state = state;
        }

        @Override
        public void advance(float amount) {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine == null || engine.isPaused() || !state.isLive(engine)) {
                return;
            }

            holdFire();
            if (state.phase == FighterPhase.WAITING) {
                state.moveToDeck();
            } else if (state.phase == FighterPhase.LAUNCHING) {
                state.moveAlongCatapultTrack();
                turnToward(state.group.deckState.getLaunchHeading());
            } else if (state.phase == FighterPhase.TAKEOFF_BURN) {
                state.updateTakeoffBurnKinematics();
                turnToward(state.takeoffHeading);
            } else if (state.phase == FighterPhase.RALLY) {
                moveTo(engine, state.getMoveTarget(), state.fighter.getMaxSpeed() * 0.78f);
                faceMovementTarget(state.getMoveTarget());
            } else if (state.phase == FighterPhase.RETURNING) {
                moveTo(engine, state.getMoveTarget(), state.fighter.getMaxSpeed());
                if (Misc.getDistance(state.fighter.getLocation(), state.group.deckState.getLandingApproachLocation()) <= 160f) {
                    turnToward(state.group.deckState.getLandingHeading());
                } else {
                    faceMovementTarget(state.getMoveTarget());
                }
            } else if (state.phase == FighterPhase.LANDING) {
                dampVelocity(engine);
                turnToward(state.group.deckState.getLandingHeading());
            }
        }

        protected void moveTo(CombatEngineAPI engine, Vector2f target, float speed) {
            float distance = Misc.getDistance(state.fighter.getLocation(), target);
            if (distance <= 18f) {
                dampVelocity(engine);
                return;
            }
            float heading = Misc.getAngleInDegrees(state.fighter.getLocation(), target);
            float desiredSpeed = Math.min(speed, Math.max(state.fighter.getMaxSpeed() * 0.35f, distance * 1.35f));
            engine.headInDirectionWithoutTurning(state.fighter, heading, desiredSpeed);
        }

        protected void moveInHeading(CombatEngineAPI engine, float heading, float speed) {
            engine.headInDirectionWithoutTurning(state.fighter, heading, speed);
        }

        protected void faceMovementTarget(Vector2f target) {
            if (Misc.getDistance(state.fighter.getLocation(), target) <= 4f) {
                return;
            }
            turnToward(Misc.getAngleInDegrees(state.fighter.getLocation(), target));
        }

        protected void turnToward(float facing) {
            Misc.turnTowardsFacingV2(state.fighter, facing, 0f);
        }

        protected void dampVelocity(CombatEngineAPI engine) {
            Vector2f velocity = state.fighter.getVelocity();
            if (velocity != null && velocity.lengthSquared() > 64f) {
                float brake = (float) Math.toDegrees(Math.atan2(velocity.y, velocity.x)) + 180f;
                engine.headInDirectionWithoutTurning(state.fighter, brake,
                        Math.min(state.fighter.getMaxSpeed(), velocity.length()));
            }
            state.fighter.giveCommand(ShipCommand.DECELERATE, null, 0);
        }

        protected void holdFire() {
            state.fighter.setHoldFire(true);
            state.fighter.setHoldFireOneFrame(true);
            for (WeaponAPI weapon : state.fighter.getAllWeapons()) {
                weapon.setForceNoFireOneFrame(true);
            }
        }

        @Override
        public void setDoNotFireDelay(float amount) {
        }

        @Override
        public void forceCircumstanceEvaluation() {
        }

        @Override
        public boolean needsRefit() {
            return false;
        }

        @Override
        public ShipwideAIFlags getAIFlags() {
            return flags;
        }

        @Override
        public void cancelCurrentManeuver() {
        }

        @Override
        public ShipAIConfig getConfig() {
            return null;
        }

        @Override
        public void setTargetOverride(ShipAPI target) {
        }
    }
}
