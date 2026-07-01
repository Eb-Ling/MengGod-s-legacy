#version 110

varying vec2 fargUV;
void main() {
    gl_Position = ftransform();
    fargUV = gl_MultiTexCoord0.xy;
}




#version 430 core

uniform sampler2D texture1;
uniform vec4 basecolor;

uniform float commonalphamult;

uniform float scaneffectbool;
uniform float scaneffectahphamult;
uniform float scaneffectmaxrange;
uniform float scaneffectminrange;

uniform float expire_alpha;

in vec2 fargUV;
out vec4 FragColor;

void main() {

    vec4 v1 = texture(texture1,fargUV);
    vec4 result = v1;
    if(result.x > 0.95 && result.y > 0.95 && result.z > 0.95){
        result = mix(vec4(result.xyz, result.w), vec4(basecolor.xyz, result.w), 0.6);
    }

    if(result.w > 0.0){
        result.w = result.w * commonalphamult;
        float dis = distance(fargUV , vec2(0.5,0.5));

        if(dis < scaneffectbool){
            result.w = result.w + result.w * scaneffectahphamult;
        }

        float abs1 = abs(dis - scaneffectbool);
        if(abs1 > scaneffectminrange && abs1 < scaneffectmaxrange ) {
            float alpha = smoothstep(scaneffectminrange, scaneffectmaxrange, abs1);
            result.w = result.w + result.w * (scaneffectahphamult * alpha);
        }
    }
    result.w = result.w * expire_alpha;
    FragColor = result;
};