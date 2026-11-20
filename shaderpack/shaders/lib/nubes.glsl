#ifndef NUBES
#define NUBES
#ifndef RUIDO_TEX
#define RUIDO_TEX
uniform sampler2D noisetex;
#endif
// Nubes volumétricas: cúmulos con base plana y cimas redondeadas que crecen con la cobertura, bordes deshilachados,
// luz del sol que se dispersa dentro de la nube (varios rebotes aproximados), oscurecimiento por dentro y ribete
// plateado al mirar hacia el sol. Encima, un velo de cirros. Todo se mueve con el viento.
const float NUBE_BASE = 230.0;
const float NUBE_TECHO = 420.0;
const float CIRRO_ALTURA = 1100.0;

float ruido3(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    vec2 uv = i.xz + f.xz + vec2(37.0, 17.0) * i.y;
    float a = texture(noisetex, (uv + 0.5) / 256.0).r;
    float b = texture(noisetex, (uv + vec2(37.0, 17.0) + 0.5) / 256.0).r;
    return mix(a, b, f.y);
}
vec3 vientoNubes() {
    return vec3(frameTimeCounter * 2.2, 0.0, frameTimeCounter * 0.9);
}
// calidad: 0 = solo forma (para sombras), 1 = con detalle
float densidadNube(vec3 p, float cob, int calidad) {
    float h = (p.y - NUBE_BASE) / (NUBE_TECHO - NUBE_BASE);
    if (h <= 0.0 || h >= 1.0 || cob <= 0.01) return 0.0;
    vec3 q = (p + vientoNubes()) / vec3(300.0, 170.0, 300.0);
    float forma = ruido3(q) * 0.56 + ruido3(q * 2.17 + 1.7) * 0.29 + ruido3(q * 4.9 + 4.2) * 0.15;
    // con más cobertura las nubes suben más (cúmulos de desarrollo); base plana, cima redondeada
    float techo = mix(0.38, 1.0, cob);
    float perfil = smoothstep(0.0, 0.07, h) * smoothstep(techo, techo * 0.45, h);
    float d = (forma * perfil - (1.0 - cob)) / max(cob, 0.08);
    if (d <= 0.0) return 0.0;
    if (calidad > 0) {
        // bordes deshilachados (erosión con ruido fino que se mueve)
        vec3 r = q * 9.0 + vec3(frameTimeCounter * 0.02);
        float e = ruido3(r) * 0.62 + ruido3(r * 2.6) * 0.38;
        d -= e * 0.55 * (1.0 - clamp(d * 1.5, 0.0, 1.0)) * mix(1.0, 0.5, h);
    }
    return clamp(d, 0.0, 1.0);
}
float hg(float mu, float g) {
    float g2 = g * g;
    return (1.0 - g2) / pow(1.0 + g2 - 2.0 * g * mu, 1.5);
}
// cirros: velo alto, estirado por el viento
vec4 velo(vec3 dir, vec3 cam, vec3 sol, vec3 luzSol, vec3 amb, float cob) {
#ifdef CIRROS
    if (dir.y < 0.02) return vec4(0.0, 0.0, 0.0, 1.0);
    float t = (CIRRO_ALTURA - cam.y) / dir.y;
    vec2 p = (cam.xz + dir.xz * t + vientoNubes().xz * 3.0) / vec2(2600.0, 900.0);
    float n = texture(noisetex, p / 4.0).r * 0.5 + texture(noisetex, p).g * 0.3 + texture(noisetex, p * 3.3).b * 0.2;
    float cant = cirros() * (1.0 - cob * 0.7);
    float a = smoothstep(1.0 - cant * 0.75, 1.05 - cant * 0.4, n) * 0.55;
    a *= smoothstep(0.02, 0.15, dir.y) * (1.0 - smoothstep(4000.0, 22000.0, t));
    float mu = dot(dir, sol);
    vec3 L = luzSol * (SOL_K / PI) * 0.5 * (0.6 + 1.6 * hg(mu, 0.75) * 0.1) + amb * 0.8;
    return vec4(L * a, 1.0 - a);
#else
    return vec4(0.0, 0.0, 0.0, 1.0);
#endif
}
// devuelve (luz de las nubes, transmitancia)
vec4 nubes(vec3 dir, vec3 cam, vec3 sol, vec3 luzSol, vec3 amb, float cob, vec2 pix) {
    vec4 v = velo(dir, cam, sol, luzSol, amb, cob);
    if (dir.y < 0.01 || cob <= 0.01) return v;
    float t0 = (NUBE_BASE - cam.y) / dir.y;
    float t1 = (NUBE_TECHO - cam.y) / dir.y;
    if (t1 <= 0.0) return v;
    t0 = max(t0, 0.0);
    if (t0 > 20000.0) return v;
    t1 = min(t1, t0 + 1600.0);
    int N = CALIDAD_NUBES;
    float ds = (t1 - t0) / float(N);
    float jit = ruidoPantalla(pix);
    float mu = dot(dir, sol);
    vec3 luz = vec3(0.0);
    float T = 1.0;
    for (int i = 0; i < 48; i++) {
        if (i >= N) break;
        vec3 p = cam + dir * (t0 + ds * (float(i) + jit));
        float d = densidadNube(p, cob, 1);
        if (d <= 0.0) continue;
        // profundidad óptica hacia el sol (pasos cada vez más largos)
        float od = 0.0;
        float paso = 10.0;
        vec3 q = p;
        for (int j = 0; j < 5; j++) {
            q += sol * paso;
            od += densidadNube(q, cob, 0) * paso;
            paso *= 1.7;
        }
        float h = (p.y - NUBE_BASE) / (NUBE_TECHO - NUBE_BASE);
        // dispersión múltiple aproximada: varias "octavas" de luz cada vez más difusa
        float energia = 0.0;
        float a = 1.0;
        float b = 1.0;
        float c = 1.0;
        for (int k = 0; k < 3; k++) {
            energia += a * exp(-od * 0.055 * b) * mix(hg(mu, 0.82 * c), hg(mu, -0.25 * c), 0.3);
            a *= 0.55;
            b *= 0.45;
            c *= 0.5;
        }
        float polvo = 1.0 - exp(-d * ds * 0.09);
        vec3 L = luzSol * (SOL_K / PI) * 0.95 * energia * mix(0.6, 1.0, polvo) + amb * mix(0.3, 1.05, h) * (0.6 + 0.4 * d);
        float Ti = exp(-d * 0.085 * ds);
        luz += T * (1.0 - Ti) * L;
        T *= Ti;
        if (T < 0.01) break;
    }
    // a lo lejos se pierden en la bruma
    float lejos = 1.0 - exp(-t0 / 9000.0);
    luz *= 1.0 - lejos;
    T = mix(T, 1.0, lejos);
    // los cirros quedan detrás de los cúmulos
    return vec4(luz + v.rgb * T, T * v.a);
}
// sombra de las nubes en el suelo
float sombraNubes(vec3 pAbs, vec3 L, float cob) {
#ifdef SOMBRAS_NUBES
    if (L.y < 0.05 || cob <= 0.01) return 1.0;
    float s = 0.0;
    for (int i = 0; i < 3; i++) {
        float y = NUBE_BASE + (NUBE_TECHO - NUBE_BASE) * (0.12 + 0.18 * float(i));
        vec3 q = pAbs + L * ((y - pAbs.y) / L.y);
        s += densidadNube(q, cob, 0);
    }
    return mix(exp(-s * 1.6), 1.0, 0.15);
#else
    return 1.0;
#endif
}
#endif
