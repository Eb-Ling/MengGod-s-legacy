package data.methods.shaders;

/**
 * GLSLSnippets - GLSL着色器可复用代码片段库。
 *
 * <h3>功能概述</h3>
 * 将项目中所有渲染效果类（含GL11Tool）里高复用性的GLSL函数提取为静态String常量，
 * 按功能分为9个类别：顶点着色器、哈希函数、值噪声、Simplex噪声、湍流、Worley噪声、
 * SDF符号距离场、渐变与锥度、曲线。
 * 使用时通过字符串拼接将所需片段插入fragment shader源码中。
 *
 * <h3>分类索引</h3>
 * <ul>
 *   <li>1. 顶点着色器: {@link #VERTEX_SHADER_STANDARD}</li>
 *   <li>2. 哈希函数: {@link #HASH_2D}, {@link #HASH2_2D}</li>
 *   <li>3. 值噪声: {@link #VNOISE_2D}, {@link #FBM_VNOISE_2D}</li>
 *   <li>4. Simplex噪声: {@link #PERMUTE}, {@link #SNOISE_3D}, {@link #SNOISE_2D}, {@link #FBM_SNOISE_2D}</li>
 *   <li>5. 湍流: {@link #TURBULENCE_3D}</li>
 *   <li>6. Worley噪声: {@link #WORLEY_NOISE_2D}, {@link #FBM_WORLEY_2D}</li>
 *   <li>7. SDF符号距离场: {@link #SDF_LANCE}, {@link #SEG_DIST}</li>
 *   <li>8. 渐变与锥度: {@link #TAPER_LANCE}, {@link #XFADE}, {@link #FADE_VERTICAL}, {@link #FADE_HORIZONTAL}</li>
 *   <li>9. 曲线: {@link #CUBIC_BEZIER}, {@link #QUARTIC_BEZIER}</li>
 * </ul>
 *
 * <h3>依赖关系图</h3>
 * <pre>
 *   HASH_2D ──→ VNOISE_2D ──→ FBM_VNOISE_2D
 *   HASH2_2D ──→ WORLEY_NOISE_2D ──→ FBM_WORLEY_2D
 *   PERMUTE ──→ SNOISE_3D ──→ TURBULENCE_3D
 *   PERMUTE ──→ SNOISE_2D ──→ FBM_SNOISE_2D
 *   TAPER_LANCE ──→ SDF_LANCE
 * </pre>
 *
 * <h3>使用规范</h3>
 * <ul>
 *   <li>所有GLSL字符串不含 #version 前缀和 uniform/varying 声明，由使用者按需添加</li>
 *   <li>所有函数均为完整独立的GLSL函数体，可直接拼接到fragment shader源码中</li>
 *   <li>使用有依赖关系的片段时，需将依赖函数声明在调用者之前</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>
 *   String fragmentSource =
 *       "#version 110\n" +
 *       "varying vec2 v_uv;\n" +
 *       "uniform float u_time;\n" +
 *       "\n" +
 *       GLSLSnippets.HASH_2D +
 *       GLSLSnippets.VNOISE_2D +
 *       GLSLSnippets.FBM_VNOISE_2D +
 *       "\n" +
 *       "void main() {\n" +
 *       "    float n = fbm(v_uv * 5.0 + u_time * 0.1);\n" +
 *       "    gl_FragColor = vec4(vec3(n), 1.0);\n" +
 *       "}\n";
 * </pre>
 *
 * <h3>提取来源</h3>
 * <ul>
 *   <li>Simplex噪声/湍流: Phasecore, MingGodCenter, BezierCurveEffect, FlameRiseEffect, LightTextureRenderEffect, TextureRenderEffect, FlashingTentacleEffect</li>
 *   <li>值噪声/FBM: LightLanceEffect</li>
 *   <li>Worley噪声: WaterRippleEffect</li>
 *   <li>SDF/渐变: LightLanceEffect</li>
 *   <li>曲线: KabbalahLifeTreeEffect, BezierCurveEffect</li>
 * </ul>
 */
public class GLSLSnippets {

    // ==================== 1. 顶点着色器 ====================

