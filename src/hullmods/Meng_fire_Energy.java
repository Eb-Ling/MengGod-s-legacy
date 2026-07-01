package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.ui.*;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.EnumSet;

public class Meng_fire_Energy extends BaseHullMod {
    private final String ID = "Meng_Fire_EntropyInc_Id";
    private final float FluxMult = 0.5f;
    private final float FluxSold = 0.75f;
    private final float TimeMult = 0.2f;
    private static final String KEY = "Meng_fire_EntropyInc_Key";
    private static final String PLUGIN_ADDED_KEY = "Meng_fire_Energy_Plugin_Added";
    
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float opad = 12.0F;
        Color highlight = new Color(255, 55, 40, 255);
        LabelAPI label1 = tooltip.addPara(
                "这是一种能够将辐能封存于亚空间的奇异状态。对辐能熔炉系统进行最强大的改造，能够将过剩的辐能导入这个额外的维度容器中。",
                opad, highlight);
        label1.setHighlight();
        label1.setHighlightColors(highlight, highlight, highlight, highlight, highlight, highlight, highlight);

        tooltip.addSectionHeading("数据分析", Alignment.MID, opad);

        LabelAPI label = tooltip.addPara(
                "#舰船获得一个大小为 %s%% 辐能容量的额外辐能条作为空展熔炉。\n#辐能超过容量最大值的 %s%% 时，舰船辐能以一半的耗散速度转入空展熔炉。\n#而当辐能不高于这个值时，舰船辐能以一半的耗散速度从空展熔炉中转入容量。\n#当空展熔炉容量饱和时，舰船时流增加 %s%% 。",
                opad, highlight, String.valueOf(Math.round(100f *FluxMult)), String.valueOf(Math.round(FluxSold *100f)), String.valueOf(Math.round(TimeMult *100f))
        );

        label.setHighlight(Math.round(100f * FluxMult) + "%", Math.round(FluxSold * 100f) + "%", Math.round(TimeMult * 100f) + "%");
        label.setHighlightColors(highlight, highlight, highlight, highlight);

    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        super.advanceInCombat(ship, amount);
        if(!ship.isAlive()) return;
        
        if (!ship.getCustomData().containsKey(KEY)) {
            ship.setCustomData(KEY, new DataContainer());
        }
        DataContainer data = (DataContainer) ship.getCustomData().get(KEY);
        
        if (!ship.getCustomData().containsKey(PLUGIN_ADDED_KEY)) {
            EmberForgeFluxBarPlugin plugin = new EmberForgeFluxBarPlugin(ship);
            Global.getCombatEngine().addLayeredRenderingPlugin(plugin);
            ship.getCustomData().put(PLUGIN_ADDED_KEY, true);
        }
        
        float maxflux = ship.getMaxFlux()*0.5f;

        MutableShipStatsAPI stats = ship.getMutableStats();
        float decamount = stats.getFluxDissipation().getModifiedValue()*amount*0.5f;
        float currentHardFlux = ship.getFluxTracker().getHardFlux();
        float currentTotalFlux = ship.getFluxTracker().getCurrFlux();
        boolean isHardFlux = (currentHardFlux >= currentTotalFlux);
        
