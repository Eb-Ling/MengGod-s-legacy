package data.shipsystems.scripts;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.scripts.util.MagicUI;
import data.weapons.Meng_clockbuildereffect;
import org.boxutil.units.standard.entity.TextFieldEntity;

import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.EnumSet;
import java.util.List;

public class Meng_Timesystem extends BaseShipSystemScript {


    public static final String KEY = "Mengtimelistener_S";
    private static final IntervalUtil Interval = new IntervalUtil(0.5f, 0.5f);
    String ids = "Meng_timesystemplugin";
    private float timer = 0f;
    private boolean init = false;
    private boolean init1 = false;
    private int venergy = 0;
    
    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) {
            return;
        }
        
        if (Global.getCombatEngine() == null || Global.getCombatEngine().isPaused()) {
            return;
        }
        
        if (!ship.getCustomData().containsKey(KEY)) {
            Meng_clockbuildereffect.DataContainer data = new Meng_clockbuildereffect.DataContainer();
            ship.setCustomData(KEY, data);
        }
        Meng_clockbuildereffect.DataContainer data = (Meng_clockbuildereffect.DataContainer) ship.getCustomData().get(KEY);

        data.timestop = true;
        if (!init1) {
            init1 = true;
            venergy = data.energy;
        }
        if (!init) {
            Interval.advance(Global.getCombatEngine().getElapsedInLastFrame());
            if (Interval.intervalElapsed() && data.energy > 0) {
                data.energy = Math.max(0, data.energy - 1);
                if (ship.getMutableStats().getPeakCRDuration().getFlatBonus(ids) != null) {
                    ship.getMutableStats().getPeakCRDuration().modifyFlat(ids, ship.getMutableStats().getPeakCRDuration().getFlatBonus(ids).value - 8f);
                } else {
                    ship.getMutableStats().getPeakCRDuration().modifyFlat(ids, -8f);
                }
            }
        }
        if (data.energy == 0) {
            init = true;
        }
        if (init) {
            timer += Global.getCombatEngine().getElapsedInLastFrame();
            if (timer >= 0.5f) {
                Global.getSoundPlayer().playSound("Meng_Xusound", 1f, 1f, ship.getLocation(), new Vector2f());
                Global.getCombatEngine().addLayeredRenderingPlugin(new Meng_TimesystemPlugin(ship, venergy));
                init = false;
                ship.getSystem().deactivate();
            }
        }

    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {

        init = false;
        init1 = false;
        venergy = 0;
        timer = 0f;
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) {
            return;
        }
        
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isCombatOver()) {
            return;
        }
        
        Boolean pluginSpawned = (Boolean) ship.getCustomData().get("Meng_timesystem_plugin_spawned");
        if (pluginSpawned == null || !pluginSpawned) {
            ship.getCustomData().put("Meng_timesystem_plugin_spawned", true);
            if(!engine.hasPluginOfClass(Meng_timePlugin.class)) engine.addPlugin(new Meng_timePlugin(ship));
        }
    }
    
    // 保留这个空的匿名类以兼容旧存档
    // 注意：不要在任何地方创建新的实例
    private static final EveryFrameScript LEGACY_SCRIPT_COMPATIBILITY = new EveryFrameScript() {
        @Override
        public boolean isDone() {
            return true;  // 立即结束
        }

        @Override
        public boolean runWhilePaused() {
            return false;
        }

        @Override
        public void advance(float amount) {
            // 空实现，不做任何事情
        }
    };
    
    private static final class Meng_timePlugin extends BaseEveryFrameCombatPlugin {
        private CombatEngineAPI engine = null;
        private TextFieldEntity textEntity = null;
        private ShipAPI ship;
        private ViewportAPI viewport;
        protected IntervalUtil checkInterval = new IntervalUtil(10f, 10f);
        private boolean expired = false;
        
        public Meng_timePlugin(ShipAPI s){
            ship = s;
        }
        
        public void init(CombatEngineAPI engine) {
            this.engine = engine;
            this.viewport = engine.getViewport();
        }

        public boolean isDone() {
            return expired || engine == null || engine.isCombatOver() || ship == null || !ship.isAlive();
        }


        public void advance(float amount, List events){
            if (engine == null || engine.isCombatOver() || ship == null || !ship.isAlive()) {
                expired = true;
                if (textEntity != null) {
                    textEntity.delete();
                    textEntity = null;
                }
                return;
            }
            
            float scale = viewport.getViewMult();
            Vector2f loc = new Vector2f(
                ship.getLocation().x - 256f * scale + (ship.getShieldRadiusEvenIfNoShield() * 0.28f) * scale,
                ship.getLocation().y - 16f * scale + (ship.getShieldRadiusEvenIfNoShield() * 0.35f) * scale
            );
            

            if (Global.getSector().getPlayerFleet() != null &&
                ship.getFleetMember() != null &&
                ship.getFleetMember().getFleetData() != null &&
                ship.getFleetMember().getFleetData().getFleet() != null) {

                if (ship.getFleetMember().getFleetData().getFleet() != Global.getSector().getPlayerFleet()) {
                    if (ship.isAlive()) {
                        checkInterval.advance(amount);
                    }
                    if (checkInterval.intervalElapsed()) {
                        String currentMusic = Global.getSoundPlayer().getCurrentMusicId();
                        if (currentMusic != null && currentMusic.replaceAll(".ogg", "").contentEquals("Meng_Timebossbgm")) {
                            Global.getSoundPlayer().resumeCustomMusic();
                        } else {
                            Global.getSoundPlayer().playCustomMusic(1, 1, "Meng_Timebossbgm", false);
                        }
                    }
                }
            }
            
            if (this.textEntity == null) {
                this.textEntity = new TextFieldEntity("graphics/fonts/insignia15LTaa.fnt");
                this.textEntity.addText("时旅者" + TextFieldEntity.LINE_FEED_SYMBOL,
                        0.0f, Misc.getButtonTextColor(),
                        false, false, true, false, 0);
                this.textEntity.setAlignment(TextFieldEntity.Alignment.MID);
                this.textEntity.setFontSpace(0.0f, 5.0f);
                this.textEntity.setFieldSize(512.0f, 128.0f);
                this.textEntity.setTextDataRefreshAllFromCurrentIndex();
                this.textEntity.submitText();
                this.textEntity.setStateVanilla(loc, 0f, new Vector2f(scale, scale));
            } else {
                this.textEntity.setStateVanilla(loc, 0f, new Vector2f(scale, scale));
                this.textEntity.submitText();
                this.textEntity.directDraw(false);
            }
        }

        public void renderInUICoords(ViewportAPI viewport) {
        }
    }
    
    public static class Meng_TimesystemPlugin implements CombatLayeredRenderingPlugin {
        public static final String KEY = "Mengtimelistener_S";
        private final ShipAPI ships;
        int energys;
        int renergy;
        String ids = "Meng_timesystemplugin";
        private float timer;

        public Meng_TimesystemPlugin(ShipAPI ship, int energy) {
            ships = ship;
            energys = energy;
            renergy = energy;
        }

        public void init(CombatEntityAPI entity) {
        }

        @Override
        public void cleanup() {
            if (ships != null && ships.isAlive()) {
                ships.getMutableStats().getTimeMult().unmodifyMult(ids);
                Global.getCombatEngine().getTimeMult().unmodify(ids);
                ships.getMutableStats().getAcceleration().unmodifyPercent(ids);
                ships.getMutableStats().getTurnAcceleration().unmodifyPercent(ids);
                ships.getMutableStats().getMaxTurnRate().unmodifyPercent(ids);
                ships.getMutableStats().getMaxSpeed().unmodifyPercent(ids);
                
                if (ships.getCustomData().containsKey(KEY)) {
                    Meng_clockbuildereffect.DataContainer data = 
                        (Meng_clockbuildereffect.DataContainer) ships.getCustomData().get(KEY);
                    if (data != null) {
                        data.timestop = false;
                    }
                }
            }
        }

        @Override
        public boolean isExpired() {
            return timer > renergy * 2f / 15f || ships == null || !ships.isAlive();
        }

        @Override
        public void advance(float amount) {
            if (ships == null || !ships.isAlive()) {
                return;
            }
            
            timer += amount;

            if (!ships.getCustomData().containsKey(KEY)) {
                Meng_clockbuildereffect.DataContainer data = new Meng_clockbuildereffect.DataContainer();
                ships.setCustomData(KEY, data);
            }
            Meng_clockbuildereffect.DataContainer data = 
                (Meng_clockbuildereffect.DataContainer) ships.getCustomData().get(KEY);
            
            MagicUI.drawHUDStatusBar(ships, 
                ((renergy * 2f) / 15f - timer) / ((renergy * 2f) / 15f), 
                new Color(34, 248, 241, 255), 
                new Color(0, 165, 255, 255), 
                0f, 
                "规律重整剩余", 
                "            " + Math.floor((renergy * 2f) * 10f / 15f - timer * 10f) + "秒", 
                false);

            if (timer >= (renergy * 2f) / 15f) {
                ships.getMutableStats().getTimeMult().unmodifyMult(ids);
                Global.getCombatEngine().getTimeMult().unmodify(ids);
                ships.getMutableStats().getAcceleration().unmodifyPercent(ids);
                ships.getMutableStats().getTurnAcceleration().unmodifyPercent(ids);
                ships.getMutableStats().getMaxTurnRate().unmodifyPercent(ids);
                ships.getMutableStats().getMaxSpeed().unmodifyPercent(ids);
                data.timestop = false;
            } else {
                if (ships == Global.getCombatEngine().getPlayerShip()) {
                    Global.getCombatEngine().getTimeMult().modifyMult(ids, 1 / 10f);
                    ships.getMutableStats().getAcceleration().modifyPercent(ids, 50f);
                    ships.getMutableStats().getTurnAcceleration().modifyPercent(ids, 50f);
                    ships.getMutableStats().getMaxTurnRate().modifyPercent(ids, 50f);
                    ships.getMutableStats().getMaxSpeed().modifyPercent(ids, 50f);
                }
                ships.getMutableStats().getTimeMult().modifyMult(ids, 15f);
            }
        }

        @Override
        public EnumSet<CombatEngineLayers> getActiveLayers() {
            return EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_LAYER);
        }

        @Override
        public float getRenderRadius() {
            return 10000f;
        }

        @Override
        public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        }
    }
}
