package data.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import data.methods.Meng_findbezierpoint;
import data.methods.Meng_arcfind;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.plugins.MagicRenderPlugin;

import java.util.ArrayList;
import java.util.EnumSet;


public class Meng_fire_smartgun implements EveryFrameWeaponEffectPlugin {
    public boolean isincombat = false;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (!isincombat) {
            isincombat = true;
            Global.getCombatEngine().addLayeredRenderingPlugin(new Meng_fire_smartgun_plugin(weapon));
        }
    }

    public static class Meng_Smartgun_proj_fix implements CombatLayeredRenderingPlugin {
        private final WeaponAPI weapon;
        private final ShipAPI target;
        private final DamagingProjectileAPI pj;
        private float timer = 0f;
        private float livetime;
        private boolean followtheweapon=true;
        public Meng_Smartgun_proj_fix(ShipAPI t, WeaponAPI w, DamagingProjectileAPI p) {
            weapon = w;
            target = t;
            pj = p;
        }

        public void init(CombatEntityAPI entity) {
            livetime = MathUtils.getDistance(weapon.getFirePoint(0), target.getLocation()) / 1300f;
            followtheweapon= MathUtils.getDistance(pj.getSpawnLocation(), weapon.getFirePoint(0)) <= 100f;
        }

        @Override
        public void cleanup() {
        }

        @Override
        public boolean isExpired() {
            return timer >= 10f;//返回值为true时，Plugin删除。
        }

        @Override
        public void advance(float amount) {
            if (target == null || weapon == null || pj == null) {
                return;
            }
            timer += amount;
            float level = timer / livetime;
            if (level <= 1f) {

                Vector2f sourceloc;
                if(followtheweapon) {
                    sourceloc = weapon.getFirePoint(0);
                }
                else {
                    sourceloc = pj.getSpawnLocation();
                }
                Vector2f targetloc = target.getLocation();
                float arg = weapon.getCurrAngle();
                float d = MathUtils.getDistance(sourceloc, targetloc) * 0.66f;
                Vector2f midpoint = new Vector2f(sourceloc.x + d * (float) Math.cos(Math.toRadians(arg)), sourceloc.y + d * (float) Math.sin(Math.toRadians(arg)));
                Vector2f p = Meng_findbezierpoint.findpointf2(sourceloc, midpoint, targetloc, level);
                Vector2f nextp = Meng_findbezierpoint.findpointf2(sourceloc, midpoint, targetloc, level * 1.01f);
                float args;
                if(level<=0.98f) args = Meng_arcfind.Findarc(p, nextp);
                else args=pj.getFacing();
                pj.getLocation().set(p);
                pj.setFacing(args);
            }
            else {
                if(!pj.didDamage()) {
                    pj.getLocation().set(target.getLocation());
                }
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

    public static class Meng_fire_smartgun_plugin implements CombatLayeredRenderingPlugin {
        public WeaponAPI weapon;
        public ShipAPI target;
        public ShipAPI lasttarget;
        private ArrayList<DamagingProjectileAPI> projs = null;
        private float targetprocess = 0f;

        public Meng_fire_smartgun_plugin(WeaponAPI w) {
            weapon = w;
        }

        public void init(CombatEntityAPI entity) {

        }

        @Override
        public void cleanup() {
        }

        @Override
        public boolean isExpired() {
            return weapon == null;//返回值为true时，Plugin删除。
        }

        @Override
        public void advance(float amount) {
            ShipAPI ship = weapon.getShip();
            if (!ship.isAlive()) return;
            ShipAPI targets = ship.getShipTarget();
            if (projs == null) {
                projs = new ArrayList<>();
            }
            if (targets != null) {
                float distance = MathUtils.getDistance(targets.getLocation(), weapon.getLocation()) - targets.getShieldRadiusEvenIfNoShield();
                if (distance <= weapon.getRange()&&targets.isAlive()) {
                    target = ship.getShipTarget();
                } else {
                    target = null;
                    float dis = 9999999f;
                    for (ShipAPI t : Global.getCombatEngine().getShips()) {
                        if (t.isAlive() && t.getOwner() != ship.getOwner() && !t.isHulk()) {
                            float d = MathUtils.getDistance(t.getLocation(), weapon.getLocation()) - t.getShieldRadiusEvenIfNoShield();
                            if (d < dis && d <= weapon.getRange()) {
                                dis = d;
                                target = t;
                            }
                        }
                    }
                }
            } else {
                target = null;
            }
            if (target == null) {
                float dis = 9999999f;
                for (ShipAPI t : Global.getCombatEngine().getShips()) {
                    if (t.isAlive() && t.getOwner() != ship.getOwner() && !t.isHulk()) {
                        float d = MathUtils.getDistance(t.getLocation(), weapon.getLocation()) - t.getShieldRadiusEvenIfNoShield();
                        if (d < dis && d <= weapon.getRange()) {
                            dis = d;
                            target = t;
                        }
                    }
                }
            }

            if (target == null) return;

            if (target == lasttarget) targetprocess = Math.min(0.5f, targetprocess + amount);
            else targetprocess = 0f;
            lasttarget = target;
            SpriteAPI targetring = Global.getSettings().getSprite("fx", "Meng_targeting_ring");
            SpriteAPI targetcore = Global.getSettings().getSprite("fx", "Meng_targeting_core");
            MagicRenderPlugin.addSingleframe(targetring, target.getLocation(), CombatEngineLayers.ABOVE_SHIPS_LAYER);
            MagicRenderPlugin.addSingleframe(targetcore, target.getLocation(), CombatEngineLayers.ABOVE_SHIPS_LAYER);
            float width = target.getShieldRadiusEvenIfNoShield();
            targetring.setSize(width * (6f - 8f * targetprocess), width * (6f - 8f * targetprocess));
            targetcore.setSize(width * 2f, width * 2f);
            targetring.setAlphaMult(targetprocess * 2f);
            targetcore.setAlphaMult(targetprocess * 2f);
            boolean shouldfire = false;
            for (WeaponGroupAPI w : weapon.getShip().getWeaponGroupsCopy()) {
                if (!ship.isHoldFire() && weapon.getShip().getSelectedGroupAPI() != w && w.isAutofiring() && w.getWeaponsCopy().contains(weapon) && target.isAlive() && target.getOwner() != ship.getOwner()) {
                    shouldfire = true;
                }
            }
            weapon.setForceFireOneFrame(weapon.getShip().getAI() != null || shouldfire);
            for (DamagingProjectileAPI pj : Global.getCombatEngine().getProjectiles()) {
                if (pj.getWeapon() == weapon) {
                    if (!projs.contains(pj)) {
                        Global.getCombatEngine().addLayeredRenderingPlugin(new Meng_Smartgun_proj_fix(target, weapon, pj));
                        projs.add(pj);
                    }
                }
            }
        }

        @Override
        public EnumSet<CombatEngineLayers> getActiveLayers() {
            return EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_LAYER);
        }

        @Override
        public float getRenderRadius() {
            return 10000000f;
        }

        @Override
        public void render(CombatEngineLayers layer, ViewportAPI viewport) {
            if (!weapon.getShip().isAlive()) return;
            if (target == null) return;
            if (layer == CombatEngineLayers.ABOVE_SHIPS_LAYER) {
                for (int i = 0; i < 500; i++) {
                    Vector2f sourceloc = weapon.getFirePoint(0);
                    Vector2f targetloc = target.getLocation();
                    float arg = weapon.getCurrAngle();
                    float d = MathUtils.getDistance(sourceloc, targetloc) * 0.66f;
                    float width = 1f;
                    Vector2f midpoint = new Vector2f(sourceloc.x + d * (float) Math.cos(Math.toRadians(arg)), sourceloc.y + d * (float) Math.sin(Math.toRadians(arg)));
                    Vector2f p = Meng_findbezierpoint.findpointf2(sourceloc, midpoint, targetloc, 1 / 500f * i);
                    Vector2f nextp = Meng_findbezierpoint.findpointf2(sourceloc, midpoint, targetloc, 1 / 500f * (Math.min(i + 1, 500)));
                    float currarg = Meng_arcfind.Findarc(p, nextp);
                    float currd = MathUtils.getDistance(p, nextp);
                    GL11.glPushMatrix();
                    GL11.glTranslatef(p.x, p.y, 0.0f);
                    GL11.glRotatef(currarg, 0f, 0f, 1f);
                    GL11.glDisable(GL11.GL_TEXTURE_2D);
                    GL11.glEnable(GL11.GL_BLEND);
                    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
                    GL11.glColor4f(0.71f, 0.61f, 1f, targetprocess * 2f);
                    GL11.glBegin(GL11.GL_QUADS);
                    GL11.glVertex2f(0f, -width);
                    GL11.glVertex2f(0f, width);
                    GL11.glVertex2f(currd, width);
                    GL11.glVertex2f(currd, -width);
                    GL11.glEnd();
                    GL11.glPopMatrix();
                }

            }
        }
    }

}
