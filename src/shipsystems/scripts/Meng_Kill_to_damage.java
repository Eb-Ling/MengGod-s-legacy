package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import data.methods.Meng_hullsizeint;
import data.scripts.plugins.MagicRenderPlugin;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Meng_Kill_to_damage extends BaseShipSystemScript {
    List<ShipAPI> targets = null;
    float timer = 0f;

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = null;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
        } else {
            return;
        }
        if (targets == null) {
            targets = new ArrayList<>();
        }
        timer += Global.getCombatEngine().getElapsedInLastFrame();
        for (ShipAPI target : Global.getCombatEngine().getShips()) {
            if (target != null) {
                if (!target.isFighter() && !target.isDrone() && target.getOwner() != ship.getOwner()) {
                    if (target.isAlive() && ship.isAlive()) {
                        if (!target.hasListenerOfClass(MyDamageListener.class)) {
                            target.addListener(new MyDamageListener(ship, target));
                            targets.add(target);
                        }
                    }
                }
            }
        }
        SpriteAPI sprite = Global.getSettings().getSprite("Meng", "Meng_Yuanzhiring");
        MagicRenderPlugin.addSingleframe(sprite, ship.getLocation(),
                CombatEngineLayers.UNDER_SHIPS_LAYER);
        sprite.setSize(200f, 200f);
        sprite.setAlphaMult(effectLevel);
        sprite.setAngle(timer * 90f);
        stats.getEnergyWeaponDamageMult().modifyPercent(id, 80f);
        stats.getBallisticWeaponDamageMult().modifyPercent(id, 80f);
        boolean player = false;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
            player = ship == Global.getCombatEngine().getPlayerShip();
        }
        if (player) {
            if (ship.isAlive()) {
                Global.getCombatEngine().maintainStatusForPlayerShip("Meng_Yuanzhiring", "graphics/fx/Meng_logo1.png", "能量与实弹武器伤害增加", 50 + "%", false);
            }
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship = null;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
        } else {
            return;
        }
        stats.getEnergyWeaponDamageMult().unmodifyPercent(id);
        stats.getBallisticWeaponDamageMult().unmodifyPercent(id);
        if (targets != null && targets.size() != 0) {
            for (ShipAPI target : targets) {
                if (target != null) {
                    if (target.hasListenerOfClass(MyDamageListener.class)) {
                        target.removeListener(new MyDamageListener(ship, target));
                    }
                }
            }
        }
        targets = null;
    }

    private static class MyDamageListener implements DamageListener {
        public ShipAPI ships;
        public ShipAPI targets;

        private boolean init = false;

        public MyDamageListener(ShipAPI ship, ShipAPI target) {
            ships = ship;
            targets = target;
        }

        @Override
        public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
            String id = "Meng_Killtodamage";
            if (ships == null || targets == null || ships.isAlive() || targets.isAlive()) {
                if (source instanceof ShipAPI) {
                    if (source == ships) {
                        if (result.getDamageToHull() >= targets.getHitpoints()) {
                            if (!init) {
                                init = true;
                                Meng_hullsizeint num = new Meng_hullsizeint();
                                if (ships.getMutableStats().getPeakCRDuration().getFlatBonus(id) != null) {
                                    ships.getMutableStats().getPeakCRDuration().modifyFlat(id, ships.getMutableStats().getPeakCRDuration().getFlatBonus(id).value + Meng_hullsizeint.Getsize(targets) * 5f / ships.getMutableStats().getPeakCRDuration().getMult() / (100f + ships.getMutableStats().getPeakCRDuration().getPercentMod()) * 100f);
                                } else {
                                    ships.getMutableStats().getPeakCRDuration().modifyFlat(id, Meng_hullsizeint.Getsize(targets) * 5f / ships.getMutableStats().getPeakCRDuration().getMult() / (100f + ships.getMutableStats().getPeakCRDuration().getPercentMod()) * 100f);
                                }
                                Global.getCombatEngine().addFloatingText(new Vector2f(ships.getLocation().getX() - 200f, ships.getLocation().getY() + 200f), "峰值时间增加" + Meng_hullsizeint.Getsize(targets) * 5f + "秒", 50f, new Color(169, 234, 255, 255), ships, 2f, 2f);
                            }
                        }
                    }
                }
            }
        }
    }
}
