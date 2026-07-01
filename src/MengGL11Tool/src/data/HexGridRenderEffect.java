package data;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;
import org.lwjgl.util.vector.Vector2f;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;

public class HexGridRenderEffect implements MyRenderTool.MyRender, MyRenderTool.InitializableEffect {

    private Vector2f position;
    private float elapsed;
    private float duration;
    private boolean expired;
    
    private float canvasWidth;
    private float canvasHeight;
    
    private int shaderProgram;
    private int vbo;
    private int ibo;
    
    private int uTimeLoc;
    private int uHexSizeLoc;
    private int uLineColorLoc;
    private int uBgColorLoc;
    private int uMaskTextureLoc;
    private int[] uWaveCenterLocs = new int[5];
    private int[] uWaveTriggerTimeLocs = new int[5];
    
    private float[] waveCenterX = new float[5];
    private float[] waveCenterY = new float[5];
    private float[] waveTriggerTime = new float[5];

    private int textureId;

    public HexGridRenderEffect() {
        this.position = new Vector2f(0f, 0f);
        this.elapsed = 0f;
        this.duration = 1000.0f;
        this.expired = false;
        this.canvasWidth = 256f;
        this.canvasHeight = 256f;

        for (int i = 0; i < 5; i++) {
            waveCenterX[i] = 0.5f;
            waveCenterY[i] = 0.5f;
            waveTriggerTime[i] = -999.0f;
        }

        createShaderProgram();
        createBuffers();
        cacheUniformLocations();
        loadMaskTexture();
    }

    private void createShaderProgram() {
        try {
            String vertexPath = "method/shaders/Hex/vertex.glsl";
            String fragmentPath = "method/shaders/Hex/fragment.glsl";
            
            String vertexSource = loadShaderFile(vertexPath);
            String fragmentSource = loadShaderFile(fragmentPath);
            
            if (vertexSource == null || fragmentSource == null) {
                System.err.println("Failed to load shader files");
                return;
            }

            int vert = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
            GL20.glShaderSource(vert, vertexSource);
            GL20.glCompileShader(vert);
            if (GL20.glGetShaderi(vert, GL20.GL_COMPILE_STATUS) == 0) {
                System.err.println("HexGrid vertex shader compile failed: " + GL20.glGetShaderInfoLog(vert, 1024));
                return;
            }

            int frag = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
            GL20.glShaderSource(frag, fragmentSource);
            GL20.glCompileShader(frag);
            if (GL20.glGetShaderi(frag, GL20.GL_COMPILE_STATUS) == 0) {
                System.err.println("HexGrid fragment shader compile failed: " + GL20.glGetShaderInfoLog(frag, 1024));
                return;
            }

            shaderProgram = GL20.glCreateProgram();
            GL20.glAttachShader(shaderProgram, vert);
            GL20.glAttachShader(shaderProgram, frag);
            GL20.glBindAttribLocation(shaderProgram, 0, "a_position");
            GL20.glBindAttribLocation(shaderProgram, 1, "a_texCoord");
            GL20.glLinkProgram(shaderProgram);

            if (GL20.glGetProgrami(shaderProgram, GL20.GL_LINK_STATUS) == 0) {
                System.err.println("HexGrid program link failed: " + GL20.glGetProgramInfoLog(shaderProgram, 1024));
                return;
            }

            GL20.glDeleteShader(vert);
            GL20.glDeleteShader(frag);

            System.out.println("HexGrid shader compiled OK");
        } catch (Exception e) {
            System.err.println("HexGrid shader error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String loadShaderFile(String path) {
        try {
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(path));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            return sb.toString();
        } catch (Exception e) {
            System.err.println("Failed to load shader file: " + path);
            e.printStackTrace();
            return null;
        }
    }

    private void createBuffers() {
        float halfWidth = canvasWidth * 0.5f;
        float halfHeight = canvasHeight * 0.5f;

        FloatBuffer verts = BufferUtils.createFloatBuffer(16);
        verts.put(new float[]{
            -halfWidth, -halfHeight,  0f, 0f,
             halfWidth, -halfHeight,  1f, 0f,
             halfWidth,  halfHeight,  1f, 1f,
            -halfWidth,  halfHeight,  0f, 1f,
        });
        verts.flip();

        IntBuffer indices = BufferUtils.createIntBuffer(6);
        indices.put(new int[]{0, 1, 2, 0, 2, 3});
        indices.flip();

        vbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, verts, GL15.GL_STATIC_DRAW);

        ibo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indices, GL15.GL_STATIC_DRAW);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    private void loadMaskTexture() {
        try {
            String path = "D:\\Starsector\\starsector-core\\graphics\\fx\\shields256.png";
            File file = new File(path);
            if (!file.exists()) {
                System.err.println("Mask texture not found: " + path);
                return;
            }
            BufferedImage image = ImageIO.read(file);
            int w = image.getWidth();
            int h = image.getHeight();
            int[] pixels = new int[w * h];
            image.getRGB(0, 0, w, h, pixels, 0, w);
            ByteBuffer buffer = BufferUtils.createByteBuffer(w * h * 4);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int pixel = pixels[y * w + x];
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
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, w, h, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
            System.out.println("Shield mask loaded: " + w + "x" + h);
        } catch (Exception e) {
            System.err.println("Mask load failed: " + e.getMessage());
        }
    }

