package data.weapons;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.BeamEffectPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;

public class Meng_Starbeam implements BeamEffectPlugin {
    private ShipAPI target;

    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        if (beam.getDamageTarget() instanceof ShipAPI) {
            target = (ShipAPI) beam.getDamageTarget();
        }
        if (target != null) {
            target.getVelocity().set(0f, 0f);
        }
    }
}
