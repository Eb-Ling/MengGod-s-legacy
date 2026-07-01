package data.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.loading.ProjectileSpawnType;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Noise;
import data.methods.Meng_arcfind;
import data.scripts.util.MagicLensFlare;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.entities.SimpleEntity;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.*;

import static org.dark.graphics.util.ShipColors.colorBlend;

public class Meng_Noname_WeaponOnHit implements BeamEffectPlugin {

    private static final String id = "Meng_bossweaponscript";

    private final IntervalUtil arcInterval = new IntervalUtil(0.04f, 0.07f);
    private final IntervalUtil visualInterval = new IntervalUtil(0.03F, 0.05F);
    private CombatEntityAPI beamtarget;
    private boolean init=false;
    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        if (beam.getBrightness() <= 0f) return;
        if (engine.isPaused()) return;

        WeaponAPI weapon = beam.getWeapon();
        ShipAPI source = beam.getSource();
        Vector2f point = weapon.getLocation();

        if(!init){
            init=true;
            beamtarget=new SimpleEntity(beam.getTo());
        }
        beamtarget.getLocation().set(beam.getTo());
        float effectFactor = amount * beam.getBrightness();


        Color color1 = new Color(17, 248, 248, 245);
        Color color2 = new Color(83, 134, 229, 245);


        beam.setCoreColor(new Color(255, 255, 255, 255));
        beam.setFringeColor(new Color(255, 255, 255, 255));


