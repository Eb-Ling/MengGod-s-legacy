#version 430
in vec2 v_uv;
out vec4 fragColor;
uniform float u_time;
uniform float u_hexSize;
uniform vec3 u_bgColor;
uniform float u_shieldFacing;
uniform float u_shieldArc;
uniform sampler2D u_maskTexture;
uniform sampler2D u_pulseMaskTexture;
uniform int u_hitEventCount;
uniform float u_fadeAlpha;
uniform int u_pulseEventCount;
uniform float u_pulseLifetime;

layout(std430, binding = 1) buffer HitEvents {
    vec4 hitEvents[];
};
layout(std430, binding = 2) buffer PulseEvents {
    vec4 pulseEvents[];
};

float calcHexDist(vec2 p, vec2 cellCenter, float size, float wave, vec2 waveOrigin) {
    float scaleMod = 1.0 + wave * 0.06;
    float distToOrigin = length(cellCenter - waveOrigin);
    vec2 dirToOrigin = (cellCenter - waveOrigin) / max(distToOrigin, 0.0001);
    float displacement = wave * 0.02;
    vec2 shiftedCenter = cellCenter + dirToOrigin * displacement;
    vec2 localPos = (p - shiftedCenter) / scaleMod;
    float d1 = abs(localPos.x);
    float d2 = abs(localPos.x * 0.5 + localPos.y * 0.8660254);
    float d3 = abs(localPos.x * 0.5 - localPos.y * 0.8660254);
    return max(max(d1, d2), d3);
}

