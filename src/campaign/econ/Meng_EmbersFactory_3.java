package data.campaign.econ;

import com.fs.starfarer.api.impl.campaign.econ.BaseHazardCondition;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public class Meng_EmbersFactory_3 extends BaseHazardCondition {

    public static final float HAZARD_BUFF = 0.5f;

    @Override
    public void apply(String id) {
        market.getHazard().modifyFlat(id, HAZARD_BUFF,"灾难痕迹");
    }

    @Override
    public void unapply(String id) {
        market.getHazard().unmodifyFlat(id);
    }

    @Override
    protected void createTooltipAfterDescription(TooltipMakerAPI tooltip, boolean expanded) {
        super.createTooltipAfterDescription(tooltip, expanded);

        String hazardinfo = "" + (int) (HAZARD_BUFF * 100f) + "%";

        tooltip.addPara("一种奇怪的物质散落于这颗星球，长期居住在这颗星球上的人总能听到莫名的低语。", 10f, Misc.getTextColor(), Misc.getHighlightColor(), hazardinfo);

    }

}
