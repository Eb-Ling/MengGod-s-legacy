package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.IntervalUtil;
import data.methods.Meng_arcfind;
import data.methods.Meng_hullsizeint;
import data.scripts.plugins.MagicRenderPlugin;
import data.scripts.plugins.MagicTrailPlugin;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.entities.SimpleEntity;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.EnumSet;

public class Meng_Starfalling extends BaseShipSystemScript {
    private static final IntervalUtil Interval = new IntervalUtil(0.4f, 0.6f);
    private float timer1 = 0f;

    private float timer2 = 0f;
    private float lastdamage = 0f;
    private boolean init1 = false;
    private boolean init2 = false;
    private boolean inits = false;
    private boolean init3 = false;
    private boolean init4 = false;
    private boolean init6 = false;
    private Vector2f loc = new Vector2f();
    private Vector2f targetloc = new Vector2f();
    private Vector2f shiploc = new Vector2f();
    private Vector2f endloc = new Vector2f();
    private Vector2f lastloc = new Vector2f();
    private float arg1 = 0f;
    private float arg2 = 0f;
    private float flux = 0f;
    private ShipAPI target;
    private ShipAPI lasttarget;
    private float ids = 0f;
    private boolean init5 = false;
    private float timer3 = 0f;

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {

        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) {
            return;
        }

