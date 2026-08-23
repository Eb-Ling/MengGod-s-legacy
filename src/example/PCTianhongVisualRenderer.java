package data.scripts.specialization;

import java.awt.Color;
import java.util.Random;

import org.apache.log4j.Level;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.ExplosionEntityPlugin.ExplosionFleetDamage;
import com.fs.starfarer.api.impl.campaign.ExplosionEntityPlugin.ExplosionParams;
import com.fs.starfarer.api.util.Misc;

public class PCTianhongVisualRenderer {

    public static final float MAX_RENDER_RADIUS = 5000f;
    protected static final String SHADER_VERT = "data/shaders/PC_Tianhong.vert";
    protected static final String SHADER_FRAG = "data/shaders/PC_Tianhong.frag";

    protected static final Color TARGET_VALID = new Color(130, 235, 255, 190);
    protected static final Color TARGET_INVALID = new Color(255, 90, 70, 175);
    protected static final Color BEAM_CORE = new Color(236, 252, 255, 245);
    protected static final Color BEAM_MID = new Color(82, 196, 255, 178);
    protected static final Color BEAM_FRINGE = new Color(36, 122, 255, 95);
    protected static final Color AURA = new Color(92, 205, 255, 118);
    protected static final Color IMPACT = new Color(255, 204, 102, 186);
    protected static final Color SWIRL_BLUE = new Color(78, 215, 255, 158);
    protected static final Color SWIRL_WHITE = new Color(245, 255, 255, 225);
    protected static final Color IMPACT_WHITE = new Color(255, 252, 226, 245);
    protected static final Color IMPACT_ORANGE = new Color(255, 142, 52, 215);
    protected static final Color IMPACT_RED = new Color(255, 62, 28, 112);
    protected static final Color GLASS_BLUE = new Color(130, 235, 255, 125);
    protected static final Color DEEP_VIOLET = new Color(92, 80, 255, 72);
    protected static final Color PATH_WAKE = new Color(110, 220, 255, 78);
    protected static final Color PATH_WAKE_CORE = new Color(235, 252, 255, 132);
    protected static final Color REVERSE_SCOOP_VEIL = new Color(70, 205, 255, 86);
    protected static final Color REVERSE_SCOOP_FRINGE = new Color(90, 120, 255, 60);
    protected static final Color REVERSE_SCOOP_CORE = new Color(238, 252, 255, 176);
    protected static final Color REVERSE_SCOOP_RIM = new Color(120, 235, 255, 132);
    protected static final Color PATH_PARTICLE_SMOKE = new Color(84, 170, 255, 58);
    protected static final Color PATH_PARTICLE_FRINGE = new Color(96, 215, 255, 112);
    protected static final Color PATH_PARTICLE_CORE = new Color(238, 252, 255, 205);

    protected static final Random RANDOM = new Random();
    protected static final VisualState STATE = new VisualState();
    protected static float elapsed = 0f;
    protected static float fade = 0f;
    protected static float boxUtilPulseElapsed = 0f;
    protected static SpriteAPI beamCoreSprite = null;
    protected static SpriteAPI beamFringeSprite = null;
    protected static SpriteAPI glowSprite = null;
    protected static SpriteAPI noiseSprite = null;
    protected static SpriteAPI streakSprite = null;
    protected static int shaderProgram = 0;
    protected static boolean shaderInitAttempted = false;
    protected static boolean shaderDisabled = false;
    protected static int uType = -1;
    protected static int uTime = -1;
    protected static int uAlpha = -1;
    protected static int uCharge = -1;
    protected static int uProgress = -1;
    protected static int uColorA = -1;
    protected static int uColorB = -1;
    protected static int uParams = -1;
    protected static int uNoiseTex = -1;
    protected static int uStreakTex = -1;
    protected static int uQuad = -1;

    public static class VisualState {
        public boolean targeting = false;
        public boolean firing = false;
        public boolean validTarget = false;
        public LocationAPI location = null;
        public CampaignFleetAPI fleet = null;
        public SectorEntityToken target = null;
        public Vector2f cursor = null;
        public float progress = 0f;
        public float lastUpdateElapsed = 999f;
    }

    public static void updateTargeting(LocationAPI location, CampaignFleetAPI fleet, Vector2f cursor,
                                       SectorEntityToken target, boolean validTarget) {
        STATE.targeting = true;
        STATE.firing = false;
        STATE.location = location;
        STATE.fleet = fleet;
        STATE.cursor = cursor == null ? null : new Vector2f(cursor);
        STATE.target = target;
        STATE.validTarget = validTarget;
        STATE.progress = 0f;
        STATE.lastUpdateElapsed = 0f;
    }

    public static void updateFiring(LocationAPI location, CampaignFleetAPI fleet, SectorEntityToken target, float progress) {
        STATE.targeting = false;
        STATE.firing = true;
        STATE.location = location;
        STATE.fleet = fleet;
        STATE.target = target;
        STATE.cursor = target == null ? null : new Vector2f(target.getLocation());
        STATE.validTarget = true;
        STATE.progress = Math.max(0f, Math.min(1f, progress));
        STATE.lastUpdateElapsed = 0f;
    }

    public static void deactivate() {
        STATE.targeting = false;
        STATE.firing = false;
        STATE.validTarget = false;
        STATE.location = null;
        STATE.fleet = null;
        STATE.target = null;
        STATE.cursor = null;
        STATE.progress = 0f;
        STATE.lastUpdateElapsed = 999f;
        boxUtilPulseElapsed = 0f;
    }

    public static void advance(float amount) {
        elapsed += amount;
        STATE.lastUpdateElapsed += amount;
        boolean show = isRenderable();
        if (show) {
            fade = Math.min(1f, fade + amount * 3.5f);
        } else {
            fade = Math.max(0f, fade - amount * 2.4f);
        }
    }

    public static boolean isVisible() {
        return fade > 0.01f;
    }