void main() {
    vec2 uv = v_uv;
    vec2 canvasCenter = vec2(0.5, 0.5);
    vec2 p = (uv - canvasCenter) * 2.0;
    float sphereRadius = 1.05;
    float r = length(p);
    float z = sqrt(max(0.0, sphereRadius * sphereRadius - r * r));
    float bulge = sphereRadius / max(z, 0.01);
    vec2 warpedP = mix(p, p * bulge, 0.3);

    float size = u_hexSize;
    float hexWidth = size * 2.0;
    float hexHeight = size * 1.7320508;
    float row = floor((warpedP.y + hexHeight * 0.5) / hexHeight);
    float minDist = 999.0;
    float cellWave = 0.0;
    vec2 closestCenter = vec2(0.0);
    vec2 closestShifted = vec2(0.0);

    for (int dr = -1; dr <= 1; dr++) {
        float nr = row + float(dr);
        float isOddRow = mod(nr, 2.0);
        float rowOffset = isOddRow * size;
        for (int dc = -1; dc <= 1; dc++) {
            float col = floor((warpedP.x - rowOffset + hexWidth * 0.5) / hexWidth) + float(dc);
            float cellCenterX = col * hexWidth + rowOffset;
            float cellCenterY = nr * hexHeight;
            vec2 cellCenter = vec2(cellCenterX, cellCenterY);
            float wave = 0.0;
            vec2 useOrigin = vec2(0.0);
            for (int wi = 0; wi < u_hitEventCount; wi++) {
                vec2 wUV = hitEvents[wi].xy;
                float wTriggerTime = hitEvents[wi].z;
                float wDamage = hitEvents[wi].w;
                float wAge = u_time - wTriggerTime;
                float dmgScale = 0.3 + clamp(wDamage/300.0, 0.0, 1.0)*0.5 + clamp((wDamage-300.0)/500.0, 0.0, 1.0)*0.4;
                float waveLifetime = 0.2+dmgScale * 0.5;
                if (wAge > 0.0 && wAge < waveLifetime) {
                    vec2 wOrigin = (wUV - canvasCenter) * 2.0;
                    float wR = length(wOrigin);
                    float wZ = sqrt(max(0.0, sphereRadius * sphereRadius - wR * wR));
                    float wBulge = sphereRadius / max(wZ, 0.01);
                    vec2 warpedOrigin = mix(wOrigin, wOrigin * wBulge, 0.3);
                    float distToOrigin = length(cellCenter - warpedOrigin);
                    float ringSpeed = 3.0;
                    float ringRadius = wAge * ringSpeed;
                    float ringWidth = dmgScale * 0.18;
                    float distToRing = abs(distToOrigin - ringRadius);
                    float ringIntensity = 1.0 - smoothstep(0.0, ringWidth, distToRing);
                    float fadeOut = 1.0 - smoothstep(0.0, waveLifetime, wAge);
                    float ringVal = ringIntensity * fadeOut * 1.2;
                    if (ringVal > wave) {
                        wave = ringVal;
                        useOrigin = warpedOrigin;
                    }
                }
            }
            float dTO = length(cellCenter - useOrigin);
            vec2 dDir = (cellCenter - useOrigin) / max(dTO, 0.0001);
            vec2 shiftedCtr = cellCenter + dDir * wave * 0.02;
            float d = calcHexDist(warpedP, cellCenter, size, wave, useOrigin);
            if (d < minDist) {
                minDist = d;
                cellWave = wave;
                closestCenter = cellCenter;
                closestShifted = shiftedCtr;
            }
        }
    }

    float borderWidth = size * 0.08;
    float edgeAlpha = smoothstep(0.0, borderWidth, abs(minDist - size));
    float opacity = 0.85 + cellWave * 0.6;
    float alpha = edgeAlpha * opacity;
    float pulseGlow = 0.0;
    for (int pi = 0; pi < u_pulseEventCount; pi++) {
        vec2 pUV = pulseEvents[pi].xy;
        float pTime = pulseEvents[pi].z;
        float pAge = u_time - pTime;
        if (pAge > 0.0 && pAge < u_pulseLifetime) {
            vec2 pOrigin = (pUV - canvasCenter) * 2.0;
            float pR = length(pOrigin);
            float pZ = sqrt(max(0.0, sphereRadius * sphereRadius - pR * pR));
            float pBulge = sphereRadius / max(pZ, 0.01);
            vec2 warpedPO = mix(pOrigin, pOrigin * pBulge, 0.3);
            float pDist = length(closestShifted - warpedPO);
            float pRingR = pAge * 0.6;
            float pRingW = 0.08 + pAge * 0.15;
            float pDistToRing = abs(pDist - pRingR);
            float pIntensity = 1.0 - smoothstep(0.0, pRingW, pDistToRing);
            pulseGlow = max(pulseGlow, pIntensity);
        }
    }
    vec2 pLocal = (warpedP - closestShifted) / (1.0 + cellWave * 0.06);
    float pHexDist = max(max(abs(pLocal.x), abs(pLocal.x * 0.5 + pLocal.y * 0.8660254)), abs(pLocal.x * 0.5 - pLocal.y * 0.8660254));
    float gapWidth = borderWidth * (1.5 + pulseGlow * 3.0);
    float gapFactor = 1.0 - smoothstep(0.0, gapWidth, abs(pHexDist - size));

    vec2 duv = uv - vec2(0.5);
    float rot1 = u_time * 0.4;
    float rot2 = u_time * -0.2;
    float cr1 = cos(rot1);
    float sr1 = sin(rot1);
    float cr2 = cos(rot2);
    float sr2 = sin(rot2);
    vec2 uv1 = vec2(0.5) + vec2(duv.x * cr1 - duv.y * sr1, duv.x * sr1 + duv.y * cr1);
    vec2 uv2 = vec2(0.5) + vec2(duv.x * cr2 - duv.y * sr2, duv.x * sr2 + duv.y * cr2);
    float maskAlpha = 0.2 + 0.8 * texture2D(u_maskTexture, uv1).a * texture2D(u_maskTexture, uv2).a;
    float circleFade = 1.0 - smoothstep(0.92, 1.05, r);
    alpha *= maskAlpha * circleFade;

    float pixelAngle = atan(p.y, p.x);
    float angleDiff = pixelAngle - u_shieldFacing;
    angleDiff = mod(angleDiff + 3.14159265, 6.2831853) - 3.14159265;
    float halfArc = u_shieldArc * 0.5;
    float arcEdge = 0.06;
    float arcMask = 1.0 - smoothstep(halfArc, halfArc + arcEdge, abs(angleDiff));
    alpha *= arcMask;

    vec3 baseColor = u_bgColor;
    vec3 pulseColor = vec3(0.7, 0.2, 1.0);
    float pulseMaskVal = texture2D(u_pulseMaskTexture, uv).a * 0.8 * arcMask * (1.0 - smoothstep(0.75, 1.0, r));
    float pulseGapAlpha = pulseGlow * gapFactor * 0.9 * pulseMaskVal;
    vec3 finalColor = mix(baseColor, pulseColor, pulseGapAlpha);
    float finalAlpha = (alpha * 0.85 + pulseGapAlpha * 0.5) * u_fadeAlpha;
    fragColor = vec4(finalColor, finalAlpha);
}
