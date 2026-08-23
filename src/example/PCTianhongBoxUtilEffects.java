package data.scripts.specialization;

import java.awt.Color;
import java.lang.reflect.Method;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.campaign.CampaignEngineLayers;

/**
 * Optional BoxUtil campaign-layer bloom/distortion support for Tianhong Haodang.
 *
 * Kept reflection-based so the ability still runs if BoxUtil is missing,
 * disabled, or changes its binary surface in a future version.
 */
public class PCTianhongBoxUtilEffects {

    protected static final Color BLUE_CORE = new Color(220, 250, 255, 245);
    protected static final Color BLUE_FRINGE = new Color(70, 205, 255, 190);
    protected static final Color ORANGE_CORE = new Color(255, 232, 170, 250);
    protected static final Color ORANGE_FRINGE = new Color(255, 92, 28, 210);

    protected static boolean initialized = false;
    protected static boolean available = false;
    protected static int failures = 0;

    protected static Class<?> renderDataClass;
    protected static Class<?> managerClass;
    protected static Class<?> flareClass;
    protected static Class<?> distortionClass;
    protected static Class<?> pointLightClass;
    protected static Method addEntityMethod;

    public static void spawnFiringPulse(Vector2f fleet, Vector2f target, float targetRadius, float progress, float elapsed) {
        // Continuous BoxUtil flares read as a large soft blob in campaign view.
        // The in-flight effect is rendered by PCTianhongVisualRenderer instead.
    }

    public static void spawnFinalBurst(Vector2f loc, float radius) {
        if (loc == null || !ensureAvailable()) {
            return;
        }

        try {
            float safeRadius = Math.max(240f, radius);
            addFlare(loc, safeRadius * 1.25f, safeRadius * 0.88f, ORANGE_CORE, ORANGE_FRINGE, 0.05f, 0.32f, 4.8f, 0.30f);
            addFlare(loc, safeRadius * 0.78f, safeRadius * 0.78f, BLUE_CORE, BLUE_FRINGE, 0.04f, 0.25f, 3.4f, 0.22f);
            addDistortion(loc, safeRadius * 0.18f, safeRadius * 0.72f, safeRadius * 1.45f, 0.004f, 0.035f, 0.010f);
            addPointLight(loc, ORANGE_CORE, 1.4f, safeRadius * 1.8f, 0.40f);
            addPointLight(loc, BLUE_CORE, 0.75f, safeRadius * 1.35f, 0.32f);
        } catch (Throwable ex) {
            markFailure();
        }
    }

    protected static boolean ensureAvailable() {
        if (initialized) {
            return available;
        }
        initialized = true;
        try {
            renderDataClass = Class.forName("org.boxutil.base.api.RenderDataAPI");
            managerClass = Class.forName("org.boxutil.manager.CampaignRenderingManager");
            flareClass = Class.forName("org.boxutil.units.standard.entity.FlareEntity");
            distortionClass = Class.forName("org.boxutil.units.standard.entity.DistortionEntity");
            pointLightClass = Class.forName("org.boxutil.units.standard.light.PointLight");
            addEntityMethod = managerClass.getMethod("addEntity", renderDataClass);
            available = true;
        } catch (Throwable ex) {
            available = false;
        }
        return available;
    }

