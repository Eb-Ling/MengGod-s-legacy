package data.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.hullmods.Meng_Dragonheart;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.plugins.MagicTrailPlugin;

import java.awt.*;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;


public class Meng_Dragonfireeffect implements EveryFrameWeaponEffectPlugin {
    public static final String KEY = "Meng_Dragonheartlistener";
    private final IntervalUtil Interval = new IntervalUtil(0.3f, 0.3f);
    protected List<Meng_DragonfirePlugin> trails;
    float timer = 0f;
    float ids = MagicTrailPlugin.getUniqueID();
    private float lastchargelevel = 0f;
    private boolean init = false;
    private boolean init1 = false;
    private boolean init2 = false;
    private boolean doown = false;
    private boolean beam = false;

    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {

        ShipAPI ship = weapon.getShip();
        float chargeLevel = weapon.getChargeLevel();

        ShipAPI source = weapon.getShip();

        if (!ship.getCustomData().containsKey(KEY)) {
            Meng_Dragonheart.DataContainer data1 = new Meng_Dragonheart.DataContainer();
            ship.setCustomData(KEY, data1);
        }
        Meng_Dragonheart.DataContainer data1 = (Meng_Dragonheart.DataContainer) ship.getCustomData().get(KEY);

        if (chargeLevel <= 1f && chargeLevel > this.lastchargelevel) {
            timer += amount;
            if (chargeLevel >= 0.3f) {
                SpriteAPI sprite = Global.getSettings().getSprite("fx", "base_trail_smooth");
                float arg = weapon.getShip().getFacing() + timer * 480f;
                float arg1 = weapon.getShip().getFacing() + timer * 480f - 180f;
                float range = Math.max(180f * (1 - timer), 0f);
                Color color = new Color(100, 255, 240, 255);
                Vector2f loc1 = new Vector2f(weapon.getFirePoint(0).getX() + (float) Math.cos(Math.toRadians(arg1)) * range, weapon.getFirePoint(0).getY() + (float) Math.sin(Math.toRadians(arg1)) * range);

                Vector2f loc = new Vector2f(weapon.getFirePoint(0).getX() + (float) Math.cos(Math.toRadians(arg)) * range, weapon.getFirePoint(0).getY() + (float) Math.sin(Math.toRadians(arg)) * range);
                MagicTrailPlugin.addTrailMemberAdvanced(ship, ids, sprite
                        , loc, 0f, 0f, arg + 90f, 0f, 0f
                        , 80f * Math.max(1 - timer, 0f), 40f * Math.max(1 - timer, 0f), color, color,
                        0.8f, 0.05f, 0.1f, 0.6f,
                        false, 100f, 60, 0f, null,
                        null,
                        CombatEngineLayers.ABOVE_SHIPS_LAYER,
                        360f);
                MagicTrailPlugin.addTrailMemberAdvanced(ship, ids + 1, sprite
                        , loc1, 0f, 0f, arg + 90f, 0f, 0f
                        , 50f, 20f, color, color, 0.8f, 0.05f, 0.1f, 0.6f, false, 100f, 60, 0f, null,
                        null,
                        CombatEngineLayers.ABOVE_SHIPS_LAYER,
                        360f);
            }
            RippleDistortion ripple = new RippleDistortion(weapon.getLocation(), new Vector2f());
            if (chargeLevel <= 0.1f) if (data1.level > 4f) {
                beam = true;
                doown = true;
            }
            if (chargeLevel > 0.1f) {
                if (!init2) {
                    init2 = true;
                    if (!beam)
                        Global.getSoundPlayer().playSound("Meng_Dragonfire", 1f, 1.2f, ship.getLocation(), new Vector2f());
                    else
                        Global.getSoundPlayer().playSound("Meng_Dragonfire_X", 1f, 1.2f, ship.getLocation(), new Vector2f());
                }
            }
            if (chargeLevel > 0.3f) {

                ripple.setSize(100f);
                ripple.setIntensity(10f);
                ripple.fadeOutSize(0.8f);
                ripple.fadeOutIntensity(0.8f);
                ripple.setFrameRate(90f / 0.8f);

                if (!init) {
                    init = true;
                    init1 = false;
                    DistortionShader.addDistortion(ripple);
                }
            }

            if (beam) if (chargeLevel > 0.8f) {
                if (!init1) {
                    init1 = true;
                    Global.getCombatEngine().addLayeredRenderingPlugin(new Meng_DragonfPlugin(weapon));
                    beam = false;
                }
            }
            if (doown) if (chargeLevel > 0.2f) {
                data1.level = (1f - chargeLevel) * 5f;
                data1.damage = (1f - chargeLevel) * 50000f;
            }
            if (chargeLevel == 1f) {
                RippleDistortion ripple1 = new RippleDistortion(weapon.getLocation(), new Vector2f());
                ripple1.setSize(chargeLevel * 500.0F * 2.0F);
                ripple1.setIntensity(chargeLevel * 500.0F * 0.2F);
                ripple1.fadeInSize(12f);
                ripple1.fadeInIntensity(5f);
                ripple1.setFrameRate(60f);
                DistortionShader.addDistortion(ripple1);
                doown = false;
            }

        } else {
            init = false;
            init1 = false;
            beam = false;
            timer = 0f;
            doown = false;
            init2 = false;
        }
        this.lastchargelevel = chargeLevel;

    }