    /**
     * 标准四边形顶点着色器（完整shader，含 #version 420 声明）。
     *
     * <p>采用BoxUtil现代管线方案：通过BoxUtil的UBO {@code BUtilGlobalData} (binding=0)
     * 获取 {@code gameViewport} 视口矩阵，通过 {@code uniform mat4 modelMatrix} 传递
     * 4x4列主序模型变换矩阵（旋转+平移），通过 {@code uniform vec2 size} 控制渲染区域大小。</p>
     *
     * <p>顶点数据来自 {@link ShaderUtil#createUniversalRectVAO()} 创建的byte顶点
     * [-128,-128]~[127,127]，经 GL_BYTE normalized 归一化到约 [-1, 1] 后乘以 size*0.5 缩放，
     * 再由 modelMatrix 变换到世界坐标，最后由 gameViewport 映射到裁剪空间。</p>
     *
     * <p>UV坐标由顶点位置自动计算: {@code v_uv = (position + 1.0) * 0.5}，
     * 将 [-1,1] 映射到 [0,1]，无需 a_texCoord attribute。</p>
     *
     * <p>消费方需在render()中设置以下uniform:</p>
     * <ul>
     *   <li>{@code modelMatrix} - 通过 {@link ShaderUtil#buildModelMatrix(float, float, float)} 构建</li>
     *   <li>{@code size} - vec2，渲染区域的全宽高（世界坐标单位，即直径而非半径），通过 glUniform2f 设置。例如护盾半径为R时，应传入 R*2</li>
     * </ul>
     *
     * <p>与 {@link ShaderUtil#createUniversalRectVAO()} 创建的VAO布局和
     * {@link ShaderUtil#createShaderProgram} 绑定的 a_position(location=0) 对应。</p>
     */
    public static final String VERTEX_SHADER_STANDARD =
            "#version 420\n" +
            "layout (location = 0) in vec2 a_position;\n" +
            "layout (std140, binding = 0) uniform BUtilGlobalData\n" +
            "{\n" +
            "    mat4 gameViewport;\n" +
            "    vec4 gameScreenBorder;\n" +
            "};\n" +
            "uniform mat4 modelMatrix;\n" +
            "uniform vec2 size;\n" +
            "out vec2 v_uv;\n" +
            "void main() {\n" +
            "    v_uv = (a_position + 1.0) * 0.5;\n" +
            "    vec4 resized = vec4(a_position * size * 0.5, 0.0, 1.0);\n" +
            "    gl_Position = gameViewport * vec4((modelMatrix * resized).xy, 0.0, 1.0);\n" +
            "}\n";

    // ==================== 2. 哈希函数 ====================

    /**
     * 2D伪随机标量哈希函数。
     *
     * <p>GLSL签名: {@code float hash(vec2 p)}</p>
     * <p>基于 sin + dot 的经典哈希，输入2D坐标返回 [0,1) 范围的伪随机值。
     * 使用大素数 (127.1, 311.7) 和放大因子 43758.5453 产生充分混沌的分布。</p>
     *
     * <p>依赖: 无</p>
     * <p>被依赖: {@link #VNOISE_2D}</p>
     * <p>来源: LightLanceEffect</p>
     */
    public static final String HASH_2D =
            "float hash(vec2 p) {\n" +
            "    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);\n" +
            "}\n";

    /**
     * 2D伪随机向量哈希函数。
     *
     * <p>GLSL签名: {@code vec2 hash2(vec2 p)}</p>
     * <p>返回2D伪随机向量，各分量独立分布于 [0,1)。
     * 使用不同的素数组合 (0.1031, 0.1030, 0.0973) 产生与 HASH_2D 不同分布的随机向量。</p>
     *
     * <p>依赖: 无</p>
     * <p>被依赖: {@link #WORLEY_NOISE_2D}</p>
     * <p>来源: WaterRippleEffect</p>
     */
    public static final String HASH2_2D =
            "vec2 hash2(vec2 p) {\n" +
            "    vec3 p3 = fract(vec3(p.xyx) * vec3(0.1031, 0.1030, 0.0973));\n" +
            "    p3 += dot(p3, p3.yzx + 33.33);\n" +
            "    return fract((p3.xx + p3.yz) * p3.zy);\n" +
            "}\n";

    // ==================== 3. 值噪声 ====================

