package data.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.combat.entities.SimpleEntity;

import java.awt.*;

public class Meng_Xbeameffect implements BeamEffectPlugin {
    private boolean init = false;

    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        if (beam.getDamageTarget() instanceof ShipAPI) {
            ShipAPI target = (ShipAPI) beam.getDamageTarget();

            if (target.getOriginalOwner() != beam.getSource().getOriginalOwner()) {
                beam.getDamage().setDamage(10f);
                if (!init) {
                    Global.getCombatEngine().spawnEmpArc(beam.getSource(), beam.getTo(), new SimpleEntity(beam.getTo()), new SimpleEntity(target.getLocation()), DamageType.ENERGY, 0f, 0f, 1000000f, null, 20f, new Color(218, 0, 255, 255), new Color(0, 225, 248, 255));
                    init = true;
                    float damage = (float) Math.random() * 2000f;
                    if (target.getHitpoints() - damage <= 0f) {
                        Global.getCombatEngine().applyDamage(target, target.getLocation(), 10000000f, DamageType.ENERGY, 0f, true, false, beam.getSource());
                    } else {
                        Global.getCombatEngine().addFloatingDamageText(beam.getTo(), damage, new Color(79, 255, 245, 255), target, beam.getSource());
                        target.setHitpoints(target.getHitpoints() - damage);
                    }
                }
            }
        }
    }
}
