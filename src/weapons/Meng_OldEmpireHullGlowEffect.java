package data.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import data.hullmods.Meng_OldEmpireBlackHoleCoreHullmod;
import java.awt.Color;

/**
 * Drives the hull-sized -2 overlay through the native decorative weapon
 * renderer. The sprite itself is positioned and rotated by Starsector.
 */
public final class Meng_OldEmpireHullGlowEffect
        implements EveryFrameWeaponEffectPlugin {
    private float elapsed;

    @Override
    public void advance(float amount, CombatEngineAPI engine,
            WeaponAPI weapon) {
        if (engine == null || weapon == null || weapon.getShip() == null) {
            return;
        }
        if (!engine.isPaused()) {
            elapsed += amount;
        }

        ShipAPI ship = weapon.getShip();
        SpriteAPI sprite = weapon.getSprite();
        if (sprite == null) {
            return;
        }

        float value = Meng_OldEmpireBlackHoleCoreHullmod.getCoreValue(ship);
        float progress = clamp((value - 1f) / 99f, 0f, 1f);
        float phaseLevel = ship.getPhaseCloak() == null
                ? 0f : clamp(
                        ship.getPhaseCloak().getEffectLevel(), 0f, 1f);
        float pulse = 0.92f + 0.08f
                * (float) Math.sin(elapsed * (2.4f + progress * 4.8f));
        float alpha = (0.72f + progress * 0.20f)
                * pulse * (1f - phaseLevel * 0.55f);
        Color color = Meng_OldEmpireBlackHoleCoreHullmod.getCoreColor(ship);

        sprite.setAdditiveBlend();
        sprite.setColor(color);
        sprite.setAlphaMult(clamp(alpha, 0f, 1f));
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
