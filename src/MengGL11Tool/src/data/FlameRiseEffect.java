package data;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.vector.Vector2f;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * 火焰升腾特效渲染类
 * 
 * 功能说明：
 * - 在圆环区域内渲染动态火焰效果
 * - 使用湍流噪声实现火焰溶解和升腾
 * - 内环到外环之间形成平滑的火焰过渡
 * - 内部透明区域直接丢弃不渲染
 * 
 * 技术实现：
 * - GLSL着色器实现像素级溶解和颜色渐变
 * - 圆环裁剪：基于距离判断内外半径
 * - 通过UV滚动实现动画效果
 * 
 * 调用方式：
 * - 继承 MyRenderTool.MyRender 和 InitializableEffect 接口
 * - 通过 initialize() 设置位置，advance() 更新时间，render() 渲染
 */
public class FlameRiseEffect implements MyRenderTool.MyRender, MyRenderTool.InitializableEffect {

    private Vector2f position;        // 特效位置
    private float elapsed;            // 已运行时间
    private float duration;           // 持续时间
    private boolean expired;          // 是否过期
    
    private float canvasWidth;        // 画布宽度
    private float canvasHeight;       // 画布高度
    
    private int shaderProgram;        // 着色器程序ID
    private int vbo;                  // 顶点缓冲对象
    private int ibo;                  // 索引缓冲对象
    private int textureSize;          // 纹理尺寸（正方形）
    
    private int uTimeLoc;             // 时间uniform位置

    /**
     * 构造函数
     * 初始化火焰特效，加载纹理、创建着色器和缓冲区
     */
    public FlameRiseEffect() {
        this.position = new Vector2f(0f, 0f);
        this.elapsed = 0f;
        this.duration = 1000.0f;
        this.expired = false;
        this.canvasWidth = 2000f;
        this.canvasHeight = 2000f;
        this.textureSize = 256;

        createShaderProgram();     // 创建着色器程序
        createBuffers();           // 创建顶点缓冲区
        cacheUniformLocations();   // 缓存uniform位置
    }





