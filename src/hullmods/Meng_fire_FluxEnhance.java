package data.hullmods;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

import com.fs.starfarer.api.plugins.DModAdderPlugin;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import org.magiclib.util.MagicSettings;

import java.awt.*;

public class Meng_fire_FluxEnhance extends BaseHullMod {
    public final float DAMAGE_MULT=MagicSettings.getFloat("Meng_fireset", "Meng_fire_FluxEnhance_DAMAGE_MULT");
    public final float SMOD_MULT=MagicSettings.getFloat("Meng_fireset", "Meng_fire_FluxEnhance_SMOD_MULT");
    public final String ids="Meng_fire_FluxEnhance_id";

    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float opad = 12.0F;
        Color highlight = new Color(255, 55, 40, 255);
        LabelAPI label1 = tooltip.addPara(
                "通过将舰船幅能系统与性能全面接轨，让舰船幅能系统的改装可以同步提高舰船整体性能。",
                opad, highlight);
        label1.setHighlight();
        label1.setHighlightColors(highlight, highlight, highlight, highlight, highlight, highlight, highlight);

        tooltip.addSectionHeading("数据分析", Alignment.MID, opad);

        LabelAPI label = tooltip.addPara(
                "#舰船每点超过幅能寄存器的幅能耗散器+ %s 武器伤害。\n#舰船每点超过幅能耗散器的幅能寄存器- %s 舰船受到的伤害。",
                opad, highlight, ""+DAMAGE_MULT+"%",""+DAMAGE_MULT+"%"
        );

        label.setHighlight(""+DAMAGE_MULT+"%", ""+DAMAGE_MULT+"%");
        label.setHighlightColors(highlight, highlight, highlight, highlight, highlight, highlight, highlight);

    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        int num=ship.getVariant().getNumFluxVents()-ship.getVariant().getNumFluxCapacitors();
        MutableShipStatsAPI stats=ship.getMutableStats();
        if(num>0){
            stats.getBallisticWeaponDamageMult().modifyPercent(ids,DAMAGE_MULT*num);
            stats.getEnergyWeaponDamageMult().modifyPercent(ids,DAMAGE_MULT*num);
            stats.getMissileWeaponDamageMult().modifyPercent(ids,DAMAGE_MULT*num);
        }
        else {
            stats.getShieldDamageTakenMult().modifyMult(ids,1f+num*(DAMAGE_MULT*0.01f));
            stats.getHullDamageTakenMult().modifyMult(ids,1f+num*(DAMAGE_MULT*0.01f));
            stats.getArmorDamageTakenMult().modifyMult(ids,1f+num*(DAMAGE_MULT*0.01f));
        }
        boolean sMod = isSMod(stats);
        if(sMod){
            stats.getBallisticWeaponDamageMult().modifyPercent(id,SMOD_MULT*ship.getVariant().getNumFluxVents());
            stats.getEnergyWeaponDamageMult().modifyPercent(id,SMOD_MULT*ship.getVariant().getNumFluxVents());
            stats.getMissileWeaponDamageMult().modifyPercent(id,SMOD_MULT*ship.getVariant().getNumFluxVents());
            stats.getShieldDamageTakenMult().modifyMult(id,1f-ship.getVariant().getNumFluxCapacitors()*(SMOD_MULT*0.01f));
            stats.getHullDamageTakenMult().modifyMult(id,1f-ship.getVariant().getNumFluxCapacitors()*(SMOD_MULT*0.01f));
            stats.getArmorDamageTakenMult().modifyMult(id,1f-ship.getVariant().getNumFluxCapacitors()*(SMOD_MULT*0.01f));
        }
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return ship.getVariant().getHullMods().contains("Meng_fire_core")||ship.getVariant().getHullMods().contains("Meng_fire_core_li");
    }

    public String getUnapplicableReason(ShipAPI ship) {
        if (!ship.getVariant().getHullMods().contains("Meng_fire_core")&&!ship.getVariant().getHullMods().contains("Meng_fire_core_li")) {
            return "只能用于火羽计划舰船";
        }
        return null;
    }
    @Override
    public boolean hasSModEffect() {
        return true;
    }

    public String getSModDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) return "" + SMOD_MULT + "%";
        if (index == 1) return "" + SMOD_MULT + "%";
        return null;
    }
}
