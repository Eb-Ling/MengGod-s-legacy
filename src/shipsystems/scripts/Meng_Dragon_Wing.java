package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.IntervalUtil;
import data.hullmods.Meng_Dragonheart;
import data.scripts.util.MagicAnim;
import org.lazywizard.lazylib.combat.entities.SimpleEntity;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;

public class Meng_Dragon_Wing extends BaseShipSystemScript {

    boolean init = false;
    float timer = 0f;
    public static final String KEY = "Meng_Dragonheartlistener";
    HashMap<Integer, String> DALSlots;
    HashMap<Integer, String> DARSlots;

    public Meng_Dragon_Wing() {
        this.DALSlots = new HashMap<>();
        this.DALSlots.put(1, "Meng_DAL2");
        this.DALSlots.put(2, "Meng_DAL1");
        this.DALSlots.put(3, "Meng_DAL3");

        this.DARSlots = new HashMap<>();
        this.DARSlots.put(1, "Meng_DAR2");
        this.DARSlots.put(2, "Meng_DAR1");
        this.DARSlots.put(3, "Meng_DAR3");
    }

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        ShipAPI DAL1 = null, DAL2 = null, DAL3 = null, DAR1 = null, DAR2 = null, DAR3 = null, DWL = null, DWR = null;
        boolean AL1 = false, AL2 = false, AL3 = false, AR1 = false, AR2 = false, AR3 = false;
        if (ship == null) {
            return;
        }
        if (ship.getSystem().isChargeup()) return;
        if (!ship.getCustomData().containsKey(KEY)) {
            Meng_Dragonheart.DataContainer data1 = new Meng_Dragonheart.DataContainer();
            ship.setCustomData(KEY, data1);
        }
        Meng_Dragonheart.DataContainer data = (Meng_Dragonheart.DataContainer) ship.getCustomData().get(KEY);

