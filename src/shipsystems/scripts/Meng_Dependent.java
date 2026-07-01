package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipSystemAPI.SystemState;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.Misc;
import data.methods.Meng_arcfind;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.EnumSet;

public class Meng_Dependent extends BaseShipSystemScript {

    public static boolean active = true;
    protected static float RANGE = 1500f;
    private boolean init = false;

    public static float getMaxRange(ShipAPI ship) {
        return ship.getMutableStats().getSystemRangeBonus().computeEffective(RANGE);
        //return RANGE;
    }

    public void apply(MutableShipStatsAPI stats, final String id, State state, float effectLevel) {
        ShipAPI ship = null;
        if (effectLevel < 1f) return;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
        } else {
            return;
        }

        ShipAPI target = findTarget(ship);
        if (target != null) {
            if (!init) {
                init = true;
                Global.getCombatEngine().addLayeredRenderingPlugin(new Meng_DependentPlugin(ship, target));
            }
            active = !active;
        }
    }

    public void unapply(MutableShipStatsAPI stats, String id) {
        init = false;
    }

    protected ShipAPI findTarget(ShipAPI ship) {
        float range = getMaxRange(ship);
        boolean player = ship == Global.getCombatEngine().getPlayerShip();
        ShipAPI target = ship.getShipTarget();
        if (target != null) {
            float dist = Misc.getDistance(ship.getLocation(), target.getLocation());
            float radSum = ship.getCollisionRadius() + target.getCollisionRadius();
            if (dist > range + radSum) target = null;
        } else {
            if (target == null || target.getOwner() != ship.getOwner()) {
                if (player) {
                    float weight = range;
                    for (ShipAPI ships : AIUtils.getNearbyAllies(ship, range)) {
                        if (!ships.isFighter() && !ships.isDrone() && ships.getOwner() == ship.getOwner()) {
                            float brightness;
                            brightness = Misc.getDistance(ship.getMouseTarget(), ships.getLocation());

                            if (brightness <= weight) {
                                weight = brightness;
                                target = ships;
                            }
                        }
                    }
                } else {
                    Object test = ship.getAIFlags().getCustom(AIFlags.MANEUVER_TARGET);
                    if (test instanceof ShipAPI) {
                        target = (ShipAPI) test;
                        float dist = Misc.getDistance(ship.getLocation(), target.getLocation());
                        float radSum = ship.getCollisionRadius() + target.getCollisionRadius();
                        if (dist > range + radSum) target = null;
                    }
                }
            }
            if (target == null) {
                target = Misc.findClosestShipEnemyOf(ship, ship.getLocation(), HullSize.FIGHTER, range, true);
            }
        }

        return target;
    }

    @Override
    public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
        if (system.isOutOfAmmo()) return null;
        if (system.getState() != SystemState.IDLE) return null;

        ShipAPI target = findTarget(ship);
        if (target != null && target != ship) {
            return "就绪";
        }
        if ((target == null) && ship.getShipTarget() != null) {
            return "超出距离";
        }
        return "无目标";
    }


    @Override
    public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
        //if (true) return true;
        ShipAPI target = findTarget(ship);
        return target != null && target != ship;
    }

    public static class Meng_DependentPlugin implements CombatLayeredRenderingPlugin {
        private final ShipAPI ship;
        private final ShipAPI target;
        private float timer = 0f;
        private Vector2f loc = new Vector2f();
        private boolean init1 = false;
        private boolean init2 = false;

        public Meng_DependentPlugin(ShipAPI ships, ShipAPI targets) {
            ship = ships;
            target = targets;
        }

        public void init(CombatEntityAPI entity) {

        }

        @Override
        public void cleanup() {
        }

        @Override
        public boolean isExpired() {
            return false;//返回值为true时，Plugin删除，因此当计时器超过三秒后删除。
        }

        private void Meng_Hunhua(ShipAPI ship, ShipAPI target) {
            String id = "Meng_Hunhuaeffect";
            target.setMaxHitpoints(ship.getMaxHitpoints() * 0.3f + target.getMaxHitpoints());
            target.setHitpoints(target.getHitpoints() + 0.3f * ship.getMaxHitpoints());
            target.getMutableStats().getFluxCapacity().modifyFlat(id, ship.getMaxFlux() * 0.3f);
            target.getMutableStats().getFluxDissipation().modifyFlat(id, ship.getMutableStats().getFluxDissipation().getModifiedValue() * 0.3f);
            target.getMutableStats().getMaxSpeed().modifyFlat(id, ship.getMaxSpeed() * 0.3f);
            target.getMutableStats().getMaxTurnRate().modifyFlat(id, ship.getMaxTurnRate() * 0.3f);
            target.getMutableStats().getTurnAcceleration().modifyFlat(id, ship.getTurnAcceleration() * 0.3f);
            target.getMutableStats().getAcceleration().modifyFlat(id, ship.getAcceleration() * 0.3f);
            float[][] grid1 = target.getArmorGrid().getGrid();
            for (int x = 0; x < grid1.length; x++)
                for (int y = 0; y < grid1[0].length; y++)
                    target.getArmorGrid().setArmorValue(x, y, grid1[x][y] + ship.getArmorGrid().getMaxArmorInCell() * 0.3f);
        }

        @Override
        public void advance(float amount) {
            timer += amount;
            if (!init1) {
                init1 = true;
                loc = new Vector2f(ship.getLocation().getX(), ship.getLocation().getY());
            }
            float arg = Meng_arcfind.Findarc(ship.getLocation(), target.getLocation());
            ship.setAlphaMult(Math.max(1f - timer, 0.25f));
            if (MathUtils.getDistance(loc, target.getLocation()) <= 50f) {
                if (!init2) {
                    init2 = true;
                    Meng_Hunhua(ship, target);
                }
                if (ship.getVelocity().length() >= 5f) {
                    target.getVelocity().set(ship.getVelocity());
                }
                target.setFacing(ship.getFacing());
                if (ship.getFluxTracker().isVenting()) {
                    target.giveCommand(ShipCommand.VENT_FLUX, null, 0);
                    target.blockCommandForOneFrame(ShipCommand.VENT_FLUX);
                }
                loc = target.getLocation();
                ship.getLocation().set(loc);
                ship.setPhased(true);
            } else {
                loc.setX(loc.getX() + 300f * amount * (timer + 1f) * (float) Math.cos(Math.toRadians(arg)));
                loc.setY(loc.getY() + 300f * amount * (timer + 1f) * (float) Math.sin(Math.toRadians(arg)));
                ship.addAfterimage(new Color(196, 136, 136, 153), 0f, 0f, 100 * (timer + 1f) * (float) Math.cos(Math.toRadians(arg)), 100 * (timer + 1f) * (float) Math.sin(Math.toRadians(arg)), 1f, 0f, 0.04f, 0.5f, true, true, true);
                ship.setPhased(true);
                ship.getLocation().set(loc);
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
