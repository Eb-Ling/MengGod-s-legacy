package data.example;

import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

/**
 * “燃料勺”专长的战役层连续虹吸光带渲染器。
 *
 * 纯静态渲染工具
 * 实际启停和目标数据由 PCFuelScoopScript 每帧推送，渲染入口由专长能力插件提供。
 */
public class PCFuelScoopVisualRenderer {

    protected static final float FADE_IN_PER_SECOND = 4.5f;
    protected static final float FADE_OUT_PER_SECOND = 3.2f;
    protected static final float MAX_RENDER_RADIUS = 1800f;

    protected static final Color VEIL_COLOR = new Color(255, 118, 62, 92);
    protected static final Color FRINGE_COLOR = new Color(255, 72, 45, 62);
    protected static final Color CORE_COLOR = new Color(255, 224, 146, 210);
    protected static final Color RIM_COLOR = new Color(255, 188, 86, 145);

    protected static final VisualState STATE = new VisualState();

    protected static float fade = 0f;
    protected static float elapsed = 0f;

    public static class VisualState {
        public boolean active = false;
        public LocationAPI location = null;
        public CampaignFleetAPI fleet = null;
        public PlanetAPI star = null;
        public float fuelPerDay = 0f;
        public float surfaceDistance = 0f;
        public float lastUpdateElapsed = 0f;
    }

    public static void activate(LocationAPI location, CampaignFleetAPI fleet, PlanetAPI star, float fuelPerDay, float surfaceDistance) {
        STATE.active = location != null && fleet != null && star != null;
        STATE.location = location;
        STATE.fleet = fleet;
        STATE.star = star;
        STATE.fuelPerDay = fuelPerDay;
        STATE.surfaceDistance = surfaceDistance;
        STATE.lastUpdateElapsed = 0f;
    }

    public static void deactivate() {
        STATE.active = false;
        STATE.location = null;
        STATE.fleet = null;
        STATE.star = null;
        STATE.fuelPerDay = 0f;
        STATE.surfaceDistance = 0f;
        STATE.lastUpdateElapsed = 0f;
    }

    public static void advance(float amount) {
        elapsed += amount;

        if (STATE.lastUpdateElapsed < 999f) {
            STATE.lastUpdateElapsed += amount;
        }

        boolean shouldShow = isStateRenderable();
        if (shouldShow) {
            fade = Math.min(1f, fade + amount * FADE_IN_PER_SECOND);
        } else {
            fade = Math.max(0f, fade - amount * FADE_OUT_PER_SECOND);
        }
    }

    public static boolean isVisible() {
        return fade > 0.01f;
    }

    public static float getRenderRange() {
        return MAX_RENDER_RADIUS;
    }

