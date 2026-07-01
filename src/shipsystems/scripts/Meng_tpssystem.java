package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import data.scripts.plugins.MagicRenderPlugin;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lwjgl.util.vector.Vector2f;

public class Meng_tpssystem extends BaseShipSystemScript {
    private boolean init = false;
    private boolean init1 = false;
    private float timer = 0f;
    private float timer1 = 0f;
    private float x = 0f;
    private float y = 0f;
    private Vector2f loc;

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) {
            return;
        }
        SpriteAPI sprite1 = Global.getSettings().getSprite("Meng", "Meng_system2");
        SpriteAPI sprite2 = Global.getSettings().getSprite("Meng", "Meng_system3");
        SpriteAPI sprite3 = Global.getSettings().getSprite("Meng", "Meng_system4");
        SpriteAPI sprites1 = Global.getSettings().getSprite("Meng", "Meng_system2");
        SpriteAPI sprites2 = Global.getSettings().getSprite("Meng", "Meng_system3");
        SpriteAPI sprites3 = Global.getSettings().getSprite("Meng", "Meng_system4");
        timer += Global.getCombatEngine().getElapsedInLastFrame();
        if (!init) {
            init = true;
            loc = ship.getLocation();
            x = loc.getX();
            y = loc.getY();
            RippleDistortion ripple = new RippleDistortion(ship.getLocation(), new Vector2f());
            ripple.setSize(2300f);
            ripple.setIntensity(1000f);
            ripple.fadeInSize(5f);
            ripple.fadeOutIntensity(3f);
            ripple.setFrameRate(45f);
            DistortionShader.addDistortion(ripple);
        }
        if (ship.getSystem().getState() == ShipSystemAPI.SystemState.IN) {
            for (FighterLaunchBayAPI bay : ship.getLaunchBaysCopy()) {
                if (bay.getWing().getLeader() != null && bay.getWing().getLeader().getWing() != null && bay.getWing().getLeader().getWing().getWingMembers() != null) {
                    for (ShipAPI wing : bay.getWing().getLeader().getWing().getWingMembers())
                        wing.setAlphaMult(Math.max(0f, 1f - timer));
                }
            }
            MagicRenderPlugin.addSingleframe(sprite1, loc, CombatEngineLayers.ABOVE_SHIPS_LAYER);
            MagicRenderPlugin.addSingleframe(sprite2, loc, CombatEngineLayers.ABOVE_SHIPS_LAYER);
            MagicRenderPlugin.addSingleframe(sprite3, loc, CombatEngineLayers.ABOVE_SHIPS_LAYER);
            sprite1.setSize(700f * timer, 700f * timer);
            sprite1.setAngle(360 * timer);
            sprite1.setAlphaMult(1F);

            sprite2.setSize(700f * timer, 700f * timer);
            sprite2.setAngle(360 * timer / 0.6f);
            sprite2.setAlphaMult(1F);

            sprite3.setSize(700f * timer, 700f * timer);
            sprite3.setAngle(360 * timer);
            sprite3.setAlphaMult(1F);
            if (timer > 1f) {
                ship.setAlphaMult(0f);
                MagicRenderPlugin.addSingleframe(sprite1, new Vector2f(x, y), CombatEngineLayers.ABOVE_SHIPS_LAYER);
                MagicRenderPlugin.addSingleframe(sprite2, new Vector2f(x, y), CombatEngineLayers.ABOVE_SHIPS_LAYER);
                MagicRenderPlugin.addSingleframe(sprite3, new Vector2f(x, y), CombatEngineLayers.ABOVE_SHIPS_LAYER);
                sprite1.setSize(Math.max(2333f * (1.3f - timer), 0), Math.max(2333f * (1.3f - timer), 0));
                sprite1.setAngle(360 * timer / 0.5f);
                sprite1.setAlphaMult(1F);

                sprite2.setSize(Math.max(2333f * (1.3f - timer), 0), Math.max(2333f * (1.3f - timer), 0));
                sprite2.setAngle(360 * timer / 0.3f);
                sprite2.setAlphaMult(1F);

                sprite3.setSize(Math.max(2333f * (1.3f - timer), 0), Math.max(2333f * (1.3f - timer), 0));
                sprite3.setAngle(360 * timer / 0.5f);
                sprite3.setAlphaMult(1F);
            }
        }

        if (ship.getSystem().getState() == ShipSystemAPI.SystemState.OUT) {
            ship.setAlphaMult(1f);

            for (FighterLaunchBayAPI bay : ship.getLaunchBaysCopy()) {
                if (bay.getWing().getLeader() != null && bay.getWing().getLeader().getWing() != null && bay.getWing().getLeader().getWing().getWingMembers() != null) {
                    for (ShipAPI wing : bay.getWing().getLeader().getWing().getWingMembers()) {
                        wing.getLocation().set(ship.getLocation());
                        wing.setAlphaMult(1f);
                    }
                }
            }

            timer1 += Global.getCombatEngine().getElapsedInLastFrame();
            if (timer1 <= 0.25f) {
                if (!init1) {
                    init1 = true;
                    RippleDistortion ripple1 = new RippleDistortion(ship.getLocation(), new Vector2f());
                    ripple1.setSize(7000f);
                    ripple1.setIntensity(1000f);
                    ripple1.fadeInSize(5f);
                    ripple1.fadeOutIntensity(3f);
                    ripple1.setFrameRate(150f);
                    DistortionShader.addDistortion(ripple1);
                }
                MagicRenderPlugin.addSingleframe(sprites1, loc, CombatEngineLayers.ABOVE_SHIPS_LAYER);
                MagicRenderPlugin.addSingleframe(sprites2, loc, CombatEngineLayers.ABOVE_SHIPS_LAYER);
                MagicRenderPlugin.addSingleframe(sprites3, loc, CombatEngineLayers.ABOVE_SHIPS_LAYER);
                sprites1.setSize(450f + 350f * timer1 * 4f, 450f + 350f * timer1 * 4f);
                sprites1.setAngle(360 * timer1 / 0.5f);
                sprites1.setAlphaMult(1F);

                sprites2.setSize(450f + 350f * timer1 * 4f, 450f + 350f * timer1 * 4f);
                sprites2.setAngle(360 * timer1 / 0.4f);
                sprites2.setAlphaMult(1F);

                sprites3.setSize(450f + 350f * timer1 * 4f, 450f + 350f * timer1 * 4f);
                sprites3.setAngle(360 * timer1 / 0.75f);
                sprites3.setAlphaMult(1F);
            } else {
                MagicRenderPlugin.addSingleframe(sprites1, loc, CombatEngineLayers.ABOVE_SHIPS_LAYER);
                MagicRenderPlugin.addSingleframe(sprites2, loc, CombatEngineLayers.ABOVE_SHIPS_LAYER);
                MagicRenderPlugin.addSingleframe(sprites3, loc, CombatEngineLayers.ABOVE_SHIPS_LAYER);
                sprites1.setSize(Math.max(1200f * (0.75f - timer1) * 1.333f, 0), Math.max(1200f * (0.75f - timer1) * 1.333f, 0));
                sprites1.setAngle(360 * timer / 0.75f);
                sprites1.setAlphaMult(1F);

                sprites2.setSize(Math.max(1200f * (0.75f - timer1) * 1.333f, 0), Math.max(1200f * (0.75f - timer1) * 1.333f, 0));
                sprites2.setAngle(360 * timer / 0.5f);
                sprites2.setAlphaMult(1F);

                sprites3.setSize(Math.max(1200f * (0.75f - timer1) * 1.333f, 0), Math.max(1200f * (0.75f - timer1) * 1.333f, 0));
                sprites3.setAngle(360 * timer / 0.75f);
                sprites3.setAlphaMult(1F);
            }


        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) {
            return;
        }

        timer = 0f;
        timer1 = 0f;
        init = false;
        init1 = false;
    }
}
