package data.scripts.campaign.intel;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import java.awt.*;
import java.util.*;
import java.util.List;

public class MengQuestTracker {
    
    private static final String QUEST_MEMORY_KEY = "$Meng_quest_tracker";
    private static final FactionAPI FACTION = Global.getSector().getFaction("independent");
    
    public enum QuestLine {
        MAIN_STORY("主线剧情"),
        BOUNTY_SYSTEM("圣殿赏金"),
        CHARACTER_RELATION("角色关系");
        
        private final String displayName;
        
        QuestLine(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    public enum QuestState {
        NOT_STARTED(0, "未开始", new Color(150, 150, 150, 255)),
        IN_PROGRESS(1, "进行中", new Color(255, 200, 50, 255)),
        COMPLETED(2, "已完成", new Color(50, 255, 100, 255)),
        FAILED(3, "已失败", new Color(255, 50, 50, 255));
        
        private final int value;
        private final String displayName;
        private final Color color;
        
        QuestState(int value, String displayName, Color color) {
            this.value = value;
            this.displayName = displayName;
            this.color = color;
        }
        
        public int getValue() {
            return value;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public Color getColor() {
            return color;
        }
    }
    
    public static class QuestNode {
        private final String questId;
        private final String title;
        private final String description;
        private final QuestLine questLine;
        private QuestState state;
        private final List<String> prerequisites;
        private final Map<String, Object> metadata;
        
        public QuestNode(String questId, String title, String description, QuestLine questLine) {
            this.questId = questId;
            this.title = title;
            this.description = description;
            this.questLine = questLine;
            this.state = QuestState.NOT_STARTED;
            this.prerequisites = new ArrayList<>();
            this.metadata = new HashMap<>();
        }
        
        public void addPrerequisite(String prerequisiteQuestId) {
            this.prerequisites.add(prerequisiteQuestId);
        }
        
        public void setMetadata(String key, Object value) {
            this.metadata.put(key, value);
        }
        
        public Object getMetadata(String key) {
            return metadata.get(key);
        }
        
        public boolean canStart() {
            if (state != QuestState.NOT_STARTED) {
                return false;
            }
            
            for (String prereq : prerequisites) {
                QuestNode prereqNode = getQuestNode(prereq);
                if (prereqNode == null || prereqNode.state != QuestState.COMPLETED) {
                    return false;
                }
            }
            
            return true;
        }
        
        public String getQuestId() {
            return questId;
        }
        
        public String getTitle() {
            return title;
        }
        
        public String getDescription() {
            return description;
        }
        
        public QuestLine getQuestLine() {
            return questLine;
        }
        
        public QuestState getState() {
            return state;
        }
        
        public void setState(QuestState state) {
            this.state = state;
        }
        
        public List<String> getPrerequisites() {
            return prerequisites;
        }
    }
    
    private static Map<String, QuestNode> questRegistry = new HashMap<>();
    
    static {
        initializeQuests();
    }
    
    private static void initializeQuests() {
        QuestNode q1 = new QuestNode("meng_step1", "初次相遇", "在酒吧遇到神秘的萌萌", QuestLine.MAIN_STORY);
        q1.setState(QuestState.COMPLETED);
        registerQuest(q1);
        
        QuestNode q2 = new QuestNode("meng_step2", "萌萌加入", "萌萌加入了你的舰队", QuestLine.MAIN_STORY);
        q2.addPrerequisite("meng_step1");
        registerQuest(q2);
        
        QuestNode q3 = new QuestNode("meng_step3", "错误的选择", "你做出了错误的选择，事件链到此为止", QuestLine.MAIN_STORY);
        q3.addPrerequisite("meng_step1");
        registerQuest(q3);
        
        QuestNode q4 = new QuestNode("meng_step4", "神降舰队情报", "探索神降舰队的秘密", QuestLine.MAIN_STORY);
        q4.addPrerequisite("meng_step2");
        registerQuest(q4);
        
        QuestNode q5 = new QuestNode("meng_step5", "诛灭神降", "歼灭神降舰队", QuestLine.MAIN_STORY);
        q5.addPrerequisite("meng_step4");
        registerQuest(q5);
        
        QuestNode q6 = new QuestNode("meng_step6", "时旅者出现", "时旅者舰队现身", QuestLine.MAIN_STORY);
        q6.addPrerequisite("meng_step5");
        registerQuest(q6);
        
        QuestNode q7 = new QuestNode("meng_step7", "诛灭时旅", "歼灭时旅者舰队", QuestLine.MAIN_STORY);
        q7.addPrerequisite("meng_step6");
        registerQuest(q7);
        
        QuestNode q8 = new QuestNode("meng_step8", "圣殿任务", "接受圣殿的赏金任务", QuestLine.BOUNTY_SYSTEM);
        q8.addPrerequisite("meng_step7");
        registerQuest(q8);
        
        QuestNode q9 = new QuestNode("meng_step9", "获取星之泪", "获得关键道具星之泪", QuestLine.MAIN_STORY);
        q9.addPrerequisite("meng_step7");
        registerQuest(q9);
        
        QuestNode q10 = new QuestNode("meng_step10", "异宙龙", "面对强大的异宙龙", QuestLine.MAIN_STORY);
        q10.addPrerequisite("meng_step9");
        registerQuest(q10);
        
        QuestNode cai_step1 = new QuestNode("cai_armor", "纳米组装-防御", "小蔡选择了防御型纳米组装", QuestLine.CHARACTER_RELATION);
        registerQuest(cai_step1);
        
        QuestNode cai_step2 = new QuestNode("cai_flux", "纳米组装-突击", "小蔡选择了突击型纳米组装", QuestLine.CHARACTER_RELATION);
        registerQuest(cai_step2);
    }
    
    private static void registerQuest(QuestNode quest) {
        questRegistry.put(quest.getQuestId(), quest);
    }
    
    public static QuestNode getQuestNode(String questId) {
        return questRegistry.get(questId);
    }
    
    public static void updateQuestState(String questId, QuestState newState) {
        QuestNode quest = getQuestNode(questId);
        if (quest != null) {
            quest.setState(newState);
            saveQuestStates();
            
            if (newState == QuestState.COMPLETED) {
                checkAndAutoStartNextQuests(questId);
            }
        }
    }
    
    private static void checkAndAutoStartNextQuests(String completedQuestId) {
        for (QuestNode quest : questRegistry.values()) {
            if (quest.canStart() && quest.getState() == QuestState.NOT_STARTED) {
                boolean hasCompletedPrereq = false;
                for (String prereq : quest.getPrerequisites()) {
                    if (prereq.equals(completedQuestId)) {
                        hasCompletedPrereq = true;
                        break;
                    }
                }
                
                if (hasCompletedPrereq) {
                    updateQuestState(quest.getQuestId(), QuestState.IN_PROGRESS);
                }
            }
        }
    }
    
    public static void syncWithLegacyStage() {
        data.scripts.campaign.bar.MengSearch.MengStep legacyStage = 
            data.scripts.campaign.bar.MengSearch.getStage();
        
        switch (legacyStage) {
            case Meng_Step1:
                updateQuestState("meng_step1", QuestState.COMPLETED);
                break;
            case Meng_Step2:
                updateQuestState("meng_step2", QuestState.COMPLETED);
                break;
            case Meng_Step3:
                updateQuestState("meng_step3", QuestState.COMPLETED);
                break;
            case Meng_Step4:
                updateQuestState("meng_step4", QuestState.IN_PROGRESS);
                break;
            case Meng_Step5:
                updateQuestState("meng_step5", QuestState.COMPLETED);
                break;
            case Meng_Step6:
                updateQuestState("meng_step6", QuestState.IN_PROGRESS);
                break;
            case Meng_Step7:
                updateQuestState("meng_step7", QuestState.COMPLETED);
                break;
            case Meng_Step8:
                updateQuestState("meng_step8", QuestState.IN_PROGRESS);
                break;
            case Meng_Step9:
                updateQuestState("meng_step9", QuestState.COMPLETED);
                break;
            case Meng_Step10:
                updateQuestState("meng_step10", QuestState.IN_PROGRESS);
                break;
        }
        
        data.scripts.campaign.bar.Cai_Search.Cai_Step caiStage = 
            data.scripts.campaign.bar.Cai_Search.getStage();
        
        if (caiStage == data.scripts.campaign.bar.Cai_Search.Cai_Step.Cai_Step1) {
            updateQuestState("cai_armor", QuestState.IN_PROGRESS);
        } else if (caiStage == data.scripts.campaign.bar.Cai_Search.Cai_Step.Cai_Step2) {
            updateQuestState("cai_flux", QuestState.IN_PROGRESS);
        }
    }
    
    private static void saveQuestStates() {
        Map<String, Integer> stateMap = new HashMap<>();
        for (Map.Entry<String, QuestNode> entry : questRegistry.entrySet()) {
            stateMap.put(entry.getKey(), entry.getValue().getState().getValue());
        }
        FACTION.getMemoryWithoutUpdate().set(QUEST_MEMORY_KEY, stateMap);
    }
    
    private static void loadQuestStates() {
        Object saved = FACTION.getMemoryWithoutUpdate().get(QUEST_MEMORY_KEY);
        if (saved instanceof Map) {
            Map<String, Integer> stateMap = (Map<String, Integer>) saved;
            for (Map.Entry<String, Integer> entry : stateMap.entrySet()) {
                QuestNode quest = getQuestNode(entry.getKey());
                if (quest != null) {
                    for (QuestState state : QuestState.values()) {
                        if (state.getValue() == entry.getValue()) {
                            quest.setState(state);
                            break;
                        }
                    }
                }
            }
        }
    }
    
    public static List<QuestNode> getQuestsByLine(QuestLine questLine) {
        List<QuestNode> quests = new ArrayList<>();
        for (QuestNode quest : questRegistry.values()) {
            if (quest.getQuestLine() == questLine) {
                quests.add(quest);
            }
        }
        return quests;
    }
    
    public static List<QuestNode> getActiveQuests() {
        List<QuestNode> active = new ArrayList<>();
        for (QuestNode quest : questRegistry.values()) {
            if (quest.getState() == QuestState.IN_PROGRESS) {
                active.add(quest);
            }
        }
        return active;
    }
    
    public static void renderQuestProgress(TooltipMakerAPI ui) {
        loadQuestStates();
        
        for (QuestLine line : QuestLine.values()) {
            List<QuestNode> quests = getQuestsByLine(line);
            boolean hasVisibleQuests = false;
            
            for (QuestNode quest : quests) {
                if (quest.getState() != QuestState.NOT_STARTED) {
                    hasVisibleQuests = true;
                    break;
                }
            }
            
            if (!hasVisibleQuests) continue;
            
            ui.addSectionHeading(line.getDisplayName(), Alignment.MID, 5f);
            
            for (QuestNode quest : quests) {
                if (quest.getState() == QuestState.NOT_STARTED) continue;
                
                Color stateColor = quest.getState().getColor();
                String statusIcon = getStateIcon(quest.getState());
                
                ui.addPara("%s %s - %s", 3f, stateColor, statusIcon, quest.getTitle(), quest.getState().getDisplayName());
                
                if (quest.getState() == QuestState.IN_PROGRESS) {
                    ui.addPara("  %s", 2f, new Color(200, 200, 200, 255), quest.getDescription());
                }
            }
        }
    }
    
    private static String getStateIcon(QuestState state) {
        switch (state) {
            case NOT_STARTED:
                return "○";
            case IN_PROGRESS:
                return "◉";
            case COMPLETED:
                return "✓";
            case FAILED:
                return "✗";
            default:
                return "?";
        }
    }
}
