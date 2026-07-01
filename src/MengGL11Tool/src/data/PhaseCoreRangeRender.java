package data;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class PhaseCoreRangeRender implements MyRenderTool.MyRender, MyRenderTool.InitializableEffect {
    private Vector2f center;
    private float radius;
    private float timer;
    private float rotationAngle;
    private float[] noiseOffsets;
    private float[] noiseSpeeds;
    private int numSegments;
    private boolean expired;

    public PhaseCoreRangeRender() {
        this.center = new Vector2f(0f, 0f);
        this.radius = 200f;
        this.timer = 0f;
        this.rotationAngle = 0f;
        this.numSegments = 90;
        this.noiseOffsets = new float[numSegments];
        this.noiseSpeeds = new float[numSegments];
        this.expired = false;
        
        for (int i = 0; i < numSegments; i++) {
            noiseOffsets[i] = (float) Math.random();
            noiseSpeeds[i] = (float) (Math.random() * 0.3f + 0.2f);
        }
    }

    @Override
    public void initialize(float x, float y) {
        center.x = x;
        center.y = y;
        timer = 0f;
        rotationAngle = 0f;
        expired = false;
        
        for (int i = 0; i < numSegments; i++) {
            noiseOffsets[i] = (float) Math.random();
            noiseSpeeds[i] = (float) (Math.random() * 0.3f + 0.2f);
        }
    }

    @Override
    public void advance(float amount) {
        timer += amount;
        rotationAngle += amount * 15f;
        if (rotationAngle >= 360f) {
            rotationAngle -= 360f;
        }
        
        for (int i = 0; i < numSegments; i++) {
            noiseOffsets[i] += amount * noiseSpeeds[i];
            if (noiseOffsets[i] > 1f) {
                noiseOffsets[i] -= 1f;
            }
        }
    }

    @Override
    public boolean isExpired() {
        return expired;
    }

    public void setExpired(boolean expired) {
        this.expired = expired;
    }

    @Override
    public void render() {
        GL11.glPushMatrix();
        GL11.glTranslatef(center.x, center.y, 0.0f);
        
        float baseAlpha = 0.5f;
        
        float innerRadius = radius - 18f;
        float maxOuterFluctuation = 15f;
        
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        
        float[] outerRadii = new float[numSegments];
        float[] outerNoiseValues = new float[numSegments];
        
        for (int i = 0; i < numSegments; i++) {
            float t = noiseOffsets[i];
            
            float noise = (float) (Math.sin(t * Math.PI * 2f) * 0.5f + 
                                  Math.sin(t * Math.PI * 4f) * 0.3f +
                                  Math.sin(t * Math.PI * 1.5f) * 0.2f);
            noise = noise * 0.5f + 0.5f;
            
            outerNoiseValues[i] = noise;
            outerRadii[i] = radius + noise * maxOuterFluctuation;
        }
        
        int gradientSteps = 60;
        for (int step = 0; step < gradientSteps; step++) {
            float t1 = (float) step / gradientSteps;
            float t2 = (float) (step + 1) / gradientSteps;
            
            GL11.glBegin(GL11.GL_QUADS);
            
            for (int i = 0; i < numSegments; i++) {
                int next = (i + 1) % numSegments;
                
                float angle1 = rotationAngle + i * 360f / numSegments;
                float angle2 = rotationAngle + next * 360f / numSegments;
                
                float whiteYellowBoundaryRadius = innerRadius + (radius - innerRadius) * 0.25f;
                
                float r1_inner;
                float r1_outer;
                float r2_inner;
                float r2_outer;
                
                if (t1 < 0.25f && t2 < 0.25f) {
                    r1_inner = innerRadius + (whiteYellowBoundaryRadius - innerRadius) * (t1 / 0.25f);
                    r1_outer = innerRadius + (whiteYellowBoundaryRadius - innerRadius) * (t2 / 0.25f);
                    r2_inner = innerRadius + (whiteYellowBoundaryRadius - innerRadius) * (t1 / 0.25f);
                    r2_outer = innerRadius + (whiteYellowBoundaryRadius - innerRadius) * (t2 / 0.25f);
                } else if (t1 < 0.25f) {
                    r1_inner = innerRadius + (whiteYellowBoundaryRadius - innerRadius) * (t1 / 0.25f);
                    r1_outer = whiteYellowBoundaryRadius + (outerRadii[i] - whiteYellowBoundaryRadius) * ((t2 - 0.25f) / 0.75f);
                    r2_inner = innerRadius + (whiteYellowBoundaryRadius - innerRadius) * (t1 / 0.25f);
                    r2_outer = whiteYellowBoundaryRadius + (outerRadii[next] - whiteYellowBoundaryRadius) * ((t2 - 0.25f) / 0.75f);
                } else {
                    r1_inner = whiteYellowBoundaryRadius + (outerRadii[i] - whiteYellowBoundaryRadius) * ((t1 - 0.25f) / 0.75f);
                    r1_outer = whiteYellowBoundaryRadius + (outerRadii[i] - whiteYellowBoundaryRadius) * ((t2 - 0.25f) / 0.75f);
                    r2_inner = whiteYellowBoundaryRadius + (outerRadii[next] - whiteYellowBoundaryRadius) * ((t1 - 0.25f) / 0.75f);
                    r2_outer = whiteYellowBoundaryRadius + (outerRadii[next] - whiteYellowBoundaryRadius) * ((t2 - 0.25f) / 0.75f);
                }
                
                float rad1 = (float) Math.toRadians(angle1);
                float rad2 = (float) Math.toRadians(angle2);
                
                float x1_inner = r1_inner * (float) Math.cos(rad1);
                float y1_inner = r1_inner * (float) Math.sin(rad1);
                float x1_outer = r1_outer * (float) Math.cos(rad1);
                float y1_outer = r1_outer * (float) Math.sin(rad1);
                
                float x2_inner = r2_inner * (float) Math.cos(rad2);
                float y2_inner = r2_inner * (float) Math.sin(rad2);
                float x2_outer = r2_outer * (float) Math.cos(rad2);
                float y2_outer = r2_outer * (float) Math.sin(rad2);
                
                float gradientFactor1 = (t1 + t2) * 0.5f;
                float gradientFactor2 = (t1 + t2) * 0.5f;
                
                float whiteToYellowFactor1;
                float yellowToRedFactor1;
                
                if (gradientFactor1 < 0.25f) {
                    whiteToYellowFactor1 = 1f - gradientFactor1 / 0.25f;
                    yellowToRedFactor1 = 0f;
                } else {
                    whiteToYellowFactor1 = 0f;
                    yellowToRedFactor1 = (gradientFactor1 - 0.25f) / 0.75f;
                }
                
                float whiteToYellowFactor2;
                float yellowToRedFactor2;
                
                if (gradientFactor2 < 0.25f) {
                    whiteToYellowFactor2 = 1f - gradientFactor2 / 0.25f;
                    yellowToRedFactor2 = 0f;
                } else {
                    whiteToYellowFactor2 = 0f;
                    yellowToRedFactor2 = (gradientFactor2 - 0.25f) / 0.75f;
                }
                
                float intensity1 = outerNoiseValues[i];
                float intensity2 = outerNoiseValues[next];
                
                float fadeOutFactor1;
                if (gradientFactor1 < 0.3f) {
                    fadeOutFactor1 = gradientFactor1 / 0.3f;
                } else {
                    fadeOutFactor1 = 1f - (gradientFactor1 - 0.25f) / 0.75f;
                }
                
                float fadeOutFactor2;
                if (gradientFactor2 < 0.3f) {
                    fadeOutFactor2 = gradientFactor2 / 0.3f;
                } else {
                    fadeOutFactor2 = 1f - (gradientFactor2 - 0.25f) / 0.75f;
                }
                
                int red1 = (int) ((255 * whiteToYellowFactor1 + (255 + 0 * intensity1) * (1f - whiteToYellowFactor1)) * (1f - yellowToRedFactor1) + 
                                 255 * yellowToRedFactor1);
                int green1 = (int) ((255 * whiteToYellowFactor1 + (220 + 35 * intensity1) * (1f - whiteToYellowFactor1)) * (1f - yellowToRedFactor1) + 
                                   0 * yellowToRedFactor1);
                int blue1 = (int) ((255 * whiteToYellowFactor1 + (80 + 40 * intensity1) * (1f - whiteToYellowFactor1)) * (1f - yellowToRedFactor1) + 
                                  0 * yellowToRedFactor1);
                float alphaBase1 = baseAlpha * fadeOutFactor1 * ((0.9f * whiteToYellowFactor1 + 0.95f * (1f - whiteToYellowFactor1)) * (1f - yellowToRedFactor1) + 
                                                                 0.7f * yellowToRedFactor1);
                int alpha1 = (int) (alphaBase1 * 255);
                
                int red2 = (int) ((255 * whiteToYellowFactor2 + (255 + 0 * intensity2) * (1f - whiteToYellowFactor2)) * (1f - yellowToRedFactor2) + 
                                 255 * yellowToRedFactor2);
                int green2 = (int) ((255 * whiteToYellowFactor2 + (220 + 35 * intensity2) * (1f - whiteToYellowFactor2)) * (1f - yellowToRedFactor2) + 
                                   0 * yellowToRedFactor2);
                int blue2 = (int) ((255 * whiteToYellowFactor2 + (80 + 40 * intensity2) * (1f - whiteToYellowFactor2)) * (1f - yellowToRedFactor2) + 
                                  0 * yellowToRedFactor2);
                float alphaBase2 = baseAlpha * fadeOutFactor2 * ((0.85f * whiteToYellowFactor2 + 0.9f * (1f - whiteToYellowFactor2)) * (1f - yellowToRedFactor2) + 
                                                                 0.65f * yellowToRedFactor2);
                int alpha2 = (int) (alphaBase2 * 255);
                
                Color innerColor = new Color(red1, green1, blue1, alpha1);
                Color outerColor = new Color(red2, green2, blue2, alpha2);
                
                GL11.glColor4ub((byte) innerColor.getRed(), (byte) innerColor.getGreen(), 
                               (byte) innerColor.getBlue(), (byte) innerColor.getAlpha());
                GL11.glVertex2f(x1_inner, y1_inner);
                GL11.glVertex2f(x1_outer, y1_outer);
                
                GL11.glColor4ub((byte) outerColor.getRed(), (byte) outerColor.getGreen(), 
                               (byte) outerColor.getBlue(), (byte) outerColor.getAlpha());
                GL11.glVertex2f(x2_outer, y2_outer);
                GL11.glVertex2f(x2_inner, y2_inner);
            }
            
            GL11.glEnd();
        }
        
        GL11.glPopMatrix();
    }
}
