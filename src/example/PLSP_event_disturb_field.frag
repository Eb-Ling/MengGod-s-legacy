#version 430

in vec2 fragUV;
out vec4 fragColor;

uniform float time;
uniform vec2 size;
uniform float edgeWidth;
uniform vec4 coreColor;
uniform vec4 edgeColor;
uniform int feedbackEventCount;

layout(std430, binding = 1) buffer FeedbackEvents
{
	vec4 feedbackEvents[];
};

const float TAU = 6.283185307179586476925286766559;
const float RING = 0.8;
const float OUTER_MIN_REACH = 6.0;
const float OUTER_MAX_REACH = 42.0;
const float INNER_MIN_REACH = 7.0;
const float INNER_MAX_REACH = 48.0;
const float INNER_IMPACT_LIFT_MIN = 28.0;
const float INNER_IMPACT_LIFT_MAX = 80.0;
const float INNER_IMPACT_LIFT_GATE_START = 0.08;
const float INNER_IMPACT_LIFT_GATE_FULL = 0.28;
const float INNER_IMPACT_TARGET_MIN = 24.0;
const float CENTER_VEIL_ALPHA = 0.5;
const float EDGE_VEIL_ALPHA = 0.3;
const float VEIL_COLOR_MIX = 0.8;
const float VEIL_COLOR_MIX_MAX = 0.8;
const float CENTER_VEIL_SOLID_END = 0.10;
const float CENTER_VEIL_FADE_END = 0.40;
const float EDGE_VEIL_FADE_START = 0.55;

float hash(vec3 p) {
	p = fract(p * vec3(0.1031, 0.1030, 0.0973));
	p += dot(p, p.yxz + 33.33);
	return fract((p.x + p.y) * p.z);
}

float noise(vec3 p) {
	vec3 i = floor(p);
	vec3 f = fract(p);
	vec3 u = f * f * (3.0 - 2.0 * f);

	float n000 = hash(i + vec3(0.0, 0.0, 0.0));
	float n100 = hash(i + vec3(1.0, 0.0, 0.0));
	float n010 = hash(i + vec3(0.0, 1.0, 0.0));
	float n110 = hash(i + vec3(1.0, 1.0, 0.0));
	float n001 = hash(i + vec3(0.0, 0.0, 1.0));
	float n101 = hash(i + vec3(1.0, 0.0, 1.0));
	float n011 = hash(i + vec3(0.0, 1.0, 1.0));
	float n111 = hash(i + vec3(1.0, 1.0, 1.0));

	float nx00 = mix(n000, n100, u.x);
	float nx10 = mix(n010, n110, u.x);
	float nx01 = mix(n001, n101, u.x);
	float nx11 = mix(n011, n111, u.x);
	float nxy0 = mix(nx00, nx10, u.y);
	float nxy1 = mix(nx01, nx11, u.y);
	return mix(nxy0, nxy1, u.z);
}

float fbm(vec3 p) {
	float value = 0.0;
	float amplitude = 0.52;
	for (int i = 0; i < 3; i++) {
		value += amplitude * noise(p);
		p = p * 2.02 + vec3(17.1, 9.2, 4.7);
		amplitude *= 0.50;
	}
	return value;
}

float angleDistance(float a, float b) {
	return atan(sin(a - b), cos(a - b));
}

float hash11(float value) {
	return fract(sin(value * 127.1 + 311.7) * 43758.5453123);
}

vec3 flameCoord(vec2 dir, float arcWorld, float temporalRate, float scaleWorld, float seed) {
	float track = (arcWorld + seed * 113.0) / scaleWorld;
	float temporal = time * temporalRate + seed * 17.0;
	return vec3(track + dir.x * 0.35 + seed * 0.19, dir.y * 0.35 + seed * 0.37, temporal);
}

vec2 curlOffset(vec3 p, float amount) {
	float n1 = noise(p * 1.37 + vec3(11.3, 5.7, 0.0));
	float n2 = noise(p * 1.61 + vec3(2.1, 17.9, 8.3));
	return (vec2(n1, n2) * 2.0 - 1.0) * amount;
}

struct EventSample {
	float rawStrength;
	float contribution;
	float life;
	float width;
	float center;
	float angular;
	float innerHitDistance;
};

EventSample emptyEventSample(float angle) {
	EventSample result;
	result.rawStrength = 0.0;
	result.contribution = 0.0;
	result.life = 0.0;
	result.width = 0.045;
	result.center = angle;
	result.angular = 0.0;
	result.innerHitDistance = 0.0;
	return result;
}

