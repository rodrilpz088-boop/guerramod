// Calcula la tabla del cielo (256x128) y la luz del sol, de la luna y del cielo para este fotograma.
#include "/lib/ajustes.glsl"
#include "/lib/comun.glsl"
#ifdef VSH
out vec2 uv;
void main() {
    gl_Position = ftransform();
    uv = gl_MultiTexCoord0.xy;
}
#endif
#ifdef FSH
#include "/lib/atmosfera.glsl"
in vec2 uv;
const vec2 LUT = vec2(256.0, 128.0);
vec3 lutDir(vec2 t, vec3 s) {
    float az = (t.x - 0.5) * 2.0 * PI + atan(s.z, s.x);
    float k = t.y * 2.0 - 1.0;
    float el = sign(k) * k * k * 0.5 * PI;
    return vec3(cos(el) * cos(az), sin(el), cos(el) * sin(az));
}
void main() {
    vec2 fc = gl_FragCoord.xy;
    if (fc.x > 256.0 || fc.y > 129.0) discard;
    vec3 c = vec3(0.0);
#if defined DIM_NETHER
    vec3 niebla = lineal(fogColor);
    if (fc.y < 128.0) c = niebla * 0.6;
    else {
        int i = int(fc.x);
        if (i == 2) c = niebla * 0.5 + vec3(0.02, 0.012, 0.008);
        else if (i == 3) c = vec3(6.0);
    }
#elif defined DIM_END
    if (fc.y < 128.0) c = vec3(0.004, 0.003, 0.007);
    else {
        int i = int(fc.x);
        if (i == 2) c = vec3(0.035, 0.028, 0.05);
        else if (i == 3) c = vec3(9.0);
    }
#else
    vec3 s = dirSolMundo();
    vec3 m = dirLunaMundo();
    float turb = 1.2 + 1.5 * rainStrength + 0.8 * wetness + 0.6 * rainfall;
    float luna = brilloLuna();
    float cubierto = max(rainStrength, smoothstep(0.55, 1.0, nubosidad()) * 0.75);
    if (fc.y < 128.0) {
        vec3 d = lutDir(fc / LUT, s);
        c = nublado(cieloFisico(d, s, m, turb, luna), cubierto);
    } else {
        int i = int(fc.x);
        vec3 L = dirLuzMundo();
        bool esSol = dot(L, s) > 0.5;
        if (i == 0) {
            vec3 t = esSol ? transmitancia(300.0, s, turb) * E_SOL : transmitancia(300.0, m, turb) * E_SOL * 0.0016 * luna;
            c = t * smoothstep(-0.01, 0.06, L.y) * (1.0 - 0.88 * rainStrength);
        } else if (i == 1) {
            c = transmitancia(2000.0, s, turb) * E_SOL + transmitancia(2000.0, m, turb) * E_SOL * 0.0016 * luna;
        } else if (i == 2) {
            vec3 z = nublado(cieloFisico(vec3(0.0, 1.0, 0.0), s, m, turb, luna), cubierto);
            vec3 h1 = nublado(cieloFisico(normalize(vec3(s.x, 0.15, s.z) + vec3(0.0001)), s, m, turb, luna), cubierto);
            vec3 h2 = nublado(cieloFisico(normalize(vec3(-s.x, 0.15, -s.z) + vec3(0.0001)), s, m, turb, luna), cubierto);
            c = z * 0.5 + (h1 + h2) * 0.25;
        } else if (i == 3) {
            vec3 z = nublado(cieloFisico(vec3(0.0, 1.0, 0.0), s, m, turb, luna), cubierto);
            c = vec3(clamp(0.82 / pow(lum(z) + 0.0034, 0.75), 1.0, 40.0));
        } else if (i == 4) {
            c = transmitancia(300.0, s, turb) * E_SOL;
        }
    }
#endif
    /* DRAWBUFFERS:7 */
    gl_FragData[0] = vec4(c, 1.0);
}
#endif
