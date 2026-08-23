#version 110

uniform float u_type;
uniform float u_time;
uniform float u_alpha;
uniform float u_charge;
uniform float u_progress;
uniform vec4 u_colorA;
uniform vec4 u_colorB;
uniform vec4 u_params;
uniform sampler2D u_noiseTex;
uniform sampler2D u_streakTex;

varying vec2 fragUV;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i + vec2(0.0, 0.0)), hash(i + vec2(1.0, 0.0)), u.x),
               mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x), u.y);
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 4; i++) {
        v += noise(p) * a;
        p = p * 2.03 + vec2(17.13, 9.71);
        a *= 0.5;
    }
    return v;
}

float ring(float d, float r, float w) {
    return 1.0 - smoothstep(w, w + 0.018, abs(d - r));
}

float sampleNoise(vec2 sampleUV) {
    return texture2D(u_noiseTex, fract(sampleUV)).a;
}

float sampleStreak(vec2 sampleUV) {
    vec4 c = texture2D(u_streakTex, fract(sampleUV));
    return max(c.a, max(c.r, max(c.g, c.b)));
}

float sampleFlow(vec2 uv, float flow, float lateral) {
    float warp = sampleNoise(vec2(uv.x * 0.72 - flow * 0.08, uv.y * 1.15 + flow * 0.05));
    vec2 sampleUV0 = uv * vec2(1.55, 0.85) + vec2(-flow * 0.34, flow * 0.07);
    sampleUV0 += vec2((warp - 0.5) * 0.18, lateral * 0.10);

    vec2 sampleUV1 = uv * vec2(3.20, 1.60) + vec2(-flow * 0.82, flow * 0.16);
    sampleUV1 += vec2((warp - 0.5) * 0.28, -lateral * 0.18);

    vec2 sampleUV2 = uv * vec2(8.40, 3.10) + vec2(-flow * 1.72, flow * 0.27);
    sampleUV2 += vec2(lateral * 0.21, (warp - 0.5) * 0.16);

    float n0 = sampleNoise(sampleUV0);
    float n1 = sampleNoise(sampleUV1);
    float n2 = sampleStreak(sampleUV2);
    return n0 * 0.38 + n1 * 0.22 + n2 * 0.40;
}

float lanceTaper(float t) {
    t = clamp(t, 0.0, 1.0);
    return smoothstep(0.0, 0.055, t) * (1.0 - smoothstep(0.20, 1.0, t));
}

float sdLance(vec2 p, float baseX, float tipX, float maxR) {
    float len = max(0.0001, tipX - baseX);
    float t = clamp((p.x - baseX) / len, 0.0, 1.0);
    float r = maxR * (0.18 + 0.82 * lanceTaper(t));
    return abs(p.y) - r;
}

float xFade(float x, float baseX, float tipX) {
    float fb = smoothstep(baseX, baseX + 0.045, x);
    float ft = 1.0 - smoothstep(tipX - 0.060, tipX + 0.010, x);
    return fb * ft;
}