EventSample eventPush(float angle) {
	EventSample result = emptyEventSample(angle);

	for (int i = 0; i < feedbackEventCount; i++) {
		vec4 eventData = feedbackEvents[i];
		float strength = clamp(eventData.z, 0.0, 1.0);
		float life = 1.0 - clamp(eventData.y, 0.0, 1.0);
		float envelope = smoothstep(0.0, 0.18, life) * life * life;
		float width = mix(0.045, 0.110, hash11(eventData.w + 2.0)) * mix(1.0, 1.35, strength);
		float angleDelta = abs(angleDistance(angle, eventData.x));
		float local = 1.0 - smoothstep(width, width * 2.4, angleDelta);
		float contribution = local * envelope * strength;
		if (contribution > result.contribution) {
			result.rawStrength = strength;
			result.contribution = contribution;
			result.life = life;
			result.width = width;
			result.center = eventData.x;
			result.angular = local;
			result.innerHitDistance = eventData.w;
		}
	}
	return result;
}

vec4 buildImpactProfile(EventSample evt, float radialWorld, float fieldRadiusWorld) {
	vec4 impact = vec4(0.0);
	if (evt.contribution <= 0.0) return impact;

	if (radialWorld > 0.0) {
		float lifeEnvelope = smoothstep(0.0, 0.18, evt.life) * evt.life * evt.life;
		float reach = mix(80.0, 220.0, clamp(evt.rawStrength / 1.6, 0.0, 1.0)) * smoothstep(0.0, 0.18, evt.life);
		float radial = exp(-radialWorld / max(reach * 0.38, 4.0));
		float edge = 1.0 - smoothstep(reach * 0.72, reach, radialWorld);
		impact.x = evt.angular * radial * edge * evt.rawStrength * lifeEnvelope;
	}
	float pressure01 = clamp(impact.x / 1.6, 0.0, 1.0);
	float target01 = clamp(evt.innerHitDistance / max(fieldRadiusWorld, 1.0), 0.0, 1.0);
	float liftGate = smoothstep(INNER_IMPACT_LIFT_GATE_START, INNER_IMPACT_LIFT_GATE_FULL, target01);
	impact.y = pressure01 * edgeWidth * mix(INNER_IMPACT_LIFT_MIN, INNER_IMPACT_LIFT_MAX, pressure01) * liftGate;

	float reachLife = smoothstep(0.0, 0.45, evt.life);
	float densityLife = smoothstep(0.0, 0.25, evt.life);
	if (evt.innerHitDistance > 0.0) {
		impact.z = clamp(evt.innerHitDistance, edgeWidth * INNER_IMPACT_TARGET_MIN, fieldRadiusWorld) * evt.angular * reachLife;
	}

	float strength01 = clamp(evt.rawStrength, 0.0, 1.0);
	impact.w = evt.angular * densityLife * mix(0.55, 1.0, strength01);
	return impact;
}

