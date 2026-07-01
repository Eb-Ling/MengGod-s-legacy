#version 110

varying vec2 MM_fargUV;
void main() {
    gl_Position = ftransform();
    MM_fargUV = gl_MultiTexCoord0.xy;
}









#version 430 core

uniform sampler2D MM_texture1;
uniform sampler2D MM_textureship;
uniform vec4 MM_texturecolor;
uniform vec2 MM_scale;


uniform float MM_commenmaxalphamult;
uniform float MM_hittedmaxalphamult;

uniform float MM_maxrange;
uniform float MM_innerrange;

uniform float MM_points_maxtime;

uniform float MM_scaneffectbool;
uniform float MM_scaneffectahphamult;
uniform float MM_scaneffectmaxrange;
uniform float MM_scaneffectminrange;

in vec2 MM_fargUV;
out vec4 FragColor;

layout(std430, binding = 1) buffer SSBO_data {float floatList[];};
uniform float MM_points_count;
void main() {
    vec2 scaledTexCoord = MM_fargUV * MM_scale * 2.0;
    vec4 v1 = texture(MM_texture1, scaledTexCoord);
    vec4 v2 = texture(MM_textureship, MM_fargUV);
    vec4 result = vec4(v1.xyx, v1.w  * v2.w);

    if(result.w != 0.0) {
        result = mix(vec4(result.xyz, result.w), vec4(MM_texturecolor.xyz, result.w), 0.6);
        result.w = result.w * MM_commenmaxalphamult;
        result.xyz = vec3((result.xyz - 0.5) * 1.3 + 0.5);
        //if(result.w > MM_maxalpha) {result.w = result.w * 0.3;}
        if(MM_scaneffectbool > 0.0 && MM_scaneffectbool < 1.0 + MM_scaneffectmaxrange){
            float dis = abs(MM_fargUV.y - MM_scaneffectbool);
            if(dis < MM_scaneffectmaxrange){
                float alpha = smoothstep(MM_scaneffectminrange, MM_scaneffectmaxrange, dis);
                if(result.w > MM_commenmaxalphamult * 0.35) {result.w = result.w + (MM_scaneffectahphamult * (1.0 - alpha));}
            }
        }

        if(MM_points_count > 0.0) {
            for(int i = 0; i < MM_points_count; i+=4) {
                vec2 point = vec2(floatList[i],floatList[i + 1]);
                float time = floatList[i + 2];
                float power = floatList[i + 3];

                float dis = distance(MM_fargUV,point);
                if(dis < MM_maxrange * power){
                    float alpha = smoothstep(MM_innerrange * power, MM_maxrange * power, dis);
                    if(result.w > MM_commenmaxalphamult * 0.35){result.w = result.w  + (MM_hittedmaxalphamult * (1.0 - alpha) * (time / MM_points_maxtime));}
                }
            }
        }
    }
    FragColor = result;
};




#version 430 core

uniform sampler2D MM_texture1;
uniform sampler2D MM_textureship;
uniform vec4 MM_texturecolor;
uniform vec2 MM_scale;
uniform vec2 MM_shipcenter;

uniform float MM_commonalpha;
uniform float MM_commonmaxalphamult;
uniform float MM_hittedmaxalphamult;

uniform float MM_change_level;

uniform float MM_maxrange;
uniform float MM_innerrange;

uniform float MM_points_maxtime;

uniform float MM_scaneffectbool;
uniform float MM_scaneffectahphamult;
uniform float MM_scaneffectmaxrange;
uniform float MM_scaneffectminrange;

uniform float MM_textureoffset1;
uniform float MM_textureoffset2;

in vec2 MM_fargUV;
out vec4 FragColor;

layout(std430, binding = 1) buffer SSBO_data {float floatList[];};
uniform float MM_points_count;
void main() {
    vec2 scaledTexCoord = MM_fargUV / 2 + vec2(MM_textureoffset1,MM_textureoffset2);//* MM_scale * 2.0+ MM_textureoffset
    vec4 v1 = texture(MM_texture1, scaledTexCoord);
    vec4 v2 = texture(MM_textureship, MM_fargUV);
    vec4 result = vec4(v1.xyx, v1.w * v2.w);
    result.w = result.w * MM_commonalpha;
    if(result.w != 0.0) {
        result = mix(vec4(result.xyz, result.w), vec4(MM_texturecolor.xyz, result.w), 0.7);
        result.w = result.w * MM_commonmaxalphamult;
        result.xyz = vec3((result.xyz - 0.5) * 1.3 + 0.5);
        result.w = result.w * MM_commonalpha;
        if(MM_commonalpha > 0.0){

            if(MM_points_count > 0.0) {
                for(int i = 0; i < MM_points_count; i+=4) {
                    vec2 point = vec2(floatList[i],floatList[i + 1]);
                    float time = floatList[i + 2];
                    float power = floatList[i + 3];

                    float dis = distance(MM_fargUV,point);
                    //float dis = abs(length(MM_fargUV-point) - ((MM_points_maxtime - time) / MM_points_maxtime));
                    if(dis < MM_maxrange * power){
                        float alpha = smoothstep(MM_innerrange, MM_maxrange * power, dis);
                        //if(result.w > MM_commonmaxalphamult * 0.35){
                            result.w = result.w  + (MM_hittedmaxalphamult * (1.0 - alpha) * (time / MM_points_maxtime));
                       //}
                    }
                }
            }

            if(MM_scaneffectbool > 0.0 && MM_scaneffectbool < 1.0 + MM_scaneffectmaxrange){
                float dis = abs(length(MM_fargUV - MM_shipcenter) - MM_scaneffectbool * 0.5);
                if(dis < MM_scaneffectmaxrange){
                    float alpha = smoothstep(MM_scaneffectminrange, MM_scaneffectmaxrange, dis);
                    //if(result.w > MM_commonmaxalphamult * 0.35) {
                        result.w = result.w + (MM_scaneffectahphamult * (1.0 - alpha));
                    //}
                }
            }
            result.w = result.w * MM_commonalpha;
        }
    }
    FragColor = result;
};