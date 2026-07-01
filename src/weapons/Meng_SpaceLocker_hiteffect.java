package data.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.loading.ProjectileSpawnType;
import com.fs.starfarer.api.util.Noise;
import data.methods.Meng_arcfind;
import data.scripts.util.MagicLensFlare;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.entities.SimpleEntity;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;

public class Meng_SpaceLocker_hiteffect implements OnHitEffectPlugin {
    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        float time = projectile.getWeapon().getCooldown() / 2f / projectile.getSource().getMutableStats().getEnergyRoFMult().getModifiedValue();
        ShipAPI ship = projectile.getSource();
        if (target instanceof ShipAPI && target.getOwner() != ship.getOwner() && !((ShipAPI) target).isHulk() && !((ShipAPI) target).isDrone()) {
            Vector2f loc = new Vector2f(point.getX(), point.getY());
            Global.getCombatEngine().addLayeredRenderingPlugin(new Meng_SpaceLocker_hiteffectPlugin(loc, ship, (ShipAPI) target, time));
        }
    }

    public static class Meng_SpaceLocker_hiteffectPlugin implements CombatLayeredRenderingPlugin {
        private final ShipAPI ships;
        private final ShipAPI targets;
        private final float time;
        private final Vector2f loc;
        private float timer = 0f;
        private float size;
        private ArrayList<DamagingProjectileAPI> projs;
        private ArrayList<DamagingProjectileAPI> spawnedprojs;
        private float[] lastnoise;
        private float[] ideanoise;
        private float[] noise;
        private float arg;
        private float f = 0f;
        private float alpha = 1f;
        public Meng_SpaceLocker_hiteffectPlugin(Vector2f locs, ShipAPI ship, ShipAPI target, float timer) {
            ships = ship;
            targets = target;
            time = timer;
            loc = locs;
        }

        public void init(CombatEntityAPI entity) {
            size = 0f;
            lastnoise = Noise.genNoise(360, 1f);
            ideanoise = Noise.genNoise(360, 1f);
            noise = Arrays.copyOf(lastnoise, 360);
            arg = (float) Math.random() * 360f;
        }

        @Override
        public void cleanup() {
        }

        @Override
        public boolean isExpired() {
            return timer >= time;//返回值为true时，Plugin删除，因此当计时器超过三秒后删除。
        }

        @Override
        public void advance(float amount) {
            CombatEngineAPI engine = Global.getCombatEngine();
            timer += amount * ships.getMutableStats().getTimeMult().getModifiedValue();
            f += amount;
            if (f > 2f) {
                f = 0f;
                lastnoise = Arrays.copyOf(ideanoise, 360);
                ideanoise = Noise.genNoise(360, 1f);
            }
            for (int i = 0; i < 360; i++) {
                noise[i] = lastnoise[i] + (ideanoise[i] - lastnoise[i]) * f/2f;
            }
            if (projs == null) {
                projs = new ArrayList<>();
            }
            if (spawnedprojs == null) {
                spawnedprojs = new ArrayList<>();
            }
            if(timer<=time/20f) {
                alpha=Math.max(0f,timer/(time/20f));
            }
            if(timer<=time/10f){
                size = Math.min(1f,timer/time * 10f) * targets.getShieldRadiusEvenIfNoShield() * 3f;
            }
            else if (timer <= time - time / 20f) {
                float range = size - targets.getShieldRadiusEvenIfNoShield();
                float distance = MathUtils.getDistance(loc, targets.getLocation());
                if (distance > range) {
                    float arg = Meng_arcfind.Findarc(loc, targets.getLocation());
                    engine.spawnEmpArcPierceShields(ships, targets.getLocation(), new SimpleEntity(targets.getLocation()), new SimpleEntity(targets.getLocation()), DamageType.ENERGY, 0f, 0f, 20000000f, null, 15f, new Color(129, 29, 29, 255), new Color(246, 209, 209, 255));
                    Vector2f idealoc = new Vector2f(loc.getX() + range * (float) Math.cos(Math.toRadians(arg)), loc.getY() + range * (float) Math.sin(Math.toRadians(arg)));
                    targets.getLocation().set(idealoc);
                    targets.getVelocity().set(new Vector2f(0f,0f));
                }
                for (DamagingProjectileAPI proj : engine.getProjectiles()) {
                    WeaponAPI weapon = proj.getWeapon();
                    if (weapon != null && MathUtils.getDistance(proj.getLocation(), weapon.getLocation()) <= 100f && proj.getWeapon().getSpec() != null && proj.getWeapon().getSpec().getProjectileSpec() != null && proj.getSource() == ships && !projs.contains(proj) && !spawnedprojs.contains(proj) && proj.getProjectileSpec() != null && proj.getSpawnType() != ProjectileSpawnType.MISSILE && !proj.isFromMissile() && proj.getWeapon().getType() != WeaponAPI.WeaponType.MISSILE) {
                        projs.add(proj);
                        float arg = (float) Math.random() * 360f;
                        Vector2f locs = new Vector2f(loc.getX() + range * (float) Math.cos(Math.toRadians(arg)), loc.getY() + range * (float) Math.sin(Math.toRadians(arg)));
                        float orderarg = Meng_arcfind.Findarc(locs, targets.getLocation());
                        MagicLensFlare.createSharpFlare(engine, ships, locs, proj.getProjectileSpec().getWidth(), proj.getProjectileSpec().getWidth() * 5f, arg, new Color(185, 37, 37, 255), new Color(246, 209, 209, 255));
                        DamagingProjectileAPI projed = (DamagingProjectileAPI) engine.spawnProjectile(proj.getWeapon().getShip(), proj.getWeapon(), proj.getWeapon().getId(), locs, orderarg, new Vector2f());
                        spawnedprojs.add(projed);
                    }
                }
            }
            else {
                alpha=Math.min(1f,(time-timer)/(time/20f));
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
            if (layer == CombatEngineLayers.ABOVE_SHIPS_LAYER) {
                GL11.glPushMatrix();
                GL11.glTranslatef(loc.x, loc.y, 0.0f);
                GL11.glRotatef(arg, 0f, 0f, 0.1f);
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                GL11.glEnable(GL11.GL_BLEND);

                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                Color color = new Color(0, 0, 0,  (int)Math.floor(50*alpha));
                GL11.glColor4ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue(), (byte) color.getAlpha());

                GL11.glBegin(GL11.GL_TRIANGLE_FAN);

                GL11.glVertex2f(0f, 0f);
                float noisesize=0.1f;
                for (int i = 0; i < 360; i++) {
                    GL11.glVertex2f((size * (1f-noisesize) + noise[i] * size * noisesize) * (float) Math.cos(Math.toRadians(i)), (size * (1f-noisesize) + noise[i] * size * noisesize) * (float) Math.sin(Math.toRadians(i)));
                }
                GL11.glVertex2f((size * (1f-noisesize) + noise[0] * size * noisesize) * (float) Math.cos(Math.toRadians(0)), (size * (1f-noisesize) + noise[0] * size * noisesize) * (float) Math.sin(Math.toRadians(0)));

                GL11.glEnd();
                GL11.glPopMatrix();
                SpriteAPI sprite = Global.getSettings().getSprite("combat", "corona_hard");
                GL11.glPushMatrix();
                GL11.glTranslatef(loc.x, loc.y, 0.0f);
                GL11.glRotatef(arg, 0f, 0f, 0.1f);
                GL11.glEnable(GL11.GL_TEXTURE_2D);
                sprite.bindTexture();
                GL11.glEnable(GL11.GL_BLEND);
                Color color1 = new Color(253, 39, 89, (int)Math.floor(255*alpha));
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
                GL11.glColor4ub((byte) color1.getRed(), (byte) color1.getGreen(), (byte) color1.getBlue(), (byte) color1.getAlpha());

                GL11.glBegin(GL11.GL_QUAD_STRIP);

                for (int i = 0; i < 360; i++) {
                    GL11.glTexCoord2f(0.2f, 0.1f);
                    GL11.glVertex2f((size * (1f-noisesize) + noise[i] * size * noisesize) * (float) Math.cos(Math.toRadians(i)), (size * (1f-noisesize) + noise[i] * size * noisesize) * (float) Math.sin(Math.toRadians(i)));
                    GL11.glTexCoord2f(0.2f, 0.9f);
                    GL11.glVertex2f((size * noisesize*3f + size * (1f-noisesize) + noise[i] * size * noisesize) * (float) Math.cos(Math.toRadians(i)), (size * noisesize*3f + size * (1f-noisesize) + noise[i] * size * noisesize) * (float) Math.sin(Math.toRadians(i)));
                }
                GL11.glTexCoord2f(0.2f, 0.1f);
                GL11.glVertex2f((size * (1f-noisesize) + noise[0] * size * noisesize) * (float) Math.cos(Math.toRadians(0)), (size * (1f-noisesize) + noise[0] * size * noisesize) * (float) Math.sin(Math.toRadians(0)));
                GL11.glTexCoord2f(0.2f, 0.9f);
                GL11.glVertex2f((size * noisesize*3f + size * (1f-noisesize) + noise[0] * size * noisesize) * (float) Math.cos(Math.toRadians(0)), (size * noisesize*3f + size * (1f-noisesize) + noise[0] * size * noisesize) * (float) Math.sin(Math.toRadians(0)));

                GL11.glEnd();
                GL11.glPopMatrix();
            }
        }

    }

}
