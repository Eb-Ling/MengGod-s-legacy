package data.shipsystems.scripts.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.lwjgl.util.vector.Vector2f;

public class Meng_Kongjianai implements ShipSystemAIScript {
    float timer;
    private CombatEngineAPI engine;
    private ShipAPI ship;
    private ShipAPI lastship;

    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.engine = engine;
        timer = 0f;
    }

    private void use(ShipAPI ship) {
        if (ship.getPhaseCloak() != null && ship.getPhaseCloak().getId().contentEquals("Meng_Timesystem")) {
            ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
        } else {
            ship.useSystem();
        }
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        timer += amount;
        float b = 0f;
        float e = 0f;
        float a = 0f;
        for (ShipAPI ships : Global.getCombatEngine().getShips()) {
            if (ships!=null&&ships.getOwner() != this.ship.getOwner() && !ships.isDrone() && !ships.isFighter() && ships.isAlive()) {

                if (ships.getHullSize() == ShipAPI.HullSize.FRIGATE) a = 1f;
                if (ships.getHullSize() == ShipAPI.HullSize.DESTROYER) a = 2f;
                if (ships.getHullSize() == ShipAPI.HullSize.CRUISER) a = 3f;
                if (ships.getHullSize() == ShipAPI.HullSize.CAPITAL_SHIP) a = 4f;

                float d = Vector2f.sub(this.ship.getLocation(), ships.getLocation(), new Vector2f()).length() + this.ship.getShieldRadiusEvenIfNoShield() + ships.getShieldRadiusEvenIfNoShield();
                if (d <= 3500f) {
                    if (a > e) {
                        lastship = ships;
                        e = a;
                    }

                    b = 1000f;
                } else {
                    if (a > b) {
                        lastship = ships;
                        b = a;
                    }


                }

            }
            if (lastship != null) {
                ship.getMouseTarget().set(new Vector2f(lastship.getLocation().getX() + lastship.getVelocity().getX() * (5f - a), lastship.getLocation().getY() + lastship.getVelocity().getY() * (5f - a)));
            }
        }
        boolean use = (!this.ship.getSystem().isCoolingDown() && timer >= 10f);
        if ((use))
            use(this.ship);
    }
}
