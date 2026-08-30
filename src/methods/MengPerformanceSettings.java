package data.methods;

import com.fs.starfarer.api.Global;

import java.lang.reflect.Method;

/**
 * Reads optional LunaLib performance settings without making LunaLib a required dependency.
 */
public final class MengPerformanceSettings {
    private static final String MOD_ID = "Meng";
    private static final String LUNALIB_MOD_ID = "lunalib";
    private static final String LOW_PERFORMANCE_EFFECTS = "lowPerformanceEffects";
    private static Method getBoolean;
    private static boolean lookupAttempted;

    private MengPerformanceSettings() {
    }

    public static boolean useLowPerformanceEffects() {
        return getBoolean(LOW_PERFORMANCE_EFFECTS, false);
    }

    private static boolean getBoolean(String fieldId, boolean fallback) {
        if (!isLunaLibEnabled()) {
            return fallback;
        }

        try {
            if (!lookupAttempted) {
                Class<?> settingsClass = Class.forName("lunalib.lunaSettings.LunaSettings");
                getBoolean = settingsClass.getMethod("getBoolean", String.class, String.class);
                lookupAttempted = true;
            }
            if (getBoolean == null) {
                return fallback;
            }
            Object value = getBoolean.invoke(null, MOD_ID, fieldId);
            return value instanceof Boolean ? (Boolean) value : fallback;
        } catch (Exception ignored) {
            lookupAttempted = true;
            return fallback;
        }
    }

    private static boolean isLunaLibEnabled() {
        return Global.getSettings() != null
                && Global.getSettings().getModManager() != null
                && Global.getSettings().getModManager().isModEnabled(LUNALIB_MOD_ID);
    }
}