        if(ship.getFluxLevel()>=FluxSold){
            if(data.totalflux <= maxflux) {
                float actualTransfer = Math.min(decamount, maxflux - data.totalflux);
                float softFlux = currentTotalFlux - currentHardFlux;

                if(!isHardFlux) {
                    float drainFromSoft = Math.min(actualTransfer, softFlux);
                    actualTransfer -= drainFromSoft;
                    data.totalflux = Math.min(maxflux, data.totalflux + drainFromSoft);
                    ship.getFluxTracker().setCurrFlux(ship.getCurrFlux() - drainFromSoft);
                }
                if(actualTransfer > 0){
                    data.hardFluxMarker = Math.min(maxflux, data.hardFluxMarker + actualTransfer);
                    data.totalflux = Math.min(maxflux, data.totalflux + actualTransfer);
                    ship.getFluxTracker().setHardFlux(ship.getFluxTracker().getHardFlux() - actualTransfer);
                    ship.getFluxTracker().setCurrFlux(ship.getCurrFlux() - actualTransfer);
                }
            }
            if(data.totalflux == maxflux){
                stats.getTimeMult().modifyPercent(ID,TimeMult*100f);
            }
            else {
                stats.getTimeMult().unmodifyPercent(ID);
            }
        }
        else {
            if(data.totalflux >= 0f) {
                float actualTransfer = Math.min(decamount, data.totalflux);
                
                float softFlux = data.totalflux - data.hardFluxMarker;
                
                if(softFlux > 0) {
                    float drainFromSoft = Math.min(actualTransfer, softFlux);
                    data.totalflux -= drainFromSoft;
                    actualTransfer -= drainFromSoft;
                    ship.getFluxTracker().setCurrFlux(ship.getCurrFlux() + drainFromSoft);
                }
                
                if(actualTransfer > 0 && data.hardFluxMarker > 0) {
                    float drainFromHard = Math.min(actualTransfer, data.hardFluxMarker);
                    data.hardFluxMarker -= drainFromHard;
                    data.totalflux -= drainFromHard;
                    ship.getFluxTracker().setHardFlux(ship.getFluxTracker().getHardFlux() + actualTransfer);
                    ship.getFluxTracker().setCurrFlux(ship.getCurrFlux() + actualTransfer);
                }
                

            }
        }
    }
    
    public static class DataContainer {
        public float totalflux=0f;
        public float hardFluxMarker=0f;
    }
    
    public static class EmberForgeFluxBarPlugin implements com.fs.starfarer.api.combat.CombatLayeredRenderingPlugin {
        private final ShipAPI ship;
        private float timer = 0f;
        private static float BAR_WIDTH = 8f;
        private static float BAR_HEIGHT = 100f;
        private static float START_TICK_WIDTH = 12f;
        private static float START_TICK_HEIGHT = 2f;
        private static float END_TICK_WIDTH = 10f;
        private static float END_TICK_HEIGHT = 2f;
        private static float VERTICAL_OFFSET = 20f;
        private static final float BASE_DISTANCE_MULT = 1.1f;
        private static final float MIN_DISTANCE_MULT = 0.4f;
        private static final float ZOOM_THRESHOLD = 1.2f;
        
        public EmberForgeFluxBarPlugin(ShipAPI ship) {
            this.ship = ship;
        }
        
        @Override
        public void init(com.fs.starfarer.api.combat.CombatEntityAPI entity) {
        }
        
        @Override
        public void cleanup() {
        }
        
        @Override
        public boolean isExpired() {
            return ship == null || !ship.isAlive();
        }
        
        @Override
        public void advance(float amount) {
            timer += amount;
        }
        
        @Override
        public EnumSet<CombatEngineLayers> getActiveLayers() {
            return EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_LAYER);
        }
        
        @Override
        public float getRenderRadius() {
            return 10000f;
        }
        
        @Override
        public void render(CombatEngineLayers layer, ViewportAPI viewport) {
            if (ship == null || !ship.isAlive()) return;
            
            DataContainer data = (DataContainer) ship.getCustomData().get(KEY);
            if (data == null) return;
            
            float maxFlux = ship.getMaxFlux() * 0.5f;
            if (maxFlux <= 0) return;
            
            float progress = Math.min(1.0f, data.totalflux / maxFlux);
            
            float shieldRadius = ship.getShieldRadiusEvenIfNoShield();
            
            float zoom = viewport.getViewMult();

            float distanceMult = BASE_DISTANCE_MULT;
            
            if (zoom < ZOOM_THRESHOLD) {
                float t = (ZOOM_THRESHOLD - zoom) / ZOOM_THRESHOLD;
                t = Math.min(1.0f, t);
                distanceMult = BASE_DISTANCE_MULT - t * (BASE_DISTANCE_MULT - MIN_DISTANCE_MULT);
            }
            else {
                float dis = zoom - 0.2f;
                zoom = 0.2f + (float) Math.sqrt(dis);
            }
            BAR_WIDTH = 10f*zoom;
            BAR_HEIGHT = 100f*zoom;
            START_TICK_WIDTH = 14f*zoom;
            START_TICK_HEIGHT = 2f*zoom;
            END_TICK_WIDTH = 14f*zoom;
            END_TICK_HEIGHT = 2f*zoom;
            VERTICAL_OFFSET = 20f*zoom;
            float distanceFromCenter = shieldRadius * distanceMult;
            
            Vector2f shipLoc = ship.getLocation();
            float facing = ship.getFacing();
            
            float barCenterX = shipLoc.x + (float)Math.cos(Math.toRadians(facing + 90f)) * distanceFromCenter;
            float barCenterY = shipLoc.y + (float)Math.sin(Math.toRadians(facing + 90f)) * distanceFromCenter + VERTICAL_OFFSET;
            
            float barLeft = barCenterX - BAR_WIDTH / 2f;
            float barBottom = barCenterY - BAR_HEIGHT / 2f;
            
            drawVerticalFluxBar(barLeft, barBottom, BAR_WIDTH, BAR_HEIGHT, progress, 
                              START_TICK_WIDTH, START_TICK_HEIGHT, END_TICK_WIDTH, END_TICK_HEIGHT
                              );
        }
        
        private void drawVerticalFluxBar(float x, float y, float width, float height, float progress,
                                        float startTickWidth, float startTickHeight, 
                                        float endTickWidth, float endTickHeight
                                        ) {
            float filledHeight = height * progress;
            
            GL11.glPushMatrix();
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            
            float backgroundAlpha = 0.65f;
            GL11.glColor4f(0.12f, 0.12f, 0.12f, backgroundAlpha);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex2f(x, y);
            GL11.glVertex2f(x + width, y);
            GL11.glVertex2f(x + width, y + height);
            GL11.glVertex2f(x, y + height);
            GL11.glEnd();
            
            float tickOffsetX = (startTickWidth - width) / 2f;
            GL11.glColor4f(0.71f, 0.61f, 1f, 0.9f);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex2f(x - tickOffsetX, y - startTickHeight);
            GL11.glVertex2f(x + width + tickOffsetX, y - startTickHeight);
            GL11.glVertex2f(x + width + tickOffsetX, y);
            GL11.glVertex2f(x - tickOffsetX, y);
            GL11.glEnd();
            
            float endTickOffsetX = (endTickWidth - width) / 2f;
            GL11.glColor4f(0.67f, 0.56f, 1f, 0.9f);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex2f(x - endTickOffsetX, y + height);
            GL11.glVertex2f(x + width + endTickOffsetX, y + height);
            GL11.glVertex2f(x + width + endTickOffsetX, y + height + endTickHeight);
            GL11.glVertex2f(x - endTickOffsetX, y + height + endTickHeight);
            GL11.glEnd();
            
            if (filledHeight > 0) {
                float r, g, b;
                if (progress <= 0.5f) {
                    float t = progress / 0.5f;
                    r = 0.55f + t * (0.71f - 0.55f);
                    g = 0.45f + t * (0.61f - 0.45f);
                    b = 1.0f - t * (1.0f - 0.9f);
                } else if (progress <= 0.75f) {
                    float t = (progress - 0.5f) / 0.25f;
                    r = 0.71f + t * (0.78f - 0.71f);
                    g = 0.61f - t * (0.61f - 0.5f);
                    b = 0.9f - t * (0.9f - 0.85f);
                } else {
                    float t = (progress - 0.75f) / 0.25f;
                    r = 0.78f + t * (0.85f - 0.78f);
                    g = 0.5f - t * 0.15f;
                    b = 0.85f - t * 0.1f;
                }
                
                float alpha = 0.9f;
                GL11.glColor4f(r, g, b, alpha);
                GL11.glBegin(GL11.GL_QUADS);
                GL11.glVertex2f(x, y);
                GL11.glVertex2f(x + width, y);
                GL11.glVertex2f(x + width, y + filledHeight);
                GL11.glVertex2f(x, y + filledHeight);
                GL11.glEnd();
                
                if (progress >= 0.9f) {
                    float pulseAlpha = (float)(0.4f + 0.3f * Math.sin(timer * 8f));
                    GL11.glColor4f(0.85f, 0.5f, 1f, pulseAlpha);
                    GL11.glBegin(GL11.GL_QUADS);
                    GL11.glVertex2f(x, y + filledHeight - 2);
                    GL11.glVertex2f(x + width, y + filledHeight - 2);
                    GL11.glVertex2f(x + width, y + filledHeight);
                    GL11.glVertex2f(x, y + filledHeight);
                    GL11.glEnd();
                }
            }
            
            DataContainer data = (DataContainer) ship.getCustomData().get(KEY);
            if (data != null && data.hardFluxMarker > 0) {
                float maxFlux = ship.getMaxFlux() * 0.5f;
                if (maxFlux > 0) {
                    float hardFluxProgress = Math.min(1.0f, data.hardFluxMarker / maxFlux);
                    float hardFluxY = y + height * hardFluxProgress;
                    
                    float markerWidth = width * 1.3f;
                    float markerHeight = 2f;
                    float markerOffsetX = (markerWidth - width) / 2f;
                    
                    GL11.glColor4f(0.9f, 0.8f, 1f, 0.95f);
                    GL11.glBegin(GL11.GL_QUADS);
                    GL11.glVertex2f(x - markerOffsetX, hardFluxY - markerHeight / 2f);
                    GL11.glVertex2f(x + width + markerOffsetX, hardFluxY - markerHeight / 2f);
                    GL11.glVertex2f(x + width + markerOffsetX, hardFluxY + markerHeight / 2f);
                    GL11.glVertex2f(x - markerOffsetX, hardFluxY + markerHeight / 2f);
                    GL11.glEnd();
                }
            }
            
            GL11.glPopMatrix();
        }
    }
    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return ship.getVariant().getHullMods().contains("Meng_fire_core")||ship.getVariant().getHullMods().contains("Meng_fire_core_li");
    }
    public String getUnapplicableReason(ShipAPI ship) {
        if (!ship.getVariant().getHullMods().contains("Meng_fire_core")&&!ship.getVariant().getHullMods().contains("Meng_fire_core_li")) {
            return "只能用于火羽计划舰船";
        }
        return null;
    }
}
