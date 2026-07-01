package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import data.methods.Meng_arcfind;
import data.methods.Meng_hullsizeint;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI.ShipEngineAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.plugins.MagicTrailPlugin;

import java.awt.*;

public class Meng_fire_fluxtrans_endless extends BaseShipSystemScript {
    float timer=0f;
    boolean init=false;
    float ids = MagicTrailPlugin.getUniqueID();
    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {

        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) {
            return;
        }
        if (state == ShipSystemStatsScript.State.OUT) {
            stats.getMaxSpeed().unmodify(id); // to slow down ship to its regular top speed while powering drive down
            stats.getMaxTurnRate().unmodify(id);
        } else {
            stats.getMaxSpeed().modifyFlat(id, 50f);
            stats.getAcceleration().modifyPercent(id, 200f * effectLevel);
            stats.getDeceleration().modifyPercent(id, 200f * effectLevel);
            stats.getTurnAcceleration().modifyFlat(id, 30f * effectLevel);
            stats.getTurnAcceleration().modifyPercent(id, 200f * effectLevel);
            stats.getMaxTurnRate().modifyFlat(id, 15f);
            stats.getMaxTurnRate().modifyPercent(id, 100f);
        }

        if (stats.getEntity() instanceof ShipAPI && false) {
            String key = ship.getId() + "_" + id;
            Object test = Global.getCombatEngine().getCustomData().get(key);
            if (state == State.IN) {
                if (test == null && effectLevel > 0.2f) {
                    Global.getCombatEngine().getCustomData().put(key, new Object());
                    ship.getEngineController().getExtendLengthFraction().advance(1f);
                    for (ShipEngineAPI engine : ship.getEngineController().getShipEngines()) {
                        if (engine.isSystemActivated()) {
                            ship.getEngineController().setFlameLevel(engine.getEngineSlot(), 1f);
                        }
                    }
                }
            } else {
                Global.getCombatEngine().getCustomData().remove(key);
            }
        }
        int num=0;
        for (ShipEngineAPI engine : ship.getEngineController().getShipEngines()) {
            if (engine.getEngineSlot().getLength()<=1f) {
                SpriteAPI sprite = Global.getSettings().getSprite("fx", "base_trail_smooth");

                Color color = new Color(250, 70, 40, 200);

                Vector2f loc = engine.getLocation();

                MagicTrailPlugin.addTrailMemberAdvanced(ship, ids+num, sprite
                        , loc, 0f, 0f, Meng_arcfind.Findarc(new Vector2f(0, 0), ship.getVelocity()), 0f, 0f
                        , 6f, 3f, color, color, 1f, 0f, 0.03f, 0.3f, true, 100f, 60, 0f, null,
                        null,
                        CombatEngineLayers.ABOVE_SHIPS_LAYER,
                        360f);
                num++;

            }
        }
        ship.addAfterimage(new Color(250, 70, 40, 35), 0f, 0f, -ship.getVelocity().x, -ship.getVelocity().y, 0f, 0f, 0.05f, 1f, false, true, false);

        FluxTrackerAPI tracker = ship.getFluxTracker();
        if (timer<=0f) {
            timer = 3f;
            float hardflux = tracker.getHardFlux();
            tracker.setHardFlux(hardflux*0.66f+(tracker.getCurrFlux()-hardflux)*0.33f);
            CombatUtils.applyForce(ship, ship.getVelocity(), ship.getMass()*1.5f);
            if(init) Global.getSoundPlayer().playSound("Meng_fire_start",1f,0.7f,ship.getLocation(),new Vector2f());
            init=true;
        }
        else {
            timer-=Global.getCombatEngine().getElapsedInLastFrame();
        }

    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) {
            return;
        }
        timer = 0f;
        init=false;
        stats.getMaxSpeed().unmodify(id);
        stats.getMaxTurnRate().unmodify(id);
        stats.getTurnAcceleration().unmodify(id);
        stats.getAcceleration().unmodify(id);
        stats.getDeceleration().unmodify(id);

    }
}
