// Ilumina lo opaco (sol con sombras, cielo, antorchas), moja el suelo con la lluvia y dibuja el cielo y las nubes.
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
#include "/lib/lut.glsl"
#include "/lib/sombras.glsl"
#include "/lib/nubes.glsl"
uniform sampler2D colortex0;
uniform sampler2D colortex1;
uniform sampler2D colortex2;
uniform sampler2D depthtex0;
in vec2 uv;

vec3 cielo(vec3 d, vec3 estrellas) {
#if defined DIM_NETHER
    return lineal(fogColor) * 0.6;
#elif defined DIM_END
    return estrellas + vec3(0.004, 0.003, 0.007);
#else
    vec3 s = dirSolMundo();
    vec3 c = cieloLUT(d);
    // disco del sol con oscurecimiento del borde
    float ang = acos(clamp(dot(d, s), -1.0, 1.0));
    float radio = 0.0125;
    if (ang < radio * 1.2) {
        float r = ang / radio;
        float limbo = sqrt(max(1.0 - r * r, 0.0));
        float disco = smoothstep(1.15, 0.95, r) * (0.4 + 0.6 * limbo);
        c += especial(4) * 22.0 * disco * (1.0 - rainStrength) * smoothstep(-0.03, 0.0, d.y);
    }
    float noche = smoothstep(0.05, -0.15, s.y);
    c += estrellas * noche * (1.0 - rainStrength);
#ifdef NUBES_VOLUMETRICAS
    vec4 nb = nubes(d, cameraPosition + vec3(0.0, 1.62, 0.0), s.y > -0.1 ? s : dirLunaMundo(), especial(1), especial(2),
                    DENSIDAD_NUBES + 0.38 * rainStrength, gl_FragCoord.xy);
    c = c * nb.a + nb.rgb * (1.0 - 0.55 * rainStrength);
#endif
    return c;
#endif
}

vec3 niebla(vec3 c, float dist, vec3 d, float cieloL) {
#if defined DIM_NETHER
    float f = 1.0 - exp(-dist * 0.012);
    return mix(c, lineal(fogColor) * 0.6, max(f, smoothstep(far * 0.6, far, dist)));
#elif defined DIM_END
    return mix(c, vec3(0.004, 0.003, 0.007), smoothstep(far * 0.5, far, dist));
#else
    // bruma del aire, más con lluvia y por la mañana en zonas húmedas
    float hora = float(worldTime);
    float manana = smoothstep(22500.0, 23500.0, hora) + (1.0 - smoothstep(500.0, 2500.0, hora)) * step(hora, 12000.0);
    float dens = (0.00035 + 0.0028 * rainStrength + 0.0035 * manana * rainfall * rainfall) * INTENSIDAD_BRUMA;
    float f = 1.0 - exp(-dist * dens);
    f = max(f, smoothstep(far * 0.72, far, dist));
    vec3 col = cieloLUT(normalize(vec3(d.x, max(d.y, 0.03), d.z)));
    col = mix(vec3(0.0006), col, max(exterior(), cieloL * cieloL));
    return mix(c, col, f);
#endif
}

void main() {
    float prof = texture(depthtex0, uv).r;
    vec3 vista = vistaDesde(uv, prof);
    vec3 dir = normalize(mat3(gbufferModelViewInverse) * vista);
    vec4 c0 = texture(colortex0, uv);
    vec3 color;
    vec4 reflejo = vec4(0.0);
    if (prof >= 1.0) {
        color = cielo(dir, c0.rgb);
    } else {
        vec4 c1 = texture(colortex1, uv);
        vec2 lm = texture(colortex2, uv).rg;
        vec3 n = normalize(c1.rgb * 2.0 - 1.0);
        int mat = int(c1.a * 16.0);
        vec3 alb = lineal(c0.rgb);
        vec3 jug = aJugador(vista);
        float dist = length(vista);
#if !defined DIM_NETHER && !defined DIM_END
        // ---- suelo mojado y charcos (solo al aire libre y donde llueve)
        float mojado = wetness * float(biome_precipitation == 1) * smoothstep(0.86, 0.95, lm.y);
        if (mat == 3) mojado *= 0.4;
        if (mojado > 0.001) {
            float poroso = (mat == 6 || mat == 2) ? 1.0 : 0.0;
            alb *= mix(1.0, mix(0.72, 0.55, poroso), mojado);
            alb = mix(vec3(lum(alb)), alb, 1.0 + 0.25 * mojado);
#ifdef CHARCOS
            if (n.y > 0.9 && mat != 2 && mat != 3) {
                vec2 p = (jug.xz + cameraPosition.xz) / 9.0;
                float r = texture(noisetex, p / 16.0).r * 0.6 + texture(noisetex, p / 5.0).g * 0.3 + texture(noisetex, p * 0.6).b * 0.1;
                float charco = smoothstep(0.62 - 0.12 * mojado, 0.70 - 0.12 * mojado, r) * mojado * (1.0 - 0.6 * poroso);
                alb *= mix(1.0, 0.45, charco);
                float refl = mix(0.15 * mojado * (1.0 - 0.7 * poroso), 1.0, charco);
                reflejo = vec4(refl, mix(0.35, 0.02, charco), charco, 1.0);
            } else {
                reflejo = vec4(0.08 * mojado * (1.0 - poroso), 0.4, 0.0, 1.0);
            }
#endif
        }
#endif
        // ---- luz
#if defined DIM_NETHER || defined DIM_END
        vec3 amb = especial(2);
        vec3 luz = amb * (0.7 + 0.3 * n.y);
#else
        vec3 L = dirLuzMundo();
        float NdotL = dot(n, L);
        float trasluz = (mat == 2 || mat == 3) ? 1.0 : 0.0;
        float sol = 0.0;
        if (NdotL > 0.0 || trasluz > 0.5) sol = sombra(jug, n * sign(max(NdotL, 0.001)), gl_FragCoord.xy);
        if (dist > shadowDistance) sol = smoothstep(0.85, 0.97, lm.y);
        float difusa = max(NdotL, 0.0) + trasluz * (0.35 + 0.45 * max(-NdotL, 0.0));
        vec3 directa = especial(0) * (SOL_K / PI) * difusa * sol;
        vec3 amb = especial(2) * pow(lm.y, 2.5) * (0.55 + 0.45 * n.y);
        vec3 luz = directa + amb;
#endif
        luz += vec3(1.0, 0.58, 0.28) * (pow(lm.x, 4.0) * 0.07 + pow(lm.x, 1.5) * 0.004);
        luz += vec3(0.0006, 0.0007, 0.0009);
        color = alb * luz;
        if (mat == 5) color += alb * 0.5;
        if (mat == 4) color += alb * 1.4;
        color = niebla(color, dist, dir, lm.y);
    }
    /* DRAWBUFFERS:05 */
    gl_FragData[0] = vec4(color, 1.0);
    gl_FragData[1] = reflejo;
}
#endif
