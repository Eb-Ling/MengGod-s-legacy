package data;

import org.lwjgl.opengl.GL11;

public class FluxBarRender implements MyRenderTool.MyRender, MyRenderTool.InitializableEffect {
    private float barX;
    private float barY;
    private float barWidth;
    private float barHeight;
    private float timer;
    private float currentFlux;
    private float maxFlux;
    
    public FluxBarRender() {
        this.barWidth = 240f;
        this.barHeight = 24f;
        this.timer = 0f;
        this.currentFlux = 0f;
        this.maxFlux = 500f;
    }
    
    @Override
    public void initialize(float x, float y) {
        barX = x - barWidth / 2f;
        barY = y - barHeight / 2f;
        timer = 0f;
    }
    
    @Override
    public void advance(float amount) {
        timer += amount;
        
        if (currentFlux < maxFlux) {
            currentFlux = Math.min(maxFlux, currentFlux + amount * 50f);
        } else {
            currentFlux = 0f;
        }
    }
    
    @Override
    public boolean isExpired() {
        return false;
    }
    
    @Override
    public void render() {
        float progress = currentFlux / maxFlux;
        drawEmberForgeFluxBar(progress, (int)currentFlux, (int)maxFlux);
    }
    
    private void drawEmberForgeFluxBar(float progress, int currentFlux, int maxFlux) {
        float filledWidth = barWidth * progress;
        
        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        
        float backgroundAlpha = 0.65f;
        GL11.glColor4f(0.12f, 0.12f, 0.12f, backgroundAlpha);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(barX, barY);
        GL11.glVertex2f(barX + barWidth, barY);
        GL11.glVertex2f(barX + barWidth, barY + barHeight);
        GL11.glVertex2f(barX, barY + barHeight);
        GL11.glEnd();
        
        GL11.glLineWidth(1.5f);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.9f);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(barX, barY);
        GL11.glVertex2f(barX + barWidth, barY);
        GL11.glVertex2f(barX + barWidth, barY + barHeight);
        GL11.glVertex2f(barX, barY + barHeight);
        GL11.glEnd();
        
        if (filledWidth > 0) {
            float r, g, b;
            if (progress <= 0.5f) {
                float t = progress / 0.5f;
                r = 0.2f + t * (0.8f - 0.2f);
                g = 1.0f - t * (1.0f - 0.6f);
                b = 0.3f - t * (0.3f - 0.2f);
            } else if (progress <= 0.75f) {
                float t = (progress - 0.5f) / 0.25f;
                r = 0.8f + t * (1.0f - 0.8f);
                g = 0.6f - t * (0.6f - 0.4f);
                b = 0.2f + t * (0.278f - 0.2f);
            } else {
                float t = (progress - 0.75f) / 0.25f;
                r = 1.0f;
                g = 0.4f - t * 0.25f;
                b = 0.278f - t * 0.1f;
            }
            
            float alpha = 0.9f;
            GL11.glColor4f(r, g, b, alpha);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex2f(barX, barY);
            GL11.glVertex2f(barX + filledWidth, barY);
            GL11.glVertex2f(barX + filledWidth, barY + barHeight);
            GL11.glVertex2f(barX, barY + barHeight);
            GL11.glEnd();
            
            if (progress >= 0.9f) {
                float pulseAlpha = (float)(0.4f + 0.3f * Math.sin(timer * 8f));
                GL11.glColor4f(1.0f, 0.15f, 0.1f, pulseAlpha);
                GL11.glBegin(GL11.GL_QUADS);
                GL11.glVertex2f(barX + filledWidth - 2, barY);
                GL11.glVertex2f(barX + filledWidth, barY);
                GL11.glVertex2f(barX + filledWidth, barY + barHeight);
                GL11.glVertex2f(barX + filledWidth - 2, barY + barHeight);
                GL11.glEnd();
            }
        }
        
        GL11.glPopMatrix();
    }
}
