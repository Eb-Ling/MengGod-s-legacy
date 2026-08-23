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
import data.scripts.campaign.bar.Meng_protect_bar_event1;
import data.scripts.campaign.bar.MengSearch;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.Random;
import java.util.Set;

public class Meng_protectfleetintel extends BaseIntelPlugin {
    private final MarketAPI market;
    private final String fleetFactionId;
    private boolean init = false;
    private SectorEntityToken orbitCenter = null;
    private StarSystemAPI picker = null;
    private CampaignFleetAPI target = null;
    private PlanetAPI pick = null;
    private float lefttime;

    public Meng_protectfleetintel(InteractionDialogAPI dialog) {

        this.market = dialog.getInteractionTarget().getMarket();
        this.fleetFactionId = "neutral";

        setImportant(true);

        spawnFleet();

        setImportant(true);

        Global.getSector().addScript(this);
        if (dialog == null) {
            Global.getSector().getIntelManager().addIntel(this, true);
        } else {
            Global.getSector().getIntelManager().addIntel(this, true, dialog.getTextPanel());
        }
    }

    public static void backDoor() {
        new Meng_protect_bar_event1();
    }

    @Override
    public void advanceImpl(float amount) {
        if (target == null) {
            spawnFleet();
            if (target == null) return;
            sendUpdateIfPlayerHasIntel(new Object(), false);
        }
        float days = Global.getSector().getClock().convertToDays(amount);
        lefttime -= days;
        boolean existCheck = target.isEmpty();
        if (existCheck) {
            if (!init) {
                MengSearch.setStage(MengSearch.MengStep.Meng_Step5);
                sendUpdateIfPlayerHasIntel(new Object(), false);
                Global.getSector().getPlayerFleet().getCargo().addHullmods("Meng_God_Protect", 1);

                init = true;
            }
            endAfterDelay();
        }

    }

    private void spawnFleet() {
        float d = 0f;
        // TASC can replace all condition-only markets, so never retry this search indefinitely.
        for (int attempt = 0; attempt < 10 && (picker == null || pick == null); attempt++) {
            float dist = Math.max(10000f, (float) Math.random() * 50000f);

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


                for (PlanetAPI planet : system.getPlanets()) {
                    if (planet.isStar()) continue;

                    if (planet.getOrbitFocus() != null && MathUtils.getDistance(planet, planet.getOrbitFocus()) < 500f)
                        continue;
                    if (planet.getMarket() == null || !planet.getMarket().isPlanetConditionMarketOnly()) continue;
                    if (Vector2f.sub(system.getLocation(), Global.getSector().getPlayerFleet().getLocation(), new Vector2f()).length() >= d && d <= dist) {
                        d = Vector2f.sub(system.getLocation(), Global.getSector().getPlayerFleet().getLocation(), new Vector2f()).length();
                        picker = system;
                        pick = planet;
                    }

                }
            }
        }
        if (picker == null || pick == null) {
            for (StarSystemAPI system : Global.getSector().getStarSystems()) {
                if (system.hasPulsar()) continue;
                for (PlanetAPI planet : system.getPlanets()) {
                    if (!planet.isStar()) {
                        picker = system;
                        pick = planet;
                        break;
                    }
                }
                if (pick != null) break;
            }
        }
        if (picker == null || pick == null) return;

        FleetParamsV3 params = new FleetParamsV3(
                null,
                null,
                fleetFactionId,
                null,
                "神性污染的舰队",
                0f,
                0f,
                0f,
                0f,
                0f,
                0f,
                0f
        );

        params.random = new Random();
        params.officerLevelBonus = 3;
        params.officerNumberBonus = 20;
        params.averageSMods = 0;
        params.ignoreMarketFleetSizeMult = true;

        target = FleetFactoryV3.createFleet(params);
        target.setTransponderOn(false);
        Misc.makeImportant(target, "interception");