    public static class Meng_DragonfPlugin implements CombatLayeredRenderingPlugin {
        private final WeaponAPI weapons;
        private final IntervalUtil interval = new IntervalUtil(0.05f, 0.05f);
        protected List<Meng_DragonfirePlugin> trails;
        private float timer1;


        public Meng_DragonfPlugin(WeaponAPI weapon) {
            weapons = weapon;
        }

        public void init(CombatEntityAPI entity) {

        }

        @Override
        public void cleanup() {
        }

        @Override
        public boolean isExpired() {
            return timer1 >= 5f;//返回值为true时，Plugin删除，因此当计时器超过5秒后删除。
        }

        @Override
        public void advance(float amount) {
            timer1 += amount;
            interval.advance(amount);

            CombatEngineAPI engine = Global.getCombatEngine();
            for (DamagingProjectileAPI proj : engine.getProjectiles()) {
                if (proj.getWeapon() == weapons && proj.getProjectileSpecId().equals("Meng_dragonfire")) {
                    engine.removeEntity(proj);
                }
            }
            float arg;
            if (Math.floor(timer1) % 2 == 1) {
                arg = weapons.getShip().getFacing() + 3f - 6f * (timer1 % 1);
            } else {
                arg = weapons.getShip().getFacing() - 3f + 6f * (timer1 % 1);
            }
            if (interval.intervalElapsed()) {
                DamagingProjectileAPI projectile = (DamagingProjectileAPI) engine.spawnProjectile(weapons.getShip(), weapons, "Meng_Dragonfire_Max", weapons.getFirePoint(0), arg, weapons.getShip().getVelocity());
                Meng_DragonfirePlugin trail = new Meng_DragonfirePlugin(trails, weapons, projectile);
                CombatEntityAPI e = engine.addLayeredRenderingPlugin(trail);
                e.getLocation().set(projectile.getLocation());

                if (trails == null) {
                    trails = new ArrayList<Meng_DragonfirePlugin>();
                }
                trails.add(0, trail);
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

    public static class Meng_DragonfirePlugin extends BaseCombatLayeredRenderingPlugin implements CombatLayeredRenderingPlugin {
        private final WeaponAPI weapons;
        protected List<Meng_DragonfirePlugin> trails;
        protected List<ParticleData> particles = new ArrayList<ParticleData>();
        protected DamagingProjectileAPI proj;
        protected float baseFacing = 0f;
        protected EnumSet<CombatEngineLayers> layers = EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER);
        private float timer1;

        public Meng_DragonfirePlugin(List<Meng_DragonfirePlugin> trail, WeaponAPI weapon, DamagingProjectileAPI proj) {
            this.proj = proj;
            weapons = weapon;
            trails = trail;

            baseFacing = proj.getFacing();

            int num = 7;
            for (int i = 0; i < num; i++) {
                particles.add(new ParticleData(proj));
            }

            float length = proj.getProjectileSpec().getLength();
            float width = proj.getProjectileSpec().getWidth();

            float index = 0;
            for (ParticleData p : particles) {
                float f = index / (particles.size() - 1);
                Vector2f dir = Misc.getUnitVectorAtDegreeAngle(proj.getFacing() + 180f);
                dir.scale(length * f);
                Vector2f.add(p.offset, dir, p.offset);

                p.offset = Misc.getPointWithinRadius(p.offset, width * 0.5f);
                //p.scale = 0.25f + 0.75f * (1 - f);

                index++;
            }

        }

        public void init(CombatEntityAPI entity) {
            super.init(entity);
        }


        @Override
        public void cleanup() {
        }

        @Override
        public boolean isExpired() {
            return proj.isExpired() || !Global.getCombatEngine().isEntityInPlay(proj);
        }

        @Override
        public void advance(float amount) {
            if (trails == null) return;

            Iterator<Meng_DragonfirePlugin> iter = trails.iterator();
            while (iter.hasNext()) {
                if (iter.next().isExpired()) iter.remove();
            }

            // sound loop playback
            if (weapons.getShip() != null) {
                float maxRange = weapons.getRange();
                ShipAPI ship = weapons.getShip();
                Vector2f com = new Vector2f();
                float weight = 0f;//亮度权重，计算音量
                float totalDist = 0f;
                Vector2f source = weapons.getLocation();
                for (Meng_DragonfirePlugin curr : trails) {
                    if (curr.proj != null) {
                        Vector2f.add(com, curr.proj.getLocation(), com);
                        weight += curr.proj.getBrightness();
                        totalDist += Misc.getDistance(source, curr.proj.getLocation());//所有弹体的总距离
                    }
                }
                if (weight > 0.1f) {
                    com.scale(1f / weight);
                    float volume = Math.min(weight, 1f);
                    if (trails.size() > 0) {
                        totalDist /= (float) trails.size();//计算平均单个弹体的距离
                        float mult = totalDist / Math.max(maxRange, 1f);//计算平均距离与总路径之比
                        mult = 1f - mult;
                        if (mult > 1f) mult = 1f;//
                        if (mult < 0f) mult = 0f;//超出距离不再播放声音
                        mult = (float) Math.sqrt(mult);
                        volume *= mult;
                    }
                    Global.getSoundPlayer().playLoop("cryoflamer_loop", ship, 1f, volume, com, ship.getVelocity());
                }
            }

            timer1 += amount;
            CombatEngineAPI engine = Global.getCombatEngine();
            if (Global.getCombatEngine().isPaused()) return;

            entity.getLocation().set(proj.getLocation());

            for (ParticleData p : particles) {
                p.advance(amount);
            }
        }

        @Override
        public EnumSet<CombatEngineLayers> getActiveLayers() {
            return layers;
        }

        @Override
        public float getRenderRadius() {
            return 300f;
        }

        @Override
        public void render(CombatEngineLayers layer, ViewportAPI viewport) {
            float x = entity.getLocation().x;
            float y = entity.getLocation().y;

            //Color color = new Color(100,150,255,50);
            Color color = proj.getProjectileSpec().getFringeColor();
            color = Misc.setAlpha(color, 50);
            float b = proj.getBrightness();
            b *= viewport.getAlphaMult();

            for (ParticleData p : particles) {
                float size = proj.getProjectileSpec().getWidth() * 0.7f;
                size *= p.scale;

                float alphaMult = 1f;
                Vector2f offset = p.offset;
                float diff = Misc.getAngleDiff(baseFacing, proj.getFacing());
                if (Math.abs(diff) > 0.1f) {
                    offset = Misc.rotateAroundOrigin(offset, diff);
                }
                Vector2f loc = new Vector2f(x + offset.x, y + offset.y);

                p.sprite.setAngle(p.angle);
                p.sprite.setSize(size, size);
                p.sprite.setAlphaMult(b * alphaMult * p.fader.getBrightness());
                p.sprite.setColor(color);
                p.sprite.renderAtCenter(loc.x, loc.y);
            }
        }

        public static class ParticleData {
            public SpriteAPI sprite;
            public Vector2f offset = new Vector2f();
            public Vector2f vel = new Vector2f();
            public float scale = 1f;
            public DamagingProjectileAPI proj;
            public float scaleIncreaseRate = 1f;
            public float turnDir = 1f;
            public float angle = 1f;
            public FaderUtil fader;

            public ParticleData(DamagingProjectileAPI proj) {
                this.proj = proj;
                sprite = Global.getSettings().getSprite("misc", "nebula_particles");
                //sprite = Global.getSettings().getSprite("misc", "dust_particles");
                float i = Misc.random.nextInt(4);
                float j = Misc.random.nextInt(4);
                sprite.setTexWidth(0.25f);
                sprite.setTexHeight(0.25f);
                sprite.setTexX(i * 0.25f);
                sprite.setTexY(j * 0.25f);
                sprite.setAdditiveBlend();

                angle = (float) Math.random() * 360f;

                float maxDur = proj.getWeapon().getRange() / proj.getWeapon().getProjectileSpeed();//使用距离除以速度得出飞行时间
                scaleIncreaseRate = 3f / maxDur;//设定与飞行时间挂钩的特效扩张速度
                scale = 0f;
//			scale = 0.1f;
//			scaleIncreaseRate = 2.9f / maxDur;
//			scale = 0.1f;
//			scaleIncreaseRate = 2.5f / maxDur;
//			scale = 0.5f;

                turnDir = Math.signum((float) Math.random() - 0.5f) * 60f * (float) Math.random();
                //turnDir = 0f;

                float driftDir = (float) Math.random() * 360f;
                vel = Misc.getUnitVectorAtDegreeAngle(driftDir);
                vel.scale(proj.getProjectileSpec().getWidth() / maxDur * 0.33f);

//			offset.x += vel.x * 1f;
//			offset.y += vel.y * 1f;
                fader = new FaderUtil(0f, 0.25f, 0.5f);
                fader.fadeIn();
            }

            public void advance(float amount) {
                scale += scaleIncreaseRate * amount;
                if (scale < 1f) {
                    scale += 2 * scaleIncreaseRate * amount * 1f;
                }//貌似多此一举

                offset.x += vel.x * amount;
                offset.y += vel.y * amount;

                angle += turnDir * amount;

                fader.advance(amount);
            }
        }

    }
}

