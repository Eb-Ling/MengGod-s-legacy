package data.shipsystems.scripts.ai;

import com.fs.starfarer.api.combat.*;
import data.hullmods.Meng_Dragonheart;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

public class Meng_DragonWingai implements ShipSystemAIScript {
    private ShipAPI ship;

    public static final String KEY = "Meng_Dragonheartlistener";
    private ShipwideAIFlags flags;

    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.flags = flags;

    }

    private void use(ShipAPI ship) {
        if (ship.getPhaseCloak() != null && ship.getPhaseCloak().getId().contentEquals("Meng_Xusystem")) {
            ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
        } else {
            ship.useSystem();
        }
    }

    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        boolean wantActiveSPREAD = (flags.hasFlag(ShipwideAIFlags.AIFlags.RUN_QUICKLY)
                || flags.hasFlag(ShipwideAIFlags.AIFlags.NEEDS_HELP)
                || flags.hasFlag(ShipwideAIFlags.AIFlags.PURSUING)
                || flags.hasFlag(ShipwideAIFlags.AIFlags.MOVEMENT_DEST)
                || flags.hasFlag(ShipwideAIFlags.AIFlags.MOVEMENT_DEST_WHILE_SIDETRACKED)
                || flags.hasFlag(ShipwideAIFlags.AIFlags.HARASS_MOVE_IN)
                || flags.hasFlag(ShipwideAIFlags.AIFlags.BACKING_OFF));
        if (!ship.getCustomData().containsKey(KEY)) {
            Meng_Dragonheart.DataContainer data1 = new Meng_Dragonheart.DataContainer();
            ship.setCustomData(KEY, data1);
        }
        Meng_Dragonheart.DataContainer data = (Meng_Dragonheart.DataContainer) ship.getCustomData().get(KEY);


        if (data.spread) {
            if (wantActiveSPREAD && !this.ship.getSystem().isActive()) {
                use(this.ship);
            }
        } else {
            if (target != null) {
                float range = MathUtils.getDistance(ship, target);
                if (range <= 2000f) {
                    if (!wantActiveSPREAD && !this.ship.getSystem().isActive()) {
                        use(this.ship);
                    }
                }
            }
        }
    }

}

