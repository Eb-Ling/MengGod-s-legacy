package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;

import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.EnumSet;
import java.util.List;

public class Meng_fire_phasecoil extends BaseShipSystemScript {
    private static final String RENDER_PLUGIN_KEY = "Meng_fire_phasecoil_render_plugin";
    private static final float SHIP_ALPHA_MULT = 0.25f;
    private static final float MAX_EXTENSION_TIME = 20.0f;
    private static final String DATA_KEY = "Meng_fire_phasecoil_data";
    private static final float PHASE_DAMAGE_RATE = 0.02f;
    private static final float MIN_HULL_PERCENT = 0.05f;
    private static final float HEAL_REDUCTION = 0.8f;
    private static final float MAX_TIME_MULT = 2.0f;
    
    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        super.apply(stats, id, state, effectLevel);
        ShipAPI ship;
        boolean player = false;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
            player = ship == Global.getCombatEngine().getPlayerShip();
            id = id + "_" + ship.getId();
        } else {
            return;
        }
        
        if (!ship.getCustomData().containsKey(DATA_KEY)) {
            ship.setCustomData(DATA_KEY, new DataContainer());
        }
        DataContainer data = (DataContainer) ship.getCustomData().get(DATA_KEY);
        
        if (state == State.IN && !data.extensionApplied) {
            FluxTrackerAPI tracker = ship.getFluxTracker();
            float currentFlux = tracker.getCurrFlux();
            float maxFlux = ship.getMaxFlux();
            tracker.setCurrFlux(0f);
            if (maxFlux > 0) {
                float fluxRatio = currentFlux / maxFlux;
                data.extensionTime = fluxRatio * MAX_EXTENSION_TIME;
                data.extensionTimer = 0f;
            }
            
            data.extensionApplied = true;
        }
        
        if(ship.getPhaseCloak().getState() == ShipSystemAPI.SystemState.ACTIVE) {
            float maxHull = ship.getMaxHitpoints();
            float currentHull = ship.getHitpoints();
            
            if (data.lastHull <= 0) {
                data.lastHull = currentHull;
            }
            
            float hullChange = currentHull - data.lastHull;
            
            if (hullChange > 0) {
                float reducedHeal = hullChange * HEAL_REDUCTION;
                ship.setHitpoints(currentHull - reducedHeal);
                currentHull = ship.getHitpoints();
            }
            
            float phaseDamage = maxHull * PHASE_DAMAGE_RATE * Global.getCombatEngine().getElapsedInLastFrame();
            ship.setHitpoints(currentHull - phaseDamage);
            
            if (ship.getHitpoints() < maxHull * MIN_HULL_PERCENT) {
                ship.getPhaseCloak().forceState(ShipSystemAPI.SystemState.OUT, 0f);
            }
            
            data.lastHull = ship.getHitpoints();
        } else {
            data.lastHull = ship.getHitpoints();
        }
        
        updateShipVisuals(ship, state, effectLevel);
        
        applyTimeDilation(stats, id, state, effectLevel, player);
        
        if (!ship.getCustomData().containsKey(RENDER_PLUGIN_KEY)) {
            PhaseCoilBoundsPlugin plugin = new PhaseCoilBoundsPlugin(ship);
            Global.getCombatEngine().addLayeredRenderingPlugin(plugin);
            ship.setCustomData(RENDER_PLUGIN_KEY, plugin);
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        super.unapply(stats, id);
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) {
            return;
        }
        
        ship.setExtraAlphaMult(1f);
        ship.setApplyExtraAlphaToEngines(false);
        ship.setCollisionClass(CollisionClass.SHIP);
        
        Global.getCombatEngine().getTimeMult().unmodify(id);
        stats.getTimeMult().unmodify(id);
        
        ship.getCustomData().remove(DATA_KEY);
        
        if (ship.getCustomData().containsKey(RENDER_PLUGIN_KEY)) {
            Object pluginObj = ship.getCustomData().get(RENDER_PLUGIN_KEY);
            if (pluginObj instanceof PhaseCoilBoundsPlugin) {
                ((PhaseCoilBoundsPlugin) pluginObj).markForRemoval();
            }
            ship.getCustomData().remove(RENDER_PLUGIN_KEY);
        }
    }

    private void applyTimeDilation(MutableShipStatsAPI stats, String id, State state, float effectLevel, boolean player) {
        if (state == State.COOLDOWN || state == State.IDLE) {
            return;
        }
        
        float levelForAlpha = effectLevel;
        if (state == State.IN || state == State.ACTIVE) {
            levelForAlpha = effectLevel;
        } else if (state == State.OUT) {
            levelForAlpha = effectLevel;
        }
        
        float shipTimeMult = 1f + (MAX_TIME_MULT - 1f) * levelForAlpha;
        stats.getTimeMult().modifyMult(id, shipTimeMult);
        
        if (player) {
            Global.getCombatEngine().getTimeMult().modifyMult(id, 1f / shipTimeMult);
        } else {
            Global.getCombatEngine().getTimeMult().unmodify(id);
        }
    }

    private void updateShipVisuals(ShipAPI ship, State state, float effectLevel) {
        float levelForAlpha = effectLevel;
        
        if (state == State.IN || state == State.ACTIVE) {
            ship.setCollisionClass(CollisionClass.NONE);
            levelForAlpha = effectLevel;
        } else if (state == State.OUT) {
            if (effectLevel > 0.5f) {
                ship.setCollisionClass(CollisionClass.NONE);
            } else {
                ship.setCollisionClass(CollisionClass.SHIP);
            }
            levelForAlpha = effectLevel;
        }
        
        ship.setExtraAlphaMult(1f - (1f - SHIP_ALPHA_MULT) * levelForAlpha);
        ship.setApplyExtraAlphaToEngines(true);
    }

    public static class PhaseCoilBoundsPlugin implements com.fs.starfarer.api.combat.CombatLayeredRenderingPlugin {
        private ShipAPI ship;
        private boolean expired = false;
        private float cycleTimer = 0f;
        private float baseLineWidth = 3.0f;
        private float staticGlowWidth = 7.2f;
        private float expandDistance = 18.0f;
        private float expandLineWidthBonus = 4.8f;
        private float fadeOutDuration = 2.0f;
        private float referenceSize = 440.0f;
        
        private float shakeTimer = 0f;
        private float[] shakeOffsets;
        private int shakeSegments = 60;
        
        private float outStateTimer = 0f;
        private float inStateTimer = 0f;
        private static final float FADE_IN_DURATION = 1.0f;
        
        private static final float REGEN_DURATION = 0.8f;
        private static final float HOLD_DURATION = 0.2f;
        private static final float EXPAND_DURATION = 1.0f;
        private static final float SHAKE_AMPLITUDE = 2.0f;
        private static final float SHAKE_SPEED = 8.0f;

        public PhaseCoilBoundsPlugin(ShipAPI ship) {
            this.ship = ship;
            shakeOffsets = new float[shakeSegments];
            for (int i = 0; i < shakeSegments; i++) {
                shakeOffsets[i] = (float) (Math.random() * Math.PI * 2);
            }
        }

        public void markForRemoval() {
            expired = true;
        }

        @Override
        public void init(com.fs.starfarer.api.combat.CombatEntityAPI entity) {
        }

        @Override
        public void cleanup() {
        }

        @Override
        public boolean isExpired() {
            return expired || ship == null || !ship.isAlive();
        }

        @Override
        public void advance(float amount) {
            cycleTimer += amount;
            shakeTimer += amount;
            float totalCycle = REGEN_DURATION + HOLD_DURATION + EXPAND_DURATION;
            if (cycleTimer >= totalCycle) {
                cycleTimer -= totalCycle;
            }

            for (int i = 0; i < shakeSegments; i++) {
                shakeOffsets[i] += amount * SHAKE_SPEED;
            }

            ShipSystemAPI.SystemState currentState = ship.getPhaseCloak().getState();
            if (currentState == ShipSystemAPI.SystemState.OUT) {
                outStateTimer += amount;
                inStateTimer = 0f;
            } else if (currentState == ShipSystemAPI.SystemState.IN) {
                inStateTimer += amount;
                outStateTimer = 0f;
            } else {
                inStateTimer = 0f;
                outStateTimer = 0f;
            }
            
            DataContainer data = (DataContainer) ship.getCustomData().get(DATA_KEY);
            if (data != null) {
                data.extensionTimer += amount;
                
                if (ship.getPhaseCloak().getState() == ShipSystemAPI.SystemState.ACTIVE) {
                    if (data.extensionTimer < data.extensionTime) {
                        ship.getPhaseCloak().forceState(ShipSystemAPI.SystemState.ACTIVE, 0f);
                    }
                }
            }
        }

        @Override
        public EnumSet<CombatEngineLayers> getActiveLayers() {
            return EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_LAYER);
        }

        @Override
        public float getRenderRadius() {
            return 1000000f;
        }

        @Override
        public void render(CombatEngineLayers layer, ViewportAPI viewport) {
            if (layer != CombatEngineLayers.ABOVE_SHIPS_LAYER) {
                return;
            }
            
            if (ship == null || !ship.isAlive()) {
                return;
            }

            ShipSystemAPI.SystemState phaseState = ship.getPhaseCloak().getState();
            
            float globalAlpha = calculateGlobalAlpha(phaseState);
            if (globalAlpha <= 0.01f) {
                return;
            }

            float zoomScale = 1f/viewport.getViewMult();

            renderPhaseCoilSprite(globalAlpha, phaseState);

            BoundsAPI bounds = ship.getExactBounds();
            
            if (bounds == null) {
                renderCollisionCircleWithEffect(globalAlpha, phaseState, zoomScale);
                return;
            }

            bounds.update(ship.getLocation(), ship.getFacing());
            
            List<BoundsAPI.SegmentAPI> segments = bounds.getSegments();
            if (segments == null || segments.isEmpty()) {
                renderCollisionCircleWithEffect(globalAlpha, phaseState, zoomScale);
                return;
            }

            float[] phaseParams = calculatePhaseParams();
            float expandProgress = phaseParams[0];
            float lineAlpha = phaseParams[1] * globalAlpha;
            lineAlpha = Math.min(lineAlpha, 0.8f);

            GL11.glEnable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_TEXTURE_2D);

            int glowLayers = 8;
            for (int i = 0; i < glowLayers; i++) {
                float layerRatio = (float) i / (glowLayers - 1);
                
                float baseOffset = staticGlowWidth * layerRatio;
                float expandOffset = expandDistance * expandProgress * layerRatio;
                float totalOffset = baseOffset + expandOffset;
                
                float alphaMult = 1.0f - layerRatio;
                alphaMult = (float) Math.pow(alphaMult, 1.5f);
                
                float lineWidthMult = (1.0f + expandLineWidthBonus * expandProgress) *zoomScale;
                
                Color layerColor;
                if (i == 0) {
                    layerColor = new Color(180, 155, 255, (int) (255 * lineAlpha));
                } else {
                    int red = (int) (180 + 40 * layerRatio);
                    int green = (int) (155 + 50 * layerRatio);
                    int blue = 255;
                    int alpha = (int) (180 * lineAlpha * alphaMult);
                    layerColor = new Color(red, green, blue, alpha);
                }

                GL11.glPushMatrix();
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
                GL11.glLineWidth((baseLineWidth + totalOffset * 1.2f) * lineWidthMult);

                for (BoundsAPI.SegmentAPI segment : segments) {
                    Vector2f p1 = segment.getP1();
                    Vector2f p2 = segment.getP2();
                    
                    Vector2f normal = calculateOutwardNormal(p1, p2);
                    Vector2f offsetP1 = new Vector2f(p1.x + normal.x * totalOffset, p1.y + normal.y * totalOffset);
                    Vector2f offsetP2 = new Vector2f(p2.x + normal.x * totalOffset, p2.y + normal.y * totalOffset);

                    GL11.glBegin(GL11.GL_LINES);
                    GL11.glColor4ub((byte) layerColor.getRed(), (byte) layerColor.getGreen(), 
                                   (byte) layerColor.getBlue(), (byte) layerColor.getAlpha());
                    GL11.glVertex2f(offsetP1.x, offsetP1.y);
                    GL11.glVertex2f(offsetP2.x, offsetP2.y);
                    GL11.glEnd();
                }

                GL11.glPopMatrix();
            }

            GL11.glLineWidth(1.0f);
        }

        private void renderPhaseCoilSprite(float globalAlpha, ShipSystemAPI.SystemState phaseState) {
            if (ship == null) return;

            SpriteAPI phaseLight = null;
            try {
                phaseLight = Global.getSettings().getSprite("Meng_fire", ship.getHullSpec().getBaseHullId() + "_phaselight");
            } catch (Exception e) {
                return;
            }
            
            if (phaseLight == null) {
                return;
            }

            float effectAlpha = globalAlpha;
            if (phaseState == ShipSystemAPI.SystemState.IN) {
                DataContainer data = (DataContainer) ship.getCustomData().get(DATA_KEY);
                if (data != null && data.extensionTime > 0) {
                    float inProgress = Math.min(1.0f, data.extensionTimer / 2.0f);
                    effectAlpha = inProgress * globalAlpha;
                } else {
                    effectAlpha = globalAlpha;
                }
            }

            Vector2f shipLoc = ship.getLocation();
            if (shipLoc == null) {
                return;
            }
            
            float breathCycle = (float) Math.sin(shakeTimer * 2.0f) * 0.5f + 0.5f;
            
            float purpleR = 0.7f;
            float purpleG = 0.4f;
            float purpleB = 1.0f;
            
            float brightRedR = 1.0f;
            float brightRedG = 0.2f;
            float brightRedB = 0.3f;
            
            float red = purpleR + (brightRedR - purpleR) * breathCycle;
            float green = purpleG + (brightRedG - purpleG) * breathCycle;
            float blue = purpleB + (brightRedB - purpleB) * breathCycle;

            float shakeX = 0f;
            float shakeY = 0f;
            if (phaseState == ShipSystemAPI.SystemState.OUT) {
                shakeX = (float) (Math.sin(shakeTimer * 80.0f) * 0.8f);
                shakeY = (float) (Math.cos(shakeTimer * 65.0f) * 0.8f);
            }

            float width = phaseLight.getWidth();
            float height = phaseLight.getHeight();
            float halfWidth = width * 0.5f;
            float halfHeight = height * 0.5f;

            GL11.glPushMatrix();
            GL11.glTranslatef(shipLoc.x + shakeX, shipLoc.y + shakeY, 0.0f);
            GL11.glRotatef(ship.getFacing() - 90f, 0f, 0f, 1f);
            
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            phaseLight.bindTexture();
            
            GL11.glColor4f(red, green, blue, effectAlpha);

            GL11.glBegin(GL11.GL_QUADS);
            
            GL11.glTexCoord2f(0.0f, 0.0f);
            GL11.glVertex2f(-halfWidth, -halfHeight);
            
            GL11.glTexCoord2f(1.0f, 0.0f);
            GL11.glVertex2f(halfWidth, -halfHeight);
            
            GL11.glTexCoord2f(1.0f, 1.0f);
            GL11.glVertex2f(halfWidth, halfHeight);
            
            GL11.glTexCoord2f(0.0f, 1.0f);
            GL11.glVertex2f(-halfWidth, halfHeight);
            
            GL11.glEnd();
            
            GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

            GL11.glPopMatrix();
        }
        private void renderCollisionCircleWithEffect(float globalAlpha, ShipSystemAPI.SystemState phaseState, float zoomScale) {
            if (ship == null) return;

            float radius = ship.getCollisionRadius();
            Vector2f center = ship.getLocation();
            
            float[] phaseParams = calculatePhaseParams();
            float expandProgress = phaseParams[0];
            float lineAlpha = phaseParams[1] * globalAlpha;

            GL11.glEnable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_TEXTURE_2D);

            int glowLayers = 8;
            int circleSegments = 72;
            
            for (int i = 0; i < glowLayers; i++) {
                float layerRatio = (float) i / (glowLayers - 1);
                
                float baseOffset = staticGlowWidth * layerRatio;
                float expandOffset = expandDistance * expandProgress * layerRatio;
                float totalOffset = baseOffset + expandOffset;
                
                float alphaMult = 1.0f - layerRatio;
                alphaMult = (float) Math.pow(alphaMult, 1.5f);
                
                float lineWidthMult = (1.0f + expandLineWidthBonus * expandProgress) * zoomScale;
                
                Color layerColor;
                if (i == 0) {
                    layerColor = new Color(180, 155, 255, (int) (255 * lineAlpha));
                } else {
                    int red = (int) (180 + 40 * layerRatio);
                    int green = (int) (155 + 50 * layerRatio);
                    int blue = 255;
                    int alpha = (int) (180 * lineAlpha * alphaMult);
                    layerColor = new Color(red, green, blue, alpha);
                }

                GL11.glPushMatrix();
                GL11.glTranslatef(center.x, center.y, 0.0f);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
                GL11.glLineWidth((baseLineWidth + totalOffset * 1.2f) * lineWidthMult);
                
                GL11.glBegin(GL11.GL_LINE_LOOP);
                
                for (int j = 0; j <= circleSegments; j++) {
                    float angle = (float) (j * 2 * Math.PI / circleSegments);
                    float expandedRadius = radius + totalOffset;
                    float x = expandedRadius * (float) Math.cos(angle);
                    float y = expandedRadius * (float) Math.sin(angle);
                    
                    GL11.glColor4ub((byte) layerColor.getRed(), (byte) layerColor.getGreen(), 
                                   (byte) layerColor.getBlue(), (byte) layerColor.getAlpha());
                    GL11.glVertex2f(x, y);
                }
                
                GL11.glEnd();
                GL11.glPopMatrix();
            }

            GL11.glLineWidth(1.0f);
        }

        private float[] calculatePhaseParams() {
            float expandProgress;
            float lineAlpha;

            if (cycleTimer < REGEN_DURATION) {
                float regenProgress = cycleTimer / REGEN_DURATION;
                expandProgress = 0f;
                lineAlpha = regenProgress;
            } else if (cycleTimer < REGEN_DURATION + HOLD_DURATION) {
                expandProgress = 0f;
                lineAlpha = 1.0f;
            } else {
                float expandPhase = (cycleTimer - REGEN_DURATION - HOLD_DURATION) / EXPAND_DURATION;
                expandProgress = expandPhase;
                lineAlpha = 1.0f - expandPhase;
            }

            return new float[]{expandProgress, lineAlpha};
        }

        private float calculateGlobalAlpha(ShipSystemAPI.SystemState phaseState) {
            if (phaseState == ShipSystemAPI.SystemState.IN) {
                float fadeInAlpha = inStateTimer / (FADE_IN_DURATION - 0.25f);
                return Math.max(0f, Math.min(1f, fadeInAlpha));
            } else if (phaseState == ShipSystemAPI.SystemState.OUT) {
                float fadeAlpha = 1.0f - (outStateTimer / (fadeOutDuration - 0.5f));
                return Math.max(0f, Math.min(1f, fadeAlpha));
            } else {
                return 1.0f;
            }
        }

        private Vector2f calculateOutwardNormal(Vector2f p1, Vector2f p2) {
            float dx = p2.x - p1.x;
            float dy = p2.y - p1.y;
            
            float length = (float) Math.sqrt(dx * dx + dy * dy);
            if (length < 0.0001f) {
                return new Vector2f(0f, 1f);
            }
            
            float nx = -dy / length;
            float ny = dx / length;
            
            Vector2f center = ship.getLocation();
            float midX = (p1.x + p2.x) * 0.5f;
            float midY = (p1.y + p2.y) * 0.5f;
            
            float toCenterX = center.x - midX;
            float toCenterY = center.y - midY;
            
            float dotProduct = nx * toCenterX + ny * toCenterY;
            
            if (dotProduct > 0) {
                nx = -nx;
                ny = -ny;
            }
            
            return new Vector2f(nx, ny);
        }
    }
    
    public static class DataContainer {
        boolean extensionApplied = false;
        float extensionTime = 0f;
        float extensionTimer = 0f;
        float lastHull = 0f;
    }
}