    /**
     * 2D值噪声函数（Hermite平滑插值）。
     *
     * <p>GLSL签名: {@code float vnoise(vec2 p)}</p>
     * <p>在整数网格点上采样 {@link #HASH_2D} 哈希值，
     * 使用 Hermite 平滑因子 f*f*(3-2f) 进行双线性插值，
     * 返回连续平滑的 [0,1) 噪声值。比纯哈希噪声更平滑，适合地形/纹理生成。</p>
     *
     * <p>依赖: {@link #HASH_2D}（需在其之前声明）</p>
     * <p>被依赖: {@link #FBM_VNOISE_2D}</p>
     * <p>来源: LightLanceEffect</p>
     */
    public static final String VNOISE_2D =
            "float vnoise(vec2 p) {\n" +
            "    vec2 i = floor(p);\n" +
            "    vec2 f = fract(p);\n" +
            "    f = f * f * (3.0 - 2.0 * f);\n" +
            "    float a = hash(i);\n" +
            "    float b = hash(i + vec2(1.0, 0.0));\n" +
            "    float c = hash(i + vec2(0.0, 1.0));\n" +
            "    float d = hash(i + vec2(1.0, 1.0));\n" +
            "    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);\n" +
            "}\n";

    /**
     * 基于值噪声的分形布朗运动（4层叠加）。
     *
     * <p>GLSL签名: {@code float fbm(vec2 p)}</p>
     * <p>4次octave叠加 {@link #VNOISE_2D}，每层频率翻倍、振幅减半。
     * 起始振幅0.5，最终值域约 [0, 1.0)。
     * 产生多尺度的自然纹理，适合火焰、云层、地形等效果。</p>
     *
     * <p>依赖: {@link #HASH_2D} → {@link #VNOISE_2D}（需按顺序声明）</p>
     * <p>来源: LightLanceEffect</p>
     */
    public static final String FBM_VNOISE_2D =
            "float fbm(vec2 p) {\n" +
            "    float v = 0.0;\n" +
            "    float a = 0.5;\n" +
            "    for (int i = 0; i < 4; i++) {\n" +
            "        v += a * vnoise(p);\n" +
            "        p *= 2.0;\n" +
            "        a *= 0.5;\n" +
            "    }\n" +
            "    return v;\n" +
            "}\n";

    // ==================== 4. Simplex 噪声 ====================

    /**
     * Simplex噪声核心置换函数。
     *
     * <p>GLSL签名: {@code vec4 permute(vec4 x)}</p>
     * <p>基于二次多项式的模运算置换: mod(((x*34+1)*x), 289)。
     * 产生良好的伪随机排列，是 Simplex 噪声的核心组件。</p>
     *
     * <p>依赖: 无</p>
     * <p>被依赖: {@link #SNOISE_3D}, {@link #SNOISE_2D}, {@link #TURBULENCE_3D}</p>
     * <p>来源: Ashima Arts (Stefan Gustavson)，7个文件复用</p>
     */
    public static final String PERMUTE =
            "vec4 permute(vec4 x) {\n" +
            "    return mod(((x * 34.0) + 1.0) * x, 289.0);\n" +
            "}\n";

    /**
     * Simplex 3D 噪声函数（Ashima Arts 经典实现）。
     *
     * <p>GLSL签名: {@code float snoise(vec3 v)}</p>
     * <p>返回 [-1, 1] 范围的3D Simplex噪声值。
     * 使用skewing方法将3D空间划分为单纯形网格，
     * 在4个最近网格点上采样梯度并加权求和。
     * 项目中复用率最高的噪声函数（7+文件）。</p>
     *
     * <p>依赖: {@link #PERMUTE}（需在其之前声明）</p>
     * <p>被依赖: {@link #TURBULENCE_3D}</p>
     * <p>来源: Ashima Arts, Phasecore/MingGodCenter/TextureRenderEffect/LightTextureRenderEffect/FlameRiseEffect/BezierCurveEffect/FlashingTentacleEffect</p>
     */
    public static final String SNOISE_3D =
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
            "}\n";

