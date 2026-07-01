package data.hullmods;

import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.Objects;

public class Meng_Starcenter extends BaseHullMod {

    public static final String KEY = "Meng_Starcenter_core";
    private final String id = "Meng_Starcenter_1";

    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float opad = 10.0F;
        Color highlight = new Color(118, 255, 108, 218);
        Color highlight1 = new Color(224, 0, 0, 255);
        tooltip.addSectionHeading("数据分析", Alignment.MID, opad);


        LabelAPI label = tooltip.addPara(
                "舰船拥有近乎无尽的能源，战备值永远锁定在 70%%。\n舰船所移动的距离的 30%%会转换为幅能容量，且强制排散速率随之提升。\n舰船所有空余空间都将为星核熔炉供能，每空闲一个武器槽位，舰船实弹与能量武器射程增加 50。\n\n",
                opad, highlight, "20"

        );
        LabelAPI labels = tooltip.addPara(
                "星核熔炉可以从被战术系统击毁的舰船残骸处吸收逸散的能量，根据其舰船属性回复结构与装甲，并为战术系统再次充能。\nPS：战术系统结束后的传送位置为发动系统时鼠标位置，战术系统的首个目标应用R锁定选取。",
                opad, new Color(171, 255, 251, 218), "20"

        );
        labels.italicize();
        labels.setHighlight(0, 100);
        label.setHighlight("70%", "5000", "30%", "50");
        label.setHighlightColors(highlight, highlight, highlight, highlight, highlight1, highlight, highlight1, highlight, highlight, highlight, highlight);

        tooltip.addSectionHeading("舰船日志", Alignment.MID, opad);

        LabelAPI label1 = tooltip.addPara(
                "既然此后一路不能同行，便让我化作你眼中的星。",
                opad, highlight, "20%"
        );
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
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (!ship.getCustomData().containsKey(KEY)) {
            Meng_Starcenter.DataContainer data = new Meng_Starcenter.DataContainer();
            ship.setCustomData(KEY, data);
        }
        Meng_Starcenter.DataContainer data = (Meng_Starcenter.DataContainer) ship.getCustomData().get(KEY);
        if (isInPlayerFleet(ship)) {
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
        if (ship.getFleetMember() != null && ship.getFleetMember().getFleetData() != null && ship.getFleetMember().getFleetData().getOfficersCopy() != null && !ship.getFleetMember().getFleetData().getOfficersCopy().isEmpty()) {
            for (OfficerDataAPI meng : ship.getFleetMember().getFleetData().getOfficersCopy()) {
                if (meng.getPerson().getStats().hasSkill("Meng_skill")) {
                    ship.getSystem().setAmmo(5);
                }
            }
        }
        if (ship.getVariant().getHullSpec().getShieldType() != ShieldAPI.ShieldType.NONE && ship.getShield() != null && ship.getVariant().getHullSpec().getShieldType() != ShieldAPI.ShieldType.PHASE) {
            ship.getShield().setRadius(ship.getShield().getRadius(), "graphics/fx/shield/Meng_shields256.png", "graphics/fx/shields256ring.png");
            ship.getShield().setRingColor(new Color(0, 225, 255, 218));
        }
        boolean init1 = data.init1;
        Vector2f loc = data.loc;
        float flux = data.flux;
        float currbrightness = data.currbrightness;
        float timer = data.timer;
        ship.setCurrentCR(0.7f);
        boolean cooldown = data.cooldown;

        if (!init1) {
            data.init1 = true;
            data.loc = ship.getLocation();
            for (WeaponSlotAPI slot : ship.getHullSpec().getAllWeaponSlotsCopy()) {
                if (ship.getVariant().getWeaponSpec(slot.getId()) == null) {
                    data.rangemult++;
                }
            }
        }
        float d = Vector2f.sub(ship.getLocation(), loc, new Vector2f()).length();
        data.loc = new Vector2f(ship.getLocation().getX(), ship.getLocation().getY());
        data.flux = flux + d * 0.3f;
        ship.getMutableStats().getFluxCapacity().modifyFlat(id, flux);
        ship.getMutableStats().getVentRateMult().modifyPercent(id, flux / 500f);
        ship.getMutableStats().getEnergyWeaponRangeBonus().modifyFlat(id, 50f * data.rangemult);
        ship.getMutableStats().getBallisticWeaponRangeBonus().modifyFlat(id, 50f * data.rangemult);
        if (Objects.equals(ship.getHullSpec().getHullId(), "Meng_MouMengship")) {
            if (!ship.getSystem().isActive() && !ship.getSystem().isOn() && !ship.getSystem().isChargeup()) {
                for (WeaponAPI weapon : ship.getAllWeapons()) {
                    if (Objects.equals(weapon.getSpec().getWeaponId(), "Meng_Starshiplight")) {
                        SpriteAPI sprite = weapon.getSprite();
                        ShipEngineControllerAPI engine = ship.getEngineController();
                        if (engine.isAccelerating()) {
                            data.currbrightness = Math.min(currbrightness + amount * 3f, 0.95f);
                        } else if (engine.isAcceleratingBackwards()
                                || engine.isStrafingLeft()
                                || engine.isStrafingRight()) {
                            data.currbrightness = Math.min(currbrightness + amount * 3f, 0.95f);
                        } else {
                            data.currbrightness = Math.max(0f, currbrightness - amount * 2f);
                        }
                        if (timer >= 1.45f) {
                            data.cooldown = true;
                        }
                        if (timer <= -1f) {
                            data.cooldown = false;
                        }
                        if (cooldown) {
                            data.timer -= amount;
                        } else {
                            data.timer += amount;
                        }
                        sprite.setColor(new Color(255, 255, 255, Math.round(255 * data.currbrightness)));
                    }
                }
            }
        }
    }

    public static class DataContainer {
        private float rangemult = 0f;
        private float timer = 0f;
        private float flux = 0f;
        private boolean init1 = false;
        private Vector2f loc = new Vector2f();
        private boolean cooldown = false;
        private float currbrightness = 0f;

    }
}
