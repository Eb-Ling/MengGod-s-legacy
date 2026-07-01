package data.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.entities.SimpleEntity;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.EnumSet;

public class Meng_Starproject_effect implements BeamEffectPlugin {
    public boolean init = false;
    public boolean init1 = false;
    public float timer0 = 0f;
    public float timer = 0f;

    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        if (beam.getDamageTarget() instanceof ShipAPI) {
            ShipAPI ship = beam.getSource();
            timer0 += amount;
            if (timer0 >= 3f) {
                if (!init1) {
                    init1 = true;
                    engine.addLayeredRenderingPlugin(new Meng_Starproject_Plugin(ship, beam.getWeapon()));
                }
                timer += amount;
                if (timer >= 1.4f) {
                    timer = 0f;
                    engine.addLayeredRenderingPlugin(new Meng_Starproject_Plugin(ship, beam.getWeapon()));
                }
            }
        } else {
            timer0 = Math.max(0f, timer0 - amount * 3f);
            timer = 0f;
            if (timer0 <= 0f) init1 = false;
        }
    }

    public static class Meng_Starproject_Plugin implements CombatLayeredRenderingPlugin {
        private final float fluxnum = 250f;
        private final ShipAPI ship;
        private final WeaponAPI weapon;
        private float timer = 0.25f;
        private int nums = 0;
        private ArrayList<DamagingProjectileAPI> projs = null;

        public Meng_Starproject_Plugin(ShipAPI ships, WeaponAPI weapons) {
            ship = ships;
            weapon = weapons;
        }

        public void init(CombatEntityAPI entity) {

        }

        @Override
        public void cleanup() {

        }

        @Override
        public boolean isExpired() {
            return timer >= 10f;
        }

        @Override
        public void advance(float amount) {
            if (!weapon.isFiring() || weapon.isKeepBeamTargetWhileChargingDown()) {
                timer = 12f;
                return;
            }
            timer += amount;
            if (projs == null) {
                projs = new ArrayList<>();
            }
            if (nums < 4 && timer >= 0.15f) {
                timer = 0f;
                nums++;
                ship.getFluxTracker().increaseFlux(fluxnum, ship.getVariant().hasHullMod("Meng_fire_core") && !ship.getSystem().getId().equals("Meng_fire_fluxtrans_attackcore"));

                Global.getCombatEngine().spawnMuzzleFlashOrSmoke(ship, weapon.getSlot(), Global.getSettings().getWeaponSpec("Meng_Stargauss"), 0, weapon.getCurrAngle() - 180f);
                float arg1 = (float) Math.random() * 90f;
                float range1 = (float) Math.random() * 20f;
                Vector2f loc1 = new Vector2f(weapon.getLocation().x + (range1 - 10f + nums * 40f) * (float) Math.cos(Math.toRadians(arg1 + weapon.getCurrAngle() - 45f)), weapon.getLocation().y + (range1 - 10f + nums * 40f) * (float) Math.sin(Math.toRadians(arg1 + weapon.getCurrAngle() - 45f)));
                float arg2 = (float) Math.random() * 90f;
                float range2 = (float) Math.random() * 20f;
                Vector2f loc2 = new Vector2f(weapon.getLocation().x + (range2 - 10f + nums * 40f) * (float) Math.cos(Math.toRadians(arg2 + weapon.getCurrAngle() - 45f)), weapon.getLocation().y + (range2 - 10f + nums * 40f) * (float) Math.sin(Math.toRadians(arg2 + weapon.getCurrAngle() - 45f)));
                Global.getSoundPlayer().playSound("Meng_Starproject_fire", 1.0f, 1.0f, weapon.getLocation(), new Vector2f());
                Global.getCombatEngine().spawnEmpArcPierceShields(ship, new Vector2f(loc1), new SimpleEntity(loc2), new SimpleEntity(loc2), DamageType.ENERGY, 0F, 0F, 100000F, null, 6f, new Color(120, 255, 255, 255), new Color(100, 100, 255, 255));
                WeaponAPI w = Global.getCombatEngine().createFakeWeapon(ship, "Meng_Stargauss");
                DamagingProjectileAPI proj = (DamagingProjectileAPI) Global.getCombatEngine().spawnProjectile(ship, w, "Meng_Stargauss", weapon.getLocation(), weapon.getCurrAngle(), new Vector2f());
                projs.add(proj);
            }
            for (DamagingProjectileAPI pj : projs) {
                float range = MathUtils.getDistance(pj.getLocation(), weapon.getLocation());
                float arg = weapon.getCurrAngle();
                pj.setFacing(arg);
                Vector2f realloc = new Vector2f(weapon.getLocation().x + range * (float) Math.cos(Math.toRadians(arg)), weapon.getLocation().y + range * (float) Math.sin(Math.toRadians(arg)));
                pj.getLocation().set(realloc);
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
