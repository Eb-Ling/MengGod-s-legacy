package data.scripts.campaign.bar;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEventWithPerson;

import java.awt.*;
import java.util.Map;

public class MouMeng_bar_event1 extends BaseBarEventWithPerson {


    private final boolean portraitActive = false;

    public boolean shouldShowAtMarket(MarketAPI market) {
        Global.getLogger(this.getClass()).info("Eventison" + market.getName());

        if (!super.shouldShowAtMarket(market)) return false;
        if (market.getPlanetEntity() == null) {
            return false;
        }
        if (!market.getFactionId().equals("independent")) return false;
        boolean stage = (MengSearch.getStage() == MengSearch.MengStep.Meng_Step11);
        return stage;

    }

    protected void regen(MarketAPI market) {
        if (this.market == market) return;
        super.regen(market);

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

    public void addPromptAndOption(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
        regen(dialog.getInteractionTarget().getMarket());


        TextPanelAPI text = dialog.getTextPanel();
        text.addPara("你在酒吧里闲逛着，突然发现陪你一起来到酒吧的萌萌不见了踪影。");

        Color c = person.getFaction().getColor();

        dialog.getOptionPanel().addOption("寻找萌萌", this, new Color(243, 35, 191, 255), null);
    }

    public void optionSelected(String optionText, Object optionData) {
        if (!(optionData instanceof MouMeng_bar_event1.OptionId)) {
            return;
        }

        MouMeng_bar_event1.OptionId option = (MouMeng_bar_event1.OptionId) optionData;
        OptionPanelAPI options = dialog.getOptionPanel();
        TextPanelAPI text = dialog.getTextPanel();
        options.clearOptions();

        if (portraitActive) {

        }

        switch (option) {
            case AFTER_ACT_1:
                text.addPara("你四处寻找着萌萌的踪迹，最后你到了楼顶，发现萌萌坐在酒吧的天台上，夜晚的繁星悬挂在她的头顶，她呆呆地望着眼前的城市，不知道思绪飞向了何方。");
                options.addOption("静静走到萌萌身边", OptionId.ACCEPT);
                options.addOption("远远地喊她的名字", OptionId.ACCEPT1);

                break;
            case ACCEPT:
                text.addPara("你静静地走到了萌萌的身边，她在你坐下的时候才反应过来，刚做出戒备的动作，在看见是你后轻轻笑了起来，" +
                        "\"是你呀，舰长大人。难道...是来刻意找我的吗？\"她露出了一个狡點的表情。看见这样的她，你的心头也有一丝开心，在为萌萌复仇之后，" +
                        "她似乎也终于放下了心头的担子，时常能看到笑容了。");
                text.addPara("你坐在她的身边静静地看着她，萌萌被你这样盯得有些慌乱，\"唔，怎么这样看着我呀。\"");
                options.addOption("\"在想什么呢？\"", OptionId.CONTINUE1);
                options.addOption("继续盯着她看", OptionId.CONTINUE1S);
                break;
            case ACCEPT1:
                text.addPara("你远远地就喊着萌萌的名字，她条件反射似的的转过了脑袋，在看见是你后轻轻笑了起来，\"是你呀，舰长大人。难道...是来刻意找我的吗？\"" +
                        "她露出了一个狡點的表情。看见这样的她，你的心头也有一丝开心，在为萌萌复仇之后，她似乎也终于放下了心头的担子，时常能看到笑容了。");
                text.addPara("你走过去坐在她的身边静静地看着她，萌萌被你这样盯得有些慌乱，\"唔，怎么这样看着我呀。\"");
                options.addOption("\"在想什么呢？\"", OptionId.CONTINUE1);
                options.addOption("继续盯着她看", OptionId.CONTINUE1S);
                break;
            case CONTINUE1:
                text.addPara("\"啊...舰长大人，这颗星球真的很像我的学院呢。在那时候的训练之余，我也常常像这样一个人躲在宿舍楼外的因斯湖边，看着满天的星星。" +
                        "只是在那天以后，就再也没这样过了...\"萌萌轻轻的说，\"曾经远处的星海是我的梦想，而如今却多少有些厌倦了。我的很多朋友都永眠在了冰冷的星空里，" +
                        "这也是大多数人的宿命。\"");
                text.addPara("萌萌看着你的眼睛，\"但是，舰长大人不一样，不知道为什么会有这样的感觉，但我总觉得，对于您来说这里并非终点。" +
                        "\"她又笑了起来，\"不需要辩解哦，这只是一个直觉而已。\"");

                options.addOption("继续", MouMeng_bar_event1.OptionId.CONTINUE2);
                break;
            case CONTINUE1S:
                text.addPara("萌萌不由自主的低下了脑袋，\"您一定想知道我在想什么吧...舰长大人，这颗星球真的很像我的学院呢。在那时候的训练之余，" +
                        "我也常常像这样一个人躲在宿舍楼外的因斯湖边，看着满天的星星。只是在那天以后，就再也没这样过了..." +
                        "\"萌萌轻轻的说，\"曾经远处的星海是我的梦想，而如今却多少有些厌倦了。我的很多朋友都永眠在了冰冷的星空里，这也是大多数人的宿命。\"");
                text.addPara("萌萌抬起头看着你的眼睛，\"但是，舰长大人不一样，不知道为什么会有这样的感觉，但我总觉得，对于您来说这里并非终点。" +
                        "\"她又笑了起来，\"不需要辩解哦，这只是一个直觉而已。\"");

                options.addOption("继续", MouMeng_bar_event1.OptionId.CONTINUE2);
                break;
            case CONTINUE2:

                text.addPara("\"不管过去了多久，许多的过往我都无法淡忘，记忆能让一个人变得脆弱，也能使她无比坚强。\"她有些出神，\"" +
                        "铁血与战火交织出这整个世界，而大多数人都忘却了所谓的温柔。坚定的人、懦弱的人，在这样的世界里，其实都有些迷茫。\"");
                text.addPara("她呆呆地望着远方，那里不断有战舰启航，驶离这片星港，\"在那天的夜色里，降临的炮火点燃了半边天空，警报拉响，毁掉了我熟悉的人，与生活的地方。\"\n" +
                        "她的声音中交杂着种种思绪，逐渐沙哑起来，\"在我还拥有我的朋友们时，我总是不敢和她们讲这些心事，我害怕被嫌弃，出现分歧，我还担心毕业后的分别，还在恐惧未来的走向...但我现在才明白这一切都可以改变，只有生死，一锤定音。\"");
                text.addPara("她闭了闭眼睛，\"谢谢你呀，舰长大人，从来没有找到一个人愿意听我说这些，但你在身边的时候，不由自主的就感到很安心...\"");

                options.addOption("摸摸她的脑袋", MouMeng_bar_event1.OptionId.CONTINUE3);
                options.addOption("表示你只是路过", MouMeng_bar_event1.OptionId.CONTINUE3S);
                break;
            case CONTINUE3S:
                text.addPara("萌萌的脸刷的就红了，\"啊，原来是这样吗，对，对不起，您就当什么都没有听到...\"");
                options.addOption("继续", MouMeng_bar_event1.OptionId.REALYLEAVE);

                break;
            case CONTINUE3:
                text.addPara("萌萌没有抗拒，轻轻地靠在你的肩膀上，\"舰长大人，我已经失去了太多东西了，人们经历过相聚与离别，才更懂得珍惜。\"");
                text.addPara("她抬起头看着你的眼睛，\"舰长大人，我不想再经历失去了，我一定会好好保护你的。\"她顿了顿，但这次却没有挪开视线，" +
                        "\"因为您也是我生命中的、珍视之人。\"");
                options.addOption("轻轻搂住萌萌", MouMeng_bar_event1.OptionId.CONTINUE4);
                options.addOption("逃走", MouMeng_bar_event1.OptionId.CONTINUE4S);
                break;
            case REALYLEAVE:
                text.addPara("萌萌站起身装作若无其事的样子走下了楼。");
                leave();
                break;
            case CONTINUE4:
                text.addPara("你轻轻的搂住萌萌的腰，陪她坐在天台上，一时无话，只有远方的星舰起落，映得夜空忽明忽灭，隔开了近处的喧嚣。");
                Global.getSector().getPlayerStats().addStoryPoints(4,text,false);
                leave();
                MouMeng_Search.setStage(MouMeng_Search.MouMengStep.MouMeng_Step2);
                Global.getSector().getMemoryWithoutUpdate().set("$MouMeng_time1", 1, 90);
                end(true);
                break;
            case CONTINUE4S:
                text.addPara("你慌乱的逃开了天台，萌萌静静地看着你离去的身影，显得有一些孤单。");
                Global.getSector().getPlayerStats().addStoryPoints(4,text,false);
                leave();
                MouMeng_Search.setStage(MouMeng_Search.MouMengStep.MouMeng_Step2);
                Global.getSector().getMemoryWithoutUpdate().set("$MouMeng_time1", 1, 90);
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
        return Global.getSector().getFaction("Meng_temple");
    }


    private enum OptionId {

        AFTER_ACT_1,
        ACCEPT,
        REALYLEAVE,
        ACCEPT1,
        LEAVE,
        CONTINUE1,
        CONTINUE1S,
        CONTINUE2,
        CONTINUE3,
        CONTINUE3S,
        CONTINUE4,
        CONTINUE4S,
        CONTINUE5,

    }
}


