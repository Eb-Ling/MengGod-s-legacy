package data;

import org.lwjgl.opengl.GL11;

public class ProgressBarRender implements MyRenderTool.MyRender, MyRenderTool.InitializableEffect {
    private float barX;
    private float barY;
    private float barWidth;
    private float barHeight;
    private float timer;
    private float maxTime;
    
    public ProgressBarRender() {
        this.barX = 0f;
        this.barY = 0f;
        this.barWidth = 200f;
        this.barHeight = 20f;
        this.timer = 0f;
        this.maxTime = 5.0f;
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
    }
    
    @Override
    public boolean isExpired() {
        return timer >= maxTime;
    }
    
    @Override
    public void render() {
        float progress = Math.min(1.0f, timer / maxTime);
        drawFluxBar(progress, (int)(progress * 500), 500);
    }
    
    private void drawFluxBar(float progress, int currentFlux, int maxFlux) {
        float filledWidth = barWidth * progress;
        
        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        
        float backgroundAlpha = 0.6f;
        GL11.glColor4f(0.15f, 0.15f, 0.15f, backgroundAlpha);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(barX, barY);
        GL11.glVertex2f(barX + barWidth, barY);
        GL11.glVertex2f(barX + barWidth, barY + barHeight);
        GL11.glVertex2f(barX, barY + barHeight);
        GL11.glEnd();
        
        GL11.glLineWidth(2f);
        GL11.glColor4f(0.5f, 0.5f, 0.5f, 0.9f);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(barX, barY);
        GL11.glVertex2f(barX + barWidth, barY);
        GL11.glVertex2f(barX + barWidth, barY + barHeight);
        GL11.glVertex2f(barX, barY + barHeight);
        GL11.glEnd();
        
        if (filledWidth > 2) {
            float r, g, b;
            if (progress <= 0.75f) {
                float t = progress / 0.75f;
                r = 1.0f;
                g = 0.196f + t * (0.6f - 0.196f);
                b = 0.196f + t * (0.2f - 0.196f);
            } else {
                float t = (progress - 0.75f) / 0.25f;
                r = 1.0f;
                g = 0.6f - t * 0.3f;
                b = 0.2f - t * 0.1f;
            }
            
            float alpha = 0.85f;
            GL11.glColor4f(r, g, b, alpha);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex2f(barX + 2, barY + 2);
            GL11.glVertex2f(barX + filledWidth - 2, barY + 2);
            GL11.glVertex2f(barX + filledWidth - 2, barY + barHeight - 2);
            GL11.glVertex2f(barX + 2, barY + barHeight - 2);
            GL11.glEnd();
            
            float glowWidth = Math.min(10f, filledWidth * 0.1f);
            if (glowWidth > 0) {
                GL11.glColor4f(r, g, b, alpha * 0.3f);
                GL11.glBegin(GL11.GL_QUADS);
                GL11.glVertex2f(barX + filledWidth - glowWidth, barY + 2);
                GL11.glVertex2f(barX + filledWidth - 2, barY + 2);
                GL11.glVertex2f(barX + filledWidth - 2, barY + barHeight - 2);
                GL11.glVertex2f(barX + filledWidth - glowWidth, barY + barHeight - 2);
                GL11.glEnd();
            }
        }
        
        GL11.glPopMatrix();
    }
}
