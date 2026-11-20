#ifndef NUBES
#define NUBES
#ifndef RUIDO_TEX
#define RUIDO_TEX
uniform sampler2D noisetex;
#endif
// Nubes volumétricas (cúmulos) entre las alturas 220 y 340 del mundo, movidas por el viento y con sombra propia.
const float NUBE_BASE = 220.0;
const float NUBE_TECHO = 340.0;

float ruido3(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    vec2 uv = i.xz + f.xz + vec2(37.0, 17.0) * i.y;
    float a = texture(noisetex, (uv + 0.5) / 256.0).r;
    float b = texture(noisetex, (uv + vec2(37.0, 17.0) + 0.5) / 256.0).r;
    return mix(a, b, f.y);
}
float densidadNube(vec3 p, float cobertura, bool detalle) {
    float h = (p.y - NUBE_BASE) / (NUBE_TECHO - NUBE_BASE);
    if (h <= 0.0 || h >= 1.0) return 0.0;
    vec3 q = p * vec3(1.0 / 170.0, 1.0 / 110.0, 1.0 / 170.0);
    q.xz += frameTimeCounter * vec2(0.006, 0.0025);
    float n = ruido3(q) * 0.62 + ruido3(q * 2.6 + 3.1) * 0.26;
    if (detalle) n += ruido3(q * 6.7 + 7.7) * 0.12;
    else n += 0.06;
    // base plana y cima redondeada, como un cúmulo
    float perfil = smoothstep(0.0, 0.12, h) * smoothstep(1.0, 0.4, h);
    float d = n * perfil - (1.0 - cobertura);
    return max(d, 0.0) * 3.0;
}
float hg(float mu, float g) {
    float g2 = g * g;
    return (1.0 - g2) / pow(1.0 + g2 - 2.0 * g * mu, 1.5);
}
// devuelve (luz de las nubes, transmitancia)
vec4 nubes(vec3 dir, vec3 cam, vec3 sol, vec3 luzSol, vec3 amb, float cobertura, vec2 pix) {
    if (dir.y < 0.015) return vec4(0.0, 0.0, 0.0, 1.0);
    float t0 = (NUBE_BASE - cam.y) / dir.y;
    float t1 = (NUBE_TECHO - cam.y) / dir.y;
    if (t1 <= 0.0) return vec4(0.0, 0.0, 0.0, 1.0);
    t0 = max(t0, 0.0);
    if (t0 > 12000.0) return vec4(0.0, 0.0, 0.0, 1.0);
    t1 = min(t1, t0 + 900.0);
    int N = CALIDAD_NUBES;
    float ds = (t1 - t0) / float(N);
    float jit = 0.5 + (ruidoPantalla(pix) - 0.5) * 0.6;
    float mu = dot(dir, sol);
    float fase = mix(hg(mu, 0.65), hg(mu, -0.2), 0.35);
    vec3 luz = vec3(0.0);
    float T = 1.0;
    for (int i = 0; i < 36; i++) {
        if (i >= N) break;
        vec3 p = cam + dir * (t0 + ds * (float(i) + jit));
        float d = densidadNube(p, cobertura, true);
        if (d <= 0.0) continue;
        float od = 0.0;
        for (int j = 1; j <= 3; j++) od += densidadNube(p + sol * (16.0 * float(j)), cobertura, false) * 16.0;
        float h = (p.y - NUBE_BASE) / (NUBE_TECHO - NUBE_BASE);
        float sombraN = exp(-od * 0.06) * (1.0 - exp(-d * 2.0)) * 2.0;
        vec3 L = luzSol * (SOL_K / PI) * 0.9 * fase * sombraN + amb * (0.45 + 0.55 * h);
        float Ti = exp(-d * 0.045 * ds);
        luz += T * (1.0 - Ti) * L;
        T *= Ti;
        if (T < 0.02) break;
    }
    // a lo lejos se pierden en la bruma
    float lejos = 1.0 - exp(-t0 / 8000.0);
    luz *= 1.0 - lejos;
    T = mix(T, 1.0, lejos);
    return vec4(luz, T);
}
#endif
