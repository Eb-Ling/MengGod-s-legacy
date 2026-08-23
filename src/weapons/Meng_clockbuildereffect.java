package data.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.loading.MissileSpecAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import data.methods.Meng_arcfind;
import data.scripts.plugins.MagicRenderPlugin;

import org.boxutil.define.BoxEnum;
import org.boxutil.manager.CombatRenderingManager;
import org.boxutil.units.standard.entity.FlareEntity;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.jetbrains.annotations.NotNull;
import org.dark.shaders.util.ShaderLib;
import org.lwjgl.opengl.ARBFramebufferObject;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.Random;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.magiclib.util.MagicLensFlare;

public class Meng_clockbuildereffect implements EveryFrameWeaponEffectPlugin {
    public static final String KEY = "Mengtimelistener_S";
    private final IntervalUtil Interval = new IntervalUtil(1f, 1f);
    private float activetimer = 0f;
    private final IntervalUtil Interval2 = new IntervalUtil(6f, 6f);
    private final IntervalUtil Interval3 = new IntervalUtil(4f, 4f);
    public LinkedList<Vector2f> locs = null;
    public LinkedList<Float> args = null;
    public LinkedList<Float> fluxs = null;
    public LinkedList<Float> hfluxs = null;
    public LinkedList<Float> minarmor = null;
    public LinkedList<Float> hits = null;
    public LinkedList<float[][]> armors = null;
    public LinkedList<int[]> ammo = null;
    SpriteAPI TBF1 = Global.getSettings().getSprite("fx", "Meng_TBF1");
    SpriteAPI TBF2 = Global.getSettings().getSprite("fx", "Meng_TBF1");
    SpriteAPI TBF3 = Global.getSettings().getSprite("fx", "Meng_TBF1");
    boolean init1 = false;
    boolean weaponactive=false;
    float timer = 0f;
    int nowtime=0;
    float time = 0f;
    float time1 = 0f;
    boolean inc = true;
    boolean init2 = false;
    boolean ready = false;
    boolean rready = false;
    float lastlevel = 0f;
    float lastenergy = 0f;
    float timer1 = 0f;
    int num = 0;
    Vector2f copyloc = new Vector2f();
    float copyarg = 0f;
    float copyhit = 0f;
    float copyflux = 0f;
    float copyhflux = 0f;
    float[][] copyarmor = null;
    int[] copyammo = null;
    String id = "Meng_Clockbuildereff";

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        ShipAPI ship = weapon.getShip();
        if (!ship.isAlive()) return;
        for(WeaponAPI wea : ship.getAllWeapons()) {
            for (WeaponSlotAPI w : ship.getHullSpec().getAllWeaponSlotsCopy()) {
                if (wea.getSlot() == w) {
                    wea.getSprite();
                    w.getLocation();
                }
            }
        }

        DataContainer data = getDataContainer(ship);
        float timemult = Global.getCombatEngine().getTimeMult().getMult();
        if (timemult <= 0.001f) timemult = 0.001f;

        updateClockAngle(ship, data, timemult, amount);
        updateClockVisuals(ship, data);
        updateEnergyPointer(weapon, data, timemult, amount);

        weapon.setCurrHealth(weapon.getMaxHealth());

        boolean aiControlled = checkAIControl(ship, weapon);

        handleAIModeTransition(ship, data, aiControlled);

        renderEnergyOrbs(ship, weapon, data);

        if (!weapon.isFiring()) {
            handleNotFiring(ship, weapon, data, engine, aiControlled, timemult, amount);
        } else if (!data.timestop) {
            handleFiring(ship, weapon, data, engine, aiControlled, amount, timemult);
        }

        if (aiControlled && data.aifire) {
            weapon.setForceFireOneFrame(true);
        }

        lastlevel = weapon.getChargeLevel();

        if (lastenergy > data.energy) {
            Global.getCombatEngine().addLayeredRenderingPlugin(new Meng_ClockbuilderEnergyPlugin(weapon, time, time1));
        }

