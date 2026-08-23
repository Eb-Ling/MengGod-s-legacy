package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import org.lazywizard.lazylib.MathUtils;
import data.methods.shaders.ShaderUtil;
import data.methods.MengPerformanceSettings;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class Meng_fire_Phasecore extends BaseHullMod {
    public static final String KEY = "Meng_fire_phasecore_listener";
    public static final String RANGE_KEY = "Meng_fire_phasecore_range_effect";
    public static final String RENDER_KEY = "Meng_fire_phasecore_render_plugin";
    
    private static final org.apache.log4j.Logger log = Global.getLogger(Meng_fire_Phasecore.class);
    
    private final float FLUX_DISSIPATION_REDUCTION = 50f;
    private final float FLUX_CAPACITY_INCREASE = 15f;
    private final float WEAPON_FLUX_INCREASE = 20f;
    private static final float EFFECT_RANGE = 2000f;
    private final float HULL_DAMAGE_TO_SOFT_FLUX_RATIO = 400f;
    private static final String IDS = "Meng_fire_phasecore_id";
    private final float FIRE_SHIP_BOUNS = 0.3f;
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float opad = 12.0F;
        Color highlight = new Color(255, 55, 40, 255);

        LabelAPI label1 = tooltip.addPara(
                "该舰船可以潜入浅层相位空间，从而实现火羽计划特有的相位战斗学说。",
                opad, highlight
        );
        label1.setHighlight();
        label1.setHighlightColors(highlight, highlight, highlight, highlight, highlight, highlight, highlight);

        tooltip.addSectionHeading("数据分析", Alignment.MID, opad);

        LabelAPI label = tooltip.addPara(
                "#舰船辐能耗散降低 %s%% 。\n#舰船辐能容量增加 %s%% 。\n#舰船武器产生的辐能增加 %s%% 。\n#周围 %s su 范围内（包括自身）所有舰船产生该舰船结构降低值的 %s%% 的硬辐能，该效果仅对虚数相位舰船生效 %s%% 。\n#舰船可以在相位空间开火并正常耗散辐能。\n#潜入相位空间将直接清空当前辐能并根据当前辐能水平增加潜行时间。",
                opad, highlight,
                String.valueOf(Math.round(-FLUX_DISSIPATION_REDUCTION)),
                String.valueOf(Math.round(FLUX_CAPACITY_INCREASE)),
                String.valueOf(Math.round(WEAPON_FLUX_INCREASE)),
                String.valueOf(Math.round(EFFECT_RANGE)),
                String.valueOf(Math.round(HULL_DAMAGE_TO_SOFT_FLUX_RATIO)),
                String.valueOf(Math.round(FIRE_SHIP_BOUNS*100f))
        );

        label.setHighlight(
                String.valueOf(Math.round(-FLUX_DISSIPATION_REDUCTION)) + "%",
                String.valueOf(Math.round(FLUX_CAPACITY_INCREASE)) + "%",
                String.valueOf(Math.round(WEAPON_FLUX_INCREASE)) + "%",
                String.valueOf(Math.round(EFFECT_RANGE)) + " su",
                String.valueOf(Math.round(HULL_DAMAGE_TO_SOFT_FLUX_RATIO)) + "%",
                String.valueOf(Math.round(FIRE_SHIP_BOUNS*100f)) + "%",
                "相位空间",
                "清空当前辐能"
        );
        label.setHighlightColors(highlight, highlight, highlight, highlight, highlight, highlight, highlight);
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getFluxDissipation().modifyMult(IDS,  (100f-FLUX_DISSIPATION_REDUCTION)/100F);
        stats.getFluxCapacity().modifyPercent(IDS, FLUX_CAPACITY_INCREASE);
        stats.getEnergyWeaponFluxCostMod().modifyPercent(IDS, WEAPON_FLUX_INCREASE);
        stats.getBallisticWeaponFluxCostMod().modifyPercent(IDS, WEAPON_FLUX_INCREASE);
        stats.getMissileWeaponFluxCostMod().modifyPercent(IDS, WEAPON_FLUX_INCREASE);
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (!ship.isAlive()) return;
        
        if (!ship.getCustomData().containsKey(RANGE_KEY)) {
            RangeEffectData data = new RangeEffectData();
            data.lastHit = ship.getHitpoints();
            ship.setCustomData(RANGE_KEY, data);
        }
        
        if (MengPerformanceSettings.useLowPerformanceEffects()
                && !ship.getCustomData().containsKey(RENDER_KEY)) {
            PhaseCoreRangePlugin plugin = new PhaseCoreRangePlugin(ship, EFFECT_RANGE, false);
            Global.getCombatEngine().addLayeredRenderingPlugin(plugin);
            ship.setCustomData(RENDER_KEY, plugin);
        } else if (!ship.getCustomData().containsKey(RENDER_KEY)) {
            PhaseCoreRangePlugin plugin = new PhaseCoreRangePlugin(ship, EFFECT_RANGE);
            Global.getCombatEngine().addLayeredRenderingPlugin(plugin);
            ship.setCustomData(RENDER_KEY, plugin);
        }
        
        RangeEffectData rangeData = (RangeEffectData) ship.getCustomData().get(RANGE_KEY);
        
        float currentHit = ship.getHitpoints();
        float damageTaken = rangeData.lastHit - currentHit;
        
        if (damageTaken > 0f) {
            float fluxToAdd = damageTaken * HULL_DAMAGE_TO_SOFT_FLUX_RATIO / 100f;
            
            for (ShipAPI target : Global.getCombatEngine().getShips()) {
                if (target == null || !target.isAlive()) continue;
                
                float distance = MathUtils.getDistance(ship.getLocation(), target.getLocation());
                if (distance <= EFFECT_RANGE) {
                    FluxTrackerAPI tracker = target.getFluxTracker();
                    float currentHardFlux = tracker.getHardFlux();
                    float currentFlux = tracker.getCurrFlux();
                    float maxFlux = target.getMaxFlux();

                    if (!target.getVariant().getHullMods().contains("Meng_fire_Phasecore")) {
                        float potentialHardFlux = currentHardFlux + fluxToAdd;

                        if (potentialHardFlux <= maxFlux) {
                            tracker.setCurrFlux(currentFlux + fluxToAdd);
                            tracker.setHardFlux(currentHardFlux + fluxToAdd);
                        } else {
                            float overflow = potentialHardFlux - maxFlux;
                            float safeFluxIncrease = fluxToAdd - overflow - 10f;
                            tracker.setCurrFlux(currentFlux + safeFluxIncrease);
                        }
                    }
                    else {
                        float potentialHardFlux = currentHardFlux + fluxToAdd * FIRE_SHIP_BOUNS;

                        if (potentialHardFlux <= maxFlux) {
                            tracker.setCurrFlux(currentFlux + fluxToAdd * FIRE_SHIP_BOUNS);
                            tracker.setHardFlux(currentHardFlux + fluxToAdd * FIRE_SHIP_BOUNS);
                        } else {
                            float overflow = potentialHardFlux - maxFlux;
                            float safeFluxIncrease = fluxToAdd * FIRE_SHIP_BOUNS - overflow - 10f;
                            tracker.setCurrFlux(currentFlux + safeFluxIncrease);
                        }
                    }
                }

            }
        }
        
        rangeData.lastHit = currentHit;
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return true;
    }
    
    private static class RangeEffectData {
        public float lastHit = 0f;
    }

    public static class PhaseCoreRangePlugin implements CombatLayeredRenderingPlugin {
        private ShipAPI ship;
        private float radius;
        private float timer;
        private float mergeRange = 4000f;
        private List<IntersectionPoint> intersections;
        
        private int shaderProgram;
        private ShaderUtil.VAOData vao;
        private int uModelMatrixLoc;
        private int uSizeLoc;
        private int uTimeLoc;
        
        private float canvasSize = 5000f;
        private float rotationAngle = 0f;
        private final boolean renderEnabled;

        public PhaseCoreRangePlugin(ShipAPI s, float r) {
            this(s, r, true);
        }

        /** Creates a logic-only instance when renderEnabled is false. */
        public PhaseCoreRangePlugin(ShipAPI s, float r, boolean renderEnabled) {
            ship = s;
            radius = r;
            this.renderEnabled = renderEnabled;
            timer = 0f;
            intersections = new ArrayList<>();

            if (renderEnabled) {
                createShaderProgram();
                createBuffers();
                cacheUniformLocations();
            }
        }
        
        private void createShaderProgram() {
            shaderProgram = ShaderUtil.createShaderProgramFromFiles(
                    "data/shaders/meng/common.vert",
                    "data/shaders/meng/phase_core.frag",
                    "PhaseCore");
        }
        private void createBuffers() {
            vao = ShaderUtil.createUniversalRectVAO();
        }
        
        private void cacheUniformLocations() {
            if (shaderProgram <= 0) return;
            int[] locs = ShaderUtil.getUniformLocations(shaderProgram, "modelMatrix", "size", "u_time");
            uModelMatrixLoc = locs[0];
            uSizeLoc = locs[1];
            uTimeLoc = locs[2];
        }

        @Override
        public void init(CombatEntityAPI entity) {
        }

        @Override
        public void cleanup() {
            ShaderUtil.cleanupAll(shaderProgram, vao, 0);
            shaderProgram = 0;
            vao = null;
        }

        @Override
        public boolean isExpired() {
            return !ship.isAlive();
        }

        @Override
        public void advance(float amount) {
            timer += amount;
            
            rotationAngle += amount * 5f;
            if (rotationAngle >= 360f) {
                rotationAngle -= 360f;
            }
            
            Vector2f center = ship.getLocation();
            if (center != null) {
                intersections = calculateIntersectionPoints(center);
            }
        }

        @Override
        public EnumSet<CombatEngineLayers> getActiveLayers() {
            return EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_LAYER);
        }

        @Override
        public float getRenderRadius() {
            return 100000f;
        }

        @Override
        public void render(CombatEngineLayers layer, ViewportAPI viewport) {
            if (!renderEnabled) return;
            if (layer == CombatEngineLayers.ABOVE_SHIPS_LAYER && ship != null && ship.isAlive() && shaderProgram > 0 && vao != null) {
                Vector2f center = ship.getLocation();
                if (center == null) return;
                
                GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
                
                GL20.glUseProgram(shaderProgram);
                FloatBuffer modelMat = ShaderUtil.buildModelMatrix(center.x, center.y, rotationAngle);
                GL20.glUniformMatrix4(uModelMatrixLoc, false, modelMat);
                GL20.glUniform2f(uSizeLoc, canvasSize, canvasSize);
                GL20.glUniform1f(uTimeLoc, timer);
                
                ShaderUtil.drawVAOQuad(vao.vaoId);
                
                GL20.glUseProgram(0);
                GL11.glPopAttrib();
            }
        }


        private List<IntersectionPoint> calculateIntersectionPoints(Vector2f center) {
            List<IntersectionPoint> result = new ArrayList<>();

            List<ShipAPI> allShips = Global.getCombatEngine().getShips();
            if (allShips == null || allShips.isEmpty()) {
                return result;
            }

            for (ShipAPI otherShip : allShips) {
                if (otherShip == null || !otherShip.isAlive() || otherShip == ship) {
                    continue;
                }

                if (!otherShip.getVariant().hasHullMod("Meng_fire_Phasecore")) {
                    continue;
                }

                Vector2f otherCenter = otherShip.getLocation();
                float distance = MathUtils.getDistance(center, otherCenter);
                
                if (distance >= mergeRange) {
                    continue;
                }

                float otherRadius = EFFECT_RANGE;
                
                if (distance > radius + otherRadius || distance < Math.abs(radius - otherRadius)) {
                    continue;
                }

                float angleToOther = (float) Math.atan2(otherCenter.y - center.y, otherCenter.x - center.x);
                float angleToOtherDeg = (float) Math.toDegrees(angleToOther);
                while (angleToOtherDeg < 0) angleToOtherDeg += 360f;
                while (angleToOtherDeg >= 360f) angleToOtherDeg -= 360f;

                float cosAngle = (radius * radius + distance * distance - otherRadius * otherRadius) / (2f * radius * distance);
                cosAngle = Math.max(-1f, Math.min(1f, cosAngle));
                float angleSpan = (float) Math.toDegrees(Math.acos(cosAngle));

                float startAngle = angleToOtherDeg - angleSpan;
                float endAngle = angleToOtherDeg + angleSpan;

                result.add(new IntersectionPoint(startAngle, endAngle));
            }

            return result;
        }

        private static class IntersectionPoint {
            float startAngle;
            float endAngle;

            IntersectionPoint(float start, float end) {
                this.startAngle = start;
                this.endAngle = end;
            }
        }
    }
}
