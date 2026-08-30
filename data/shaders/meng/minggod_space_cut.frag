#version 430

in vec2 v_fragUV;
in vec2 v_screenUV;
out vec4 fragColor;

uniform float u_time;
uniform float u_alpha;
uniform vec3 u_auraColor;
uniform vec3 u_coreColor;
uniform sampler2D u_screenTexture;
uniform int u_sectorMap[8];
uniform int u_hoverSector;
uniform int u_selectedSector;
uniform int u_swapFirstSector;
uniform int u_swapSecondSector;
uniform float u_swapFlash;
uniform float u_resolveFlash;

const float CUT_INTERVAL = 0.25;
const float CUT_DRAW_TIME = 0.25;
const float SECTOR_ANGLE = 0.78539816;
const float SCREEN_EDGE_SOFTNESS = 0.02;

float cutField(vec2 localUv, vec2 axis, float delay) {
    float progress = clamp((u_time - delay) / CUT_DRAW_TIME, 0.0, 1.0);
    vec2 crossAxis = vec2(-axis.y, axis.x);
    float along = dot(localUv, axis);
    float across = abs(dot(localUv, crossAxis));
    float end = -1.0 + progress * 2.0;
    float segment = smoothstep(-1.0, -0.99, along) * (1.0 - smoothstep(end - 0.01, end, along));
    float aura = 1.0 - smoothstep(0.004, 0.016, across);
    float core = 1.0 - smoothstep(0.0007, 0.0035, across);
    float shimmer = 0.8 + 0.2 * sin((along - across) * 40.0 + u_time * 8.0);
    return segment * progress * (aura * 0.35 + core * 0.90) * shimmer;
}

float borderField(float radius) {
    float edgeDist = abs(radius - 0.99);
    float aura = 1.0 - smoothstep(0.004, 0.016, edgeDist);
    float core = 1.0 - smoothstep(0.0007, 0.0035, edgeDist);
    float shimmer = 0.8 + 0.2 * sin(radius * 40.0 + u_time * 8.0);
    return (aura * 0.35 + core * 0.90) * shimmer;
}

int sectorIndex(vec2 localUv) {
    float angle = atan(localUv.y, localUv.x);
    if (angle < 0.0) {
        angle += 6.28318531;
    }
    return min(7, int(floor(angle / SECTOR_ANGLE)));
}

vec2 rotateSector(vec2 localUv, int currentSector, int sourceSector) {
    float angle = float(sourceSector - currentSector) * SECTOR_ANGLE;
    float sine = sin(angle);
    float cosine = cos(angle);
    return vec2(localUv.x * cosine - localUv.y * sine, localUv.x * sine + localUv.y * cosine);
}

vec2 sourceScreenUV(vec2 localUv, vec2 sourceLocalUv) {
    vec2 screenStepX = dFdx(v_screenUV) / dFdx(localUv).x;
    vec2 screenStepY = dFdy(v_screenUV) / dFdy(localUv).y;
    vec2 localOffset = sourceLocalUv - localUv;
    return v_screenUV + screenStepX * localOffset.x + screenStepY * localOffset.y;
}

float screenSampleMask(vec2 screenUV) {
    float edgeDist = min(min(screenUV.x, screenUV.y), min(1.0 - screenUV.x, 1.0 - screenUV.y));
    return smoothstep(0.0, SCREEN_EDGE_SOFTNESS, edgeDist);
}

void main() {
    vec2 fragUV = v_fragUV;
    vec2 uv = fragUV;
    vec2 localUv = (uv * 2.0 - 1.0) * 1.2;
    vec2 aspectUv = localUv;
    float radius = length(aspectUv);
    float circle = 1.0 - smoothstep(1.0, 1.02, radius);
    int currentSector = sectorIndex(localUv);
    int sourceSector = u_sectorMap[currentSector];
    vec2 sourceLocalUv = rotateSector(localUv, currentSector, sourceSector);
    float cuts = 0.0;
    cuts += cutField(localUv, vec2(1.0, 0.0), 0.0);
    cuts += cutField(localUv, normalize(vec2(-1.0, -1.0)), CUT_INTERVAL);
    cuts += cutField(localUv, normalize(vec2(-1.0, 1.0)), CUT_INTERVAL * 2.0);
    cuts += cutField(localUv, vec2(0.0, -1.0), CUT_INTERVAL * 3.0);
    float border = borderField(radius);
    float intensity = min(cuts + border, 1.0);
    vec3 lineColor = mix(u_auraColor, u_coreColor, intensity);
    vec2 rotatedScreenUV = sourceScreenUV(localUv, sourceLocalUv);
    vec3 originalBackground = texture(u_screenTexture, clamp(v_screenUV, 0.0, 1.0)).rgb;
    vec3 rotatedBackground = texture(u_screenTexture, clamp(rotatedScreenUV, 0.0, 1.0)).rgb;
    float sourceMask = currentSector == sourceSector ? 1.0 : screenSampleMask(rotatedScreenUV);
    vec3 backgroundColor = mix(originalBackground, rotatedBackground, sourceMask);
    float selectionBreath = smoothstep(0.1, 1.0, 0.5 + 0.5 * sin(u_time * 4.0));
    float hoverHighlight = currentSector == u_hoverSector ? 0.15 * selectionBreath : 0.0;
    float selectedHighlight = currentSector == u_selectedSector ? 0.45 * selectionBreath : 0.0;
    float swapHighlight = (currentSector == u_swapFirstSector || currentSector == u_swapSecondSector) ? u_swapFlash : 0.0;
    float highlight = max(max(hoverHighlight, selectedHighlight), swapHighlight) * circle;
    vec3 color = mix(backgroundColor, lineColor, intensity * u_alpha);
    float resolveEdge = 1.0 - smoothstep(0.0, 1.0, radius);
    float resolveAura = smoothstep(1.0, 1.16, radius) * (1.0 - smoothstep(1.16, 1.2, radius));
    float resolveHighlight = max(resolveEdge, resolveAura) * u_resolveFlash;
    color = mix(color, u_coreColor, resolveHighlight);
    color = mix(color, u_coreColor, highlight);
    float alpha = max(circle, resolveAura * u_resolveFlash);
    if (alpha < 0.001) discard;
    fragColor = vec4(color, alpha);
}
