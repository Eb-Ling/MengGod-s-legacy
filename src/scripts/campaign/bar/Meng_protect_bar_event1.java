package data.scripts.campaign.bar;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.comm.CommMessageAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEventWithPerson;
import com.fs.starfarer.api.impl.campaign.intel.MessageIntel;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.intel.Meng_protectfleetintel;
import data.scripts.campaign.intel.Meng_timefleetintel;

import java.awt.*;
import java.util.Map;

public class Meng_protect_bar_event1 extends BaseBarEventWithPerson {


    public static String MengKey = "$Meng_officer";
    private Meng_protectfleetintel intel = null;

    public boolean shouldShowAtMarket(MarketAPI market) {
        Global.getLogger(this.getClass()).info("Eventison" + market.getName());

        if (!super.shouldShowAtMarket(market)) return false;
        boolean stage = MengSearch.getStage() == MengSearch.MengStep.Meng_Step2;
        if (Global.getSector().getMemory().contains("$Meng_protecttime")) return false;
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


    public String getPromptIcon() {
        return "graphics/factions/Meng_embers_s.png";
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
        text.addPara("在你进入酒馆时，跟在你身后的萌萌欲言又止的样子。");

        Color c = person.getFaction().getColor();
        dialog.getOptionPanel().addOption("和萌萌谈话", this, new Color(243, 35, 191, 255), null);
    }

    public void optionSelected(String optionText, Object optionData) {
        if (!(optionData instanceof Meng_protect_bar_event1.OptionId)) {
            return;
        }

        Meng_protect_bar_event1.OptionId option = (Meng_protect_bar_event1.OptionId) optionData;
        OptionPanelAPI options = dialog.getOptionPanel();
        TextPanelAPI text = dialog.getTextPanel();
        options.clearOptions();

        boolean portraitActive = false;
        if (portraitActive) {

        }

        switch (option) {
            case AFTER_ACT_1:
                text.addPara("\"舰长大人，不知道您是否还记得关于我过去的事。\"萌萌有些局促地说，\"经历了这段时间的相处，" +
                        "我认为您是一个可以信赖的、可靠的舰长，这些日子真的很开心，但越是这样，人就越容易想起过去。\"");
                text.addPara("她浅浅地叹了一口气，这位少女在你舰队时无论面对任何敌人都一副坚强的样子，在你面前却时常流露出少有的无助。" +
                        "\"尽管眼前的一切都如此美好，但是在梦里，或者战后眺望战场的残骸，我总会想起那片记忆里始终昏沉的星空。" +
                        "在我承载了整艘舰船的希望被推进逃生舱的那一刻，我就注定无法释怀。\"萌萌闭上了眼睛，身体有些颤抖。");
                options.addOption("握住她的手", Meng_protect_bar_event1.OptionId.ACCEPT);
                options.addOption("摸摸她的脑袋", Meng_protect_bar_event1.OptionId.LEAVE);

                break;
            case ACCEPT:
                text.addPara("快握住她的手的那一刻，萌萌的手下意识的一缩，但还是没有真正躲开。" +
                        "\"舰长大人...我觉得您是一位真正可以信赖的人，真好呢...\"萌萌轻轻地回握住了你的手，\"其实，在那一场战斗的背后，还隐藏着一个可怕的秘密，我不敢说出口，" +
                        "只是不想再看到因我而起的别离了。但是，您是可以信赖的人，在您舰队的日子里，我也感受到了您的睿智，强大和潜力。" +
                        "\"萌萌抿了抿嘴，脸微微一红，\"以及，您的温柔。\"");
                text.addPara("\"不知道您愿不愿意听我讲述这个秘密，这可能会带给您危险，所以若您拒绝，我不会有任何的介意。" +
                        "如果您有一定的兴趣，我相信睿智的您可以做出足够明智的判断，至于是为我的过去报仇，或者只是倾听，我都不会介意。" +
                        "\"萌萌抬起头，鼓起勇气望着你，你能感受到她的期待，以及一丝隐藏的惶恐。");
                options.addOption("表示你愿意倾听", Meng_protect_bar_event1.OptionId.CONTINUE1);
                options.addOption("表示并不关心", Meng_protect_bar_event1.OptionId.REALYLEAVE);
                break;
            case LEAVE:
                text.addPara("你伸出手准备摸摸她的脑袋，萌萌的下意识的一缩，但还是没有真正躲开。" +
                        "\"舰长大人...我觉得您是一位真正可以信赖的人，真好呢...\"萌萌低垂着脑袋，你看不到她的表情，\"" +
                        "其实，在那一场战斗的背后，还隐藏着一个可怕的秘密，我不敢说出口，只是不想再看到因我而起的别离了。但是，您是可以信赖的人，在您舰队的日子里，" +
                        "我也感受到了您的睿智，强大和潜力。\"萌萌抿了抿嘴，微不可查地说道，\"以及，您的温柔。\"");
                text.addPara("\"不知道您愿不愿意听我讲述这个秘密，这可能会带给您危险，所以若您拒绝，我不会有任何的介意。" +
                        "如果您有一定的兴趣，我相信睿智的您可以做出足够明智的判断，至于是为我的过去报仇，或者只是倾听，我都不会在意。" +
                        "\"萌萌抬起头，鼓起勇气望着你，你能感受到她的期待，以及一丝隐藏的惶恐。");
                options.addOption("表示你愿意倾听", Meng_protect_bar_event1.OptionId.CONTINUE1);
                options.addOption("表示并不关心", Meng_protect_bar_event1.OptionId.REALYLEAVE);
                break;
            case REALYLEAVE:
                text.addPara("\"没关系的，我可以理解您的选择，就让我把这一切藏在心底吧，或许这样才是最正确的选择。" +
                        "\"萌萌微微笑着，似乎和往常没什么两样。");
                leave();
                MengSearch.setStage(MengSearch.MengStep.Meng_Step3);
                break;
            case CONTINUE1:
                person.setPortraitSprite(Global.getSettings().getSpriteName("intel", "MouMeng"));
                text.addPara("听到了你的答复，萌萌变得有些沉默，她犹豫了很久，带你进入了酒吧的包间，" +
                        "\"舰长大人，我没想到您会愿意为了相识不久的我承担风险，谢谢您...我逃离了那场战斗，我痛恨我的懦弱，在被路过的商队营救的那天，" +
                        "我用账户上最后的积蓄租赁了一艘飞船和一些船员，返回了那片战场，我做好了拼命的准备，但是那支噩梦般的舰队已经离开，只剩下漂浮的残骸。\"");
                text.addPara("\"我不断地在残骸里寻找，安葬了我能找到的战友们的遗体，在我精神濒临崩溃的时候，我发现了一个不同寻常的残骸，" +
                        "她似乎是我们舰队唯一击沉的一艘最小型的舰船，我怀揣着恐惧登上了它，这艘船上竟然没有一个船员的尸体，我在舰船面目全非的控制室里发现了储存着舰船日志的芯片。" +
                        "\"随着她的讲述，萌萌眼中的恐惧也逐渐浓郁，她打开了Tripad，在加密的文件夹里，有着一份日志文件。");

                options.addOption("查看舰船日志", Meng_protect_bar_event1.OptionId.CONTINUE2);
                break;
            case CONTINUE2:
                text.addPara("\"星历XX年9月16日，司令又下达了命令，真不知道这片星域有什么好探索的，我们已经是第四批来这里调查的舰队了，" +
                        "区区一个调查任务竟然需要我们出动？舰长似乎也不太清楚，不管他了，我们可是精锐中的精锐，相信没有人敢于挑衅霸主的威严。\"");
                text.addPara("\"星历XX年9月30日，我们到达了目的地，已经探索了几天了，没有任何的收获，这里普通的不能再普通，" +
                        "甚至连一片废铁都没有，Jack一直在和我抱怨，他说最近睡觉时老是能听到奇怪的声音，舰队的医生给他开了抗焦虑药，我知道他的第一个儿子即将出生，" +
                        "比起在这里进行没有意义的调查，他肯定更想亲眼看着孩子出生，而不是在这里蒙受着担忧。这该死的任务，真不知道司令在想什么。\"");
                text.addPara("\"星历XX年10月2日，奇怪，我们一周前进入的这片星系，现在调查已经结束，但是我们却找不到进入时的跳跃点了！" +
                        "横轴跳跃似乎受到了什么东西的影响，我们无法校准跃迁的目的地，随舰科学家说这是什么风暴的缘故，但和那些蠢货不一样，" +
                        "该死的，我知道这可不是简单的风暴。Jack最近的焦虑症状越来越明显了，医生的药物似乎没有起到任何作用，他不断地告诉我们他听到的声音越来越清晰了，" +
                        "这家伙真的是疯了。\"");

                options.addOption("继续", Meng_protect_bar_event1.OptionId.CONTINUE3);
                break;
            case CONTINUE3:
                text.addPara("\"星历XX年10月8日，又经过了一周的时间，我们依旧被困在这个星系，" +
                        "舰长也开始着急了，他不断地催促科学家们研究出去的方法，我知道他着急的原因，舰船上携带了很多补给，但能源终究有耗尽的时候，" +
                        "他下令减少维生系统的供能和食物配给，得到了很多怨言，但我知道他是对的。Jack已经彻底疯了，从今天早上开始，他见人就嘶喊着：" +
                        "'进入，进入....'除此之外，他对我们的询问置若罔闻，医生把他关进了舰船的隔离室，这可怜的家伙，希望他能早日好转。\"");
                text.addPara("\"星历XX年10月23日，舰船上许多人出现了和Jack一样的症状，舰长也意识到了事情的不同寻常，他下令将出现症状的人集中隔离起来观察。" +
                        "经过一段时间的研究和探索，我们发现当舰船越接近某个方向时，他们的症状就越严重，而反之，他们的症状就得到缓解，真是怪事。" +
                        "那'进入'两个字似乎在告诉着我们什么，舰长又召集科学家进行了紧急会议。\"");
                text.addPara("\"星历XX年11月8日，科学家们研究出了一些东西，那些人的大脑似乎受到了一种弥漫于整个星系的物质的影响，" +
                        "这个物质也是阻止我们进入超空间的元凶，它是在某一天突然出现，然后弥漫到整个星系的，而一切的源头就在那个方向，我知道舰船上的补给已经到了十分紧张的程度，" +
                        "没时间再进行更细致的研究了，舰长做出了决定，让我们沿着那个方向前进，不管一切的尽头是什么在捣鬼，在绝对的力量面前都会被霸主的荣光撕成碎片。\"");
                options.addOption("继续", Meng_protect_bar_event1.OptionId.CONTINUE4);
                break;

            case CONTINUE4:
                text.addPara("\"星历XX年11月19日，随着前进，舰队里疯狂的人越来越多，科学家们说，我们已经很接近源头了，阻隔我们进行超空间跃迁的屏障也开始了松动，" +
                        "我们的方向果然是正确的，只要再接近一点，就可以离开这个鬼地方了。最近我似乎也开始听到一些奇怪的声音，该死的，一定是这古怪的星系让我过分紧张了。\"");
                text.addPara("\"星历XX年11月23日，我们终于看到了一切的源头，那里竟然有一个全新的跳跃点！终于能离开那个鬼地方了，" +
                        "我耳边的声音也越来越清晰了，我知道我也出了一些问题，但没有关系了，狗屎，离开了这个星系，一切都会回归正常的。" +
                        "旗舰们都通过了那个跳跃点，他们没有发出报警信息，看来一切正常，现在轮到我们护卫舰最后一批驶离了，正在接近中...校准，定位，跳跃。" +
                        "在离开的那一刻，我终于听清了耳边的声音，\n它说的好像是：\"不要进入...\"\"");
                text.addPara("中间的一大段日志似乎遭到了损毁，你只能看到最后的一篇日志。");

                options.addOption("继续", Meng_protect_bar_event1.OptionId.CONTINUE5);
                break;
            case CONTINUE5:
                text.addPara("\"星历XX年12月31日，在无尽黑暗的神国里漂泊，我们终于看到了光。\"");

                options.addOption("结束阅读", Meng_protect_bar_event1.OptionId.CONTINUE6);
                break;
            case CONTINUE6:
                text.addPara("你和萌萌一起看着最后一篇日志，房间里出现了漫长的沉默。最终，萌萌先开了口：" +
                        "\"舰长大人，这就是我从袭击我们的那支舰队的舰船残骸里找到的日志，这后面一定隐藏了恐怖的真相，所以我并不会强求您进行调查，" +
                        "我知道的，有些事情还是忘掉最好...\"似乎是看出了你的犹豫，萌萌靠在了你的肩头，你感受到了她的惶恐，以及话语暗含的低落和迷茫。失去了一切的她，" +
                        "现在只能依靠在你的身旁了。");

                options.addOption("表示愿意帮她调查", Meng_protect_bar_event1.OptionId.CONTINUE7);
                options.addOption("表示希望彼此忘掉这件事", OptionId.REALYLEAVE);
                break;

            case CONTINUE7:
                text.addPara("萌萌打开了另一份文件：\"舰长大人，之所以要今天告诉你这一切，是因为前不久我派去监视那个星系的雇佣小队有了新的发现，" +
                        "那支舰队似乎又在那个星系出现了。这是那个星系的位置，如果您做好了准备，可以带上我前去调查。\"萌萌深吸了一口气，\"舰长大人，关于那支舰队，" +
                        "我只知道一个关键的信息，千万不要尝试从正面击溃他...如果成功为我复仇，我或许就能帮助您在残骸里提取相关的技术，那一定会是能颠覆时代的手段。\"");

                text.addPara("你的任务日志已经更新", Misc.getHighlightColor());

                intel = new Meng_protectfleetintel(dialog);

                Global.getSector().getCampaignUI().addMessage(intel, CommMessageAPI.MessageClickAction.INTEL_TAB);
                
                leave();
                MengSearch.setStage(MengSearch.MengStep.Meng_Step4);

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
        CONTINUE4,
        CONTINUE5,
        CONTINUE6,
        CONTINUE7,
    }
}


