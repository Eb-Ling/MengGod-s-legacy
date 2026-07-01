package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import java.awt.*;

public class Meng_fire_EntropyInc extends BaseHullMod {
    private final String ID = "Meng_Fire_EntropyInc_Id";
    private final float FluxMult = 0.25f;
    private final float FluxSold = 0.75f;
    private final float DamageMult = 0.1f;
    private final float BurstSpeed = 0.15f;
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float opad = 12.0F;
        Color highlight = new Color(255, 55, 40, 255);
        LabelAPI label1 = tooltip.addPara(
                "火羽研究所在虚子炉的熵态平衡时发现，系统的混乱度增长在虚子炉内部并非完全不可控。通过刻意引导熵增过程，可以在特定阈值下实现能量输出的质变——这是一种游走在失控边缘的危险艺术。",
                opad, highlight);
        label1.setHighlight();
        label1.setHighlightColors(highlight, highlight, highlight, highlight, highlight, highlight, highlight);

        tooltip.addSectionHeading("数据分析", Alignment.MID, opad);

        LabelAPI label = tooltip.addPara(
                "#舰船武器额外产生 %s%% 软辐能。\n#辐能达到容量最大值的 %s%% 以上持续超过两秒，提升舰船 %s%% 的伤害和 %s%% 的武器射速。\n#并且不再产生额外的软辐能",
                opad, highlight, String.valueOf(Math.round(100f *FluxMult)), String.valueOf(Math.round(FluxSold *100f)), String.valueOf(Math.round(DamageMult *100f)), String.valueOf(Math.round(BurstSpeed *100f))
        );

        label.setHighlight(Math.round(100f * FluxMult) + "%", Math.round(FluxSold * 100f) + "%", Math.round(DamageMult * 100f) + "%", Math.round(BurstSpeed * 100f) + "%");
        label.setHighlightColors(highlight, highlight, highlight, highlight);

    }

    //舰船武器额外产生25%软辐能，已在虚子炉辐散系统插件中实现。

    //若辐能达到容量最大值的75%以上持续超过两秒，提升舰船10%的伤害和15%的武器射速。
    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        super.advanceInCombat(ship, amount);
        if(!ship.isAlive()) return;
        MutableShipStatsAPI stats = ship.getMutableStats();
        if(ship.getFluxLevel()>=FluxSold){
            stats.getBallisticWeaponDamageMult().modifyPercent(ID,DamageMult*100f);
            stats.getEnergyWeaponDamageMult().modifyPercent(ID,DamageMult*100f);
            stats.getMissileWeaponDamageMult().modifyPercent(ID,DamageMult*100f);
            stats.getEnergyRoFMult().modifyPercent(ID,BurstSpeed*100f);
            stats.getBallisticRoFMult().modifyPercent(ID,BurstSpeed*100f);
        }

    }
    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return ship.getVariant().getHullMods().contains("Meng_fire_core")||ship.getVariant().getHullMods().contains("Meng_fire_core_li");
    }
    public String getUnapplicableReason(ShipAPI ship) {
        if (!ship.getVariant().getHullMods().contains("Meng_fire_core")&&!ship.getVariant().getHullMods().contains("Meng_fire_core_li")) {
            return "只能用于火羽计划舰船";
        }
        return null;
    }
}
