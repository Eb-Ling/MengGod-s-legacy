package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicSettings;

import java.awt.*;
import java.util.ArrayList;

public class Meng_fire_SafeFlux extends BaseHullMod {

    public final float COOLDOWN=30f;
    public final String KEY = "Meng_fire_SafeFlux_key";
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float opad = 12.0F;
        Color highlight = new Color(255, 55, 40, 255);
        LabelAPI label1 = tooltip.addPara(
                "一种内置于舰船的安全协议，用以应对火羽舰船常常面临的过载的危急时刻。",
                opad, highlight);
        label1.setHighlight();
        label1.setHighlightColors(highlight, highlight, highlight, highlight, highlight, highlight, highlight);

        tooltip.addSectionHeading("数据分析", Alignment.MID, opad);

        LabelAPI label = tooltip.addPara(
                "#当舰船过载时，结束过载并立刻开始强制排散幅能。\n#当舰船强制排散幅能时，如果排散用时超过 20秒 ，立刻结束排散并降低 10%% 的硬幅能水平。\n#该效果拥有 %s秒 的冷却时间。",
                opad, highlight, String.valueOf(Math.round(COOLDOWN))
        );

        label.setHighlight("20秒","10"+"%",Math.round(COOLDOWN)+"秒");
        label.setHighlightColors(highlight, highlight, highlight, highlight, highlight, highlight, highlight);

    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        super.advanceInCombat(ship, amount);
        if(!ship.isAlive()) return;
        if (!ship.getCustomData().containsKey(KEY)) {
            DataContainer data = new DataContainer();
            ship.setCustomData(KEY, data);
        }
        DataContainer data = (DataContainer)ship.getCustomData().get(KEY);
        if(data.timer>0f){
            data.timer-=amount;
        }
        FluxTrackerAPI tracker=ship.getFluxTracker();
        if(tracker.isOverloaded()&&data.timer<=0f){
            data.timer=COOLDOWN;
            tracker.stopOverload();
            tracker.ventFlux();
            Global.getSoundPlayer().playSound("cr_allied_warning", 1.0F, 1.0F,ship.getLocation(),new Vector2f());
            Global.getCombatEngine().addFloatingText(new Vector2f(ship.getLocation().getX() + ship.getShieldRadiusEvenIfNoShield(), ship.getLocation().getY() + ship.getShieldRadiusEvenIfNoShield()), "火羽安全协议已生效！", ship.getShieldRadiusEvenIfNoShield()*0.15f, new Color(255, 55, 40, 255), ship, 1f, 2f);

            if(tracker.getTimeToVent()>=20f){
                tracker.stopVenting();
                float hflux=tracker.getHardFlux();
                tracker.setCurrFlux(tracker.getCurrFlux()-hflux*0.1f);
                tracker.setHardFlux(hflux*0.9f);
            }
        }
        if(tracker.isVenting()&&data.timer<=0f){
            if(tracker.getTimeToVent()>=20f){
                data.timer=COOLDOWN;
                Global.getCombatEngine().addFloatingText(new Vector2f(ship.getLocation().getX() + ship.getShieldRadiusEvenIfNoShield(), ship.getLocation().getY() + ship.getShieldRadiusEvenIfNoShield()), "火羽安全协议已生效！", ship.getShieldRadiusEvenIfNoShield()*0.15f, new Color(255, 55, 40, 255), ship, 1f, 2f);
                tracker.stopVenting();
                Global.getSoundPlayer().playSound("cr_allied_warning", 1.0F, 1.0F,ship.getLocation(),new Vector2f());
                float hflux=tracker.getHardFlux();
                tracker.setCurrFlux(tracker.getCurrFlux()-hflux*0.1f);
                tracker.setHardFlux(hflux*0.9f);
            }
        }
        if (ship == Global.getCombatEngine().getPlayerShip()) {
            if (data.timer > 0.0f)
                Global.getCombatEngine().maintainStatusForPlayerShip(KEY, "graphics/fx/Meng_logo1.png", "火羽安全协议剩余冷却时间:", Math.round(data.timer) + "秒", false);
            else {
                Global.getCombatEngine().maintainStatusForPlayerShip(KEY, "graphics/fx/Meng_logo1.png", "火羽安全协议", "已冷却", false);
            }
        }
    }
    public static class DataContainer {
        public float timer=0f;
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

}
