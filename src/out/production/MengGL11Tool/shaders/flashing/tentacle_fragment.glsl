
#version 110
varying vec2 v_uv;
uniform float u_time;
uniform float u_duration;
uniform float u_progress;

// Simplex noise function
vec4 permute(vec4 x) {
    return mod(((x * 34.0) + 1.0) * x, 289.0);
}

float snoise(vec2 v) {
    const vec4 C = vec4(0.211324865405187, 0.366025403784439,
    -0.577350269189626, 0.024390243902439);
    vec2 i  = floor(v + dot(v, C.yy));
    vec2 x0 = v - i + dot(i, C.xx);
    vec2 i1 = (x0.x > x0.y) ? vec2(1.0, 0.0) : vec2(0.0, 1.0);
    vec4 x12 = x0.xyxy + C.xxzz;
    x12.xy -= i1;
    i = mod(i, 289.0);
    vec4 p = permute(permute(i.y + vec4(0.0, i1.y, 1.0, i1.y))
    + i.x + vec4(0.0, i1.x, 1.0, i1.x));

    float n_ = 1.0 / 7.0;
    vec3 ns = n_ * vec3(1.0, 2.0, 0.0) - vec3(1.0/7.0, 0.0, 2.0/7.0);

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

    vec4 norm = 1.0 / vec4(dot(p0, p0), dot(p1, p1), dot(p2, p2), dot(p3, p3));
    p0 *= norm.x;
    p1 *= norm.y;
    p2 *= norm.z;
    p3 *= norm.w;

    vec4 m = max(0.6 - vec4(dot(x0, x0), dot(x12.xy, x12.xy), dot(x12.zw, x12.zw), 0.0), 0.0);
    m = m * m;
    return 42.0 * dot(m * m, vec4(dot(p0, vec3(x0, 0.0)), dot(p1, vec3(x12.xy, 0.0)), dot(p2, vec3(x12.zw, 0.0)), dot(p3, vec3(0.0))));
}

// Multi-octave noise for natural variation
float fbm(vec2 p, int octaves) {
    float value = 0.0;
    float amplitude = 0.5;
    float frequency = 1.0;

    for (int i = 0; i < octaves; i++) {
        value += amplitude * snoise(p * frequency);
        amplitude *= 0.5;
        frequency *= 2.0;
    }

    return value;
}

void main() {
    // UV coordinates: x along tentacle length, y across width
    float t = v_uv.x;  // 0 to 1 along length
    float w = v_uv.y;  // 0 to 1 across width (0=bottom, 1=top)

    // Multiple wave layers with different frequencies and phases
    float wave1 = sin(t * 8.0 + u_time * 2.0) * 0.15;
    float wave2 = sin(t * 13.0 - u_time * 1.5) * 0.1;
    float wave3 = sin(t * 21.0 + u_time * 3.0) * 0.05;

    // Noise-based distortion for organic movement
    vec2 noiseCoord = vec2(t * 5.0, u_time * 0.8);
    float noiseWave = fbm(noiseCoord, 4) * 0.2;

    // Combined wave displacement
    float totalWave = wave1 + wave2 + wave3 + noiseWave;

    // Width modulation along the tentacle (tapered at ends, variable in middle)
    float baseWidth = sin(t * 3.14159) * 0.3;  // Tapered shape
    float widthVariation = 1.0 + totalWave * 2.0;
    float currentWidth = baseWidth * widthVariation;

    // Distance from center line (accounting for wave displacement)
    float distFromCenter = abs(w - 0.5 - totalWave * 0.3);

    // Smooth edge falloff
    float edgeFade = smoothstep(currentWidth, currentWidth * 0.7, distFromCenter);

    // Longitudinal brightness variation (periodic bright spots)
    float longBrightness = pow(sin(t * 3.14159), 0.5);
    float periodicBright = pow(abs(sin(t * 15.0 + u_time * 2.0)), 0.3);

    // Flash effect - intermittent bright pulses
    float flashFreq = 3.0;
    float flash = pow(max(0.0, sin(u_time * flashFreq + t * 10.0)), 8.0) * 0.6;

    // Color gradient along the tentacle
    vec3 baseColor = vec3(0.6, 0.2, 0.9);  // Purple base
    vec3 highlightColor = vec3(1.0, 0.7, 1.0);  // Light purple/white highlight
    vec3 color = mix(baseColor, highlightColor, periodicBright);

    // Apply all effects
    float alpha = edgeFade * longBrightness * (0.7 + flash * 0.3);
    alpha *= (0.8 + periodicBright * 0.2);

    // Fade in/out based on progress
    if (u_progress < 0.1) {
        alpha *= u_progress / 0.1;
    } else if (u_progress > 0.9) {
        alpha *= (1.0 - u_progress) / 0.1;
    }

    gl_FragColor = vec4(color * alpha, alpha);
}
