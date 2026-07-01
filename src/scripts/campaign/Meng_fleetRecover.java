package data.scripts.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.util.Misc;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class Meng_fleetRecover extends FleetEncounterContext {
    @Override
    public List<FleetMemberAPI> getRecoverableShips(BattleAPI battle, CampaignFleetAPI winningFleet, CampaignFleetAPI otherFleet) {
        List<FleetMemberAPI> result = super.getRecoverableShips(battle, winningFleet, otherFleet);
        if (Misc.isPlayerOrCombinedContainingPlayer(otherFleet)) {
            return result;
        }
        DataForEncounterSide winnerData = getDataFor(winningFleet);

        float playerContribMult = computePlayerContribFraction();
        List<FleetMemberData> enemyCasualties = winnerData.getEnemyCasualties();
        Set<String> recoveredTypes = new HashSet<>();
        for (FleetMemberAPI member : result) {
            if (member.getHullSpec().getTags().contains("Meng_embers")) {
                recoveredTypes.add(member.getHullSpec().getHullId());
            }
        }
        for (FleetMemberData data : enemyCasualties) {
            if (Misc.isUnboardable(data.getMember())) {
                continue;
            }
            if ((data.getStatus() != Status.DISABLED) && (data.getStatus() != Status.DESTROYED)) {
                continue;
            }
            if (result.contains(data.getMember())) {
                continue;
            }
            if (getStoryRecoverableShips().contains(data.getMember())) {
                continue;
            }
            if (recoveredTypes.contains(data.getMember().getHullSpec().getHullId())) {
                continue;
            }
            if (playerContribMult > 0f) {
                data.getMember().setCaptain(Global.getFactory().createPerson());

                ShipVariantAPI variant = data.getMember().getVariant();
                variant = variant.clone();
                variant.setSource(VariantSource.REFIT);
                variant.setOriginalVariant(null);
                data.getMember().setVariant(variant, false, true);

                Random dModRandom = new Random(1000000L * data.getMember().getId().hashCode() + Global.getSector().getPlayerBattleSeed());
                dModRandom = Misc.getRandom(dModRandom.nextLong(), 5);
                DModManager.addDMods(data, false, Global.getSector().getPlayerFleet(), dModRandom);
                if (DModManager.getNumDMods(variant) > 0) {
                    DModManager.setDHull(variant);
                }

                float weaponProb = Global.getSettings().getFloat("salvageWeaponProb");
                float wingProb = Global.getSettings().getFloat("salvageWingProb");


                prepareShipForRecovery(data.getMember(), false, true, true, weaponProb, wingProb, getSalvageRandom());

                getStoryRecoverableShips().add(data.getMember());
                recoveredTypes.add(data.getMember().getHullSpec().getHullId());
            }
        }
        return result;
    }
}
