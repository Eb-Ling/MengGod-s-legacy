package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.boxutil.define.BoxEnum;
import org.boxutil.manager.CombatRenderingManager;
import org.boxutil.manager.ShaderCore;
import org.boxutil.units.standard.entity.TextFieldEntity;
import org.boxutil.units.standard.misc.ArcObject;
import org.boxutil.util.TransformUtil;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;
import java.util.Objects;

public class Mengtime extends BaseHullMod {
    public static final String KEY = "Mengtimelistener_hullmod";
    public static final String id = "Mengtimecenters";
    private float currbrightness = 0f;

    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float opad = 10.0F;
        Color highlight = Misc.getHighlightColor();

        tooltip.addSectionHeading("数据分析", Alignment.MID, opad);


        LabelAPI label = tooltip.addPara(
                "噬时之瓶时刻维持着过去 15秒 的叠加状态，因此将会消耗相当多的资源进行维护，舰船的维护费用提升 150%% 。\n\n噬时之瓶将会吞噬友军舰船的时间流速。\n#除时旅者外全体友方舰船时间流速降低 10%% 。\n#每当舰船使用一点噬时之瓶内存储的时间，峰值将会降低 8秒 。",
                opad, highlight);
        label.setHighlight("15秒", "150%", "10%",
                "8秒"
        );
        label.setHighlightColors(highlight, highlight, highlight, highlight, highlight);

        tooltip.addSectionHeading("舰船日志", Alignment.MID, opad);

        label = tooltip.addPara(
                "请珍惜你拥有的一切，哪怕是时间轴线的尽头，也总有无法夺回的过去。",
                opad, highlight);
        LabelAPI para = tooltip.addSectionHeading("阶梯计划", Alignment.MID, opad);
        para.setHighlightColors(new Color(0, 255, 221, 218));
        para.setHighlight(0, 100);

        label = tooltip.addPara(
                "阶梯计划在创立之初便做好了失控的应对，当舰队中同时出现一艘以上的阶梯计划舰船时，所有的阶梯计划舰船的系统都会全部下线。",
                opad, highlight);
        label.italicize();
        label.setHighlightColors(new Color(250, 0, 0, 255));
        label.setHighlight(0, 100);
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getSuppliesPerMonth().modifyMult(id, 2.5f);
        stats.getFuelUseMod().modifyMult(id, 2.5f);
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {

        if (ship.getVariant().getHullSpec().getShieldType() != ShieldAPI.ShieldType.NONE && ship.getShield() != null && ship.getVariant().getHullSpec().getShieldType() != ShieldAPI.ShieldType.PHASE) {
            ship.getShield().setRadius(ship.getShield().getRadius(), "graphics/fx/shield/Meng_TeleShipRing.png", "graphics/fx/shields256ring.png");
            ship.getShield().setRingColor(new Color(110, 170, 200, 255));
        }
        if (!isInPlayerFleet(ship)) {
            ship.getMutableStats().getMaxSpeed().modifyFlat(id, 25f);
            ship.getMutableStats().getAcceleration().modifyPercent(id, 30f);
            ship.getMutableStats().getTurnAcceleration().modifyPercent(id, 20f);
            ship.getMutableStats().getMaxTurnRate().modifyPercent(id, 20f);
            ship.getMutableStats().getPeakCRDuration().modifyPercent(id, 100f);
            ship.getMutableStats().getArmorBonus().modifyPercent(id, 15f);
            ship.getMutableStats().getShieldDamageTakenMult().modifyMult(id, 0.8f);
            ship.getMutableStats().getFluxCapacity().modifyPercent(id, 20f);
            ship.getMutableStats().getFluxDissipation().modifyPercent(id, 15f);
        } else {
            for (ShipAPI ships : Global.getCombatEngine().getShips()) {
                if (ships.getOwner() == ship.getOwner()) {
                    if (!ships.isFighter() || !ships.isDrone() && ships != ship) {
                        ships.getMutableStats().getTimeMult().modifyMult(id, 0.9f);
                    }
                }
            }
            PersonAPI fc = null;
            FleetMemberAPI member = ship.getFleetMember();
            if (member != null) {
                for (FleetMemberAPI mem : ship.getFleetMember().getFleetData().getMembersListCopy()) {
                    if (mem.getHullSpec() != member.getHullSpec() && mem.getHullSpec().getTags().contains("Meng_embers") && !mem.isMothballed()) {
                        ship.setCurrentCR(0f);
                    }
                }
            }
        }
        for (WeaponAPI weapon : ship.getAllWeapons()) {
            if (Objects.equals(weapon.getSpec().getWeaponId(), "Meng_timebosslight")) {
                SpriteAPI sprite = weapon.getSprite();
                sprite.setAdditiveBlend();
                ShipEngineControllerAPI e = ship.getEngineController();
                if (e.isAccelerating()) {
                    currbrightness = Math.min(currbrightness + amount * 3f, 0.95f);
                } else if (e.isAcceleratingBackwards()
                        || e.isStrafingLeft()
                        || e.isStrafingRight()) {
                    currbrightness = Math.min(currbrightness + amount * 3f, 0.95f);
                } else {
                    currbrightness = Math.max(0f, currbrightness - amount * 2f);
                }
                sprite.setColor(new Color(255, 255, 255, Math.round(255 * currbrightness)));
            }
        }
    }

}

