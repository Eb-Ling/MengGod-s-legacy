package data.weapons;


import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.CombatEntityPluginWithParticles;
import com.fs.starfarer.api.impl.combat.RiftLanceEffect;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lazywizard.lazylib.combat.entities.SimpleEntity;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * IMPORTANT: will be multiple instances of this, as this doubles as the every frame effect and the on fire effect (same instance)
 * But also as the visual for each individual shot (created via onFire, using the non-default constructor)
 */
public class Meng_Realityeffect_1 extends CombatEntityPluginWithParticles {

    public static int MAX_ARC_RANGE = 300;
    public static Color UNDERCOLOR = new Color(50, 106, 224, 255);
    //public static int ARCS_ON_HIT = 15;
    public static Color RIFT_COLOR = new Color(22, 155, 152, 255);
    private final List<CombatEntityAPI> alreadyDamagedTargets = new ArrayList<CombatEntityAPI>();
    protected WeaponAPI weapon;
    protected DamagingProjectileAPI proj;
    protected IntervalUtil interval = new IntervalUtil(0.1f, 0.2f);
    protected IntervalUtil arcInterval = new IntervalUtil(0.17f, 0.23f);
    protected float delay = 1f;
    private float damageThisShot = 0f;
    private float empFactor = 0f;

    public Meng_Realityeffect_1(WeaponAPI weapon) {
        super();
        this.weapon = weapon;
        arcInterval = new IntervalUtil(0.17f, 0.23f);
        delay = 0.5f;
        setSpriteSheetKey("fx_particles2");
    }

    public static boolean isProjectileExpired(DamagingProjectileAPI proj) {
        return proj.isExpired() || proj.didDamage() || !Global.getCombatEngine().isEntityInPlay(proj);
    }

    public static boolean isWeaponCharging(WeaponAPI weapon) {
        return weapon.getChargeLevel() > 0 && weapon.getCooldownRemaining() <= 0;
    }

    public void attachToProjectile(DamagingProjectileAPI proj) {
        this.proj = proj;
    }

    public void advance(float amount) {
        Vector2f point = weapon.getLocation();


        ShipAPI source;
        if (weapon.isFiring()) {
            CombatEngineAPI engine = Global.getCombatEngine();
            Random random = new Random();
            float radius1 = random.nextFloat() * 60.0f;
            source = weapon.getShip();
            float angle = random.nextFloat() * 360.0F;
            Vector2f point1 = MathUtils.getPointOnCircumference(point, radius1, angle);
            Vector2f point2 = MathUtils.getPointOnCircumference(point, radius1, angle + 60.0F);
            if (weapon.getShip().getHitpoints() >= weapon.getShip().getMaxHitpoints() * 0.3) {
                engine.spawnEmpArc(source, point, new SimpleEntity(point1), new SimpleEntity(point2), DamageType.ENERGY, 0f, 0f, 150.0F, null, MathUtils.getRandomNumberInRange(10.0F, 20.0F), new Color(84, 1, 110, 185), new Color(100, 255, 240, 100));

                source.setHitpoints(source.getHitpoints() - 150f * amount);
            }
        }


        if (Global.getCombatEngine().isPaused()) return;
        if (proj != null) {
            entity.getLocation().set(proj.getLocation());
        } else {
            entity.getLocation().set(weapon.getFirePoint(0));
        }
        super.advance(amount);

        boolean keepSpawningParticles = isWeaponCharging(weapon) ||
                (proj != null && !isProjectileExpired(proj) && !proj.isFading());
        if (keepSpawningParticles) {
            interval.advance(amount);
            if (interval.intervalElapsed()) {
                addChargingParticles(weapon);
            }
        }

        if (proj != null && !isProjectileExpired(proj) && !proj.isFading()) {
            delay -= amount;
            if (delay <= 0) {
                arcInterval.advance(amount);
                if (arcInterval.intervalElapsed()) {

                    spawnArc();
                }
            }
        }
        if (proj != null) {
            Global.getSoundPlayer().playLoop("realitydisruptor_loop", proj, 1f, proj.getBrightness(),
                    proj.getLocation(), proj.getVelocity());
        }

//		if (proj != null) {
//			proj.setFacing(proj.getFacing() + 30f * amount);
//		}
    }

