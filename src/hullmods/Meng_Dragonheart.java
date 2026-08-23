package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import data.methods.Meng_BlackHolePlugin;
import data.methods.Meng_arcfind;
import data.scripts.plugins.MagicTrailPlugin;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.EnumSet;
import java.util.Objects;

public class Meng_Dragonheart extends BaseHullMod {
    public static final String KEY = "Meng_Dragonheartlistener";
    public static final String id = "Meng_Dragonheartsign";
    float ids = MagicTrailPlugin.getUniqueID();
    Meng_BlackHolePlugin blackHole;
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float opad = 12.0F;
        Color highlight = new Color(255, 232, 87, 255);
        TooltipMakerAPI text;
        LabelAPI label = tooltip.addPara(
                "来自此舰船的所有伤害将转化为未知的力量，注入龙的心脏，当舰船充盈龙力时，将会消耗所有龙力改变下一次轴炮的发射方式。",
                opad, highlight);
        label.setHighlight();
        label.setHighlightColors(highlight, highlight, highlight, highlight, highlight, highlight, highlight);

        tooltip.addSectionHeading("数据分析", Alignment.MID, opad);
        tooltip.addPara("", 0f);
        text = tooltip.beginImageWithText("graphics/icons/hullsys/emp_emitter.png", 32);
        LabelAPI label1 = text.addPara("完全充能喷吐的龙息将会持续造成共计 %s 点能量伤害。",
                2f, highlight,
                "40000");
        label1.setHighlight(
                "40000");
        label1.setHighlightColors(highlight);

        tooltip.addImageWithText(0f);
        tooltip.addPara("#舰船造成的伤害会充盈至舰船首部的能量槽中。当龙眼溢出流光时，代表充能已经完成。", 4f);
        tooltip.addSectionHeading("圣殿史录", Alignment.MID, 20f);


        LabelAPI label2 = tooltip.addPara(
                "“神话中的事物在现实中出现，巨龙喷吐出遮天的怒焰，而一切都将在其中湮灭。”",
                opad, new Color(106, 255, 255, 218), "“神话中的事物在现实中出现，巨龙喷吐出遮天的怒焰，而一切都将在其中湮灭。”"

        );
        label2.setHighlight(0, 100);
        LabelAPI para = tooltip.addSectionHeading("阶梯计划", Alignment.MID, opad);
        para.setHighlightColors(new Color(0, 255, 221, 218));
        para.setHighlight(0, 100);

        label = tooltip.addPara(
                "?阶梯计划在创立之初便做好了失控的应对，当舰队中同时出现一艘以上的阶梯计划舰船时，所有的阶梯计划舰船的系统都会全部下线。",
                opad, highlight);
        label.italicize();
        label.setHighlightColors(new Color(250, 0, 0, 255));
        label.setHighlight(0, 100);
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (isInPlayerFleet(ship)) {
            PersonAPI fc = null;
            FleetMemberAPI member = ship.getFleetMember();
            if (member != null) {
                for (FleetMemberAPI mem : ship.getFleetMember().getFleetData().getMembersListCopy()) {
                    if (mem.getHullSpec() != member.getHullSpec() && mem.getHullSpec().getTags().contains("Meng_embers") && !mem.isMothballed()) {
                        ship.setCurrentCR(0f);
                    }
                }
            }
        }
        for (ShipAPI target : Global.getCombatEngine().getShips()) {
            if (target != null) {
                if (target.getOwner() != ship.getOwner()) {
                    if (target.isAlive() && ship.isAlive()) {
                        //筛选全场符合要求的舰船。
                        if (!target.getCustomData().containsKey(KEY)) {
                            DataContainer data = new DataContainer();
                            target.setCustomData(KEY, data);
                        }
                        DataContainer data = (DataContainer) target.getCustomData().get(KEY);
                        //为目标舰船附上专属的数据库。
                        data.ship = ship;
                        if (!target.hasListenerOfClass(MyDamageListener1.class)) {
                            target.addListener(new MyDamageListener1(target));
                        }

                    }
                }
            }
        }


