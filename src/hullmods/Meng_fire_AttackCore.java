package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import org.magiclib.util.MagicSettings;

import java.awt.*;

public class Meng_fire_AttackCore extends BaseHullMod {
    public final float ARMOR_MULT=MagicSettings.getFloat("Meng_fireset", "Meng_fire_AttackCore_ARMOR_MULT");
    public final float HULL_MULT=MagicSettings.getFloat("Meng_fireset", "Meng_fire_AttackCore_HULL_MULT");
    public final float FLUX_MULT=MagicSettings.getFloat("Meng_fireset", "Meng_fire_AttackCore_FLUX_MULT");
    public final float HFLUX_MULT=MagicSettings.getFloat("Meng_fireset", "Meng_fire_AttackCore_HFLUX_MULT");
    public final String ids="Meng_fire_AttackCore_id";

    public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float opad = 12.0F;
        Color highlight = new Color(255, 55, 40, 255);
        LabelAPI label1 = tooltip.addPara(
                "将舰船部分装甲板块改造为幅散系统，通过损失装甲结构值来增加幅能网络性能。",
                opad, highlight);
        label1.setHighlight();
        label1.setHighlightColors(highlight, highlight, highlight, highlight, highlight, highlight, highlight);

        tooltip.addSectionHeading("数据分析", Alignment.MID, opad);

        LabelAPI label = tooltip.addPara(
                "#舰船装甲值降低 %s ，结构值降低 %s 。\n#舰船幅能容量与幅能耗散提高 %s 。",
                opad, highlight, ""+(int)(ARMOR_MULT*100f)+"%",""+(int)(HULL_MULT*100f)+"%",""+(int)FLUX_MULT+"%"
        );

        label.setHighlight(""+(int)(ARMOR_MULT*100f)+"%",""+(int)(HULL_MULT*100f)+"%",""+(int)FLUX_MULT+"%");
        label.setHighlightColors(highlight, highlight, highlight, highlight, highlight, highlight, highlight);

    }

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getArmorBonus().modifyMult(id,1f-ARMOR_MULT);
        stats.getHullBonus().modifyMult(id,1f-HULL_MULT);
        stats.getFluxDissipation().modifyMult(id,1f+FLUX_MULT*0.01f);
        stats.getFluxCapacity().modifyMult(id,1f+FLUX_MULT*0.01f);
        boolean sMod = isSMod(stats);
        if(sMod){
            stats.getHardFluxDissipationFraction().modifyFlat(id, HFLUX_MULT);
        }
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return ship.getVariant().getHullMods().contains("Meng_fire_core");
    }

    public String getUnapplicableReason(ShipAPI ship) {
        if (!ship.getVariant().getHullMods().contains("Meng_fire_core")) {
            return "只能用于火羽计划舰船";
        }
        return null;
    }
    @Override
    public boolean hasSModEffect() {
        return true;
    }
    
    public String getSModDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) return "" + (int)(HFLUX_MULT*100f) + "%";
        return null;
    }
}
