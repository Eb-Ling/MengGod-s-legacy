package data.methods;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.lwjgl.util.vector.Vector2f;

public final class Meng_OldEmpireMath {
    private Meng_OldEmpireMath() {}

    public static float random(float min, float max) {
        return min + (float) Math.random() * (max - min);
    }

    public static Vector2f fromAngle(float angle, float length) {
        double rad = Math.toRadians(angle);
        return new Vector2f((float) Math.cos(rad) * length, (float) Math.sin(rad) * length);
    }

    public static Vector2f offset(Vector2f point, float angle, float length) {
        Vector2f v = fromAngle(angle, length);
        return new Vector2f(point.x + v.x, point.y + v.y);
    }

    public static float angle(Vector2f from, Vector2f to) {
        return (float) Math.toDegrees(Math.atan2(to.y - from.y, to.x - from.x));
    }

    public static float distanceSquared(Vector2f a, Vector2f b) {
        float dx = a.x - b.x;
        float dy = a.y - b.y;
        return dx * dx + dy * dy;
    }

    public static ShipAPI nearestEnemy(CombatEngineAPI engine, CombatEntityAPI origin, float range) {
        if (engine == null || origin == null) return null;
        ShipAPI result = null;
        float best = range * range;
        for (ShipAPI ship : engine.getShips()) {
            if (ship == null || ship == origin || !ship.isAlive() || ship.isHulk()) continue;
            if (ship.getOwner() == origin.getOwner()) continue;
            float dist = distanceSquared(origin.getLocation(), ship.getLocation());
            if (dist < best) {
                best = dist;
                result = ship;
            }
        }
        return result;
    }
}