        if (!ship.getCustomData().containsKey(KEY)) {
            DataContainer data1 = new DataContainer();
            ship.setCustomData(KEY, data1);
        }
        DataContainer data1 = (DataContainer) ship.getCustomData().get(KEY);
        if(!data1.init) {
            data1.init=true;
            blackHole = new Meng_BlackHolePlugin(
                    ship.getLocation(), 500f);
            blackHole.setLifetime(100000f);
            Global.getCombatEngine().addLayeredRenderingPlugin(blackHole);
        }
        if(data1.init) blackHole.setPosition(Global.getCombatEngine().getPlayerShip().getMouseTarget());
        //为每一艘目标舰船添加监听。
        for (WeaponAPI weapon : ship.getAllWeapons()) {
            for (int num = 1; num < 5; num++) {
                if (Objects.equals(weapon.getSpec().getWeaponId(), "Meng_Dragonfirelight_" + num)) {
                    SpriteAPI sprite = weapon.getSprite();
                    sprite.setColor(new Color(255, 255, 255, Math.max(0, Math.min(255, Math.round(255 * (data1.level - num + 1))))));
                }
            }
        }
        if (data1.level >= 4f) {

            for (WeaponAPI weapon : ship.getAllWeapons()) {
                if (Objects.equals(weapon.getSpec().getWeaponId(), "Meng_Dragonfirelight_4")) {
                    SpriteAPI sprite = Global.getSettings().getSprite("fx", "base_trail_smooth");
                    float arg = ship.getFacing() - 7.6f;
                    float arg1 = ship.getFacing() + 7.6f;
                    float arg2 = ship.getFacing() - 7.5f;
                    float arg3 = ship.getFacing() + 7.5f;
                    Color color = new Color(136, 255, 247, 255);
                    float range = 170f;
                    float range1 = 160f;
                    Vector2f loc = new Vector2f(weapon.getLocation().getX() + (float) Math.cos(Math.toRadians(arg)) * range, weapon.getLocation().getY() + (float) Math.sin(Math.toRadians(arg)) * range);
                    Vector2f loc1 = new Vector2f(weapon.getLocation().getX() + (float) Math.cos(Math.toRadians(arg1)) * range, weapon.getLocation().getY() + (float) Math.sin(Math.toRadians(arg1)) * range);
                    Vector2f loc2 = new Vector2f(weapon.getLocation().getX() + (float) Math.cos(Math.toRadians(arg2)) * range1, weapon.getLocation().getY() + (float) Math.sin(Math.toRadians(arg2)) * range1);
                    Vector2f loc3 = new Vector2f(weapon.getLocation().getX() + (float) Math.cos(Math.toRadians(arg3)) * range1, weapon.getLocation().getY() + (float) Math.sin(Math.toRadians(arg3)) * range1);

                    MagicTrailPlugin.AddTrailMemberAdvanced(ship, ids, sprite
                            , loc, 0f, 0f, Meng_arcfind.Findarc(new Vector2f(0, 0), ship.getVelocity()), 0f, 0f
                            , 15f, 10f, color, color, 1f, 0.2f, 0.1f, 0.5f, true, 100f, 60, 0f, null,
                            null,
                            CombatEngineLayers.ABOVE_SHIPS_LAYER,
                            360f);
                    MagicTrailPlugin.AddTrailMemberAdvanced(ship, ids + 1, sprite
                            , loc1, 0f, 0f, Meng_arcfind.Findarc(new Vector2f(0, 0), ship.getVelocity()), 0f, 0f
                            , 15f, 10f, color, color, 1f, 0.2f, 0.1f, 0.5f, true, 100f, 60, 0f, null,
                            null,
                            CombatEngineLayers.ABOVE_SHIPS_LAYER,
                            360f);
                    MagicTrailPlugin.AddTrailMemberAdvanced(ship, ids + 2, sprite
                            , loc2, 0f, 0f, Meng_arcfind.Findarc(new Vector2f(0, 0), ship.getVelocity()), 0f, 0f
                            , 15f, 10f, color, color, 1f, 0.2f, 0.1f, 0.5f, true, 100f, 60, 0f, null,
                            null,
                            CombatEngineLayers.ABOVE_SHIPS_LAYER,
                            360f);
                    MagicTrailPlugin.AddTrailMemberAdvanced(ship, ids + 3, sprite
                            , loc3, 0f, 0f, Meng_arcfind.Findarc(new Vector2f(0, 0), ship.getVelocity()), 0f, 0f
                            , 15f, 10f, color, color, 1f, 0.2f, 0.1f, 0.5f, true, 100f, 60, 0f, null,
                            null,
                            CombatEngineLayers.ABOVE_SHIPS_LAYER,
                            360f);
                }
            }
        }
        if (Objects.equals(ship.getHullSpec().getHullId(), "Meng_Starkiller") && ship.getVariant().getHullSpec().getShieldType() != ShieldAPI.ShieldType.NONE && ship.getShield() != null && ship.getVariant().getHullSpec().getShieldType() != ShieldAPI.ShieldType.PHASE) {
            ship.getShield().setRadius(ship.getShield().getRadius(), "graphics/fx/shield/Meng_shields256.png", "graphics/fx/shields256ring.png");
            ship.getShield().setRingColor(new Color(28, 184, 190, 218));
        }
    }

    public static class DataContainer {
        public float damage = 0f;
        public float level = 0f;
        ShipAPI ship;
        boolean timeend = false;
        public boolean spread = false;
        boolean init = false;
    }

    private static class MyDamageListener1 implements DamageListener {
        public ShipAPI target;

        public MyDamageListener1(ShipAPI ship) {
            this.target = ship;
        }

        @Override
        public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
            if (!this.target.getCustomData().containsKey(KEY)) {
                DataContainer data = new DataContainer();
                this.target.setCustomData(KEY, data);
            }
            DataContainer data = (DataContainer) this.target.getCustomData().get(KEY);
            if (!data.ship.getCustomData().containsKey(KEY)) {
                DataContainer data1 = new DataContainer();
                data.ship.setCustomData(KEY, data1);
            }
            DataContainer data1 = (DataContainer) data.ship.getCustomData().get(KEY);
            if (source instanceof ShipAPI && this.target.isAlive()) {
                ShipAPI targets = (ShipAPI) source;
                float damage1 = result.getDamageToHull() + result.getDamageToShields() + result.getTotalDamageToArmor();
                if (damage1 > 0f && targets == data.ship) {
                    if (!data1.timeend) {
                        data1.damage += damage1;
                    }
                    //当效果还未触发时，目标所受的伤害总量加入data.damage。
                    data1.level = data1.damage / 10000f;
                }
            }
        }
    }

    public static class Meng_DragonheartPlugin implements CombatLayeredRenderingPlugin {
        private final ShipAPI ships;
        private float timer1;

        public Meng_DragonheartPlugin(ShipAPI ship) {
            ships = ship;
        }

        public void init(CombatEntityAPI entity) {

        }

        @Override
        public void cleanup() {
        }

        @Override
        public boolean isExpired() {
            return timer1 >= 5f;//返回值为true时，Plugin删除，因此当计时器超过三秒后删除。
        }

        @Override
        public void advance(float amount) {
            timer1 += amount;
            for (ShipAPI m : ships.getChildModulesCopy()) {
                switch (m.getStationSlot().getId()) {
                    case "Meng_DAL1":
                    case "Meng_DAL2":
                    case "Meng_DAL3":
                    case "Meng_DAR1":
                    case "Meng_DAR2":
                    case "Meng_DAR3":
                    case "Meng_DWL":
                    case "Meng_DWR":
                        m.addAfterimage(new Color(232, 92, 255, 255), 0f, 0f, -ships.getVelocity().x, -ships.getVelocity().y, 0f, 0f, 0f, 0.8f, true, true, false);
                        break;
                }
            }
            ships.addAfterimage(new Color(255, 171, 171, 255), 0f, 0f, -ships.getVelocity().x, -ships.getVelocity().y, 0f, 0f, 0f, 0.8f, true, true, false);
            if (timer1 >= 4.9f) {
                for (ShipAPI m : ships.getChildModulesCopy()) {
                    m.setPhased(false);
                }
                ships.setPhased(false);
                ships.getMutableStats().getAcceleration().unmodifyMult(id);
                ships.getMutableStats().getDeceleration().unmodifyMult(id);
                ships.getMutableStats().getMaxSpeed().unmodifyFlat(id);
                ships.getMutableStats().getMaxTurnRate().unmodifyMult(id);
                ships.getMutableStats().getZeroFluxMinimumFluxLevel().unmodifyFlat(id);
            } else {
                for (ShipAPI m : ships.getChildModulesCopy()) {
                    m.setPhased(true);
                }
                ships.setPhased(true);
                ships.getMutableStats().getAcceleration().modifyMult(id, 7f);
                ships.getMutableStats().getDeceleration().modifyMult(id, 7f);
                ships.getMutableStats().getMaxSpeed().modifyFlat(id, 400f);
                ships.getMutableStats().getMaxTurnRate().modifyMult(id, 2f);
                ships.getMutableStats().getZeroFluxMinimumFluxLevel().modifyFlat(id, 2f);
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