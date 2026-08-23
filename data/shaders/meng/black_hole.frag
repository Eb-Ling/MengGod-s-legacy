#version 420
#define AA 1
#define _Speed 3.0
#define _Steps 12.0
#define _Size 0.03
#define _LensInner 0.30
#define _LensOuter 0.48
#define _HorizonInner 0.055
#define _HorizonOuter 0.085
#define _LensStrength 0.1
#define _LensMaxOffset 0.050
#define _GlowStrength 0.0

in vec2 v_uv;
in vec2 v_screenUV;
out vec4 fragColor;

uniform float u_time;
uniform sampler2D noise_texture;
uniform sampler2D screen_texture;
uniform vec2 screen_resolution;
uniform float u_viewScale;
uniform float u_zoomScale;
float hash(float x) { return fract(sin(x) * 152754.742); }
float hash2(vec2 x) { return hash(x.x + hash(x.y)); }
float value(vec2 p, float f) {
    float bl = hash2(floor(p * f + vec2(0.0, 0.0)));
    float br = hash2(floor(p * f + vec2(1.0, 0.0)));
    float tl = hash2(floor(p * f + vec2(0.0, 1.0)));
    float tr = hash2(floor(p * f + vec2(1.0, 1.0)));
    vec2 fr = fract(p * f);
    fr = (3.0 - 2.0 * fr) * fr * fr;
    float b = mix(bl, br, fr.x);
    float t = mix(tl, tr, fr.x);
    return mix(b, t, fr.y);
}
void Rotate(inout vec3 v, vec2 angle) {
    v.yz = cos(angle.y) * v.yz + sin(angle.y) * vec2(-1.0, 1.0) * v.zy;
    v.xz = cos(angle.x) * v.xz + sin(angle.x) * vec2(-1.0, 1.0) * v.zx;
}
void UnRotate(inout vec3 v, vec2 angle) {
    v.xz = cos(angle.x) * v.xz + sin(angle.x) * vec2(1.0, -1.0) * v.zx;
    v.yz = cos(angle.y) * v.yz + sin(angle.y) * vec2(1.0, -1.0) * v.zy;
}
vec4 background(vec3 ray, vec2 baseScreenUV, vec2 deflection, float effectMask) {
    float deflectionLen = length(deflection);
    if (deflectionLen > _LensMaxOffset) {
        deflection *= _LensMaxOffset / deflectionLen;
    }
    vec2 off = deflection;
    vec2 screenUV = clamp(baseScreenUV + off, 0.0, 1.0);
    vec4 bg = texture(screen_texture, screenUV);

    return bg;
}
vec4 raymarchDisk(vec3 ray, vec3 zeroPos) {
    vec3 pos = zeroPos;
    float lengthPos = length(pos.xz);
    float dist = min(1.0, lengthPos * (1.0 / _Size) * 0.5) * _Size * 0.4 * (1.0 / _Steps) / abs(ray.y);
    pos += dist * _Steps * ray * 0.5;

    vec2 deltaPos;
    deltaPos.x = -zeroPos.z * 0.01 + zeroPos.x;
    deltaPos.y = zeroPos.x * 0.01 + zeroPos.z;
    deltaPos = normalize(deltaPos - zeroPos.xz);

    float parallel = dot(ray.xz, deltaPos);
    parallel /= sqrt(lengthPos);
    parallel *= 0.5;
    float redShift = parallel + 0.3;
    redShift *= redShift;
    redShift = clamp(redShift, 0.0, 1.0);

    float disMix = clamp((lengthPos - _Size * 2.0) * (1.0 / _Size) * 0.24, 0.0, 1.0);
    vec3 insideCol = mix(vec3(1.0, 0.8, 0.0), vec3(0.5, 0.13, 0.02) * 0.2, disMix);
    insideCol *= mix(vec3(0.4, 0.2, 0.1), vec3(1.6, 2.4, 4.0), redShift);
    insideCol *= 1.25;
    redShift += 0.12;
    redShift *= redShift;

    vec4 o = vec4(0.0);
    for (float i = 0.0; i < _Steps; i++) {
        pos -= dist * ray;
        float intensity = clamp(1.0 - abs((i - 0.8) * (1.0 / _Steps) * 2.0), 0.0, 1.0);
        float lp = length(pos.xz);
        float distMult = 1.0;
        distMult *= clamp((lp - _Size * 0.75) * (1.0 / _Size) * 1.5, 0.0, 1.0);
        distMult *= clamp((_Size * 10.0 - lp) * (1.0 / _Size) * 0.20, 0.0, 1.0);
        distMult *= distMult;

        float u = lp + u_time * _Size * 0.3 + intensity * _Size * 0.2;
        vec2 xy;
        float rot = mod(u_time * _Speed, 8192.0);
        xy.x = -pos.z * sin(rot) + pos.x * cos(rot);
        xy.y = pos.x * sin(rot) + pos.z * cos(rot);

        float x = abs(xy.x / (xy.y + 0.0001));
        float angle2 = 0.02 * atan(x);
        const float f = 70.0;
        float noise = value(vec2(angle2, u * (1.0 / _Size) * 0.05), f);
        noise = noise * 0.66 + 0.33 * value(vec2(angle2, u * (1.0 / _Size) * 0.05), f * 2.0);

        float extraWidth = noise * 1.0 * (1.0 - clamp(i * (1.0 / _Steps) * 2.0 - 1.0, 0.0, 1.0));
        float alpha = clamp(noise * (intensity + extraWidth) * ((1.0 / _Size) * 10.0 + 0.01) * dist * distMult, 0.0, 1.0);

        vec3 col = 2.0 * mix(vec3(0.3, 0.2, 0.15) * insideCol, insideCol, min(1.0, intensity * 2.0));
        o = clamp(vec4(col * alpha + o.rgb * (1.0 - alpha), o.a * (1.0 - alpha) + alpha), vec4(0.0), vec4(1.0));

        lp *= (1.0 / _Size);
        o.rgb += redShift * (intensity * 1.0 + 0.5) * (1.0 / _Steps) * 100.0 * distMult / (lp * lp + 0.001);
    }
    o.rgb = clamp(o.rgb - 0.005, 0.0, 1.0);
    return o;
}

