package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import java.awt.*;

public class Meng_DragonArmor extends BaseHullMod {
    public static final String KEY = "Meng_Dragonarmorlistener";
    public static final String id = "Meng_Dragonarmorsign";
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float opad = 12.0F;
        Color highlight = new Color(224, 0, 0, 255);
        LabelAPI label1 = tooltip.addPara(
                "舰船的装甲模块与本体相互依存，共享结构值，当装甲模块被摧毁时，舰船也会随之消亡。",
                opad, highlight, "20"
        );
        label1.setHighlight();
        label1.setHighlightColors(highlight, highlight, highlight, highlight, highlight, highlight, highlight);

        tooltip.addSectionHeading("数据分析", Alignment.MID, opad);


        LabelAPI label = tooltip.addPara(
                "#龙鳞装甲拥有更高的装甲值，但同样会使得本体扣除结构。",
                opad, highlight, "20"

        );
        label.setHighlight();
        label.setHighlightColors(highlight, highlight, highlight, highlight, highlight, highlight, highlight);

    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (Global.getCombatEngine() == null) return;
        float nowhit = ship.getHitpoints();
        if (!ship.getCustomData().containsKey(KEY)) {
            DataContainer data = new DataContainer();
            data.lasthit = ship.getHitpoints();
            for (ShipAPI m : ship.getChildModulesCopy()) {
                m.setHitpoints(nowhit);
                m.setMaxHitpoints(nowhit);
            }
            ship.setCustomData(KEY, data);
        }
        DataContainer data = (DataContainer) ship.getCustomData().get(KEY);
        if (ship.isAlive()) for (ShipAPI m : ship.getChildModulesCopy()) {
            m.setMaxHitpoints(ship.getMaxHitpoints());
            if (nowhit > data.lasthit) {
                m.setHitpoints(nowhit);
                continue;
              
            }
            if (m.getHitpoints() < nowhit) nowhit = m.getHitpoints();
            if (nowhit <= 0f) {
                Global.getCombatEngine().applyDamage(ship, ship.getLocation(), 9999999f, DamageType.ENERGY, 0f, true, false, null);
                return;
            }
            m.setHitpoints(nowhit);
        }
        ship.setHitpoints(nowhit);
        data.lasthit = nowhit;//保存数据

    }

    private static class DataContainer {
        float lasthit = 25000f;
    }
}