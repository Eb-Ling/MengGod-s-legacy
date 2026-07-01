package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class Meng_phasepush extends BaseHullMod {

    public static final String KEY = "Mengphasepushlistener";
    private final Object Meng_phasepush = new Object();

    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float opad = 10.0F;
        Color highlight = Misc.getHighlightColor();

        tooltip.addSectionHeading("数据分析", Alignment.MID, opad);


        LabelAPI label = tooltip.addPara(
                "当舰船进入相位时，我们将两层空间之间转换的势能加以利用，使得舰船向原有的行进方向 推进一段距离 ，这个效果在 5 秒内只能生效一次。",
                opad, highlight);
        label.setHighlight("推进一段距离",
                "5");
        label.setHighlightColors(highlight, highlight, highlight, highlight, highlight);

        tooltip.addSectionHeading("圣殿史录", Alignment.MID, opad);

        label = tooltip.addPara(
                "万物的规则指引着我们，朝着神的方向。",
                opad, highlight);
    }

    public void advanceInCombat(ShipAPI ship, float amount) {
        if (!ship.getCustomData().containsKey(KEY)) {
            ship.setCustomData(KEY, new Meng_phasepush.DataContainer());
        }
        Meng_phasepush.DataContainer data = (Meng_phasepush.DataContainer) ship.getCustomData().get(KEY);
        boolean init = data.init;
        float timer = data.timer;
        boolean cooldown = data.cooldown;
        if (!cooldown) {
            data.timer = timer + Global.getCombatEngine().getElapsedInLastFrame();
            Global.getCombatEngine().maintainStatusForPlayerShip(Meng_phasepush, "graphics/fx/Meng_sword.png", "相位滑流冷却剩余", Math.round(5 - timer) + "秒", false);
        } else {
            data.timer = 0f;
        }
        if (timer >= 5f) {
            data.cooldown = true;
        }
        if (ship.getHullSpec().isPhase()) {
            if (ship.isPhased()) {
                if (!init) {
                    data.init = true;

                    if (cooldown) {
                        data.cooldown = false;
                        for (ShipEngineControllerAPI.ShipEngineAPI engine : ship.getEngineController().getShipEngines()) {
                            Global.getCombatEngine().addNebulaParticle(engine.getLocation(), new Vector2f(), 150f, 0.5f, 1f, 0.5f, 0.5f, new Color(87, 0, 65, 255));
                        }
                        CombatUtils.applyForce(ship, ship.getVelocity(), ship.getMass() * 2f);

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
        boolean init = false;
        float timer = 0f;
        boolean cooldown = true;
    }

}
