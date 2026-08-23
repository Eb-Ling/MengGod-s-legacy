package data.methods.shaders;

import com.fs.starfarer.api.Global;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

import java.awt.*;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * ShaderUtil - GPU着色器渲染工具类，封装Starsector模组开发中常用的OpenGL操作。
 *
 * <h3>功能概述</h3>
 * 本类从项目中多个渲染插件（Phasecore、HexShield、MingGodCenter、PCTianhongVisualRenderer）
 * 中提取重复的shader编译、FBO管理、VBO/IBO创建、矩阵操作、uniform设置、渲染管线等代码，
 * 统一封装为静态工具方法，供所有渲染插件复用。
 *
 * <h3>依赖的API</h3>
 * <ul>
 *   <li>LWJGL {@link org.lwjgl.opengl.GL11} - 基础OpenGL操作（混合、纹理、矩阵、视口）</li>
 *   <li>LWJGL {@link org.lwjgl.opengl.GL15} - VBO/IBO缓冲区对象管理</li>
 *   <li>LWJGL {@link org.lwjgl.opengl.GL20} - GLSL着色器编译/链接/uniform设置</li>
 *   <li>LWJGL {@link org.lwjgl.opengl.GL30} - FBO帧缓冲对象（核心API）</li>
 *   <li>LWJGL {@link org.lwjgl.opengl.ARBFramebufferObject} - FBO帧缓冲对象（ARB扩展）</li>
 *   <li>LWJGL {@link org.lwjgl.opengl.EXTFramebufferObject} - FBO帧缓冲对象（EXT扩展）</li>
 *   <li>Starsector {@link Global#getLogger} - 日志系统</li>
 * </ul>
 *
 * <h3>成员变量分组</h3>
 * <ul>
 *   <li>日志: {@link #log} - 全局Logger实例</li>
 * </ul>
 *
 * <h3>方法分类</h3>
 * <ul>
 *   <li>Shader编译与链接: {@link #compileShader}, {@link #createShaderProgram}</li>
 *   <li>Uniform位置缓存: {@link #getUniformLocations}</li>
 *   <li>Uniform值设置: {@link #setUniform1f}, {@link #setUniform1i}, {@link #setUniform2f},
 *       {@link #setUniform3f}, {@link #setUniform4f}, {@link #setUniformColor}</li>
 *   <li>VBO/IBO创建: {@link #createQuadVBO}, {@link #createQuadIBO}</li>
 *   <li>FBO管理: {@link #createFBO}, {@link #destroyFBO}, {@link #bindFBO},
 *       {@link #unbindFBO}, {@link #clearFBO}, {@link #nextPowerOfTwo}</li>
 *   <li>矩阵操作: {@link #pushAllMatrices}, {@link #popAllMatrices},
 *       {@link #setupFBOMatrices}, {@link #setupWorldTransform}</li>
 *   <li>渲染管线: {@link #renderQuadAdditive}, {@link #renderQuadAlpha},
 *       {@link #bindAndDrawQuad}(private)</li>
 *   <li>GPU资源清理: {@link #deleteProgram}, {@link #deleteBuffers}, {@link #cleanupAll}</li>
 * </ul>
 *
 * <h3>典型调用流程</h3>
 * <pre>
 *   // 初始化阶段（构造函数或init中调用）
 *   int program = ShaderUtil.createShaderProgram(vertSrc, fragSrc, "MyEffect");
 *   int vbo = ShaderUtil.createQuadVBO(200f, 40f);
 *   int ibo = ShaderUtil.createQuadIBO();
 *   int[] locs = ShaderUtil.getUniformLocations(program, "u_time", "u_progress");
 *
 *   // 每帧渲染阶段（render方法中调用）
 *   GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
 *   ShaderUtil.pushAllMatrices();
 *   ShaderUtil.setupWorldTransform(shipX, shipY, rotationDeg);
 *   GL20.glUseProgram(program);
 *   ShaderUtil.setUniform1f(locs[0], elapsed);
 *   ShaderUtil.setUniform1f(locs[1], progress);
 *   ShaderUtil.renderQuadAdditive(program, vbo, ibo);
 *   ShaderUtil.popAllMatrices();
 *   GL11.glPopAttrib();
 *
 *   // 销毁阶段（cleanup方法中调用）
 *   ShaderUtil.cleanupAll(program, vbo, ibo);
 * </pre>
 *
 * <h3>FBO渲染流程</h3>
 * <pre>
 *   // 初始化
 *   ShaderUtil.FBOData fbo = ShaderUtil.createFBO(512, 512, "MyEffect");
 *
 *   // 渲染到FBO
 *   ShaderUtil.bindFBO(fbo.fboId);
 *   ShaderUtil.clearFBO(fbo.width, fbo.height);  // 重置colorMask + 清空
 *   ShaderUtil.setupFBOMatrices(fbo.width, fbo.height);
 *   GL20.glUseProgram(program);
 *   // ... 设置uniform ...
 *   ShaderUtil.renderQuadAlpha(program, vbo, ibo);
 *   ShaderUtil.unbindFBO();
 *
 *   // 将FBO纹理绘制到屏幕
 *   GL11.glBindTexture(GL11.GL_TEXTURE_2D, fbo.fboTexId);
 *   // ... glBegin/glEnd绘制 ...
 *
 *   // 销毁
 *   ShaderUtil.destroyFBO(fbo.fboId, fbo.fboTexId);
 * </pre>
 *
 * <h3>日志规范</h3>
 * 所有错误/信息日志统一通过 {@code Global.getLogger(ShaderUtil.class)} 输出，
 * 禁止使用 System.out 或 System.err。
 *
 * <h3>提取来源</h3>
 * 本类方法提取自以下文件中的重复代码模式：
 * <ul>
 *   <li>{@code src/hullmods/Meng_fire_Phasecore.java} (PhaseCoreRangePlugin内部类)</li>
 *   <li>{@code src/hullmods/Meng_HexShieldPlugin.java} (含FBO完整流程)</li>
 *   <li>{@code src/hullmods/Meng_MingGodCenter.java} (Meng_MingGodSignPlugin内部类)</li>
 *   <li>{@code src/example/PCTianhongVisualRenderer.java} (含uniform辅助方法和shader文件加载)</li>
 * </ul>
 *
 * @see org.lwjgl.opengl.GL20
 * @see org.lwjgl.opengl.GL15
 * @see org.lwjgl.opengl.GL30
 */
public class ShaderUtil {

    // ==================== 日志 ====================

    /**
     * 全局Logger实例，用于ShaderUtil所有操作的日志输出。
     * 通过 {@link Global#getLogger(Class)} 获取，遵循Starsector日志规范。
     * 输出级别：error用于编译/链接/创建失败，info用于成功信息，debug用于调试。
     */
    private static final org.apache.log4j.Logger log = Global.getLogger(ShaderUtil.class);

    // ==================== Shader 编译与链接 ====================

    /**
     * 编译单个GLSL着色器（顶点或片段着色器）。
     *
     * <h4>实现功能</h4>
     * 调用GL20 API创建shader对象、上传源码、触发编译，并检查编译状态。
     * 编译失败时通过Logger输出详细错误日志（含着色器类型和logTag标识），
     * 自动删除失败的shader对象，返回0表示失败。
     *
     * <h4>依赖API</h4>
     * <ul>
     *   <li>{@link GL20#glCreateShader(int)} - 创建shader对象</li>
     *   <li>{@link GL20#glShaderSource(int, CharSequence)} - 上传GLSL源码</li>
     *   <li>{@link GL20#glCompileShader(int)} - 触发编译</li>
     *   <li>{@link GL20#glGetShaderi(int, int)} - 查询编译状态</li>
     *   <li>{@link GL20#glGetShaderInfoLog(int, int)} - 获取编译日志</li>
     *   <li>{@link GL20#glDeleteShader(int)} - 失败时清理</li>
     * </ul>
     *
     * <h4>调用路径</h4>
     * 由 {@link #createShaderProgram(String, String, String)} 内部调用，
     * 分别编译vertex和fragment shader。一般不直接对外调用。
     *
     * @param source  GLSL着色器源码字符串（需包含 #version 110 声明）
     * @param type    着色器类型：{@link GL20#GL_VERTEX_SHADER} 或 {@link GL20#GL_FRAGMENT_SHADER}
     * @param logTag  日志标识符，用于区分调用来源（如 "PhaseCore"、"HexShield"、"MingGodSign"）
     * @return 编译成功的shader handle（正整数），失败返回 0
     */
    public static int compileShader(String source, int type, String logTag) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == 0) {
            String typeName = (type == GL20.GL_VERTEX_SHADER) ? "Vertex" : "Fragment";
            log.error("[" + logTag + "] " + typeName + " shader compile failed: "
                    + GL20.glGetShaderInfoLog(shader, 1024));
            GL20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    /**
     * 编译顶点+片段着色器并链接为完整的shader program。
     *
     * <h4>实现功能</h4>
     * <ol>
     *   <li>调用 {@link #compileShader} 分别编译vertex和fragment shader</li>
     *   <li>创建program对象，attach两个shader</li>
     *   <li>绑定attribute位置：a_position=0, a_texCoord=1（与VBO顶点布局对应）</li>
     *   <li>链接program并检查链接状态</li>
     *   <li>无论成功失败，都删除中间的shader对象（已链接到program中不再需要）</li>
     *   <li>失败时删除program对象并返回0</li>
     * </ol>
     *
     * <h4>依赖API</h4>
     * <ul>
     *   <li>{@link #compileShader(String, int, String)} - 编译单个shader</li>
     *   <li>{@link GL20#glCreateProgram()} - 创建program对象</li>
     *   <li>{@link GL20#glAttachShader(int, int)} - 关联shader到program</li>
     *   <li>{@link GL20#glBindAttribLocation(int, int, CharSequence)} - 绑定属性位置</li>
     *   <li>{@link GL20#glLinkProgram(int)} - 链接program</li>
     *   <li>{@link GL20#glGetProgrami(int, int)} - 查询链接状态</li>
     * </ul>
     *
     * <h4>调用路径</h4>
     * 在渲染插件的构造函数或 {@code init()} 方法中调用，创建shader program handle。
     * 返回的handle需存储为成员变量，在每帧render()中通过 {@link GL20#glUseProgram(int)} 激活。
     *
     * <h4>attribute绑定约定</h4>
     * <ul>
     *   <li>location 0 = "a_position" (vec2) - 顶点位置坐标</li>
     *   <li>location 1 = "a_texCoord" (vec2) - 纹理UV坐标</li>
     * </ul>
     * 与 {@link #createQuadVBO(float, float)} 创建的VBO布局 [x, y, u, v] 对应。
     *
     * @param vertSource  顶点着色器GLSL源码（需包含 #version 110 声明）
     * @param fragSource  片段着色器GLSL源码（需包含 #version 110 声明）
     * @param logTag      日志标识符，用于区分调用来源
     * @return 链接成功的program handle（正整数），失败返回 0
     */
    public static int createShaderProgram(String vertSource, String fragSource, String logTag) {
        try {
            int vert = compileShader(vertSource, GL20.GL_VERTEX_SHADER, logTag);
            int frag = compileShader(fragSource, GL20.GL_FRAGMENT_SHADER, logTag);
            if (vert == 0 || frag == 0) {
                if (vert != 0) GL20.glDeleteShader(vert);
                if (frag != 0) GL20.glDeleteShader(frag);
                return 0;
            }

            int program = GL20.glCreateProgram();
            GL20.glAttachShader(program, vert);
            GL20.glAttachShader(program, frag);
            GL20.glBindAttribLocation(program, 0, "a_position");
            GL20.glBindAttribLocation(program, 1, "a_texCoord");
            GL20.glLinkProgram(program);

            if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == 0) {
                log.error("[" + logTag + "] Shader program link failed: "
                        + GL20.glGetProgramInfoLog(program, 1024));
                GL20.glDeleteProgram(program);
                GL20.glDeleteShader(vert);
                GL20.glDeleteShader(frag);
                return 0;
            }

            GL20.glDeleteShader(vert);
            GL20.glDeleteShader(frag);
            return program;
        } catch (Exception e) {
            log.error("[" + logTag + "] Shader creation error: " + e.getMessage(), e);
            return 0;
        }
    }

    // ==================== Uniform 位置缓存 ====================

    /**
     * 批量获取shader program中uniform变量的location。
     *
     * <h4>实现功能</h4>
     * 对program中每个指定的uniform名称调用 {@link GL20#glGetUniformLocation(int, CharSequence)}，
     * 返回与names数组一一对应的int[]位置数组。
     * 若某个uniform在shader中不存在或被优化掉，对应位置返回 -1。
     *
     * <h4>依赖API</h4>
     * <ul>
     *   <li>{@link GL20#glGetUniformLocation(int, CharSequence)} - 查询uniform位置</li>
     * </ul>
     *
     * <h4>调用路径</h4>
     * 在 {@link #createShaderProgram(String, String, String)} 成功返回后调用，
     * 通常在 {@code cacheUniformLocations()} 或初始化方法中。
     * 返回的location数组需存储为成员变量，在render()中传给setUniform*方法。
     *
     * <h4>使用示例</h4>
     * <pre>
     *   int[] locs = ShaderUtil.getUniformLocations(program, "u_time", "u_progress", "u_color");
     *   // locs[0] = u_time的位置
     *   // locs[1] = u_progress的位置
     *   // locs[2] = u_color的位置
     *   ShaderUtil.setUniform1f(locs[0], elapsed);
     * </pre>
     *
     * @param program  已链接的shader program handle
     * @param names    uniform变量名列表，按声明顺序传入
     * @return int[] 位置数组，与names一一对应；-1表示该uniform不存在
     */
    public static int[] getUniformLocations(int program, String... names) {
        int[] locations = new int[names.length];
        for (int i = 0; i < names.length; i++) {
            locations[i] = GL20.glGetUniformLocation(program, names[i]);
        }
        return locations;
    }

    // ==================== Uniform 值设置 ====================

    /**
     * 设置float类型uniform值。
     *
     * <h4>依赖API</h4>
     * {@link GL20#glUniform1f(int, float)}
     *
     * <h4>调用路径</h4>
     * 在render()方法中，{@link GL20#glUseProgram(int)} 之后、绘制之前调用。
     * 若location为-1（uniform不存在）则静默跳过，不产生GL错误。
     *
     * @param location  uniform位置（来自 {@link #getUniformLocations}）
     * @param value     float值
     */
    public static void setUniform1f(int location, float value) {
        if (location >= 0) GL20.glUniform1f(location, value);
    }

    /**
     * 设置int类型uniform值。
     *
     * <h4>依赖API</h4>
     * {@link GL20#glUniform1i(int, int)}
     *
     * <h4>调用路径</h4>
     * 常用于设置sampler2D uniform的纹理单元索引（如 setUniform1i(uNoiseTex, 0)）。
     * 需在 {@link GL13#glActiveTexture(int)} 之后、绘制之前调用。
     *
     * @param location  uniform位置
     * @param value     int值（通常为纹理单元索引 0~31）
     */
    public static void setUniform1i(int location, int value) {
        if (location >= 0) GL20.glUniform1i(location, value);
    }

    /**
     * 设置vec2类型uniform值。
     *
     * <h4>依赖API</h4>
     * {@link GL20#glUniform2f(int, float, float)}
     *
     * <h4>调用路径</h4>
     * 常用于设置2D坐标类uniform（如wave center、offset等）。
     *
     * @param location  uniform位置
     * @param x         vec2的x分量
     * @param y         vec2的y分量
     */
    public static void setUniform2f(int location, float x, float y) {
        if (location >= 0) GL20.glUniform2f(location, x, y);
    }

    /**
     * 设置vec3类型uniform值。
     *
     * <h4>依赖API</h4>
     * {@link GL20#glUniform3f(int, float, float, float)}
     *
     * <h4>调用路径</h4>
     * 常用于设置RGB颜色或3D方向向量类uniform。
     *
     * @param location  uniform位置
     * @param x         vec3的x分量
     * @param y         vec3的y分量
     * @param z         vec3的z分量
     */
    public static void setUniform3f(int location, float x, float y, float z) {
        if (location >= 0) GL20.glUniform3f(location, x, y, z);
    }

    /**
     * 设置vec4类型uniform值。
     *
     * <h4>依赖API</h4>
     * {@link GL20#glUniform4f(int, float, float, float, float)}
     *
     * <h4>调用路径</h4>
     * 常用于设置RGBA颜色、四参数配置等uniform。
     * 也用于 {@link #setUniformColor(int, Color)} 内部调用。
     *
     * @param location  uniform位置
     * @param x         vec4的x分量
     * @param y         vec4的y分量
     * @param z         vec4的z分量
     * @param w         vec4的w分量
     */
    public static void setUniform4f(int location, float x, float y, float z, float w) {
        if (location >= 0) GL20.glUniform4f(location, x, y, z, w);
    }

    /**
     * 从java.awt.Color设置vec4 uniform，自动将RGBA通道从[0,255]归一化到[0.0,1.0]。
     *
     * <h4>依赖API</h4>
     * {@link GL20#glUniform4f(int, float, float, float, float)}
     *
     * <h4>调用路径</h4>
     * 当shader中需要接收java.awt.Color类型的颜色参数时使用。
     * 典型场景：设置光束/护盾/光环的颜色uniform。
     * 若location为-1或color为null则静默跳过。
     *
     * @param location  uniform位置
     * @param color     java.awt.Color对象（RGBA各通道0~255）
     */
    public static void setUniformColor(int location, Color color) {
        if (location >= 0 && color != null) {
            GL20.glUniform4f(location,
                    color.getRed() / 255f,
                    color.getGreen() / 255f,
                    color.getBlue() / 255f,
                    color.getAlpha() / 255f);
        }
    }

    // ==================== VBO/IBO 创建 ====================

    /**
     * 创建以原点为中心的矩形VBO（Vertex Buffer Object）。
     *
     * <h4>实现功能</h4>
     * 创建一个4顶点的矩形VBO，顶点布局为 [x, y, u, v]（每顶点4个float，stride=16字节）。
     * 矩形以坐标原点(0,0)为中心，范围从(-halfW,-halfH)到(+halfW,+halfH)。
     * UV坐标固定为：左下(0,0) → 右下(1,0) → 右上(1,1) → 左上(0,1)。
     *
     * <h4>顶点布局详解</h4>
     * <pre>
     *   vertex 0: pos(-halfW, -halfH), uv(0, 0)  // 左下
     *   vertex 1: pos(+halfW, -halfH), uv(1, 0)  // 右下
     *   vertex 2: pos(+halfW, +halfH), uv(1, 1)  // 右上
     *   vertex 3: pos(-halfW, +halfH), uv(0, 1)  // 左上
     * </pre>
     * 与 {@link #createShaderProgram} 中绑定的 a_position(location=0) 和 a_texCoord(location=1) 对应。
     * 与 {@link #bindAndDrawQuad} 中的 glVertexAttribPointer stride=16, offset=0/8 对应。
     *
     * <h4>依赖API</h4>
     * <ul>
     *   <li>{@link BufferUtils#createFloatBuffer(int)} - 创建NIO FloatBuffer</li>
     *   <li>{@link GL15#glGenBuffers()} - 生成VBO handle</li>
     *   <li>{@link GL15#glBindBuffer(int, int)} - 绑定到GL_ARRAY_BUFFER目标</li>
     *   <li>{@link GL15#glBufferData(int, java.nio.FloatBuffer, int)} - 上传数据，GL_STATIC_DRAW</li>
     * </ul>
     *
     * <h4>调用路径</h4>
     * 在渲染插件构造函数或init()中调用一次，返回的handle存储为成员变量。
     * 每帧render()中通过 {@link #bindAndDrawQuad} 绑定并绘制。
     *
     * @param halfW  矩形半宽（世界/画布坐标单位），如光矛效果中 canvasWidth/2
     * @param halfH  矩形半高（世界/画布坐标单位），如光矛效果中 canvasHeight/2
     * @return VBO handle（GL15 buffer ID，正整数）
     */
    public static int createQuadVBO(float halfW, float halfH) {
        FloatBuffer verts = BufferUtils.createFloatBuffer(16);
        verts.put(new float[]{
            -halfW, -halfH,  0f, 0f,
             halfW, -halfH,  1f, 0f,
             halfW,  halfH,  1f, 1f,
            -halfW,  halfH,  0f, 1f,
        });
        verts.flip();

        int vbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, verts, GL15.GL_STATIC_DRAW);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        return vbo;
    }

    /**
     * 创建标准矩形的IBO（Index Buffer Object）。
     *
     * <h4>实现功能</h4>
     * 创建一个包含6个索引的IBO，定义两个三角形覆盖4个顶点：
     * <ul>
     *   <li>三角形1: 索引 0-1-2（左下→右下→右上）</li>
     *   <li>三角形2: 索引 0-2-3（左下→右上→左上）</li>
     * </ul>
     * 与 {@link #createQuadVBO(float, float)} 创建的VBO顶点顺序配合使用。
     *
     * <h4>依赖API</h4>
     * <ul>
     *   <li>{@link BufferUtils#createIntBuffer(int)} - 创建NIO IntBuffer</li>
     *   <li>{@link GL15#glGenBuffers()} - 生成IBO handle</li>
     *   <li>{@link GL15#glBindBuffer(int, int)} - 绑定到GL_ELEMENT_ARRAY_BUFFER目标</li>
     *   <li>{@link GL15#glBufferData(int, java.nio.IntBuffer, int)} - 上传数据，GL_STATIC_DRAW</li>
     * </ul>
     *
     * <h4>调用路径</h4>
     * 在渲染插件构造函数或init()中调用一次，返回的handle存储为成员变量。
     * 通常一个IBO可被多个VBO共享（只要顶点数量<=4）。
     *
     * @return IBO handle（GL15 buffer ID，正整数）
     */
    public static int createQuadIBO() {
        IntBuffer indices = BufferUtils.createIntBuffer(6);
        indices.put(new int[]{0, 1, 2, 0, 2, 3});
        indices.flip();

        int ibo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indices, GL15.GL_STATIC_DRAW);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
        return ibo;
    }

    // ==================== FBO 管理 ====================

    /**
     * FBO数据容器，保存帧缓冲对象ID、关联纹理ID和实际尺寸。
     *
     * <h4>实现功能</h4>
     * 封装 {@link #createFBO(int, int, String)} 的返回值，
     * 包含渲染到FBO和后续绘制FBO纹理所需的全部GPU资源引用。
     * 实际宽高可能与请求值不同（自动向上取2的幂次）。
     *
     * <h4>成员变量</h4>
     * <ul>
     *   <li>{@link #fboId} - GL30帧缓冲对象handle，用于bindFBO/unbindFBO</li>
     *   <li>{@link #fboTexId} - 关联的RGBA纹理handle，用于后续glBindTexture绘制</li>
     *   <li>{@link #width} - 纹理实际宽度（POT），用于clearFBO和setupFBOMatrices</li>
     *   <li>{@link #height} - 纹理实际高度（POT），用于clearFBO和setupFBOMatrices</li>
     * </ul>
     */
    public static class FBOData {
        /** OpenGL帧缓冲对象handle（GL30.glGenFramebuffers返回值） */
        public int fboId;
        /** 关联的RGBA纹理handle（GL11.glGenTextures返回值），作为FBO的颜色附着点 */
        public int fboTexId;
        /** 纹理实际宽度（已向上取到2的幂次），用于glViewport和正交投影参数 */
        public int width;
        /** 纹理实际高度（已向上取到2的幂次），用于glViewport和正交投影参数 */
        public int height;

        public FBOData(int fboId, int fboTexId, int width, int height) {
            this.fboId = fboId;
            this.fboTexId = fboTexId;
            this.width = width;
            this.height = height;
        }
    }

    /**
     * 创建FBO（帧缓冲对象）并关联一张RGBA纹理作为颜色缓冲区。
     *
     * <h4>实现功能</h4>
     * <ol>
     *   <li>将请求尺寸向上取到2的幂次（{@link #nextPowerOfTwo(int)}）</li>
     *   <li>创建RGBA纹理：GL_LINEAR过滤、GL_CLAMP包裹、无初始数据</li>
     *   <li>创建FBO并将纹理附着为GL_COLOR_ATTACHMENT0</li>
     *   <li>检查FBO完整性（{@link GL30#glCheckFramebufferStatus(int)}）</li>
     *   <li>失败时自动清理所有已创建的GL资源并返回null</li>
     *   <li>成功时解绑FBO和纹理，返回FBOData</li>
     * </ol>
     *
     * <h4>依赖API</h4>
     * <ul>
     *   <li>{@link GL11#glGenTextures()} - 创建纹理对象</li>
     *   <li>{@link GL11#glBindTexture(int, int)} - 绑定纹理</li>
     *   <li>{@link GL11#glTexImage2D(int, int, int, int, int, int, int, int, ByteBuffer)} - 分配纹理存储</li>
     *   <li>{@link GL11#glTexParameteri(int, int, int)} - 设置纹理参数</li>
     *   <li>{@link GL30#glGenFramebuffers()} - 创建FBO</li>
     *   <li>{@link GL30#glBindFramebuffer(int, int)} - 绑定FBO</li>
     *   <li>{@link GL30#glFramebufferTexture2D(int, int, int, int, int)} - 附着纹理到FBO</li>
     *   <li>{@link GL30#glCheckFramebufferStatus(int)} - 检查FBO完整性</li>
     * </ul>
     *
     * <h4>调用路径</h4>
     * 在渲染插件的 {@code ensureFBO()} 或init()中调用。
     * 通常采用延迟创建模式：首帧render()时检查fbo==null再创建，避免在非GL线程初始化。
     * 返回的 {@link FBOData} 存储为成员变量。
     *
     * @param width   请求的FBO宽度（像素），会自动向上取POT
     * @param height  请求的FBO高度（像素），会自动向上取POT
     * @param logTag  日志标识符
     * @return 成功返回FBOData（包含fboId、fboTexId、实际宽高），失败返回null
     */
    public static FBOData createFBO(int width, int height, String logTag) {
        int tw = nextPowerOfTwo(width);
        int th = nextPowerOfTwo(height);

        int texId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texId);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, tw, th, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);

        int fboId = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fboId);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                GL11.GL_TEXTURE_2D, texId, 0);

        int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
        if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
            log.error("[" + logTag + "] FBO creation failed, status: " + status);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
            GL11.glDeleteTextures(texId);
            GL30.glDeleteFramebuffers(fboId);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
            return null;
        }

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        return new FBOData(fboId, texId, tw, th);
    }

    /**
     * 销毁FBO及其关联纹理，释放GPU资源。
     *
     * <h4>依赖API</h4>
     * <ul>
     *   <li>{@link GL30#glDeleteFramebuffers(int)} - 删除帧缓冲对象</li>
     *   <li>{@link GL11#glDeleteTextures(int)} - 删除纹理对象</li>
     * </ul>
     *
     * <h4>调用路径</h4>
     * 在渲染插件的 {@code cleanup()} 方法中调用。
     * handle为0时静默跳过（未初始化或已销毁的情况）。
     *
     * @param fboId     帧缓冲对象handle（0=跳过）
     * @param fboTexId  纹理handle（0=跳过）
     */
    public static void destroyFBO(int fboId, int fboTexId) {
        if (fboId != 0) GL30.glDeleteFramebuffers(fboId);
        if (fboTexId != 0) GL11.glDeleteTextures(fboTexId);
    }

    /**
     * 绑定FBO为当前渲染目标，自动检测并兼容GL30/ARB/EXT帧缓冲扩展。
     *
     * <h4>实现功能</h4>
     * 通过反射检查Starsector内部的 {@code com.fs.starfarer.api.impl.shader.ShaderLib} 类，
     * 确定当前GL环境支持的帧缓冲扩展类型，选择对应的绑定API：
     * <ul>
     *   <li>GL30核心: {@link GL30#glBindFramebuffer(int, int)}</li>
     *   <li>ARB扩展: {@link ARBFramebufferObject#glBindFramebuffer(int, int)}</li>
     *   <li>EXT扩展: {@link EXTFramebufferObject#glBindFramebufferEXT(int, int)}</li>
     * </ul>
     * 反射失败时回退到GL30核心API。
     *
     * <h4>依赖API</h4>
     * <ul>
     *   <li>{@link GL30#glBindFramebuffer(int, int)} - 核心API</li>
     *   <li>{@link ARBFramebufferObject#glBindFramebuffer(int, int)} - ARB扩展</li>
     *   <li>{@link EXTFramebufferObject#glBindFramebufferEXT(int, int)} - EXT扩展</li>
     * </ul>
     *
     * <h4>调用路径</h4>
     * 在render()方法中，FBO渲染阶段开始时调用。
     * 调用顺序：bindFBO → clearFBO → setupFBOMatrices → 绘制 → unbindFBO。
     *
     * @param fboId  要绑定的帧缓冲对象handle（来自 {@link FBOData#fboId}）
     */
    public static void bindFBO(int fboId) {
        try {
            Class<?> shaderLibClass = Class.forName("com.fs.starfarer.api.impl.shader.ShaderLib");
            boolean useCore = (Boolean) shaderLibClass.getMethod("useBufferCore").invoke(null);
            boolean useARB = (Boolean) shaderLibClass.getMethod("useBufferARB").invoke(null);

            if (useCore) {
                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fboId);
            } else if (useARB) {
                ARBFramebufferObject.glBindFramebuffer(
                        ARBFramebufferObject.GL_FRAMEBUFFER, fboId);
            } else {
                EXTFramebufferObject.glBindFramebufferEXT(
                        EXTFramebufferObject.GL_FRAMEBUFFER_EXT, fboId);
            }
        } catch (Exception e) {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fboId);
        }
    }

    /**
     * 解绑当前FBO，恢复默认帧缓冲（屏幕）为渲染目标。
     *
     * <h4>实现功能</h4>
     * 与 {@link #bindFBO(int)} 使用相同的GL30/ARB/EXT扩展检测逻辑，
     * 将帧缓冲绑定回0（默认帧缓冲）。
     * 必须在FBO渲染完成后、绘制FBO纹理到屏幕之前调用。
     *
     * <h4>依赖API</h4>
     * 同 {@link #bindFBO(int)}
     *
     * <h4>调用路径</h4>
     * 在render()方法中，FBO内绘制完成后调用。
     * 调用顺序：bindFBO → clearFBO → 绘制到FBO → unbindFBO → 绘制FBO纹理到屏幕。
     */
    public static void unbindFBO() {
        try {
            Class<?> shaderLibClass = Class.forName("com.fs.starfarer.api.impl.shader.ShaderLib");
            boolean useCore = (Boolean) shaderLibClass.getMethod("useBufferCore").invoke(null);
            boolean useARB = (Boolean) shaderLibClass.getMethod("useBufferARB").invoke(null);

            if (useCore) {
                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
            } else if (useARB) {
                ARBFramebufferObject.glBindFramebuffer(
                        ARBFramebufferObject.GL_FRAMEBUFFER, 0);
            } else {
                EXTFramebufferObject.glBindFramebufferEXT(
                        EXTFramebufferObject.GL_FRAMEBUFFER_EXT, 0);
            }
        } catch (Exception e) {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        }
    }

    /**
     * 准备FBO进行渲染：设置视口、重置colorMask、清空颜色缓冲区。
     *
     * <h4>实现功能</h4>
     * <ol>
     *   <li>{@link GL11#glViewport(int, int, int, int)} - 设置FBO内部渲染视口为完整尺寸</li>
     *   <li>{@link GL11#glColorMask(boolean, boolean, boolean, boolean)} - 重置为全部通道可写
     *       （防止前一渲染阶段的colorMask限制影响当前FBO写入）</li>
     *   <li>{@link GL11#glClearColor(float, float, float, float)} - 设置清除色为透明黑(0,0,0,0)</li>
     *   <li>{@link GL11#glClear(int)} - 清空颜色缓冲区</li>
     * </ol>
     *
     * <h4>调用路径</h4>
     * 必须在 {@link #bindFBO(int)} 之后、任何FBO内绘制之前调用。
     * 典型顺序：bindFBO(fboId) → clearFBO(w, h) → setupFBOMatrices(w, h) → 绘制。
     *
     * @param fboW  FBO实际宽度（来自 {@link FBOData#width}）
     * @param fboH  FBO实际高度（来自 {@link FBOData#height}）
     */
    public static void clearFBO(int fboW, int fboH) {
        GL11.glViewport(0, 0, fboW, fboH);
        GL11.glColorMask(true, true, true, true);
        GL11.glClearColor(0f, 0f, 0f, 0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
    }

    /**
     * 计算大于等于给定值的最小2的幂次。
     *
     * <h4>实现功能</h4>
     * 使用位运算高效计算下一个POT值。算法：先减1，然后通过右移+或运算逐步填充所有低位，
     * 最后加1得到2的幂次。
     * 例：300 → 512, 512 → 512, 1 → 1, 0 → 1。
     *
     * <h4>调用路径</h4>
     * 由 {@link #createFBO(int, int, String)} 内部调用，用于将FBO纹理尺寸对齐到POT。
     * 某些GPU对非POT纹理支持不完善，使用POT可确保最大兼容性。
     *
     * @param value  输入值（需为正整数）
     * @return 大于等于value的最小2的幂次；value<=0时返回1
     */
    public static int nextPowerOfTwo(int value) {
        if (value <= 0) return 1;
        value--;
        value |= value >> 1;
        value |= value >> 2;
        value |= value >> 4;
        value |= value >> 8;
        value |= value >> 16;
        return value + 1;
    }

    // ==================== 矩阵操作 ====================

    /**
     * 压栈OpenGL三个矩阵栈：投影矩阵、纹理矩阵、模型视图矩阵。
     *
     * <h4>实现功能</h4>
     * 保存当前GL矩阵状态，以便后续自由修改而不影响外部渲染。
     * 必须与 {@link #popAllMatrices()} 配对使用，形成push/pop保护块。
     *
     * <h4>依赖API</h4>
     * <ul>
     *   <li>{@link GL11#glMatrixMode(int)} - 切换当前矩阵模式</li>
     *   <li>{@link GL11#glPushMatrix()} - 压栈当前矩阵</li>
     * </ul>
     *
     * <h4>调用路径</h4>
     * 在每个render()方法的最开始调用（通常在 {@code glPushAttrib} 之后），
     * 保护Starsector引擎的矩阵状态不被修改。
     *
     * <h4>压栈顺序</h4>
     * PROJECTION → TEXTURE → MODELVIEW（与popAllMatrices的弹出顺序相反）
     */
    public static void pushAllMatrices() {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glMatrixMode(GL11.GL_TEXTURE);
        GL11.glPushMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
    }

    /**
     * 弹出OpenGL三个矩阵栈，恢复之前保存的矩阵状态。
     *
     * <h4>实现功能</h4>
     * 与 {@link #pushAllMatrices()} 配对，以逆序弹出矩阵栈。
     *
     * <h4>依赖API</h4>
     * <ul>
     *   <li>{@link GL11#glMatrixMode(int)} - 切换当前矩阵模式</li>
     *   <li>{@link GL11#glPopMatrix()} - 弹出并恢复矩阵</li>
     * </ul>
     *
     * <h4>调用路径</h4>
     * 在每个render()方法的最后调用（通常在 {@code glPopAttrib} 之前）。
     *
     * <h4>弹出顺序</h4>
     * MODELVIEW → TEXTURE → PROJECTION（与pushAllMatrices的压栈顺序相反）
     */
    public static void popAllMatrices() {
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_TEXTURE);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
    }

    /**
     * 配置FBO内部渲染的正交投影和以中心为原点的模型视图矩阵。
     *
     * <h4>实现功能</h4>
     * <ul>
     *   <li>投影矩阵：{@link GL11#glOrtho(double, double, double, double, double, double)}
     *       设置为 (0, fboW, 0, fboH)，Y轴朝上</li>
     *   <li>纹理矩阵：LoadIdentity（不做变换）</li>
     *   <li>模型视图矩阵：LoadIdentity后平移到(fboW/2, fboH/2, 0)，使原点位于FBO中心</li>
     * </ul>
     * Z范围设为[-2000, 2000]，提供充足的深度排序空间。
     *
     * <h4>依赖API</h4>
     * <ul>
     *   <li>{@link GL11#glMatrixMode(int)} - 切换矩阵模式</li>
     *   <li>{@link GL11#glLoadIdentity()} - 重置为单位矩阵</li>
     *   <li>{@link GL11#glOrtho(double, double, double, double, double, double)} - 正交投影</li>
     *   <li>{@link GL11#glPushMatrix()} - 压栈（纹理和模型视图）</li>
     *   <li>{@link GL11#glTranslatef(float, float, float)} - 平移</li>
     * </ul>
     *
     * <h4>调用路径</h4>
     * 在FBO渲染流程中，{@link #bindFBO(int)} + {@link #clearFBO(int, int)} 之后调用。
     * 此方法对投影、纹理、模型视图三个矩阵栈都做了push，
     * 调用方需在FBO渲染结束后通过 {@link #popAllMatrices()} 弹出恢复。
     *
     * @param fboW  FBO宽度（来自 {@link FBOData#width}）
     * @param fboH  FBO高度（来自 {@link FBOData#height}）
     */
    public static void setupFBOMatrices(int fboW, int fboH) {
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
    }

    /**
     * 配置世界空间渲染的模型视图变换：平移到目标位置并可选旋转。
     *
     * <h4>实现功能</h4>
     * 在当前模型视图矩阵基础上施加平移（和可选旋转），
     * 使后续绘制操作以(x,y)为原点、可选旋转rotationDeg度。
     * 应在 {@link #pushAllMatrices()} 之后调用。
     *
     * <h4>依赖API</h4>
     * <ul>
     *   <li>{@link GL11#glTranslatef(float, float, float)} - 平移到世界坐标</li>
     *   <li>{@link GL11#glRotatef(float, float, float, float)} - 绕Z轴旋转（仅rotationDeg!=0时）</li>
     * </ul>
     *
     * <h4>调用路径</h4>
     * 在render()方法中，pushAllMatrices()之后、shader绑定和绘制之前调用。
     * 典型场景：将shader效果渲染到舰船位置、光束起点等世界坐标。
     *
     * @param x             世界X坐标（如舰船位置 ship.getLocation().x）
     * @param y             世界Y坐标（如舰船位置 ship.getLocation().y）
     * @param rotationDeg   旋转角度（度），0表示不旋转；正值逆时针
     */
    public static void setupWorldTransform(float x, float y, float rotationDeg) {
        GL11.glTranslatef(x, y, 0f);
        if (rotationDeg != 0f) {
            GL11.glRotatef(rotationDeg, 0f, 0f, 1f);
        }
    }

    // ==================== 渲染管线 ====================

    /**
     * 使用加法混合（additive blending）渲染一个四边形。
     *
     * <h4>实现功能</h4>
     * 设置混合模式为 {@code GL_SRC_ALPHA, GL_ONE}（加法混合），
     * 然后调用 {@link #bindAndDrawQuad(int, int, int)} 执行完整渲染流程。
     * 加法混合使颜色值累加到帧缓冲上，适用于发光、光束、能量场等特效。
     *
     * <h4>依赖API</h4>
     * <ul>
     *   <li>{@link GL11#glEnable(int)} - 启用GL_BLEND</li>
     *   <li>{@link GL11#glBlendFunc(int, int)} - 设置混合函数</li>
     *   <li>{@link #bindAndDrawQuad(int, int, int)} - 内部绘制方法</li>
     * </ul>
     *
     * <h4>调用路径</h4>
     * 在render()方法中，shader uniform设置完成后调用。
     * 内部会重新调用glUseProgram，不影响之前设置的uniform值（uniform存储在program中）。
     *
     * <h4>使用示例</h4>
     * <pre>
     *   ShaderUtil.pushAllMatrices();
     *   ShaderUtil.setupWorldTransform(x, y, 0f);
     *   GL20.glUseProgram(program);
     *   ShaderUtil.setUniform1f(timeLoc, elapsed);
     *   ShaderUtil.renderQuadAdditive(program, vbo, ibo);
     *   ShaderUtil.popAllMatrices();
     * </pre>
     *
     * @param shaderProgram  已链接的shader program handle
     * @param vbo            矩形VBO handle（来自 {@link #createQuadVBO}）
     * @param ibo            矩形IBO handle（来自 {@link #createQuadIBO}）
     */
    public static void renderQuadAdditive(int shaderProgram, int vbo, int ibo) {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        bindAndDrawQuad(shaderProgram, vbo, ibo);
    }

    /**
     * 使用标准Alpha混合渲染一个四边形。
     *
     * <h4>实现功能</h4>
     * 设置混合模式为 {@code GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA}（标准Alpha混合），
     * 然后调用 {@link #bindAndDrawQuad(int, int, int)} 执行完整渲染流程。
     * Alpha混合按透明度正确叠加颜色，适用于护盾、UI面板、半透明遮罩等。
     *
     * <h4>依赖API</h4>
     * 同 {@link #renderQuadAdditive(int, int, int)}
     *
     * <h4>调用路径</h4>
     * 同 {@link #renderQuadAdditive(int, int, int)}
     *
     * @param shaderProgram  已链接的shader program handle
     * @param vbo            矩形VBO handle
     * @param ibo            矩形IBO handle
     */
    public static void renderQuadAlpha(int shaderProgram, int vbo, int ibo) {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        bindAndDrawQuad(shaderProgram, vbo, ibo);
    }

    /**
     * 内部方法：绑定shader和VBO/IBO，启用顶点属性，执行绘制，清理状态。
     *
     * <h4>实现功能</h4>
     * 完整的单次绘制流程：
     * <ol>
     *   <li>{@link GL20#glUseProgram(int)} - 激活shader program</li>
     *   <li>{@link GL15#glBindBuffer(int, int)} - 绑定VBO到GL_ARRAY_BUFFER</li>
     *   <li>{@link GL15#glBindBuffer(int, int)} - 绑定IBO到GL_ELEMENT_ARRAY_BUFFER</li>
     *   <li>{@link GL20#glEnableVertexAttribArray(int)} - 启用属性数组0和1</li>
     *   <li>{@link GL20#glVertexAttribPointer(int, int, int, boolean, int, long)}
     *       - 配置属性指针：location 0 = position(x,y), offset=0; location 1 = texCoord(u,v), offset=8</li>
     *   <li>{@link GL11#glDrawElements(int, int, int, long)} - 绘制6个索引（2个三角形）</li>
     *   <li>禁用属性数组、解绑VBO/IBO、解绑shader（glUseProgram(0)）</li>
     * </ol>
     *
     * <h4>顶点布局约定</h4>
     * 每顶点4个float（16字节）：[x, y, u, v]
     * <ul>
     *   <li>location 0 (a_position): 2个float, offset=0, stride=16</li>
     *   <li>location 1 (a_texCoord): 2个float, offset=8, stride=16</li>
     * </ul>
     *
     * <h4>调用路径</h4>
     * 由 {@link #renderQuadAdditive(int, int, int)} 和 {@link #renderQuadAlpha(int, int, int)} 内部调用，
     * 不应直接外部调用。
     *
     * @param shaderProgram  shader program handle
     * @param vbo            VBO handle
     * @param ibo            IBO handle
     */
    private static void bindAndDrawQuad(int shaderProgram, int vbo, int ibo) {
        GL20.glUseProgram(shaderProgram);

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
    }

    // ==================== GPU 资源清理 ====================

    /**
     * 删除shader program对象，释放GPU资源。
     *
     * <h4>依赖API</h4>
     * {@link GL20#glDeleteProgram(int)}
     *
     * <h4>调用路径</h4>
     * 在渲染插件的 {@code cleanup()} 方法中调用。
     * handle为0或负数时静默跳过（未初始化或已销毁的情况）。
     *
     * @param program  shader program handle（0=跳过）
     */
    public static void deleteProgram(int program) {
        if (program > 0) GL20.glDeleteProgram(program);
    }

    /**
     * 删除VBO和IBO缓冲区对象，释放GPU显存。
     *
     * <h4>依赖API</h4>
     * {@link GL15#glDeleteBuffers(int)}
     *
     * <h4>调用路径</h4>
     * 在渲染插件的 {@code cleanup()} 方法中调用。
     * handle为0时静默跳过。
     *
     * @param vbo  VBO handle（0=跳过）
     * @param ibo  IBO handle（0=跳过）
     */
    public static void deleteBuffers(int vbo, int ibo) {
        if (vbo != 0) GL15.glDeleteBuffers(vbo);
        if (ibo != 0) GL15.glDeleteBuffers(ibo);
    }

    /**
     * 一次性清理shader program + VBO + IBO全部GPU资源。
     *
     * <h4>实现功能</h4>
     * 依次调用 {@link #deleteProgram(int)} 和 {@link #deleteBuffers(int, int)}，
     * 简化cleanup()方法中的资源释放代码。
     *
     * <h4>调用路径</h4>
     * 在渲染插件的 {@code cleanup()} 方法中调用，通常在插件过期或被移除时触发。
     * 调用后应将对应的成员变量设为0，防止重复释放。
     *
     * <h4>使用示例</h4>
     * <pre>
     *   public void cleanup() {
     *       ShaderUtil.cleanupAll(shaderProgram, vbo, ibo);
     *       shaderProgram = 0;
     *       vbo = 0;
     *       ibo = 0;
     *   }
     * </pre>
     *
     * @param program  shader program handle
     * @param vbo      VBO handle
     * @param ibo      IBO handle
     */
    public static void cleanupAll(int program, int vbo, int ibo) {
        deleteProgram(program);
        deleteBuffers(vbo, ibo);
    }
}
