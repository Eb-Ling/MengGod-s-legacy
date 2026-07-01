package data.hullmods;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import org.magiclib.util.MagicSettings;

import java.awt.*;

public class Meng_fire_HardArmor extends BaseHullMod {
    public final float FLUX_MULT=MagicSettings.getFloat("Meng_fireset", "Meng_fire_HardArmor_FluxPercent");
    public final float ARMOR_MULT=MagicSettings.getFloat("Meng_fireset", "Meng_fire_HardArmor_Armormult");
    public final float SHIELD_MULT=MagicSettings.getFloat("Meng_fireset", "Meng_fire_HardArmor_Shieldmult");

    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float opad = 12.0F;
        Color highlight = new Color(255, 55, 40, 255);
        LabelAPI label1 = tooltip.addPara(
                "这些被护圣部队些淘汰下来的幅化装甲，可以通过动能转化实现一些不算复杂的功能，同时也进一步强化了舰船的装甲水平。",
                opad, highlight);
        label1.setHighlight();
        label1.setHighlightColors(highlight, highlight, highlight, highlight, highlight, highlight, highlight);

        tooltip.addSectionHeading("数据分析", Alignment.MID, opad);

        String s = String.valueOf(Math.round(FLUX_MULT));
        LabelAPI label = tooltip.addPara(
                "#舰船的最大装甲值增加 %s%% 。\n#舰船硬幅能水平在装甲受损时降低，数额为装甲受到伤害的 %s 倍。\n#舰船护盾承受伤害增加 %s 。",
                opad, highlight, String.valueOf(Math.round(ARMOR_MULT * 100f)),s,""+(int)SHIELD_MULT+"%"
        );

        label.setHighlight(Math.round(ARMOR_MULT * 100f) + "%", s,""+(int)SHIELD_MULT+"%");
        label.setHighlightColors(highlight, highlight, highlight, highlight, highlight, highlight, highlight);

    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        super.applyEffectsBeforeShipCreation(hullSize, stats, id);
        stats.getArmorBonus().modifyPercent(id,ARMOR_MULT*100f);
        boolean sMod = isSMod(stats);
        if(!sMod) {
            stats.getShieldDamageTakenMult().modifyPercent(id, SHIELD_MULT);
        }
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        super.advanceInCombat(ship, amount);
        if (!ship.hasListenerOfClass(MyDamageListener1.class)) {
            ship.addListener(new MyDamageListener1(FLUX_MULT,ship));
        }
    }
    private static class MyDamageListener1 implements DamageListener {
        public ShipAPI ship;
        public float FLUX_MULT;
        public MyDamageListener1(float mult,ShipAPI s) {
            ship = s;
            FLUX_MULT=mult;
        }

        @Override
        public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
            float damage=result.getTotalDamageToArmor();
            FluxTrackerAPI tracker=ship.getFluxTracker();
            tracker.setHardFlux(Math.max(0f,tracker.getHardFlux()-damage*FLUX_MULT));
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

    public String getSModDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        return null;
    }
}