    private void cacheUniformLocations() {
        if (shaderProgram <= 0) return;
        uTimeLoc = GL20.glGetUniformLocation(shaderProgram, "u_time");
        uHexSizeLoc = GL20.glGetUniformLocation(shaderProgram, "u_hexSize");
        uLineColorLoc = GL20.glGetUniformLocation(shaderProgram, "u_lineColor");
        uBgColorLoc = GL20.glGetUniformLocation(shaderProgram, "u_bgColor");
        uMaskTextureLoc = GL20.glGetUniformLocation(shaderProgram, "u_maskTexture");
        for (int i = 0; i < 5; i++) {
            uWaveCenterLocs[i] = GL20.glGetUniformLocation(shaderProgram, "u_waveCenters[" + i + "]");
            uWaveTriggerTimeLocs[i] = GL20.glGetUniformLocation(shaderProgram, "u_waveTriggerTimes[" + i + "]");
        }
    }

    @Override
    public void initialize(float x, float y) {
        this.position.x = x;
        this.position.y = y;
        this.elapsed = 0f;
        this.expired = false;
    }

    @Override
    public void advance(float amount) {
        elapsed += amount;
        
        if (elapsed >= duration) {
            expired = true;
            return;
        }
    }

    @Override
    public void render() {
        if (expired || shaderProgram <= 0) return;

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glMatrixMode(GL11.GL_TEXTURE);
        GL11.glPushMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();

        GL11.glTranslatef(position.x, position.y, 0f);

        GL20.glUseProgram(shaderProgram);
        GL20.glUniform1f(uTimeLoc, elapsed);
        GL20.glUniform1f(uHexSizeLoc, 0.05f);
        GL20.glUniform3f(uLineColorLoc, 0.2f, 0.6f, 1.0f);
        GL20.glUniform3f(uBgColorLoc, 0.56f, 0.0f, 1.0f);
        GL20.glUniform1i(uMaskTextureLoc, 0);
        for (int i = 0; i < 5; i++) {
            GL20.glUniform2f(uWaveCenterLocs[i], waveCenterX[i], waveCenterY[i]);
            GL20.glUniform1f(uWaveTriggerTimeLocs[i], waveTriggerTime[i]);
        }

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);

        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 16, 0);
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 16, 8);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL11.glDrawElements(GL11.GL_TRIANGLES, 6, GL11.GL_UNSIGNED_INT, 0);

        GL20.glDisableVertexAttribArray(0);
        GL20.glDisableVertexAttribArray(1);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
        GL20.glUseProgram(0);

        GL11.glDisable(GL11.GL_TEXTURE_2D);

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_TEXTURE);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();

        GL11.glPopAttrib();
    }

    @Override
    public boolean isExpired() {
        return expired;
    }

    public void cleanup() {
        expired = true;
        if (shaderProgram > 0) {
            GL20.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }
        if (vbo != 0) {
            GL15.glDeleteBuffers(vbo);
            vbo = 0;
        }
        if (ibo != 0) {
            GL15.glDeleteBuffers(ibo);
            ibo = 0;
        }
        if (textureId != 0) {
            GL11.glDeleteTextures(textureId);
            textureId = 0;
        }
    }

    public void setDuration(float dur) {
        this.duration = dur;
    }

    public void setHexSize(float size) {
        if (shaderProgram > 0) {
            GL20.glUseProgram(shaderProgram);
            GL20.glUniform1f(uHexSizeLoc, size);
            GL20.glUseProgram(0);
        }
    }

    public void setLineColor(float r, float g, float b) {
        if (shaderProgram > 0) {
            GL20.glUseProgram(shaderProgram);
            GL20.glUniform3f(uLineColorLoc, r, g, b);
            GL20.glUseProgram(0);
        }
    }

    public void setBgColor(float r, float g, float b) {
        if (shaderProgram > 0) {
            GL20.glUseProgram(shaderProgram);
            GL20.glUniform3f(uBgColorLoc, r, g, b);
            GL20.glUseProgram(0);
        }
    }

    public void triggerWave(float x, float y) {
        for (int i = 0; i < 5; i++) {
            if (waveTriggerTime[i] < 0 || elapsed - waveTriggerTime[i] > 2.0f) {
                waveCenterX[i] = x;
                waveCenterY[i] = y;
                waveTriggerTime[i] = elapsed;
                return;
            }
        }
        int oldest = 0;
        for (int i = 1; i < 5; i++) {
            if (waveTriggerTime[i] < waveTriggerTime[oldest]) {
                oldest = i;
            }
        }
        waveCenterX[oldest] = x;
        waveCenterY[oldest] = y;
        waveTriggerTime[oldest] = elapsed;
    }

    public Vector2f getPosition() {
        return new Vector2f(position.x, position.y);
    }
}
