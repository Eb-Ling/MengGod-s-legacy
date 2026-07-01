package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.combat.AutofireAIPlugin;
import com.fs.starfarer.api.combat.ShipAIPlugin;
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

public class Mengplugin extends BaseModPlugin {

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
            LightData.readLightDataCSV("data/config/modFiles/meng_light_data.csv.csv");
            TextureData.readTextureDataCSV("data/config/modFiles/meng_texture_data.csv");
        }
        BoxUtilModPlugin.initPre();
    }
}
