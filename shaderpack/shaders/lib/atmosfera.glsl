#ifndef ATMOSFERA
#define ATMOSFERA
// Atmósfera física (Nishita): Rayleigh + Mie + ozono, Tierra esférica. Mismo modelo que el cielo del mod.
const float RE = 6360e3;
const float RA = 6420e3;
const float HR = 8000.0;
const float HM = 1200.0;
const vec3 BR = vec3(5.8e-6, 13.5e-6, 33.1e-6);
const vec3 BO = vec3(0.65e-6, 1.881e-6, 0.085e-6);
const vec3 ANG = vec3(0.76, 1.0, 1.38);
const float E_SOL = 20.0;

float ozono(float h) {
    return max(0.0, 1.0 - abs(h - 25000.0) / 15000.0);
}
// intersección rayo-esfera centrada en el origen: (cerca, lejos); -1 si no corta
vec2 esfera(vec3 o, vec3 d, float r) {
    float b = dot(o, d);
    float c = dot(o, o) - r * r;
    float disc = b * b - c;
    if (disc < 0.0) return vec2(-1.0);
    float s = sqrt(disc);
    return vec2(-b - s, -b + s);
}
// profundidad óptica (R, M, O) hacia la luz y cuánto llega (sombra de la Tierra suave)
vec3 profundidadLuz(vec3 p, vec3 l, out float iluminado) {
    iluminado = 1.0;
    if (dot(p, l) < 0.0) {
        float d = length(cross(p, l));
        iluminado = smoothstep(RE - 3000.0, RE + 3000.0, d);
    }
    float t = max(esfera(p, l, RA).y, 0.0);
    float ds = t / 6.0;
    vec3 acc = vec3(0.0);
    for (int j = 0; j < 6; j++) {
        vec3 q = p + l * (ds * (float(j) + 0.5));
        float h = max(length(q) - RE, 0.0);
        acc += vec3(exp(-h / HR), exp(-h / HM), ozono(h)) * ds;
    }
    return acc;
}
vec3 betaMie(float turb) {
    return 21e-6 * 0.5 * turb * ANG;
}
vec3 transmitancia(float altura, vec3 l, float turb) {
    float il;
    vec3 od = profundidadLuz(vec3(0.0, RE + altura, 0.0), l, il);
    return exp(-(BR * od.x + betaMie(turb) * 1.1 * od.y + BO * od.z)) * il;
}
vec3 dispersion(vec3 d, vec3 l, float intens, float turb, float h0) {
    vec3 BM = betaMie(turb);
    vec3 o = vec3(0.0, RE + h0, 0.0);
    float tmax = esfera(o, d, RA).y;
    vec2 g = esfera(o, d, RE);
    if (g.x > 0.0) tmax = g.x;
    vec3 od = vec3(0.0);
    vec3 sR = vec3(0.0);
    vec3 sM = vec3(0.0);
    for (int i = 0; i < 10; i++) {
        float u0 = float(i) / 10.0;
        float u1 = float(i + 1) / 10.0;
        u0 *= u0;
        u1 *= u1;
        float t = tmax * (u0 + u1) * 0.5;
        float ds = tmax * (u1 - u0);
        vec3 p = o + d * t;
        float h = max(length(p) - RE, 0.0);
        vec3 dens = vec3(exp(-h / HR), exp(-h / HM), ozono(h)) * ds;
        od += dens;
        float il;
        vec3 lt = profundidadLuz(p, l, il);
        if (il < 0.001) continue;
        vec3 tau = BR * (od.x + lt.x) + BM * 1.1 * (od.y + lt.y) + BO * (od.z + lt.z);
        vec3 at = exp(-tau) * il;
        sR += at * dens.x;
        sM += at * dens.y;
    }
    float mu = dot(d, l);
    float pR = 3.0 / (16.0 * PI) * (1.0 + mu * mu);
    float gm = 0.8;
    float pM = 3.0 / (8.0 * PI) * ((1.0 - gm * gm) * (1.0 + mu * mu)) / ((2.0 + gm * gm) * pow(1.0 + gm * gm - 2.0 * gm * mu, 1.5));
    return intens * (sR * BR * pR + sM * BM * pM);
}
// cielo despejado en la dirección d (radiancia lineal)
vec3 cieloFisico(vec3 d, vec3 s, vec3 m, float turb, float luna) {
    vec3 c = dispersion(d, s, E_SOL, turb, 300.0);
    if (s.y < -0.05 && m.y > -0.1) c += dispersion(d, m, E_SOL * 0.0016 * luna, turb, 300.0);
    float th = degrees(asin(clamp(s.y, -1.0, 1.0)));
    float amb = E_SOL * 0.0021 / (1.0 + exp(-(th + 5.5) / 1.9));
    float f = 0.6 + 0.4 * max(d.y, 0.0);
    c += amb * vec3(0.32, 0.52, 1.0) * f + vec3(0.0003, 0.0004, 0.0009);
    return c;
}
// cielo cubierto por la lluvia
vec3 nublado(vec3 c, float lluvia) {
    float y = lum(c);
    vec3 gris = y * vec3(0.95, 0.97, 1.0) * (1.0 - 0.45 * lluvia);
    return mix(c, gris, lluvia * 0.9);
}
#endif
