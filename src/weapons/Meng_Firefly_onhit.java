package data.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import data.methods.Meng_arcfind;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicLensFlare;

import java.awt.*;
import java.util.EnumSet;

public class Meng_Firefly_onhit implements OnHitEffectPlugin {
    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (!shieldHit) {
            WeaponAPI weapon=engine.createFakeWeapon(projectile.getSource(),"Meng_firefly_multineedler");
            if (weapon != null && weapon.getDamage() != null) {
                engine.applyDamage(target,point,weapon.getDamage().getDamage(),weapon.getDamageType(),0f,true,false,projectile.getSource(),false);
            }
        }
        else {
            if (!(target instanceof ShipAPI)) {
                return;
            }

            ShipAPI targets = (ShipAPI) target;
            float arg = Meng_arcfind.Findarc(point, target.getLocation());
            Vector2f spawnloc = projectile.getSpawnLocation();
            float inarg = Meng_arcfind.Findarc(point, spawnloc);

            if (Float.isNaN(arg) || Float.isInfinite(arg)) {
                arg = 0f;
            }
            if (Float.isNaN(inarg) || Float.isInfinite(inarg)) {
                inarg = 0f;
            }

            float outarg = inarg - 2 * (inarg - (180f + arg));

            while (outarg < 0f) outarg += 360f;
            while (outarg >= 360f) outarg -= 360f;

            Vector2f nextpoint = new Vector2f(point.x - 15f * (float) Math.cos(Math.toRadians(arg)), point.y - 15f * (float) Math.sin(Math.toRadians(arg)));
            Vector2f newpoint = new Vector2f(nextpoint.x + 20f * (float) Math.cos(Math.toRadians(outarg)), nextpoint.y + 20f * (float) Math.sin(Math.toRadians(outarg)));
            if (MathUtils.getDistance(newpoint, target.getLocation()) <= targets.getShieldRadiusEvenIfNoShield() + 5f) {
                float args = Meng_arcfind.Findarc(target.getLocation(), newpoint);
                if (Float.isNaN(args) || Float.isInfinite(args)) {
                    args = 0f;
                }
                newpoint = new Vector2f(target.getLocation().x + (((ShipAPI) target).getShieldRadiusEvenIfNoShield() + 10f) * (float) Math.cos(Math.toRadians(args)), target.getLocation().y + (((ShipAPI) target).getShieldRadiusEvenIfNoShield() + 10f) * (float) Math.sin(Math.toRadians(args)));
            }

            WeaponAPI weapon=engine.createFakeWeapon(projectile.getSource(),"Meng_fire_firefly");
            engine.addLayeredRenderingPlugin(new Meng_firefly_hiteffectPlugin(newpoint, outarg, targets, weapon));

        }
    }

    public static class Meng_firefly_hiteffectPlugin implements CombatLayeredRenderingPlugin {
        float outarg;
        ShipAPI target;
        WeaponAPI source;
        Vector2f point;
        Vector2f vec;
        boolean init = false;
        boolean init0 = false;
        DamagingProjectileAPI proj;
        float width=10f;
        DamagingProjectileAPI proj1;
        boolean proj1Failed = false;

        public Meng_firefly_hiteffectPlugin(Vector2f points, float outargs, ShipAPI targets, WeaponAPI sources) {
            outarg = outargs;
            target = targets;
            source = sources;
            point = points;
        }

        public void init(CombatEntityAPI entity) {

        }

        @Override
        public void cleanup() {
        }

        @Override
        public boolean isExpired() {
            if (proj1 != null) {
                return proj1.isExpired();
            }
            return proj1Failed;
        }

        @Override
        public void advance(float amount) {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine == null || engine.isPaused()) return;

            if (source == null || source.getShip() == null || !source.getShip().isAlive()) {
                return;
            }

            if (!init0) {
                init0 = true;
                try {
                    proj = (DamagingProjectileAPI) engine.spawnProjectile(source.getShip(), source, "Meng_fire_firefly", point, outarg, new Vector2f());
                    width = proj.getProjectileSpec().getWidth();
                } catch (Exception e) {
                    proj1Failed = true;
                    return;
                }
            }
            if (proj != null) {
                if (!init) {
                    init = true;
                    vec = new Vector2f(proj.getVelocity().x, proj.getVelocity().y);
                }
                proj.getVelocity().set(proj.getVelocity().x - vec.x * 0.02f, proj.getVelocity().y - vec.y * 0.02f);
                if (proj.getVelocity().length() <= vec.length() * 0.05f) {
                    Vector2f loc = new Vector2f(proj.getLocation().x, proj.getLocation().y);
                    if (proj.getProjectileSpec() != null) {
                        MagicLensFlare.createSharpFlare(engine, source.getShip(), loc, width, width * 3f, (float) Math.random() * 360f, new Color(255, 228, 228, 255), new Color(255, 0, 0, 255));
                    }

                    try {
                        float safeAngle = Meng_arcfind.Findarc(loc, target.getLocation());
                        if (Float.isNaN(safeAngle) || Float.isInfinite(safeAngle)) {
                            safeAngle = 0f;
                        }
                        proj1 = (DamagingProjectileAPI) engine.spawnProjectile(source.getShip(), source, "Meng_firefly_multineedler", loc, safeAngle, new Vector2f());
                    } catch (Exception e) {
                        proj1Failed = true;
                        return;
                    }
                    proj.setHitpoints(0f);

                }
            }
        }

        @Override
        public EnumSet<CombatEngineLayers> getActiveLayers() {
            return EnumSet.of(CombatEngineLayers.BELOW_SHIPS_LAYER);
        }

        @Override
        public float getRenderRadius() {
            return 1000f;
        }

        @Override
        public void render(CombatEngineLayers layer, ViewportAPI viewport) {

        }


    }
}
