package data.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.AfterShipCreationSkillEffect;
import com.fs.starfarer.api.characters.ShipSkillEffect;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;


public class Meng_skill1 {

    public static final int mult = 150;

    public static class Level1 implements ShipSkillEffect, AfterShipCreationSkillEffect {
        public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
            ship.addListener(new Meng_skill1_listener(ship));
        }

        public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
            ship.removeListenerOfClass(Meng_skill1_listener.class);
        }


        public String getEffectDescription(float level) {
            return "当玩家的舰船处于毁灭的危险时，萌萌将会启动反维度发生器形成庇佑的力场，";
        }

        public String getEffectPerLevelDescription() {
            return null;
        }

        public ScopeDescription getScopeDescription() {
            return ScopeDescription.PILOTED_SHIP;
        }

        @Override
        public void apply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id, float level) {

        }

        @Override
        public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {

        }
    }

    public static class Level2 implements ShipSkillEffect {

        public void apply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id, float level) {

            stats.getAutofireAimAccuracy().modifyPercent(id, mult);
        }

        public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {

            stats.getAutofireAimAccuracy().unmodifyPercent(id);
        }

        public String getEffectDescription(float level) {
            return "+" + (mult) + "%自动射击精度，来自该舰船的弹体武器命中时均会生成一次10%破片伤害的裂隙爆炸。";
        }

        public String getEffectPerLevelDescription() {
            return null;
        }

        public ScopeDescription getScopeDescription() {
            return ScopeDescription.PILOTED_SHIP;
        }

    }

    public static class Level3 implements ShipSkillEffect {

        public void apply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id, float level) {
        }

        public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {
        }

        public String getEffectDescription(float level) {
            return "并无视自身安危进行强行跃迁，跨越空间来到你的面前。";
        }

        public String getEffectPerLevelDescription() {
            return null;
        }

        public ScopeDescription getScopeDescription() {
            return ScopeDescription.PILOTED_SHIP;
        }
    }


    public static class Meng_skill1_listener implements DamageTakenModifier, AdvanceableListener {
        private final IntervalUtil interval = new IntervalUtil(0.3f, 0.3f);
        protected ShipAPI ship;
        protected boolean inited = false;
        private float timer = 0f;
        private boolean active = false;
        private boolean boom = false;

        public Meng_skill1_listener(ShipAPI ship) {
            this.ship = ship;

        }

        protected void init() {
            inited = false;
            timer = 0f;
            active = false;
        }

        private void spawnMine(ShipAPI source, Vector2f mineLoc, DamageAPI damage) {
            CombatEngineAPI engine = Global.getCombatEngine();

            MissileAPI mine = (MissileAPI) engine.spawnProjectile(source, null, "Meng_Zerolayer", mineLoc, (float) Math.random() * 360f, null);

            if (source != null) {
                Global.getCombatEngine().applyDamageModifiersToSpawnedProjectileWithNullWeapon(source, WeaponAPI.WeaponType.MISSILE, false, damage);
            }

            float fadeInTime = 1f;
            mine.getVelocity().scale(0);
            mine.fadeOutThenIn(fadeInTime);

            float liveTime = 0f;
            //liveTime = 0.01f;
            mine.setFlightTime(mine.getMaxFlightTime() - liveTime);
            mine.addDamagedAlready(source);
            mine.setNoMineFFConcerns(true);
        }

        public void advance(float amount) {
            String id = "Meng_SKILL";
            ShipAPI player = Global.getCombatEngine().getPlayerShip();
            if (this.ship.isAlive()) {
                if (!boom) {
                    interval.advance(amount);
                }
                if (interval.intervalElapsed()) {
                    boom = true;
                }
                for (DamagingProjectileAPI proj : Global.getCombatEngine().getProjectiles()) {
                    if (proj.getSource() == this.ship) {
                        if (proj.didDamage() && boom) {
                            DamageAPI damage = proj.getDamage();
                            if (damage.getDamage() >= 150f) {
                                boom = false;
                                damage.setType(DamageType.FRAGMENTATION);
                                damage.setDamage(damage.getBaseDamage() * 0.1f);
                                spawnMine(this.ship, proj.getLocation(), damage);
                            }
                        }
                    }
                }
            }
            if (player != null && player.isAlive() && this.ship.isAlive() && player != this.ship) {
                if (player.getHitpoints() <= player.getMaxHitpoints() * 0.4f && !inited) {
                    active = true;
                    inited = true;
                }
                if (active) {
                    player.getMutableStats().getArmorDamageTakenMult().modifyMult(id, 0f);
                    player.getMutableStats().getHullDamageTakenMult().modifyMult(id, 0f);
                    this.ship.getMutableStats().getArmorDamageTakenMult().modifyMult(id, 0f);
                    this.ship.getMutableStats().getHullDamageTakenMult().modifyMult(id, 0f);
                    timer += amount;
                    if (timer <= 1f) {
                        this.ship.setJitter(this.ship, new Color(236, 164, 198, 255), 15f, 2, 20f * timer);
                    } else if (timer <= 2f) {
                        this.ship.getLocation().set(new Vector2f(player.getLocation().getX() - (player.getShieldRadiusEvenIfNoShield() * 1.5f) * (float) Math.cos(Math.toRadians(player.getFacing() - 90f)), player.getLocation().getY() - (player.getShieldRadiusEvenIfNoShield() * 1.5f) * (float) Math.sin(Math.toRadians(player.getFacing() - 90f))));
                        this.ship.setFacing(player.getFacing());
                        this.ship.setJitter(this.ship, new Color(236, 164, 198, 255), 15f, 2, 20f * (2f - timer));
                    }
                }
                if (timer >= 5f) {
                    player.getMutableStats().getArmorDamageTakenMult().unmodifyMult(id);
                    player.getMutableStats().getHullDamageTakenMult().unmodifyMult(id);
                    this.ship.getMutableStats().getArmorDamageTakenMult().unmodifyMult(id);
                    this.ship.getMutableStats().getHullDamageTakenMult().unmodifyMult(id);
                    active = false;
                }
            }
        }

        @Override
        public String modifyDamageTaken(Object param,
                                        CombatEntityAPI target, DamageAPI damage,
                                        Vector2f point, boolean shieldHit) {
            return null;
        }
    }
}