    /**
     * Simplex 2D 噪声函数（Ashima Arts 2D变体）。
     *
     * <p>GLSL签名: {@code float snoise(vec2 v)}</p>
     * <p>返回 [-1, 1] 范围的2D Simplex噪声值。
     * 与 {@link #SNOISE_3D} 使用相同的置换策略但针对2D输入优化。
     * 适用于平面纹理生成、2D流动效果等场景。</p>
     *
     * <p>依赖: {@link #PERMUTE}（需在其之前声明）</p>
     * <p>被依赖: {@link #FBM_SNOISE_2D}</p>
     * <p>来源: tentacle_fragment.glsl</p>
     */
    public static final String SNOISE_2D =
            "float snoise(vec2 v) {\n" +
            "    const vec4 C = vec4(0.211324865405187, 0.366025403784439,\n" +
            "    -0.577350269189626, 0.024390243902439);\n" +
            "    vec2 i  = floor(v + dot(v, C.yy));\n" +
            "    vec2 x0 = v - i + dot(i, C.xx);\n" +
            "    vec2 i1 = (x0.x > x0.y) ? vec2(1.0, 0.0) : vec2(0.0, 1.0);\n" +
            "    vec4 x12 = x0.xyxy + C.xxzz;\n" +
            "    x12.xy -= i1;\n" +
            "    i = mod(i, 289.0);\n" +
            "    vec4 p = permute(permute(i.y + vec4(0.0, i1.y, 1.0, i1.y))\n" +
            "    + i.x + vec4(0.0, i1.x, 1.0, i1.x));\n" +
            "\n" +
            "    float n_ = 1.0 / 7.0;\n" +
            "    vec3 ns = n_ * vec3(1.0, 2.0, 0.0) - vec3(1.0/7.0, 0.0, 2.0/7.0);\n" +
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
            "    vec4 m = max(0.6 - vec4(dot(x0, x0), dot(x12.xy, x12.xy), dot(x12.zw, x12.zw), 0.0), 0.0);\n" +
            "    m = m * m;\n" +
            "    return 42.0 * dot(m * m, vec4(dot(p0, vec3(x0, 0.0)), dot(p1, vec3(x12.xy, 0.0)), dot(p2, vec3(x12.zw, 0.0)), dot(p3, vec3(0.0))));\n" +
            "}\n";

    /**
     * 基于Simplex 2D噪声的可配置层数分形布朗运动。
     *
     * <p>GLSL签名: {@code float fbm(vec2 p, int octaves)}</p>
     * <p>octaves次octave叠加 {@link #SNOISE_2D}，每层频率翻倍、振幅减半。
     * 起始振幅0.5，起始频率1.0。层数由参数控制，灵活适应不同精度需求。</p>
     *
     * <p>依赖: {@link #PERMUTE} → {@link #SNOISE_2D}（需按顺序声明）</p>
     * <p>来源: tentacle_fragment.glsl</p>
     */
    public static final String FBM_SNOISE_2D =
            "float fbm(vec2 p, int octaves) {\n" +
            "    float value = 0.0;\n" +
            "    float amplitude = 0.5;\n" +
            "    float frequency = 1.0;\n" +
            "    for (int i = 0; i < octaves; i++) {\n" +
            "        value += amplitude * snoise(p * frequency);\n" +
            "        amplitude *= 0.5;\n" +
            "        frequency *= 2.0;\n" +
            "    }\n" +
            "    return value;\n" +
            "}\n";

    // ==================== 5. 湍流 ====================

    /**
     * 3D湍流函数（6层Simplex噪声叠加）。
     *
     * <p>GLSL签名: {@code float turbulence(vec3 p)}</p>
     * <p>6次octave叠加 abs({@link #SNOISE_3D})，每层频率翻倍、振幅减半。
     * 使用绝对值使噪声始终为正，产生尖锐的湍流纹理（而非平滑的FBM）。
     * 最终乘以0.5归一化，值域约 [0, 1.0)。</p>
     *
     * <p>依赖: {@link #PERMUTE} → {@link #SNOISE_3D}（需按顺序声明）</p>
     * <p>来源: Phasecore/MingGodCenter/BezierCurveEffect/TextureRenderEffect/LightTextureRenderEffect/FlameRiseEffect (6文件复用)</p>
     */
    public static final String TURBULENCE_3D =
            "float turbulence(vec3 p) {\n" +
            "    float value = 0.0;\n" +
            "    float amplitude = 1.0;\n" +
            "    float frequency = 1.0;\n" +
            "    for (int i = 0; i < 6; i++) {\n" +
            "        value += amplitude * abs(snoise(p * frequency));\n" +
            "        amplitude *= 0.5;\n" +
            "        frequency *= 2.0;\n" +
            "    }\n" +
            "    return value * 0.5;\n" +
            "}\n";

