package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import data.methods.Meng_OldEmpireRangeModifier;

public abstract class Meng_OldEmpireBaseHullmod extends BaseHullMod {
    protected abstract String ownId();
    protected abstract String[] category();
    protected abstract String categoryName();

    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (ship != null && !ship.hasListenerOfClass(Meng_OldEmpireRangeModifier.class)) {
            ship.addListener(new Meng_OldEmpireRangeModifier());
        }
    }

    public boolean isApplicableToShip(ShipAPI ship) {
        if (ship == null || ship.getVariant() == null) return false;
        for (String id : category()) {
            if (!ownId().equals(id) && ship.getVariant().hasHullMod(id)) return false;
        }
        return true;
    }

    public String getUnapplicableReason(ShipAPI ship) {
        return "同一艘舰船只能安装一个" + categoryName() + "插件";
    }
}
