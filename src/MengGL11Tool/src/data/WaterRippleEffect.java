package data;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.util.vector.Vector2f;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * 水波纹理渲染效果
 * 
 * 实现功能：
 * - 水蓝色底色背景
 * - 基于分形布朗运动(FBM)的沃利噪声(Worley Noise)生成水波纹理
 * - RGB通道分离模拟光的折射效果
 * - 支持动态时间演化
 * 
 * 技术要点：
 * - 使用FBO进行离屏渲染
 * - GLSL着色器内联实现所有视觉效果
 * - 多层噪声叠加产生自然的水波形态
 */
public class WaterRippleEffect implements MyRenderTool.MyRender, MyRenderTool.InitializableEffect {

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

    public WaterRippleEffect() {
        this.position = new Vector2f(0f, 0f);
        this.elapsed = 0f;
        this.duration = 100.0f;
        this.expired = false;
        this.width = 800f;
        this.height = 600f;

        createShaderProgram();
        createBuffers();
        cacheUniformLocations();
    }

    /**
     * 创建GLSL着色器程序
     * 
     * 顶点着色器：简单的四边形变换
     * 片段着色器：实现水波纹理和RGB折射效果
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
                "vec2 hash2(vec2 p) {\n" +
                "    vec3 p3 = fract(vec3(p.xyx) * vec3(0.1031, 0.1030, 0.0973));\n" +
                "    p3 += dot(p3, p3.yzx + 33.33);\n" +
                "    return fract((p3.xx + p3.yz) * p3.zy);\n" +
                "}\n" +
                "\n" +
                "float worleyNoise(vec2 st) {\n" +
                "    vec2 i_st = floor(st);\n" +
                "    vec2 f_st = fract(st);\n" +
                "    float m_dist = 1.0;\n" +
                "    for (int y = -1; y <= 1; y++) {\n" +
                "        for (int x = -1; x <= 1; x++) {\n" +
                "            vec2 neighbor = vec2(float(x), float(y));\n" +
                "            vec2 point = hash2(i_st + neighbor);\n" +
                "            vec2 diff = neighbor + point - f_st;\n" +
                "            float dist = length(diff);\n" +
                "            m_dist = min(m_dist, dist);\n" +
                "        }\n" +
                "    }\n" +
                "    return m_dist;\n" +
                "}\n" +
                "\n" +
                "float fbmWorley(vec2 p) {\n" +
                "    float value = 0.0;\n" +
                "    float amplitude = 0.5;\n" +
                "    float frequency = 6.0;\n" +
                "    for (int i = 0; i < 5; i++) {\n" +
                "        value += amplitude * worleyNoise(p * frequency);\n" +
                "        frequency *= 2.0;\n" +
                "        amplitude *= 0.5;\n" +
                "    }\n" +
                "    return value;\n" +
                "}\n" +
                "\n" +
                "void main(void) {\n" +
                "    vec2 uv = v_uv;\n" +
                "    vec2 p = vec2(uv.x, 1.0 - uv.y);\n" +
                "    float time = u_time * 0.3;\n" +
                "    \n" +
                "    float rOffset = sin(time * 0.7) * 0.01;\n" +
                "    float gOffset = cos(time * 0.5) * 0.008;\n" +
                "    float bOffset = sin(time * 0.9 + 1.0) * 0.012;\n" +
                "    \n" +
                "    float noiseR = fbmWorley(p * 3.0 + vec2(rOffset, time * 0.1));\n" +
                "    float noiseG = fbmWorley(p * 3.0 + vec2(gOffset, time * 0.12));\n" +
                "    float noiseB = fbmWorley(p * 3.0 + vec2(bOffset, time * 0.08));\n" +
                "    \n" +
                "    float detailNoise = fbmWorley(p * 6.0 + vec2(time * 0.2, -time * 0.15)) * 0.2;\n" +
                "    noiseR += detailNoise;\n" +
                "    noiseG += detailNoise;\n" +
                "    noiseB += detailNoise;\n" +
                "    \n" +
                "    noiseR = smoothstep(0.25, 0.75, noiseR);\n" +
                "    noiseG = smoothstep(0.25, 0.75, noiseG);\n" +
                "    noiseB = smoothstep(0.25, 0.75, noiseB);\n" +
                "    \n" +
                "    vec3 waterBaseColor = vec3(0.2, 0.6, 0.8);\n" +
                "    vec3 color = waterBaseColor * (0.7 + 0.3 * vec3(noiseR, noiseG, noiseB));\n" +
                "    \n" +
                "    float highlight = pow(max(0.0, sin(noiseR * 6.28318) * sin(noiseG * 6.28318)), 3.0);\n" +
                "    color += vec3(0.3, 0.5, 0.6) * highlight * 0.4;\n" +
                "    \n" +
                "    float edgeFadeX = smoothstep(0.0, 0.1, uv.x) * smoothstep(1.0, 0.9, uv.x);\n" +
                "    float edgeFadeY = smoothstep(0.0, 0.1, uv.y) * smoothstep(1.0, 0.9, uv.y);\n" +
                "    float edgeFade = edgeFadeX * edgeFadeY;\n" +
                "    \n" +
                "    float alpha = 0.85 * edgeFade;\n" +
                "    gl_FragColor = vec4(color, alpha);\n" +
                "}\n";

            
            int vert = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
            GL20.glShaderSource(vert, vertexSource);
            GL20.glCompileShader(vert);
            if (GL20.glGetShaderi(vert, GL20.GL_COMPILE_STATUS) == 0) {
                System.err.println("WaterRipple vert compile failed: " + GL20.glGetShaderInfoLog(vert, 1024));
                return;
            }

            int frag = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
            GL20.glShaderSource(frag, fragmentSource);
            GL20.glCompileShader(frag);
            if (GL20.glGetShaderi(frag, GL20.GL_COMPILE_STATUS) == 0) {
                System.err.println("WaterRipple frag compile failed: " + GL20.glGetShaderInfoLog(frag, 1024));
                return;
            }

            shaderProgram = GL20.glCreateProgram();
            GL20.glAttachShader(shaderProgram, vert);
            GL20.glAttachShader(shaderProgram, frag);
            GL20.glBindAttribLocation(shaderProgram, 0, "a_position");
            GL20.glBindAttribLocation(shaderProgram, 1, "a_texCoord");
            GL20.glLinkProgram(shaderProgram);

            if (GL20.glGetProgrami(shaderProgram, GL20.GL_LINK_STATUS) == 0) {
                System.err.println("WaterRipple link failed: " + GL20.glGetProgramInfoLog(shaderProgram, 1024));
                return;
            }

            GL20.glDeleteShader(vert);
            GL20.glDeleteShader(frag);

            System.out.println("WaterRippleEffect shader compiled OK");
        } catch (Exception e) {
            System.err.println("WaterRippleEffect shader error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 创建顶点缓冲和索引缓冲
     * 定义一个覆盖整个渲染区域的四边形
     */
    private void createBuffers() {
        float halfWidth = width / 2f;
        float halfHeight = height / 2f;

        FloatBuffer verts = org.lwjgl.BufferUtils.createFloatBuffer(16);
        verts.put(new float[]{
            -halfWidth, -halfHeight,  0f, 0f,
             halfWidth, -halfHeight,  1f, 0f,
             halfWidth,  halfHeight,  1f, 1f,
            -halfWidth,  halfHeight,  0f, 1f,
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

    /**
     * 缓存着色器uniform变量位置
     */
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

    /**
     * 确保FBO存在且尺寸正确
     * 使用2的幂次方纹理以获得最佳兼容性
     */
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

    /**
     * 销毁FBO和相关纹理
     */
    private void destroyFBO() {
        if (fboId != 0) { GL30.glDeleteFramebuffers(fboId); fboId = 0; }
        if (fboTexId != 0) { GL11.glDeleteTextures(fboTexId); fboTexId = 0; }
    }

    /**
     * 计算大于等于value的最小2的幂次方
     */
    private int nextPowerOfTwo(int value) {
        if (value <= 0) return 1;
        value--;
        value |= value >> 1; value |= value >> 2;
        value |= value >> 4; value |= value >> 8;
        value |= value >> 16;
        return value + 1;
    }

    /**
     * 渲染到FBO
     * 返回FBO纹理ID供后续使用
     */
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
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
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

    /**
     * 清理OpenGL资源
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
        destroyFBO();
    }

    /**
     * 设置渲染区域大小
     */
    public void setSize(float width, float height) {
        this.width = width;
        this.height = height;
    }

    /**
     * 设置效果持续时间
     */
    public void setDuration(float dur) {
        this.duration = dur;
    }
}
