#ifndef SOMBRAS
#define SOMBRAS
uniform sampler2DShadow shadowtex1;

vec3 distorsion(vec3 p) {
    float f = length(p.xy) * 0.9 + 0.1;
    return vec3(p.xy / f, p.z * 0.25);
}
// sombra del sol (o la luna) en una posición del jugador; 1 = iluminado
float sombra(vec3 jugador, vec3 n, vec2 pix) {
    float dist = length(jugador);
    vec3 p = jugador + n * (0.04 + dist * 0.0025);
    vec4 sp = shadowProjection * (shadowModelView * vec4(p, 1.0));
    sp.xyz /= sp.w;
    sp.xyz = distorsion(sp.xyz) * 0.5 + 0.5;
    if (sp.x <= 0.0 || sp.x >= 1.0 || sp.y <= 0.0 || sp.y >= 1.0 || sp.z >= 1.0) return 1.0;
    float borde = smoothstep(shadowDistance * 0.8, shadowDistance, dist);
    float z = sp.z - 0.00008;
#ifdef SOMBRAS_SUAVES
    float r = 1.4 / float(shadowMapResolution);
    float ang = ruidoPantalla(pix) * 6.2831;
    mat2 rot = mat2(cos(ang), -sin(ang), sin(ang), cos(ang));
    float s = 0.0;
    for (int i = 0; i < 8; i++) {
        float a = float(i) * 2.39996;
        float rr = sqrt((float(i) + 0.5) / 8.0);
        vec2 o = rot * vec2(cos(a), sin(a)) * rr * r;
        s += texture(shadowtex1, vec3(sp.xy + o, z));
    }
    s /= 8.0;
#else
    float s = texture(shadowtex1, vec3(sp.xy, z));
#endif
    return mix(s, 1.0, borde);
}
#endif
