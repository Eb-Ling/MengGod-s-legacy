package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.plugins.MagicRenderPlugin;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.Objects;

public class Meng_Karbala extends BaseHullMod {
    public static final String KEY = "MengEmplistener";
    private static final float transval = 20f;
    private final float weaponrange = 90f;
    private final float empmult = 0.1f;
    private final float fluxtransmult = 1f;
    IntervalUtil interval = new IntervalUtil(0.1f, 0.1f);

    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float opad = 10.0F;
        Color highlight = new Color(88, 213, 168, 218);
        tooltip.addSectionHeading("数据分析", Alignment.MID, opad);


        String s = String.valueOf(Math.round((1f - empmult) * 100f));
        String s2 = String.valueOf(Math.round(fluxtransmult * 100f));
        String s1 = s2;
        LabelAPI label = tooltip.addPara(
                "#装甲值清零。武器射程增加 %s%% 。\n#设计类型为虚想树的武器对结构造成伤害数值的 %s%% 会增加自身船体结构值和结构上限。\n" +
                        "\n#受到的EMP伤害减少 %s%% ，武器引擎修复时间减少 %s%% 。\n#不会过载。\n" +
                        "\n#结构受到伤害的 %s%% 会转化为硬幅能。\n#幅能值增加的 %s%% 会转化回复结构值，当幅能上升到X时达到阀值，其后幅能上升无法回复结构。\n（数值X为星舰当前的结构值上限）",
                opad, highlight, String.valueOf(Math.round(weaponrange)), String.valueOf(Math.round(transval)),
                s, s, s2, s1);
        label.setHighlight(String.valueOf(Math.round(weaponrange)), String.valueOf(Math.round(transval)),
                s, s, s2, s1);
        label.setHighlightColors(highlight, highlight, highlight, highlight, highlight, highlight, highlight, highlight, highlight, highlight, highlight);

    }


    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getBallisticWeaponRangeBonus().modifyPercent(id, weaponrange);
        stats.getEnergyWeaponRangeBonus().modifyPercent(id, weaponrange);
        stats.getEngineDamageTakenMult().modifyMult(id, empmult);
        stats.getWeaponDamageTakenMult().modifyMult(id, empmult);
        stats.getEmpDamageTakenMult().modifyMult(id, empmult);
        stats.getCombatWeaponRepairTimeMult().modifyMult(id, empmult);
        stats.getArmorBonus().modifyMult(id, 0f);
        //对舰船的各项免伤属性进行固定的调整。
    }

    public void advanceInCombat(ShipAPI ship, float amount) {

        ship.setDHullOverlay("graphics/Meng/hullmods/meng_cover.png");
        ship.clearDamageDecals();
        //清除不美观因素。
        FluxTrackerAPI tracker = ship.getFluxTracker();
        float flux1 = tracker.getCurrFlux() / tracker.getMaxFlux();
        if (ship.isRetreating()) {
            ship.setRetreating(false, false);
        }
        //检测当前幅能占总幅能的水平。
        if (tracker.isOverloaded()) tracker.stopOverload();
        MutableShipStatsAPI stats = ship.getMutableStats();
        ship.setNextHitHullDamageThresholdMult(ship.getMaxHitpoints() * 0.3f, 0f);
        //防止被阴间武器秒杀，将单次受到的伤害上限设置为总血量的0.3。
        String id = "Meng_emps";


        interval.advance(amount);
        if (interval.intervalElapsed()) {
            ship.setHitpoints(Math.min(ship.getMaxHitpoints(), ship.getHitpoints() + 50f));
        }
        if (!ship.getCustomData().containsKey(KEY)) {
            ship.setCustomData(KEY, new DataContainer());
        }
        if (!ship.hasListenerOfClass(MyDamageListener.class)) {
            ship.addListener(new MyDamageListener(ship));
        }
        DataContainer data = (DataContainer) ship.getCustomData().get(KEY);
        //为该舰船引入一个独属于它的数据库，防止全局变量冲突，并为它其挂上listener监听其所受伤害。

        float timer = data.timer;
        float timer1 = data.timer1;
        float timer2 = data.timer2;
        boolean init2 = data.init2;
        float arc = data.arc;
        data.timer = timer + Global.getCombatEngine().getElapsedInLastFrame();
        if (Objects.equals(ship.getHullSpec().getHullId(), "Meng_Xushu")) {
            SpriteAPI sprite = Global.getSettings().getSprite("Meng", "Meng_xuweapon1");
            MagicRenderPlugin.addSingleframe(sprite, ship.getLocation(), CombatEngineLayers.UNDER_SHIPS_LAYER);
            //利用Magiclib为舰船添加一个下层贴图（纯懒，其实可以做装饰武器）。
            if (ship.getSystem().getState() == ShipSystemAPI.SystemState.IN) {
                data.timer1 = timer + Global.getCombatEngine().getElapsedInLastFrame();
                if (timer1 >= 1f) {
                    sprite.setAlphaMult(0f);
                }
            } else {
                sprite.setAlphaMult(1F);
                data.timer1 = 0f;
            }
            //为贴图的透明度等属性进行不同状态下的定义，当系统（传送）启用时，贴图透明度变为0。

            if (ship.getPhaseCloak().getState() == ShipSystemAPI.SystemState.IN) {
                data.timer2 = timer2 + Global.getCombatEngine().getElapsedInLastFrame();
                sprite.setAngle(ship.getFacing() + 30f * timer + arc + 180f * timer2 * timer2);
                data.init2 = false;
                sprite.setAlphaMult(Math.min(0.7f + 0.4f * timer2 / 2f, 1f));
            } else {
                if (!init2) {
                    data.init2 = true;
                    data.arc = arc + 180f * timer2 * timer2;
                }
                sprite.setAngle(ship.getFacing() + 30f * timer - 180f * timer2 * timer2 + arc);
                data.timer2 = Math.max(timer2 - 0.5f * Global.getCombatEngine().getElapsedInLastFrame(), 0f);
                if (ship.getSystem().getState() != ShipSystemAPI.SystemState.IN) {
                    sprite.setAlphaMult(Math.min(0.7f + 0.4f * timer2 / 2f, 1f));
                }
            }
            //为贴图添加不同状态下的旋转属性，达成技能启用时旋转速度逐渐增加而后逐渐减慢的需求。
        }

        FluxTrackerAPI tracker1 = ship.getFluxTracker();
        tracker1.increaseFlux(fluxtransmult * data.flux, true);
        data.flux = 0f;
        //用listener监听来的伤害数值来达成只有一帧的幅能增长的效果，该条目也可以写在listener中。

        float lastflux = data.lastflux;
        if (ship.isAlive()) {
            if (ship.isRetreating() || ship.isDirectRetreat()) {
                ship.setRetreating(false, false);
            }
            float maxHull = ship.getMaxHitpoints();
            float fluxs = tracker.getCurrFlux();
            if (fluxs <= maxHull) {
                float hullbonus = fluxtransmult * (fluxs - lastflux);
                ship.setHitpoints(Math.min(Math.max((ship.getHitpoints() + hullbonus), ship.getHitpoints()), maxHull));
                if (hullbonus > 0f && ship.getHitpoints() < ship.getMaxHitpoints()) {
                    Global.getCombatEngine().addFloatingDamageText(new Vector2f(ship.getLocation().getX() - 200f, ship.getLocation().getY() + 200f), hullbonus, new Color(134, 255, 190, 255), ship, ship);
                }
            }
            data.lastflux = tracker.getCurrFlux();
        }
        //回复效果相关

        for (ShipAPI target : Global.getCombatEngine().getShips()) {
            if (target != null) {
                if (target.getOwner() != ship.getOwner() && !target.isDrone() && !target.isHulk() && !target.isFighter()) {
                    if (target.isAlive() && ship.isAlive()) {
                        if (!target.hasListenerOfClass(KarbalaDamageListener1.class)) {
                            target.addListener(new KarbalaDamageListener1(ship));
                        }
                    }
                }
            }
        }
        //添加监听,并在其受到伤害时为舰船增加结构上限
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (ship.getVariant().getHullMods().contains("targetingunit")) {
            ship.getVariant().removeMod("targetingunit");
            Global.getSoundPlayer().playUISound("cr_allied_warning", 1.0F, 1.0F);
        }
        if (ship.getVariant().getHullMods().contains("dedicated_targeting_core")) {
            ship.getVariant().removeMod("dedicated_targeting_core");
            Global.getSoundPlayer().playUISound("cr_allied_warning", 1.0F, 1.0F);
        }
    }

    private static class DataContainer {
        float lastflux;
        float flux;
        float timer = 0f;
        float timer1 = 0f;
        float timer2 = 0f;
        float arc = 0f;
        boolean init2 = false;
    }

    private static class MyDamageListener implements DamageListener {
        public ShipAPI ship;

        public MyDamageListener(ShipAPI ship) {
            this.ship = ship;
        }

        @Override
        public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {

            if (!this.ship.getCustomData().containsKey(KEY)) {
                this.ship.setCustomData(KEY, new Meng_Karbala.DataContainer());
            }
            Meng_Karbala.DataContainer data = (Meng_Karbala.DataContainer) this.ship.getCustomData().get(KEY);
            float flux = result.getDamageToHull();
            if (flux > 0f) {
                data.flux += flux;
                //将监听得到的舰体所受伤害数值输入data.flux。
            }
        }
    }

    private static class KarbalaDamageListener1 implements DamageListener {
        public ShipAPI ship;

        public KarbalaDamageListener1(ShipAPI ship) {
            this.ship = ship;
        }

        @Override
        public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
            if (ship == null || !ship.isAlive()) return;
            ShipAPI targets = (ShipAPI) target;
            if (targets.isAlive() && !targets.isHulk()) {
                if (source instanceof ShipAPI && source == ship) {
                    float hullbouns = result.getDamageToHull() * transval * 0.005f;
                    if (hullbouns > 0f) {
                        ship.setMaxHitpoints(ship.getMaxHitpoints() + hullbouns);
                        ship.setHitpoints(ship.getHitpoints() + hullbouns);
                    }
                }
            }
        }
    }
}










