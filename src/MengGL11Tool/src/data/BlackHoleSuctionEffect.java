package data;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

public class BlackHoleSuctionEffect implements MyRenderTool.MyRender, MyRenderTool.InitializableEffect {
    private Vector2f position;
    private float elapsed;
    private float duration;
    private int textureId;
    private int textureWidth;
    private int textureHeight;
    private boolean expired;
    private float initialRadius;
    private float currentRadius;
    private float[][] noiseMap;
    private float[] segmentSpeeds;
    private int noiseSegments;
    
    public BlackHoleSuctionEffect() {
        this.position = new Vector2f(0f, 0f);
        this.elapsed = 0f;
        this.duration = 2.0f;
        this.expired = false;
        this.initialRadius = 150f;
        this.currentRadius = initialRadius;
        this.noiseSegments = 64;
        this.textureId = 0;
        this.textureWidth = 0;
        this.textureHeight = 0;
        this.segmentSpeeds = new float[noiseSegments];
        
        generateNoiseMap();
        loadTexture();
    }
    
    private void generateNoiseMap() {
        noiseMap = new float[noiseSegments][2];
        long seed = System.currentTimeMillis();
        java.util.Random random = new java.util.Random(seed);
        
        for (int i = 0; i < noiseSegments; i++) {
            float angle = (float) (2 * Math.PI * i / noiseSegments);
            
            float noise1 = (float) (Math.sin(i * 0.4 + seed * 0.01) * 0.12);
            float noise2 = (float) (Math.cos(i * 0.9 + seed * 0.02) * 0.08);
            float noise3 = (float) (Math.sin(i * 1.5 - seed * 0.015) * 0.06);
            float noise4 = (float) (Math.cos(i * 2.1 + seed * 0.025) * 0.04);
            
            float combinedNoise = noise1 + noise2 + noise3 + noise4;
            combinedNoise = Math.max(-0.25f, Math.min(0.25f, combinedNoise));
            
            noiseMap[i][0] = angle;
            noiseMap[i][1] = 1.0f + combinedNoise;
            
            segmentSpeeds[i] = 0.3f + random.nextFloat() * 1.5f;
        }
    }
    
