package data.scripts.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.Meng_Rocifer;
import data.scripts.campaign.Meng_mengmeng;

import java.awt.*;
import java.util.List;
import java.util.Map;

public class Meng_Templerelationadd extends BaseCommandPlugin {
    public static final String Rocifer_key = "$Rocifer_key";
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        if(dialog==null){
            return  false;
        }
        int num=params.get(0).getInt(memoryMap);

        PersonAPI person= Meng_Rocifer.getRocifer();
        FactionAPI targetFaction = person.getFaction();
        targetFaction.getRelToPlayer().setRel(0.01f*num+targetFaction.getRelToPlayer().getRel());
        dialog.getTextPanel().addParagraph("你与余烬圣殿的关系 +"+num,new Color(173, 235, 255));
        targetFaction.getMemoryWithoutUpdate().set(Rocifer_key,person);
        return  true;
    }
}