        if (!init) {
            init = true;
            data.spread = !data.spread;
            if (!data.spread) timer = 1f;
        }
        if (data.spread) {
            timer += Global.getCombatEngine().getElapsedInLastFrame();
            if (timer > 1f) {
                timer = 1f;
            }
            for (ShipAPI m : ship.getChildModulesCopy()) {
                switch (m.getStationSlot().getId()) {
                    case "Meng_DAL1":
                        DAL1 = m;
                        AL1 = true;
                        break;
                    case "Meng_DAL2":
                        DAL2 = m;
                        AL2 = true;
                        break;
                    case "Meng_DAL3":
                        DAL3 = m;
                        AL3 = true;
                        break;
                    case "Meng_DAR1":
                        DAR1 = m;
                        AR1 = true;
                        break;
                    case "Meng_DAR2":
                        DAR2 = m;
                        AR2 = true;
                        break;
                    case "Meng_DAR3":
                        DAR3 = m;
                        AR3 = true;
                        break;
                    case "Meng_DWL":
                        DWL = m;
                        break;
                    case "Meng_DWR":
                        DWR = m;
                        break;
                }
            }
            if (DWL != null) {
                DWL.getStationSlot().setAngle(90f * Math.min(timer, 0.5f));
            }
            if (DWR != null) {
                DWR.getStationSlot().setAngle(-90f * Math.min(timer, 0.5f));
            }
            if (AL1) {
                DAL1.getModuleOffset().set(-(MagicAnim.smoothNormalizeRange(timer, 0.15f, 1f) * 4f), -(MagicAnim.smoothNormalizeRange(timer, 0, 0.75f) * 10f));
            }
            if (AL2) {
                DAL2.getModuleOffset().set(-(MagicAnim.smoothNormalizeRange(timer, 0.25f, 1f) * 13f), -(MagicAnim.smoothNormalizeRange(timer, 0.05F, 0.75f) * 10f));
            }
            if (AL3) {
                DAL3.getModuleOffset().set(-(MagicAnim.smoothNormalizeRange(timer, 0.5f, 1f) * 33f), -(MagicAnim.smoothNormalizeRange(timer, 0.15f, 0.85f) * 18f));
            }
            if (AR1) {
                DAR1.getModuleOffset().set(-(MagicAnim.smoothNormalizeRange(timer, 0.15f, 1f) * 4f), (MagicAnim.smoothNormalizeRange(timer, 0, 0.75f) * 10f));
            }
            if (AR2) {
                DAR2.getModuleOffset().set(-(MagicAnim.smoothNormalizeRange(timer, 0.25f, 1f) * 13f), (MagicAnim.smoothNormalizeRange(timer, 0.05F, 0.75f) * 10f));
            }
            if (AR3) {
                DAR3.getModuleOffset().set(-(MagicAnim.smoothNormalizeRange(timer, 0.5f, 1f) * 33f), (MagicAnim.smoothNormalizeRange(timer, 0.15f, 0.85f) * 18f));
            }
        } else {
            timer -= Global.getCombatEngine().getElapsedInLastFrame();
            if (timer < 0f) {
                timer = 0f;
            }
            for (ShipAPI m : ship.getChildModulesCopy()) {
                switch (m.getStationSlot().getId()) {
                    case "Meng_DAL1":
                        DAL1 = m;
                        AL1 = true;
                        break;
                    case "Meng_DAL2":
                        DAL2 = m;
                        AL2 = true;
                        break;
                    case "Meng_DAL3":
                        DAL3 = m;
                        AL3 = true;
                        break;
                    case "Meng_DAR1":
                        DAR1 = m;
                        AR1 = true;
                        break;
                    case "Meng_DAR2":
                        DAR2 = m;
                        AR2 = true;
                        break;
                    case "Meng_DAR3":
                        DAR3 = m;
                        AR3 = true;
                        break;
                    case "Meng_DWL":
                        DWL = m;
                        break;
                    case "Meng_DWR":
                        DWR = m;
                        break;
                }
            }
            if (DWL != null) {
                DWL.getStationSlot().setAngle(90f * Math.min(timer, 0.5f));
            }
            if (DWR != null) {
                DWR.getStationSlot().setAngle(-90f * Math.min(timer, 0.5f));
            }
            if (AL1) {
                DAL1.getModuleOffset().set(-(MagicAnim.smoothNormalizeRange(timer, 0.15f, 1f) * 4f), -(MagicAnim.smoothNormalizeRange(timer, 0, 0.75f) * 10f));
            }
            if (AL2) {
                DAL2.getModuleOffset().set(-(MagicAnim.smoothNormalizeRange(timer, 0.25f, 1f) * 13f), -(MagicAnim.smoothNormalizeRange(timer, 0.05F, 0.75f) * 10f));
            }
            if (AL3) {
                DAL3.getModuleOffset().set(-(MagicAnim.smoothNormalizeRange(timer, 0.5f, 1f) * 33f), -(MagicAnim.smoothNormalizeRange(timer, 0.15f, 0.85f) * 18f));
            }
            if (AR1) {
                DAR1.getModuleOffset().set(-(MagicAnim.smoothNormalizeRange(timer, 0.15f, 1f) * 4f), (MagicAnim.smoothNormalizeRange(timer, 0, 0.75f) * 10f));
            }
            if (AR2) {
                DAR2.getModuleOffset().set(-(MagicAnim.smoothNormalizeRange(timer, 0.25f, 1f) * 13f), (MagicAnim.smoothNormalizeRange(timer, 0.05F, 0.75f) * 10f));
            }
            if (AR3) {
                DAR3.getModuleOffset().set(-(MagicAnim.smoothNormalizeRange(timer, 0.5f, 1f) * 33f), (MagicAnim.smoothNormalizeRange(timer, 0.15f, 0.85f) * 18f));
            }
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) {
            return;
        }
        if (!ship.getCustomData().containsKey(KEY)) {
            Meng_Dragonheart.DataContainer data1 = new Meng_Dragonheart.DataContainer();
            ship.setCustomData(KEY, data1);
        }
        Meng_Dragonheart.DataContainer data = (Meng_Dragonheart.DataContainer) ship.getCustomData().get(KEY);

