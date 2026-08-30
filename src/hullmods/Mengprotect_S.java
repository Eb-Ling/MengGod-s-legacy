package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.plugins.MagicRenderPlugin;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.Objects;

public class Mengprotect_S extends BaseHullMod {
    public static final String KEY = "Mengprotectslistener";

    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float opad = 10.0F;
        Color highlight = Misc.getHighlightColor();

        tooltip.addSectionHeading("数据分析", Alignment.MID, opad);


        LabelAPI label = tooltip.addPara(
                "高维的能量洗礼过他们的舰船，一旦 ？？##数据损坏#？？ ，神的意志便会于整条舰船上苏醒。",
                opad, highlight);
        label.setHighlight("？？##数据损坏#？？");
        label.setHighlightColors(highlight, highlight, highlight, highlight, highlight);

        tooltip.addSectionHeading("舰船日志", Alignment.MID, opad);

        label = tooltip.addPara(
                "在无尽黑暗的神国里漂泊，我们终于看到了光。",
                opad, highlight);

        tooltip.addSectionHeading("研究记录", Alignment.MID, opad);

        label = tooltip.addPara(
                "我们打捞到了这艘舰船，但它浸染的高维气息已经近乎逸散完毕。目前的效果为：当舰船过载时，将会取消过载且完全排散掉所有幅能，但是舰船会因为强行联系高位时空而受到6000点能量伤害。",
                opad, highlight);


    }

    public void advanceInCombat(ShipAPI ship, float amount) {
        float length = 52f;
        String id = "Meng_protectmodify";
        ArmorGridAPI armor = ship.getArmorGrid();
        if (ship.getVariant().getHullSpec().getShieldType() != ShieldAPI.ShieldType.NONE && ship.getShield() != null && ship.getVariant().getHullSpec().getShieldType() != ShieldAPI.ShieldType.PHASE) {
            ship.getShield().setRadius(ship.getShield().getRadius(), "graphics/shields/Meng_shields256.png", "graphics/fx/shields256ring.png");
            ship.getShield().setRingColor(new Color(11, 239, 229, 218));
            ship.getShield().setInnerColor(new Color(46, 159, 155, 218));
        }
        ship.setOverloadColor(new Color(11, 239, 229, 218));
        ship.setVentCoreColor(new Color(3, 28, 28, 218));
        ship.setVentFringeColor(new Color(46, 159, 155, 218));
        if (!ship.getCustomData().containsKey(KEY)) {
            Mengprotect_S.DataContainer data = new Mengprotect_S.DataContainer();

            data.cooldown = true;
            data.timer = 0f;
            ship.setCustomData(KEY, data);

        }
        Mengprotect_S.DataContainer data = (Mengprotect_S.DataContainer) ship.getCustomData().get(KEY);
        if (Objects.equals(ship.getHullSpec().getHullId(), "Meng_protect_boss1") && ship.isAlive()) {
            data.timer1 += amount;
            if (data.timer1 >= 3f) {
                data.timer1 = 0f;
                data.timer2 = 0f;
                data.init1 = false;
            }
            if (data.timer1 >= 0.25) {
                if (!data.init1) {
                    data.init1 = true;
                    data.timer2 = 0f;
                }
            }
            if (data.timer2 <= 0.15) {
                data.timer2 += amount;
                SpriteAPI sprite1 = Global.getSettings().getSprite("Meng", "Meng_protect_ship_light1");
                MagicRenderPlugin.addSingleframe(sprite1, new Vector2f(ship.getLocation().getX() + length * (float) Math.cos(Math.toRadians(ship.getFacing())), ship.getLocation().getY() + length * (float) Math.sin(Math.toRadians(ship.getFacing()))), CombatEngineLayers.ABOVE_SHIPS_LAYER);
                sprite1.setAngle(ship.getFacing() - 90f);
                sprite1.setAlphaMult(ship.getAlphaMult() * 0.95f);
            }
            ShipEngineControllerAPI engine = ship.getEngineController();
            if (engine.isAccelerating()) {
                data.currbrightness = Math.min(data.currbrightness + amount * 3f, 0.95f);
            } else if (engine.isAcceleratingBackwards()
                    || engine.isStrafingLeft()
                    || engine.isStrafingRight()) {
                data.currbrightness = Math.min(data.currbrightness + amount * 3f, 0.95f);
            } else {
                data.currbrightness = Math.max(0f, data.currbrightness - amount * 2f);
            }
            SpriteAPI sprite1 = Global.getSettings().getSprite("Meng", "Meng_protect_ship_light2");
            MagicRenderPlugin.addSingleframe(sprite1, new Vector2f(ship.getLocation().getX() + length * (float) Math.cos(Math.toRadians(ship.getFacing())), ship.getLocation().getY() + length * (float) Math.sin(Math.toRadians(ship.getFacing()))), CombatEngineLayers.ABOVE_SHIPS_LAYER);
            sprite1.setAngle(ship.getFacing() - 90f);
            sprite1.setAlphaMult(data.currbrightness);
        }
        boolean cooldown = data.cooldown;
        float timer = data.timer;
        if (timer >= 1.0f) {
            data.timer -= amount;
            data.cooldown = false;
        } else {
            data.cooldown = true;
        }
        if (Global.getSector().getPlayerFleet() != null) {
            if (!isInPlayerFleet(ship)) {
                if (cooldown) {
                    if (ship.isAlive()) {
                        ship.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK);
                        float maxFlux = ship.getMaxFlux();
                        float nowFlux = ship.getCurrFlux();
                        if (nowFlux >= maxFlux) {
                            data.timer = 45f;
                            ship.setJitterUnder(this, new Color(128, 0, 192, 255), 1.0F, 25, 0.0F, 15.0F);
                            FluxTrackerAPI tracker = ship.getFluxTracker();
                            float MaxHull = ship.getMaxHitpoints();
                            ship.setHitpoints(Math.min(ship.getHitpoints() + (MaxHull - ship.getHitpoints()) * 0.4f, MaxHull));
                            tracker.stopOverload();
                            tracker.setCurrFlux(0f);
                        }
                        if (ship.getShield() != null && ship.getVariant().getHullSpec().getShieldType() != ShieldAPI.ShieldType.NONE) {
                            if (!ship.getShield().isOn()) {
                                ship.getShield().toggleOn();
                            }
                        }
                        ship.getMutableStats().getTimeMult().unmodifyMult(id);
                        ship.getMutableStats().getMissileWeaponDamageMult().unmodifyMult(id);
                        ship.getMutableStats().getBallisticWeaponDamageMult().unmodifyMult(id);
                        ship.getMutableStats().getEnergyWeaponDamageMult().unmodifyMult(id);
                        ship.getMutableStats().getMaxTurnRate().unmodifyMult(id);
                        ship.getMutableStats().getMaxSpeed().unmodifyMult(id);
                    }
                } else {
                    if (ship.isAlive()) {
                        if (ship.getVariant().getHullSpec().getShieldType() != ShieldAPI.ShieldType.NONE) {
                            ship.getShield().toggleOff();
                        }
                        ship.setJitterUnder(this, new Color(128, 0, 192, 255), 1.0F, 25, 0.0F, 15.0F);
                        ship.getMutableStats().getTimeMult().modifyMult(id, 2f);
                        ship.getMutableStats().getMissileWeaponDamageMult().modifyMult(id, 1.3f);
                        ship.getMutableStats().getBallisticWeaponDamageMult().modifyMult(id, 1.3f);
                        ship.getMutableStats().getEnergyWeaponDamageMult().modifyMult(id, 1.3f);
                        ship.getMutableStats().getMaxTurnRate().modifyMult(id, 2f);
                        ship.getMutableStats().getMaxSpeed().modifyMult(id, 1.5f);
                    }
                }
            } else {
                toggleShieldField(ship);
            }
        } else {
            toggleShieldField(ship);
        }
    }

    private void toggleShieldField(ShipAPI ship) {
        if (ship.isAlive()) {
            if (ship.getAI() != null && ship.getShield() != null && ship.getVariant().getHullSpec().getShieldType() != ShieldAPI.ShieldType.NONE) {
                ship.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK);
                if (!ship.getShield().isOn()) {
                    ship.getShield().toggleOn();
                }
            }
            FluxTrackerAPI tracker = ship.getFluxTracker();
            float maxFlux = ship.getMaxFlux();
            float nowFlux = ship.getCurrFlux();
            if (nowFlux >= maxFlux) {
                tracker.setCurrFlux(0f);
                if (ship.getAllWeapons().size() > 0) {
                    Global.getCombatEngine().applyDamage(ship, ship.getAllWeapons().get(Math.max(0, (int) Math.round((ship.getAllWeapons().size() - 1) * Math.random()))).getLocation(), 6000, DamageType.ENERGY, 0f, true, false, ship, true);
                } else {
                    Global.getCombatEngine().applyDamage(ship, ship.getLocation(), 6000, DamageType.ENERGY, 0f, true, false, ship, true);
                }
                tracker.stopOverload();
            }
        }
    }

    public static class DataContainer {

        float timer;
        boolean cooldown;
        float timer1 = 3f;
        float timer2 = 0f;
        boolean init1 = false;
        float currbrightness = 0f;
    }


}