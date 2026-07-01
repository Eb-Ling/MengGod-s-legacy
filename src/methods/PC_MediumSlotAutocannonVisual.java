package data.methods;

import java.awt.Color;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseCombatLayeredRenderingPlugin;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

/**
 * 可控的中槽多用途自动炮外观覆盖入口。
 *
 * 效果主体放在船插监听器与船插内置渲染器中：
 * 1) 船插监听器只登记安装了本船插的舰船；
 * 2) 内置渲染器只扫描已登记舰船的武器，覆盖战斗中的武器外观；
 * 3) 弹丸重定位只对已登记舰船的有效中槽武器生效。
 */
public class PC_MediumSlotAutocannonVisual extends BaseHullMod {
    public static final String ID = "PC_MediumSlotAutocannonVisual";
    public static final String ENGINE_KEY = "pc_medium_slot_autocannon_visual_renderer";

    private static final String PROJECTILE_KEY = "pc_medium_slot_autocannon_visual_relocated";

    private static final String TURRET_BASE_SPRITE = "graphics/weapons/PC_heavyautocannon_turret_base.png";
    private static final String TURRET_BARREL_SPRITE = "graphics/weapons/PC_heavyautocannon_turret_recoil.png";
    private static final String HARDPOINT_BASE_SPRITE = "graphics/weapons/PC_heavyautocannon_hardpoint_base.png";
    private static final String HARDPOINT_BARREL_SPRITE = "graphics/weapons/PC_heavyautocannon_hardpoint_recoil.png";

    private static final float TURRET_MUZZLE_FORWARD = 25.5f;
    private static final float HARDPOINT_MUZZLE_FORWARD = 21.5f;
    private static final float MUZZLE_LATERAL = 3.5f;
    private static final float RECOIL_DISTANCE = 10f;
    private static final float RECOIL_RETURN_SPEED = 7f;
    private static final float PROJECTILE_RELOCATE_MAX_AGE = 0.12f;
    private static final float VIEWPORT_PADDING = 120f;

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (ship == null) {
            return;
        }

