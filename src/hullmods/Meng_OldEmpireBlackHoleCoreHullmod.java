package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseCombatLayeredRenderingPlugin;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import java.awt.Color;
import java.util.EnumSet;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.dark.shaders.distortion.WaveDistortion;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

/**
 * Five-stage procedural black-hole core shared by the OldEmpire hull family.
 * The custom-data keys intentionally match the axial-weapon prototype.
 */
public class Meng_OldEmpireBlackHoleCoreHullmod extends BaseHullMod {
    public static final String VALUE_KEY = "moyu_black_hole_core_value";
    public static final String MIN_RADIUS_KEY = "moyu_black_hole_core_min_radius";
    public static final String MAX_RADIUS_KEY = "moyu_black_hole_core_max_radius";
    public static final String FORWARD_OFFSET_KEY = "moyu_black_hole_core_forward_offset";
    public static final String SIDE_OFFSET_KEY = "moyu_black_hole_core_side_offset";

    private static final String RENDERER_KEY = "Meng_OldEmpire_black_hole_core_renderer";
    private static final String CONFIGURED_KEY = "Meng_OldEmpire_black_hole_core_configured";
    private static final float DEFAULT_MIN_RADIUS = 8f;
    private static final float DEFAULT_MAX_RADIUS = 15f;