void main() {
    fragColor = vec4(0.0);

    float localRadius = length(v_uv - 0.5);
    float effectMask = 1.0 - smoothstep(_LensInner, _LensOuter, localRadius);
    vec2 angle = vec2(u_time * 0.1, 0.2);
    angle.y = -sin(u_time*0.1) * 0.2  ;

    for (int j = 0; j < AA; j++)
    for (int i = 0; i < AA; i++) {
        vec2 uv = (v_uv - 0.5) * u_viewScale;
        float camD = 3.0 * 3.0 * 0.05;
        vec3 pos = vec3(0.0, 0.05, -camD);
        Rotate(pos, angle);
        vec3 forward = normalize(-pos);
        vec3 right = cross(vec3(0.0, 1.0, 0.0), forward);
        right = dot(right, right) < 0.0001 ? vec3(1.0, 0.0, 0.0) : normalize(right);
        vec3 up = normalize(cross(forward, right));
        vec3 initialRay = normalize(forward + uv.x * right + uv.y * up);
        vec3 ray = initialRay;

        vec4 col = vec4(0.0);
        vec4 glow = vec4(0.0);
        vec4 outCol = vec4(100.0);
        for (int disks = 0; disks < 20; disks++) {
            for (int h = 0; h < 6; h++) {
                float dotpos = dot(pos, pos);
                float invDist = inversesqrt(dotpos);
                float centDist = dotpos * invDist;
                float stepDist = 0.92 * abs(pos.y / (ray.y + 0.0001));
                float farLimit = centDist * 0.5;
                float closeLimit = centDist * 0.1 + 0.05 * centDist * centDist * (1.0 / _Size);
                stepDist = min(stepDist, min(farLimit, closeLimit));

                float invDistSqr = invDist * invDist;
                float bendForce = stepDist * invDistSqr * _Size * 0.625;
                ray = normalize(ray - (bendForce * invDist) * pos);
                pos += stepDist * ray;

                glow += vec4(1.2, 1.1, 1.0, 1.0) * (0.01 * stepDist * invDistSqr * invDistSqr * clamp(centDist * 2.0 - 1.2, 0.0, 1.0));
            }

            float d2 = length(pos);

            if (d2 < _Size * 0.1) {
                vec2 capturedScreen = vec2(dot(ray, right), dot(ray, up)) / max(dot(ray, forward), 0.01);
                vec2 initialScreen = vec2(dot(initialRay, right), dot(initialRay, up)) / max(dot(initialRay, forward), 0.01);
                vec2 deflection = (capturedScreen - initialScreen) * _LensStrength * effectMask * u_zoomScale;
                vec4 bg = background(ray, v_screenUV, deflection, effectMask);
                float horizonMask = 1.0 - smoothstep(_HorizonInner, _HorizonOuter, localRadius);
                vec3 capturedRgb = mix(bg.rgb, vec3(0.0), horizonMask);
                outCol = vec4(col.rgb * col.a + capturedRgb * (1.0 - col.a) + glow.rgb * _GlowStrength * (1.0 - col.a), 1.0);
                break;
            } else if (d2 > _Size * 1000.0) {
                vec2 escapedScreen = vec2(dot(ray, right), dot(ray, up)) / max(dot(ray, forward), 0.01);
                vec2 initialScreen = vec2(dot(initialRay, right), dot(initialRay, up)) / max(dot(initialRay, forward), 0.01);
                vec2 deflection = (escapedScreen - initialScreen) * _LensStrength * effectMask * u_zoomScale;
                vec4 bg = background(ray, v_screenUV, deflection, effectMask);
                outCol = vec4(col.rgb * col.a + bg.rgb * (1.0 - col.a) + glow.rgb * _GlowStrength * (1.0 - col.a), 1.0);
                break;
            } else if (abs(pos.y) <= _Size * 0.002) {
                vec4 diskCol = raymarchDisk(ray, pos);
                pos.y = 0.0;
                pos += abs(_Size * 0.001 / (ray.y + 0.0001)) * ray;
                col = vec4(diskCol.rgb * (1.0 - col.a) + col.rgb, col.a + diskCol.a * (1.0 - col.a));
            }
        }
        if (outCol.r >= 99.0)
        {
            vec2 unresolvedScreen = vec2(dot(ray, right), dot(ray, up)) / max(dot(ray, forward), 0.01);
            vec2 initialScreen = vec2(dot(initialRay, right), dot(initialRay, up)) / max(dot(initialRay, forward), 0.01);
            vec2 deflection = (unresolvedScreen - initialScreen) * _LensStrength * effectMask * u_zoomScale;
            vec4 bg = background(ray, v_screenUV, deflection, effectMask);
            outCol = vec4(col.rgb * col.a + bg.rgb * (1.0 - col.a) + glow.rgb * _GlowStrength * (1.0 - col.a), 1.0);
        }

        fragColor += outCol / float(AA * AA);
    }
    fragColor.a = 1.0;
}
