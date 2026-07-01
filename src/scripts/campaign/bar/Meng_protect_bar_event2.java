package data.scripts.campaign.bar;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEventWithPerson;
import data.scripts.campaign.intel.Meng_timefleetintel;

import java.awt.*;
import java.util.Map;

public class Meng_protect_bar_event2 extends BaseBarEventWithPerson {

    public static String MengKey = "$Meng_officer";
    private final boolean portraitActive = false;
    protected CampaignFleetAPI player_fleet;
    protected PersonAPI officer;
    protected OfficerDataAPI officer_data;
    private Meng_timefleetintel intel = null;
    private MarketAPI market;
    private int requiredAmount;
    private String reward;
    private float timeLeft;

    public boolean shouldShowAtMarket(MarketAPI market) {
        Global.getLogger(this.getClass()).info("Eventison" + market.getName());

        if (!super.shouldShowAtMarket(market)) return false;
        if (!market.getFactionId().equals("Meng_temple")) return false;
        boolean stage = MengSearch.getStage() == MengSearch.MengStep.Meng_Step5;
        return stage;

    }

    protected void regen(MarketAPI market) {
        if (this.market == market) return;
        super.regen(market);

        person.setPortraitSprite(Global.getSettings().getSpriteName("intel","Meng_intel"));
        person.setName(new FullName("圣殿通知", " ", FullName.Gender.FEMALE));
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

    public void addPromptAndOption(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
        regen(dialog.getInteractionTarget().getMarket());


        TextPanelAPI text = dialog.getTextPanel();
        text.addPara("你正在环视四周，突然发现从神降舰队上找到的通讯设备收到了信息。");

        Color c = person.getFaction().getColor();

        dialog.getOptionPanel().addOption("查看信息", this, new Color(243, 35, 191, 255), null);
    }

    public void optionSelected(String optionText, Object optionData) {
        if (!(optionData instanceof Meng_protect_bar_event2.OptionId)) {
            return;
        }

        Meng_protect_bar_event2.OptionId option = (Meng_protect_bar_event2.OptionId) optionData;
        OptionPanelAPI options = dialog.getOptionPanel();
        TextPanelAPI text = dialog.getTextPanel();
        options.clearOptions();

        if (portraitActive) {

        }

        switch (option) {
            case AFTER_ACT_1:
                text.addPara("面板上弹出了一份邮件，封面上第一句话就吸引住了你。\"所有圣殿成员：A级悬赏，'阶梯计划'的二号神格'时旅者'失联。\"" +
                        "其中是大片的隐藏信息，你知道需要接受悬赏才能看到。");
                options.addOption("点击接受悬赏", Meng_protect_bar_event2.OptionId.ACCEPT);
                options.addOption("关闭面板", OptionId.REALYLEAVE);

                break;
            case ACCEPT:
                text.addPara("你接受了悬赏，面板上邮件拆封，一段也许是标志着这个势力的cg开始播放，雪白的树伸展出无数的枝丫直向天际，树梢上缀着六颗明星，像是托起了整片寰宇。");
                text.addPara("终于，邮件的内容浮现：\"Temple of God：'欢迎接受赏金，教徒编号3145，权限：B，任务内容：'圣殿的探索者，" +
                        "时旅者级时间信标于星系任务中失联，我们都知道，这是不可能的事情。作为圣殿神阶的第二段基石，整片星域也不会存在对它的威胁，但这一切的确发生了。'\"");
                options.addOption("继续", Meng_protect_bar_event2.OptionId.CONTINUE1);
                break;
            case REALYLEAVE:
                text.addPara("你关闭了面板，从此不再想它。");
                leave();
                MengSearch.setStage(MengSearch.MengStep.Meng_Step3);
                break;
            case CONTINUE1:
                text.addPara("\"所以，这次的任务被评定为A级，你只需要找到时旅者的所在位置，并让它与圣殿重新建立联系。长老院推测大概率是因为通讯受阻的原因，你只需要确认情况。\"");
                text.addPara("\"但是我们依旧会向你提供情报，接下来的内容属于机密条例：时旅者的核心是名为'噬时之瓶'的武器，它赋予了时旅者级时间倒流的能力，" +
                        "可以随意的回到过去25秒内的任意一秒，这也是它肩负最困难探索任务的原因。同时，舰船可以引导出噬时之瓶的能量，扭曲时间规律，达到时间停止的效果。\"");

                options.addOption("继续", Meng_protect_bar_event2.OptionId.CONTINUE2);
                break;
            case CONTINUE2:
                text.addPara("\"也许听起来它不可战胜，但是神座之下的造物终有缺陷，扭曲时间需要名为时间源点的特殊能量，只有噬时之瓶可以每4秒收集一点这种能量，" +
                        "因此它的这种能力是有极限的，这也是它最大的弱点。\"");
                text.addPara("\"任务内容，完毕，3145，请接受赏金位置，尽快完成任务。\"");

                options.addOption("继续", Meng_protect_bar_event2.OptionId.CONTINUE3);
                break;
            case CONTINUE3:
                text.addPara("你的思绪乱了起来，原来这那支可怕的神降舰队是隶属于这个神秘的圣殿组织的舰船，不过似乎遭受了什么圣殿不知道的意外，而现在，" +
                        "一个更复杂的任务摆在了你面前，寻找一艘听起来拥有神话般能力的舰船？");
                text.addPara("任务的坐标已经发送到你的面板，你需要仔细考虑是否前去，一旦介入其中，可能就无法脱身了。");
                options.addOption("查看坐标地点", Meng_protect_bar_event2.OptionId.CONTINUE8);
                options.addOption("关闭面板", OptionId.REALYLEAVE);
                break;
            case CONTINUE8:
                text.addPara("你查看了任务地点，是一个相当偏远的星系，你的眉头皱了起来。这时身边的萌萌似乎想说什么，但终于沉默了下去。");
                text.addPara("你的任务日志已经更新");
                text.addPara("任务危险程度极高，请提高警惕！", new Color(23, 245, 253, 255));
                intel = new Meng_timefleetintel(dialog);
                leave();
                MengSearch.setStage(MengSearch.MengStep.Meng_Step6);
                Global.getSector().getMemoryWithoutUpdate().set("$Meng_timetime", 1, 90);
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

    public FactionAPI getFactionForUIColors() {
        return Global.getSector().getFaction("independent");
    }


    private enum OptionId {
        REALYLEAVE,
        AFTER_ACT_1,
        ACCEPT,
        LEAVE,
        CONTINUE1,
        CONTINUE2,
        CONTINUE3,

        CONTINUE8,
    }
}


