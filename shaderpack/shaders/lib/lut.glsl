#ifndef LUT_CIELO
#define LUT_CIELO
// Tabla del cielo (256x128 en la esquina de colortex7) y píxeles especiales en la fila 128:
// 0 luz directa (sol o luna) · 1 luz del sol en las nubes · 2 cielo medio (luz ambiente) · 3 exposición · 4 color del sol
uniform sampler2D colortex7;
const vec2 LUT = vec2(256.0, 128.0);

vec2 lutUV(vec3 d, vec3 s) {
    float az = atan(d.z, d.x) - atan(s.z, s.x);
    float u = fract(az / (2.0 * PI) + 0.5);
    float el = asin(clamp(d.y, -1.0, 1.0));
    float v = 0.5 + 0.5 * sign(el) * sqrt(abs(el) / (0.5 * PI));
    return vec2(u, v);
}
vec3 lutDir(vec2 uv, vec3 s) {
    float az = (uv.x - 0.5) * 2.0 * PI + atan(s.z, s.x);
    float t = uv.y * 2.0 - 1.0;
    float el = sign(t) * t * t * 0.5 * PI;
    return vec3(cos(el) * cos(az), sin(el), cos(el) * sin(az));
}
vec3 cieloLUT(vec3 d) {
    vec2 uv = lutUV(normalize(d), dirSolMundo());
    vec2 px = clamp(uv * LUT, vec2(0.5), LUT - 0.5);
    return texture(colortex7, px / vec2(viewWidth, viewHeight)).rgb;
}
vec3 especial(int i) {
    return texelFetch(colortex7, ivec2(i, 128), 0).rgb;
}
#endif
