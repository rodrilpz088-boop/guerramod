// Agua (la sombrea composite con reflejos y refracción) y demás traslúcidos (cristal, hielo...).
#include "/lib/ajustes.glsl"
#include "/lib/comun.glsl"

#ifdef VSH
#ifndef MANO
in vec4 mc_Entity;
#endif
out vec2 tc;
out vec2 lm;
out vec4 col;
out vec3 nor;
out vec3 mundo;
out float agua;

void main() {
    tc = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    lm = clamp((gl_TextureMatrix[1] * gl_MultiTexCoord1).xy, 0.0, 1.0);
    col = gl_Color;
    nor = normalize(mat3(gbufferModelViewInverse) * (gl_NormalMatrix * gl_Normal));
    vec4 vista = gl_ModelViewMatrix * gl_Vertex;
    mundo = (gbufferModelViewInverse * vista).xyz + cameraPosition;
    agua = 0.0;
#ifndef MANO
    if (abs(mc_Entity.x - 10001.0) < 0.5) agua = 1.0;
#endif
    gl_Position = gl_ProjectionMatrix * vista;
}
#endif

#ifdef FSH
#include "/lib/lut.glsl"
#include "/lib/luz.glsl"
#include "/lib/agua.glsl"
uniform sampler2D gtexture;
in vec2 tc;
in vec2 lm;
in vec4 col;
in vec3 nor;
in vec3 mundo;
in float agua;

void main() {
    /* DRAWBUFFERS:046 */
    if (agua > 0.5) {
        vec3 n = normalize(nor);
        if (n.y > 0.5) {
            float lluvia = rainStrength * float(biome_precipitation == 1) * smoothstep(0.85, 0.95, lm.y);
            n = normalAgua(mundo.xz, frameTimeCounter, lluvia);
        }
        gl_FragData[0] = vec4(0.0);
        gl_FragData[1] = vec4(n * 0.5 + 0.5, 1.0);
        gl_FragData[2] = vec4(col.rgb, lm.y);
    } else {
        vec4 a = texture(gtexture, tc) * col;
        if (a.a < 0.01) discard;
        vec3 c = luzAdelante(lineal(a.rgb), normalize(nor), lm);
        gl_FragData[0] = vec4(c, a.a);
        gl_FragData[1] = vec4(normalize(nor) * 0.5 + 0.5, 0.5);
        gl_FragData[2] = vec4(a.rgb, lm.y);
    }
}
#endif