        lastenergy = data.energy;
    }

    private DataContainer getDataContainer(ShipAPI ship) {
        if (!ship.getCustomData().containsKey(KEY)) {
            DataContainer data = new DataContainer();
            ship.setCustomData(KEY, data);
            return data;
        }
        return (DataContainer) ship.getCustomData().get(KEY);
    }

    private void updateClockAngle(ShipAPI ship, DataContainer data, float timemult, float amount) {
        float arg1 = ship.getFacing() - 165f;
        if (Math.abs(arg1 - data.arg) <= 180f) {
            if (data.arg <= arg1) {
                data.arg = Math.min(data.arg + 6f * amount / timemult, arg1);
                if (data.arg <= arg1 - 3f) {
                    data.arg = arg1 - 3f;
                }
                if (data.arg >= arg1 + 15f) {
                    data.arg = arg1 + 15f;
                }
            } else {
                data.arg = Math.max(data.arg - 6f * amount / timemult, arg1);
                if (data.arg <= arg1 - 3f) {
                    data.arg = arg1 - 3f;
                }
                if (data.arg >= arg1 + 15f) {
                    data.arg = arg1 + 15f;
                }
            }
        } else {
            if (data.arg <= arg1) {
                data.arg = Math.max(data.arg - 6f * amount / timemult, arg1 - 360f);
                if (data.arg <= arg1 - 363f) {
                    data.arg = arg1 - 363f;
                }
                if (data.arg >= arg1 + 345f) {
                    data.arg = arg1 + 345f;
                }
            } else {
                data.arg = Math.min(data.arg + 6f * amount / timemult, arg1 + 360f);
                if (data.arg <= arg1 + 357f) {
                    data.arg = arg1 + 357f;
                }
                if (data.arg >= arg1 + 375f) {
                    data.arg = arg1 + 375f;
                }
            }
        }
    }

    private void updateClockVisuals(ShipAPI ship, DataContainer data) {
        Vector2f loc1 = new Vector2f(ship.getLocation().getX() + 295f * (float) Math.cos(Math.toRadians(data.arg + 2)), ship.getLocation().getY() + 295f * (float) Math.sin(Math.toRadians(data.arg + 2)));
        Vector2f loc2 = new Vector2f(ship.getLocation().getX() + 300f * (float) Math.cos(Math.toRadians(data.arg + 20f)), ship.getLocation().getY() + 300f * (float) Math.sin(Math.toRadians(data.arg + 20f)));
        Vector2f loc3 = new Vector2f(ship.getLocation().getX() + 290f * (float) Math.cos(Math.toRadians(data.arg + 38f)), ship.getLocation().getY() + 290f * (float) Math.sin(Math.toRadians(data.arg + 38f)));

        float args1 = Meng_arcfind.Findarc(loc1, ship.getLocation());
        float args2 = Meng_arcfind.Findarc(loc2, ship.getLocation());
        float args3 = Meng_arcfind.Findarc(loc3, ship.getLocation());

        renderClockSprite(TBF1, loc1, args1);
        renderClockSprite(TBF2, loc2, args2);
        renderClockSprite(TBF3, loc3, args3);
    }

    private void renderClockSprite(SpriteAPI sprite, Vector2f loc, float angle) {
        MagicRenderPlugin.addSingleframe(sprite, loc, CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER);
        sprite.setAngle(angle - 95f);
    }

    private void updateEnergyPointer(WeaponAPI weapon, DataContainer data, float timemult, float amount) {
        float arg2 = 24f * data.energy;
        if (Math.abs(arg2 - data.arg1) <= 180f) {
            if (data.arg1 <= arg2) {
                data.arg1 = Math.min(data.arg1 + 60f * amount / timemult, arg2);
            } else {
                data.arg1 = Math.max(data.arg1 - 60f * amount / timemult, arg2);
            }
        } else {
            if (data.arg1 <= arg2) {
                data.arg1 = Math.max(data.arg1 - 60f * amount / timemult, arg2 - 360f);
            } else {
                data.arg1 = Math.min(data.arg1 + 60f * amount / timemult, arg2 + 360f);
            }
        }
        weapon.setCurrAngle(data.arg1 + 90f);
    }

    private boolean checkAIControl(ShipAPI ship, WeaponAPI weapon) {
        boolean aicontrol = ship.getAI() != null;
        for (WeaponGroupAPI w : weapon.getShip().getWeaponGroupsCopy()) {
            if (weapon.getShip().getSelectedGroupAPI() != w && w.isAutofiring() && w.getWeaponsCopy().contains(weapon)) {
                aicontrol = true;
            }
        }
        return aicontrol;
    }

    private void handleAIModeTransition(ShipAPI ship, DataContainer data, boolean aiControlled) {
        if (!aiControlled && data.aifire) {
            ship.getMutableStats().getArmorDamageTakenMult().unmodifyMult(id);
            ship.getMutableStats().getShieldDamageTakenMult().unmodifyMult(id);
            ship.getMutableStats().getHullDamageTakenMult().unmodifyMult(id);
            ship.getMutableStats().getTurnAcceleration().unmodifyMult(id);
            ship.getMutableStats().getMaxTurnRate().unmodifyMult(id);
            ship.getMutableStats().getShieldUnfoldRateMult().unmodifyMult(id);
            data.aifire = false;
        }
    }

    private void handleNotFiring(ShipAPI ship, WeaponAPI weapon, DataContainer data, CombatEngineAPI engine, boolean aiControlled, float timemult, float amount) {
        ready = false;
        timer = Math.max(0f, timer - amount / timemult * 4f);
        init1 = false;
        weapon.getSprite().setColor(new Color(255, 255, 255, 255));

        if (aiControlled && !data.aifire && data.energy > 0) {
            checkAndTriggerAIActivation(ship, data);
        }

        updateChargeIntervals(ship, data, timemult, amount);
        recordState(ship);
    }

    private void checkAndTriggerAIActivation(ShipAPI ship, DataContainer data) {
        timer1 += Global.getCombatEngine().getElapsedInLastFrame();
        if (timer1 < 1.05f) return;

        if (locs == null || locs.isEmpty() || fluxs == null || hits == null ||
            minarmor == null || ammo == null) return;

        float minflux;
        float maxhit;
        float timeweight = 0f;
        float minarmors;
        int besttime = 0;

        for (int i = 1; i <= data.energy && i <= locs.size() && !data.timestop; ++i) {
            int index = locs.size() - i;
            minflux = fluxs.get(index);
            maxhit = hits.get(index);
            minarmors = minarmor.get(index);

            float armorlevel = minarmors - getAverageArmor(ship);
            float hitlevel = maxhit - ship.getHitpoints();
            float fluxlevel = ship.getCurrFlux() - minflux;

            float ammoDamageWeight = calculateAmmoDamageWeight(ship, index);

            float nowweight = (armorlevel * 25f + hitlevel * 2.5f + fluxlevel + ammoDamageWeight) * (0.2f + (float) Math.pow(0.8f, i));

            if (nowweight > timeweight) {
                timeweight = nowweight;
                besttime = i;
            }
        }

        if (timeweight > 4000f && data.energy >= 1 && timer1 > 1.1f) {
            data.aifire = true;
            data.besttime = besttime;
        }
    }

    private float calculateAmmoDamageWeight(ShipAPI ship, int index) {
        int[] recordedAmmo = ammo.get(index);

        float totalDamageWeight = 0f;

        int i = 0;
        for (WeaponAPI weapon : ship.getAllWeapons()) {
            i++;
            int ammoDiff = recordedAmmo[i] - weapon.getAmmo();

            if (ammoDiff <= 0) continue;

            float weaponDamage = estimateWeaponDamage(weapon);
            float damageWeight = ammoDiff * weaponDamage;

            if (weapon.getSpec().getType() == WeaponAPI.WeaponType.MISSILE) {
                damageWeight *= 4f;
            }

            totalDamageWeight += damageWeight;
        }

        return totalDamageWeight / 16f;
    }

    private float estimateWeaponDamage(WeaponAPI weapon) {
        if (weapon.getSpec().getMaxAmmo() <= 0) {
            return 0f;
        }

        return weapon.getDamage().getDamage();
    }

    private float getAverageArmor(ShipAPI ship) {
        float[][] grid = ship.getArmorGrid().getGrid();
        int armnum = 0;
        float allarm = 0f;
        for (float[] floats : grid) {
            for (float armor : floats) {
                allarm += armor;
                armnum++;
            }
        }
        return armnum > 0 ? allarm / armnum : 0f;
    }

    private void updateChargeIntervals(ShipAPI ship, DataContainer data, float timemult, float amount) {
        Interval2.advance(amount / timemult);
        Interval3.advance(amount / timemult);
        Interval.advance(amount / timemult);

        if (data.timestop) return;

        boolean isPlayerShip = checkIfPlayerShip(ship);

        if (isPlayerShip) {
            if (Interval2.intervalElapsed()) {
                data.energy = Math.min(15, data.energy + 1);
                Global.getSoundPlayer().playSound("Meng_fclock", 1.0f, 1.0f, ship.getLocation(), new Vector2f());
            }
        } else {
            if (Interval3.intervalElapsed()) {
                data.energy = Math.min(15, data.energy + 1);
                Global.getSoundPlayer().playSound("Meng_fclock", 1.0f, 1.0f, ship.getLocation(), new Vector2f());
            }
        }
    }

    private boolean checkIfPlayerShip(ShipAPI ship) {
        if (Global.getSector().getPlayerFleet() == null || ship.getFleetMember() == null) {
            return false;
        }
        if (ship.getFleetMember().getFleetData() == null || ship.getFleetMember().getFleetData().getFleet() == null) {
            return false;
        }
        return ship.getFleetMember().getFleetData().getFleet() == Global.getSector().getPlayerFleet();
    }

    private void recordState(ShipAPI ship) {
        if (!Interval.intervalElapsed()) return;

        initializeLists();

        float[][] grid = ship.getArmorGrid().getGrid();
        float[][] yy = new float[grid.length][grid[0].length];
        int armnum = 0;
        float allarm = 0f;

        for (int x = 0; x < grid.length; x++) {
            for (int y = 0; y < grid[0].length; ++y) {
                float armor = grid[x][y];
                allarm += armor;
                armnum++;
            }
            System.arraycopy(grid[x], 0, yy[x], 0, grid[0].length);
        }

        int weaponCount = ship.getAllWeapons().size();
        int[] ammo1 = new int[weaponCount + 1];
        int i = 0;
        for (WeaponAPI weapons : ship.getAllWeapons()) {
            i++;
            ammo1[i] = weapons.getAmmo();
        }

        if (locs.size() <= 20) {
            locs.addLast(new Vector2f(ship.getLocation().x, ship.getLocation().y));
            args.addLast(ship.getFacing());
            hits.addLast(ship.getHitpoints());
            fluxs.addLast(ship.getFluxTracker().getCurrFlux());
            hfluxs.addLast(ship.getFluxTracker().getHardFlux());
            armors.addLast(yy);
            minarmor.addLast(allarm / armnum);
            ammo.addLast(ammo1);
        } else {
            locs.addLast(new Vector2f(ship.getLocation().x, ship.getLocation().y));
            hits.addLast(ship.getHitpoints());
            args.addLast(ship.getFacing());
            fluxs.addLast(ship.getFluxTracker().getCurrFlux());
            hfluxs.addLast(ship.getFluxTracker().getHardFlux());
            armors.addLast(yy);
            minarmor.addLast(allarm / armnum);
            ammo.addLast(ammo1);

            hits.removeFirst();
            minarmor.removeFirst();
            armors.removeFirst();
            ammo.removeFirst();
            args.removeFirst();
            fluxs.removeFirst();
            hfluxs.removeFirst();
            locs.removeFirst();
        }
    }

    private void initializeLists() {
        if (locs == null) locs = new LinkedList<>();
        if (minarmor == null) minarmor = new LinkedList<>();
        if (args == null) args = new LinkedList<>();
        if (hits == null) hits = new LinkedList<>();
        if (fluxs == null) fluxs = new LinkedList<>();
        if (hfluxs == null) hfluxs = new LinkedList<>();
        if (armors == null) armors = new LinkedList<>();
        if (ammo == null) ammo = new LinkedList<>();
    }

    private void handleFiring(ShipAPI ship, WeaponAPI weapon, DataContainer data, CombatEngineAPI engine, boolean aiControlled, float amount, float timemult) {
        if (!init1) {
            initializeRewind(ship, weapon, data);
        }

        updateAnimationTimers(amount);

        float chargelevel = weapon.getChargeLevel();
        weapon.getSprite().setColor(new Color(0, 0, 0, 0));

        renderChargeBar(weapon, data, chargelevel);

        if (chargelevel == 1 && weaponactive) {
            handleFullCharge(ship, weapon, data, engine, aiControlled, amount);
        }

        if ((chargelevel < lastlevel || data.energy == 0) && ready) {
            handleChargeDecrease(ship, weapon, data, engine);
        }
    }

    private void initializeRewind(ShipAPI ship, WeaponAPI weapon, DataContainer data) {
        ready = false;
        rready = false;
        init1 = true;
        init2 = false;
        weaponactive = true;
        activetimer = 0.15f;
        nowtime = 0;

        copyloc = new Vector2f(ship.getLocation().getX(), ship.getLocation().getY());
        copyarg = ship.getFacing();
        copyhit = ship.getHitpoints();
        copyflux = ship.getCurrFlux();
        copyhflux = ship.getFluxTracker().getHardFlux();

        float[][] grid = ship.getArmorGrid().getGrid();
        copyarmor = new float[grid.length][grid[0].length];
        for (int x = 0; x < grid.length; x++) {
            System.arraycopy(grid[x], 0, copyarmor[x], 0, grid[0].length);
        }

        num = ship.getAllWeapons().size();
        copyammo = new int[num + 1];
        int i = 0;
        for (WeaponAPI weapons : ship.getAllWeapons()) {
            i++;
            copyammo[i] = weapons.getAmmo();
        }
    }

    private void updateAnimationTimers(float amount) {
        if (time <= -1f) {
            inc = true;
        } else if (time >= 1f) {
            inc = false;
        }
        if (inc) {
            time += amount;
        } else {
            time -= amount;
        }
        time1 += amount;
    }

    private void renderEnergyOrbs(ShipAPI ship, WeaponAPI weapon, DataContainer data) {
        for (int i = 1; i <= data.energy; ++i) {
            if (!data.active) {
                SpriteAPI BEL = Global.getSettings().getSprite("fx", "Meng_BEL");
                SpriteAPI SEL = Global.getSettings().getSprite("fx", "Meng_SEL");

                Vector2f loc = new Vector2f(weapon.getLocation().getX() + 500f * (float) Math.cos(Math.toRadians(i * 24f + 90f)), weapon.getLocation().getY() + 500f * (float) Math.sin(Math.toRadians(i * 24f + 90f)));
                MagicRenderPlugin.addSingleframe(BEL, loc, CombatEngineLayers.UNDER_SHIPS_LAYER);
                MagicRenderPlugin.addSingleframe(SEL, loc, CombatEngineLayers.UNDER_SHIPS_LAYER);
                BEL.setAlphaMult(0.4f);
                BEL.setSize(100f + 20 * time, 100f + 20 * time);
                BEL.setAngle(i * 24f + 30f * time1);
                SEL.setAlphaMult(0.4f);
                SEL.setSize(120f, 120f);
                SEL.setAngle(i * 24f);
            }
        }
    }

    private void renderChargeBar(WeaponAPI weapon, DataContainer data, float chargelevel) {
        double cos = Math.cos(Math.toRadians(data.arg1 + 90f));
        double sin = Math.sin(Math.toRadians(data.arg1 + 90f));

        renderTopSegment(weapon, data, chargelevel, cos, sin);
        renderMidSegments(weapon, data, chargelevel, cos, sin);
        renderTailAndLight(weapon, data, chargelevel);
    }

    private void renderTopSegment(WeaponAPI weapon, DataContainer data, float chargelevel, double cos, double sin) {
        if (chargelevel < 0.4f) return;

        SpriteAPI top = Global.getSettings().getSprite("fx", "Meng_TBTop");
        float length = 156.5f;
        Vector2f newloc = new Vector2f(weapon.getLocation().x + length * (float) cos, weapon.getLocation().y + length * (float) sin);

        MagicRenderPlugin.addSingleframe(top, newloc, CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER);
        top.setAngle(data.arg1);
        top.setAlphaMult(Math.max(0, Math.min(1f, (chargelevel - 0.4f) * 5f)));
    }

    private void renderMidSegments(WeaponAPI weapon, DataContainer data, float chargelevel, double cos, double sin) {
        SpriteAPI[] mids = new SpriteAPI[4];
        float[] lengths = {49.5f, 71.5f, 93.5f, 115.5f};
        float[] thresholds = {0f, 0.1f, 0.2f, 0.3f};

        for (int i = 0; i < 4; i++) {
            mids[i] = Global.getSettings().getSprite("fx", "Meng_TBMid0");
        }

        for (int i = 3; i >= 0; i--) {
            if (chargelevel < thresholds[i]) continue;

            Vector2f newloc = new Vector2f(weapon.getLocation().x + lengths[i] * (float) cos, weapon.getLocation().y + lengths[i] * (float) sin);
            MagicRenderPlugin.addSingleframe(mids[i], newloc, CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER);
            mids[i].setAngle(data.arg1);

            float alpha;
            if (i == 0) {
                alpha = Math.min(1f, chargelevel * 10f);
            } else {
                alpha = Math.max(0, Math.min(1f, (chargelevel - thresholds[i]) * 10f));
            }
            mids[i].setAlphaMult(alpha);
        }
    }

    private void renderTailAndLight(WeaponAPI weapon, DataContainer data, float chargelevel) {
        SpriteAPI tail = Global.getSettings().getSprite("fx", "Meng_TBTail");
        MagicRenderPlugin.addSingleframe(tail, weapon.getLocation(), CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER);
        tail.setAngle(data.arg1);

        if (chargelevel >= 0.6f) {
            SpriteAPI light = Global.getSettings().getSprite("fx", "Meng_TBlight");
            MagicRenderPlugin.addSingleframe(light, weapon.getLocation(), CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER);
            light.setAngle(data.arg1);
            light.setAlphaMult(Math.max(0, Math.min(1f, (chargelevel - 0.6f) * 3f)));
        }
    }

    private void handleFullCharge(ShipAPI ship, WeaponAPI weapon, DataContainer data, CombatEngineAPI engine, boolean aiControlled, float amount) {
        activetimer += amount;
        init2 = false;

        if (!ready) {
            applyInvulnerability(ship, engine);
            ready = true;
        }

        if (rready) {
            renderGhostShip(ship);
        }

        if (activetimer >= 0.25f && data.energy > 0) {
            performRewind(ship, weapon, data, engine);
        }

        if (data.aifire && nowtime >= data.besttime) {
            data.aifire = false;
        }

        if (aiControlled) {
            ship.setHoldFireOneFrame(true);
            ship.blockCommandForOneFrame(ShipCommand.FIRE);
        }
    }

    private void applyInvulnerability(ShipAPI ship, CombatEngineAPI engine) {
        ship.getMutableStats().getArmorDamageTakenMult().modifyMult(id, 0f);
        ship.getMutableStats().getShieldDamageTakenMult().modifyMult(id, 0f);
        ship.getMutableStats().getHullDamageTakenMult().modifyMult(id, 0f);
        ship.getMutableStats().getMaxTurnRate().modifyMult(id, 3f);
        ship.getMutableStats().getTurnAcceleration().modifyMult(id, 3f);
        ship.getMutableStats().getShieldUnfoldRateMult().modifyMult(id, 3f);

        if (ship == engine.getPlayerShip()) {
            Global.getCombatEngine().getTimeMult().modifyMult(id, 1 / 3f);
        }
    }

    private void renderGhostShip(ShipAPI ship) {
        SpriteAPI ships = ship.getSpriteAPI();
        MagicRenderPlugin.addSingleframe(ships, copyloc, CombatEngineLayers.UNDER_SHIPS_LAYER);
        ships.setAlphaMult(0.3f);
        ships.setAngle(copyarg - 90f);
    }

    private void performRewind(ShipAPI ship, WeaponAPI weapon, DataContainer data, CombatEngineAPI engine) {
        activetimer = 0f;
        rready = true;
        Global.getSoundPlayer().playSound("Meng_nclock", 1.0f, 1.0f, ship.getLocation(), new Vector2f());
        data.energy--;
        nowtime++;

        applyPeakCRPenalty(ship);

        if (locs.size() >= data.energy) {
            restoreState(ship, weapon, data);
        }
    }

    private void applyPeakCRPenalty(ShipAPI ship) {
        if (ship.getMutableStats().getPeakCRDuration().getFlatBonus(id) != null) {
            ship.getMutableStats().getPeakCRDuration().modifyFlat(id, ship.getMutableStats().getPeakCRDuration().getFlatBonus(id).value - 8f);
        } else {
            ship.getMutableStats().getPeakCRDuration().modifyFlat(id, -8f);
        }
    }

    private void restoreState(ShipAPI ship, WeaponAPI weapon, DataContainer data) {
        copyarg = args.removeLast();
        copyammo = ammo.removeLast();
        copyflux = fluxs.removeLast();
        copyhflux = hfluxs.removeLast();
        copyarmor = armors.removeLast();
        copyhit = hits.removeLast();
        copyloc = locs.removeLast();
        minarmor.removeLast();

        ship.getFluxTracker().setCurrFlux(copyflux);
        ship.getFluxTracker().setHardFlux(copyhflux);

        float[][] grid1 = ship.getArmorGrid().getGrid();
        for (int x = 0; x < grid1.length; x++) {
            for (int y = 0; y < grid1[0].length; y++) {
                ship.getArmorGrid().setArmorValue(x, y, copyarmor[x][y]);
            }
        }

        int ammonum = restoreAmmo(ship, copyammo);

        if (ammonum > 0) {
            Global.getCombatEngine().addFloatingText(
                new Vector2f(ship.getLocation().getX() + 300f, ship.getLocation().getY() + 400f),
                "已回溯" + ammonum, 50f, new Color(169, 234, 255, 255), ship, 2f, 2f);
        }

        ship.setHitpoints(copyhit);
    }

    private int restoreAmmo(ShipAPI ship, int[] copyammo) {
        int ammonum = 0;
        int i = 0;
        for (WeaponAPI weapons : ship.getAllWeapons()) {
            i++;
            ammonum = ammonum + (copyammo[i] - weapons.getAmmo());
            weapons.setAmmo(copyammo[i]);
            weapons.setRemainingCooldownTo(0f);
        }
        return ammonum;
    }

    private void handleChargeDecrease(ShipAPI ship, WeaponAPI weapon, DataContainer data, CombatEngineAPI engine) {
        if (init2) return;

        weaponactive = false;
        Global.getCombatEngine().getTimeMult().modifyMult(id, 1f);
        Global.getCombatEngine().getTimeMult().unmodify(id);
        init2 = true;
        timer1 = 0f;
        ready = false;

        if (rready) {
            Global.getSoundPlayer().playSound("Meng_TransSound_active", 1.0f, 1.0f, ship.getLocation(), new Vector2f());
            Global.getCombatEngine().addLayeredRenderingPlugin(new Meng_ClockbuilderPlugin(weapon, copyarg, copyloc, id, TBF1, TBF2, TBF3));

            // 强制禁用武器开火
            weapon.setForceNoFireOneFrame(true);
        }
    }

    public static class DataContainer {
        public float arg = 165f;
        public boolean timestop = false;
        public boolean aifire = false;
        public int energy = 0;
        public int besttime = 0;
        public boolean active = false;
        float arg1 = 0f;
    }

    public static class Meng_ClockbuilderPlugin implements CombatLayeredRenderingPlugin {
        private final WeaponAPI weapons;
        Vector2f locs;
        Vector2f lastlocs;
        float args;
        String id;
        SpriteAPI TBF1;
        SpriteAPI TBF2;
        SpriteAPI TBF3;
        private float timer1;
        private boolean init1 = false;
        private float timer2 = 0f;
        private float timer = 0f;
        private boolean init = false;
        private float[][] noiseMap;
        private float[] segmentSpeeds;
        private int noiseSegments;
        private float shipFacing;
        private WeaponAPI weaponRef;
        // 新增：保存前半段每一帧的噪声状态
        private java.util.List<float[][]> noiseHistory;
        private final EnumSet<CombatEngineLayers> activeLayers= EnumSet.of(CombatEngineLayers.UNDER_SHIPS_LAYER);

        public Meng_ClockbuilderPlugin(WeaponAPI weapon, float arg, Vector2f loc, String ids, SpriteAPI tbf1, SpriteAPI tbf2, SpriteAPI tbf3) {
            weapons = weapon;
            weaponRef = weapon;
            args = arg;
            locs = loc;
            id = ids;
            TBF1 = tbf1;
            TBF2 = tbf2;
            TBF3 = tbf3;
            this.noiseSegments = 64;
            this.segmentSpeeds = new float[noiseSegments];
            this.shipFacing = arg;
            this.noiseHistory = new java.util.ArrayList<>();

        }

        public void init(CombatEntityAPI entity) {
            generateNoiseMap();
        }
        @Override
        public void cleanup() {
            destroyFBO();
        }

        @Override
        public boolean isExpired() {
            return timer >= 5f;
        }


        @Override
        public void advance(float amount) {
            ShipAPI ships = weapons.getShip();
            if (timer <= 4.5f && ships.getAI() != null) {
                ships.setHoldFireOneFrame(true);
                ships.blockCommandForOneFrame(ShipCommand.FIRE);
            }

            DataContainer data = getDataContainer(ships);
            timer += amount;
            timer2 += amount;
            if (!init) {
                init = true;
                lastlocs = new Vector2f(ships.getLocation().getX(), ships.getLocation().getY());
                shipFacing = ships.getFacing();
                createRippleEffect(ships.getLocation());
            }
            if(timer2 <= 0.2f) {
                timer=0f;
                ships.setAlphaMult(1f-timer2*5f);
                setClockSpritesAlpha(1f-timer2*5f);
                ships.getLocation().set(lastlocs);
            }
            if(timer == 0f) return;

            // 前半段：更新噪声变形（0-1.5 秒）
            if (timer <= 1.5f) {
                updateNoiseDeformation(amount);
                saveNoiseFrame();
                if (weaponRef != null) weaponRef.setForceNoFireOneFrame(true);
            }

            // 空闲间隔：1.5-2.5 秒，锁定位置，不可见
            if (timer > 1.5f && timer <= 2.5f) {
                ships.getLocation().set(locs);
                ships.setAlphaMult(0f);
                setClockSpritesAlpha(0f);
                data.active = false;
                if (weaponRef != null) weaponRef.setForceNoFireOneFrame(true);
            }

            // 后半段：更新展开噪声变形（2.5-4.0 秒）
            if (timer > 2.5f && timer <= 4.0f) {
                ships.getLocation().set(locs);
                if (timer - amount <= 2.5f) {
                    createRippleEffect(locs);
                    Global.getSoundPlayer().playSound("Meng_TransSound_end", 1.0f, 1.0f, ships.getLocation(), new Vector2f());
                }
                updateExpandNoiseDeformation(amount);
                if (weaponRef != null) weaponRef.setForceNoFireOneFrame(true);
            }

            // 收缩完成闪光 @ 1.2s
            if (timer >= 1.2f && timer - amount < 1.2f) {
                createFlashEffect(lastlocs);
            }

            // 展开开始闪光 @ 2.5s（原 1.5s + 1.0s）
            if (timer >= 2.5f && timer - amount < 2.5f) {
                createFlashEffect(locs);
            }

            if (timer <= 1.5f) {
                handleFadeOutPhase(ships, data, amount);
            } else {
                handleFadeInPhase(ships, data, amount);
            }

            if (timer >= 4.5f) {
                cleanupModifiers(ships);
            }
        }

        private void generateNoiseMap() {
            noiseMap = new float[noiseSegments][2];
            Random random = new Random(42);

            for (int i = 0; i < noiseSegments; i++) {
                float angle = (float) (2 * Math.PI * i / noiseSegments);

                noiseMap[i][0] = angle;
                noiseMap[i][1] = 1.0f;

                segmentSpeeds[i] = 0.2f + random.nextFloat() * 1.8f;
            }
        }


        private void updateNoiseDeformation(float amount) {
            float progress = timer1 / 1.5f;
            if (progress > 1.0f) return;

            float phase0End = 0.133f;
            float phase1End = 0.6f;

            for (int i = 0; i < noiseSegments; i++) {
                float currentNoiseFactor = noiseMap[i][1];

                float turbulence1 = (float) (Math.sin(i * 0.5 + timer1 * 6.0) * 0.06);
                float turbulence2 = (float) (Math.cos(i * 0.9 - timer1 * 4.5) * 0.05);
                float turbulence3 = (float) (Math.sin(i * 1.4 + timer1 * 7.5) * 0.04);
                float turbulence4 = (float) (Math.cos(i * 2.0 - timer1 * 5.5) * 0.03);

                float dynamicTurbulence = turbulence1 + turbulence2 + turbulence3 + turbulence4;

                if (progress < phase0End) {
                    currentNoiseFactor = 1.0f;

                } else if (progress < phase1End) {
                    float speedMultiplier = segmentSpeeds[i];
                    float contractionAmount = 0.012f * speedMultiplier * amount * 60.0f;
                    float irregularity = 1.0f + dynamicTurbulence * 0.7f;

                    currentNoiseFactor -= contractionAmount * irregularity;
                    currentNoiseFactor = Math.max(0.1f, Math.min(1.4f, currentNoiseFactor));

                } else {
                    float phase2Progress = (progress - phase1End) / (1.0f - phase1End);

                    float slowThreshold = 0.4f;
                    if (currentNoiseFactor > slowThreshold) {
                        float accelerationFactor = 2.0f + phase2Progress * 3.5f;
                        float fastContraction = 0.02f * accelerationFactor * amount * 60.0f;

                        currentNoiseFactor -= fastContraction;
                        currentNoiseFactor = Math.max(0.0f, currentNoiseFactor);
                    }

                    float smoothFactor = 1.0f - phase2Progress;
                    currentNoiseFactor = currentNoiseFactor * smoothFactor + 0.0f * (1.0f - smoothFactor);
                }

                noiseMap[i][1] = currentNoiseFactor;
            }
        }

        /**
         * 保存当前帧的噪声状态
         */
        private void saveNoiseFrame() {
            float[][] frameCopy = new float[noiseSegments][2];
            for (int i = 0; i < noiseSegments; i++) {
                frameCopy[i][0] = noiseMap[i][0];
                frameCopy[i][1] = noiseMap[i][1];
            }
            noiseHistory.add(frameCopy);
        }

        /**
         * 后半段展开噪声更新 - 直接倒序读取前半段保存的状态
         */
        private void updateExpandNoiseDeformation(float amount) {
            float expandTimer = timer - 2.5f;
            float progress = expandTimer / 1.5f;
            if (progress > 1.0f) return;

            if (noiseHistory == null || noiseHistory.isEmpty()) return;

            // 计算应该读取的历史帧索引（倒序）
            int totalFrames = noiseHistory.size();
            int frameIndex = (int) (progress * (totalFrames - 1));
            frameIndex = Math.max(0, Math.min(totalFrames - 1, frameIndex));

            // 直接读取历史帧
            float[][] targetFrame = noiseHistory.get(totalFrames - 1 - frameIndex);

            for (int i = 0; i < noiseSegments; i++) {
                noiseMap[i][0] = targetFrame[i][0];
                noiseMap[i][1] = targetFrame[i][1];
            }
        }

        private void createFlashEffect(Vector2f location) {

            ShipAPI ship = weapons.getShip();
            float radius = ship.getShieldRadiusEvenIfNoShield() * 2f;

            FlareEntity flare = new FlareEntity();
            flare.setSmoothDisc();
            flare.setSize(radius, radius * 0.02f);
            flare.autoAspect();
            flare.setDiscRatio(0.5f);
            flare.setCoreColor(new Color(30, 100, 255, 255));
            flare.setFringeColor(new Color(150, 200, 255, 255));

            flare.setLocation(location);
            flare.setFlick(true);
            flare.setFlickMixValue(0.3f);
            flare.setFlickWhenPaused(false);
            flare.setSyncFlick(false);
            flare.setGlowPower(1.0f);
            flare.setAdditiveBlend();
            flare.setGlobalTimer(0.0f, 0.5f, 0.3f);
            flare.setLayer(CombatEngineLayers.ABOVE_SHIPS_LAYER);
            CombatRenderingManager.addEntity(flare);
        }

        private void handleFadeOutPhase(ShipAPI ships, DataContainer data, float amount) {
            ships.getLocation().set(lastlocs);

            // 提前设置舰船和时钟精灵透明
            ships.setAlphaMult(0f);
            setClockSpritesAlpha(0f);
            data.active = false;

            if (shouldToggleShieldOff(ships)) {
                ships.getShield().toggleOff();
            }

            timer1 += amount;
        }

        private void handleFadeInPhase(ShipAPI ships, DataContainer data, float amount) {
            if (!init1) {
                init1 = true;
                ships.getLocation().set(locs);
                ships.setFacing(args);
            }

            // 保持舰船和时钟精灵透明，直到特效结束
            ships.setAlphaMult(0f);
            setClockSpritesAlpha(0f);
            data.active = false;

            // 在 4.0-4.1 秒之间恢复透明度（0.1秒）
            if (timer >= 4.0f && timer < 4.1f) {
                float restoreProgress = (timer - 4.0f) / 0.1f;
                restoreProgress = Math.max(0f, Math.min(1f, restoreProgress));
                ships.setAlphaMult(restoreProgress);
                setClockSpritesAlpha(restoreProgress);
            } else if (timer >= 4.1f) {
                ships.setAlphaMult(1f);
                setClockSpritesAlpha(1f);
            }
        }

        private boolean shouldToggleShieldOff(ShipAPI ship) {
            return ship.getVariant().getHullSpec().getShieldType() != ShieldAPI.ShieldType.NONE &&
                    ship.getShield() != null &&
                    ship.getVariant().getHullSpec().getShieldType() != ShieldAPI.ShieldType.PHASE;
        }

        private void cleanupModifiers(ShipAPI ships) {
            for (WeaponAPI weapons : ships.getAllWeapons()) {
                weapons.repair();
            }
            ships.getMutableStats().getArmorDamageTakenMult().unmodifyMult(id);
            ships.getMutableStats().getShieldDamageTakenMult().unmodifyMult(id);
            ships.getMutableStats().getHullDamageTakenMult().unmodifyMult(id);
            ships.getMutableStats().getTurnAcceleration().unmodifyMult(id);
            ships.getMutableStats().getMaxTurnRate().unmodifyMult(id);
            ships.getMutableStats().getShieldUnfoldRateMult().unmodifyMult(id);
        }

        private DataContainer getDataContainer(ShipAPI ship) {
            if (!ship.getCustomData().containsKey(KEY)) {
                DataContainer data = new DataContainer();
                ship.setCustomData(KEY, data);
                return data;
            }
            return (DataContainer) ship.getCustomData().get(KEY);
        }

        private void createRippleEffect(Vector2f location) {
            RippleDistortion ripple1 = new RippleDistortion(location, new Vector2f());
            ripple1.setSize(3000f);
            ripple1.setIntensity(1000f);
            ripple1.fadeInSize(5f);
            ripple1.fadeOutIntensity(3f);
            ripple1.setFrameRate(60f);
            DistortionShader.addDistortion(ripple1);
        }

        @Override
        public EnumSet<CombatEngineLayers> getActiveLayers() {
            return activeLayers;
        }

        @Override
        public float getRenderRadius() {
            return 10000000f;
        }

        // FBO for ship + weapon compositing
        private int fboId = 0;
        private int fboTexId = 0;
        private int fboW = 0;
        private int fboH = 0;

        @Override
        public void render(CombatEngineLayers layer, ViewportAPI viewport) {
            if (layer != CombatEngineLayers.UNDER_SHIPS_LAYER) return;
            if (!init) return;
            if (lastlocs == null) return;
            if (timer > 4.1f) return;
            if (timer > 1.5f && timer < 2.5f) return;

            ShipAPI ship = weapons.getShip();
            SpriteAPI shipSprite = ship.getSpriteAPI();
            if (shipSprite == null) return;

            float spriteWidth = shipSprite.getWidth();
            float spriteHeight = shipSprite.getHeight();

            float progress, alpha, globalScale;
            Vector2f renderLoc;
            float renderRot;

            if (timer <= 1.5f) {
                progress = timer / 1.5f;
                if (progress > 1.0f) return;
                alpha = 1.0f - progress;
                globalScale = 1.0f - (float) Math.pow(progress, 2.5);
                renderLoc = lastlocs;
                renderRot = shipFacing;
            } else {
                float expandTimer = timer - 2.5f;
                progress = expandTimer / 1.5f;
                if (progress > 1.0f) progress = 1.0f;
                alpha = progress;
                globalScale = (float) Math.pow(progress, 0.4f);
                renderLoc = ship.getLocation();
                renderRot = ship.getFacing();
            }
            alpha = Math.max(0.0f, Math.min(1.0f, alpha));

            java.nio.FloatBuffer before = org.lwjgl.BufferUtils.createFloatBuffer(16);
            GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, before);


            int texId = compositeToFBO(shipSprite, ship, spriteWidth, spriteHeight);

            // 强制重新绑定，无论compositeToFBO里发生了什么
            if (texId != 0) {
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, texId);
            }
            int texW = fboW;
            int texH = fboH;
            boolean useFBO = (texId != 0);

            float uvScaleX = spriteWidth / (float) texW;
            float uvScaleY = spriteHeight / (float) texH;
            float uvOffX = (236f - spriteWidth * 0.5f) / (float) texW;
            float uvOffY = (406f - spriteHeight * 0.5f) / (float) texH;

            float halfW = spriteWidth * 0.5f;
            float halfH = spriteHeight * 0.5f;

            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPushMatrix();
            GL11.glMatrixMode(GL11.GL_TEXTURE);
            GL11.glPushMatrix();
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPushMatrix();

            GL11.glTranslatef(renderLoc.x, renderLoc.y, 0f);
            GL11.glRotatef(renderRot - 90f, 0f, 0f, 1f);

            if (useFBO) {
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, texId);
            } else {
                shipSprite.bindTexture();
            }
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4f(1f, 1f, 1f, alpha);

            GL11.glBegin(GL11.GL_TRIANGLE_FAN);
            GL11.glTexCoord2f(0.5f * uvScaleX + uvOffX, 0.5f * uvScaleY + uvOffY);
            GL11.glVertex2f(0f, 0f);

            for (int i = 0; i <= noiseSegments; i++) {
                int index = i % noiseSegments;
                float angle = noiseMap[index][0];
                float noiseFactor = noiseMap[index][1];
                float ca = (float) Math.cos(angle);
                float sa = (float) Math.sin(angle);

                float x = ca * halfW * noiseFactor * globalScale;
                float y = sa * halfH * noiseFactor * globalScale;
                float u = 0.5f * uvScaleX + uvOffX + 0.5f * ca * uvScaleX;
                float v = 0.5f * uvScaleY + uvOffY + 0.5f * sa * uvScaleY;

                GL11.glTexCoord2f(u, v);
                GL11.glVertex2f(x, y);
            }
            GL11.glEnd();

            GL11.glDepthMask(true);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_TEXTURE);
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPopMatrix();

            GL11.glPopAttrib();
        }

        // ===== FBO compositing =====

        private void ensureFBO(int w, int h) {
            int tw = nextPowerOfTwo(w);
            int th = nextPowerOfTwo(h);
            if (fboId != 0 && fboW == tw && fboH == th) return;
            destroyFBO();
            fboW = tw;
            fboH = th;

            fboTexId = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, fboTexId);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, fboW, fboH,
                    0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (java.nio.ByteBuffer) null);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);

            fboId = GL30.glGenFramebuffers();
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fboId);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                    GL11.GL_TEXTURE_2D, fboTexId, 0);
            int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
            if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
                destroyFBO();
                return;
            }
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        }

        private void destroyFBO() {
            if (fboId != 0) { GL30.glDeleteFramebuffers(fboId); fboId = 0; }
            if (fboTexId != 0) { GL11.glDeleteTextures(fboTexId); fboTexId = 0; }
        }

        private int compositeToFBO(SpriteAPI shipSprite, ShipAPI ship, float sw, float sh) {
            ensureFBO((int) sw, (int) sh);
            if (fboId == 0) {
                shipSprite.bindTexture();
                return 0;
            }

            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            GL20.glUseProgram(0);

            if (ShaderLib.useBufferCore()) {
                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fboId);
            } else if (ShaderLib.useBufferARB()) {
                ARBFramebufferObject.glBindFramebuffer(ARBFramebufferObject.GL_FRAMEBUFFER, fboId);
            } else {
                EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, fboId);
            }

            GL11.glViewport(0, 0, fboW, fboH);

            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPushMatrix();
            GL11.glLoadIdentity();
            GL11.glOrtho(0, fboW, 0, fboH, -2000, 2000);
            GL11.glMatrixMode(GL11.GL_TEXTURE);
            GL11.glPushMatrix();
            GL11.glLoadIdentity();
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPushMatrix();
            GL11.glLoadIdentity();



            float centerX = 236f;
            float centerY = 406f;
            float shipLeft = 0f;
            float shipBot = 0f;
            float uScale = sw / (float) fboW;
            float vScale = sh / (float) fboH;
            float uOff = shipLeft / (float) fboW;
            float vOff = shipBot / (float) fboH;

            shipSprite.bindTexture();
            shipSprite.setNormalBlend();
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColorMask(true, true, true, true);
            GL11.glClearColor(0f, 0f, 0f, 0f);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            GL11.glColor4f(1f, 1f, 1f, 1f);

            GL11.glBegin(GL11.GL_QUADS);
            GL11.glTexCoord2f(uOff, vOff);                    GL11.glVertex2f(shipLeft, shipBot);
            GL11.glTexCoord2f(uOff + uScale, vOff);           GL11.glVertex2f(shipLeft + sw, shipBot);
            GL11.glTexCoord2f(uOff + uScale, vOff + vScale);  GL11.glVertex2f(shipLeft + sw, shipBot + sh);
            GL11.glTexCoord2f(uOff, vOff + vScale);           GL11.glVertex2f(shipLeft, shipBot + sh);
            GL11.glEnd();

            float facing = ship.getFacing();
            for (WeaponAPI wea : ship.getAllWeapons()) {
                for (WeaponSlotAPI slot : ship.getHullSpec().getAllWeaponSlotsCopy()) {
                    if (wea.getSlot() != slot) continue;
                    SpriteAPI wsp = wea.getSprite();
                    if (wsp == null) continue;

                    float slotLocalX = slot.getLocation().x;
                    float slotLocalY = slot.getLocation().y;
                    float rotatedX = -slotLocalY;
                    float rotatedY = slotLocalX;
                    float wsX = centerX + rotatedX;
                    float wsY = centerY + rotatedY;
                    if (slot.isHardpoint()) {
                        float slotAngleRad = (float) Math.toRadians(slot.getAngle() - facing);
                        wsX -= 8f * (float) Math.cos(slotAngleRad);
                        wsY -= 8f * (float) Math.sin(slotAngleRad);
                    }
                    float wsHalfW = wsp.getWidth() * 0.5f;
                    float wsHalfH = wsp.getHeight() * 0.5f;
                    int weaponTexWidth = nextPowerOfTwo((int) wsp.getWidth());
                    int weaponTexHeight = nextPowerOfTwo((int) wsp.getHeight());
                    float wsUScale = wsp.getWidth() / (float) weaponTexWidth;
                    float wsVScale = wsp.getHeight() / (float) weaponTexHeight;

                    GL11.glPushMatrix();
                    GL11.glTranslatef(wsX, wsY, 0f);
                    GL11.glRotatef(wea.getCurrAngle() - facing, 0f, 0f, 1f);
                    wsp.bindTexture();
                    wsp.setNormalBlend();
                    GL11.glEnable(GL11.GL_BLEND);
                    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                    GL11.glColor4f(1f, 1f, 1f, 1f);
                    GL11.glBegin(GL11.GL_QUADS);
                    GL11.glTexCoord2f(0, 0);               GL11.glVertex2f(-wsHalfW, -wsHalfH);
                    GL11.glTexCoord2f(wsUScale, 0);        GL11.glVertex2f( wsHalfW, -wsHalfH);
                    GL11.glTexCoord2f(wsUScale, wsVScale); GL11.glVertex2f( wsHalfW,  wsHalfH);
                    GL11.glTexCoord2f(0, wsVScale);        GL11.glVertex2f(-wsHalfW,  wsHalfH);
                    GL11.glEnd();
                    GL11.glPopMatrix();
                    break;
                }
            }

            GL11.glDisable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_TEXTURE_2D);

            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_TEXTURE);
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPopMatrix();

            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
            GL11.glPopAttrib();

            // 还原viewport

            // 绑定FBO纹理供render使用
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, fboTexId);

            return fboTexId;
        }
        private int nextPowerOfTwo(int value) {
            if (value <= 0) return 1;
            value--;
            value |= value >> 1; value |= value >> 2;
            value |= value >> 4; value |= value >> 8;
            value |= value >> 16;
            return value + 1;
        }

        private void setClockSpritesAlpha(float alpha) {
            TBF1.setAlphaMult(alpha);
            TBF2.setAlphaMult(alpha);
            TBF3.setAlphaMult(alpha);
        }
    }
    public static class Meng_ClockbuilderEnergyPlugin implements CombatLayeredRenderingPlugin {
        private final WeaponAPI weapons;
        float timer = 0f;
        float time;
        float time1;

        public Meng_ClockbuilderEnergyPlugin(WeaponAPI weapon, float timer1, float timer2) {
            weapons = weapon;
            time = timer1;
            time1 = timer2;
        }

        public void init(CombatEntityAPI entity) {
        }

        @Override
        public void cleanup() {
        }

        @Override
        public boolean isExpired() {
            return timer >= 0.5f;
        }

        @Override
        public void advance(float amount) {
            ShipAPI ships = weapons.getShip();
            DataContainer data = getDataContainer(ships);
            float timemult = Global.getCombatEngine().getTimeMult().getMult();

            timer += timer / 30f + amount / timemult;

            float t = 0.5f - timer;
            Vector2f loc = new Vector2f(
                weapons.getLocation().getX() + 500f * t * 2 * (float) Math.cos(Math.toRadians(data.arg1 + 90f)),
                weapons.getLocation().getY() + 500f * t * 2 * (float) Math.sin(Math.toRadians(data.arg1 + 90f))
            );

            renderEnergySprites(loc, data, t);
        }

        private void renderEnergySprites(Vector2f loc, DataContainer data, float t) {
            SpriteAPI BEL = Global.getSettings().getSprite("fx", "Meng_BEL");
            SpriteAPI SEL = Global.getSettings().getSprite("fx", "Meng_SEL");

            MagicRenderPlugin.addSingleframe(BEL, loc, CombatEngineLayers.ABOVE_SHIPS_LAYER);
            MagicRenderPlugin.addSingleframe(SEL, loc, CombatEngineLayers.ABOVE_SHIPS_LAYER);

            BEL.setAlphaMult(1);
            BEL.setSize((100f + 20 * time) * 2 * t, (100f + 20 * time) * 2 * t);
            BEL.setAngle((data.energy + 1) * 24f + 30f * time1);

            SEL.setAlphaMult(1);
            SEL.setSize(120f * 2 * t, 120f * 2 * t);
            SEL.setAngle((data.energy + 1) * 24f);
        }

        private DataContainer getDataContainer(ShipAPI ship) {
            if (!ship.getCustomData().containsKey(KEY)) {
                DataContainer data = new DataContainer();
                ship.setCustomData(KEY, data);
                return data;
            }
            return (DataContainer) ship.getCustomData().get(KEY);
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