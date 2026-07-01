package data.campaign.econ;

import com.fs.starfarer.api.impl.campaign.econ.BaseHazardCondition;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public class Meng_EmbersFactory_2 extends BaseHazardCondition {

    public static final float STABILITY_BUFF = 2f;

    @Override
    public void apply(String id) {
        market.getStability().modifyFlat(id,STABILITY_BUFF, "Combus的荣光");
    }

    @Override
    public void unapply(String id) {
        market.getStability().unmodifyFlat(id);
    }

    @Override
    protected void createTooltipAfterDescription(TooltipMakerAPI tooltip, boolean expanded) {
        super.createTooltipAfterDescription(tooltip, expanded);

        String stabilityinfo = " + " + (int) (STABILITY_BUFF);
        tooltip.addPara("数十年前废弃的Combus学院的遗址，某些未被拆解的基础设施依然可以运作，幸存的学员与教官重新汇集于此。\n\n"+stabilityinfo+" 稳定度", 10f, Misc.getTextColor(), Misc.getHighlightColor(), stabilityinfo);

    }

}
