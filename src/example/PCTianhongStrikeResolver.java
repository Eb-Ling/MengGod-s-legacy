package data.scripts.specialization;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignTerrainAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Planets;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.population.CoreImmigrationPluginImpl;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin.DebrisFieldParams;
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin.DebrisFieldSource;
import com.fs.starfarer.api.util.Misc;

public class PCTianhongStrikeResolver {

    public static final float TARGET_RANGE = 1000f;
    public static final float OWNER_REP_DELTA = -1f;
    public static final float GLOBAL_REP_DELTA = -0.8f;
    public static final String DEVASTATED_PLANET_DESCRIPTION_ID = "pc_tianhong_devastated_planet";

    protected static final String[] ECOLOGY_REMOVE = new String[] {
            Conditions.HABITABLE,
            Conditions.MILD_CLIMATE,
            Conditions.FARMLAND_POOR,
            Conditions.FARMLAND_ADEQUATE,
            Conditions.FARMLAND_RICH,
            Conditions.FARMLAND_BOUNTIFUL,
            Conditions.DECIVILIZED,
            Conditions.DECIVILIZED_SUBPOP,
            Conditions.COLD,
            Conditions.VERY_COLD,
            Conditions.INIMICAL_BIOSPHERE
    };

    protected static final String[] HAZARD_ADD = new String[] {
            Conditions.POLLUTION,
            Conditions.VERY_HOT,
            Conditions.IRRADIATED,
            Conditions.EXTREME_TECTONIC_ACTIVITY
    };

    protected static final String[][] RESOURCE_CHAINS = new String[][] {
            {Conditions.ORE_SPARSE, Conditions.ORE_MODERATE, Conditions.ORE_ABUNDANT, Conditions.ORE_RICH, Conditions.ORE_ULTRARICH},
            {Conditions.RARE_ORE_SPARSE, Conditions.RARE_ORE_MODERATE, Conditions.RARE_ORE_ABUNDANT, Conditions.RARE_ORE_RICH, Conditions.RARE_ORE_ULTRARICH},
            {Conditions.VOLATILES_TRACE, Conditions.VOLATILES_DIFFUSE, Conditions.VOLATILES_ABUNDANT, Conditions.VOLATILES_PLENTIFUL},
            {Conditions.ORGANICS_TRACE, Conditions.ORGANICS_COMMON, Conditions.ORGANICS_ABUNDANT, Conditions.ORGANICS_PLENTIFUL}
    };

    public static boolean isLegalTarget(CampaignFleetAPI fleet, SectorEntityToken target) {
        if (fleet == null || target == null || !target.isAlive()) {
            return false;
        }
        if (target == fleet || target instanceof CampaignFleetAPI) {
            return false;
        }
        if (target.getContainingLocation() == null || target.getContainingLocation() != fleet.getContainingLocation()) {
            return false;
        }
        if (target.isStar() || target.isSystemCenter() || target.hasTag(Tags.STAR) || target.hasTag(Tags.SYSTEM_ANCHOR)) {
            return false;
        }
        if (target.hasTag(Tags.TERRAIN) || target.hasTag(Tags.NON_CLICKABLE) || target.hasTag(Tags.JUMP_POINT)) {
            return false;
        }
        if (getSurfaceDistance(fleet, target) > TARGET_RANGE) {
            return false;
        }

        if (target instanceof PlanetAPI) {
            PlanetAPI planet = (PlanetAPI) target;
            return !planet.isStar() && !planet.isBlackHole();
        }

        if (getMarketForTarget(target) != null) {
            return true;
        }

        return target.hasTag(Tags.STATION)
                || target.hasTag(Tags.OBJECTIVE)
                || target.hasTag(Tags.SALVAGEABLE)
                || target.hasTag(Tags.HAS_INTERACTION_DIALOG)
                || target.getCustomPlugin() != null
                || target.getCustomEntityType() != null;
    }

