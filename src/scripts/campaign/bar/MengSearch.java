package data.scripts.campaign.bar;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;

public class MengSearch {
    public static final String Meng_Step_key = "$Meng_stepsearch";

    public static MengStep getStage() {
        FactionAPI targetFaction = Global.getSector().getFaction("independent");
        Object tryStage = targetFaction.getMemoryWithoutUpdate().get(Meng_Step_key);
        return tryStage == null ? MengStep.Meng_Step1 : (MengStep) tryStage;
    }

    public static void setStage(MengStep stage) {
        FactionAPI targetFaction = Global.getSector().getFaction("independent");
        targetFaction.getMemoryWithoutUpdate().set(Meng_Step_key, stage);
    }

    public enum MengStep {
        Meng_Step1,
        Meng_Step2,
        Meng_Step3,
        Meng_Step4,
        Meng_Step5,
        Meng_Step6,
        Meng_Step7,
        Meng_Step8,
        Meng_Step9,
        Meng_Step10,
        Meng_Step11,
        Meng_Step12,
    }
}
