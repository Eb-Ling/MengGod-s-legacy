package data.methods;

import org.lwjgl.util.vector.Vector2f;

public class Meng_V2arcfind {
    public static float Findarc(Vector2f vel) {
        float arg;
        if (vel.getX() > 0) {
            arg = (float) Math.toDegrees(Math.atan(vel.y/vel.x));
        } else {
            arg = 180f+(float) Math.toDegrees(Math.atan(vel.y/vel.x));
        }
        return arg;
    }
}