void main() {
    vec2 uv = fragUV;
    float alpha = 0.0;
    vec3 color = u_colorA.rgb;

    if (u_type < 0.5) {
        float x = uv.x;
        float lateral = (uv.y - 0.5) * 2.0;
        float y = abs(lateral);
        if (x < 0.0 || x > 1.0 || y > 1.04) {
            discard;
        }

        float pr = clamp(u_progress, 0.0, 1.0);
        float ext = smoothstep(0.16, 0.46, pr);
        float pillarT = smoothstep(0.70, 0.92, pr);
        float tipX = mix(0.10, 1.00, ext);
        float baseX = 0.015;
        float hFade = mix(xFade(x, baseX, tipX), smoothstep(0.0, 0.045, x) * (1.0 - smoothstep(0.94, 1.0, x)), pillarT);

        float flow = u_time * (0.70 + u_charge * 1.15);
        vec2 beamUV = vec2(x, uv.y);
        float stream = sampleFlow(beamUV, flow, lateral);
        float fine = sampleFlow(beamUV * vec2(2.4, 1.0) + vec2(0.17, 0.0), flow * 1.80, -lateral);
        float longStreak = sampleStreak(vec2(x * 1.95 - flow * 0.98, uv.y * 0.74 + lateral * 0.05));
        float dissolve = smoothstep(0.38, 0.86, stream + longStreak * 0.30 + x * 0.08 - y * 0.34);

        float coreWidth = u_params.x;
        float hotWidth = u_params.y;
        float auraWidth = u_params.z;
        float flame = u_params.w;

        vec2 p = vec2(x, lateral);
        float coreShape = mix(sdLance(p, baseX, tipX, coreWidth * 1.50), y - coreWidth, pillarT);
        float hotShape = mix(sdLance(p, baseX, tipX, hotWidth * 1.35), y - hotWidth, pillarT);
        float auraShape = mix(sdLance(p, baseX, tipX, auraWidth * 1.05), y - auraWidth, pillarT * 0.65);

        float transverseFade = 1.0 - smoothstep(0.62, 1.02, y);
        float auraFade = 1.0 - smoothstep(auraWidth * 0.70, min(1.02, auraWidth + 0.22), y);

        float core = (1.0 - smoothstep(-0.010, 0.030, coreShape)) * hFade;
        float hot = (1.0 - smoothstep(-0.018, 0.105, hotShape)) * hFade;
        float aura = (1.0 - smoothstep(-0.018, 0.420, auraShape)) * hFade * auraFade;
        float sheathEdge = smoothstep(hotWidth * 0.55, auraWidth, y);
        float wave = sheathEdge * aura * dissolve * transverseFade
                * (0.12 + 0.34 * fine + 0.22 * longStreak) * flame;

        float flashFreq = 14.0 + pillarT * 54.0;
        float flash = 0.88 + 0.12 * sin(u_time * flashFreq) + 0.06 * sin(u_time * flashFreq * 1.73);
        float shimmer = 0.88 + 0.12 * sampleNoise(vec2(x * 8.0 - flow, uv.y * 1.5));
        float pillarBoost = 1.0 + pillarT * pillarT * 1.85;

        vec3 outerColor = mix(u_colorB.rgb, vec3(0.18, 0.38, 1.0), 0.35);
        vec3 hotColor = mix(vec3(0.42, 0.78, 1.0), vec3(1.0, 0.62, 0.26), clamp(wave * 1.45, 0.0, 1.0));
        color = mix(outerColor, hotColor, clamp(hot * 0.55 + wave * 0.90, 0.0, 1.0));
        color = mix(color, vec3(0.96, 1.0, 1.0), clamp(core * 1.35, 0.0, 1.0));
        color *= pillarBoost;

        alpha = (core * 0.95 + hot * (0.15 + 0.08 * longStreak) + aura * 0.018 + wave * 0.18)
                * transverseFade * shimmer * flash * u_alpha;
    } else if (u_type < 1.5) {
        vec2 p = uv * 2.0 - 1.0;
        float d = length(p);
        if (d > 1.06) {
            discard;
        }
        vec2 n = d > 0.0001 ? p / d : vec2(0.0, 1.0);
        float angle = atan(n.y, n.x);
        float spin = u_time * (0.25 + u_charge * 0.75);

        float contract = 1.0 - 0.42 * smoothstep(0.0, 1.0, u_charge);
        float gather = smoothstep(0.0, 0.55, u_progress) * (1.0 - smoothstep(0.72, 1.0, u_progress));
        vec2 ringSampleUV = n * 0.42 + vec2(0.5 + spin * 0.07, 0.5 - spin * 0.05);
        float jitterSample = sampleNoise(ringSampleUV) * 0.65 + sampleNoise(ringSampleUV * 2.3 + spin * 0.11) * 0.35;
        float jitter = (jitterSample - 0.5) * 0.040 * gather;

        float arcNoise = sampleNoise(n * 0.52 + vec2(0.5 + spin * 0.12, 0.5 - spin * 0.09));
        float arcWave = sin(angle * 3.0 + spin * 5.0) * 0.5 + 0.5;
        float arcGate = smoothstep(0.38, 0.82, arcNoise * 0.72 + arcWave * 0.28);

        float r1 = ring(d, (0.78 + jitter) * contract, 0.010) * arcGate;
        float r2 = ring(d, (0.53 - jitter) * contract, 0.008) * smoothstep(0.25, 0.75, arcNoise);
        float r3 = ring(d, (0.30 + jitter * 0.6) * contract, 0.007) * smoothstep(0.45, 0.82, 1.0 - arcNoise);
        float spoke = 1.0 - smoothstep(0.006, 0.018, abs(fract((angle + spin) / 1.5707963) - 0.5));
        spoke *= smoothstep(0.20, 0.42, d) * (1.0 - smoothstep(0.62, 0.90, d)) * arcGate;

        float center = 1.0 - smoothstep(0.0, 0.34, d);
        float heat = center * center * (0.35 + u_charge * 0.65);
        float rings = r1 * 0.30 + r2 * 0.22 + r3 * 0.18 + spoke * 0.050 * gather;

        color = mix(u_colorA.rgb, u_colorB.rgb, clamp(heat + r3, 0.0, 1.0));
        alpha = (rings + heat * 0.22) * u_alpha * (1.0 - smoothstep(0.98, 1.08, d));
    } else {
        float impactU = u_params.x;
        float aspect = u_params.y;
        vec2 p = vec2((uv.x - impactU) * aspect, uv.y - 0.5);
        float d = length(vec2(p.x * 1.18, p.y * 1.72));
        if (abs(p.y) > 0.42 || p.x < -0.22 || p.x > 0.56 || d > 0.70) {
            discard;
        }

        float pr = clamp(u_progress, 0.0, 1.0);
        float impactT = smoothstep(0.42, 0.82, pr);
        float settle = 1.0 - smoothstep(0.92, 1.0, pr);
        float flow = u_time * (0.95 + u_charge * 1.35);

        vec2 nUv0 = uv * vec2(2.6, 1.4) + vec2(-flow * 0.22, flow * 0.08);
        float warp = sampleNoise(nUv0);
        vec2 sampleUV = uv * vec2(5.2, 2.4) + vec2(-flow * 0.72, flow * 0.16);
        sampleUV += vec2((warp - 0.5) * 0.20, p.y * 0.24);
        float breakup = sampleNoise(sampleUV) * 0.45
                + sampleStreak(sampleUV * vec2(1.7, 0.85) + vec2(-flow * 0.55, 0.0)) * 0.55;

        float surface = smoothstep(-0.06, 0.10, p.x);
        float beamContact = (1.0 - smoothstep(0.010, 0.085, abs(p.y)))
                * (1.0 - smoothstep(-0.14, 0.18, p.x)) * smoothstep(-0.22, -0.02, p.x);
        float hotSpot = (1.0 - smoothstep(0.018, 0.180, d)) * (0.72 + 0.28 * breakup);
        float glare = (1.0 - smoothstep(0.018, 0.270, d)) * (0.62 + 0.38 * impactT);
        float molten = (1.0 - smoothstep(0.065, 0.280, d)) * surface * smoothstep(0.28, 0.80, breakup + impactT * 0.12);

        float r = length(vec2((p.x - 0.08) * 0.78, p.y * 1.18));
        float a = atan(p.y, p.x - 0.08);
        float arcNoise = sampleNoise(vec2(cos(a), sin(a)) * 0.48 + vec2(0.5 + flow * 0.04, 0.5 - flow * 0.03));
        float arcGate = smoothstep(0.44, 0.86, arcNoise + 0.18 * sin(a * 5.0 - flow * 2.2));
        float shock0 = ring(r, 0.22 + 0.040 * impactT, 0.008) * arcGate * surface;
        float shock1 = ring(r, 0.34 + 0.065 * impactT, 0.006) * smoothstep(0.58, 0.94, arcNoise) * surface;

        float whiteCore = max(beamContact, hotSpot * 0.74);
        float orangeHeat = molten * 0.58 + shock0 * 0.18;
        float blueEdge = shock1 * 0.22 + glare * 0.08;

        vec3 thermal = mix(vec3(1.0, 0.36, 0.12), vec3(1.0, 0.74, 0.30), clamp(breakup, 0.0, 1.0));
        color = thermal * orangeHeat + vec3(0.30, 0.68, 1.0) * blueEdge
                + vec3(1.20, 1.20, 1.12) * whiteCore + vec3(0.95, 0.78, 0.50) * glare * 0.18;
        color = mix(color, vec3(1.0, 0.94, 0.78), clamp(hotSpot, 0.0, 0.85));

        alpha = (whiteCore * 0.66 + glare * 0.12 + orangeHeat * 0.28 + shock0 * 0.10 + shock1 * 0.06)
                * u_alpha * impactT * settle;
    }

    alpha = clamp(alpha, 0.0, 1.0);
    if (alpha <= 0.002) {
        discard;
    }
    gl_FragColor = vec4(color, alpha);
}
