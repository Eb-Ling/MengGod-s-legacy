package data.scripts.campaign.intel;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.impl.campaign.intel.MessageIntel;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.bar.*;


import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class Meng_eventtitle extends BaseIntelPlugin {

    public static final List<String> SHIPS = Collections.singletonList("Meng_protect_boss1_variant");
    public static final List<String> WEAPONS = Collections.emptyList();
    public static final float PAD_INIT = 5f;
    public static final float PAD_HEAD = 10f;
    public static final float PAD_LINE = 6f;
    private float timer1=0f;
    private float totaldays=0f;
    public static final Color HL = Misc.getHighlightColor();
    public final String Button1 = "Cai_flux";
    public final String Button2 = "Cai_armor";
    public final String Button3 = "Meng_show";
    public final String Button4 = "Cai_speed";
    public final String Button5 = "Meng_Dragonship";
    private boolean choosen=false;
    private boolean tartchoosen=false;
    protected CargoAPI showCargo = null;

    public Meng_eventtitle(InteractionDialogAPI dialog) {
        setImportant(true);

        Global.getSector().addScript(this);
        if (dialog == null) {
            Global.getSector().getIntelManager().addIntel(this, false);
        } else {
            Global.getSector().getIntelManager().addIntel(this, false, dialog.getTextPanel());
        }
        this.important = true;
        this.neverClicked = true;
    }

    public void buttonPressConfirmed(Object buttonId, IntelUIAPI ui) {
        if (buttonId.equals(Button1)) {
            Cai_Search.setStage(Cai_Search.Cai_Step.Cai_Step2);
        }
        if (buttonId.equals(Button2)) {
            Cai_Search.setStage(Cai_Search.Cai_Step.Cai_Step1);
        }
        if (buttonId.equals(Button3)) {
            tartchoosen=!tartchoosen;
        }
        if (buttonId.equals(Button4)) {
            choosen=true;
        }
        if (buttonId.equals(Button5)) {
            Meng_Starshipintel intel = new Meng_Starshipintel(null);
            MengSearch.setStage(MengSearch.MengStep.Meng_Step10);
            Global.getSector().getMemoryWithoutUpdate().set("$Meng_startime", 1, 90);
        }
        ui.updateUIForItem(this);
    }

    @Override
    public void createLargeDescription(CustomPanelAPI panel, float width, float height) {
        float offset = 8f;
        float space = 4f;

        TooltipMakerAPI left = panel.createUIElement(width / 2f - space / 2f - offset, height, true);
        TooltipMakerAPI right = panel.createUIElement(width / 2f - space / 2f - offset, height, true);
        TooltipMakerAPI center = panel.createUIElement(space, height, false);
        
        buildLeftPanel(left, width);
        buildRightPanel(right, width);
        
        panel.addUIElement(left).inTL(0f, 0f);
        panel.addUIElement(right).inTR(0f, 0f);
        panel.addUIElement(center).inTMid(0f);
    }

    private void buildLeftPanel(TooltipMakerAPI left, float width) {
        left.addSectionHeading("为小蔡选择纳米机器人组装类型", Alignment.MID, PAD_INIT);

        ButtonAPI Buttons1 = left.addButton("纳米机械组装-突击", Button1, getFactionForUIColors().getBaseUIColor(), getFactionForUIColors().getDarkUIColor(), Alignment.MID, CutStyle.ALL, width * 0.2f, 20f, PAD_INIT);
        ButtonAPI Buttons2 = left.addButton("纳米机械组装-防御", Button2, getFactionForUIColors().getBaseUIColor(), getFactionForUIColors().getDarkUIColor(), Alignment.MID, CutStyle.ALL, width * 0.2f, 20f, PAD_INIT);
        
        renderNanobotInfo(left);
        renderEventProgress(left);
        renderStageIntroduction(left);
    }

    private void renderNanobotInfo(TooltipMakerAPI left) {
        Cai_Search.Cai_Step currentStage = Cai_Search.getStage();
        
        if (currentStage == Cai_Search.Cai_Step.Cai_Step2) {
            left.addPara("{%s}当前技能类型为:纳米机械组装-突击", PAD_LINE, HL, "-");
            left.addPara("{%s}武器伤害+{%s}。", PAD_LINE, HL, "#", "15%");
            left.addPara("{%s}幅能容量与耗散+{%s}。", PAD_LINE, HL, "#", "20%");
            left.addPara("{%s}舰船航速+{%s}，机动性+{%s}。", PAD_LINE, HL, "#", "15%", "35%");
            left.addPara("{%s}武器射程-{%s}。", PAD_LINE, HL, "#", "10%");
            left.addPara("{%s}武器幅能产生+{%s}。", PAD_LINE, HL, "#", "15%");
            left.addPara("{%s}护盾与相位效率-{%s}。", PAD_LINE, HL, "#", "15%");
            left.addPara("{%s}峰值时间-{%s}。", PAD_LINE, HL, "#", "15%");
            left.addPara("{%s}战备衰减速率+{%s}。", PAD_LINE, HL, "#", "50%");
        } else if (currentStage == Cai_Search.Cai_Step.Cai_Step1) {
            left.addPara("{%s}当前技能类型为:纳米机械组装-防御", PAD_LINE, HL, "-");
            left.addPara("{%s}装甲可以自我修复。", PAD_LINE, HL, "#");
            left.addPara("  {%s}每秒恢复基础装甲值的{%s}。", PAD_LINE, HL, "#", "2.5%");
            left.addPara("  {%s}单个装甲格最多恢复装甲上限的{%s}。", PAD_LINE, HL, "#", "200%");
            left.addPara("{%s}过载/强制排散时恢复速率降低{%s}。", PAD_LINE, HL, "#", "50%");
            left.addPara("{%s}舰船盾效+{%s}。", PAD_LINE, HL, "#", "15%");
            left.addPara("{%s}武器与引擎受到的伤害-{%s}。", PAD_LINE, HL, "#", "50%");
            left.addPara("{%s}装甲值-{%s}。", PAD_LINE, HL, "#", "20%");
            left.addPara("{%s}护盾维持效率+{%s}。", PAD_LINE, HL, "#", "35%");
            left.addPara("{%s}舰船航速-{%s}，机动性-{%s}。", PAD_LINE, HL, "#", "15%", "35%");
            left.addPara("{%s}全武器伤害-{%s}。", PAD_LINE, HL, "#", "5%");
        }
    }

    private void renderEventProgress(TooltipMakerAPI left) {
        left.addSectionHeading("事件进度", Alignment.MID, PAD_INIT);
        MengQuestTracker.renderQuestProgress(left);
    }

    private void renderStageIntroduction(TooltipMakerAPI left) {
        left.addSectionHeading("阶段介绍", Alignment.MID, PAD_HEAD);
        
        MengSearch.MengStep stage = MengSearch.getStage();
        
        switch (stage) {
            case Meng_Step2:
                left.addPara("{%s}萌萌已经加入了舰队，你知道她还藏着很多事情没有吐露，或许一段时日之后能有下一步的进展", PAD_LINE, HL, "-");
                break;
            case Meng_Step3:
                left.addPara("{%s}你做出了错误的选择，事件链到此为止。", PAD_LINE, HL, "-");
                break;
            case Meng_Step4:
                addShenjiangIntro(left);
                break;
            case Meng_Step5:
                left.addPara("{%s}你歼灭了神降舰队。", PAD_LINE, HL, "-");
                break;
            case Meng_Step6:
                addTimeTravelerIntro(left);
                break;
            case Meng_Step7:
                left.addPara("{%s}你歼灭了时旅者舰队，你的评级来到了A-，也许事情会有下一步的进展...", PAD_LINE, HL, "-");
                break;
            case Meng_Step9:
                addYizhouLongIntro(left);
                break;
            default:
                break;
        }
    }

    private void addShenjiangIntro(TooltipMakerAPI left) {
        left.addPara("{%s}神降舰队介绍：", PAD_LINE, HL, "-");
        left.addPara("神降舰队遭受了未知神性的污染，其舰船结构大变，并且拥有着 神降船插。", PAD_LINE, HL, "神降船插");
        left.addImage("graphics/Meng/hullmods/meng_protect_S.png", 40f, 40f, PAD_LINE);
        left.addPara("神降船插可以庇佑其舰船在过载时的一切危险，当舰船过载后神降舰船将立刻结束过载且回溯状态，并在接下来的十五秒内不可开盾，获得高额时流。因此，从背后找到突破口是你唯一的办法。", PAD_LINE, HL, "神降船插");
    }

    private void addTimeTravelerIntro(TooltipMakerAPI left) {
        left.addPara("{%s}时旅者介绍：", PAD_LINE, HL, "-");
        left.addPara("时旅者拥有时间倒流的能力，但是它倒流时间所用的能量充能更慢，尝试着耗尽它的能量，然后一举歼灭它。", PAD_LINE, HL, "-");
        left.addPara("提示：利用时间倒流的特点，区分有效与无效的打击，从而掌握制胜的手段。", PAD_LINE, HL, "-");
    }

    private void addYizhouLongIntro(TooltipMakerAPI left) {
        left.addPara("{%s}异宙龙介绍：", PAD_LINE, HL, "-");
        left.addPara("{%s}异宙龙拥有着独一无二的核心，它可以通过伤害敌人积累自身的暴怒，最终释放出毁天灭地的龙息。", PAD_LINE, HL, "-");
        left.addPara("战术系统，黄昏之翼：异宙龙可以切换高航速形态与低航速战斗形态，通过对两种形态的灵活切换，它可以在战场中游荡自如。", PAD_LINE, HL, "黄昏之翼");
    }

    private void buildRightPanel(TooltipMakerAPI right, float width) {
        ensureHasCargo();
        right.addSectionHeading("圣殿赏金面板：", Alignment.MID, PAD_INIT);
        
        ButtonAPI bountyButton = right.addButton("打开圣殿赏金面板", Button3, getFactionForUIColors().getBaseUIColor(), getFactionForUIColors().getDarkUIColor(), Alignment.MID, CutStyle.ALL, width * 0.2f, 20f, PAD_INIT);
        bountyButton.setText(tartchoosen ? "关闭圣殿赏金面板" : "打开圣殿赏金面板");
        
        if (tartchoosen) {
            renderBountyPanel(right, width);
        }
        
        renderAffinityInfo(right);
    }

    private void renderBountyPanel(TooltipMakerAPI right, float width) {
        if (MengSearch.getStage() != MengSearch.MengStep.Meng_Step8) {
            return;
        }
        
        if (choosen) {
            right.addPara("{%s}欢迎接受赏金，教徒编号3145，权限：A。", PAD_LINE, HL, "-");
            right.addPara("{%s}任务内容：在这个偏远星系内探测到了记录中的波动，由于资料遗失，我们不能确定它的来源，请前往探查并上报结果。", PAD_LINE, HL, "-");
            right.addPara("{%s}在对此任务的评估中没有任何风险。", PAD_LINE, HL, "-");
            right.addButton("接受任务", Button5, getFactionForUIColors().getBaseUIColor(), getFactionForUIColors().getDarkUIColor(), Alignment.MID, CutStyle.ALL, width * 0.15f, 15f, PAD_INIT);
        } else {
            right.addButton("遗弃舰勘探", Button4, getFactionForUIColors().getBaseUIColor(), getFactionForUIColors().getDarkUIColor(), Alignment.MID, CutStyle.ALL, width * 0.15f, 15f, PAD_INIT);
            right.addPara("{%s}需求权限：A", PAD_LINE, HL, "-");
            right.addPara("{%s}调查未知星系中的兴趣点。", PAD_LINE, HL, "-");
        }
    }

    private void renderAffinityInfo(TooltipMakerAPI right) {
        right.addSectionHeading("萌萌好感度", Alignment.MID, PAD_INIT);
        
        MengSearch.MengStep mengStage = MengSearch.getStage();
        MouMeng_Search.MouMengStep mouMengStage = MouMeng_Search.getStage();
        
        if (mengStage == MengSearch.MengStep.Meng_Step2) {
            right.addPara("{%s}普通", PAD_LINE, HL, "-");
        } else if (isStageReached(mengStage, MengSearch.MengStep.Meng_Step4)) {
            right.addPara("{%s}友好", PAD_LINE, HL, "-");
            right.addPara("{%s}专属技能 逆流之风解锁新效果:萌萌与你共享了独一无二的技术，当萌萌驾驶的舰船与玩家在2000su范围内时，玩家与萌萌舰船距离越近，幅能耗散越高，最近时提高20%%。", PAD_LINE, HL, "-");
        }
        
        if (mouMengStage == MouMeng_Search.MouMengStep.MouMeng_Step2) {
            right.addPara("{%s}珍视", PAD_LINE, HL, "-");
            right.addPara("{%s}专属技能 逆流之风解锁新效果:萌萌在你的舰船上加装了改造，当萌萌驾驶的舰船与玩家在2000su范围内时，两者舰船向彼此靠近时移速增加30%%。", PAD_LINE, HL, "-");
        }
    }

    private boolean isStageReached(MengSearch.MengStep current, MengSearch.MengStep target) {
        return current.ordinal() >= target.ordinal();
    }

    @Override
    public void advanceImpl(float amount) {
        float days = Global.getSector().getClock().convertToDays(amount);
        
        if (MengSearch.getStage() != MengSearch.MengStep.Meng_Step7) {
            timer1 = 0f;
            totaldays = 0f;
            return;
        }
        
        timer1 += days;
        totaldays += days;
        
        if (timer1 >= 1f) {
            timer1 = 0f;
            
            float triggerProbability = Math.min(totaldays / 60f, 1.0f);
            if (Math.random() < triggerProbability) {
                triggerBountyMission();
            }
        }
    }
    
    private void triggerBountyMission() {
        MengSearch.setStage(MengSearch.MengStep.Meng_Step8);
        totaldays = 0f;
        timer1 = 0f;
        
        MessageIntel intel = new MessageIntel("新的圣殿任务已发送");
        intel.setSound("ui_discovered_entity");
        intel.setIcon(Global.getSettings().getSpriteName("intel", "discovered_entity"));
        sendUpdateIfPlayerHasIntel(intel, false);
    }
    
    private void ensureHasCargo() {
        if (showCargo == null) {
            showCargo = Global.getFactory().createCargo(true);
            showCargo.initMothballedShips(Factions.PLAYER);
            showCargo.addMothballedShip(FleetMemberType.SHIP, "Meng_protect_boss1_variant", null);

            for (String id : WEAPONS) {
                showCargo.addWeapons(id, 1);
            }
        }
    }

    public void resetCargo() {
        showCargo = null;
    }

    @Override
    protected String getName() {
        return "神之遗卷";
    }

    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = super.getIntelTags(map);
        tags.add(Tags.INTEL_STORY);
        return tags;
    }

    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        info.addPara("已获得事件链面板", new Color(226, 23, 253, 255), 0f);
    }
    @Override
    public String getIcon() {
        return "graphics/portraits/Mou_Meng.png";
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }

    @Override
    public boolean hasSmallDescription() {
        return true;
    }
    protected void unindent(TooltipMakerAPI info) {
        info.setBulletedListMode(null);
        info.setTextWidthOverride(0);
    }
    @Override
    protected void addBulletPoints(TooltipMakerAPI info, ListInfoMode mode, boolean isUpdate, Color tc, float initPad) {
        Color h = Misc.getHighlightColor();
        Color g = new Color(226, 23, 253, 255);
        float pad = 3f;
        float opad = 10f;
        isUpdate = getListInfoParam() != null;

        if (mode == ListInfoMode.IN_DESC) initPad = opad;
        FactionAPI faction = getFactionForUIColors();

        bullet(info);
        if (isUpdate) {
            info.addPara("圣殿赏金面板有了新的消息", initPad, g, h);
        } else {
            info.addPara("赏金面板暂无新的消息", initPad, g, h);
        }
        unindent(info);
    }

    @Override
    public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
        Color c = new Color(226, 23, 253, 255);
        info.addPara(getSmallDescriptionTitle(), c, 0f);
        addBulletPoints(info, mode);
    }

    @Override
    public String getSortString() {
        return getSmallDescriptionTitle();
    }

    @Override
    public String getSmallDescriptionTitle() {
        return "事件链面板";
    }

    public static void backDoor() {
        new Meng_protect_bar_event1();
    }

    @Override
    public boolean hasLargeDescription() {
        return true;
    }

}
