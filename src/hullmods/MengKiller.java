package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.combat.AIUtils;

import java.awt.*;

public class MengKiller extends BaseHullMod {
    public static final String KEY = "MengKillerlistener";

    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float opad = 10.0F;
        Color highlight = Misc.getHighlightColor();

        tooltip.addSectionHeading("数据分析", Alignment.MID, opad);


        LabelAPI label = tooltip.addPara(
                "在时间轴上钉死锁定舰船的结构，被锁定舰船的结构值回复效果降低 60%% 。\n但与此同时，被锁定舰船只受到的所有伤害降低 10%% 。",
                opad, highlight);
        label.setHighlight("60%",
                "10%");
        label.setHighlightColors(highlight, highlight, highlight, highlight, highlight);

        tooltip.addSectionHeading("圣殿史录", Alignment.MID, opad);

        label = tooltip.addPara(
                "神宣判了你的罪孽，便无处得以偿还。",
                opad, highlight);


    }

    public void advanceInCombat(ShipAPI ship, float amount) {
        ShipAPI target;
        if (ship == Global.getCombatEngine().getPlayerShip()) {
            target = ship.getShipTarget();
        } else {
            target = AIUtils.getNearestShip(ship);
        }

        if (!ship.getCustomData().containsKey(KEY)) {
            MengKiller.DataContainer data = new MengKiller.DataContainer();
            data.target = ship;
            ship.setCustomData(KEY, data);

        }
        MengKiller.DataContainer data = (MengKiller.DataContainer) ship.getCustomData().get(KEY);


        String id = "Meng_killerid";
        if (target != null && target.getOwner() != ship.getOwner()) {

            if (!data.init) {
                data.init = true;
                data.hullbonus = target.getHitpoints();
            }
            if (target.getHitpoints() >= data.hullbonus) {
                target.setHitpoints(target.getHitpoints() - (target.getHitpoints() - data.hullbonus) * 0.6f);
                target.getMutableStats().getHullDamageTakenMult().modifyMult(id, 0.9f);
                target.getMutableStats().getArmorDamageTakenMult().modifyMult(id, 0.9f);
                target.getMutableStats().getEmpDamageTakenMult().modifyMult(id, 0.9f);
            }
            if (data.target != target) {
                data.target.getMutableStats().getHullDamageTakenMult().unmodifyMult(id);
                data.target.getMutableStats().getArmorDamageTakenMult().unmodifyMult(id);
                data.target.getMutableStats().getEmpDamageTakenMult().unmodifyMult(id);
            }
            data.hullbonus = target.getHitpoints();
            data.target = target;
        } else {
            data.target.getMutableStats().getHullDamageTakenMult().unmodifyMult(id);
            data.target.getMutableStats().getArmorDamageTakenMult().unmodifyMult(id);
            data.target.getMutableStats().getEmpDamageTakenMult().unmodifyMult(id);
            data.init = false;
        }
    }

    public static class DataContainer {

        float hullbonus = 0f;
        boolean init = false;
        ShipAPI target;
    }


}