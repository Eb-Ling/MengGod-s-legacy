package data.methods;

import org.lwjgl.util.vector.Vector2f;

public class Meng_arcfind {
    public static float Findarc(Vector2f source, Vector2f target) {
        float dx = target.getX() - source.getX();
        float dy = target.getY() - source.getY();
        
        if (Math.abs(dy) < 0.0001f) {
            if (dx > 0) {
                return 0f;
            } else if (dx < 0) {
                return 180f;
            } else {
                return 0f;
            }
        }
        
        float arg;
        if (dy > 0) {
            arg = 90f - (float) Math.toDegrees(Math.atan(dx / dy));
        } else {
            arg = 270f - (float) Math.toDegrees(Math.atan(dx / dy));
        }
        return arg;
    }
}