    public static void render(CampaignEngineLayers layer, ViewportAPI viewport) {
        if (viewport == null || fade <= 0.01f || !isRenderable()) {
            return;
        }

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

            float alpha = viewport.getAlphaMult() * fade;
            if (STATE.targeting) {
                renderTargeting(layer, viewport, alpha);
            } else if (STATE.firing) {
                renderFiring(layer, viewport, alpha);
            }
        } finally {
            if (shaderProgram != 0) {
                GL20.glUseProgram(0);
            }
            GL11.glLineWidth(1f);
            GL11.glDisable(GL11.GL_LINE_SMOOTH);
            GL11.glPopAttrib();
        }
    }

    public static void spawnFiringParticles(float amount) {
        if (!isFiringRenderable()) {
            return;
        }

        LocationAPI location = STATE.location;
        Vector2f from = new Vector2f(STATE.fleet.getLocation());
        Vector2f to = getTargetPoint();
        Vector2f line = Vector2f.sub(to, from, new Vector2f());
        float length = line.length();
        if (length <= 1f) {
            return;
        }

        line.normalise();
        Vector2f normal = new Vector2f(-line.y, line.x);
        float warmupCharge = getWarmupCharge();
        float beamCharge = getBeamCharge();
        float impactCharge = getImpactCharge();

        int count = Math.max(0, Math.min(5, Math.round(amount * 22f * beamCharge)));
        for (int i = 0; i < count; i++) {
            float t = RANDOM.nextFloat();
            Vector2f loc = pointOnLine(from, line, normal, length, t,
                    (RANDOM.nextFloat() - 0.5f) * (5f + 16f * beamCharge));
            Vector2f vel = new Vector2f(line);
            vel.scale(260f + RANDOM.nextFloat() * (220f + 200f * beamCharge));
            Vector2f sideVel = new Vector2f(normal);
            sideVel.scale((RANDOM.nextFloat() - 0.5f) * 9f * beamCharge);
            Vector2f.add(vel, sideVel, vel);
            Color color = RANDOM.nextFloat() < 0.72f ? BEAM_MID : BEAM_CORE;
            location.addHitParticle(loc, vel, 1.8f + RANDOM.nextFloat() * (3.4f + 2.8f * beamCharge), 0.65f,
                    0.16f + RANDOM.nextFloat() * 0.24f, color);
        }

        int wakeCount = Math.max(0, Math.min(18, Math.round(amount * 170f * beamCharge)));
        for (int i = 0; i < wakeCount; i++) {
            float t = RANDOM.nextFloat();
            float width = getReverseScoopWidth(t, beamCharge);
            Vector2f loc = pointOnLine(from, line, normal, length, t, (RANDOM.nextFloat() - 0.5f) * width);
            Vector2f vel = new Vector2f(line);
            vel.scale(70f + RANDOM.nextFloat() * (100f + 80f * beamCharge));
            Vector2f sideVel = new Vector2f(normal);
            sideVel.scale((RANDOM.nextFloat() - 0.5f) * (5f + 8f * beamCharge));
            Vector2f.add(vel, sideVel, vel);

            float size = (1.8f + RANDOM.nextFloat() * 4.6f) * (0.70f + 0.24f * beamCharge) * (0.78f + RANDOM.nextFloat() * 0.48f);
            float brightness = 0.20f + RANDOM.nextFloat() * 0.16f;
            Color color = RANDOM.nextFloat() < 0.58f ? PATH_PARTICLE_SMOKE : PATH_PARTICLE_FRINGE;
            location.addHitParticle(loc, vel, size, brightness,
                    0.34f + RANDOM.nextFloat() * 0.44f, color);
        }

        int coreWakeCount = Math.max(0, Math.min(8, Math.round(amount * 86f * beamCharge)));
        for (int i = 0; i < coreWakeCount; i++) {
            float t = RANDOM.nextFloat();
            float width = 7f + 12f * beamCharge;
            Vector2f loc = pointOnLine(from, line, normal, length, t, (RANDOM.nextFloat() - 0.5f) * width);
            Vector2f vel = new Vector2f(line);
            vel.scale(170f + RANDOM.nextFloat() * (160f + 90f * beamCharge));

            float size = (1.2f + RANDOM.nextFloat() * 3.2f) * (0.82f + 0.18f * beamCharge) * (0.75f + RANDOM.nextFloat() * 0.55f);
            location.addHitParticle(loc, vel, size, 0.48f + RANDOM.nextFloat() * 0.32f,
                    0.20f + RANDOM.nextFloat() * 0.28f, PATH_PARTICLE_CORE);
        }

        int vortexCount = Math.max(0, Math.min(4, Math.round(amount * (8f + 18f * warmupCharge))));
        for (int i = 0; i < vortexCount; i++) {
            Vector2f loc = randomAround(from, 18f + RANDOM.nextFloat() * (26f + 36f * warmupCharge));
            Vector2f inward = Vector2f.sub(from, loc, new Vector2f());
            if (inward.length() > 1f) {
                inward.normalise();
            }
            Vector2f tangent = new Vector2f(-inward.y, inward.x);
            tangent.scale(35f + RANDOM.nextFloat() * (42f + 52f * warmupCharge));
            inward.scale(24f + RANDOM.nextFloat() * (34f + 32f * warmupCharge));
            Vector2f.add(tangent, inward, tangent);
            location.addHitParticle(loc, tangent, 1.8f + RANDOM.nextFloat() * (3f + 3.5f * warmupCharge), 0.65f,
                    0.18f + RANDOM.nextFloat() * 0.28f, RANDOM.nextFloat() < 0.72f ? SWIRL_BLUE : SWIRL_WHITE);
        }
    }

    public static void spawnFinalBurst(LocationAPI location, Vector2f loc, float radius) {
        if (location == null || loc == null) {
            return;
        }
        float safeRadius = Math.max(150f, radius);
        float explosionRadius = Math.max(900f, safeRadius * 3.1f);
        ExplosionParams params = new ExplosionParams(new Color(120, 245, 205, 255),
                location, new Vector2f(loc), explosionRadius, 1.55f);
        params.damage = ExplosionFleetDamage.NONE;
        SectorEntityToken explosion = location.addCustomEntity(Misc.genUID(), "Tianhong shock", "explosion", "neutral", params);
        if (explosion != null) {
            explosion.setLocation(loc.x, loc.y);
        }
        PCTianhongBoxUtilEffects.spawnFinalBurst(loc, safeRadius);
    }

    protected static boolean isRenderable() {
        if (STATE.lastUpdateElapsed > 0.35f || STATE.location == null || STATE.fleet == null) {
            return false;
        }
        if (!STATE.location.isCurrentLocation() || STATE.fleet.getContainingLocation() != STATE.location) {
            return false;
        }
        return STATE.targeting || isFiringRenderable();
    }

    protected static boolean isFiringRenderable() {
        return STATE.firing
                && STATE.target != null
                && STATE.target.isAlive()
                && STATE.target.getContainingLocation() == STATE.location;
    }

    protected static void renderTargeting(CampaignEngineLayers layer, ViewportAPI viewport, float alpha) {
        if (layer != CampaignEngineLayers.ABOVE || STATE.cursor == null) {
            return;
        }

        Vector2f loc = STATE.target != null ? STATE.target.getLocation() : STATE.cursor;
        if (!viewport.isNearViewport(loc, 700f)) {
            return;
        }

        Color color = STATE.validTarget ? TARGET_VALID : TARGET_INVALID;
        float radius = STATE.target != null ? Math.max(80f, STATE.target.getRadius() + 45f) : 95f;
        float pulse = 0.82f + 0.18f * (float) Math.sin(elapsed * 8f);

        GL11.glLineWidth(2.3f);
        setColor(color, alpha * pulse);
        drawCircleLine(loc.x, loc.y, radius, 64);
        drawCornerBox(loc, radius * 1.25f, alpha * pulse, color);
    }

    protected static void renderFiring(CampaignEngineLayers layer, ViewportAPI viewport, float alpha) {
        if (!isFiringRenderable()) {
            return;
        }
        Vector2f from = new Vector2f(STATE.fleet.getLocation());
        Vector2f targetCenter = getTargetPoint();
        Vector2f aim = Vector2f.sub(targetCenter, from, new Vector2f());
        if (aim.length() <= 1f) {
            return;
        }
        aim.normalise();
        Vector2f to = getVisualImpactPoint(targetCenter, aim);
        float length = Misc.getDistance(from, to);
        if (!viewport.isNearViewport(from, length + 800f) && !viewport.isNearViewport(targetCenter, length + 800f)) {
            return;
        }

        Vector2f line = Vector2f.sub(to, from, new Vector2f());
        if (line.length() <= 1f) {
            return;
        }
        line.normalise();
        Vector2f normal = new Vector2f(-line.y, line.x);
        float warmupCharge = getWarmupCharge();
        float beamCharge = getBeamCharge();
        float impactCharge = getImpactCharge();
        float beamSize = 0.22f + 0.78f * beamCharge;
        float fleetCharge = 0.28f + 0.72f * warmupCharge;
        float targetCharge = 0.18f + 0.82f * impactCharge;
        float pulse = 0.86f + 0.14f * (float) Math.sin(elapsed * 10.5f);

        boolean shaderReady = ensureShader();
        if (shaderReady) {
            if (layer == CampaignEngineLayers.TERRAIN_8 && beamCharge > 0.02f) {
                renderReverseScoopVeil(from, line, normal, length, alpha * pulse * beamCharge, beamCharge);
                renderShaderBeam(from, line, length, 54f + 28f * beamSize, alpha * pulse * beamCharge * 0.46f,
                        beamCharge, STATE.progress, new Color(72, 178, 255, 110), new Color(54, 92, 255, 80),
                        0.026f, 0.092f, 0.58f, 0.34f);
            } else if (layer == CampaignEngineLayers.TERRAIN_9 && beamCharge > 0.02f) {
                renderReverseScoopFilaments(from, line, normal, length, alpha * pulse * beamCharge, beamCharge);
                renderShaderBeam(from, line, length, 24f + 10f * beamSize, alpha * pulse * beamCharge * 0.34f,
                        beamCharge, STATE.progress, new Color(160, 238, 255, 150), new Color(62, 150, 255, 100),
                        0.060f, 0.180f, 0.46f, 0.0f);
                renderShaderBeam(from, line, length, 34f + 16f * beamSize, alpha * pulse * beamCharge * 0.78f,
                        beamCharge, STATE.progress, IMPACT_WHITE, BEAM_MID,
                        0.038f, 0.125f, 0.66f, 0.12f);
                renderShaderBeam(from, line, length, 10f + 6f * beamSize, alpha * pulse * beamCharge,
                        beamCharge, STATE.progress, SWIRL_WHITE, new Color(142, 235, 255, 160),
                        0.160f, 0.320f, 0.72f, 0.0f);
            } else if (layer == CampaignEngineLayers.ABOVE) {
                Vector2f chargeCenter = offset(from, line, 8f + 28f * warmupCharge);
                float chargeRadius = (104f + 48f * fleetCharge) * (1f - 0.22f * warmupCharge);
                renderShaderRing(chargeCenter, chargeRadius, alpha * (0.18f + 0.26f * warmupCharge),
                        warmupCharge, STATE.progress, AURA, SWIRL_WHITE);
                renderEmitterBloom(chargeCenter, line, normal, alpha * (0.22f + 0.30f * warmupCharge), fleetCharge);
                if (impactCharge > 0.04f) {
                    float impactSize = Math.max(76f, STATE.target.getRadius() * 0.26f + 42f) * (0.82f + 0.16f * targetCharge);
                    renderShaderImpact(to, line, impactSize, alpha * impactCharge * 0.54f,
                            impactCharge, STATE.progress);
                    renderImpactFlare(to, line, normal, alpha * impactCharge, targetCharge);
                }
            }
        } else {
            if (layer == CampaignEngineLayers.TERRAIN_8 && beamCharge > 0.02f) {
                renderReverseScoopVeil(from, line, normal, length, alpha * pulse * beamCharge, beamCharge);
                renderLanceSheath(from, line, normal, length, alpha * pulse * beamCharge, beamSize);
            } else if (layer == CampaignEngineLayers.TERRAIN_9 && beamCharge > 0.02f) {
                renderReverseScoopFilaments(from, line, normal, length, alpha * pulse * beamCharge, beamCharge);
                renderLanceCore(from, to, line, normal, length, alpha * pulse * beamCharge, beamSize);
            } else if (layer == CampaignEngineLayers.ABOVE) {
                renderEmitterBloom(from, line, normal, alpha * (0.42f + 0.44f * warmupCharge), fleetCharge);
                if (beamCharge > 0.05f) {
                    renderLanceMuzzle(from, line, normal, alpha * beamCharge, beamSize);
                }
                if (impactCharge > 0.04f) {
                    renderSurfaceImpact(to, targetCenter, line, normal, alpha * impactCharge, targetCharge);
                }
            }
        }
    }

    protected static Vector2f getVisualImpactPoint(Vector2f targetCenter, Vector2f fleetToTarget) {
        if (targetCenter == null || fleetToTarget == null || STATE.target == null) {
            return targetCenter == null ? new Vector2f() : new Vector2f(targetCenter);
        }
        float radius = Math.max(0f, STATE.target.getRadius());
        if (radius < 70f) {
            return new Vector2f(targetCenter);
        }

        Vector2f result = new Vector2f(targetCenter);
        Vector2f towardFleet = new Vector2f(fleetToTarget);
        towardFleet.scale(-radius * 0.78f);
        Vector2f.add(result, towardFleet, result);
        return result;
    }

    protected static void renderLanceSheath(Vector2f from, Vector2f line, Vector2f normal, float length,
                                            float alpha, float charge) {
        renderSoftBeamRibbon(from, line, normal, length, 16f + 4f * charge, 24f + 8f * charge,
                DEEP_VIOLET, alpha * 0.045f, 24, elapsed * 0.45f);
        renderSoftBeamRibbon(from, line, normal, length, 7f + 3f * charge, 11f + 4f * charge,
                GLASS_BLUE, alpha * 0.075f, 24, -elapsed * 0.75f);
    }

    protected static void renderPathWake(Vector2f from, Vector2f line, Vector2f normal,
                                         float length, float alpha, float charge) {
        if (from == null || line == null || normal == null || length <= 0f || alpha <= 0f) {
            return;
        }

        for (int band = 0; band < 3; band++) {
            float phase = elapsed * (1.45f + band * 0.16f) + band * 1.77f;
            float sideBias = (band - 1f) * (6f + 5f * charge);
            float bandAlpha = alpha * (0.026f - band * 0.005f);
            Color color = band == 1 ? PATH_WAKE_CORE : PATH_WAKE;

            GL11.glBegin(GL11.GL_QUAD_STRIP);
            for (int i = 0; i <= 34; i++) {
                float t = i / 34f;
                float width = getPathWakeWidth(t, charge) * (1f + band * 0.18f);
                float stream = (float) Math.sin(t * 8.6f - phase) * width * 0.075f
                        + (float) Math.sin(t * 18.0f - phase * 1.45f) * width * 0.026f;
                float localAlpha = bandAlpha * endFade(t)
                        * (0.74f + 0.26f * (float) Math.sin(phase + t * 11.0f));
                Vector2f center = pointOnLine(from, line, normal, length, t, stream + sideBias * (1f - t * 0.45f));
                Vector2f left = offset(center, normal, -width * 0.5f);
                Vector2f right = offset(center, normal, width * 0.5f);

                setColor(color, localAlpha);
                GL11.glVertex2f(left.x, left.y);
                GL11.glVertex2f(right.x, right.y);
            }
            GL11.glEnd();
        }

        GL11.glLineWidth(0.85f);
        GL11.glBegin(GL11.GL_LINES);
        for (int i = 0; i < 7; i++) {
            float t = (elapsed * (0.55f + i * 0.025f) + i * 0.137f) % 1f;
            float width = getPathWakeWidth(t, charge);
            float side = ((i % 3) - 1f) * width * 0.28f;
            Vector2f head = pointOnLine(from, line, normal, length, t, side);
            Vector2f tail = pointOnLine(from, line, normal, length,
                    Math.max(0f, t - 0.030f - 0.010f * charge), side * 0.86f);
            setColor(PATH_WAKE_CORE, alpha * 0.055f * endFade(t));
            GL11.glVertex2f(head.x, head.y);
            setColor(PATH_WAKE, 0f);
            GL11.glVertex2f(tail.x, tail.y);
        }
        GL11.glEnd();
    }

    protected static void renderReverseScoopVeil(Vector2f from, Vector2f line, Vector2f normal,
                                                 float length, float alpha, float charge) {
        if (from == null || line == null || normal == null || length <= 0f || alpha <= 0f) {
            return;
        }

        for (int band = 0; band < 4; band++) {
            float phase = -elapsed * (1.05f + band * 0.18f) + band * 1.73f;
            float sideBias = (band - 1.5f) * 0.33f;
            float bandAlpha = alpha * (0.105f - band * 0.012f);
            Color color = band % 2 == 0 ? REVERSE_SCOOP_VEIL : REVERSE_SCOOP_FRINGE;

            GL11.glBegin(GL11.GL_QUAD_STRIP);
            for (int i = 0; i <= 30; i++) {
                float t = i / 30f;
                float width = getReverseScoopWidth(t, charge) * (1.08f + band * 0.13f) * 0.5f;
                float wander = (float) Math.sin(t * 8.2f + phase) * width * 0.0367f
                        + (float) Math.sin(t * 17.0f - phase * 0.7f) * width * 0.015f;
                float centerBias = sideBias * width * (1f - t) * 0.55f;
                float edgeFade = endFade(t);
                float localAlpha = bandAlpha * edgeFade
                        * (0.72f + 0.28f * (float) Math.sin(phase + t * 10.0f));

                Vector2f center = pointOnLine(from, line, normal, length, t, wander + centerBias);
                Vector2f left = offset(center, normal, -width * 0.5f);
                Vector2f right = offset(center, normal, width * 0.5f);

                setColor(color, localAlpha);
                GL11.glVertex2f(left.x, left.y);
                GL11.glVertex2f(right.x, right.y);
            }
            GL11.glEnd();
        }
    }

    protected static void renderReverseScoopFilaments(Vector2f from, Vector2f line, Vector2f normal,
                                                      float length, float alpha, float charge) {
        if (from == null || line == null || normal == null || length <= 0f || alpha <= 0f) {
            return;
        }

        for (int f = 0; f < 9; f++) {
            float phase = -elapsed * (2.1f + f * 0.07f) + f * 12.9898f;
            float side = (f - 4f) / 8f;
            float lineAlpha = alpha * (0.18f + 0.08f * (float) Math.sin(phase));
            GL11.glLineWidth(1.05f + (f % 3) * 0.28f);

            GL11.glBegin(GL11.GL_LINE_STRIP);
            for (int i = 0; i <= 34; i++) {
                float t = i / 34f;
                float width = getReverseScoopWidth(t, charge);
                float spiral = (float) Math.sin(t * 16.5f + phase) * width * 0.13f;
                float sideOffset = side * width * (0.52f + 0.18f * (float) Math.sin(t * 9f + phase));
                Vector2f point = pointOnLine(from, line, normal, length, t, sideOffset + spiral);
                setColor(f % 3 == 0 ? REVERSE_SCOOP_CORE : REVERSE_SCOOP_RIM, lineAlpha * endFade(t));
                GL11.glVertex2f(point.x, point.y);
            }
            GL11.glEnd();
        }

        for (int pass = 0; pass < 2; pass++) {
            float coreWidth = (8f + pass * 6f) * (0.70f + charge * 0.35f);
            float coreAlpha = alpha * (0.16f - pass * 0.050f);
            GL11.glBegin(GL11.GL_QUAD_STRIP);
            for (int i = 0; i <= 34; i++) {
                float t = i / 34f;
                float wave = (float) Math.sin(t * 13.6f - elapsed * 7.5f + pass) * coreWidth * 0.10f;
                float width = coreWidth * (0.55f + 0.45f * t) * endFade(t);
                Vector2f center = pointOnLine(from, line, normal, length, t, wave);
                Vector2f left = offset(center, normal, -width * 0.5f);
                Vector2f right = offset(center, normal, width * 0.5f);
                setColor(REVERSE_SCOOP_CORE, coreAlpha * endFade(t));
                GL11.glVertex2f(left.x, left.y);
                GL11.glVertex2f(right.x, right.y);
            }
            GL11.glEnd();
        }
    }

    protected static void renderReverseScoopMouth(Vector2f from, Vector2f line, Vector2f normal,
                                                  float alpha, float charge) {
        if (from == null || line == null || normal == null || alpha <= 0f) {
            return;
        }

        Vector2f center = offset(from, line, 26f + 22f * charge);
        float radius = 34f + 34f * charge;
        float pulse = 0.88f + 0.12f * (float) Math.sin(elapsed * 7.5f);

        renderGlow(center, radius * 2.0f, radius * 1.45f, REVERSE_SCOOP_RIM, alpha * 0.070f * pulse);
        renderGlow(offset(center, line, 14f + 10f * charge), radius * 0.95f, radius * 0.62f,
                REVERSE_SCOOP_CORE, alpha * 0.085f * pulse);

        GL11.glLineWidth(1.25f);
        setColor(REVERSE_SCOOP_RIM, alpha * 0.18f);
        drawArcLine(center.x, center.y, radius, elapsed * 0.58f, (float) Math.PI * 1.18f, 56);
        setColor(REVERSE_SCOOP_CORE, alpha * 0.11f);
        drawArcLine(center.x, center.y, radius * 0.62f, -elapsed * 0.72f + 1.2f, (float) Math.PI * 0.92f, 42);
    }

    protected static void renderLanceCore(Vector2f from, Vector2f to, Vector2f line, Vector2f normal,
                                          float length, float alpha, float charge) {
        SpriteAPI fringe = getBeamFringeSprite();
        SpriteAPI core = getBeamCoreSprite();
        if (fringe == null || core == null) {
            renderBeamCore(from, line, normal, length, alpha, charge);
            return;
        }

        float angle = (float) Math.toDegrees(Math.atan2(line.y, line.x));
        Vector2f center = new Vector2f((from.x + to.x) * 0.5f, (from.y + to.y) * 0.5f);
        float flicker = 0.94f + 0.06f * (float) Math.sin(elapsed * 13.0f);
        renderBeamSprite(fringe, center, angle, length, (13f + 5f * charge) * flicker,
                new Color(82, 205, 255, 120), alpha * 0.115f);
        renderBeamSprite(core, center, angle, length, (3.8f + 2.2f * charge) * flicker,
                new Color(248, 255, 255, 255), alpha * 0.95f);
        renderBeamSprite(core, center, angle, length, (1.4f + 0.8f * charge) * flicker,
                new Color(255, 255, 255, 255), alpha);
    }

    protected static void renderEmitterBloom(Vector2f loc, Vector2f line, Vector2f normal, float alpha, float charge) {
        float size = 28f + 24f * charge;
        renderGlow(loc, size * 1.7f, size * 1.7f, AURA, alpha * 0.050f);
        renderGlow(offset(loc, line, 18f + 14f * charge), size * 0.85f, size * 0.45f,
                SWIRL_WHITE, alpha * 0.065f);
    }

    protected static void renderLanceMuzzle(Vector2f loc, Vector2f line, Vector2f normal, float alpha, float charge) {
        Vector2f mouth = offset(loc, line, 34f + 16f * charge);
        renderGlow(mouth, 54f + 30f * charge, 18f + 8f * charge, SWIRL_WHITE, alpha * 0.070f);
    }

    protected static void renderSurfaceImpact(Vector2f impact, Vector2f targetCenter, Vector2f line, Vector2f normal,
                                              float alpha, float charge) {
        float targetRadius = STATE.target == null ? 120f : Math.max(90f, STATE.target.getRadius());
        float glow = 34f + targetRadius * 0.12f * charge;
        renderGlow(impact, glow * 2.4f, glow * 2.4f, IMPACT_ORANGE, alpha * 0.060f);
        renderGlow(impact, glow * 1.35f, glow * 1.35f, BEAM_MID, alpha * 0.055f);
        renderGlow(impact, glow * 0.55f, glow * 0.55f, IMPACT_WHITE, alpha * 0.125f);

        GL11.glLineWidth(1.0f);
        GL11.glBegin(GL11.GL_LINES);
        for (int i = 0; i < 7; i++) {
            float side = (i - 3f) / 3f;
            float angle = (float) Math.atan2(-line.y, -line.x) + side * (0.30f + 0.10f * charge);
            Vector2f dir = new Vector2f((float) Math.cos(angle), (float) Math.sin(angle));
            Vector2f start = offset(impact, dir, 8f + 5f * charge);
            Vector2f end = offset(impact, dir, 42f + targetRadius * 0.10f * charge);
            setColor(i == 3 ? IMPACT_WHITE : IMPACT_ORANGE, alpha * (0.055f + 0.035f * charge));
            GL11.glVertex2f(start.x, start.y);
            setColor(IMPACT_RED, 0f);
            GL11.glVertex2f(end.x, end.y);
        }
        GL11.glEnd();
    }

    protected static void renderTexturedBeam(Vector2f from, Vector2f to, Vector2f line, Vector2f normal,
                                             float length, float alpha, float charge) {
        SpriteAPI fringe = getBeamFringeSprite();
        SpriteAPI core = getBeamCoreSprite();
        if (fringe == null || core == null) {
            return;
        }

        float angle = (float) Math.toDegrees(Math.atan2(line.y, line.x));
        Vector2f center = new Vector2f((from.x + to.x) * 0.5f, (from.y + to.y) * 0.5f);
        float breathing = 0.96f + 0.04f * (float) Math.sin(elapsed * 11.5f);
        float coronaWidth = (42f + 34f * charge) * breathing;
        float fringeWidth = (18f + 18f * charge) * breathing;
        float coreWidth = (5.5f + 5.0f * charge) * breathing;

        renderSoftBeamRibbon(from, line, normal, length, 18f + 8f * charge, coronaWidth,
                DEEP_VIOLET, alpha * 0.060f, 28, elapsed * 0.70f);
        renderSoftBeamRibbon(from, line, normal, length, 10f + 5f * charge, fringeWidth,
                BEAM_MID, alpha * 0.115f, 30, -elapsed * 0.95f);
        renderBeamSprite(fringe, center, angle, length, fringeWidth * 0.74f,
                new Color(105, 226, 255, 145), alpha * 0.12f);
        renderBeamSprite(core, center, angle, length, coreWidth,
                new Color(246, 255, 255, 248), alpha * 0.92f);

        renderBeamPackets(center, line, normal, length, angle, alpha, charge);
    }

    protected static void renderBeamPackets(Vector2f center, Vector2f line, Vector2f normal,
                                            float length, float angle, float alpha, float charge) {
        SpriteAPI glow = getGlowSprite();
        if (glow == null) {
            return;
        }
        for (int i = 0; i < 6; i++) {
            float t = (elapsed * (0.62f + i * 0.03f) + i * 0.17f) % 1f;
            Vector2f point = new Vector2f(center);
            Vector2f along = new Vector2f(line);
            along.scale((t - 0.5f) * length);
            Vector2f.add(point, along, point);
            Vector2f side = new Vector2f(normal);
            side.scale((float) Math.sin(elapsed * 2.1f + i) * (1.5f + 3f * charge));
            Vector2f.add(point, side, point);
            float localAlpha = alpha * (0.045f + 0.075f * (float) Math.sin(t * Math.PI));
            renderBeamSprite(glow, point, angle, 34f + 26f * charge, 7f + 5f * charge,
                    new Color(210, 250, 255, 210), localAlpha);
        }
    }

    protected static void renderBeamVeil(Vector2f from, Vector2f line, Vector2f normal, float length,
                                         float alpha, float charge) {
        renderSoftBeamRibbon(from, line, normal, length, 44f + 18f * charge, 86f + 44f * charge,
                DEEP_VIOLET, alpha * 0.040f, 26, elapsed * 0.45f);
        renderSoftBeamRibbon(from, line, normal, length, 18f + 8f * charge, 50f + 26f * charge,
                GLASS_BLUE, alpha * 0.052f, 28, -elapsed * 0.72f);

        for (int pass = 0; pass < 2; pass++) {
            float phase = elapsed * (0.75f + pass * 0.14f) + pass * 1.73f;
            float sideBias = (pass - 1f) * 0.11f;
            GL11.glBegin(GL11.GL_QUAD_STRIP);
            for (int i = 0; i <= 28; i++) {
                float t = i / 28f;
                float width = getStrikeStreamWidth(t) * (0.22f + pass * 0.10f) * charge;
                float wave = (float) Math.sin(t * 6.2f + phase) * width * 0.045f
                        + (float) Math.sin(t * 15.0f - phase * 0.65f) * width * 0.018f;
                float centerBias = sideBias * width * (0.50f + 0.50f * t);
                float edgeFade = endFade(t);
                Vector2f center = pointOnLine(from, line, normal, length, t, wave + centerBias);
                Vector2f left = offset(center, normal, -width * 0.5f);
                Vector2f right = offset(center, normal, width * 0.5f);
                setColor(pass == 0 ? BEAM_MID : BEAM_FRINGE, alpha * (0.014f - pass * 0.003f) * edgeFade);
                GL11.glVertex2f(left.x, left.y);
                GL11.glVertex2f(right.x, right.y);
            }
            GL11.glEnd();
        }
    }

    protected static void renderBeamCore(Vector2f from, Vector2f line, Vector2f normal, float length,
                                         float alpha, float charge) {
        for (int pass = 0; pass < 2; pass++) {
            float coreWidth = (4.5f + pass * 7.5f) * charge;
            float passAlpha = alpha * (0.30f - pass * 0.105f) * charge;
            GL11.glBegin(GL11.GL_QUAD_STRIP);
            for (int i = 0; i <= 34; i++) {
                float t = i / 34f;
                float wave = (float) Math.sin(t * 18.6f - elapsed * 8.5f + pass) * coreWidth * 0.035f;
                float width = coreWidth * (0.85f + 0.20f * t) * endFade(t);
                Vector2f center = pointOnLine(from, line, normal, length, t, wave);
                Vector2f left = offset(center, normal, -width * 0.5f);
                Vector2f right = offset(center, normal, width * 0.5f);
                setColor(pass == 0 ? SWIRL_WHITE : BEAM_MID, passAlpha * endFade(t));
                GL11.glVertex2f(left.x, left.y);
                GL11.glVertex2f(right.x, right.y);
            }
            GL11.glEnd();
        }
    }

    protected static void renderBeamFilaments(Vector2f from, Vector2f line, Vector2f normal, float length,
                                              float alpha, float charge) {
        for (int f = 0; f < 7; f++) {
            float side = (f - 3f) / 3f;
            float phase = elapsed * (1.65f + f * 0.05f) + f * 1.91f;
            GL11.glLineWidth(0.75f + (f % 3) * 0.22f);
            GL11.glBegin(GL11.GL_LINE_STRIP);
            for (int i = 0; i <= 36; i++) {
                float t = i / 36f;
                float width = getStrikeStreamWidth(t) * charge * 0.32f;
                float spiral = (float) Math.sin(t * 14.5f + phase) * width * 0.055f;
                float sideOffset = side * width * (0.72f + 0.12f * (float) Math.sin(t * 9f + phase));
                float edgeFade = endFade(t);
                Vector2f point = pointOnLine(from, line, normal, length, t, sideOffset + spiral);
                setColor(f % 3 == 0 ? SWIRL_WHITE : SWIRL_BLUE, alpha * (0.032f + 0.012f * (float) Math.sin(phase)) * edgeFade);
                GL11.glVertex2f(point.x, point.y);
            }
            GL11.glEnd();
        }
    }

    protected static void renderFleetAura(Vector2f loc, float alpha, float charge) {
        float radius = 42f + 34f * charge + 4f * (float) Math.sin(elapsed * 5.5f);
        renderGlow(loc, radius * 1.80f, radius * 1.80f, AURA, alpha * 0.055f);
        renderGlow(loc, radius * 0.58f, radius * 0.58f, SWIRL_WHITE, alpha * 0.060f);
        GL11.glLineWidth(1.05f);
        setColor(AURA, alpha * 0.12f);
        drawArcLine(loc.x, loc.y, radius, elapsed * 0.55f, (float) Math.PI * 0.82f, 48);
        setColor(BEAM_CORE, alpha * 0.060f);
        drawArcLine(loc.x, loc.y, radius * 1.38f, -elapsed * 0.45f + 0.8f, (float) Math.PI * 0.55f, 38);
    }

    protected static void renderFleetAperture(Vector2f loc, Vector2f line, Vector2f normal, float alpha, float charge) {
        float radius = 46f + 34f * charge;
        float phase = elapsed * 1.35f;
        GL11.glLineWidth(1.15f);
        for (int ring = 0; ring < 3; ring++) {
            float r = radius * (0.50f + ring * 0.33f);
            setColor(ring == 0 ? SWIRL_WHITE : SWIRL_BLUE, alpha * (0.18f - ring * 0.045f));
            drawArcLine(loc.x, loc.y, r, phase + ring * 2.05f, (float) Math.PI * (0.78f + ring * 0.12f), 46);
        }

        GL11.glLineWidth(0.95f);
        GL11.glBegin(GL11.GL_LINES);
        for (int i = 0; i < 5; i++) {
            float angle = phase * 0.22f + i * (float) Math.PI * 2f / 5f;
            Vector2f dir = new Vector2f((float) Math.cos(angle), (float) Math.sin(angle));
            Vector2f inner = offset(loc, dir, radius * 0.55f);
            Vector2f outer = offset(loc, dir, radius * 0.96f);
            setColor(SWIRL_BLUE, alpha * 0.032f);
            GL11.glVertex2f(inner.x, inner.y);
            setColor(SWIRL_WHITE, 0f);
            GL11.glVertex2f(outer.x, outer.y);
        }
        GL11.glEnd();

        Vector2f focus = offset(loc, line, 42f + 26f * charge);
        renderGlow(focus, 46f + 26f * charge, 22f + 12f * charge, SWIRL_WHITE, alpha * 0.095f);
    }

    protected static void renderBeamMouth(Vector2f loc, Vector2f line, Vector2f normal, float alpha, float charge) {
        Vector2f mouth = offset(loc, line, 64f + 38f * charge);
        float angle = (float) Math.toDegrees(Math.atan2(line.y, line.x));
        renderGlow(mouth, 76f + 54f * charge, 34f + 18f * charge, SWIRL_WHITE, alpha * 0.105f);
        renderGlow(mouth, 150f + 92f * charge, 72f + 36f * charge, SWIRL_BLUE, alpha * 0.055f);

        SpriteAPI fringe = getBeamFringeSprite();
        if (fringe != null) {
            for (int i = -1; i <= 1; i++) {
                Vector2f base = offset(mouth, normal, i * (9f + 12f * charge));
                renderBeamSprite(fringe, offset(base, line, 28f + 18f * charge), angle,
                        74f + 46f * charge, 8f + 8f * charge,
                        i == 0 ? SWIRL_WHITE : SWIRL_BLUE, alpha * (i == 0 ? 0.18f : 0.075f));
            }
        }
    }

    protected static void renderImpactHalo(Vector2f loc, float alpha, float charge) {
        float radius = Math.max(62f, STATE.target.getRadius() * 0.34f + 34f) * charge;
        renderGlow(loc, radius * 2.8f, radius * 2.8f, IMPACT_ORANGE, alpha * 0.055f);
        renderGlow(loc, radius * 1.45f, radius * 1.45f, SWIRL_BLUE, alpha * 0.055f);
        renderGlow(loc, radius * 0.55f, radius * 0.55f, IMPACT_WHITE, alpha * 0.115f);

        GL11.glLineWidth(1.05f);
        setColor(IMPACT, alpha * 0.16f);
        drawArcLine(loc.x, loc.y, radius * 1.05f, elapsed * 0.38f, (float) Math.PI * 1.72f, 116);
        setColor(BEAM_CORE, alpha * 0.12f);
        drawArcLine(loc.x, loc.y, radius * 0.55f, -elapsed * 0.62f + 1.8f, (float) Math.PI * 1.18f, 86);
    }

    protected static void renderPlanetSurfaceScars(Vector2f loc, Vector2f line, Vector2f normal, float alpha, float charge) {
        float planetRadius = Math.max(80f, STATE.target.getRadius());
        float entryAngle = (float) Math.atan2(-line.y, -line.x);
        float hotArcRadius = planetRadius * (0.82f + 0.05f * (float) Math.sin(elapsed * 4f));

        GL11.glLineWidth(1.55f);
        setColor(IMPACT_ORANGE, alpha * 0.22f);
        drawArcLine(loc.x, loc.y, hotArcRadius, entryAngle - 0.42f, 0.84f, 52);
        GL11.glLineWidth(0.95f);
        setColor(IMPACT_WHITE, alpha * 0.14f);
        drawArcLine(loc.x, loc.y, hotArcRadius * 0.92f, entryAngle - 0.25f, 0.50f, 38);

        GL11.glBegin(GL11.GL_LINES);
        for (int i = 0; i < 13; i++) {
            float side = (i - 6f) / 6f;
            float phase = elapsed * (1.35f + i * 0.06f) + i * 1.31f;
            float angle = entryAngle + side * (0.26f + 0.16f * charge);
            Vector2f dir = new Vector2f((float) Math.cos(angle), (float) Math.sin(angle));
            float start = planetRadius * (0.07f + 0.08f * (float) Math.sin(phase));
            float end = planetRadius * (0.34f + 0.23f * charge + 0.06f * (float) Math.sin(phase * 0.7f));
            Vector2f a = offset(loc, dir, start);
            Vector2f b = offset(loc, dir, end);
            setColor(i % 4 == 0 ? IMPACT_WHITE : IMPACT_ORANGE, alpha * (0.060f + 0.050f * charge));
            GL11.glVertex2f(a.x, a.y);
            setColor(IMPACT_RED, 0f);
            GL11.glVertex2f(b.x, b.y);
        }
        GL11.glEnd();

        for (int i = 0; i < 5; i++) {
            float phase = elapsed * (1.1f + i * 0.12f) + i * 2.0f;
            float angle = entryAngle + (float) Math.sin(phase) * 0.20f;
            Vector2f dir = new Vector2f((float) Math.cos(angle), (float) Math.sin(angle));
            Vector2f center = offset(loc, dir, planetRadius * (0.30f + 0.15f * (float) Math.sin(phase * 0.7f)));
            renderGlow(center, 22f + 26f * charge, 18f + 20f * charge,
                    i % 2 == 0 ? IMPACT_WHITE : IMPACT_ORANGE, alpha * (0.020f + 0.025f * charge));
        }
    }

    protected static void renderImpactColumn(Vector2f loc, Vector2f line, Vector2f normal, float alpha, float charge) {
        float targetRadius = Math.max(130f, STATE.target.getRadius());
        float columnLength = targetRadius * 0.26f + 98f * charge;
        float columnWidth = 7f + 15f * charge;
        Vector2f cap = offset(loc, line, -columnLength * 0.22f);
        Vector2f far = offset(loc, line, columnLength);

        SpriteAPI fringe = getBeamFringeSprite();
        SpriteAPI core = getBeamCoreSprite();
        if (fringe != null) {
            renderSoftBeamRibbon(cap, line, normal, columnLength * 1.22f, columnWidth * 0.8f, columnWidth * 3.4f,
                    IMPACT_ORANGE, alpha * 0.080f, 18, elapsed * 1.5f);
            renderSoftBeamRibbon(cap, line, normal, columnLength * 1.22f, columnWidth * 0.5f, columnWidth * 2.0f,
                    BEAM_MID, alpha * 0.070f, 18, -elapsed * 1.8f);
        }
        if (core != null) {
            float angle = (float) Math.toDegrees(Math.atan2(line.y, line.x));
            renderBeamSprite(core, offset(loc, line, columnLength * 0.32f), angle,
                    columnLength * 1.12f, 4.5f + 5.5f * charge,
                    IMPACT_WHITE, alpha * 0.30f);
        }

        renderGlow(loc, targetRadius * 0.42f, targetRadius * 0.42f, IMPACT_WHITE, alpha * 0.060f);
        renderGlow(far, columnWidth * 4.5f, columnWidth * 2.5f, IMPACT_ORANGE, alpha * 0.032f);
    }

    protected static void renderImpactShockwaves(Vector2f loc, float alpha, float charge) {
        float base = Math.max(44f, STATE.target.getRadius() * 0.32f + 36f);
        for (int ring = 0; ring < 2; ring++) {
            float cycle = (elapsed * (0.30f + ring * 0.025f) + ring * 0.34f) % 1f;
            float radius = base * (0.62f + ring * 0.24f) + cycle * (58f + 34f * charge);
            float a = alpha * (1f - cycle) * (0.070f - ring * 0.018f);
            GL11.glLineWidth(1.05f - ring * 0.20f);
            setColor(ring % 2 == 0 ? IMPACT_ORANGE : SWIRL_WHITE, a);
            drawArcLine(loc.x, loc.y, radius, elapsed * 0.32f + ring * 2.2f, (float) Math.PI * 1.55f, 92);
        }
    }

    protected static void renderTargetFirestorm(Vector2f loc, Vector2f line, Vector2f normal, float alpha, float charge) {
        float radius = Math.max(150f, STATE.target.getRadius() + 90f);
        for (int i = 0; i < 6; i++) {
            float angle = i * (float) Math.PI * 2f / 6f + elapsed * (0.08f + (i % 3) * 0.015f);
            float len = radius * (0.12f + 0.22f * charge) * (0.85f + 0.25f * (float) Math.sin(elapsed * 3.7f + i * 1.9f));
            float baseWidth = 5f + 9f * charge + (i % 3) * 3f;
            Vector2f dir = new Vector2f((float) Math.cos(angle), (float) Math.sin(angle));
            Vector2f tangent = new Vector2f(-dir.y, dir.x);
            Vector2f base = offset(loc, dir, radius * 0.12f);
            Vector2f tip = offset(loc, dir, radius * (0.20f + 0.08f * charge) + len);
            Vector2f left = offset(base, tangent, -baseWidth);
            Vector2f right = offset(base, tangent, baseWidth);
            GL11.glBegin(GL11.GL_TRIANGLES);
            setColor(IMPACT_WHITE, alpha * 0.070f);
            GL11.glVertex2f(base.x, base.y);
            setColor(i % 2 == 0 ? IMPACT_ORANGE : IMPACT_RED, alpha * 0.045f);
            GL11.glVertex2f(left.x, left.y);
            setColor(IMPACT_RED, 0f);
            GL11.glVertex2f(tip.x, tip.y);

            setColor(IMPACT_WHITE, alpha * 0.055f);
            GL11.glVertex2f(base.x, base.y);
            setColor(IMPACT_RED, 0f);
            GL11.glVertex2f(tip.x, tip.y);
            setColor(i % 2 == 0 ? IMPACT_ORANGE : IMPACT_RED, alpha * 0.045f);
            GL11.glVertex2f(right.x, right.y);
            GL11.glEnd();
        }
    }

    protected static Vector2f getTargetPoint() {
        return STATE.target == null ? new Vector2f() : new Vector2f(STATE.target.getLocation());
    }

    protected static Vector2f pointOnLine(Vector2f from, Vector2f line, Vector2f normal, float length, float t, float side) {
        Vector2f result = new Vector2f(from);
        Vector2f along = new Vector2f(line);
        along.scale(length * t);
        Vector2f.add(result, along, result);
        Vector2f off = new Vector2f(normal);
        off.scale(side);
        Vector2f.add(result, off, result);
        return result;
    }

    protected static Vector2f offset(Vector2f point, Vector2f dir, float amount) {
        Vector2f result = new Vector2f(point);
        Vector2f off = new Vector2f(dir);
        off.scale(amount);
        Vector2f.add(result, off, result);
        return result;
    }

    protected static Vector2f randomAround(Vector2f center, float radius) {
        float angle = RANDOM.nextFloat() * (float) Math.PI * 2f;
        float dist = RANDOM.nextFloat() * radius;
        return new Vector2f(center.x + (float) Math.cos(angle) * dist, center.y + (float) Math.sin(angle) * dist);
    }

    protected static Vector2f randomVelocity(float min, float max) {
        float angle = RANDOM.nextFloat() * (float) Math.PI * 2f;
        float speed = min + RANDOM.nextFloat() * Math.max(0f, max - min);
        return new Vector2f((float) Math.cos(angle) * speed, (float) Math.sin(angle) * speed);
    }

    protected static float endFade(float t) {
        float in = Math.min(1f, t / 0.08f);
        float out = Math.min(1f, (1f - t) / 0.08f);
        return Math.max(0f, Math.min(1f, in * out));
    }

    protected static float getStrikeStreamWidth(float t) {
        float narrow = 20f;
        float wide = 135f;
        float curved = t;
        curved = curved * curved * (3f - 2f * curved);
        return narrow + wide * curved;
    }

    protected static float getPathWakeWidth(float t, float charge) {
        float rise = smoothstep(clamp01(t / 0.42f));
        float fall = 1f - smoothstep(clamp01((t - 0.78f) / 0.22f));
        float body = rise * fall;
        return (14f + 10f * charge) + (42f + 34f * charge) * body;
    }

    protected static float getReverseScoopWidth(float t, float charge) {
        float narrow = 24f + 8f * charge;
        float wide = 145f + 55f * charge;
        float curved = 1f - t;
        curved = curved * curved * (3f - 2f * curved);
        return (narrow + wide * curved) * endFade(t);
    }

    protected static float getWarmupCharge() {
        return smoothstep(clamp01(STATE.progress / 0.28f));
    }

    protected static float getBeamCharge() {
        return smoothstep(clamp01((STATE.progress - 0.16f) / 0.42f));
    }

    protected static float getImpactCharge() {
        return smoothstep(clamp01((STATE.progress - 0.42f) / 0.46f));
    }

    protected static float smoothstep(float x) {
        x = clamp01(x);
        return x * x * (3f - 2f * x);
    }

    protected static float clamp01(float x) {
        return Math.max(0f, Math.min(1f, x));
    }

    protected static void drawCornerBox(Vector2f loc, float size, float alpha, Color color) {
        float half = size * 0.5f;
        float corner = size * 0.22f;
        setColor(color, alpha);
        GL11.glBegin(GL11.GL_LINES);
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sy = -1; sy <= 1; sy += 2) {
                float x = loc.x + sx * half;
                float y = loc.y + sy * half;
                GL11.glVertex2f(x, y);
                GL11.glVertex2f(x - sx * corner, y);
                GL11.glVertex2f(x, y);
                GL11.glVertex2f(x, y - sy * corner);
            }
        }
        GL11.glEnd();
    }

    protected static void drawCircleLine(float cx, float cy, float radius, int segments) {
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i < segments; i++) {
            float angle = (float) (Math.PI * 2.0 * i / segments);
            GL11.glVertex2f(cx + (float) Math.cos(angle) * radius, cy + (float) Math.sin(angle) * radius);
        }
        GL11.glEnd();
    }

    protected static void drawArcLine(float cx, float cy, float radius, float startAngle, float arc, int segments) {
        if (radius <= 0f || segments < 2) {
            return;
        }
        GL11.glBegin(GL11.GL_LINE_STRIP);
        for (int i = 0; i <= segments; i++) {
            float t = i / (float) segments;
            float angle = startAngle + arc * t;
            GL11.glVertex2f(cx + (float) Math.cos(angle) * radius, cy + (float) Math.sin(angle) * radius);
        }
        GL11.glEnd();
    }

    protected static void drawFilledDisc(float cx, float cy, float radius, Color color, float alpha, int segments) {
        if (radius <= 0f || segments < 8 || alpha <= 0f) {
            return;
        }
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        setColor(color, alpha);
        GL11.glVertex2f(cx, cy);
        setColor(color, 0f);
        for (int i = 0; i <= segments; i++) {
            float angle = (float) (Math.PI * 2.0 * i / segments);
            GL11.glVertex2f(cx + (float) Math.cos(angle) * radius, cy + (float) Math.sin(angle) * radius);
        }
        GL11.glEnd();
    }

    protected static void renderSoftBeamRibbon(Vector2f from, Vector2f line, Vector2f normal, float length,
                                               float startWidth, float endWidth, Color color, float alpha,
                                               int segments, float phase) {
        if (from == null || line == null || normal == null || length <= 0f || alpha <= 0f || segments < 2) {
            return;
        }

        for (int sideSign = -1; sideSign <= 1; sideSign += 2) {
            GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
            for (int i = 0; i <= segments; i++) {
                float t = i / (float) segments;
                float width = startWidth + (endWidth - startWidth) * smoothstep(t);
                float wave = (float) Math.sin(t * 8.0f + phase) * width * 0.025f
                        + (float) Math.sin(t * 19.0f - phase * 0.7f) * width * 0.010f;
                Vector2f center = pointOnLine(from, line, normal, length, t, wave);
                Vector2f edge = offset(center, normal, sideSign * width * 0.5f);
                float localAlpha = alpha * endFade(t) * (0.86f + 0.14f * (float) Math.sin(phase + t * 5.0f));

                setColor(color, localAlpha);
                GL11.glVertex2f(center.x, center.y);
                setColor(color, 0f);
                GL11.glVertex2f(edge.x, edge.y);
            }
            GL11.glEnd();
        }
    }

    protected static void renderGlow(Vector2f center, float width, float height, Color color, float alpha) {
        SpriteAPI glow = getGlowSprite();
        if (glow == null || center == null) {
            return;
        }
        renderBeamSprite(glow, center, 0f, width, height, color, alpha);
    }

    protected static void renderBeamSprite(SpriteAPI sprite, Vector2f center, float angle, float length,
                                           float width, Color color, float alpha) {
        if (sprite == null || center == null || length <= 0f || width <= 0f || alpha <= 0f) {
            return;
        }
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        sprite.setAdditiveBlend();
        sprite.setAngle(angle);
        sprite.setSize(length, width);
        sprite.setColor(color);
        sprite.setAlphaMult(Math.max(0f, Math.min(1f, alpha)));
        sprite.renderAtCenter(center.x, center.y);
        sprite.setAlphaMult(1f);
        sprite.setAngle(0f);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
    }

    protected static SpriteAPI getBeamCoreSprite() {
        if (beamCoreSprite == null) {
            beamCoreSprite = Global.getSettings().getSprite("graphics/fx/beam_laser_core.png");
        }
        return beamCoreSprite;
    }

    protected static SpriteAPI getBeamFringeSprite() {
        if (beamFringeSprite == null) {
            beamFringeSprite = Global.getSettings().getSprite("graphics/fx/beam_laser_fringe.png");
        }
        return beamFringeSprite;
    }

    protected static SpriteAPI getGlowSprite() {
        if (glowSprite == null) {
            glowSprite = Global.getSettings().getSprite("graphics/fx/hit_glow.png");
        }
        return glowSprite;
    }

    protected static SpriteAPI getNoiseSprite() {
        if (noiseSprite == null) {
            noiseSprite = Global.getSettings().getSprite("graphics/fx/noise.png");
        }
        return noiseSprite;
    }

    protected static SpriteAPI getStreakSprite() {
        if (streakSprite == null) {
            streakSprite = Global.getSettings().getSprite("graphics/fx/beam_rough2_fringe.png");
        }
        return streakSprite;
    }

    protected static void bindShaderNoiseTexture() {
        SpriteAPI noise = getNoiseSprite();
        SpriteAPI streak = getStreakSprite();
        if (noise != null) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, noise.getTextureId());
        }
        if (streak != null) {
            GL13.glActiveTexture(GL13.GL_TEXTURE1);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, streak.getTextureId());
        }
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
    }

    protected static void unbindShaderNoiseTexture() {
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
    }

    protected static boolean ensureShader() {
        if (shaderDisabled) {
            return false;
        }
        if (shaderProgram != 0) {
            return true;
        }
        if (shaderInitAttempted) {
            return false;
        }
        shaderInitAttempted = true;

        try {
            if (!GLContext.getCapabilities().OpenGL20) {
                shaderDisabled = true;
                return false;
            }

            String vert = Global.getSettings().loadText(SHADER_VERT);
            String frag = Global.getSettings().loadText(SHADER_FRAG);
            shaderProgram = createShaderProgram(vert, frag);
            if (shaderProgram == 0) {
                shaderDisabled = true;
                return false;
            }

            GL20.glUseProgram(shaderProgram);
            uType = GL20.glGetUniformLocation(shaderProgram, "u_type");
            uTime = GL20.glGetUniformLocation(shaderProgram, "u_time");
            uAlpha = GL20.glGetUniformLocation(shaderProgram, "u_alpha");
            uCharge = GL20.glGetUniformLocation(shaderProgram, "u_charge");
            uProgress = GL20.glGetUniformLocation(shaderProgram, "u_progress");
            uColorA = GL20.glGetUniformLocation(shaderProgram, "u_colorA");
            uColorB = GL20.glGetUniformLocation(shaderProgram, "u_colorB");
            uParams = GL20.glGetUniformLocation(shaderProgram, "u_params");
            uNoiseTex = GL20.glGetUniformLocation(shaderProgram, "u_noiseTex");
            uStreakTex = GL20.glGetUniformLocation(shaderProgram, "u_streakTex");
            uQuad = GL20.glGetUniformLocation(shaderProgram, "u_quad");
            setUniform1i(uNoiseTex, 0);
            setUniform1i(uStreakTex, 1);
            GL20.glUseProgram(0);
            return true;
        } catch (Throwable ex) {
            Global.getLogger(PCTianhongVisualRenderer.class).log(Level.ERROR,
                    "Tianhong shader disabled: failed to initialize.", ex);
            shaderProgram = 0;
            shaderDisabled = true;
            GL20.glUseProgram(0);
            return false;
        }
    }

    protected static void renderShaderBeam(Vector2f from, Vector2f line, float length, float width,
                                           float alpha, float charge, float progress,
                                           Color colorA, Color colorB,
                                           float coreWidth, float hotWidth, float auraWidth, float flame) {
        if (shaderProgram == 0 || from == null || line == null || length <= 0f || width <= 0f || alpha <= 0f) {
            return;
        }

        float angle = (float) Math.toDegrees(Math.atan2(line.y, line.x));
        GL20.glUseProgram(shaderProgram);
        setShaderCommon(0f, alpha, charge, progress, colorA, colorB, coreWidth, hotWidth, auraWidth, flame);
        bindShaderNoiseTexture();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glPushMatrix();
        GL11.glTranslatef(from.x, from.y, 0f);
        GL11.glRotatef(angle, 0f, 0f, 1f);
        renderShaderQuad(0f, -width * 0.5f, length, width);
        GL11.glPopMatrix();
        unbindShaderNoiseTexture();
        GL20.glUseProgram(0);
    }

    protected static void renderShaderRing(Vector2f center, float radius, float alpha, float charge, float progress,
                                           Color colorA, Color colorB) {
        if (shaderProgram == 0 || center == null || radius <= 0f || alpha <= 0f) {
            return;
        }

        GL20.glUseProgram(shaderProgram);
        setShaderCommon(1f, alpha, charge, progress, colorA, colorB, 0f, 0f, 0f, 0f);
        bindShaderNoiseTexture();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glPushMatrix();
        GL11.glTranslatef(center.x, center.y, 0f);
        renderShaderQuad(-radius, -radius, radius * 2f, radius * 2f);
        GL11.glPopMatrix();
        unbindShaderNoiseTexture();
        GL20.glUseProgram(0);
    }

    protected static void renderShaderImpact(Vector2f impact, Vector2f line, float radius,
                                             float alpha, float charge, float progress) {
        if (shaderProgram == 0 || impact == null || line == null || radius <= 0f || alpha <= 0f) {
            return;
        }

        float back = radius * (0.22f + 0.04f * charge);
        float front = radius * (0.62f + 0.10f * charge);
        float width = radius * (0.72f + 0.08f * charge);
        float total = back + front;
        float angle = (float) Math.toDegrees(Math.atan2(line.y, line.x));

        GL20.glUseProgram(shaderProgram);
        setShaderCommon(2f, alpha, charge, progress, IMPACT_WHITE, new Color(255, 126, 46, 220),
                back / Math.max(1f, total), total / Math.max(1f, width), 0f, 0f);
        bindShaderNoiseTexture();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glPushMatrix();
        GL11.glTranslatef(impact.x, impact.y, 0f);
        GL11.glRotatef(angle, 0f, 0f, 1f);
        renderShaderQuad(-back, -width * 0.5f, total, width);
        GL11.glPopMatrix();
        unbindShaderNoiseTexture();
        GL20.glUseProgram(0);
    }

    protected static void renderImpactFlare(Vector2f impact, Vector2f line, Vector2f normal, float alpha, float charge) {
        if (impact == null || line == null || normal == null || alpha <= 0f) {
            return;
        }

        float targetRadius = STATE.target == null ? 120f : Math.max(90f, STATE.target.getRadius());
        float flare = 18f + targetRadius * (0.035f + 0.045f * charge);
        float pulse = 0.90f + 0.10f * (float) Math.sin(elapsed * 18.0f);
        Vector2f entryGlow = offset(impact, line, -flare * 0.22f);
        Vector2f surfaceGlow = offset(impact, line, flare * 0.06f);
        Vector2f contact = offset(impact, line, -flare * 0.03f);

        renderGlow(entryGlow, flare * 2.1f, flare * 2.1f,
                new Color(118, 218, 255, 145), alpha * 0.034f * pulse);
        renderGlow(surfaceGlow, flare * 1.34f, flare * 1.34f,
                IMPACT_WHITE, alpha * 0.080f * pulse);
        renderGlow(contact, flare * 0.58f, flare * 0.58f,
                new Color(255, 246, 204, 235), alpha * 0.130f * pulse);
    }

    protected static void setShaderCommon(float type, float alpha, float charge, float progress,
                                          Color colorA, Color colorB,
                                          float p0, float p1, float p2, float p3) {
        setUniform1f(uType, type);
        setUniform1f(uTime, elapsed);
        setUniform1f(uAlpha, clamp01(alpha));
        setUniform1f(uCharge, clamp01(charge));
        setUniform1f(uProgress, clamp01(progress));
        setUniformColor(uColorA, colorA);
        setUniformColor(uColorB, colorB);
        setUniform4f(uParams, p0, p1, p2, p3);
    }

    protected static void renderShaderQuad(float x, float y, float w, float h) {
        setUniform4f(uQuad, x, y, w, h);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0f, 0f);
        GL11.glVertex2f(x, y);
        GL11.glTexCoord2f(1f, 0f);
        GL11.glVertex2f(x + w, y);
        GL11.glTexCoord2f(1f, 1f);
        GL11.glVertex2f(x + w, y + h);
        GL11.glTexCoord2f(0f, 1f);
        GL11.glVertex2f(x, y + h);
        GL11.glEnd();
    }

    protected static void setUniform1f(int location, float value) {
        if (location >= 0) {
            GL20.glUniform1f(location, value);
        }
    }

    protected static void setUniform1i(int location, int value) {
        if (location >= 0) {
            GL20.glUniform1i(location, value);
        }
    }

    protected static void setUniform4f(int location, float x, float y, float z, float w) {
        if (location >= 0) {
            GL20.glUniform4f(location, x, y, z, w);
        }
    }

    protected static void setUniformColor(int location, Color color) {
        if (location >= 0 && color != null) {
            GL20.glUniform4f(location,
                    color.getRed() / 255f,
                    color.getGreen() / 255f,
                    color.getBlue() / 255f,
                    color.getAlpha() / 255f);
        }
    }

    protected static int createShaderProgram(String vertSource, String fragSource) {
        int vert = createShader(vertSource, GL20.GL_VERTEX_SHADER);
        int frag = createShader(fragSource, GL20.GL_FRAGMENT_SHADER);
        if (vert == 0 || frag == 0) {
            return 0;
        }

        int program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vert);
        GL20.glAttachShader(program, frag);
        GL20.glLinkProgram(program);

        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            Global.getLogger(PCTianhongVisualRenderer.class).log(Level.ERROR,
                    "Tianhong shader link failed:\n" +
                            GL20.glGetProgramInfoLog(program, GL20.glGetProgrami(program, GL20.GL_INFO_LOG_LENGTH)));
            GL20.glDeleteProgram(program);
            GL20.glDeleteShader(vert);
            GL20.glDeleteShader(frag);
            return 0;
        }

        GL20.glDetachShader(program, vert);
        GL20.glDetachShader(program, frag);
        GL20.glDeleteShader(vert);
        GL20.glDeleteShader(frag);
        return program;
    }

    protected static int createShader(String source, int type) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            Global.getLogger(PCTianhongVisualRenderer.class).log(Level.ERROR,
                    "Tianhong shader compile failed:\n" +
                            GL20.glGetShaderInfoLog(shader, GL20.glGetShaderi(shader, GL20.GL_INFO_LOG_LENGTH)));
            GL20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    protected static void setColor(Color color, float alphaMult) {
        int alpha = Math.max(0, Math.min(255, (int) (color.getAlpha() * alphaMult)));
        GL11.glColor4ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue(), (byte) alpha);
    }

    protected PCTianhongVisualRenderer() {
    }
}
