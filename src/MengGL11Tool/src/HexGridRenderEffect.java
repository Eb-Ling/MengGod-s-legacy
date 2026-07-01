import data.MyRenderTool;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.vector.Vector2f;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

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
    private int uWaveCenterLoc;
    private int uWaveTriggerTimeLoc;
    
    private float waveCenterX = 0.5f;
    private float waveCenterY = 0.5f;
    private float waveTriggerTime = -999.0f;

    public HexGridRenderEffect() {
        this.position = new Vector2f(0f, 0f);
        this.elapsed = 0f;
        this.duration = 1000.0f;
        this.expired = false;
        this.canvasWidth = 200f;
        this.canvasHeight = 200f;

        createShaderProgram();
        createBuffers();
        cacheUniformLocations();
    }

    private void createShaderProgram() {
        try {
            String vertexSource =
                "#version 110\n" +
                "attribute vec2 a_position;\n" +
                "attribute vec2 a_texCoord;\n" +
                "varying vec2 v_uv;\n" +
                "void main() {\n" +
                "    v_uv = a_texCoord;\n" +
                "    gl_Position = gl_ModelViewProjectionMatrix * vec4(a_position, 0.0, 1.0);\n" +
                "}\n";

            String fragmentSource = 
                "#version 110\n" +
                "varying vec2 v_uv;\n" +
                "uniform float u_time;\n" +
                "uniform float u_hexSize;\n" +
                "uniform vec3 u_lineColor;\n" +
                "uniform vec3 u_bgColor;\n" +
                "uniform vec2 u_waveCenter;\n" +
                "uniform float u_waveTriggerTime;\n" +
                "\n" +
                "float calcHexDist(vec2 p, vec2 cellCenter, float size, float wave, vec2 waveOrigin) {\n" +
                "    float scaleMod = 1.0 + wave * 0.06;\n" +
                "    float distToOrigin = length(cellCenter - waveOrigin);\n" +
                "    vec2 dirToOrigin = (cellCenter - waveOrigin) / max(distToOrigin, 0.0001);\n" +
                "    float displacement = wave * 0.02;\n" +
                "    vec2 shiftedCenter = cellCenter + dirToOrigin * displacement;\n" +
                "    vec2 localPos = (p - shiftedCenter) / scaleMod;\n" +
                "    float d1 = abs(localPos.x);\n" +
                "    float d2 = abs(localPos.x * 0.5 + localPos.y * 0.8660254);\n" +
                "    float d3 = abs(localPos.x * 0.5 - localPos.y * 0.8660254);\n" +
                "    return max(max(d1, d2), d3);\n" +
                "}\n" +
                "\n" +
                "void main() {\n" +
                "    vec2 uv = v_uv;\n" +
                "    \n" +
                "    vec2 canvasCenter = vec2(0.5, 0.5);\n" +
                "    vec2 p = (uv - canvasCenter) * 2.0;\n" +
                "    vec2 waveOrigin = (u_waveCenter - canvasCenter) * 2.0;\n" +
                "    \n" +
                "    float r = length(p);\n" +
                "    float sphereRadius = 1.05;\n" +
                "    float z = sqrt(max(0.0, sphereRadius * sphereRadius - r * r));\n" +
                "    float bulge = sphereRadius / max(z, 0.01);\n" +
                "    vec2 warpedP = mix(p, p * bulge, 0.3);\n" +
                "    \n" +
                "    float size = u_hexSize;\n" +
                "    float hexWidth = size * 2.0;\n" +
                "    float hexHeight = size * 1.7320508;\n" +
                "    \n" +
                "    float row = floor((warpedP.y + hexHeight * 0.5) / hexHeight);\n" +
                "    \n" +
                "    float minDist = 999.0;\n" +
                "    float cellWave = 0.0;\n" +
                "    float waveAge = u_time - u_waveTriggerTime;\n" +
                "    float pulseLifetime = 1.5;\n" +
                "    \n" +
                "    for (int dr = -1; dr <= 1; dr++) {\n" +
                "        float nr = row + float(dr);\n" +
                "        float isOddRow = mod(nr, 2.0);\n" +
                "        float rowOffset = isOddRow * size;\n" +
                "        \n" +
                "        for (int dc = -1; dc <= 1; dc++) {\n" +
                "            float col = floor((warpedP.x - rowOffset + hexWidth * 0.5) / hexWidth) + float(dc);\n" +
                "            float cellCenterX = col * hexWidth + rowOffset;\n" +
                "            float cellCenterY = nr * hexHeight;\n" +
                "            vec2 cellCenter = vec2(cellCenterX, cellCenterY);\n" +
                "            \n" +
                "            float wave = 0.0;\n" +
                "            if (waveAge > 0.0 && waveAge < pulseLifetime) {\n" +
                "                float distToOrigin = length(cellCenter - waveOrigin);\n" +
                "                float ringRadius = waveAge * 1.5;\n" +
                "                float ringWidth = 0.18;\n" +
                "                float distToRing = abs(distToOrigin - ringRadius);\n" +
                "                float ringIntensity = 1.0 - smoothstep(0.0, ringWidth, distToRing);\n" +
                "                float fadeOut = 1.0 - smoothstep(0.0, pulseLifetime, waveAge);\n" +
                "                wave = ringIntensity * fadeOut * 0.8;\n" +
                "            }\n" +
                "            \n" +
                "            float d = calcHexDist(warpedP, cellCenter, size, wave, waveOrigin);\n" +
                "            if (d < minDist) {\n" +
                "                minDist = d;\n" +
                "                cellWave = wave;\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "    \n" +
                "    float borderWidth = size * 0.08;\n" +
                "    float edgeAlpha = smoothstep(0.0, borderWidth, abs(minDist - size));\n" +
                "    float opacity = 0.7 + cellWave * 0.375;\n" +
                "    float alpha = edgeAlpha * opacity;\n" +
                "    \n" +
                "    float circleMask = 1.0 - smoothstep(0.93, 1.0, r);\n" +
                "    alpha *= circleMask;\n" +
                "    \n" +
                "    gl_FragColor = vec4(u_bgColor, alpha);\n" +
                "}\n";

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

    private void cacheUniformLocations() {
        if (shaderProgram <= 0) return;
        uTimeLoc = GL20.glGetUniformLocation(shaderProgram, "u_time");
        uHexSizeLoc = GL20.glGetUniformLocation(shaderProgram, "u_hexSize");
        uLineColorLoc = GL20.glGetUniformLocation(shaderProgram, "u_lineColor");
        uBgColorLoc = GL20.glGetUniformLocation(shaderProgram, "u_bgColor");
        uWaveCenterLoc = GL20.glGetUniformLocation(shaderProgram, "u_waveCenter");
        uWaveTriggerTimeLoc = GL20.glGetUniformLocation(shaderProgram, "u_waveTriggerTime");
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
        GL20.glUniform1f(uHexSizeLoc, 0.06f);
        GL20.glUniform3f(uLineColorLoc, 0.2f, 0.6f, 1.0f);
        GL20.glUniform3f(uBgColorLoc, 0.53f, 0.81f, 0.98f);
        GL20.glUniform2f(uWaveCenterLoc, waveCenterX, waveCenterY);
        GL20.glUniform1f(uWaveTriggerTimeLoc, waveTriggerTime);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);

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
        this.waveCenterX = x;
        this.waveCenterY = y;
        this.waveTriggerTime = elapsed;
    }

    public Vector2f getPosition() {
        return new Vector2f(position.x, position.y);
    }
}
