package data.scripts.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.Meng_mengmeng;

import java.awt.*;
import java.util.List;
import java.util.Map;

import static java.util.logging.Logger.global;

public class Meng_relationadd extends BaseCommandPlugin {
    public static final String Mengmeng_key = "$Mengmeng_key";
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        if(dialog==null){
            return  false;
        }
        int num=params.get(0).getInt(memoryMap);
        PersonAPI person= Meng_mengmeng.getMengmeng();
        String personname=person.getName().getFullName();
        person.getRelToPlayer().setRel(0.01f*num+person.getRelToPlayer().getRel());
        dialog.getTextPanel().addParagraph(personname+"的好感度 +"+num,new Color(255, 0,255));
        FactionAPI targetFaction = Global.getSector().getFaction("independent");
        targetFaction.getMemoryWithoutUpdate().set(Mengmeng_key,person);
        return  true;
    }
}
