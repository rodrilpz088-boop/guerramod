// Exposición (el ojo se adapta al día, a la noche y a las cuevas), resplandor, curva de color y gamma.
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
uniform sampler2D colortex0;
const bool colortex0MipmapEnabled = true;
in vec2 uv;

vec3 aces(vec3 x) {
    return clamp((x * (2.51 * x + 0.03)) / (x * (2.43 * x + 0.59) + 0.14), 0.0, 1.0);
}
void main() {
    vec3 c = texture(colortex0, uv).rgb;
#ifdef BLOOM
    vec3 b = textureLod(colortex0, uv, 2.0).rgb * 0.45 + textureLod(colortex0, uv, 3.5).rgb * 0.35 + textureLod(colortex0, uv, 5.0).rgb * 0.2;
    c += b * 0.06;
#endif
    float expo = especial(3).r;
    // en cuevas el ojo se abre (las antorchas se ven bien)
#if !defined DIM_NETHER && !defined DIM_END
    expo = mix(25.0, expo, exterior());
#endif
    c *= expo * BRILLO;
    c = aces(c);
    c = pow(c, vec3(1.0 / 2.2));
    float y = lum(c);
    c = mix(vec3(y), c, SATURACION);
    vec2 v = uv - 0.5;
    c *= 1.0 - dot(v, v) * 0.35;
    c += (ruidoPantalla(gl_FragCoord.xy) - 0.5) / 255.0;
    gl_FragData[0] = vec4(c, 1.0);
}
#endif