    public static MarketAPI getMarketForTarget(SectorEntityToken target) {
        if (target == null) {
            return null;
        }
        if (target.getMarket() != null) {
            return target.getMarket();
        }
        if (Global.getSector() == null || Global.getSector().getEconomy() == null) {
            return null;
        }
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            if (market == null) {
                continue;
            }
            if (market.getPrimaryEntity() == target || market.getConnectedEntities().contains(target)) {
                return market;
            }
        }
        return null;
    }

    public static float getSurfaceDistance(CampaignFleetAPI fleet, SectorEntityToken target) {
        if (fleet == null || target == null) {
            return Float.MAX_VALUE;
        }
        float distance = Misc.getDistance(fleet.getLocation(), target.getLocation());
        return Math.max(0f, distance - Math.max(0f, target.getRadius()));
    }

    public static String getTargetName(SectorEntityToken target) {
        if (target == null) {
            return "No target";
        }
        MarketAPI market = getMarketForTarget(target);
        if (market != null) {
            return market.getName();
        }
        String name = target.getName();
        return name == null || name.length() <= 0 ? "Unknown target" : name;
    }

    public static String getOwnerFactionId(SectorEntityToken target) {
        MarketAPI market = getMarketForTarget(target);
        if (market != null && market.getFaction() != null) {
            return market.getFactionId();
        }
        FactionAPI faction = target == null ? null : target.getFaction();
        if (faction == null || faction.isNeutralFaction() || faction.isPlayerFaction()) {
            return null;
        }
        return faction.getId();
    }

    public static void resolveStrike(SectorEntityToken target) {
        if (target == null || !target.isAlive()) {
            return;
        }

        LocationAPI location = target.getContainingLocation();
        Vector2f targetLoc = new Vector2f(target.getLocation());
        float targetRadius = Math.max(180f, target.getRadius() + 240f);
        MarketAPI targetMarket = getMarketForTarget(target);
        boolean hadRealMarket = isRealMarket(targetMarket);
        String ownerId = hadRealMarket ? getOwnerFactionId(target) : null;

        if (target instanceof PlanetAPI) {
            devastatePlanet((PlanetAPI) target);
        } else {
            if (targetMarket != null && isStationLike(target, targetMarket)) {
                abandonStationMarket(target, targetMarket);
            } else if (location != null) {
                detachMarket(targetMarket);
                location.removeEntity(target);
            }
        }

        if (!(target instanceof PlanetAPI) || hadRealMarket) {
            addDebrisField(location, targetLoc, targetRadius);
        }
        PCTianhongVisualRenderer.spawnFinalBurst(location, targetLoc, targetRadius);
        if (hadRealMarket) {
            applyReputationPenalty(ownerId);
        }
    }

    protected static boolean isRealMarket(MarketAPI market) {
        return market != null && market.isInEconomy() && !market.isPlanetConditionMarketOnly() && market.getSize() > 0;
    }

    protected static boolean isStationLike(SectorEntityToken target, MarketAPI market) {
        if (target == null) {
            return false;
        }
        if (target.hasTag(Tags.STATION)) {
            return true;
        }
        if (market != null) {
            for (Industry industry : market.getIndustries()) {
                if (industry != null && industry.getSpec() != null && industry.getSpec().hasTag("station")) {
                    return true;
                }
            }
        }
        return !(target instanceof PlanetAPI) && market != null;
    }

    protected static void devastatePlanet(PlanetAPI planet) {
        MarketAPI market = getMarketForTarget(planet);
        boolean realMarket = isRealMarket(market);
        int originalSize = realMarket ? market.getSize() : 0;

        if (!Planets.IRRADIATED.equals(planet.getTypeId())) {
            planet.changeType(Planets.IRRADIATED, StarSystemGenerator.random);
        }
        planet.setCustomDescriptionId(DEVASTATED_PLANET_DESCRIPTION_ID);

        if (market != null) {
            removeEcologyConditions(market);
            downgradeResources(market);
            for (String condition : HAZARD_ADD) {
                addConditionIfMissing(market, condition);
            }
            if (realMarket) {
                addRuinsForSize(market, originalSize);
                stripMarket(market);
                market.setPlanetConditionMarketOnly(true);
                market.setFactionId(Factions.NEUTRAL);
                market.setPlayerOwned(false);
                market.setSize(1);
                market.getPopulation().setWeight(CoreImmigrationPluginImpl.getWeightForMarketSizeStatic(1));
                market.getPopulation().normalize();
                Global.getSector().getEconomy().removeMarket(market);
                Misc.removeRadioChatter(market);
            }
            market.advance(0f);
        }
    }

    protected static void abandonStationMarket(SectorEntityToken station, MarketAPI market) {
        String marketId = market == null || market.getId() == null ? station.getId() + "_tianhong_ruins" : market.getId() + "_tianhong_ruins";
        detachMarket(market);
        station.setFaction(Factions.NEUTRAL);
        Misc.setAbandonedStationMarket(marketId, station);
    }

    protected static void detachMarket(MarketAPI market) {
        if (market == null || Global.getSector() == null || Global.getSector().getEconomy() == null) {
            return;
        }
        stripMarket(market);
        Global.getSector().getEconomy().removeMarket(market);
        Misc.removeRadioChatter(market);
        market.advance(0f);
    }

    protected static void stripMarket(MarketAPI market) {
        if (market == null) {
            return;
        }
        market.setAdmin(null);
        market.getCommDirectory().clear();
        for (SubmarketAPI submarket : market.getSubmarketsCopy()) {
            market.removeSubmarket(submarket.getSpecId());
        }
        for (Industry industry : new ArrayList<Industry>(market.getIndustries())) {
            market.removeIndustry(industry.getId(), null, false);
        }
        market.clearCommodities();
        for (SectorEntityToken entity : new ArrayList<SectorEntityToken>(market.getConnectedEntities())) {
            entity.setFaction(Factions.NEUTRAL);
        }
        market.getMemoryWithoutUpdate().set("$wasCivilized", true);
        market.setIncentiveCredits(0);
    }

    protected static void removeEcologyConditions(MarketAPI market) {
        for (String condition : ECOLOGY_REMOVE) {
            market.removeCondition(condition);
        }
    }

    protected static void downgradeResources(MarketAPI market) {
        for (String[] chain : RESOURCE_CHAINS) {
            downgradeResourceChain(market, chain);
        }
    }

    protected static void downgradeResourceChain(MarketAPI market, String[] chain) {
        int found = -1;
        for (int i = 0; i < chain.length; i++) {
            if (market.hasCondition(chain[i])) {
                found = i;
                break;
            }
        }
        if (found < 0) {
            return;
        }
        for (String condition : chain) {
            market.removeCondition(condition);
        }
        if (found > 0) {
            market.addCondition(chain[found - 1]);
        }
    }

    protected static void addRuinsForSize(MarketAPI market, int size) {
        market.removeCondition(Conditions.RUINS_SCATTERED);
        market.removeCondition(Conditions.RUINS_WIDESPREAD);
        market.removeCondition(Conditions.RUINS_EXTENSIVE);
        market.removeCondition(Conditions.RUINS_VAST);

        String id;
        if (size <= 3) {
            id = market.addCondition(Conditions.RUINS_SCATTERED);
        } else if (size <= 4) {
            id = market.addCondition(Conditions.RUINS_WIDESPREAD);
        } else if (size <= 6) {
            id = market.addCondition(Conditions.RUINS_EXTENSIVE);
        } else {
            id = market.addCondition(Conditions.RUINS_VAST);
        }
        MarketConditionAPI ruins = market.getSpecificCondition(id);
        if (ruins != null) {
            ruins.setSurveyed(true);
        }
    }

    protected static void addConditionIfMissing(MarketAPI market, String condition) {
        if (market != null && condition != null && !market.hasCondition(condition)) {
            market.addCondition(condition);
        }
    }

    protected static void addDebrisField(LocationAPI location, Vector2f loc, float radius) {
        if (location == null || loc == null) {
            return;
        }
        DebrisFieldParams params = new DebrisFieldParams(Math.max(350f, radius), 1.0f, 240f, 35f);
        params.source = DebrisFieldSource.MIXED;
        params.baseSalvageXP = 750;
        params.glowColor = new Color(120, 220, 255, 255);
        SectorEntityToken debris = Misc.addDebrisField(location, params, new Random());
        if (debris != null) {
            debris.setFixedLocation(loc.x, loc.y);
            debris.setSensorProfile(null);
            debris.setDiscoverable(null);
            debris.addTag(Tags.SALVAGEABLE);
            if (debris instanceof CampaignTerrainAPI) {
                ((CampaignTerrainAPI) debris).setRadius(Math.max(350f, radius));
            }
        }
    }

    protected static void applyReputationPenalty(String ownerId) {
        if (Global.getSector() == null || Global.getSector().getPlayerFaction() == null) {
            return;
        }

        FactionAPI player = Global.getSector().getPlayerFaction();
        List<FactionAPI> factions = Global.getSector().getAllFactions();
        for (FactionAPI faction : factions) {
            if (faction == null || faction.isPlayerFaction() || faction.isNeutralFaction()) {
                continue;
            }
            float delta = faction.getId().equals(ownerId) ? OWNER_REP_DELTA : GLOBAL_REP_DELTA;
            player.adjustRelationship(faction.getId(), delta);
            faction.adjustRelationship(Factions.PLAYER, delta);
        }

        if (Global.getSector().getCharacterData() != null) {
            float atrocities = Global.getSector().getCharacterData().getMemoryWithoutUpdate().getFloat("$pc_tianhong_atrocities");
            Global.getSector().getCharacterData().getMemoryWithoutUpdate().set("$pc_tianhong_atrocities", atrocities + 1f);
        }
    }

    protected PCTianhongStrikeResolver() {
    }
}
