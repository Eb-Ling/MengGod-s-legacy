#version 110

// ==================== 输入变量 ====================
varying vec2 v_uv;              // 从顶点着色器传来的纹理坐标 (0.0 ~ 1.0)
uniform sampler2D u_texture;    // 输入的纹理贴图
uniform float u_time;           // 累计时间（秒），用于动画控制

// ==================== Simplex 噪声函数 ====================

/**
 * 4维向量置换函数
 * 用于 Simplex 噪声的哈希计算，通过多项式变换打乱输入值
 */
vec4 permute(vec4 x) {
    return mod(((x * 34.0) + 1.0) * x, 289.0);
}

/**
 * 3D Simplex 噪声函数
 * 生成平滑的伪随机噪声值，范围约为 [-1, 1]
 * 
 * @param v 3D 输入坐标
 * @return 噪声值
 */
float snoise(vec3 v) {
    const vec2 C = vec2(1.0 / 6.0, 1.0 / 3.0);
    const vec4 D = vec4(0.0, 0.5, 1.0, 2.0);

    // 第一步：找到包含输入点的单形（simplex）的整数坐标
    vec3 i  = floor(v + dot(v, C.yyy));
    vec3 x0 = v - i + dot(i, C.xxx);

    // 第二步：确定单形中顶点的相对顺序
    vec3 g = step(x0.yzx, x0.xyz);
    vec3 l = 1.0 - g;
    vec3 i1 = min(g.xyz, l.zxy);
    vec3 i2 = max(g.xyz, l.zxy);

    // 第三步：计算到四个顶点的偏移量
    vec3 x1 = x0 - i1 + C.xxx;
    vec3 x2 = x0 - i2 + C.yyy;
    vec3 x3 = x0 - D.yyy;

    // 第四步：哈希化顶点索引以获取梯度
    i = mod(i, 289.0);
    vec4 p = permute(permute(permute(
             i.z + vec4(0.0, i1.z, i2.z, 1.0))
           + i.y + vec4(0.0, i1.y, i2.y, 1.0))
           + i.x + vec4(0.0, i1.x, i2.x, 1.0));

    // 第五步：计算梯度向量和贡献值
    float n_ = 0.142857142857;
    vec3 ns = n_ * D.wyz - D.xzx;

    vec4 j = p - 49.0 * floor(p * ns.z * ns.z);

    vec4 x_ = floor(j * ns.z);
    vec4 y_ = floor(j - 7.0 * x_);

    vec4 x = x_ * ns.x + ns.yyyy;
    vec4 y = y_ * ns.x + ns.yyyy;
    vec4 h = 1.0 - abs(x) - abs(y);

    vec4 b0 = vec4(x.xy, y.xy);
    vec4 b1 = vec4(x.zw, y.zw);

    vec4 s0 = floor(b0) * 2.0 + 1.0;
    vec4 s1 = floor(b1) * 2.0 + 1.0;
    vec4 sh = -step(h, vec4(0.0));

    vec4 a0 = b0.xzyw + s0.xzyw * sh.xxyy;
    vec4 a1 = b1.xzyw + s1.xzyw * sh.zzww;

    vec3 p0 = vec3(a0.xy, h.x);
    vec3 p1 = vec3(a0.zw, h.y);
    vec3 p2 = vec3(a1.xy, h.z);
    vec3 p3 = vec3(a1.zw, h.w);

    // 第六步：归一化梯度向量并计算最终噪声值
    vec4 norm = 1.0 / vec4(dot(p0, p0), dot(p1, p1), dot(p2, p2), dot(p3, p3));
    p0 *= norm.x;
    p1 *= norm.y;
    p2 *= norm.z;
    p3 *= norm.w;

    vec4 m = max(0.6 - vec4(dot(x0, x0), dot(x1, x1), dot(x2, x2), dot(x3, x3)), 0.0);
    m = m * m;
    return 42.0 * dot(m * m, vec4(dot(p0, x0), dot(p1, x1), dot(p2, x2), dot(p3, x3)));
}

/**
 * 湍流噪声函数（分形布朗运动 FBM）
 * 通过多层噪声叠加产生更复杂的自然纹理
 * 
 * @param p 3D 输入坐标
 * @return 湍流噪声值 (0.0 ~ 1.0)
 */
float turbulence(vec3 p) {
    float value = 0.0;
    float amplitude = 1.0;    // 振幅（每层减半）
    float frequency = 1.0;    // 频率（每层翻倍）
    
    // 叠加 5 层不同频率和振幅的噪声
    for (int i = 0; i < 5; i++) {
        value += amplitude * abs(snoise(p * frequency));
        amplitude *= 0.5;     // 振幅递减
        frequency *= 2.0;     // 频率递增
    }
    
    return value * 0.5;       // 归一化到合理范围
}

