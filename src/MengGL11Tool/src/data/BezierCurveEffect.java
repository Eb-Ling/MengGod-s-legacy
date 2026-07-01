package data;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.util.vector.Vector2f;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class BezierCurveEffect implements MyRenderTool.MyRender, MyRenderTool.InitializableEffect {

    private Vector2f position;
    private float elapsed;
    private float duration;
    private boolean expired;

    private float width;
    private float height;
    
    private int shaderProgram;
    private int vbo;
    private int ibo;
    private int fboId = 0;
    private int fboTexId = 0;
    private int fboW = 0;
    private int fboH = 0;

    private int uTimeLoc;
    private int uDurationLoc;
    private int uProgressLoc;

    public BezierCurveEffect() {
        this.position = new Vector2f(0f, 0f);
        this.elapsed = 0f;
        this.duration = 100.0f;
        this.expired = false;
        this.width = 800f;
        this.height = 200f;

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
                "uniform float u_duration;\n" +
                "uniform float u_progress;\n" +
                "\n" +
                "vec4 permute(vec4 x) {\n" +
                "    return mod(((x * 34.0) + 1.0) * x, 289.0);\n" +
                "}\n" +
                "\n" +
                "float snoise(vec3 v) {\n" +
                "    const vec2 C = vec2(1.0 / 6.0, 1.0 / 3.0);\n" +
                "    const vec4 D = vec4(0.0, 0.5, 1.0, 2.0);\n" +
                "\n" +
                "    vec3 i  = floor(v + dot(v, C.yyy));\n" +
                "    vec3 x0 = v - i + dot(i, C.xxx);\n" +
                "\n" +
                "    vec3 g = step(x0.yzx, x0.xyz);\n" +
                "    vec3 l = 1.0 - g;\n" +
                "    vec3 i1 = min(g.xyz, l.zxy);\n" +
                "    vec3 i2 = max(g.xyz, l.zxy);\n" +
                "\n" +
                "    vec3 x1 = x0 - i1 + C.xxx;\n" +
                "    vec3 x2 = x0 - i2 + C.yyy;\n" +
                "    vec3 x3 = x0 - D.yyy;\n" +
                "\n" +
                "    i = mod(i, 289.0);\n" +
                "    vec4 p = permute(permute(permute(\n" +
                "             i.z + vec4(0.0, i1.z, i2.z, 1.0))\n" +
                "           + i.y + vec4(0.0, i1.y, i2.y, 1.0))\n" +
                "           + i.x + vec4(0.0, i1.x, i2.x, 1.0));\n" +
                "\n" +
                "    float n_ = 0.142857142857;\n" +
                "    vec3 ns = n_ * D.wyz - D.xzx;\n" +
                "\n" +
                "    vec4 j = p - 49.0 * floor(p * ns.z * ns.z);\n" +
                "\n" +
                "    vec4 x_ = floor(j * ns.z);\n" +
                "    vec4 y_ = floor(j - 7.0 * x_);\n" +
                "\n" +
                "    vec4 x = x_ * ns.x + ns.yyyy;\n" +
                "    vec4 y = y_ * ns.x + ns.yyyy;\n" +
                "    vec4 h = 1.0 - abs(x) - abs(y);\n" +
                "\n" +
                "    vec4 b0 = vec4(x.xy, y.xy);\n" +
                "    vec4 b1 = vec4(x.zw, y.zw);\n" +
                "\n" +
                "    vec4 s0 = floor(b0) * 2.0 + 1.0;\n" +
                "    vec4 s1 = floor(b1) * 2.0 + 1.0;\n" +
                "    vec4 sh = -step(h, vec4(0.0));\n" +
                "\n" +
                "    vec4 a0 = b0.xzyw + s0.xzyw * sh.xxyy;\n" +
                "    vec4 a1 = b1.xzyw + s1.xzyw * sh.zzww;\n" +
                "\n" +
                "    vec3 p0 = vec3(a0.xy, h.x);\n" +
                "    vec3 p1 = vec3(a0.zw, h.y);\n" +
                "    vec3 p2 = vec3(a1.xy, h.z);\n" +
                "    vec3 p3 = vec3(a1.zw, h.w);\n" +
                "\n" +
                "    vec4 norm = 1.0 / vec4(dot(p0, p0), dot(p1, p1), dot(p2, p2), dot(p3, p3));\n" +
                "    p0 *= norm.x;\n" +
                "    p1 *= norm.y;\n" +
                "    p2 *= norm.z;\n" +
                "    p3 *= norm.w;\n" +
                "\n" +
                "    vec4 m = max(0.6 - vec4(dot(x0, x0), dot(x1, x1), dot(x2, x2), dot(x3, x3)), 0.0);\n" +
                "    m = m * m;\n" +
                "    return 42.0 * dot(m * m, vec4(dot(p0, x0), dot(p1, x1), dot(p2, x2), dot(p3, x3)));\n" +
                "}\n" +
                "\n" +
                "float turbulence(vec3 p) {\n" +
                "    float value = 0.0;\n" +
                "    float amplitude = 1.0;\n" +
                "    float frequency = 1.0;\n" +
                "\n" +
                "    for (int i = 0; i < 6; i++) {\n" +
                "        value += amplitude * abs(snoise(p * frequency));\n" +
                "        amplitude *= 0.5;\n" +
                "        frequency *= 2.0;\n" +
                "    }\n" +
                "\n" +
                "    return value * 0.5;\n" +
                "}\n" +
                "\n" +
                "float bezier4(vec2 p0, vec2 p1, vec2 p2, vec2 p3, vec2 p4, float t) {\n" +
                "    float u = 1.0 - t;\n" +
                "    float tt = t * t;\n" +
                "    float uu = u * u;\n" +
                "    float uuu = uu * u;\n" +
                "    float uuuu = uuu * u;\n" +
                "    float ttt = tt * t;\n" +
                "    float tttt = ttt * t;\n" +
                "    \n" +
                "    vec2 point = uuuu * p0 + 4.0 * uuu * t * p1 + 6.0 * uu * tt * p2 + 4.0 * u * ttt * p3 + tttt * p4;\n" +
                "    return point.y;\n" +
                "}\n" +
                "\n" +
                "void main() {\n" +
                "    vec2 uv = v_uv;\n" +
                "    \n" +
                "    float canvasWidth = 800.0;\n" +
                "    float canvasHeight = 200.0;\n" +
                "    vec2 normalizedUV = vec2(uv.x*2.0, 1.0 - uv.y);\n" +
                "    \n" +
                "    float p1Amplitude = 0.05;\n" +
                "    float p1Frequency = 1.5;\n" +
                "    float p1Offset = sin(p1Frequency*u_time) * p1Amplitude;\n" +
                "    \n" +
                "    float p2Amplitude = 0.08;\n" +
                "    float p2Frequency = 2.0;\n" +
                "    float p2Offset = sin( p2Frequency*u_time + 1.0) * p2Amplitude;\n" +
                "    \n" +
                "    float p3Amplitude = 0.12;\n" +
                "    float p3Frequency = 1.8;\n" +
                "    float p3Offset = sin( p3Frequency*u_time + 2.0) * p3Amplitude;\n" +
                "    \n" +
                "    float p4Amplitude = 0.3;\n" +
                "    float p4Frequency = 1.2;\n" +
                "    float p4Offset = sin( p4Frequency*u_time + 3.0) * p4Amplitude;\n" +
                "    \n" +
                "    vec2 p0 = vec2(0.0, 0.5);\n" +
                "    vec2 p1 = vec2(0.25, 0.5 + p1Offset);\n" +
                "    vec2 p2 = vec2(0.5, 0.5 + p2Offset);\n" +
                "    vec2 p3 = vec2(0.75, 0.5 + p3Offset);\n" +
                "    vec2 p4 = vec2(1.0, 0.5 + p4Offset);\n" +
                "    \n" +
                "    float bezierY = bezier4(p0, p1, p2, p3, p4, normalizedUV.x);\n" +
                "    \n" +
                "    float baseWidth = 0.02 + normalizedUV.x * 0.03;\n" +
                "    float maxDistance = baseWidth * pow(normalizedUV.x, 0.4);\n" +
                "    \n" +
                "    float expandedMaxDistance = maxDistance * 1.5;\n" +
                "    \n" +
                "    float distanceFromCurve = abs(normalizedUV.y - bezierY);\n" +
                "    \n" +
                "    if (distanceFromCurve > expandedMaxDistance) {\n" +
                "        discard;\n" +
                "    }\n" +
                "    \n" +
                "    float edgeFade = 1.0 - smoothstep(maxDistance, expandedMaxDistance, distanceFromCurve);\n" +
                "    \n" +
                "    float startFade = smoothstep(0.0, 0.05, normalizedUV.x);\n" +
                "    float endFade = smoothstep(1.0, 0.85, normalizedUV.x);\n" +
                "    float longitudinalFade = startFade * endFade;\n" +
                "    \n" +
                "    float normalizedPos = (normalizedUV.y - (bezierY - expandedMaxDistance)) / (2.0 * expandedMaxDistance);\n" +
                "    normalizedPos = clamp(normalizedPos, 0.0, 1.0);\n" +
                "    \n" +
                "    float cosValue = cos(normalizedPos * 3.14159);\n" +
                "    \n" +
                "    vec3 noiseCoord = vec3(cosValue*0.5, 1.0*(pow(normalizedUV.x,1.2) - u_time * 0.5), u_time * 0.1);\n" +
                "    float noiseValue = turbulence(noiseCoord);\n" +
                "    \n" +
                "    vec3 color = vec3(0.8, 0.4, 1.0);\n" +
                "    float adjustedNoise = noiseValue * 1.8 - 0.6;\n" +
                "    \n" +
                "    float alpha = max(0.2,(1.0 * adjustedNoise)) * edgeFade * endFade;\n" +
                "    \n" +
                "    gl_FragColor = vec4(color*alpha,alpha);\n" +
                "}\n";

            
            int vert = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
            GL20.glShaderSource(vert, vertexSource);
            GL20.glCompileShader(vert);
            if (GL20.glGetShaderi(vert, GL20.GL_COMPILE_STATUS) == 0) {
                System.err.println("BezierCurve vert compile failed: " + GL20.glGetShaderInfoLog(vert, 1024));
                return;
            }

            int frag = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
            GL20.glShaderSource(frag, fragmentSource);
            GL20.glCompileShader(frag);
            if (GL20.glGetShaderi(frag, GL20.GL_COMPILE_STATUS) == 0) {
                System.err.println("BezierCurve frag compile failed: " + GL20.glGetShaderInfoLog(frag, 1024));
                return;
            }

            shaderProgram = GL20.glCreateProgram();
            GL20.glAttachShader(shaderProgram, vert);
            GL20.glAttachShader(shaderProgram, frag);
            GL20.glBindAttribLocation(shaderProgram, 0, "a_position");
            GL20.glBindAttribLocation(shaderProgram, 1, "a_texCoord");
            GL20.glLinkProgram(shaderProgram);

            if (GL20.glGetProgrami(shaderProgram, GL20.GL_LINK_STATUS) == 0) {
                System.err.println("BezierCurve link failed: " + GL20.glGetProgramInfoLog(shaderProgram, 1024));
                return;
            }

            GL20.glDeleteShader(vert);
            GL20.glDeleteShader(frag);

            System.out.println("BezierCurveEffect shader compiled OK");
        } catch (Exception e) {
            System.err.println("BezierCurveEffect shader error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createBuffers() {
        float leftHalfWidth = width * 0.3f;
        float rightHalfWidth = width * 0.7f;
        float halfHeight = height / 2f;

        FloatBuffer verts = org.lwjgl.BufferUtils.createFloatBuffer(16);
        verts.put(new float[]{
            -leftHalfWidth, -halfHeight,  0f, 0f,
             rightHalfWidth, -halfHeight,  1f, 0f,
             rightHalfWidth,  halfHeight,  1f, 1f,
            -leftHalfWidth,  halfHeight,  0f, 1f,
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
    }

    @Override
    public void advance(float amount) {
        elapsed += amount;
        
        if (elapsed >= duration) {
            expired = true;
            return;
        }
    }

    private void ensureFBO(int w, int h) {
        int tw = nextPowerOfTwo(w);
        int th = nextPowerOfTwo(h);
        if (fboId != 0 && fboW == tw && fboH == th) return;
        destroyFBO();
        fboW = tw;
        fboH = th;

        fboTexId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, fboTexId);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, fboW, fboH,
                0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (java.nio.ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);

        fboId = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fboId);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                GL11.GL_TEXTURE_2D, fboTexId, 0);
        int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
        if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
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

    private int renderToFBO() {
        ensureFBO((int) width, (int) height);
        if (fboId == 0) return 0;

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL20.glUseProgram(0);

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fboId);
        GL11.glViewport(0, 0, fboW, fboH);

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(-width/2, width/2, -height/2, height/2, -1, 1);
        GL11.glMatrixMode(GL11.GL_TEXTURE);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glColorMask(true, true, true, true);
        GL11.glClearColor(0f, 0f, 0f, 0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        float progress = elapsed / duration;
        progress = Math.max(0.0f, Math.min(1.0f, progress));

        GL20.glUseProgram(shaderProgram);
        GL20.glUniform1f(uTimeLoc, elapsed);
        GL20.glUniform1f(uDurationLoc, duration);
        GL20.glUniform1f(uProgressLoc, progress);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);

        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 16, 0);
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 16, 8);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);

        GL11.glDrawElements(GL11.GL_TRIANGLES, 6, GL11.GL_UNSIGNED_INT, 0);

        GL20.glDisableVertexAttribArray(0);
        GL20.glDisableVertexAttribArray(1);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
        GL20.glUseProgram(0);

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_TEXTURE);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        GL11.glPopAttrib();

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, fboTexId);
        return fboTexId;
    }

    @Override
    public void render() {
        if (expired || shaderProgram <= 0) return;

        float halfWidth = width * 0.5f;
        float halfHeight = height * 0.5f;

        int texId = renderToFBO();
        if (texId == 0) return;

        java.nio.FloatBuffer before = org.lwjgl.BufferUtils.createFloatBuffer(16);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, before);

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glMatrixMode(GL11.GL_TEXTURE);
        GL11.glPushMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();

        GL11.glTranslatef(position.x, position.y, 0f);

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texId);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glColor4f(1f, 1f, 1f, 1f);

        float uvScaleX = width / (float) fboW;
        float uvScaleY = height / (float) fboH;

        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0f, 0f);
        GL11.glVertex2f(-halfWidth, -halfHeight);
        GL11.glTexCoord2f(uvScaleX, 0f);
        GL11.glVertex2f(halfWidth, -halfHeight);
        GL11.glTexCoord2f(uvScaleX, uvScaleY);
        GL11.glVertex2f(halfWidth, halfHeight);
        GL11.glTexCoord2f(0f, uvScaleY);
        GL11.glVertex2f(-halfWidth, halfHeight);
        GL11.glEnd();

        GL11.glDepthMask(true);
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
        destroyFBO();
    }

    public void setSize(float width, float height) {
        this.width = width;
        this.height = height;
    }

    public void setDuration(float dur) {
        this.duration = dur;
    }
}
