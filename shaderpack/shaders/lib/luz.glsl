#ifndef LUZ
#define LUZ
// iluminación simple para lo que se dibuja "hacia delante" (partículas, cristal, lluvia)
vec3 luzAdelante(vec3 alb, vec3 n, vec2 lm) {
    vec3 L = dirLuzMundo();
    vec3 directa = especial(0) * (SOL_K / PI) * max(dot(n, L), 0.0) * smoothstep(0.82, 0.95, lm.y);
    vec3 amb = especial(2) * pow(lm.y, 2.5) * (0.6 + 0.4 * n.y);
    vec3 bloque = vec3(1.0, 0.58, 0.28) * (pow(lm.x, 4.0) * 0.07 + pow(lm.x, 1.5) * 0.004);
    return alb * (directa + amb + bloque + vec3(0.0006, 0.0007, 0.0009));
}
#endif