    protected static void addFlare(Vector2f loc, float width, float height, Color core, Color fringe,
                                   float fullTime, float fadeOut, float glowPower, float alpha) throws Exception {
        Object flare = flareClass.getConstructor().newInstance();
        invoke(flare, "setLayer", new Class[] {Object.class}, new Object[] {CampaignEngineLayers.ABOVE});
        invoke(flare, "setLocation", new Class[] {float.class, float.class}, new Object[] {loc.x, loc.y});
        invoke(flare, "setGlobalTimer", new Class[] {float.class, float.class, float.class}, new Object[] {0.03f, fullTime, fadeOut});
        invoke(flare, "setAdditiveBlend", new Class[0], new Object[0]);
        invoke(flare, "setSmoothDisc", new Class[0], new Object[0]);
        invoke(flare, "setFlick", new Class[] {boolean.class}, new Object[] {true});
        invoke(flare, "setFlickWhenPaused", new Class[] {boolean.class}, new Object[] {false});
        invoke(flare, "setFlickMixValue", new Class[] {float.class}, new Object[] {0.35f});
        invoke(flare, "setSize", new Class[] {float.class, float.class}, new Object[] {width, height});
        invoke(flare, "setCoreColor", new Class[] {Color.class}, new Object[] {core});
        invoke(flare, "setFringeColor", new Class[] {Color.class}, new Object[] {fringe});
        invoke(flare, "setGlowPower", new Class[] {float.class}, new Object[] {glowPower});
        invoke(flare, "setGlobalAlpha", new Class[] {float.class}, new Object[] {alpha});
        addEntityMethod.invoke(null, flare);
    }

    protected static void addDistortion(Vector2f loc, float sizeIn, float sizeFull, float sizeOut,
                                        float powerIn, float powerFull, float powerOut) throws Exception {
        Object distortion = distortionClass.getConstructor().newInstance();
        invoke(distortion, "setLayer", new Class[] {Object.class}, new Object[] {CampaignEngineLayers.ABOVE});
        invoke(distortion, "setLocation", new Class[] {float.class, float.class}, new Object[] {loc.x, loc.y});
        invoke(distortion, "setGlobalTimer", new Class[] {float.class, float.class, float.class}, new Object[] {0.04f, 0.18f, 0.56f});
        invoke(distortion, "setAdditiveBlend", new Class[0], new Object[0]);
        invoke(distortion, "setSizeIn", new Class[] {float.class, float.class}, new Object[] {sizeIn, sizeIn});
        invoke(distortion, "setSizeFull", new Class[] {float.class, float.class}, new Object[] {sizeFull, sizeFull});
        invoke(distortion, "setSizeOut", new Class[] {float.class, float.class}, new Object[] {sizeOut, sizeOut});
        invoke(distortion, "setPowerIn", new Class[] {float.class}, new Object[] {powerIn});
        invoke(distortion, "setPowerFull", new Class[] {float.class}, new Object[] {powerFull});
        invoke(distortion, "setPowerOut", new Class[] {float.class}, new Object[] {powerOut});
        invoke(distortion, "setRingHardness", new Class[] {float.class}, new Object[] {0.42f});
        invoke(distortion, "setInnerHardness", new Class[] {float.class}, new Object[] {0.12f});
        addEntityMethod.invoke(null, distortion);
    }

    protected static void addPointLight(Vector2f loc, Color color, float strength, float radius, float fullTime) throws Exception {
        Object light = pointLightClass.getConstructor().newInstance();
        invoke(light, "setLayer", new Class[] {Object.class}, new Object[] {CampaignEngineLayers.ABOVE});
        invoke(light, "setLocation", new Class[] {float.class, float.class}, new Object[] {loc.x, loc.y});
        invoke(light, "setGlobalTimer", new Class[] {float.class, float.class, float.class}, new Object[] {0.04f, fullTime, 0.42f});
        invoke(light, "setColor", new Class[] {Color.class}, new Object[] {color});
        invoke(light, "setStrength", new Class[] {float.class}, new Object[] {strength});
        invoke(light, "setAttenuationRadius", new Class[] {float.class}, new Object[] {radius});
        invoke(light, "setSquareAttenuation", new Class[0], new Object[0]);
        addEntityMethod.invoke(null, light);
    }

    protected static void invoke(Object target, String methodName, Class[] types, Object[] args) throws Exception {
        Method method = target.getClass().getMethod(methodName, types);
        method.invoke(target, args);
    }

    protected static void markFailure() {
        failures++;
        if (failures >= 5) {
            available = false;
        }
    }

    protected static float smoothstep(float value) {
        value = clamp01(value);
        return value * value * (3f - 2f * value);
    }

    protected static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    protected PCTianhongBoxUtilEffects() {
    }
}