// ==================== 主函数 ====================

void main() {
    vec2 uv = v_uv;
    vec2 center = vec2(0.5, 0.5);   // 纹理中心点
    
    // --- 可调节参数 ---
    float u_noiseScale = 15.0;      // 噪声缩放系数（越大细节越密集）
    float u_edgeWidth = 0.3;        // 燃烧边缘宽度
    float u_burnProgress = clamp(u_time / 8.0, 0.0, 1.0);  // 全局燃烧进度（8秒完成）
    
    // --- 步骤 1: 计算距离场 ---
    float dist = distance(uv, center);          // 当前像素到中心的距离
    float maxDist = 0.7071;                      // 最大可能距离（√0.5，对角线的一半）
    float normalizedDist = dist / maxDist;       // 归一化距离 (0.0 ~ 1.0)
    
    // --- 步骤 2: 计算局部燃烧进度 ---
    // 距离中心越远的地方越早开始燃烧（* 0.5 控制扩散速度）
    // * 3.0 加速过渡过程
    float localBurnProgress = clamp((u_burnProgress - normalizedDist * 0.5) * 3.0, 0.0, 1.0);
    
    // --- 步骤 3: 生成静态噪声阈值 ---
    // 使用 2D 噪声作为基础阈值，创造不规则的燃烧边界
    float staticNoise = turbulence(vec3(uv * u_noiseScale, 0.0));
    float threshold = staticNoise * 0.6 + 0.2;   // 阈值范围 [0.2, 0.8]
    
    // --- 步骤 4: 生成动态噪声（随时间变化）---
    // 第三维使用时间，使噪声图案流动
    float dynamicNoise = turbulence(vec3(uv * u_noiseScale * 1.3, u_time * 0.25));
    
    // --- 步骤 5: 计算燃烧状态 ---
    // burned: 已完全燃烧的区域（平滑过渡）
    float burned = smoothstep(threshold - u_edgeWidth, threshold + 0.1, localBurnProgress);
    
    // edge: 正在燃烧的边缘区域（两个 smoothstep 相减得到带状区域）
    float edge = smoothstep(threshold - u_edgeWidth * 0.8, threshold, localBurnProgress)
               - smoothstep(threshold, threshold + u_edgeWidth * 0.6, localBurnProgress);
    edge = max(0.0, edge);   // 确保非负
    
    // --- 步骤 6: 计算火焰扰动偏移 ---
    // 基于动态噪声生成随机方向的偏移向量
    float angle = dynamicNoise * 6.28318;                          // 随机角度 (0 ~ 2π)
    float speed = 0.15 + dynamicNoise * 0.5;                       // 随机速度
    vec2 offset = vec2(cos(angle), sin(angle)) * (localBurnProgress - threshold) * speed;
    offset *= smoothstep(threshold, threshold + 0.25, localBurnProgress);  // 仅在边缘生效
    
    // --- 步骤 7: 采样纹理（应用扰动）---
    vec2 sampleUV = uv + offset * burned;   // 已燃烧区域产生扭曲效果
    vec4 texColor = texture2D(u_texture, sampleUV);
    
    // --- 步骤 8: 计算火焰颜色渐变 ---
    vec3 fireColor;
    if (edge < 0.33) {
        // 阶段 1: 亮黄色 → 橙色（高温区）
        fireColor = mix(vec3(1.0, 1.0, 0.0), vec3(1.0, 0.3, 0.0), edge / 0.33);
    } else if (edge < 0.6) {
        // 阶段 2: 橙色 → 暗红色（中温区）
        fireColor = mix(vec3(1.0, 0.3, 0.0), vec3(0.6, 0.0, 0.0), (edge - 0.33) / 0.27);
    } else {
        // 阶段 3: 暗红色 → 黑色（低温区/余烬）
        fireColor = mix(vec3(0.6, 0.0, 0.0), vec3(0.0, 0.0, 0.0), (edge - 0.6) / 0.4);
    }
    
    // 将火焰颜色混合到原始纹理上（边缘区域 85% 强度）
    texColor.rgb = mix(texColor.rgb, fireColor, edge * 0.85);
    
    // --- 步骤 9: 计算透明度并输出 ---
    float alpha = texColor.a * (1.0 - burned);  // 已燃烧区域逐渐透明
    if (alpha < 0.01) discard;                  // 丢弃几乎不可见的像素（性能优化）
    
    gl_FragColor = vec4(texColor.rgb, alpha);
}
