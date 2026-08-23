package data.methods;

import java.lang.reflect.Method;

/** Reads the optional LunaLib setting without making LunaLib a hard dependency. */
public final class MengPerformanceSettings {
    private static final String MOD_ID = "Meng";
    private static final String LOW_PERFORMANCE_EFFECTS = "lowPerformanceEffects";
    private static Method getBoolean;
    private static boolean lookupAttempted;

    private MengPerformanceSettings() {
    }

    public static boolean useLowPerformanceEffects() {
        try {
            if (!lookupAttempted) {
                Class<?> settingsClass = Class.forName("lunalib.lunaSettings.LunaSettings");
                getBoolean = settingsClass.getMethod("getBoolean", String.class, String.class);
                lookupAttempted = true;
            }
            if (getBoolean == null) {
                return false;
            }
            Object value = getBoolean.invoke(null, MOD_ID, LOW_PERFORMANCE_EFFECTS);
            return Boolean.TRUE.equals(value);
        } catch (Exception ignored) {
            lookupAttempted = true;
            return false;
        }
    }
}