    // ==================== 6. Worley 噪声 ====================

    /**
     * 2D Worley噪声（细胞噪声/Voronoi距离）。
     *
     * <p>GLSL签名: {@code float worleyNoise(vec2 st)}</p>
     * <p>在3x3邻域内搜索最近的随机特征点（由 {@link #HASH2_2D} 生成），
     * 返回到最近特征点的欧几里得距离。产生细胞状/气泡状纹理，
     * 适合水波纹、岩石表面、生物组织等效果。</p>
     *
     * <p>依赖: {@link #HASH2_2D}（需在其之前声明）</p>
     * <p>被依赖: {@link #FBM_WORLEY_2D}</p>
     * <p>来源: WaterRippleEffect</p>
     */
    public static final String WORLEY_NOISE_2D =
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
            "}\n";

    /**
     * 基于Worley噪声的分形叠加（5层）。
     *
     * <p>GLSL签名: {@code float fbmWorley(vec2 p)}</p>
     * <p>5次octave叠加 {@link #WORLEY_NOISE_2D}，起始频率6.0，每层频率翻倍、振幅减半。
     * 高频起始使纹理更细腻，适合水面、液体表面等复杂纹理。</p>
     *
     * <p>依赖: {@link #HASH2_2D} → {@link #WORLEY_NOISE_2D}（需按顺序声明）</p>
     * <p>来源: WaterRippleEffect</p>
     */
    public static final String FBM_WORLEY_2D =
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
            "}\n";

    // ==================== 7. SDF 符号距离场 ====================

    /**
     * 光矛形状SDF（Signed Distance Field）。
     *
     * <p>GLSL签名: {@code float sdLance(vec2 p, float baseX, float tipX, float maxR)}</p>
     * <p>计算点p到光矛轮廓的有符号距离。光矛从baseX延伸到tipX，
     * 最大半径maxR在 {@link #TAPER_LANCE} 控制下从根部到尖端渐变。
     * 负值在形状内部，正值在外部，用于光矛多层渲染。</p>
     *
     * <p>依赖: {@link #TAPER_LANCE}（需在其之前声明）</p>
     * <p>来源: LightLanceEffect</p>
     */
    public static final String SDF_LANCE =
            "float sdLance(vec2 p, float baseX, float tipX, float maxR) {\n" +
            "    float len = tipX - baseX;\n" +
            "    float t = clamp((p.x - baseX) / len, 0.0, 1.0);\n" +
            "    float r = maxR * lanceTaper(t);\n" +
            "    return abs(p.y) - r;\n" +
            "}\n";

    /**
     * 点到线段的距离函数。
     *
     * <p>GLSL签名: {@code float segDist(vec2 p, vec2 a, vec2 b)}</p>
     * <p>计算点p到线段ab的最短距离。使用投影+clamp方法：
     * 将p投影到ab直线上，clamp到[0,1]范围内，然后计算距离。</p>
     *
     * <p>依赖: 无</p>
     * <p>来源: KabbalahLifeTreeEffect</p>
     */
    public static final String SEG_DIST =
            "float segDist(vec2 p, vec2 a, vec2 b) {\n" +
            "    vec2 pa = p - a;\n" +
            "    vec2 ba = b - a;\n" +
            "    float h = clamp(dot(pa, ba) / max(dot(ba, ba), 0.0001), 0.0, 1.0);\n" +
            "    return length(pa - ba * h);\n" +
            "}\n";

    // ==================== 8. 渐变与锥度 ====================

    /**
     * 光矛非对称锥度函数。
     *
     * <p>GLSL签名: {@code float lanceTaper(float t)}</p>
     * <p>输入 t 在 [0,1] 范围内（0=根部，1=尖端），输出形状宽度系数。
     * 快速起始(smoothstep 0~0.08) + 长尾衰减(1-smoothstep 0.15~1.0)，
     * 产生锐利的矛尖造型。</p>
     *
     * <p>依赖: 无</p>
     * <p>被依赖: {@link #SDF_LANCE}</p>
     * <p>来源: LightLanceEffect</p>
     */
    public static final String TAPER_LANCE =
            "float lanceTaper(float t) {\n" +
            "    t = clamp(t, 0.0, 1.0);\n" +
            "    return smoothstep(0.0, 0.08, t) * (1.0 - smoothstep(0.15, 1.0, t));\n" +
            "}\n";

