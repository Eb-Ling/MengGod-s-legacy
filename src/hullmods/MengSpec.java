package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;

public class MengSpec extends BaseHullMod {
    public static final String KEY = "MengSpeclistener";

    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float opad = 10.0F;
        Color highlight = new Color(118, 255, 108, 218);
        Color highlight1 = new Color(255, 0, 0, 255);
        tooltip.addSectionHeading("数据分析", Alignment.MID, opad);


        LabelAPI label = tooltip.addPara(
                "利用时间轴的技术，舰船结构受到的所有伤害减少 %s%% 。\n但与此同时，不成熟的运用使得舰船结构部分坠入高维时空，禁用护盾，装甲值近乎 清零 ，硬幅能耗散速率减少 %s%% ，维持相位状态幅能增加 %s%% ，相位冷却时间增加 %s%% ，且结构受到伤害的 %s 倍转化为硬辐能。 当舰船过载或强制排散时，伤害减免消失。",
                opad, highlight, "80",
                "70",
                "20",
                "100",
                "4");
        label.setHighlight("80%",
                "清零",
                "70%",
                "20%",
                "100%",
                "4",
                "当舰船过载或强制排散时，伤害减免消失。");
        label.setHighlightColors(highlight, highlight1, highlight1, highlight1, highlight1, highlight1, highlight);

        tooltip.addSectionHeading("圣殿史录", Alignment.MID, opad);

        label = tooltip.addPara(
                "当一切意义在时间轴上拉伸，伤痛将不复存在。\n\n不兼容于 能质转换 。",
                opad, highlight);
        label.setHighlight("延展时空");
        highlight = Misc.getNegativeHighlightColor();
        label.setHighlightColors(highlight1, highlight1, highlight1, highlight1, highlight1);


    }

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getArmorBonus().modifyMult(id, 0f);
        stats.getPhaseCloakCooldownBonus().modifyPercent(id, 100f);
        stats.getPhaseCloakUpkeepCostBonus().modifyPercent(id, 20f);
    }

    public void advanceInCombat(ShipAPI ship, float amount) {
        MutableShipStatsAPI stats = ship.getMutableStats();
        if (!ship.getCustomData().containsKey(KEY)) {
            ship.setCustomData(KEY, new DataContainer());
        }
        if (!ship.hasListenerOfClass(MyDamageListener.class)) {
            ship.addListener(new MyDamageListener(ship));

        }

        DataContainer data = (DataContainer) ship.getCustomData().get(KEY);

        FluxTrackerAPI tracker = ship.getFluxTracker();
        if (!tracker.isOverloaded() && !tracker.isVenting()) {
            ship.setJitterUnder(this, new Color(0, 231, 185, 255), 1.0F, 25, 0.0F, 15.0F);

            stats.getHullDamageTakenMult().modifyMult("Meng_Specid", 0.2f);
            stats.getEmpDamageTakenMult().modifyMult("Meng_Specid", 0.2f);
            if (tracker.getHardFlux() <= data.hardflux) {
                tracker.setHardFlux(tracker.getHardFlux() + (data.hardflux - tracker.getHardFlux()) * 0.7f);
            }
        } else {
            stats.getHullDamageTakenMult().unmodifyMult("Meng_Specid");
            stats.getEmpDamageTakenMult().unmodifyMult("Meng_Specid");
        }
        data.hardflux = tracker.getHardFlux();
    }

    public boolean isApplicableToShip(ShipAPI ship) {
        if (ship.getVariant().getHullSpec().getShieldType() == ShieldAPI.ShieldType.NONE &&
                !ship.getVariant().hasHullMod("frontshield")) return false;
        if (ship.getVariant().hasHullMod("Meng_God_Spec")) return true;
        return ship.getShield() != null || ship.getVariant().getHullSpec().getShieldType() == ShieldAPI.ShieldType.PHASE;
    }

    public String getUnapplicableReason(ShipAPI ship) {
        return "Ship has no shields";
    }

    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ship.setShield(ShieldAPI.ShieldType.NONE, 0f, 1f, 1f);
        if (ship.getVariant().getHullMods().contains("Meng_God_MEC")) {
            ship.getVariant().removeMod("Meng_God_MEC");
            Global.getSoundPlayer().playUISound("cr_allied_warning", 1.0F, 1.0F);
        }
    }

    public static class DataContainer {

        float flux;
        float hardflux = 0f;
    }

    public static class MyDamageListener implements DamageListener {
        public ShipAPI ship;

        public MyDamageListener(ShipAPI ship) {
            this.ship = ship;
        }

        @Override
        public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {

            if (!this.ship.getCustomData().containsKey(KEY)) {
                this.ship.setCustomData(KEY, new DataContainer());
            }
            DataContainer data = (DataContainer) this.ship.getCustomData().get(KEY);
            data.flux = result.getDamageToHull();
            FluxTrackerAPI tracker = this.ship.getFluxTracker();
            tracker.increaseFlux(data.flux * 4f, true);

        }
    }


}