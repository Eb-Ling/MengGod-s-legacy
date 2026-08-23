package data;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;
import org.lwjgl.util.vector.Vector2f;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * LightLanceEffect - SDF-based light lance (light lance) core rendering effect.
 *
 * Uses Signed Distance Field (SDF) to render an elongated lance-shaped glow core,
 * with smoothstep edge feathering for soft boundaries. No external textures required.
 *
 * Implements MyRender interface for lifecycle management (advance/render/isExpired).
 * Implements InitializableEffect for position initialization via initialize(x, y).
 *
 * Shader approach:
 *   - Outer lance body: elongated lens SDF built from two offset circles, stretched horizontally
 *   - Inner bright core: thinner lens SDF layered on top
 *   - Edge feathering: smoothstep on SDF distance for soft alpha falloff
 *   - Animation: sinusoidal pulse along lance body + breathing brightness
 *
 * Usage: Instantiate and add to MyRenderTool's active effects list.
 *   LightLanceEffect lance = new LightLanceEffect();
 *   lance.initialize(400f, 300f);
 *   activeEffects.add(lance);
 */
public class LightLanceEffect implements MyRenderTool.MyRender, MyRenderTool.InitializableEffect {

    /** Effect position in world/canvas coordinates */
    private Vector2f position;

    /** Elapsed time in seconds since effect started */
    private float elapsed;

    /** Total lifetime duration in seconds before effect expires */
    private float duration;

    /** Whether the effect has expired and should be removed */
    private boolean expired;

    /** Canvas width for the rendering quad (horizontal span of the lance) */
    private float canvasWidth;

    /** Canvas height for the rendering quad (vertical span of the lance) */
    private float canvasHeight;

    /** Compiled shader program handle */
    private int shaderProgram;

    /** Vertex buffer object for the rendering quad */
    private int vbo;

    /** Index buffer object for the rendering quad (two triangles) */
    private int ibo;

    /** Cached uniform location: elapsed time */
    private int uTimeLoc;

    /** Cached uniform location: total duration */
    private int uDurationLoc;

    /** Cached uniform location: animation progress (elapsed/duration) */
    private int uProgressLoc;

    /**
     * Default constructor. Initializes the lance effect with default dimensions
     * and compiles the shader program.
     * Path: Constructor -> createBuffers() -> createShaderProgram() -> cacheUniformLocations()
     */
    public LightLanceEffect() {
        this.position = new Vector2f(0f, 0f);
        this.elapsed = 0f;
        this.duration = 5.0f;
        this.expired = false;
        this.canvasWidth = 400f;
        this.canvasHeight = 80f;

        createBuffers();
        createShaderProgram();
        cacheUniformLocations();
    }

