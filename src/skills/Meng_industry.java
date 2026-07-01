package data.skills;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketImmigrationModifier;
import com.fs.starfarer.api.characters.CharacterStatsSkillEffect;
import com.fs.starfarer.api.characters.MarketSkillEffect;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.population.PopulationComposition;
import data.scripts.campaign.Meng_mengmeng;

public class Meng_industry implements MarketImmigrationModifier {
    public static int SUPPLY_BONUS = 1;
    public static MarketImmigrationModifier Meng_industry = new Meng_industry();

    public void modifyIncoming(MarketAPI market, PopulationComposition incoming) {
        incoming.add("player", 10.0F);
        incoming.getWeight().modifyFlat(market.getFactionId(), -Math.round(market.getIncoming().getPositiveWeight() * 0.5f), "被监视的生活");
    }

    public static class Level1 implements CharacterStatsSkillEffect {
        public void apply(MutableCharacterStatsAPI stats, String id, float level) {
            stats.getDynamic().getMod(Stats.SUPPLY_BONUS_MOD).modifyFlat(id, 1, "运算调度");
            stats.getDynamic().getMod(Stats.DEMAND_REDUCTION_MOD).modifyFlat(id, 1, "运算调度");
        }

        public void unapply(MutableCharacterStatsAPI stats, String id) {
            stats.getDynamic().getMod(Stats.SUPPLY_BONUS_MOD).unmodifyFlat(id);
            stats.getDynamic().getMod(Stats.DEMAND_REDUCTION_MOD).unmodifyFlat(id);
        }

        public String getEffectDescription(float level) {
            return "所有工业设施产出增加与需求减少 " + SUPPLY_BONUS + " 额外单位。";
        }

        public String getEffectPerLevelDescription() {
            return null;
        }

        public ScopeDescription getScopeDescription() {
            return ScopeDescription.GOVERNED_OUTPOST;
        }
    }

    public static class Level2 implements MarketSkillEffect {
        public void apply(MarketAPI market, String id, float level) {
            market.getUpkeepMult().modifyMult(id, 1.2f, "虚拟星球维护");
        }

        public void unapply(MarketAPI market, String id) {
            market.getUpkeepMult().unmodifyMult(id);
        }

        public String getEffectDescription(float level) {
            return "+" + 20 + "% 殖民地维护费。";
        }

        public String getEffectPerLevelDescription() {
            return null;
        }

        public ScopeDescription getScopeDescription() {
            return ScopeDescription.GOVERNED_OUTPOST;
        }
    }

    public static class Level3 implements MarketSkillEffect {
        public void apply(MarketAPI market, String id, float level) {
            market.getIncomeMult().modifyPercent(id, 40f, "量子并行贸易");
        }

        public void unapply(MarketAPI market, String id) {
            market.getIncomeMult().unmodifyPercent(id);
        }

        public String getEffectDescription(float level) {
            return "+" + 40 + "% 殖民地收入。";
        }

        public String getEffectPerLevelDescription() {
            return null;
        }

        public ScopeDescription getScopeDescription() {
            return ScopeDescription.GOVERNED_OUTPOST;
        }
    }

    public static class Level4 implements MarketSkillEffect {
        public void apply(MarketAPI market, String id, float level) {
            market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).modifyFlat(id, 0.2f, "中央舰队演算");
        }

