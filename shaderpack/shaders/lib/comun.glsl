#ifndef COMUN
#define COMUN
uniform mat4 gbufferModelView;
uniform mat4 gbufferModelViewInverse;
uniform mat4 gbufferProjection;
uniform mat4 gbufferProjectionInverse;
uniform mat4 shadowModelView;
uniform mat4 shadowProjection;
uniform vec3 cameraPosition;
uniform vec3 sunPosition;
uniform vec3 moonPosition;
uniform vec3 shadowLightPosition;
uniform vec3 fogColor;
uniform float frameTimeCounter;
uniform float rainStrength;
uniform float wetness;
uniform float viewWidth;
uniform float viewHeight;
uniform float near;
uniform float far;
uniform float rainfall;
uniform int isEyeInWater;
uniform int worldTime;
uniform int worldDay;
uniform int moonPhase;
uniform int biome_precipitation;
uniform ivec2 eyeBrightnessSmooth;

const float PI = 3.14159265;

vec3 proyectar(mat4 m, vec3 p) {
    vec4 h = m * vec4(p, 1.0);
    return h.xyz / h.w;
}
vec3 vistaDesde(vec2 uv, float d) {
    return proyectar(gbufferProjectionInverse, vec3(uv, d) * 2.0 - 1.0);
}
vec3 aJugador(vec3 v) {
    return mat3(gbufferModelViewInverse) * v + gbufferModelViewInverse[3].xyz;
}
vec3 lineal(vec3 c) {
    return pow(max(c, vec3(0.0)), vec3(2.2));
}
float lum(vec3 c) {
    return dot(c, vec3(0.2126, 0.7152, 0.0722));
}
vec3 dirSolMundo() {
    return normalize(mat3(gbufferModelViewInverse) * sunPosition);
}
vec3 dirLunaMundo() {
    return normalize(mat3(gbufferModelViewInverse) * moonPosition);
}
vec3 dirLuzMundo() {
    return normalize(mat3(gbufferModelViewInverse) * shadowLightPosition);
}
float ruidoPantalla(vec2 p) {
    return fract(52.9829189 * fract(dot(p, vec2(0.06711056, 0.00583715))));
}
float brilloLuna() {
    int f = moonPhase;
    return 1.0 - float(min(f, 8 - f)) / 4.0;
}
// fracción de "aire libre" del jugador (0 en cuevas, 1 fuera)
float exterior() {
    return clamp(float(eyeBrightnessSmooth.y) / 240.0, 0.0, 1.0);
}
// ---------------------------------------------------------------- tiempo del día: unos días despejados, otros nublados
float hash1(float n) {
    return fract(sin(n * 12.9898 + 4.1414) * 43758.5453);
}
// cobertura de nubes (0 despejado .. 1 cubierto); cambia poco a poco de un día a otro, y la lluvia lo cubre todo
float nubosidad() {
#if TIEMPO == 1
    float c = 0.0;
#elif TIEMPO == 2
    float c = 0.45;
#elif TIEMPO == 3
    float c = 0.85;
#else
    float t = float(worldDay) + float(worldTime) / 24000.0;
    float d = floor(t);
    float f = fract(t);
    f = f * f * (3.0 - 2.0 * f);
    float c = mix(hash1(d), hash1(d + 1.0), f);
    c = pow(c, 1.6) * 0.9;
#endif
    c = mix(c, 0.95, rainStrength);
    return clamp(c * CANTIDAD_NUBES, 0.0, 1.0);
}
// cirros altos (velo de buen tiempo) según el día
float cirros() {
    float t = float(worldDay) + float(worldTime) / 24000.0;
    float d = floor(t);
    float f = fract(t);
    return mix(hash1(d + 71.0), hash1(d + 72.0), f * f * (3.0 - 2.0 * f));
}
#endif