vec3 sampleFire(vec2 dir, float arcWorld, float radialWorld, float side, vec4 impact, float seed) {
	vec3 result = vec3(0.0);

	float minReach = edgeWidth * (side > 0.0 ? OUTER_MIN_REACH : INNER_MIN_REACH);
	float maxReach = edgeWidth * (side > 0.0 ? OUTER_MAX_REACH : INNER_MAX_REACH);
	float flowPulse = side > 0.0 ? 1.0 : 1.08;
	float blastPulse = side > 0.0 ? 1.0 : 0.94;

	vec3 heightCoord = flameCoord(dir, arcWorld, 0.37 * flowPulse, 210.0, seed + 1.0);
	vec2 heightCurl = curlOffset(heightCoord, 0.74);
	float broad = fbm(vec3(arcWorld / 220.0 + heightCurl.x * 0.22 + seed, dir.y * 1.85 + heightCurl.y * 0.22, time * 0.31 * flowPulse + seed * 0.41));
	float burst = broad * 0.55 + noise(vec3(arcWorld / 72.0 + heightCurl.x * 0.36 + seed * 1.37, dir.y * 3.20 + heightCurl.y * 0.36, time * 0.67 * blastPulse + seed * 0.53)) * 0.45;
	float drive = clamp((broad + burst) * 0.655 - 0.30, 0.0, 1.0);
	float heightLift = pow(smoothstep(0.10, 0.92, drive), 2.35);
	float surgeWave = 0.5 + 0.5 * sin(broad * TAU + burst * 2.7 + time * (2.6 + seed * 0.03));
	float baseHeight = mix(minReach, maxReach, heightLift);
	baseHeight *= mix(0.78, 1.16, surgeWave);
	float flameHeight = side < 0.0 ? max(baseHeight + impact.y, impact.z) : baseHeight;

	float frontAA = max(fwidth(radialWorld - flameHeight) * 2.0, 0.85);
	float front = 1.0 - smoothstep(flameHeight - frontAA, flameHeight + frontAA, radialWorld);
	if (front <= 0.0) return result;

	float radial01 = clamp(radialWorld / max(flameHeight, 1.0), 0.0, 1.0);
	float root = exp(-radialWorld / max(edgeWidth * 0.42, 0.8));
	vec3 fuelCoord = flameCoord(dir + heightCurl * 0.30, arcWorld + heightCurl.x * 120.0, 0.93, 39.0, seed + 8.0);
	vec3 ripCoord = flameCoord(dir + heightCurl * 0.18, arcWorld - heightCurl.y * 90.0, 1.47, 18.0, seed + 17.0);

	float fuel = noise(fuelCoord) * 0.70 + noise(fuelCoord * 1.73 + vec3(4.1, 9.7, time * 0.24)) * 0.30;
	float rip = noise(ripCoord) * 0.75 + noise(ripCoord * 1.91 + vec3(11.3, 2.5, time * 0.41)) * 0.25;
	float threshold = mix(0.42, 0.86, radial01) + mix(0.08, -0.04, heightLift) - impact.x * 0.06 - impact.w * 0.08;
	float fireBody = smoothstep(threshold - 0.030, threshold + 0.050, fuel + rip * 0.22);
	float edgeTear = smoothstep(0.40, 0.92, rip + burst * 0.20);
	float baseShape = pow(1.0 - radial01, mix(0.86, 1.68, edgeTear));
	float impactShape = 1.0 - smoothstep(0.86, 1.0, radial01);
	float bodyShape = mix(baseShape, impactShape, impact.w);
	float rootBurn = root * smoothstep(0.34, 0.78, fuel + broad * 0.34) * 0.36;
	float density = front * (fireBody * bodyShape + rootBurn);
	density *= mix(0.38, 1.20, edgeTear);
	density = clamp(pow(density, 1.48), 0.0, 1.0);

	float impactHeat = clamp(impact.x / 1.6, 0.0, 1.0);
	float heat = clamp(root * 0.42 + fireBody * (1.0 - radial01 * 0.62) + heightLift * 0.24 + impactHeat * 0.32, 0.0, 1.0);
	result = vec3(density, heat, flameHeight);
	return result;
}

vec2 sampleVeil(vec2 dir, float dist) {
	float inner01 = clamp(dist / RING, 0.0, 1.0);
	float centerMask = 1.0 - smoothstep(CENTER_VEIL_SOLID_END, CENTER_VEIL_FADE_END, inner01);
	float edgeMask = smoothstep(EDGE_VEIL_FADE_START, 1.0, inner01);
	float ringAA = max(fwidth(dist) * 1.5, 0.001);
	float insideRing = 1.0 - smoothstep(RING - ringAA, RING + ringAA, dist);
	float radialAlpha = max(centerMask * CENTER_VEIL_ALPHA, edgeMask * EDGE_VEIL_ALPHA) * insideRing;
	vec2 flowUV = dir * (dist * 3.6) + vec2(time * 0.36, -time * 0.22);
	float broadFlow = noise(vec3(flowUV, time * 0.42));
	vec2 detailUV = dir * (dist * 12.5) + vec2(-time * 1.15, time * 0.86);
	float detailFlow = noise(vec3(detailUV + broadFlow * 0.85, time * 1.35));
	float filament = smoothstep(0.50, 0.72, detailFlow + broadFlow * 0.34);
	float flicker = 0.70 + 0.30 * sin(time * 9.0 + broadFlow * TAU * 2.0 + detailFlow * TAU);

	return vec2(radialAlpha * filament * flicker, filament);
}

bool shouldDiscardByRadius(float dist, float fieldRadiusWorld) {
	float outerReachWorld = edgeWidth * OUTER_MAX_REACH * 1.16;
	float outerLimit = RING + outerReachWorld / fieldRadiusWorld;

	return dist > outerLimit;
}

