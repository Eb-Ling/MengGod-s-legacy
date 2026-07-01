package data.scripts.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.PersonImportance;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;

public class Meng_mengmeng {
    public static final String Mengmeng_key = "$Mengmeng_key";
    public static PersonAPI getMengmeng() {
        FactionAPI targetFaction = Global.getSector().getFaction("independent");
        if(targetFaction.getMemoryWithoutUpdate().get(Mengmeng_key)==null){
            float relation=0f;
            PersonAPI mengmeng = Global.getFactory().createPerson();
            mengmeng.setId("Mengmeng");
            mengmeng.setPortraitSprite(Global.getSettings().getSpriteName("intel", "MouMeng"));
            mengmeng.setName(new FullName("Meng", "meng", FullName.Gender.FEMALE));
            mengmeng.getRelToPlayer().setRel(relation);
            mengmeng.setGender(FullName.Gender.FEMALE);
            mengmeng.setImportance(PersonImportance.VERY_HIGH);
            targetFaction.getMemoryWithoutUpdate().set(Mengmeng_key,mengmeng);
            return mengmeng;
        }
        else {
            return (PersonAPI) targetFaction.getMemoryWithoutUpdate().get(Mengmeng_key);
        }

    }

}
