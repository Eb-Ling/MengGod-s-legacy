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

public class Meng_forthetruth extends BaseIntelPlugin {
    private final MarketAPI market;
    private final MarketAPI fleets = null;
    private boolean init = false;
    private SectorEntityToken orbitCenter = null;
    private StarSystemAPI picker = null;
    private CampaignFleetAPI target = null;
    private PlanetAPI pick = null;

    public Meng_forthetruth(InteractionDialogAPI dialog) {
        this.market = dialog.getInteractionTarget().getMarket();

        setImportant(true);

        spawnFleet();

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
            spawnFleet();
            if (target == null) return;
            sendUpdateIfPlayerHasIntel(new Object(), false);
        }

        boolean existCheck = target.isEmpty();
        if (existCheck) {
            if (!init) {
                Global.getSoundPlayer().setSuspendDefaultMusicPlayback(false);
                Global.getSoundPlayer().playCustomMusic(1,0,null,false);
                sendUpdateIfPlayerHasIntel(new Object(), false);
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
                fleets,
                null,
                "neutral",
                null,
                "叛逃者",
                0f, // combatPts
                0f, // freighterPts
                0f, // tankerPts
                0f, // transportPts
                0f, // linerPts
                0f, // utilityPts
                2f // qualityMod
        );

        params.random = new Random();
        params.officerLevelBonus = 3;
        params.officerNumberBonus = 20;
        params.averageSMods = 2;
        params.ignoreMarketFleetSizeMult = true;

        target = FleetFactoryV3.createFleet(params);
        target.setTransponderOn(false);
        Misc.makeImportant(target, "interception");

        FleetMemberAPI member = target.getFleetData().addFleetMember("astral_Attack");
        member.getRepairTracker().setCR(1f);
        PersonAPI officer = OfficerManagerEvent.createOfficer(getFactionForUIColors(), 7, FleetFactoryV3.getSkillPrefForShip(member), false, target, true, true, 4, params.random);
        member.setCaptain(officer);
        member = target.getFleetData().addFleetMember("paragon_Elite");
        member.getRepairTracker().setCR(1f);
        officer = OfficerManagerEvent.createOfficer(getFactionForUIColors(), 7, FleetFactoryV3.getSkillPrefForShip(member), false, target, true, true, 4, params.random);
        member.setCaptain(officer);
        member = target.getFleetData().addFleetMember("odyssey_Balanced");
        member.getRepairTracker().setCR(1f);
        officer = OfficerManagerEvent.createOfficer(getFactionForUIColors(), 7, FleetFactoryV3.getSkillPrefForShip(member), false, target, true, true, 4, params.random);
        member.setCaptain(officer);
        member = target.getFleetData().addFleetMember("aurora_Assault");
        member.getRepairTracker().setCR(1f);
        officer = OfficerManagerEvent.createOfficer(getFactionForUIColors(), 7, FleetFactoryV3.getSkillPrefForShip(member), false, target, true, true, 4, params.random);
        member.setCaptain(officer);
        member = target.getFleetData().addFleetMember("doom_Strike");
        member.getRepairTracker().setCR(1f);
        officer = OfficerManagerEvent.createOfficer(getFactionForUIColors(), 7, FleetFactoryV3.getSkillPrefForShip(member), false, target, true, true, 4, params.random);
        member.setCaptain(officer);

        member = target.getFleetData().addFleetMember("hyperion_Attack");
        member.getRepairTracker().setCR(1f);
        officer = OfficerManagerEvent.createOfficer(getFactionForUIColors(), 7, FleetFactoryV3.getSkillPrefForShip(member), false, target, true, true, 4, params.random);
        member.setCaptain(officer);

        member = target.getFleetData().addFleetMember("hyperion_Attack");
        member.getRepairTracker().setCR(1f);
        officer = OfficerManagerEvent.createOfficer(getFactionForUIColors(), 7, FleetFactoryV3.getSkillPrefForShip(member), false, target, true, true, 4, params.random);
        member.setCaptain(officer);

        member = target.getFleetData().addFleetMember("Meng_timeboss_variant");
        member.getVariant().addTag("no_autofit");
        member.getVariant().addTag(Tags.VARIANT_ALWAYS_RECOVERABLE);
        member.getRepairTracker().setCR(1f);

        PersonAPI Cishe = OfficerManagerEvent.createOfficer(getFactionForUIColors(), 14, FleetFactoryV3.getSkillPrefForShip(member), false, target, true, false, 4, params.random);
        params.commander = Cishe;

        orbitCenter = picker.getCenter();
        target.getFleetData().sort();

        target.getFleetData().setFlagship(member);
        target.setCommander(Cishe);
        target.getCommander().setName(new FullName("Ci", "she", FullName.Gender.FEMALE));
        target.getFleetData().addOfficer(Cishe);
        target.getFlagship().setCaptain(Cishe);


        target.getMemoryWithoutUpdate().set("$Meng_timeFleet", true);


        target.removeAbility(Abilities.SENSOR_BURST);
        target.removeAbility(Abilities.INTERDICTION_PULSE);

        LocationAPI location = pick.getContainingLocation();
        location.addEntity(target);
        target.setLocation(pick.getLocation().x, pick.getLocation().y);
        target.getAI().addAssignment(FleetAssignment.ORBIT_PASSIVE, pick, 1000000f, null);


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
            info.addPara("圣殿赠送了你新的船插。", initPad, tc, h, picker.getName());// write your code here
        } else {
            // either in small description, or in tooltip/intel list
            if (isEnding()) {
                info.addPara("未知的神明，前方究竟藏着什么。", initPad, tc, h);
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
            return "圣殿组织浮出了水面。";
        }
        return "时间之外的旅者";
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
        info.addImages(width, 80, opad, opad * 2f, "graphics/factions/Meng_embers_s.png");
        if (isEnded() || isEnding()) {
            info.addPara("你终结了这个失控的舰队，他们的意志似乎已经不再清醒...", opad, h, target.getContainingLocation().getName());
        } else {
            info.addPara("你记录下了任务位置，终结它，或者在无尽轮回中死去。", opad, h, target.getContainingLocation().getName());
            // write your code gere
        }
    }

    @Override
    public String getIcon() {

        return "graphics/factions/Meng_embers_s.png";
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
