#ifndef AGUA
#define AGUA
#ifndef RUIDO_TEX
#define RUIDO_TEX
uniform sampler2D noisetex;
#endif
// olas: varias ondas que se cruzan + ruido que se desplaza
float alturaOla(vec2 p, float t) {
    float h = 0.0;
    h += sin(dot(p, vec2(0.62, 0.78)) * 1.3 + t * 1.6) * 0.045;
    h += sin(dot(p, vec2(-0.85, 0.52)) * 2.1 + t * 2.1) * 0.03;
    h += sin(dot(p, vec2(0.33, -0.94)) * 3.7 + t * 2.9) * 0.014;
    h += texture(noisetex, p / 48.0 + t * vec2(0.012, 0.007)).r * 0.07;
    h += texture(noisetex, p / 14.0 - t * vec2(0.02, 0.031)).r * 0.035;
    return h;
}
// gotas de lluvia: anillos que se abren en celdas al azar
float anillos(vec2 p, float t) {
    vec2 c = floor(p * 1.6);
    float h = 0.0;
    for (int i = -1; i <= 1; i++) {
        for (int j = -1; j <= 1; j++) {
            vec2 cc = c + vec2(float(i), float(j));
            vec2 rnd = texture(noisetex, (cc + 0.5) / 256.0).gb;
            vec2 centro = (cc + rnd) / 1.6;
            float fase = fract(t * 1.1 + rnd.x * 7.0);
            float d = length(p - centro);
            float r = fase * 0.55;
            h += sin(clamp((d - r) * 40.0, -3.1416, 3.1416)) * (1.0 - fase) * smoothstep(0.08, 0.0, abs(d - r));
        }
    }
    return h * 0.012;
}
vec3 normalAgua(vec2 p, float t, float lluvia) {
    float e = 0.04;
    float h0 = alturaOla(p, t) + anillos(p, t) * lluvia;
    float hx = alturaOla(p + vec2(e, 0.0), t) + anillos(p + vec2(e, 0.0), t) * lluvia;
    float hz = alturaOla(p + vec2(0.0, e), t) + anillos(p + vec2(0.0, e), t) * lluvia;
    return normalize(vec3(h0 - hx, e, h0 - hz));
}
#endif