        boolean player = false;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
            player = ship == Global.getCombatEngine().getPlayerShip();
        }
        if (ship.getSystem().getState() == ShipSystemAPI.SystemState.IN) {
            ship.getVelocity().set(0, 0);


            if (!init1) {
                init1 = true;
                loc = new Vector2f(ship.getLocation().getX(), ship.getLocation().getY());
                arg1 = ship.getFacing();
                RippleDistortion ripple1 = new RippleDistortion(loc, new Vector2f());
                ripple1.setSize(600f);
                ripple1.setIntensity(30f);
                ripple1.fadeInSize(0.2f);
                ripple1.fadeInIntensity(0.1f);
                ripple1.setFrameRate(150f);
                DistortionShader.addDistortion(ripple1);
                flux = ship.getCurrFlux();
            }
            ship.getFluxTracker().setCurrFlux(flux);
            timer1 += Global.getCombatEngine().getElapsedInLastFrame();
            if (timer1 <= 3f) {
                stats.getTimeMult().modifyMult(id, 5f);
                if (player) {
                    Global.getCombatEngine().getTimeMult().modifyMult(id, 1f / 5f);
                }
                SpriteAPI sprite = Global.getSettings().getSprite("Meng", "Meng_Starring1");
                MagicRenderPlugin.addSingleframe(sprite, loc, CombatEngineLayers.ABOVE_SHIPS_LAYER);
                sprite.setAngle(timer1 * 60f);

                if (timer1 <= 0.5f) {
                    sprite.setAlphaMult(timer1 * 1.5f);
                    sprite.setSize(500 * timer1, 500f * timer1);
                } else if (timer1 <= 2.5f) {
                    sprite.setAlphaMult(0.75f);
                } else {
                    sprite.setAlphaMult((3f - timer1) * 1.5f);
                }
            }
            if (timer1 <= 2f) {
                SpriteAPI starpoint = Global.getSettings().getSprite("Meng", "Meng_Starfalling5");
                if (timer1 <= 0.5f) {
                    MagicRenderPlugin.addSingleframe(starpoint, loc, CombatEngineLayers.ABOVE_SHIPS_LAYER);
                    starpoint.setAngle(arg1 + timer1 * 1500f);
                    starpoint.setSize(100f + timer1 * 200f, 100f + timer1 * 200f);
                } else if (timer1 <= 1f) {
                    MagicRenderPlugin.addSingleframe(starpoint, loc, CombatEngineLayers.ABOVE_SHIPS_LAYER);
                    starpoint.setAngle(arg1 + timer1 * 1500f);
                    starpoint.setSize(300f - timer1 * 200f, 300f - timer1 * 200f);
                } else if (timer1 <= 1.5f) {
                    MagicRenderPlugin.addSingleframe(starpoint, loc, CombatEngineLayers.ABOVE_SHIPS_LAYER);
                    starpoint.setAngle(arg1 + timer1 * 1500f);
                    starpoint.setSize(100f + (timer1 - 1f) * 200f, 100f + (timer1 - 1f) * 200f);
                } else if (timer1 <= 2f) {
                    MagicRenderPlugin.addSingleframe(starpoint, loc, CombatEngineLayers.ABOVE_SHIPS_LAYER);
                    starpoint.setAngle(arg1 + timer1 * 1500f);
                    starpoint.setSize(400f * (2f - timer1), 400f * (2f - timer1));
                }
                ship.setAlphaMult(1f - timer1 / 4f);
                ship.setFacing(arg1);
                ship.getLocation().set(loc.getX() + 300f * timer1 / 2f * (float) Math.sin(Math.toRadians(arg1)), loc.getY() + 300f * timer1 / 2f * (float) Math.cos(Math.toRadians(arg1)));
                SpriteAPI sprite1 = ship.getSpriteAPI();
                MagicRenderPlugin.addSingleframe(sprite1, new Vector2f(loc.getX() + 400f * timer1 / 2f * (float) Math.sin(Math.toRadians(arg1 - 200f)), loc.getY() + 400f * timer1 / 2f * (float) Math.cos(Math.toRadians(arg1 - 200f))), CombatEngineLayers.ABOVE_SHIPS_LAYER);
                Interval.advance(Global.getCombatEngine().getElapsedInLastFrame());
                if (Interval.intervalElapsed()) {
                    Global.getCombatEngine().spawnEmpArcPierceShields(ship, loc, new SimpleEntity(new Vector2f(loc.getX() + 400f * timer1 / 2f * (float) Math.sin(Math.toRadians(arg1 - 200f)), loc.getY() + 400f * timer1 / 2f * (float) Math.cos(Math.toRadians(arg1 - 200f)))), new SimpleEntity(new Vector2f(loc.getX() + 400f * timer1 / 2f * (float) Math.sin(Math.toRadians(arg1 - 200f)), loc.getY() + 400f * timer1 / 2f * (float) Math.cos(Math.toRadians(arg1 - 200f)))), DamageType.ENERGY, 0f, 0f, 100000f, null, 10f, new Color(1, 93, 169, 255), new Color(0, 248, 237, 255));
                    Global.getCombatEngine().spawnEmpArcPierceShields(ship, loc, new SimpleEntity(new Vector2f(loc.getX() + 300f * timer1 / 2f * (float) Math.sin(Math.toRadians(arg1)), loc.getY() + 300f * timer1 / 2f * (float) Math.cos(Math.toRadians(arg1)))), new SimpleEntity(new Vector2f(loc.getX() + 300f * timer1 / 2f * (float) Math.sin(Math.toRadians(arg1)), loc.getY() + 300f * timer1 / 2f * (float) Math.cos(Math.toRadians(arg1)))), DamageType.ENERGY, 0f, 0f, 100000f, null, 10f, new Color(1, 93, 169, 255), new Color(0, 248, 237, 255));
                }
            } else if (timer1 <= 3f) {
                ship.setAlphaMult(0.5f - (timer1 - 2f) / 2f);
                ship.setFacing(arg1);
                ship.getLocation().set(loc.getX() + 300f * timer1 / 2f * (float) Math.sin(Math.toRadians(arg1)) + 300f * (timer1 - 2f) * (float) Math.sin(Math.toRadians(arg1)), loc.getY() + 300f * timer1 / 2f * (float) Math.cos(Math.toRadians(arg1)) + 300f * (timer1 - 2f) * (float) Math.cos(Math.toRadians(arg1)));
                SpriteAPI sprite1 = ship.getSpriteAPI();
                MagicRenderPlugin.addSingleframe(sprite1, new Vector2f(loc.getX() + 400f * timer1 / 2f * (float) Math.sin(Math.toRadians(arg1 - 200f)) + 500f * (timer1 - 2f) * (float) Math.sin(Math.toRadians(arg1 - 200f)), loc.getY() + 400f * timer1 / 2f * (float) Math.cos(Math.toRadians(arg1 - 200f)) + 500f * (timer1 - 2f) * (float) Math.cos(Math.toRadians(arg1 - 200f))), CombatEngineLayers.ABOVE_SHIPS_LAYER);
                SpriteAPI sprite2 = Global.getSettings().getSprite("Meng", "Meng_Starfalling1");
                MagicRenderPlugin.addSingleframe(sprite2, ship.getLocation(), CombatEngineLayers.ABOVE_SHIPS_LAYER);
                sprite2.setAlphaMult(timer1 - 2f);
                sprite2.setAngle(arg1 - 90f);
                SpriteAPI sprite3 = Global.getSettings().getSprite("Meng", "Meng_Starring2");
                sprite3.setAlphaMult(timer1 - 2f);
                sprite3.setSize(500f, 500f);
                MagicRenderPlugin.addSingleframe(sprite3, ship.getLocation(), CombatEngineLayers.UNDER_SHIPS_LAYER);
                sprite3.setAngle(timer1 * 60f);
            } else if (timer1 <= 21f) {

                timer2 += Global.getCombatEngine().getElapsedInLastFrame();
                if (timer2 <= 1f) {
                    stats.getTimeMult().modifyMult(id, 5f);
                    if (player) {
                        Global.getCombatEngine().getTimeMult().modifyMult(id, 1f / 5f);
                    }
                    if (!init2) {
                        ids = MagicTrailPlugin.getUniqueID();
                        init2 = true;
                        float d = 0f;
                        lastloc = new Vector2f(loc.getX() + 300f * timer1 / 2f * (float) Math.sin(Math.toRadians(arg1)) + 300f * (timer1 - 2f) * (float) Math.sin(Math.toRadians(arg1)), loc.getY() + 300f * timer1 / 2f * (float) Math.cos(Math.toRadians(arg1)) + 300f * (timer1 - 2f) * (float) Math.cos(Math.toRadians(arg1)));
                        if (ship.getShipTarget() != null && ship.getShipTarget().isAlive() && Vector2f.sub(ship.getShipTarget().getLocation(), ship.getLocation(), new Vector2f()).length() - ship.getShieldRadiusEvenIfNoShield() - ship.getShipTarget().getShieldRadiusEvenIfNoShield() <= 4000f) {
                            target = ship.getShipTarget();
                        } else {
                            for (ShipAPI ships : AIUtils.getNearbyEnemies(ship, 4000f)) {
                                if (!ships.isDrone() && ships.isAlive() && !ships.isFighter()) {
                                    if (d < Vector2f.sub(loc, ships.getLocation(), new Vector2f()).length()) {
                                        d = Vector2f.sub(loc, ships.getLocation(), new Vector2f()).length();
                                        target = ships;
                                    }
                                }
                            }
                        }
                        lasttarget = target;
                    }
                    if (target != null) {
                        if (target.getLocation().getY() > lastloc.getY()) {
                            arg2 = 90f - (float) Math.toDegrees(Math.atan((target.getLocation().getX() - lastloc.getX()) / (target.getLocation().getY() - lastloc.getY())));
                        } else {
                            arg2 = 270f - (float) Math.toDegrees(Math.atan((target.getLocation().getX() - lastloc.getX()) / (target.getLocation().getY() - lastloc.getY())));
                        }
                        ship.getLocation().set(lastloc);
                        ship.setPhased(true);
                        SpriteAPI sprite2 = Global.getSettings().getSprite("Meng", "Meng_Starfalling1");
                        MagicRenderPlugin.addSingleframe(sprite2, new Vector2f(ship.getLocation().getX() + 20f * (float) Math.random() * timer2, ship.getLocation().getY() + 20f * (float) Math.random() * timer2), CombatEngineLayers.ABOVE_SHIPS_LAYER);
                        sprite2.setAlphaMult(1f);
                        SpriteAPI sprite3 = Global.getSettings().getSprite("Meng", "Meng_Starring2");
                        sprite3.setAlphaMult(1f);
                        sprite3.setSize(500f, 500f);
                        MagicRenderPlugin.addSingleframe(sprite3, ship.getLocation(), CombatEngineLayers.UNDER_SHIPS_LAYER);
                        sprite3.setAngle(timer1 * 60f);
                        for (int i = 1; i <= 10f; i++) {
                            if (arg1 >= 360f) {
                                arg1 = arg1 - 360f;
                            } else if (arg1 <= 0f) {
                                arg1 = arg1 + 360f;
                            }
                            if (arg2 >= 360f) {
                                arg2 = arg2 - 360f;
                            } else if (arg2 <= 0f) {
                                arg2 = arg2 + 360f;
                            }
                        }
                        if (Math.abs(arg1 - arg2) <= 180f) {
                            if (arg1 <= arg2) {
                                ship.setFacing(arg1 + timer2 * (arg2 - arg1));
                                sprite2.setAngle(arg1 - 90f + timer2 * (arg2 - arg1));
                            } else {
                                ship.setFacing(arg1 - timer2 * (arg1 - arg2));
                                sprite2.setAngle(arg1 - 90f - timer2 * (arg1 - arg2));
                            }
                        } else {
                            if (arg1 <= arg2) {
                                ship.setFacing(arg1 - timer2 * (360f - arg2 + arg1));
                                sprite2.setAngle(arg1 - 90f - timer2 * (360f - arg2 + arg1));
                            } else {
                                ship.setFacing(arg1 + timer2 * (360f - arg1 + arg2));
                                sprite2.setAngle(arg1 - 90f + timer2 * (360f - arg1 + arg2));
                            }
                        }
                        targetloc = new Vector2f(target.getLocation().getX(), target.getLocation().getY());
                        shiploc = new Vector2f(ship.getLocation().getX(), ship.getLocation().getY());
                    }
                } else if (timer2 <= 3f) {

                    if (target != null) {
                        stats.getTimeMult().modifyMult(id, 5f);
                        if (player) {
                            Global.getCombatEngine().getTimeMult().modifyMult(id, 1f / 5f);
                        }
                        if (!inits) {
                            inits = true;
                            ship.setShipTarget(null);
                            RippleDistortion ripple1 = new RippleDistortion(shiploc, new Vector2f());
                            ripple1.setSize(600f);
                            ripple1.setIntensity(100f);
                            ripple1.fadeInSize(0.4f);
                            ripple1.fadeInIntensity(0.3f);
                            ripple1.setFrameRate(120f);
                            DistortionShader.addDistortion(ripple1);
                            Global.getSoundPlayer().playSound("Meng_Starfalling1", 1f, 1f, shiploc, new Vector2f());
                            if (player) {
                                SpriteAPI sprites = Global.getSettings().getSprite("Meng", "Meng_Starring3");
                                MagicRenderPlugin.addBattlespace(sprites, shiploc, new Vector2f(), new Vector2f(), 10, 1f, 3f, 3.5f, CombatEngineLayers.ABOVE_SHIPS_LAYER);
                                sprites.setSize(800f, 800f);
                                sprites.setAlphaMult(0.85f);
                                sprites.setAngle((float) Math.random() * 360f);
                            } else {
                                SpriteAPI sprites = Global.getSettings().getSprite("Meng", "Meng_Starring3");
                                MagicRenderPlugin.addBattlespace(sprites, shiploc, new Vector2f(), new Vector2f(), 10, 0.15f, 0.5f, 0.6f, CombatEngineLayers.ABOVE_SHIPS_LAYER);
                                sprites.setSize(800f, 800f);
                                sprites.setAlphaMult(0.85f);
                                sprites.setAngle((float) Math.random() * 360f);
                            }
                        }
                        float d = Vector2f.sub(shiploc, targetloc, new Vector2f()).length();
                        float d1 = Vector2f.sub(ship.getLocation(), target.getLocation(), new Vector2f()).length();

                        ship.setPhased(true);
                        SpriteAPI sprite2 = Global.getSettings().getSprite("Meng", "Meng_Starfalling1");
                        MagicRenderPlugin.addSingleframe(sprite2, new Vector2f(ship.getLocation().getX() + 30f * (float) Math.random(), ship.getLocation().getY() + 30f * (float) Math.random()), CombatEngineLayers.ABOVE_SHIPS_LAYER);
                        sprite2.setAlphaMult(1f);
                        SpriteAPI sprite3 = Global.getSettings().getSprite("Meng", "Meng_Starring2");
                        if (timer2 <= 1.5f) {
                            sprite3.setAlphaMult(1f);
                        } else if (timer2 <= 2.5f) {
                            sprite3.setAlphaMult(1f);
                        } else {
                            sprite3.setAlphaMult((3f - timer2) * 2f);
                        }
                        sprite3.setSize(500f, 500f);
                        MagicRenderPlugin.addSingleframe(sprite3, ship.getLocation(), CombatEngineLayers.UNDER_SHIPS_LAYER);
                        sprite3.setAngle(timer1 * 60f);
                        target.getLocation().set(targetloc);
                        float arg = arg2;
                        if (d1 <= 100f) {
                            if (!init3) {
                                init3 = true;
                                float[][] grid = target.getArmorGrid().getGrid();
                                float val = 1000000f;
                                for (int x = 0; x < grid.length; x++)
                                    for (int y = 0; y < grid[0].length; y++) {
                                        if (target.getArmorGrid().getArmorValue(x, y) <= val) {
                                            val = target.getArmorGrid().getArmorValue(x, y);
                                        }
                                    }
                                float damage = lastdamage + 50f + target.getFluxTracker().getCurrFlux() * 0.003f + (target.getArmorGrid().getMaxArmorInCell() - val) * 0.10f + (target.getMaxHitpoints() - target.getHitpoints()) * 0.01f;

                                for (float i = 0; i <= d * 1.5f; i = i + 3) {
                                    Vector2f point1 = new Vector2f((float) (shiploc.getX() + (i * Math.sin(Math.toRadians(90f - arg)))), (float) (shiploc.getY() + (i * Math.cos(Math.toRadians(90f - arg)))));
                                    Global.getCombatEngine().applyDamage(target, point1, damage, DamageType.ENERGY, 10f, true, false, ship);
                                }
                                lastdamage = lastdamage + target.getMaxHitpoints() * 0.005f;
                            }
                        }


                        ship.setFacing(arg);
                        sprite2.setAngle(arg - 90f);
                        float range = Math.max(1200f, d * 1.5f);
                        ship.getLocation().set((float) (shiploc.getX() + (-(range) / 4 * (timer2 - 1f - 2f) * (timer2 - 1f - 2f) + (range)) * Math.sin(Math.toRadians(90f - arg))), (float) (shiploc.getY() + (-(range) / 4 * (timer2 - 1f - 2f) * (timer2 - 1f - 2f) + (range)) * Math.cos(Math.toRadians(90f - arg))));
                        SpriteAPI sprites1 = Global.getSettings().getSprite("Meng", "Meng_Starfalling7");

                        MagicTrailPlugin.AddTrailMemberAdvanced(ship, ids + 1,
                                sprites1,
                                ship.getLocation(),
                                10f,
                                40f,
                                ship.getFacing(),
                                0f,
                                0f,
                                200f,
                                1000f,
                                new Color(255, 255, 255, 255),
                                new Color(255, 255, 255, 255),
                                1f,
                                0.02f,
                                0.05f,
                                0.05f,
                                true,
                                256f,
                                10,
                                10f,
                                null,
                                null,
                                CombatEngineLayers.UNDER_SHIPS_LAYER,
                                60f);
                        SpriteAPI sprites = Global.getSettings().getSprite("Meng", "Meng_Starfalling4");

                        MagicTrailPlugin.AddTrailMemberAdvanced(ship, ids,
                                sprites,
                                ship.getLocation(),
                                0f,
                                0f,
                                ship.getFacing(),
                                0f,
                                0f,
                                60f,
                                80f,
                                new Color(255, 255, 255, 255),
                                new Color(255, 255, 255, 255),
                                1f,
                                2f,
                                0.5f,
                                1.0f,
                                true,
                                256f,
                                10,
                                10f,
                                null,
                                null,
                                CombatEngineLayers.UNDER_SHIPS_LAYER,
                                60f);
                    } else {
                        stats.getTimeMult().modifyMult(id, 15f);
                        if (player) {
                            Global.getCombatEngine().getTimeMult().unmodifyMult(id);
                        }
                    }
                } else if (timer2 <= 5.5f) {
                    inits = false;
                    init3 = false;
                    if (!init4) {
                        if (lasttarget != null && !lasttarget.isAlive()) {
                            Vector2f loc = new Vector2f(lasttarget.getLocation().getX(), lasttarget.getLocation().getY());
                            int amo = Meng_hullsizeint.Getsize(lasttarget);
                            Global.getCombatEngine().addLayeredRenderingPlugin(new Meng_StarfallingsPlugin(loc, lasttarget.getMaxHitpoints(), lasttarget.getArmorGrid().getMaxArmorInCell(), amo, ship));
                        } else {
                            if (!init6) {
                                init6 = true;
                                Vector2f newloc = new Vector2f(ship.getLocation().getX(), ship.getLocation().getY());
                                if (player) {
                                    SpriteAPI sprites = Global.getSettings().getSprite("Meng", "Meng_Starring3");
                                    MagicRenderPlugin.addBattlespace(sprites, newloc, new Vector2f(), new Vector2f(), 10, 1f, 3f, 3.5f, CombatEngineLayers.ABOVE_SHIPS_LAYER);
                                    sprites.setSize(800f, 800f);
                                    sprites.setAlphaMult(0.85f);
                                    sprites.setAngle((float) Math.random() * 360f);
                                } else {
                                    SpriteAPI sprites = Global.getSettings().getSprite("Meng", "Meng_Starring3");
                                    MagicRenderPlugin.addBattlespace(sprites, newloc, new Vector2f(), new Vector2f(), 10, 0.15f, 0.5f, 0.6f, CombatEngineLayers.ABOVE_SHIPS_LAYER);
                                    sprites.setSize(800f, 800f);
                                    sprites.setAlphaMult(0.85f);
                                    sprites.setAngle((float) Math.random() * 360f);
                                }
                            }
                        }
                        init4 = true;
                        target = null;
                        arg1 = arg2;
                        loc = new Vector2f(ship.getLocation().getX(), ship.getLocation().getY());
                    }

                    if (lasttarget != null && !lasttarget.isAlive()) {
                        stats.getTimeMult().modifyMult(id, 15f);
                        if (player) {
                            Global.getCombatEngine().getTimeMult().modifyMult(id, 1f / 8f);
                        }
                        if (ship.getShipTarget() != null && ship.getShipTarget().isAlive() && Vector2f.sub(ship.getShipTarget().getLocation(), ship.getLocation(), new Vector2f()).length() - ship.getShieldRadiusEvenIfNoShield() - ship.getShipTarget().getShieldRadiusEvenIfNoShield() <= 2000f) {
                            target = ship.getShipTarget();
                        } else {
                            float weight = 0f;
                            for (ShipAPI ships : AIUtils.getNearbyEnemies(ship, 2000f)) {
                                if (!ships.isFighter() && !ships.isDrone()) {
                                    float brightness;
                                    float length = Vector2f.sub(ship.getMouseTarget(), ships.getLocation(), new Vector2f()).length() - ships.getShieldRadiusEvenIfNoShield();
                                    brightness = Math.max((1000f - length), 0f);

                                    if (brightness >= weight) {
                                        weight = brightness;
                                        target = ships;
                                    }
                                }
                            }
                        }

                        if (target != null) {
                            ship.setFacing(arg1);
                            ship.getLocation().set(loc);
                            SpriteAPI sprite2 = Global.getSettings().getSprite("Meng", "Meng_Starfalling1");
                            MagicRenderPlugin.addSingleframe(sprite2, new Vector2f(ship.getLocation().getX() + 10f * (float) Math.random() * (timer2 - 3f), ship.getLocation().getY() + 10f * (float) Math.random() * (timer2 - 3f)), CombatEngineLayers.ABOVE_SHIPS_LAYER);
                            sprite2.setAlphaMult(1f);
                            sprite2.setAngle(arg1 - 90f);
                            SpriteAPI spritess = Global.getSettings().getSprite("Meng", "Meng_Starfalling6");
                            MagicRenderPlugin.addSingleframe(spritess, new Vector2f(target.getLocation().getX() + target.getSpriteAPI().getWidth() * 0.6f, target.getLocation().getY() + target.getSpriteAPI().getWidth() * 0.6f), CombatEngineLayers.ABOVE_SHIPS_LAYER);
                            spritess.setAngle(timer2 * 360f);
                            spritess.setAlphaMult(1f);
                            spritess.setSize(100f, 100f);
                            if (target.getLocation().getY() > loc.getY()) {
                                arg2 = 90f - (float) Math.toDegrees(Math.atan((target.getLocation().getX() - loc.getX()) / (target.getLocation().getY() - loc.getY())));
                            } else {
                                arg2 = 270f - (float) Math.toDegrees(Math.atan((target.getLocation().getX() - loc.getX()) / (target.getLocation().getY() - loc.getY())));
                            }
                            for (int i = 1; i <= 10f; i++) {
                                if (arg1 >= 360f) {
                                    arg1 = arg1 - 360f;
                                } else if (arg1 <= 0f) {
                                    arg1 = arg1 + 360f;
                                }
                                if (arg2 >= 360f) {
                                    arg2 = arg2 - 360f;
                                } else if (arg2 <= 0f) {
                                    arg2 = arg2 + 360f;
                                }
                            }
                        }
                    }

                } else if (timer2 <= 6f) {
                    lasttarget = null;
                    if (target != null) {
                        stats.getTimeMult().modifyMult(id, 5f);
                        if (player) {
                            Global.getCombatEngine().getTimeMult().modifyMult(id, 1f / 5f);
                        }
                        SpriteAPI sprites = Global.getSettings().getSprite("Meng", "Meng_Starfalling4");

                        MagicTrailPlugin.AddTrailMemberAdvanced(ship, ids,
                                sprites,
                                ship.getLocation(),
                                0f,
                                0f,
                                ship.getFacing(),
                                0f,
                                0f,
                                60f,
                                80f,
                                new Color(255, 255, 255, 255),
                                new Color(255, 255, 255, 255),
                                1f,
                                2f,
                                0.5f,
                                1.0f,
                                true,
                                256f,
                                10,
                                10f,
                                null,
                                null,
                                CombatEngineLayers.UNDER_SHIPS_LAYER,
                                60f);

                        SpriteAPI spritess = Global.getSettings().getSprite("Meng", "Meng_Starfalling6");
                        MagicRenderPlugin.addSingleframe(spritess, new Vector2f(target.getLocation().getX() + target.getSpriteAPI().getWidth() * 0.6f, target.getLocation().getY() + target.getSpriteAPI().getWidth() * 0.6f), CombatEngineLayers.ABOVE_SHIPS_LAYER);
                        spritess.setAngle(timer2 * 360f);
                        spritess.setAlphaMult(1f);
                        spritess.setSize(100f, 100f);
                        ship.getLocation().set(loc);
                        SpriteAPI sprite2 = Global.getSettings().getSprite("Meng", "Meng_Starfalling1");
                        MagicRenderPlugin.addSingleframe(sprite2, new Vector2f(ship.getLocation().getX() + 25f * (float) Math.random(), ship.getLocation().getY() + 25f * (float) Math.random()), CombatEngineLayers.ABOVE_SHIPS_LAYER);
                        sprite2.setAlphaMult(1f);

                        if (Math.abs(arg1 - arg2) <= 180f) {
                            if (arg1 <= arg2) {
                                ship.setFacing(arg1 + 2 * (timer2 - 5.5f) * (arg2 - arg1));
                                sprite2.setAngle(arg1 - 90f + 2 * (timer2 - 5.5f) * (arg2 - arg1));
                            } else {
                                ship.setFacing(arg1 - 2 * (timer2 - 5.5f) * (arg1 - arg2));
                                sprite2.setAngle(arg1 - 90f - 2 * (timer2 - 5.5f) * (arg1 - arg2));
                            }
                        } else {
                            if (arg1 <= arg2) {
                                ship.setFacing(arg1 - 2 * (timer2 - 5.5f) * (360f - arg2 + arg1));
                                sprite2.setAngle(arg1 - 90f - 2 * (timer2 - 5.5f) * (360f - arg2 + arg1));
                            } else {
                                ship.setFacing(arg1 + 2 * (timer2 - 5.5f) * (360f - arg1 + arg2));
                                sprite2.setAngle(arg1 - 90f + 2 * (timer2 - 5.5f) * (360f - arg1 + arg2));
                            }
                        }
                        targetloc = new Vector2f(target.getLocation().getX(), target.getLocation().getY());
                        shiploc = new Vector2f(ship.getLocation().getX(), ship.getLocation().getY());
                    } else {
                        stats.getTimeMult().modifyMult(id, 15f);
                        if (player) {
                            Global.getCombatEngine().getTimeMult().unmodifyMult(id);
                        }
                    }
                } else if (timer2 <= 8f) {
                    if (target != null) {
                        stats.getTimeMult().modifyMult(id, 5f);
                        if (player) {
                            Global.getCombatEngine().getTimeMult().modifyMult(id, 1f / 5f);
                        }
                        if (!inits) {
                            inits = true;
                            lasttarget = target;
                            ship.setShipTarget(null);
                            RippleDistortion ripple1 = new RippleDistortion(shiploc, new Vector2f());
                            ripple1.setSize(600f);
                            ripple1.setIntensity(100f);
                            ripple1.fadeInSize(0.4f);
                            ripple1.fadeInIntensity(0.3f);
                            ripple1.setFrameRate(120f);
                            DistortionShader.addDistortion(ripple1);

                            if (player) {
                                SpriteAPI sprites = Global.getSettings().getSprite("Meng", "Meng_Starring3");
                                MagicRenderPlugin.addBattlespace(sprites, shiploc, new Vector2f(), new Vector2f(), 10, 1f, 3f, 3.5f, CombatEngineLayers.ABOVE_SHIPS_LAYER);
                                sprites.setSize(1100f, 1100f);
                                sprites.setAlphaMult(0.85f);
                                sprites.setAngle((float) Math.random() * 360f);
                            } else {
                                SpriteAPI sprites = Global.getSettings().getSprite("Meng", "Meng_Starring3");
                                MagicRenderPlugin.addBattlespace(sprites, shiploc, new Vector2f(), new Vector2f(), 10, 0.15f, 0.5f, 0.6f, CombatEngineLayers.ABOVE_SHIPS_LAYER);
                                sprites.setSize(1100f, 1100f);
                                sprites.setAlphaMult(0.85f);
                                sprites.setAngle((float) Math.random() * 360f);
                            }
                        }
                        float d = Vector2f.sub(shiploc, targetloc, new Vector2f()).length();
                        float d1 = Vector2f.sub(ship.getLocation(), target.getLocation(), new Vector2f()).length();

                        ship.setPhased(true);
                        SpriteAPI sprite2 = Global.getSettings().getSprite("Meng", "Meng_Starfalling1");
                        MagicRenderPlugin.addSingleframe(sprite2, new Vector2f(ship.getLocation().getX() + 20f * (float) Math.random(), ship.getLocation().getY() + 20f * (float) Math.random()), CombatEngineLayers.ABOVE_SHIPS_LAYER);
                        sprite2.setAlphaMult(1f);
                        SpriteAPI sprite3 = Global.getSettings().getSprite("Meng", "Meng_Starring2");
                        if (timer2 <= 6.5f) {
                            sprite3.setAlphaMult((timer2 - 6f) * 2f);
                        } else if (timer2 <= 7.5f) {
                            sprite3.setAlphaMult(1f);
                        } else {
                            sprite3.setAlphaMult((8f - timer2) * 2f);
                        }
                        sprite3.setSize(500f, 500f);
                        MagicRenderPlugin.addSingleframe(sprite3, ship.getLocation(), CombatEngineLayers.UNDER_SHIPS_LAYER);
                        sprite3.setAngle(timer1 * 60f);
                        target.getLocation().set(targetloc);
                        float arg = arg2;


                        if (d1 <= 100f) {
                            if (!init3) {
                                init3 = true;
                                float[][] grid = target.getArmorGrid().getGrid();
                                float val = 1000000f;
                                for (int x = 0; x < grid.length; x++)
                                    for (int y = 0; y < grid[0].length; y++) {
                                        if (target.getArmorGrid().getArmorValue(x, y) <= val) {
                                            val = target.getArmorGrid().getArmorValue(x, y);
                                        }
                                    }
                                float damage = lastdamage + 50f + target.getFluxTracker().getCurrFlux() * 0.003f + (target.getArmorGrid().getMaxArmorInCell() - val) * 0.10f + (target.getMaxHitpoints() - target.getHitpoints()) * 0.01f;

                                for (float i = 0; i <= d * 1.5f; i = i + 3) {
                                    Vector2f point1 = new Vector2f((float) (shiploc.getX() + (i * Math.sin(Math.toRadians(90f - arg)))), (float) (shiploc.getY() + (i * Math.cos(Math.toRadians(90f - arg)))));
                                    Global.getCombatEngine().applyDamage(target, point1, damage, DamageType.ENERGY, 10f, true, false, ship);
                                }
                                lastdamage = lastdamage + target.getMaxHitpoints() * 0.006f;
                            }
                        }


                        ship.setFacing(arg);
                        sprite2.setAngle(arg - 90f);
                        float range = Math.max(1200f, d * 1.5f);
                        ship.getLocation().set((float) (shiploc.getX() + (-(range) / 4 * (timer2 - 6f - 2f) * (timer2 - 6f - 2f) + (range)) * Math.sin(Math.toRadians(90f - arg))), (float) (shiploc.getY() + (-(range) / 4 * (timer2 - 6f - 2f) * (timer2 - 6f - 2f) + (range)) * Math.cos(Math.toRadians(90f - arg))));
                        SpriteAPI sprites1 = Global.getSettings().getSprite("Meng", "Meng_Starfalling7");

                        MagicTrailPlugin.AddTrailMemberAdvanced(ship, ids + 1,
                                sprites1,
                                ship.getLocation(),
                                10f,
                                40f,
                                ship.getFacing(),
                                0f,
                                0f,
                                200f,
                                1000f,
                                new Color(255, 255, 255, 255),
                                new Color(255, 255, 255, 255),
                                1f,
                                0.02f,
                                0.05f,
                                0.05f,
                                true,
                                256f,
                                10,
                                10f,
                                null,
                                null,
                                CombatEngineLayers.UNDER_SHIPS_LAYER,
                                60f);
                        SpriteAPI sprites = Global.getSettings().getSprite("Meng", "Meng_Starfalling4");

                        MagicTrailPlugin.AddTrailMemberAdvanced(ship, ids,
                                sprites,
                                ship.getLocation(),
                                0f,
                                0f,
                                ship.getFacing(),
                                0f,
                                0f,
                                60f,
                                80f,
                                new Color(255, 255, 255, 255),
                                new Color(255, 255, 255, 255),
                                1f,
                                2f,
                                0.5f,
                                1.0f,
                                true,
                                256f,
                                10,
                                10f,
                                null,
                                null,
                                CombatEngineLayers.UNDER_SHIPS_LAYER,
                                60f);
                    } else {
                        stats.getTimeMult().modifyMult(id, 15f);
                        if (player) {
                            Global.getCombatEngine().getTimeMult().unmodifyMult(id);
                        }
                    }
                    init4 = false;
                } else if (timer2 <= 10.5f) {
                    inits = false;
                    init3 = false;
                    if (!init4) {
                        if (lasttarget != null && !lasttarget.isAlive()) {
                            Vector2f loc = new Vector2f(lasttarget.getLocation().getX(), lasttarget.getLocation().getY());
                            int amo = Meng_hullsizeint.Getsize(lasttarget);
                            Global.getCombatEngine().addLayeredRenderingPlugin(new Meng_StarfallingsPlugin(loc, lasttarget.getMaxHitpoints(), lasttarget.getArmorGrid().getMaxArmorInCell(), amo, ship));
                        } else {
                            if (!init6) {
                                init6 = true;
                                Vector2f newloc = new Vector2f(ship.getLocation().getX(), ship.getLocation().getY());
                                if (player) {
                                    SpriteAPI sprites = Global.getSettings().getSprite("Meng", "Meng_Starring3");
                                    MagicRenderPlugin.addBattlespace(sprites, newloc, new Vector2f(), new Vector2f(), 10, 1f, 3f, 3.5f, CombatEngineLayers.ABOVE_SHIPS_LAYER);
                                    sprites.setSize(800f, 800f);
                                    sprites.setAlphaMult(0.85f);
                                    sprites.setAngle((float) Math.random() * 360f);
                                } else {
                                    SpriteAPI sprites = Global.getSettings().getSprite("Meng", "Meng_Starring3");
                                    MagicRenderPlugin.addBattlespace(sprites, newloc, new Vector2f(), new Vector2f(), 10, 0.15f, 0.5f, 0.6f, CombatEngineLayers.ABOVE_SHIPS_LAYER);
                                    sprites.setSize(800f, 800f);
                                    sprites.setAlphaMult(0.85f);
                                    sprites.setAngle((float) Math.random() * 360f);
                                }
                            }
                        }
                        init4 = true;
                        target = null;
                        arg1 = arg2;
                        loc = new Vector2f(ship.getLocation().getX(), ship.getLocation().getY());
                    }
                    if (lasttarget != null && !lasttarget.isAlive()) {
                        stats.getTimeMult().modifyMult(id, 15f);

                        if (player) {
                            Global.getCombatEngine().getTimeMult().modifyMult(id, 1f / 8f);
                        }
                        if (ship.getShipTarget() != null && ship.getShipTarget().isAlive() && Vector2f.sub(ship.getShipTarget().getLocation(), ship.getLocation(), new Vector2f()).length() - ship.getShieldRadiusEvenIfNoShield() - ship.getShipTarget().getShieldRadiusEvenIfNoShield() <= 2000f) {
                            target = ship.getShipTarget();
                        } else {
                            float weight = 0f;
                            for (ShipAPI ships : AIUtils.getNearbyEnemies(ship, 2000f)) {
                                if (!ships.isFighter() && !ships.isDrone()) {
                                    float brightness;
                                    float length = Vector2f.sub(ship.getMouseTarget(), ships.getLocation(), new Vector2f()).length() - ships.getShieldRadiusEvenIfNoShield();
                                    brightness = Math.max((1000f - length), 0f);

                                    if (brightness >= weight) {
                                        weight = brightness;
                                        target = ships;
                                    }
                                }
                            }
                        }

                        if (target != null) {
                            ship.setFacing(arg1);
                            ship.getLocation().set(loc);
                            SpriteAPI sprite2 = Global.getSettings().getSprite("Meng", "Meng_Starfalling1");
                            MagicRenderPlugin.addSingleframe(sprite2, new Vector2f(ship.getLocation().getX() + 20f * (float) Math.random(), ship.getLocation().getY() + 20f * (float) Math.random()), CombatEngineLayers.ABOVE_SHIPS_LAYER);
                            sprite2.setAlphaMult(1f);
                            sprite2.setAngle(arg1 - 90f);
                            SpriteAPI spritess = Global.getSettings().getSprite("Meng", "Meng_Starfalling6");
                            MagicRenderPlugin.addSingleframe(spritess, new Vector2f(target.getLocation().getX() + target.getSpriteAPI().getWidth() * 0.6f, target.getLocation().getY() + target.getSpriteAPI().getWidth() * 0.6f), CombatEngineLayers.ABOVE_SHIPS_LAYER);
                            spritess.setAngle(timer2 * 360f);
                            spritess.setAlphaMult(1f);
                            spritess.setSize(100f, 100f);
                            if (target.getLocation().getY() > loc.getY()) {
                                arg2 = 90f - (float) Math.toDegrees(Math.atan((target.getLocation().getX() - loc.getX()) / (target.getLocation().getY() - loc.getY())));
                            } else {
                                arg2 = 270f - (float) Math.toDegrees(Math.atan((target.getLocation().getX() - loc.getX()) / (target.getLocation().getY() - loc.getY())));
                            }
                            for (int i = 1; i <= 10f; i++) {
                                if (arg1 >= 360f) {
                                    arg1 = arg1 - 360f;
                                } else if (arg1 <= 0f) {
                                    arg1 = arg1 + 360f;
                                }
                                if (arg2 >= 360f) {
                                    arg2 = arg2 - 360f;
                                } else if (arg2 <= 0f) {
                                    arg2 = arg2 + 360f;
                                }
                            }
                        }
                    }

                } else if (timer2 <= 11f) {
                    lasttarget = null;
                    if (target != null) {
                        stats.getTimeMult().modifyMult(id, 5f);
                        if (player) {
                            Global.getCombatEngine().getTimeMult().modifyMult(id, 1f / 5f);
                        }
                        SpriteAPI sprites = Global.getSettings().getSprite("Meng", "Meng_Starfalling4");

                        MagicTrailPlugin.AddTrailMemberAdvanced(ship, ids,
                                sprites,
                                ship.getLocation(),
                                0f,
                                0f,
                                ship.getFacing(),
                                0f,
                                0f,
                                60f,
                                80f,
                                new Color(255, 255, 255, 255),
                                new Color(255, 255, 255, 255),
                                1f,
                                2f,
                                0.5f,
                                1.0f,
                                true,
                                256f,
                                10,
                                10f,
                                null,
                                null,
                                CombatEngineLayers.UNDER_SHIPS_LAYER,
                                60f);

                        SpriteAPI spritess = Global.getSettings().getSprite("Meng", "Meng_Starfalling6");
                        MagicRenderPlugin.addSingleframe(spritess, new Vector2f(target.getLocation().getX() + target.getSpriteAPI().getWidth() * 0.6f, target.getLocation().getY() + target.getSpriteAPI().getWidth() * 0.6f), CombatEngineLayers.ABOVE_SHIPS_LAYER);
                        spritess.setAngle(timer2 * 360f);
                        spritess.setAlphaMult(1f);
                        spritess.setSize(100f, 100f);
                        ship.getLocation().set(loc);
                        SpriteAPI sprite2 = Global.getSettings().getSprite("Meng", "Meng_Starfalling1");
                        MagicRenderPlugin.addSingleframe(sprite2, new Vector2f(ship.getLocation().getX() + 25f * (float) Math.random(), ship.getLocation().getY() + 25f * (float) Math.random()), CombatEngineLayers.ABOVE_SHIPS_LAYER);
                        sprite2.setAlphaMult(1f);

                        if (Math.abs(arg1 - arg2) <= 180f) {
                            if (arg1 <= arg2) {
                                ship.setFacing(arg1 + 2 * (timer2 - 10.5f) * (arg2 - arg1));
                                sprite2.setAngle(arg1 - 90f + 2 * (timer2 - 10.5f) * (arg2 - arg1));
                            } else {
                                ship.setFacing(arg1 - 2 * (timer2 - 10.5f) * (arg1 - arg2));
                                sprite2.setAngle(arg1 - 90f - 2 * (timer2 - 10.5f) * (arg1 - arg2));
                            }
                        } else {
                            if (arg1 <= arg2) {
                                ship.setFacing(arg1 - 2 * (timer2 - 10.5f) * (360f - arg2 + arg1));
                                sprite2.setAngle(arg1 - 90f - 2 * (timer2 - 10.5f) * (360f - arg2 + arg1));
                            } else {
                                ship.setFacing(arg1 + 2 * (timer2 - 10.5f) * (360f - arg1 + arg2));
                                sprite2.setAngle(arg1 - 90f + 2 * (timer2 - 10.5f) * (360f - arg1 + arg2));
                            }
                        }
                        targetloc = new Vector2f(target.getLocation().getX(), target.getLocation().getY());
                        shiploc = new Vector2f(ship.getLocation().getX(), ship.getLocation().getY());
                    } else {
                        stats.getTimeMult().modifyMult(id, 15f);
                        if (player) {
                            Global.getCombatEngine().getTimeMult().unmodifyMult(id);
                        }
                    }
                } else if (timer2 <= 13f) {
                    if (target != null) {
                        stats.getTimeMult().modifyMult(id, 5f);
                        if (player) {
                            Global.getCombatEngine().getTimeMult().modifyMult(id, 1f / 5f);
                        }
                        if (!inits) {
                            inits = true;
                            lasttarget = target;
                            ship.setShipTarget(null);
                            RippleDistortion ripple1 = new RippleDistortion(shiploc, new Vector2f());
                            ripple1.setSize(600f);
                            ripple1.setIntensity(100f);
                            ripple1.fadeInSize(0.4f);
                            ripple1.fadeInIntensity(0.3f);
                            ripple1.setFrameRate(120f);
                            DistortionShader.addDistortion(ripple1);
                            if (player) {
                                Global.getSoundPlayer().playSound("Meng_Starfalling1", 1f, 1f, shiploc, new Vector2f());
                                SpriteAPI sprites = Global.getSettings().getSprite("Meng", "Meng_Starring3");
                                MagicRenderPlugin.addBattlespace(sprites, shiploc, new Vector2f(), new Vector2f(), 10, 1f, 3f, 3.5f, CombatEngineLayers.ABOVE_SHIPS_LAYER);
                                sprites.setSize(950f, 950f);
                                sprites.setAlphaMult(0.85f);
                                sprites.setAngle((float) Math.random() * 360f);
                            } else {
                                SpriteAPI sprites = Global.getSettings().getSprite("Meng", "Meng_Starring3");
                                MagicRenderPlugin.addBattlespace(sprites, shiploc, new Vector2f(), new Vector2f(), 10, 0.15f, 0.5f, 0.6f, CombatEngineLayers.ABOVE_SHIPS_LAYER);
                                sprites.setSize(950f, 950f);
                                sprites.setAlphaMult(0.85f);
                                sprites.setAngle((float) Math.random() * 360f);
                            }
                        }
                        float d = Vector2f.sub(shiploc, targetloc, new Vector2f()).length();
                        float d1 = Vector2f.sub(ship.getLocation(), target.getLocation(), new Vector2f()).length();

                        ship.setPhased(true);
                        SpriteAPI sprite2 = Global.getSettings().getSprite("Meng", "Meng_Starfalling1");
                        MagicRenderPlugin.addSingleframe(sprite2, new Vector2f(ship.getLocation().getX() + 30f * (float) Math.random(), ship.getLocation().getY() + 30f * (float) Math.random()), CombatEngineLayers.ABOVE_SHIPS_LAYER);
                        sprite2.setAlphaMult(1f);
                        SpriteAPI sprite3 = Global.getSettings().getSprite("Meng", "Meng_Starring2");
                        if (timer2 <= 11.5f) {
                            sprite3.setAlphaMult((timer2 - 11f) * 2f);
                        } else if (timer2 <= 12.5f) {
                            sprite3.setAlphaMult(1f);
                        } else {
                            sprite3.setAlphaMult((13f - timer2) * 2f);
                        }
                        sprite3.setSize(500f, 500f);
                        MagicRenderPlugin.addSingleframe(sprite3, ship.getLocation(), CombatEngineLayers.UNDER_SHIPS_LAYER);
                        sprite3.setAngle(timer1 * 60f);
                        target.getLocation().set(targetloc);
                        float arg = arg2;


                        if (d1 <= 100f) {
                            if (!init3) {
                                init3 = true;
                                float[][] grid = target.getArmorGrid().getGrid();
                                float val = 1000000f;
                                for (int x = 0; x < grid.length; x++)
                                    for (int y = 0; y < grid[0].length; y++) {
                                        if (target.getArmorGrid().getArmorValue(x, y) <= val) {
                                            val = target.getArmorGrid().getArmorValue(x, y);
                                        }
                                    }
                                float damage = lastdamage + 50f + target.getFluxTracker().getCurrFlux() * 0.003f + (target.getArmorGrid().getMaxArmorInCell() - val) * 0.10f + (target.getMaxHitpoints() - target.getHitpoints()) * 0.01f;

                                for (float i = 0; i <= d * 1.5f; i = i + 3) {
                                    Vector2f point1 = new Vector2f((float) (shiploc.getX() + (i * Math.sin(Math.toRadians(90f - arg)))), (float) (shiploc.getY() + (i * Math.cos(Math.toRadians(90f - arg)))));
                                    Global.getCombatEngine().applyDamage(target, point1, damage, DamageType.ENERGY, 10f, true, false, ship);
                                }
                                lastdamage = lastdamage + target.getMaxHitpoints() * 0.007f;
                            }
                        }


                        ship.setFacing(arg);
                        sprite2.setAngle(arg - 90f);
                        float range = Math.max(1200f, d * 1.5f);
                        ship.getLocation().set((float) (shiploc.getX() + (-(range) / 4 * (timer2 - 11f - 2f) * (timer2 - 11f - 2f) + (range)) * Math.sin(Math.toRadians(90f - arg))), (float) (shiploc.getY() + (-(range) / 4 * (timer2 - 11f - 2f) * (timer2 - 11f - 2f) + (range)) * Math.cos(Math.toRadians(90f - arg))));
                        SpriteAPI sprites1 = Global.getSettings().getSprite("Meng", "Meng_Starfalling7");

                        MagicTrailPlugin.AddTrailMemberAdvanced(ship, ids + 1,
                                sprites1,
                                ship.getLocation(),
                                10f,
                                40f,
                                ship.getFacing(),
                                0f,
                                0f,
                                200f,
                                1000f,
                                new Color(255, 255, 255, 255),
                                new Color(255, 255, 255, 255),
                                1f,
                                0.02f,
                                0.05f,
                                0.05f,
                                true,
                                256f,
                                10,
                                10f,
                                null,
                                null,
                                CombatEngineLayers.UNDER_SHIPS_LAYER,
                                60f);
                        SpriteAPI sprites = Global.getSettings().getSprite("Meng", "Meng_Starfalling4");

                        MagicTrailPlugin.AddTrailMemberAdvanced(ship, ids,
                                sprites,
                                ship.getLocation(),
                                0f,
                                0f,
                                ship.getFacing(),
                                0f,
                                0f,
                                60f,
                                80f,
                                new Color(255, 255, 255, 255),
                                new Color(255, 255, 255, 255),
                                1f,
                                2f,
                                0.5f,
                                1.0f,
                                true,
                                256f,
                                10,
                                10f,
                                null,
                                null,
                                CombatEngineLayers.UNDER_SHIPS_LAYER,
                                60f);
                    } else {
                        stats.getTimeMult().modifyMult(id, 15f);
                        if (player) {
                            Global.getCombatEngine().getTimeMult().unmodifyMult(id);
                        }
                    }
                    init4 = false;
                } else if (timer2 <= 15.5f) {
                    inits = false;
                    init3 = false;
                    if (!init4) {
                        if (lasttarget != null && !lasttarget.isAlive()) {
                            Vector2f loc = new Vector2f(lasttarget.getLocation().getX(), lasttarget.getLocation().getY());
                            int amo = Meng_hullsizeint.Getsize(lasttarget);
                            Global.getCombatEngine().addLayeredRenderingPlugin(new Meng_StarfallingsPlugin(loc, lasttarget.getMaxHitpoints(), lasttarget.getArmorGrid().getMaxArmorInCell(), amo, ship));
                        } else {
                            if (!init6) {
                                init6 = true;
                                Vector2f newloc = new Vector2f(ship.getLocation().getX(), ship.getLocation().getY());
                                if (player) {
                                    SpriteAPI sprites = Global.getSettings().getSprite("Meng", "Meng_Starring3");
                                    MagicRenderPlugin.addBattlespace(sprites, newloc, new Vector2f(), new Vector2f(), 10, 1f, 3f, 3.5f, CombatEngineLayers.ABOVE_SHIPS_LAYER);
                                    sprites.setSize(800f, 800f);
                                    sprites.setAlphaMult(0.85f);
                                    sprites.setAngle((float) Math.random() * 360f);
                                } else {
                                    SpriteAPI sprites = Global.getSettings().getSprite("Meng", "Meng_Starring3");
                                    MagicRenderPlugin.addBattlespace(sprites, newloc, new Vector2f(), new Vector2f(), 10, 0.15f, 0.5f, 0.6f, CombatEngineLayers.ABOVE_SHIPS_LAYER);
                                    sprites.setSize(800f, 800f);
                                    sprites.setAlphaMult(0.85f);
                                    sprites.setAngle((float) Math.random() * 360f);
                                }
                            }
                        }
                        init4 = true;
                        target = null;
                        arg1 = arg2;
                        loc = new Vector2f(ship.getLocation().getX(), ship.getLocation().getY());
                    }
                    if (lasttarget != null && !lasttarget.isAlive()) {
                        stats.getTimeMult().modifyMult(id, 15f);
                        if (player) {
                            Global.getCombatEngine().getTimeMult().modifyMult(id, 1f / 8f);
                        }
                        if (ship.getShipTarget() != null && ship.getShipTarget().isAlive() && Vector2f.sub(ship.getShipTarget().getLocation(), ship.getLocation(), new Vector2f()).length() - ship.getShieldRadiusEvenIfNoShield() - ship.getShipTarget().getShieldRadiusEvenIfNoShield() <= 2000f) {
                            target = ship.getShipTarget();
                        } else {
                            float weight = 0f;
                            for (ShipAPI ships : AIUtils.getNearbyEnemies(ship, 2000f)) {
                                if (!ships.isFighter() && !ships.isDrone()) {
                                    float brightness;
                                    float length = Vector2f.sub(ship.getMouseTarget(), ships.getLocation(), new Vector2f()).length() - ships.getShieldRadiusEvenIfNoShield();
                                    brightness = Math.max((1000f - length), 0f);
                                    if (brightness >= weight) {
                                        weight = brightness;
                                        target = ships;
                                    }
                                }
                            }
                        }

                        if (target != null) {
                            ship.setFacing(arg1);
                            ship.getLocation().set(loc);
                            SpriteAPI sprite2 = Global.getSettings().getSprite("Meng", "Meng_Starfalling1");
                            MagicRenderPlugin.addSingleframe(sprite2, new Vector2f(ship.getLocation().getX() + 20f * (float) Math.random(), ship.getLocation().getY() + 20f * (float) Math.random()), CombatEngineLayers.ABOVE_SHIPS_LAYER);
                            sprite2.setAlphaMult(1f);
                            sprite2.setAngle(arg1 - 90f);
                            SpriteAPI spritess = Global.getSettings().getSprite("Meng", "Meng_Starfalling6");
                            MagicRenderPlugin.addSingleframe(spritess, new Vector2f(target.getLocation().getX() + target.getSpriteAPI().getWidth() * 0.6f, target.getLocation().getY() + target.getSpriteAPI().getWidth() * 0.6f), CombatEngineLayers.ABOVE_SHIPS_LAYER);
                            spritess.setAngle(timer2 * 360f);
                            spritess.setAlphaMult(1f);
                            spritess.setSize(100f, 100f);
                            if (target.getLocation().getY() > loc.getY()) {
                                arg2 = 90f - (float) Math.toDegrees(Math.atan((target.getLocation().getX() - loc.getX()) / (target.getLocation().getY() - loc.getY())));
                            } else {
                                arg2 = 270f - (float) Math.toDegrees(Math.atan((target.getLocation().getX() - loc.getX()) / (target.getLocation().getY() - loc.getY())));
                            }
                            for (int i = 1; i <= 10f; i++) {
                                if (arg1 >= 360f) {
                                    arg1 = arg1 - 360f;
                                } else if (arg1 <= 0f) {
                                    arg1 = arg1 + 360f;
                                }
                                if (arg2 >= 360f) {
                                    arg2 = arg2 - 360f;
                                } else if (arg2 <= 0f) {
                                    arg2 = arg2 + 360f;
                                }
                            }
                        }
                    }

                } else if (timer2 <= 16f) {
                    if (target != null) {
                        stats.getTimeMult().modifyMult(id, 5f);
                        if (player) {
                            Global.getCombatEngine().getTimeMult().modifyMult(id, 1f / 5f);
                        }
                        SpriteAPI sprites = Global.getSettings().getSprite("Meng", "Meng_Starfalling4");

                        MagicTrailPlugin.AddTrailMemberAdvanced(ship, ids,
                                sprites,
                                ship.getLocation(),
                                0f,
                                0f,
                                ship.getFacing(),
                                0f,
                                0f,
                                60f,
                                80f,
                                new Color(255, 255, 255, 255),
                                new Color(255, 255, 255, 255),
                                1f,
                                2f,
                                0.5f,
                                1.0f,
                                true,
                                256f,
                                10,
                                10f,
                                null,
                                null,
                                CombatEngineLayers.UNDER_SHIPS_LAYER,
                                60f);

                        SpriteAPI spritess = Global.getSettings().getSprite("Meng", "Meng_Starfalling6");
                        MagicRenderPlugin.addSingleframe(spritess, new Vector2f(target.getLocation().getX() + target.getSpriteAPI().getWidth() * 0.6f, target.getLocation().getY() + target.getSpriteAPI().getWidth() * 0.6f), CombatEngineLayers.ABOVE_SHIPS_LAYER);
                        spritess.setAngle(timer2 * 360f);
                        spritess.setAlphaMult(1f);
                        spritess.setSize(100f, 100f);
                        ship.getLocation().set(loc);
                        SpriteAPI sprite2 = Global.getSettings().getSprite("Meng", "Meng_Starfalling1");
                        MagicRenderPlugin.addSingleframe(sprite2, new Vector2f(ship.getLocation().getX() + 25f * (float) Math.random(), ship.getLocation().getY() + 25f * (float) Math.random()), CombatEngineLayers.ABOVE_SHIPS_LAYER);
                        sprite2.setAlphaMult(1f);

                        if (Math.abs(arg1 - arg2) <= 180f) {
                            if (arg1 <= arg2) {
                                ship.setFacing(arg1 + 2 * (timer2 - 15.5f) * (arg2 - arg1));
                                sprite2.setAngle(arg1 - 90f + 2 * (timer2 - 15.5f) * (arg2 - arg1));
                            } else {
                                ship.setFacing(arg1 - 2 * (timer2 - 15.5f) * (arg1 - arg2));
                                sprite2.setAngle(arg1 - 90f - 2 * (timer2 - 15.5f) * (arg1 - arg2));
                            }
                        } else {
                            if (arg1 <= arg2) {
                                ship.setFacing(arg1 - 2 * (timer2 - 15.5f) * (360f - arg2 + arg1));
                                sprite2.setAngle(arg1 - 90f - 2 * (timer2 - 15.5f) * (360f - arg2 + arg1));
                            } else {
                                ship.setFacing(arg1 + 2 * (timer2 - 15.5f) * (360f - arg1 + arg2));
                                sprite2.setAngle(arg1 - 90f + 2 * (timer2 - 15.5f) * (360f - arg1 + arg2));
                            }
                        }
                        targetloc = new Vector2f(target.getLocation().getX(), target.getLocation().getY());
                        shiploc = new Vector2f(ship.getLocation().getX(), ship.getLocation().getY());
                    } else {
                        stats.getTimeMult().modifyMult(id, 5f);
                        if (player) {
                            Global.getCombatEngine().getTimeMult().unmodifyMult(id);
                        }
                    }
                } else if (timer2 <= 18f) {
                    if (target != null) {
                        stats.getTimeMult().modifyMult(id, 5f);
                        if (player) {
                            Global.getCombatEngine().getTimeMult().modifyMult(id, 1f / 5f);
                        }
                        if (!inits) {
                            inits = true;
                            lasttarget = target;
                            ship.setShipTarget(null);
                            RippleDistortion ripple1 = new RippleDistortion(shiploc, new Vector2f());
                            ripple1.setSize(600f);
                            ripple1.setIntensity(100f);
                            ripple1.fadeInSize(0.4f);
                            ripple1.fadeInIntensity(0.3f);
                            ripple1.setFrameRate(120f);
                            DistortionShader.addDistortion(ripple1);
                            if (player) {
                                Global.getSoundPlayer().playSound("Meng_Starfalling1", 1f, 1f, shiploc, new Vector2f());
                                SpriteAPI sprites = Global.getSettings().getSprite("Meng", "Meng_Starring3");
                                MagicRenderPlugin.addBattlespace(sprites, shiploc, new Vector2f(), new Vector2f(), 10, 1f, 3f, 3.5f, CombatEngineLayers.ABOVE_SHIPS_LAYER);
                                sprites.setSize(1150f, 1150f);
                                sprites.setAlphaMult(0.85f);
                                sprites.setAngle((float) Math.random() * 360f);
                            } else {
                                SpriteAPI sprites = Global.getSettings().getSprite("Meng", "Meng_Starring3");
                                MagicRenderPlugin.addBattlespace(sprites, shiploc, new Vector2f(), new Vector2f(), 10, 0.15f, 0.5f, 0.6f, CombatEngineLayers.ABOVE_SHIPS_LAYER);
                                sprites.setSize(1150f, 1150f);
                                sprites.setAlphaMult(0.85f);
                                sprites.setAngle((float) Math.random() * 360f);
                            }
                        }
                        float d = Vector2f.sub(shiploc, targetloc, new Vector2f()).length();
                        float d1 = Vector2f.sub(ship.getLocation(), target.getLocation(), new Vector2f()).length();

                        ship.setPhased(true);
                        SpriteAPI sprite2 = Global.getSettings().getSprite("Meng", "Meng_Starfalling1");
                        MagicRenderPlugin.addSingleframe(sprite2, new Vector2f(ship.getLocation().getX() + 30f * (float) Math.random(), ship.getLocation().getY() + 30f * (float) Math.random()), CombatEngineLayers.ABOVE_SHIPS_LAYER);
                        sprite2.setAlphaMult(Math.min(1f, 18f - timer2));
                        SpriteAPI sprite3 = Global.getSettings().getSprite("Meng", "Meng_Starring2");
                        if (timer2 <= 16.5f) {
                            sprite3.setAlphaMult((timer2 - 16f) * 2f);
                        } else if (timer2 <= 17.5f) {
                            sprite3.setAlphaMult(1f);
                        } else {
                            sprite3.setAlphaMult((18f - timer2) * 2f);
                        }
                        sprite3.setSize(500f, 500f);
                        MagicRenderPlugin.addSingleframe(sprite3, ship.getLocation(), CombatEngineLayers.UNDER_SHIPS_LAYER);
                        sprite3.setAngle(timer1 * 60f);
                        target.getLocation().set(targetloc);
                        float arg = arg2;


                        if (d1 <= 100f) {
                            if (!init3) {
                                init3 = true;
                                float[][] grid = target.getArmorGrid().getGrid();
                                float val = 1000000f;
                                for (int x = 0; x < grid.length; x++)
                                    for (int y = 0; y < grid[0].length; y++) {
                                        if (target.getArmorGrid().getArmorValue(x, y) <= val) {
                                            val = target.getArmorGrid().getArmorValue(x, y);
                                        }
                                    }
                                float damage = lastdamage + 50f + target.getFluxTracker().getCurrFlux() * 0.003f + (target.getArmorGrid().getMaxArmorInCell() - val) * 0.10f + (target.getMaxHitpoints() - target.getHitpoints()) * 0.01f;

                                for (float i = 0; i <= d * 1.5f; i = i + 3) {
                                    Vector2f point1 = new Vector2f((float) (shiploc.getX() + (i * Math.sin(Math.toRadians(90f - arg)))), (float) (shiploc.getY() + (i * Math.cos(Math.toRadians(90f - arg)))));
                                    Global.getCombatEngine().applyDamage(target, point1, damage, DamageType.ENERGY, 10f, true, false, ship);
                                }
                                lastdamage = lastdamage + target.getMaxHitpoints() * 0.007f;
                            }
                        }


                        ship.setFacing(arg);
                        sprite2.setAngle(arg - 90f);
                        float range = Math.max(1200f, d * 1.5f);
                        ship.getLocation().set((float) (shiploc.getX() + (-(range) / 4 * (timer2 - 16f - 2f) * (timer2 - 16f - 2f) + (range)) * Math.sin(Math.toRadians(90f - arg))), (float) (shiploc.getY() + (-(range) / 4 * (timer2 - 16f - 2f) * (timer2 - 16f - 2f) + (range)) * Math.cos(Math.toRadians(90f - arg))));
                        SpriteAPI sprites1 = Global.getSettings().getSprite("Meng", "Meng_Starfalling7");

                        MagicTrailPlugin.AddTrailMemberAdvanced(ship, ids + 1,
                                sprites1,
                                ship.getLocation(),
                                10f,
                                40f,
                                ship.getFacing(),
                                0f,
                                0f,
                                200f,
                                1000f,
                                new Color(255, 255, 255, 255),
                                new Color(255, 255, 255, 255),
                                1f,
                                0.02f,
                                0.05f,
                                0.05f,
                                true,
                                256f,
                                10,
                                10f,
                                null,
                                null,
                                CombatEngineLayers.UNDER_SHIPS_LAYER,
                                60f);
                        SpriteAPI sprites = Global.getSettings().getSprite("Meng", "Meng_Starfalling4");

                        MagicTrailPlugin.AddTrailMemberAdvanced(ship, ids,
                                sprites,
                                ship.getLocation(),
                                0f,
                                0f,
                                ship.getFacing(),
                                0f,
                                0f,
                                60f,
                                80f,
                                new Color(255, 255, 255, 255),
                                new Color(255, 255, 255, 255),
                                1f,
                                2f,
                                0.5f,
                                1.0f,
                                true,
                                256f,
                                10,
                                10f,
                                null,
                                null,
                                CombatEngineLayers.UNDER_SHIPS_LAYER,
                                60f);
                    } else {
                        stats.getTimeMult().modifyMult(id, 5f);
                        if (player) {
                            Global.getCombatEngine().getTimeMult().unmodifyMult(id);
                        }
                    }
                    endloc = new Vector2f(ship.getLocation().getX(), ship.getLocation().getY());
                }

            }
        } else {
            ship.setPhased(true);
            ship.setHoldFire(true);
            timer3 += Global.getCombatEngine().getElapsedInLastFrame();
            stats.getTimeMult().modifyMult(id, 5f);
            if (player) {
                Global.getCombatEngine().getTimeMult().modifyMult(id, 1f / 5f);
            }
            if (!init5) {
                if (!init6) {
                    init6 = true;
                    if (player) {
                        SpriteAPI sprites = Global.getSettings().getSprite("Meng", "Meng_Starring3");
                        MagicRenderPlugin.addBattlespace(sprites, endloc, new Vector2f(), new Vector2f(), 10, 1f, 3f, 3.5f, CombatEngineLayers.ABOVE_SHIPS_LAYER);
                        sprites.setSize(800f, 800f);
                        sprites.setAlphaMult(0.85f);
                        sprites.setAngle((float) Math.random() * 360f);
                    } else {
                        SpriteAPI sprites = Global.getSettings().getSprite("Meng", "Meng_Starring3");
                        MagicRenderPlugin.addBattlespace(sprites, endloc, new Vector2f(), new Vector2f(), 10, 0.15f, 0.5f, 0.6f, CombatEngineLayers.ABOVE_SHIPS_LAYER);
                        sprites.setSize(800f, 800f);
                        sprites.setAlphaMult(0.85f);
                        sprites.setAngle((float) Math.random() * 360f);
                    }
                }
                if (lasttarget != null && !lasttarget.isAlive()) {
                    Vector2f loc = new Vector2f(lasttarget.getLocation().getX(), lasttarget.getLocation().getY());
                    int amo = Meng_hullsizeint.Getsize(lasttarget);
                    Global.getCombatEngine().addLayeredRenderingPlugin(new Meng_StarfallingsPlugin(loc, lasttarget.getMaxHitpoints(), lasttarget.getArmorGrid().getMaxArmorInCell(), amo, ship));
                }

                init5 = true;
                loc = new Vector2f(ship.getLocation().getX(), ship.getLocation().getY());
                arg1 = ship.getFacing();
            }
            SpriteAPI sprite = Global.getSettings().getSprite("Meng", "Meng_Starring1");
            MagicRenderPlugin.addSingleframe(sprite, loc, CombatEngineLayers.ABOVE_SHIPS_LAYER);
            sprite.setAngle(timer3 * 60f);

            if (timer3 <= 0.5f) {
                sprite.setAlphaMult(timer3 * 1.5f);
                sprite.setSize(1600f * timer3, 1600f * timer3);
            } else if (timer3 <= 2.5f) {
                sprite.setSize(400f + 200f * (2.5f - timer3), 400f + 200f * (2.5f - timer3));
                sprite.setAlphaMult(0.75f);
            } else {
                sprite.setSize(400f + 800f * (timer3 - 2.5f), 400f + 800f * (timer3 - 2.5f));
                sprite.setAlphaMult((3f - timer3) * 1.5f);
            }
            if (timer3 <= 2f) {
                ship.setAlphaMult(timer3 / 3f);
                ship.setFacing(arg1);
                ship.getLocation().set(loc.getX() + (800f - 500f * timer3 / 2f) * (float) Math.sin(Math.toRadians(arg1)), loc.getY() + (800f - 500f * timer3 / 2f) * (float) Math.cos(Math.toRadians(arg1)));
                SpriteAPI sprite1 = ship.getSpriteAPI();
                MagicRenderPlugin.addSingleframe(sprite1, new Vector2f(loc.getX() + (800f - 500f * timer3 / 2f) * (float) Math.sin(Math.toRadians(arg1 - 120f)), loc.getY() + (800f - 500f * timer3 / 2f) * (float) Math.cos(Math.toRadians(arg1 - 120f))), CombatEngineLayers.ABOVE_SHIPS_LAYER);
            } else if (timer3 <= 3f) {
                ship.setAlphaMult(timer3 / 3f);
                ship.setFacing(arg1);
                ship.getLocation().set(loc.getX() + 300f * (3f - timer3) * (float) Math.sin(Math.toRadians(arg1)), loc.getY() + +300f * (3f - timer3) * (float) Math.cos(Math.toRadians(arg1)));
                SpriteAPI sprite1 = ship.getSpriteAPI();
                MagicRenderPlugin.addSingleframe(sprite1, new Vector2f(loc.getX() + 300f * (3f - timer3) * (float) Math.sin(Math.toRadians(arg1 - 120f)), loc.getY() + +300f * (3f - timer3) * (float) Math.cos(Math.toRadians(arg1 - 120f))), CombatEngineLayers.ABOVE_SHIPS_LAYER);
            }

        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {

        ShipAPI ship = (ShipAPI) stats.getEntity();
        ship.setHoldFire(false);
        ship.setPhased(false);
        stats.getTimeMult().unmodifyMult(id);
        Global.getCombatEngine().getTimeMult().unmodify(id);
        timer1 = 0f;
        timer2 = 0f;
        init1 = false;
        init2 = false;
        inits = false;
        init3 = false;
        init4 = false;
        loc = new Vector2f();
        targetloc = new Vector2f();
        shiploc = new Vector2f();
        lastloc = new Vector2f();
        arg1 = 0f;
        arg2 = 0f;
        target = null;
        lasttarget = null;
        ids = 0f;
        init5 = false;
        timer3 = 0f;
        lastdamage = 0f;
        flux = 0f;
        init6 = false;
        endloc = new Vector2f();

    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            return new StatusData("提高引擎出力", false);
        }
        return null;
    }

    public static class Meng_StarfallingsPlugin implements CombatLayeredRenderingPlugin {
        private final Vector2f orilocs;
        private final ShipAPI ships;
        private final float hitpoints;
        private final float armors;
        private final int ammo;
        float ids = MagicTrailPlugin.getUniqueID();
        Vector2f locs;
        private boolean end = false;
        private float timer1;

        public Meng_StarfallingsPlugin(Vector2f loc, float hitpoint, float armor, int amo, ShipAPI own) {
            orilocs = loc;
            ships = own;
            armors = armor;
            hitpoints = hitpoint;
            ammo = amo;
        }

        public void init(CombatEntityAPI entity) {
            locs = new Vector2f(orilocs.getX(), orilocs.getY());
        }

        @Override
        public void cleanup() {
        }

        @Override
        public boolean isExpired() {
            return end;//返回值为true时，Plugin删除，因此当计时器超过三秒后删除。
        }

        @Override
        public void advance(float amount) {
            timer1 += amount;

            float arg = Meng_arcfind.Findarc(locs, ships.getLocation());
            SpriteAPI sprite = Global.getSettings().getSprite("Meng", "Meng_Starring3");
            SpriteAPI sprites = Global.getSettings().getSprite("Meng", "Meng_Starfalling_trail");
            locs.setX(locs.getX() + 300f * amount * timer1 / 2F * (float) Math.cos(Math.toRadians(arg)));
            locs.setY(locs.getY() + 300f * amount * timer1 / 2F * (float) Math.sin(Math.toRadians(arg)));
            sprite.setSize(300f + ammo * 50f, 300f + ammo * 50f);
            sprite.setAngle(timer1 * 60f);
            MagicTrailPlugin.AddTrailMemberAdvanced(ships, ids,
                    sprites,
                    locs,
                    0f,
                    0f,
                    arg,
                    0f,
                    0f,
                    20f + ammo * 5f,
                    40f + ammo * 10f,
                    new Color(173, 253, 221, 255),
                    new Color(113, 255, 231, 255),
                    1f,
                    0.3f,
                    0.7f,
                    2.0f,
                    true,
                    256f,
                    10,
                    10f,
                    null,
                    null,
                    CombatEngineLayers.ABOVE_SHIPS_LAYER,
                    360f);
            MagicRenderPlugin.addSingleframe(sprite, locs, CombatEngineLayers.ABOVE_SHIPS_LAYER);
            if (MathUtils.getDistance(locs, ships.getLocation()) <= 100f && timer1 >= 5f) {
                ships.setHitpoints(Math.min(ships.getHitpoints() + hitpoints * 0.3f, ships.getMaxHitpoints()));
                ships.getSystem().setCooldownRemaining(0f);
                int nowammo = ships.getSystem().getAmmo();
                ships.getSystem().setAmmo(nowammo + ammo);
                float[][] grid1 = ships.getArmorGrid().getGrid();
                for (int x = 0; x < grid1.length; x++)
                    for (int y = 0; y < grid1[0].length; y++)
                        ships.getArmorGrid().setArmorValue(x, y, Math.min(grid1[x][y] + armors * 0.2f, ships.getArmorGrid().getMaxArmorInCell()));
                ships.setJitter(ships, new Color(34, 248, 241, 255), 10f, 1, 10f);
                Global.getSoundPlayer().playSound("Meng_Xweaponmic", 1.0f, 1.0f, ships.getLocation(), new Vector2f());
                end = true;
            }
        }

        @Override
        public EnumSet<CombatEngineLayers> getActiveLayers() {
            return null;
        }

        @Override
        public float getRenderRadius() {
            return 0;
        }

        @Override
        public void render(CombatEngineLayers layer, ViewportAPI viewport) {

        }

    }
}
