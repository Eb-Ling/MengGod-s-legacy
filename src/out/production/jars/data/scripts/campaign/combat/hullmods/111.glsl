#version 330 core

uniform sampler2D _MainTex;      // 主纹理（场景渲染结果）
uniform sampler2D noiseTex;     // 噪声纹理（用于生成扰动）
uniform float time;             // 时间变量（用于动画效果）
uniform float distortionStrength; // 扭曲强度控制
uniform float speed;            // 动画速度
varying vec2 uv;                // 纹理坐标

// 随机噪声生成函数
float rand(vec2 coord) {
    return fract(sin(dot(coord, vec2(12.9898, 78.233))) * 43758.5453);
}

// 2D噪声函数（基于梯度噪声）
vec2 hash22(vec2 coord) {
    coord = vec2(dot(coord, vec2(127.1, 311.7)), dot(coord, vec2(269.5, 183.3))));
return -1.0 + 2.0 * fract(sin(coord)) * 43758.5453123);
}

// 改进的Perlin噪声函数
float noise(vec2 coord) {
    vec2 pi = floor(coord);
    vec2 pf = coord - pi;

    // 使用五次多项式缓和曲线实现平滑过渡
    vec2 w = pf * pf * pf * (pf * (6.0 * pf - 15.0) + 10.0);

    // 双线性插值计算四个角点的噪声值
    float aa = mix(dot(hash22(pi + vec2(0.0, 0.0)), pf),
                   dot(hash22(pi + vec2(1.0, 0.0)), pf - vec2(1.0, 0.0)), w.x);
    float bb = mix(dot(hash22(pi + vec2(0.0, 1.0)), pf - vec2(0.0, 1.0)),
                   dot(hash22(pi + vec2(1.0, 1.0)), pf - vec2(1.0, 1.0)), w.x);

    return mix(aa, bb, w.y);
}

// 分形布朗运动（FBM） - 多层噪声叠加
float fbm(vec2 coord) {
    float value = 0.0;
    float amplitude = 0.5;
    float frequency = 1.0;

    // 叠加4层噪声，每层频率加倍，振幅减半
    for(int i = 0; i < 4; i++) {
        value += amplitude * noise(frequency * coord);
        frequency *= 2.0;
        amplitude *= 0.5;
    }
    return value;
}

void main() {
    // 基础噪声采样（使用外部噪声纹理）
    vec4 noiseColor = texture2D(noiseTex, uv * 2.0 + time * 0.1);

    // 程序化噪声计算（增加动态变化）
    float proceduralNoise = fbm(uv * 3.0 + time * speed);

    // 结合外部噪声和程序化噪声
    float combinedNoise = (noiseColor.r + proceduralNoise) * 0.5;

    // 创建热蒸汽扭曲偏移量
    vec2 distortion = vec2(
    // X方向偏移：基于噪声和时间
    sin(combinedNoise * 10.0 + time * 3.0) * 0.01 * distortionStrength,
    // Y方向偏移：基于噪声和时间（相位略有不同）
    cos(combinedNoise * 8.0 + time * 2.5) * 0.008 * distortionStrength
    );

    // 应用扭曲效果到UV坐标
    vec2 distortedUV = uv + distortion;

    // 采样主纹理（应用扭曲后的坐标）
    vec4 sceneColor = texture2D(_MainTex, distortedUV);

    // 添加热蒸汽颜色效果（淡黄色调）
    vec3 steamColor = vec3(1.0, 0.9, 0.8);

    // 根据噪声强度混合场景颜色和蒸汽颜色
    float steamIntensity = combinedNoise * 0.3;
    vec3 finalColor = mix(sceneColor.rgb, steamColor, steamIntensity);

    // 输出最终颜色（保持原始透明度）
    gl_FragColor = vec4(finalColor, sceneColor.a);
}