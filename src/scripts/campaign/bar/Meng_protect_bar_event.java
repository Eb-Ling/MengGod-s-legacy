package data.scripts.campaign.bar;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.AdminData;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEventWithPerson;
import com.fs.starfarer.api.plugins.OfficerLevelupPlugin;
import data.Utils.I18nUtil;
import data.scripts.campaign.intel.Meng_eventtitle;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Meng_protect_bar_event extends BaseBarEventWithPerson {

    public static String MengKey = "$Meng_officer";
    protected CampaignFleetAPI player_fleet;
    protected AdminData player_admin;
    protected PersonAPI officer;
    protected PersonAPI officer1;
    protected PersonAPI officer2;
    protected OfficerDataAPI officer1_data;
    protected OfficerDataAPI officer2_data;
    protected OfficerDataAPI officer_data;

    @Override
    public boolean shouldShowAtMarket(MarketAPI market) {
        Global.getLogger(this.getClass()).info("Eventison" + market.getName());
        if (!super.shouldShowAtMarket(market)) return false;
        boolean stage = MengSearch.getStage() == MengSearch.MengStep.Meng_Step1;
        return stage;
    }

    protected void regen(MarketAPI market) {
        if (this.market == market) return;
        super.regen(market);
        person.setId("Mengmeng");
        person.setPortraitSprite(Global.getSettings().getSpriteName("intel", "MouMeng"));
        person.setName(new FullName("Meng", "meng", FullName.Gender.FEMALE));
    }

    @Override
    public boolean isAlwaysShow() {
        return true;
    }

    @Override
    public void init(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
        super.init(dialog, memoryMap);

        done = false;
        dialog.getVisualPanel().showPersonInfo(person, true);
        optionSelected(null, OptionId.AFTER_ACT_1);
    }

    @Override
    public void addPromptAndOption(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
        regen(dialog.getInteractionTarget().getMarket());


        TextPanelAPI text = dialog.getTextPanel();
        text.addPara(I18nUtil.getString("event","Meng_Opentalk_1"));

        Color c = person.getFaction().getColor();
        dialog.getOptionPanel().addOption(I18nUtil.getString("event","Meng_Opentalk_Option_1"), this, new Color(243, 35, 191, 255), null);

    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        if (!(optionData instanceof OptionId)) {
            return;

        }

        OptionId option = (OptionId) optionData;
        OptionPanelAPI options = dialog.getOptionPanel();
        TextPanelAPI text = dialog.getTextPanel();
        options.clearOptions();

        boolean portraitActive = false;
        if (portraitActive) {

        }

        switch (option) {
            case AFTER_ACT_1:
                text.addPara(I18nUtil.getString("event","Meng_Talk_1"));
                text.addPara(I18nUtil.getString("event","Meng_Talk_2") +"\n"+
                        I18nUtil.getString("event","Meng_Talk_3"));
                options.addOption(I18nUtil.getString("event","Meng_Option_1"), OptionId.ACCEPT);
                options.addOption(I18nUtil.getString("event","Meng_Option_2"), OptionId.LEAVE);

                break;
            case ACCEPT:
                text.addPara(I18nUtil.getString("event","Meng_Talk_4"));
                text.addPara(I18nUtil.getString("event","Meng_Talk_5"));

                options.addOption(I18nUtil.getString("event","Meng_Continue"), OptionId.CONTINUE1);

                break;
            case LEAVE:
                text.addPara(I18nUtil.getString("event","Meng_Leave"));
                leave();
                MengSearch.setStage(MengSearch.MengStep.Meng_Step3);
                break;
            case CONTINUE1:
                text.addPara(I18nUtil.getString("event","Meng_Talk_6") +
                        I18nUtil.getString("event","Meng_Talk_7") );
                text.addPara(I18nUtil.getString("event","Meng_Talk_8") +
                        I18nUtil.getString("event","Meng_Talk_9"));

                options.addOption(I18nUtil.getString("event","Meng_Continue"), OptionId.CONTINUE2);
                break;
            case CONTINUE2:
                person.setPortraitSprite(Global.getSettings().getSpriteName("intel", "MouMeng"));
                text.addPara(I18nUtil.getString("event","Meng_Talk_10"));
                text.addPara(I18nUtil.getString("event","Meng_Talk_11"));

                options.addOption(I18nUtil.getString("event","Meng_Continue"), OptionId.CONTINUE3);
                break;
            case CONTINUE3:

                text.addPara(I18nUtil.getString("event","Meng_Talk_12"));
                text.addPara(I18nUtil.getString("event","Meng_Talk_13"));

                options.addOption(I18nUtil.getString("event","Meng_Option_3"), OptionId.CONTINUE4);
                options.addOption(I18nUtil.getString("event","Meng_Option_4"), OptionId.LEAVE);
                break;

            case CONTINUE4:
                person.setPortraitSprite(Global.getSettings().getSpriteName("intel", "MouMeng"));
                text.addPara(I18nUtil.getString("event","Meng_Talk_14"));
                text.addPara(I18nUtil.getString("event","Meng_Talk_15"));
                text.addPara(I18nUtil.getString("event","Meng_Talk_16"), new Color(255, 132, 132, 255));

                List<String> meng = new ArrayList<>();
                meng.add("Meng_skill1");
                meng.add("Meng_skill");

                List<String> meng1 = new ArrayList<>();
                meng1.add("Meng_industry");

                List<String> cai1 = new ArrayList<>();
                cai1.add("Cai_skills");
                cai1.add("Cai_skill");
                //testing
                //doExtraConfirmActions();
                if (!Global.getSector().getMemoryWithoutUpdate().contains(MengKey)) {
                    Global.getSector().getMemoryWithoutUpdate().set(MengKey, true);
                }

                officer = Global.getFactory().createPerson();
                officer1 = Global.getFactory().createPerson();
                officer2 = Global.getFactory().createPerson();
                officer_data = Global.getFactory().createOfficerData(officer);
                officer1_data = Global.getFactory().createOfficerData(officer);
                officer2_data = Global.getFactory().createOfficerData(officer);
                for (String Cai_skill : cai1) {
                    officer2.getStats().setSkillLevel(Cai_skill, 3);
                }
                for (String Cai_skill : cai1) {
                    officer2.getStats().setSkillLevel(Cai_skill, 3);
                }
                for (String Meng_skill : meng) {
                    officer.getStats().setSkillLevel(Meng_skill, 3);
                }
                for (String Meng_skill : meng) {
                    officer.getStats().setSkillLevel(Meng_skill, 3);
                }
                for (String Meng_skill : meng1) {
                    officer1.getStats().setSkillLevel(Meng_skill, 1);
                }
                OfficerLevelupPlugin plugin = (OfficerLevelupPlugin) Global.getSettings().getPlugin("officerLevelUp");
                officer.getMemoryWithoutUpdate().set(MemFlags.OFFICER_MAX_ELITE_SKILLS,20f);
                officer.getMemoryWithoutUpdate().set(MemFlags.OFFICER_MAX_LEVEL,14f);
                officer2.getMemoryWithoutUpdate().set(MemFlags.OFFICER_MAX_ELITE_SKILLS,20f);
                officer2.getMemoryWithoutUpdate().set(MemFlags.OFFICER_MAX_LEVEL,14f);
                officer.getStats().addXP(plugin.getXPForLevel(1));
                officer1.getStats().addXP(plugin.getXPForLevel(1));
                officer2.getStats().addXP(plugin.getXPForLevel(1));

                officer1.setPersonality(Personalities.AGGRESSIVE);
                officer1.setName(person.getName());
                officer1.setPortraitSprite(person.getPortraitSprite());
                officer1.setGender(person.getGender());
                officer1.setPostId(Ranks.POST_ADMINISTRATOR);
                Global.getSector().getCharacterData().addAdmin(officer1);

                officer.setPersonality(Personalities.AGGRESSIVE);
                officer.setName(person.getName());
                officer.setPortraitSprite(person.getPortraitSprite());
                officer.setGender(person.getGender());
                officer2.setPersonality(Personalities.AGGRESSIVE);
                officer2.setName(new FullName("小蔡", " ", FullName.Gender.FEMALE));
                officer2.setPortraitSprite(Global.getSettings().getSpriteName("intel", "Meng_Cai"));
                officer2.setGender(person.getGender());

                player_fleet = Global.getSector().getPlayerFleet();
                player_fleet.getFleetData().addOfficer(officer);
                player_fleet.getFleetData().addOfficer(officer2);

                options.addOption("结束对话", OptionId.CONTINUE7);
                Global.getSector().getMemoryWithoutUpdate().set("$Meng_protecttime", 1, 90);
                break;
            case CONTINUE7:
                leave();
                MengSearch.setStage(MengSearch.MengStep.Meng_Step2);
                Meng_eventtitle intel=new Meng_eventtitle(dialog);

                end(true);
                break;
        }

    }


    private void leave() {
        done = true;

    }

    private void end(boolean giveUp) {
        leave();
        BarEventManager.getInstance().notifyWasInteractedWith(this);


    }


    private enum OptionId {
        AFTER_ACT_1,
        ACCEPT,
        LEAVE,
        CONTINUE1,
        CONTINUE2,
        CONTINUE3,
        CONTINUE4,
        CONTINUE7,
    }
}