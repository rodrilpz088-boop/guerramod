// Agua con refracción, absorción, reflejos reales (SSR) y brillo del sol; reflejos en charcos; bajo el agua.
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
#include "/lib/agua.glsl"
uniform sampler2D colortex0;
uniform sampler2D colortex4;
uniform sampler2D colortex5;
uniform sampler2D colortex6;
uniform sampler2D depthtex0;
uniform sampler2D depthtex1;
in vec2 uv;

// reflejos en pantalla: avanza por el rayo reflejado hasta chocar con algo visible
vec4 ssr(vec3 origen, vec3 dir) {
#ifdef REFLEJOS_SSR
    vec3 paso = dir * (0.35 + 0.015 * length(origen));
    vec3 p = origen + paso * (0.5 + 0.5 * ruidoPantalla(gl_FragCoord.xy));
    for (int i = 0; i < 26; i++) {
        p += paso;
        vec3 s = proyectar(gbufferProjection, p) * 0.5 + 0.5;
        if (s.x < 0.0 || s.x > 1.0 || s.y < 0.0 || s.y > 1.0 || s.z > 1.0 || p.z > -near) return vec4(0.0);
        float dz = texture(depthtex1, s.xy).r;
        if (dz < 1.0) {
            vec3 q = vistaDesde(s.xy, dz);
            float dif = q.z - p.z;
            if (dif > 0.0 && dif < length(paso) * 2.0 + 0.3) {
                for (int j = 0; j < 5; j++) {
                    paso *= 0.5;
                    s = proyectar(gbufferProjection, p) * 0.5 + 0.5;
                    q = vistaDesde(s.xy, texture(depthtex1, s.xy).r);
                    p += (q.z - p.z > 0.0) ? -paso : paso;
                }
                s = proyectar(gbufferProjection, p) * 0.5 + 0.5;
                vec2 b = min(s.xy, 1.0 - s.xy);
                float borde = smoothstep(0.0, 0.08, min(b.x, b.y));
                return vec4(texture(colortex0, s.xy).rgb, borde);
            }
        }
        paso *= 1.16;
    }
#endif
    return vec4(0.0);
}

float ggx(vec3 n, vec3 v, vec3 l, float r) {
    vec3 h = normalize(v + l);
    float nh = max(dot(n, h), 0.0);
    float nl = max(dot(n, l), 0.0);
    float nv = max(dot(n, v), 0.001);
    float a2 = r * r * r * r;
    float dd = nh * nh * (a2 - 1.0) + 1.0;
    float D = a2 / (PI * dd * dd);
    float k = r * r * 0.5;
    float G = nl / (nl * (1.0 - k) + k) * nv / (nv * (1.0 - k) + k);
    float F = 0.02 + 0.98 * pow(1.0 - max(dot(h, v), 0.0), 5.0);
    return D * G * F / (4.0 * nv);
}

vec3 cieloReflejado(vec3 rM, float cieloL) {
#if defined DIM_NETHER
    return lineal(fogColor) * 0.5;
#elif defined DIM_END
    return vec3(0.004, 0.003, 0.007);
#else
    vec3 c = cieloLUT(normalize(vec3(rM.x, max(rM.y, 0.02), rM.z)));
    return c * smoothstep(0.4, 0.9, cieloL) + vec3(0.0004);
#endif
}