    private void loadTexture() {
        try {
            String texturePath = "D:\\Starsector\\mods\\MengGod's legacy\\graphics\\Meng\\ships\\Meng_timeboss.png";
            java.io.File textureFile = new java.io.File(texturePath);
            
            if (!textureFile.exists()) {
                System.err.println("纹理文件不存在: " + texturePath);
                return;
            }
            
            java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(textureFile);
            
            if (image == null) {
                System.err.println("无法读取图片文件");
                return;
            }
            
            textureWidth = image.getWidth();
            textureHeight = image.getHeight();
            
            int[] pixels = new int[textureWidth * textureHeight];
            image.getRGB(0, 0, textureWidth, textureHeight, pixels, 0, textureWidth);
            
            java.nio.ByteBuffer buffer = org.lwjgl.BufferUtils.createByteBuffer(textureWidth * textureHeight * 4);
            
            for (int y = 0; y < textureHeight; y++) {
                for (int x = 0; x < textureWidth; x++) {
                    int pixel = pixels[y * textureWidth + x];
                    buffer.put((byte) ((pixel >> 16) & 0xFF));
                    buffer.put((byte) ((pixel >> 8) & 0xFF));
                    buffer.put((byte) (pixel & 0xFF));
                    buffer.put((byte) ((pixel >> 24) & 0xFF));
                }
            }
            
            buffer.flip();
            
            textureId = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
            
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
            
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA,
                    textureWidth, textureHeight, 0,
                    GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
            
            System.out.println("黑洞纹理加载成功: " + textureWidth + "x" + textureHeight);
            
        } catch (Exception e) {
            System.err.println("纹理加载失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public void initialize(float x, float y) {
        this.position.x = x;
        this.position.y = y;
        this.elapsed = 0f;
        this.expired = false;
        this.currentRadius = initialRadius;
        generateNoiseMap();
    }
    
    @Override
    public void advance(float amount) {
        elapsed += amount;
        
        if (elapsed >= duration) {
            expired = true;
            return;
        }
        
        float progress = elapsed / duration;
        float phase1End = 0.6f;
        
        for (int i = 0; i < noiseSegments; i++) {
            float baseAngle = noiseMap[i][0];
            float currentNoiseFactor = noiseMap[i][1];
            
            float turbulence1 = (float) (Math.sin(i * 0.6 + elapsed * 7.0) * 0.08);
            float turbulence2 = (float) (Math.cos(i * 1.1 - elapsed * 5.5) * 0.06);
            float turbulence3 = (float) (Math.sin(i * 1.8 + elapsed * 9.0) * 0.04);
            float turbulence4 = (float) (Math.cos(i * 2.5 - elapsed * 6.5) * 0.03);
            
            float dynamicTurbulence = turbulence1 + turbulence2 + turbulence3 + turbulence4;
            
            if (progress < phase1End) {
                float phase1Progress = progress / phase1End;
                
                float speedMultiplier = segmentSpeeds[i];
                
                float contractionAmount = 0.015f * speedMultiplier * amount * 60.0f;
                
                float irregularity = 1.0f + dynamicTurbulence * 0.8f;
                
                currentNoiseFactor -= contractionAmount * irregularity;
                
                currentNoiseFactor = Math.max(0.15f, Math.min(1.3f, currentNoiseFactor));
                
            } else {
                float phase2Progress = (progress - phase1End) / (1.0f - phase1End);
                
                float slowThreshold = 0.5f;
                if (currentNoiseFactor > slowThreshold) {
                    float accelerationFactor = 2.5f + phase2Progress * 3.0f;
                    
                    float fastContraction = 0.025f * accelerationFactor * amount * 60.0f;
                    
                    float targetValue = 0.0f;
                    
                    currentNoiseFactor -= fastContraction;
                    
                    currentNoiseFactor = Math.max(targetValue, currentNoiseFactor);
                }
                
                float smoothFactor = 1.0f - phase2Progress;
                currentNoiseFactor = currentNoiseFactor * smoothFactor + 0.0f * (1.0f - smoothFactor);
            }
            
            noiseMap[i][1] = currentNoiseFactor;
        }
        
        float globalShrink = 1.0f - (float) Math.pow(progress, 2.5);
        currentRadius = initialRadius * globalShrink;
    }
    
    @Override
    public void render() {
        if (expired || textureId <= 0) return;
        
        float progress = elapsed / duration;
        float alpha = 1.0f - progress;
        alpha = Math.max(0.0f, Math.min(1.0f, alpha));
        
        GL11.glPushMatrix();
        GL11.glTranslatef(position.x, position.y, 0f);
        
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        
        GL11.glColor4f(1f, 1f, 1f, alpha);
        
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glTexCoord2f(0.5f, 0.5f);
        GL11.glVertex2f(0f, 0f);
        
        for (int i = 0; i <= noiseSegments; i++) {
            int index = i % noiseSegments;
            float angle = noiseMap[index][0];
            float noiseFactor = noiseMap[index][1];
            
            float distortedRadius = currentRadius * noiseFactor;
            
            float x = (float) Math.cos(angle) * distortedRadius;
            float y = (float) Math.sin(angle) * distortedRadius;
            
            float u = 0.5f + 0.5f * (float) Math.cos(angle);
            float v = 0.5f + 0.5f * (float) Math.sin(angle);
            
            GL11.glTexCoord2f(u, v);
            GL11.glVertex2f(x, y);
        }
        
        GL11.glEnd();
        
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        
        GL11.glPopMatrix();
    }
    
    @Override
    public boolean isExpired() {
        return expired;
    }
}