    /**
     * 横向两端smoothstep渐出函数。
     *
     * <p>GLSL签名: {@code float xFade(vec2 p, float baseX, float tipX)}</p>
     * <p>在baseX和tipX两端各8像素宽度的smoothstep渐变带内平滑过渡到0，
     * 中间区域为1。用于光矛边缘柔化，避免硬边。</p>
     *
     * <p>依赖: 无</p>
     * <p>来源: LightLanceEffect</p>
     */
    public static final String XFADE =
            "float xFade(vec2 p, float baseX, float tipX) {\n" +
            "    float fb = smoothstep(baseX - 8.0, baseX + 8.0, p.x);\n" +
            "    float ft = smoothstep(tipX + 8.0, tipX - 8.0, p.x);\n" +
            "    return fb * ft;\n" +
            "}\n";

    /**
     * 纵向smoothstep渐变函数。
     *
     * <p>GLSL签名: {@code float fadeVertical(float v, float edge0, float edge1)}</p>
     * <p>在 [edge0, edge1] 范围内从0平滑过渡到1。
     * 适用于UV.y方向的渐变效果，如从上到下的透明度渐变。</p>
     *
     * <p>依赖: 无</p>
     */
    public static final String FADE_VERTICAL =
            "float fadeVertical(float v, float edge0, float edge1) {\n" +
            "    return smoothstep(edge0, edge1, v);\n" +
            "}\n";

    /**
     * 横向smoothstep渐变函数。
     *
     * <p>GLSL签名: {@code float fadeHorizontal(float u, float edge0, float edge1)}</p>
     * <p>在 [edge0, edge1] 范围内从0平滑过渡到1。
     * 适用于UV.x方向的渐变效果，如从左到右的亮度渐变。</p>
     *
     * <p>依赖: 无</p>
     */
    public static final String FADE_HORIZONTAL =
            "float fadeHorizontal(float u, float edge0, float edge1) {\n" +
            "    return smoothstep(edge0, edge1, u);\n" +
            "}\n";

    // ==================== 9. 曲线 ====================

    /**
     * 三次贝塞尔曲线插值函数。
     *
     * <p>GLSL签名: {@code vec2 cubic(vec2 p0, vec2 p1, vec2 p2, vec2 p3, float t)}</p>
     * <p>给定4个2D控制点 p0~p3 和参数 t(0~1)，返回曲线上的2D点坐标。
     * 使用 de Casteljau 展开形式计算，适用于路径绘制、曲线边框等。</p>
     *
     * <p>依赖: 无</p>
     * <p>来源: KabbalahLifeTreeEffect</p>
     */
    public static final String CUBIC_BEZIER =
            "vec2 cubic(vec2 p0, vec2 p1, vec2 p2, vec2 p3, float t) {\n" +
            "    float u = 1.0 - t;\n" +
            "    return u*u*u*p0 + 3.0*u*u*t*p1 + 3.0*u*t*t*p2 + t*t*t*p3;\n" +
            "}\n";

    /**
     * 四次贝塞尔曲线Y值插值函数。
     *
     * <p>GLSL签名: {@code float bezier4(vec2 p0, vec2 p1, vec2 p2, vec2 p3, vec2 p4, float t)}</p>
     * <p>给定5个2D控制点和参数 t(0~1)，返回曲线上的Y坐标值。
     * 常用于将X坐标映射为Y值的场景（如光束弯曲、路径变形）。</p>
     *
     * <p>依赖: 无</p>
     * <p>来源: BezierCurveEffect</p>
     */
    public static final String QUARTIC_BEZIER =
            "float bezier4(vec2 p0, vec2 p1, vec2 p2, vec2 p3, vec2 p4, float t) {\n" +
            "    float u = 1.0 - t;\n" +
            "    float tt = t * t;\n" +
            "    float uu = u * u;\n" +
            "    float uuu = uu * u;\n" +
            "    float uuuu = uuu * u;\n" +
            "    float ttt = tt * t;\n" +
            "    float tttt = ttt * t;\n" +
            "    vec2 point = uuuu * p0 + 4.0 * uuu * t * p1 + 6.0 * uu * tt * p2 + 4.0 * u * ttt * p3 + tttt * p4;\n" +
            "    return point.y;\n" +
            "}\n";
}
