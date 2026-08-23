#version 110

varying vec2 v_uv;
uniform float u_time;
uniform float u_duration;
uniform float u_progress;

float lanceTaper(float t) {
    t = clamp(t, 0.0, 1.0);
    return smoothstep(0.0, 0.08, t) * (1.0 - smoothstep(0.15, 1.0, t));
}

float sdLance(vec2 p, float baseX, float tipX, float maxR) {
    float len = tipX - baseX;
    float t = clamp((p.x - baseX) / len, 0.0, 1.0);
    float r = maxR * lanceTaper(t);
    return abs(p.y) - r;
}

float xFade(vec2 p, float baseX, float tipX) {
    float fb = smoothstep(baseX - 8.0, baseX + 8.0, p.x);
    float ft = smoothstep(tipX + 8.0, tipX - 8.0, p.x);
    return fb * ft;
}

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float vnoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 4; i++) {
        v += a * vnoise(p);
        p *= 2.0;
        a *= 0.5;
    }
    return v;
}

void main() {
    vec2 p = (v_uv - 0.5) * vec2(400.0, 80.0);
    vec2 cp = vec2(p.x, p.y);

    float pr = u_progress;
    float baseX = -170.0;
    float finalTip = 160.0;

    float ext = smoothstep(0.0, 0.3, pr);
    float tipX = baseX + (finalTip - baseX) * ext;

    float pillarT = smoothstep(0.72, 0.92, pr);
    float flashFreq = 18.0 + pillarT * 65.0;
    float flashAmp = 0.15 + pillarT * 0.85;
    float flicker = (1.0 - flashAmp) + flashAmp * (sin(u_time * flashFreq) * 0.5 + sin(u_time * flashFreq * 1.73) * 0.5);

    float hFade = xFade(cp, baseX, tipX);

    float coreDist = sdLance(cp, baseX, tipX, 6.0);
    float coreDistPillar = abs(cp.y) - 6.0;
    float coreAlpha = smoothstep(6.0, -8.0, mix(coreDist, coreDistPillar, pillarT)) * hFade;

    float glowDist = sdLance(cp, baseX, tipX, 18.0);
    float glowAlpha = smoothstep(6.0, -8.0, glowDist) * hFade * 0.2 * (1.0 - pillarT);

    float haloDist = sdLance(cp, baseX, tipX, 25.0);
    vec2 nCoord = vec2(cp.x * 0.01 - u_time * 2.0, cp.y * 0.1);
    float fbmVal = fbm(nCoord);
    float haloAlpha = pow(smoothstep(15.0, -25.0, haloDist), 1.2) * hFade * fbmVal * (1.0 - pillarT);

    float brightness = 1.0 + pillarT * pillarT * 5.0;

    vec3 coreColor = vec3(1.6, 1.6, 1.8) * coreAlpha;
    vec3 glowColor = vec3(0.3, 0.55, 1.0) * glowAlpha;
    vec3 haloColor = vec3(0.2, 0.4, 0.9) * haloAlpha;

    vec3 color = (coreColor + glowColor) * flicker + haloColor;
    color *= brightness;
    float alpha = clamp(coreAlpha + glowAlpha + haloAlpha, 0.0, 1.0);

    float fadeIn = smoothstep(0.0, 0.05, pr);
    float fadeOut = 1.0 - smoothstep(0.95, 1.0, pr);
    alpha *= fadeIn * fadeOut;
    color *= fadeIn * fadeOut;

    if (alpha < 0.001) discard;
    gl_FragColor = vec4(color, alpha);
}