    /**
     * 创建着色器程序
     * 包含顶点和片段着色器，实现火焰溶解和升腾效果
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
                "\n" +
                "// Simplex 3D Noise function\n" +
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
                "// Turbulence function using Simplex noise\n" +
                "float turbulence(vec3 p) {\n" +
                "    float value = 0.0;\n" +
                "    float amplitude = 1.0;\n" +
                "    float frequency = 1.0;\n" +
                "    \n" +
                "    for (int i = 0; i < 5; i++) {\n" +
                "        value += amplitude * abs(snoise(p * frequency));\n" +
                "        amplitude *= 0.5;\n" +
                "        frequency *= 2.0;\n" +
                "    }\n" +
                "    \n" +
                "    return value * 0.5;\n" +
                "}\n" +
                "\n" +
                "void main() {\n" +
                "    vec2 uv = vec2(v_uv.x,1.0-v_uv.y);\n" +
                "    \n" +
                "    vec2 center = vec2(0.5, 0.5);\n" +
                "    float dist = distance(uv, center);\n" +
                "    float maxDist = 0.7071;\n" +
                "    float normalizedDist = dist / maxDist;\n" +
                "    \n" +
                "    float innerRadius = 0.685;\n" +
                "    float fadeinRadius = 0.68;\n" +
                "    float outerRadius = 0.7;\n" +
                "    \n" +
                "    if (normalizedDist < fadeinRadius) discard;\n" +
                "    if (normalizedDist > outerRadius) discard;\n" +
                "    \n" +
                "    float fadevalue = smoothstep(fadeinRadius,innerRadius,normalizedDist);\n" +
                "    float ringProgress = max(0.0,(normalizedDist - innerRadius)) / (outerRadius - innerRadius);\n" +
                "    \n" +
                "    float dx = uv.x - 0.5;\n" +
                "    float dy = v_uv.y - 0.5;\n" +
                "    float angle= abs(atan(dx / dy));\n" +
                "    \n" +
                "    float scrollSpeed = 0.3;\n" +
                "    vec3 noiseCoord = vec3(angle * 40.0 , dist*42.0 - u_time * scrollSpeed, 0.0);\n" +
                "    \n" +
                "    float noiseValue = pow(turbulence(noiseCoord),1.0);\n" +
                "    \n" +
                "    float gradientValue = 1.0*(1.0-ringProgress);\n" +
                "    float totalalpha = fadevalue*(1.0-ringProgress);\n" +
                "    \n" +
                "    float dissolveThreshold = gradientValue * 0.7;\n" +
                "    \n" +
                "    float dissolveWidth = 0.3; " +
                "    float dissolved = smoothstep(dissolveThreshold - dissolveWidth, \n" +
                "                                 dissolveThreshold + dissolveWidth, \n" +
                "                                 noiseValue);\n" +
                "    \n" +
                "    float coreProtection = smoothstep(innerRadius , innerRadius + 0.01, normalizedDist);\n" +
                "    float protectedDissolved =1.0 - dissolved * coreProtection;\n" +
                "    \n" +
                "    vec3 flameBaseColor = vec3(0.9, 0.75, 1.0);\n" +
                "    vec3 outerFlameColor = vec3(0.7, 0.4, 0.95);\n" +
                "    \n" +
                "    vec3 finalColor = mix(outerFlameColor, flameBaseColor, pow(protectedDissolved,8.0));\n" +
                "    \n" +
                "    float innerDist = abs(normalizedDist - innerRadius);\n" +
                "    float glowIntensity = totalalpha;\n" +
                "    vec3 glowColor = vec3(0.92, 0.82, 1.0);\n" +
                "    \n" +
                "    float alpha = totalalpha * protectedDissolved + glowIntensity * 0.3;\n" +
                "    \n" +
                "    gl_FragColor = vec4(finalColor, alpha*0.4);\n" +
                "}\n";

            int vert = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
            GL20.glShaderSource(vert, vertexSource);
            GL20.glCompileShader(vert);
            if (GL20.glGetShaderi(vert, GL20.GL_COMPILE_STATUS) == 0) {
                System.err.println("Vertex shader compile failed: " + GL20.glGetShaderInfoLog(vert, 1024));
                return;
            }

            int frag = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
            GL20.glShaderSource(frag, fragmentSource);
            GL20.glCompileShader(frag);
            if (GL20.glGetShaderi(frag, GL20.GL_COMPILE_STATUS) == 0) {
                System.err.println("Fragment shader compile failed: " + GL20.glGetShaderInfoLog(frag, 1024));
                return;
            }

            shaderProgram = GL20.glCreateProgram();
            GL20.glAttachShader(shaderProgram, vert);
            GL20.glAttachShader(shaderProgram, frag);
            GL20.glBindAttribLocation(shaderProgram, 0, "a_position");
            GL20.glBindAttribLocation(shaderProgram, 1, "a_texCoord");
            GL20.glLinkProgram(shaderProgram);

            if (GL20.glGetProgrami(shaderProgram, GL20.GL_LINK_STATUS) == 0) {
                System.err.println("Shader program link failed: " + GL20.glGetProgramInfoLog(shaderProgram, 1024));
                return;
            }

            GL20.glDeleteShader(vert);
            GL20.glDeleteShader(frag);

            System.out.println("FlameRiseEffect shader compiled OK");
        } catch (Exception e) {
            System.err.println("Shader error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 创建顶点缓冲区和索引缓冲区
     * 定义一个正方形quad，覆盖整个画布区域
     */
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

    /**
     * 缓存着色器uniform变量位置
     * 避免每次渲染时重复查询，提升性能
     */
    private void cacheUniformLocations() {
        if (shaderProgram <= 0) return;
        uTimeLoc = GL20.glGetUniformLocation(shaderProgram, "u_time");
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

    /**
     * 清理OpenGL资源
     * 删除着色器程序、缓冲区和纹理
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
     * 设置特效持续时间
     * @param dur 持续时间（秒）
     */
    public void setDuration(float dur) {
        this.duration = dur;
    }

    /**
     * 设置画布尺寸
     * @param width 宽度
     * @param height 高度
     */
    public void setCanvasSize(float width, float height) {
        this.canvasWidth = width;
        this.canvasHeight = height;
        // 重新创建缓冲区以适配新尺寸
        if (vbo != 0) {
            GL15.glDeleteBuffers(vbo);
            vbo = 0;
        }
        if (ibo != 0) {
            GL15.glDeleteBuffers(ibo);
            ibo = 0;
        }
        createBuffers();
    }
}