        if (!data.spread) for (ShipAPI m : ship.getChildModulesCopy()) {
            switch (m.getStationSlot().getId()) {
                case "Meng_DWL":
                case "Meng_DWR":
                    m.getStationSlot().setAngle(0f);
                    break;
            }
        }
        init = false;
        timer = 0f;
        Global.getCombatEngine().addLayeredRenderingPlugin(new Meng_Dragon_WingPlugin(ship, data.spread));//当除了第一次生效以外，舰船战术系统结束后，加入一个暂时的Plugin，调整舰船的透明度形成渐出效果。
    }

    public static class Meng_Dragon_WingPlugin implements CombatLayeredRenderingPlugin {
        private static final IntervalUtil Interval = new IntervalUtil(0.8f, 1.5f);
        private static final IntervalUtil Interval1 = new IntervalUtil(0.8f, 1.5f);
        private final ShipAPI ships;
        private final boolean spreads;
        private final String id = "Meng_Dragonwingsystem";
        private float timer1;

        public Meng_Dragon_WingPlugin(ShipAPI ship, boolean spread) {
            ships = ship;
            spreads = spread;
        }

        public void init(CombatEntityAPI entity) {
        }

        @Override
        public void cleanup() {
        }

        @Override
        public boolean isExpired() {
            return ships.getSystem().isChargeup();//返回值为true时，Plugin删除。
        }

        @Override
        public void advance(float amount) {
            if (ships.isAlive()) {
                timer1 += amount;
                if (!spreads) {
                    for (WeaponAPI w : ships.getAllWeapons()) {
                        if (w.getSpec().getWeaponId().equals("Meng_DragonbiuR") || w.getSpec().getWeaponId().equals("Meng_DragonbiuL")) {
                            w.setAmmo(0);
                        }
                    }
                    if (ships.getChildModulesCopy() != null) {
                        for (ShipAPI m : ships.getChildModulesCopy()) {
                            if (m != null) {
                                switch (m.getStationSlot().getId()) {
                                    case "Meng_DWL":
                                    case "Meng_DWR":
                                        m.addAfterimage(new Color(91, 203, 183, 50), 0f, 0f, -ships.getVelocity().x, -ships.getVelocity().y, 0f, 0f, 0.05f, 0.5f, false, true, false);
                                        break;
                                }
                            }
                        }
                    }
                    ships.getMutableStats().getFluxDissipation().unmodifyPercent(id);
                    ships.getMutableStats().getShieldDamageTakenMult().unmodifyPercent(id);
                    ships.getMutableStats().getMaxSpeed().modifyMult(id, 1.3f);
                    ships.getMutableStats().getAcceleration().modifyMult(id, 2f);
                    ships.getMutableStats().getDeceleration().modifyMult(id, 2f);
                    ships.getMutableStats().getMaxTurnRate().modifyMult(id, 1.3f);
                    if (ships.getFluxTracker().getFluxLevel() <= 0.5f) {
                        ships.getMutableStats().getZeroFluxMinimumFluxLevel().modifyFlat(id, 2f);
                    } else {
                        ships.getMutableStats().getZeroFluxMinimumFluxLevel().unmodifyFlat(id);
                    }
                    if (ships == Global.getCombatEngine().getPlayerShip()) {
                        Global.getCombatEngine().maintainStatusForPlayerShip(id + '2', "graphics/fx/Meng_logo1.png", "舰船航速与机动性提高", 20 + "%", false);
                    }
                } else {
                    Interval.advance(amount);
                    Interval1.advance(amount);
                    ships.addAfterimage(new Color(91, 203, 183, 50), 0f, 0f, -ships.getVelocity().x, -ships.getVelocity().y, 0f, 0f, 0.05f, 0.5f, false, true, false);
                    ships.getMutableStats().getFluxDissipation().modifyPercent(id, 25f);
                    ships.getMutableStats().getShieldDamageTakenMult().modifyPercent(id, -25f);
                    ships.getMutableStats().getMaxSpeed().modifyMult(id, 0.75f);
                    ships.getMutableStats().getDeceleration().modifyMult(id, 0.75f);
                    ships.getMutableStats().getAcceleration().modifyMult(id, 0.75f);
                    ships.getMutableStats().getMaxTurnRate().modifyMult(id, 0.75f);
                    ships.getMutableStats().getZeroFluxMinimumFluxLevel().unmodifyFlat(id);
                    if (ships == Global.getCombatEngine().getPlayerShip()) {
                        Global.getCombatEngine().maintainStatusForPlayerShip(id + '1', "graphics/fx/Meng_logo1.png", "舰船幅能耗散与盾效提高", 25 + "%", false);
                        Global.getCombatEngine().maintainStatusForPlayerShip(id + '2', "graphics/fx/Meng_logo1.png", "舰船航速与机动性降低", 25 + "%", true);
                    }
                    for (WeaponAPI w : ships.getAllWeapons()) {
                        if (w.getSpec().getWeaponId().equals("Meng_DragonbiuR") || w.getSpec().getWeaponId().equals("Meng_DragonbiuL")) {
                            w.setAmmo(10);
                        }
                    }
                    for (ShipAPI m : ships.getChildModulesCopy()) {
                        switch (m.getStationSlot().getId()) {
                            case "Meng_DAL1":
                            case "Meng_DAL2":
                            case "Meng_DAL3":
                            case "Meng_DAR1":
                            case "Meng_DAR2":
                            case "Meng_DAR3":
                                m.addAfterimage(new Color(14, 141, 129, 50), 0f, 0f, -ships.getVelocity().x, -ships.getVelocity().y, 0f, 0f, 0.03f, 0.5f, true, true, false);
                                break;
                        }
                    }
                    if (Interval.intervalElapsed()) {
                        List<ShipAPI> leftchilds = new ArrayList<>();
                        for (ShipAPI m : ships.getChildModulesCopy()) {
                            switch (m.getStationSlot().getId()) {
                                case "Meng_DAL1":
                                case "Meng_DAL2":
                                case "Meng_DAL3":
                                    leftchilds.add(m);
                                    break;
                            }
                        }
                        if (leftchilds.size() > 0) {
                            int num = Math.round((float) Math.random() * (leftchilds.size() - 1));
                            ShipAPI m = leftchilds.get(num);
                            float arg = (float) Math.toRadians(m.getFacing() - 90f - 15f + 30f * Math.random());
                            Global.getCombatEngine().spawnEmpArcPierceShields(ships, new Vector2f(m.getLocation().getX() + (float) Math.cos(arg - 180f) * 10f, m.getLocation().getY() + (float) Math.sin(arg - 180f) * 10f), new SimpleEntity(new Vector2f(m.getLocation().getX() + (float) Math.cos(arg) * 25f, m.getLocation().getY() + (float) Math.sin(arg) * 25f)), new SimpleEntity(new Vector2f(m.getLocation().getX() + (float) Math.cos(arg) * 25f, m.getLocation().getY() + (float) Math.sin(arg) * 25f)), DamageType.ENERGY, 0f, 0f, 10000f, null
                                    , 5f, new Color(0, 243, 253, 150), new Color(0, 243, 253, 218));
                        }
                    }
                    if (Interval1.intervalElapsed()) {
                        List<ShipAPI> rightchilds = new ArrayList<>();
                        for (ShipAPI m : ships.getChildModulesCopy()) {
                            switch (m.getStationSlot().getId()) {
                                case "Meng_DAR1":
                                case "Meng_DAR2":
                                case "Meng_DAR3":
                                    rightchilds.add(m);
                                    break;
                            }
                        }
                        if (rightchilds.size() > 0) {
                            int num = Math.round((float) Math.random() * (rightchilds.size() - 1));
                            ShipAPI m = rightchilds.get(num);
                            float arg = (float) Math.toRadians(m.getFacing() - 15f + 90f + 30f * Math.random());
                            Global.getCombatEngine().spawnEmpArcPierceShields(ships, new Vector2f(m.getLocation().getX() + (float) Math.cos(arg - 180f) * 10f, m.getLocation().getY() + (float) Math.sin(arg - 180f) * 10f), new SimpleEntity(new Vector2f(m.getLocation().getX() + (float) Math.cos(arg) * 25f, m.getLocation().getY() + (float) Math.sin(arg) * 25f)), new SimpleEntity(new Vector2f(m.getLocation().getX() + (float) Math.cos(arg) * 25f, m.getLocation().getY() + (float) Math.sin(arg) * 25f)), DamageType.ENERGY, 0f, 0f, 10000f, null
                                    , 5f, new Color(0, 243, 253, 150), new Color(0, 243, 253, 218));
                        }
                    }
                }
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
