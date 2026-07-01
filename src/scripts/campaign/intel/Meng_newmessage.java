package data.scripts.campaign.intel;


import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.Abilities;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.bar.MengSearch;
import data.scripts.campaign.bar.Meng_protect_bar_event1;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.Random;
import java.util.Set;

public class Meng_newmessage extends BaseIntelPlugin {
    private final MarketAPI market;
    private final MarketAPI fleets = null;
    private boolean init = false;
    private SectorEntityToken orbitCenter = null;
    private StarSystemAPI picker = null;
    private CampaignFleetAPI target = null;
    private PlanetAPI pick = null;
    private float lefttime;

    public Meng_newmessage(InteractionDialogAPI dialog) {

        this.market = dialog.getInteractionTarget().getMarket();

        setImportant(true);


        setImportant(true);

        Global.getSector().addScript(this);
        if (dialog == null) {
            Global.getSector().getIntelManager().addIntel(this, false);
        } else {
            Global.getSector().getIntelManager().addIntel(this, false, dialog.getTextPanel());
        }
    }

    public static void backDoor() {
        new Meng_protect_bar_event1();
    }

    @Override
    public void advanceImpl(float amount) {
    }


    @Override
    public boolean runWhilePaused() {
        return false;
    }

    @Override
    protected void addBulletPoints(TooltipMakerAPI info, ListInfoMode mode, boolean isUpdate, Color tc, float initPad) {
        Color h = Misc.getHighlightColor();
        Color g = Misc.getGrayColor();
        float pad = 3f;
        float opad = 10f;

        if (mode == ListInfoMode.IN_DESC) initPad = opad;
        FactionAPI faction = getFactionForUIColors();

        bullet(info);
        if (isUpdate) {

            // 3 possible updates: de-posted/expired, failed, completed
            info.addPara("萌萌研究了舰船的残骸，为你提供了一个新的船插。你获取了舰船的通讯设备，试图找到尘封的真相。", initPad, tc, h, picker.getName());// write your code here
        } else {
            // either in small description, or in tooltip/intel list
            if (isEnding()) {
                info.addPara("你获取了舰船的通讯设备，试图找到尘封的真相。", initPad, tc, h);
                initPad = 0f;
            } else {
                if (mode != ListInfoMode.IN_DESC) {
                    initPad = 0f;
                }
                info.addPara("星系位置", initPad, tc, h, picker.getName());

            }
            // write your code here

        }


        unindent(info);

    }

    @Override
    public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
        Color c = getTitleColor(mode);
        info.addPara(getSmallDescriptionTitle(), c, 0f);
        addBulletPoints(info, mode);
    }

    @Override
    public String getSortString() {
        return getSmallDescriptionTitle();
    }

    @Override
    public String getSmallDescriptionTitle() {
        if (isEnded() || isEnding()) {
            return "你获取了舰船的通讯设备，试图找到尘封的真相。";
        }
        return "神降舰队";
    }

    @Override
    public String getName() {
        return getSmallDescriptionTitle();
    }

    @Override
    public FactionAPI getFactionForUIColors() {
        return market.getFaction();
    }


    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        Color h = Misc.getHighlightColor();
        Color g = Misc.getGrayColor();
        Color tc = Misc.getTextColor();
        float pad = 3f;
        float opad = 10f;
        float expad = 20f;

        FactionAPI faction = getFactionForUIColors();
        info.addImages(width, 80, opad, opad * 2f, "graphics/portraits/Mou_Meng.png");
        if (isEnded() || isEnding()) {
            info.addPara("一切尘埃落定，萌萌也完成了复仇，但是在这支舰队背后隐藏的秘密，让你知道一切才刚刚开始。", opad, h, target.getContainingLocation().getName());
        } else {
            info.addPara("萌萌向你指出了当初一切发生的位置...去探索一下这个星系，也许会发现什么。但在此之前，要记住她和你强调的话，千万不要尝试从正面击溃他。", opad, h, target.getContainingLocation().getName());
            // write your code gere
        }
    }

    @Override
    public String getIcon() {
        return "graphics/portraits/Mou_Meng.png";
    }

    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = super.getIntelTags(map);
        tags.add(getFactionForUIColors().getId());

        // write your code here

        return tags;
    }

    @Override
    public SectorEntityToken getMapLocation(SectorMapAPI map) {
        return orbitCenter;
    }
}