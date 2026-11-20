#include "/lib/ajustes.glsl"
#ifdef VSH
in vec4 mc_Entity;
out vec2 tc;
out vec4 col;
out float agua;
vec3 distorsion(vec3 p) {
    float f = length(p.xy) * 0.9 + 0.1;
    return vec3(p.xy / f, p.z * 0.25);
}
void main() {
    tc = gl_MultiTexCoord0.xy;
    col = gl_Color;
    agua = abs(mc_Entity.x - 10001.0) < 0.5 ? 1.0 : 0.0;
    vec4 p = ftransform();
    p.xyz = distorsion(p.xyz);
    gl_Position = p;
}
#endif
#ifdef FSH
uniform sampler2D gtexture;
in vec2 tc;
in vec4 col;
in float agua;
void main() {
    if (agua > 0.5) discard;
    vec4 a = texture(gtexture, tc) * col;
    if (a.a < 0.1) discard;
    gl_FragData[0] = a;
}
#endif
