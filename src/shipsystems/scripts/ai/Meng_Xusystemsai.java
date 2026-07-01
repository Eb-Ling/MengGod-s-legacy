package data.shipsystems.scripts.ai;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

public class Meng_Xusystemsai implements ShipSystemAIScript {
    private CombatEngineAPI engine;
    private ShipAPI ship;

    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.engine = engine;
    }

    private void use(ShipAPI ship) {
        if (ship.getPhaseCloak() != null && ship.getPhaseCloak().getId().contentEquals("Meng_Xusystem")) {
            ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
        } else {
            ship.useSystem();
        }
    }

    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        if ((this.engine.isPaused()) || (!this.ship.isAlive())) {
            return;
        }
        target = findTarget(this.ship);


        boolean use = (target != null) && (target.isAlive()) && (target.getOwner() != this.ship.getOwner());


        if ((use))
            use(this.ship);
    }

    private ShipAPI findTarget(ShipAPI ship) {

        WeightedRandomPicker<ShipAPI> targets = new WeightedRandomPicker<>();

        for (ShipAPI target : AIUtils.getNearbyEnemies(ship, 1500f)) {
            if (target.isFighter() || target.isDrone()) continue;

            targets.add(target);
        }

        return targets.pick();
    }
}

