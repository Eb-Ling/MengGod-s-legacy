package data.scripts.campaign.bar;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;

public class Cai_Search {
    public static final String Cai_Step_key = "$Cai_stepsearch";

    public static Cai_Step getStage() {
        FactionAPI targetFaction = Global.getSector().getFaction("independent");
        Object tryStage = targetFaction.getMemoryWithoutUpdate().get(Cai_Step_key);
        return tryStage == null ? Cai_Step.Cai_Step1 : (Cai_Step) tryStage;
    }

    public static void setStage(Cai_Step stage) {
        FactionAPI targetFaction = Global.getSector().getFaction("independent");
        targetFaction.getMemoryWithoutUpdate().set(Cai_Step_key, stage);
    }

    public enum Cai_Step {
        Cai_Step1,
        Cai_Step2,
        Cai_Step3,
        Cai_Step4


    }
}
