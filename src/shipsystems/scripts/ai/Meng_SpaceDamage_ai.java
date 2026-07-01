package data.shipsystems.scripts.ai;

import com.fs.starfarer.api.combat.*;
import data.shipsystems.scripts.Meng_MingGod_Spacedamage;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;

public class Meng_SpaceDamage_ai implements ShipSystemAIScript {
    private final String KEY1 = "Meng_MingGod_eyecharge_own";
    private CombatEngineAPI engine;
    private ShipAPI ship;

    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.engine = engine;
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
        if (!this.ship.getCustomData().containsKey(KEY1)) {
            Meng_MingGod_Spacedamage.Meng_MingGod_EyePlugin.DataContainer data1 = new Meng_MingGod_Spacedamage.Meng_MingGod_EyePlugin.DataContainer();
            this.ship.setCustomData(KEY1, data1);
        }
        Meng_MingGod_Spacedamage.Meng_MingGod_EyePlugin.DataContainer data1 = (Meng_MingGod_Spacedamage.Meng_MingGod_EyePlugin.DataContainer) this.ship.getCustomData().get(KEY1);
        if(data1.endtargets==null){
            data1.endtargets=new ArrayList<>();
        }
        if (data1.endtargets.size() > 0) {
            use(this.ship);
        }
    }
}
