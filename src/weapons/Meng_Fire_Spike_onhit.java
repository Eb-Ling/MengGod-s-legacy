package data.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.util.IntervalUtil;
import data.methods.Meng_arcfind;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.plugins.MagicRenderPlugin;
import org.magiclib.plugins.MagicTrailPlugin;
import org.magiclib.util.MagicRender;

import java.awt.*;
import java.util.EnumSet;

public class Meng_Fire_Spike_onhit implements OnHitEffectPlugin {
    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (target instanceof ShipAPI t) {
            if (!shieldHit) return;
            float arg = Meng_arcfind.Findarc(t.getLocation(), point);
            float shieldarg = arg - t.getShield().getFacing();
            float radius = MathUtils.getDistance(t.getLocation(), point);
            engine.addLayeredRenderingPlugin(new Meng_Fire_Spike_onhit_Plugin(engine, shieldarg, t, projectile, radius));
        }

    }
    public static class Meng_Fire_Spike_onhit_Plugin implements CombatLayeredRenderingPlugin {
        private final ShipAPI t;
        private float shielda;
        private final IntervalUtil interval=new IntervalUtil(1f,1f);
        private DamagingProjectileAPI proj;
        private ShipAPI ship;
        private float r;
        private float timer=0f;
        private CombatEngineAPI engine;
        private float damage;
        public Meng_Fire_Spike_onhit_Plugin(CombatEngineAPI eng,float shieldarg,ShipAPI target,DamagingProjectileAPI projectile,float radius) {
            t = target;
            shielda=shieldarg;
            proj=projectile;
            r=radius;
            engine=eng;
        }

        public void init(CombatEntityAPI entity) {
            damage = proj.getDamageAmount()*0.25f;
            ship=proj.getSource();
        }

        @Override
        public void cleanup() {
        }

        @Override
        public boolean isExpired() {
            return t.getShield().getActiveArc()<=2f*Math.abs(shielda);//返回值为true时，Plugin删除，因此当计时器超过三秒后删除。
        }

        @Override
        public void advance(float amount) {
            timer+=amount;
            float currentarg=shielda+t.getShield().getFacing();
            Vector2f loc=new Vector2f(t.getLocation().x+(float)Math.cos(Math.toRadians(currentarg))*r,t.getLocation().y+(float) Math.sin(Math.toRadians(currentarg))*r);
            SpriteAPI sprite= Global.getSettings().getSprite("fx","Meng_spike_flash");
            MagicRenderPlugin.addSingleframe(sprite,loc,CombatEngineLayers.ABOVE_SHIPS_LAYER);
            sprite.setAngle(timer*60f);
            interval.advance(amount);
            if(interval.intervalElapsed()){
                CombatEngineAPI engine=Global.getCombatEngine();
                engine.addLayeredRenderingPlugin(new Meng_Fire_Spike_flashring(loc));
                Color color1 = new Color(250, 40, 40, 255);
                Color color2 = new Color(255, 200, 200, 200);
                float size=50f;
                DamagingExplosionSpec spc = new DamagingExplosionSpec(
                        0.5f,
                        size,
                        size ,
                        damage,
                        damage,
                        CollisionClass.PROJECTILE_FF,
                        CollisionClass.PROJECTILE_FIGHTER, // irrelevant - no explosion for fighters
                        size * 0.06f,
                        size * 0.08f,
                        size * 0.005f,
                        Math.round(size * 0.15f),
                        color1, color2);
                spc.setDamageType(DamageType.KINETIC);
                spc.setShowGraphic(false);
                engine.spawnDamagingExplosion(spc, ship, loc);
                RippleDistortion ripple = new RippleDistortion(loc, new Vector2f());
                ripple.setSize(size);
                ripple.setIntensity(size * 0.25f);
                ripple.fadeOutSize(4f);
                ripple.fadeOutIntensity(2f);
                ripple.setFrameRate(90f);
                DistortionShader.addDistortion(ripple);
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
    public static class Meng_Fire_Spike_flashring implements CombatLayeredRenderingPlugin {
        private Vector2f loc;
        private float timer=0f;
        public Meng_Fire_Spike_flashring(Vector2f location) {
           loc=location;
        }

        public void init(CombatEntityAPI entity) {

        }

        @Override
        public void cleanup() {
        }

        @Override
        public boolean isExpired() {
            return timer>=1.05f;//返回值为true时，Plugin删除，因此当计时器超过三秒后删除。
        }

        @Override
        public void advance(float amount) {
            timer+=2F*amount;
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
                if(timer>=1f) return;
                float sizes = 30f*timer;

                float width = 4f;

                for (int i = 0; i < 360; i++) {
                    Vector2f locs1 = new Vector2f(loc.getX() + sizes * (float) Math.cos(Math.toRadians(i)), loc.getY() + sizes * (float) Math.sin(Math.toRadians(i)));
                    Vector2f locs2 = new Vector2f(loc.getX() + (width + sizes) * (float) Math.cos(Math.toRadians(i)), loc.getY() + sizes * (float) Math.sin(Math.toRadians(i)));
                    Vector2f locs3 = new Vector2f(loc.getX() + sizes * (float) Math.cos(Math.toRadians(i + 1)), loc.getY() + sizes * (float) Math.sin(Math.toRadians(i + 1)));
                    Vector2f locs4 = new Vector2f(loc.getX() + (width + sizes) * (float) Math.cos(Math.toRadians(i + 1)), loc.getY() + sizes * (float) Math.sin(Math.toRadians(i + 1)));
                    GL11.glPushMatrix();
                    GL11.glTranslatef(0.0f, 0.0f, 0.0f);
                    GL11.glRotatef(0f, 0f, 0f, 0.1f);
                    GL11.glDisable(GL11.GL_TEXTURE_2D);
                    GL11.glEnable(GL11.GL_BLEND);

                    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
                    Color c = new Color(255, 50, 50, 255);
                    GL11.glColor4ub((byte) c.getRed(), (byte) c.getGreen(), (byte) c.getBlue(), (byte) c.getAlpha());

                    GL11.glBegin(GL11.GL_QUADS);

                    GL11.glVertex2f(locs1.x, locs1.y);
                    GL11.glVertex2f(locs2.x, locs2.y);
                    GL11.glVertex2f(locs4.x, locs4.y);
                    GL11.glVertex2f(locs3.x, locs3.y);

                    GL11.glEnd();
                    GL11.glPopMatrix();
                }
            }
        }


    }
}
