package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import data.methods.Meng_V2arcfind;
import data.methods.Meng_arcfind;
import data.methods.Meng_argfix;
import data.methods.Meng_lightMethod;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.entities.SimpleEntity;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicSettings;

import java.awt.*;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Objects;

public class Meng_fire_core_li extends BaseHullMod {
    public static final String KEY = "Meng_fire_coreeffect";
    public static final String LIGHT_EFFECT_KEY = "Meng_fire_core_light_effect";
    public static final String WING_OFFSET_KEY = "Meng_fire_wing_offset";
    public final float hardfluxred = 50;
    public final float softfluxred = 300;
    public final float PeakCR = 100;
    public final String ids = "Meng_fire_core_id";
    public static final float MAX_WING_ANGLE = 15f;

    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float opad = 12.0F;
        Color highlight = new Color(255, 55, 40, 255);
        LabelAPI label1 = tooltip.addPara(
                "这是火羽计划的结晶，大长老贝塔计划中诞生的最终成品————虚子炉系统，对璃进行特化改装的虚子炉拥有更加强大的性能。",
                opad, highlight
        );
        label1.setHighlight();
        label1.setHighlightColors(highlight, highlight, highlight, highlight, highlight, highlight, highlight);

        tooltip.addSectionHeading("数据分析", Alignment.MID, opad);

        LabelAPI label = tooltip.addPara(
                "#舰船的所有武器将会产生 硬幅能 。\n#舰船获得 %s%% 硬幅能耗散，且软幅能耗散增加至 %s%% 。\n#舰船所受到的峰值降低效果减少 %s%% 。",
                opad, highlight, String.valueOf(Math.round(hardfluxred * 100f)), String.valueOf(Math.round(softfluxred * 100f)), String.valueOf(Math.round(PeakCR * 100f))
        );

