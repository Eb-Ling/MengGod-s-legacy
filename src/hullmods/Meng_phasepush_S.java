package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Meng_phasepush_S extends BaseHullMod {

    public static final String KEY = "Mengphasepush_Slistener";

    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float opad = 10.0F;
        Color highlight = Misc.getHighlightColor();

        tooltip.addSectionHeading("数据分析", Alignment.MID, opad);


        LabelAPI label = tooltip.addPara(
                "每隔 20 秒，当舰船进入相位时，如果舰船结构值高于 30%% ，匪夷所思的力量会将两个维度彻底融合，从而使得现实的舰船周围召唤出一个本舰船的高维投影，高维投影拥有舰船当前 50%%的结构值与 35%%的幅能容量，且可继续召唤高维投影。所有分身将平分前一级本体的时间流速，当舰船被摧毁时，其所有分身均会消亡。",
                opad, highlight);
        label.setHighlight("20",
                "30%",
                "50%",
                "35%"
        );
        label.setHighlightColors(highlight, highlight, highlight, highlight, highlight);

        tooltip.addSectionHeading("舰船日志", Alignment.MID, opad);

        label = tooltip.addPara(
                "主啊，我将献祭所有的一切，只为进入您的神国。\n救赎的时刻到来了，我会拯救这些失去信仰的迷途羔羊。",
                opad, highlight);
        tooltip.addSectionHeading("关于船插", Alignment.MID, opad);

        label = tooltip.addPara(
                "这是尚未实装的boss船插，不到万不得已不要使用。 \n如果被闭眼填数据的阴间飞船恶心的话，用它恶心回去也是个不错的选择(笑)。",
                opad, highlight);
    }

    public void advanceInCombat(ShipAPI ship, float amount) {
        if (!ship.getCustomData().containsKey(KEY)) {
            ship.setCustomData(KEY, new Meng_phasepush_S.DataContainer());
        }

        Meng_phasepush_S.DataContainer data = (Meng_phasepush_S.DataContainer) ship.getCustomData().get(KEY);
        if (data.ships == null) {
            data.ships = new ArrayList();
        }
        if (data.allships == null) {
            data.allships = new ArrayList();
        }
        if (data.ships != null) {
            if (data.ships.size() >= 1) {
                if (!ship.isAlive()) {
                    for (ShipAPI target : data.ships) {
                        if (target != null) {
                            if (target.isAlive()) {
                                Global.getCombatEngine().applyDamage(target, target.getLocation(), 10000000f, DamageType.ENERGY, 0f, true, false, ship);
                            }
                        }
                    }
                }
            }
            if (data.ships.size() >= 1) {
                if (ship.isAlive()) {
                    for (ShipAPI target : data.ships) {
                        if (target != null) {
                            target.getMutableStats().getTimeMult().modifyMult("Meng_phasepushs", ship.getMutableStats().getTimeMult().getModifiedValue() / data.ships.size());

                        }
                    }
                }
            }
        }
        if (data.allships != null) {
            if (data.ships.size() >= 1) {
                for (ShipAPI target : data.allships) {
                    if (!target.isAlive())
                        data.ships.remove(target);
                }
            }
        }
        boolean player = false;
        player = ship == Global.getCombatEngine().getPlayerShip();
        boolean init = data.init;
        float timer = data.timer;
        boolean cooldown = data.cooldown;
        Object Meng_phasepush = data.Meng_phasepush;
        if (!cooldown) {
            data.timer = timer + Global.getCombatEngine().getElapsedInLastFrame();
            if (player) {
                Global.getCombatEngine().maintainStatusForPlayerShip(Meng_phasepush, "graphics/fx/Meng_sword.png", "维度坍缩冷却剩余", Math.round(20 - timer) + "秒", false);
            }
        } else {
            data.timer = 0f;
        }
        if (timer >= 20f) {
            data.cooldown = true;
        }
        if (ship.getHullSpec().isPhase()) {
            if (ship.isPhased()) {
                if (!init) {
                    data.init = true;

                    if (cooldown) {
                        CombatFleetManagerAPI manager = Global.getCombatEngine().getFleetManager(ship.getOwner());
                        if (ship.getHitpoints() >= ship.getMaxHitpoints() * 0.3f) {
                            if (data.ships.size() <= 5f) {
                                if (player) {

                                    FleetMemberAPI member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, ship.getVariant());
                                    ShipAPI newShip = Global.getCombatEngine().getFleetManager(ship.getOwner()).spawnFleetMember(member, ship.getMouseTarget(), ship.getFacing(), 0f);
                                    newShip.setHitpoints(ship.getHitpoints() * 0.5f);
                                    newShip.setCurrentCR(ship.getCurrentCR());
                                    newShip.setCRAtDeployment(ship.getCurrentCR());
                                    newShip.setControlsLocked(false);
                                    newShip.getMutableStats().getFluxCapacity().modifyMult("Meng_phasepushs", 0.35f);
                                    newShip.setAlphaMult(0.5f);
                                    data.ships.add(newShip);
                                    data.allships.add(newShip);
                                } else {
                                    FleetMemberAPI member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, ship.getVariant());
                                    ShipAPI newShip = Global.getCombatEngine().getFleetManager(ship.getOwner()).spawnFleetMember(member, new Vector2f(ship.getLocation().getX() + 500f, ship.getLocation().getY() + 500f), ship.getFacing(), 0f);
                                    newShip.setHitpoints(ship.getHitpoints() * 0.5f);
                                    newShip.setCurrentCR(ship.getCurrentCR());
                                    newShip.setCRAtDeployment(ship.getCurrentCR());
                                    newShip.setControlsLocked(false);
                                    newShip.getMutableStats().getFluxCapacity().modifyMult("Meng_phasepushs", 0.35f);
                                    newShip.setAlphaMult(0.5f);
                                    data.ships.add(newShip);
                                    data.allships.add(newShip);
                                }
                                data.cooldown = false;
                            }
                        }
                    }

                }
            } else {
                data.init = false;
            }
        }

    }

    public boolean isApplicableToShip(ShipAPI ship) {
        if (ship.getVariant().hasHullMod(HullMods.ADAPTIVE_COILS)) return false;
        return ship.getHullSpec().isPhase();
    }

    @Override
    public String getUnapplicableReason(ShipAPI ship) {
        if (ship.getVariant().hasHullMod(HullMods.ADAPTIVE_COILS)) {
            return "Incompatible with Adaptive Phase Coils";
        }
        if (!ship.getHullSpec().isPhase()) {
            return "Can only be installed on phase ships";
        }
        return super.getUnapplicableReason(ship);
    }

    private static class DataContainer {
        public float num = 0f;
        boolean init = false;
        List<ShipAPI> ships = null;
        List<ShipAPI> allships = null;
        float timer = 0f;
        boolean cooldown = false;
        Object Meng_phasepush = new Object();
    }

}
