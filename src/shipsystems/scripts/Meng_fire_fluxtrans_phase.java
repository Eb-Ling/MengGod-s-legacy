package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import data.methods.Meng_hullsizeint;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.EnumSet;

public class Meng_fire_fluxtrans_phase extends BaseShipSystemScript {
    boolean init = false;
    private static final String RECOVERY_PLUGIN_KEY = "Meng_fire_fluxtrans_phase_recovery";
    private static final float RECOVERY_DURATION = 2.0f;
    private static final float RECOVERY_RATIO = 0.3f;

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {

        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) {
            return;
        }
        FluxTrackerAPI tracker = ship.getFluxTracker();
        if (!init) {
            init = true;
            float hardflux = tracker.getHardFlux();
            tracker.setHardFlux(tracker.getCurrFlux() - hardflux);

            float fluxDifference = Math.abs(tracker.getCurrFlux() - hardflux - hardflux);
            if (fluxDifference > 0) {
                createRecoveryEffect(ship, fluxDifference * RECOVERY_RATIO);
            }
        }
        ShipAPI target = ship.getShipTarget();
        if (target != null && Meng_hullsizeint.Getsize(target) <= Meng_hullsizeint.Getsize(ship)) {
            if (ship.getAI() != null) {
                if (tracker.getCurrFlux() <= ship.getMaxFlux() * 0.6f && target.getVelocity().length() <= ship.getMaxSpeed()) {
                    if (target.getFluxTracker().getCurrFlux() >= target.getMaxFlux() * 0.5f) {
                        ship.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.PURSUING);
                    } else {
                        ship.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.DO_NOT_BACK_OFF);
                    }
                }
            }
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) {
            return;
        }
        
        FluxTrackerAPI tracker = ship.getFluxTracker();
        float hardflux = tracker.getHardFlux();
        
        float fluxDifference = Math.abs(tracker.getCurrFlux() - hardflux - hardflux);
        if (fluxDifference > 0) {
            createRecoveryEffect(ship, fluxDifference * RECOVERY_RATIO);
        }
        
        tracker.setHardFlux(tracker.getCurrFlux() - hardflux);
        
        init = false;
        if (ship.getAI() != null) {
            ship.getAIFlags().removeFlag(ShipwideAIFlags.AIFlags.DO_NOT_BACK_OFF);
            ship.getAIFlags().removeFlag(ShipwideAIFlags.AIFlags.PURSUING);
        }
    }

    private void createRecoveryEffect(ShipAPI ship, float fluxDifference) {
        Object existingPlugin = ship.getCustomData().get(RECOVERY_PLUGIN_KEY);
        if (existingPlugin instanceof FluxRecoveryPlugin) {
            FluxRecoveryPlugin plugin = (FluxRecoveryPlugin) existingPlugin;
            if (!plugin.isExpired()) {
                plugin.addRecoveryAmount(fluxDifference);
            } else {
                FluxRecoveryPlugin newPlugin = new FluxRecoveryPlugin(ship, fluxDifference);
                Global.getCombatEngine().addLayeredRenderingPlugin(newPlugin);
                ship.setCustomData(RECOVERY_PLUGIN_KEY, newPlugin);
            }
        } else {
            FluxRecoveryPlugin plugin = new FluxRecoveryPlugin(ship, fluxDifference);
            Global.getCombatEngine().addLayeredRenderingPlugin(plugin);
            ship.setCustomData(RECOVERY_PLUGIN_KEY, plugin);
        }
    }

    public static class FluxRecoveryPlugin implements CombatLayeredRenderingPlugin {
        private ShipAPI ship;
        private boolean expired = false;
        private float timer = 0f;
        private float totalRecoveryAmount = 0f;
        private float remainingRecovery = 0f;

        public FluxRecoveryPlugin(ShipAPI ship, float initialAmount) {
            this.ship = ship;
            this.totalRecoveryAmount = initialAmount;
            this.remainingRecovery = initialAmount;
        }

        public void addRecoveryAmount(float amount) {
            this.totalRecoveryAmount += amount;
            this.remainingRecovery += amount;
        }

        @Override
        public void init(CombatEntityAPI entity) {
        }

        @Override
        public void cleanup() {
            if (ship != null && ship.isAlive()) {
                ship.getCustomData().remove(RECOVERY_PLUGIN_KEY);
            }
        }

        @Override
        public boolean isExpired() {
            return expired || ship == null || !ship.isAlive() || timer >= RECOVERY_DURATION;
        }

        @Override
        public void advance(float amount) {
            timer += amount;

            if (remainingRecovery > 0 && ship.isAlive()) {
                float recoveryThisFrame = remainingRecovery * (amount / RECOVERY_DURATION);
                if (recoveryThisFrame > remainingRecovery) {
                    recoveryThisFrame = remainingRecovery;
                }

                float currentHull = ship.getHitpoints();
                float maxHull = ship.getMaxHitpoints();
                float newHull = Math.min(maxHull, currentHull + recoveryThisFrame);
                ship.setHitpoints(newHull);

                remainingRecovery -= recoveryThisFrame;

                if (remainingRecovery <= 0.1f) {
                    remainingRecovery = 0f;
                }
            }

            if (timer >= RECOVERY_DURATION) {
                expired = true;
            }
        }

        @Override
        public EnumSet<CombatEngineLayers> getActiveLayers() {
            return EnumSet.noneOf(CombatEngineLayers.class);
        }

        @Override
        public float getRenderRadius() {
            return 0f;
        }

        @Override
        public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        }
    }
}