void main() {

	vec2 centeredUV = (fragUV - vec2(0.5, 0.5)) * 2.0;
	float dist = length(centeredUV);
	if (dist <= 0.0001) {
		discard;
	}

	vec2 dir = centeredUV / dist;
	float angle = atan(dir.y, dir.x);
	float fieldRadiusWorld = size.x * 0.5;
	float ringRadiusWorld = fieldRadiusWorld * RING;
	float distWorld = dist * fieldRadiusWorld;
	float signedWorld = distWorld - ringRadiusWorld;
	float arcWorld = angle * ringRadiusWorld;
	if (shouldDiscardByRadius(dist, fieldRadiusWorld)) {
		discard;
	}

	float lineHalfWidth = max(edgeWidth * 0.5, 0.25);
	float lineAA = max(fwidth(signedWorld) * 0.85, lineHalfWidth * 0.12);
	float attachAA = max(fwidth(signedWorld) * 0.75, lineHalfWidth * 0.14);
	float rootCoreWidth = max(lineHalfWidth * 0.85 + attachAA * 0.40, lineAA * 1.35);
	float flameRootOffset = 0.0;

	EventSample evt = emptyEventSample(angle);
	if (signedWorld <= rootCoreWidth * 1.5) {
		evt = eventPush(angle);
	}
	float innerRadial = max(-signedWorld - flameRootOffset, 0.0);
	vec4 impact = buildImpactProfile(evt, innerRadial, fieldRadiusWorld);
	vec4 noImpact = vec4(0.0);

	vec3 rootCoord = flameCoord(dir, arcWorld, 38.0, 115.0, 3.0);
	float rootHeat = clamp(0.58 + noise(rootCoord + vec3(0.0, 0.0, time * 0.12)) * 0.32 + impact.x * 0.55, 0.0, 1.0);
	float rootCore = exp(-pow(abs(signedWorld) / rootCoreWidth, 2.0)) * mix(0.94, 1.0, rootHeat);

	float outerAttach = smoothstep(-rootCoreWidth * 0.35, rootCoreWidth * 0.85, signedWorld);
	float innerAttach = 1.0 - smoothstep(-rootCoreWidth * 0.85, rootCoreWidth * 0.35, signedWorld);
	float outerRadial = max(signedWorld - flameRootOffset, 0.0);
	float outerMaxRadial = edgeWidth * OUTER_MAX_REACH * 1.16;
	float innerMaxRadial = max(edgeWidth * INNER_MAX_REACH * 1.16, impact.z + edgeWidth * 8.0);
	vec3 outer = vec3(0.0);
	if (outerAttach > 0.0 && outerRadial <= outerMaxRadial) {
		outer = sampleFire(dir, arcWorld, outerRadial, 1.0, noImpact, 0.0);
	}
	vec3 inner = vec3(0.0);
	if (innerAttach > 0.0 && innerRadial <= innerMaxRadial) {
		inner = sampleFire(dir, arcWorld, innerRadial, -1.0, impact, 8.0);
	}

	float lineDensity = clamp(rootCore * 0.58, 0.0, 0.82);
	float outerDensity = outer.x * outerAttach * 0.78;
	float innerDensity = inner.x * innerAttach * (0.84 + impact.x * 0.30);
	float density = clamp(lineDensity + outerDensity + innerDensity, 0.0, 0.92);
	float heat = clamp((rootHeat * lineDensity + outer.y * outerDensity + inner.y * innerDensity) / max(lineDensity + outerDensity + innerDensity, 0.001), 0.0, 1.0);

	vec2 veil = sampleVeil(dir, dist);
	density = clamp(density + veil.x, 0.0, 0.92);

	float coreMix = smoothstep(0.44, 0.96, heat) * smoothstep(0.28, 0.82, density);
	float edgeBias = 1.0 - smoothstep(0.18, 0.74, density);
	coreMix *= mix(0.55, 1.0, 1.0 - edgeBias);
	vec3 color = mix(edgeColor.rgb, coreColor.rgb, coreMix);
	color *= mix(0.68, 1.08, coreMix) * mix(0.78, 1.12, smoothstep(0.18, 0.90, density));
	vec3 veilColor = mix(edgeColor.rgb, coreColor.rgb, veil.y);
	color = mix(color, veilColor, clamp(veil.x * VEIL_COLOR_MIX, 0.0, VEIL_COLOR_MIX_MAX));

	float baseAlpha = mix(edgeColor.a, coreColor.a, coreMix);
	float alpha = clamp((pow(density, 1.12) + veil.x) * baseAlpha, 0.0, 1.0);
	if (alpha <= 0.001) discard;

	fragColor = vec4(color, alpha);
}
