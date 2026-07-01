package data.world.systems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.MusicPlayerPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.StarAge;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.combat.entities.terrain.Planet;
import com.fs.starfarer.loading.specs.PlanetSpec;
import data.scripts.campaign.Meng_Rocifer;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;

import static data.world.MengGen.addMarketplace;


public class Meng_Embers_System {
    public PersonAPI officer;
    public void generate(SectorAPI sector) {
        //create a star system
        StarSystemAPI system = sector.createStarSystem("Embers");
        //set its location
        system.getLocation().set(-35000f, 10000f);
        //set background image
        system.setBackgroundTextureFilename("graphics/backgrounds/wormhole_dest_stars2.jpg");
        //set bgm
        system.getMemoryWithoutUpdate().set(MusicPlayerPluginImpl.MUSIC_SET_MEM_KEY, "Meng_Embersbgm");

        //the star
        PlanetAPI embers = system.initStar("embers", "star_yellow", 800f, 350f);
        embers.setCustomDescriptionId("Meng_embers_des");
        //background light color
        system.setLightColor(new Color(245, 237, 203));

        //make asteroid belt surround it
        system.addAsteroidBelt(embers, 100, 2000f, 150f, 180, 360, Terrain.ASTEROID_BELT, "e");
        system.addRingBand(embers, "misc", "rings_dust0", 256f, 1, Color.WHITE, 256f, 2000f, 300f);

        //have some nebula
        SectorEntityToken nebula = Misc.addNebulaFromPNG("graphics/misc/Meng_Embers_nebula.png", 0, 0,
                system, "terrain", "nebula_blue", 4, 4, StarAge.AVERAGE);
//        SectorEntityToken nebula = system.addTerrain(Terrain.NEBULA, new BaseTiledTerrain.TileParams(
//                "xxxxxxxx"
//                +"xxx   xx"
//                +"xx     x",8,3,"terrain","nebula",1800,1000,"星云"
//        ));
//
//        nebula.setLocation(0,13000f);
        //a gate
        SectorEntityToken gate = system.addCustomEntity("Meng_Embers_gate", // unique id
                "圣殿之门", // name - if null, defaultName from custom_entities.json will be used
                Entities.INACTIVE_GATE, // type of object, defined in custom_entities.json
                null); // faction
        gate.setCircularOrbit(embers, 240f, 10000f, 1200f);
        system.addAsteroidBelt(embers, 120, 4000, 256, 400, 500);
        system.addAsteroidBelt(embers, 240, 7600, 256, 400, 500);
        system.addRingBand(embers, "misc", "rings_dust0", 256f, 3, Color.white, 256f, 7200, 180f);
        system.addRingBand(embers, "misc", "rings_dust0", 256f, 3, Color.white, 256f, 2400, 220f);

        //a new planet for people
        PlanetAPI Embers = system.addPlanet("Meng_Combus", embers, "Embers", "tundra", 215, 150f, 8000f, 365f);

        //a new market for planet
        MarketAPI Market = addMarketplace("Meng_temple", Embers, null
                , Embers.getName(), 8,
                new ArrayList<>(
                        Arrays.asList(
                                Conditions.POPULATION_8, // population
                                Conditions.HABITABLE,
                                Conditions.FARMLAND_BOUNTIFUL,
                                Conditions.REGIONAL_CAPITAL,
                                Conditions.RUINS_VAST,
                                "Meng_EmbersFactory_1",
                                "Meng_EmbersFactory_2",
                                "Meng_EmbersFactory_3"
                        )),
                new ArrayList<>(
                        Arrays.asList(
                                Submarkets.GENERIC_MILITARY,
                                Submarkets.SUBMARKET_BLACK,
                                Submarkets.SUBMARKET_OPEN,
                                Submarkets.SUBMARKET_STORAGE
                        )),
                new ArrayList<>(
                        Arrays.asList(
                                Industries.POPULATION,
                                Industries.MEGAPORT,
                                Industries.STARFORTRESS_HIGH,
                                Industries.FARMING,
                                Industries.HIGHCOMMAND,
                                Industries.ORBITALWORKS,
                                Industries.WAYSTATION,
                                Industries.LIGHTINDUSTRY,
                                Industries.HEAVYBATTERIES,
                                Industries.PLANETARYSHIELD,
                                Industries.COMMERCE
                        )),
                0.3f,
                false,
                true);
        //make a custom description which is specified in descriptions.csv
        Embers.setCustomDescriptionId("Meng_Combus");
        Embers.setInteractionImage("illustrations", "eochu_bres");
        //give the orbital works a gamma core
        Market.getIndustry(Industries.MEGAPORT).setAICoreId(Commodities.BETA_CORE);
        Market.getIndustry(Industries.POPULATION).setAICoreId(Commodities.BETA_CORE);
        Market.getIndustry(Industries.HIGHCOMMAND).setAICoreId(Commodities.ALPHA_CORE);
        Market.getIndustry(Industries.STARFORTRESS_HIGH).setAICoreId(Commodities.ALPHA_CORE);
        Market.getIndustry(Industries.ORBITALWORKS).setAICoreId(Commodities.ALPHA_CORE);
        Market.getIndustry(Industries.ORBITALWORKS).setSpecialItem(new SpecialItemData(Items.PRISTINE_NANOFORGE, null));
        officer = Meng_Rocifer.getRocifer();
        Market.getCommDirectory().addPerson(officer);


        PlanetAPI Cardiac = system.addPlanet("Meng_Cardiac", embers, "Cardiac", "lava", 215, 180f, 4000f, 180f);
        //a new market for planet
        MarketAPI Market1 = addMarketplace("Meng_temple", Cardiac, null
                , Cardiac.getName(), 6,
                new ArrayList<>(
                        Arrays.asList(
                                Conditions.POPULATION_6, // population
                                Conditions.ORE_RICH,
                                Conditions.RARE_ORE_ULTRARICH,
                                Conditions.VERY_HOT,
                                Conditions.VOLATILES_PLENTIFUL,
                                Conditions.TECTONIC_ACTIVITY
                        )),
                new ArrayList<>(
                        Arrays.asList(
                                Submarkets.GENERIC_MILITARY,
                                Submarkets.SUBMARKET_BLACK,
                                Submarkets.SUBMARKET_OPEN,
                                Submarkets.SUBMARKET_STORAGE
                        )),
                new ArrayList<>(
                        Arrays.asList(
                                Industries.POPULATION,
                                Industries.MEGAPORT,
                                Industries.STARFORTRESS_HIGH,
                                Industries.MINING,
                                Industries.HIGHCOMMAND,
                                Industries.ORBITALWORKS,
                                Industries.WAYSTATION,
                                Industries.HEAVYBATTERIES,
                                Industries.FUELPROD,
                                Industries.MILITARYBASE
                        )),
                0.3f,
                false,
                true);
        //make a custom description which is specified in descriptions.csv
        Cardiac.setCustomDescriptionId("Meng_Cardiac");

        Cardiac.setInteractionImage("illustrations", "eochu_bres");
        //give the orbital works a gamma core
        Market1.getIndustry(Industries.MEGAPORT).setAICoreId(Commodities.GAMMA_CORE);
        Market1.getIndustry(Industries.POPULATION).setAICoreId(Commodities.GAMMA_CORE);
        Market1.getIndustry(Industries.HIGHCOMMAND).setAICoreId(Commodities.BETA_CORE);
        Market1.getIndustry(Industries.MINING).setAICoreId(Commodities.ALPHA_CORE);
        Market1.getIndustry(Industries.MINING).setSpecialItem(new SpecialItemData(Items.MANTLE_BORE, null));
        Market1.getIndustry(Industries.HIGHCOMMAND).setSpecialItem(new SpecialItemData(Items.CRYOARITHMETIC_ENGINE, null));
        Market1.getIndustry(Industries.ORBITALWORKS).setSpecialItem(new SpecialItemData(Items.CORRUPTED_NANOFORGE, null));


        JumpPointAPI jumppoint = Global.getFactory().createJumpPoint("embers_jump", "圣殿跳跃点");
        OrbitAPI the_orbit = Global.getFactory().createCircularOrbit(Embers, 90, 450f, 200f);
        jumppoint.setOrbit(the_orbit);
        jumppoint.setRelatedPlanet(Embers);
        jumppoint.setStandardWormholeToHyperspaceVisual();
        // 添加自定义标签用于rules系统识别
        jumppoint.getMemoryWithoutUpdate().set("$tag:Meng_embers_jump", true);
        // 添加HAS_INTERACTION_DIALOG标签，使JumpPoint使用基于rules的对话框而非默认的JumpPointInteractionDialogPluginImpl
        jumppoint.addTag(com.fs.starfarer.api.impl.campaign.ids.Tags.HAS_INTERACTION_DIALOG);
        system.addEntity(jumppoint);

        // generates hyperspace destinations for in-system jump points
        system.autogenerateHyperspaceJumpPoints(true, true);


    }


}
