package data.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.AfterShipCreationSkillEffect;
import com.fs.starfarer.api.characters.ShipSkillEffect;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import data.scripts.campaign.bar.Cai_Search;
import org.lazywizard.lazylib.combat.entities.SimpleEntity;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.Random;


public class Cai_skill {

    public static final int damage = 50;
    public static final int Damage = 15;


    public static class Level1 implements ShipSkillEffect, AfterShipCreationSkillEffect {
        public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
            ship.addListener(new Cai_skills_listener(ship));
        }

        public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
            ship.removeListenerOfClass(Cai_skills_listener.class);
        }


        public String getEffectDescription(float level) {
            return "  弹体武器命中目标时，有25%几率在自身附近虚空产生电弧射线，对目标造成75%穿盾伤害。";
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
            switch (Cai_Search.getStage()) {
                case Cai_Step1:
                    stats.getShieldDamageTakenMult().modifyMult(id, 0.85f);
                    stats.getEmpDamageTakenMult().modifyMult(id, 0.5f);
                    stats.getArmorBonus().modifyMult(id, 0.8f);
                    stats.getShieldUpkeepMult().modifyMult(id, 0.65f);
                    stats.getMaxSpeed().modifyMult(id, 0.85f);
                    stats.getMaxTurnRate().modifyMult(id, 0.65f);
                    stats.getEnergyWeaponDamageMult().modifyMult(id, 0.95f);
                    stats.getBallisticWeaponDamageMult().modifyMult(id, 0.95f);
                    stats.getMissileWeaponDamageMult().modifyMult(id, 0.95f);
                    break;
                case Cai_Step2:
                    stats.getEnergyWeaponDamageMult().modifyPercent(id, 15f);
                    stats.getBallisticWeaponDamageMult().modifyPercent(id, 15f);
                    stats.getMissileWeaponDamageMult().modifyPercent(id, 15f);
                    stats.getFluxCapacity().modifyPercent(id, 20f);
                    stats.getFluxDissipation().modifyPercent(id, 20f);

                    stats.getBallisticWeaponRangeBonus().modifyMult(id, 0.9f);
                    stats.getEnergyWeaponRangeBonus().modifyMult(id, 0.9f);
                    stats.getMissileWeaponRangeBonus().modifyMult(id, 0.9f);
                    stats.getBallisticWeaponFluxCostMod().modifyPercent(id, 15f);
                    stats.getEnergyWeaponFluxCostMod().modifyPercent(id, 15f);
                    stats.getMissileWeaponFluxCostMod().modifyPercent(id, 15f);
                    stats.getShieldDamageTakenMult().modifyPercent(id, 15f);
                    stats.getPhaseCloakUpkeepCostBonus().modifyPercent(id, 15f);
                    stats.getPeakCRDuration().modifyMult(id, 0.85f);
                    stats.getCRLossPerSecondPercent().modifyPercent(id, 50F);
                    break;
                case Cai_Step3:

                    break;
                case Cai_Step4:

                    break;
            }
        }

        public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {

            stats.getShieldDamageTakenMult().unmodifyMult(id);
            stats.getEmpDamageTakenMult().unmodifyMult(id);
            stats.getArmorBonus().unmodifyMult(id);
            stats.getShieldUpkeepMult().unmodifyMult(id);
            stats.getMaxSpeed().unmodifyMult(id);
            stats.getMaxTurnRate().unmodifyMult(id);
            stats.getEnergyWeaponDamageMult().unmodifyMult(id);
            stats.getBallisticWeaponDamageMult().unmodifyMult(id);
            stats.getMissileWeaponDamageMult().unmodifyMult(id);
            stats.getEnergyWeaponDamageMult().unmodifyPercent(id);
            stats.getBallisticWeaponDamageMult().unmodifyPercent(id);
            stats.getMissileWeaponDamageMult().unmodifyPercent(id);
            stats.getFluxCapacity().unmodifyPercent(id);
            stats.getFluxDissipation().unmodifyPercent(id);

            stats.getBallisticWeaponRangeBonus().unmodifyMult(id);
            stats.getEnergyWeaponRangeBonus().unmodifyMult(id);
            stats.getMissileWeaponRangeBonus().unmodifyMult(id);
            stats.getBallisticWeaponFluxCostMod().unmodifyPercent(id);
            stats.getEnergyWeaponFluxCostMod().unmodifyPercent(id);
            stats.getMissileWeaponFluxCostMod().unmodifyPercent(id);
            stats.getShieldDamageTakenMult().unmodifyPercent(id);
            stats.getPhaseCloakUpkeepCostBonus().unmodifyPercent(id);
            stats.getPeakCRDuration().unmodifyMult(id);
            stats.getCRLossPerSecondPercent().unmodifyPercent(id);

        }

        public String getEffectDescription(float level) {
            return "  小蔡利用复现的技术操纵有限的纳米机器人，可以组装为舰船部件为性能进行扩充，请在事件索引里进行选择。";
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
            return " ";
        }

        public String getEffectPerLevelDescription() {
            return null;
        }

        public ScopeDescription getScopeDescription() {
            return ScopeDescription.PILOTED_SHIP;
        }
    }


    public static class Cai_skills_listener implements DamageTakenModifier, AdvanceableListener {
        private final String id = "Caiskills1XD";
        protected ShipAPI ship;
        protected boolean inited = false;
        private float[][] maxrecover;
        private float[][] grid1;
        public Cai_skills_listener(ShipAPI ship) {
            this.ship = ship;
        }

        protected void init() {

        }

        public void advance(float amount) {
            if (Cai_Search.getStage() == null) return;
            if (!inited) {
                inited = true;
                grid1 = ship.getArmorGrid().getGrid();
                maxrecover = new float[grid1.length][grid1[0].length];
            } else {
                switch (Cai_Search.getStage()) {
                    case Cai_Step1:
                        float maxarm = ship.getArmorGrid().getMaxArmorInCell();
                        for (int x = 0; x < grid1.length; x++)
                            for (int y = 0; y < grid1[0].length; y++) {
                                if (maxrecover[x][y] <= maxarm * 2f) {
                                    if (grid1[x][y] < maxarm) {
                                        float dis = 0f;
                                        if (ship.getFluxTracker().isOverloaded() || ship.getFluxTracker().isVenting()) {
                                            dis = Math.min(maxarm - grid1[x][y], maxarm * 0.025f * amount * 0.5f);
                                        } else {
                                            dis = Math.min(maxarm - grid1[x][y], maxarm * 0.025f * amount);
                                        }
                                        ship.getArmorGrid().setArmorValue(x, y, grid1[x][y] + dis);
                                        maxrecover[x][y] += dis;
                                    }
                                }
                            }
                        break;
                    case Cai_Step2:

                        break;
                    case Cai_Step3:

                        break;
                    case Cai_Step4:

                        break;
                }
            }
            for (ShipAPI target : Global.getCombatEngine().getShips()) {
                if (target != null) {
                    if (!target.isFighter() && !target.isDrone() && target.getOwner() != ship.getOwner()) {
                        if (target.isAlive() && ship.isAlive()) {
                            if (!target.hasListenerOfClass(MyDamageListener1.class)) {
                                target.addListener(new MyDamageListener1(target, ship));
                            }
                        }
                    }
                }
            }
        }

        @Override
        public String modifyDamageTaken(Object param,
                                        CombatEntityAPI target, DamageAPI damage,
                                        Vector2f point, boolean shieldHit) {
            return null;
        }

        private static class MyDamageListener1 implements DamageListener {
            private final ShipAPI target;
            private final ShipAPI ship;

            public MyDamageListener1(ShipAPI targets, ShipAPI ships) {
                target = targets;
                ship = ships;
            }

            @Override
            public void reportDamageApplied(Object source, CombatEntityAPI targets, ApplyDamageResultAPI result) {
                if (source instanceof ShipAPI) {
                    if (source != ship) {
                        return;
                    }
                }
                if (!result.isDps() && result.getDamageToShields() >= 25f) {
                    if (!target.isDrone() && target.isAlive() && !target.isFighter()) {
                        float arg = (float) (360f * Math.random());
                        float range = target.getSpriteAPI().getWidth() / 2f + (float) (target.getSpriteAPI().getWidth() / 1.5f * Math.random());
                        Vector2f sourceloc = new Vector2f(ship.getLocation().getX() + (float) Math.cos(Math.toRadians(arg)) * range, ship.getLocation().getY() + (float) Math.sin(Math.toRadians(arg)) * range);
                        if(target.getAllWeapons().size()>0) {
                            CombatEntityAPI targetloc = new SimpleEntity(target.getAllWeapons().get(Math.max(0, (int) Math.round((target.getAllWeapons().size() - 1) * Math.random()))).getLocation());
                            float weight = new Random().nextFloat();
                            if (weight >= 0.75) {
                                Global.getCombatEngine().spawnEmpArcPierceShields(ship, sourceloc, targetloc, target, result.getType(), result.getDamageToShields() * 0.75f, 0f, 100000f, null, range * 0.05f, new Color(255, 20, 70, 185), new Color(100, 255, 240, 100));
                            }
                        }
                    }
                }
            }
        }
    }
}

