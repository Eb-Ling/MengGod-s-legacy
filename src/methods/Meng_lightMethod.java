package data.methods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.EnumSet;

public class Meng_lightMethod implements CombatLayeredRenderingPlugin {
    private ShipAPI ship;
    private float times;
    private float timer = 0f;
    private float length;
    private Color color;

    public Meng_lightMethod(ShipAPI s, float l, float time,Color c) {
        ship = s;
        times = time;
        length = l;
        color = c;
    }

    public void init(CombatEntityAPI entity) {

    }

    @Override
    public void cleanup() {
    }

    @Override
    public boolean isExpired() {
        return timer >= times || ship == null || !ship.isAlive();
    }

    @Override
    public void advance(float amount) {
        timer += amount;
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
        if (layer != CombatEngineLayers.UNDER_SHIPS_LAYER) {
            return;
        }
        
        if (ship == null || !ship.isAlive()) {
            return;
        }

        float level = Math.min(1.0f, timer / times);
        
        SpriteAPI light = null;
        try {
            light = Global.getSettings().getSprite("Meng_fire", ship.getHullSpec().getBaseHullId() + "_light");
        } catch (Exception e) {
            return;
        }
        
        if (light == null) {
            return;
        }

        Vector2f shiploc = ship.getLocation();
        if (shiploc == null) {
            return;
        }

        float h = light.getHeight() * 0.5f;
        
        for (int j = 0; j < length; ++j) {
            float alpha;
            if (j < length / 2f) {
                alpha = (j + 1) / 10f;
            } else {
                alpha = (length - j) / 10f;
            }
            alpha = Math.max(0.0f, Math.min(1.0f, alpha));
            
            GL11.glPushMatrix();
            GL11.glTranslatef(shiploc.x, shiploc.y, 0.0f);
            GL11.glRotatef(ship.getFacing() - 90f, 0f, 0f, 1f);
            
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            light.bindTexture();
            GL11.glEnable(GL11.GL_BLEND);
            
            int alphaInt = Math.max(0, Math.min(255, Math.round(alpha * 255)));
            Color colors = new Color(color.getRed(), color.getGreen(), color.getBlue(), alphaInt);

            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4ub((byte) colors.getRed(), (byte) colors.getGreen(), 
                           (byte) colors.getBlue(), (byte) colors.getAlpha());

            GL11.glBegin(GL11.GL_QUAD_STRIP);

            for (int i = 0; i <= 360; i++) {
                float angle = (float) Math.toRadians(i % 360);
                float cos = (float) Math.cos(angle);
                float sin = (float) Math.sin(angle);

                float scaleInner = level + j * 0.01f;
                float scaleOuter = level + (j + 1) * 0.01f;
                
                float texScaleInner = Math.min(1.0f, scaleInner);
                float texScaleOuter = Math.min(1.0f, scaleOuter);

                float InnertexU = 0.5f + 0.5f * texScaleInner * cos;
                float InnertexV = 0.5f + 0.5f * texScaleInner * sin;

                float texU = 0.5f + 0.5f * texScaleOuter * cos;
                float texV = 0.5f + 0.5f * texScaleOuter * sin;
                
                float InnervertexX = h * scaleInner * cos;
                float InnervertexY = h * scaleInner * sin;

                float vertexX = h * scaleOuter * cos;
                float vertexY = h * scaleOuter * sin;

                GL11.glTexCoord2f(InnertexU, InnertexV);
                GL11.glVertex2f(InnervertexX, InnervertexY);
                GL11.glTexCoord2f(texU, texV);
                GL11.glVertex2f(vertexX, vertexY);
            }

            GL11.glEnd();
            GL11.glPopMatrix();
        }
    }

}