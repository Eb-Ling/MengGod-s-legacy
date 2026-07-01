package data.scripts.campaign.bar;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;

import static data.scripts.campaign.bar.Meng_Search.MengIntel_Step.MengIntel_Step1;

public class Meng_Search {
    public static final String Meng_IntelStep_key = "$Mengintel_stepsearch";

    public static MengIntel_Step getStage() {
        FactionAPI targetFaction = Global.getSector().getFaction("independent");
        Object tryStage = targetFaction.getMemoryWithoutUpdate().get(Meng_IntelStep_key);
        return tryStage == null ? MengIntel_Step.MengIntel_Step1 : (MengIntel_Step) tryStage;
    }

    public static void setStage(MengIntel_Step stage) {
        FactionAPI targetFaction = Global.getSector().getFaction("independent");
        targetFaction.getMemoryWithoutUpdate().set(Meng_IntelStep_key, stage);
    }

    public enum MengIntel_Step {
        MengIntel_Step1,
        MengIntel_Step2,

    }
}
