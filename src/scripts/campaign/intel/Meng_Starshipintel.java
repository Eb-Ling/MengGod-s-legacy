package data.scripts.campaign.intel;


import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.bar.MengSearch;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.Set;

public class Meng_Starshipintel extends BaseIntelPlugin {
    private final MarketAPI fleets = null;
    private boolean init = false;
    private SectorEntityToken orbitCenter = null;
    private StarSystemAPI picker = null;
    private SectorEntityToken target = null;
    private PlanetAPI pick = null;
    private float lefttime;

    public Meng_Starshipintel(InteractionDialogAPI dialog) {


        spawnTarget();

        setImportant(true);

        Global.getSector().addScript(this);
        if (dialog == null) {
            Global.getSector().getIntelManager().addIntel(this, false);
        } else {
            Global.getSector().getIntelManager().addIntel(this, false, dialog.getTextPanel());
        }
    }

    @Override
    public void advanceImpl(float amount) {
        if (target == null) {
            spawnTarget();
            sendUpdateIfPlayerHasIntel(new Object(), false);
        }
        float days = Global.getSector().getClock().convertToDays(amount);
        lefttime -= days;
        boolean existCheck = target.getMemoryWithoutUpdate().getBoolean("$Meng_explored");
        if (existCheck) {
            if (!init) {
                MengSearch.setStage(MengSearch.MengStep.Meng_Step9);
                sendUpdateIfPlayerHasIntel(new Object(), false);
                Global.getSector().getPlayerFleet().getFleetData().addFleetMember("Meng_Starkiller_end");
                init = true;
            }
            endAfterDelay();
        }

    }

    private void spawnTarget() {
        float d = 0f;

        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            if (system.hasPulsar()) continue;

            float systemMult = 0f;
            if (system.hasTag(Tags.THEME_MISC_SKIP)) {
                systemMult = 1f;
            } else if (system.hasTag(Tags.THEME_MISC)) {
                systemMult = 3f;
            } else if (system.hasTag(Tags.THEME_REMNANT_NO_FLEETS)) {
                systemMult = 3f;
            } else if (system.hasTag(Tags.THEME_RUINS)) {
                systemMult = 5f;
            } else if (system.hasTag(Tags.THEME_REMNANT_DESTROYED)) {
                systemMult = 3f;
            } else if (system.hasTag(Tags.THEME_REMNANT_MAIN)) {
                systemMult = 0f;
            } else if (system.hasTag(Tags.THEME_REMNANT_SECONDARY)) {
                systemMult = 0f;
            } else if (system.hasTag(Tags.THEME_CORE_UNPOPULATED)) {
                systemMult = 0f;
            }

            for (MarketAPI market : Misc.getMarketsInLocation(system)) {
                if (market.isHidden()) continue;
                systemMult = 0f;
                break;
            }

            if (systemMult <= 0f) continue;

            float dist = system.getLocation().length();

            for (PlanetAPI planet : system.getPlanets()) {
                if (planet.isStar()) continue;
                if (planet.getOrbitFocus() != null && MathUtils.getDistance(planet, planet.getOrbitFocus()) < 500f)
                    continue;
                if (planet.getMarket() == null || !planet.getMarket().isPlanetConditionMarketOnly()) continue;
                if (Vector2f.sub(system.getLocation(), Global.getSector().getPlayerFleet().getLocation(), new Vector2f()).length() >= d) {
                    d = Vector2f.sub(system.getLocation(), Global.getSector().getPlayerFleet().getLocation(), new Vector2f()).length();
                    picker = system;
                    pick = planet;
                }


                if (planet.getOrbitFocus() != null && MathUtils.getDistance(planet, planet.getOrbitFocus()) < 500f)
                    continue;
                if (planet.getMarket() == null || !planet.getMarket().isPlanetConditionMarketOnly()) continue;
                d = Vector2f.sub(picker.getLocation(), Global.getSector().getPlayerFleet().getLocation(), new Vector2f()).length();
                if (Vector2f.sub(system.getLocation(), Global.getSector().getPlayerFleet().getLocation(), new Vector2f()).length() >= d - 500f) {

                    picker = system;
                    pick = planet;
                    break;

                }

            }
        }


        orbitCenter = picker.getCenter();
        target = picker.addCustomEntity("Meng_StarshipMist", "掩人耳目的遗迹", "Meng_StarshipMist_type", "independent");
        target.setCircularOrbitPointingDown(pick, 90.0F, 500.0F, 180.0F);
        LocationAPI location = pick.getContainingLocation();
        location.addEntity(target);

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
            info.addPara("你获得了异宙龙级超级主力舰。", initPad, tc, h, picker.getName());// write your code here
        } else {
            // either in small description, or in tooltip/intel list
            if (isEnding()) {
                info.addPara("异宙之龙。", initPad, tc, h);
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
            return "重见天日。";
        }
        return "古代的遗产";
    }

    @Override
    public String getName() {
        return getSmallDescriptionTitle();
    }

    @Override
    public FactionAPI getFactionForUIColors() {
        return Global.getSector().getFaction("independent");
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
        info.addImages(width, 80, opad, opad * 2f, "graphics/factions/Meng_embers_s.png");
        if (isEnded() || isEnding()) {
            info.addPara("传说中巨龙的心脏...", opad, h, target.getContainingLocation().getName());
        } else {
            info.addPara("你接受了圣殿的委托，请前往目标星系调查。", opad, h, target.getContainingLocation().getName());
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