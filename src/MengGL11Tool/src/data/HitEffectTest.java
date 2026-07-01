package data;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class HitEffectTest implements MyRenderTool.MyRender, MyRenderTool.InitializableEffect {
    private Vector2f location;
    private float timer;
    private float maxTime;
    
    public HitEffectTest() {
        this.location = new Vector2f(0f, 0f);
        this.timer = 0f;
        this.maxTime = 1.05f;
    }
    
    @Override
    public void initialize(float x, float y) {
        location.x = x;
        location.y = y;
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
        if (timer >= 1f) return;
        
        drawFlashRing(location, timer);
    }
    
    private void drawFlashRing(Vector2f loc, float t) {
        float sizes = 30f * t;
        float width = 4f;

        for (int i = 0; i < 360; i++) {
            Vector2f locs1 = new Vector2f(
                loc.x + sizes * (float) Math.cos(Math.toRadians(i)),
                loc.y + sizes * (float) Math.sin(Math.toRadians(i))
            );
            Vector2f locs2 = new Vector2f(
                loc.x + (width + sizes) * (float) Math.cos(Math.toRadians(i)),
                loc.y + sizes * (float) Math.sin(Math.toRadians(i))
            );
            Vector2f locs3 = new Vector2f(
                loc.x + sizes * (float) Math.cos(Math.toRadians(i + 1)),
                loc.y + sizes * (float) Math.sin(Math.toRadians(i + 1))
            );
            Vector2f locs4 = new Vector2f(
                loc.x + (width + sizes) * (float) Math.cos(Math.toRadians(i + 1)),
                loc.y + sizes * (float) Math.sin(Math.toRadians(i + 1))
            );
            
            GL11.glPushMatrix();
            GL11.glTranslatef(0.0f, 0.0f, 0.0f);
            GL11.glRotatef(0f, 0f, 0f, 0.1f);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            
            float alpha = 1f - t;
            Color c = new Color(255, 50, 50, (int)(255 * alpha));
            GL11.glColor4ub((byte) c.getRed(), (byte) c.getGreen(), 
                           (byte) c.getBlue(), (byte) c.getAlpha());

            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex2f(locs1.x, locs1.y);
            GL11.glVertex2f(locs2.x, locs2.y);
            GL11.glVertex2f(locs4.x, locs4.y);
            GL11.glVertex2f(locs3.x, locs3.y);
            GL11.glEnd();
            
            GL11.glPopMatrix();
        }
    }
}
