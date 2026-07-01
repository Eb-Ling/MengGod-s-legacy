package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;

public class MengMEC_S extends BaseHullMod {
    public static final String KEY = "Meng_MEC_s_core";

    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float opad = 10.0F;
        Color highlight = Misc.getHighlightColor();

        tooltip.addSectionHeading("数据分析", Alignment.MID, opad);


        LabelAPI label = tooltip.addPara(
                "幅能已经不再是衡量能量的单位了，一只脚跨入另一个维度的舰船将产生的所有幅能在时间轴上叠加，并作用于它的未来。",
                opad, highlight, "3");


        tooltip.addSectionHeading("舰船日志", Alignment.MID, opad);

        label = tooltip.addPara(
                "在实验成功的那一秒，我才知道一切都已经迟了。",
                opad, highlight);

        tooltip.addSectionHeading("关于船插", Alignment.MID, opad);

        label = tooltip.addPara(
                "这是尚未实装的boss船插，不到万不得已不要使用。 \n如果被闭眼填数据的阴间飞船恶心的话，用它恶心回去也是个不错的选择(笑)。",
                opad, highlight);


    }

    public void advanceInCombat(ShipAPI ship, float amount) {
        if (!ship.getCustomData().containsKey(KEY)) {
            MengMEC_S.DataContainer data = new MengMEC_S.DataContainer();
            ship.setCustomData(KEY, data);
        }
        MengMEC_S.DataContainer data = (MengMEC_S.DataContainer) ship.getCustomData().get(KEY);
        float flux = data.flux;
        FluxTrackerAPI tracker = ship.getFluxTracker();
        float nowflux = tracker.getCurrFlux();
        String id = "Meng_Mec_s";
        ship.getMutableStats().getFluxCapacity().modifyFlat(id, (nowflux - flux) * 0.5f);
        data.flux = nowflux;
    }

    public static class DataContainer {
        private float flux = 0f;
    }
}