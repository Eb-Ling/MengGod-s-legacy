#version 420
layout (location = 0) in vec2 a_position;
layout (std140, binding = 0) uniform BUtilGlobalData
{
    mat4 gameViewport;
    vec4 gameScreenBorder;
};
uniform mat4 modelMatrix;
uniform vec2 size;
out vec2 v_uv;
out vec2 v_screenUV;
void main() {
    v_uv = (a_position + 1.0) * 0.5;
    vec4 resized = vec4(a_position * size * 0.5, 0.0, 1.0);
    gl_Position = gameViewport * vec4((modelMatrix * resized).xy, 0.0, 1.0);
    // Screen-space UV from clip position for background sampling
    v_screenUV = gl_Position.xy / gl_Position.w * 0.5 + 0.5;
}

