package data.scripts.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.JumpPointAPI;
import com.fs.starfarer.api.campaign.JumpPointAPI.JumpDestination;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;

import java.util.List;
import java.util.Map;

/**
 * 圣殿跳跃点专用跳跃命令
 * 
 * 使用方法:
 * MengEmbersJumpCMD jump <destination_index>
 * 
 * 参数说明:
 * - destination_index: 目标索引(从0开始),对应jumpPoint.getDestinations()列表中的位置
 * 
 * 示例:
 * MengEmbersJumpCMD jump 0  // 跳跃到第一个目的地
 * 
 * 功能:
 * - 执行超空间跳跃
 * - 消耗燃料
 * - 关闭对话框
 * - 触发跳跃动画
 */
public class MengEmbersJumpCMD extends BaseCommandPlugin {
    
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        if (params.size() < 2) {
            return false;
        }
        
        String command = params.get(0).getString(memoryMap);
        if (command == null) {
            return false;
        }
        
        SectorEntityToken entity = dialog.getInteractionTarget();
        if (!(entity instanceof JumpPointAPI)) {
            return false;
        }
        
        JumpPointAPI jumpPoint = (JumpPointAPI) entity;
        
        if ("jump".equals(command)) {
            // 获取目标索引
            int destIndex = params.get(1).getInt(memoryMap);
            
            // 验证索引有效性
            if (destIndex < 0 || destIndex >= jumpPoint.getDestinations().size()) {
                return false;
            }
            
            // 执行跳跃
            performJump(dialog, jumpPoint, destIndex);
            return true;
        }
        
        return false;
    }
    
    /**
     * 执行跳跃逻辑
     * 
     * @param dialog 交互对话框
     * @param jumpPoint 跳跃点实体
     * @param destIndex 目标索引
     */
    private void performJump(InteractionDialogAPI dialog, JumpPointAPI jumpPoint, int destIndex) {
        // 获取玩家舰队
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        
        // 获取目标
        JumpDestination dest = jumpPoint.getDestinations().get(destIndex);
        
        // 计算燃料消耗
        float fuelCost = calculateFuelCost(jumpPoint, dest);
        
        if (Global.getSector().getUIData().getCourseTarget() == dialog.getInteractionTarget()) {
            Global.getSector().getCampaignUI().clearLaidInCourse();
        }
        
        // 关闭对话框
        dialog.dismiss();
        
        // 取消暂停
        Global.getSector().setPaused(false);
        
        // 执行超空间跳跃
        Global.getSector().doHyperspaceTransition(playerFleet, jumpPoint, dest);
        
        // 移除燃料
        if (fuelCost > 0) {
            playerFleet.getCargo().removeFuel(fuelCost);
        }
    }
    
    /**
     * 计算燃料消耗
     * 
     * @param jumpPoint 跳跃点
     * @param dest 目标
     * @return 燃料消耗量
     */
    private float calculateFuelCost(JumpPointAPI jumpPoint, JumpDestination dest) {
        // 如果是虫洞,燃料消耗乘以5倍
        if (jumpPoint.isWormhole()) {
            float baseCost = Global.getSector().getPlayerFleet().getLogistics().getFuelCostPerLightYear();
            return baseCost * 5f;
        }
        
        // 如果在超空间中,不消耗燃料
        if (jumpPoint.isInHyperspace()) {
            return 0f;
        }
        
        // 普通跳跃点,消耗基础燃料
        float fuelCost = Global.getSector().getPlayerFleet().getLogistics().getFuelCostPerLightYear();
        
        // 四舍五入
        float rounded = Math.round(fuelCost);
        if (fuelCost > 0 && rounded <= 0) {
            rounded = 1;
        }
        
        return rounded;
    }
}
