package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Noise;
import data.methods.Meng_V2arcfind;
import data.methods.Meng_arcfind;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lazywizard.lazylib.JSONUtils;
import org.lazywizard.lazylib.MathUtils;
import data.methods.shaders.ShaderUtil;
import data.methods.MengPerformanceSettings;
import org.lwjgl.input.Controller;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Objects;

public class Meng_MingGodCenter extends BaseHullMod {
    public static final String KEY = "Meng_MingGodheartlistener";
    public static final String id = "Meng_MingGodheartsign";
    public final float range = 80f;

    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float opad = 12.0F;
        Color highlight = new Color(255, 225, 171, 255);
        tooltip.addSectionHeading("数据分析", Alignment.MID, opad);
        LabelAPI label2 = tooltip.addPara(
                "#舰船每隔 %s 秒将会重新装填所有导弹武器的弹药。\n#舰船投射的弹体将经由差分空间潮汐加速 %s%% 。\n#所有非导弹武器射程增加 %s%% 。",
                opad, highlight, "10", "100", "80"
        );
        label2.setHighlight("10", "100%", "80%");
        label2.setHighlightColors(highlight, highlight, highlight);

        tooltip.addSectionHeading("冥", Alignment.MID, opad);


        LabelAPI label = tooltip.addPara(
                "这艘舰船的能量来源于名为 \"冥\" 的差分空间锁，舰船朝向范围内60°的且距离在3000su以内的舰船将会被 \"冥\"注视。\n\n?当注视完全成型，舰船可通过战术系统对所有被注视的目标投射 空间裂缝。\n?冥河摧毁的目标将会被\"冥\"注视，在原地张开 虚无空间，形成标记，虚无空间将随时间逐渐活化，并会持续吸收周围弹体加速活化进度，当活化达到一定程度将会发生爆炸，造成至多 5000点能量伤害。",
                opad, highlight, "20"

        );
        label.setHighlight("\"冥\"", "空间裂缝", "虚无空间", "次元裂隙", "5000");
        label.setHighlightColors(highlight, highlight, highlight, highlight, highlight, highlight, highlight);

        tooltip.addSectionHeading("圣殿史录", Alignment.MID, opad);


        LabelAPI label1 = tooltip.addPara(
                "“冥域注视之人，亦无容身之地。”",
                opad, new Color(106, 255, 255, 218), "“冥域注视之人，亦无容身之地。”"

        );
        label1.setHighlight(0, 100);
        LabelAPI para = tooltip.addSectionHeading("阶梯计划", Alignment.MID, opad);
        para.setHighlightColors(new Color(0, 255, 221, 218));
        para.setHighlight(0, 100);

        label = tooltip.addPara(
                "阶梯计划在创立之初便做好了失控的应对，当舰队中同时出现一艘以上的阶梯计划舰船时，所有的阶梯计划舰船的系统都会全部下线。",
                opad, highlight);
        label.italicize();
        label.setHighlightColors(new Color(250, 0, 0, 255));
        label.setHighlight(0, 100);
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getEnergyWeaponRangeBonus().modifyPercent(id, range);
        stats.getBallisticWeaponRangeBonus().modifyPercent(id, range);
        stats.getProjectileSpeedMult().modifyPercent(id, 100f);
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (!ship.isAlive()) return;

        if (isInPlayerFleet(ship)) {
            PersonAPI fc = null;
            FleetMemberAPI member = ship.getFleetMember();
            if (member != null) {
                for (FleetMemberAPI mem : ship.getFleetMember().getFleetData().getMembersListCopy()) {
                    if (mem.getHullSpec() != member.getHullSpec() && mem.getHullSpec().getTags().contains("Meng_embers") && !mem.isMothballed()) {
                        ship.setCurrentCR(0f);
                    }
                }
            }
        }

        for (ShipAPI target : Global.getCombatEngine().getShips()) {
            if (target != null) {
                if (target.getOwner() != ship.getOwner()) {
                    if (ship.isAlive()) {
                        //筛选全场符合要求的舰船。
                        if (!target.hasListenerOfClass(MyDamageListener1.class)) {
                            target.addListener(new MyDamageListener1(ship, target));
                        }
                    }
                }
            }
        }
        if (!ship.getCustomData().containsKey(KEY)) {
            DataContainer data = new DataContainer();
            ship.setCustomData(KEY, data);
        }
        DataContainer data = (DataContainer) ship.getCustomData().get(KEY);
        data.interval.advance(amount);
        if (data.interval.intervalElapsed()) {
            for (WeaponAPI w : ship.getAllWeapons()) {
                if (w.getType() != WeaponAPI.WeaponType.MISSILE) continue;

                if (w.usesAmmo() && w.getAmmo() < w.getMaxAmmo()) {
                    w.setAmmo(w.getMaxAmmo());
                }
            }
        }
        //为每一艘目标舰船添加监听。