    public static void render(CampaignEngineLayers layer, ViewportAPI viewport) {
        if (fade <= 0.01f || viewport == null || !isStateRenderable()) {
            return;
        }

        Vector2f from = getSurfacePoint(STATE.star, STATE.fleet.getLocation());
        Vector2f to = new Vector2f(STATE.fleet.getLocation());
        Vector2f line = Vector2f.sub(to, from, new Vector2f());
        float length = line.length();
        if (length <= 8f) {
            return;
        }

        if (!viewport.isNearViewport(to, length + 400f) && !viewport.isNearViewport(from, length + 400f)) {
            return;
        }

        line.normalise();
        Vector2f normal = new Vector2f(-line.y, line.x);

        float alphaMult = viewport.getAlphaMult() * fade;
        float intensity = getIntensity();
        float pulse = 0.86f + 0.14f * (float) Math.sin(elapsed * 5.2f);
        alphaMult *= pulse;

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

            if (layer == CampaignEngineLayers.TERRAIN_8) {
                renderBroadVeil(from, line, normal, length, alphaMult, intensity);
            } else if (layer == CampaignEngineLayers.TERRAIN_9) {
                renderFringeFilaments(from, line, normal, length, alphaMult, intensity);
                renderCoreBeam(from, line, normal, length, alphaMult, intensity);
            } else if (layer == CampaignEngineLayers.ABOVE) {
                renderIntakeTongues(to, line, normal, alphaMult, intensity);
                renderSolarMouth(from, line, normal, alphaMult, intensity);
            }
        } finally {
            GL11.glLineWidth(1f);
            GL11.glDisable(GL11.GL_LINE_SMOOTH);
            GL11.glPopAttrib();
        }
    }

    protected static boolean isStateRenderable() {
        if (!STATE.active || STATE.location == null || STATE.fleet == null || STATE.star == null) {
            return false;
        }
        if (!STATE.location.isCurrentLocation()) {
            return false;
        }
        if (STATE.lastUpdateElapsed > 0.35f) {
            return false;
        }
        if (STATE.fleet.getContainingLocation() != STATE.location || STATE.star.getContainingLocation() != STATE.location) {
            return false;
        }
        return STATE.fuelPerDay > 0f;
    }

    protected static float getIntensity() {
        float fuelIntensity = Math.max(0.55f, Math.min(1.55f, STATE.fuelPerDay / 120f));
        float minDistance = PCSpecializationConstants.FUEL_SCOOP_MIN_SURFACE_DISTANCE;
        float maxDistance = PCSpecializationConstants.FUEL_SCOOP_MAX_SURFACE_DISTANCE;
        float distanceIntensity = (maxDistance - STATE.surfaceDistance) / Math.max(1f, maxDistance - minDistance);
        distanceIntensity = Math.max(0f, Math.min(1f, distanceIntensity));
        return fuelIntensity * (0.72f + distanceIntensity * 0.55f);
    }

    protected static void renderBroadVeil(Vector2f from, Vector2f line, Vector2f normal, float length, float alphaMult, float intensity) {
        int bands = 4;
        for (int band = 0; band < bands; band++) {
            float phase = elapsed * (1.05f + band * 0.18f) + band * 1.73f;
            float sideBias = (band - (bands - 1f) * 0.5f) * 0.33f;
            float alpha = alphaMult * intensity * (0.105f - band * 0.012f);
            Color color = band % 2 == 0 ? VEIL_COLOR : FRINGE_COLOR;

            GL11.glBegin(GL11.GL_QUAD_STRIP);
            for (int i = 0; i <= 30; i++) {
                float t = i / 30f;
                float width = getStreamWidth(t) * (1.08f + band * 0.13f);
                float wander = (float) Math.sin(t * 8.2f + phase) * width * 0.11f
                        + (float) Math.sin(t * 17.0f - phase * 0.7f) * width * 0.045f;
                float centerBias = sideBias * width * (1f - t) * 0.55f;
                float edgeFade = getEndFade(t);
                float localAlpha = edgeFade * (0.72f + 0.28f * (float) Math.sin(phase + t * 10f));

                Vector2f center = getPoint(from, line, normal, length, t, wander + centerBias);
                Vector2f left = offset(center, normal, -width * 0.5f);
                Vector2f right = offset(center, normal, width * 0.5f);

                float a = alpha * localAlpha;
                setColor(color, a);
                GL11.glVertex2f(left.x, left.y);
                GL11.glVertex2f(right.x, right.y);
            }
            GL11.glEnd();
        }
    }

    protected static void renderFringeFilaments(Vector2f from, Vector2f line, Vector2f normal, float length, float alphaMult, float intensity) {
        int filaments = 9;
        for (int f = 0; f < filaments; f++) {
            float seed = f * 12.9898f;
            float phase = elapsed * (2.1f + f * 0.07f) + seed;
            float side = (f - (filaments - 1f) * 0.5f) / Math.max(1f, filaments - 1f);
            float lineAlpha = alphaMult * intensity * (0.18f + 0.08f * (float) Math.sin(phase));
            GL11.glLineWidth(1.25f + (f % 3) * 0.35f);

            GL11.glBegin(GL11.GL_LINE_STRIP);
            for (int i = 0; i <= 32; i++) {
                float t = i / 32f;
                float width = getStreamWidth(t);
                float spiral = (float) Math.sin(t * 16.5f + phase) * width * 0.13f;
                float sideOffset = side * width * (0.52f + 0.18f * (float) Math.sin(t * 9f + phase));
                float endFade = getEndFade(t);
                float alpha = lineAlpha * endFade;
                setColor(RIM_COLOR, alpha);
                Vector2f point = getPoint(from, line, normal, length, t, sideOffset + spiral);
                GL11.glVertex2f(point.x, point.y);
            }
            GL11.glEnd();
        }
    }

    protected static void renderCoreBeam(Vector2f from, Vector2f line, Vector2f normal, float length, float alphaMult, float intensity) {
        for (int pass = 0; pass < 3; pass++) {
            float coreWidth = (18f + pass * 14f) * intensity;
            float alpha = alphaMult * (0.34f - pass * 0.085f) * intensity;

            GL11.glBegin(GL11.GL_QUAD_STRIP);
            for (int i = 0; i <= 34; i++) {
                float t = i / 34f;
                float wave = (float) Math.sin(t * 13.6f - elapsed * 7.5f + pass) * coreWidth * 0.12f;
                float width = coreWidth * (0.65f + 0.35f * (1f - t)) * getEndFade(t);
                Vector2f center = getPoint(from, line, normal, length, t, wave);
                Vector2f left = offset(center, normal, -width * 0.5f);
                Vector2f right = offset(center, normal, width * 0.5f);
                setColor(CORE_COLOR, alpha * getEndFade(t));
                GL11.glVertex2f(left.x, left.y);
                GL11.glVertex2f(right.x, right.y);
            }
            GL11.glEnd();
        }
    }

    protected static void renderIntakeTongues(Vector2f to, Vector2f line, Vector2f normal, float alphaMult, float intensity) {
        int tongues = 5;
        for (int i = 0; i < tongues; i++) {
            float phase = elapsed * (4.2f + i * 0.17f) + i * 1.47f;
            float side = (i - (tongues - 1f) * 0.5f) * (7f + intensity * 3f);
            float length = (34f + i * 7f) * intensity * (0.88f + 0.12f * (float) Math.sin(phase));
            float width = (10f + i * 2.5f) * intensity;
            float alpha = alphaMult * intensity * (0.22f - i * 0.018f);

            Vector2f anchor = offset(to, normal, side + (float) Math.sin(phase) * 5f);
            Vector2f tip = offset(anchor, line, -length);
            Vector2f left = offset(anchor, normal, -width * 0.55f);
            Vector2f right = offset(anchor, normal, width * 0.55f);
            Vector2f glowCenter = offset(anchor, line, -length * 0.35f);

            GL11.glBegin(GL11.GL_TRIANGLES);
            setColor(CORE_COLOR, alpha);
            GL11.glVertex2f(anchor.x, anchor.y);
            setColor(FRINGE_COLOR, alpha * 0.35f);
            GL11.glVertex2f(left.x, left.y);
            setColor(FRINGE_COLOR, 0f);
            GL11.glVertex2f(tip.x, tip.y);

            setColor(CORE_COLOR, alpha);
            GL11.glVertex2f(anchor.x, anchor.y);
            setColor(FRINGE_COLOR, 0f);
            GL11.glVertex2f(tip.x, tip.y);
            setColor(FRINGE_COLOR, alpha * 0.35f);
            GL11.glVertex2f(right.x, right.y);
            GL11.glEnd();

            GL11.glBegin(GL11.GL_TRIANGLE_FAN);
            setColor(CORE_COLOR, alpha * 0.28f);
            GL11.glVertex2f(glowCenter.x, glowCenter.y);
            setColor(CORE_COLOR, 0f);
            for (int s = 0; s <= 12; s++) {
                float angle = (float) (Math.PI * 2.0 * s / 12.0);
                GL11.glVertex2f(glowCenter.x + (float) Math.cos(angle) * width * 1.8f,
                        glowCenter.y + (float) Math.sin(angle) * width * 1.2f);
            }
            GL11.glEnd();
        }
    }

    protected static void renderSolarMouth(Vector2f from, Vector2f line, Vector2f normal, float alphaMult, float intensity) {
        Vector2f center = offset(from, line, 20f);
        float radius = 54f * intensity;
        float alpha = alphaMult * intensity * 0.34f;

        GL11.glLineWidth(2.2f);
        setColor(CORE_COLOR, alpha);
        drawCircleLine(center.x, center.y, radius, 56);

        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        setColor(CORE_COLOR, alpha * 0.35f);
        GL11.glVertex2f(center.x, center.y);
        setColor(FRINGE_COLOR, 0f);
        for (int i = 0; i <= 60; i++) {
            float angle = (float) (Math.PI * 2.0 * i / 60.0);
            GL11.glVertex2f(center.x + (float) Math.cos(angle) * radius * 1.6f,
                    center.y + (float) Math.sin(angle) * radius * 1.6f);
        }
        GL11.glEnd();
    }

    protected static float getStreamWidth(float t) {
        float wide = 210f;
        float narrow = 34f;
        float curved = 1f - t;
        curved = curved * curved * (3f - 2f * curved);
        return narrow + wide * curved;
    }

    protected static float getEndFade(float t) {
        float in = Math.min(1f, t / 0.10f);
        float out = Math.min(1f, (1f - t) / 0.08f);
        return Math.max(0f, Math.min(1f, in * out));
    }

    protected static Vector2f getPoint(Vector2f from, Vector2f line, Vector2f normal, float length, float t, float sideOffset) {
        Vector2f result = new Vector2f(from);
        Vector2f along = new Vector2f(line);
        along.scale(length * t);
        Vector2f.add(result, along, result);

        Vector2f side = new Vector2f(normal);
        side.scale(sideOffset);
        Vector2f.add(result, side, result);
        return result;
    }

    protected static Vector2f offset(Vector2f point, Vector2f dir, float amount) {
        Vector2f result = new Vector2f(point);
        Vector2f off = new Vector2f(dir);
        off.scale(amount);
        Vector2f.add(result, off, result);
        return result;
    }

    protected static void drawCircleLine(float cx, float cy, float radius, int segments) {
        if (radius <= 0f || segments < 8) {
            return;
        }

        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i < segments; i++) {
            float angle = (float) (Math.PI * 2.0 * i / segments);
            GL11.glVertex2f(cx + (float) Math.cos(angle) * radius, cy + (float) Math.sin(angle) * radius);
        }
        GL11.glEnd();
    }

    protected static Vector2f getSurfacePoint(PlanetAPI star, Vector2f target) {
        Vector2f result = new Vector2f(star.getLocation());
        Vector2f dir = Vector2f.sub(target, star.getLocation(), new Vector2f());
        if (dir.length() <= 1f) {
            return result;
        }

        dir.normalise();
        dir.scale(Math.max(0f, star.getRadius()));
        Vector2f.add(result, dir, result);
        return result;
    }

    protected static void setColor(Color color, float alphaMult) {
        int alpha = Math.max(0, Math.min(255, (int) (color.getAlpha() * alphaMult)));
        GL11.glColor4ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue(), (byte) alpha);
    }

    protected PCFuelScoopVisualRenderer() {
    }
}