        visualInterval.advance(effectFactor);
        if (visualInterval.intervalElapsed()) {
            engine.spawnEmpArc(source, weapon.getLocation(), new SimpleEntity(weapon.getLocation()), beamtarget, DamageType.ENERGY, 0f, 0f, 1000000f, null, 15f, color1, color2);
        }
        arcInterval.advance(effectFactor);
        if(arcInterval.intervalElapsed()){
            engine.addLayeredRenderingPlugin(new Meng_NonameWeapon_hiteffectPlugin(beam, weapon, beamtarget));
        }
    }
    public static class Meng_NonameWeapon_hiteffectPlugin implements CombatLayeredRenderingPlugin {
        private BeamAPI beam;
        private WeaponAPI weapon;
        private CombatEntityAPI beamtarget;
        private float timer=0f;
        private float alpha=1f;
        private Vector2f point;
        private float angle;
        private final IntervalUtil Interval = new IntervalUtil(0.1F, 0.1F);
        private final IntervalUtil visualInterval = new IntervalUtil(0.2F, 0.3F);
        public Meng_NonameWeapon_hiteffectPlugin(BeamAPI beams, WeaponAPI weapons,CombatEntityAPI beamtargets) {
           beam=beams;
           weapon=weapons;
           beamtarget=beamtargets;

        }

        public void init(CombatEntityAPI entity) {
            Random random = new Random();
            Vector2f beamTo = beam.getRayEndPrevFrame();
            angle = weapon.getArcFacing()+weapon.getShip().getFacing()-30f+60f*random.nextFloat()-60f*random.nextFloat()+60f*random.nextFloat();
            float radius =1000f+1000f*random.nextFloat();
            point = MathUtils.getPointOnCircumference(beamTo, radius, angle);
            Interval.forceCurrInterval(0.1f);
            visualInterval.forceCurrInterval(0.2f);
        }

        @Override
        public void cleanup() {
        }

        @Override
        public boolean isExpired() {
            return timer >= 2f;//返回值为true时，Plugin删除，因此当计时器超过三秒后删除。
        }

        @Override
        public void advance(float amount) {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine.isPaused()) return;
            ShipAPI ship=weapon.getShip();
            if(ship==null||!ship.isAlive()) return;
            float effectFactor = amount * beam.getBrightness();
            timer+=amount;
            Color color1 = new Color(17, 248, 248, 205);
            Color color2 = new Color(83, 134, 229, 205);

            if(timer>=1.5f){
                alpha= Math.max(0f,alpha-amount*2f);
            }
            else {
                visualInterval.advance(effectFactor);

                if (visualInterval.intervalElapsed()) {
                    engine.spawnEmpArcPierceShields(ship, beamtarget.getLocation(), beamtarget, new SimpleEntity(point), DamageType.ENERGY, 777f, 777f, 1000000f, null, 15f, color1, color2);
                }
                Interval.advance(amount);
                if(Interval.intervalElapsed()) {
                    Random random = new Random();
                    float angles = angle - 60f + 120f * random.nextFloat();
                    float radiu = 500f + 1500f * random.nextFloat();
                    Vector2f points = MathUtils.getPointOnCircumference(point, radiu, angles);
                    ShipAPI target = null;
                    float range = 100000f;
                    for (ShipAPI s : engine.getShips()) {
                        if (s.getOwner() != weapon.getShip().getOwner()) {
                            if (MathUtils.isWithinRange(s.getLocation(), points, 300f + s.getShieldRadiusEvenIfNoShield())) {
                                if (MathUtils.getDistance(s.getLocation(), points) < range) {
                                    target = s;
                                    range = MathUtils.getDistance(s.getLocation(), points);
                                }
                            }
                        }
                    }
                    if(target!=null){
                        engine.spawnEmpArc(ship, point, new SimpleEntity(point), target, DamageType.ENERGY, 777f, 777f, 1000000f, null, 8f, color1, color2);
                    }
                    else {
                        engine.spawnEmpArc(ship, point, new SimpleEntity(point), new SimpleEntity(points), DamageType.ENERGY, 777f, 777f, 1000000f, null, 8f, color1, color2);
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
            if (layer == CombatEngineLayers.ABOVE_SHIPS_LAYER) {
                SpriteAPI sprite = Global.getSettings().getSprite("fx", "Meng_Beamb");
                GL11.glPushMatrix();
                GL11.glTranslatef(0.0f,0.0f, 0.0f);
                GL11.glRotatef(0f, 0f, 0f, 0.1f);
                GL11.glEnable(GL11.GL_TEXTURE_2D);
                sprite.bindTexture();
                GL11.glEnable(GL11.GL_BLEND);
                Color color1 = new Color(17, 248, 248, (int)Math.floor(255*beam.getBrightness()*alpha));
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
                GL11.glColor4ub((byte) color1.getRed(), (byte) color1.getGreen(), (byte) color1.getBlue(), (byte) color1.getAlpha());

                GL11.glBegin(GL11.GL_QUAD_STRIP);
                float width = 20f;
                Vector2f point1 = new Vector2f(point.x+0.5f*width*(float) Math.cos(Math.toRadians(angle+90f)),point.y+0.5f*width*(float) Math.sin(Math.toRadians(angle+90f)));
                Vector2f point2 = new Vector2f(point.x+0.5f*width*(float) Math.cos(Math.toRadians(angle-90f)),point.y+0.5f*width*(float) Math.sin(Math.toRadians(angle-90f)));
                Vector2f point3 = new Vector2f(beamtarget.getLocation().x+0.5f*width*(float) Math.cos(Math.toRadians(angle+90f)),beamtarget.getLocation().y+0.5f*width*(float) Math.sin(Math.toRadians(angle+90f)));
                Vector2f point4 = new Vector2f(beamtarget.getLocation().x+0.5f*width*(float) Math.cos(Math.toRadians(angle-90f)),beamtarget.getLocation().y+0.5f*width*(float) Math.sin(Math.toRadians(angle-90f)));

                GL11.glTexCoord2f(0f, 0f);
                GL11.glVertex2f(point1.x,point1.y);
                GL11.glTexCoord2f(0f, 1f);
                GL11.glVertex2f(point2.x,point2.y);
                GL11.glTexCoord2f(1f, 0f);
                GL11.glVertex2f(point3.x,point3.y);
                GL11.glTexCoord2f(1f, 1f);
                GL11.glVertex2f(point4.x,point4.y);

                GL11.glEnd();
                GL11.glPopMatrix();
            }
        }

    }

}