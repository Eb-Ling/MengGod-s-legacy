package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.combat.AutofireAIPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager;
import com.thoughtworks.xstream.XStream;
import data.scripts.campaign.Meng_EmbersJumpPlugin;
import data.scripts.campaign.Meng_fleetRecover;
import data.scripts.campaign.Meng_recoverplugin;
import data.scripts.campaign.bar.*;
import data.world.MengGen;
import exerelin.campaign.SectorManager;
import org.boxutil.BoxUtilModPlugin;
import org.dark.shaders.light.LightData;
import org.dark.shaders.util.ShaderLib;
import org.dark.shaders.util.TextureData;
import lunalib.lunaRefit.LunaRefitManager;
import data.methods.Meng_ModuleRefitButton;
import data.methods.Meng_ModuleSelectorScript;

public class Mengplugin extends BaseModPlugin {

    /** OldEmpire浮动模块改装按钮是否已注册（防止重复注册） */
    private static boolean oldEmpireLunaButtonsRegistered = false;

    public void onGameLoad(boolean newGame) {

        BarEventManager bars = BarEventManager.getInstance();
        if (!bars.hasEventCreator(Meng_protect_bar_eventcreator.class)) {
            bars.addEventCreator(new Meng_protect_bar_eventcreator());
        }
        if (!bars.hasEventCreator(Meng_protect_bar_eventcreator_1.class)) {
            bars.addEventCreator(new Meng_protect_bar_eventcreator_1());
        }
        if (!bars.hasEventCreator(Meng_protect_bar_eventcreator_2.class)) {
            bars.addEventCreator(new Meng_protect_bar_eventcreator_2());
        }
        if (!bars.hasEventCreator(MouMeng_bar_eventcreator_1.class)) {
            bars.addEventCreator(new MouMeng_bar_eventcreator_1());
        }
        
        // 注册圣殿跳跃点Memory标记脚本
        Global.getSector().removeScriptsOfClass(Meng_EmbersJumpPlugin.class);
        Global.getSector().addScript(new Meng_EmbersJumpPlugin());

        // 注册OldEmpire浮动模块改装按钮
        registerOldEmpireLunaButtons();
    }

    @Override
    public PluginPick<AutofireAIPlugin> pickWeaponAutofireAI(WeaponAPI weapon) {
        return super.pickWeaponAutofireAI(weapon);
    }

    @Override
    public void configureXStream(XStream x) {
        x.alias("Meng_Mainstory", Meng_fleetRecover.class);
        x.alias("Meng_Mainstory1", Meng_recoverplugin.class);
    }
    @Override
    public void onNewGame() {
        //Nex compatibility setting, if there is no nex or corvus mode(Nex), just generate the system
        boolean haveNexerelin = Global.getSettings().getModManager().isModEnabled("nexerelin");
        if (!haveNexerelin || SectorManager.getManager().isCorvusMode()) {
            new MengGen().generate(Global.getSector());
        }
    }
    @Override
    public void onApplicationLoad() {

        ShaderLib.init();

        if (ShaderLib.areShadersAllowed() && ShaderLib.areBuffersAllowed()) {
            //LightData.readLightDataCSV("");
            // TextureData.readTextureDataCSV("");
        }

        if (Global.getSettings().getModManager().isModEnabled("shaderLib")) {
            ShaderLib.init();
            LightData.readLightDataCSV("data/config/modFiles/meng_light_data.csv");
            TextureData.readTextureDataCSV("data/config/modFiles/meng_texture_data.csv");
        }
        BoxUtilModPlugin.initPre();

        // 加载浮动模块CSV配置数据（必须在按钮注册前调用）
        Meng_ModuleSelectorScript.loadModuleData();

        // 注册OldEmpire浮动模块改装按钮
        registerOldEmpireLunaButtons();
    }

    /**
     * 向LunaRefit注册OldEmpire浮动模块切换按钮。
     * 根据CSV中注册的模块数量动态注册按钮（轻/重型各自有多少种模式就注册多少个），
     * 玩家在改装界面可通过按钮在不同浮动模块模式间切换。
     * 相位纹理的预加载已改由settings.json的"Meng_OldEmpire_phase"分类声明，
     * 由游戏引擎在加载时自动完成，无需在代码中手动加载。
     */
    private static void registerOldEmpireLunaButtons() {
        if (oldEmpireLunaButtonsRegistered) {
            return;
        }
        // 动态获取模式数量（从CSV加载），轻/重型取最大值以确保全部覆盖
        int maxModes = Math.max(
                Meng_ModuleSelectorScript.getModeCount(true),
                Meng_ModuleSelectorScript.getModeCount(false));
        if (maxModes <= 0) {
            Global.getLogger(Mengplugin.class)
                    .warn("OldEmpire floating module data is empty; refit buttons were not registered");
            return;
        }
        for (int i = 0; i < maxModes; i++) {
            // A组：轻型和重型各一种模式
            LunaRefitManager.addRefitButton(new Meng_ModuleRefitButton(true, i));
            LunaRefitManager.addRefitButton(new Meng_ModuleRefitButton(false, i));
            // B组：额外的第二组浮动模块槽位
            LunaRefitManager.addRefitButton(new Meng_ModuleRefitButton(false, i, 1));
        }
        oldEmpireLunaButtonsRegistered = true;
    }
}
