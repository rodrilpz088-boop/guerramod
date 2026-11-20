// Cielo de Minecraft: solo se aprovechan las estrellas y la luna (el resto lo calcula deferred).
#include "/lib/ajustes.glsl"
#include "/lib/comun.glsl"
#ifdef VSH
out vec2 tc;
out vec4 col;
void main() {
    tc = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    col = gl_Color;
    gl_Position = ftransform();
}
#endif
#ifdef FSH
uniform int renderStage;
uniform sampler2D gtexture;
in vec2 tc;
in vec4 col;
void main() {
    /* DRAWBUFFERS:0 */
#if defined NUBES_VANILLA
    discard;
#elif defined CIELO_BASICO
    if (renderStage != MC_RENDER_STAGE_STARS) discard;
    gl_FragData[0] = vec4(col.rgb * col.a * 0.5, 1.0);
#else
    vec4 a = texture(gtexture, tc) * col;
#ifdef DIM_END
    gl_FragData[0] = vec4(lineal(a.rgb) * 0.35, 1.0);
#else
    if (renderStage == MC_RENDER_STAGE_SUN) discard;
    gl_FragData[0] = vec4(lineal(a.rgb) * a.a * 0.22, a.a);
#endif
#endif
}
#endif
