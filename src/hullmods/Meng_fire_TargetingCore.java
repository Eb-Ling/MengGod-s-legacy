package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.WeaponBaseRangeModifier;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import org.magiclib.util.MagicSettings;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class Meng_fire_TargetingCore extends BaseHullMod {
    public final float RANGE_BONES= MagicSettings.getFloat("Meng_fireset", "Meng_fire_TargetingCore_RANGEBONES");
    private static final Map<ShipAPI.HullSize, Float> RANGE_THRESHOLD = new HashMap<>();
    static {
        RANGE_THRESHOLD.put(ShipAPI.HullSize.FRIGATE, 700f);
        RANGE_THRESHOLD.put(ShipAPI.HullSize.DESTROYER, 1000f);
        RANGE_THRESHOLD.put(ShipAPI.HullSize.CRUISER, 1300f);
        RANGE_THRESHOLD.put(ShipAPI.HullSize.CAPITAL_SHIP, 1600f);
    }
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float opad = 12.0F;
        Color highlight = new Color(255, 55, 40, 255);
        LabelAPI label1 = tooltip.addPara(
                "搭载虚子炉舰船专用的目标定位系统，足以一劳永逸地解决舰船武器射程参差不齐的问题。",
                opad, highlight);
        label1.setHighlight();
        label1.setHighlightColors(highlight, highlight, highlight, highlight, highlight, highlight, highlight);

        tooltip.addSectionHeading("数据分析", Alignment.MID, opad);

        LabelAPI label = tooltip.addPara(
                "#舰船的实弹与能量武器射程增加 %s%% 。\n#舰船武器射程按舰船大小分别无法超过 %s ， %s ， %s ， %s 。",
                opad, highlight, String.valueOf(Math.round(RANGE_BONES)), String.valueOf(Math.round(RANGE_THRESHOLD.get(ShipAPI.HullSize.FRIGATE))), String.valueOf(Math.round(RANGE_THRESHOLD.get(ShipAPI.HullSize.DESTROYER))), String.valueOf(Math.round(RANGE_THRESHOLD.get(ShipAPI.HullSize.CRUISER))), String.valueOf(Math.round(RANGE_THRESHOLD.get(ShipAPI.HullSize.CAPITAL_SHIP)))
        );

        label.setHighlight(Math.round(RANGE_BONES)+"%",String.valueOf(Math.round(RANGE_THRESHOLD.get(ShipAPI.HullSize.FRIGATE))), String.valueOf(Math.round(RANGE_THRESHOLD.get(ShipAPI.HullSize.DESTROYER))), String.valueOf(Math.round(RANGE_THRESHOLD.get(ShipAPI.HullSize.CRUISER))), String.valueOf(Math.round(RANGE_THRESHOLD.get(ShipAPI.HullSize.CAPITAL_SHIP))));
        label.setHighlightColors(highlight, highlight, highlight, highlight, highlight, highlight, highlight);

    }
    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        super.applyEffectsBeforeShipCreation(hullSize, stats, id);
        stats.getEnergyWeaponRangeBonus().modifyPercent(id,RANGE_BONES);
        stats.getBallisticWeaponRangeBonus().modifyPercent(id,RANGE_BONES);
    }
    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return !ship.getVariant().getHullMods().contains("dedicated_targeting_core") &&
                !ship.getVariant().getHullMods().contains(HullMods.DISTRIBUTED_FIRE_CONTROL) &&
                !ship.getVariant().getHullMods().contains("advancedcore")&&
                !ship.getVariant().getHullMods().contains(HullMods.INTEGRATED_TARGETING_UNIT)&&
                ship.getVariant().getHullMods().contains("Meng_fire_core")||
                ship.getVariant().getHullMods().contains("Meng_fire_core_li");
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        float range=RANGE_THRESHOLD.get(ship.getHullSize());
        for(WeaponAPI weapon:ship.getAllWeapons()){
            if(weapon.getRange()>=range) {
                if (weapon.getSpec().getMountType() != WeaponAPI.WeaponType.MISSILE) {
                    weapon.ensureClonedSpec();
                    weapon.getSpec().setMaxRange(weapon.getSpec().getMaxRange() - 1f);
                }
            }
        }
    }

    public String getUnapplicableReason(ShipAPI ship) {
        if (ship.getVariant().getHullMods().contains("dedicated_targeting_core")) {
            return "不兼容于 专注型目标锁定核心";
        }
        if (ship.getVariant().getHullMods().contains("advancedcore")) {
            return "不兼容于 先进目标定位核心";
        }
        if (ship.getVariant().getHullMods().contains(HullMods.DISTRIBUTED_FIRE_CONTROL)) {
            return "不兼容于 分布式火控系统";
        }
        if (ship.getVariant().getHullMods().contains(HullMods.INTEGRATED_TARGETING_UNIT)) {
            return "不兼容于 目标定位系统";
        }
        if (!ship.getVariant().getHullMods().contains("Meng_fire_core")) {
            return "只能用于火羽计划舰船";
        }
        return null;
    }

}
