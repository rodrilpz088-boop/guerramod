// Partículas, líneas, lluvia, brillos y cosas que brillan solas: se iluminan directamente.
#include "/lib/ajustes.glsl"
#include "/lib/comun.glsl"

#ifdef VSH
out vec2 tc;
out vec2 lm;
out vec4 col;
out vec3 nor;
void main() {
    tc = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    lm = clamp((gl_TextureMatrix[1] * gl_MultiTexCoord1).xy, 0.0, 1.0);
    col = gl_Color;
    nor = normalize(mat3(gbufferModelViewInverse) * (gl_NormalMatrix * gl_Normal));
    gl_Position = ftransform();
}
#endif

#ifdef FSH
#include "/lib/lut.glsl"
#include "/lib/luz.glsl"
uniform sampler2D gtexture;
in vec2 tc;
in vec2 lm;
in vec4 col;
in vec3 nor;
void main() {
    /* DRAWBUFFERS:0 */
#if defined BASICO
    gl_FragData[0] = vec4(lineal(col.rgb), col.a);
#else
    vec4 a = texture(gtexture, tc) * col;
    if (a.a < 0.01) discard;
#if defined EMISIVO
    gl_FragData[0] = vec4(lineal(a.rgb) * 1.6, a.a);
#elif defined CLIMA
    // lluvia: gotas finas y casi transparentes; nieve: copos blancos
    vec3 amb = especial(2) * 1.6 + especial(0) * 0.03;
    bool lluvia = a.b > a.r * 1.05;
    if (lluvia) gl_FragData[0] = vec4(lineal(vec3(0.72, 0.77, 0.83)) * amb, a.a * 0.28);
    else gl_FragData[0] = vec4(lineal(a.rgb) * amb * 1.2, a.a);
#else
    gl_FragData[0] = vec4(luzAdelante(lineal(a.rgb), vec3(0.0, 1.0, 0.0), lm), a.a);
#endif
#endif
}
#endif