        public void unapply(MarketAPI market, String id) {
            market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).unmodifyFlat(id);
        }

        public String getEffectDescription(float level) {
            return "+" + 20 + "% 舰队规模。";
        }

        public String getEffectPerLevelDescription() {
            return null;
        }

        public ScopeDescription getScopeDescription() {
            return ScopeDescription.GOVERNED_OUTPOST;
        }
    }

    public static class Level5 implements MarketSkillEffect {
        public void apply(MarketAPI market, String id, float level) {
            market.getAccessibilityMod().modifyFlat(id, 0.4f, "量子航道规划");
        }

        public void unapply(MarketAPI market, String id) {
            market.getAccessibilityMod().unmodifyFlat(id);
        }

        public String getEffectDescription(float level) {
            return "+" + 40 + "% 殖民地流通性。";
        }

        public String getEffectPerLevelDescription() {
            return null;
        }

        public ScopeDescription getScopeDescription() {
            return ScopeDescription.GOVERNED_OUTPOST;
        }
    }

    public static class Level6 implements MarketSkillEffect {
        public void apply(MarketAPI market, String id, float level) {
            market.getStats().getDynamic().getMod(Stats.MAX_INDUSTRIES).modifyFlat(id, 1, "量子统筹");
        }

        public void unapply(MarketAPI market, String id) {
            market.getStats().getDynamic().getMod(Stats.MAX_INDUSTRIES).unmodifyFlat(id);
            market.getPopulation().getWeight().unmodifyMult(id);
        }

        public String getEffectDescription(float level) {
            return "+" + 1 + " 工业建筑上限。";
        }

        public String getEffectPerLevelDescription() {
            return null;
        }

        public ScopeDescription getScopeDescription() {
            return ScopeDescription.GOVERNED_OUTPOST;
        }
    }

    public static class Level7 implements MarketSkillEffect {
        PersonAPI person = Meng_mengmeng.getMengmeng();

        public void apply(MarketAPI market, String id, float level) {
            market.getStability().modifyFlat(id, 3, "最高演算");
            if (market.getCommDirectory().getEntryForPerson(person) == null) {
                market.getCommDirectory().addPerson(person, 1);
            }
        }

        public void unapply(MarketAPI market, String id) {
            market.getStability().unmodifyFlat(id);
            market.getPopulation().getWeight().unmodifyMult(id);
            market.removePerson(person);
            market.getCommDirectory().removePerson(person);
        }

        public String getEffectDescription(float level) {
            return "+" + 3 + " 殖民地稳定性。";
        }

        public String getEffectPerLevelDescription() {
            return null;
        }

        public ScopeDescription getScopeDescription() {
            return ScopeDescription.GOVERNED_OUTPOST;
        }
    }

    public static class Level8 implements MarketSkillEffect {
        public void apply(MarketAPI market, String id, float level) {
            market.addTransientImmigrationModifier(Meng_industry);
        }

        public void unapply(MarketAPI market, String id) {
            market.removeTransientImmigrationModifier(Meng_industry);
        }

        public String getEffectDescription(float level) {
            return "-" + 50 + "%殖民地发展度。";
        }

        public String getEffectPerLevelDescription() {
            return null;
        }

        public ScopeDescription getScopeDescription() {
            return ScopeDescription.GOVERNED_OUTPOST;
        }
    }
}
//	public static class Level1A implements CharacterStatsSkillEffect {
//
//		public void apply(MutableCharacterStatsAPI stats, String id, float level) {
//			stats.getDynamic().getMod(Stats.DEMAND_REDUCTION_MOD).modifyFlat(id, DEMAND_REDUCTION);
//		}
//
//		public void unapply(MutableCharacterStatsAPI stats, String id) {
//			stats.getDynamic().getMod(Stats.DEMAND_REDUCTION_MOD).unmodifyFlat(id);
//		}
//		
//		public String getEffectDescription(float level) {
//			return "All industries require " + DEMAND_REDUCTION + " less unit of all the commodities they need";
//		}
//		
//		public String getEffectPerLevelDescription() {
//			return null;
//		}
//
//		public ScopeDescription getScopeDescription() {
//			return ScopeDescription.GOVERNED_OUTPOST;
//		}
//	}
//	
//	public static class Level1B implements MarketSkillEffect {
//		public void apply(MarketAPI market, String id, float level) {
//			market.getUpkeepMult().modifyMult(id, UPKEEP_MULT, "Industrial planning");
//		}
//
//		public void unapply(MarketAPI market, String id) {
//			market.getUpkeepMult().unmodifyMult(id);
//		}
//		
//		public String getEffectDescription(float level) {
//			return "-" + (int)Math.round(Math.abs((1f - UPKEEP_MULT)) * 100f) + "% upkeep for colonies";
//		}
//		
//		public String getEffectPerLevelDescription() {
//			return null;
//		}
//
//		public ScopeDescription getScopeDescription() {
//			return ScopeDescription.GOVERNED_OUTPOST;
//		}
//	}
//	
//	public static class Level3A implements MarketSkillEffect {
//		public void apply(MarketAPI market, String id, float level) {
//			market.getIncomeMult().modifyMult(id, INCOME_MULT, "Industrial planning");
//		}
//
//		public void unapply(MarketAPI market, String id) {
//			market.getIncomeMult().unmodifyMult(id);
//		}
//		
//		public String getEffectDescription(float level) {
//			return "+" + (int)Math.round((INCOME_MULT - 1f) * 100f) + "% income from colonies, including exports";
//		}
//		
//		public String getEffectPerLevelDescription() {
//			return null;
//		}
//
//		public ScopeDescription getScopeDescription() {
//			return ScopeDescription.GOVERNED_OUTPOST;
//		}
//	}

//	public static void main(String[] args) {
//		System.out.println((int)((1.331 - 1.) * 1000.));
//	}
	



