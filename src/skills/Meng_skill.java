package data.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.AfterShipCreationSkillEffect;
import com.fs.starfarer.api.characters.ShipSkillEffect;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import data.scripts.campaign.bar.MouMeng_Search;
import data.scripts.campaign.bar.MengSearch;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;


public class Meng_skill {

    public static final int damage = 50;
    public static final int Damage = 15;


    public static class Level1 implements ShipSkillEffect, AfterShipCreationSkillEffect {
        public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
            ship.addListener(new Meng_skill_listener(ship));
        }

        public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
            ship.removeListenerOfClass(Meng_skill_listener.class);
        }


        public String getEffectDescription(float level) {
            return "萌萌用不知从何得来的神秘技术对舰船进行了半自动化改装，她似乎可以将神经连接至整艘舰船。\n但是由于对舰船的修改，舰船交战盾效降低，舰船幅能容量、结构值分别减少20%、10%。\n                                                                                       —\"一同欣赏吧，这是学院残留的荣光。\" ";
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
            float bonus = damage;

            stats.getShieldTurnRateMult().modifyPercent(id,100f);
            stats.getShieldUnfoldRateMult().modifyPercent(id,100f);
            stats.getDamageToTargetWeaponsMult().modifyPercent(id, bonus);
            stats.getDamageToTargetEnginesMult().modifyPercent(id, bonus);
            stats.getOverloadTimeMod().modifyMult(id,0.5f);
        }

        public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {

            stats.getDamageToTargetWeaponsMult().unmodifyPercent(id);
            stats.getDamageToTargetEnginesMult().unmodifyPercent(id);
            stats.getShieldTurnRateMult().unmodifyPercent(id);
            stats.getShieldUnfoldRateMult().unmodifyPercent(id);
            stats.getOverloadTimeMod().unmodifyMult(id);

        }

        public String getEffectDescription(float level) {
            return "+" + (damage) + "%对武器和引擎的伤害，+100%护盾灵敏度，-50%过载时间。    —\"神经接驳装置已连接。\"";
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
            stats.getFluxCapacity().modifyMult(id, 0.80f);
            stats.getHullBonus().modifyMult(id, 0.90f);

        }

        public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {
            stats.getFluxCapacity().unmodifyMult(id);
            stats.getHullBonus().unmodifyMult(id);
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


    public static class Meng_skill_listener implements DamageTakenModifier, AdvanceableListener {
        protected ShipAPI ship;
        protected boolean inited = false;

        public Meng_skill_listener(ShipAPI ship) {
            this.ship = ship;

        }

        protected void init() {
            inited = true;
        }

        public void advance(float amount) {
            String id = "Meng_SKILL";

            FluxTrackerAPI tracker = ship.getFluxTracker();
            MutableShipStatsAPI stats = ship.getMutableStats();
            float flux = tracker.getCurrFlux() / tracker.getMaxFlux();
            if (flux > 0f) {
                ship.addAfterimage(new Color(255, Math.max(0, Math.min(255, 160 - Math.round(flux * 140))), Math.max(0, Math.min(255, 220 - Math.round(flux * 50))), Math.max(0, Math.min(255, 160 + Math.round(flux * 80)))), 0f, 0f, -ship.getVelocity().x, -ship.getVelocity().y, 0f, 0f, 0.03f * flux, 0.5f * flux, true, false, false);
                ship.setJitterUnder(ship, new Color(255, Math.max(0, Math.min(255, 148 - Math.round(flux * 140))), Math.max(0, Math.min(255, 205 - Math.round(flux * 50))), 255), 1.0F, 25, 0.0F, 0f + Math.round(flux * 30f));
            }

            stats.getTimeMult().modifyMult(id, 0.9f + Math.round(10f*flux * 0.6f)*0.1f);
            stats.getFluxDissipation().modifyMult(id, 0.9f + flux * 0.35f);
            stats.getBallisticWeaponDamageMult().modifyPercent(id, 0.9f + flux * 0.3f);
            stats.getShieldDamageTakenMult().modifyMult(id, 1f + flux * 0.2f);
            stats.getEnergyWeaponDamageMult().modifyPercent(id, 0.9f + flux * 0.3f);
            stats.getMaxSpeed().modifyPercent(id, 1f + flux * 0.3f);


        }

        @Override
        public String modifyDamageTaken(Object param,
                                        CombatEntityAPI target, DamageAPI damage,
                                        Vector2f point, boolean shieldHit) {
            return null;
        }
    }
}

