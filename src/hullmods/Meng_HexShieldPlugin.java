package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;
import org.lwjgl.util.vector.Vector2f;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.EnumSet;

public class Meng_HexShieldPlugin implements CombatLayeredRenderingPlugin {

    private static final org.apache.log4j.Logger LOG = Global.getLogger(Meng_HexShieldPlugin.class);

    private ShipAPI ship;
    private float timer = 0f;
    private boolean expired = false;
    private boolean initOnce = false;
    private boolean glInitialized = false;
    private float shieldRadius;

    // FBO
    private int fboId = 0;
    private int fboTexId = 0;
    private int fboW = 0;
    private int fboH = 0;
    private static final int CANVAS = 512;

    // Shader
    private int shaderProgram;
    private int uTimeLoc, uHexSizeLoc, uBgColorLoc;
    private int uShieldFacingLoc, uShieldArcLoc;
    private int uMaskTextureLoc;
    private int[] uWaveCenterLocs = new int[3];
    private int[] uWaveTriggerTimeLocs = new int[3];
    private int[] uWaveDamageLocs = new int[3];

    // Wave data
    private float[] waveCenterX = new float[3];
    private float[] waveCenterY = new float[3];
    private float[] waveTriggerTime = new float[3];
    private float[] waveDamage = new float[3];

    // Shield mask sprite (loaded via Starsector sprite system)
    private SpriteAPI shieldSprite;

    // Damage modifier
    private float lastHitTime = -0.3f;
    private MyDamageDealtModifier damageModifier;

    private float fadeAlpha = 0f;
    private float shieldOffTimer = 0f;
    private static final float SHIELD_FADE_DURATION = 0.4f;
    private float lastActiveArcRad = 0f;
    private float lastShieldFacingDeg = 0f;

    // GL resources for fullscreen quad
    private int vbo, ibo;

    public Meng_HexShieldPlugin(ShipAPI ship) {
        this.ship = ship;
        this.shieldRadius = ship.getShieldRadiusEvenIfNoShield();
        
        try {
            this.shieldSprite = Global.getSettings().getSprite("fx", "Meng_Shield");
            LOG.info("[HexShield] Sprite loaded successfully for: " + ship.getName());
        } catch (Exception e) {
            LOG.error("[HexShield] Failed to load sprite: " + e.getMessage(), e);
            this.shieldSprite = null;
        }

        for (int i = 0; i < 3; i++) {
            waveCenterX[i] = 0.5f;
            waveCenterY[i] = 0.5f;
            waveTriggerTime[i] = -999f;
            waveDamage[i] = 0f;
        }
    }

    private void initGL() {
        if (glInitialized) return;
        createShaderProgram();
        createBuffers();
        cacheUniformLocations();
        glInitialized = true;
    }

