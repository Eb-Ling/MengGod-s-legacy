package data.methods.shaders;

import com.fs.starfarer.api.Global;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

import java.awt.*;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import org.lwjgl.util.vector.Vector2f;

/**
 * ShaderUtil - GPU着色器渲染工具类，封装Starsector模组开发中常用的OpenGL操作。
 *
 * <h3>功能概述</h3>
 * 本类从项目中多个渲染插件中提取重复的shader编译、FBO管理、VAO创建、
 * 模型变换矩阵构建、SSBO管理、uniform设置等代码，统一封装为静态工具方法。
 * 采用现代着色器管线方案：VAO管理顶点数据、modelMatrix uniform传递4x4列主序变换矩阵、
 * SSBO传递每帧动态结构化数据。
 *
 * <h3>依赖的API</h3>
 * <ul>
 *   <li>LWJGL {@link org.lwjgl.opengl.GL11} - 基础OpenGL操作（混合、纹理、视口）</li>
 *   <li>LWJGL {@link org.lwjgl.opengl.GL15} - VBO/SSBO缓冲区对象管理</li>
 *   <li>LWJGL {@link org.lwjgl.opengl.GL20} - GLSL着色器编译/链接/uniform设置/顶点属性</li>
 *   <li>LWJGL {@link org.lwjgl.opengl.GL30} - VAO顶点数组对象、FBO帧缓冲对象</li>
 *   <li>LWJGL {@link org.lwjgl.opengl.GL43} - SSBO着色器存储缓冲区对象</li>
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
 *   <li>VAO创建: {@link #createUniversalRectVAO}</li>
 *   <li>模型变换矩阵: {@link #buildModelMatrix}</li>
 *   <li>FBO管理: {@link #createFBO}, {@link #destroyFBO}, {@link #bindFBO},
 *       {@link #unbindFBO}, {@link #clearFBO}, {@link #nextPowerOfTwo}</li>
 *   <li>FBO旧管线渲染: {@link #pushAllMatrices}, {@link #popAllMatrices},
 *       {@link #setupFBOMatrices}, {@link #setupWorldTransform},
 *       {@link #renderQuadAdditive}, {@link #renderQuadAlpha},
 *       {@link #bindAndDrawQuad}(private)</li>
 *   <li>SSBO管理: {@link #createSSBO}, {@link #uploadSSBO}, {@link #bindSSBO},
 *       {@link #unbindSSBO}, {@link #deleteSSBO}</li>
 *   <li>渲染管线: {@link #drawVAOQuad}</li>
 *   <li>GPU资源清理: {@link #deleteProgram}, {@link #deleteBuffers}, {@link #deleteVAO},
 *       {@link #cleanupAll}</li>
 * </ul>
 *
 * <h3>典型调用流程</h3>
 * <pre>
 *   // 初始化阶段
 *   VAOData vao = ShaderUtil.createUniversalRectVAO();
 *   int program = ShaderUtil.createShaderProgram(vertSrc, fragSrc, "MyEffect");
 *   int[] locs = ShaderUtil.getUniformLocations(program, "modelMatrix", "size", "u_time");
 *   int ssbo = ShaderUtil.createSSBO();
 *
 *   // 每帧渲染阶段
 *   GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
 *   GL11.glEnable(GL11.GL_BLEND);
 *   GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
 *   GL20.glUseProgram(program);
 *   FloatBuffer mat = ShaderUtil.buildModelMatrix(x, y, angleDeg);
 *   GL20.glUniformMatrix4(locs[0], false, mat);
 *   ShaderUtil.setUniform2f(locs[1], fullSize, fullSize); // size是全尺寸(直径)，非半径
 *   ShaderUtil.setUniform1f(locs[2], timer);
 *   ShaderUtil.uploadSSBO(ssbo, dataBuffer, floatCount);
 *   ShaderUtil.bindSSBO(ssbo, 1);
 *   ShaderUtil.drawVAOQuad(vao.vaoId);
 *   ShaderUtil.unbindSSBO(1);
 *   GL20.glUseProgram(0);
 *   GL11.glPopAttrib();
 *
 *   // 销毁阶段
 *   ShaderUtil.cleanupAll(program, vao, ssbo);
 * </pre>
 *
 * <h3>FBO渲染流程</h3>
 * <pre>
 *   ShaderUtil.FBOData fbo = ShaderUtil.createFBO(512, 512, "MyEffect");
 *   ShaderUtil.bindFBO(fbo.fboId);
 *   ShaderUtil.clearFBO(fbo.width, fbo.height);
 *   GL20.glUseProgram(program);
 *   FloatBuffer mat = ShaderUtil.buildModelMatrix(0, 0, 0);
 *   GL20.glUniformMatrix4(modelMatrixLoc, false, mat);
 *   ShaderUtil.drawVAOQuad(vao.vaoId);
 *   ShaderUtil.unbindFBO();
 *   ShaderUtil.destroyFBO(fbo.fboId, fbo.fboTexId);
 * </pre>
 *
 * <h3>日志规范</h3>
 * 所有错误/信息日志统一通过 {@code Global.getLogger(ShaderUtil.class)} 输出，
 * 禁止使用 System.out 或 System.err。
 *
 * <h3>提取来源</h3>
 * <ul>
 *   <li>{@code src/hullmods/Meng_fire_Phasecore.java} (PhaseCoreRangePlugin内部类)</li>
 *   <li>{@code src/hullmods/Meng_HexShieldPlugin.java} (含FBO完整流程)</li>
 *   <li>{@code src/hullmods/Meng_MingGodCenter.java} (Meng_MingGodSignPlugin内部类)</li>
 *   <li>{@code src/example/PLSP_EventDisturbVisual.java} (VAO+SSBO+modelMatrix范式参考)</li>
 * </ul>
 *
 * @see org.lwjgl.opengl.GL20
 * @see org.lwjgl.opengl.GL15
 * @see org.lwjgl.opengl.GL30
 * @see org.lwjgl.opengl.GL43
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

    /**
     * 从模组资源目录读取顶点和片段着色器，并创建链接后的 shader program。
     *
     * <p>调用 {@link Global#getSettings()} 的 {@code loadText()}，因此资源文件必须位于
     * mod 根目录下并随发布 jar 一起分发。读取、编译或链接失败时，本方法会记录完整错误，
     * 返回 {@code 0}，调用方应停止本次效果的 GPU 初始化。</p>
     *
     * @param vertPath 顶点着色器相对模组根目录的路径
     * @param fragPath 片段着色器相对模组根目录的路径
     * @param logTag 日志标识符，用于区分效果来源
     * @return 链接成功的 program handle，失败返回 {@code 0}
     */
    public static int createShaderProgramFromFiles(String vertPath, String fragPath, String logTag) {
        try {
            return createShaderProgram(
                    Global.getSettings().loadText(vertPath),
                    Global.getSettings().loadText(fragPath),
                    logTag);
        } catch (Exception e) {
            log.error("[" + logTag + "] Failed to load shader resources: "
                    + vertPath + ", " + fragPath, e);
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
     *   <li>{@link #width} - 纹理实际宽度（POT），用于clearFBO视口设置</li>
     *   <li>{@link #height} - 纹理实际高度（POT），用于clearFBO视口设置</li>
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
     * 调用顺序：bindFBO → clearFBO → 设置shader+uniform → 绘制 → unbindFBO。
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
     * 典型顺序：bindFBO(fboId) → clearFBO(w, h) → 设置shader+uniform → 绘制。
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

    // ==================== FBO 旧管线渲染 ====================

    /**
     * 压入 OpenGL 三个矩阵栈（PROJECTION、TEXTURE、MODELVIEW），保存当前矩阵状态。
     *
     * <h4>实现功能</h4>
     * 依次对投影、纹理、模型视图三个矩阵栈执行 glPushMatrix，
     * 与 {@link #popAllMatrices()} 配对使用，保护游戏引擎的矩阵状态不被破坏。
     * 适用于旧管线（gl_ModelViewProjectionMatrix）渲染流程。
     *
     * <h4>调用路径</h4>
     * 在 render() 方法中、修改矩阵之前调用，渲染完成后通过 {@link #popAllMatrices()} 恢复。
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
     * 弹出 OpenGL 三个矩阵栈，恢复之前保存的矩阵状态。
     *
     * <h4>实现功能</h4>
     * 与 {@link #pushAllMatrices()} 配对，以逆序弹出矩阵栈：
     * MODELVIEW → TEXTURE → PROJECTION。
     *
     * <h4>调用路径</h4>
     * 在每个 render() 方法的最后调用（通常在 glPopAttrib 之前）。
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
     * 配置 FBO 内部渲染的正交投影和以中心为原点的模型视图矩阵。
     *
     * <h4>实现功能</h4>
     * <ul>
     *   <li>投影矩阵：glOrtho(0, fboW, 0, fboH)，Y轴朝上</li>
     *   <li>纹理矩阵：LoadIdentity（不做变换）</li>
     *   <li>模型视图矩阵：LoadIdentity 后平移到 (fboW/2, fboH/2, 0)，使原点位于 FBO 中心</li>
     * </ul>
     * Z 范围设为 [-2000, 2000]，提供充足的深度排序空间。
     * 此方法对三个矩阵栈都做了 push，调用方需通过 {@link #popAllMatrices()} 弹出恢复。
     *
     * <h4>调用路径</h4>
     * 在 FBO 渲染流程中，bindFBO + clearFBO 之后调用。
     * 渲染完成后通过 {@link #popAllMatrices()} + unbindFBO 恢复。
     *
     * @param fboW  FBO 宽度（来自 {@link FBOData#width}）
     * @param fboH  FBO 高度（来自 {@link FBOData#height}）
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
     * 在当前模型视图矩阵基础上施加平移和可选旋转，
     * 使后续绘制操作以 (x, y) 为原点、可选旋转 rotationDeg 度。
     * 应在 {@link #pushAllMatrices()} 之后调用。
     *
     * <h4>调用路径</h4>
     * 在 render() 方法中，pushAllMatrices() 之后、shader 绑定和绘制之前调用。
     *
     * @param x             世界 X 坐标
     * @param y             世界 Y 坐标
     * @param rotationDeg   旋转角度（度），0 表示不旋转
     */
    public static void setupWorldTransform(float x, float y, float rotationDeg) {
        GL11.glTranslatef(x, y, 0f);
        if (rotationDeg != 0f) {
            GL11.glRotatef(rotationDeg, 0f, 0f, 1f);
        }
    }

    /**
     * 使用加法混合渲染四边形（旧管线 VBO+IBO 方案）。
     *
     * <h4>实现功能</h4>
     * 设置 GL_SRC_ALPHA + GL_ONE 加法混合，然后调用 {@link #bindAndDrawQuad} 绘制。
     * 适用于发光、光束、能量场等特效。
     *
     * <h4>调用路径</h4>
     * 在旧管线 render() 中，shader uniform 设置完成后调用。
     *
     * @param shaderProgram  shader program handle
     * @param vbo            矩形 VBO handle（来自 {@link #createQuadVBO}）
     * @param ibo            矩形 IBO handle（来自 {@link #createQuadIBO}）
     */
    public static void renderQuadAdditive(int shaderProgram, int vbo, int ibo) {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        bindAndDrawQuad(shaderProgram, vbo, ibo);
    }

    /**
     * 使用标准 Alpha 混合渲染四边形（旧管线 VBO+IBO 方案）。
     *
     * <h4>实现功能</h4>
     * 设置 GL_SRC_ALPHA + GL_ONE_MINUS_SRC_ALPHA 混合，然后调用 {@link #bindAndDrawQuad} 绘制。
     * 适用于护盾、UI 面板、半透明遮罩等。
     *
     * <h4>调用路径</h4>
     * 在旧管线 render() 中，shader uniform 设置完成后调用。
     *
     * @param shaderProgram  shader program handle
     * @param vbo            矩形 VBO handle
     * @param ibo            矩形 IBO handle
     */
    public static void renderQuadAlpha(int shaderProgram, int vbo, int ibo) {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        bindAndDrawQuad(shaderProgram, vbo, ibo);
    }

    /**
     * 内部方法：绑定 shader 和 VBO/IBO，启用顶点属性，执行绘制，清理状态。
     *
     * <h4>顶点布局</h4>
     * 每顶点 4 个 float（16 字节）：[x, y, u, v]
     * <ul>
     *   <li>location 0 (a_position): 2 个 float, offset=0, stride=16</li>
     *   <li>location 1 (a_texCoord): 2 个 float, offset=8, stride=16</li>
     * </ul>
     *
     * <h4>调用路径</h4>
     * 由 {@link #renderQuadAdditive} 和 {@link #renderQuadAlpha} 内部调用，不应直接外部调用。
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

    // ==================== VAO 创建 ====================

    /**
     * VAO数据容器，保存顶点数组对象ID和关联的VBO ID。
     *
     * <h4>成员变量</h4>
     * <ul>
     *   <li>{@link #vaoId} - GL30顶点数组对象handle，用于绑定顶点属性配置</li>
     *   <li>{@link #vboId} - GL15顶点缓冲区对象handle，存储实际顶点数据</li>
     * </ul>
     */
    public static class VAOData {
        /** OpenGL顶点数组对象handle */
        public int vaoId;
        /** 关联的顶点缓冲区对象handle */
        public int vboId;

        public VAOData(int vaoId, int vboId) {
            this.vaoId = vaoId;
            this.vboId = vboId;
        }
    }

    /**
     * 创建通用矩形VAO+VBO，使用byte顶点数据。
     *
     * <h4>实现功能</h4>
     * 创建一个包含4个顶点的矩形VAO，顶点数据为 [-128,-128] 到 [127,127] 的byte值，
     * 通过 glAttribPointer 归一化到 [-1, 1) 浮点范围。使用 GL_TRIANGLE_STRIP 绘制。
     * 实际大小由 shader 的 size/modelMatrix uniform 控制。
     *
     * <h4>顶点布局</h4>
     * <pre>
     *   vertex 0: (-128, -128)  // 左下
     *   vertex 1: ( 127, -128)  // 右下
     *   vertex 2: (-128,  127)  // 左上
     *   vertex 3: ( 127,  127)  // 右上
     * </pre>
     *
     * <h4>依赖API</h4>
     * <ul>
     *   <li>{@link GL30#glGenVertexArrays()} - 创建VAO</li>
     *   <li>{@link GL15#glGenBuffers()} - 创建VBO</li>
     *   <li>{@link GL15#glBufferData(int, java.nio.ByteBuffer, int)} - 上传byte顶点数据</li>
     *   <li>{@link GL20#glVertexAttribPointer} - 配置顶点属性(location=0, 2 bytes, normalized)</li>
     * </ul>
     *
     * <h4>调用路径</h4>
     * 在渲染插件的构造函数或init()中调用一次，返回的 VAOData 存储为成员变量。
     * 每帧render()中通过 {@link #drawVAOQuad(int)} 绑定并绘制。
     *
     * @return VAOData 包含vaoId和vboId的数据容器
     */
    public static VAOData createUniversalRectVAO() {
        byte[] BASIC_VERTICES = new byte[]{-128, -128, 127, -128, -128, 127, 127, 127};
        ByteBuffer vertexBuffer = ByteBuffer.allocateDirect(BASIC_VERTICES.length);
        vertexBuffer.put(BASIC_VERTICES);
        vertexBuffer.flip();

        int vaoId = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vaoId);

        int vboId = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexBuffer, GL15.GL_STATIC_DRAW);

        GL20.glVertexAttribPointer(0, 2, GL11.GL_BYTE, true, 2, 0);
        GL20.glEnableVertexAttribArray(0);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);

        return new VAOData(vaoId, vboId);
    }

    // ==================== 模型变换矩阵 ====================

    /**
     * 构建4x4列主序模型变换矩阵（旋转+平移）。
     *
     * <h4>实现功能</h4>
     * 构建标准OpenGL列主序4x4矩阵，包含Z轴旋转和平移变换：
     * <pre>
     *   列0: [cos, sin, 0, 0]     列1: [-sin, cos, 0, 0]
     *   列2: [0, 0, 1, 0]          列3: [x, y, 0, 1]
     * </pre>
     * 矩阵以 FloatBuffer 形式传递给shader的 modelMatrix uniform。
     *
     * <h4>依赖API</h4>
     * <ul>
     *   <li>{@link BufferUtils#createFloatBuffer(int)} - 创建16元素FloatBuffer</li>
     * </ul>
     *
     * <h4>调用路径</h4>
     * 在render()方法中，glUseProgram()之后、drawVAOQuad()之前调用。
     * 返回的FloatBuffer直接传给 {@link GL20#glUniformMatrix4(int, boolean, FloatBuffer)}。
     * 调用者应缓存返回的FloatBuffer避免每帧重新分配。
     *
     * @param x           世界X坐标（平移分量）
     * @param y           世界Y坐标（平移分量）
     * @param angleDeg    旋转角度（度），0表示不旋转
     * @return 16元素FloatBuffer（列主序4x4矩阵），已flip可直接传递给glUniformMatrix4
     */
    public static FloatBuffer buildModelMatrix(float x, float y, float angleDeg) {
        float radians = (float) Math.toRadians(angleDeg);
        float sin = (float) Math.sin(radians);
        float cos = (float) Math.cos(radians);

        FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
        buffer.put(cos);    buffer.put(sin);    buffer.put(0f);     buffer.put(0f);
        buffer.put(-sin);   buffer.put(cos);    buffer.put(0f);     buffer.put(0f);
        buffer.put(0f);     buffer.put(0f);     buffer.put(1f);     buffer.put(0f);
        buffer.put(x);      buffer.put(y);      buffer.put(0f);     buffer.put(1f);
        buffer.flip();
        return buffer;
    }

    // ==================== 渲染管线 ====================

    /**
     * 绘制VAO矩形（4顶点GL_TRIANGLE_STRIP）。
     *
     * <h4>实现功能</h4>
     * 绑定指定VAO，执行 {@link GL11#glDrawArrays(int, int, int)} 绘制4个顶点，
     * 然后解绑VAO。混合模式和shader program由调用者自行管理。
     *
     * <h4>依赖API</h4>
     * <ul>
     *   <li>{@link GL30#glBindVertexArray(int)} - 绑定/解绑VAO</li>
     *   <li>{@link GL11#glDrawArrays(int, int, int)} - 绘制4顶点TRIANGLE_STRIP</li>
     * </ul>
     *
     * <h4>调用路径</h4>
     * 在render()方法中，shader和uniform设置完成后调用。
     * 调用者需自行管理混合模式(glEnable/glBlendFunc)和shader绑定(glUseProgram)。
     *
     * @param vaoId  VAO handle（来自 {@link #createUniversalRectVAO}）
     */
    public static void drawVAOQuad(int vaoId) {
        GL30.glBindVertexArray(vaoId);
        GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);
        GL30.glBindVertexArray(0);
    }

    // ==================== SSBO 管理 ====================

    /**
     * 创建SSBO（Shader Storage Buffer Object）缓冲区。
     *
     * <h4>实现功能</h4>
     * 调用 {@link GL15#glGenBuffers()} 生成一个SSBO handle，
     * 用于向shader传递任意大小的结构化数据（如事件列表、粒子数组等）。
     *
     * <h4>调用路径</h4>
     * 在渲染插件的init()中调用一次，返回的handle存储为成员变量。
     *
     * @return SSBO handle（正整数）
     */
    public static int createSSBO() {
        return GL15.glGenBuffers();
    }

    /**
     * 上传FloatBuffer数据到SSBO。
     *
     * <h4>实现功能</h4>
     * 绑定SSBO到 {@link GL43#GL_SHADER_STORAGE_BUFFER}，根据capacity判断：
     * <ul>
     *   <li>capacity &lt; floatCount: 调用 {@link GL15#glBufferData} 重新分配并上传</li>
     *   <li>capacity &gt;= floatCount: 调用 {@link GL15#glBufferSubData} 仅更新数据</li>
     * </ul>
     * 上传后不绑定到binding point，需额外调用 {@link #bindSSBO}。
     *
     * <h4>依赖API</h4>
     * <ul>
     *   <li>{@link GL15#glBindBuffer(int, int)} - 绑定SSBO</li>
     *   <li>{@link GL15#glBufferData(int, FloatBuffer, int)} - 分配并上传</li>
     *   <li>{@link GL15#glBufferSubData(int, long, FloatBuffer)} - 仅更新数据</li>
     * </ul>
     *
     * <h4>调用路径</h4>
     * 在每帧render()中，{@link #drawVAOQuad} 之前调用。
     * 调用者需维护capacity变量以跟踪SSBO当前分配大小。
     *
     * @param ssboId      SSBO handle
     * @param data        FloatBuffer数据（已flip）
     * @param floatCount  本次上传的float数量
     * @param capacity    SSBO当前分配容量（调用者维护），0表示首次上传
     * @return 更新后的capacity（若重新分配则等于floatCount，否则不变）
     */
    public static int uploadSSBO(int ssboId, FloatBuffer data, int floatCount, int capacity) {
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, ssboId);
        if (capacity < floatCount) {
            GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, data, GL15.GL_STREAM_DRAW);
            capacity = floatCount;
        } else {
            GL15.glBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0, data);
        }
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
        return capacity;
    }

    /**
     * 绑定SSBO到指定binding point。
     *
     * <h4>依赖API</h4>
     * {@link GL30#glBindBufferBase(int, int, int)}
     *
     * <h4>调用路径</h4>
     * 在 {@link #uploadSSBO} 之后、{@link #drawVAOQuad} 之前调用。
     * binding值对应shader中 {@code layout(std430, binding = N)} 的N。
     *
     * @param ssboId   SSBO handle
     * @param binding  binding point索引（如1、2、3...）
     */
    public static void bindSSBO(int ssboId, int binding) {
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, binding, ssboId);
    }

    /**
     * 解绑指定binding point上的SSBO。
     *
     * <h4>依赖API</h4>
     * {@link GL30#glBindBufferBase(int, int, int)}
     *
     * <h4>调用路径</h4>
     * 在 {@link #drawVAOQuad} 之后调用，恢复binding point为0（默认）。
     *
     * @param binding  binding point索引
     */
    public static void unbindSSBO(int binding) {
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, binding, 0);
    }

    /**
     * 删除SSBO缓冲区，释放GPU显存。
     *
     * <h4>依赖API</h4>
     * {@link GL15#glDeleteBuffers(int)}
     *
     * <h4>调用路径</h4>
     * 在渲染插件的 {@code cleanup()} 方法中调用。
     *
     * @param ssboId  SSBO handle（0=跳过）
     */
    public static void deleteSSBO(int ssboId) {
        if (ssboId != 0) GL15.glDeleteBuffers(ssboId);
    }

    // ==================== GPU 资源清理 ======================================

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
     * 删除VAO及其关联VBO，释放GPU资源。
     *
     * <h4>依赖API</h4>
     * <ul>
     *   <li>{@link GL30#glDeleteVertexArrays(int)} - 删除VAO</li>
     *   <li>{@link GL15#glDeleteBuffers(int)} - 删除VBO</li>
     * </ul>
     *
     * @param vao  VAOData对象（null=跳过）
     */
    public static void deleteVAO(VAOData vao) {
        if (vao == null) return;
        if (vao.vaoId > 0) GL30.glDeleteVertexArrays(vao.vaoId);
        if (vao.vboId > 0) GL15.glDeleteBuffers(vao.vboId);
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

    /**
     * 一次性清理 shader program + VAO + SSBO 全部GPU资源。
     *
     * <h4>调用路径</h4>
     * 在使用 {@link #createUniversalRectVAO()} 和 {@link #createSSBO()} 的渲染插件
     * 的 {@code cleanup()} 方法中调用。调用后应将成员变量置零。
     *
     * <h4>使用示例</h4>
     * <pre>
     *   public void cleanup() {
     *       ShaderUtil.cleanupAll(shaderProgram, vao, ssboId);
     *       shaderProgram = 0;
     *       vao = null;
     *       ssboId = 0;
     *   }
     * </pre>
     *
     * @param program  shader program handle
     * @param vao      VAOData对象（null=跳过）
     * @param ssboId   SSBO handle（0=跳过）
     */
    public static void cleanupAll(int program, VAOData vao, int ssboId) {
        deleteProgram(program);
        deleteVAO(vao);
        deleteSSBO(ssboId);
    }
}
