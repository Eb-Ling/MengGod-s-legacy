package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.EnumSet;

public class Meng_Burndrive extends BaseShipSystemScript {

    private static final IntervalUtil Interval = new IntervalUtil(0.1f, 0.2f);
    private float timer = 0f;
    private boolean init1 = false;

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) {
            return;
        }
        timer += Global.getCombatEngine().getElapsedInLastFrame();
        ship.setAlphaMult(Math.max(0.5f, 1f - timer / 2f));//设置舰船的透明度，使其逐渐透明。
        ship.addAfterimage(new Color(73, 189, 210, 153), 0f, 0f, -ship.getVelocity().x, -ship.getVelocity().y, 0f, 0f, 0.03f, 0.5f, true, true, false);
        //形成残像特效并设定颜色。
        if (state == State.OUT) {
            stats.getMaxSpeed().unmodifyFlat(id);
        } else {
            stats.getMaxSpeed().modifyFlat(id, 250f * effectLevel);
            stats.getAcceleration().modifyFlat(id, 400f * effectLevel);//调整最大速度与加速度
            ship.setPhased(true);//设置舰船进入相位。
        }
        Interval.advance(Global.getCombatEngine().getElapsedInLastFrame());
        if (Interval.intervalElapsed()) {
            Global.getCombatEngine().addNebulaSmokeParticle(ship.getLocation(), new Vector2f(ship.getVelocity().getX() * 0.3f, ship.getVelocity().getY() * 0.3f), 200f, 3f, 0.9f, 0.3f, 1.5f, new Color(73, 210, 173, 153));
        }//每隔指定时间生成烟雾。
    }

    public void unapply(MutableShipStatsAPI stats, String id) {
        stats.getMaxSpeed().unmodify(id);
        stats.getMaxTurnRate().unmodify(id);
        stats.getTurnAcceleration().unmodify(id);
        stats.getAcceleration().unmodify(id);
        stats.getDeceleration().unmodify(id);
        ShipAPI ship = (ShipAPI) stats.getEntity();
        ship.setPhased(false);
        CombatEngineAPI engine1 = Global.getCombatEngine();
        timer = 0f;//当技能释放结束后，对各项已更改数值进行还原。
        if (init1) {
            engine1.addLayeredRenderingPlugin(new Meng_BurndrivePlugin(ship));//当除了第一次生效以外，舰船战术系统结束后，加入一个暂时的Plugin，调整舰船的透明度形成渐出效果。
        }
        if (!init1) {
            init1 = true;
        }//因为当战斗刚开始时，此unapply中的描述将会生效一次，所以要排除第一次生效。
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            return new StatusData("提高引擎出力", false);
        }
        return null;
    }

    public static class Meng_BurndrivePlugin implements CombatLayeredRenderingPlugin {
        private final ShipAPI ships;
        private float timer1;

        public Meng_BurndrivePlugin(ShipAPI ship) {
            ships = ship;
        }

        public void init(CombatEntityAPI entity) {

        }

        @Override
        public void cleanup() {
        }

        @Override
        public boolean isExpired() {
            return timer1 >= 3f;//返回值为true时，Plugin删除，因此当计时器超过三秒后删除。
        }

        @Override
        public void advance(float amount) {
            timer1 += amount;
            ships.setAlphaMult(Math.min(1f, ships.getAlphaMult() + timer1 / 2f));//舰船的渐出效果。
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