    // ==================== SHADER ====================

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
                "uniform vec3 u_bgColor;\n" +
                "uniform float u_shieldFacing;\n" +
                "uniform float u_shieldArc;\n" +
                "uniform sampler2D u_maskTexture;\n" +
                "uniform vec2 u_waveCenters[3];\n" +
                "uniform float u_waveTriggerTimes[3];\n" +
                "uniform float u_waveDamages[3];\n" +
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
                "    vec2 canvasCenter = vec2(0.5, 0.5);\n" +
                "    vec2 p = (uv - canvasCenter) * 2.0;\n" +
                "    float sphereRadius = 1.05;\n" +
                "    float r = length(p);\n" +
                "    float z = sqrt(max(0.0, sphereRadius * sphereRadius - r * r));\n" +
                "    float bulge = sphereRadius / max(z, 0.01);\n" +
                "    vec2 warpedP = mix(p, p * bulge, 0.3);\n" +
                "\n" +
                "    vec2 warpedOrigins[3];\n" +
                "    for (int wi = 0; wi < 3; wi++) {\n" +
                "        vec2 wOrigin = (u_waveCenters[wi] - canvasCenter) * 2.0;\n" +
                "        float wR = length(wOrigin);\n" +
                "        float wZ = sqrt(max(0.0, sphereRadius * sphereRadius - wR * wR));\n" +
                "        float wBulge = sphereRadius / max(wZ, 0.01);\n" +
                "        warpedOrigins[wi] = mix(wOrigin, wOrigin * wBulge, 0.3);\n" +
                "    }\n" +
                "\n" +
                "    float size = u_hexSize;\n" +
                "    float hexWidth = size * 2.0;\n" +
                "    float hexHeight = size * 1.7320508;\n" +
                "    float row = floor((warpedP.y + hexHeight * 0.5) / hexHeight);\n" +
                "    float minDist = 999.0;\n" +
                "    float cellWave = 0.0;\n" +
                "\n" +
                "    for (int dr = -1; dr <= 1; dr++) {\n" +
                "        float nr = row + float(dr);\n" +
                "        float isOddRow = mod(nr, 2.0);\n" +
                "        float rowOffset = isOddRow * size;\n" +
                "        for (int dc = -1; dc <= 1; dc++) {\n" +
                "            float col = floor((warpedP.x - rowOffset + hexWidth * 0.5) / hexWidth) + float(dc);\n" +
                "            float cellCenterX = col * hexWidth + rowOffset;\n" +
                "            float cellCenterY = nr * hexHeight;\n" +
                "            vec2 cellCenter = vec2(cellCenterX, cellCenterY);\n" +
                "            float wave = 0.0;\n" +
                "            vec2 useOrigin = vec2(0.0);\n" +
                "            for (int wi = 0; wi < 3; wi++) {\n" +
                "                float wAge = u_time - u_waveTriggerTimes[wi];\n" +
                "                float dmgScale =0.3+clamp(u_waveDamages[wi]/100.0, 0.0, 1.0)*0.6+clamp((u_waveDamages[wi]-100.0)/400.0, 0.0, 1.0)*0.3;\n" +
                "                float waveLifetime = dmgScale*0.5;\n" +
                "                if (wAge > 0.0 && wAge < waveLifetime) {\n" +
                "                    float distToOrigin = length(cellCenter - warpedOrigins[wi]);\n" +
                "                    float ringSpeed =  3.0;\n" +
                "                    float ringRadius = wAge * ringSpeed;\n" +
                "                    float ringWidth =  dmgScale * 0.18;\n" +
                "                    float distToRing = abs(distToOrigin - ringRadius);\n" +
                "                    float ringIntensity = 1.0 - smoothstep(0.0, ringWidth, distToRing);\n" +
                "                    float fadeOut = 1.0 - smoothstep(0.0, waveLifetime, wAge);\n" +
                "                    float ringVal = ringIntensity * fadeOut * 1.2;\n" +
                "                    if (ringVal > wave) {\n" +
                "                        wave = ringVal;\n" +
                "                        useOrigin = warpedOrigins[wi];\n" +
                "                    }\n" +
                "                }\n" +
                "            }\n" +
                "            float d = calcHexDist(warpedP, cellCenter, size, wave, useOrigin);\n" +
                "            if (d < minDist) {\n" +
                "                minDist = d;\n" +
                "                cellWave = wave;\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    float borderWidth = size * 0.08;\n" +
                "    float edgeAlpha = smoothstep(0.0, borderWidth, abs(minDist - size));\n" +
                "    float opacity = 0.85 + cellWave * 0.6;\n" +
                "    float alpha = edgeAlpha * opacity;\n" +
                "\n" +
                "    vec2 duv = uv - vec2(0.5);\n" +
                "    float rot1 = u_time * 0.4;\n" +
                "    float rot2 = u_time * -0.2;\n" +
                "    float cr1 = cos(rot1);\n" +
                "    float sr1 = sin(rot1);\n" +
                "    float cr2 = cos(rot2);\n" +
                "    float sr2 = sin(rot2);\n" +
                "    vec2 uv1 = vec2(0.5) + vec2(duv.x * cr1 - duv.y * sr1, duv.x * sr1 + duv.y * cr1);\n" +
                "    vec2 uv2 = vec2(0.5) + vec2(duv.x * cr2 - duv.y * sr2, duv.x * sr2 + duv.y * cr2);\n" +
                "    float maskAlpha = 0.3 + 0.7 * texture2D(u_maskTexture, uv1).a * texture2D(u_maskTexture, uv2).a;\n" +
                "    float circleFade = 1.0 - smoothstep(0.92, 1.05, r);\n" +
                "    alpha *= maskAlpha * circleFade;\n" +
                "\n" +
                "    float pixelAngle = atan(p.y, p.x);\n" +
                "    float angleDiff = pixelAngle - u_shieldFacing;\n" +
                "    angleDiff = mod(angleDiff + 3.14159265, 6.2831853) - 3.14159265;\n" +
                "    float halfArc = u_shieldArc * 0.5;\n" +
                "    float arcEdge = 0.06;\n" +
                "    float arcMask = 1.0 - smoothstep(halfArc , halfArc + arcEdge, abs(angleDiff));\n" +
                "    alpha *= arcMask;\n" +
                "\n" +
                "    gl_FragColor = vec4(u_bgColor, alpha * 0.85);\n" +

