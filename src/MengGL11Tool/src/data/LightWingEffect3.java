package data;


import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.vector.Vector2f;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class LightWingEffect3 implements MyRenderTool.MyRender, MyRenderTool.InitializableEffect {

    private Vector2f position;
    private float elapsed;
    private float duration;
    private boolean expired;

    private float wingLength;
    private float wingWidth;
    private float wingAngle;
    private float flowSpeed;

    private float[] colorInner;
    private float[] colorOuter;
    private float[] colorGlow;

    private int shaderProgram;
    private int vbo;
    private int ibo;

    private int uPositionLoc;
    private int uWingLengthLoc;
    private int uWingWidthLoc;
    private int uWingAngleLoc;
    private int uTimeLoc;
    private int uFlowSpeedLoc;
    private int uLocalAlphaLoc;
    private int uColorInnerLoc;
    private int uColorOuterLoc;
    private int uColorGlowLoc;
    private int uPulseColor1Loc;
    private int uPulseColor2Loc;
    private int uPulseColor3Loc;

    private static final String VERT_SRC =
        "#version 110\n" +
        "attribute vec2 a_position;\n" +
        "attribute vec2 a_texCoord;\n" +
        "varying vec2 v_uv;\n" +
        "void main() {\n" +
        "    v_uv = a_texCoord;\n" +
        "    gl_Position = gl_ModelViewProjectionMatrix * vec4(a_position, 0.0, 1.0);\n" +
        "}\n";

    private static final String FRAG_SRC =
        "#version 110\n" +
        "varying vec2 v_uv;\n" +
        "uniform float u_time;\n" +
        "uniform float u_flowSpeed;\n" +
        "uniform vec3 u_colorInner;\n" +
        "uniform vec3 u_colorOuter;\n" +
        "uniform vec3 u_colorGlow;\n" +
        "uniform float u_localAlpha;\n" +
        "uniform vec3 u_pulseColor1;\n" +
        "uniform vec3 u_pulseColor2;\n" +
        "\n" +
        "vec2 hash2(vec2 p) {\n" +
        "    p = vec2(dot(p, vec2(127.1, 311.7)),\n" +
        "             dot(p, vec2(269.5, 183.3)));\n" +
        "    return vec2(\n" +
        "        fract(sin(p.x) * 43758.5453123),\n" +
        "        fract(sin(p.y) * 28647.8923190)\n" +
        "    );\n" +
        "}\n" +
        "\n" +
        "float noise(vec2 p) {\n" +
        "    vec2 i = floor(p);\n" +
        "    vec2 f = fract(p);\n" +
        "    vec2 u = f * f * f * (f * (f * 6.0 - 15.0) + 10.0);\n" +
        "    float a = dot(hash2(i + vec2(0.0, 0.0)) * 2.0 - 1.0, f - vec2(0.0, 0.0));\n" +
        "    float b = dot(hash2(i + vec2(1.0, 0.0)) * 2.0 - 1.0, f - vec2(1.0, 0.0));\n" +
        "    float c = dot(hash2(i + vec2(0.0, 1.0)) * 2.0 - 1.0, f - vec2(0.0, 1.0));\n" +
        "    float d = dot(hash2(i + vec2(1.0, 1.0)) * 2.0 - 1.0, f - vec2(1.0, 1.0));\n" +
        "    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y) * 0.5 + 0.5;\n" +
        "}\n" +
        "\n" +
        "void main() {\n" +
        "    float ux = v_uv.x;\n" +
        "    float vy = v_uv.y;\n" +
        "    float t = u_time * u_flowSpeed;\n" +
        "    float distFromCenter = abs(vy - 0.5) * 2.0;\n" +
        "\n" +
        "    float tipWarp = noise(vec2(vy * 6.0 + t * 0.35, t * 0.2)) * 0.12\n" +
        "                 + noise(vec2(vy * 15.0 + t * 0.15, 1.7)) * 0.05;\n" +
        "    float tipFade = smoothstep(max(0.95 + tipWarp, 0.78 + tipWarp * 0.3 + 0.17), 0.75 + tipWarp * 0.3, ux);\n" +
        "    float rootFade = smoothstep(0.0, 0.06, ux);\n" +
        "    float shapeMask = tipFade * rootFade;\n" +
        "\n" +
        "    if (shapeMask < 0.01) discard;\n" +
        "\n" +
        "    float edgeFade = smoothstep(1.0, 0.75, distFromCenter);\n" +
        "\n" +
        "    float ribbon = (1.0 - ux * 0.55) * (1.0 - distFromCenter * distFromCenter * 0.4);\n" +
        "    ribbon *= 0.92 + 0.08 * noise(vec2(ux * 4.0 - t * 0.5, vy * 3.0));\n" +
        "\n" +
        "    float cycle = mod(ux * 3.0 - t * 0.7, 3.5);\n" +
        "    float halfP = 1.75;\n" +
        "    float behind = halfP - (cycle < 0.0 ? cycle + 3.5 : cycle);\n" +
        "\n" +
        "    float trail = behind > 0.0 ? smoothstep(halfP, 0.0, behind) : 0.0;\n" +
        "\n" +
        "    float pulseShape = pow(max(0.0, 1.0 - abs(cycle - halfP) * 12.0), 6.0) * edgeFade * tipFade;\n" +
        "    pulseShape *= 0.5 + noise(vec2(vy * 4.0, ux * 3.0 + t * 0.3)) * 0.5;\n" +
        "\n" +
        "    float cycle2 = mod(ux * 3.0 - t * 0.9 + 1.5, 2.5);\n" +
        "    float halfP2 = 1.25;\n" +
        "    float behind2 = halfP2 - (cycle2 < 0.0 ? cycle2 + 2.5 : cycle2);\n" +
        "    float trail2 = behind2 > 0.0 ? smoothstep(halfP2, 0.0, behind2) : 0.0;\n" +
        "\n" +
        "    float pulseShape2 = pow(max(0.0, 1.0 - abs(cycle2 - halfP2) * 10.0), 5.0) * edgeFade * tipFade;\n" +
        "    pulseShape2 *= 0.5 + noise(vec2(vy * 4.0 + 3.0, ux * 3.0 + t * 0.4)) * 0.5;\n" +
        "\n" +
        "    float pulseGlow = pow(max(0.0, 1.0 - abs(cycle - halfP) * 3.0), 2.0) * edgeFade * tipFade * 0.06;\n" +
        "    float pulseGlow2 = pow(max(0.0, 1.0 - abs(cycle2 - halfP2) * 3.5), 2.0) * edgeFade * tipFade * 0.05;\n" +
        "\n" +
        "    float energy = 0.18 + ribbon * edgeFade * 0.6 + (pulseShape + pulseShape2) * 0.15;\n" +
        "\n" +
        "    float edgeGlow = pow(ux, 2.0) * 0.25;\n" +
        "    float rootGlow = pow(1.0 - ux, 3.0) * 0.5;\n" +
        "\n" +
        "    vec3 col = mix(u_colorInner, u_colorOuter, ux);\n" +
        "    col += u_colorGlow * (edgeGlow + rootGlow);\n" +
        "    col += u_pulseColor1 * pulseShape * 0.08;\n" +
        "    col += u_pulseColor2 * pulseShape2 * 0.07;\n" +
        "    col += u_pulseColor1 * (pulseShape * 0.3 + pulseGlow);\n" +
        "    col += u_pulseColor2 * (pulseShape2 * 0.25 + pulseGlow2);\n" +
        "    col *= (0.65 + energy * 1.0);\n" +
        "    col += u_pulseColor1 * trail * 0.6;\n" +
        "    col += u_pulseColor2 * trail2 * 0.5;\n" +
        "\n" +
        "    float alpha = shapeMask * energy * u_localAlpha * 0.75;\n" +
        "\n" +
        "    gl_FragColor = vec4(col * alpha * 1.8, alpha);\n" +
        "}\n";

    private static final float[][] PALETTES = {
        {0.4f, 0.65f, 1.0f,  0.1f, 0.25f, 1.0f,  0.55f, 0.8f, 1.0f},
        {1.0f, 0.3f, 0.5f,   0.85f, 0.08f, 0.25f, 1.0f, 0.45f, 0.6f},
        {0.3f, 1.0f, 0.55f,  0.08f, 0.7f, 0.18f,  0.45f, 1.0f, 0.65f},
        {1.0f, 0.8f, 0.25f,  1.0f, 0.35f, 0.08f,  1.0f, 0.9f, 0.45f},
        {0.7f, 0.3f, 1.0f,   0.4f, 0.08f, 0.85f,  0.8f, 0.5f, 1.0f},
    };
    private int paletteIndex = 0;

    public LightWingEffect3() {
        this.position = new Vector2f(0f, 0f);
        this.elapsed = 0f;
        this.duration = -1f;
        this.expired = false;

        this.wingLength = 220f;
        this.wingWidth = 55f;
        this.wingAngle = 0f;
        this.flowSpeed = 1.5f;

        applyPalette(0);

        createShaderProgram();
        createBuffers();
        cacheUniformLocations();
    }

    private void applyPalette(int index) {
        paletteIndex = index % PALETTES.length;
        float[] p = PALETTES[paletteIndex];
        colorInner = new float[]{p[0], p[1], p[2]};
        colorOuter = new float[]{p[3], p[4], p[5]};
        colorGlow  = new float[]{p[6], p[7], p[8]};
    }

    private void createShaderProgram() {
        try {
            int vert = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
            GL20.glShaderSource(vert, VERT_SRC);
            GL20.glCompileShader(vert);
            if (GL20.glGetShaderi(vert, GL20.GL_COMPILE_STATUS) == 0) {
                System.err.println("LightWing3 vert compile failed: " + GL20.glGetShaderInfoLog(vert, 1024));
                return;
            }

            int frag = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
            GL20.glShaderSource(frag, FRAG_SRC);
            GL20.glCompileShader(frag);
            if (GL20.glGetShaderi(frag, GL20.GL_COMPILE_STATUS) == 0) {
                System.err.println("LightWing3 frag compile failed: " + GL20.glGetShaderInfoLog(frag, 1024));
                return;
            }

            shaderProgram = GL20.glCreateProgram();
            GL20.glAttachShader(shaderProgram, vert);
            GL20.glAttachShader(shaderProgram, frag);
            GL20.glBindAttribLocation(shaderProgram, 0, "a_position");
            GL20.glBindAttribLocation(shaderProgram, 1, "a_texCoord");
            GL20.glLinkProgram(shaderProgram);

            if (GL20.glGetProgrami(shaderProgram, GL20.GL_LINK_STATUS) == 0) {
                System.err.println("LightWing3 link failed: " + GL20.glGetProgramInfoLog(shaderProgram, 1024));
                return;
            }

            GL20.glDeleteShader(vert);
            GL20.glDeleteShader(frag);

            System.out.println("LightWing3 shader compiled OK");
        } catch (Exception e) {
            System.err.println("LightWing3 shader error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createBuffers() {
        float halfW = wingLength;
        float halfH = wingWidth;

        FloatBuffer verts = org.lwjgl.BufferUtils.createFloatBuffer(16);
        verts.put(new float[]{
            -halfW, -halfH,  0f, 0f,
             halfW, -halfH,  1f, 0f,
             halfW,  halfH,  1f, 1f,
            -halfW,  halfH,  0f, 1f,
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
        uPositionLoc     = GL20.glGetUniformLocation(shaderProgram, "u_position");
        uWingLengthLoc   = GL20.glGetUniformLocation(shaderProgram, "u_wingLength");
        uWingWidthLoc    = GL20.glGetUniformLocation(shaderProgram, "u_wingWidth");
        uWingAngleLoc    = GL20.glGetUniformLocation(shaderProgram, "u_wingAngle");
        uTimeLoc         = GL20.glGetUniformLocation(shaderProgram, "u_time");
        uFlowSpeedLoc    = GL20.glGetUniformLocation(shaderProgram, "u_flowSpeed");
        uLocalAlphaLoc   = GL20.glGetUniformLocation(shaderProgram, "u_localAlpha");
        uColorInnerLoc   = GL20.glGetUniformLocation(shaderProgram, "u_colorInner");
        uColorOuterLoc   = GL20.glGetUniformLocation(shaderProgram, "u_colorOuter");
        uColorGlowLoc    = GL20.glGetUniformLocation(shaderProgram, "u_colorGlow");
        uPulseColor1Loc  = GL20.glGetUniformLocation(shaderProgram, "u_pulseColor1");
        uPulseColor2Loc  = GL20.glGetUniformLocation(shaderProgram, "u_pulseColor2");
        uPulseColor3Loc  = GL20.glGetUniformLocation(shaderProgram, "u_pulseColor3");
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
        if (duration > 0f && elapsed >= duration) {
            expired = true;
        }
    }

    @Override
    public void render() {
        if (expired || shaderProgram <= 0) return;

        float localAlpha = 1.0f;
        if (duration > 0f) {
            float progress = elapsed / duration;
            localAlpha = 1.0f - progress * progress;
            localAlpha = Math.max(0f, Math.min(1f, localAlpha));
        }

        GL11.glPushMatrix();

        GL20.glUseProgram(shaderProgram);

        GL20.glUniform2f(uPositionLoc, position.x, position.y);
        GL20.glUniform1f(uWingLengthLoc, wingLength);
        GL20.glUniform1f(uWingWidthLoc, wingWidth);
        GL20.glUniform1f(uWingAngleLoc, wingAngle);
        GL20.glUniform1f(uTimeLoc, elapsed);
        GL20.glUniform1f(uFlowSpeedLoc, flowSpeed);
        GL20.glUniform1f(uLocalAlphaLoc, localAlpha);
        GL20.glUniform3f(uColorInnerLoc, colorInner[0], colorInner[1], colorInner[2]);
        GL20.glUniform3f(uColorOuterLoc, colorOuter[0], colorOuter[1], colorOuter[2]);
        GL20.glUniform3f(uColorGlowLoc, colorGlow[0], colorGlow[1], colorGlow[2]);
        GL20.glUniform3f(uPulseColor1Loc, 0.8f, 0.2f, 0.05f);
        GL20.glUniform3f(uPulseColor2Loc, 0.15f, 0.5f, 0.9f);
        GL20.glUniform3f(uPulseColor3Loc, 0f, 0f, 0f);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

        GL11.glTranslatef(position.x, position.y, 0f);
        GL11.glRotatef(wingAngle, 0f, 0f, 1f);

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

    public void setWingParams(float length, float width, float angle) {
        this.wingLength = length;
        this.wingWidth = width;
        this.wingAngle = angle;
    }

    public void setFlowSpeed(float speed) {
        this.flowSpeed = speed;
    }

    public void setDuration(float dur) {
        this.duration = dur;
    }

    public void cyclePalette() {
        applyPalette(paletteIndex + 1);
        String[] names = {"Blue-Cyan", "Red-Pink", "Green", "Gold", "Purple"};
        System.out.println("LightWing3 palette: " + names[paletteIndex]);
    }
}
