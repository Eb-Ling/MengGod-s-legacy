package data.campaign.econ;

import com.fs.starfarer.api.impl.campaign.econ.BaseHazardCondition;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public class Meng_EmbersFactory_1 extends BaseHazardCondition {

    public static final float HAZARD_BUFF = -1f;

    @Override
    public void apply(String id) {
        market.getHazard().modifyFlat(id, HAZARD_BUFF,"纳米风暴");
    }

    @Override
    public void unapply(String id) {
        market.getHazard().unmodifyFlat(id);
    }

    @Override
    protected void createTooltipAfterDescription(TooltipMakerAPI tooltip, boolean expanded) {
        super.createTooltipAfterDescription(tooltip, expanded);

        String hazardinfo = "" + (int) (HAZARD_BUFF * 100f) + "%";

        tooltip.addPara("大量的纳米机器充斥着这颗星球，形成灰色的风暴，极大的改善了这里的气候。", 10f, Misc.getTextColor(), Misc.getHighlightColor(), hazardinfo);

    }

}