void main() {
    vec3 col = texture(colortex0, uv).rgb;
    float d0 = texture(depthtex0, uv).r;
    float d1 = texture(depthtex1, uv).r;
    vec4 w = texture(colortex4, uv);
    vec3 v0 = vistaDesde(uv, d0);
    vec3 dirV = normalize(v0);
    vec3 dirM = mat3(gbufferModelViewInverse) * dirV;
    if (isEyeInWater == 0) {
        if (w.a > 0.9) {
            // ---------------- agua
            vec3 n = normalize(w.rgb * 2.0 - 1.0);
            vec3 nV = normalize(mat3(gbufferModelView) * n);
            vec4 t6 = texture(colortex6, uv);
            vec3 tinte = lineal(t6.rgb);
            float cieloL = t6.a;
            // refracción: el fondo se ve desplazado por las olas
            vec2 uvR = uv + nV.xy * 0.035 / (1.0 + length(v0) * 0.05);
            float d1R = texture(depthtex1, uvR).r;
            if (d1R < d0) {
                uvR = uv;
                d1R = d1;
            }
            vec3 fondo = texture(colortex0, uvR).rgb;
            float prof = d1R >= 1.0 ? 80.0 : distance(v0, vistaDesde(uvR, d1R));
            // el agua absorbe primero el rojo: cuanto más honda, más azul verdosa y oscura
            vec3 absorcion = (1.0 - tinte) * 0.32 + vec3(0.02);
            vec3 T = exp(-absorcion * prof);
            vec3 L = dirLuzMundo();
            vec3 luzAmb = especial(2) * pow(cieloL, 2.0) + especial(0) * (SOL_K / PI) * max(L.y, 0.0) * 0.5;
            vec3 dispersa = tinte * luzAmb * 0.55 * (1.0 - T);
            vec3 refr = fondo * T + dispersa;
            // reflejo
            vec3 rV = reflect(dirV, nV);
            vec3 rM = mat3(gbufferModelViewInverse) * rV;
            vec4 sr = ssr(v0, rV);
            vec3 refl = mix(cieloReflejado(rM, cieloL), sr.rgb, sr.a);
            float F = 0.02 + 0.98 * pow(1.0 - max(dot(-dirV, nV), 0.0), 5.0);
            vec3 c = mix(refr, refl, F);
            // brillo del sol
#if !defined DIM_NETHER && !defined DIM_END
            vec3 jug = aJugador(v0);
            float sol = sombra(jug, n, gl_FragCoord.xy);
            c += especial(0) * SOL_K * ggx(n, -dirM, L, 0.12) * max(dot(n, L), 0.0) * sol;
#endif
            // bruma hasta la superficie
            float dist = length(v0);
            float dens = (0.00035 + 0.0028 * rainStrength) * INTENSIDAD_BRUMA;
            float f = max(1.0 - exp(-dist * dens), smoothstep(far * 0.72, far, dist));
            col = mix(c, cieloReflejado(vec3(dirM.x, max(dirM.y, 0.03), dirM.z), 1.0) * max(exterior(), cieloL), f);
        } else if (w.a > 0.4) {
            // cristal: un poco de reflejo del cielo
            vec3 n = normalize(w.rgb * 2.0 - 1.0);
            vec3 nV = normalize(mat3(gbufferModelView) * n);
            float F = 0.04 + 0.96 * pow(1.0 - max(dot(-dirV, nV), 0.0), 5.0);
            vec3 rM = mat3(gbufferModelViewInverse) * reflect(dirV, nV);
            col = mix(col, cieloReflejado(rM, texture(colortex6, uv).a), F * 0.6);
        } else if (d0 < 1.0) {
            // ---------------- suelo mojado y charcos
            vec4 m = texture(colortex5, uv);
            if (m.r > 0.01) {
                vec3 jug = aJugador(v0);
                vec3 n = vec3(0.0, 1.0, 0.0);
                if (m.b > 0.05) n = normalAgua((jug.xz + cameraPosition.xz), frameTimeCounter * 0.4, rainStrength * 2.0);
                n = normalize(mix(vec3(0.0, 1.0, 0.0), n, 0.35));
                vec3 nV = normalize(mat3(gbufferModelView) * n);
                vec3 rV = reflect(dirV, nV);
                vec3 rM = mat3(gbufferModelViewInverse) * rV;
                vec4 sr = ssr(v0, rV);
                vec3 refl = mix(cieloReflejado(rM, 1.0), sr.rgb, sr.a);
                float F = 0.02 + 0.98 * pow(1.0 - max(dot(-dirV, nV), 0.0), 5.0);
                float k = m.r * mix(F, 0.15 + 0.85 * F, m.b);
                col = mix(col, refl, clamp(k, 0.0, 1.0));
            }
        }
    } else if (isEyeInWater == 1) {
        // ---------------- bajo el agua: el color se pierde con la distancia (primero el rojo)
        float dist = length(v0);
        vec3 absorcion = vec3(0.30, 0.075, 0.055);
        vec3 T = exp(-absorcion * dist);
        vec3 luzAmb = especial(2) * exterior() + especial(0) * (SOL_K / PI) * 0.4;
        col = col * T + vec3(0.04, 0.16, 0.18) * luzAmb * (1.0 - T);
    } else if (isEyeInWater == 2) {
        col = mix(col, vec3(1.2, 0.3, 0.02), 1.0 - exp(-length(v0) * 0.9));
    } else if (isEyeInWater == 3) {
        col = mix(col, vec3(0.6, 0.65, 0.7) * especial(2) * 3.0, 1.0 - exp(-length(v0) * 0.8));
    }
    /* DRAWBUFFERS:0 */
    gl_FragData[0] = vec4(col, 1.0);
}
#endif
