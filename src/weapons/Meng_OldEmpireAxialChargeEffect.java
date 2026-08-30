package data.weapons;

import com.fs.starfarer.api.combat.BaseCombatLayeredRenderingPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.hullmods.Meng_OldEmpireBlackHoleCoreHullmod;
import java.util.EnumSet;
import java.util.Map;
import java.util.WeakHashMap;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

/** Charge core for Crimson Spike. */
public final class Meng_OldEmpireAxialChargeEffect
        implements EveryFrameWeaponEffectPlugin {
    private static final Map<WeaponAPI, ChargeVisual> VISUALS =
            new WeakHashMap<WeaponAPI, ChargeVisual>();

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine == null || weapon == null || weapon.getShip() == null
                || !Meng_OldEmpireAxialIds.CRIMSON_SPIKE.equals(weapon.getId())) {
            return;
        }

        boolean empowered = Meng_OldEmpireBlackHoleCoreHullmod
                .isEmpoweredShotAvailable(weapon.getShip());
        float glow = weapon.isDisabled() ? 0f
                : 0.16f + 0.76f * weapon.getChargeLevel();
        weapon.setGlowAmount(glow, Meng_OldEmpireAxialFx.accent(empowered));

        ChargeVisual visual = VISUALS.get(weapon);
        if (visual == null || visual.isExpired()) {
            visual = new ChargeVisual(weapon, engine);
            VISUALS.put(weapon, visual);
            engine.addLayeredRenderingPlugin(visual);
        }
    }

    private static final class ChargeVisual
            extends BaseCombatLayeredRenderingPlugin {
        private final WeaponAPI weapon;
        private final CombatEngineAPI engine;
        private float elapsed;

        private ChargeVisual(WeaponAPI weapon, CombatEngineAPI engine) {
            super(CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER);
            this.weapon = weapon;
            this.engine = engine;
        }

        @Override
        public EnumSet<CombatEngineLayers> getActiveLayers() {
            return EnumSet.of(
                    CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER);
        }

        @Override
        public float getRenderRadius() {
            return 180f;
        }

        @Override
        public boolean isExpired() {
            ShipAPI ship = weapon == null ? null : weapon.getShip();
            return engine == null || weapon == null || ship == null
                    || !engine.isEntityInPlay(ship);
        }

        @Override
        public void advance(float amount) {
            if (engine != null && !engine.isPaused()) {
                elapsed += amount;
            }
        }

        @Override
        public void render(CombatEngineLayers layer, ViewportAPI viewport) {
            if (weapon == null || weapon.getShip() == null
                    || weapon.isDisabled()) {
                return;
            }
            float charge = weapon.getChargeLevel();
            if (charge <= 0.015f) {
                return;
            }

            Vector2f location = weapon.getFirePoint(0);
            if (location == null) {
                location = weapon.getLocation();
            }
            if (location == null || !viewport.isNearViewport(location, 180f)) {
                return;
            }

            boolean empowered = Meng_OldEmpireBlackHoleCoreHullmod
                    .isEmpoweredShotAvailable(weapon.getShip());
            float pulse = 1f + 0.08f
                    * (float) Math.sin(elapsed * 6.8f);
            float radius = (8f + charge * 17f) * pulse;

            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            try {
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                GL11.glDisable(GL11.GL_CULL_FACE);
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glEnable(GL11.GL_LINE_SMOOTH);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

                Meng_OldEmpireAxialFx.drawDisc(location, radius * 1.75f,
                        Meng_OldEmpireAxialFx.deep(empowered),
                        0.15f + charge * 0.12f, 24);
                Meng_OldEmpireAxialFx.drawDisc(location, radius,
                        Meng_OldEmpireAxialFx.BLACK,
                        0.70f + charge * 0.22f, 24);
                Meng_OldEmpireAxialFx.drawRing(location, radius * 1.08f,
                        2.2f + charge * 1.8f,
                        Meng_OldEmpireAxialFx.accent(empowered),
                        0.34f + charge * 0.32f, 26);
                Meng_OldEmpireAxialFx.drawRing(location, radius * 1.48f,
                        1.2f, Meng_OldEmpireAxialFx.hot(empowered),
                        0.18f * charge, 28);
            } finally {
                GL11.glPopAttrib();
            }
        }
    }
}