        label.setHighlight("硬幅能", Math.round(Math.round(hardfluxred * 100f)) + "%", Math.round(softfluxred * 100f) + "%", Math.round(PeakCR * 100f) + "%");
        label.setHighlightColors(highlight, highlight, highlight, highlight, highlight, highlight, highlight);

    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        super.applyEffectsBeforeShipCreation(hullSize, stats, id);
        stats.getHardFluxDissipationFraction().modifyFlat(id, hardfluxred);
    }


    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {

        if (!ship.isAlive()) return;
        if (ship.getVariant().getHullSpec().getShieldType() != ShieldAPI.ShieldType.NONE && ship.getShield() != null && ship.getVariant().getHullSpec().getShieldType() != ShieldAPI.ShieldType.PHASE) {
            ship.getShield().setRadius(ship.getShield().getRadius(), "graphics/fx/shield/Meng_shields256.png", "graphics/fx/shields256ring.png");
            ship.getShield().setInnerColor(new Color(0, 0, 0, 0));
            ship.getShield().setRingColor(new Color(170, 144, 255, 75));
        }
        boolean specialeffect = false;
        if (ship.getSystem() != null)
            specialeffect = (Objects.equals(ship.getSystem().getId(), "Meng_fire_fluxtrans_attackcore") && ship.getSystem().isActive());
        if (!ship.getCustomData().containsKey(KEY)) {
            DataContainer data = new DataContainer();
            ship.setCustomData(KEY, data);

            String lightWeaponId = ship.getHullSpec().getBaseHullId() + "_light";
            for (WeaponAPI weapon : ship.getAllWeapons()) {
                if (weapon.getSpec().getWeaponId().equals(lightWeaponId)) {
                    weapon.getSprite().setColor(new Color(255,255,255,0));
                    data.hasLightWeapon = true;
                    break;
                }
            }

            if (data.hasLightWeapon && !ship.getCustomData().containsKey(LIGHT_EFFECT_KEY)) {
                Global.getCombatEngine().addLayeredRenderingPlugin(new Meng_Fire_LightPlugin(ship));
                ship.setCustomData(LIGHT_EFFECT_KEY, true);
            }
            
            if (!ship.getCustomData().containsKey(WING_OFFSET_KEY)) {
                Global.getCombatEngine().addLayeredRenderingPlugin(new Meng_Fire_WingOffsetPlugin(ship));
                ship.setCustomData(WING_OFFSET_KEY, true);
            }
            
            if (!ship.getCustomData().containsKey("Meng_HexShield")) {
                Meng_HexShieldPlugin hexPlugin = new Meng_HexShieldPlugin(ship);
                Global.getCombatEngine().addLayeredRenderingPlugin(hexPlugin);
                ship.setCustomData("Meng_HexShield", hexPlugin);
            }
        }
        DataContainer data = (DataContainer) ship.getCustomData().get(KEY);
        if (!data.init) {
            data.init = true;
            MutableShipStatsAPI stats = ship.getMutableStats();
            stats.getPeakCRDuration().modifyFlat(ids, Math.max(0f, -stats.getPeakCRDuration().getFlatBonus() * PeakCR));
            stats.getPeakCRDuration().modifyPercent(ids, Math.max(0f, -stats.getPeakCRDuration().getPercentMod() * PeakCR));
            stats.getPeakCRDuration().modifyMult(ids, (Math.min(1f, stats.getPeakCRDuration().getMult()) * (1 - PeakCR) + PeakCR) / Math.min(1f, stats.getPeakCRDuration().getMult()));
        }
        
        if (ship.getHardFluxLevel() < data.lasthardflux) {
            data.totalhardflux += data.lasthardflux - ship.getHardFluxLevel();
        }
        if (data.totalhardflux >= 0.05f) {
            data.venttime++;
            data.totalhardflux -= 0.05f;
        }
        if (data.venttime == 0) {
            data.ventcolddown = 0f;
        }
        if (data.weapons == null) {
            data.weapons = new ArrayList<>();
        }
        data.weaponflux = 0f;
        for (WeaponAPI weapon : ship.getAllWeapons()) {
            if (weapon.getChargeLevel() == 1f) {
                if (weapon.isBeam()) {
                    data.weaponflux += weapon.getFluxCostToFire() * amount;
                } else {
                    if (!data.weapons.contains(weapon)) {
                        data.weapons.add(weapon);
                        data.weaponflux += weapon.getFluxCostToFire();
                    }
                }
            } else {
                if (!weapon.isBeam()) {
                    data.weapons.remove(weapon);
                }
            }
        }
        FluxTrackerAPI tracker = ship.getFluxTracker();
        if (tracker.getHardFlux() <= tracker.getCurrFlux() - 1f) {
            ship.getMutableStats().getFluxDissipation().modifyMult(KEY, softfluxred);
        } else {
            ship.getMutableStats().getFluxDissipation().unmodifyMult(KEY);
        }
        float hardflux = tracker.getHardFlux();
        if (!specialeffect) {
            if (ship.getVariant().getHullMods().contains("Meng_fire_EntropyInc")) {
                if(ship.getFluxLevel() <= 0.75f) tracker.setCurrFlux(tracker.getCurrFlux()+data.weaponflux*0.25f);
            }
            tracker.setHardFlux(hardflux + data.weaponflux);
        }
        Vector2f coreloc = new Vector2f();
        boolean finded = false;
        for (WeaponAPI w : ship.getAllWeapons()) {
            if (w.getSpec().getWeaponId().equals("Meng_cover")) {
                finded = true;
                coreloc = w.getLocation();
            }
        }
        if (finded) {
            if (ship.getSystem().isOn()){
                if(!data.started) {
                    data.started = true;
                    if (!specialeffect) Global.getCombatEngine().addLayeredRenderingPlugin(new Meng_Fire_DissFlashPlugin(ship, 0.8f, 0));
                    else Global.getCombatEngine().addLayeredRenderingPlugin(new Meng_Fire_DissFlashPlugin(ship, 0.8f, 1));
                }
            }
            else data.started=false;
            float size = ship.getShieldRadiusEvenIfNoShield()*0.6f;

            if (data.venttime > 0) {
                data.ventcolddown -= amount;
                if (data.ventcolddown <= 0f) {
                    data.venttime--;
                    Global.getCombatEngine().addLayeredRenderingPlugin(new Meng_Fire_DissPlugin(ship, size * 0.6f, 1f));
                    Global.getSoundPlayer().playSound("flux_loop", 1.0f, 1.0f, coreloc, new Vector2f());
                    data.ventcolddown = 0.2f;
                }
            }

        } else {
            if (data.venttime > 0) {
                data.ventcolddown -= amount;
                if (data.ventcolddown <= 0f) {
                    data.venttime--;
                    Global.getCombatEngine().addLayeredRenderingPlugin(new Meng_Fire_DissPlugin(ship, ship.getShieldRadiusEvenIfNoShield() * 0.7f, 1f));
                    if (!ship.isFighter())
                        Global.getSoundPlayer().playSound("flux_loop", 1.0f, 1.0f, ship.getLocation(), new Vector2f());
                    else Global.getSoundPlayer().playSound("flux_loop", 1.0f, 0.5f, ship.getLocation(), new Vector2f());

                    data.ventcolddown = 0.2f;
                }
            }
        }
        if (ship.getSystem() != null) {
            if (ship.getSystem().isOn()) data.lighttimer += 3 * amount;
            else data.lighttimer += amount;
        }
        if(data.lighttimer>=6f){
            if (ship.getSystem() != null) {
                if (!ship.getSystem().isOn()) {
                    data.lighttimer=0f;
                    Global.getCombatEngine().addLayeredRenderingPlugin(new Meng_lightMethod(ship, 30f, 3f,new Color(255, 110, 66, 255)));
                }
                else {
                    data.lighttimer=0f;
                    Global.getCombatEngine().addLayeredRenderingPlugin(new Meng_lightMethod(ship, 25f, 1.5f,new Color(255, 14, 8, 255)));

                }
            }
        }
        data.lasthardflux = ship.getHardFluxLevel();
    }

    public static class DataContainer {
        public ArrayList<WeaponAPI> weapons;
        public float weaponflux = 0f;
        public float timer = 0f;
        public float lighttimer = 0f;
        public float lasthardflux = 0f;
        public float totalhardflux = 0f;
        public int venttime = 0;
        public float ventcolddown = 0f;
        public boolean started = false;
        public boolean init = false;
        public boolean hasLightWeapon = false;
    }

    public static class Meng_Fire_DissPlugin implements CombatLayeredRenderingPlugin {
        private ShipAPI ship;
        private WeaponAPI weapon;
        private Vector2f loc;
        private float radius;
        private float args;
        private float times;
        private float timer = 0f;
        private ArrayList<Integer> picturenum = null;

        public Meng_Fire_DissPlugin(ShipAPI s, float r, float time) {
            ship = s;
            radius = r;
            times = time;
        }

        public void init(CombatEntityAPI entity) {
            if (picturenum == null) {
                picturenum = new ArrayList<>();
            }
            for (int i = 0; i < 16; i++) {
                picturenum.add(Math.round((float) Math.floor(Math.random() * 16.9999f)));
            }
            for (WeaponAPI w : ship.getAllWeapons()) {
                if (w.getSpec().getWeaponId().equals("Meng_cover")) {
                    weapon = w;
                }
            }
            if (weapon == null) {
                loc = ship.getLocation();
            }
            args = (float) Math.random() * 360f;
        }

        @Override
        public void cleanup() {
        }

        @Override
        public boolean isExpired() {
            return timer > times;//返回值为true时，Plugin删除。
        }

        @Override
        public void advance(float amount) {
            timer += amount;

        }

        @Override
        public EnumSet<CombatEngineLayers> getActiveLayers() {
            return EnumSet.of(CombatEngineLayers.CONTRAILS_LAYER);
        }

        @Override
        public float getRenderRadius() {
            return 10000000f;
        }

        @Override
        public void render(CombatEngineLayers layer, ViewportAPI viewport) {
            if (layer == CombatEngineLayers.CONTRAILS_LAYER) {
                float level = timer / times;
                SpriteAPI sprite = Global.getSettings().getSprite("fx", "Meng_flux_diss");
                if (weapon != null) {
                    loc = weapon.getLocation();
                }
                for (int i = 0; i < 16; i++) {
                    float arg = args + i * 360f / 16f;
                    Vector2f cloudsloc = new Vector2f(loc.x + radius * (1f + level * 0.75f) * (float) Math.cos(Math.toRadians(arg)), loc.y + radius * (1f + level * 0.75f) * (float) Math.sin(Math.toRadians(arg)));

                    int row = picturenum.get(i) % 4;
                    int column = picturenum.get(i) / 4;
                    float size = radius * 0.25f * (1f + level);
                    GL11.glPushMatrix();

                    GL11.glTranslatef(cloudsloc.x, cloudsloc.y, 0.0f);

                    GL11.glRotatef(0f, 0f, 0f, 1f);
                    GL11.glEnable(GL11.GL_TEXTURE_2D);
                    sprite.bindTexture();
                    GL11.glEnable(GL11.GL_BLEND);
                    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
                    GL11.glColor4f(0.66f, 0.56f, 1f, 0.75f*(1f - level * level));
                    GL11.glBegin(GL11.GL_QUAD_STRIP);

                    GL11.glTexCoord2f(row * 0.25f, column * 0.25f);
                    GL11.glVertex2f(-size, size);
                    GL11.glTexCoord2f((row + 1) * 0.25f, column * 0.25f);
                    GL11.glVertex2f(size, size);
                    GL11.glTexCoord2f(row * 0.25f, (column + 1) * 0.25f);
                    GL11.glVertex2f(-size, -size);
                    GL11.glTexCoord2f((row + 1) * 0.25f, (column + 1) * 0.25f);
                    GL11.glVertex2f(size, -size);
                    GL11.glEnd();

                    GL11.glPopMatrix();


                }
            }
        }

    }
    public static class Meng_Fire_DissFlashPlugin implements CombatLayeredRenderingPlugin {
        private ShipAPI ship;
        private float times;
        private float timer = 0f;
        private WeaponAPI weapon;
        private int sizes;
        private float alpha=0f;
        private float arg=0f;
        private static final IntervalUtil Interval1 = new IntervalUtil(0.5f, 1.0f);
        private static final IntervalUtil Interval = new IntervalUtil(0.25f, 0.5f);

        public Meng_Fire_DissFlashPlugin(ShipAPI s, float time,int size) {
            ship = s;
            sizes=size;
            times = time;
        }

        public void init(CombatEntityAPI entity) {

            for (WeaponAPI w : ship.getAllWeapons()) {
                if (w.getSpec().getWeaponId().equals("Meng_cover")) {
                    weapon = w;
                }
            }

        }

        @Override
        public void cleanup() {
        }

        @Override
        public boolean isExpired() {
            return timer > times+0.5f;//返回值为true时，Plugin删除。
        }

        @Override
        public void advance(float amount) {
            timer += amount;
            arg+=amount;
            if(timer>=times){
                if (ship.getSystem() != null) {
                    if (ship.getSystem().isOn()) timer=0f;
                }
            }

            if(timer>=times) alpha=Math.max(alpha-amount*2f,0f);
            else alpha=Math.min(alpha+amount*2f,1f);
            Interval.advance(amount);
            Interval1.advance(amount);
            Vector2f loc;
            if (weapon != null) {
                loc = weapon.getLocation();
            }
            else return;
            float r=ship.getShieldRadiusEvenIfNoShield();
            if(Interval.intervalElapsed()){
                float arc = 360f * (float) Math.random();
                float rad = r*0.1f+(float) Math.random()*r*0.35f;
                Vector2f point1=new Vector2f(loc.x+rad*(float) Math.cos(Math.toRadians(arc)),loc.y+rad*(float) Math.sin(Math.toRadians(arc)));
                arc = 360f * (float) Math.random();
                rad = r*0.1f+(float) Math.random()*r*0.35f;
                Vector2f point2=new Vector2f(loc.x+rad*(float) Math.cos(Math.toRadians(arc)),loc.y+rad*(float) Math.sin(Math.toRadians(arc)));
                Global.getCombatEngine().spawnEmpArcPierceShields(ship, point1, new SimpleEntity(point2), new SimpleEntity(point2), DamageType.ENERGY, 0f, 0f, 10000f, null
                        , 5f, new Color(180, 155, 255, 255), new Color(140, 120, 230, 255));
            }
            if(Interval1.intervalElapsed()){
                float arc = 360f * (float) Math.random();
                float rad = r*0.1f+(float) Math.random()*r*0.35f;
                Vector2f point1=new Vector2f(loc.x+rad*(float) Math.cos(Math.toRadians(arc)),loc.y+rad*(float) Math.sin(Math.toRadians(arc)));
                arc = 360f * (float) Math.random();
                rad = r*0.1f+(float) Math.random()*r*0.35f;
                Vector2f point2=new Vector2f(loc.x+rad*(float) Math.cos(Math.toRadians(arc)),loc.y+rad*(float) Math.sin(Math.toRadians(arc)));
                Global.getCombatEngine().spawnEmpArcPierceShields(ship, point1, new SimpleEntity(point2), new SimpleEntity(point2), DamageType.ENERGY, 0f, 0f, 10000f, null
                        , 5f, new Color(180, 155, 255, 255), new Color(140, 120, 230, 255));
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
            if(weapon==null) return;
            float size=48f;
            if (sizes == 1) {
                size = 64f;
            }
            size=size*0.5f;
            if (layer == CombatEngineLayers.ABOVE_SHIPS_LAYER) {
                float level = timer / times;
                int num=Math.min(12,(int)Math.floor(level*12f)+1);
                SpriteAPI flash = Global.getSettings().getSprite("Meng_fire", "Meng_fire_flash_"+num);
                Vector2f loc;
                if (weapon != null) {
                    loc = weapon.getLocation();
                }
                else return;

                GL11.glPushMatrix();

                GL11.glTranslatef(loc.x, loc.y, 0.0f);

                GL11.glRotatef(ship.getFacing() - 90f+arg*60f, 0f, 0f, 1f);
                GL11.glEnable(GL11.GL_TEXTURE_2D);
                flash.bindTexture();
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
                Color colors = new Color(255,255,255,Math.max(0, Math.min(255, Math.round(alpha * 255))));
                GL11.glColor4ub((byte) colors.getRed(), (byte) colors.getGreen(), (byte) colors.getBlue(), (byte) colors.getAlpha());

                GL11.glBegin(GL11.GL_QUAD_STRIP);

                GL11.glTexCoord2f(0f, 0f);
                GL11.glVertex2f(-size, -size);
                GL11.glTexCoord2f(0f, 1f);
                GL11.glVertex2f(-size, size);
                GL11.glTexCoord2f(1f, 0f);
                GL11.glVertex2f(size, -size);
                GL11.glTexCoord2f(1f, 1f);
                GL11.glVertex2f(size, size);
                GL11.glEnd();

                GL11.glPopMatrix();



            }
        }

    }

    public static class Meng_Fire_WingOffsetPlugin implements CombatLayeredRenderingPlugin {
        private final ShipAPI ship;
        private boolean valid = true;
        private Vector2f lastVelocity = new Vector2f();

        public Meng_Fire_WingOffsetPlugin(ShipAPI s) {
            ship = s;
        }

        @Override
        public void init(CombatEntityAPI entity) {
        }

        @Override
        public void cleanup() {
        }

        @Override
        public boolean isExpired() {
            return !ship.isAlive() || !valid;
        }

        @Override
        public void advance(float amount) {
            updateWingAngles(ship, amount);
        }

        private void updateWingAngles(ShipAPI ship, float amount) {
            Vector2f currentVelocity = ship.getVelocity();

            // 加速度 = 速度变化量 / 时间
            Vector2f acceleration = new Vector2f(
                    (currentVelocity.x - lastVelocity.x) / amount,
                    (currentVelocity.y - lastVelocity.y) / amount
            );

            // 更新上一帧速度
            lastVelocity.set(currentVelocity);
            float speed = acceleration.length();
            if(currentVelocity.length()>=ship.getMaxSpeed()*0.98f&&(ship.getEngineController().isAccelerating()||ship.getEngineController().isDecelerating()
                    ||ship.getEngineController().isDecelerating()||ship.getEngineController().isStrafingLeft()||ship.getEngineController().isStrafingRight())){
                acceleration = currentVelocity;
                speed = 30f;
            }
            if (speed <= 10f) {
                for (WeaponAPI weapon : ship.getAllWeapons()) {
                    String weaponId = weapon.getSpec().getWeaponId();
                    if (weaponId.equals("Meng_fire_li_leftwing1") ||
                            weaponId.equals("Meng_fire_li_leftwing2")||weaponId.equals("Meng_fire_li_rightwing1") ||
                                    weaponId.equals("Meng_fire_li_rightwing2")) {


                        float targetAngle = ship.getFacing();
                        targetAngle = Meng_argfix.fix(targetAngle);
                        float currentAngle = weapon.getCurrAngle();
                        currentAngle = Meng_argfix.fix(currentAngle);
                        float resultangel;
                        if (Math.abs(targetAngle - currentAngle) <= 180f) {
                            resultangel = targetAngle - currentAngle;
                        } else {
                            if (targetAngle > currentAngle) resultangel = targetAngle - currentAngle - 360f;
                            else resultangel = targetAngle - currentAngle + 360f;
                        }
                        if (Math.abs(resultangel) <= 0.01f) continue;
                        float newAngle = currentAngle + (resultangel) / Math.abs(resultangel) * amount * 20f ;
                        weapon.setFacing(newAngle);
                    }
                }
            }
            else {
                float shipFacing = ship.getFacing();
                if(shipFacing>360f) shipFacing-=360f;
                if(shipFacing<0f) shipFacing+=360f;
                float velocityAngle = Meng_V2arcfind.Findarc(acceleration);
                velocityAngle = Meng_argfix.fix(velocityAngle);
                float relativeAngle = velocityAngle;

                for (WeaponAPI weapon : ship.getAllWeapons()) {
                    String weaponId = weapon.getSpec().getWeaponId();
                    if (weaponId.equals("Meng_fire_li_leftwing1") ||
                            weaponId.equals("Meng_fire_li_leftwing2")) {
                        float arg1 = relativeAngle;
                        float arg2 = shipFacing - 45f;
                        arg2 = Meng_argfix.fix(arg2);
                        float resultangel;
                        if (Math.abs(arg1 - arg2) <= 180f) {
                            resultangel = arg1 - arg2;
                        } else {
                            if (arg1 > arg2) resultangel = arg1 - arg2 - 360f;
                            else resultangel = arg1 - arg2 + 360f;
                        }

                        float targetAngle = shipFacing + Math.max(-MAX_WING_ANGLE, Math.min(MAX_WING_ANGLE, resultangel));
                        targetAngle = Meng_argfix.fix(targetAngle);
                        float currentAngle = weapon.getCurrAngle();
                        currentAngle = Meng_argfix.fix(currentAngle);
                        if (Math.abs(targetAngle - currentAngle) <= 180f) {
                            resultangel = targetAngle - currentAngle;
                        } else {
                            if (targetAngle > currentAngle) resultangel = targetAngle - currentAngle - 360f;
                            else resultangel = targetAngle - currentAngle + 360f;
                        }
                        if (Math.abs(resultangel) <= 0.01f) continue;
                        float newAngle = currentAngle + (resultangel) / Math.abs(resultangel) * amount * 20f * Math.min(2f,speed / 30f);
                        weapon.setFacing(newAngle);
                    } else if (weaponId.equals("Meng_fire_li_rightwing1") ||
                            weaponId.equals("Meng_fire_li_rightwing2")) {
                        float arg1 = relativeAngle;
                        float arg2 = shipFacing + 45f;
                        arg2 = Meng_argfix.fix(arg2);
                        float resultangel;
                        if (Math.abs(arg1 - arg2) <= 180f) {
                            resultangel = arg1 - arg2;
                        } else {
                            if (arg1 > arg2) resultangel = arg1 - arg2 - 360f;
                            else resultangel = arg1 - arg2 + 360f;
                        }

                        float targetAngle = shipFacing + Math.max(-MAX_WING_ANGLE, Math.min(MAX_WING_ANGLE, resultangel));
                        targetAngle = Meng_argfix.fix(targetAngle);
                        float currentAngle = weapon.getCurrAngle();
                        currentAngle = Meng_argfix.fix(currentAngle);
                        if (Math.abs(targetAngle - currentAngle) <= 180f) {
                            resultangel = targetAngle - currentAngle;
                        } else {
                            if (targetAngle > currentAngle) resultangel = targetAngle - currentAngle - 360f;
                            else resultangel = targetAngle - currentAngle + 360f;
                        }
                        if (Math.abs(resultangel) <= 0.01f) continue;
                        float newAngle = currentAngle + (resultangel) / Math.abs(resultangel) * amount * 20f * Math.min(2f,speed / 30f);
                        weapon.setFacing(newAngle);
                    }
                }
            }
        }

        @Override
        public EnumSet<CombatEngineLayers> getActiveLayers() {
            return EnumSet.noneOf(CombatEngineLayers.class);
        }

        @Override
        public float getRenderRadius() {
            return 10000000f;
        }

        @Override
        public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        }
    }

    public static class Meng_Fire_LightPlugin implements CombatLayeredRenderingPlugin {
        private ShipAPI ship;
        private SpriteAPI lightSprite;
        private boolean valid = true;

        public Meng_Fire_LightPlugin(ShipAPI s) {
            ship = s;
            try {
                lightSprite = Global.getSettings().getSprite("Meng_fire", ship.getHullSpec().getBaseHullId() + "_light");
            } catch (Exception e) {
                valid = false;
            }
        }

        @Override
        public void init(CombatEntityAPI entity) {
        }

        @Override
        public void cleanup() {
        }

        @Override
        public boolean isExpired() {
            return !ship.isAlive() || !valid;
        }

        @Override
        public void advance(float amount) {
        }

        @Override
        public EnumSet<CombatEngineLayers> getActiveLayers() {
            return EnumSet.of(CombatEngineLayers.UNDER_SHIPS_LAYER);
        }

        @Override
        public float getRenderRadius() {
            return 10000000f;
        }

        @Override
        public void render(CombatEngineLayers layer, ViewportAPI viewport) {
            if (layer == CombatEngineLayers.UNDER_SHIPS_LAYER && valid && ship.isAlive()) {
                Vector2f shiploc = ship.getLocation();

                GL11.glPushMatrix();
                GL11.glTranslatef(shiploc.x, shiploc.y, 0.0f);
                GL11.glRotatef(ship.getFacing() - 90f, 0f, 0f, 1f);

                lightSprite.bindTexture();
                GL11.glEnable(GL11.GL_TEXTURE_2D);
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

                Color color = new Color(254, 167, 39, 255);
                GL11.glColor4ub((byte) color.getRed(), (byte) color.getGreen(),
                        (byte) color.getBlue(), (byte) color.getAlpha());

                float size = lightSprite.getWidth() * 0.5f;

                GL11.glBegin(GL11.GL_QUAD_STRIP);
                GL11.glTexCoord2f(0f, 0f);
                GL11.glVertex2f(-size, -size);
                GL11.glTexCoord2f(0f, 1f);
                GL11.glVertex2f(-size, size);
                GL11.glTexCoord2f(1f, 0f);
                GL11.glVertex2f(size, -size);
                GL11.glTexCoord2f(1f, 1f);
                GL11.glVertex2f(size, size);
                GL11.glEnd();

                GL11.glPopMatrix();
            }
        }
    }
}
