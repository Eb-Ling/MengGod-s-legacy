package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import data.methods.Meng_MingGodPlugin;
import data.methods.Meng_arcfind;
import data.scripts.plugins.MagicRenderPlugin;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.EnumSet;

public class Meng_MingGod_Spacedamage extends BaseShipSystemScript {
    private final String KEY1 = "Meng_MingGod_eyecharge_own";
    boolean init = false;
    boolean init1 = false;

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {

        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) {
            return;
        }
        if (!ship.getCustomData().containsKey(KEY1)) {
            Meng_MingGod_EyePlugin.DataContainer data1 = new Meng_MingGod_EyePlugin.DataContainer();
            ship.setCustomData(KEY1, data1);
        }
        Meng_MingGod_EyePlugin.DataContainer data1 = (Meng_MingGod_EyePlugin.DataContainer) ship.getCustomData().get(KEY1);
        ship.setJitter(ship, new Color(157, 21, 21, 255), 25f * effectLevel, 1, 5f);
        if (state != State.IN) {
            if (!init) {
                init = true;
                RippleDistortion ripple = new RippleDistortion(ship.getLocation(), ship.getVelocity());
                ripple.setSize(2000f);
                ripple.setIntensity(100f);
                ripple.fadeInSize(10f);
                ripple.fadeInIntensity(5f);
                ripple.setFrameRate(60f);
                DistortionShader.addDistortion(ripple);
                for (ShipAPI target : data1.endtargets) {
                    Global.getCombatEngine().addLayeredRenderingPlugin(new Meng_MingGodPlugin(target, ship));
                }
            }
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) {
            return;
        }
        init = false;
        if (!init1) {
            init1 = true;
            Global.getCombatEngine().addLayeredRenderingPlugin(new Meng_MingGod_EyePlugin(ship));
        }
    }

    public static class Meng_MingGod_EyePlugin implements CombatLayeredRenderingPlugin {
        private final ShipAPI ship;
        private final String KEY = "Meng_MingGod_eyecharge";
        private final String KEY1 = "Meng_MingGod_eyecharge_own";

        public Meng_MingGod_EyePlugin(ShipAPI source) {
            ship = source;
        }

        public void init(CombatEntityAPI entity) {
        }

        @Override
        public void cleanup() {
        }

        @Override
        public boolean isExpired() {
            return ship == null || !ship.isAlive();
        }

        @Override
        public void advance(float amount) {
            if (ship == null || !ship.isAlive()) return;
            if (!ship.getCustomData().containsKey(KEY1)) {
                DataContainer data1 = new DataContainer();
                ship.setCustomData(KEY1, data1);
            }
            DataContainer data1 = (DataContainer) ship.getCustomData().get(KEY1);
            if (data1.endtargets == null) {
                data1.endtargets = new ArrayList<>();
            }
            float arg = ship.getFacing();

            CombatEngineAPI engine = Global.getCombatEngine();
            for (ShipAPI target : engine.getShips()) {
                if (!target.getCustomData().containsKey(KEY)) {
                    DataContainer data = new DataContainer();
                    target.setCustomData(KEY, data);
                }
                DataContainer data = (DataContainer) target.getCustomData().get(KEY);
                if (target.isAlive()) {
                    if (!target.isHulk() && !target.isDrone() && !target.isFighter() && target.getOwner() != ship.getOwner()) {
                        float targetarg = Meng_arcfind.Findarc(ship.getLocation(), target.getLocation());
                        boolean argok;
                        if (arg <= 30f) {
                            if (targetarg >= 330f) {
                                argok = Math.abs(targetarg - 360f - arg) <= 30f;
                            } else {
                                argok = Math.abs(targetarg - arg) <= 30f;
                            }
                        } else if (arg >= 330f) {
                            if (targetarg >= 330f) {
                                argok = Math.abs(targetarg - arg) <= 30f;
                            } else {
                                argok = Math.abs(targetarg + 360f - arg) <= 30f;
                            }
                        } else {
                            argok = Math.abs(targetarg - arg) <= 30f;
                        }
                        float distance = MathUtils.getDistance(ship.getLocation(), target.getLocation());
                        if (argok && distance <= 3000f) {
                            data.chargelevel = Math.min(1f, data.chargelevel + amount * 0.2f);
                        } else {
                            data.chargelevel = Math.max(0f, data.chargelevel - amount * 0.2f);
                        }
                        if (data.chargelevel >= 1f) {
                            if (!data1.endtargets.contains(target)) {
                                data1.endtargets.add(target);
                            } else {
                                data.endlevel = Math.min(1f, data.endlevel + amount);
                            }
                        } else {
                            data1.endtargets.remove(target);
                            data.endlevel = Math.max(0f, data.endlevel - amount * 5f);
                        }
                        if (data.chargelevel > 0f) {
                            SpriteAPI Meng_MingGod_Eyesides = Global.getSettings().getSprite("Meng_MingGod", "Meng_MingGod_Eyesides");
                            Vector2f loc = new Vector2f(target.getLocation().getX(), target.getLocation().getY() + target.getShieldRadiusEvenIfNoShield());
                            MagicRenderPlugin.addSingleframe(Meng_MingGod_Eyesides, loc, CombatEngineLayers.ABOVE_SHIPS_LAYER);
                            Meng_MingGod_Eyesides.setSize(target.getShieldRadiusEvenIfNoShield() * 0.4f, data.chargelevel * target.getShieldRadiusEvenIfNoShield() * 0.4f * 140f / 360f);
                        }
                        if (data.endlevel > 0f) {
                            SpriteAPI Meng_MingGod_Eye = Global.getSettings().getSprite("Meng_MingGod", "Meng_MingGod_Eye");
                            Vector2f loc = new Vector2f(target.getLocation().getX(), target.getLocation().getY() + target.getShieldRadiusEvenIfNoShield());
                            MagicRenderPlugin.addSingleframe(Meng_MingGod_Eye, loc, CombatEngineLayers.ABOVE_SHIPS_LAYER);
                            Meng_MingGod_Eye.setSize(target.getShieldRadiusEvenIfNoShield() * 0.4f * 100f / 360f, target.getShieldRadiusEvenIfNoShield() * 0.4f * 100f / 360f);
                            Meng_MingGod_Eye.setAlphaMult(data.endlevel);
                        }
                    }
                } else {
                    data1.endtargets.remove(target);
                }
            }
        }

        @Override
        public EnumSet<CombatEngineLayers> getActiveLayers() {
            return EnumSet.of(CombatEngineLayers.UNDER_SHIPS_LAYER);
        }

        @Override
        public float getRenderRadius() {
            return 10000000f;
        }

        @Override
        public void render(CombatEngineLayers layer, ViewportAPI viewport) {
            if (layer == CombatEngineLayers.UNDER_SHIPS_LAYER) {
                Vector2f location = ship.getLocation();

                float sizes = 3000f;

                float width = 1f;
                float facing = ship.getFacing();

                GL11.glPushMatrix();

                GL11.glTranslatef(location.x, location.y, 0.0f);

                GL11.glRotatef(facing + 30f, 0f, 0f, 1f);
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                GL11.glEnable(GL11.GL_BLEND);

                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
                GL11.glColor4f(0.6f, 0.1f, 0.1f, 0.7f);

                GL11.glBegin(GL11.GL_QUADS);

                GL11.glVertex2f(0f, -width);
                GL11.glVertex2f(0f, width);
                GL11.glVertex2f(sizes * 1.05f, width);
                GL11.glVertex2f(sizes * 1.05f, -width);

                GL11.glEnd();
                GL11.glPopMatrix();
                GL11.glPushMatrix();

                GL11.glTranslatef(location.x, location.y, 0.0f);

                GL11.glRotatef(facing - 30f, 0f, 0f, 1f);
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                GL11.glEnable(GL11.GL_BLEND);

                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
                GL11.glColor4f(0.6f, 0.1f, 0.1f, 0.7f);

                GL11.glBegin(GL11.GL_QUADS);

                GL11.glVertex2f(0f, -width);
                GL11.glVertex2f(0f, width);
                GL11.glVertex2f(sizes * 1.05f, width);
                GL11.glVertex2f(sizes * 1.05f, -width);

                GL11.glEnd();
                GL11.glPopMatrix();

                for (int i = 1; i < 60; i++) {
                    float s = sizes * (float) Math.sin(Math.toRadians(0.5f));
                    Vector2f locs = new Vector2f(ship.getLocation().getX() + sizes * (float) Math.cos(Math.toRadians(facing - 30f + i)), ship.getLocation().getY() + sizes * (float) Math.sin(Math.toRadians(facing - 30f + i)));
                    GL11.glPushMatrix();
                    GL11.glTranslatef(locs.x, locs.y, 0.0f);
                    GL11.glRotatef(90f + facing - 30f + i, 0f, 0f, 0.1f);
                    GL11.glDisable(GL11.GL_TEXTURE_2D);
                    GL11.glEnable(GL11.GL_BLEND);

                    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
                    GL11.glColor4f(0.6f, 0.1f, 0.1f, 1f);

                    GL11.glBegin(GL11.GL_QUADS);

                    GL11.glVertex2f(0f, -width);
                    GL11.glVertex2f(0f, width);
                    GL11.glVertex2f(s, width);
                    GL11.glVertex2f(s, -width);
                    GL11.glEnd();
                    GL11.glPopMatrix();
                }
            }
        }

        public static class DataContainer {
            public ArrayList<ShipAPI> endtargets;
            float chargelevel = 0f;
            float endlevel = 0f;
        }

    }
}