        FleetMemberAPI member = target.getFleetData().addFleetMember("Meng_protect_boss1_variant");
        member.getRepairTracker().setCR(1f);
        PersonAPI officer = OfficerManagerEvent.createOfficer(getFactionForUIColors(), 7, FleetFactoryV3.getSkillPrefForShip(member), false, target, true, true, 4, params.random);
        member.setCaptain(officer);
        member = target.getFleetData().addFleetMember("Meng_protect_boss3_variant");
        member.getRepairTracker().setCR(1f);
        officer = OfficerManagerEvent.createOfficer(getFactionForUIColors(), 7, FleetFactoryV3.getSkillPrefForShip(member), false, target, true, true, 4, params.random);
        member.setCaptain(officer);
        member = target.getFleetData().addFleetMember("Meng_protect_boss3_variant");
        member.getRepairTracker().setCR(1f);
        officer = OfficerManagerEvent.createOfficer(getFactionForUIColors(), 7, FleetFactoryV3.getSkillPrefForShip(member), false, target, true, true, 4, params.random);
        member.setCaptain(officer);
        member = target.getFleetData().addFleetMember("Meng_protect_boss3_variant");
        member.getRepairTracker().setCR(1f);
        officer = OfficerManagerEvent.createOfficer(getFactionForUIColors(), 7, FleetFactoryV3.getSkillPrefForShip(member), false, target, true, true, 4, params.random);
        member.setCaptain(officer);
        member = target.getFleetData().addFleetMember("Meng_protect_boss3_variant");
        member.getRepairTracker().setCR(1f);
        officer = OfficerManagerEvent.createOfficer(getFactionForUIColors(), 7, FleetFactoryV3.getSkillPrefForShip(member), false, target, true, true, 4, params.random);
        member.setCaptain(officer);
        member = target.getFleetData().addFleetMember("Meng_protect_boss2_variant");
        member.getRepairTracker().setCR(1f);
        officer = OfficerManagerEvent.createOfficer(getFactionForUIColors(), 7, FleetFactoryV3.getSkillPrefForShip(member), false, target, true, true, 4, params.random);
        member.setCaptain(officer);

        member = target.getFleetData().addFleetMember("Meng_protect_boss2_variant");
        member.getRepairTracker().setCR(1f);
        officer = OfficerManagerEvent.createOfficer(getFactionForUIColors(), 7, FleetFactoryV3.getSkillPrefForShip(member), false, target, true, true, 4, params.random);
        member.setCaptain(officer);

        member = target.getFleetData().addFleetMember("Meng_protect_boss2_variant");
        member.getRepairTracker().setCR(1f);
        officer = OfficerManagerEvent.createOfficer(getFactionForUIColors(), 7, FleetFactoryV3.getSkillPrefForShip(member), false, target, true, true, 4, params.random);
        member.setCaptain(officer);

        member = target.getFleetData().addFleetMember("Meng_protect_boss1_variant");
        member.getRepairTracker().setCR(1f);

        PersonAPI Jack = OfficerManagerEvent.createOfficer(getFactionForUIColors(), 14, FleetFactoryV3.getSkillPrefForShip(member), false, target, true, true, 4, params.random);
        params.commander = Jack;

        orbitCenter = picker.getCenter();
        target.getFleetData().sort();

        target.getFleetData().setFlagship(member);
        target.setCommander(Jack);
        target.getCommander().setName(new FullName("Jack", "smith", FullName.Gender.FEMALE));
        target.getCommander().setPortraitSprite(Global.getSettings().getSpriteName("intel", "Meng_JACK"));
        target.getFleetData().addOfficer(Jack);
        target.getFlagship().setCaptain(Jack);


        target.getMemoryWithoutUpdate().set("$Meng_protectFleet", true);


        target.removeAbility(Abilities.SENSOR_BURST);
        target.removeAbility(Abilities.INTERDICTION_PULSE);

        LocationAPI location = pick.getContainingLocation();
        location.addEntity(target);
        target.setLocation(pick.getLocation().x, pick.getLocation().y);
        target.getAI().addAssignment(FleetAssignment.ORBIT_PASSIVE, pick, 1000000f, null);

        target.forceSync();
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

            info.addPara("萌萌研究了舰船的残骸，为你提供了一个新的船插。你获取了舰船的通讯设备，试图找到尘封的真相。", initPad, tc, h, picker != null ? picker.getName() : "未知星系");
        } else {
            if (isEnding()) {
                info.addPara("你获取了舰船的通讯设备，试图找到尘封的真相。", initPad, tc, h);
                initPad = 0f;
            } else {
                if (mode != ListInfoMode.IN_DESC) {
                    initPad = 0f;
                }
                if (picker != null) {
                    info.addPara("星系位置", initPad, tc, h, picker.getName());
                } else {
                    info.addPara("正在定位目标星系...", initPad, tc, h);
                }

            }

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
        tags.add(Tags.INTEL_STORY);
        if (getFactionForUIColors() != null) {
            tags.add(getFactionForUIColors().getId());
        }

        return tags;
    }

    @Override
    public SectorEntityToken getMapLocation(SectorMapAPI map) {
        return orbitCenter;
    }
}
