package data.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.AfterShipCreationSkillEffect;
import com.fs.starfarer.api.characters.ShipSkillEffect;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.EnumSet;


public class Cai_skills {

    public static final int damage = 50;
    public static final int Damage = 15;
    public String id = "Cai_SKILL";

    public static class Level1 implements ShipSkillEffect, AfterShipCreationSkillEffect {
        public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
            ship.addListener(new Cai_skill_listener(ship));
        }

        public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
            ship.removeListenerOfClass(Cai_skill_listener.class);
        }


        public String getEffectDescription(float level) {
            return "  当小蔡驾驶舰船为战列/巡洋/驱逐/护卫舰时增加2000范围内友军的10%射程/10%幅能耗散/10%幅能容量/10%航速与机动性。\n  每个队友获得增益时提高1.5%电子战。";
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

        }

        public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {

        }

        public String getEffectDescription(float level) {
            return "  若小蔡与萌萌同时处于战场，当自身舰船护盾结构受损时，增加萌萌的武器伤害与幅能产生，效果可叠加并持续七秒。";
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


    public static class Cai_skill_listener implements DamageTakenModifier, AdvanceableListener {
        protected ShipAPI ship;
        protected boolean inited = false;
        private float timer = 0f;
        private boolean buffer = false;
        private float timers = 0f;
        private float totaldamage = 0f;
        private int buffnum = 0;

        public Cai_skill_listener(ShipAPI ships) {
            ship = ships;

        }

        protected void init() {
            inited = true;
        }

        public void advance(float amount) {
            if (ship == null || !ship.isAlive()) return;
            String id = "Cai_SKILL";
            String id1 = "Cai_SKILL1";
            ShipAPI target = null;
            int num = 0;
            for (ShipAPI teammates : Global.getCombatEngine().getShips()) {
                if (teammates.getFleetMember() != null && teammates.getFleetMember().getFleetData() != null && teammates.getFleetMember().getFleetData().getOfficersCopy() != null && !teammates.getFleetMember().getFleetData().getOfficersCopy().isEmpty()) {
                    if (teammates.getFleetMember().getCaptain().getStats().hasSkill("Meng_skill")) {
                        target = teammates;
                    }
                }
                if (teammates.getOwner() == ship.getOwner() && teammates.isAlive()) {
                    float d = Vector2f.sub(ship.getLocation(), teammates.getLocation(), new Vector2f()).length() - ship.getShieldRadiusEvenIfNoShield() - teammates.getShieldRadiusEvenIfNoShield();
                    if (d <= 2000f) {
                        num++;
                        float bonus = 10f * (2000f - d) / 2000f;
                        boolean isplayer = teammates == Global.getCombatEngine().getPlayerShip();
                        if (isplayer)
                            Global.getCombatEngine().maintainStatusForPlayerShip(id, "graphics/fx/Meng_logo1.png", "舰船对应属性提高", Math.round(bonus) + "%", false);

                        if (ship.getHullSize() == ShipAPI.HullSize.CAPITAL_SHIP) {
                            teammates.getMutableStats().getBallisticWeaponRangeBonus().modifyPercent(id, bonus);
                        } else if (ship.getHullSize() == ShipAPI.HullSize.CRUISER) {
                            teammates.getMutableStats().getFluxDissipation().modifyPercent(id, bonus);
                        } else if (ship.getHullSize() == ShipAPI.HullSize.DESTROYER) {
                            teammates.getMutableStats().getFluxCapacity().modifyPercent(id, bonus);
                        } else if (ship.getHullSize() == ShipAPI.HullSize.FRIGATE) {
                            teammates.getMutableStats().getMaxSpeed().modifyPercent(id, bonus);
                            teammates.getMutableStats().getAcceleration().modifyPercent(id, bonus);
                            teammates.getMutableStats().getMaxTurnRate().modifyPercent(id, bonus);
                            teammates.getMutableStats().getTurnAcceleration().modifyPercent(id, bonus);
                        }
                    }
                }
            }
            if (target != null) {
                timers += amount;
                if (timers >= 7f) {
                    timers = 0f;
                    totaldamage = 0f;
                }
                if (buffer) {
                    timer += amount;
                    Global.getCombatEngine().maintainStatusForPlayerShip(id1, "graphics/fx/Meng_logo1.png", "xxxx", 5 + "%", false);

                    float bonus = buffnum * 2.5f;
                    target.getMutableStats().getEnergyWeaponDamageMult().modifyPercent(id1, bonus);
                    target.getMutableStats().getBallisticWeaponDamageMult().modifyPercent(id1, bonus);
                    target.getMutableStats().getEnergyWeaponFluxCostMod().modifyPercent(id1, bonus * 0.4f);
                    target.getMutableStats().getBallisticWeaponFluxCostMod().modifyPercent(id1, bonus * 0.4f);
                    target.setWeaponGlow(buffnum / 20f, new Color(199, 17, 17, 255), EnumSet.allOf(WeaponAPI.WeaponType.class));

                } else {
                    target.getMutableStats().getEnergyWeaponDamageMult().unmodifyPercent(id1);
                    target.getMutableStats().getBallisticWeaponDamageMult().unmodifyPercent(id1);
                    target.getMutableStats().getEnergyWeaponFluxCostMod().unmodifyPercent(id1);
                    target.getMutableStats().getBallisticWeaponFluxCostMod().unmodifyPercent(id1);
                }
                if (timer >= 7f) {
                    timer = 0f;
                    buffer = false;
                }
            }
            if (!BaseSkillEffectDescription.isCivilian(ship.getMutableStats())) {
                ship.getMutableStats().getDynamic().getMod(Stats.ELECTRONIC_WARFARE_FLAT).modifyFlat(id, Math.min(30f, 1.5f * num));
            }
        }

        @Override
        public String modifyDamageTaken(Object param,
                                        CombatEntityAPI target, DamageAPI damage,
                                        Vector2f point, boolean shieldHit) {
            if (shieldHit) {
                timers = 0f;
                totaldamage += damage.getDamage();
                if (totaldamage >= ship.getMaxHitpoints() * 0.075f) {
                    if (!buffer) {
                        buffer = true;
                    }
                    timer = 0f;
                }
            } else {
                timers = 0f;
                totaldamage += damage.getDamage();
                if (totaldamage >= ship.getMaxHitpoints() * 0.075f) {
                    if (!buffer) {
                        buffer = true;
                    }
                    timer = 0f;
                }
            }
            buffnum = Math.round(totaldamage / (ship.getMaxFlux() * 0.1f));
            if (buffnum >= 20) {
                buffnum = 20;
            }
            return null;
        }
    }
}

