#version 430
in vec2 v_uv;
out vec4 fragColor;
uniform float u_time;
vec4 permute(vec4 x) {
    return mod(((x * 34.0) + 1.0) * x, 289.0);
}

float snoise(vec3 v) {
    const vec2 C = vec2(1.0 / 6.0, 1.0 / 3.0);
    const vec4 D = vec4(0.0, 0.5, 1.0, 2.0);

    vec3 i  = floor(v + dot(v, C.yyy));
    vec3 x0 = v - i + dot(i, C.xxx);

    vec3 g = step(x0.yzx, x0.xyz);
    vec3 l = 1.0 - g;
    vec3 i1 = min(g.xyz, l.zxy);
    vec3 i2 = max(g.xyz, l.zxy);

    vec3 x1 = x0 - i1 + C.xxx;
    vec3 x2 = x0 - i2 + C.yyy;
    vec3 x3 = x0 - D.yyy;

    i = mod(i, 289.0);
    vec4 p = permute(permute(permute(
             i.z + vec4(0.0, i1.z, i2.z, 1.0))
           + i.y + vec4(0.0, i1.y, i2.y, 1.0))
           + i.x + vec4(0.0, i1.x, i2.x, 1.0));

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

    vec4 norm = 1.0 / vec4(dot(p0, p0), dot(p1, p1), dot(p2, p2), dot(p3, p3));
    p0 *= norm.x;
    p1 *= norm.y;
    p2 *= norm.z;
    p3 *= norm.w;

    vec4 m = max(0.6 - vec4(dot(x0, x0), dot(x1, x1), dot(x2, x2), dot(x3, x3)), 0.0);
    m = m * m;
    return 42.0 * dot(m * m, vec4(dot(p0, x0), dot(p1, x1), dot(p2, x2), dot(p3, x3)));
}
float turbulence(vec3 p) {
    float value = 0.0;
    float amplitude = 1.0;
    float frequency = 1.0;
    
    for (int i = 0; i < 5; i++) {
        value += amplitude * abs(snoise(p * frequency));
        amplitude *= 0.5;
        frequency *= 2.0;
    }
    
    return value * 0.5;
}

void main() {
    vec2 uv = vec2(v_uv.x,1.0-v_uv.y);
    
    vec2 center = vec2(0.5, 0.5);
    float dist = distance(uv, center);
    float maxDist = 0.7071;
    float normalizedDist = dist / maxDist;
    
    float innerRadius = 0.685;
    float fadeinRadius = 0.68;
    float outerRadius = 0.7;
    
    if (normalizedDist < fadeinRadius) discard;
    if (normalizedDist > outerRadius) discard;
    
    float fadevalue = smoothstep(fadeinRadius,innerRadius,normalizedDist);
    float ringProgress = max(0.0,(normalizedDist - innerRadius)) / (outerRadius - innerRadius);
    
    float dx = uv.x - 0.5;
    float dy = v_uv.y - 0.5;
    float angle= abs(atan(dx / dy));
    
    float scrollSpeed = 0.3;
    vec3 noiseCoord = vec3(angle * 40.0 , dist*42.0 - u_time * scrollSpeed, 0.0);
    
    float noiseValue = pow(turbulence(noiseCoord),1.0);
    
    float gradientValue = 1.0*(1.0-ringProgress);
    float totalalpha = fadevalue*(1.0-ringProgress);
    
    float dissolveThreshold = gradientValue * 0.7;
    
    float dissolveWidth = 0.3;     float dissolved = smoothstep(dissolveThreshold - dissolveWidth, 
                                 dissolveThreshold + dissolveWidth, 
                                 noiseValue);
    
    float coreProtection = smoothstep(innerRadius , innerRadius + 0.01, normalizedDist);
    float protectedDissolved =1.0 - dissolved * coreProtection;
    
    vec3 flameBaseColor = vec3(0.9, 0.75, 1.0);
    vec3 outerFlameColor = vec3(0.7, 0.4, 0.95);
    
    vec3 finalColor = mix(outerFlameColor, flameBaseColor, pow(protectedDissolved,8.0));
    
    float innerDist = abs(normalizedDist - innerRadius);
    float glowIntensity = totalalpha;
    vec3 glowColor = vec3(1.0, 0.95, 0.5);
    
    float alpha = totalalpha * protectedDissolved + glowIntensity * 0.3;
    
    fragColor = vec4(finalColor, alpha*0.4);
}
