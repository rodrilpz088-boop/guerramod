// G-buffer de lo opaco: terreno, entidades, bloques con entidad y la mano.
#include "/lib/ajustes.glsl"
#include "/lib/comun.glsl"

#ifdef VSH
#ifdef TERRENO
in vec4 mc_Entity;
in vec4 mc_midTexCoord;
#endif
out vec2 tc;
out vec2 lm;
out vec4 col;
out vec3 nor;
out float mat;

vec2 viento(vec3 p, float cielo) {
    float t = frameTimeCounter;
    // rachas: el viento sube y baja despacio y recorre el paisaje como una ola
    float racha = 0.55 + 0.45 * sin(t * 0.37 + p.x * 0.045 + p.z * 0.031) * sin(t * 0.21 + p.z * 0.027);
    float fuerza = (0.045 + 0.11 * rainStrength) * cielo * (0.6 + 0.8 * racha);
    return vec2(sin(t * 1.7 + p.x * 0.55 + p.z * 0.31) + 0.4 * sin(t * 3.1 + p.z * 1.3) + 0.25 * sin(t * 7.3 + p.x * 2.9 + p.y * 1.7),
                sin(t * 1.3 + p.z * 0.63 + p.x * 0.22) + 0.4 * sin(t * 2.7 + p.x * 1.1) + 0.25 * sin(t * 6.1 + p.z * 3.1 + p.y * 1.3)) * fuerza;
}

void main() {
    tc = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    lm = clamp((gl_TextureMatrix[1] * gl_MultiTexCoord1).xy, 0.0, 1.0);
    col = gl_Color;
    nor = normalize(mat3(gbufferModelViewInverse) * (gl_NormalMatrix * gl_Normal));
    mat = 0.0;
    vec4 vista = gl_ModelViewMatrix * gl_Vertex;
#ifdef TERRENO
    float id = mc_Entity.x;
    if (abs(id - 10002.0) < 0.5 || abs(id - 10004.0) < 0.5) mat = 2.0;
    else if (abs(id - 10003.0) < 0.5) mat = 3.0;
    else if (abs(id - 10005.0) < 0.5) mat = 4.0;
    else if (abs(id - 10006.0) < 0.5) mat = 5.0;
    else if (abs(id - 10008.0) < 0.5) mat = 6.0;
#ifdef PLANTAS_ONDULANTES
    if (mat > 1.5 && mat < 3.5) {
        vec3 jug = (gbufferModelViewInverse * vista).xyz;
        vec3 pa = jug + cameraPosition;
        bool arriba = gl_MultiTexCoord0.t < mc_midTexCoord.t || abs(id - 10004.0) < 0.5;
        if (mat < 2.5 && arriba) jug.xz += viento(pa, lm.y);
        if (mat > 2.5) jug += vec3(viento(pa, lm.y).x, viento(pa.zyx, lm.y).y * 0.35, viento(pa, lm.y).y) * 0.9;
        vista = gbufferModelView * vec4(jug, 1.0);
    }
#endif
#endif
    gl_Position = gl_ProjectionMatrix * vista;
}
#endif

#ifdef FSH
#ifdef MANO_SOLIDA
#include "/lib/lut.glsl"
#include "/lib/luz.glsl"
#endif
uniform sampler2D gtexture;
#ifdef ENTIDAD
uniform vec4 entityColor;
#endif
in vec2 tc;
in vec2 lm;
in vec4 col;
in vec3 nor;
in float mat;

void main() {
    vec4 a = texture(gtexture, tc) * col;
    if (a.a < 0.1) discard;
#ifdef ENTIDAD
    a.rgb = mix(a.rgb, entityColor.rgb, entityColor.a);
#endif
#ifdef MANO_SOLIDA
    /* DRAWBUFFERS:0125 */
    gl_FragData[0] = vec4(luzAdelante(lineal(a.rgb), normalize(nor), lm), 1.0);
    gl_FragData[1] = vec4(normalize(nor) * 0.5 + 0.5, 15.5 / 16.0);
    gl_FragData[2] = vec4(lm, 0.0, 1.0);
    gl_FragData[3] = vec4(0.0);
#else
    /* DRAWBUFFERS:012 */
    gl_FragData[0] = vec4(a.rgb, 1.0);
    gl_FragData[1] = vec4(normalize(nor) * 0.5 + 0.5, (mat + 0.5) / 16.0);
    gl_FragData[2] = vec4(lm, 0.0, 1.0);
#endif
}
#endif
