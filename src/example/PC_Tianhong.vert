#version 110

uniform vec4 u_quad;

varying vec2 fragUV;

void main() {
    gl_Position = ftransform();
    fragUV = (gl_Vertex.xy - u_quad.xy) / max(u_quad.zw, vec2(0.0001, 0.0001));
}