    @Override
    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        // pass in proj as last argument to have particles rotate
        super.render(layer, viewport, null);
    }

    public boolean isExpired() {
        boolean keepSpawningParticles = isWeaponCharging(weapon) ||
                (proj != null && !isProjectileExpired(proj) && !proj.isFading());
        return super.isExpired() && (!keepSpawningParticles || (!weapon.getShip().isAlive() && proj == null));
    }

    public float getRenderRadius() {
        return 500f;
    }

    @Override
    protected float getGlobalAlphaMult() {
        if (proj != null && proj.isFading()) {
            return proj.getBrightness();
        }
        return super.getGlobalAlphaMult();
    }

    public void spawnArc() {
        CombatEngineAPI engine = Global.getCombatEngine();
        CombatEntityAPI target = findTarget(proj, weapon, engine);

        float thickness = 30f;
        float coreWidthMult = 0.67f;
        Color color = new Color(51, 0, 77, 255);

        if (target != null) {
            if (weapon.getShip().getHitpoints() >= weapon.getShip().getMaxHitpoints() * 0.5f) {
                damageThisShot = 1200f;
                empFactor = 5f;
                alreadyDamagedTargets.clear();
                CombatEntityAPI firstTarget = null;

                float range = 600f;
                firstTarget = target;

                //If we didn't find a target on the line, the shot was a dud: spawn a decorative EMP arc to the end destination

                //Initializes values for our loop's first iteration
                CombatEntityAPI currentTarget = firstTarget;
                CombatEntityAPI previousTarget = weapon.getShip();
                Vector2f firingPoint = proj.getLocation();

                //Run a repeating loop to find new targets and deal damage to them in a chain
                while (damageThisShot > 1f) {

                    CombatEntityAPI nextTarget = null;
                    float tempStorage = 0f;

                    tempStorage = Math.max(damageThisShot - 300f, 0);


                    //Finds a new target, in case we are going to overkill our current one
                    List<CombatEntityAPI> targetList = CombatUtils.getEntitiesWithinRange(currentTarget.getLocation(), range);
                    for (CombatEntityAPI potentialTarget : targetList) {
                        //Checks for dissallowed targets, and ignores them
                        if (!(potentialTarget instanceof ShipAPI) && !(potentialTarget instanceof MissileAPI)) {
                            continue;
                        }
                        if (potentialTarget.getOwner() == weapon.getShip().getOwner()) {
                            continue;
                        }
                        if ((potentialTarget instanceof ShipAPI)) {
                            if (((ShipAPI) potentialTarget).isPhased()) {
                                continue;
                            }
                        }
                        if (alreadyDamagedTargets.contains(potentialTarget)) {
                            continue;
                        }

                        //If we found any applicable targets, pick the closest one
                        if (nextTarget == null) {
                            nextTarget = potentialTarget;
                        } else if (MathUtils.getDistance(nextTarget, currentTarget) > MathUtils.getDistance(potentialTarget, currentTarget)) {
                            nextTarget = potentialTarget;
                        }
                    }

                    //If we didn't find any targets, the lightning stops here
                    if (nextTarget == null) {
                        tempStorage = 0f;
                    }

                    //Sets our previous target to our current one (before damaging it, that is)
                    CombatEntityAPI tempPreviousTarget = previousTarget;
                    previousTarget = currentTarget;

                    //If our target is a missile, *and* our EMP is not higher than 1/3rd of the missile's HP, we don't count as an EMP arc hit; increase the EMP resistance by one before firing the arc
                    if (currentTarget instanceof MissileAPI) {
                        if (damageThisShot * empFactor < currentTarget.getHitpoints() / 3f) {
                            ((MissileAPI) currentTarget).setEmpResistance(((MissileAPI) currentTarget).getEmpResistance() + 1);
                        }
                    }
                    EmpArcEntityAPI arc;
                    if (currentTarget instanceof ShipAPI) {
                        arc = Global.getCombatEngine().spawnEmpArc(weapon.getShip(), firingPoint, tempPreviousTarget, currentTarget,
                                DamageType.HIGH_EXPLOSIVE, //Damage type
                                300f, //Damage
                                0.5f * empFactor, //Emp
                                100000f, //Max range
                                "tachyon_lance_emp_impact", //Impact sound
                                thickness, // thickness of the lightning bolt
                                color,
                                new Color(255, 255, 255, 255)
                        );
                    } else {
                        //Actually spawn the lightning arc
                        arc = Global.getCombatEngine().spawnEmpArc(weapon.getShip(), firingPoint, tempPreviousTarget, currentTarget,
                                DamageType.HIGH_EXPLOSIVE, //Damage type
                                300f, //Damage
                                0.5f * empFactor, //Emp
                                100000f, //Max range
                                "tachyon_lance_emp_impact", //Impact sound
                                thickness, // thickness of the lightning bolt
                                color,
                                new Color(255, 255, 255, 255)
                        );

                        //A second decorative arc
                        Global.getCombatEngine().spawnEmpArc(weapon.getShip(), firingPoint, tempPreviousTarget, currentTarget,
                                DamageType.HIGH_EXPLOSIVE, //Damage type
                                300f, //Damage
                                0.5f * empFactor, //Emp
                                100000f, //Max range
                                "tachyon_lance_emp_impact", //Impact sound
                                thickness, // thickness of the lightning bolt
                                color,
                                new Color(255, 255, 255, 255)
                        );
                    }
                    arc.setCoreWidthOverride(thickness * coreWidthMult);

                    spawnEMPParticles(EMPArcHitType.SOURCE, proj.getLocation(), null);
                    spawnEMPParticles(EMPArcHitType.DEST, arc.getTargetLocation(), target);


                    //Adjusts variables for the next iteration
                    firingPoint = previousTarget.getLocation();
                    damageThisShot = tempStorage;
                    alreadyDamagedTargets.add(nextTarget);
                    currentTarget = nextTarget;
                }
            } else {
                EmpArcEntityAPI arc = engine.spawnEmpArc(proj.getSource(), proj.getLocation(), null,
                        target,
                        DamageType.ENERGY,
                        100f,
                        200f, // emp
                        100000f, // max range
                        "realitydisruptor_emp_impact",
                        thickness, // thickness
                        color,
                        new Color(255, 255, 255, 255)
                );
                arc.setCoreWidthOverride(thickness * coreWidthMult);

                spawnEMPParticles(EMPArcHitType.SOURCE, proj.getLocation(), null);
                spawnEMPParticles(EMPArcHitType.DEST, arc.getTargetLocation(), target);
            }
        } else {
            Vector2f from = new Vector2f(proj.getLocation());
            Vector2f to = pickNoTargetDest(proj, weapon, engine);
            EmpArcEntityAPI arc = engine.spawnEmpArcVisual(from, null, to, null, thickness, color, Color.white);
            arc.setCoreWidthOverride(thickness * coreWidthMult);
            Global.getSoundPlayer().playSound("realitydisruptor_emp_impact", 1f, 1f, to, new Vector2f());

            spawnEMPParticles(EMPArcHitType.SOURCE, from, null);
            spawnEMPParticles(EMPArcHitType.DEST_NO_TARGET, to, null);
        }
    }

    public Vector2f pickNoTargetDest(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        float range = 200f;
        Vector2f from = projectile.getLocation();
        Vector2f dir = Misc.getUnitVectorAtDegreeAngle((float) Math.random() * 360f);
        dir.scale(range);
        Vector2f.add(from, dir, dir);
        dir = Misc.getPointWithinRadius(dir, range * 0.25f);
        return dir;
    }

    public CombatEntityAPI findTarget(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        float range = MAX_ARC_RANGE;
        Vector2f from = projectile.getLocation();

        Iterator<Object> iter = Global.getCombatEngine().getAllObjectGrid().getCheckIterator(from,
                range * 2f, range * 2f);
        int owner = weapon.getShip().getOwner();
        CombatEntityAPI best = null;
        float minScore = Float.MAX_VALUE;
        while (iter.hasNext()) {
            Object o = iter.next();
            if (!(o instanceof MissileAPI) &&
                    //!(o instanceof CombatAsteroidAPI) &&
                    !(o instanceof ShipAPI)) continue;
            CombatEntityAPI other = (CombatEntityAPI) o;
            if (other.getOwner() == owner) continue;

            if (other instanceof ShipAPI) {
                ShipAPI otherShip = (ShipAPI) other;
                if (otherShip.isHulk()) continue;
                if (otherShip.isPhased()) continue;
            }
            if (other.getCollisionClass() == CollisionClass.NONE) continue;

            float radius = Misc.getTargetingRadius(from, other, false);
            float dist = Misc.getDistance(from, other.getLocation()) - radius - 50f;
            if (dist > range) continue;

            //float angleTo = Misc.getAngleInDegrees(from, other.getLocation());
            //float score = Misc.getAngleDiff(weapon.getCurrAngle(), angleTo);
            float score = dist;

            if (score < minScore) {
                minScore = score;
                best = other;
            }
        }
        return best;
    }

    public void addChargingParticles(WeaponAPI weapon) {
        //CombatEngineAPI engine = Global.getCombatEngine();
        Color color = RiftLanceEffect.getColorForDarkening(RIFT_COLOR);

//		float b = 1f;
//		color = Misc.scaleAlpha(color, b);
        //undercolor = Misc.scaleAlpha(undercolor, b);

        float size = 50f;
        float underSize = 75f;
        //underSize = 100f;

        float in = 0.25f;
        float out = 0.75f;

        out *= 3f;

        float velMult = 0.2f;

        if (isWeaponCharging(weapon)) {
            size *= 0.25f + weapon.getChargeLevel() * 0.75f;
        }

        addDarkParticle(size, in, out, 1f, size * 0.5f * velMult, 0f, color);
        randomizePrevParticleLocation(size * 0.33f);

        if (proj != null) {
            Vector2f dir = Misc.getUnitVectorAtDegreeAngle(proj.getFacing() + 180f);
            //size = 40f;
            if (proj.getElapsed() > 0.2f) {
                addDarkParticle(size, in, out, 1.5f, size * 0.5f * velMult, 0f, color);
                Vector2f offset = new Vector2f(dir);
                offset.scale(size * 0.6f + (float) Math.random() * 0.2f);
                Vector2f.add(prev.offset, offset, prev.offset);
            }
            if (proj.getElapsed() > 0.4f) {
                addDarkParticle(size, in, out, 1.3f, size * 0.5f * velMult, 0f, color);
                Vector2f offset = new Vector2f(dir);
                offset.scale(size * 1.2f + (float) Math.random() * 0.2f);
                Vector2f.add(prev.offset, offset, prev.offset);
            }
            if (proj.getElapsed() > 0.6f) {
                addDarkParticle(size * .8f, in, out, 1.1f, size * 0.5f * velMult, 0f, color);
                Vector2f offset = new Vector2f(dir);
                offset.scale(size * 1.6f + (float) Math.random() * 0.2f);
                Vector2f.add(prev.offset, offset, prev.offset);
            }

            if (proj.getElapsed() > 0.8f) {
                addDarkParticle(size * .8f, in, out, 1.1f, size * 0.5f * velMult, 0f, color);
                Vector2f offset = new Vector2f(dir);
                offset.scale(size * 2.0f + (float) Math.random() * 0.2f);
                Vector2f.add(prev.offset, offset, prev.offset);
            }
//			int num = (int) Math.round(proj.getElapsed() / 0.5f * 10f);
//			if (num > 15) num = 15;
//			for (int i = 0; i < num; i++) {
//				addDarkParticle(size, in, out, 1f, size * 0.5f, 0f, color);
//				Vector2f offset = new Vector2f(dir);
//				offset.scale(size * 0.1f * i);
//				Vector2f.add(prev.offset, offset, prev.offset);
//			}
        }

//		UNDERCOLOR = new Color(100, 0, 100, 100);
//		UNDERCOLOR = NSProjEffect.EXPLOSION_UNDERCOLOR;

        // defaults:
        //public static Color EXPLOSION_UNDERCOLOR = new Color(100, 0, 25, 100);
        //public static Color STANDARD_RIFT_COLOR = new Color(100,60,255,255);

        //"glowColor":[100,200,255,255], #ion cannon
//		RIFT_COLOR = new Color(100, 200, 255, 255);

//		UNDERCOLOR = NSProjEffect.EXPLOSION_UNDERCOLOR;
//		RIFT_COLOR = NSProjEffect.STANDARD_RIFT_COLOR;

//		UNDERCOLOR = new Color(255, 0, 25, 100);
//		UNDERCOLOR = new Color(100, 0, 25, 100);

        addParticle(underSize * 0.5f, in, out, 1.5f * 3f, 0f, 0f, UNDERCOLOR);
        randomizePrevParticleLocation(underSize * 0.67f);
        addParticle(underSize * 0.5f, in, out, 1.5f * 3f, 0f, 0f, UNDERCOLOR);
        randomizePrevParticleLocation(underSize * 0.67f);

//		float facing = weapon.getCurrAngle();
//		if (proj != null) facing = proj.getFacing();
//		Vector2f dir = Misc.getUnitVectorAtDegreeAngle(facing + 210f * ((float) Math.random() - 0.5f));
//		dir.scale(underSize * 0.25f * (float) Math.random());
//		Vector2f.add(prev.offset, dir, prev.offset);
    }

    public void spawnEMPParticles(EMPArcHitType type, Vector2f point, CombatEntityAPI target) {
        CombatEngineAPI engine = Global.getCombatEngine();

        Color color = RiftLanceEffect.getColorForDarkening(RIFT_COLOR);

        float size = 30f;
        float baseDuration = 1.5f;
        Vector2f vel = new Vector2f();
        int numNegative = 5;
        switch (type) {
            case DEST:
                size = 50f;
                vel.set(target.getVelocity());
                break;
            case DEST_NO_TARGET:
                break;
            case SOURCE:
                size = 40f;
                numNegative = 10;
                break;
        }
        Vector2f dir = Misc.getUnitVectorAtDegreeAngle(proj.getFacing() + 180f);
        //dir.negate();
        //numNegative = 0;
        for (int i = 0; i < numNegative; i++) {
            float dur = baseDuration + baseDuration * (float) Math.random();
            //float nSize = size * (1f + 0.0f * (float) Math.random());
            //float nSize = size * (0.75f + 0.5f * (float) Math.random());
            float nSize = size;
            if (type == EMPArcHitType.SOURCE) {
                nSize *= 1.5f;
            }
            Vector2f pt = Misc.getPointWithinRadius(point, nSize * 0.5f);
            Vector2f v = Misc.getUnitVectorAtDegreeAngle((float) Math.random() * 360f);
            v.scale(nSize + nSize * (float) Math.random() * 0.5f);
            v.scale(0.2f);

            float endSizeMult = 2f;
            if (type == EMPArcHitType.SOURCE) {
                pt = Misc.getPointWithinRadius(point, nSize * 0f);
                Vector2f offset = new Vector2f(dir);
                offset.scale(size * 0.2f * i);
                Vector2f.add(pt, offset, pt);
                endSizeMult = 1.5f;
                v.scale(0.5f);
            }
            Vector2f.add(vel, v, v);

            float maxSpeed = nSize * 1.5f * 0.2f;
            float minSpeed = nSize * 1f * 0.2f;
            float overMin = v.length() - minSpeed;
            if (overMin > 0) {
                float durMult = 1f - overMin / (maxSpeed - minSpeed);
                if (durMult < 0.1f) durMult = 0.1f;
                dur *= 0.5f + 0.5f * durMult;
            }
            engine.addNegativeNebulaParticle(pt, v, nSize, endSizeMult,
                    //engine.addNegativeSwirlyNebulaParticle(pt, v, nSize * 1f, endSizeMult,
                    0.25f / dur, 0f, dur, color);
        }

        float dur = baseDuration;
        float rampUp = 0.5f / dur;
        color = UNDERCOLOR;
        for (int i = 0; i < 7; i++) {
            Vector2f loc = new Vector2f(point);
            loc = Misc.getPointWithinRadius(loc, size);
            float s = size * 4f * (0.5f + (float) Math.random() * 0.5f);
            engine.addSwirlyNebulaParticle(loc, vel, s, 1.5f, rampUp, 0f, dur, color, false);
        }
    }

    public enum EMPArcHitType {
        SOURCE,
        DEST,
        DEST_NO_TARGET,
    }


}