                "}\n";

            int vert = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
            GL20.glShaderSource(vert, vertexSource);
            GL20.glCompileShader(vert);
            if (GL20.glGetShaderi(vert, GL20.GL_COMPILE_STATUS) == 0) {
                LOG.error("[HexShield] Vertex shader compile fail: " + GL20.glGetShaderInfoLog(vert, 1024));
                return;
            }

            int frag = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
            GL20.glShaderSource(frag, fragmentSource);
            GL20.glCompileShader(frag);
            if (GL20.glGetShaderi(frag, GL20.GL_COMPILE_STATUS) == 0) {
                LOG.error("[HexShield] Fragment shader compile fail: " + GL20.glGetShaderInfoLog(frag, 1024));
                return;
            }

            shaderProgram = GL20.glCreateProgram();
            GL20.glAttachShader(shaderProgram, vert);
            GL20.glAttachShader(shaderProgram, frag);
            GL20.glBindAttribLocation(shaderProgram, 0, "a_position");
            GL20.glBindAttribLocation(shaderProgram, 1, "a_texCoord");
            GL20.glLinkProgram(shaderProgram);

            if (GL20.glGetProgrami(shaderProgram, GL20.GL_LINK_STATUS) == 0) {
                LOG.error("[HexShield] Shader link fail: " + GL20.glGetProgramInfoLog(shaderProgram, 1024));
                return;
            }

