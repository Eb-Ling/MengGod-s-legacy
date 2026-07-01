package data.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.entities.SimpleEntity;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.plugins.MagicFakeBeamPlugin;
import org.magiclib.util.MagicFakeBeam;

import java.awt.*;
import java.util.Random;

import static org.dark.graphics.util.ShipColors.colorBlend;

public class Meng_bossweapon implements BeamEffectPlugin {

    private static final String id = "Meng_bossweaponscript";
    private static final float HP_DROP_PER_SECOND = 750f;
    private static final float HP_THRESHOLD = 0.2f;
    private final IntervalUtil arcInterval = new IntervalUtil(0.17f, 0.23f);
    private final IntervalUtil visualInterval = new IntervalUtil(0.08F, 0.12F);
    private final IntervalUtil mineInterval = new IntervalUtil(0.02F, 0.02F);

    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        if (beam.getBrightness() <= 0f) return;
        if (engine.isPaused()) return;

        WeaponAPI weapon = beam.getWeapon();
        ShipAPI source = beam.getSource();
        Vector2f point = weapon.getLocation();


        float effectFactor = amount * beam.getBrightness();

        arcInterval.advance(effectFactor);
        if (arcInterval.intervalElapsed()) {

            Random random = new Random();
            float radius1 = random.nextFloat() * 60.0f;
            source = weapon.getShip();
            float angle = random.nextFloat() * 360.0F;
            Vector2f point1 = MathUtils.getPointOnCircumference(point, radius1, angle);
            Vector2f point2 = MathUtils.getPointOnCircumference(point, radius1, angle + 60.0F);
            if (source.getHitpoints() >= source.getMaxHitpoints() * HP_THRESHOLD) {
                engine.spawnEmpArc(source, point, new SimpleEntity(point1), new SimpleEntity(point2), DamageType.ENERGY, 0f, 0f, 150.0F, null, MathUtils.getRandomNumberInRange(10.0F, 20.0F), new Color(255, 20, 70, 185), new Color(100, 255, 240, 100));
            }
        }

        Color color1 = new Color(17, 248, 248, 245);
        Color color2 = new Color(83, 134, 229, 245);
        Random random = new Random();
        Vector2f beamTo = beam.getRayEndPrevFrame();

        float angle = random.nextFloat() * 360.0F;
        float radius1 = random.nextFloat() * 180.0f;
        float radius2 = random.nextFloat() * 60f;
        Vector2f point1 = MathUtils.getPointOnCircumference(beamTo, radius1, angle);
        Vector2f point2 = MathUtils.getPointOnCircumference(beamTo, radius1, angle + 60.0F);
        Vector2f point3 = MathUtils.getPointOnCircumference(beamTo, radius2, angle);
        Vector2f point4 = MathUtils.getPointOnCircumference(beamTo, radius2, angle);
        Color color = colorBlend(color1, color2, random.nextFloat());

        if (source.getHitpoints() >= source.getMaxHitpoints() * HP_THRESHOLD) {
            source.setHitpoints(source.getHitpoints() - HP_DROP_PER_SECOND * effectFactor);

            beam.setCoreColor(new Color(255, 255, 255, 255));
            beam.setFringeColor(new Color(255, 255, 255, 255));

            mineInterval.advance(effectFactor);
            if (mineInterval.intervalElapsed()) {
                spawnMine(source, point3);
                spawnMine(source, point4);
            }

            visualInterval.advance(effectFactor);
            if (visualInterval.intervalElapsed()) {
                Global.getCombatEngine().spawnEmpArc(source, weapon.getLocation(), new SimpleEntity(weapon.getLocation()), new SimpleEntity(beam.getTo()), DamageType.ENERGY, 0f, 0f, 1000000f, null, 10f, new Color(176, 0, 0, 236), new Color(100, 255, 240, 100));
                engine.addNegativeNebulaParticle(MathUtils.getRandomPointOnLine(beam.getFrom(), beam.getTo()), new Vector2f(0.0F, 0.0F), MathUtils.getRandomNumberInRange(30.0F, 80.0F), 1.3F, 0.1F, 0.3F, MathUtils.getRandomNumberInRange(0.4F, 0.8F), new Color(100, 255, 240, 100));
                engine.addNebulaParticle(MathUtils.getRandomPointOnLine(beam.getFrom(), beam.getTo()), new Vector2f(0.0F, 0.0F), MathUtils.getRandomNumberInRange(30.0F, 80.0F), 1.3F, 0.1F, 0.3F, MathUtils.getRandomNumberInRange(0.4F, 0.8F), new Color(255, 20, 70, 185));

                if (Math.random() > 0.75D) {
                    engine.addNegativeNebulaParticle(beam.getFrom(), beam.getWeapon().getShip().getVelocity(), MathUtils.getRandomNumberInRange(50.0F, 70.0F), 1.3F, 0.1F, 0.3F, MathUtils.getRandomNumberInRange(0.4F, 0.8F), new Color(100, 255, 240, 100));
                }

                if ((beam.getDamageTarget() != null)) {
                    engine.addNegativeSwirlyNebulaParticle(beam.getTo(), new Vector2f(0.0F, 0.0F), MathUtils.getRandomNumberInRange(60.0F, 120.0F), 1.3F, 0.0F, 0.3F, MathUtils.getRandomNumberInRange(0.5F, 1.0F), new Color(100, 255, 240, 100));
                }

                engine.addSwirlyNebulaParticle(beam.getTo(), new Vector2f(0.0F, 0.0F), MathUtils.getRandomNumberInRange(60.0F, 120.0F), 1.3F, 0.0F, 0.3F, MathUtils.getRandomNumberInRange(0.5F, 1.0F), new Color(255, 20, 70, 185), false);
                engine.addSwirlyNebulaParticle(beam.getTo(), new Vector2f(0.0F, 0.0F), MathUtils.getRandomNumberInRange(60.0F, 120.0F), 1.3F, 0.0F, 0.3F, MathUtils.getRandomNumberInRange(0.5F, 1.0F), new Color(255, 20, 70, 185).brighter(), false);
            }
            beam.getDamage().getModifier().modifyMult(id, 0f);
        } else {
            beam.getDamage().getModifier().unmodifyFlat(id);

            engine.spawnEmpArc(source, point1, new SimpleEntity(point2), new SimpleEntity(point2), DamageType.ENERGY, 0f, 500f, 100f, "tachyon_lance_emp_impact", 20f, new Color(11, 220, 195, 236), new Color(176, 0, 0, 236));
            beam.setFringeTexture("graphics/fx/emp_arcs.png");
            beam.setCoreColor(new Color(61, 0, 152, 236));
            beam.setFringeColor(new Color(12, 255, 226, 236));
        }
    }

    private void spawnMine(ShipAPI source, Vector2f mineLoc) {
        CombatEngineAPI engine = Global.getCombatEngine();

        MissileAPI mine = (MissileAPI) engine.spawnProjectile(source, null, "Meng_Zerolayer", mineLoc, (float) Math.random() * 360f, null);
        if (source != null) {
            Global.getCombatEngine().applyDamageModifiersToSpawnedProjectileWithNullWeapon(source, WeaponAPI.WeaponType.MISSILE, false, mine.getDamage());
        }

        float fadeInTime = 1f;
        mine.getVelocity().scale(0);
        mine.fadeOutThenIn(fadeInTime);

        float liveTime = 0f;
        //liveTime = 0.01f;
        mine.setFlightTime(mine.getMaxFlightTime() - liveTime);
        mine.addDamagedAlready(source);
        mine.setNoMineFFConcerns(true);
    }
}