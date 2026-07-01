package data.scripts.campaign.bar;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;

public class MouMeng_Search {
    public static final String MouMeng_Step_key = "$MouMeng_stepsearch";

    public static MouMengStep getStage() {
        FactionAPI targetFaction = Global.getSector().getFaction("independent");
        Object tryStage = targetFaction.getMemoryWithoutUpdate().get(MouMeng_Step_key);
        return tryStage == null ? MouMengStep.MouMeng_Step1 : (MouMengStep) tryStage;
    }

    public static void setStage(MouMengStep stage) {
        FactionAPI targetFaction = Global.getSector().getFaction("independent");
        targetFaction.getMemoryWithoutUpdate().set(MouMeng_Step_key, stage);
    }

    public enum MouMengStep {
        MouMeng_Step1,
        MouMeng_Step2,
        MouMeng_Step3,


    }
}
