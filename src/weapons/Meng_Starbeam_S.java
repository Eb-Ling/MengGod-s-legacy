package data.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.prototype.entities.Ship;

import java.awt.*;
import java.util.EnumSet;

public class Meng_Starbeam_S implements BeamEffectPlugin {
    private final static String id = "Meng_starbeam";
    private static ShipAPI target;
    private static boolean inits = false;

    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        if (!inits) {
            inits = true;
            target = null;
        }

        if (beam.getDamageTarget() instanceof ShipAPI) {
            target = (ShipAPI) beam.getDamageTarget();
        }
        if (target != null) {
            target.getVelocity().set(0f, 0f);
            CombatEngineAPI engine1 = Global.getCombatEngine();
            engine1.addLayeredRenderingPlugin(new Meng_StarbeamPlugin());
        }
    }

    public static class Meng_StarbeamPlugin implements CombatLayeredRenderingPlugin {
        private float timer = 0f;
        private ShipAPI targets;
        private boolean init = false;

        public void init(CombatEntityAPI entity) {

        }

        @Override
        public void cleanup() {

        }

        @Override
        public boolean isExpired() {
            return timer >= 7f;
        }

        @Override
        public void advance(float amount) {
            if (!init) {
                init = true;
                targets = target;
            }
            timer += amount;

            if (timer <= 5f) {
                if (targets != null) {
                    targets.getMutableStats().getMaxArmorDamageReduction().modifyMult(id, 0.7f);
                    targets.setJitter(targets, new Color(255, 255, 255, 255), 3f, 1, 3f);
                }
            } else {
                if (targets != null) {
                    targets.getMutableStats().getMaxArmorDamageReduction().unmodifyMult(id);
                }
                inits = false;

            }
        }

        @Override
        public EnumSet<CombatEngineLayers> getActiveLayers() {
            return null;
        }

        @Override
        public float getRenderRadius() {
            return 0;
        }

        @Override
        public void render(CombatEngineLayers layer, ViewportAPI viewport) {

        }

    }
}
