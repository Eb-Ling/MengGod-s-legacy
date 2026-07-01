package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.plugins.MagicRenderPlugin;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Meng_Xusystem extends BaseShipSystemScript {

    List<ShipAPI> targets = null;
    List<ShipAPI> targets1 = null;
    private boolean init = false;
    private boolean init1 = false;
    private float timer = 0f;
    private CombatEngineAPI engine;

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {

        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) {
            return;
        }
        if (ship.getPhaseCloak().getState() == ShipSystemAPI.SystemState.IN || ship.getPhaseCloak().getState() == ShipSystemAPI.SystemState.OUT) {
            return;
        }
        timer += Global.getCombatEngine().getElapsedInLastFrame();

        if (!init) {
            init = true;
            Global.getSoundPlayer().playSound("Meng_Xusound", 1f, 1f, ship.getLocation(), new Vector2f());
            RippleDistortion ripple = new RippleDistortion(ship.getLocation(), new Vector2f());
            ripple.setSize(30000f);
            ripple.setIntensity(1000f);
            ripple.fadeInSize(12f);
            ripple.fadeInIntensity(5f);
            ripple.setFrameRate(60f);
            DistortionShader.addDistortion(ripple);
        }
        SpriteAPI sprite0 = Global.getSettings().getSprite("Meng", "Meng_system0");
        SpriteAPI sprite1 = Global.getSettings().getSprite("Meng", "Meng_system1");


        if (timer <= 3f) {
            MagicRenderPlugin.addSingleframe(sprite0, ship.getLocation(), CombatEngineLayers.ABOVE_SHIPS_LAYER);
            MagicRenderPlugin.addSingleframe(sprite1, ship.getLocation(), CombatEngineLayers.ABOVE_SHIPS_LAYER);
            sprite0.setSize(Math.min(4400 * timer * 2f, 4400), Math.min(4400 * timer * 2f, 4400));
            sprite0.setAngle(360);
            sprite0.setAlphaMult(Math.min(timer / 2, 0.5f));
            sprite1.setSize(Math.min(4400 * timer * 2f, 4400), Math.min(4400 * timer * 2f, 4400));
            sprite1.setAngle(-40 * timer);
            sprite1.setAlphaMult(Math.min(timer / 2, 0.5f));
        } else {
            if (timer <= 5f) {
                MagicRenderPlugin.addSingleframe(sprite0, ship.getLocation(), CombatEngineLayers.ABOVE_SHIPS_LAYER);
                MagicRenderPlugin.addSingleframe(sprite1, ship.getLocation(), CombatEngineLayers.ABOVE_SHIPS_LAYER);
                sprite0.setSize(4400, 4400);
                sprite0.setAngle(360);
                sprite0.setAlphaMult(Math.min(0.5f - (timer - 3) / 4, 0.5f));
                sprite1.setSize(4400, 4400);
                sprite1.setAlphaMult(Math.min(0.5f - (timer - 3) / 4, 0.5f));
                sprite1.setAngle(-40 * timer);
            }
        }


        if (effectLevel < 1f) return;

        if (this.engine == null) {
            this.engine = Global.getCombatEngine();
        }
        if (targets == null) {
            targets = new ArrayList();
        }
        if (targets1 == null) {
            targets1 = new ArrayList();
        }

        if (targets1 != null) {

            ShipAPI target2 = findTarget1(ship);
            if (target2 != null && !targets1.contains(target2)) targets1.add(target2);

            for (ShipAPI target : targets1) {

                MutableShipStatsAPI stat = target.getMutableStats();

                stat.getMaxSpeed().modifyMult(id, 0f);
                stat.getTimeMult().modifyMult(id, 0.3f);
                stat.getMaxTurnRate().modifyMult(id, 0f);
                target.setJitterUnder(this, new Color(206, 77, 241, 218), 1.0F, 25, 0.0F, 10f);
            }
        }
        if (!init1) {
            init1 = true;
            for (MissileAPI target3 : AIUtils.getNearbyEnemyMissiles(ship, 2000f)) {
                DamageAPI damage = target3.getDamage();

                if (target3 != null) {
                    if (target3.getOwner() != ship.getOwner()) {
                        spawnMine(ship, target3.getLocation(), damage);
                        target3.setHitpoints(0F);
                    }
                }
            }

        }

        if (targets != null) {

            ShipAPI target1 = findTarget(ship);
            if (target1 != null && !targets.contains(target1)) targets.add(target1);

            for (ShipAPI target : targets) {

                MutableShipStatsAPI stat = target.getMutableStats();

                stat.getMaxSpeed().modifyMult(id, 0f);
                stat.getTimeMult().modifyMult(id, 0.3f);
                stat.getMaxTurnRate().modifyMult(id, 0f);
                target.setJitterUnder(this, new Color(54, 255, 214, 218), 1.0F, 25, 0.0F, 15f);
            }
        }

    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        timer = 0f;
        init = false;
        init1 = false;
        ShipAPI ship = (ShipAPI) stats.getEntity();

        if (ship == null) {
            return;
        }

        if (targets != null) {
            for (ShipAPI target : targets) {

                MutableShipStatsAPI stat = target.getMutableStats();

                stat.getMaxSpeed().unmodifyMult(id);
                stat.getTimeMult().unmodifyMult(id);
                stat.getMaxTurnRate().unmodifyMult(id);
            }

            targets = null;
        }
        if (targets1 != null) {
            for (ShipAPI target : targets1) {

                MutableShipStatsAPI stat = target.getMutableStats();

                stat.getMaxSpeed().unmodifyMult(id);
                stat.getTimeMult().unmodifyMult(id);
                stat.getMaxTurnRate().unmodifyMult(id);
            }

            targets1 = null;
        }
    }

    private ShipAPI findTarget(ShipAPI ship) {

        WeightedRandomPicker<ShipAPI> targets = new WeightedRandomPicker<>();

        for (ShipAPI target : AIUtils.getNearbyEnemies(ship, 2000f)) {
            if (target.isFighter() || target.isDrone()) continue;

            targets.add(target);
        }

        return targets.pick();
    }

    private ShipAPI findTarget1(ShipAPI ship) {

        WeightedRandomPicker<ShipAPI> targets1 = new WeightedRandomPicker<>();

        for (ShipAPI target : AIUtils.getNearbyEnemies(ship, 2000f)) {
            if (target.isFighter() || target.isDrone()) {

                targets1.add(target);
            }
        }
        return targets1.pick();
    }

    public ShipSystemStatsScript.StatusData getStatusData(int index, ShipSystemStatsScript.State state, float effectLevel) {
        if ((state == ShipSystemStatsScript.State.IN) || (state == ShipSystemStatsScript.State.ACTIVE)) {
            if (index == 0)
                return new ShipSystemStatsScript.StatusData("时空领域已开启", false);
        }


        return null;
    }

    private void spawnMine(ShipAPI source, Vector2f mineLoc, DamageAPI Damage1) {
        CombatEngineAPI engine = Global.getCombatEngine();


        //Vector2f currLoc = mineLoc;
        MissileAPI mine = (MissileAPI) engine.spawnProjectile(source, null,
                "riftbeam_minelayer",
                mineLoc,
                (float) Math.random() * 360f, null);
        if (source != null) {
            Global.getCombatEngine().applyDamageModifiersToSpawnedProjectileWithNullWeapon(
                    source, WeaponAPI.WeaponType.MISSILE, false, Damage1);
        }


        float fadeInTime = 0.05f;
        mine.getVelocity().scale(0);
        mine.fadeOutThenIn(fadeInTime);

        float liveTime = 0f;
        //liveTime = 0.01f;
        mine.setFlightTime(mine.getMaxFlightTime() - liveTime);
        mine.addDamagedAlready(source);
        mine.setNoMineFFConcerns(true);
    }

}

