package data.weapons;

import java.awt.Color;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

/** Shared rendering and geometry helpers for the remaining axial weapon. */
final class Meng_OldEmpireAxialFx {
    static final Color BLACK = new Color(5, 0, 2);
    static final Color DEEP_RED = new Color(82, 3, 15);
    static final Color RED = new Color(205, 18, 43);
    static final Color HOT_RED = new Color(255, 92, 104);
    static final Color PALE_RED = new Color(255, 206, 212);
    static final Color DEEP_BLUE = new Color(3, 30, 92);
    static final Color BLUE = new Color(22, 105, 238);
    static final Color HOT_BLUE = new Color(75, 181, 255);
    static final Color PALE_BLUE = new Color(194, 235, 255);

    private Meng_OldEmpireAxialFx() {
    }

    static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    static float wrap(float angle) {
        float result = angle % 360f;
        return result < 0f ? result + 360f : result;
    }

    static Vector2f polar(float angle, float length) {
        double radians = Math.toRadians(angle);
        return new Vector2f((float) Math.cos(radians) * length,
                (float) Math.sin(radians) * length);
    }

    static Vector2f point(Vector2f center, float angle, float length) {
        Vector2f result = polar(angle, length);
        result.x += center.x;
        result.y += center.y;
        return result;
    }

    static float angle(Vector2f from, Vector2f to) {
        return wrap((float) Math.toDegrees(
                Math.atan2(to.y - from.y, to.x - from.x)));
    }

    static float distance(Vector2f first, Vector2f second) {
        float x = first.x - second.x;
        float y = first.y - second.y;
        return (float) Math.sqrt(x * x + y * y);
    }

    static Color deep(boolean empowered) {
        return empowered ? DEEP_RED : DEEP_BLUE;
    }

    static Color accent(boolean empowered) {
        return empowered ? RED : BLUE;
    }

    static Color hot(boolean empowered) {
        return empowered ? HOT_RED : HOT_BLUE;
    }

    static Color pale(boolean empowered) {
        return empowered ? PALE_RED : PALE_BLUE;
    }

    static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(),
                Math.max(0, Math.min(255, alpha)));
    }

    static void drawDisc(Vector2f center, float radius, Color color,
            float alpha, int segments) {
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        setColor(color, alpha);
        GL11.glVertex2f(center.x, center.y);
        for (int i = 0; i <= segments; i++) {
            float angle = 360f * i / segments;
            setColor(color, 0f);
            Vector2f point = point(center, angle, radius);
            GL11.glVertex2f(point.x, point.y);
        }
        GL11.glEnd();
    }

    static void drawRing(Vector2f center, float radius, float width,
            Color color, float alpha, int segments) {
        float inner = Math.max(0.1f, radius - width);
        float outer = radius + width;
        GL11.glBegin(GL11.GL_QUAD_STRIP);
        for (int i = 0; i <= segments; i++) {
            float angle = 360f * i / segments;
            setColor(color, alpha);
            vertex(point(center, angle, inner));
            vertex(point(center, angle, outer));
        }
        GL11.glEnd();
    }

    private static void setColor(Color color, float alpha) {
        GL11.glColor4f(color.getRed() / 255f, color.getGreen() / 255f,
                color.getBlue() / 255f, clamp(alpha, 0f, 1f));
    }

    private static void vertex(Vector2f point) {
        GL11.glVertex2f(point.x, point.y);
    }
}

final class Meng_OldEmpireAxialIds {
    static final String CRIMSON_SPIKE = "Meng_OldEmpire_axis_crimson_spike";
    static final String CRIMSON_SPIKE_BURST =
            "Meng_OldEmpire_axis_crimson_spike_burst";

    private Meng_OldEmpireAxialIds() {
    }
}