    private static final Color[] STAGE_COLORS = new Color[] {
        new Color(42, 112, 255),
        new Color(35, 225, 225),
        new Color(255, 224, 70),
        new Color(255, 137, 35),
        new Color(255, 45, 38)
    };

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || ship == null || !engine.isEntityInPlay(ship)) {
            return;
        }
        configureCore(ship);
        if (!(ship.getCustomData().get(VALUE_KEY) instanceof Number)) {
            ship.setCustomData(VALUE_KEY, Float.valueOf(1f));
        }
        if (!(ship.getCustomData().get(RENDERER_KEY)
                instanceof BlackHoleRenderer)) {
            BlackHoleRenderer renderer = new BlackHoleRenderer(ship, engine);
            ship.setCustomData(RENDERER_KEY, renderer);
            engine.addLayeredRenderingPlugin(renderer);
        }
        Meng_OldEmpireCorePdSystem.attach(ship, engine);
    }

    public static void setCoreValue(ShipAPI ship, float value) {
        if (ship != null) {
            ship.setCustomData(VALUE_KEY, Float.valueOf(clamp(value, 1f, 100f)));
        }
    }

    public static float getCoreValue(ShipAPI ship) {
        return number(ship, VALUE_KEY, 1f);
    }

    public static Color getCoreColor(ShipAPI ship) {
        return stageColor(getCoreValue(ship));
    }

    public static boolean isEmpoweredShotAvailable(ShipAPI ship) {
        return getCoreValue(ship) > 70f;
    }

    public static boolean tryConsumeEmpoweredShot(ShipAPI ship) {
        float value = getCoreValue(ship);
        if (value <= 70f) {
            return false;
        }
        setCoreValue(ship, value - 40f);
        return true;
    }

    public static int getCoreStage(ShipAPI ship) {
        return Math.min(5, 1 + (int) ((getCoreValue(ship) - 1f) / 20f));
    }

    public static void setCoreSize(ShipAPI ship, float minimumRadius, float maximumRadius) {
        if (ship == null) {
            return;
        }
        float min = Math.max(1f, minimumRadius);
        float max = Math.max(min, maximumRadius);
        ship.setCustomData(MIN_RADIUS_KEY, Float.valueOf(min));
        ship.setCustomData(MAX_RADIUS_KEY, Float.valueOf(max));
    }

    public static void setCoreOffset(ShipAPI ship, float forward, float side) {
        if (ship == null) {
            return;
        }
        ship.setCustomData(FORWARD_OFFSET_KEY, Float.valueOf(forward));
        ship.setCustomData(SIDE_OFFSET_KEY, Float.valueOf(side));
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return false;
    }

    private static void configureCore(ShipAPI ship) {
        if (ship.getCustomData().get(CONFIGURED_KEY) != null) {
            return;
        }
        String hullId = ship.getHullSpec() == null ? "" : ship.getHullSpec().getHullId();
        if ("Meng_OldEmpire_001".equals(hullId)) {
            setCoreSize(ship, 5.2f, 10.2f);
            setCoreOffset(ship, -11f, 0f);
        } else if ("Meng_OldEmpire_002".equals(hullId)) {
            setCoreSize(ship, 7.2f, 13.5f);
            setCoreOffset(ship, -5f, 0f);
        } else if ("Meng_OldEmpire_003".equals(hullId)) {
            setCoreSize(ship, 13.5f, 25.5f);
            setCoreOffset(ship, 1f, 0f);
        } else if ("Meng_OldEmpire_004".equals(hullId)) {
            setCoreSize(ship, 17.5f, 33f);
            setCoreOffset(ship, -32f, 0f);
        } else if ("Meng_OldEmpire_005".equals(hullId)) {
            setCoreSize(ship, 12.5f, 23.5f);
            setCoreOffset(ship, -8f, 0f);
        } else {
            setCoreSize(ship, DEFAULT_MIN_RADIUS, DEFAULT_MAX_RADIUS);
            setCoreOffset(ship, 0f, 0f);
        }
        ship.setCustomData(CONFIGURED_KEY, Boolean.TRUE);
    }

    private static final class BlackHoleRenderer extends BaseCombatLayeredRenderingPlugin {
        private final ShipAPI ship;
        private final CombatEngineAPI engine;
        private final WaveDistortion distortion;
        private final FlowArc[] flowArcs = new FlowArc[12];
        private float elapsed;
        private float sizeJolt = 1f;
        private float sizeJoltTarget = 1f;
        private float sizeJoltTimer;
        private int lastStage;
        private float transitionAge = 1f;

        private BlackHoleRenderer(ShipAPI ship, CombatEngineAPI engine) {
            this.ship = ship;
            this.engine = engine;
            distortion = new WaveDistortion(ship.getLocation(), new Vector2f());
            distortion.flip(true);
            distortion.setLifetime(0.2f);
            DistortionShader.addDistortion(distortion);
            lastStage = getCoreStage(ship);
            for (int i = 0; i < flowArcs.length; i++) {
                flowArcs[i] = new FlowArc(i);
                resetFlowArc(flowArcs[i], true);
            }
        }

        @Override
        public EnumSet<CombatEngineLayers> getActiveLayers() {
            return EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_LAYER);
        }

        @Override
        public float getRenderRadius() {
            return 1000000f;
        }

        @Override
        public boolean isExpired() {
            return ship == null || engine == null || !engine.isEntityInPlay(ship);
        }

        @Override
        public void advance(float amount) {
            if (engine.isPaused() || ship == null) {
                return;
            }
            elapsed += amount;
            setCoreValue(ship, getCoreValue(ship) + amount);
            float progress = clamp((getCoreValue(ship) - 1f) / 99f, 0f, 1f);

            int stage = getCoreStage(ship);
            if (stage > lastStage) {
                triggerStageTransition(stage, progress);
            }
            lastStage = stage;
            transitionAge += amount;
            advanceFlowArcs(amount, progress);

            float finalStage = smooth(clamp((getCoreValue(ship) - 80f) / 8f, 0f, 1f));
            if (finalStage > 0f) {
                sizeJoltTimer -= amount;
                if (sizeJoltTimer <= 0f) {
                    sizeJoltTarget = random(0.78f, 1.24f);
                    sizeJoltTimer = random(0.32f, 0.72f);
                }
                float response = 1f - (float) Math.pow(0.002f, amount);
                sizeJolt += (sizeJoltTarget - sizeJolt) * response;
            } else {
                sizeJolt += (1f - sizeJolt) * Math.min(1f, amount * 4f);
            }
            updateDistortion();
        }

        @Override
        public void render(CombatEngineLayers layer, ViewportAPI viewport) {
            if (ship == null || !ship.isAlive()) {
                return;
            }
            float value = getCoreValue(ship);
            float progress = clamp((value - 1f) / 99f, 0f, 1f);
            float eased = smooth(progress);
            float minRadius = number(ship, MIN_RADIUS_KEY, DEFAULT_MIN_RADIUS);
            float maxRadius = Math.max(minRadius,
                    number(ship, MAX_RADIUS_KEY, DEFAULT_MAX_RADIUS));
            float radius = lerp(minRadius, maxRadius, eased);
            Vector2f center = getCoreCenter(ship, elapsed, progress);
            if (!viewport.isNearViewport(center, radius * 3f + 24f)) {
                return;
            }

            Color color = stageColor(value);
            float coreRotation = elapsed * lerp(12f, 82f, eased);
            float flowRotation = elapsed * lerp(20f, 168f, eased);
            float pulse = 1f + (0.012f + 0.028f * progress)
                    * (float) Math.sin(elapsed * lerp(2.4f, 6.8f, progress));
            float finalStage = smooth(clamp((value - 80f) / 8f, 0f, 1f));
            radius *= pulse * lerp(1f, sizeJolt, finalStage);
            if (transitionAge < 0.42f) {
                float transition = clamp(transitionAge / 0.42f, 0f, 1f);
                radius *= 1f - 0.12f * (float) Math.sin(Math.PI * transition);
            }

            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glEnable(GL11.GL_LINE_SMOOTH);

            drawOuterHalo(center, radius, color, progress);
            drawAccretionArcs(center, radius, color, progress, flowArcs, ship.getFacing());
            drawStageTransition(center, radius, color, transitionAge);
            drawInfall(center, radius, flowRotation, color, progress);
            drawEventHorizon(center, radius, color, progress);
            drawPhotonRing(center, radius, coreRotation, color, progress);

            GL11.glPopAttrib();
        }

        private void updateDistortion() {
            float value = getCoreValue(ship);
            float progress = clamp((value - 1f) / 99f, 0f, 1f);
            float eased = smooth(progress);
            float minRadius = number(ship, MIN_RADIUS_KEY, DEFAULT_MIN_RADIUS);
            float maxRadius = Math.max(minRadius,
                    number(ship, MAX_RADIUS_KEY, DEFAULT_MAX_RADIUS));
            float radius = lerp(minRadius, maxRadius, eased);
            float finalStage = smooth(clamp((value - 80f) / 8f, 0f, 1f));
            radius *= lerp(1f, sizeJolt, finalStage);

            float pulse = 1f + (0.025f + 0.055f * progress)
                    * (float) Math.sin(elapsed * lerp(3.2f, 9.5f, progress));
            float intensity = lerp(0.6f, 6.4f,
                    (float) Math.pow(progress, 1.35f)) * pulse;
            distortion.setLocation(getCoreCenter(ship, elapsed, progress));
            distortion.setSize(radius * lerp(1.02f, 1.16f, progress));
            distortion.setIntensity(Math.max(0.1f, intensity));
            distortion.setLifetime(0.2f);
        }

        private void advanceFlowArcs(float amount, float progress) {
            int activeCount = 5 + Math.round(progress * 7f);
            float speedScale = lerp(0.72f, 2.15f, smooth(progress));
            for (int i = 0; i < flowArcs.length; i++) {
                FlowArc arc = flowArcs[i];
                if (i >= activeCount) {
                    continue;
                }
                arc.age += amount;
                arc.angle = wrap(arc.angle + arc.speed * speedScale * amount);
                if (arc.age >= arc.life) {
                    resetFlowArc(arc, false);
                }
            }
        }

        private void triggerStageTransition(int stage, float progress) {
            transitionAge = 0f;
            float minRadius = number(ship, MIN_RADIUS_KEY, DEFAULT_MIN_RADIUS);
            float maxRadius = Math.max(minRadius,
                    number(ship, MAX_RADIUS_KEY, DEFAULT_MAX_RADIUS));
            float radius = lerp(minRadius, maxRadius, smooth(progress));
            Vector2f center = getCoreCenter(ship, elapsed, progress);

            RippleDistortion ripple = new RippleDistortion(center,
                    new Vector2f(ship.getVelocity()));
            ripple.flip(true);
            ripple.setSize(radius * 1.34f);
            ripple.setIntensity(1.8f + stage * 1.15f);
            ripple.setFrameRate(72f + stage * 7f);
            ripple.fadeInSize(0.20f);
            ripple.fadeOutIntensity(0.36f);
            DistortionShader.addDistortion(ripple);
        }
    }

    private static final class FlowArc {
        private final int seed;
        private float age;
        private float life;
        private float angle;
        private float speed;
        private float orbit;
        private float outerLift;
        private float sweep;
        private float thickness;
        private float alpha;
        private float hotness;

        private FlowArc(int seed) {
            this.seed = seed;
        }
    }

    private static void resetFlowArc(FlowArc arc, boolean initial) {
        arc.life = random(1.25f, 3.10f);
        arc.age = initial ? random(0f, arc.life) : 0f;
        arc.angle = random(0f, 360f);
        arc.speed = (Math.random() < 0.82f ? 1f : -1f) * random(27f, 62f);
        arc.orbit = random(0.70f, 1.18f);
        arc.outerLift = random(0.08f, 0.43f);
        arc.sweep = random(72f, 176f);
        arc.thickness = random(0.035f, 0.074f);
        arc.alpha = random(0.17f, 0.29f);
        arc.hotness = random(0.22f, 0.76f);
    }

    private static Vector2f getCoreCenter(ShipAPI ship, float elapsed, float progress) {
        float forward = number(ship, FORWARD_OFFSET_KEY, 0f);
        float side = number(ship, SIDE_OFFSET_KEY, 0f);
        float jitterLevel = smooth(clamp((progress - 0.20f) / 0.80f, 0f, 1f));
        float amplitude = 2.2f * jitterLevel * jitterLevel;
        float jitterForward = amplitude * (0.68f * (float) Math.sin(elapsed * 18.5f)
                + 0.32f * (float) Math.sin(elapsed * 33.7f + 1.4f));
        float jitterSide = amplitude * (0.64f * (float) Math.sin(elapsed * 23.4f + 2.2f)
                + 0.36f * (float) Math.sin(elapsed * 41.2f + 0.5f));

        Vector2f result = new Vector2f(ship.getLocation());
        Vector2f.add(result, polar(ship.getFacing(), forward + jitterForward), result);
        Vector2f.add(result, polar(ship.getFacing() + 90f, side + jitterSide), result);
        return result;
    }

    static Vector2f getCoreCenterForEffects(ShipAPI ship, float elapsed) {
        float progress = clamp((getCoreValue(ship) - 1f) / 99f, 0f, 1f);
        return getCoreCenter(ship, elapsed, progress);
    }

    static float getCoreRadiusForEffects(ShipAPI ship) {
        float progress = clamp((getCoreValue(ship) - 1f) / 99f, 0f, 1f);
        float minimum = number(ship, MIN_RADIUS_KEY, DEFAULT_MIN_RADIUS);
        float maximum = Math.max(minimum,
                number(ship, MAX_RADIUS_KEY, DEFAULT_MAX_RADIUS));
        return lerp(minimum, maximum, smooth(progress));
    }

    private static void drawOuterHalo(Vector2f center, float radius, Color color,
                                      float progress) {
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        drawSoftRing(center, radius * (1.14f + 0.10f * progress),
                radius * (0.92f + 0.28f * progress), color,
                0.105f + 0.15f * progress, 72);
        drawSoftRing(center, radius * (1.54f + 0.26f * progress),
                radius * (0.64f + 0.48f * progress), color,
                0.035f + 0.085f * progress, 72);
        drawSoftRing(center, radius * (1.92f + 0.36f * progress),
                radius * (0.42f + 0.48f * progress), color,
                0.012f + 0.045f * progress, 72);
    }

    private static void drawAccretionArcs(Vector2f center, float radius, Color color,
                                          float progress, FlowArc[] arcs,
                                          float shipFacing) {
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        int arcCount = 5 + Math.round(progress * 7f);
        for (int i = 0; i < arcCount; i++) {
            FlowArc arc = arcs[i];
            float fadeTime = Math.min(0.52f, arc.life * 0.30f);
            float fade = smooth(Math.min(clamp(arc.age / fadeTime, 0f, 1f),
                    clamp((arc.life - arc.age) / fadeTime, 0f, 1f)));
            float orbit = radius * (arc.orbit + arc.outerLift * progress);
            float start = arc.angle;
            float sweep = arc.sweep + progress * 24f;
            float thickness = radius * (arc.thickness + progress * 0.026f);

            float midpoint = start + sweep * 0.5f;
            float approaching = 0.5f + 0.5f * (float) Math.cos(
                    Math.toRadians(midpoint - (shipFacing + 90f)));
            float beaming = lerp(0.78f, lerp(0.38f, 1.42f, approaching),
                    0.38f + 0.62f * progress);
            Color arcColor = blend(color, Color.WHITE,
                    arc.hotness * (0.18f + 0.62f * approaching));
            arcColor = scaleColor(arcColor, lerp(0.62f, 1f, approaching));
            float alpha = arc.alpha * fade * beaming * (0.72f + progress * 1.15f);
            drawWispyArc(center, orbit, thickness, start, sweep, arcColor,
                    alpha, 30 + Math.round(progress * 10f), arc.seed);
        }
    }

    private static void drawStageTransition(Vector2f center, float radius,
                                            Color color, float age) {
        final float duration = 0.42f;
        if (age < 0f || age >= duration) {
            return;
        }
        float progress = clamp(age / duration, 0f, 1f);
        float inward = smooth(progress);
        float ringRadius = radius * lerp(1.48f, 0.62f, inward);
        float alpha = (float) Math.sin(Math.PI * progress) * 0.42f;
        drawSoftRing(center, ringRadius, radius * lerp(0.13f, 0.055f, inward),
                blend(color, Color.WHITE, 0.28f), alpha, 64);
    }

    private static void drawInfall(Vector2f center, float radius, float rotation,
                                   Color color, float progress) {
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glLineWidth(1f);
        GL11.glBegin(GL11.GL_LINES);
        for (int i = 0; i < 18; i++) {
            float phase = elapsedPhase(rotation * 0.0028f + i * 0.137f);
            float inward = 1f - phase;
            float orbit = radius * (0.58f + 1.12f * inward);
            float angle = rotation * (0.72f + 0.025f * i)
                    + i * 137.5f + inward * inward * 126f;
            Vector2f p = offset(center, angle, orbit);
            Vector2f q = offset(p, angle + 112f,
                    radius * (0.055f + 0.06f * inward));
            float alpha = (float) Math.sin(Math.PI * phase)
                    * (0.20f + 0.28f * progress);
            glColor(blend(color, Color.WHITE, 0.38f), alpha);
            vertex(p);
            glColor(color, 0f);
            vertex(q);
        }
        GL11.glEnd();
    }

    private static void drawEventHorizon(Vector2f center, float radius,
                                         Color color, float progress) {
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glColor4f(0f, 0f, 0.006f, 0.995f);
        vertex(center);
        for (int i = 0; i <= 72; i++) {
            float angle = 360f * i / 72f;
            float edge = radius * (0.505f + 0.008f
                    * (float) Math.sin(Math.toRadians(angle * 3f)));
            GL11.glColor4f(0f, 0f, 0.008f, 0.965f);
            vertex(offset(center, angle, edge));
        }
        GL11.glEnd();

        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        drawSoftRing(center, radius * 0.515f, radius * 0.10f,
                color, 0.42f + 0.18f * progress, 72);
    }

    private static void drawPhotonRing(Vector2f center, float radius,
                                       float rotation, Color color,
                                       float progress) {
        Color hot = blend(color, Color.WHITE, 0.68f - 0.18f * progress);
        drawArcLine(center, radius * 0.535f, rotation, 252f,
                hot, 0.80f, 1.65f + progress * 0.9f, 54, 1);
        drawArcLine(center, radius * 0.565f, -rotation * 0.82f + 210f, 104f,
                color, 0.70f, 1.15f + progress * 0.65f, 28, 4);
        drawArcLine(center, radius * 0.625f, rotation * 0.36f + 25f, 74f,
                blend(color, Color.WHITE, 0.35f), 0.34f, 0.9f, 22, 7);
    }

    private static void drawSoftRing(Vector2f center, float ringRadius,
                                     float halfWidth, Color color, float alpha,
                                     int segments) {
        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
        for (int i = 0; i <= segments; i++) {
            float angle = 360f * i / segments;
            glColor(color, 0f);
            vertex(offset(center, angle, ringRadius + halfWidth));
            glColor(color, alpha);
            vertex(offset(center, angle, ringRadius));
        }
        GL11.glEnd();
        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
        for (int i = 0; i <= segments; i++) {
            float angle = 360f * i / segments;
            glColor(color, alpha);
            vertex(offset(center, angle, ringRadius));
            glColor(color, 0f);
            vertex(offset(center, angle, Math.max(0f, ringRadius - halfWidth)));
        }
        GL11.glEnd();
    }

    private static void drawArcStrip(Vector2f center, float radius,
                                     float thickness, float start, float sweep,
                                     Color color, float alpha, int segments,
                                     int seed) {
        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
        for (int i = 0; i <= segments; i++) {
            float along = (float) i / segments;
            float angle = start + sweep * along;
            float taper = (float) Math.sin(Math.PI * along);
            float wobble = thickness * 0.18f
                    * (float) Math.sin(along * Math.PI * (3f + seed % 4) + seed);
            float width = thickness * (0.18f + 0.82f * taper);
            glColor(color, alpha * taper);
            vertex(offset(center, angle, radius + wobble + width));
            vertex(offset(center, angle, radius + wobble - width));
        }
        GL11.glEnd();
    }

    private static void drawWispyArc(Vector2f center, float radius,
                                     float thickness, float start, float sweep,
                                     Color color, float alpha, int segments,
                                     int seed) {
        drawArcStrip(center, radius, thickness * 2.8f, start, sweep,
                color, alpha * 0.10f, segments, seed);
        drawArcStrip(center, radius, thickness * 1.75f, start, sweep,
                color, alpha * 0.22f, segments, seed);
        drawArcStrip(center, radius, thickness, start, sweep,
                color, alpha * 0.58f, segments, seed);
    }

    private static void drawArcLine(Vector2f center, float radius, float start,
                                    float sweep, Color color, float alpha,
                                    float width, int segments, int seed) {
        GL11.glLineWidth(width);
        GL11.glBegin(GL11.GL_LINE_STRIP);
        for (int i = 0; i <= segments; i++) {
            float along = (float) i / segments;
            float angle = start + sweep * along;
            float taper = (float) Math.sin(Math.PI * along);
            float shaped = radius * (1f + 0.012f
                    * (float) Math.sin(along * Math.PI * (4f + seed) + seed));
            glColor(color, alpha * taper);
            vertex(offset(center, angle, shaped));
        }
        GL11.glEnd();
    }

    private static Color stageColor(float value) {
        float scaled = clamp((value - 1f) / 80f, 0f, 1f)
                * (STAGE_COLORS.length - 1);
        int lower = Math.min(STAGE_COLORS.length - 1, (int) Math.floor(scaled));
        int upper = Math.min(STAGE_COLORS.length - 1, lower + 1);
        return blend(STAGE_COLORS[lower], STAGE_COLORS[upper],
                smooth(scaled - lower));
    }

    private static Color blend(Color a, Color b, float amount) {
        float t = clamp(amount, 0f, 1f);
        return new Color(
                Math.round(lerp(a.getRed(), b.getRed(), t)),
                Math.round(lerp(a.getGreen(), b.getGreen(), t)),
                Math.round(lerp(a.getBlue(), b.getBlue(), t)));
    }

    private static Color scaleColor(Color color, float scale) {
        return new Color(
                Math.round(clamp(color.getRed() * scale, 0f, 255f)),
                Math.round(clamp(color.getGreen() * scale, 0f, 255f)),
                Math.round(clamp(color.getBlue() * scale, 0f, 255f)));
    }

    private static float number(ShipAPI ship, String key, float fallback) {
        if (ship == null) {
            return fallback;
        }
        Object value = ship.getCustomData().get(key);
        return value instanceof Number ? ((Number) value).floatValue() : fallback;
    }

    private static float elapsedPhase(float value) {
        float result = value - (float) Math.floor(value);
        return result < 0f ? result + 1f : result;
    }

    private static float wrap(float angle) {
        float result = angle % 360f;
        return result < 0f ? result + 360f : result;
    }

    private static Vector2f offset(Vector2f center, float angle, float distance) {
        Vector2f result = polar(angle, distance);
        Vector2f.add(result, center, result);
        return result;
    }

    private static Vector2f polar(float angle, float distance) {
        double radians = Math.toRadians(angle);
        return new Vector2f((float) Math.cos(radians) * distance,
                (float) Math.sin(radians) * distance);
    }

    private static float smooth(float value) {
        float x = clamp(value, 0f, 1f);
        return x * x * (3f - 2f * x);
    }

    private static float lerp(float a, float b, float amount) {
        return a + (b - a) * amount;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float random(float min, float max) {
        return min + (float) Math.random() * (max - min);
    }

    private static void glColor(Color color, float alpha) {
        GL11.glColor4f(color.getRed() / 255f, color.getGreen() / 255f,
                color.getBlue() / 255f, clamp(alpha, 0f, 1f));
    }

    private static void vertex(Vector2f point) {
        GL11.glVertex2f(point.x, point.y);
    }
}
