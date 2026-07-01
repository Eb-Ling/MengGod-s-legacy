package data.shipsystems.scripts.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import data.weapons.Meng_clockbuildereffect;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

public class Meng_Timesystemai implements ShipSystemAIScript {
    public static final String KEY = "Mengtimelistener_S";
    float timer;
    float timer1;
    private CombatEngineAPI engine;
    private ShipAPI ship;

    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.engine = engine;
        this.timer = 0f;
        this.timer1 = 0f;
    }

    private void use(ShipAPI ship) {
        if (ship.getPhaseCloak() != null && ship.getPhaseCloak().getId().contentEquals("Meng_Timesystem")) {
            ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
        } else {
            ship.useSystem();
        }
    }

    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        if ((this.engine.isPaused()) || (!this.ship.isAlive())) {
            return;
        }
        if (!this.ship.getCustomData().containsKey(KEY)) {
            Meng_clockbuildereffect.DataContainer data = new Meng_clockbuildereffect.DataContainer();
            this.ship.setCustomData(KEY, data);
        }
        Meng_clockbuildereffect.DataContainer data = (Meng_clockbuildereffect.DataContainer) this.ship.getCustomData().get(KEY);
        if (ship.getAI() != null&&!data.timestop&&!data.aifire) {
            boolean use = false;
            if (this.ship.getShipTarget() != null) {
                float range = MathUtils.getDistance(this.ship.getLocation(), this.ship.getShipTarget().getLocation());
                if (Global.getSector().getPlayerFleet() != null && ship.getFleetMember() != null && ship.getFleetMember().getFleetData() != null && ship.getFleetMember().getFleetData().getFleet() != null) {
                    if (ship.getFleetMember().getFleetData().getFleet() == Global.getSector().getPlayerFleet()) {
                        use = (range <= 2000f && data.energy >= 7 && !this.ship.getSystem().isCoolingDown() && (this.ship.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.IN_ATTACK_RUN) || this.ship.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.BIGGEST_THREAT)));
                    } else {
                        use = (range <= 1500f && data.energy >= 10 && !this.ship.getSystem().isCoolingDown() && (!this.ship.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.HAS_INCOMING_DAMAGE) && this.ship.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.IN_ATTACK_RUN) || this.ship.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.BIGGEST_THREAT)));
                    }
                } else {
                    use = (range <= 2000f && data.energy >= 7 && !this.ship.getSystem().isCoolingDown() && (this.ship.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.IN_ATTACK_RUN) || this.ship.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.BIGGEST_THREAT)));
                }
            }
            if ((use))
                use(this.ship);
        }
    }
}


