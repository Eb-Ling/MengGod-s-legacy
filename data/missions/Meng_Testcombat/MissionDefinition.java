package data.missions.Meng_Testcombat;

import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;

public class MissionDefinition implements MissionDefinitionPlugin {

	public void defineMission(MissionDefinitionAPI api) {

		// Set up the fleets so we can add ships and fighter wings to them.
		// In this scenario, the fleets are attacking each other, but
		// in other scenarios, a fleet may be defending or trying to escape
		api.initFleet(FleetSide.PLAYER, "ISS", FleetGoal.ATTACK, false, 5);
		api.initFleet(FleetSide.ENEMY, "", FleetGoal.ATTACK, true);

		// Set a small blurb for each fleet that shows up on the mission detail and
		// mission results screens to identify each side.
		api.setFleetTagline(FleetSide.PLAYER, "Embers学院最后的防线");
		api.setFleetTagline(FleetSide.ENEMY, "势力联合舰队");
		
		// These show up as items in the bulleted list under 
		// "Tactical Objectives" on the mission detail screen
		api.addBriefingItem("击败所有敌军");
		api.addBriefingItem("星落必须存活");
		api.addBriefingItem("利用机动性与成长性拖延时间");
		// Set up the player's fleet.  Variant names come from the
		// files in data/variants and data/variants/fighters
		
		// Set up the player's fleet.  Variant names come from the
		// files in data/variants and data/variants/fighters
		//api.addToFleet(FleetSide.PLAYER, "afflictor_Strike", FleetMemberType.SHIP, "ISS Black Star", true, CrewXPLevel.VETERAN);
		//api.addToFleet(FleetSide.PLAYER, "station_small_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "Meng_MouMengship_variant", FleetMemberType.SHIP, "星落", true);
		api.addToFleet(FleetSide.PLAYER, "Meng_Starkiller_end", FleetMemberType.SHIP, "异宙龙", true);
		api.addToFleet(FleetSide.PLAYER, "Meng_MingGod_variant", FleetMemberType.SHIP, "冥河", true);
		api.addToFleet(FleetSide.PLAYER, "Meng_Xushu_variant", FleetMemberType.SHIP, "虚数", true);
		api.addToFleet(FleetSide.PLAYER, "Meng_protect_boss1_variant", FleetMemberType.SHIP, "神性攻势", true);
		api.addToFleet(FleetSide.PLAYER, "Meng_Yuanzhi_variant", FleetMemberType.SHIP, "源质星", true);
		api.addToFleet(FleetSide.PLAYER, "Meng_fire_spark_Standard", FleetMemberType.SHIP, "星火", true);
		api.addToFleet(FleetSide.PLAYER, "Meng_fire_firewood_Attack", FleetMemberType.SHIP, "薪木", true);
		api.addToFleet(FleetSide.PLAYER, "Meng_fire_arsonist_Standard", FleetMemberType.SHIP, "纵火者", true);
		api.addToFleet(FleetSide.PLAYER, "Meng_fire_firesmoke_Standard", FleetMemberType.SHIP, "烟火", true);
		api.addToFleet(FleetSide.PLAYER, "Meng_fire_firewatch_attack", FleetMemberType.SHIP, "助燃", true);
		api.addToFleet(FleetSide.PLAYER, "Meng_fire_fuse_Standard", FleetMemberType.SHIP, "导火索", true);
		api.addToFleet(FleetSide.PLAYER, "Meng_fire_redfeather_Standard", FleetMemberType.SHIP, "赤羽", true);
		api.addToFleet(FleetSide.PLAYER, "Meng_fire_endless_variant", FleetMemberType.SHIP, "不尽", true);
		api.addToFleet(FleetSide.PLAYER, "Meng_fire_StageFire_Standard", FleetMemberType.SHIP, "驿烛", true);
		api.addToFleet(FleetSide.PLAYER, "Meng_fire_offlight_Standard", FleetMemberType.SHIP, "熄灯人", true);
		api.addToFleet(FleetSide.PLAYER, "Meng_fire_firelight_Standard", FleetMemberType.SHIP, "逐光者", true);
		api.addToFleet(FleetSide.PLAYER, "Meng_fire_brandwing_Standard", FleetMemberType.SHIP, "燔翼", true);
		api.addToFleet(FleetSide.PLAYER, "Meng_fire_li_variant", FleetMemberType.SHIP, "璃", true);
		api.addToFleet(FleetSide.PLAYER, "Meng_Noname_variant", FleetMemberType.SHIP, "无名", true);
		api.addToFleet(FleetSide.PLAYER, "Meng_timeboss_variant", FleetMemberType.SHIP, "时旅者", true);

		//fleetMember = api.addToFleet(FleetSide.PLAYER, "buffalo_tritachyon_Standard", FleetMemberType.SHIP, false);
		//fleetMember = api.addToFleet(FleetSide.PLAYER, "falcon_CS", FleetMemberType.SHIP, false);
		
//		api.addToFleet(FleetSide.PLAYER, "mining_drone_wing", FleetMemberType.FIGHTER_WING, false);
//		api.addToFleet(FleetSide.PLAYER, "mining_drone_wing", FleetMemberType.FIGHTER_WING, false);
		
//		api.addToFleet(FleetSide.PLAYER, "enforcer_Assault", FleetMemberType.SHIP, "ISS Hamatsu", true);
//		api.addToFleet(FleetSide.PLAYER, "medusa_PD", FleetMemberType.SHIP, false);
//		api.addToFleet(FleetSide.PLAYER, "omen_PD", FleetMemberType.SHIP, false);
//		api.addToFleet(FleetSide.PLAYER, "hyperion_Attack", FleetMemberType.SHIP, false);
//		api.addToFleet(FleetSide.PLAYER, "wolf_CS", FleetMemberType.SHIP, false);
//		api.addToFleet(FleetSide.PLAYER, "medusa_Attack", FleetMemberType.SHIP, false);
//		api.addToFleet(FleetSide.PLAYER, "tempest_Attack", FleetMemberType.SHIP, false);
		
		//api.addToFleet(FleetSide.PLAYER, "mining_drone_wing", FleetMemberType.FIGHTER_WING, false);
		//api.addToFleet(FleetSide.PLAYER, "mining_drone_wing", FleetMemberType.FIGHTER_WING, false);
		//api.addToFleet(FleetSide.PLAYER, "mining_drone_wing", FleetMemberType.FIGHTER_WING, false);
		//api.addToFleet(FleetSide.PLAYER, "longbow_wing", FleetMemberType.FIGHTER_WING, false);
		//api.addToFleet(FleetSide.PLAYER, "longbow_wing", FleetMemberType.FIGHTER_WING, false);
		//api.addToFleet(FleetSide.PLAYER, "longbow_wing", FleetMemberType.FIGHTER_WING, false);
		//api.addToFleet(FleetSide.PLAYER, "longbow_wing", FleetMemberType.FIGHTER_WING, false);
		
		// Mark both ships as essential - losing either one results
		// in mission failure. Could also be set on an enemy ship,
		// in which case destroying it would result in a win.
		api.defeatOnShipLoss("星落");
		
		// Set up the enemy fleet.
		// It's got more ships than the player's, but they're not as strong.
		//api.addToFleet(FleetSide.ENEMY, "station_small_Standard", FleetMemberType.SHIP, false).getCaptain().setPersonality(Personalities.AGGRESSIVE);
		
		api.addToFleet(FleetSide.ENEMY, "Meng_timeboss_variant", FleetMemberType.SHIP, false).getCaptain().setPersonality(Personalities.AGGRESSIVE);
		api.addToFleet(FleetSide.ENEMY, "legion_Assault", FleetMemberType.SHIP, false).getCaptain().setPersonality(Personalities.AGGRESSIVE);
		api.addToFleet(FleetSide.ENEMY, "conquest_Elite", FleetMemberType.SHIP, false).getCaptain().setPersonality(Personalities.AGGRESSIVE);
		api.addToFleet(FleetSide.ENEMY, "falcon_Attack", FleetMemberType.SHIP, false).getCaptain().setPersonality(Personalities.AGGRESSIVE);
		api.addToFleet(FleetSide.ENEMY, "tempest_Attack", FleetMemberType.SHIP, false).getCaptain().setPersonality(Personalities.AGGRESSIVE);
		api.addToFleet(FleetSide.ENEMY, "scarab_Experimental", FleetMemberType.SHIP, false).getCaptain().setPersonality(Personalities.AGGRESSIVE);
		api.addToFleet(FleetSide.ENEMY, "scarab_Experimental", FleetMemberType.SHIP, false).getCaptain().setPersonality(Personalities.AGGRESSIVE);
		
		
		
		// Set up the map.
		float width = 5000f;
		float height = 5000f;
		api.initMap((float)-width/2f, (float)width/2f, (float)-height/2f, (float)height/2f);
		
		float minX = -width/2;
		float minY = -height/2;
		
		// All the addXXX methods take a pair of coordinates followed by data for
		// whatever object is being added.
		
		// Add two big nebula clouds
		api.addNebula(minX + width * 0.75f, minY + height * 0.5f, 2000);
		api.addNebula(minX + width * 0.25f, minY + height * 0.5f, 1000);
		
		// And a few random ones to spice up the playing field.
		// A similar approach can be used to randomize everything
		// else, including fleet composition.
		for (int i = 0; i < 5; i++) {
			float x = (float) Math.random() * width - width/2;
			float y = (float) Math.random() * height - height/2;
			float radius = 100f + (float) Math.random() * 400f; 
			api.addNebula(x, y, radius);
		}
		
		// Add objectives. These can be captured by each side
		// and provide stat bonuses and extra command points to
		// bring in reinforcements.
		// Reinforcements only matter for large fleets - in this
		// case, assuming a 100 command point battle size,
		// both fleets will be able to deploy fully right away.
	
		
		// Add an asteroid field going diagonally across the
		// battlefield, 2000 pixels wide, with a maximum of 
		// 100 asteroids in it.
		// 20-70 is the range of asteroid speeds.
		api.addAsteroidField(minY, minY, 45, 2000f,
								20f, 70f, 100);
		
		// Add some planets.  These are defined in data/config/planets.json.
		
	}

}






