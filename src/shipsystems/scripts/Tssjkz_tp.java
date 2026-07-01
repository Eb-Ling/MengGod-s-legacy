package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.entities.SimpleEntity;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.EnumSet;

public class Tssjkz_tp extends BaseShipSystemScript {
    private boolean init = false;
    private boolean init1 = false;
    private float timer = 0f;
    private float timer1 = 0f;

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) {
            return;
        }
        float amount = Global.getCombatEngine().getElapsedInLastFrame();
        float indur = ship.getSystem().getChargeUpDur();
        float outdur = ship.getSystem().getChargeDownDur();


        if (ship.getSystem().getState() == ShipSystemAPI.SystemState.IN) {
            if (!init) {
                init = true;
                Vector2f loc = ship.getLocation();
                RippleDistortion ripple = new RippleDistortion(loc, new Vector2f());
                ripple.setSize(2300f);
                ripple.setIntensity(1000f);
                ripple.fadeInSize(5f);
                ripple.fadeOutIntensity(3f);
                ripple.setFrameRate(45f);
                DistortionShader.addDistortion(ripple);
            }
            timer += amount;
            float alphamult = Math.max(0f, (indur - timer) / indur);
            Alphacontrol(ship, alphamult);
        }

        if (ship.getSystem().getState() == ShipSystemAPI.SystemState.OUT) {
            if (!init1) {
                init1 = true;
                Global.getCombatEngine().addLayeredRenderingPlugin(new Tssjkz_tp_plugin(ship));//当除了第一次生效以外，舰船战术系统结束后，加入一个暂时的Plugin。
            }
            timer1 += amount;
            float alphamult = Math.min(1f, timer1 / outdur);
            Alphacontrol(ship, alphamult);
        }
    }

    private void Alphacontrol(ShipAPI ship, float Alphamult) {
        for (FighterLaunchBayAPI bay : ship.getLaunchBaysCopy()) {
            if (bay.getWing() != null && bay.getWing().getLeader() != null && bay.getWing().getLeader().getWing() != null) {
                for (ShipAPI wing : bay.getWing().getLeader().getWing().getWingMembers())
                    wing.setAlphaMult(Alphamult);
            }
        }
        ship.setAlphaMult(Alphamult);
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) {
            return;
        }

        Alphacontrol(ship, 1f);
        timer = 0f;
        timer1 = 0f;
        init = false;
        init1 = false;
    }

    public static class Tssjkz_tp_plugin implements CombatLayeredRenderingPlugin {
        private final ShipAPI ships;
        private final String id = "Tssjkz_tp_timemultid";
        private final float timemult = 0.5f;//时流比例
        private final float amplifytime = 3f;//时流与电弧生效时间
        private final float damage = 1000f;//电弧伤害
        private final float ranges = 1000f;//电弧最远扩散距离
        private float timer1 = 0f;
        private float timer2 = 0f;

        public Tssjkz_tp_plugin(ShipAPI ship) {
            ships = ship;
        }

        public void init(CombatEntityAPI entity) {

        }

        public void Spawnemparcs(ShipAPI ship, int arcringamount, float range) {
            Vector2f loc = ship.getLocation();
            CombatEngineAPI engine = Global.getCombatEngine();
            for (int i = 0; i < arcringamount; i++) {
                float angel = (float) i / (float) (arcringamount) * 360f;
                float nextangel = (float) (i + 1) / (float) (arcringamount) * 360f;
                Vector2f arcloc = new Vector2f((float) (loc.x + ((-0.1f + Math.random() * 0.2f) * range + range) * Math.cos(Math.toRadians(angel))), (float) (loc.y + ((-0.1f + Math.random() * 0.2f) * range + range) * Math.sin(Math.toRadians(angel))));
                Vector2f nextarcloc = new Vector2f((float) (loc.x + ((-0.1f + Math.random() * 0.2f) * range + range) * Math.cos(Math.toRadians(nextangel))), (float) (loc.y + ((-0.1f + Math.random() * 0.2f) * range + range) * Math.sin(Math.toRadians(nextangel))));
                boolean spawned = false;
                for (ShipAPI target : Global.getCombatEngine().getShips()) {
                    if (target.getOwner() != ship.getOwner()) {
                        if (MathUtils.getDistance(nextarcloc, target.getLocation()) <= target.getShieldRadiusEvenIfNoShield() * 1.1f) {
                            engine.spawnEmpArc(ship, arcloc, target, target, DamageType.ENERGY, damage, 0f, 100000f, null, (float) Math.sqrt(arcringamount), new Color(0, 255, 225, 255), new Color(2, 122, 108, 255));
                            spawned = true;
                        }
                    }
                }
                if (!spawned)
                    engine.spawnEmpArc(ship, arcloc, new SimpleEntity(nextarcloc), new SimpleEntity(nextarcloc), DamageType.ENERGY, damage, 0f, 100000f, null, (float) Math.sqrt(arcringamount), new Color(0, 255, 225, 255), new Color(2, 122, 108, 255));
            }
        }

        @Override
        public void cleanup() {
        }

        @Override
        public boolean isExpired() {
            return timer1 >= amplifytime + 0.1f;//返回值为true时，Plugin删除，因此当计时器超过三秒后删除。
        }

        @Override
        public void advance(float amount) {
            timer1 += amount;

            ships.getMutableStats().getTimeMult().modifyPercent(id, timemult * 100f);
            int alphamult = Math.round(Math.max(0f, (float) Math.round(255 * (amplifytime - timer1) / amplifytime)));
            ships.addAfterimage(new Color(22, 188, 210, alphamult), 0f, 0f, -ships.getVelocity().x, -ships.getVelocity().y, 0f, 0f, 0.05f, 0.5f, false, false, true);
            timer2 += amount;
            if (timer2 >= 0.5f) {
                timer2 = 0f;
                Spawnemparcs(ships, Math.round(timer1 * 16f), timer1 / amplifytime * ranges);
            }
            if (timer1 >= amplifytime) {
                ships.getMutableStats().getTimeMult().unmodifyPercent(id);
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
