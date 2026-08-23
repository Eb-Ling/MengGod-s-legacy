package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import org.lazywizard.lazylib.MathUtils;
import data.methods.shaders.ShaderUtil;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
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
        
        if (!ship.getCustomData().containsKey(RENDER_KEY)) {
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
        private int vbo;
        private int ibo;
        private int uTimeLoc;
        
        private float canvasSize = 5000f;
        private float rotationAngle = 0f;

        public PhaseCoreRangePlugin(ShipAPI s, float r) {
            ship = s;
            radius = r;
            timer = 0f;
            intersections = new ArrayList<>();
            
            createShaderProgram();
            createBuffers();
            cacheUniformLocations();
        }
        
        private void createShaderProgram() {
            try {
                String vertexSource = 
                    "#version 110\n" +
                    "attribute vec2 a_position;\n" +
                    "attribute vec2 a_texCoord;\n" +
                    "varying vec2 v_uv;\n" +
                    "void main() {\n" +
                    "    v_uv = a_texCoord;\n" +
                    "    gl_Position = gl_ModelViewProjectionMatrix * vec4(a_position, 0.0, 1.0);\n" +
                    "}\n";

                String fragmentSource =
                        "#version 110\n" +
                                "varying vec2 v_uv;\n" +
                                "uniform float u_time;\n" +
                                "\n" +
                                "// Simplex 3D Noise function\n" +
                                "vec4 permute(vec4 x) {\n" +
                                "    return mod(((x * 34.0) + 1.0) * x, 289.0);\n" +
                                "}\n" +
                                "\n" +
                                "float snoise(vec3 v) {\n" +
                                "    const vec2 C = vec2(1.0 / 6.0, 1.0 / 3.0);\n" +
                                "    const vec4 D = vec4(0.0, 0.5, 1.0, 2.0);\n" +
                                "\n" +
                                "    vec3 i  = floor(v + dot(v, C.yyy));\n" +
                                "    vec3 x0 = v - i + dot(i, C.xxx);\n" +
                                "\n" +
                                "    vec3 g = step(x0.yzx, x0.xyz);\n" +
                                "    vec3 l = 1.0 - g;\n" +
                                "    vec3 i1 = min(g.xyz, l.zxy);\n" +
                                "    vec3 i2 = max(g.xyz, l.zxy);\n" +
                                "\n" +
                                "    vec3 x1 = x0 - i1 + C.xxx;\n" +
                                "    vec3 x2 = x0 - i2 + C.yyy;\n" +
                                "    vec3 x3 = x0 - D.yyy;\n" +
                                "\n" +
                                "    i = mod(i, 289.0);\n" +
                                "    vec4 p = permute(permute(permute(\n" +
                                "             i.z + vec4(0.0, i1.z, i2.z, 1.0))\n" +
                                "           + i.y + vec4(0.0, i1.y, i2.y, 1.0))\n" +
                                "           + i.x + vec4(0.0, i1.x, i2.x, 1.0));\n" +
                                "\n" +
                                "    float n_ = 0.142857142857;\n" +
                                "    vec3 ns = n_ * D.wyz - D.xzx;\n" +
                                "\n" +
                                "    vec4 j = p - 49.0 * floor(p * ns.z * ns.z);\n" +
                                "\n" +
                                "    vec4 x_ = floor(j * ns.z);\n" +
                                "    vec4 y_ = floor(j - 7.0 * x_);\n" +
                                "\n" +
                                "    vec4 x = x_ * ns.x + ns.yyyy;\n" +
                                "    vec4 y = y_ * ns.x + ns.yyyy;\n" +
                                "    vec4 h = 1.0 - abs(x) - abs(y);\n" +
                                "\n" +
                                "    vec4 b0 = vec4(x.xy, y.xy);\n" +
                                "    vec4 b1 = vec4(x.zw, y.zw);\n" +
                                "\n" +
                                "    vec4 s0 = floor(b0) * 2.0 + 1.0;\n" +
                                "    vec4 s1 = floor(b1) * 2.0 + 1.0;\n" +
                                "    vec4 sh = -step(h, vec4(0.0));\n" +
                                "\n" +
                                "    vec4 a0 = b0.xzyw + s0.xzyw * sh.xxyy;\n" +
                                "    vec4 a1 = b1.xzyw + s1.xzyw * sh.zzww;\n" +
                                "\n" +
                                "    vec3 p0 = vec3(a0.xy, h.x);\n" +
                                "    vec3 p1 = vec3(a0.zw, h.y);\n" +
                                "    vec3 p2 = vec3(a1.xy, h.z);\n" +
                                "    vec3 p3 = vec3(a1.zw, h.w);\n" +
                                "\n" +
                                "    vec4 norm = 1.0 / vec4(dot(p0, p0), dot(p1, p1), dot(p2, p2), dot(p3, p3));\n" +
                                "    p0 *= norm.x;\n" +
                                "    p1 *= norm.y;\n" +
                                "    p2 *= norm.z;\n" +
                                "    p3 *= norm.w;\n" +
                                "\n" +
                                "    vec4 m = max(0.6 - vec4(dot(x0, x0), dot(x1, x1), dot(x2, x2), dot(x3, x3)), 0.0);\n" +
                                "    m = m * m;\n" +
                                "    return 42.0 * dot(m * m, vec4(dot(p0, x0), dot(p1, x1), dot(p2, x2), dot(p3, x3)));\n" +
                                "}\n" +
                                "\n" +
                                "// Turbulence function using Simplex noise\n" +
                                "float turbulence(vec3 p) {\n" +
                                "    float value = 0.0;\n" +
                                "    float amplitude = 1.0;\n" +
                                "    float frequency = 1.0;\n" +
                                "    \n" +
                                "    for (int i = 0; i < 5; i++) {\n" +
                                "        value += amplitude * abs(snoise(p * frequency));\n" +
                                "        amplitude *= 0.5;\n" +
                                "        frequency *= 2.0;\n" +
                                "    }\n" +
                                "    \n" +
                                "    return value * 0.5;\n" +
                                "}\n" +
                                "\n" +
                                "void main() {\n" +
                                "    vec2 uv = vec2(v_uv.x,1.0-v_uv.y);\n" +
                                "    \n" +
                                "    vec2 center = vec2(0.5, 0.5);\n" +
                                "    float dist = distance(uv, center);\n" +
                                "    float maxDist = 0.7071;\n" +
                                "    float normalizedDist = dist / maxDist;\n" +
                                "    \n" +
                                "    float innerRadius = 0.685;\n" +
                                "    float fadeinRadius = 0.68;\n" +
                                "    float outerRadius = 0.7;\n" +
                                "    \n" +
                                "    if (normalizedDist < fadeinRadius) discard;\n" +
                                "    if (normalizedDist > outerRadius) discard;\n" +
                                "    \n" +
                                "    float fadevalue = smoothstep(fadeinRadius,innerRadius,normalizedDist);\n" +
                                "    float ringProgress = max(0.0,(normalizedDist - innerRadius)) / (outerRadius - innerRadius);\n" +
                                "    \n" +
                                "    float dx = uv.x - 0.5;\n" +
                                "    float dy = v_uv.y - 0.5;\n" +
                                "    float angle= abs(atan(dx / dy));\n" +
                                "    \n" +
                                "    float scrollSpeed = 0.3;\n" +
                                "    vec3 noiseCoord = vec3(angle * 40.0 , dist*42.0 - u_time * scrollSpeed, 0.0);\n" +
                                "    \n" +
                                "    float noiseValue = pow(turbulence(noiseCoord),1.0);\n" +
                                "    \n" +
                                "    float gradientValue = 1.0*(1.0-ringProgress);\n" +
                                "    float totalalpha = fadevalue*(1.0-ringProgress);\n" +
                                "    \n" +
                                "    float dissolveThreshold = gradientValue * 0.7;\n" +
                                "    \n" +
                                "    float dissolveWidth = 0.3; " +
                                "    float dissolved = smoothstep(dissolveThreshold - dissolveWidth, \n" +
                                "                                 dissolveThreshold + dissolveWidth, \n" +
                                "                                 noiseValue);\n" +
                                "    \n" +
                                "    float coreProtection = smoothstep(innerRadius , innerRadius + 0.01, normalizedDist);\n" +
                                "    float protectedDissolved =1.0 - dissolved * coreProtection;\n" +
                                "    \n" +
                                "    vec3 flameBaseColor = vec3(0.9, 0.75, 1.0);\n" +
                                "    vec3 outerFlameColor = vec3(0.7, 0.4, 0.95);\n" +
                                "    \n" +
                                "    vec3 finalColor = mix(outerFlameColor, flameBaseColor, pow(protectedDissolved,8.0));\n" +
                                "    \n" +
                                "    float innerDist = abs(normalizedDist - innerRadius);\n" +
                                "    float glowIntensity = totalalpha;\n" +
                                "    vec3 glowColor = vec3(1.0, 0.95, 0.5);\n" +
                                "    \n" +
                                "    float alpha = totalalpha * protectedDissolved + glowIntensity * 0.3;\n" +
                                "    \n" +
                                "    gl_FragColor = vec4(finalColor, alpha*0.4);\n" +
                                "}\n";

                shaderProgram = ShaderUtil.createShaderProgram(vertexSource, fragmentSource, "PhaseCore");

            } catch (Exception e) {
                log.error("PhaseCore Shader error: " + e.getMessage(), e);
            }
        }
        
        private void createBuffers() {
            float halfSize = canvasSize * 0.5f;
            vbo = ShaderUtil.createQuadVBO(halfSize, halfSize);
            ibo = ShaderUtil.createQuadIBO();
        }
        
        private void cacheUniformLocations() {
            if (shaderProgram <= 0) return;
            int[] locs = ShaderUtil.getUniformLocations(shaderProgram, "u_time");
            uTimeLoc = locs[0];
        }

        @Override
        public void init(CombatEntityAPI entity) {
        }

        @Override
        public void cleanup() {
            ShaderUtil.cleanupAll(shaderProgram, vbo, ibo);
            shaderProgram = 0;
            vbo = 0;
            ibo = 0;
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
            if (layer == CombatEngineLayers.ABOVE_SHIPS_LAYER && ship != null && ship.isAlive() && shaderProgram > 0) {
                Vector2f center = ship.getLocation();
                if (center == null) return;
                
                GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
                ShaderUtil.pushAllMatrices();
                
                ShaderUtil.setupWorldTransform(center.x, center.y, rotationAngle);
                
                GL20.glUseProgram(shaderProgram);
                GL20.glUniform1f(uTimeLoc, timer);
                
                ShaderUtil.renderQuadAdditive(shaderProgram, vbo, ibo);
                
                ShaderUtil.popAllMatrices();
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