        ship.removeListenerOfClass(MediumSlotAutocannonVisualController.class);
        ship.addListener(new MediumSlotAutocannonVisualController(ship));
        registerShipForVisuals(ship);
    }

    @Override
    public boolean shouldAddDescriptionToTooltip(ShipAPI.HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
        return false;
    }

    private static void registerShipForVisuals(ShipAPI ship) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || ship == null || !hasVisualHullmod(ship)) {
            return;
        }

        MediumSlotAutocannonVisualRenderer renderer = MediumSlotAutocannonVisualRenderer.getOrCreate(engine);
        if (renderer != null) {
            renderer.registerShip(ship);
        }
    }

    private static boolean hasVisualHullmod(ShipAPI ship) {
        return ship != null
                && ship.getVariant() != null
                && ship.getVariant().hasHullMod(ID);
    }

    private static boolean isEligibleMediumWeapon(WeaponAPI weapon) {
        if (weapon == null) {
            return false;
        }

        ShipAPI ship = weapon.getShip();
        if (!hasVisualHullmod(ship) || weapon.getSlot() == null) {
            return false;
        }
        if (weapon.getSize() != WeaponAPI.WeaponSize.MEDIUM) {
            return false;
        }
        if (weapon.isDecorative() || weapon.getSlot().isDecorative()) {
            return false;
        }
        if (weapon.getSlot().isHidden() || weapon.getSlot().isSystemSlot() || !weapon.getSlot().isWeaponSlot()) {
            return false;
        }
        if (weapon.getType() == WeaponAPI.WeaponType.DECORATIVE
                || weapon.getType() == WeaponAPI.WeaponType.SYSTEM
                || weapon.getType() == WeaponAPI.WeaponType.STATION_MODULE
                || weapon.getType() == WeaponAPI.WeaponType.LAUNCH_BAY) {
            return false;
        }

        String id = weapon.getId();
        return id == null || (!id.equals("PC_heavyac_AP") && !id.equals("PC_heavyac_HE")
                && !id.equals("PC_heavyac_VT") && !id.equals("PC_heavyac_AB"));
    }

    private static Vector2f getMuzzleLocation(WeaponAPI weapon, int barrelIndex) {
        if (weapon == null || weapon.getLocation() == null) {
            return null;
        }

        boolean hardpoint = weapon.getSlot() != null && weapon.getSlot().isHardpoint();
        float forward = hardpoint ? HARDPOINT_MUZZLE_FORWARD : TURRET_MUZZLE_FORWARD;
        float lateral = (barrelIndex % 2 == 0) ? MUZZLE_LATERAL : -MUZZLE_LATERAL;
        return getOffsetPoint(weapon.getLocation(), weapon.getCurrAngle(), forward, lateral);
    }

    private static Vector2f getOffsetPoint(Vector2f center, float facing, float forward, float lateral) {
        Vector2f result = new Vector2f(center);
        Vector2f forwardVector = Misc.getUnitVectorAtDegreeAngle(facing);
        Vector2f lateralVector = new Vector2f(-forwardVector.y, forwardVector.x);

        forwardVector.scale(forward);
        lateralVector.scale(lateral);

        Vector2f.add(result, forwardVector, result);
        Vector2f.add(result, lateralVector, result);
        return result;
    }

    public static class MediumSlotAutocannonVisualController implements AdvanceableListener {
        private final ShipAPI ship;

        public MediumSlotAutocannonVisualController(ShipAPI ship) {
            this.ship = ship;
        }

        @Override
        public void advance(float amount) {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine == null || ship == null || !engine.isEntityInPlay(ship) || !ship.isAlive() || ship.isHulk()) {
                return;
            }

            registerShipForVisuals(ship);
        }
    }

    public static class MediumSlotAutocannonVisualRenderer extends BaseCombatLayeredRenderingPlugin {
        private final Set<ShipAPI> registeredShips = new HashSet<ShipAPI>();
        private final Map<WeaponAPI, WeaponVisualState> weaponStates = new HashMap<WeaponAPI, WeaponVisualState>();

        private transient SpriteAPI turretBase;
        private transient SpriteAPI turretBarrel;
        private transient SpriteAPI hardpointBase;
        private transient SpriteAPI hardpointBarrel;

        public static MediumSlotAutocannonVisualRenderer getOrCreate(CombatEngineAPI engine) {
            if (engine == null) {
                return null;
            }

            Object existing = engine.getCustomData().get(ENGINE_KEY);
            if (existing instanceof MediumSlotAutocannonVisualRenderer) {
                return (MediumSlotAutocannonVisualRenderer) existing;
            }

            MediumSlotAutocannonVisualRenderer renderer = new MediumSlotAutocannonVisualRenderer();
            engine.addLayeredRenderingPlugin(renderer);
            engine.getCustomData().put(ENGINE_KEY, renderer);
            return renderer;
        }

        public void registerShip(ShipAPI ship) {
            if (ship != null) {
                registeredShips.add(ship);
            }
        }

        @Override
        public void advance(float amount) {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine == null) {
                restoreAllWeaponSprites();
                registeredShips.clear();
                weaponStates.clear();
                return;
            }

            cleanupRegisteredShips(engine);
            updateWeaponStates(engine, engine.isPaused() ? 0f : amount);
            if (!engine.isPaused()) {
                relocateNewProjectiles(engine);
            }
        }

        private void cleanupRegisteredShips(CombatEngineAPI engine) {
            Iterator<ShipAPI> shipIt = registeredShips.iterator();
            while (shipIt.hasNext()) {
                ShipAPI ship = shipIt.next();
                if (ship == null || !engine.isEntityInPlay(ship) || !ship.isAlive() || ship.isHulk() || !hasVisualHullmod(ship)) {
                    shipIt.remove();
                }
            }
        }

        private void updateWeaponStates(CombatEngineAPI engine, float amount) {
            Set<WeaponAPI> aliveWeapons = new HashSet<WeaponAPI>();

            for (ShipAPI ship : registeredShips) {
                if (ship == null || !engine.isEntityInPlay(ship) || !ship.isAlive() || ship.isHulk()) {
                    continue;
                }

                for (WeaponAPI weapon : ship.getAllWeapons()) {
                    if (!isEligibleMediumWeapon(weapon)) {
                        continue;
                    }

                    WeaponVisualState state = weaponStates.get(weapon);
                    if (state == null) {
                        state = new WeaponVisualState(weapon);
                        weaponStates.put(weapon, state);
                    }
                    state.advance(amount);
                    state.hideOriginalWeaponSprites();
                    aliveWeapons.add(weapon);
                }
            }

            Iterator<Map.Entry<WeaponAPI, WeaponVisualState>> iter = weaponStates.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<WeaponAPI, WeaponVisualState> entry = iter.next();
                WeaponAPI weapon = entry.getKey();
                WeaponVisualState state = entry.getValue();
                if (!aliveWeapons.contains(weapon)) {
                    if (state != null) {
                        state.restoreOriginalWeaponSprites();
                    }
                    iter.remove();
                }
            }
        }

        private void relocateNewProjectiles(CombatEngineAPI engine) {
            for (DamagingProjectileAPI projectile : engine.getProjectiles()) {
                if (projectile == null || projectile.wasRemoved() || projectile.isExpired()) {
                    continue;
                }
                if (projectile.getCustomData().containsKey(PROJECTILE_KEY)) {
                    continue;
                }
                if (projectile.getElapsed() > PROJECTILE_RELOCATE_MAX_AGE) {
                    continue;
                }

                WeaponAPI weapon = projectile.getWeapon();
                if (!isRegisteredEligibleWeapon(weapon) || weapon.isBeam()) {
                    continue;
                }

                WeaponVisualState state = weaponStates.get(weapon);
                if (state == null) {
                    state = new WeaponVisualState(weapon);
                    weaponStates.put(weapon, state);
                }

                Vector2f muzzle = state.getAndAdvanceMuzzleLocation();
                if (muzzle == null || projectile.getLocation() == null) {
                    projectile.setCustomData(PROJECTILE_KEY, Boolean.TRUE);
                    continue;
                }

                float dx = muzzle.x - projectile.getLocation().x;
                float dy = muzzle.y - projectile.getLocation().y;

                projectile.getLocation().x = muzzle.x;
                projectile.getLocation().y = muzzle.y;

                Vector2f tail = projectile.getTailEnd();
                if (tail != null) {
                    tail.x += dx;
                    tail.y += dy;
                }

                state.triggerRecoil();
                projectile.setCustomData(PROJECTILE_KEY, Boolean.TRUE);
            }
        }

        private boolean isRegisteredEligibleWeapon(WeaponAPI weapon) {
            return weapon != null
                    && weapon.getShip() != null
                    && registeredShips.contains(weapon.getShip())
                    && isEligibleMediumWeapon(weapon);
        }

        @Override
        public EnumSet<CombatEngineLayers> getActiveLayers() {
            return EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_LAYER);
        }

        @Override
        public float getRenderRadius() {
            return 999999f;
        }

        @Override
        public boolean isExpired() {
            return false;
        }

        @Override
        public void render(CombatEngineLayers layer, ViewportAPI viewport) {
            if (layer != CombatEngineLayers.ABOVE_SHIPS_LAYER || viewport == null) {
                return;
            }

            ensureSprites();

            for (WeaponVisualState state : weaponStates.values()) {
                if (state == null || !state.isRenderable(viewport)) {
                    continue;
                }
                renderWeaponOverlay(state, viewport.getAlphaMult());
            }
        }

        private void renderWeaponOverlay(WeaponVisualState state, float viewportAlpha) {
            WeaponAPI weapon = state.weapon;
            if (weapon == null || weapon.getLocation() == null) {
                return;
            }

            boolean hardpoint = weapon.getSlot() != null && weapon.getSlot().isHardpoint();
            SpriteAPI base = hardpoint ? hardpointBase : turretBase;
            SpriteAPI barrel = hardpoint ? hardpointBarrel : turretBarrel;
            if (base == null || barrel == null) {
                return;
            }

            float alpha = getWeaponAlpha(weapon) * viewportAlpha;
            if (alpha <= 0.01f) {
                return;
            }

            float facing = weapon.getCurrAngle();
            Vector2f center = weapon.getLocation();
            Vector2f barrelCenter = getBarrelRenderCenter(center, facing, state.recoilLevel);

            renderSprite(barrel, barrelCenter, facing, alpha);
            renderSprite(base, center, facing, alpha);
        }

        private Vector2f getBarrelRenderCenter(Vector2f center, float facing, float recoilLevel) {
            Vector2f result = new Vector2f(center);
            if (recoilLevel <= 0f) {
                return result;
            }

            Vector2f backward = Misc.getUnitVectorAtDegreeAngle(facing + 180f);
            backward.scale(RECOIL_DISTANCE * recoilLevel);
            Vector2f.add(result, backward, result);
            return result;
        }

        private void renderSprite(SpriteAPI sprite, Vector2f center, float facing, float alpha) {
            if (sprite == null || center == null) {
                return;
            }

            sprite.setAngle(facing - 90f);
            sprite.setAlphaMult(alpha);
            sprite.renderAtCenter(center.x, center.y);
            sprite.setAlphaMult(1f);
        }

        private float getWeaponAlpha(WeaponAPI weapon) {
            if (weapon == null) {
                return 0f;
            }

            ShipAPI ship = weapon.getShip();
            float alpha = 1f;
            if (ship != null) {
                alpha *= ship.getAlphaMult();
                alpha *= ship.getExtraAlphaMult();
                alpha *= ship.getExtraAlphaMult2();
            }
            if (weapon.isDisabled() || weapon.isPermanentlyDisabled()) {
                alpha *= 0.45f;
            }
            return Math.max(0f, Math.min(1f, alpha));
        }

        private void ensureSprites() {
            if (turretBase == null) {
                turretBase = Global.getSettings().getSprite(TURRET_BASE_SPRITE);
            }
            if (turretBarrel == null) {
                turretBarrel = Global.getSettings().getSprite(TURRET_BARREL_SPRITE);
            }
            if (hardpointBase == null) {
                hardpointBase = Global.getSettings().getSprite(HARDPOINT_BASE_SPRITE);
            }
            if (hardpointBarrel == null) {
                hardpointBarrel = Global.getSettings().getSprite(HARDPOINT_BARREL_SPRITE);
            }
        }

        private void restoreAllWeaponSprites() {
            for (WeaponVisualState state : weaponStates.values()) {
                if (state != null) {
                    state.restoreOriginalWeaponSprites();
                }
            }
        }
    }

    private static class WeaponVisualState {
        private final WeaponAPI weapon;
        private int nextBarrelIndex = 0;
        private float recoilLevel = 0f;
        private boolean wasFiring = false;
        private boolean originalSpritesHidden = false;
        private Color originalMainColor;
        private Color originalBarrelColor;
        private Color originalUnderColor;
        private Color originalGlowColor;

        private WeaponVisualState(WeaponAPI weapon) {
            this.weapon = weapon;
            rememberOriginalSpriteColors();
        }

        private void advance(float amount) {
            if (amount <= 0f) {
                return;
            }

            recoilLevel = Math.max(0f, recoilLevel - amount * RECOIL_RETURN_SPEED);

            boolean firing = weapon != null && weapon.isFiring();
            if (weapon != null && weapon.isBeam() && firing && !wasFiring) {
                triggerRecoil();
            }
            wasFiring = firing;
        }

        private void rememberOriginalSpriteColors() {
            if (weapon == null) {
                return;
            }

            originalMainColor = weapon.getSprite() == null ? null : weapon.getSprite().getColor();
            originalBarrelColor = weapon.getBarrelSpriteAPI() == null ? null : weapon.getBarrelSpriteAPI().getColor();
            originalUnderColor = weapon.getUnderSpriteAPI() == null ? null : weapon.getUnderSpriteAPI().getColor();
            originalGlowColor = weapon.getGlowSpriteAPI() == null ? null : weapon.getGlowSpriteAPI().getColor();
        }

        private void hideOriginalWeaponSprites() {
            if (weapon == null || originalSpritesHidden) {
                return;
            }

            setSpriteAlpha(weapon.getSprite(), originalMainColor, 0);
            setSpriteAlpha(weapon.getBarrelSpriteAPI(), originalBarrelColor, 0);
            setSpriteAlpha(weapon.getUnderSpriteAPI(), originalUnderColor, 0);
            setSpriteAlpha(weapon.getGlowSpriteAPI(), originalGlowColor, 0);
            originalSpritesHidden = true;
        }

        private void restoreOriginalWeaponSprites() {
            if (weapon == null || !originalSpritesHidden) {
                return;
            }

            if (weapon.getSprite() != null && originalMainColor != null) {
                weapon.getSprite().setColor(originalMainColor);
            }
            if (weapon.getBarrelSpriteAPI() != null && originalBarrelColor != null) {
                weapon.getBarrelSpriteAPI().setColor(originalBarrelColor);
            }
            if (weapon.getUnderSpriteAPI() != null && originalUnderColor != null) {
                weapon.getUnderSpriteAPI().setColor(originalUnderColor);
            }
            if (weapon.getGlowSpriteAPI() != null && originalGlowColor != null) {
                weapon.getGlowSpriteAPI().setColor(originalGlowColor);
            }
            originalSpritesHidden = false;
        }

        private void setSpriteAlpha(SpriteAPI sprite, Color original, int alpha) {
            if (sprite == null) {
                return;
            }

            Color base = original == null ? Color.WHITE : original;
            sprite.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha));
        }

        private Vector2f getAndAdvanceMuzzleLocation() {
            Vector2f point = getMuzzleLocation(weapon, nextBarrelIndex);
            nextBarrelIndex++;
            return point;
        }

        private void triggerRecoil() {
            recoilLevel = 1f;
        }

        private boolean isRenderable(ViewportAPI viewport) {
            if (!isEligibleMediumWeapon(weapon)) {
                return false;
            }
            if (weapon.getLocation() == null) {
                return false;
            }
            ShipAPI ship = weapon.getShip();
            if (ship == null || !ship.isAlive() || ship.isHulk()) {
                return false;
            }
            return viewport == null || viewport.isNearViewport(weapon.getLocation(), VIEWPORT_PADDING);
        }
    }
}