    /**
     * Creates VBO and IBO for a fullscreen quad used to render the SDF shader.
     * Vertex layout: [x, y, u, v] per vertex, 4 vertices forming 2 triangles.
     * Path: Called from constructor.
     */
    private void createBuffers() {
        float halfW = canvasWidth * 0.5f;
        float halfH = canvasHeight * 0.5f;

        FloatBuffer verts = BufferUtils.createFloatBuffer(16);
        verts.put(new float[]{
            -halfW, -halfH,  0f, 0f,
             halfW, -halfH,  1f, 0f,
             halfW,  halfH,  1f, 1f,
            -halfW,  halfH,  0f, 1f,
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

    /**
     * Compiles and links the SDF light lance shader program.
     * Vertex shader: standard passthrough (a_position + a_texCoord -> v_uv).
     * Fragment shader: SDF lance body + inner core + edge feathering + animation.
     * Path: Called from constructor.
     */
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
                "uniform float u_duration;\n" +
                "uniform float u_progress;\n" +
                "\n" +
                "float lanceTaper(float t) {\n" +
                "    t = clamp(t, 0.0, 1.0);\n" +
                "    return smoothstep(0.0, 0.08, t) * (1.0 - smoothstep(0.15, 1.0, t));\n" +
                "}\n" +
                "\n" +
                "float sdLance(vec2 p, float baseX, float tipX, float maxR) {\n" +
                "    float len = tipX - baseX;\n" +
                "    float t = clamp((p.x - baseX) / len, 0.0, 1.0);\n" +
                "    float r = maxR * lanceTaper(t);\n" +
                "    return abs(p.y) - r;\n" +
                "}\n" +
                "\n" +
                "float xFade(vec2 p, float baseX, float tipX) {\n" +
                "    float fb = smoothstep(baseX - 8.0, baseX + 8.0, p.x);\n" +
                "    float ft = smoothstep(tipX + 8.0, tipX - 8.0, p.x);\n" +
                "    return fb * ft;\n" +
                "}\n" +
                "\n" +
                "float hash(vec2 p) {\n" +
                "    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);\n" +
                "}\n" +
                "\n" +
                "float vnoise(vec2 p) {\n" +
                "    vec2 i = floor(p);\n" +
                "    vec2 f = fract(p);\n" +
                "    f = f * f * (3.0 - 2.0 * f);\n" +
                "    float a = hash(i);\n" +
                "    float b = hash(i + vec2(1.0, 0.0));\n" +
                "    float c = hash(i + vec2(0.0, 1.0));\n" +
                "    float d = hash(i + vec2(1.0, 1.0));\n" +
                "    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);\n" +
                "}\n" +
                "\n" +
                "float fbm(vec2 p) {\n" +
                "    float v = 0.0;\n" +
                "    float a = 0.5;\n" +
                "    for (int i = 0; i < 4; i++) {\n" +
                "        v += a * vnoise(p);\n" +
                "        p *= 2.0;\n" +
                "        a *= 0.5;\n" +
                "    }\n" +
                "    return v;\n" +
                "}\n" +
                "\n" +
                "void main() {\n" +
                "    vec2 p = (v_uv - 0.5) * vec2(400.0, 80.0);\n" +
                "    vec2 cp = vec2(p.x, p.y);\n" +
                "\n" +
                "    float pr = u_progress;\n" +
                "    float baseX = -170.0;\n" +
                "    float finalTip = 160.0;\n" +
                "\n" +
                "    float ext = smoothstep(0.0, 0.3, pr);\n" +
                "    float tipX = baseX + (finalTip - baseX) * ext;\n" +
                "\n" +
                "    float pillarT = smoothstep(0.72, 0.92, pr);\n" +
                "    float flashFreq = 18.0 + pillarT * 65.0;\n" +
                "    float flashAmp = 0.15 + pillarT * 0.85;\n" +
                "    float flicker = (1.0 - flashAmp) + flashAmp * (sin(u_time * flashFreq) * 0.5 + sin(u_time * flashFreq * 1.73) * 0.5);\n" +
                "\n" +
                "    float hFade = xFade(cp, baseX, tipX);\n" +
                "\n" +
                "    float coreDist = sdLance(cp, baseX, tipX, 6.0);\n" +
                "    float coreDistPillar = abs(cp.y) - 6.0;\n" +
                "    float coreAlpha = smoothstep(6.0, -8.0, mix(coreDist, coreDistPillar, pillarT)) * hFade;\n" +
                "\n" +
                "    float glowDist = sdLance(cp, baseX, tipX, 18.0);\n" +
                "    float glowAlpha = smoothstep(6.0, -8.0, glowDist) * hFade * 0.2 * (1.0 - pillarT);\n" +
                "\n" +
                "    float haloDist = sdLance(cp, baseX, tipX, 25.0);\n" +
                "    vec2 nCoord = vec2(cp.x * 0.01 - u_time * 2.0, cp.y * 0.1);\n" +
                "    float fbmVal = fbm(nCoord);\n" +
                "    float haloAlpha = pow(smoothstep(15.0, -25.0, haloDist), 1.2) * hFade * fbmVal * (1.0 - pillarT);\n" +
                "\n" +
                "    float brightness = 1.0 + pillarT * pillarT * 5.0;\n" +
                "\n" +
                "    vec3 coreColor = vec3(1.6, 1.6, 1.8) * coreAlpha;\n" +
                "    vec3 glowColor = vec3(0.3, 0.55, 1.0) * glowAlpha;\n" +
                "    vec3 haloColor = vec3(0.2, 0.4, 0.9) * haloAlpha;\n" +
                "\n" +
                "    vec3 color = (coreColor + glowColor) * flicker + haloColor;\n" +
                "    color *= brightness;\n" +
                "    float alpha = clamp(coreAlpha + glowAlpha + haloAlpha, 0.0, 1.0);\n" +
                "\n" +
                "    float fadeIn = smoothstep(0.0, 0.05, pr);\n" +
                "    float fadeOut = 1.0 - smoothstep(0.95, 1.0, pr);\n" +
                "    alpha *= fadeIn * fadeOut;\n" +
                "    color *= fadeIn * fadeOut;\n" +
                "\n" +
                "    if (alpha < 0.001) discard;\n" +
                "    gl_FragColor = vec4(color, alpha);\n" +
                "}\n";

            int vert = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
            GL20.glShaderSource(vert, vertexSource);
            GL20.glCompileShader(vert);
            if (GL20.glGetShaderi(vert, GL20.GL_COMPILE_STATUS) == 0) {
                System.err.println("LightLance vertex shader compile failed: " + GL20.glGetShaderInfoLog(vert, 1024));
                return;
            }

            int frag = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
            GL20.glShaderSource(frag, fragmentSource);
            GL20.glCompileShader(frag);
            if (GL20.glGetShaderi(frag, GL20.GL_COMPILE_STATUS) == 0) {
                System.err.println("LightLance fragment shader compile failed: " + GL20.glGetShaderInfoLog(frag, 1024));
                return;
            }

            shaderProgram = GL20.glCreateProgram();
            GL20.glAttachShader(shaderProgram, vert);
            GL20.glAttachShader(shaderProgram, frag);
            GL20.glBindAttribLocation(shaderProgram, 0, "a_position");
            GL20.glBindAttribLocation(shaderProgram, 1, "a_texCoord");
            GL20.glLinkProgram(shaderProgram);

            if (GL20.glGetProgrami(shaderProgram, GL20.GL_LINK_STATUS) == 0) {
                System.err.println("LightLance shader program link failed: " + GL20.glGetProgramInfoLog(shaderProgram, 1024));
                return;
            }

            GL20.glDeleteShader(vert);
            GL20.glDeleteShader(frag);

            System.out.println("LightLanceEffect shader compiled OK");
        } catch (Exception e) {
            System.err.println("LightLanceEffect shader error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Caches uniform locations for the shader program to avoid repeated lookups.
     * Uniforms: u_time (float), u_duration (float), u_progress (float).
     * Path: Called from constructor after createShaderProgram().
     */
    private void cacheUniformLocations() {
        if (shaderProgram <= 0) return;
        uTimeLoc = GL20.glGetUniformLocation(shaderProgram, "u_time");
        uDurationLoc = GL20.glGetUniformLocation(shaderProgram, "u_duration");
        uProgressLoc = GL20.glGetUniformLocation(shaderProgram, "u_progress");
    }

    /**
     * Initializes the effect position and resets the timer.
     * Called by MyRenderTool when the effect is spawned at a specific location.
     *
     * @param x Horizontal position in canvas coordinates
     * @param y Vertical position in canvas coordinates
     */
    @Override
    public void initialize(float x, float y) {
        this.position.x = x;
        this.position.y = y;
        this.elapsed = 0f;
        this.expired = false;
    }

    /**
     * Advances the effect simulation by the given time delta.
     * Marks the effect as expired when elapsed time exceeds duration.
     *
     * @param amount Time delta in seconds (from MyRenderTool main loop)
     */
    @Override
    public void advance(float amount) {
        elapsed += amount;
        if (elapsed >= duration) {
            expired = true;
            return;
        }
    }

    /**
     * Renders the SDF light lance effect at the current position.
     * Uses additive blending (GL_SRC_ALPHA, GL_ONE) for glow accumulation.
     * Path: Called each frame by MyRenderTool's render loop.
     */
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

        // Translate to effect position
        GL11.glTranslatef(position.x, position.y, 0f);

        // Bind shader and set uniforms
        GL20.glUseProgram(shaderProgram);
        GL20.glUniform1f(uTimeLoc, elapsed);                  // Already in seconds
        GL20.glUniform1f(uDurationLoc, duration);
        GL20.glUniform1f(uProgressLoc, elapsed / duration);

        // Bind VBO/IBO and configure vertex attributes
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);

        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 16, 0);
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 16, 8);

        // Additive blending for glow effect
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

        GL11.glDrawElements(GL11.GL_TRIANGLES, 6, GL11.GL_UNSIGNED_INT, 0);

        // Cleanup state
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

    /**
     * Returns whether this effect has expired and should be removed.
     *
     * @return true if elapsed time >= duration
     */
    @Override
    public boolean isExpired() {
        return expired;
    }

    /**
     * Releases all GPU resources (shader program, VBO, IBO).
     * Should be called when the effect is removed from the active list.
     * Path: Called externally during effect cleanup.
     */
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

    /**
     * Sets the total lifetime duration of the effect.
     *
     * @param dur Duration in seconds
     */
    public void setDuration(float dur) {
        this.duration = dur;
    }
}