        if (Objects.equals(ship.getHullSpec().getHullId(), "Meng_MingGod") && ship.getVariant().getHullSpec().getShieldType() != ShieldAPI.ShieldType.NONE && ship.getShield() != null && ship.getVariant().getHullSpec().getShieldType() != ShieldAPI.ShieldType.PHASE) {
            ship.getShield().setRingColor(new Color(250, 27, 27, 255));
        }
    }

    public static class DataContainer {
        IntervalUtil interval = new IntervalUtil(10f, 10f);
    }

    private static class MyDamageListener1 implements DamageListener {
        public ShipAPI target;
        public ShipAPI ships;
        private boolean init = false;

        public MyDamageListener1(ShipAPI ship, ShipAPI target) {
            this.target = target;
            ships = ship;
        }

        @Override
        public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
            if (source instanceof ShipAPI && !this.target.isAlive() && ships != null) {
                ShipAPI targets = (ShipAPI) source;
                if (targets == ships && result.getDamageToHull() > this.target.getHitpoints()) {
                    if (!init) {
                        init = true;
                        if (MengPerformanceSettings.useLowPerformanceEffects()) {
                            Global.getCombatEngine().addLayeredRenderingPlugin(new Meng_MingGodSignPlugin(this.target, ships, false));
                        } else {
                            Global.getCombatEngine().addLayeredRenderingPlugin(new Meng_MingGodSignPlugin(this.target, ships));
                        }
                    }
                }
            }
        }
    }

    public static class Meng_MingGodSignPlugin implements CombatLayeredRenderingPlugin {
        private final ShipAPI target;
        private final ShipAPI ship;
        private float chargelevel = 0f;
        private float energy = 0f;
        private ArrayList<DamagingProjectileAPI> targets;
        private float timer = 0f;
        private float f = 0f;
        private float spritecharger = 0f;
        private boolean end = false;
        private float size;
        private float[] lastnoise;
        private float[] ideanoise;
        private float[] noise;
        private Vector2f loc;
        private float arg;
        
        private int shaderProgram;
        private ShaderUtil.VAOData vao;
        private int uModelMatrixLoc;
        private int uSizeLoc;
        private int uTimeLoc;
        private int uDurationLoc;
        private int uProgressLoc;
        private final boolean renderEnabled;

        public Meng_MingGodSignPlugin(ShipAPI targets, ShipAPI source) {
            this(targets, source, true);
        }

        /** Creates a logic-only instance when renderEnabled is false. */
        public Meng_MingGodSignPlugin(ShipAPI targets, ShipAPI source, boolean renderEnabled) {
            target = targets;
            ship = source;
            this.renderEnabled = renderEnabled;
        }

        public void init(CombatEntityAPI entity) {
            loc = new Vector2f(target.getLocation().x, target.getLocation().y);
            size = 0f;
            lastnoise = Noise.genNoise(360, 1f);
            ideanoise = Noise.genNoise(360, 1f);
            noise = Arrays.copyOf(lastnoise, 360);
            arg = (float) Math.random() * 360f;
            
            if (renderEnabled) {
                createShaderProgram();
            }
        }
        
        private void createShaderProgram() {
            shaderProgram = ShaderUtil.createShaderProgramFromFiles(
                    "data/shaders/meng/common.vert",
                    "data/shaders/meng/ming_god_sign.frag",
                    "MingGodSign");
            if (shaderProgram > 0) {
                int[] locs = ShaderUtil.getUniformLocations(shaderProgram,
                        "modelMatrix", "size", "u_time", "u_duration", "u_progress");
                uModelMatrixLoc = locs[0];
                uSizeLoc = locs[1];
                uTimeLoc = locs[2];
                uDurationLoc = locs[3];
                uProgressLoc = locs[4];
                createBuffers();
            }
        }
        private void createBuffers() {
            vao = ShaderUtil.createUniversalRectVAO();
        }

        @Override
        public void cleanup() {
            ShaderUtil.cleanupAll(shaderProgram, vao, 0);
            shaderProgram = 0;
            vao = null;
        }

        @Override
        public boolean isExpired() {
            return end;
        }

        @Override
        public void advance(float amount) {
            CombatEngineAPI engine = Global.getCombatEngine();
            timer += amount * (1 + chargelevel);
            f += amount;
            if (f > 1f) {
                f = 0f;
                lastnoise = Arrays.copyOf(ideanoise, 360);
                ideanoise = Noise.genNoise(360, 1f);
            }
            for (int i = 0; i < 360; i++) {
                noise[i] = lastnoise[i] + (ideanoise[i] - lastnoise[i]) * f;
            }
            if (targets == null) {
                targets = new ArrayList<>();
            }
            if (chargelevel <= 0f) {
                spritecharger += amount;
                size = target.getShieldRadiusEvenIfNoShield() * 2f * spritecharger;
            }
            if (spritecharger >= 1f) {
                float range = size*1.5f;
                for (ShipAPI target : engine.getShips()) {
                    float nowrange = MathUtils.getDistance(loc, target.getLocation());
                    if (nowrange <= range && target.isHulk() && target.getOwner() != ship.getOwner()) {
                        Vector2f vel = new Vector2f(nowrange * (loc.getX() - target.getLocation().getX()), nowrange * (loc.getY() - target.getLocation().getY()));
                        target.getVelocity().set(vel);
                    }
                }
                for (DamagingProjectileAPI target : engine.getProjectiles()) {
                    float nowrange = MathUtils.getDistance(loc, target.getLocation());
                    if (nowrange <= range) {
                        if (!targets.contains(target)) {
                            targets.add(target);
                            energy += target.getDamageAmount() / size / 10f;
                        }

                        Vector2f force = new Vector2f(500f * (float) Math.cos(Math.toRadians(Meng_arcfind.Findarc(target.getLocation(), loc))), 500f * (float) Math.sin(Math.toRadians(Meng_arcfind.Findarc(target.getLocation(), loc))));
                        Vector2f vel = new Vector2f(target.getVelocity().x, target.getVelocity().y);
                        Vector2f.add(vel, force, vel);

                        target.getVelocity().set(vel);
                        target.setFacing(Meng_V2arcfind.Findarc(vel));
                        if (nowrange < range * 0.3f) {
                            engine.removeEntity(target);
                        }

                    }
                }
                chargelevel += 300f / size * amount * (0.05f + energy * 0.05f);
            }
            Color color1 = new Color(135, 15, 200, 120);
            Color color2 = new Color(180, 60, 250, 150);
            if (chargelevel >= 1f) {
                DamagingExplosionSpec spc = new DamagingExplosionSpec(
                        size * 0.25f,
                        size * 6f,
                        size * 3f,
                        size * 40f,
                        size * 4f,
                        CollisionClass.PROJECTILE_NO_FF,
                        CollisionClass.PROJECTILE_NO_FF,
                        size * 0.06f,
                        size * 0.01f,
                        size * 0.015f,
                        Math.round(size * 0.015f),
                        color1, color2);
                spc.setDamageType(DamageType.ENERGY);
                spc.setShowGraphic(true);
                engine.spawnDamagingExplosion(spc, ship, loc);
                end = true;
            }
        }

        @Override
        public EnumSet<CombatEngineLayers> getActiveLayers() {
            return EnumSet.of(CombatEngineLayers.ABOVE_PARTICLES);
        }

        @Override
        public float getRenderRadius() {
            return 10000000f;
        }

        @Override
        public void render(CombatEngineLayers layer, ViewportAPI viewport) {
            if (!renderEnabled) return;
            if (layer == CombatEngineLayers.ABOVE_PARTICLES && shaderProgram > 0 && size > 0f && vao != null) {
                float progress = Math.min(chargelevel, 1.0f);
                float duration = 100.0f;
                float elapsed = timer;
                
                float currentRadius = size*1.8f * Math.min(spritecharger, 1.0f);

                GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

                GL20.glUseProgram(shaderProgram);
                FloatBuffer modelMat = ShaderUtil.buildModelMatrix(loc.x, loc.y, arg);
                GL20.glUniformMatrix4(uModelMatrixLoc, false, modelMat);
                float fullSize = currentRadius * 2f;
                GL20.glUniform2f(uSizeLoc, fullSize, fullSize);
                GL20.glUniform1f(uTimeLoc, elapsed);
                GL20.glUniform1f(uDurationLoc, duration);
                GL20.glUniform1f(uProgressLoc, progress);

                ShaderUtil.drawVAOQuad(vao.vaoId);

                GL20.glUseProgram(0);
                GL11.glPopAttrib();
            }
        }

    }
}
