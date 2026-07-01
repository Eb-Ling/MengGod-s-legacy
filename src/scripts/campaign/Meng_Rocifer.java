package data.scripts.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.PersonImportance;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;

public class Meng_Rocifer {
    public static final String Rocifer_key = "$Rocifer_key";
    public static PersonAPI getRocifer() {
        FactionAPI targetFaction = Global.getSector().getFaction("Meng_temple");
        if(targetFaction.getMemoryWithoutUpdate().get(Rocifer_key)==null){
            float relation=0f;
            PersonAPI Rocifer = Global.getFactory().createPerson();
            Rocifer.setId("Meng_Rocifer");
            Rocifer.setPortraitSprite(Global.getSettings().getSpriteName("characters","Meng_Luciflux"));
            Rocifer.setName(new FullName("Rocifer", " ", FullName.Gender.ANY));
            Rocifer.getRelToPlayer().setRel(relation);
            Rocifer.setGender(FullName.Gender.ANY);
            Rocifer.setPostId("factionAILeader");
            Rocifer.setImportance(PersonImportance.VERY_HIGH);
            Rocifer.setFaction("Meng_temple");
            targetFaction.getMemoryWithoutUpdate().set(Rocifer_key,Rocifer);
            return Rocifer;
        }
        else {
            return (PersonAPI) targetFaction.getMemoryWithoutUpdate().get(Rocifer_key);
        }

    }

}