            GL20.glDeleteShader(vert);
            GL20.glDeleteShader(frag);
            LOG.info("[HexShield] Shader compiled and linked successfully");
        } catch (Exception e) {
            LOG.error("[HexShield] Shader creation error: " + e.getMessage(), e);
        }
    }

    private void createBuffers() {
        float half = CANVAS * 0.5f;
        
        // 每个顶点: x, y, u, v → 4 floats × 4 vertices = 16 floats
        FloatBuffer verts = BufferUtils.createFloatBuffer(16);
        verts.put(new float[]{
            -half, -half, 0f, 0f,   // vertex 0: pos(-,-), uv(0,0)
             half, -half, 1f, 0f,   // vertex 1: pos(+,-), uv(1,0)
             half,  half, 1f, 1f,   // vertex 2: pos(+,+), uv(1,1)
            -half,  half, 0f, 1f,   // vertex 3: pos(-,+), uv(0,1)
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
        uBgColorLoc = GL20.glGetUniformLocation(shaderProgram, "u_bgColor");
        uShieldFacingLoc = GL20.glGetUniformLocation(shaderProgram, "u_shieldFacing");
        uShieldArcLoc = GL20.glGetUniformLocation(shaderProgram, "u_shieldArc");
        uMaskTextureLoc = GL20.glGetUniformLocation(shaderProgram, "u_maskTexture");
        for (int i = 0; i < 3; i++) {
            uWaveCenterLocs[i] = GL20.glGetUniformLocation(shaderProgram, "u_waveCenters[" + i + "]");
            uWaveTriggerTimeLocs[i] = GL20.glGetUniformLocation(shaderProgram, "u_waveTriggerTimes[" + i + "]");
            uWaveDamageLocs[i] = GL20.glGetUniformLocation(shaderProgram, "u_waveDamages[" + i + "]");
        }
    }

    // ==================== FBO ====================

    private void ensureFBO(int w, int h) {
        int tw = nextPowerOfTwo(w);
        int th = nextPowerOfTwo(h);
        if (fboId != 0 && fboW == tw && fboH == th) return;
        destroyFBO();
        fboW = tw;
        fboH = th;

        fboTexId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, fboTexId);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, fboW, fboH, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);

        fboId = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fboId);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, fboTexId, 0);
        int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
        if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
            System.err.println("HexShield FBO fail: " + status);
            destroyFBO();
            return;
        }
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    private void destroyFBO() {
        if (fboId != 0) { GL30.glDeleteFramebuffers(fboId); fboId = 0; }
        if (fboTexId != 0) { GL11.glDeleteTextures(fboTexId); fboTexId = 0; }
    }

    private int nextPowerOfTwo(int value) {
        if (value <= 0) return 1;
        value--;
        value |= value >> 1; value |= value >> 2;
        value |= value >> 4; value |= value >> 8;
        value |= value >> 16;
        return value + 1;
    }

    // ==================== CombatLayeredRenderingPlugin ====================

    @Override
    public void init(CombatEntityAPI entity) {}

    @Override
    public void cleanup() {
        destroyFBO();
        if (shaderProgram > 0) { GL20.glDeleteProgram(shaderProgram); shaderProgram = 0; }
        if (vbo != 0) { GL15.glDeleteBuffers(vbo); vbo = 0; }
        if (ibo != 0) { GL15.glDeleteBuffers(ibo); ibo = 0; }
        if (ship != null && damageModifier != null) {
            ship.removeListener(damageModifier);
            damageModifier = null;
        }
    }

    @Override
    public boolean isExpired() {
        return expired;
    }

    public void markExpired() {
        expired = true;
    }

    @Override
    public void advance(float amount) {
        if (expired) return;
        if (!ship.isAlive()) { expired = true; return; }

        timer += amount;

        ShieldAPI shield = ship.getShield();
        if (shield != null && shield.isOn()) {
            fadeAlpha = 1f;
            shieldOffTimer = 0f;
            lastActiveArcRad = (float) Math.toRadians(shield.getActiveArc());
            lastShieldFacingDeg = shield.getFacing();
        } else if (fadeAlpha > 0f) {
            shieldOffTimer += amount;
            fadeAlpha = Math.max(0f, 1f - shieldOffTimer / SHIELD_FADE_DURATION);
        }

        if (!initOnce) {
            initOnce = true;
        }

        if (damageModifier == null) {
            damageModifier = new MyDamageDealtModifier(this);
            ship.addListener(damageModifier);
        }
    }

    @Override
    public EnumSet<CombatEngineLayers> getActiveLayers() {
        return EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_LAYER);
    }

    @Override
    public float getRenderRadius() {
        // Use a very large radius to prevent frustum culling (like clockbuilder does)
        return 10000000f;
    }

    // ==================== RENDER ====================

    @Override
    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        if (layer != CombatEngineLayers.ABOVE_SHIPS_LAYER) return;
        if (expired || !ship.isAlive()) return;

        initGL();
        if (shaderProgram <= 0) {
            LOG.debug("[HexShield] Shader not initialized, skipping render");
            return;
        }
        if (shieldSprite == null) {
            LOG.warn("[HexShield] Shield sprite is null, skipping render");
            return;
        }

        ShieldAPI shield = ship.getShield();
        if (shield == null) {
            LOG.debug("[HexShield] Shield is null, skipping render");
            return;
        }
        if (!shield.isOn() && fadeAlpha <= 0f) return;

        float shieldR = shield.isOn() ? shield.getRadius() : shieldRadius;

        if (shieldR <= 0f) {
            LOG.debug("[HexShield] Shield radius <= 0 (" + shieldR + "), skipping render");
            return;
        }

        ensureFBO(CANVAS, CANVAS);
        if (fboId == 0) {
            LOG.error("[HexShield] FBO creation failed, skipping render");
            return;
        }

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

        GL20.glUseProgram(0);
        
        // Bind FBO using same logic as ensureFBO
        try {
            Class<?> shaderLibClass = Class.forName("com.fs.starfarer.api.impl.shader.ShaderLib");
            java.lang.reflect.Method useBufferCoreMethod = shaderLibClass.getMethod("useBufferCore");
            java.lang.reflect.Method useBufferARBMethod = shaderLibClass.getMethod("useBufferARB");
            
            boolean useCore = (Boolean) useBufferCoreMethod.invoke(null);
            boolean useARB = (Boolean) useBufferARBMethod.invoke(null);
            
            if (useCore) {
                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fboId);
            } else if (useARB) {
                org.lwjgl.opengl.ARBFramebufferObject.glBindFramebuffer(
                    org.lwjgl.opengl.ARBFramebufferObject.GL_FRAMEBUFFER, fboId);
            } else {
                org.lwjgl.opengl.EXTFramebufferObject.glBindFramebufferEXT(
                    org.lwjgl.opengl.EXTFramebufferObject.GL_FRAMEBUFFER_EXT, fboId);
            }
        } catch (Exception e) {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fboId);
        }
        
        GL11.glViewport(0, 0, fboW, fboH);

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0, fboW, 0, fboH, -2000, 2000);
        GL11.glMatrixMode(GL11.GL_TEXTURE);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glTranslatef(fboW * 0.5f, fboH * 0.5f, 0f);

        GL11.glColorMask(true, true, true, true);
        GL11.glClearColor(0f, 0f, 0f, 0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        // Now bind shader and set uniforms
        GL20.glUseProgram(shaderProgram);
        
        int glError = GL11.glGetError();
        if (glError != GL11.GL_NO_ERROR) {
            LOG.error("[HexShield] GL error after glUseProgram: " + glError);
        }
        
        GL20.glUniform1f(uTimeLoc, timer);
        GL20.glUniform1f(uHexSizeLoc, 0.05f);
        GL20.glUniform3f(uBgColorLoc, 0.56f, 0.0f, 1.0f);

        float shieldFacingDeg = shield.isOn() ? shield.getFacing() : lastShieldFacingDeg;
        float shieldFacingRad = 1.5707963f;
        float shieldArcRad = shield.isOn() ? (float) Math.toRadians(shield.getActiveArc()) : lastActiveArcRad;
        GL20.glUniform1f(uShieldFacingLoc, shieldFacingRad);
        GL20.glUniform1f(uShieldArcLoc, shieldArcRad);

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, shieldSprite.getTextureId());
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
        GL20.glUniform1i(uMaskTextureLoc, 0);

        for (int i = 0; i < 3; i++) {
            GL20.glUniform2f(uWaveCenterLocs[i], waveCenterX[i], waveCenterY[i]);
            GL20.glUniform1f(uWaveTriggerTimeLocs[i], waveTriggerTime[i]);
            GL20.glUniform1f(uWaveDamageLocs[i], waveDamage[i]);
        }

        glError = GL11.glGetError();
        if (glError != GL11.GL_NO_ERROR) {
            LOG.error("[HexShield] GL error after setting uniforms: " + glError);
        }

        // Bind VBO and IBO
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);

        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 16, 0);
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 16, 8);

        glError = GL11.glGetError();
        if (glError != GL11.GL_NO_ERROR) {
            LOG.error("[HexShield] GL error after vertex attrib setup: " + glError);
        }

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_TEXTURE_2D);

        GL11.glDrawElements(GL11.GL_TRIANGLES, 6, GL11.GL_UNSIGNED_INT, 0);

        GL11.glDisable(GL11.GL_TEXTURE_2D);

        glError = GL11.glGetError();

        if (glError != GL11.GL_NO_ERROR) {
            LOG.error("[HexShield] GL error after draw: " + glError + " (1282=GL_INVALID_OPERATION)");
        }

        GL20.glDisableVertexAttribArray(0);
        GL20.glDisableVertexAttribArray(1);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        GL20.glUseProgram(0);

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_TEXTURE);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();

        // Unbind FBO
        try {
            Class<?> shaderLibClass = Class.forName("com.fs.starfarer.api.impl.shader.ShaderLib");
            java.lang.reflect.Method useBufferCoreMethod = shaderLibClass.getMethod("useBufferCore");
            java.lang.reflect.Method useBufferARBMethod = shaderLibClass.getMethod("useBufferARB");
            
            boolean useCore = (Boolean) useBufferCoreMethod.invoke(null);
            boolean useARB = (Boolean) useBufferARBMethod.invoke(null);
            
            if (useCore) {
                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
            } else if (useARB) {
                org.lwjgl.opengl.ARBFramebufferObject.glBindFramebuffer(
                    org.lwjgl.opengl.ARBFramebufferObject.GL_FRAMEBUFFER, 0);
            } else {
                org.lwjgl.opengl.EXTFramebufferObject.glBindFramebufferEXT(
                    org.lwjgl.opengl.EXTFramebufferObject.GL_FRAMEBUFFER_EXT, 0);
            }
        } catch (Exception e) {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        }
        
        GL11.glPopAttrib();

        // Step 2: draw FBO texture to screen (following clockbuilder pattern)
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glMatrixMode(GL11.GL_TEXTURE);
        GL11.glPushMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();

        Vector2f shipLoc = ship.getLocation();
        GL11.glTranslatef(shipLoc.x, shipLoc.y, 0f);
        GL11.glRotatef(shieldFacingDeg - 90f, 0f, 0f, 1f);

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, fboTexId);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1f, 1f, 1f, fadeAlpha);

        float halfSz = shieldR * 1.08f;
        
        // Map full FBO texture to shield quad
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0f, 0f); GL11.glVertex2f(-halfSz, -halfSz);
        GL11.glTexCoord2f(1f, 0f); GL11.glVertex2f( halfSz, -halfSz);
        GL11.glTexCoord2f(1f, 1f); GL11.glVertex2f( halfSz,  halfSz);
        GL11.glTexCoord2f(0f, 1f); GL11.glVertex2f(-halfSz,  halfSz);
        GL11.glEnd();

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_TEXTURE);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();

        GL11.glPopAttrib();
    }

    // ==================== WAVE TRIGGER ====================

    public void triggerWave(float uvX, float uvY, float damage) {
        for (int i = 0; i < 3; i++) {
            if (waveTriggerTime[i] < 0 || timer - waveTriggerTime[i] > 0.5f) {
                waveCenterX[i] = uvX;
                waveCenterY[i] = uvY;
                waveTriggerTime[i] = timer;
                waveDamage[i] = damage;
                return;
            }
        }
        int oldest = 0;
        for (int i = 1; i < 3; i++) {
            if (waveTriggerTime[i] < waveTriggerTime[oldest]) {
                oldest = i;
            }
        }
        waveCenterX[oldest] = uvX;
        waveCenterY[oldest] = uvY;
        waveTriggerTime[oldest] = timer;
        waveDamage[oldest] = damage;
    }

    private void onShieldHit(Vector2f hitWorldPos, float shieldDmg) {
        if (shieldDmg < 20f) return;
        if (timer - lastHitTime < 0.166f) return;
        lastHitTime = timer;

        ShieldAPI shield = ship.getShield();

        if (shield == null) return;
        float sr = shield.getRadius();
        if (sr <= 0f) return;

        Vector2f shipLoc = ship.getLocation();
        float dx = hitWorldPos.x - shipLoc.x;
        float dy = hitWorldPos.y - shipLoc.y;

        float facingRad = (float) Math.toRadians(shield.getFacing() - 90f);
        float cosA = (float) Math.cos(-facingRad);
        float sinA = (float) Math.sin(-facingRad);
        float localX = dx * cosA - dy * sinA;
        float localY = dx * sinA + dy * cosA;

        float uvX = (localX / sr) * 0.5f + 0.5f;
        float uvY = (localY / sr) * 0.5f + 0.5f;

        triggerWave(uvX, uvY, shieldDmg);

    }

    // ==================== DAMAGE LISTENER ====================

    private static class MyDamageDealtModifier implements DamageTakenModifier {
        private Meng_HexShieldPlugin plugin;

        public MyDamageDealtModifier(Meng_HexShieldPlugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage,
                                        Vector2f point, boolean shieldHit) {
            if (plugin == null || plugin.expired) return null;
            if (!shieldHit) return null;

            float shieldDmg = damage.getDamage();
            if (shieldDmg <= 0f) return null;

            plugin.onShieldHit(point, shieldDmg);
            return null;
        }
    }
}
