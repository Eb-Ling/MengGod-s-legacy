package data;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.vector.Vector2f;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class PurpleSpiralSuctionEffect implements MyRenderTool.MyRender, MyRenderTool.InitializableEffect {

    private Vector2f position;
    private float elapsed;
    private float duration;
    private boolean expired;

    private float initialRadius;
    private float currentRadius;
    
    private int shaderProgram;
    private int vbo;
    private int ibo;

    private int uPositionLoc;
    private int uRadiusLoc;
    private int uTimeLoc;
    private int uDurationLoc;
    private int uProgressLoc;

    public PurpleSpiralSuctionEffect() {
        this.position = new Vector2f(0f, 0f);
        this.elapsed = 0f;
        this.duration = 100.0f;
        this.expired = false;
        this.initialRadius = 400f;
        this.currentRadius = initialRadius;

        createShaderProgram();
        createBuffers();
        cacheUniformLocations();
    }

    private void createShaderProgram() {
        try {
            String vertexSource = loadShaderFromFile("shaders/PurpleSpiralSuctionEffect/vertex.glsl");
            String fragmentSource = loadShaderFromFile("shaders/PurpleSpiralSuctionEffect/fragment.glsl");
            
            if (vertexSource.isEmpty() || fragmentSource.isEmpty()) {
                System.err.println("无法加载着色器文件");
                return;
            }
            
            int vert = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
            GL20.glShaderSource(vert, vertexSource);
            GL20.glCompileShader(vert);
            if (GL20.glGetShaderi(vert, GL20.GL_COMPILE_STATUS) == 0) {
                System.err.println("PurpleSpiral vert compile failed: " + GL20.glGetShaderInfoLog(vert, 1024));
                return;
            }

            int frag = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
            GL20.glShaderSource(frag, fragmentSource);
            GL20.glCompileShader(frag);
            if (GL20.glGetShaderi(frag, GL20.GL_COMPILE_STATUS) == 0) {
                System.err.println("PurpleSpiral frag compile failed: " + GL20.glGetShaderInfoLog(frag, 1024));
                return;
            }

            shaderProgram = GL20.glCreateProgram();
            GL20.glAttachShader(shaderProgram, vert);
            GL20.glAttachShader(shaderProgram, frag);
            GL20.glBindAttribLocation(shaderProgram, 0, "a_position");
            GL20.glBindAttribLocation(shaderProgram, 1, "a_texCoord");
            GL20.glLinkProgram(shaderProgram);

            if (GL20.glGetProgrami(shaderProgram, GL20.GL_LINK_STATUS) == 0) {
                System.err.println("PurpleSpiral link failed: " + GL20.glGetProgramInfoLog(shaderProgram, 1024));
                return;
            }

            GL20.glDeleteShader(vert);
            GL20.glDeleteShader(frag);

            System.out.println("PurpleSpiral shader compiled OK");
        } catch (Exception e) {
            System.err.println("PurpleSpiral shader error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String loadShaderFromFile(String filePath) {
        StringBuilder source = new StringBuilder();
        
        java.io.InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filePath);
        if (inputStream == null) {
            System.err.println("无法找到着色器资源: " + filePath);
            return "";
        }
        
        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(inputStream, java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                source.append(line).append("\n");
            }
        } catch (java.io.IOException e) {
            System.err.println("无法加载着色器文件: " + filePath);
            e.printStackTrace();
        }
        return source.toString();
    }

    private void createBuffers() {
        float halfSize = initialRadius;

        FloatBuffer verts = org.lwjgl.BufferUtils.createFloatBuffer(16);
        verts.put(new float[]{
            -halfSize, -halfSize,  0f, 0f,
             halfSize, -halfSize,  1f, 0f,
             halfSize,  halfSize,  1f, 1f,
            -halfSize,  halfSize,  0f, 1f,
        });
        verts.flip();

        IntBuffer indices = org.lwjgl.BufferUtils.createIntBuffer(6);
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

    private void cacheUniformLocations() {
        if (shaderProgram <= 0) return;
        uPositionLoc = GL20.glGetUniformLocation(shaderProgram, "u_position");
        uRadiusLoc = GL20.glGetUniformLocation(shaderProgram, "u_radius");
        uTimeLoc = GL20.glGetUniformLocation(shaderProgram, "u_time");
        uDurationLoc = GL20.glGetUniformLocation(shaderProgram, "u_duration");
        uProgressLoc = GL20.glGetUniformLocation(shaderProgram, "u_progress");
    }

    @Override
    public void initialize(float x, float y) {
        this.position.x = x;
        this.position.y = y;
        this.elapsed = 0f;
        this.expired = false;
        this.currentRadius = initialRadius;
    }

    @Override
    public void advance(float amount) {
        elapsed += amount;
        
        if (elapsed >= duration) {
            expired = true;
            return;
        }
        
        float progress = elapsed / duration;
        float shrinkFactor = 1.0f - (float) Math.pow(progress, 2.0);
        currentRadius = initialRadius * shrinkFactor;
    }

    @Override
    public void render() {
        if (expired || shaderProgram <= 0) return;

        float progress = elapsed / duration;
        progress = Math.max(0.0f, Math.min(1.0f, progress));

        GL11.glPushMatrix();
        GL11.glTranslatef(position.x, position.y, 0f);

        GL20.glUseProgram(shaderProgram);

        GL20.glUniform2f(uPositionLoc, 0f, 0f);
        GL20.glUniform1f(uRadiusLoc, currentRadius);
        GL20.glUniform1f(uTimeLoc, elapsed);
        GL20.glUniform1f(uDurationLoc, duration);
        GL20.glUniform1f(uProgressLoc, progress);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);

        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 16, 0);
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 16, 8);

        GL11.glDrawElements(GL11.GL_TRIANGLES, 6, GL11.GL_UNSIGNED_INT, 0);

        GL20.glDisableVertexAttribArray(0);
        GL20.glDisableVertexAttribArray(1);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);

        GL20.glUseProgram(0);

        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glPopMatrix();
    }

    @Override
    public boolean isExpired() {
        return expired;
    }

    public void setRadius(float radius) {
        this.initialRadius = radius;
        this.currentRadius = radius;
    }

    public void setDuration(float dur) {
        this.duration = dur;
    }
}
